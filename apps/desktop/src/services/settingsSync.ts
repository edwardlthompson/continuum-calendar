import {
  SETTINGS_APP_DATA_FILENAME,
  decidePeerReconcile,
  defaultContinuumSettings,
  isValidEnvelope,
  normalizeCalendarNotifyPrefs,
  normalizeFirstDayOfWeek,
  prepareSettingsWrite,
  shouldApplyRemote,
  type ContinuumSettings,
  type ContinuumSettingsEnvelope,
} from '@continuum/shared'
import { ensureFreshTokens } from '../auth/googleAuth'
import { getDeviceId } from '../auth/tokenStore'
import { continuumLogger } from '../diagnostics/continuumLogger'
import { runPeerDriveOp } from './peerSyncControl'

const DRIVE = 'https://www.googleapis.com/drive/v3'
const UPLOAD = 'https://www.googleapis.com/upload/drive/v3'
const LOCAL_ENVELOPE = 'continuum.settings.envelope'
const LOCAL_ETAG = 'continuum.settings.etag'
const LOCAL_APPLIED_REV = 'continuum.settings.appliedRev'
const LOCAL_SYNC_ERROR = 'continuum.settings.syncError'
const LOCAL_PENDING_PEER = 'continuum.settings.pendingPeerPush'

let lastSyncError: string | null = localStorage.getItem(LOCAL_SYNC_ERROR)

export function markPendingPeerPush(): void {
  localStorage.setItem(LOCAL_PENDING_PEER, '1')
}

export function clearPendingPeerPush(): void {
  localStorage.removeItem(LOCAL_PENDING_PEER)
}

export function hasPendingPeerPush(): boolean {
  return localStorage.getItem(LOCAL_PENDING_PEER) === '1'
}

export function getSettingsSyncError(): string | null {
  return lastSyncError
}

function setSettingsSyncError(msg: string | null): void {
  lastSyncError = msg
  if (msg) localStorage.setItem(LOCAL_SYNC_ERROR, msg)
  else localStorage.removeItem(LOCAL_SYNC_ERROR)
}

async function bearer(): Promise<string> {
  const tokens = await ensureFreshTokens()
  if (!tokens) throw new Error('Not authenticated')
  return tokens.accessToken
}

function updatedBy() {
  return { platform: 'desktop' as const, deviceId: getDeviceId(), appVersion: '0.1.0' }
}

/** Normalize remote settings so missing keys keep defaults but explicit `false` is preserved. */
export function normalizeSettings(raw: ContinuumSettings): ContinuumSettings {
  const defaults = defaultContinuumSettings()
  return {
    ...defaults,
    ...raw,
    workingHours: { ...defaults.workingHours, ...raw.workingHours },
    showContactBirthdays:
      typeof raw.showContactBirthdays === 'boolean'
        ? raw.showContactBirthdays
        : defaults.showContactBirthdays,
    use24HourFormat:
      typeof raw.use24HourFormat === 'boolean' ? raw.use24HourFormat : defaults.use24HourFormat,
    firstDayOfWeek: normalizeFirstDayOfWeek(
      raw.firstDayOfWeek !== undefined ? raw.firstDayOfWeek : defaults.firstDayOfWeek,
    ),
    useGoogleCalendar:
      typeof raw.useGoogleCalendar === 'boolean' ? raw.useGoogleCalendar : defaults.useGoogleCalendar,
    weeklyViewDays: Math.min(14, Math.max(1, Number(raw.weeklyViewDays) || defaults.weeklyViewDays)),
    defaultSnoozeMinutes: Math.max(
      0,
      Number(raw.defaultSnoozeMinutes) || defaults.defaultSnoozeMinutes,
    ),
    notificationEnabled:
      typeof raw.notificationEnabled === 'boolean'
        ? raw.notificationEnabled
        : defaults.notificationEnabled,
    calendarNotifyPrefs: normalizeCalendarNotifyPrefs(raw.calendarNotifyPrefs),
  }
}

export function loadLocalSettings(): ContinuumSettings {
  const env = loadLocalEnvelope()
  if (!env?.settings) return defaultContinuumSettings()
  return normalizeSettings(env.settings)
}

export function loadLocalEnvelope(): ContinuumSettingsEnvelope | null {
  try {
    const raw = localStorage.getItem(LOCAL_ENVELOPE)
    if (!raw) return null
    const parsed = JSON.parse(raw) as unknown
    return isValidEnvelope(parsed) ? parsed : null
  } catch {
    return null
  }
}

function persistLocal(envelope: ContinuumSettingsEnvelope, etag: string | null): void {
  localStorage.setItem(LOCAL_ENVELOPE, JSON.stringify(envelope))
  localStorage.setItem(LOCAL_APPLIED_REV, String(envelope.revision))
  if (etag) localStorage.setItem(LOCAL_ETAG, etag)
}

export function getLastAppliedRevision(): number {
  return Number(localStorage.getItem(LOCAL_APPLIED_REV) ?? '0')
}

async function findSettingsFileId(accessToken: string): Promise<{ id: string; etag: string } | null> {
  // Drive v3 rejects unknown field selectors (e.g. files(etag)) with HTTP 400.
  const params = new URLSearchParams({
    spaces: 'appDataFolder',
    q: `name = '${SETTINGS_APP_DATA_FILENAME}' and trashed = false`,
    fields: 'files(id)',
    pageSize: '10',
  })
  const res = await fetch(`${DRIVE}/files?${params}`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  })
  if (!res.ok) {
    const detail = await res.text().catch(() => '')
    throw new Error(`Drive list failed: ${res.status}${detail ? ` ${detail.slice(0, 200)}` : ''}`)
  }
  const data = (await res.json()) as { files?: Array<{ id: string }> }
  const f = data.files?.[0]
  if (!f) return null
  return { id: f.id, etag: '' }
}

export async function pullSettingsFromDrive(): Promise<ContinuumSettingsEnvelope | null> {
  const accessToken = await bearer()
  const meta = await findSettingsFileId(accessToken)
  if (!meta) {
    setSettingsSyncError(null)
    return null
  }
  const res = await fetch(`${DRIVE}/files/${meta.id}?alt=media`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  })
  if (!res.ok) throw new Error(`Drive download failed: ${res.status}`)
  const json = (await res.json()) as unknown
  if (!isValidEnvelope(json)) throw new Error('Invalid settings envelope')
  const etag = res.headers.get('ETag') ?? meta.etag
  if (shouldApplyRemote(json, getLastAppliedRevision())) {
    persistLocal(json, etag)
  }
  setSettingsSyncError(null)
  return json
}

export async function pushSettingsPatch(patch: Partial<ContinuumSettings>): Promise<ContinuumSettingsEnvelope> {
  const accessToken = await bearer()
  let remote = loadLocalEnvelope()
  let etag = localStorage.getItem(LOCAL_ETAG)
  const meta = await findSettingsFileId(accessToken)
  if (meta) {
    const res = await fetch(`${DRIVE}/files/${meta.id}?alt=media`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    })
    if (res.ok) {
      const json = (await res.json()) as unknown
      if (isValidEnvelope(json)) {
        remote = json
        etag = res.headers.get('ETag') ?? meta.etag
      }
    }
  }

  let attempt = 0
  while (attempt < 5) {
    attempt++
    const next = await prepareSettingsWrite({
      remote,
      lastAppliedRevision: getLastAppliedRevision(),
      pendingPatch: patch,
      updatedBy: updatedBy(),
    })
    const body = JSON.stringify(next)
    if (!meta) {
      const create = await fetch(`${UPLOAD}/files?uploadType=multipart&spaces=appDataFolder`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${accessToken}`,
          'Content-Type': `multipart/related; boundary=continuum`,
        },
        body: [
          '--continuum',
          'Content-Type: application/json; charset=UTF-8',
          '',
          JSON.stringify({ name: SETTINGS_APP_DATA_FILENAME, parents: ['appDataFolder'] }),
          '--continuum',
          'Content-Type: application/json',
          '',
          body,
          '--continuum--',
        ].join('\r\n'),
      })
      if (!create.ok) throw new Error(`Drive create failed: ${create.status}`)
      const created = (await create.json()) as { id: string; etag?: string }
      persistLocal(next, created.etag ?? null)
      clearPendingPeerPush()
      setSettingsSyncError(null)
      return next
    }

    const headers: Record<string, string> = {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
    }
    if (etag) headers['If-Match'] = etag
    const put = await fetch(`${UPLOAD}/files/${meta.id}?uploadType=media`, {
      method: 'PATCH',
      headers,
      body,
    })
    if (put.status === 412) {
      const again = await pullSettingsFromDrive()
      remote = again
      etag = localStorage.getItem(LOCAL_ETAG)
      continue
    }
    if (!put.ok) throw new Error(`Drive update failed: ${put.status}`)
    const newEtag = put.headers.get('ETag')
    persistLocal(next, newEtag)
    clearPendingPeerPush()
    setSettingsSyncError(null)
    return next
  }
  throw new Error('Settings CAS exceeded retries')
}

/** Apply remote settings into local prefs when revision advances; returns settings if changed. */
export async function pollSettingsOnce(): Promise<ContinuumSettings | null> {
  try {
    const polled = await runPeerDriveOp('Settings peer poll', async () => {
      const before = getLastAppliedRevision()
      const remote = await pullSettingsFromDrive()
      if (!remote) return null
      if (remote.revision > before) return normalizeSettings(remote.settings)
      return null
    })
    return polled
  } catch (e) {
    const msg = e instanceof Error ? e.message : 'Settings sync failed'
    setSettingsSyncError(msg)
    // Transient failures are rate-limited inside runPeerDriveOp.
    if (!(e instanceof TypeError) && !(e instanceof Error && /failed to fetch/i.test(e.message))) {
      continuumLogger.error('Settings poll failed', e)
    }
    return null
  }
}

/**
 * Peer reconcile with Android via Drive App Data.
 * Seeds the remote if missing, pushes offline pending edits, otherwise pulls newer revision.
 */
export async function reconcilePeerSettings(opts?: {
  force?: boolean
}): Promise<{
  settings: ContinuumSettings
  action: ReturnType<typeof decidePeerReconcile>
}> {
  const result = await runPeerDriveOp(
    'Peer settings reconcile',
    async () => {
      const accessToken = await bearer()
      const meta = await findSettingsFileId(accessToken)
      let remote: ContinuumSettingsEnvelope | null = null
      if (meta) {
        const res = await fetch(`${DRIVE}/files/${meta.id}?alt=media`, {
          headers: { Authorization: `Bearer ${accessToken}` },
        })
        if (!res.ok) throw new Error(`Drive download failed: ${res.status}`)
        const json = (await res.json()) as unknown
        if (!isValidEnvelope(json)) throw new Error('Invalid settings envelope')
        remote = json
        const etag = res.headers.get('ETag') ?? meta.etag
        localStorage.setItem(LOCAL_ETAG, etag)
      }

      const action = decidePeerReconcile({
        hasRemote: Boolean(remote),
        remoteRevision: remote?.revision ?? 0,
        localRevision: getLastAppliedRevision(),
        pendingLocalPush: hasPendingPeerPush(),
      })

      if (action === 'seed' || action === 'push-pending') {
        const local = loadLocalSettings()
        const env = await pushSettingsPatch(local)
        return { settings: normalizeSettings(env.settings), action }
      }

      if (action === 'pull' && remote) {
        const normalized = normalizeSettings(remote.settings)
        persistLocal({ ...remote, settings: normalized }, localStorage.getItem(LOCAL_ETAG))
        setSettingsSyncError(null)
        return { settings: normalized, action }
      }

      if (remote) {
        const normalized = normalizeSettings(remote.settings)
        persistLocal({ ...remote, settings: normalized }, localStorage.getItem(LOCAL_ETAG))
        setSettingsSyncError(null)
        return { settings: normalized, action: 'noop' as const }
      }

      return { settings: loadLocalSettings(), action: 'noop' as const }
    },
    { force: opts?.force },
  )
  if (!result) {
    return { settings: loadLocalSettings(), action: 'noop' }
  }
  return result
}

/** @deprecated use reconcilePeerSettings — kept for call-site clarity during transition */
export async function pullAndApplySettingsOnSignIn(): Promise<ContinuumSettings | null> {
  try {
    const { settings } = await reconcilePeerSettings({ force: true })
    return settings
  } catch (e) {
    const msg = e instanceof Error ? e.message : 'Settings sync failed'
    setSettingsSyncError(msg)
    continuumLogger.error('Peer settings reconcile failed', e)
    throw e
  }
}

export function startSettingsPollLoop(
  onUpdate: (settings: ContinuumSettings, meta: { from: string }) => void,
  intervalMs = 20_000,
): () => void {
  let active = true
  let ticking = false
  const tick = async () => {
    if (!active || ticking || document.visibilityState !== 'visible') return
    ticking = true
    try {
      const updated = await pollSettingsOnce()
      if (updated) onUpdate(updated, { from: 'drive' })
    } finally {
      ticking = false
    }
  }
  const id = window.setInterval(() => void tick(), intervalMs)
  void tick()
  return () => {
    active = false
    window.clearInterval(id)
  }
}

export function exportSettingsJson(): string {
  const env = loadLocalEnvelope()
  return JSON.stringify(env ?? { settings: defaultContinuumSettings() }, null, 2)
}

export function importSettingsJson(raw: string): ContinuumSettings {
  const parsed = JSON.parse(raw) as unknown
  if (isValidEnvelope(parsed)) {
    persistLocal(parsed, null)
    return parsed.settings
  }
  throw new Error('Invalid settings file')
}

import {
  LOCAL_EVENTS_APP_DATA_FILENAME,
  LOCAL_EVENTS_SCHEMA_VERSION,
  decidePeerReconcile,
  emptyLocalEventsEnvelope,
  filterContinuumOwnedCalendars,
  filterContinuumOwnedEvents,
  isValidLocalEventsEnvelope,
  mergeLocalEventsPayload,
  type CalendarEvent,
  type CalendarListEntry,
  type ContinuumLocalEventsEnvelope,
  type LocalEventTombstone,
} from '@continuum/shared'
import { ensureFreshTokens } from '../auth/googleAuth'
import { getDeviceId } from '../auth/tokenStore'
import {
  loadCalendars,
  loadEvents,
  saveCalendars,
  saveEvents,
} from '../data/localStore'
import { runPeerDriveOp } from './peerSyncControl'

const DRIVE = 'https://www.googleapis.com/drive/v3'
const UPLOAD = 'https://www.googleapis.com/upload/drive/v3'
const LOCAL_ENVELOPE = 'continuum.localEvents.envelope'
const LOCAL_ETAG = 'continuum.localEvents.etag'
const LOCAL_APPLIED_REV = 'continuum.localEvents.appliedRev'
const LOCAL_PENDING = 'continuum.localEvents.pendingPeerPush'
const MAX_BYTES = 2_000_000

function updatedBy() {
  return { platform: 'desktop' as const, deviceId: getDeviceId(), appVersion: '0.1.0' }
}

async function bearer(): Promise<string> {
  const tokens = await ensureFreshTokens()
  if (!tokens) throw new Error('Not authenticated')
  return tokens.accessToken
}

export function markLocalEventsPending(): void {
  localStorage.setItem(LOCAL_PENDING, '1')
}

function clearPending(): void {
  localStorage.removeItem(LOCAL_PENDING)
}

function hasPending(): boolean {
  return localStorage.getItem(LOCAL_PENDING) === '1'
}

function getAppliedRev(): number {
  return Number(localStorage.getItem(LOCAL_APPLIED_REV) ?? '0')
}

function persistEnvelope(env: ContinuumLocalEventsEnvelope, etag: string | null): void {
  localStorage.setItem(LOCAL_ENVELOPE, JSON.stringify(env))
  localStorage.setItem(LOCAL_APPLIED_REV, String(env.revision))
  if (etag) localStorage.setItem(LOCAL_ETAG, etag)
}

function loadEnvelope(): ContinuumLocalEventsEnvelope | null {
  try {
    const raw = localStorage.getItem(LOCAL_ENVELOPE)
    if (!raw) return null
    const parsed = JSON.parse(raw) as unknown
    return isValidLocalEventsEnvelope(parsed) ? parsed : null
  } catch {
    return null
  }
}

function snapshotFromStore(): {
  calendars: CalendarListEntry[]
  events: CalendarEvent[]
  deletedIds: LocalEventTombstone[]
} {
  const prev = loadEnvelope()
  return {
    calendars: filterContinuumOwnedCalendars(loadCalendars()),
    events: filterContinuumOwnedEvents(loadEvents()),
    deletedIds: prev?.deletedIds ?? [],
  }
}

function applyPayloadToStore(payload: {
  calendars: CalendarListEntry[]
  events: CalendarEvent[]
}): void {
  const nonOwnedCals = loadCalendars().filter((c) => c.source !== 'local' && c.source !== 'ics_import')
  saveCalendars([...payload.calendars, ...nonOwnedCals])
  const nonOwnedEv = loadEvents().filter(
    (e) => e.source === 'google' || e.source === 'caldav' || e.source === 'holidays',
  )
  saveEvents([...nonOwnedEv, ...payload.events])
}

async function findFileId(accessToken: string): Promise<{ id: string } | null> {
  const params = new URLSearchParams({
    spaces: 'appDataFolder',
    q: `name = '${LOCAL_EVENTS_APP_DATA_FILENAME}' and trashed = false`,
    fields: 'files(id)',
    pageSize: '10',
  })
  const res = await fetch(`${DRIVE}/files?${params}`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  })
  if (!res.ok) {
    const detail = await res.text().catch(() => '')
    throw new Error(`Drive local-events list failed: ${res.status} ${detail.slice(0, 160)}`)
  }
  const data = (await res.json()) as { files?: Array<{ id: string }> }
  const f = data.files?.[0]
  return f ? { id: f.id } : null
}

async function downloadEnvelope(
  accessToken: string,
  id: string,
): Promise<{ env: ContinuumLocalEventsEnvelope; etag: string }> {
  const res = await fetch(`${DRIVE}/files/${id}?alt=media`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  })
  if (!res.ok) throw new Error(`Drive local-events download failed: ${res.status}`)
  const json = (await res.json()) as unknown
  if (!isValidLocalEventsEnvelope(json)) throw new Error('Invalid local-events envelope')
  return { env: json, etag: res.headers.get('ETag') ?? '' }
}

async function uploadEnvelope(
  accessToken: string,
  env: ContinuumLocalEventsEnvelope,
  existingId: string | null,
  etag: string | null,
): Promise<{ id: string; etag: string | null }> {
  const body = JSON.stringify(env)
  if (body.length > MAX_BYTES) throw new Error('Local events payload too large to sync')

  if (!existingId) {
    const create = await fetch(`${UPLOAD}/files?uploadType=multipart&spaces=appDataFolder`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'Content-Type': 'multipart/related; boundary=continuum',
      },
      body: [
        '--continuum',
        'Content-Type: application/json; charset=UTF-8',
        '',
        JSON.stringify({ name: LOCAL_EVENTS_APP_DATA_FILENAME, parents: ['appDataFolder'] }),
        '--continuum',
        'Content-Type: application/json',
        '',
        body,
        '--continuum--',
      ].join('\r\n'),
    })
    if (!create.ok) throw new Error(`Drive local-events create failed: ${create.status}`)
    const created = (await create.json()) as { id: string; etag?: string }
    return { id: created.id, etag: created.etag ?? null }
  }

  const headers: Record<string, string> = {
    Authorization: `Bearer ${accessToken}`,
    'Content-Type': 'application/json',
  }
  if (etag) headers['If-Match'] = etag
  const put = await fetch(`${UPLOAD}/files/${existingId}?uploadType=media`, {
    method: 'PATCH',
    headers,
    body,
  })
  if (put.status === 412) throw new Error('CAS_CONFLICT')
  if (!put.ok) throw new Error(`Drive local-events update failed: ${put.status}`)
  return { id: existingId, etag: put.headers.get('ETag') }
}

async function hashPayload(value: unknown): Promise<string> {
  const canonical = JSON.stringify(value)
  if (typeof crypto !== 'undefined' && crypto.subtle) {
    const data = new TextEncoder().encode(canonical)
    const digest = await crypto.subtle.digest('SHA-256', data)
    return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, '0')).join('')
  }
  let h = 0
  for (let i = 0; i < canonical.length; i++) h = (Math.imul(31, h) + canonical.charCodeAt(i)) | 0
  return `fallback-${h}`
}

async function buildNextEnvelope(
  remote: ContinuumLocalEventsEnvelope | null,
  localSnap: ReturnType<typeof snapshotFromStore>,
): Promise<ContinuumLocalEventsEnvelope> {
  const base = remote ?? emptyLocalEventsEnvelope(updatedBy())
  const merged = mergeLocalEventsPayload({
    baseCalendars: base.calendars,
    baseEvents: base.events,
    baseDeleted: base.deletedIds,
    incomingCalendars: localSnap.calendars,
    incomingEvents: localSnap.events,
    incomingDeleted: localSnap.deletedIds,
  })
  const revision = (remote?.revision ?? 0) + 1
  const contentHash = await hashPayload(merged)
  return {
    schemaVersion: LOCAL_EVENTS_SCHEMA_VERSION,
    revision,
    updatedAt: new Date().toISOString(),
    updatedBy: updatedBy(),
    contentHash,
    ...merged,
  }
}

/** Record a local delete for peer tombstone sync. */
export function recordLocalEventTombstone(calendarId: string, id: string): void {
  const env = loadEnvelope() ?? emptyLocalEventsEnvelope(updatedBy())
  const deletedIds = [
    ...env.deletedIds.filter((t) => !(t.calendarId === calendarId && t.id === id)),
    { id, calendarId, deletedAt: new Date().toISOString() },
  ]
  persistEnvelope({ ...env, deletedIds }, localStorage.getItem(LOCAL_ETAG))
  markLocalEventsPending()
}

export async function reconcileLocalEventsPeer(opts?: {
  force?: boolean
}): Promise<{
  events: CalendarEvent[]
  calendars: CalendarListEntry[]
  action: ReturnType<typeof decidePeerReconcile>
  updatedByDeviceId?: string
}> {
  const result = await runPeerDriveOp(
    'Local events peer reconcile',
    async () => {
      const accessToken = await bearer()
      const meta = await findFileId(accessToken)
      let remote: ContinuumLocalEventsEnvelope | null = null
      let etag: string | null = localStorage.getItem(LOCAL_ETAG)
      if (meta) {
        const dl = await downloadEnvelope(accessToken, meta.id)
        remote = dl.env
        etag = dl.etag || etag
      }

      const action = decidePeerReconcile({
        hasRemote: Boolean(remote),
        remoteRevision: remote?.revision ?? 0,
        localRevision: getAppliedRev(),
        pendingLocalPush: hasPending(),
      })

      const localSnap = snapshotFromStore()

      if (action === 'seed' || action === 'push-pending') {
        let attempt = 0
        let fileId = meta?.id ?? null
        while (attempt < 5) {
          attempt++
          try {
            if (fileId && attempt > 1) {
              const dl = await downloadEnvelope(accessToken, fileId)
              remote = dl.env
              etag = dl.etag
            }
            const next = await buildNextEnvelope(remote, localSnap)
            const up = await uploadEnvelope(accessToken, next, fileId, etag)
            fileId = up.id
            persistEnvelope(next, up.etag)
            applyPayloadToStore(next)
            clearPending()
            return {
              events: next.events,
              calendars: loadCalendars(),
              action,
              updatedByDeviceId: next.updatedBy?.deviceId,
            }
          } catch (e) {
            if (e instanceof Error && e.message === 'CAS_CONFLICT' && fileId) continue
            throw e
          }
        }
        throw new Error('Local events CAS exceeded retries')
      }

      if (remote && (action === 'pull' || action === 'noop')) {
        const merged = mergeLocalEventsPayload({
          baseCalendars: remote.calendars,
          baseEvents: remote.events,
          baseDeleted: remote.deletedIds,
          incomingCalendars: action === 'noop' ? localSnap.calendars : [],
          incomingEvents: action === 'noop' ? localSnap.events : [],
          incomingDeleted: action === 'noop' ? localSnap.deletedIds : [],
        })
        const env: ContinuumLocalEventsEnvelope = {
          ...remote,
          ...merged,
        }
        persistEnvelope(env, etag)
        applyPayloadToStore(merged)
        return {
          events: loadEvents(),
          calendars: loadCalendars(),
          action,
          updatedByDeviceId: remote.updatedBy?.deviceId,
        }
      }

      return { events: loadEvents(), calendars: loadCalendars(), action: 'noop' as const }
    },
    { force: opts?.force },
  )
  if (!result) {
    return { events: loadEvents(), calendars: loadCalendars(), action: 'noop' }
  }
  return result
}

export async function pushLocalEventsNow(): Promise<void> {
  markLocalEventsPending()
  await reconcileLocalEventsPeer({ force: true })
}

/** After local upsert — mark pending; caller may await push when signed in. */
export function noteLocalEventsChanged(): void {
  markLocalEventsPending()
  const snap = snapshotFromStore()
  const prev = loadEnvelope() ?? emptyLocalEventsEnvelope(updatedBy())
  persistEnvelope(
    {
      ...prev,
      calendars: snap.calendars,
      events: snap.events,
    },
    localStorage.getItem(LOCAL_ETAG),
  )
}

/** Foreground poll so Android pushes land on desktop without waiting for a settings change. */
export function startLocalEventsPollLoop(
  onUpdate: (result: {
    events: CalendarEvent[]
    calendars: CalendarListEntry[]
    action: ReturnType<typeof decidePeerReconcile>
    updatedByDeviceId?: string
  }) => void,
  intervalMs = 20_000,
): () => void {
  let active = true
  let ticking = false
  let lastRev = getAppliedRev()
  const tick = async () => {
    if (!active || ticking || document.visibilityState !== 'visible') return
    ticking = true
    try {
      const result = await reconcileLocalEventsPeer()
      const rev = getAppliedRev()
      if (rev !== lastRev || result.action === 'pull' || result.action === 'push-pending') {
        lastRev = rev
        onUpdate(result)
      }
    } catch {
      /* backoff handled in runPeerDriveOp */
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

/** Normalize editor datetime-local values to UTC ISO so Android Instant.parse works. */
export function toPeerIso(value: string, allDay?: boolean): string {
  if (!value) return value
  if (allDay || /^\d{4}-\d{2}-\d{2}$/.test(value)) return value.slice(0, 10)
  if (value.endsWith('Z') || /[+-]\d{2}:\d{2}$/.test(value)) return value
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(value)) {
    return new Date(`${value}:00`).toISOString()
  }
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/.test(value)) {
    return new Date(value).toISOString()
  }
  const t = Date.parse(value)
  return Number.isFinite(t) ? new Date(t).toISOString() : value
}

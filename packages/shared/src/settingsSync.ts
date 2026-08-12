import type { ContinuumSettings, ContinuumSettingsEnvelope } from './settings.js'
import { SETTINGS_SCHEMA_VERSION, defaultContinuumSettings } from './settings.js'

/** Stable SHA-256 hex of canonical JSON for settings object (Web Crypto). */
export async function hashSettings(settings: ContinuumSettings): Promise<string> {
  const canonical = JSON.stringify(settings, Object.keys(settings).sort())
  if (typeof crypto !== 'undefined' && crypto.subtle) {
    const data = new TextEncoder().encode(canonical)
    const digest = await crypto.subtle.digest('SHA-256', data)
    return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, '0')).join('')
  }
  // Fallback for non-crypto environments (tests / Node without subtle)
  let h = 0
  for (let i = 0; i < canonical.length; i++) h = (Math.imul(31, h) + canonical.charCodeAt(i)) | 0
  return `fallback-${h}`
}

export function mergeSettings(
  base: ContinuumSettings,
  patch: Partial<ContinuumSettings>,
): ContinuumSettings {
  return {
    ...base,
    ...patch,
    workingHours: patch.workingHours ? { ...base.workingHours, ...patch.workingHours } : base.workingHours,
    visibleCalendarIds: patch.visibleCalendarIds ?? base.visibleCalendarIds,
  }
}

/**
 * Prepare next envelope for CAS write: apply remote first if newer, then local patch.
 * Caller must PUT with If-Match: remoteEtag.
 */
export async function prepareSettingsWrite(args: {
  remote: ContinuumSettingsEnvelope | null
  lastAppliedRevision: number
  pendingPatch: Partial<ContinuumSettings>
  updatedBy: ContinuumSettingsEnvelope['updatedBy']
}): Promise<ContinuumSettingsEnvelope> {
  const remote = args.remote
  let settings = remote?.settings ?? defaultContinuumSettings()
  if (remote && remote.revision > args.lastAppliedRevision) {
    settings = remote.settings
  }
  settings = mergeSettings(settings, args.pendingPatch)
  const revision = (remote?.revision ?? 0) + 1
  const contentHash = await hashSettings(settings)
  return {
    schemaVersion: SETTINGS_SCHEMA_VERSION,
    revision,
    updatedAt: new Date().toISOString(),
    updatedBy: args.updatedBy,
    contentHash,
    settings,
  }
}

export function shouldApplyRemote(
  remote: ContinuumSettingsEnvelope,
  lastAppliedRevision: number,
): boolean {
  return remote.revision > lastAppliedRevision
}

/**
 * Peer remote protocol (desktop ↔ Android):
 * Drive App Data `continuum-settings.json` is the shared remote.
 * Either device may write (CAS revision++); the other polls and applies.
 * If the remote file is missing, the first signed-in peer seeds it from local prefs.
 * Offline local edits set a pending flag and push on next reconcile when signed in.
 */
export type PeerReconcileAction = 'seed' | 'pull' | 'push-pending' | 'noop'

export function decidePeerReconcile(args: {
  hasRemote: boolean
  remoteRevision: number
  localRevision: number
  pendingLocalPush: boolean
}): PeerReconcileAction {
  if (!args.hasRemote) return 'seed'
  if (args.pendingLocalPush || args.localRevision > args.remoteRevision) return 'push-pending'
  if (args.remoteRevision > args.localRevision) return 'pull'
  return 'noop'
}

export function isValidEnvelope(value: unknown): value is ContinuumSettingsEnvelope {
  if (!value || typeof value !== 'object') return false
  const v = value as ContinuumSettingsEnvelope
  return (
    typeof v.schemaVersion === 'number' &&
    typeof v.revision === 'number' &&
    typeof v.updatedAt === 'string' &&
    typeof v.contentHash === 'string' &&
    v.settings != null &&
    typeof v.settings === 'object'
  )
}

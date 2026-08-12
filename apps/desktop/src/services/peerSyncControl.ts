import { continuumLogger } from '../diagnostics/continuumLogger'

/** Shared gate for Drive App Data peer sync (settings + local events). */

const BASE_BACKOFF_MS = 15_000
const MAX_BACKOFF_MS = 5 * 60_000
const LOG_COOLDOWN_MS = 2 * 60_000

let backoffUntil = 0
let consecutiveFailures = 0
let lastLogAt = 0
let queue: Promise<unknown> = Promise.resolve()

export function resetPeerSyncBackoff(): void {
  backoffUntil = 0
  consecutiveFailures = 0
}

export function isPeerSyncInBackoff(): boolean {
  return Date.now() < backoffUntil
}

function noteSuccess(): void {
  consecutiveFailures = 0
  backoffUntil = 0
}

function noteFailure(label: string, err: unknown): void {
  consecutiveFailures += 1
  const exp = Math.min(5, consecutiveFailures - 1)
  const delay = Math.min(MAX_BACKOFF_MS, BASE_BACKOFF_MS * 2 ** exp)
  backoffUntil = Date.now() + delay
  const now = Date.now()
  if (consecutiveFailures === 1 || now - lastLogAt >= LOG_COOLDOWN_MS) {
    lastLogAt = now
    continuumLogger.warn(
      `${label} — retry in ${Math.round(delay / 1000)}s (quiet until then)`,
      err,
    )
  }
}

export function isTransientPeerError(err: unknown): boolean {
  if (!(err instanceof Error)) return false
  const m = err.message.toLowerCase()
  return (
    m.includes('failed to fetch') ||
    m.includes('networkerror') ||
    m.includes('network request failed') ||
    m.includes('load failed') ||
    m.includes('not authenticated') ||
    /\b(429|500|502|503|504)\b/.test(m)
  )
}

/**
 * Serialize Drive peer ops and apply exponential backoff after failures.
 * Background polls pass force=false; user Refresh / settings push pass force=true.
 */
export async function runPeerDriveOp<T>(
  label: string,
  fn: () => Promise<T>,
  opts?: { force?: boolean },
): Promise<T | null> {
  const force = opts?.force === true
  if (!force && isPeerSyncInBackoff()) return null

  let result: T | null = null
  let opError: unknown
  queue = queue
    .catch(() => undefined)
    .then(async () => {
      if (!force && isPeerSyncInBackoff()) {
        result = null
        return
      }
      try {
        result = await fn()
        noteSuccess()
      } catch (e) {
        opError = e
        noteFailure(label, e)
      }
    })
  await queue
  if (opError) throw opError
  return result
}

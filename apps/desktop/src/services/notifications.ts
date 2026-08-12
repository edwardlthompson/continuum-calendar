import type { CalendarEvent, CalendarNotifyPrefs, ContinuumSettings } from '@continuum/shared'
import { resolveCalendarNotifyPrefs } from '@continuum/shared'

const SCHEDULED = 'continuum.scheduledReminders'
const PEER_NOTIFIED = 'continuum.peerNewEventNotified'

type Key = string

const pendingTimeouts = new Map<Key, number>()

function keyOf(eventId: string, fireAt: number): Key {
  return `${eventId}:${fireAt}`
}

function saveScheduled(set: Set<string>): void {
  localStorage.setItem(SCHEDULED, JSON.stringify([...set]))
}

/** Cancel all pending reminder timers and clear the scheduled key set. */
export function clearScheduledReminders(): void {
  for (const id of pendingTimeouts.values()) {
    window.clearTimeout(id)
  }
  pendingTimeouts.clear()
  saveScheduled(new Set())
}

function clearLiveTimersOnly(): void {
  for (const id of pendingTimeouts.values()) {
    window.clearTimeout(id)
  }
  pendingTimeouts.clear()
}

/** Serialize toggle + reschedule so enable/disable cannot race. */
let scheduleChain: Promise<unknown> = Promise.resolve()

function enqueueSchedule<T>(fn: () => Promise<T>): Promise<T> {
  const run = scheduleChain.then(fn, fn)
  scheduleChain = run.then(
    () => undefined,
    () => undefined,
  )
  return run
}

type NotifiedMap = Record<string, number>

function loadPeerNotified(): NotifiedMap {
  try {
    return JSON.parse(localStorage.getItem(PEER_NOTIFIED) ?? '{}') as NotifiedMap
  } catch {
    return {}
  }
}

function savePeerNotified(map: NotifiedMap): void {
  const pruneBefore = Date.now() - 90 * 24 * 60 * 60 * 1000
  const next: NotifiedMap = {}
  for (const [k, ts] of Object.entries(map)) {
    if (ts >= pruneBefore) next[k] = ts
  }
  localStorage.setItem(PEER_NOTIFIED, JSON.stringify(next))
}

/** Request OS notification permission; false if denied or unsupported. */
export async function ensurePermission(): Promise<boolean> {
  try {
    if (!('Notification' in window)) return false
    if (Notification.permission === 'granted') return true
    if (Notification.permission === 'denied') return false
    const r = await Notification.requestPermission()
    return r === 'granted'
  } catch {
    return false
  }
}

function prefsForEvent(
  event: CalendarEvent,
  settings: Pick<ContinuumSettings, 'notificationEnabled' | 'calendarNotifyPrefs'>,
  logicalIdByCalendarId: Record<string, string>,
): CalendarNotifyPrefs & { master: boolean } {
  const logicalId = logicalIdByCalendarId[event.calendarId] ?? `local:${event.calendarId}`
  const prefs = resolveCalendarNotifyPrefs(settings.calendarNotifyPrefs, logicalId)
  return { ...prefs, master: settings.notificationEnabled }
}

/** Schedule local reminder notifications (rebuilds live timers; ignores stale disk keys). */
export async function rescheduleReminders(
  events: CalendarEvent[],
  settings: Pick<ContinuumSettings, 'notificationEnabled' | 'calendarNotifyPrefs'>,
  logicalIdByCalendarId: Record<string, string> = {},
): Promise<number> {
  return enqueueSchedule(async () => {
    if (!settings.notificationEnabled) {
      clearScheduledReminders()
      return 0
    }
    const ok = await ensurePermission()
    if (!ok) {
      clearScheduledReminders()
      return 0
    }
    // Drop dead timers; do not trust disk keys from a previous session (no live timeouts).
    clearLiveTimersOnly()
    const scheduled = new Set<string>()
    const now = Date.now()
    let count = 0
    for (const ev of events) {
      const prefs = prefsForEvent(ev, settings, logicalIdByCalendarId)
      if (!prefs.reminder) continue
      for (const rem of ev.reminders ?? []) {
        const start = new Date(ev.start).getTime()
        const fireAt = start - rem.minutes * 60_000
        if (fireAt <= now || fireAt > now + 7 * 24 * 60 * 60_000) continue
        const k = keyOf(ev.id, fireAt)
        if (scheduled.has(k)) continue
        const delay = fireAt - now
        const tid = window.setTimeout(() => {
          pendingTimeouts.delete(k)
          try {
            // eslint-disable-next-line no-new
            new Notification(ev.title, {
              body: `Starts in ${rem.minutes} minutes`,
              tag: k,
            })
          } catch {
            /* WebView may reject Notification */
          }
        }, delay)
        pendingTimeouts.set(k, tid)
        scheduled.add(k)
        count++
      }
    }
    saveScheduled(scheduled)
    return count
  })
}

/** @deprecated use rescheduleReminders */
export async function rescheduleNotifications(
  events: CalendarEvent[],
  enabled: boolean,
): Promise<number> {
  return rescheduleReminders(events, { notificationEnabled: enabled, calendarNotifyPrefs: {} })
}

/** Heads-up when a peer device adds an event (deduped by calendarId:id). */
export async function notifyNewPeerEvent(
  event: CalendarEvent,
  settings: Pick<ContinuumSettings, 'notificationEnabled' | 'calendarNotifyPrefs'>,
  logicalId: string,
): Promise<boolean> {
  if (!settings.notificationEnabled) return false
  const prefs = resolveCalendarNotifyPrefs(settings.calendarNotifyPrefs, logicalId)
  if (!prefs.newEvent) return false
  const key = `${event.calendarId}:${event.id}`
  const map = loadPeerNotified()
  if (map[key]) return false
  const ok = await ensurePermission()
  if (!ok) return false
  try {
    // eslint-disable-next-line no-new
    new Notification(event.title || 'New event', {
      body: 'Added from another device',
      tag: `peer-new:${key}`,
    })
  } catch {
    return false
  }
  map[key] = Date.now()
  savePeerNotified(map)
  return true
}

/** Diff previous vs next event ids and notify for newly seen peer events. */
export async function notifyNewPeerEventsFromPull(opts: {
  previousIds: Set<string>
  events: CalendarEvent[]
  settings: Pick<ContinuumSettings, 'notificationEnabled' | 'calendarNotifyPrefs'>
  logicalIdByCalendarId: Record<string, string>
  skipDeviceId?: string
  envelopeDeviceId?: string
}): Promise<number> {
  if (
    opts.skipDeviceId &&
    opts.envelopeDeviceId &&
    opts.skipDeviceId === opts.envelopeDeviceId
  ) {
    return 0
  }
  let n = 0
  for (const ev of opts.events) {
    const key = `${ev.calendarId}:${ev.id}`
    if (opts.previousIds.has(key)) continue
    const logicalId = opts.logicalIdByCalendarId[ev.calendarId] ?? `local:${ev.calendarId}`
    if (await notifyNewPeerEvent(ev, opts.settings, logicalId)) n++
  }
  return n
}

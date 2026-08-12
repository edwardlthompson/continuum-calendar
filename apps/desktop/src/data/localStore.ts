import type { CalendarEvent, CalendarListEntry, SyncCursor } from '@continuum/shared'
import { logicalCalendarId } from '@continuum/shared'

const EVENTS_KEY = 'continuum.events.v1'
const CALS_KEY = 'continuum.calendars.v1'
const CURSOR_KEY = 'continuum.syncCursors.v1'

function readJson<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key)
    return raw ? (JSON.parse(raw) as T) : fallback
  } catch {
    return fallback
  }
}

function writeJson(key: string, value: unknown): void {
  localStorage.setItem(key, JSON.stringify(value))
}

export function loadEvents(): CalendarEvent[] {
  return readJson(EVENTS_KEY, [])
}

export function saveEvents(events: CalendarEvent[]): void {
  writeJson(EVENTS_KEY, events)
}

export function upsertEvents(incoming: CalendarEvent[], deletedIds: string[] = []): CalendarEvent[] {
  const map = new Map(loadEvents().map((e) => [`${e.source ?? 'local'}:${e.calendarId}:${e.id}`, e]))
  for (const id of deletedIds) {
    for (const key of [...map.keys()]) {
      if (key.endsWith(`:${id}`)) map.delete(key)
    }
  }
  for (const e of incoming) {
    map.set(`${e.source ?? 'local'}:${e.calendarId}:${e.id}`, e)
  }
  const next = [...map.values()]
  saveEvents(next)
  return next
}

function eventOverlapsWindow(e: CalendarEvent, minMs: number, maxMs: number): boolean {
  const startRaw = e.allDay ? `${e.start.slice(0, 10)}T00:00:00` : e.start
  const endRaw = e.allDay ? `${e.end.slice(0, 10)}T23:59:59` : e.end
  const start = Date.parse(startRaw)
  const end = Date.parse(endRaw)
  if (!Number.isFinite(start)) return false
  const endMs = Number.isFinite(end) ? end : start
  return start < maxMs && endMs > minMs
}

/**
 * Full Google window pull: upsert fetched rows and drop local Google events for this
 * calendar that overlap the window but are no longer returned (deleted on another device).
 */
export function replaceGoogleEventsInWindow(
  calendarId: string,
  fetched: CalendarEvent[],
  timeMin: Date,
  timeMax: Date,
  aliasCalendarIds: string[] = [],
): CalendarEvent[] {
  const calIds = new Set([calendarId, ...aliasCalendarIds].filter(Boolean))
  const fetchedIds = new Set(fetched.map((e) => e.id))
  const minMs = timeMin.getTime()
  const maxMs = timeMax.getTime()
  const staleIds: string[] = []
  for (const e of loadEvents()) {
    if (e.source !== 'google' || !calIds.has(e.calendarId)) continue
    if (fetchedIds.has(e.id)) continue
    if (eventOverlapsWindow(e, minMs, maxMs)) staleIds.push(e.id)
  }
  return upsertEvents(fetched, staleIds)
}

export function deleteLocalEvent(calendarId: string, eventId: string, source?: string): CalendarEvent[] {
  const next = loadEvents().filter(
    (e) => !(e.id === eventId && e.calendarId === calendarId && (source ? e.source === source : true)),
  )
  saveEvents(next)
  return next
}

export function loadCalendars(): CalendarListEntry[] {
  const existing = readJson<CalendarListEntry[]>(CALS_KEY, [])
  if (existing.length) return existing
  const seed: CalendarListEntry[] = [
    {
      id: 'primary',
      accountId: 'google-default',
      displayName: 'Google Primary',
      color: '#0f6e8c',
      visible: true,
      writable: true,
      source: 'google',
      logicalId: logicalCalendarId('google', 'primary'),
    },
    {
      id: 'local-default',
      accountId: 'local',
      displayName: 'Local',
      color: '#5b6b82',
      visible: true,
      writable: true,
      source: 'local',
      logicalId: logicalCalendarId('local', 'local-default'),
    },
    {
      id: 'holidays',
      accountId: 'holidays',
      displayName: 'Holidays',
      color: '#c45c26',
      visible: true,
      writable: false,
      source: 'holidays',
      logicalId: logicalCalendarId('holidays', 'holidays'),
    },
  ]
  writeJson(CALS_KEY, seed)
  return seed
}

export function saveCalendars(calendars: CalendarListEntry[]): void {
  writeJson(CALS_KEY, calendars)
}

export function loadCursors(): SyncCursor[] {
  return readJson(CURSOR_KEY, [])
}

export function saveCursor(cursor: SyncCursor): void {
  const all = loadCursors().filter((c) => c.calendarId !== cursor.calendarId || c.source !== cursor.source)
  all.push(cursor)
  writeJson(CURSOR_KEY, all)
}

export function mockSeedEvents(): CalendarEvent[] {
  const startOfToday = new Date()
  startOfToday.setHours(0, 0, 0, 0)
  const at = (dayOffset: number, hour: number, minute = 0) => {
    const d = new Date(startOfToday)
    d.setDate(d.getDate() + dayOffset)
    d.setHours(hour, minute, 0, 0)
    return d.toISOString()
  }
  return [
    {
      id: 'mock-1',
      calendarId: 'local-default',
      source: 'local',
      title: 'Focus block',
      start: at(0, 10),
      end: at(0, 11, 30),
      reminders: [{ minutes: 10, method: 'popup' }],
    },
    {
      id: 'mock-2',
      calendarId: 'primary',
      source: 'google',
      title: 'Team sync',
      start: at(1, 14),
      end: at(1, 15),
      attendees: [{ email: 'teammate@example.com', displayName: 'Teammate' }],
      reminders: [{ minutes: 10, method: 'popup' }],
    },
    {
      id: 'mock-3',
      calendarId: 'local-default',
      source: 'local',
      title: 'Doctor',
      start: at(3, 9),
      end: at(3, 10),
      reminders: [{ minutes: 60, method: 'popup' }],
    },
  ]
}

export function ensureSeededEvents(): CalendarEvent[] {
  const existing = loadEvents()
  if (existing.length) return existing
  const seed = mockSeedEvents()
  saveEvents(seed)
  return seed
}

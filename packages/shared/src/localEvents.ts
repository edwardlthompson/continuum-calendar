import type { CalendarEvent } from './events.js'
import type { CalendarListEntry } from './calendars.js'
import type { SettingsUpdatedBy } from './settings.js'
import { LOCAL_EVENTS_SCHEMA_VERSION } from './settings.js'

export interface LocalEventTombstone {
  id: string
  calendarId: string
  deletedAt: string
}

/** Drive App Data envelope for Continuum-owned local calendars/events. */
export interface ContinuumLocalEventsEnvelope {
  schemaVersion: number
  revision: number
  updatedAt: string
  updatedBy: SettingsUpdatedBy
  contentHash: string
  calendars: CalendarListEntry[]
  events: CalendarEvent[]
  deletedIds: LocalEventTombstone[]
}

export function emptyLocalEventsEnvelope(
  updatedBy: SettingsUpdatedBy,
): ContinuumLocalEventsEnvelope {
  return {
    schemaVersion: LOCAL_EVENTS_SCHEMA_VERSION,
    revision: 0,
    updatedAt: new Date().toISOString(),
    updatedBy,
    contentHash: '',
    calendars: [],
    events: [],
    deletedIds: [],
  }
}

export function eventKey(calendarId: string, id: string): string {
  return `${calendarId}:${id}`
}

function eventUpdatedMs(ev: CalendarEvent): number {
  if (ev.updated) {
    const t = Date.parse(ev.updated)
    if (Number.isFinite(t)) return t
  }
  const start = Date.parse(ev.start)
  return Number.isFinite(start) ? start : 0
}

/** Keep Continuum-owned rows only (local + ics_import). */
export function filterContinuumOwnedCalendars(cals: CalendarListEntry[]): CalendarListEntry[] {
  return cals.filter((c) => c.source === 'local' || c.source === 'ics_import')
}

export function filterContinuumOwnedEvents(events: CalendarEvent[]): CalendarEvent[] {
  return events.filter((e) => e.source === 'local' || e.source === 'ics_import' || !e.source)
}

/**
 * Merge remote + local peer payloads (LWW by `updated`, tombstones win when newer).
 */
export function mergeLocalEventsPayload(args: {
  baseCalendars: CalendarListEntry[]
  baseEvents: CalendarEvent[]
  baseDeleted: LocalEventTombstone[]
  incomingCalendars: CalendarListEntry[]
  incomingEvents: CalendarEvent[]
  incomingDeleted: LocalEventTombstone[]
}): {
  calendars: CalendarListEntry[]
  events: CalendarEvent[]
  deletedIds: LocalEventTombstone[]
} {
  const calMap = new Map<string, CalendarListEntry>()
  for (const c of [
    ...filterContinuumOwnedCalendars(args.baseCalendars),
    ...filterContinuumOwnedCalendars(args.incomingCalendars),
  ]) {
    calMap.set(c.logicalId || `${c.source}:${c.id}`, c)
  }

  const tomb = new Map<string, LocalEventTombstone>()
  for (const t of [...args.baseDeleted, ...args.incomingDeleted]) {
    const k = eventKey(t.calendarId, t.id)
    const prev = tomb.get(k)
    if (!prev || Date.parse(t.deletedAt) >= Date.parse(prev.deletedAt)) tomb.set(k, t)
  }

  const evMap = new Map<string, CalendarEvent>()
  for (const e of [
    ...filterContinuumOwnedEvents(args.baseEvents),
    ...filterContinuumOwnedEvents(args.incomingEvents),
  ]) {
    const k = eventKey(e.calendarId, e.id)
    const del = tomb.get(k)
    if (del && Date.parse(del.deletedAt) >= eventUpdatedMs(e)) continue
    const prev = evMap.get(k)
    if (!prev || eventUpdatedMs(e) >= eventUpdatedMs(prev)) evMap.set(k, e)
  }

  // Drop tombstones older than 90 days to bound size.
  const cutoff = Date.now() - 90 * 24 * 60 * 60 * 1000
  const deletedIds = [...tomb.values()].filter((t) => Date.parse(t.deletedAt) >= cutoff)

  return {
    calendars: [...calMap.values()],
    events: [...evMap.values()],
    deletedIds,
  }
}

export function isValidLocalEventsEnvelope(value: unknown): value is ContinuumLocalEventsEnvelope {
  if (!value || typeof value !== 'object') return false
  const v = value as ContinuumLocalEventsEnvelope
  return (
    typeof v.schemaVersion === 'number' &&
    typeof v.revision === 'number' &&
    typeof v.updatedAt === 'string' &&
    typeof v.contentHash === 'string' &&
    Array.isArray(v.calendars) &&
    Array.isArray(v.events) &&
    Array.isArray(v.deletedIds)
  )
}

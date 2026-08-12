import type { AgendaDaySection, CalendarEvent } from './events.js'

/** Today's agenda display phase after events / working hours. */
export type TodayAgendaPhase = 'active' | 'open' | 'empty'

/** Calendar date key in local timezone (never UTC via toISOString). */
function toDateKey(iso: string): string {
  const trimmed = iso.trim()
  if (/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) return trimmed
  const d = new Date(trimmed)
  if (!Number.isFinite(d.getTime())) return localDateKey()
  return localDateKey(d.getTime())
}

function addDays(dateKey: string, n: number): string {
  const d = new Date(`${dateKey}T12:00:00`)
  d.setDate(d.getDate() + n)
  return localDateKey(d.getTime())
}

/** Local calendar date YYYY-MM-DD for a timestamp. */
export function localDateKey(ms: number = Date.now()): string {
  const d = new Date(ms)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** True for flagged all-day rows or date-only ISO starts. */
export function isAllDayEvent(event: CalendarEvent): boolean {
  if (event.allDay) return true
  return /^\d{4}-\d{2}-\d{2}$/.test(event.start.trim())
}

/** Day has at least one timed (non-all-day) event. */
export function dayHasTimedEvents(events: CalendarEvent[]): boolean {
  return events.some((e) => !isAllDayEvent(e))
}

/**
 * Show Continuum “Open” when the day is empty or only has all-day / anniversary rows
 * (all-day does not fill the schedule).
 */
export function dayShouldShowOpen(events: CalendarEvent[]): boolean {
  if (events.length === 0) return true
  return !dayHasTimedEvents(events)
}

/** Inclusive end instant used for past checks (all-day → end of end date, local). */
export function eventEndMs(event: CalendarEvent): number {
  if (isAllDayEvent(event)) {
    const endKey = toDateKey(event.end || event.start)
    return new Date(`${endKey}T23:59:59.999`).getTime()
  }
  const t = new Date(event.end).getTime()
  return Number.isFinite(t) ? t : 0
}

/** True when the event has fully ended before now. */
export function isEventPast(event: CalendarEvent, nowMs: number): boolean {
  return eventEndMs(event) < nowMs
}

/** Split a day's events into past vs not-yet-ended, chrono by start. */
export function splitDayEventsAtNow(
  events: CalendarEvent[],
  nowMs: number,
): { past: CalendarEvent[]; future: CalendarEvent[] } {
  const sorted = [...events].sort(
    (a, b) => a.start.localeCompare(b.start) || a.end.localeCompare(b.end),
  )
  const past: CalendarEvent[] = []
  const future: CalendarEvent[] = []
  for (const ev of sorted) {
    if (isEventPast(ev, nowMs)) past.push(ev)
    else future.push(ev)
  }
  return { past, future }
}

/** Parse HH:mm on dateKey to local ms; invalid → 17:00. */
export function workingHoursEndMs(dateKey: string, workingHoursEnd: string): number {
  const m = /^(\d{1,2}):(\d{2})$/.exec((workingHoursEnd || '17:00').trim())
  const h = m ? Math.min(23, Math.max(0, Number(m[1]))) : 17
  const min = m ? Math.min(59, Math.max(0, Number(m[2]))) : 0
  const d = new Date(`${dateKey}T00:00:00`)
  d.setHours(h, min, 0, 0)
  return d.getTime()
}

/**
 * Today-only agenda phase:
 * - active: any event not yet ended
 * - open: all ended (or none) and now before workingHours.end
 * - empty: all ended (or none) and now at/after workingHours.end
 */
export function todayAgendaPhase(
  events: CalendarEvent[],
  nowMs: number,
  workingHoursEnd: string,
  todayKey: string = localDateKey(nowMs),
): TodayAgendaPhase {
  const { future } = splitDayEventsAtNow(events, nowMs)
  if (future.length > 0) return 'active'
  return nowMs < workingHoursEndMs(todayKey, workingHoursEnd) ? 'open' : 'empty'
}

/** Build agenda day sections; optionally include empty days. */
export function buildAgendaSections(
  events: CalendarEvent[],
  rangeStart: string,
  rangeEnd: string,
  showEmptyDays: boolean,
): AgendaDaySection[] {
  const byDay = new Map<string, CalendarEvent[]>()
  for (const ev of events) {
    const key = toDateKey(ev.start)
    const list = byDay.get(key) ?? []
    list.push(ev)
    byDay.set(key, list)
  }

  const sections: AgendaDaySection[] = []
  let cursor = rangeStart
  while (cursor <= rangeEnd) {
    const dayEvents = byDay.get(cursor) ?? []
    const isEmpty = dayEvents.length === 0
    if (!isEmpty || showEmptyDays) {
      sections.push({
        date: cursor,
        events: dayEvents.sort((a, b) => a.start.localeCompare(b.start)),
        isEmpty,
      })
    }
    cursor = addDays(cursor, 1)
  }
  return sections
}

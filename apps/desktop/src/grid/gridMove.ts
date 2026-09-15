import type { CalendarEvent, CalendarListEntry } from '@continuum/shared'

const FALLBACK_MIN = '06:00:00'
const FALLBACK_MAX = '22:00:00'
const HM = /^(\d{1,2}):(\d{2})(?::(\d{2}))?$/

export type EventSaveResult = 'saved' | 'conflict' | 'failed'

export type GridMoveArg = {
  eventId: string
  start: Date | null
  end: Date | null
  allDay: boolean
  startStr?: string
  endStr?: string
}

export function ignoreGridMove(arg: GridMoveArg): boolean {
  if (!arg.eventId) return true
  if (arg.allDay) return !(arg.startStr || arg.start)
  return !arg.start
}

export function isSeriesEvent(event: CalendarEvent): boolean {
  return Boolean(event.recurringEventId || event.recurrence?.length)
}

export function isGridEventEditable(
  event: CalendarEvent,
  calendars: CalendarListEntry[],
): boolean {
  if (!event.id || event.readOnly) return false
  const cal = calendars.find((c) => c.id === event.calendarId)
  if (!cal) return true
  return cal.writable !== false && cal.source !== 'holidays'
}

function toFcTime(raw: string | undefined): string | null {
  if (!raw) return null
  const m = HM.exec(raw.trim())
  if (!m) return null
  const h = Number(m[1])
  const min = Number(m[2])
  const sec = Number(m[3] ?? '0')
  if (h > 23 || min > 59 || sec > 59) return null
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(h)}:${pad(min)}:${pad(sec)}`
}

function toMinutes(hhmmss: string): number {
  const [h, m, s] = hhmmss.split(':').map(Number)
  return h * 60 + m + s / 60
}

/** FullCalendar slot window from working hours; inverted/empty → 06:00–22:00. */
export function slotWindowFromWorkingHours(hours?: {
  start?: string
  end?: string
}): { slotMinTime: string; slotMaxTime: string } {
  const min = toFcTime(hours?.start)
  const max = toFcTime(hours?.end)
  if (!min || !max || toMinutes(min) >= toMinutes(max)) {
    return { slotMinTime: FALLBACK_MIN, slotMaxTime: FALLBACK_MAX }
  }
  return { slotMinTime: min, slotMaxTime: max }
}

/** Persist payload from a FullCalendar drop/resize. Timed events use ISO instants. */
export function eventTimesFromFcEvent(arg: GridMoveArg): { start: string; end: string } | null {
  if (ignoreGridMove(arg)) return null
  if (arg.allDay) {
    const start = (arg.startStr || '').slice(0, 10)
    if (!start) return null
    const end = (arg.endStr || start).slice(0, 10)
    return { start, end }
  }
  if (!arg.start) return null
  return {
    start: arg.start.toISOString(),
    end: (arg.end ?? arg.start).toISOString(),
  }
}

export function applyGridMove(
  event: CalendarEvent,
  arg: GridMoveArg,
): CalendarEvent | null {
  const times = eventTimesFromFcEvent(arg)
  if (!times) return null
  return { ...event, start: times.start, end: times.end, allDay: arg.allDay }
}

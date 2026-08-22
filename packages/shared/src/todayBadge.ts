import type { CalendarEvent } from './events.js'

function dayKey(ms: number): string {
  const d = new Date(ms)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function eventDay(iso: string): string {
  if (/^\d{4}-\d{2}-\d{2}$/.test(iso)) return iso
  const t = new Date(iso).getTime()
  return Number.isFinite(t) ? dayKey(t) : iso.slice(0, 10)
}

function eventEndMs(event: CalendarEvent): number {
  if (event.allDay || /^\d{4}-\d{2}-\d{2}$/.test(event.end)) {
    const d = new Date(`${event.end.slice(0, 10)}T23:59:59`)
    return d.getTime()
  }
  const t = new Date(event.end).getTime()
  return Number.isFinite(t) ? t : 0
}

/** Timed + all-day events still on today that have not ended. */
export function remainingTodayCount(events: CalendarEvent[], nowMs = Date.now()): number {
  const today = dayKey(nowMs)
  return events.filter((e) => eventDay(e.start) === today && eventEndMs(e) > nowMs).length
}

export function nextRemainingTickMs(events: CalendarEvent[], nowMs = Date.now()): number {
  const today = dayKey(nowMs)
  const ends = events
    .filter((e) => eventDay(e.start) === today && eventEndMs(e) > nowMs)
    .map((e) => eventEndMs(e))
    .sort((a, b) => a - b)
  return ends[0] ?? nowMs + 60_000
}

import type { CalendarEvent, CalendarSource } from '@continuum/shared'
import { loadEvents, saveEvents } from './localStore'

/** Drop prior rows for one calendar+source, then persist the incoming set. */
export function replaceEventsForCalendar(
  calendarId: string,
  source: CalendarSource,
  incoming: CalendarEvent[],
): CalendarEvent[] {
  const kept = loadEvents().filter((e) => !(e.calendarId === calendarId && (e.source ?? 'local') === source))
  const next = [...kept, ...incoming]
  saveEvents(next)
  return next
}

import type { CalendarListEntry, CalendarSource } from '@continuum/shared'

export function resolveDefaultWriteCalendar(
  calendars: CalendarListEntry[],
  defaultWriteCalendarId: string,
): CalendarListEntry | undefined {
  const preferred = calendars.find(
    (c) => c.logicalId === defaultWriteCalendarId && c.writable !== false,
  )
  if (preferred) return preferred
  return calendars.find((c) => c.writable !== false && c.source !== 'holidays')
}

export function newEventDefaults(
  calendars: CalendarListEntry[],
  defaultWriteCalendarId: string,
): { calendarId: string; source: CalendarSource; logicalId: string } {
  const cal = resolveDefaultWriteCalendar(calendars, defaultWriteCalendarId)
  return {
    calendarId: cal?.id ?? 'local-default',
    source: cal?.source ?? 'local',
    logicalId: cal?.logicalId ?? 'local:local-default',
  }
}

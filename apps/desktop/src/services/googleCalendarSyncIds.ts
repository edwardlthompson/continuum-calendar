import type { CalendarListEntry } from '@continuum/shared'

/** Primary always; hidden secondary Google calendars stay display-only (no API pull). */
export function googleCalendarIdsToSync(calendars: CalendarListEntry[]): string[] {
  const google = calendars.filter((c) => c.source === 'google')
  const primary =
    google.find((c) => c.logicalId === 'google:primary') ?? google.find((c) => c.id === 'primary')
  const ids = new Set<string>()
  if (primary) ids.add(primary.id)
  else ids.add('primary')
  for (const c of google) {
    if (c.visible) ids.add(c.id)
  }
  return [...ids]
}

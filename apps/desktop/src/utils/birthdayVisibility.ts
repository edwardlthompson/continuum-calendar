import { isBirthdayCalendarEntry, type CalendarListEntry } from '@continuum/shared'
import { saveCalendars } from '../data/localStore'

/** Align birthday calendar checkbox visibility with Continuum `showContactBirthdays`. */
export function applyBirthdayCalendarVisibility(
  calendars: CalendarListEntry[],
  showContactBirthdays: boolean,
): CalendarListEntry[] {
  let changed = false
  const next = calendars.map((c) => {
    if (!isBirthdayCalendarEntry(c)) return c
    if (c.visible === showContactBirthdays) return c
    changed = true
    return { ...c, visible: showContactBirthdays }
  })
  if (changed) saveCalendars(next)
  return next
}

/** Detect Google automated birthday calendars / events — not manual birthday parties. */

const NAME_HINTS = [
  'birthday',
  'birthdays',
  'geburtstag',
  'geburtstage',
  'anniversaire',
  'cumpleaños',
  'compleanno',
  'verjaardag',
  'urodziny',
]

/** Contacts Birthdays only — not Holidays (`en.usa#holiday@group…`). */
const GOOGLE_BIRTHDAY_ID_HINTS = [
  'addressbook#group.v.calendar.google.com',
  'addressbook#group.calendar.google.com',
]

const HOLIDAY_ID_HINTS = ['holiday@group', '#wendy.h@example.net', 'holidays']

export function isBirthdayCalendarLabel(nameOrId: string | undefined | null): boolean {
  const value = (nameOrId ?? '').trim().toLowerCase()
  if (!value) return false
  if (HOLIDAY_ID_HINTS.some((h) => value.includes(h))) return false
  // Exact title match only (dedicated Birthdays calendar).
  if (NAME_HINTS.some((h) => value === h)) return true
  if (GOOGLE_BIRTHDAY_ID_HINTS.some((h) => value.includes(h))) return true
  return false
}

export function isBirthdayCalendarEntry(entry: {
  id?: string
  displayName?: string
  logicalId?: string
}): boolean {
  return (
    isBirthdayCalendarLabel(entry.displayName) ||
    isBirthdayCalendarLabel(entry.id) ||
    isBirthdayCalendarLabel(entry.logicalId)
  )
}

/**
 * Google Calendar API automated contact birthdays (`eventType === 'birthday'`).
 * Manual yearly “Sofia's Birthday” events are `default` and must stay visible.
 */
export function isContactBirthdayEvent(event: {
  title?: string
  allDay?: boolean
  eventType?: string
  recurrence?: string | string[]
}): boolean {
  return (event.eventType ?? '').toLowerCase() === 'birthday'
}

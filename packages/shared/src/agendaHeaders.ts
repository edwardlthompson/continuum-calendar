/** Continuum agenda day headers — keep in lockstep with Android Formatter.getAgendaSectionTitle. */

const WEEKDAYS_LONG = [
  'Sunday',
  'Monday',
  'Tuesday',
  'Wednesday',
  'Thursday',
  'Friday',
  'Saturday',
] as const

const WEEKDAYS_SHORT = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'] as const

const MONTHS_LONG = [
  'January',
  'February',
  'March',
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December',
] as const

/** `YYYY-MM-DD` → Date at local noon. */
function parseDay(dateKey: string): Date {
  return new Date(`${dateKey}T12:00:00`)
}

export function formatAgendaSectionTitle(dateKey: string, todayKey: string): string {
  const d = parseDay(dateKey)
  const weekdayLong = WEEKDAYS_LONG[d.getDay()] ?? ''
  const weekdayShort = WEEKDAYS_SHORT[d.getDay()] ?? ''
  const month = MONTHS_LONG[d.getMonth()] ?? ''
  const day = d.getDate()
  if (dateKey === todayKey) {
    return `Today · ${weekdayLong}, ${month} ${day}`
  }
  return `${weekdayShort} · ${month} ${day}`
}

/** Teal used for empty “Open” day placeholders (Android Continuum). */
export const CONTINUUM_OPEN_DAY_COLOR = '#0F6E8C'

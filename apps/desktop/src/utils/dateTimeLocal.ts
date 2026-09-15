/** Parse/join editor date+time without native datetime-local (broken on WebKitGTK). */

export function pad2(n: number): string {
  return String(n).padStart(2, '0')
}

/** Local `YYYY-MM-DDTHH:mm` from a Date (wall clock, not UTC). */
export function toLocalDateTimeValue(d: Date): string {
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}T${pad2(d.getHours())}:${pad2(d.getMinutes())}`
}

/** Local `YYYY-MM-DD` or `YYYY-MM-DDTHH:mm` from an ISO instant or date-only string. */
export function toLocalInput(iso?: string, allDay?: boolean): string {
  if (!iso) return ''
  if (allDay) return iso.slice(0, 10)
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso.slice(0, 16)
  return toLocalDateTimeValue(d)
}

export function splitDateTime(value: string): { date: string; time: string } {
  if (!value) return { date: '', time: '09:00' }
  if (value.includes('T')) {
    const [d, t = '09:00'] = value.split('T')
    return { date: d.slice(0, 10), time: t.slice(0, 5) || '09:00' }
  }
  return { date: value.slice(0, 10), time: '09:00' }
}

export function joinDateTime(date: string, time: string, allDay: boolean): string {
  if (!date) return ''
  if (allDay) return date.slice(0, 10)
  return `${date.slice(0, 10)}T${(time || '09:00').slice(0, 5)}`
}

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/
const ISO_TIME = /^\d{2}:\d{2}$/

export function isIsoDate(s: string): boolean {
  if (!ISO_DATE.test(s)) return false
  const [y, m, d] = s.split('-').map(Number)
  const dt = new Date(y, m - 1, d)
  return dt.getFullYear() === y && dt.getMonth() === m - 1 && dt.getDate() === d
}

export function isIsoTime(s: string): boolean {
  if (!ISO_TIME.test(s)) return false
  const [h, min] = s.split(':').map(Number)
  return h >= 0 && h <= 23 && min >= 0 && min <= 59
}

export const HOUR_OPTIONS = Array.from({ length: 24 }, (_, i) => pad2(i))
export const MINUTE_OPTIONS = Array.from({ length: 60 }, (_, i) => pad2(i))

export const WEEKDAY_LABELS = ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa']

export function weekdayLabels(firstDayOfWeek = 0): string[] {
  const start = ((firstDayOfWeek % 7) + 7) % 7
  return [...WEEKDAY_LABELS.slice(start), ...WEEKDAY_LABELS.slice(0, start)]
}

/** Sunday-based month cells padded to full weeks; `null` is a leading/trailing blank. */
export function monthCells(
  year: number,
  month0: number,
  firstDayOfWeek = 0,
): Array<{ iso: string; day: number } | null> {
  const first = new Date(year, month0, 1)
  const startPad = (first.getDay() - (((firstDayOfWeek % 7) + 7) % 7) + 7) % 7
  const daysInMonth = new Date(year, month0 + 1, 0).getDate()
  const cells: Array<{ iso: string; day: number } | null> = Array.from({ length: startPad }, () => null)
  for (let day = 1; day <= daysInMonth; day += 1) {
    cells.push({ iso: `${year}-${pad2(month0 + 1)}-${pad2(day)}`, day })
  }
  while (cells.length % 7 !== 0) cells.push(null)
  return cells
}

export function shiftMonth(year: number, month0: number, delta: number): { year: number; month0: number } {
  const d = new Date(year, month0 + delta, 1)
  return { year: d.getFullYear(), month0: d.getMonth() }
}

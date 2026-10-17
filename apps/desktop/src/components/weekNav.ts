import { localDateKey } from '@continuum/shared'

function startOfDay(d: Date): Date {
  const x = new Date(d)
  x.setHours(0, 0, 0, 0)
  x.setMilliseconds(0)
  return x
}

/** Week start from [firstDayOfWeek] when rollingWeekFromToday is false. */
export function weekAnchor(fromToday: boolean, firstDayOfWeek = 0, base = new Date()): Date {
  const d = startOfDay(base)
  if (fromToday) return d
  const day = d.getDay()
  const offset = (day - firstDayOfWeek + 7) % 7
  d.setDate(d.getDate() - offset)
  return d
}

/** YYYY-MM-DD for the leftmost column of the week grid. */
export function weekViewAnchorKey(
  fromToday: boolean,
  firstDayOfWeek = 0,
  base = new Date(),
): string {
  return localDateKey(weekAnchor(fromToday, firstDayOfWeek, base).getTime())
}

/** Shift a YYYY-MM-DD key by whole days (noon-based to avoid DST edges). */
export function shiftDateKey(dateKey: string, days: number): string {
  const d = new Date(`${dateKey}T12:00:00`)
  if (!Number.isFinite(d.getTime())) return dateKey
  d.setDate(d.getDate() + days)
  return localDateKey(d.getTime())
}

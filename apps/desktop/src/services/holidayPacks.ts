import type { CalendarEvent } from '@continuum/shared'

export type HolidayPackId = 'none' | 'us' | 'ca' | 'gb' | 'de'

export const HOLIDAY_PACKS: { id: HolidayPackId; label: string }[] = [
  { id: 'none', label: 'No holiday pack' },
  { id: 'us', label: 'United States' },
  { id: 'ca', label: 'Canada' },
  { id: 'gb', label: 'United Kingdom' },
  { id: 'de', label: 'Germany' },
]

function nthWeekday(year: number, month: number, dow: number, nth: number): string {
  const first = new Date(year, month, 1)
  const day = 1 + ((dow - first.getDay() + 7) % 7) + (nth - 1) * 7
  return iso(year, month, day)
}

function lastWeekday(year: number, month: number, dow: number): string {
  const last = new Date(year, month + 1, 0)
  return iso(year, month, last.getDate() - ((last.getDay() - dow + 7) % 7))
}

function iso(year: number, month: number, day: number): string {
  return `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

function ev(id: string, title: string, date: string): CalendarEvent {
  return {
    id,
    calendarId: 'holidays',
    source: 'holidays',
    title,
    start: date,
    end: date,
    allDay: true,
    readOnly: true,
    busy: false,
  }
}

export function holidayEventsForYear(pack: HolidayPackId, year: number): CalendarEvent[] {
  if (pack === 'none') return []
  const rows: Array<[string, string, string]> = []
  rows.push(['nyd', 'New Year’s Day', iso(year, 0, 1)])
  rows.push(['xmas', 'Christmas Day', iso(year, 11, 25)])
  if (pack === 'us') {
    rows.push(['mlk', 'Martin Luther King Jr. Day', nthWeekday(year, 0, 1, 3)])
    rows.push(['pres', 'Presidents’ Day', nthWeekday(year, 1, 1, 3)])
    rows.push(['mem', 'Memorial Day', lastWeekday(year, 4, 1)])
    rows.push(['jun', 'Juneteenth', iso(year, 5, 19)])
    rows.push(['jul4', 'Independence Day', iso(year, 6, 4)])
    rows.push(['labor', 'Labor Day', nthWeekday(year, 8, 1, 1)])
    rows.push(['thanks', 'Thanksgiving', nthWeekday(year, 10, 4, 4)])
  }
  if (pack === 'ca') {
    rows.push(['canada', 'Canada Day', iso(year, 6, 1)])
    rows.push(['labour', 'Labour Day', nthWeekday(year, 8, 1, 1)])
    rows.push(['thanks', 'Thanksgiving', nthWeekday(year, 9, 1, 2)])
    rows.push(['boxing', 'Boxing Day', iso(year, 11, 26)])
  }
  if (pack === 'gb') {
    rows.push(['boxing', 'Boxing Day', iso(year, 11, 26)])
  }
  if (pack === 'de') {
    rows.push(['unity', 'German Unity Day', iso(year, 9, 3)])
    rows.push(['boxing', 'Second Christmas Day', iso(year, 11, 26)])
  }
  return rows.map(([id, title, date]) => ev(`holiday-${pack}-${year}-${id}`, title, date))
}

export function holidayEvents(pack: HolidayPackId, year = new Date().getFullYear()): CalendarEvent[] {
  return [...holidayEventsForYear(pack, year), ...holidayEventsForYear(pack, year + 1)]
}

export function mergeHolidayEvents(existing: CalendarEvent[], pack: HolidayPackId): CalendarEvent[] {
  return [...existing.filter((e) => e.source !== 'holidays'), ...holidayEvents(pack)]
}

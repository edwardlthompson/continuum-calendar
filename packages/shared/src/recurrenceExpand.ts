import type { CalendarEvent } from './events.js'
import {
  dayBefore,
  freqFromRrule,
  monthlyModeFromRrule,
  occurrenceDateKey,
  rruleBlob,
  rruleFromParts,
  untilFromRrule,
  type RepeatFreq,
} from './recurrence.js'

export type RecurrenceEditScope = 'this' | 'following' | 'all'

function localDateKey(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function parseEventStart(iso: string, allDay?: boolean): Date {
  if (allDay || /^\d{4}-\d{2}-\d{2}$/.test(iso)) {
    const [y, mo, d] = iso.slice(0, 10).split('-').map(Number)
    return new Date(y, (mo ?? 1) - 1, d ?? 1)
  }
  const dt = new Date(iso)
  return Number.isNaN(dt.getTime()) ? new Date() : dt
}

function formatOccurrence(base: Date, allDay?: boolean): string {
  if (allDay) return localDateKey(base)
  return base.toISOString()
}

function nthWeekdayOfMonth(year: number, month: number, dow: number, nth: number): Date {
  const first = new Date(year, month, 1)
  const shift = (dow - first.getDay() + 7) % 7
  const day = 1 + shift + (nth - 1) * 7
  return new Date(year, month, day)
}

function lastWeekdayOfMonth(year: number, month: number, dow: number): Date {
  const last = new Date(year, month + 1, 0)
  const shift = (last.getDay() - dow + 7) % 7
  return new Date(year, month, last.getDate() - shift)
}

function parseByDay(blob: string): { nth: number; dow: number } | null {
  const m = blob.match(/BYDAY=(-?\d)(SU|MO|TU|WE|TH|FR|SA)/)
  if (!m) return null
  const names = ['SU', 'MO', 'TU', 'WE', 'TH', 'FR', 'SA']
  return { nth: Number(m[1]), dow: names.indexOf(m[2]) }
}

function advance(cur: Date, freq: Exclude<RepeatFreq, 'none'>, rules?: string[]): Date {
  const mode = monthlyModeFromRrule(rules)
  if (freq === 'daily') return new Date(cur.getFullYear(), cur.getMonth(), cur.getDate() + 1)
  if (freq === 'weekly') return new Date(cur.getFullYear(), cur.getMonth(), cur.getDate() + 7)
  if (freq === 'yearly') return new Date(cur.getFullYear() + 1, cur.getMonth(), cur.getDate())
  if (mode === 'last') return new Date(cur.getFullYear(), cur.getMonth() + 2, 0)
  const by = parseByDay(rruleBlob(rules))
  if (mode === 'byweekday' && by) {
    const nextMonth = cur.getMonth() + 1
    const y = cur.getFullYear() + Math.floor(nextMonth / 12)
    const mo = nextMonth % 12
    return by.nth < 0 ? lastWeekdayOfMonth(y, mo, by.dow) : nthWeekdayOfMonth(y, mo, by.dow, by.nth)
  }
  return new Date(cur.getFullYear(), cur.getMonth() + 1, cur.getDate())
}

export function expandRecurringEvents(
  events: CalendarEvent[],
  rangeStart: Date,
  rangeEnd: Date,
): CalendarEvent[] {
  const out: CalendarEvent[] = []
  for (const ev of events) {
    const freq = freqFromRrule(ev.recurrence)
    if (freq === 'none' || ev.source === 'google') {
      out.push(ev)
      continue
    }
    const skip = new Set((ev.recurrenceExceptions ?? []).map((d) => d.slice(0, 10)))
    const start = parseEventStart(ev.start, ev.allDay)
    const end = parseEventStart(ev.end, ev.allDay)
    const duration = Math.max(0, end.getTime() - start.getTime())
    const until = untilFromRrule(ev.recurrence)
    const untilMs = until ? parseEventStart(until, true).setHours(23, 59, 59, 999) : rangeEnd.getTime()
    const cap = Math.min(untilMs, rangeEnd.getTime())
    let cur = new Date(start.getTime())
    let n = 0
    while (cur.getTime() <= cap && n < 400) {
      const key = localDateKey(cur)
      const occEnd = new Date(cur.getTime() + duration)
      if (!skip.has(key) && occEnd.getTime() >= rangeStart.getTime() && cur.getTime() <= rangeEnd.getTime()) {
        const occStartIso = formatOccurrence(cur, ev.allDay)
        out.push({
          ...ev,
          id: `${ev.id}::${occStartIso}`,
          start: occStartIso,
          end: formatOccurrence(occEnd, ev.allDay),
        })
      }
      cur = advance(cur, freq, ev.recurrence)
      n += 1
    }
  }
  return out
}

export function applyRecurrenceEdit(
  scope: RecurrenceEditScope,
  master: CalendarEvent,
  occurrenceStart: string,
  draft: CalendarEvent,
): CalendarEvent[] {
  const occ = occurrenceDateKey(occurrenceStart)
  if (scope === 'all' || !master.recurrence?.length) {
    return [{ ...draft, id: master.id, recurrence: draft.recurrence ?? master.recurrence }]
  }
  if (scope === 'this') {
    const exceptions = [...new Set([...(master.recurrenceExceptions ?? []), occ])]
    const single: CalendarEvent = {
      ...draft,
      id: `${master.id}-exc-${occ}`,
      recurrence: undefined,
      recurrenceExceptions: undefined,
    }
    return [{ ...master, recurrenceExceptions: exceptions }, single]
  }
  const until = dayBefore(occ)
  const head: CalendarEvent = {
    ...master,
    recurrence: rruleFromParts({
      freq: freqFromRrule(master.recurrence),
      until,
      monthly: monthlyModeFromRrule(master.recurrence),
      start: master.start,
    }),
  }
  const tail: CalendarEvent = {
    ...draft,
    id: `${master.id}-from-${occ}`,
    start: draft.start,
    end: draft.end,
  }
  return [head, tail]
}

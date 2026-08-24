import type { CalendarEvent, FreeSlot } from './events.js'
import type { WorkingHours } from './settings.js'
import { localDateKey } from './agenda.ts'
// .ts suffix keeps Node --experimental-strip-types tests resolvable (value import).
import { proposeMeetingTimes } from './proposeTimes.ts'

export interface EventConflict {
  a: CalendarEvent
  b: CalendarEvent
}

export type ConflictCandidate = Pick<CalendarEvent, 'start' | 'end'> & {
  id?: string
  allDay?: boolean
  busy?: boolean
}

function startMs(e: { start: string }): number {
  return new Date(e.start).getTime()
}
function endMs(e: { end: string }): number {
  return new Date(e.end).getTime()
}

function overlaps(a: { start: string; end: string }, b: { start: string; end: string }): boolean {
  return startMs(a) < endMs(b) && endMs(a) > startMs(b)
}

function resolveNowMs(now?: Date | number): number {
  if (now instanceof Date) return now.getTime()
  if (typeof now === 'number') return now
  return Date.now()
}

/** Overlap is warn-worthy only while some portion is still upcoming. */
function isActiveOverlap(
  a: { start: string; end: string },
  b: { start: string; end: string },
  nowMs: number,
): boolean {
  if (!overlaps(a, b)) return false
  return Math.min(endMs(a), endMs(b)) > nowMs
}

export type ConflictDetectOptions = {
  /** Defaults to Date.now(); inject in tests. */
  now?: Date | number
}

/** Occurrence key so a repeating event is flagged only on the day it actually overlaps. */
export function eventOccurrenceKey(e: { id?: string; start: string }): string {
  return `${e.id ?? ''}:${e.start}`
}

/** True when a row should count as a busy timed meeting (not an all-day / anniversary block). */
export function isTimedBusyEvent(e: {
  allDay?: boolean
  busy?: boolean
  start: string
  end: string
}): boolean {
  if (e.allDay || e.busy === false) return false
  // Date-only ISO (YYYY-MM-DD) is all-day even if allDay was omitted.
  if (/^\d{4}-\d{2}-\d{2}$/.test(e.start.trim()) && !e.start.includes('T')) return false
  const start = startMs(e)
  const end = endMs(e)
  if (!Number.isFinite(start) || !Number.isFinite(end) || end <= start) return false
  const dur = end - start
  // >= 20h blocks (lost all-day flag / UTC date-only shifted into local TZ).
  if (dur >= 20 * 60 * 60 * 1000) return false
  // Midnight → local noon or later: Fossify 12h, DST 11h, or end-of-day.
  const d = new Date(start)
  const atMidnight = d.getHours() === 0 && d.getMinutes() === 0 && d.getSeconds() === 0
  if (atMidnight) {
    const noon = new Date(d)
    noon.setHours(12, 0, 0, 0)
    if (end >= noon.getTime()) return false
  }
  return true
}

/** Detect overlapping timed (non-all-day) events that are still upcoming. */
export function detectConflicts(
  events: CalendarEvent[],
  options: ConflictDetectOptions = {},
): EventConflict[] {
  const nowMs = resolveNowMs(options.now)
  const timed = events
    .filter((e) => isTimedBusyEvent(e))
    .sort((a, b) => startMs(a) - startMs(b))
  const out: EventConflict[] = []
  for (let i = 0; i < timed.length; i++) {
    for (let j = i + 1; j < timed.length; j++) {
      if (startMs(timed[j]) >= endMs(timed[i])) break
      if (isActiveOverlap(timed[i], timed[j], nowMs)) {
        out.push({ a: timed[i], b: timed[j] })
      }
    }
  }
  return out
}

const PEER_OR_OVERLAY = new Set(['local', 'ics_import', 'caldav'])

/** True when one side is Google and the other is local / ICS / CalDAV (two writers). */
export function isCrossSourceConflict(pair: EventConflict): boolean {
  const a = pair.a.source ?? 'local'
  const b = pair.b.source ?? 'local'
  if (a === b) return false
  return (a === 'google' && PEER_OR_OVERLAY.has(b)) || (b === 'google' && PEER_OR_OVERLAY.has(a))
}

export function crossSourceConflicts(
  events: CalendarEvent[],
  options: ConflictDetectOptions = {},
): EventConflict[] {
  return detectConflicts(events, options).filter(isCrossSourceConflict)
}

export function formatConflictSources(pair: EventConflict): string {
  const label = (e: CalendarEvent) => `${e.title || '(No title)'} (${e.source ?? 'local'})`
  return `${label(pair.a)} ↔ ${label(pair.b)}`
}

/** Local YYYY-MM-DD for each overlap, first occurrence of a day wins. */
export function uniqueConflictDates(pairs: EventConflict[]): string[] {
  const seen = new Set<string>()
  const out: string[] = []
  for (const pair of pairs) {
    const key = localDateKey(startMs(pair.a))
    if (seen.has(key)) continue
    seen.add(key)
    out.push(key)
  }
  return out
}

/** Next conflict day when cycling from [currentJumpDate] (wraps). */
export function peekNextConflictDate(dates: string[], currentJumpDate: string): string | undefined {
  if (!dates.length) return undefined
  const idx = dates.indexOf(currentJumpDate)
  return dates[(idx + 1) % dates.length] ?? dates[0]
}

/** Earliest timed overlap on a day as HH:mm:ss for calendar scroll. */
export function earliestConflictTimeOnDate(pairs: EventConflict[], dateKey: string): string | undefined {
  let min = Infinity
  for (const pair of pairs) {
    for (const e of [pair.a, pair.b]) {
      if (localDateKey(startMs(e)) !== dateKey) continue
      const t = startMs(e)
      if (t < min) min = t
    }
  }
  if (!Number.isFinite(min)) return undefined
  const d = new Date(min)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:00`
}

/** Events that overlap a candidate (excludes the candidate's own id when editing). */
export function conflictsForEvent(
  candidate: ConflictCandidate,
  events: CalendarEvent[],
  options: ConflictDetectOptions = {},
): CalendarEvent[] {
  if (!isTimedBusyEvent(candidate)) return []
  const nowMs = resolveNowMs(options.now)
  const start = startMs(candidate)
  const end = endMs(candidate)
  if (!Number.isFinite(start) || !Number.isFinite(end) || end <= start) return []
  return events.filter((e) => {
    if (!isTimedBusyEvent(e)) return false
    if (candidate.id && e.id === candidate.id) return false
    return isActiveOverlap(candidate, e, nowMs)
  })
}

/**
 * First free slot matching the candidate's duration, inside working hours
 * (may roll to a later day within `days`).
 */
export function suggestConflictFreeTime(
  candidate: ConflictCandidate,
  events: CalendarEvent[],
  options: {
    workingHours?: WorkingHours
    travelBufferMinutes?: number
    days?: number
    from?: Date
  } = {},
): FreeSlot | null {
  if (!isTimedBusyEvent(candidate)) return null
  const start = startMs(candidate)
  const end = endMs(candidate)
  if (!Number.isFinite(start) || !Number.isFinite(end) || end <= start) return null
  const durationMinutes = Math.max(15, Math.round((end - start) / 60_000))
  const others = (candidate.id ? events.filter((e) => e.id !== candidate.id) : events).filter((e) =>
    isTimedBusyEvent(e),
  )
  const slots = proposeMeetingTimes(others, {
    from: options.from ?? new Date(Math.min(start, Date.now())),
    days: options.days ?? 14,
    durationMinutes,
    count: 1,
    workingHours: options.workingHours,
    travelBufferMinutes: options.travelBufferMinutes,
  })
  return slots[0] ?? null
}

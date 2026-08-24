import assert from 'node:assert/strict'
import { test } from 'node:test'
import type { CalendarEvent } from './events.ts'
import {
  conflictsForEvent,
  crossSourceConflicts,
  detectConflicts,
  eventOccurrenceKey,
  formatConflictSources,
  isTimedBusyEvent,
  suggestConflictFreeTime,
  uniqueConflictDates,
  peekNextConflictDate,
  earliestConflictTimeOnDate,
} from './conflicts.ts'

function ev(
  id: string,
  start: string,
  end: string,
  extra: Partial<CalendarEvent> = {},
): CalendarEvent {
  return {
    id,
    calendarId: 'local',
    title: id,
    start,
    end,
    source: 'local',
    ...extra,
  }
}

/** Fixed clock while an overlap on 2026-08-12 is still in progress. */
const duringAug12 = { now: new Date('2026-08-12T10:45:00') }

test('detectConflicts finds overlap', () => {
  const a = ev('a', '2026-08-12T10:00:00', '2026-08-12T11:00:00')
  const b = ev('b', '2026-08-12T10:30:00', '2026-08-12T11:30:00')
  assert.equal(detectConflicts([a, b], duringAug12).length, 1)
})

test('detectConflicts ignores fully past overlaps', () => {
  const a = ev('a', '2026-08-12T10:00:00', '2026-08-12T11:00:00')
  const b = ev('b', '2026-08-12T10:30:00', '2026-08-12T11:30:00')
  assert.equal(detectConflicts([a, b], { now: new Date('2026-08-12T12:00:00') }).length, 0)
  assert.equal(detectConflicts([a, b], duringAug12).length, 1)
})

test('conflictsForEvent ignores self id and all-day', () => {
  const a = ev('a', '2026-08-12T10:00:00', '2026-08-12T11:00:00')
  const b = ev('b', '2026-08-12T10:30:00', '2026-08-12T11:30:00')
  assert.equal(conflictsForEvent(a, [a, b], duringAug12).length, 1)
  assert.equal(conflictsForEvent({ ...a, allDay: true }, [a, b], duringAug12).length, 0)
})

test('conflictsForEvent ignores past blockers', () => {
  const past = ev('past', '2026-08-12T10:00:00', '2026-08-12T11:00:00')
  const future = {
    start: '2026-08-25T10:00:00',
    end: '2026-08-25T11:00:00',
  }
  assert.equal(conflictsForEvent(future, [past], { now: new Date('2026-08-25T09:00:00') }).length, 0)
})

test('all-day and long special-day blocks never conflict with timed events', () => {
  const meeting = ev('m', '2026-08-16T10:00:00', '2026-08-16T12:30:00')
  const birthday = ev('bday', '2026-08-16', '2026-08-17', { allDay: true })
  const anniversary = ev('ann', '2026-08-16T00:00:00', '2026-08-17T00:00:00') // lost allDay flag
  const fossifyNoon = ev('fossify', '2026-08-16T00:00:00', '2026-08-16T12:00:00')
  const dstShort = ev('dst', '2026-08-16T00:00:00', '2026-08-16T11:00:00') // midnight → after clocks; still before noon
  const duringAug16 = { now: new Date('2026-08-16T11:00:00') }
  assert.equal(detectConflicts([meeting, birthday, anniversary, fossifyNoon], duringAug16).length, 0)
  assert.equal(conflictsForEvent(meeting, [birthday, anniversary, fossifyNoon], duringAug16).length, 0)
  assert.equal(isTimedBusyEvent(fossifyNoon), false)
  assert.equal(isTimedBusyEvent(dstShort), true)
})

test('half-day meeting starting at 8am is still busy', () => {
  const longMeeting = ev('long', '2026-08-16T08:00:00', '2026-08-16T20:00:00')
  const other = ev('other', '2026-08-16T10:00:00', '2026-08-16T11:00:00')
  assert.equal(isTimedBusyEvent(longMeeting), true)
  assert.equal(detectConflicts([longMeeting, other], { now: new Date('2026-08-16T10:30:00') }).length, 1)
})

test('eventOccurrenceKey distinguishes repeating occurrences', () => {
  const sun9 = ev('church', '2026-08-09T10:00:00', '2026-08-09T12:30:00')
  const sun16 = ev('church', '2026-08-16T10:00:00', '2026-08-16T12:30:00')
  assert.notEqual(eventOccurrenceKey(sun9), eventOccurrenceKey(sun16))
})

test('suggestConflictFreeTime returns a non-overlapping work-hours slot', () => {
  const blocker = ev('busy', '2026-08-12T09:00:00', '2026-08-12T17:00:00')
  const draft = {
    start: '2026-08-12T10:00:00',
    end: '2026-08-12T11:00:00',
  }
  const slot = suggestConflictFreeTime(draft, [blocker], {
    from: new Date('2026-08-12T08:00:00'),
    workingHours: { start: '09:00', end: '17:00' },
    days: 3,
  })
  assert.ok(slot)
  assert.ok(slot!.start.getTime() >= new Date('2026-08-13T09:00:00').getTime())
})

test('crossSourceConflicts flags local vs Google only', () => {
  const duringAug20 = { now: new Date('2026-08-20T10:45:00') }
  const local = ev('local-1', '2026-08-20T10:00:00', '2026-08-20T11:00:00', { source: 'local' })
  const google = ev('g-1', '2026-08-20T10:30:00', '2026-08-20T11:30:00', { source: 'google', calendarId: 'primary' })
  const sameSrc = ev('g-2', '2026-08-20T10:45:00', '2026-08-20T11:15:00', { source: 'google', calendarId: 'primary' })
  assert.equal(crossSourceConflicts([local, google], duringAug20).length, 1)
  assert.equal(crossSourceConflicts([google, sameSrc], duringAug20).length, 0)
  assert.match(formatConflictSources({ a: local, b: google }), /local.*google/)
})

test('uniqueConflictDates keeps first day of each overlap', () => {
  const a = ev('a', '2026-08-12T10:00:00', '2026-08-12T11:00:00')
  const b = ev('b', '2026-08-12T10:30:00', '2026-08-12T11:30:00')
  const c = ev('c', '2026-08-13T09:00:00', '2026-08-13T10:00:00')
  const d = ev('d', '2026-08-13T09:30:00', '2026-08-13T10:30:00')
  assert.deepEqual(uniqueConflictDates(detectConflicts([a, b, c, d], duringAug12)), ['2026-08-12', '2026-08-13'])
})

test('peekNextConflictDate cycles conflict days', () => {
  const dates = ['2026-08-12', '2026-08-13']
  assert.equal(peekNextConflictDate(dates, '2026-08-11'), '2026-08-12')
  assert.equal(peekNextConflictDate(dates, '2026-08-12'), '2026-08-13')
  assert.equal(peekNextConflictDate(dates, '2026-08-13'), '2026-08-12')
})

test('earliestConflictTimeOnDate returns scroll time', () => {
  const a = ev('a', '2026-08-12T10:15:00', '2026-08-12T11:00:00')
  const b = ev('b', '2026-08-12T10:30:00', '2026-08-12T11:30:00')
  const pairs = detectConflicts([a, b], duringAug12)
  assert.equal(earliestConflictTimeOnDate(pairs, '2026-08-12'), '10:15:00')
})

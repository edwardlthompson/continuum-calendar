import assert from 'node:assert/strict'
import { test } from 'node:test'
import type { CalendarEvent } from './events.ts'
import { conflictsForEvent, detectConflicts, suggestConflictFreeTime } from './conflicts.ts'

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

test('detectConflicts finds overlap', () => {
  const a = ev('a', '2026-08-12T10:00:00', '2026-08-12T11:00:00')
  const b = ev('b', '2026-08-12T10:30:00', '2026-08-12T11:30:00')
  assert.equal(detectConflicts([a, b]).length, 1)
})

test('conflictsForEvent ignores self id and all-day', () => {
  const a = ev('a', '2026-08-12T10:00:00', '2026-08-12T11:00:00')
  const b = ev('b', '2026-08-12T10:30:00', '2026-08-12T11:30:00')
  assert.equal(conflictsForEvent(a, [a, b]).length, 1)
  assert.equal(conflictsForEvent({ ...a, allDay: true }, [a, b]).length, 0)
})

test('all-day and long special-day blocks never conflict with timed events', () => {
  const meeting = ev('m', '2026-08-16T10:00:00', '2026-08-16T12:30:00')
  const birthday = ev('bday', '2026-08-16', '2026-08-17', { allDay: true })
  const anniversary = ev('ann', '2026-08-16T00:00:00', '2026-08-17T00:00:00') // lost allDay flag
  assert.equal(detectConflicts([meeting, birthday, anniversary]).length, 0)
  assert.equal(conflictsForEvent(meeting, [birthday, anniversary]).length, 0)
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

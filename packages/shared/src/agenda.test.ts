import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  dayShouldShowOpen,
  isEventPast,
  splitDayEventsAtNow,
  todayAgendaPhase,
  workingHoursEndMs,
  localDateKey,
} from './agenda.ts'
import type { CalendarEvent } from './events.ts'

function ev(partial: Partial<CalendarEvent> & Pick<CalendarEvent, 'id' | 'start' | 'end'>): CalendarEvent {
  return {
    calendarId: 'local:1',
    title: partial.title ?? partial.id,
    ...partial,
  }
}

test('isEventPast: timed event by end', () => {
  const e = ev({ id: 'a', start: '2026-08-11T10:00:00', end: '2026-08-11T11:00:00' })
  assert.equal(isEventPast(e, Date.parse('2026-08-11T10:30:00')), false)
  assert.equal(isEventPast(e, Date.parse('2026-08-11T11:00:01')), true)
})

test('isEventPast: all-day uses end of end date', () => {
  const e = ev({ id: 'b', start: '2026-08-11', end: '2026-08-11', allDay: true })
  const noon = Date.parse('2026-08-11T12:00:00')
  const nextMorning = Date.parse('2026-08-12T00:00:01')
  assert.equal(isEventPast(e, noon), false)
  assert.equal(isEventPast(e, nextMorning), true)
})

test('splitDayEventsAtNow orders chrono and splits', () => {
  const events = [
    ev({ id: '2', start: '2026-08-11T14:00:00', end: '2026-08-11T15:00:00' }),
    ev({ id: '1', start: '2026-08-11T09:00:00', end: '2026-08-11T10:00:00' }),
  ]
  const { past, future } = splitDayEventsAtNow(events, Date.parse('2026-08-11T12:00:00'))
  assert.deepEqual(
    past.map((e) => e.id),
    ['1'],
  )
  assert.deepEqual(
    future.map((e) => e.id),
    ['2'],
  )
})

test('todayAgendaPhase active → open → empty', () => {
  const day = '2026-08-11'
  const events = [ev({ id: '1', start: `${day}T09:00:00`, end: `${day}T10:00:00` })]
  assert.equal(
    todayAgendaPhase(events, Date.parse(`${day}T09:30:00`), '17:00', day),
    'active',
  )
  assert.equal(
    todayAgendaPhase(events, Date.parse(`${day}T10:00:01`), '17:00', day),
    'open',
  )
  assert.equal(
    todayAgendaPhase(events, Date.parse(`${day}T17:00:00`), '17:00', day),
    'empty',
  )
})

test('todayAgendaPhase empty day uses work hours', () => {
  const day = '2026-08-11'
  assert.equal(todayAgendaPhase([], Date.parse(`${day}T12:00:00`), '17:00', day), 'open')
  assert.equal(todayAgendaPhase([], Date.parse(`${day}T18:00:00`), '17:00', day), 'empty')
})

test('workingHoursEndMs defaults invalid to 17:00', () => {
  const day = '2026-08-11'
  assert.equal(workingHoursEndMs(day, 'nope'), workingHoursEndMs(day, '17:00'))
})

test('localDateKey stable for noon', () => {
  const ms = Date.parse('2026-08-11T12:00:00')
  assert.equal(localDateKey(ms), '2026-08-11')
})

test('dayShouldShowOpen: empty and all-day-only, not timed', () => {
  assert.equal(dayShouldShowOpen([]), true)
  assert.equal(
    dayShouldShowOpen([ev({ id: 'b', start: '2026-08-16', end: '2026-08-16', allDay: true })]),
    true,
  )
  assert.equal(
    dayShouldShowOpen([
      ev({ id: 'b', start: '2026-08-16', end: '2026-08-16', allDay: true }),
      ev({ id: 't', start: '2026-08-16T10:00:00', end: '2026-08-16T11:00:00' }),
    ]),
    false,
  )
})

test('buildAgendaSections buckets timed events by local day', async () => {
  const { buildAgendaSections } = await import('./agenda.ts')
  const localNoon = '2026-08-11T12:00:00'
  const sections = buildAgendaSections(
    [
      {
        id: '1',
        calendarId: 'c',
        title: 'Local noon',
        start: localNoon,
        end: '2026-08-11T13:00:00',
      },
    ],
    '2026-08-11',
    '2026-08-11',
    false,
  )
  assert.equal(sections.length, 1)
  assert.equal(sections[0]?.date, '2026-08-11')
  assert.equal(sections[0]?.events[0]?.id, '1')
})

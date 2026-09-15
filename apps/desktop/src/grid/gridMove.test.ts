import assert from 'node:assert/strict'
import { test } from 'node:test'
import type { CalendarEvent, CalendarListEntry } from '@continuum/shared'
import {
  applyGridMove,
  eventTimesFromFcEvent,
  ignoreGridMove,
  isGridEventEditable,
  isSeriesEvent,
  slotWindowFromWorkingHours,
} from './gridMove.ts'

const cal = (over: Partial<CalendarListEntry> = {}): CalendarListEntry => ({
  id: 'cal-1',
  accountId: 'a',
  displayName: 'Work',
  color: '#08c',
  visible: true,
  writable: true,
  source: 'local',
  logicalId: 'local:cal-1',
  ...over,
})

const ev = (over: Partial<CalendarEvent> = {}): CalendarEvent => ({
  id: 'e1',
  calendarId: 'cal-1',
  title: 'Standup',
  start: '2026-09-14T13:00:00.000Z',
  end: '2026-09-14T13:30:00.000Z',
  ...over,
})

test('ignoreGridMove skips empty id and missing start', () => {
  assert.equal(ignoreGridMove({ eventId: '', start: new Date(), end: null, allDay: false }), true)
  assert.equal(ignoreGridMove({ eventId: 'e1', start: null, end: null, allDay: false }), true)
  assert.equal(
    ignoreGridMove({ eventId: 'e1', start: new Date(), end: null, allDay: false }),
    false,
  )
})

test('slotWindowFromWorkingHours falls back when empty or inverted', () => {
  assert.deepEqual(slotWindowFromWorkingHours(undefined), {
    slotMinTime: '06:00:00',
    slotMaxTime: '22:00:00',
  })
  assert.deepEqual(slotWindowFromWorkingHours({ start: '17:00', end: '09:00' }), {
    slotMinTime: '06:00:00',
    slotMaxTime: '22:00:00',
  })
  assert.deepEqual(slotWindowFromWorkingHours({ start: '09:00', end: '17:00' }), {
    slotMinTime: '09:00:00',
    slotMaxTime: '17:00:00',
  })
})

test('isGridEventEditable respects read-only calendars and events', () => {
  assert.equal(isGridEventEditable(ev(), [cal()]), true)
  assert.equal(isGridEventEditable(ev({ readOnly: true }), [cal()]), false)
  assert.equal(isGridEventEditable(ev(), [cal({ writable: false })]), false)
  assert.equal(isGridEventEditable(ev(), [cal({ source: 'holidays' })]), false)
})

test('isSeriesEvent detects recurrence and Google instances', () => {
  assert.equal(isSeriesEvent(ev()), false)
  assert.equal(isSeriesEvent(ev({ recurrence: ['RRULE:FREQ=WEEKLY'] })), true)
  assert.equal(isSeriesEvent(ev({ recurringEventId: 'series1' })), true)
})

test('eventTimesFromFcEvent uses ISO instants for timed events', () => {
  const start = new Date('2026-09-14T13:00:00.000Z')
  const end = new Date('2026-09-14T14:00:00.000Z')
  assert.deepEqual(
    eventTimesFromFcEvent({ eventId: 'e1', start, end, allDay: false }),
    { start: start.toISOString(), end: end.toISOString() },
  )
})

test('eventTimesFromFcEvent uses date-only strings for all-day', () => {
  assert.deepEqual(
    eventTimesFromFcEvent({
      eventId: 'e1',
      start: null,
      end: null,
      allDay: true,
      startStr: '2026-09-14',
      endStr: '2026-09-15',
    }),
    { start: '2026-09-14', end: '2026-09-15' },
  )
})

test('applyGridMove returns null for empty payload', () => {
  assert.equal(
    applyGridMove(ev(), { eventId: '', start: new Date(), end: null, allDay: false }),
    null,
  )
})

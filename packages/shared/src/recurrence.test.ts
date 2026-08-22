import assert from 'node:assert/strict'
import { test } from 'node:test'
import type { CalendarEvent } from './events.ts'
import { rruleFromFreq, rruleFromParts, seriesEventId, untilFromRrule } from './recurrence.ts'
import { remainingTodayCount } from './todayBadge.ts'

test('rruleFromFreq writes UNTIL', () => {
  assert.deepEqual(rruleFromFreq('weekly', '2026-09-01'), ['RRULE:FREQ=WEEKLY;UNTIL=20260901'])
  assert.equal(rruleFromFreq('none'), undefined)
})

test('rruleFromParts writes monthly weekday and last day', () => {
  assert.ok(rruleFromParts({ freq: 'monthly', monthly: 'last' })?.[0]?.includes('BYMONTHDAY=-1'))
  assert.ok(
    rruleFromParts({ freq: 'monthly', monthly: 'byweekday', start: '2026-08-11' })?.[0]?.includes(
      'BYDAY=',
    ),
  )
})

test('untilFromRrule parses ICS', () => {
  assert.equal(untilFromRrule(['RRULE:FREQ=MONTHLY;UNTIL=20261231']), '2026-12-31')
})

test('seriesEventId strips occurrence suffix', () => {
  assert.equal(seriesEventId('abc::2026-08-21'), 'abc')
})

test('remainingTodayCount drops ended events', () => {
  const today = new Date()
  const y = today.getFullYear()
  const m = String(today.getMonth() + 1).padStart(2, '0')
  const d = String(today.getDate()).padStart(2, '0')
  const key = `${y}-${m}-${d}`
  const past: CalendarEvent = {
    id: 'p',
    calendarId: 'local',
    title: 'Past',
    start: `${key}T00:00:00`,
    end: `${key}T00:01:00`,
  }
  const future: CalendarEvent = {
    id: 'f',
    calendarId: 'local',
    title: 'Later',
    start: `${key}T23:00:00`,
    end: `${key}T23:30:00`,
  }
  assert.equal(remainingTodayCount([past, future], new Date(`${key}T12:00:00`).getTime()), 1)
})

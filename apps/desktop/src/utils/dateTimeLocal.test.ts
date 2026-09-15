import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  isIsoDate,
  isIsoTime,
  joinDateTime,
  monthCells,
  shiftMonth,
  splitDateTime,
  toLocalDateTimeValue,
  toLocalInput,
  weekdayLabels,
} from './dateTimeLocal.ts'

test('splitDateTime reads datetime-local and date-only', () => {
  assert.deepEqual(splitDateTime('2026-09-10T14:30'), { date: '2026-09-10', time: '14:30' })
  assert.deepEqual(splitDateTime('2026-09-10'), { date: '2026-09-10', time: '09:00' })
  assert.deepEqual(splitDateTime(''), { date: '', time: '09:00' })
})

test('joinDateTime omits time for all-day', () => {
  assert.equal(joinDateTime('2026-09-10', '14:30', true), '2026-09-10')
  assert.equal(joinDateTime('2026-09-10', '14:30', false), '2026-09-10T14:30')
  assert.equal(joinDateTime('', '14:30', false), '')
})

test('isIsoDate rejects impossible days', () => {
  assert.equal(isIsoDate('2026-09-10'), true)
  assert.equal(isIsoDate('2026-02-30'), false)
  assert.equal(isIsoDate('26-09-10'), false)
})

test('isIsoTime bounds hours and minutes', () => {
  assert.equal(isIsoTime('00:00'), true)
  assert.equal(isIsoTime('23:59'), true)
  assert.equal(isIsoTime('24:00'), false)
  assert.equal(isIsoTime('12:60'), false)
})

test('monthCells pads from firstDayOfWeek', () => {
  // 1 Sep 2026 is Tuesday
  const sun = monthCells(2026, 8, 0)
  assert.equal(sun.filter(Boolean).length, 30)
  assert.equal(sun[0], null)
  assert.equal(sun[1], null)
  assert.equal(sun[2]?.iso, '2026-09-01')
  // 1 Jun 2026 is Monday
  const mon = monthCells(2026, 5, 1)
  assert.equal(mon[0]?.iso, '2026-06-01')
})

test('weekdayLabels rotates from first day', () => {
  assert.deepEqual(weekdayLabels(0).slice(0, 2), ['Su', 'Mo'])
  assert.deepEqual(weekdayLabels(1).slice(0, 2), ['Mo', 'Tu'])
})

test('shiftMonth wraps years', () => {
  assert.deepEqual(shiftMonth(2026, 0, -1), { year: 2025, month0: 11 })
  assert.deepEqual(shiftMonth(2026, 11, 1), { year: 2027, month0: 0 })
})

test('toLocalInput formats a known instant in local zone without throwing', () => {
  const v = toLocalInput('2026-09-10T18:30:00.000Z', false)
  assert.match(v, /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/)
  assert.equal(toLocalInput('2026-09-10', true), '2026-09-10')
})

test('toLocalDateTimeValue uses wall time not UTC slice', () => {
  const d = new Date(2026, 8, 14, 9, 0, 0)
  const local = toLocalDateTimeValue(d)
  const utcSlice = d.toISOString().slice(0, 16)
  assert.equal(local, '2026-09-14T09:00')
  if (d.getTimezoneOffset() !== 0) {
    assert.notEqual(local, utcSlice)
  }
})

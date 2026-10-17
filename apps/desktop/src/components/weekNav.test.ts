import assert from 'node:assert/strict'
import test from 'node:test'
import { shiftDateKey, weekViewAnchorKey } from './weekNav'

test('shiftDateKey moves by whole days', () => {
  assert.equal(shiftDateKey('2026-10-16', 1), '2026-10-17')
  assert.equal(shiftDateKey('2026-10-16', -1), '2026-10-15')
  assert.equal(shiftDateKey('2026-10-31', 1), '2026-11-01')
})

test('weekViewAnchorKey from today is that local day', () => {
  const base = new Date(2026, 9, 16, 15, 30, 0) // Thu
  assert.equal(weekViewAnchorKey(true, 0, base), '2026-10-16')
})

test('weekViewAnchorKey week start uses firstDayOfWeek', () => {
  const base = new Date(2026, 9, 16, 15, 30, 0) // Thu
  assert.equal(weekViewAnchorKey(false, 0, base), '2026-10-11') // Sunday
  assert.equal(weekViewAnchorKey(false, 1, base), '2026-10-12') // Monday
})

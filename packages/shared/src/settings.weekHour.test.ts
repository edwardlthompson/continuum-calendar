import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  defaultContinuumSettings,
  normalizeWeekHourDisplay,
} from './settings.ts'

test('defaultContinuumSettings uses scroll week hours', () => {
  assert.equal(defaultContinuumSettings().weekHourDisplay, 'scroll')
})

test('normalizeWeekHourDisplay accepts fit24 and scroll', () => {
  assert.equal(normalizeWeekHourDisplay('fit24'), 'fit24')
  assert.equal(normalizeWeekHourDisplay('scroll'), 'scroll')
  assert.equal(normalizeWeekHourDisplay(undefined), 'scroll')
  assert.equal(normalizeWeekHourDisplay('nope'), 'scroll')
})

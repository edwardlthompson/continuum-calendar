import assert from 'node:assert/strict'
import { test } from 'node:test'
import { calendarCaption } from './calendarLabel.ts'

const google = { id: 'primary', displayName: 'Google Primary', source: 'google' as const }
const local = { id: 'local-default', displayName: 'Local', source: 'local' as const }

test('signed-out google is relabeled not hidden', () => {
  const caption = calendarCaption(google, 'signed-out')
  assert.match(caption, /Sign in to sync/)
  assert.match(caption, /Google Primary/)
})

test('signed-in google keeps the name', () => {
  assert.equal(calendarCaption(google, 'signed-in'), 'Google Primary')
})

test('empty displayName uses id', () => {
  assert.equal(calendarCaption({ id: 'primary', displayName: '  ', source: 'google' }, 'signed-in'), 'primary')
})

test('local calendars keep their name when signed out', () => {
  assert.equal(calendarCaption(local, 'signed-out'), 'Local')
})

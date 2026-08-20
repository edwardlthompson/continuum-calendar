import assert from 'node:assert/strict'
import { test } from 'node:test'
import { displayNameFromIcsUrl, subscriptionCalendarId } from './icsSubscribeId.ts'

test('subscriptionCalendarId is stable for the same URL', () => {
  const a = subscriptionCalendarId('https://example.com/holidays.ics')
  const b = subscriptionCalendarId('https://example.com/holidays.ics')
  assert.equal(a, b)
  assert.match(a, /^ics-sub-[0-9a-f]+$/)
})

test('subscriptionCalendarId differs by URL', () => {
  const a = subscriptionCalendarId('https://example.com/a.ics')
  const b = subscriptionCalendarId('https://example.com/b.ics')
  assert.notEqual(a, b)
})

test('displayNameFromIcsUrl uses the file stem', () => {
  assert.equal(displayNameFromIcsUrl('https://cal.example/pub/US-Holidays.ics'), 'US-Holidays')
})

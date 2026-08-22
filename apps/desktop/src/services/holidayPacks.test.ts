import assert from 'node:assert/strict'
import { test } from 'node:test'
import { holidayEventsForYear } from './holidayPacks.ts'

test('US pack includes Independence Day', () => {
  const ev = holidayEventsForYear('us', 2026)
  assert.ok(ev.some((e) => e.start === '2026-07-04' && e.title.includes('Independence')))
})

test('none pack is empty', () => {
  assert.deepEqual(holidayEventsForYear('none', 2026), [])
})

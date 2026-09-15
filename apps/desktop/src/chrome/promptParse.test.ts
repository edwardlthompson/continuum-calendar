import assert from 'node:assert/strict'
import { test } from 'node:test'
import { parseJumpDate, promptFields, promptTitle } from './promptParse.ts'

test('parseJumpDate accepts calendar dates only', () => {
  assert.equal(parseJumpDate('2026-09-14'), '2026-09-14')
  assert.equal(parseJumpDate(' 2026-09-14 '), '2026-09-14')
  assert.equal(parseJumpDate('2026-02-30'), null)
  assert.equal(parseJumpDate('09-14'), null)
  assert.equal(parseJumpDate(''), null)
})

test('promptFields for jump and caldav', () => {
  assert.equal(promptFields('jump', '2026-09-14')[0]?.defaultValue, '2026-09-14')
  assert.equal(promptFields('caldav', '').length, 3)
  assert.equal(promptTitle('jump'), 'Go to date')
})

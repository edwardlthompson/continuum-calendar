import assert from 'node:assert/strict'
import { test } from 'node:test'
import { OPEN_MARK_SVG, SPLASH_MARK_SVG } from './marks.ts'

test('Open and splash marks export without external URLs', () => {
  assert.match(OPEN_MARK_SVG, /<svg/)
  assert.match(SPLASH_MARK_SVG, /<svg/)
  assert.doesNotMatch(OPEN_MARK_SVG, /https?:\/\//)
  assert.doesNotMatch(SPLASH_MARK_SVG, /https?:\/\//)
})

import assert from 'node:assert/strict'
import { test } from 'node:test'
import { readFileSync } from 'node:fs'
import { APP_TITLE } from './appTitle.ts'

test('H1 is Continuum Calendar without a version', () => {
  assert.equal(APP_TITLE, 'Continuum Calendar')
  assert.doesNotMatch(APP_TITLE, /\d+\.\d+\.\d+/)
  const src = readFileSync(new URL('./AppTitle.tsx', import.meta.url), 'utf8')
  assert.match(src, /\{APP_TITLE\}/)
  assert.doesNotMatch(src, /getVersion/)
  assert.doesNotMatch(src, /1\.0\.0/)
})

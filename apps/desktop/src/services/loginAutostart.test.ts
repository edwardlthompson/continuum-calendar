import assert from 'node:assert/strict'
import { test } from 'node:test'
import { allowEnableAutostart } from './loginAutostart.ts'

test('installed release may register Start at login', () => {
  assert.equal(allowEnableAutostart(false), true)
})

test('dev / tauri:dev must not register Start at login', () => {
  assert.equal(allowEnableAutostart(true), false)
})

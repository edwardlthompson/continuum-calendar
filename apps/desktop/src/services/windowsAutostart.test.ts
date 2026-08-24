import assert from 'node:assert/strict'
import { test } from 'node:test'
import { allowEnableAutostart } from './windowsAutostart.ts'

test('installed release may register Start with Windows', () => {
  assert.equal(allowEnableAutostart(false), true)
})

test('dev / tauri:dev must not register Start with Windows', () => {
  assert.equal(allowEnableAutostart(true), false)
})

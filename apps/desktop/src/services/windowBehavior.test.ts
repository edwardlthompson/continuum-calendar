import assert from 'node:assert/strict'
import { test } from 'node:test'
import { defaultWindowBehavior, parseWindowBehavior, toNativeArgs } from './windowBehavior.ts'

test('defaults minimize to taskbar and close to tray', () => {
  const d = defaultWindowBehavior()
  assert.equal(d.minimizeTo, 'taskbar')
  assert.equal(d.closeTo, 'tray')
  assert.deepEqual(toNativeArgs(d), { closeToTray: true, minimizeToTray: false })
})

test('parseWindowBehavior rejects unknown values', () => {
  assert.deepEqual(parseWindowBehavior({ minimizeTo: 'nope', closeTo: 'quit' }), {
    minimizeTo: 'taskbar',
    closeTo: 'quit',
  })
})

import assert from 'node:assert/strict'
import { test } from 'node:test'
import { DESKTOP_HOTKEY_CATALOG, hotkeyTitle } from './desktopHotkeys.ts'

test('hotkey catalog lists core navigation shortcuts', () => {
  assert.ok(DESKTOP_HOTKEY_CATALOG.length >= 8)
  assert.ok(DESKTOP_HOTKEY_CATALOG.some((e) => e.label === 'Today' && e.hint === 'T'))
})

test('hotkeyTitle formats tooltip copy', () => {
  assert.equal(hotkeyTitle('Today', 'T'), 'Today (T)')
  assert.equal(hotkeyTitle('Search events', '/ or F'), 'Search events (/ or F)')
})

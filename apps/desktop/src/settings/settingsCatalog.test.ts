import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  SETTINGS_CATEGORIES,
  categoryVisible,
  textMatches,
} from './settingsCatalog.ts'

test('empty search shows every category', () => {
  assert.equal(SETTINGS_CATEGORIES.length, 7)
  for (const cat of SETTINGS_CATEGORIES) {
    assert.equal(categoryVisible(cat, ''), true)
  }
})

test('search finds Window & startup from boot / tray', () => {
  const win = SETTINGS_CATEGORIES.find((c) => c.id === 'window')
  assert.ok(win)
  assert.equal(categoryVisible(win, 'boot'), true)
  assert.equal(categoryVisible(win, 'tray'), true)
  assert.equal(categoryVisible(win, 'keyboard'), true)
  assert.equal(categoryVisible(win, 'theme'), false)
})

test('textMatches is case-insensitive and empty-open', () => {
  assert.equal(textMatches('', 'Theme'), true)
  assert.equal(textMatches('DARK', 'Theme', 'dark', 'light'), true)
  assert.equal(textMatches('snooze', 'Theme'), false)
})

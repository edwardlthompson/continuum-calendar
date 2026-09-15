import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  CC_DARK_ACCENT,
  CC_DARK_ON_ACCENT,
  CC_LIGHT_ACCENT,
  CC_LIGHT_ON_ACCENT,
  contrastRatio,
} from './contrast.ts'

test('dark CTA pair meets WCAG AA', () => {
  assert.ok(contrastRatio(CC_DARK_ACCENT, CC_DARK_ON_ACCENT) >= 4.5)
})

test('light CTA pair meets WCAG AA', () => {
  assert.ok(contrastRatio(CC_LIGHT_ACCENT, CC_LIGHT_ON_ACCENT) >= 4.5)
})

test('invalid hex returns 0', () => {
  assert.equal(contrastRatio('nope', '#ffffff'), 0)
  assert.equal(contrastRatio('#4eb6d4', ''), 0)
})

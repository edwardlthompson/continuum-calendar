import assert from 'node:assert/strict'
import { test } from 'node:test'
import { badgeLabel } from './dayBadge.ts'

test('badgeLabel hides empty remaining counts', () => {
  assert.equal(badgeLabel(0), '')
  assert.equal(badgeLabel(-1), '')
})

test('badgeLabel caps at 99+', () => {
  assert.equal(badgeLabel(7), '7')
  assert.equal(badgeLabel(99), '99')
  assert.equal(badgeLabel(100), '99+')
})

test('overlay is empty when there is no remaining count', async () => {
  const { renderOverlayPng } = await import('./dayBadge.ts')
  assert.deepEqual(renderOverlayPng(0), [])
})

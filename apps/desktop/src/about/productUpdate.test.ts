import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  MS_DAY,
  isNewerVersion,
  parseAssetVersion,
  selectProductAsset,
  shouldCheckDaily,
  shouldNudgeDonate,
  shouldPromptUpdate,
} from './productUpdate.ts'

test('daily check waits a full day', () => {
  assert.equal(shouldCheckDaily(null, 0), true)
  assert.equal(shouldCheckDaily(0, MS_DAY - 1), false)
  assert.equal(shouldCheckDaily(0, MS_DAY), true)
})

test('asset versions come from product filenames, not template tags', () => {
  assert.equal(parseAssetVersion('Continuum-Calendar-0.17.4-x64-setup.exe', 'exe'), '0.17.4')
  assert.equal(parseAssetVersion('continuum-calendar-1.10.8-foss.apk', 'apk'), '1.10.8')
  assert.equal(parseAssetVersion('v0.22.1', 'exe'), null)
})

test('selects the matching installer URL', () => {
  const picked = selectProductAsset(
    [
      { name: 'sbom.cyclonedx.json', url: 'https://example.com/sbom' },
      { name: 'Continuum-Calendar-0.18.0-x64-setup.exe', url: 'https://example.com/setup.exe' },
    ],
    'exe',
  )
  assert.deepEqual(picked, { version: '0.18.0', url: 'https://example.com/setup.exe' })
})

test('donate nudge only after a version change', () => {
  assert.equal(shouldNudgeDonate(null, '0.17.3'), false)
  assert.equal(shouldNudgeDonate('0.17.3', '0.17.3'), false)
  assert.equal(shouldNudgeDonate('0.17.3', '0.17.4'), true)
})

test('update prompt skips dismissed or equal versions', () => {
  assert.equal(isNewerVersion('0.17.3', '0.17.4'), true)
  assert.equal(shouldPromptUpdate('0.17.3', '0.17.4', null), true)
  assert.equal(shouldPromptUpdate('0.17.3', '0.17.4', '0.17.4'), false)
  assert.equal(shouldPromptUpdate('0.17.4', '0.17.4', null), false)
  assert.equal(shouldPromptUpdate('0.17.3', null, null), false)
})

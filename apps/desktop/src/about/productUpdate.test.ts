import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  MS_DAY,
  detectDesktopOs,
  isNewerVersion,
  parseAssetVersion,
  productKindForOs,
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
  assert.equal(
    parseAssetVersion('Continuum-Calendar-0.17.4-x86_64.AppImage', 'appimage'),
    '0.17.4',
  )
  assert.equal(
    parseAssetVersion('Continuum Calendar_0.18.0_amd64.AppImage', 'appimage'),
    '0.18.0',
  )
  assert.equal(parseAssetVersion('v0.22.1', 'exe'), null)
  assert.equal(parseAssetVersion('Continuum-Calendar-0.17.4-x64-setup.exe', 'appimage'), null)
})

test('selects the matching installer URL', () => {
  const assets = [
    { name: 'sbom.cyclonedx.json', url: 'https://example.com/sbom' },
    { name: 'Continuum-Calendar-0.18.0-x64-setup.exe', url: 'https://example.com/setup.exe' },
    {
      name: 'Continuum-Calendar-0.18.0-x86_64.AppImage',
      url: 'https://example.com/app.AppImage',
    },
  ]
  assert.deepEqual(selectProductAsset(assets, 'exe'), {
    version: '0.18.0',
    url: 'https://example.com/setup.exe',
  })
  assert.deepEqual(selectProductAsset(assets, 'appimage'), {
    version: '0.18.0',
    url: 'https://example.com/app.AppImage',
  })
})

test('desktop OS maps to release asset kind', () => {
  assert.equal(detectDesktopOs('Mozilla/5.0 (Windows NT 10.0; Win64; x64)'), 'windows')
  assert.equal(detectDesktopOs('Mozilla/5.0 (X11; Linux x86_64)'), 'linux')
  assert.equal(detectDesktopOs('Mozilla/5.0 (Linux; Android 14)'), 'other')
  assert.equal(productKindForOs('windows'), 'exe')
  assert.equal(productKindForOs('linux'), 'appimage')
  assert.equal(productKindForOs('other'), null)
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

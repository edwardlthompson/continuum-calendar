import assert from 'node:assert/strict'
import { test } from 'node:test'

test('openExternal rejects empty URLs without touching the browser', async () => {
  const { openExternal } = await import('./openExternal.ts')
  await assert.rejects(() => openExternal(''), /Missing URL/)
  await assert.rejects(() => openExternal('   '), /Missing URL/)
})

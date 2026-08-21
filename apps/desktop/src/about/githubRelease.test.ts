import assert from 'node:assert/strict'
import { test } from 'node:test'
import { parseGithubRelease } from './githubRelease.ts'

test('parseGithubRelease ignores empty or malformed payloads', () => {
  assert.equal(parseGithubRelease(null), null)
  assert.deepEqual(parseGithubRelease({ html_url: 'https://example.com/r', assets: [] })?.assets, [])
})

test('parseGithubRelease keeps named download URLs', () => {
  const parsed = parseGithubRelease({
    html_url: 'https://example.com/r',
    assets: [{ name: 'Continuum-Calendar-0.18.0-x64-setup.exe', browser_download_url: 'https://example.com/e' }],
  })
  assert.equal(parsed?.htmlUrl, 'https://example.com/r')
  assert.equal(parsed?.assets[0]?.url, 'https://example.com/e')
})

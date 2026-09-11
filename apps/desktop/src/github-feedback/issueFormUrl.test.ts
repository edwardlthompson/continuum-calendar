import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  buildIssueFormUrl,
  crashTitle,
  isPlaceholderRepo,
  MAX_QUERY_CHARS,
} from './issueFormUrl.ts'

test('crashTitle formats fingerprint and type', () => {
  assert.equal(crashTitle('A1B2C3D4E5F6ffff', 'TypeError: x'), '[crash] a1b2c3d4e5f6 TypeError')
})

test('placeholder repos get an empty issue URL', () => {
  assert.equal(isPlaceholderRepo(''), true)
  assert.equal(isPlaceholderRepo('OWNER/REPO'), true)
  assert.equal(buildIssueFormUrl('OWNER/REPO', 'crash_report.yml', { description: 'x' }).url, '')
})

test('prefills small fields', () => {
  const built = buildIssueFormUrl('acme/app', 'crash_report.yml', {
    title: '[crash] abc TypeError',
    description: 'boom',
  })
  assert.equal(built.bodyTooLarge, false)
  assert.equal(built.url.includes('github.com/acme/app/issues/new'), true)
  assert.equal(built.url.includes('template=crash_report.yml'), true)
})

test('drops a large stack from the query string', () => {
  const stack = 'x'.repeat(MAX_QUERY_CHARS + 500)
  const built = buildIssueFormUrl('acme/app', 'crash_report.yml', { stack, title: '[crash] ab' })
  assert.equal(built.bodyTooLarge, true)
  assert.equal(built.url.length < MAX_QUERY_CHARS, true)
  assert.equal(built.url.includes(stack.slice(0, 40)), false)
  assert.equal(built.clipboardMarkdown, stack)
})

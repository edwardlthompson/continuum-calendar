import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { test } from 'node:test'
import { fingerprintCrash } from './fingerprint.ts'
import { buildReportMarkdown } from './markdown.ts'
import { sanitizeReportText } from './sanitize.ts'

const fixture = JSON.parse(
  readFileSync(join(dirname(fileURLToPath(import.meta.url)), 'sanitize-fixtures.json'), 'utf8'),
) as { stack: string; must_not_contain: string[]; must_contain: string[] }

test('sanitize treats null as empty', () => {
  assert.equal(sanitizeReportText(null), '')
})

test('redacts secrets, JWT, AWS, and home paths', () => {
  const out = sanitizeReportText(fixture.stack, true)
  for (const leak of fixture.must_not_contain) {
    assert.equal(out.includes(leak), false, `leaked ${leak}`)
  }
  for (const keep of fixture.must_contain) {
    assert.equal(out.includes(keep), true, `missing ${keep}`)
  }
})

test('fingerprint is stable when only the username changes', async () => {
  const a = await fingerprintCrash('Error\n    at C:\\Users\\Ada\\app\\main.ts:1')
  const b = await fingerprintCrash('Error\n    at C:\\Users\\Bob\\app\\main.ts:1')
  assert.equal(a, b)
  assert.equal(a.length, 12)
})

test('report markdown strips tokens from description', () => {
  const md = buildReportMarkdown({
    kind: 'crash',
    description: 'user ghp_abcdefghijklmnopqrstuvwxyz012345 leaked',
  })
  assert.equal(md.includes('ghp_'), false)
  assert.equal(md.includes('crash'), true)
})

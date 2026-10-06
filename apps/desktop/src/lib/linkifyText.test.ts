import assert from 'node:assert/strict'
import { test } from 'node:test'
import { linkifyText } from './linkifyText.ts'

test('empty input yields one empty text segment', () => {
  assert.deepEqual(linkifyText(''), [{ type: 'text', value: '' }])
  assert.deepEqual(linkifyText(null), [{ type: 'text', value: '' }])
  assert.deepEqual(linkifyText(undefined), [{ type: 'text', value: '' }])
})

test('linkifies https and strips trailing punctuation', () => {
  const segs = linkifyText('See https://example.com/path.')
  assert.deepEqual(segs, [
    { type: 'text', value: 'See ' },
    { type: 'link', value: 'https://example.com/path', href: 'https://example.com/path' },
    { type: 'text', value: '.' },
  ])
})

test('normalizes www to https', () => {
  const segs = linkifyText('www.example.com')
  assert.equal(segs.length, 1)
  assert.equal(segs[0]?.type, 'link')
  if (segs[0]?.type === 'link') {
    assert.equal(segs[0].href, 'https://www.example.com')
  }
})

test('allows mailto and rejects javascript', () => {
  const mail = linkifyText('mailto:a@b.com')
  assert.equal(mail[0]?.type, 'link')
  const bad = linkifyText('javascript:alert(1)')
  assert.equal(bad.every((s) => s.type === 'text'), true)
})

test('preserves surrounding text', () => {
  const segs = linkifyText('before https://a.test after')
  assert.deepEqual(segs, [
    { type: 'text', value: 'before ' },
    { type: 'link', value: 'https://a.test', href: 'https://a.test' },
    { type: 'text', value: ' after' },
  ])
})

test('parses Google-style HTML anchor without blending into label', () => {
  const html =
    'A total of 1 seats.\n<a href="https://i-heart-pr-tours.bokun.io/reports/passengers?activity-ids=1106251&date=2026-10-06">View passenger list in Bókun.</a>'
  const segs = linkifyText(html)
  assert.deepEqual(segs, [
    { type: 'text', value: 'A total of 1 seats.\n' },
    {
      type: 'link',
      value: 'View passenger list in Bókun.',
      href: 'https://i-heart-pr-tours.bokun.io/reports/passengers?activity-ids=1106251&date=2026-10-06',
    },
  ])
})

test('HTML href with &amp; decodes and stops before quote', () => {
  const segs = linkifyText('<a href="https://ex.test/a?b=1&amp;c=2">Open</a>')
  assert.deepEqual(segs, [
    { type: 'link', value: 'Open', href: 'https://ex.test/a?b=1&c=2' },
  ])
})

test('bare URL next to HTML delimiters does not swallow View', () => {
  const segs = linkifyText('https://ex.test/path">View')
  assert.equal(segs[0]?.type, 'link')
  if (segs[0]?.type === 'link') {
    assert.equal(segs[0].href, 'https://ex.test/path')
  }
  assert.equal(segs.some((s) => s.type === 'text' && s.value.includes('View')), true)
})

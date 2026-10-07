import assert from 'node:assert/strict'
import { test } from 'node:test'
import { filterPaletteCommands, PALETTE_COMMANDS } from './commandPalette.ts'

test('empty query returns all commands', () => {
  assert.equal(filterPaletteCommands('').length, PALETTE_COMMANDS.length)
})

test('filter by label', () => {
  const hits = filterPaletteCommands('week')
  assert.equal(hits.length, 1)
  assert.equal(hits[0]?.id, 'week')
})

test('no match is empty', () => {
  assert.deepEqual(filterPaletteCommands('zzzz'), [])
})

test('does not parse event titles as commands', () => {
  assert.deepEqual(filterPaletteCommands('Team sync'), [])
  assert.deepEqual(filterPaletteCommands('Doctor at 9'), [])
})

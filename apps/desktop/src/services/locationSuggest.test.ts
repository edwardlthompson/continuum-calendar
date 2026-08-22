import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  formatPhotonProperties,
  mergeLocationSuggestions,
  parsePhotonFeatures,
  recentEventLocations,
} from './locationSuggest.ts'

test('formatPhotonProperties prefers name then city/state', () => {
  assert.equal(
    formatPhotonProperties({
      name: 'Library',
      city: 'Austin',
      state: 'Texas',
      country: 'United States',
    }),
    'Library, Austin, Texas, United States',
  )
})

test('parsePhotonFeatures skips empty payloads', () => {
  assert.deepEqual(parsePhotonFeatures(null), [])
  assert.deepEqual(
    parsePhotonFeatures({
      features: [{ properties: { name: 'Cafe', city: 'Austin' } }],
    }),
    ['Cafe, Austin'],
  )
})

test('recentEventLocations filters and de-dupes', () => {
  const events = [
    { location: 'Home' },
    { location: 'Library' },
    { location: 'Home' },
    { location: '' },
  ]
  assert.deepEqual(recentEventLocations(events, ''), ['Home', 'Library'])
  assert.deepEqual(recentEventLocations(events, 'lib'), ['Library'])
})

test('mergeLocationSuggestions puts history first', () => {
  assert.deepEqual(mergeLocationSuggestions(['Home'], ['Library', 'Home'], 12), ['Home', 'Library'])
})

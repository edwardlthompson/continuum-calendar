import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  formatPhotonProperties,
  mergeLocationSuggestions,
  mapsSearchUrl,
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

test('mapsSearchUrl is null until there is a query', () => {
  assert.equal(mapsSearchUrl(''), null)
  assert.equal(mapsSearchUrl('   '), null)
  assert.equal(
    mapsSearchUrl('AC Hotel San Juan'),
    'https://www.google.com/maps/search/?api=1&query=AC%20Hotel%20San%20Juan',
  )
})

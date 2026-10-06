import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  formatPhotonProperties,
  mapsSearchUrl,
  mergeLocationSuggestions,
  osmStaticMapUrl,
  parsePhotonFeatures,
  parsePhotonPlaces,
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

test('parsePhotonPlaces requires coordinates', () => {
  assert.deepEqual(
    parsePhotonPlaces({
      features: [{ properties: { name: 'Cafe', city: 'Austin' } }],
    }),
    [],
  )
  assert.deepEqual(
    parsePhotonPlaces({
      features: [
        {
          properties: { name: 'Cafe', city: 'Austin' },
          geometry: { coordinates: [-97.7431, 30.2672] },
        },
      ],
    }),
    [{ label: 'Cafe, Austin', lat: 30.2672, lon: -97.7431 }],
  )
})

test('osmStaticMapUrl builds FOSS static map URL', () => {
  const url = osmStaticMapUrl(30.2672, -97.7431, 400, 180, 14)
  assert.match(url, /staticmap\.openstreetmap\.de/)
  assert.match(url, /center=30\.2672,-97\.7431/)
})

test('mapsSearchUrl encodes query', () => {
  assert.equal(mapsSearchUrl(''), null)
  assert.match(mapsSearchUrl('San Juan') ?? '', /google\.com\/maps\/search/)
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

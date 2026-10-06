import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  appleMapsUrl,
  googleMapsUrl,
  mapProviderUrl,
  osmMapsUrl,
} from './mapProviders.ts'

test('osm / google / apple URLs for query-only', () => {
  const t = { query: 'San Juan PR' }
  assert.match(osmMapsUrl(t), /openstreetmap\.org\/search/)
  assert.match(googleMapsUrl(t), /google\.com\/maps\/search/)
  assert.match(appleMapsUrl(t), /maps\.apple\.com\/\?q=/)
  assert.equal(mapProviderUrl('osm', t), osmMapsUrl(t))
})

test('coord URLs prefer lat/lon', () => {
  const t = { query: 'Cave', lat: 18.4, lon: -66.1 }
  assert.match(osmMapsUrl(t), /mlat=18\.4/)
  assert.match(googleMapsUrl(t), /query=18\.4%2C-66\.1/)
  assert.match(appleMapsUrl(t), /ll=18\.4,-66\.1/)
})

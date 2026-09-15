import assert from 'node:assert/strict'
import { test } from 'node:test'
import { listTimeZones } from './timeZones.ts'

test('device zone is first', () => {
  const list = listTimeZones({ deviceZone: 'Asia/Tokyo' })
  assert.equal(list[0], 'Asia/Tokyo')
  assert.ok(list.length <= 24)
})

test('filter by substring', () => {
  const list = listTimeZones({ deviceZone: 'UTC', query: 'london' })
  assert.deepEqual(list, ['Europe/London'])
})

test('unknown current id is preserved', () => {
  const list = listTimeZones({ deviceZone: 'UTC', current: 'Pacific/Honolulu' })
  assert.ok(list.includes('Pacific/Honolulu'))
})

test('common list length is at most 24', () => {
  assert.ok(listTimeZones({ deviceZone: 'UTC' }).length <= 24)
})

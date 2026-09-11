import assert from 'node:assert/strict'
import { test } from 'node:test'
import type { CalendarListEntry } from '@continuum/shared'
import { googleCalendarIdsToSync } from './googleCalendarSyncIds.ts'

function cal(partial: Partial<CalendarListEntry> & Pick<CalendarListEntry, 'id'>): CalendarListEntry {
  return {
    accountId: 'google',
    displayName: partial.id,
    color: '#000',
    visible: true,
    writable: true,
    source: 'google',
    logicalId: `google:${partial.id}`,
    ...partial,
  }
}

test('always includes primary even when unchecked', () => {
  const ids = googleCalendarIdsToSync([
    cal({ id: 'primary-id', logicalId: 'google:primary', visible: false }),
    cal({ id: 'work', visible: false }),
  ])
  assert.deepEqual(ids, ['primary-id'])
})

test('includes visible secondaries and skips hidden ones', () => {
  const ids = googleCalendarIdsToSync([
    cal({ id: 'primary-id', logicalId: 'google:primary', visible: true }),
    cal({ id: 'work', visible: true }),
    cal({ id: 'hidden', visible: false }),
  ])
  assert.equal(ids.includes('primary-id'), true)
  assert.equal(ids.includes('work'), true)
  assert.equal(ids.includes('hidden'), false)
})

test('falls back to primary alias when list is empty', () => {
  assert.deepEqual(googleCalendarIdsToSync([]), ['primary'])
})

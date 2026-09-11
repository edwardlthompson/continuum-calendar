import assert from 'node:assert/strict'
import { test } from 'node:test'
import { GOOGLE_SCOPES } from '../../../../packages/shared/src/oauth.ts'
import { hasTasksScope, mapGoogleTask, TASKS_CALENDAR_ID } from './googleTasksMap.ts'

test('hasTasksScope matches the Tasks OAuth URL', () => {
  assert.equal(hasTasksScope('https://www.googleapis.com/auth/calendar'), false)
  assert.equal(hasTasksScope(GOOGLE_SCOPES.tasks), true)
})

test('mapGoogleTask uses due date as an all-day agenda row', () => {
  const ev = mapGoogleTask(
    { id: 't1', title: 'Buy milk', due: '2026-09-12T00:00:00.000Z' },
    'listA',
  )
  assert.ok(ev)
  assert.equal(ev.calendarId, TASKS_CALENDAR_ID)
  assert.equal(ev.id, 'listA:t1')
  assert.equal(ev.start, '2026-09-12')
  assert.equal(ev.allDay, true)
  assert.equal(ev.busy, false)
})

test('mapGoogleTask skips tasks without a due date', () => {
  assert.equal(mapGoogleTask({ id: 't1', title: 'Someday' }, 'listA'), null)
})

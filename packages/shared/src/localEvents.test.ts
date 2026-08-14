import assert from 'node:assert/strict'
import { test } from 'node:test'
import type { CalendarEvent } from './events.ts'
import { mergeLocalEventsPayload } from './localEvents.ts'

function localEv(partial: Partial<CalendarEvent> & Pick<CalendarEvent, 'id' | 'calendarId'>): CalendarEvent {
  return {
    title: partial.title ?? partial.id,
    start: '2026-08-14T13:00:00.000Z',
    end: '2026-08-14T14:00:00.000Z',
    source: 'local',
    updated: '2026-08-12T12:00:00.000Z',
    ...partial,
  }
}

test('tombstone newer than event.updated drops the peer event', () => {
  const merged = mergeLocalEventsPayload({
    baseCalendars: [],
    baseEvents: [
      localEv({ id: 'mock-3', calendarId: 'local-default', title: 'Doctor' }),
    ],
    baseDeleted: [],
    incomingCalendars: [],
    incomingEvents: [],
    incomingDeleted: [
      { id: 'mock-3', calendarId: 'local-default', deletedAt: '2026-08-13T11:00:00.000Z' },
    ],
  })
  assert.equal(merged.events.length, 0)
  assert.equal(merged.deletedIds.length, 1)
  assert.equal(merged.deletedIds[0]?.id, 'mock-3')
})

test('event.updated newer than tombstone keeps the event', () => {
  const merged = mergeLocalEventsPayload({
    baseCalendars: [],
    baseEvents: [
      localEv({
        id: 'mock-3',
        calendarId: 'local-default',
        updated: '2026-08-13T12:00:00.000Z',
      }),
    ],
    baseDeleted: [{ id: 'mock-3', calendarId: 'local-default', deletedAt: '2026-08-13T11:00:00.000Z' }],
    incomingCalendars: [],
    incomingEvents: [],
    incomingDeleted: [],
  })
  assert.equal(merged.events.length, 1)
  assert.equal(merged.events[0]?.id, 'mock-3')
})

test('empty deletedIds keeps remote local events (regression for Android push)', () => {
  const merged = mergeLocalEventsPayload({
    baseCalendars: [],
    baseEvents: [localEv({ id: 'mock-3', calendarId: 'local-default', title: 'Doctor' })],
    baseDeleted: [],
    incomingCalendars: [],
    incomingEvents: [],
    incomingDeleted: [],
  })
  assert.equal(merged.events[0]?.id, 'mock-3')
})

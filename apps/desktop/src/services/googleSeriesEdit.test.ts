import assert from 'node:assert/strict'
import { test } from 'node:test'
import type { CalendarEvent } from '@continuum/shared'
import { applySeriesHydration, googleSeriesWritePlan, inferGoogleSeriesId, mergeSeriesTimes } from './googleSeriesEdit.ts'

const master: CalendarEvent = {
  id: 'series1',
  calendarId: 'primary',
  title: 'Church',
  start: '2020-01-05T14:00:00.000Z',
  end: '2020-01-05T16:00:00.000Z',
  source: 'google',
  recurrence: ['RRULE:FREQ=WEEKLY;BYDAY=SU'],
}

test('inferGoogleSeriesId reads API field or instance id suffix', () => {
  assert.equal(inferGoogleSeriesId('abc_20260913T140000Z'), 'abc')
  assert.equal(
    inferGoogleSeriesId('45hutda5h7kojoh6siocrticcr_20260913T140000Z'),
    '45hutda5h7kojoh6siocrticcr',
  )
  assert.equal(inferGoogleSeriesId('plain-id'), undefined)
  assert.equal(inferGoogleSeriesId('abc_20260913T140000Z', 'from-api'), 'from-api')
})

test('mergeSeriesTimes keeps master date and uses draft clock', () => {
  const merged = mergeSeriesTimes(master, {
    start: '2026-09-13T15:00:00.000Z',
    end: '2026-09-13T16:30:00.000Z',
  })
  const start = new Date(merged.start)
  const masterStart = new Date(master.start)
  const draft = new Date('2026-09-13T15:00:00.000Z')
  assert.equal(start.getFullYear(), masterStart.getFullYear())
  assert.equal(start.getMonth(), masterStart.getMonth())
  assert.equal(start.getDate(), masterStart.getDate())
  assert.equal(start.getHours(), draft.getHours())
  assert.equal(start.getMinutes(), draft.getMinutes())
})

test('applySeriesHydration copies RRULE onto instances', () => {
  const instance: CalendarEvent = {
    id: 'series1_20260913T140000Z',
    calendarId: 'primary',
    title: 'Church',
    start: '2026-09-13T14:00:00.000Z',
    end: '2026-09-13T16:00:00.000Z',
    source: 'google',
    recurringEventId: 'series1',
  }
  const out = applySeriesHydration([instance], new Map([['series1', master]]))
  assert.deepEqual(out[0]?.recurrence, ['RRULE:FREQ=WEEKLY;BYDAY=SU'])
})

test('this-event plan patches the instance without RRULE', () => {
  const instance = {
    ...master,
    id: 'series1_20260913T140000Z',
    recurringEventId: 'series1',
    start: '2026-09-13T14:00:00.000Z',
    end: '2026-09-13T16:00:00.000Z',
    title: 'Church (baptism)',
  }
  const plan = googleSeriesWritePlan('this', instance, master)
  assert.equal(plan.patch.length, 1)
  assert.equal(plan.patch[0]?.id, instance.id)
  assert.equal(plan.patch[0]?.recurrence, undefined)
  assert.equal(plan.patch[0]?.title, 'Church (baptism)')
})

test('all-events plan patches the master id', () => {
  const instance = {
    ...master,
    id: 'series1_20260913T140000Z',
    recurringEventId: 'series1',
    start: '2026-09-13T14:00:00.000Z',
    end: '2026-09-13T16:00:00.000Z',
    title: 'Sunday service',
  }
  const plan = googleSeriesWritePlan('all', instance, master)
  assert.equal(plan.patch[0]?.id, 'series1')
  assert.equal(plan.patch[0]?.title, 'Sunday service')
  assert.ok(plan.patch[0]?.recurrence?.[0]?.includes('WEEKLY'))
})

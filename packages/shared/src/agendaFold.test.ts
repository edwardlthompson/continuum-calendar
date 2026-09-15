import assert from 'node:assert/strict'
import { test } from 'node:test'
import { collapseEmptyAgendaSections } from './agendaFold.ts'
import type { AgendaDaySection, CalendarEvent } from './events.ts'

function emptyDay(date: string): AgendaDaySection {
  return { date, events: [], isEmpty: true }
}

function timedDay(date: string): AgendaDaySection {
  const events: CalendarEvent[] = [
    {
      id: `t-${date}`,
      calendarId: 'c',
      title: 'Busy',
      start: `${date}T14:00:00`,
      end: `${date}T15:00:00`,
    },
  ]
  return { date, events, isEmpty: false }
}

function allDayOnly(date: string): AgendaDaySection {
  const events: CalendarEvent[] = [
    {
      id: `a-${date}`,
      calendarId: 'holidays',
      title: 'Holiday',
      start: date,
      end: date,
      allDay: true,
    },
  ]
  return { date, events, isEmpty: false }
}

test('empty list returns empty', () => {
  const out = collapseEmptyAgendaSections([], { todayKey: '2026-09-14' })
  assert.deepEqual(out, { visible: [], foldedCount: 0 })
})

test('maxEmpty 0 is treated as 7', () => {
  const sections = Array.from({ length: 10 }, (_, i) => emptyDay(`2026-09-${String(14 + i).padStart(2, '0')}`))
  const out = collapseEmptyAgendaSections(sections, { todayKey: '2026-09-14', maxEmpty: 0 })
  assert.equal(out.visible.length, 7)
  assert.equal(out.foldedCount, 3)
})

test('always keeps today and busy days', () => {
  const sections = [
    emptyDay('2026-09-14'),
    timedDay('2026-09-15'),
    emptyDay('2026-09-16'),
    emptyDay('2026-09-17'),
  ]
  const out = collapseEmptyAgendaSections(sections, { todayKey: '2026-09-14', maxEmpty: 1 })
  assert.equal(out.visible.map((s) => s.date).join(','), '2026-09-14,2026-09-15')
  assert.equal(out.foldedCount, 2)
})

test('all-day-only Open days count toward the empty cap', () => {
  const sections = [
    emptyDay('2026-09-14'),
    allDayOnly('2026-09-15'),
    emptyDay('2026-09-16'),
  ]
  const out = collapseEmptyAgendaSections(sections, { todayKey: '2026-09-14', maxEmpty: 1 })
  assert.equal(out.visible.map((s) => s.date).join(','), '2026-09-14')
  assert.equal(out.foldedCount, 2)
})

test('today with past timed events counts as Open when nowMs is after them', () => {
  const today = '2026-09-14'
  const sections = [
    timedDay(today),
    emptyDay('2026-09-15'),
    emptyDay('2026-09-16'),
  ]
  const nowMs = new Date(`${today}T18:00:00`).getTime()
  const out = collapseEmptyAgendaSections(sections, {
    todayKey: today,
    nowMs,
    workingHoursEnd: '17:00',
    maxEmpty: 1,
  })
  assert.equal(out.visible.map((s) => s.date).join(','), today)
  assert.equal(out.foldedCount, 2)
})

test('skips sections with empty date', () => {
  const out = collapseEmptyAgendaSections([{ date: '', events: [], isEmpty: true }], { todayKey: '2026-09-14' })
  assert.equal(out.visible.length, 0)
  assert.equal(out.foldedCount, 0)
})

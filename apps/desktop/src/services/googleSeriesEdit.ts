import type { CalendarEvent } from '@continuum/shared'

export type GoogleSeriesScope = 'this' | 'all'

/**
 * Google `events.list?singleEvents=true` often omits `recurringEventId`.
 * Instance ids are `{master}_{YYYYMMDDTHHMMSSZ}`.
 */
export function inferGoogleSeriesId(instanceId: string, apiRecurringEventId?: string): string | undefined {
  const fromApi = apiRecurringEventId?.trim()
  if (fromApi) return fromApi
  const timed = /^(.*)_(\d{8}T\d{6}Z)$/.exec(instanceId)
  return timed?.[1] || undefined
}

/** Copy master RRULE onto expanded Google instances. */
export function applySeriesHydration(
  events: CalendarEvent[],
  masters: Map<string, CalendarEvent>,
): CalendarEvent[] {
  return events.map((e) => {
    const m = e.recurringEventId ? masters.get(e.recurringEventId) : undefined
    if (!m?.recurrence?.length) return e
    return { ...e, recurrence: m.recurrence, timeZone: e.timeZone ?? m.timeZone }
  })
}

/** Keep the series' original date; apply this occurrence's clock time and duration. */
export function mergeSeriesTimes(
  master: Pick<CalendarEvent, 'start' | 'end' | 'allDay'>,
  draft: Pick<CalendarEvent, 'start' | 'end' | 'allDay'>,
): { start: string; end: string } {
  const allDay = draft.allDay ?? master.allDay
  if (allDay || /^\d{4}-\d{2}-\d{2}$/.test(master.start)) {
    return { start: master.start.slice(0, 10), end: master.end.slice(0, 10) }
  }
  const masterStart = new Date(master.start)
  const draftStart = new Date(draft.start)
  const draftEnd = new Date(draft.end)
  if ([masterStart, draftStart, draftEnd].some((d) => Number.isNaN(d.getTime()))) {
    return { start: draft.start, end: draft.end }
  }
  const start = new Date(masterStart.getTime())
  start.setHours(draftStart.getHours(), draftStart.getMinutes(), 0, 0)
  const duration = Math.max(0, draftEnd.getTime() - draftStart.getTime())
  return { start: start.toISOString(), end: new Date(start.getTime() + duration).toISOString() }
}

export function googleSeriesWritePlan(
  scope: GoogleSeriesScope,
  draft: CalendarEvent & { id: string },
  master: CalendarEvent,
): { patch: CalendarEvent[] } {
  if (scope === 'this') {
    return { patch: [{ ...draft, recurrence: undefined }] }
  }
  const times = mergeSeriesTimes(master, draft)
  return {
    patch: [
      {
        ...master,
        ...draft,
        id: master.id,
        start: times.start,
        end: times.end,
        recurrence: draft.recurrence ?? master.recurrence,
        recurringEventId: undefined,
        etag: master.etag,
      },
    ],
  }
}

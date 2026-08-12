/** Continuum peer-synced clock format helpers. */

export function formatEventTime(iso: string, use24HourFormat: boolean, allDay?: boolean): string {
  if (allDay) return 'All day'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleTimeString(undefined, {
    hour: 'numeric',
    minute: '2-digit',
    hour12: !use24HourFormat,
  })
}

/** Timed events: "9:00 AM – 10:30 AM"; all-day stays "All day". */
export function formatEventTimeRange(
  startIso: string,
  endIso: string | undefined,
  use24HourFormat: boolean,
  allDay?: boolean,
): string {
  if (allDay) return 'All day'
  const start = formatEventTime(startIso, use24HourFormat, false)
  if (!endIso) return start
  const end = formatEventTime(endIso, use24HourFormat, false)
  if (end === endIso && Number.isNaN(new Date(endIso).getTime())) return start
  return `${start} – ${end}`
}

export function formatEventDateTime(iso: string, use24HourFormat: boolean): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleString(undefined, {
    hour: 'numeric',
    minute: '2-digit',
    hour12: !use24HourFormat,
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

/** FullCalendar time format objects. */
export function fullCalendarTimeFormats(use24HourFormat: boolean) {
  if (use24HourFormat) {
    return {
      eventTimeFormat: { hour: '2-digit' as const, minute: '2-digit' as const, hour12: false },
      slotLabelFormat: { hour: '2-digit' as const, minute: '2-digit' as const, hour12: false },
    }
  }
  return {
    eventTimeFormat: { hour: 'numeric' as const, minute: '2-digit' as const, hour12: true },
    slotLabelFormat: { hour: 'numeric' as const, minute: '2-digit' as const, hour12: true },
  }
}

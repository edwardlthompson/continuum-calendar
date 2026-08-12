import type { CalendarEvent, FreeSlot } from '@continuum/shared'

function toDate(value: string | Date): Date {
  return value instanceof Date ? value : new Date(value)
}

/** Compute open gaps between timed events within [rangeStart, rangeEnd]. */
export function computeFreeSlots(
  events: CalendarEvent[],
  rangeStart: Date,
  rangeEnd: Date,
  options?: {
    dayStartHour?: number
    dayEndHour?: number
    minMinutes?: number
    travelBufferMinutes?: number
  },
): FreeSlot[] {
  const dayStartHour = options?.dayStartHour ?? 9
  const dayEndHour = options?.dayEndHour ?? 17
  const minMinutes = options?.minMinutes ?? 30
  const bufferMs = (options?.travelBufferMinutes ?? 0) * 60_000

  const slots: FreeSlot[] = []
  const cursor = new Date(rangeStart)
  cursor.setHours(0, 0, 0, 0)

  while (cursor < rangeEnd) {
    const windowStart = new Date(cursor)
    windowStart.setHours(dayStartHour, 0, 0, 0)
    const windowEnd = new Date(cursor)
    windowEnd.setHours(dayEndHour, 0, 0, 0)

    const dayEvents = events
      .filter((e) => !e.allDay && e.busy !== false)
      .map((e) => ({
        start: new Date(toDate(e.start).getTime() - bufferMs),
        end: new Date(toDate(e.end).getTime() + bufferMs),
      }))
      .filter((e) => e.end > windowStart && e.start < windowEnd)
      .sort((a, b) => a.start.getTime() - b.start.getTime())

    let freeStart = windowStart
    for (const ev of dayEvents) {
      const busyStart = ev.start < windowStart ? windowStart : ev.start
      if (busyStart > freeStart) {
        const minutes = (busyStart.getTime() - freeStart.getTime()) / 60_000
        if (minutes >= minMinutes) {
          slots.push({ start: new Date(freeStart), end: new Date(busyStart) })
        }
      }
      const busyEnd = ev.end > windowEnd ? windowEnd : ev.end
      if (busyEnd > freeStart) freeStart = busyEnd
    }
    if (windowEnd > freeStart) {
      const minutes = (windowEnd.getTime() - freeStart.getTime()) / 60_000
      if (minutes >= minMinutes) {
        slots.push({ start: new Date(freeStart), end: new Date(windowEnd) })
      }
    }

    cursor.setDate(cursor.getDate() + 1)
  }

  return slots
}

function formatSlotLine(slot: FreeSlot, use24HourFormat = false): string {
  const opts: Intl.DateTimeFormatOptions = {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
    hour: use24HourFormat ? '2-digit' : 'numeric',
    minute: '2-digit',
    hour12: !use24HourFormat,
  }
  const start = slot.start.toLocaleString(undefined, opts)
  const end = slot.end.toLocaleTimeString(undefined, {
    hour: use24HourFormat ? '2-digit' : 'numeric',
    minute: '2-digit',
    hour12: !use24HourFormat,
  })
  return `${start} – ${end}`
}

/** Format free slots as plain text suitable for pasting into email/chat. */
export function formatFreeSlotsPlainText(
  slots: FreeSlot[],
  heading = 'Available times',
  use24HourFormat = false,
): string {
  if (slots.length === 0) return `${heading}\n(none in range)`
  return [heading, ...slots.map((s) => `• ${formatSlotLine(s, use24HourFormat)}`)].join('\n')
}

/** Copy free slots to the system clipboard as plain text. */
export async function copyFreeSlotsToClipboard(
  events: CalendarEvent[],
  rangeStart: Date,
  rangeEnd: Date,
  options?: {
    dayStartHour?: number
    dayEndHour?: number
    minMinutes?: number
    travelBufferMinutes?: number
    use24HourFormat?: boolean
  },
): Promise<string> {
  const slots = computeFreeSlots(events, rangeStart, rangeEnd, options)
  const text = formatFreeSlotsPlainText(slots, 'Available times', options?.use24HourFormat ?? false)
  await navigator.clipboard.writeText(text)
  return text
}

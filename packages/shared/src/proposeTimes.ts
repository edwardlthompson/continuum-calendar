import type { CalendarEvent, FreeSlot } from './events.js'
import type { WorkingHours } from './settings.js'

function parseHm(hm: string): { h: number; m: number } {
  const [h, m] = hm.split(':').map(Number)
  return { h: h || 0, m: m || 0 }
}

function dayWindow(day: Date, hours: WorkingHours): { start: Date; end: Date } {
  const { h: sh, m: sm } = parseHm(hours.start)
  const { h: eh, m: em } = parseHm(hours.end)
  const start = new Date(day)
  start.setHours(sh, sm, 0, 0)
  const end = new Date(day)
  end.setHours(eh, em, 0, 0)
  return { start, end }
}

/** Propose up to `count` free windows of `durationMinutes` across the next `days` days. */
export function proposeMeetingTimes(
  events: CalendarEvent[],
  options: {
    from?: Date
    days?: number
    durationMinutes?: number
    count?: number
    workingHours?: WorkingHours
    travelBufferMinutes?: number
  } = {},
): FreeSlot[] {
  const from = options.from ?? new Date()
  const days = options.days ?? 7
  const duration = (options.durationMinutes ?? 30) * 60_000
  const count = options.count ?? 5
  const hours = options.workingHours ?? { start: '09:00', end: '17:00' }
  const buffer = (options.travelBufferMinutes ?? 0) * 60_000

  const busy = events
    .filter((e) => !e.allDay && e.busy !== false)
    .map((e) => ({
      start: new Date(e.start).getTime() - buffer,
      end: new Date(e.end).getTime() + buffer,
    }))
    .sort((a, b) => a.start - b.start)

  const proposals: FreeSlot[] = []
  const cursor = new Date(from)
  cursor.setHours(0, 0, 0, 0)

  for (let d = 0; d < days && proposals.length < count; d++) {
    const day = new Date(cursor)
    day.setDate(cursor.getDate() + d)
    const { start: winStart, end: winEnd } = dayWindow(day, hours)
    let t = Math.max(winStart.getTime(), from.getTime())
    while (t + duration <= winEnd.getTime() && proposals.length < count) {
      const slotEnd = t + duration
      const overlaps = busy.some((b) => t < b.end && slotEnd > b.start)
      if (!overlaps) {
        proposals.push({ start: new Date(t), end: new Date(slotEnd) })
        t = slotEnd
      } else {
        const blocker = busy.find((b) => t < b.end && slotEnd > b.start)
        t = blocker ? blocker.end : t + 15 * 60_000
      }
    }
  }
  return proposals
}

import type { CalendarEvent } from '@continuum/shared'
import { replaceEventsForCalendar } from '../data/eventReplace'
import type { CalDavAccount } from './caldav'
import { calDavAuthHeader } from './caldav'
import { calDavTimeRange, extractCalendarDataBlocks } from './caldavParse'
import { parseIcs } from './ics'

const REPORT_DAYS_BACK = 7
const REPORT_DAYS_AHEAD = 90

function calendarQueryBody(from: Date, to: Date): string {
  const { start, end } = calDavTimeRange(from, to)
  return `<?xml version="1.0" encoding="utf-8"?>
<c:calendar-query xmlns:d="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav">
  <d:prop><d:getetag/><c:calendar-data/></d:prop>
  <c:filter>
    <c:comp-filter name="VCALENDAR">
      <c:comp-filter name="VEVENT">
        <c:time-range start="${start}" end="${end}"/>
      </c:comp-filter>
    </c:comp-filter>
  </c:filter>
</c:calendar-query>`
}

export function calDavCalendarId(account: CalDavAccount): string {
  return `caldav-${account.id}`
}

/** REPORT calendar-query on the account URL and replace stored CalDAV events. */
export async function syncCalDavEvents(account: CalDavAccount): Promise<CalendarEvent[]> {
  const url = account.serverUrl.replace(/\/?$/, '/')
  const from = new Date()
  from.setDate(from.getDate() - REPORT_DAYS_BACK)
  const to = new Date()
  to.setDate(to.getDate() + REPORT_DAYS_AHEAD)
  const res = await fetch(url, {
    method: 'REPORT',
    headers: {
      Authorization: calDavAuthHeader(account),
      Depth: '1',
      'Content-Type': 'application/xml; charset=utf-8',
    },
    body: calendarQueryBody(from, to),
  })
  if (!res.ok && res.status !== 207) {
    throw new Error(`CalDAV REPORT failed: ${res.status}`)
  }
  const calendarId = calDavCalendarId(account)
  const events = extractCalendarDataBlocks(await res.text()).flatMap((ics) =>
    parseIcs(ics, calendarId, { source: 'caldav', readOnly: true }),
  )
  return replaceEventsForCalendar(calendarId, 'caldav', events)
}

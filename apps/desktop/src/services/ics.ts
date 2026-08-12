import type { CalendarEvent } from '@continuum/shared'

function formatIcsDate(iso: string, allDay?: boolean): string {
  if (allDay) return iso.slice(0, 10).replace(/-/g, '')
  return new Date(iso).toISOString().replace(/[-:]/g, '').replace(/\.\d{3}/, '')
}

export function eventsToIcs(events: CalendarEvent[], calName = 'Continuum'): string {
  const lines = [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//Continuum Calendar//EN',
    `X-WR-CALNAME:${calName}`,
  ]
  for (const ev of events) {
    lines.push('BEGIN:VEVENT')
    lines.push(`UID:${ev.id}@continuum`)
    lines.push(`SUMMARY:${(ev.title || '').replace(/\n/g, ' ')}`)
    if (ev.description) lines.push(`DESCRIPTION:${ev.description.replace(/\n/g, '\\n')}`)
    if (ev.location) lines.push(`LOCATION:${ev.location}`)
    if (ev.allDay) {
      lines.push(`DTSTART;VALUE=DATE:${formatIcsDate(ev.start, true)}`)
      lines.push(`DTEND;VALUE=DATE:${formatIcsDate(ev.end, true)}`)
    } else {
      lines.push(`DTSTART:${formatIcsDate(ev.start)}`)
      lines.push(`DTEND:${formatIcsDate(ev.end)}`)
    }
    lines.push('END:VEVENT')
  }
  lines.push('END:VCALENDAR')
  return lines.join('\r\n')
}

export function parseIcs(ics: string, calendarId = 'ics-import'): CalendarEvent[] {
  const events: CalendarEvent[] = []
  const blocks = ics.split('BEGIN:VEVENT').slice(1)
  let i = 0
  for (const block of blocks) {
    const body = block.split('END:VEVENT')[0] ?? ''
    const get = (key: string) => {
      const line = body.split(/\r?\n/).find((l) => l.startsWith(key))
      return line?.split(':').slice(1).join(':')?.trim()
    }
    const summary = get('SUMMARY') ?? '(No title)'
    const dtStart = get('DTSTART') ?? get('DTSTART;VALUE=DATE')
    const dtEnd = get('DTEND') ?? get('DTEND;VALUE=DATE')
    if (!dtStart || !dtEnd) continue
    const allDay = dtStart.length === 8
    const toIso = (v: string) => {
      if (v.length === 8) return `${v.slice(0, 4)}-${v.slice(4, 6)}-${v.slice(6, 8)}`
      const m = v.match(/^(\d{4})(\d{2})(\d{2})T(\d{2})(\d{2})(\d{2})Z?$/)
      if (!m) return v
      return `${m[1]}-${m[2]}-${m[3]}T${m[4]}:${m[5]}:${m[6]}.000Z`
    }
    events.push({
      id: get('UID') ?? `ics-${Date.now()}-${i++}`,
      calendarId,
      source: 'ics_import',
      title: summary,
      description: get('DESCRIPTION')?.replace(/\\n/g, '\n'),
      location: get('LOCATION'),
      start: toIso(dtStart),
      end: toIso(dtEnd),
      allDay,
      readOnly: true,
    })
  }
  return events
}

export async function downloadIcsFile(events: CalendarEvent[], filename = 'continuum-export.ics') {
  const blob = new Blob([eventsToIcs(events)], { type: 'text/calendar' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

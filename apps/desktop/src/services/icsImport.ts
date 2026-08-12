import { logicalCalendarId, type CalendarEvent, type CalendarListEntry } from '@continuum/shared'
import { loadCalendars, saveCalendars, upsertEvents } from '../data/localStore'
import { parseIcs } from './ics'

const MAX_ICS_BYTES = 2_000_000
const FETCH_TIMEOUT_MS = 20_000

export function ensureIcsImportCalendar(): CalendarListEntry {
  const cals = loadCalendars()
  const existing = cals.find((c) => c.id === 'ics-import')
  if (existing) return existing
  const entry: CalendarListEntry = {
    id: 'ics-import',
    accountId: 'ics',
    displayName: 'Imported ICS',
    color: '#5b6b82',
    visible: true,
    writable: false,
    source: 'ics_import',
    logicalId: logicalCalendarId('ics_import', 'ics-import'),
  }
  saveCalendars([...cals, entry])
  return entry
}

export function importIcsText(text: string): { events: CalendarEvent[]; count: number } {
  if (text.length > MAX_ICS_BYTES) {
    throw new Error(`ICS file too large (max ${MAX_ICS_BYTES} bytes)`)
  }
  ensureIcsImportCalendar()
  const imported = parseIcs(text, 'ics-import')
  if (!imported.length) throw new Error('No events found in ICS')
  const events = upsertEvents(imported)
  return { events, count: imported.length }
}

/** Map webcal → https and fetch ICS with timeout + size cap. */
export async function fetchIcsFromUrl(rawUrl: string): Promise<string> {
  let url = rawUrl.trim()
  if (url.startsWith('webcal://')) url = `https://${url.slice('webcal://'.length)}`
  else if (url.startsWith('webcals://')) url = `https://${url.slice('webcals://'.length)}`
  if (!/^https?:\/\//i.test(url)) throw new Error('URL must be http(s) or webcal')

  const ctrl = new AbortController()
  const timer = window.setTimeout(() => ctrl.abort(), FETCH_TIMEOUT_MS)
  try {
    const res = await fetch(url, { signal: ctrl.signal })
    if (!res.ok) throw new Error(`Fetch failed: ${res.status}`)
    const buf = await res.arrayBuffer()
    if (buf.byteLength > MAX_ICS_BYTES) {
      throw new Error(`ICS response too large (max ${MAX_ICS_BYTES} bytes)`)
    }
    return new TextDecoder('utf-8').decode(buf)
  } catch (e) {
    if (e instanceof DOMException && e.name === 'AbortError') {
      throw new Error('Calendar link timed out')
    }
    throw e
  } finally {
    window.clearTimeout(timer)
  }
}

export async function importIcsFromUrl(rawUrl: string): Promise<{ events: CalendarEvent[]; count: number }> {
  const text = await fetchIcsFromUrl(rawUrl)
  return importIcsText(text)
}

export function looksLikeIcsFileName(name: string): boolean {
  return /\.ics$/i.test(name) || /\.ical$/i.test(name)
}

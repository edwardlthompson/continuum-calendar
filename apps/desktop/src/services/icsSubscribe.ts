import { logicalCalendarId, type CalendarEvent, type CalendarListEntry } from '@continuum/shared'
import { replaceEventsForCalendar } from '../data/eventReplace'
import { loadCalendars, loadEvents, saveCalendars } from '../data/localStore'
import { fetchIcsFromUrl, normalizeIcsUrl } from './icsImport'
import { parseIcs } from './ics'
import { displayNameFromIcsUrl, subscriptionCalendarId } from './icsSubscribeId'

export { displayNameFromIcsUrl, subscriptionCalendarId } from './icsSubscribeId'

export interface IcsSubscription {
  id: string
  url: string
  calendarId: string
  displayName: string
  lastFetchedAt?: string
  lastError?: string
}

const SUBS_KEY = 'continuum.ics.subscriptions'

export function loadIcsSubscriptions(): IcsSubscription[] {
  try {
    return JSON.parse(localStorage.getItem(SUBS_KEY) ?? '[]') as IcsSubscription[]
  } catch {
    return []
  }
}

export function saveIcsSubscriptions(subs: IcsSubscription[]): void {
  localStorage.setItem(SUBS_KEY, JSON.stringify(subs))
}

function ensureCalendar(sub: IcsSubscription): void {
  const cals = loadCalendars()
  if (cals.some((c) => c.id === sub.calendarId)) return
  const entry: CalendarListEntry = {
    id: sub.calendarId,
    accountId: 'ics-sub',
    displayName: sub.displayName,
    color: '#5b6b82',
    visible: true,
    writable: false,
    source: 'ics_import',
    logicalId: logicalCalendarId('ics_import', sub.calendarId),
  }
  saveCalendars([...cals, entry])
}

function patchSub(id: string, patch: Partial<IcsSubscription>): void {
  saveIcsSubscriptions(loadIcsSubscriptions().map((s) => (s.id === id ? { ...s, ...patch } : s)))
}

export async function refreshIcsSubscriptions(): Promise<CalendarEvent[]> {
  for (const sub of loadIcsSubscriptions()) {
    try {
      ensureCalendar(sub)
      const imported = parseIcs(await fetchIcsFromUrl(sub.url), sub.calendarId)
      replaceEventsForCalendar(sub.calendarId, 'ics_import', imported)
      patchSub(sub.id, { lastFetchedAt: new Date().toISOString(), lastError: undefined })
    } catch (e) {
      patchSub(sub.id, { lastError: e instanceof Error ? e.message : 'ICS refresh failed' })
    }
  }
  return loadEvents()
}

export async function subscribeIcsUrl(rawUrl: string): Promise<{ count: number; events: CalendarEvent[] }> {
  const url = normalizeIcsUrl(rawUrl)
  const existing = loadIcsSubscriptions()
  const calendarId = subscriptionCalendarId(url)
  if (!existing.some((s) => s.calendarId === calendarId)) {
    const sub: IcsSubscription = {
      id: calendarId,
      url,
      calendarId,
      displayName: displayNameFromIcsUrl(url),
    }
    ensureCalendar(sub)
    saveIcsSubscriptions([...existing, sub])
  }
  const imported = parseIcs(await fetchIcsFromUrl(url), calendarId)
  const events = replaceEventsForCalendar(calendarId, 'ics_import', imported)
  patchSub(calendarId, { lastFetchedAt: new Date().toISOString(), lastError: undefined })
  return { count: imported.length, events }
}

export function unsubscribeIcs(calendarId: string): CalendarEvent[] {
  saveIcsSubscriptions(loadIcsSubscriptions().filter((s) => s.calendarId !== calendarId))
  saveCalendars(loadCalendars().filter((c) => c.id !== calendarId))
  return replaceEventsForCalendar(calendarId, 'ics_import', [])
}

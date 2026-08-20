import type { CalendarEvent, CalendarListEntry } from '@continuum/shared'
import { loadCalendars, loadEvents } from '../data/localStore'
import { loadCalDavAccounts } from './caldav'
import { syncCalDavEvents } from './caldavSync'
import { loadIcsSubscriptions, refreshIcsSubscriptions } from './icsSubscribe'

export async function refreshDesktopOverlays(): Promise<{
  events: CalendarEvent[]
  calendars: CalendarListEntry[]
  errors: string[]
}> {
  const errors: string[] = []
  for (const account of loadCalDavAccounts()) {
    try {
      await syncCalDavEvents(account)
    } catch (e) {
      errors.push(e instanceof Error ? e.message : 'CalDAV sync failed')
    }
  }
  await refreshIcsSubscriptions()
  for (const sub of loadIcsSubscriptions()) {
    if (sub.lastError) errors.push(`${sub.displayName}: ${sub.lastError}`)
  }
  return { events: loadEvents(), calendars: loadCalendars(), errors }
}

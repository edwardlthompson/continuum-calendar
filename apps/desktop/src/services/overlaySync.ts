import type { CalendarEvent, CalendarListEntry } from '@continuum/shared'
import { loadCalendars, loadEvents } from '../data/localStore'
import { loadCalDavAccounts } from './caldav'
import { syncCalDavEvents } from './caldavSync'
import { listGoogleTasks, TASKS_CALENDAR_ID } from './googleTasks'
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
  let tasks: CalendarEvent[] = []
  try {
    tasks = await listGoogleTasks()
  } catch (e) {
    errors.push(e instanceof Error ? e.message : 'Tasks sync failed')
  }
  const events = [
    ...loadEvents().filter((e) => e.calendarId !== TASKS_CALENDAR_ID),
    ...tasks,
  ]
  return { events, calendars: loadCalendars(), errors }
}

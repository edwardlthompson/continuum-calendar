/** Where an event or calendar originates. */
export type CalendarSource = 'google' | 'caldav' | 'local' | 'ics_import' | 'holidays'

export interface CalendarAccount {
  id: string
  displayName: string
  source: CalendarSource
  /** CalDAV principal / server URL when source is caldav. */
  serverUrl?: string
  username?: string
}

export interface CalendarListEntry {
  id: string
  accountId: string
  displayName: string
  color: string
  visible: boolean
  writable: boolean
  source: CalendarSource
  /** Logical id for cross-device settings, e.g. google:primary */
  logicalId: string
}

export interface SyncCursor {
  calendarId: string
  source: CalendarSource
  syncToken?: string
  updatedMin?: string
  ctag?: string
  etag?: string
  lastSyncedAt?: string
}

export function logicalCalendarId(source: CalendarSource, calendarId: string): string {
  return `${source}:${calendarId}`
}

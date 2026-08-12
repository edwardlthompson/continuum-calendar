/** Shared calendar event shape used by desktop and sync layers. */
export type CalendarSource = import('./calendars.js').CalendarSource

export interface CalendarAttendee {
  email: string
  displayName?: string
  responseStatus?: 'needsAction' | 'declined' | 'tentative' | 'accepted'
  optional?: boolean
}

export interface EventReminder {
  /** Minutes before start. */
  minutes: number
  method: 'popup' | 'email' | 'sms' | 'alarm'
}

export interface CalendarEvent {
  id: string
  calendarId: string
  title: string
  description?: string
  location?: string
  /** ISO 8601 start (date or date-time). */
  start: string
  /** ISO 8601 end (date or date-time). */
  end: string
  allDay?: boolean
  attendees?: CalendarAttendee[]
  reminders?: EventReminder[]
  source?: CalendarSource
  /** Google event etag for optimistic concurrency. */
  etag?: string
  updated?: string
  htmlLink?: string
  /** When true, counts as busy for free-slot math (default true). */
  busy?: boolean
  /** Read-only overlay calendar. */
  readOnly?: boolean
  /**
   * Google Calendar API `eventType` when known.
   * Automated contact birthdays use `"birthday"` — distinct from manual yearly events.
   */
  eventType?: 'default' | 'birthday' | 'focusTime' | 'outOfOffice' | 'workingLocation' | 'fromGmail' | string
}

export interface FreeSlot {
  start: Date
  end: Date
}

export interface AgendaDaySection {
  /** YYYY-MM-DD */
  date: string
  events: CalendarEvent[]
  /** True when the day has no events (explicit empty day). */
  isEmpty: boolean
  /** Longest free block within working hours, if computed. */
  longestFreeSlot?: FreeSlot
}

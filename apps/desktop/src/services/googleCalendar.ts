import type {
  CalendarEvent,
  CalendarListEntry,
  EventReminder,
  GoogleOAuthTokens,
} from '@continuum/shared'
import { logicalCalendarId } from '@continuum/shared'
import { ensureFreshTokens } from '../auth/googleAuth'

const CALENDAR_BASE = 'https://www.googleapis.com/calendar/v3'
const PEOPLE_BASE = 'https://people.googleapis.com/v1'

async function authHeaders(): Promise<Headers> {
  const tokens = await ensureFreshTokens()
  if (!tokens) throw new Error('Not authenticated with Google')
  const headers = new Headers()
  headers.set('Authorization', `Bearer ${tokens.accessToken}`)
  return headers
}

function mapReminders(raw: Record<string, unknown>): EventReminder[] | undefined {
  const reminders = raw.reminders as { overrides?: Array<{ minutes?: number; method?: string }> } | undefined
  const overrides = reminders?.overrides
  if (!overrides?.length) return undefined
  return overrides.map((o) => ({
    minutes: o.minutes ?? 10,
    method: (o.method as EventReminder['method']) ?? 'popup',
  }))
}

function mapGoogleEvent(raw: Record<string, unknown>, calendarId: string): CalendarEvent {
  const startObj = (raw.start ?? {}) as { dateTime?: string; date?: string }
  const endObj = (raw.end ?? {}) as { dateTime?: string; date?: string }
  const attendees = Array.isArray(raw.attendees)
    ? (raw.attendees as Array<Record<string, unknown>>).map((a) => ({
        email: String(a.email ?? ''),
        displayName: a.displayName ? String(a.displayName) : undefined,
        responseStatus: a.responseStatus as
          | 'needsAction'
          | 'declined'
          | 'tentative'
          | 'accepted'
          | undefined,
        optional: Boolean(a.optional),
      }))
    : undefined

  const eventType = raw.eventType ? String(raw.eventType) : 'default'
  return {
    id: String(raw.id),
    calendarId,
    title: String(raw.summary ?? '(No title)'),
    description: raw.description ? String(raw.description) : undefined,
    location: raw.location ? String(raw.location) : undefined,
    start: startObj.dateTime ?? startObj.date ?? '',
    end: endObj.dateTime ?? endObj.date ?? '',
    allDay: Boolean(startObj.date && !startObj.dateTime),
    attendees,
    reminders: mapReminders(raw),
    source: 'google',
    etag: raw.etag ? String(raw.etag) : undefined,
    updated: raw.updated ? String(raw.updated) : undefined,
    htmlLink: raw.htmlLink ? String(raw.htmlLink) : undefined,
    busy: eventType !== 'birthday',
    eventType,
  }
}

function googleColorToHex(backgroundColor?: string, colorId?: string): string {
  if (backgroundColor && /^#?[0-9a-fA-F]{6}$/.test(backgroundColor)) {
    return backgroundColor.startsWith('#') ? backgroundColor : `#${backgroundColor}`
  }
  // Fallback palette indexed by Google colorId when backgroundColor absent.
  const palette = [
    '#0f6e8c',
    '#7986cb',
    '#33b679',
    '#8e24aa',
    '#e67c73',
    '#f6bf26',
    '#f4511e',
    '#039be5',
    '#616161',
    '#3f51b5',
    '#0b8043',
    '#d50000',
  ]
  const idx = Number(colorId)
  if (Number.isFinite(idx) && idx >= 0) return palette[idx % palette.length] ?? '#0f6e8c'
  return '#0f6e8c'
}

export async function listGoogleCalendars(): Promise<CalendarListEntry[]> {
  const headers = await authHeaders()
  const res = await fetch(`${CALENDAR_BASE}/users/me/calendarList?maxResults=250`, { headers })
  if (!res.ok) throw new Error(`Calendar list failed: ${res.status}`)
  const data = (await res.json()) as {
    items?: Array<{
      id?: string
      summary?: string
      backgroundColor?: string
      colorId?: string
      accessRole?: string
      selected?: boolean
      primary?: boolean
    }>
  }
  const out: CalendarListEntry[] = []
  for (const item of data.items ?? []) {
    const id = item.id?.trim()
    if (!id) continue
    const writable = item.accessRole === 'owner' || item.accessRole === 'writer'
    out.push({
      id,
      accountId: 'google',
      displayName: item.summary?.trim() || id,
      color: googleColorToHex(item.backgroundColor, item.colorId),
      visible: item.selected !== false,
      writable,
      source: 'google',
      // Invites live on primary; keep a stable logical id across email vs "primary" alias.
      logicalId: item.primary ? logicalCalendarId('google', 'primary') : logicalCalendarId('google', id),
    })
  }
  if (!out.length) {
    out.push({
      id: 'primary',
      accountId: 'google',
      displayName: 'Google Primary',
      color: '#0f6e8c',
      visible: true,
      writable: true,
      source: 'google',
      logicalId: logicalCalendarId('google', 'primary'),
    })
  }
  return out
}

async function fetchAllGoogleEventPages(
  calendarId: string,
  baseParams: URLSearchParams,
): Promise<{ items: Record<string, unknown>[]; nextSyncToken?: string }> {
  const headers = await authHeaders()
  const items: Record<string, unknown>[] = []
  let pageToken: string | undefined
  let nextSyncToken: string | undefined
  // Cap pages so a runaway token cannot hang the UI; 40 × 2500 ≫ normal calendars.
  for (let page = 0; page < 40; page++) {
    const params = new URLSearchParams(baseParams)
    if (pageToken) params.set('pageToken', pageToken)
    const res = await fetch(
      `${CALENDAR_BASE}/calendars/${encodeURIComponent(calendarId)}/events?${params}`,
      { headers },
    )
    if (res.status === 410) throw new Error('SYNC_TOKEN_EXPIRED')
    if (!res.ok) throw new Error(`Calendar list failed: ${res.status}`)
    const data = (await res.json()) as {
      items?: Record<string, unknown>[]
      nextPageToken?: string
      nextSyncToken?: string
    }
    items.push(...(data.items ?? []))
    pageToken = data.nextPageToken
    if (data.nextSyncToken) nextSyncToken = data.nextSyncToken
    if (!pageToken) break
  }
  return { items, nextSyncToken }
}

export async function listGoogleEvents(
  calendarId: string,
  timeMin: Date,
  timeMax: Date,
): Promise<{ events: CalendarEvent[]; nextSyncToken?: string }> {
  const params = new URLSearchParams({
    timeMin: timeMin.toISOString(),
    timeMax: timeMax.toISOString(),
    singleEvents: 'true',
    orderBy: 'startTime',
    maxResults: '2500',
  })
  const { items, nextSyncToken } = await fetchAllGoogleEventPages(calendarId, params)
  return {
    events: items
      .filter((item) => item.status !== 'cancelled')
      .map((item) => mapGoogleEvent(item, calendarId)),
    nextSyncToken,
  }
}

export async function syncGoogleEventsIncremental(
  calendarId: string,
  syncToken: string,
): Promise<{ events: CalendarEvent[]; deletedIds: string[]; nextSyncToken?: string }> {
  const params = new URLSearchParams({ syncToken, maxResults: '2500' })
  const { items, nextSyncToken } = await fetchAllGoogleEventPages(calendarId, params)
  const events: CalendarEvent[] = []
  const deletedIds: string[] = []
  for (const item of items) {
    if (item.status === 'cancelled') deletedIds.push(String(item.id))
    else events.push(mapGoogleEvent(item, calendarId))
  }
  return { events, deletedIds, nextSyncToken }
}

export async function createGoogleEvent(
  calendarId: string,
  event: Omit<CalendarEvent, 'id' | 'etag' | 'updated' | 'htmlLink' | 'source'>,
): Promise<CalendarEvent> {
  const headers = await authHeaders()
  headers.set('Content-Type', 'application/json')
  const body = {
    summary: event.title,
    description: event.description,
    location: event.location,
    start: event.allDay ? { date: event.start.slice(0, 10) } : { dateTime: event.start },
    end: event.allDay ? { date: event.end.slice(0, 10) } : { dateTime: event.end },
    attendees: event.attendees?.map((a) => ({
      email: a.email,
      displayName: a.displayName,
      optional: a.optional,
    })),
    reminders: event.reminders
      ? { useDefault: false, overrides: event.reminders.map((r) => ({ method: r.method, minutes: r.minutes })) }
      : undefined,
  }
  const res = await fetch(`${CALENDAR_BASE}/calendars/${encodeURIComponent(calendarId)}/events`, {
    method: 'POST',
    headers,
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error(`Calendar create failed: ${res.status}`)
  return mapGoogleEvent((await res.json()) as Record<string, unknown>, calendarId)
}

export async function updateGoogleEvent(event: CalendarEvent): Promise<CalendarEvent> {
  const headers = await authHeaders()
  headers.set('Content-Type', 'application/json')
  if (event.etag) headers.set('If-Match', event.etag)
  const body = {
    summary: event.title,
    description: event.description,
    location: event.location,
    start: event.allDay ? { date: event.start.slice(0, 10) } : { dateTime: event.start },
    end: event.allDay ? { date: event.end.slice(0, 10) } : { dateTime: event.end },
    attendees: event.attendees?.map((a) => ({
      email: a.email,
      displayName: a.displayName,
      optional: a.optional,
    })),
  }
  const res = await fetch(
    `${CALENDAR_BASE}/calendars/${encodeURIComponent(event.calendarId)}/events/${encodeURIComponent(event.id)}`,
    { method: 'PUT', headers, body: JSON.stringify(body) },
  )
  if (!res.ok) throw new Error(`Calendar update failed: ${res.status}`)
  return mapGoogleEvent((await res.json()) as Record<string, unknown>, event.calendarId)
}

export async function deleteGoogleEvent(calendarId: string, eventId: string): Promise<void> {
  const headers = await authHeaders()
  const res = await fetch(
    `${CALENDAR_BASE}/calendars/${encodeURIComponent(calendarId)}/events/${encodeURIComponent(eventId)}`,
    { method: 'DELETE', headers },
  )
  // 404/410: already gone on Google — treat as success so local delete still sticks.
  if (!res.ok && res.status !== 204 && res.status !== 404 && res.status !== 410) {
    throw new Error(`Calendar delete failed: ${res.status}`)
  }
}

export async function searchGoogleContacts(query: string, pageSize = 10) {
  const q = query.trim()
  if (!q) return []
  const headers = await authHeaders()
  const params = new URLSearchParams({
    query: q,
    pageSize: String(pageSize),
    readMask: 'names,emailAddresses,photos',
  })
  const res = await fetch(`${PEOPLE_BASE}/people:searchContacts?${params}`, { headers })
  if (!res.ok) throw new Error(`Contacts search failed: ${res.status}`)
  const data = (await res.json()) as {
    results?: Array<{
      person?: {
        resourceName?: string
        names?: Array<{ displayName?: string }>
        emailAddresses?: Array<{ value?: string }>
        photos?: Array<{ url?: string }>
      }
    }>
  }
  const contacts = []
  for (const r of data.results ?? []) {
    const person = r.person
    if (!person) continue
    const emails = (person.emailAddresses ?? []).map((e) => e.value).filter((v): v is string => Boolean(v))
    if (!emails.length) continue
    contacts.push({
      resourceName: person.resourceName,
      displayName: person.names?.[0]?.displayName ?? emails[0],
      emails,
      photoUrl: person.photos?.[0]?.url,
    })
  }
  return contacts
}

export type { GoogleOAuthTokens }

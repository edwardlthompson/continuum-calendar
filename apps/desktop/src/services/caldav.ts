import type { CalendarEvent, CalendarListEntry } from '@continuum/shared'
import { logicalCalendarId } from '@continuum/shared'

export interface CalDavAccount {
  id: string
  displayName: string
  serverUrl: string
  username: string
  /** App password — stored locally only, never synced via Drive. */
  password: string
}

const ACCOUNTS_KEY = 'continuum.caldav.accounts'

export function loadCalDavAccounts(): CalDavAccount[] {
  try {
    return JSON.parse(localStorage.getItem(ACCOUNTS_KEY) ?? '[]') as CalDavAccount[]
  } catch {
    return []
  }
}

export function saveCalDavAccounts(accounts: CalDavAccount[]): void {
  localStorage.setItem(ACCOUNTS_KEY, JSON.stringify(accounts))
}

/**
 * Minimal CalDAV discovery + sync stub.
 * Full REPORT/multiget can be swapped for `tsdav` once networked; this validates
 * credentials with a PROPFIND and registers calendar entries for the sidebar.
 */
export async function discoverCalDavCalendars(account: CalDavAccount): Promise<CalendarListEntry[]> {
  const url = account.serverUrl.replace(/\/?$/, '/')
  const auth = btoa(`${account.username}:${account.password}`)
  const res = await fetch(url, {
    method: 'PROPFIND',
    headers: {
      Authorization: `Basic ${auth}`,
      Depth: '0',
      'Content-Type': 'application/xml',
    },
    body: `<?xml version="1.0"?><d:propfind xmlns:d="DAV:"><d:prop><d:displayname/></d:prop></d:propfind>`,
  })
  if (!res.ok && res.status !== 207) {
    throw new Error(`CalDAV PROPFIND failed: ${res.status}`)
  }
  const id = `caldav-${account.id}`
  return [
    {
      id,
      accountId: account.id,
      displayName: account.displayName || 'CalDAV',
      color: '#2a9d8f',
      visible: true,
      writable: true,
      source: 'caldav',
      logicalId: logicalCalendarId('caldav', id),
    },
  ]
}

/** Placeholder sync — returns empty until full CalDAV REPORT is wired. */
export async function syncCalDavEvents(_account: CalDavAccount): Promise<CalendarEvent[]> {
  return []
}

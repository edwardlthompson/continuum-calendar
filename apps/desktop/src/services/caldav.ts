import type { CalendarListEntry } from '@continuum/shared'
import { logicalCalendarId } from '@continuum/shared'
import { parseCalDavDisplayName } from './caldavParse'

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

export function calDavAuthHeader(account: CalDavAccount): string {
  return `Basic ${btoa(`${account.username}:${account.password}`)}`
}

/**
 * PROPFIND credential check + sidebar entry. Event bodies come from REPORT in caldavSync.
 */
export async function discoverCalDavCalendars(account: CalDavAccount): Promise<CalendarListEntry[]> {
  const url = account.serverUrl.replace(/\/?$/, '/')
  const res = await fetch(url, {
    method: 'PROPFIND',
    headers: {
      Authorization: calDavAuthHeader(account),
      Depth: '0',
      'Content-Type': 'application/xml',
    },
    body: `<?xml version="1.0"?><d:propfind xmlns:d="DAV:"><d:prop><d:displayname/></d:prop></d:propfind>`,
  })
  if (!res.ok && res.status !== 207) {
    throw new Error(`CalDAV PROPFIND failed: ${res.status}`)
  }
  const xml = await res.text()
  const id = `caldav-${account.id}`
  const name = account.displayName || parseCalDavDisplayName(xml) || 'CalDAV'
  return [
    {
      id,
      accountId: account.id,
      displayName: name,
      color: '#2a9d8f',
      visible: true,
      writable: false,
      source: 'caldav',
      logicalId: logicalCalendarId('caldav', id),
    },
  ]
}

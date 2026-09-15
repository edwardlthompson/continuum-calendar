import type { CalendarListEntry } from '@continuum/shared'
import type { GoogleAuthStatus } from '../auth/authSession'

/** Sidebar label. Never hides the calendar — signed-out Google stays listed. */
export function calendarCaption(
  entry: Pick<CalendarListEntry, 'id' | 'displayName' | 'source'>,
  authStatus: GoogleAuthStatus,
): string {
  const name = entry.displayName?.trim() || entry.id
  if (entry.source === 'google' && authStatus !== 'signed-in') {
    return `${name} · Sign in to sync`
  }
  return name
}

import type { CalendarEvent, CalendarListEntry } from '@continuum/shared'
import { listGoogleCalendars, listGoogleEvents, syncGoogleEventsIncremental } from './googleCalendar'
import { ensureFreshTokens } from '../auth/googleAuth'
import {
  loadCalendars,
  loadCursors,
  loadEvents,
  replaceGoogleEventsInWindow,
  saveCalendars,
  saveCursor,
  saveEvents,
  upsertEvents,
} from '../data/localStore'

export interface SyncStatus {
  lastSyncedAt: string | null
  lastError: string | null
  source: string
}

let lastStatus: SyncStatus = { lastSyncedAt: null, lastError: null, source: 'idle' }

const CURSOR_PAGINATE_MIGRATE_KEY = 'continuum.syncCursors.paginate.v2'

/** One-time: drop Google sync tokens so the next pull is a full paginated window (invites included). */
function migrateClearGoogleCursorsForPagination(): boolean {
  try {
    if (localStorage.getItem(CURSOR_PAGINATE_MIGRATE_KEY)) return false
    const kept = loadCursors().filter((c) => c.source !== 'google')
    localStorage.setItem('continuum.syncCursors.v1', JSON.stringify(kept))
    localStorage.setItem(CURSOR_PAGINATE_MIGRATE_KEY, '1')
    return true
  } catch {
    return false
  }
}

export function getSyncStatus(): SyncStatus {
  return lastStatus
}

/** Point orphaned `calendarId: "primary"` Google events at the real primary calendar id. */
function remapPrimaryAliasEvents(primaryId: string): void {
  if (!primaryId || primaryId === 'primary') return
  const events = loadEvents()
  let changed = false
  const next = events.map((e) => {
    if (e.source === 'google' && e.calendarId === 'primary') {
      changed = true
      return { ...e, calendarId: primaryId }
    }
    return e
  })
  if (changed) saveEvents(next)
}

function resolvePrimaryGoogleCalendar(calendars: CalendarListEntry[]): CalendarListEntry | undefined {
  return (
    calendars.find((c) => c.source === 'google' && c.logicalId === 'google:primary') ??
    calendars.find((c) => c.source === 'google' && c.id === 'primary')
  )
}

/** Merge Google calendarList into local store; keep non-Google calendars. */
export function mergeGoogleCalendarList(remote: CalendarListEntry[]): CalendarListEntry[] {
  const existing = loadCalendars()
  const nonGoogle = existing.filter((c) => c.source !== 'google')
  const prevVisibility = new Map(
    existing.filter((c) => c.source === 'google').map((c) => [c.id, c.visible] as const),
  )
  const google = remote.map((c) => {
    const isPrimary = c.logicalId === 'google:primary'
    // Invites land on primary — keep it display-visible so they never vanish from the UI.
    const visible = isPrimary
      ? true
      : prevVisibility.has(c.id)
        ? Boolean(prevVisibility.get(c.id))
        : c.visible
    return { ...c, visible }
  })
  const next = [...google, ...nonGoogle]
  saveCalendars(next)
  const primary = resolvePrimaryGoogleCalendar(next)
  if (primary) remapPrimaryAliasEvents(primary.id)
  return next
}

export async function refreshGoogleCalendarList(): Promise<CalendarListEntry[]> {
  const tokens = await ensureFreshTokens()
  if (!tokens) return loadCalendars()
  const remote = await listGoogleCalendars()
  return mergeGoogleCalendarList(remote)
}

async function syncOneGoogleCalendar(
  calendarId: string,
  options: { forceFull?: boolean } = {},
): Promise<CalendarEvent[]> {
  const cursor = loadCursors().find((c) => c.calendarId === calendarId && c.source === 'google')
  if (!options.forceFull && cursor?.syncToken) {
    try {
      const { events, deletedIds, nextSyncToken } = await syncGoogleEventsIncremental(
        calendarId,
        cursor.syncToken,
      )
      const merged = upsertEvents(events, deletedIds)
      if (nextSyncToken) {
        saveCursor({
          calendarId,
          source: 'google',
          syncToken: nextSyncToken,
          lastSyncedAt: new Date().toISOString(),
        })
      }
      return merged
    } catch (e) {
      if (!(e instanceof Error && e.message === 'SYNC_TOKEN_EXPIRED')) throw e
    }
  }

  const timeMin = new Date()
  timeMin.setDate(timeMin.getDate() - 30)
  const timeMax = new Date()
  timeMax.setDate(timeMax.getDate() + 90)
  const { events, nextSyncToken } = await listGoogleEvents(calendarId, timeMin, timeMax)
  // Full pulls omit cancelled/deleted items — prune local Google rows missing from the window.
  // Include "primary" alias when syncing the real primary calendar id.
  const primary = resolvePrimaryGoogleCalendar(loadCalendars())
  const aliasIds =
    calendarId === 'primary' || calendarId === primary?.id ? ['primary', primary?.id ?? ''] : []
  const merged = replaceGoogleEventsInWindow(calendarId, events, timeMin, timeMax, aliasIds)
  if (nextSyncToken) {
    saveCursor({
      calendarId,
      source: 'google',
      syncToken: nextSyncToken,
      lastSyncedAt: new Date().toISOString(),
    })
  }
  return merged
}

/** Sync one calendar (legacy helper). */
export async function syncGoogleCalendar(calendarId = 'primary'): Promise<CalendarEvent[]> {
  const tokens = await ensureFreshTokens()
  if (!tokens) {
    lastStatus = { ...lastStatus, lastError: 'Not signed in', source: 'google' }
    return upsertEvents([])
  }
  try {
    const merged = await syncOneGoogleCalendar(calendarId)
    lastStatus = { lastSyncedAt: new Date().toISOString(), lastError: null, source: 'google' }
    return merged
  } catch (e) {
    lastStatus = {
      lastSyncedAt: lastStatus.lastSyncedAt,
      lastError: e instanceof Error ? e.message : 'Sync failed',
      source: 'google',
    }
    throw e
  }
}

export interface MultiSyncResult {
  events: CalendarEvent[]
  calendars: CalendarListEntry[]
  errors: string[]
}

/** Refresh calendarList and sync Google calendars (partial success OK). */
export async function syncAllVisibleGoogleCalendars(): Promise<MultiSyncResult> {
  const forcedFull = migrateClearGoogleCursorsForPagination()

  const tokens = await ensureFreshTokens()
  if (!tokens) {
    lastStatus = { ...lastStatus, lastError: 'Not signed in', source: 'google' }
    return { events: upsertEvents([]), calendars: loadCalendars(), errors: ['Not signed in'] }
  }

  const errors: string[] = []
  let calendars: CalendarListEntry[]
  try {
    calendars = await refreshGoogleCalendarList()
  } catch (e) {
    calendars = loadCalendars()
    errors.push(e instanceof Error ? e.message : 'calendarList failed')
  }

  // Sync every Google calendar (visibility is display-only). Invites/Meetings can live
  // on primary or on secondary calendars that were unchecked in the sidebar.
  const primary = resolvePrimaryGoogleCalendar(calendars)
  const idSet = new Set(calendars.filter((c) => c.source === 'google').map((c) => c.id))
  if (primary) idSet.add(primary.id)
  else idSet.add('primary')
  const ids = [...idSet]

  let events = upsertEvents([])
  for (const id of ids) {
    try {
      const isPrimary = id === 'primary' || id === primary?.id
      // Primary always does a full window pull so new invites are not skipped by stale syncTokens.
      events = await syncOneGoogleCalendar(id, { forceFull: forcedFull || isPrimary })
    } catch (e) {
      errors.push(`${id}: ${e instanceof Error ? e.message : 'sync failed'}`)
    }
  }

  if (primary) remapPrimaryAliasEvents(primary.id)
  events = loadEvents()

  lastStatus = {
    lastSyncedAt: new Date().toISOString(),
    lastError: errors.length ? errors.join('; ') : null,
    source: 'google',
  }
  return { events, calendars, errors }
}

/** Poll while focused — call from App effect. Runs once immediately so cold start pulls invites. */
export function startFocusSyncLoop(onTick: () => void, intervalMs = 45_000): () => void {
  if (document.visibilityState === 'visible') onTick()
  const id = window.setInterval(() => {
    if (document.visibilityState === 'visible') onTick()
  }, intervalMs)
  const onVis = () => {
    if (document.visibilityState === 'visible') onTick()
  }
  document.addEventListener('visibilitychange', onVis)
  return () => {
    window.clearInterval(id)
    document.removeEventListener('visibilitychange', onVis)
  }
}

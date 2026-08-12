import type { ThemeMode } from './theme.js'

export interface WorkingHours {
  start: string
  end: string
}

/** Per-calendar notification channels (peer-synced by logicalId). */
export interface CalendarNotifyPrefs {
  /** Alert when a peer device adds/updates an event on this calendar. */
  newEvent: boolean
  /** Existing event-reminder alarms for this calendar. */
  reminder: boolean
}

export interface ContinuumSettings {
  themeMode: ThemeMode
  showEmptyDaysInAgenda: boolean
  rollingWeekFromToday: boolean
  weeklyViewDays: number
  workingHours: WorkingHours
  defaultReminderMinutes: number
  notificationEnabled: boolean
  defaultSnoozeMinutes: number
  visibleCalendarIds: string[]
  defaultWriteCalendarId: string
  agendaRangeDays: number
  slotMinMinutes: number
  /** When true, mask event titles in UI for demos / screenshots. */
  redactTitlesInScreenshots: boolean
  /** Travel buffer minutes applied around events for free-slot math. */
  travelBufferMinutes: number
  agendaDensity: 'compact' | 'comfortable'
  secondaryTimeZone?: string
  /** When false, hide contact birthday events / birthday calendars. */
  showContactBirthdays: boolean
  /** 24-hour clock (peer-synced). */
  use24HourFormat: boolean
  /** First day of week: 0=Sunday … 6=Saturday (JS / FullCalendar). */
  firstDayOfWeek: number
  /**
   * When false, privacy mode: do not sync/show Google Calendar;
   * Continuum local calendars still peer-sync via Drive App Data.
   */
  useGoogleCalendar: boolean
  /** Per logicalId notify prefs (New event / Event reminder). */
  calendarNotifyPrefs: Record<string, CalendarNotifyPrefs>
}

export interface SettingsUpdatedBy {
  platform: 'android' | 'desktop' | 'unknown'
  deviceId: string
  appVersion: string
}

/** Versioned Drive App Data envelope — monotonic revision is authoritative. */
export interface ContinuumSettingsEnvelope {
  schemaVersion: number
  revision: number
  updatedAt: string
  updatedBy: SettingsUpdatedBy
  contentHash: string
  settings: ContinuumSettings
}

export const SETTINGS_SCHEMA_VERSION = 1
export const SETTINGS_APP_DATA_FILENAME = 'continuum-settings.json'
export const SETTINGS_APP_DATA_PREV_FILENAME = 'continuum-settings.prev.json'
/** Peer-synced Continuum-owned local calendars + events (privacy path). */
export const LOCAL_EVENTS_APP_DATA_FILENAME = 'continuum-local-events.json'
export const LOCAL_EVENTS_SCHEMA_VERSION = 1

export function defaultContinuumSettings(): ContinuumSettings {
  return {
    themeMode: 'system',
    showEmptyDaysInAgenda: true,
    rollingWeekFromToday: true,
    weeklyViewDays: 7,
    workingHours: { start: '09:00', end: '17:00' },
    defaultReminderMinutes: 10,
    notificationEnabled: true,
    defaultSnoozeMinutes: 10,
    visibleCalendarIds: ['google:primary'],
    defaultWriteCalendarId: 'google:primary',
    agendaRangeDays: 30,
    slotMinMinutes: 30,
    redactTitlesInScreenshots: false,
    travelBufferMinutes: 0,
    agendaDensity: 'comfortable',
    showContactBirthdays: true,
    use24HourFormat: false,
    firstDayOfWeek: 0,
    useGoogleCalendar: true,
    calendarNotifyPrefs: {},
  }
}

/** Defaults: newEvent off for holidays; otherwise both on. */
export function defaultCalendarNotifyPrefs(logicalId: string): CalendarNotifyPrefs {
  const isHoliday = logicalId.startsWith('holidays:')
  return { newEvent: !isHoliday, reminder: true }
}

export function resolveCalendarNotifyPrefs(
  prefs: Record<string, CalendarNotifyPrefs> | undefined,
  logicalId: string,
): CalendarNotifyPrefs {
  const d = defaultCalendarNotifyPrefs(logicalId)
  const raw = prefs?.[logicalId]
  if (!raw || typeof raw !== 'object') return d
  return {
    newEvent: typeof raw.newEvent === 'boolean' ? raw.newEvent : d.newEvent,
    reminder: typeof raw.reminder === 'boolean' ? raw.reminder : d.reminder,
  }
}

/** Normalize a remote/partial map; drop empty keys. */
export function normalizeCalendarNotifyPrefs(
  raw: unknown,
): Record<string, CalendarNotifyPrefs> {
  if (!raw || typeof raw !== 'object') return {}
  const out: Record<string, CalendarNotifyPrefs> = {}
  for (const [key, value] of Object.entries(raw as Record<string, unknown>)) {
    if (!key.trim()) continue
    out[key] = resolveCalendarNotifyPrefs({ [key]: value as CalendarNotifyPrefs }, key)
  }
  return out
}

export function defaultSettingsEnvelope(
  updatedBy: SettingsUpdatedBy,
  settings: ContinuumSettings = defaultContinuumSettings(),
): ContinuumSettingsEnvelope {
  return {
    schemaVersion: SETTINGS_SCHEMA_VERSION,
    revision: 1,
    updatedAt: new Date().toISOString(),
    updatedBy,
    contentHash: '',
    settings,
  }
}

/** Clamp firstDayOfWeek to 0..6. */
export function normalizeFirstDayOfWeek(value: unknown): number {
  const n = Number(value)
  if (!Number.isFinite(n)) return 0
  const i = Math.trunc(n)
  if (i < 0 || i > 6) return 0
  return i
}

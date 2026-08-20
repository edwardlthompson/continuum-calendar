export type { ThemeMode } from './theme.js'
export { THEME_MODES, resolveTheme } from './theme.js'
export type {
  CalendarAttendee,
  CalendarEvent,
  EventReminder,
  FreeSlot,
  AgendaDaySection,
} from './events.js'
export type { ContactSummary } from './contacts.js'
export type { GoogleOAuthTokens } from './oauth.js'
export { GOOGLE_SCOPES, GOOGLE_SCOPE_STRING, isTokenExpired } from './oauth.js'
export type { CalendarSource, CalendarAccount, CalendarListEntry, SyncCursor } from './calendars.js'
export { logicalCalendarId } from './calendars.js'
export type {
  CalendarNotifyPrefs,
  ContinuumSettings,
  ContinuumSettingsEnvelope,
  SettingsUpdatedBy,
  WorkingHours,
} from './settings.js'
export {
  SETTINGS_SCHEMA_VERSION,
  SETTINGS_APP_DATA_FILENAME,
  SETTINGS_APP_DATA_PREV_FILENAME,
  LOCAL_EVENTS_APP_DATA_FILENAME,
  LOCAL_EVENTS_SCHEMA_VERSION,
  defaultCalendarNotifyPrefs,
  defaultContinuumSettings,
  defaultSettingsEnvelope,
  normalizeCalendarNotifyPrefs,
  normalizeFirstDayOfWeek,
  resolveCalendarNotifyPrefs,
} from './settings.js'
export type { ContinuumLocalEventsEnvelope, LocalEventTombstone } from './localEvents.js'
export {
  emptyLocalEventsEnvelope,
  eventKey,
  filterContinuumOwnedCalendars,
  filterContinuumOwnedEvents,
  mergeLocalEventsPayload,
  isValidLocalEventsEnvelope,
} from './localEvents.js'
export {
  hashSettings,
  mergeSettings,
  prepareSettingsWrite,
  shouldApplyRemote,
  isValidEnvelope,
  decidePeerReconcile,
} from './settingsSync.js'
export type { PeerReconcileAction } from './settingsSync.js'
export {
  buildAgendaSections,
  dayHasTimedEvents,
  dayShouldShowOpen,
  eventEndMs,
  isAllDayEvent,
  isEventPast,
  localDateKey,
  splitDayEventsAtNow,
  todayAgendaPhase,
  workingHoursEndMs,
} from './agenda.js'
export type { TodayAgendaPhase } from './agenda.js'
export { formatAgendaSectionTitle, CONTINUUM_OPEN_DAY_COLOR } from './agendaHeaders.js'
export {
  detectConflicts,
  eventOccurrenceKey,
  isTimedBusyEvent,
  conflictsForEvent,
  suggestConflictFreeTime,
  isCrossSourceConflict,
  crossSourceConflicts,
  formatConflictSources,
} from './conflicts.js'
export type { EventConflict, ConflictCandidate } from './conflicts.js'
export { proposeMeetingTimes } from './proposeTimes.js'
export {
  isBirthdayCalendarLabel,
  isBirthdayCalendarEntry,
  isContactBirthdayEvent,
} from './birthdays.js'

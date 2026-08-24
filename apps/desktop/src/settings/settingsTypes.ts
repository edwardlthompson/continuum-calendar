import type { CalendarEvent, CalendarListEntry, ContinuumSettings } from '@continuum/shared'
import type { GoogleAuthStatus } from '../auth/authSession'
import type { HolidayPackId } from '../services/holidayPacks'
import type { CloseTarget, MinimizeTarget, WindowBehavior } from '../services/windowBehavior'

export type SettingsFormModel = {
  query: string
  onQuery: (q: string) => void
  settings: ContinuumSettings
  persistSettings: (patch: Partial<ContinuumSettings>, toast?: string) => void
  applySettings: (next: ContinuumSettings, toast?: string) => void
  authStatus: GoogleAuthStatus
  signedIn: boolean
  resolvedTheme: string
  lastSyncedAt: string | number | Date | null | undefined
  lastSyncError: string | null | undefined
  onSignIn: () => void
  windowBehavior: WindowBehavior
  setWindowBehavior: (next: WindowBehavior) => void
  startWithWindows: boolean
  setStartWithWindows: (on: boolean) => void
  holidayPack: HolidayPackId
  setHolidayPack: (id: HolidayPackId) => void
  calendars: CalendarListEntry[]
  displayCalendars: CalendarListEntry[]
  setCalendars: (next: CalendarListEntry[]) => void
  visibleEvents: CalendarEvent[]
  setEvents: (next: CalendarEvent[]) => void
  flash: (msg: string) => void
  onToggleNotifications: (on: boolean) => void
  onImportIcs: (file: File) => void
  onOpenCalendarLink: () => void
  onSubscribeIcs: () => void
  onAddCalDav: () => void
}

export type SettingsSectionProps = {
  form: SettingsFormModel
  query: string
}

export type { CloseTarget, MinimizeTarget }

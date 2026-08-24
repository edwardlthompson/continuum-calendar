import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  conflictsForEvent,
  crossSourceConflicts,
  detectConflicts,
  applyRecurrenceEdit,
  expandRecurringEvents,
  eventOccurrenceKey,
  remainingTodayCount,
  nextRemainingTickMs,
  seriesEventId,
  formatConflictSources,
  uniqueConflictDates,
  peekNextConflictDate,
  earliestConflictTimeOnDate,
  localDateKey,
  isBirthdayCalendarEntry,
  isContactBirthdayEvent,
  proposeMeetingTimes,
  suggestConflictFreeTime,
  type RecurrenceEditScope,
  type CalendarEvent,
  type CalendarNotifyPrefs,
  type ContinuumSettings,
  type FreeSlot,
} from '@continuum/shared'
import { RollingWeekView } from './components/RollingWeekView'
import { AgendaView } from './components/AgendaView'
import { EventEditor } from './components/EventEditor'
import { CalendarSidebar } from './components/CalendarSidebar'
import { ContinuumSplash } from './components/ContinuumSplash'
import { AppTitle } from './components/AppTitle'
import { useTheme } from './theme/ThemeContext'
import {
  exchangeCodeForTokens,
  ensureFreshTokens,
  getStoredTokens,
  humanizeOAuthFailure,
  isGoogleConfigured,
  isInsufficientDriveScope,
  parseOAuthCallback,
  signInWithGoogle,
  signOutGoogle,
} from './auth/googleAuth'
import {
  authStatusFromTokens,
  clearNeedsReauth,
  loadNeedsReauth,
  onAuthExpired,
  type GoogleAuthStatus,
} from './auth/authSession'
import {
  deleteLocalEvent,
  ensureSeededEvents,
  loadCalendars,
  loadEvents,
  saveCalendars,
  saveEvents,
  upsertEvents,
} from './data/localStore'
import { syncAllVisibleGoogleCalendars, startFocusSyncLoop, getSyncStatus } from './services/syncService'
import {
  loadLocalSettings,
  pushSettingsPatch,
  startSettingsPollLoop,
  reconcilePeerSettings,
  markPendingPeerPush,
} from './services/settingsSync'
import { isTransientPeerError, resetPeerSyncBackoff } from './services/peerSyncControl'
import { applyBirthdayCalendarVisibility } from './utils/birthdayVisibility'
import {
  clearScheduledReminders,
  ensurePermission,
  notifyNewPeerEventsFromPull,
  rescheduleReminders,
} from './services/notifications'
import { getDeviceId } from './auth/tokenStore'
import { copyFreeSlotsToClipboard } from './utils/freeSlots'
import { importIcsFromUrl, importIcsText, looksLikeIcsFileName } from './services/icsImport'
import { discoverCalDavCalendars, loadCalDavAccounts, saveCalDavAccounts, type CalDavAccount } from './services/caldav'
import { syncCalDavEvents } from './services/caldavSync'
import { refreshDesktopOverlays } from './services/overlaySync'
import {
  subscribeIcsUrl,
} from './services/icsSubscribe'
import {
  createGoogleEvent,
  createGoogleEventCopy,
  deleteGoogleEvent,
  updateGoogleEvent,
} from './services/googleCalendar'
import {
  noteLocalEventsChanged,
  pushLocalEventsNow,
  reconcileLocalEventsPeer,
  recordLocalEventTombstone,
  startLocalEventsPollLoop,
  toPeerIso,
} from './services/localEventsSync'
import { continuumLogger } from './diagnostics/continuumLogger'
import { newEventDefaults } from './utils/defaultCalendar'
import { VENMO_DONATE_URL } from './about/donate'
import { openExternal } from './about/openExternal'
import { decideLaunchPrompt, type LaunchPrompt } from './about/runAppUpdates'
import { markUpdateChecked, markVersionSeen } from './about/updatePrefs'
import { DonateNudgeDialog, UpdateAvailableDialog } from './components/AppUpdateDialogs'
import { AuthReconnectBanner } from './components/AuthReconnectBanner'
import { CalendarToolbar, type MainView } from './components/CalendarToolbar'
import { useDesktopHotkeys } from './hooks/useDesktopHotkeys'
import { hotkeyTitle } from './hooks/desktopHotkeys'
import { mergeHolidayEvents, type HolidayPackId } from './services/holidayPacks'
import { SettingsPanel } from './settings/SettingsPanel'
import { renderBadgePng, renderOverlayPng } from './services/dayBadge'
import {
  loadWindowBehavior,
  toNativeArgs,
  type WindowBehavior,
} from './services/windowBehavior'
import { readStartWithWindows } from './services/windowsAutostart'
import { listen } from '@tauri-apps/api/event'
import { invoke } from '@tauri-apps/api/core'

function startOfDay(d: Date): Date {
  const x = new Date(d)
  x.setHours(0, 0, 0, 0)
  return x
}
function addDays(d: Date, n: number): Date {
  const x = new Date(d)
  x.setDate(x.getDate() + n)
  return x
}

function isTauri(): boolean {
  return typeof window !== 'undefined' && '__TAURI_INTERNALS__' in window
}

export default function App() {
  const { mode, setMode, resolved } = useTheme()
  const [settings, setSettings] = useState<ContinuumSettings>(() => loadLocalSettings())
  const knownPeerEventKeysRef = useRef<Set<string>>(
    new Set(
      loadEvents()
        .filter((e) => e.source === 'local' || e.source === 'ics_import')
        .map((e) => `${e.calendarId}:${e.id}`),
    ),
  )
  const [events, setEvents] = useState<CalendarEvent[]>(() => ensureSeededEvents())
  const [calendars, setCalendars] = useState(() => loadCalendars())
  const [view, setView] = useState<MainView>('agenda')
  const [eventQuery, setEventQuery] = useState('')
  const [jumpDate, setJumpDate] = useState(() => localDateKey())
  const [focusSeq, setFocusSeq] = useState(0)
  const [holidayPack, setHolidayPack] = useState<HolidayPackId>(() => {
    const raw = localStorage.getItem('continuum.holidayPack')
    return raw === 'us' || raw === 'ca' || raw === 'gb' || raw === 'de' ? raw : 'none'
  })
  const searchRef = useRef<HTMLInputElement>(null)
  const [windowBehavior, setWindowBehavior] = useState<WindowBehavior>(loadWindowBehavior)
  const [startWithWindows, setStartWithWindows] = useState(false)
  const [authStatus, setAuthStatus] = useState<GoogleAuthStatus>(() =>
    loadNeedsReauth() ? 'needs-reauth' : 'signed-out',
  )
  const signedIn = authStatus === 'signed-in'
  const [statusMsg, setStatusMsg] = useState<string | null>(null)
  const statusTimerRef = useRef<number | null>(null)
  const [editing, setEditing] = useState<Partial<CalendarEvent> | null>(null)
  const [conflictPrompt, setConflictPrompt] = useState<{
    draft: Omit<CalendarEvent, 'id'> & { id?: string }
    blockers: CalendarEvent[]
    suggestion: FreeSlot | null
  } | null>(null)
  const [deletePrompt, setDeletePrompt] = useState<Partial<CalendarEvent> | null>(null)
  const [showSettings, setShowSettings] = useState(false)
  const [settingsQuery, setSettingsQuery] = useState('')
  const [menuOpen, setMenuOpen] = useState(false)
  const [hideConflictBanner, setHideConflictBanner] = useState(false)
  const [launchPrompt, setLaunchPrompt] = useState<LaunchPrompt | null>(null)
  const installedVersionRef = useRef('0.17.3')

  const [syncInfo, setSyncInfo] = useState(getSyncStatus())
  const [syncing, setSyncing] = useState(false)
  const [dropActive, setDropActive] = useState(false)

  const visibleIds = useMemo(
    () => new Set(calendars.filter((c) => c.visible).map((c) => c.id)),
    [calendars],
  )
  const displayCalendars = useMemo(() => {
    if (settings.useGoogleCalendar) return calendars
    return calendars.filter((c) => c.source !== 'google')
  }, [calendars, settings.useGoogleCalendar])

  const visibleEvents = useMemo(() => {
    const primaryIds = new Set(
      calendars
        .filter((c) => c.source === 'google' && (c.logicalId === 'google:primary' || c.id === 'primary'))
        .map((c) => c.id),
    )
    primaryIds.add('primary')
    const filtered = events.filter((e) => {
      const cal = calendars.find((c) => c.id === e.calendarId)
      if (!settings.useGoogleCalendar && (e.source === 'google' || cal?.source === 'google')) {
        return false
      }
      const onPrimary =
        e.source === 'google' &&
        (primaryIds.has(e.calendarId) || cal?.logicalId === 'google:primary')
      const onVisibleCalendar = visibleIds.has(e.calendarId) || onPrimary
      if (!onVisibleCalendar) return false
      if (settings.showContactBirthdays) return true
      if (
        isBirthdayCalendarEntry({
          id: e.calendarId,
          displayName: cal?.displayName,
          logicalId: cal?.logicalId,
        })
      ) {
        return false
      }
      if (isContactBirthdayEvent(e)) return false
      return true
    })
    const from = new Date()
    from.setDate(from.getDate() - 7)
    from.setHours(0, 0, 0, 0)
    const to = new Date()
    to.setDate(to.getDate() + Math.max(settings.agendaRangeDays ?? 30, 42))
    return expandRecurringEvents(filtered, from, to)
  }, [events, visibleIds, calendars, settings.showContactBirthdays, settings.useGoogleCalendar, settings.agendaRangeDays])
  const displayEvents = useMemo(() => {
    const q = eventQuery.trim().toLowerCase()
    if (!q) return visibleEvents
    return visibleEvents.filter((e) =>
      [e.title, e.location, e.description].some((s) => s?.toLowerCase().includes(q)),
    )
  }, [visibleEvents, eventQuery])
  const conflicts = useMemo(() => detectConflicts(visibleEvents), [visibleEvents])
  const crossConflicts = useMemo(() => crossSourceConflicts(visibleEvents), [visibleEvents])
  const conflictDates = useMemo(() => uniqueConflictDates(conflicts), [conflicts])
  const nextConflictJump = useMemo(
    () => peekNextConflictDate(conflictDates, jumpDate),
    [conflictDates, jumpDate],
  )
  const jumpScrollTime = useMemo(() => {
    if (view === 'agenda' || !jumpDate) return undefined
    return earliestConflictTimeOnDate(conflicts, jumpDate)
  }, [conflicts, jumpDate, focusSeq, view])

  useEffect(() => {
    setHideConflictBanner(false)
  }, [conflictDates.join('|')])

  const flash = useCallback((msg: string) => {
    setStatusMsg(msg)
    if (statusTimerRef.current != null) window.clearTimeout(statusTimerRef.current)
    statusTimerRef.current = window.setTimeout(() => {
      setStatusMsg(null)
      statusTimerRef.current = null
    }, 4000)
  }, [])

  useEffect(() => {
    const crash = continuumLogger.consumeLastCrash()
    if (crash) {
      flash('Recovered from a previous error — see Settings → Download error log')
      continuumLogger.error('Startup crash notice', crash)
    }
  }, [flash])

  useEffect(() => {
    let cancelled = false
    void (async () => {
      let version = '0.17.3'
      try {
        if (isTauri()) {
          const { getVersion } = await import('@tauri-apps/api/app')
          version = await getVersion()
        }
      } catch {
        /* Vite fallback */
      }
      installedVersionRef.current = version
      const prompt = await decideLaunchPrompt(version)
      if (!cancelled) setLaunchPrompt(prompt)
    })()
    return () => {
      cancelled = true
    }
  }, [])

  const applySettings = useCallback(
    (next: ContinuumSettings, toast?: string) => {
      setSettings(next)
      if (next.themeMode !== mode) setMode(next.themeMode)
      setCalendars((prev) => applyBirthdayCalendarVisibility(prev, next.showContactBirthdays))
      if (toast) flash(toast)
    },
    [flash, mode, setMode],
  )

  const hydrateSettingsFromDrive = useCallback(async (opts?: { force?: boolean }) => {
    try {
      const { settings: remote, action } = await reconcilePeerSettings({
        force: opts?.force !== false,
      })
      setSettings((prev) => {
        if (action === 'noop') {
          // Avoid identity churn that restarts sync loops / re-renders.
          const same =
            prev.showContactBirthdays === remote.showContactBirthdays &&
            prev.themeMode === remote.themeMode &&
            prev.useGoogleCalendar === remote.useGoogleCalendar &&
            prev.use24HourFormat === remote.use24HourFormat
          if (same) return prev
        }
        if (prev.showContactBirthdays !== remote.showContactBirthdays) {
          flash(
            remote.showContactBirthdays
              ? 'Settings synced: showing Google birthdays'
              : 'Settings synced: Google automated birthdays hidden',
          )
        } else if (action === 'seed') {
          flash('Continuum settings published for Android')
        } else if (action === 'push-pending') {
          flash('Local Continuum settings pushed to peer remote')
        }
        return remote
      })
      if (remote.themeMode !== mode) setMode(remote.themeMode)
      setCalendars((prev) => applyBirthdayCalendarVisibility(prev, remote.showContactBirthdays))
    } catch (e) {
      if (isTransientPeerError(e)) {
        // Rate-limited warning already emitted by peerSyncControl.
        return
      }
      continuumLogger.error('Peer settings reconcile failed', e)
      flash(e instanceof Error ? e.message : 'Settings sync failed')
    }
  }, [flash, mode, setMode])

  const persistSettings = useCallback(
    async (patch: Partial<ContinuumSettings>, toast?: string) => {
      const merged = { ...settings, ...patch }
      applySettings(merged, toast)
      try {
        if (signedIn) {
          const env = await pushSettingsPatch(patch)
          applySettings(env.settings)
        } else {
          markPendingPeerPush()
          localStorage.setItem(
            'continuum.settings.envelope',
            JSON.stringify({
              schemaVersion: 1,
              revision: Date.now(),
              updatedAt: new Date().toISOString(),
              updatedBy: { platform: 'desktop', deviceId: 'local', appVersion: '0.1.0' },
              contentHash: '',
              settings: merged,
            }),
          )
          localStorage.setItem('continuum.settings.appliedRev', String(Date.now()))
        }
      } catch (e) {
        markPendingPeerPush()
        flash(e instanceof Error ? e.message : 'Settings sync failed')
      }
    },
    [applySettings, flash, settings, signedIn],
  )

  const onToggleNotifications = useCallback(
    async (enabled: boolean) => {
      if (!enabled) {
        clearScheduledReminders()
        await persistSettings({ notificationEnabled: false })
        flash('Notifications off')
        return
      }
      const ok = await ensurePermission()
      if (!ok) {
        flash('Notification permission denied or unavailable')
        return
      }
      await persistSettings({ notificationEnabled: true }, 'Notifications enabled')
      const logicalIdByCalendarId = Object.fromEntries(calendars.map((c) => [c.id, c.logicalId]))
      void rescheduleReminders(
        visibleEvents,
        { ...settings, notificationEnabled: true },
        logicalIdByCalendarId,
      )
    },
    [calendars, flash, persistSettings, settings, visibleEvents],
  )

  const runLocalEventsPeer = useCallback(async (opts?: { force?: boolean; quiet?: boolean }) => {
    try {
      const result = await reconcileLocalEventsPeer({ force: opts?.force })
      // Backoff skip returns current store + noop — still safe to apply.
      setCalendars(applyBirthdayCalendarVisibility(result.calendars, settings.showContactBirthdays))
      setEvents(result.events)
      if (opts?.quiet) return
      if (result.action === 'pull') flash('Local events synced from peer')
      else if (result.action === 'seed' || result.action === 'push-pending') {
        flash('Local events published for peer devices')
      }
    } catch (e) {
      if (isTransientPeerError(e)) return
      continuumLogger.error('Local events peer reconcile failed', e)
      flash(humanizeOAuthFailure(e))
    }
  }, [flash, settings.showContactBirthdays])

  const runMultiSync = useCallback(async () => {
    const errors: string[] = []
    if (settings.useGoogleCalendar) {
      const result = await syncAllVisibleGoogleCalendars()
      errors.push(...result.errors)
      if (result.errors.length) flash(`Partial sync: ${result.errors[0]}`)
      else {
        const googleCount = result.events.filter((e) => e.source === 'google').length
        flash(`Google sync · ${googleCount} events`)
      }
    } else {
      setSyncInfo(getSyncStatus())
    }
    await runLocalEventsPeer({ quiet: true })
    const overlay = await refreshDesktopOverlays()
    errors.push(...overlay.errors)
    setCalendars(applyBirthdayCalendarVisibility(overlay.calendars, settings.showContactBirthdays))
    setEvents(overlay.events.length ? overlay.events : ensureSeededEvents())
    setSyncInfo(getSyncStatus())
    if (overlay.errors.length) flash(`Calendar overlay: ${overlay.errors[0]}`)
    return { calendars: overlay.calendars, events: overlay.events, errors }
  }, [flash, settings.showContactBirthdays, settings.useGoogleCalendar, runLocalEventsPeer])

  const onRefresh = useCallback(async () => {
    setSyncing(true)
    try {
      resetPeerSyncBackoff()
      await hydrateSettingsFromDrive({ force: true })
      await runMultiSync()
    } catch (e) {
      continuumLogger.error('Manual refresh failed', e)
      flash(humanizeOAuthFailure(e))
      setSyncInfo(getSyncStatus())
    } finally {
      setSyncing(false)
    }
  }, [flash, hydrateSettingsFromDrive, runMultiSync])

  useEffect(() => {
    void (async () => {
      if (loadNeedsReauth()) {
        setAuthStatus('needs-reauth')
        return
      }
      const stored = await getStoredTokens()
      if (!stored) {
        setAuthStatus('signed-out')
        return
      }
      const fresh = await ensureFreshTokens()
      setAuthStatus(authStatusFromTokens(Boolean(fresh)))
    })()
    return onAuthExpired(() => {
      setAuthStatus('needs-reauth')
      flash(
        'Google sign-in expired. Your events are still saved on this PC. Sign in again to sync.',
      )
    })
  }, [flash])

  useEffect(() => {
    const cb = parseOAuthCallback(window.location.href)
    if (cb) {
      void exchangeCodeForTokens(cb.code, cb.state)
        .then(async () => {
          clearNeedsReauth()
          setAuthStatus('signed-in')
          window.history.replaceState({}, '', '/')
          flash('Signed in with Google')
          await hydrateSettingsFromDrive()
          await runMultiSync()
        })
        .catch((e) => {
          continuumLogger.error('OAuth callback failed', e)
          const msg = humanizeOAuthFailure(e)
          flash(msg)
          window.alert(msg)
        })
    }
  }, [flash, runMultiSync, hydrateSettingsFromDrive])

  // Mouse/browser Back closes the event editor and returns to the calendar view.
  const eventEditorOpen = editing !== null
  useEffect(() => {
    if (!eventEditorOpen) return

    let closedByPop = false
    window.history.pushState({ continuumEventEditor: true }, '')

    const onPopState = () => {
      closedByPop = true
      setEditing(null)
    }
    // WebView mouse Back (XButton1) — route through history so cleanup stays consistent.
    const onAuxClick = (e: MouseEvent) => {
      if (e.button !== 3) return
      e.preventDefault()
      window.history.back()
    }

    window.addEventListener('popstate', onPopState)
    window.addEventListener('auxclick', onAuxClick)
    return () => {
      window.removeEventListener('popstate', onPopState)
      window.removeEventListener('auxclick', onAuxClick)
      if (
        !closedByPop &&
        window.history.state &&
        (window.history.state as { continuumEventEditor?: boolean }).continuumEventEditor
      ) {
        window.history.back()
      }
    }
  }, [eventEditorOpen])

  const settingsRef = useRef(settings)
  settingsRef.current = settings
  const applySettingsRef = useRef(applySettings)
  applySettingsRef.current = applySettings
  const runMultiSyncRef = useRef(runMultiSync)
  runMultiSyncRef.current = runMultiSync
  const hydrateSettingsRef = useRef(hydrateSettingsFromDrive)
  hydrateSettingsRef.current = hydrateSettingsFromDrive
  const runLocalEventsPeerRef = useRef(runLocalEventsPeer)
  runLocalEventsPeerRef.current = runLocalEventsPeer

  useEffect(() => {
    if (!signedIn) return
    void hydrateSettingsRef.current({ force: true })
    void runLocalEventsPeerRef.current({ force: true, quiet: true })
    const stopSync = startFocusSyncLoop(() => {
      void runMultiSyncRef.current().catch(() => setSyncInfo(getSyncStatus()))
    })
    const stopSettings = startSettingsPollLoop((remote, meta) => {
      applySettingsRef.current(remote, `Updated from ${meta.from}: settings revision applied`)
    })
    const stopLocalEvents = startLocalEventsPollLoop((result) => {
      const s = settingsRef.current
      setCalendars(applyBirthdayCalendarVisibility(result.calendars, s.showContactBirthdays))
      setEvents(result.events)
      if (result.action === 'pull') {
        const owned = result.events.filter((e) => e.source === 'local' || e.source === 'ics_import')
        const logicalIdByCalendarId = Object.fromEntries(
          result.calendars.map((c) => [c.id, c.logicalId]),
        )
        void notifyNewPeerEventsFromPull({
          previousIds: knownPeerEventKeysRef.current,
          events: owned,
          settings: s,
          logicalIdByCalendarId,
          skipDeviceId: getDeviceId(),
          envelopeDeviceId: result.updatedByDeviceId,
        }).finally(() => {
          knownPeerEventKeysRef.current = new Set(owned.map((e) => `${e.calendarId}:${e.id}`))
        })
      } else {
        const owned = result.events.filter((e) => e.source === 'local' || e.source === 'ics_import')
        knownPeerEventKeysRef.current = new Set(owned.map((e) => `${e.calendarId}:${e.id}`))
      }
    })
    return () => {
      stopSync()
      stopSettings()
      stopLocalEvents()
    }
  }, [signedIn])

  useEffect(() => {
    if (settings.useGoogleCalendar) return
    const prevEv = loadEvents()
    const prevCal = loadCalendars()
    const nextEv = prevEv.filter((e) => e.source !== 'google')
    const nextCal = prevCal.filter((c) => c.source !== 'google')
    if (nextEv.length !== prevEv.length || nextCal.length !== prevCal.length) {
      saveEvents(nextEv)
      saveCalendars(nextCal)
      setEvents(nextEv)
      setCalendars(applyBirthdayCalendarVisibility(nextCal, settings.showContactBirthdays))
    }
    if (settings.defaultWriteCalendarId.startsWith('google:')) {
      void persistSettings({ defaultWriteCalendarId: 'local:local-default' })
    }
    // Intentionally omit persistSettings from deps — one-shot remap when privacy turns on.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [settings.useGoogleCalendar, settings.showContactBirthdays, settings.defaultWriteCalendarId])

  useEffect(() => {
    const logicalIdByCalendarId = Object.fromEntries(calendars.map((c) => [c.id, c.logicalId]))
    void rescheduleReminders(visibleEvents, settings, logicalIdByCalendarId)
  }, [visibleEvents, settings, calendars])

  useEffect(() => {
    if (settings.themeMode !== mode) setMode(settings.themeMode)
  }, [settings.themeMode, mode, setMode])

  const applyIcsText = useCallback(
    (text: string) => {
      try {
        const { events: next, count } = importIcsText(text)
        setEvents(next)
        setCalendars(loadCalendars())
        noteLocalEventsChanged()
        if (signedIn) {
          void pushLocalEventsNow().catch((e) => continuumLogger.error('Local events push failed', e))
        }
        flash(`Imported ${count} events`)
      } catch (e) {
        continuumLogger.error('ICS import failed', e)
        flash(e instanceof Error ? e.message : 'ICS import failed')
      }
    },
    [flash, signedIn],
  )

  useEffect(() => {
    const onDragOver = (e: DragEvent) => {
      if (![...e.dataTransfer?.types ?? []].includes('Files')) return
      e.preventDefault()
      setDropActive(true)
    }
    const onDragLeave = () => setDropActive(false)
    const onDrop = (e: DragEvent) => {
      setDropActive(false)
      e.preventDefault()
      const file = e.dataTransfer?.files?.[0]
      if (!file || !looksLikeIcsFileName(file.name)) {
        flash('Drop an .ics calendar file')
        return
      }
      void file.text().then(applyIcsText)
    }
    window.addEventListener('dragover', onDragOver)
    window.addEventListener('dragleave', onDragLeave)
    window.addEventListener('drop', onDrop)
    return () => {
      window.removeEventListener('dragover', onDragOver)
      window.removeEventListener('dragleave', onDragLeave)
      window.removeEventListener('drop', onDrop)
    }
  }, [applyIcsText, flash])

  useEffect(() => {
    if (!isTauri()) return
    let unlistenOpen: (() => void) | undefined
    let unlistenUrl: (() => void) | undefined
    void (async () => {
      try {
        const pending = await invoke<string[]>('take_pending_open_paths')
        for (const path of pending) {
          if (path.startsWith('webcal') || path.startsWith('http')) {
            const { events: next, count } = await importIcsFromUrl(path)
            setEvents(next)
            setCalendars(loadCalendars())
            flash(`Imported ${count} events from link`)
          } else {
            const text = await invoke<string>('read_text_file_limited', { path, maxBytes: 2_000_000 })
            applyIcsText(text)
          }
        }
      } catch {
        /* command may be unavailable in pure web */
      }
      try {
        unlistenOpen = await listen<string>('open-ics-path', (ev) => {
          void invoke<string>('read_text_file_limited', { path: ev.payload, maxBytes: 2_000_000 })
            .then(applyIcsText)
            .catch((e) => flash(e instanceof Error ? e.message : 'Could not open file'))
        })
        unlistenUrl = await listen<string>('open-calendar-url', (ev) => {
          void importIcsFromUrl(ev.payload)
            .then(({ events: next, count }) => {
              setEvents(next)
              setCalendars(loadCalendars())
              flash(`Imported ${count} events from link`)
            })
            .catch((e) => {
              continuumLogger.error('Calendar URL open failed', e)
              flash(e instanceof Error ? e.message : 'Could not open calendar link')
            })
        })
      } catch {
        /* ignore */
      }
    })()
    return () => {
      unlistenOpen?.()
      unlistenUrl?.()
    }
  }, [applyIcsText, flash])

  const rangeStart = useMemo(() => startOfDay(new Date()), [])
  const rangeEnd = useMemo(() => addDays(rangeStart, 7), [rangeStart])

  async function onSignIn() {
    if (!isGoogleConfigured()) {
      window.alert(
        [
          'Sign in with Google lets YOU connect YOUR calendar — Continuum never collects passwords.',
          '',
          'Google requires Continuum (the app) to ship a public Client ID. That is a one-time product setup by Continuum maintainers, not something each user creates.',
          '',
          'This build does not have Continuum’s Client ID embedded yet.',
          'Set VITE_GOOGLE_CLIENT_ID in apps/desktop/.env (see docs/GOOGLE_API_SETUP.md), then restart the app.',
          '',
          'On Android you can sync today without that: Settings → Continuum → Connect Google calendars',
          '(uses the Google account already on your phone).',
        ].join('\n'),
      )
      return
    }
    try {
      flash('Opening Google sign-in in your browser…')
      const result = await signInWithGoogle()
      if (result === 'pending-redirect') return
      clearNeedsReauth()
      setAuthStatus('signed-in')
      flash('Signed in with Google')
      await hydrateSettingsFromDrive()
      await runMultiSync()
    } catch (e) {
      continuumLogger.error('Google sign-in failed', e)
      const msg = humanizeOAuthFailure(e)
      flash(msg)
      window.alert(msg)
    }
  }

  async function onSignOut() {
    await signOutGoogle()
    clearNeedsReauth()
    setAuthStatus('signed-out')
    flash('Signed out')
  }

  async function onCopyFreeSlots() {
    try {
      await copyFreeSlotsToClipboard(visibleEvents, rangeStart, rangeEnd, {
        dayStartHour: Number(settings.workingHours.start.split(':')[0]),
        dayEndHour: Number(settings.workingHours.end.split(':')[0]),
        minMinutes: settings.slotMinMinutes,
        travelBufferMinutes: settings.travelBufferMinutes,
        use24HourFormat: settings.use24HourFormat,
      })
      flash('Free slots copied')
    } catch (e) {
      continuumLogger.error('Copy free slots failed', e)
      flash(e instanceof Error ? e.message : 'Copy failed')
    }
  }

  function onProposeTimes() {
    const slots = proposeMeetingTimes(visibleEvents, {
      durationMinutes: 30,
      count: 5,
      workingHours: settings.workingHours,
      travelBufferMinutes: settings.travelBufferMinutes,
    })
    const timeOpts: Intl.DateTimeFormatOptions = {
      hour: settings.use24HourFormat ? '2-digit' : 'numeric',
      minute: '2-digit',
      hour12: !settings.use24HourFormat,
    }
    const text = [
      'Proposed times',
      ...slots.map(
        (s) =>
          `• ${s.start.toLocaleString(undefined, { ...timeOpts, weekday: 'short', month: 'short', day: 'numeric' })} – ${s.end.toLocaleTimeString(undefined, timeOpts)}`,
      ),
    ].join('\n')
    void navigator.clipboard.writeText(text)
    flash('Proposed times copied')
  }

  function onToggleCalendar(id: string, visible: boolean) {
    const next = calendars.map((c) => (c.id === id ? { ...c, visible } : c))
    setCalendars(next)
    saveCalendars(next)
    void persistSettings({
      visibleCalendarIds: next.filter((c) => c.visible).map((c) => c.logicalId),
    })
  }

  function toLocalDateTimeValue(d: Date): string {
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
  }

  function formatConflictSlot(slot: FreeSlot): string {
    const timeOpts: Intl.DateTimeFormatOptions = {
      hour: settings.use24HourFormat ? '2-digit' : 'numeric',
      minute: '2-digit',
      hour12: !settings.use24HourFormat,
    }
    return `${slot.start.toLocaleString(undefined, {
      ...timeOpts,
      weekday: 'short',
      month: 'short',
      day: 'numeric',
    })} – ${slot.end.toLocaleTimeString(undefined, timeOpts)}`
  }

  async function onSaveEvent(
    draft: Omit<CalendarEvent, 'id'> & { id?: string },
    scope?: RecurrenceEditScope,
    occurrenceStart?: string,
  ) {
    const master = draft.id ? events.find((e) => e.id === seriesEventId(draft.id ?? '')) : undefined
    if (master?.recurrence?.length && scope && occurrenceStart) {
      const next = applyRecurrenceEdit(scope, master, occurrenceStart, {
        ...master,
        ...draft,
        id: master.id,
      })
      const saved = upsertEvents(next)
      setEvents(saved)
      setEditing(null)
      const wroteLocal = next.some((e) => e.source === 'local' || e.source === 'ics_import' || !e.source)
      if (wroteLocal) noteLocalEventsChanged()
      if (signedIn && wroteLocal) {
        void pushLocalEventsNow()
          .then(() => flash('Event saved · synced to peers'))
          .catch((e) => {
            continuumLogger.error('Local events push failed', e)
            flash(humanizeOAuthFailure(e))
          })
        return
      }
      flash('Event saved')
      return
    }
    const blockers = conflictsForEvent(draft, visibleEvents)
    if (blockers.length) {
      const suggestion = suggestConflictFreeTime(draft, visibleEvents, {
        workingHours: settings.workingHours,
        travelBufferMinutes: settings.travelBufferMinutes,
        days: 14,
      })
      setConflictPrompt({ draft, blockers, suggestion })
      return
    }
    await commitSaveEvent(draft)
  }

  async function commitSaveEvent(draft: Omit<CalendarEvent, 'id'> & { id?: string }) {
    const cal = calendars.find((c) => c.id === draft.calendarId)
    const writeGoogle =
      settings.useGoogleCalendar &&
      signedIn &&
      (draft.source === 'google' || cal?.source === 'google' || draft.calendarId === 'primary')
    let saved: CalendarEvent
    if (writeGoogle) {
      try {
        const { source: _source, ...forGoogle } = draft
        if (draft.id) {
          saved = await updateGoogleEvent({
            ...forGoogle,
            id: draft.id,
            calendarId: draft.calendarId || 'primary',
            title: draft.title,
            start: draft.start,
            end: draft.end,
            source: 'google',
          })
        } else {
          saved = await createGoogleEvent(draft.calendarId || 'primary', {
            ...forGoogle,
            calendarId: draft.calendarId || 'primary',
            title: draft.title,
            start: draft.start,
            end: draft.end,
          })
        }
      } catch (e) {
        continuumLogger.error('Google save event failed', e)
        saved = {
          ...draft,
          id: draft.id ?? `local-${Date.now()}`,
          source: 'local',
          updated: new Date().toISOString(),
        } as CalendarEvent
        flash(e instanceof Error ? e.message : 'Google save failed — saved locally')
      }
    } else {
      saved = {
        ...draft,
        id: draft.id ?? `local-${Date.now()}`,
        source: draft.source === 'ics_import' ? 'ics_import' : 'local',
        start: toPeerIso(draft.start, draft.allDay),
        end: toPeerIso(draft.end, draft.allDay),
        updated: new Date().toISOString(),
      } as CalendarEvent
    }
    if (saved.source === 'local' || saved.source === 'ics_import' || !saved.source) {
      saved = {
        ...saved,
        start: toPeerIso(saved.start, saved.allDay),
        end: toPeerIso(saved.end, saved.allDay),
        updated: saved.updated ?? new Date().toISOString(),
        source: saved.source ?? 'local',
      }
    }
    const next = upsertEvents([saved])
    setEvents(next)
    setEditing(null)
    if (saved.source === 'local' || saved.source === 'ics_import' || !saved.source) {
      noteLocalEventsChanged()
      if (signedIn) {
        void pushLocalEventsNow()
          .then(() => flash('Event saved · synced to peers'))
          .catch(async (e) => {
            if (settings.useGoogleCalendar && isInsufficientDriveScope(e)) {
              try {
                const gcal =
                  calendars.find((c) => c.logicalId === 'google:primary')?.id ??
                  (cal?.source === 'google' ? cal.id : 'primary')
                const published = await createGoogleEventCopy(gcal, saved)
                deleteLocalEvent(saved.calendarId, saved.id, saved.source)
                setEvents(upsertEvents([published]))
                flash('Event saved to Google Calendar — pull calendars on your phone')
                return
              } catch (pubErr) {
                continuumLogger.error('Google Calendar publish after Drive 403 failed', pubErr)
              }
            }
            continuumLogger.error('Local events push failed', e)
            flash(humanizeOAuthFailure(e))
          })
        return
      }
    }
    flash('Event saved')
  }

  function requestDeleteEvent(ev: Partial<CalendarEvent>) {
    if (!ev.id) {
      flash('Cannot delete — event has no id')
      return
    }
    setDeletePrompt(ev)
  }

  async function confirmDeleteEvent(ev: Partial<CalendarEvent>) {
    const id = ev.id
    if (!id) return
    const calendarId =
      ev.calendarId ||
      events.find((e) => e.id === id)?.calendarId ||
      calendars.find((c) => c.logicalId === 'google:primary')?.id
    if (!calendarId) {
      flash('Cannot delete — missing calendar')
      setDeletePrompt(null)
      return
    }
    const source = ev.source ?? events.find((e) => e.id === id)?.source
    const cal = calendars.find((c) => c.id === calendarId)
    const isGoogle =
      settings.useGoogleCalendar &&
      signedIn &&
      (source === 'google' || cal?.source === 'google')
    const wasLocal = source === 'local' || source === 'ics_import' || cal?.source === 'local'

    // Optimistic: remove locally and return to agenda immediately (Tauri has no reliable window.confirm).
    const next = deleteLocalEvent(calendarId, id, source)
    setEvents(next)
    setEditing(null)
    setDeletePrompt(null)
    if (wasLocal) {
      recordLocalEventTombstone(calendarId, id)
      if (signedIn) {
        void pushLocalEventsNow().catch((e) => continuumLogger.error('Local events push failed', e))
      }
    }
    flash('Event deleted')

    if (isGoogle) {
      try {
        await deleteGoogleEvent(calendarId, id)
      } catch (e) {
        continuumLogger.error('Google delete failed after local remove', e)
        flash(e instanceof Error ? e.message : 'Removed locally — Google delete failed')
      }
    }
  }

  function goToday() {
    jumpCalendarTo(localDateKey())
  }

  function jumpCalendarTo(dateKey: string) {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(dateKey)) return
    setEditing(null)
    setShowSettings(false)
    setConflictPrompt(null)
    const today = localDateKey()
    const agendaEnd = (() => {
      const d = new Date(`${today}T12:00:00`)
      d.setDate(d.getDate() + Math.max(1, settings.agendaRangeDays ?? 30))
      return localDateKey(d.getTime())
    })()
    if (view === 'agenda' && (dateKey < today || dateKey > agendaEnd)) {
      setView('rolling')
    }
    setJumpDate(dateKey)
    setFocusSeq((n) => n + 1)
  }

  function jumpToOverlap(pairs: typeof conflicts) {
    const dates = uniqueConflictDates(pairs)
    if (!dates.length) return
    const idx = dates.indexOf(jumpDate)
    jumpCalendarTo(dates[(idx + 1) % dates.length] ?? dates[0]!)
  }

  function formatConflictJumpDay(dateKey: string): string {
    const d = new Date(`${dateKey}T12:00:00`)
    return d.toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' })
  }

  function openNewEvent(partial: Partial<CalendarEvent> = {}) {
    const defaults = newEventDefaults(displayCalendars, settings.defaultWriteCalendarId)
    setEditing({
      title: '',
      ...partial,
      calendarId: partial.calendarId ?? defaults.calendarId,
      source: partial.source ?? defaults.source,
      reminders: partial.reminders ?? [{ minutes: settings.defaultReminderMinutes, method: 'popup' }],
    })
  }

  function onImportIcs(file: File) {
    void file.text().then(applyIcsText)
  }

  async function onOpenCalendarLink() {
    const raw = window.prompt('Paste a webcal:// or https://…ics calendar link')
    if (!raw?.trim()) return
    try {
      const { events: next, count } = await importIcsFromUrl(raw.trim())
      setEvents(next)
      setCalendars(loadCalendars())
      flash(`Imported ${count} events from link`)
    } catch (e) {
      continuumLogger.error('Calendar link import failed', e)
      flash(e instanceof Error ? e.message : 'Link import failed')
    }
  }

  async function onSubscribeIcs() {
    const raw = window.prompt('Subscribe to a webcal:// or https://…ics URL (refreshes on each sync)')
    if (!raw?.trim()) return
    try {
      const { events: next, count } = await subscribeIcsUrl(raw.trim())
      setEvents(next)
      setCalendars(loadCalendars())
      flash(`Subscribed · ${count} events (will refresh on sync)`)
    } catch (e) {
      continuumLogger.error('ICS subscribe failed', e)
      flash(e instanceof Error ? e.message : 'Subscribe failed')
    }
  }

  async function onAddCalDav() {
    const serverUrl = window.prompt('CalDAV server URL', 'https://example.com/remote.php/dav/')
    if (!serverUrl) return
    const username = window.prompt('Username') ?? ''
    const password = window.prompt('App password') ?? ''
    const account: CalDavAccount = {
      id: `acc-${Date.now()}`,
      displayName: 'CalDAV',
      serverUrl,
      username,
      password,
    }
    try {
      const discovered = await discoverCalDavCalendars(account)
      const accounts = [...loadCalDavAccounts(), account]
      saveCalDavAccounts(accounts)
      const nextCals = [...calendars, ...discovered]
      setCalendars(nextCals)
      saveCalendars(nextCals)
      const synced = await syncCalDavEvents(account)
      setEvents(synced)
      flash(`CalDAV account added · ${synced.filter((e) => e.source === 'caldav').length} events`)
    } catch (e) {
      flash(e instanceof Error ? e.message : 'CalDAV failed')
    }
  }

  function jumpToNextFree() {
    const slots = proposeMeetingTimes(visibleEvents, {
      durationMinutes: settings.slotMinMinutes,
      count: 1,
      workingHours: settings.workingHours,
      travelBufferMinutes: settings.travelBufferMinutes,
    })
    if (!slots[0]) {
      flash('No free block found')
      return
    }
    openNewEvent({
      start: slots[0].start.toISOString().slice(0, 16),
      end: slots[0].end.toISOString().slice(0, 16),
    })
  }

  useDesktopHotkeys({
    enabled: !editing,
    onNew: () => openNewEvent(),
    onToday: goToday,
    onView: setView,
    onSearch: () => searchRef.current?.focus(),
    onJump: () => {
      const raw = window.prompt('Jump to date (YYYY-MM-DD)', jumpDate)
      if (raw && /^\d{4}-\d{2}-\d{2}$/.test(raw)) jumpCalendarTo(raw)
    },
  })

  useEffect(() => {
    const native = toNativeArgs(windowBehavior)
    void invoke('set_window_behavior', native).catch(() => undefined)
  }, [windowBehavior])

  useEffect(() => {
    void readStartWithWindows().then(setStartWithWindows)
  }, [])

  useEffect(() => {
    localStorage.setItem('continuum.holidayPack', holidayPack)
    const next = mergeHolidayEvents(loadEvents(), holidayPack)
    const prev = loadEvents()
    if (JSON.stringify(prev.filter((e) => e.source === 'holidays')) === JSON.stringify(next.filter((e) => e.source === 'holidays'))) {
      return
    }
    saveEvents(next)
    setEvents(next)
  }, [holidayPack])

  useEffect(() => {
    let timer = 0
    let cancelled = false
    const tick = async () => {
      const n = remainingTodayCount(visibleEvents)
      let png: number[] = []
      let overlay: number[] = []
      try {
        png = await renderBadgePng(n)
        overlay = renderOverlayPng(n)
      } catch {
        png = []
        overlay = []
      }
      if (cancelled) return
      try {
        await invoke('set_day_badge', {
          count: n,
          png,
          overlay,
          summary: n ? `${n} remaining today` : 'No remaining events today',
        })
      } catch {
        /* tray badge is best-effort */
      }
      const wait = Math.min(60_000, Math.max(5_000, nextRemainingTickMs(visibleEvents) - Date.now()))
      timer = window.setTimeout(() => void tick(), wait)
    }
    void tick()
    return () => {
      cancelled = true
      window.clearTimeout(timer)
    }
  }, [visibleEvents])

  useEffect(() => {
    let stop: (() => void) | undefined
    void listen('tray-new-event', () => openNewEvent())
      .then((fn) => {
        stop = fn
      })
      .catch(() => undefined)
    return () => stop?.()
  }, [])

  return (
    <div
      className={`relative flex h-full flex-col gap-2 p-4 pb-10 ${dropActive ? 'ring-2 ring-[var(--cc-accent)] ring-inset' : ''}`}
    >
      <ContinuumSplash />
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <AppTitle />
          <p className="text-sm text-[var(--cc-muted)]">
            Agenda · Multi-calendar ·{' '}
            {syncInfo.lastSyncedAt
              ? `Synced ${new Date(syncInfo.lastSyncedAt).toLocaleTimeString()}`
              : 'Not synced'}
            {conflicts.length ? (
              <>
                {' · '}
                <button
                  type="button"
                  className="underline decoration-amber-500 underline-offset-2 hover:text-amber-800 dark:hover:text-amber-200"
                  aria-label={`Jump to overlapping events on ${nextConflictJump ?? conflictDates[0] ?? 'that day'}`}
                  onClick={() => jumpToOverlap(conflicts)}
                >
                  {conflicts.length} overlap{conflicts.length === 1 ? '' : 's'} — jump
                </button>
              </>
            ) : null}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            className="rounded border border-[var(--cc-border)] px-2 py-1 text-sm disabled:opacity-50"
            disabled={syncing}
            title="Refresh calendars and settings"
            aria-label="Refresh calendars and settings"
            onClick={() => void onRefresh()}
          >
            {syncing ? 'Refreshing…' : 'Refresh'}
          </button>
          <div className="relative">
            <button
              type="button"
              className="rounded border border-[var(--cc-border)] px-2 py-1 text-sm"
              aria-label="Menu"
              aria-haspopup="true"
              aria-expanded={menuOpen}
              onClick={() => setMenuOpen((o) => !o)}
            >
              Menu
            </button>
            {menuOpen ? (
              <div className="absolute right-0 z-20 mt-1 w-56 rounded-lg border border-[var(--cc-border)] bg-[var(--cc-surface)] py-1 shadow-lg">
                <button
                  type="button"
                  className="block w-full px-3 py-1.5 text-left text-sm hover:bg-[var(--cc-accent-soft)]"
                  onClick={() => {
                    setMenuOpen(false)
                    void onCopyFreeSlots()
                  }}
                >
                  Copy free slots
                </button>
                <button
                  type="button"
                  className="block w-full px-3 py-1.5 text-left text-sm hover:bg-[var(--cc-accent-soft)]"
                  onClick={() => {
                    setMenuOpen(false)
                    onProposeTimes()
                  }}
                >
                  Propose times
                </button>
                <button
                  type="button"
                  className="block w-full px-3 py-1.5 text-left text-sm hover:bg-[var(--cc-accent-soft)]"
                  onClick={() => {
                    setMenuOpen(false)
                    jumpToNextFree()
                  }}
                >
                  Jump to next free block
                </button>
                <button
                  type="button"
                  className="block w-full px-3 py-1.5 text-left text-sm hover:bg-[var(--cc-accent-soft)]"
                  onClick={() => {
                    setMenuOpen(false)
                    void onOpenCalendarLink()
                  }}
                >
                    Open calendar link…
                </button>
                <button
                  type="button"
                  className="block w-full px-3 py-1.5 text-left text-sm hover:bg-[var(--cc-accent-soft)]"
                  onClick={() => {
                    setMenuOpen(false)
                    void onSubscribeIcs()
                  }}
                >
                  Subscribe to ICS URL…
                </button>
                <button
                  type="button"
                  className="block w-full px-3 py-1.5 text-left text-sm hover:bg-[var(--cc-accent-soft)]"
                  onClick={() => {
                    setMenuOpen(false)
                    void openExternal(VENMO_DONATE_URL)
                  }}
                >
                  Donate via Venmo
                </button>
              </div>
            ) : null}
          </div>
          {authStatus === 'signed-in' ? (
            <button
              type="button"
              className="rounded border border-[var(--cc-border)] px-2 py-1 text-sm"
              aria-label="Sign out of Google"
              onClick={() => void onSignOut()}
            >
              Sign out
            </button>
          ) : (
            <button
              type="button"
              className="rounded bg-[var(--cc-accent)] px-2 py-1 text-sm text-white"
              aria-label={authStatus === 'needs-reauth' ? 'Sign in again' : 'Sign in with Google'}
              title={
                isGoogleConfigured()
                  ? 'Connect your Google Calendar, Contacts, and Tasks'
                  : 'Continuum Sign in with Google (your account — Continuum never collects passwords)'
              }
              onClick={() => void onSignIn()}
            >
              {authStatus === 'needs-reauth' ? 'Sign in again' : 'Sign in with Google'}
            </button>
          )}
          <button
            type="button"
            className="rounded border border-[var(--cc-border)] px-2 py-1 text-sm"
            aria-label="Settings"
            aria-pressed={showSettings}
            onClick={() => setShowSettings((s) => !s)}
          >
            Settings
          </button>
        </div>
      </header>
      {authStatus === 'needs-reauth' ? <AuthReconnectBanner onSignIn={() => void onSignIn()} /> : null}
      <CalendarToolbar
        view={view}
        onView={setView}
        query={eventQuery}
        onQuery={setEventQuery}
        jumpDate={jumpDate}
        onJumpDate={jumpCalendarTo}
        onToday={goToday}
        searchRef={searchRef}
      />

      <div className="flex min-h-0 flex-1 gap-5">
        <CalendarSidebar
          calendars={displayCalendars}
          defaultWriteCalendarId={settings.defaultWriteCalendarId}
          onToggle={onToggleCalendar}
          onSetDefaultWrite={(logicalId) => void persistSettings({ defaultWriteCalendarId: logicalId })}
          calendarNotifyPrefs={settings.calendarNotifyPrefs ?? {}}
          onNotifyPrefsChange={(logicalId, prefs: CalendarNotifyPrefs) => {
            void persistSettings({
              calendarNotifyPrefs: {
                ...(settings.calendarNotifyPrefs ?? {}),
                [logicalId]: prefs,
              },
            })
          }}
        />

        <div className="flex min-h-0 min-w-0 flex-1 flex-col border-l border-[var(--cc-border)] pl-5">
          <main className="flex min-h-0 min-w-0 flex-1 flex-col">
            {conflicts.length > 0 && !hideConflictBanner ? (
              <div
                className="mb-2 flex items-start justify-between gap-2 rounded-lg border border-amber-400 bg-amber-50 px-3 py-2 text-sm text-amber-950 dark:bg-amber-950/40 dark:text-amber-100"
                role="status"
              >
                <button
                  type="button"
                  className="min-w-0 flex-1 text-left underline decoration-amber-600 underline-offset-2 hover:decoration-2"
                  aria-label={
                    nextConflictJump
                      ? `Jump to overlapping events on ${nextConflictJump}`
                      : 'Jump to overlapping events'
                  }
                  onClick={() => jumpToOverlap(conflicts)}
                >
                  <span className="font-medium">
                    {conflicts.length} overlapping event{conflicts.length === 1 ? '' : 's'}
                  </span>
                  {nextConflictJump ? (
                    <span>
                      {' '}
                      on {formatConflictJumpDay(nextConflictJump)} — click to jump, then reschedule or
                      dismiss.
                    </span>
                  ) : (
                    <span> — click to jump to the conflict day.</span>
                  )}
                  {crossConflicts.length > 0 ? (
                    <span className="mt-0.5 block text-xs opacity-90">
                      {crossConflicts.length} involve local/peer vs Google
                      {crossConflicts[0] ? ` (${formatConflictSources(crossConflicts[0])})` : ''}
                    </span>
                  ) : null}
                </button>
                <button
                  type="button"
                  className="shrink-0 rounded px-2 py-0.5 text-xs underline"
                  aria-label="Dismiss overlap notice"
                  onClick={() => setHideConflictBanner(true)}
                >
                  Dismiss
                </button>
              </div>
            ) : null}
            {editing ? (
              <EventEditor
                initial={editing}
                calendars={displayCalendars}
                defaultCalendarId={
                  newEventDefaults(displayCalendars, settings.defaultWriteCalendarId).calendarId
                }
                defaultReminderMinutes={settings.defaultReminderMinutes}
                googleSignedIn={signedIn && settings.useGoogleCalendar}
                onCancel={() => setEditing(null)}
                onSave={(e, scope, occ) => void onSaveEvent(e, scope, occ)}
                onDelete={editing.id ? () => requestDeleteEvent(editing) : undefined}
              />
            ) : view === 'agenda' ? (
              <AgendaView
                events={displayEvents}
                calendars={displayCalendars}
                focusDate={jumpDate}
                focusSeq={focusSeq}
                showEmptyDays={settings.showEmptyDaysInAgenda}
                rangeDays={settings.agendaRangeDays}
                redactTitles={settings.redactTitlesInScreenshots}
                density={settings.agendaDensity}
                use24HourFormat={settings.use24HourFormat}
                workingHours={settings.workingHours}
                conflictIds={new Set(conflicts.flatMap((c) => [eventOccurrenceKey(c.a), eventOccurrenceKey(c.b)]))}
                onSelectEvent={(ev) => {
                  const master = events.find((e) => e.id === seriesEventId(ev.id)) ?? ev
                  setEditing({ ...master, occurrenceStart: ev.start })
                }}
                onOpenDay={(dateKey) => {
                  const startHm = (settings.workingHours.start || '09:00').slice(0, 5)
                  const [hh, mm] = startHm.split(':').map((x) => Number(x))
                  const start = new Date(`${dateKey}T12:00:00`)
                  start.setHours(Number.isFinite(hh) ? hh : 9, Number.isFinite(mm) ? mm : 0, 0, 0)
                  const end = new Date(start.getTime() + 60 * 60 * 1000)
                  const pad = (n: number) => String(n).padStart(2, '0')
                  const toLocal = (d: Date) =>
                    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
                  openNewEvent({ start: toLocal(start), end: toLocal(end), allDay: false })
                }}
              />
            ) : (
              <RollingWeekView
                events={displayEvents}
                calendarView={
                  view === 'month' ? 'dayGridMonth' : view === 'year' ? 'multiMonthYear' : 'rollingWeek'
                }
                focusDate={jumpDate}
                focusSeq={focusSeq}
                focusScrollTime={jumpScrollTime}
                calendars={displayCalendars}
                rollingWeekFromToday={settings.rollingWeekFromToday}
                redactTitles={settings.redactTitlesInScreenshots}
                use24HourFormat
                firstDayOfWeek={settings.firstDayOfWeek}
                weeklyViewDays={settings.weeklyViewDays}
                conflictIds={new Set(conflicts.flatMap((c) => [eventOccurrenceKey(c.a), eventOccurrenceKey(c.b)]))}
                onSelectEvent={(ev) => {
                  const master = events.find((e) => e.id === seriesEventId(ev.id)) ?? ev
                  setEditing({ ...master, occurrenceStart: ev.start })
                }}
                onSelectSlot={(start, end) =>
                  openNewEvent({
                    start: start.toISOString().slice(0, 16),
                    end: end.toISOString().slice(0, 16),
                  })
                }
              />
            )}
          </main>
        </div>

        {showSettings ? (
          <SettingsPanel
            form={{
              query: settingsQuery,
              onQuery: setSettingsQuery,
              settings,
              persistSettings,
              applySettings,
              authStatus,
              signedIn,
              resolvedTheme: resolved,
              lastSyncedAt: syncInfo.lastSyncedAt,
              lastSyncError: syncInfo.lastError,
              onSignIn: () => void onSignIn(),
              windowBehavior,
              setWindowBehavior,
              startWithWindows,
              setStartWithWindows,
              holidayPack,
              setHolidayPack,
              calendars,
              displayCalendars,
              setCalendars,
              visibleEvents,
              setEvents,
              flash,
              onToggleNotifications,
              onImportIcs,
              onOpenCalendarLink: () => void onOpenCalendarLink(),
              onSubscribeIcs: () => void onSubscribeIcs(),
              onAddCalDav: () => void onAddCalDav(),
            }}
          />
        ) : null}
      </div>

      {!editing ? (
        <button
          type="button"
          className="cc-fab absolute bottom-12 right-6 z-30 flex h-14 w-14 items-center justify-center rounded-full bg-[var(--cc-accent)] text-2xl font-light text-white shadow-lg"
          aria-label="New event"
          title={hotkeyTitle('New event', 'N')}
          onClick={() => openNewEvent()}
        >
          +
        </button>
      ) : null}

      {launchPrompt?.kind === 'update' ? (
        <UpdateAvailableDialog
          version={launchPrompt.version}
          onLater={() => {
            markUpdateChecked(Date.now(), launchPrompt.version)
            setLaunchPrompt(null)
          }}
          onInstall={() => {
            markUpdateChecked(Date.now(), launchPrompt.version)
            void openExternal(launchPrompt.url)
            setLaunchPrompt(null)
          }}
        />
      ) : null}

      {launchPrompt?.kind === 'donate' ? (
        <DonateNudgeDialog
          onLater={() => {
            markVersionSeen(installedVersionRef.current)
            setLaunchPrompt(null)
          }}
          onDonate={() => {
            markVersionSeen(installedVersionRef.current)
            void openExternal(VENMO_DONATE_URL)
            setLaunchPrompt(null)
          }}
        />
      ) : null}

      {deletePrompt ? (
        <div
          className="absolute inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
          role="alertdialog"
          aria-modal="true"
          aria-labelledby="cc-delete-title"
        >
          <div className="w-full max-w-md space-y-3 rounded-xl border border-[var(--cc-border)] bg-[var(--cc-surface)] p-4 shadow-xl">
            <h2 id="cc-delete-title" className="text-base font-semibold text-[var(--cc-text)]">
              Delete event?
            </h2>
            <p className="text-sm text-[var(--cc-muted)]">
              Remove{' '}
              <strong className="text-[var(--cc-text)]">
                {deletePrompt.title?.trim() || '(No title)'}
              </strong>
              ? This cannot be undone from Continuum.
            </p>
            <div className="flex flex-wrap justify-end gap-2 pt-1">
              <button
                type="button"
                className="rounded px-3 py-1.5 text-sm"
                onClick={() => setDeletePrompt(null)}
              >
                Cancel
              </button>
              <button
                type="button"
                className="rounded bg-red-600 px-3 py-1.5 text-sm font-medium text-white"
                onClick={() => void confirmDeleteEvent(deletePrompt)}
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {conflictPrompt ? (
        <div
          className="absolute inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
          role="alertdialog"
          aria-modal="true"
          aria-labelledby="cc-conflict-title"
        >
          <div className="w-full max-w-md space-y-3 rounded-xl border border-amber-400 bg-[var(--cc-surface)] p-4 shadow-xl">
            <h2 id="cc-conflict-title" className="flex items-center gap-2 text-base font-semibold text-amber-700">
              <span aria-hidden className="text-xl">
                ⚠️
              </span>
              Scheduling conflict
            </h2>
            <p className="text-sm text-[var(--cc-text)]">
              This overlaps{' '}
              <strong>
                {conflictPrompt.blockers
                  .slice(0, 3)
                  .map((e) => `${e.title || '(No title)'} (${e.source ?? 'local'})`)
                  .join(', ')}
              </strong>
              {conflictPrompt.blockers.length > 3
                ? ` and ${conflictPrompt.blockers.length - 3} more`
                : ''}
              .
            </p>
            {conflictPrompt.suggestion ? (
              <p className="rounded-md bg-amber-50 px-3 py-2 text-sm text-amber-950 dark:bg-amber-950/40 dark:text-amber-100">
                Suggested free time (within work hours):{' '}
                <strong>{formatConflictSlot(conflictPrompt.suggestion)}</strong>
              </p>
            ) : (
              <p className="text-sm text-[var(--cc-muted)]">
                No free work-hours slot found in the next two weeks for this duration.
              </p>
            )}
            <div className="flex flex-wrap justify-end gap-2 pt-1">
              <button
                type="button"
                className="rounded border border-[var(--cc-border)] px-3 py-1.5 text-sm"
                onClick={() => {
                  jumpCalendarTo(localDateKey(new Date(conflictPrompt.draft.start).getTime()))
                }}
              >
                View on calendar
              </button>
              <button
                type="button"
                className="rounded px-3 py-1.5 text-sm"
                onClick={() => setConflictPrompt(null)}
              >
                Cancel
              </button>
              <button
                type="button"
                className="rounded border border-amber-500 px-3 py-1.5 text-sm text-amber-800"
                onClick={() => {
                  const draft = conflictPrompt.draft
                  setConflictPrompt(null)
                  void commitSaveEvent(draft)
                }}
              >
                Save anyway
              </button>
              {conflictPrompt.suggestion ? (
                <button
                  type="button"
                  className="rounded bg-amber-500 px-3 py-1.5 text-sm font-medium text-white"
                  onClick={() => {
                    const { draft, suggestion } = conflictPrompt
                    if (!suggestion) return
                    setConflictPrompt(null)
                    void commitSaveEvent({
                      ...draft,
                      start: toLocalDateTimeValue(suggestion.start),
                      end: toLocalDateTimeValue(suggestion.end),
                      allDay: false,
                    })
                  }}
                >
                  Use suggested time
                </button>
              ) : null}
            </div>
          </div>
        </div>
      ) : null}

      <footer
        className="pointer-events-none absolute inset-x-0 bottom-0 z-20 border-t border-[var(--cc-border)] bg-[var(--cc-surface)]/95 px-4 py-1.5 backdrop-blur-sm"
        role="status"
        aria-live="polite"
      >
        <p
          className={`truncate text-xs ${statusMsg ? 'text-[var(--cc-accent)]' : 'text-[var(--cc-muted)]'}`}
        >
          {statusMsg ??
            (syncInfo.lastSyncedAt
              ? `Last sync ${new Date(syncInfo.lastSyncedAt).toLocaleTimeString()}`
              : 'Ready')}
        </p>
      </footer>
    </div>
  )
}

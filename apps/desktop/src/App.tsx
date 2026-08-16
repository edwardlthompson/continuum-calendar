import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  defaultContinuumSettings,
  conflictsForEvent,
  detectConflicts,
  eventOccurrenceKey,
  isBirthdayCalendarEntry,
  isContactBirthdayEvent,
  proposeMeetingTimes,
  suggestConflictFreeTime,
  type CalendarEvent,
  type CalendarNotifyPrefs,
  type ContinuumSettings,
  type FreeSlot,
  type ThemeMode,
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
  getStoredTokens,
  humanizeOAuthFailure,
  isGoogleConfigured,
  parseOAuthCallback,
  signInWithGoogle,
  signOutGoogle,
} from './auth/googleAuth'
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
  exportSettingsJson,
  importSettingsJson,
  reconcilePeerSettings,
  getSettingsSyncError,
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
import { downloadIcsFile } from './services/ics'
import { importIcsFromUrl, importIcsText, looksLikeIcsFileName } from './services/icsImport'
import { discoverCalDavCalendars, loadCalDavAccounts, saveCalDavAccounts, type CalDavAccount } from './services/caldav'
import { createGoogleEvent, deleteGoogleEvent, updateGoogleEvent } from './services/googleCalendar'
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
import { listen } from '@tauri-apps/api/event'
import { invoke } from '@tauri-apps/api/core'
type MainView = 'rolling' | 'agenda'

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
  const [signedIn, setSignedIn] = useState(false)
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

  const settingsMatch = useCallback((...labels: string[]) => {
    const q = settingsQuery.trim().toLowerCase()
    if (!q) return true
    return labels.some((l) => l.toLowerCase().includes(q))
  }, [settingsQuery])
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
    return events.filter((e) => {
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
  }, [events, visibleIds, calendars, settings.showContactBirthdays, settings.useGoogleCalendar])
  const conflicts = useMemo(() => detectConflicts(visibleEvents), [visibleEvents])

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
    }
  }, [flash, settings.showContactBirthdays])

  const runMultiSync = useCallback(async () => {
    if (!settings.useGoogleCalendar) {
      setSyncInfo(getSyncStatus())
      await runLocalEventsPeer({ quiet: true })
      return { calendars: loadCalendars(), events: loadEvents(), errors: [] as string[] }
    }
    const result = await syncAllVisibleGoogleCalendars()
    const base = result.calendars.length ? result.calendars : loadCalendars()
    setCalendars(applyBirthdayCalendarVisibility(base, settings.showContactBirthdays))
    setEvents(result.events.length ? result.events : ensureSeededEvents())
    setSyncInfo(getSyncStatus())
    if (result.errors.length) {
      flash(`Partial sync: ${result.errors[0]}`)
    } else {
      const googleCount = result.events.filter((e) => e.source === 'google').length
      flash(`Google sync · ${googleCount} events`)
    }
    await runLocalEventsPeer({ quiet: true })
    return result
  }, [flash, settings.showContactBirthdays, settings.useGoogleCalendar, runLocalEventsPeer])

  const onRefresh = useCallback(async () => {
    setSyncing(true)
    try {
      resetPeerSyncBackoff()
      await hydrateSettingsFromDrive({ force: true })
      await runMultiSync()
    } catch (e) {
      continuumLogger.error('Manual refresh failed', e)
      flash(e instanceof Error ? e.message : 'Refresh failed')
      setSyncInfo(getSyncStatus())
    } finally {
      setSyncing(false)
    }
  }, [flash, hydrateSettingsFromDrive, runMultiSync])

  useEffect(() => {
    void getStoredTokens().then((t) => setSignedIn(Boolean(t)))
    const cb = parseOAuthCallback(window.location.href)
    if (cb) {
      void exchangeCodeForTokens(cb.code, cb.state)
        .then(async () => {
          setSignedIn(true)
          window.history.replaceState({}, '', '/')
          flash('Signed in with Google')
          await hydrateSettingsFromDrive()
          await runMultiSync()
        })
        .catch((e) => {
          continuumLogger.error('OAuth callback failed', e)
          flash(e instanceof Error ? e.message : 'Sign-in failed')
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
      const openAudience = window.confirm(
        [
          'Google will warn that Continuum is unverified. That is expected.',
          '',
          'If Continue then shows “An unknown error has occurred”, Google is blocking this account — Continuum never gets a login code.',
          '',
          'Add this exact Gmail as a Test user first (Google Cloud → Audience), wait one minute, then sign in again.',
          '',
          'OK = open the Test users page now. Cancel = I already added this account, continue sign-in.',
        ].join('\n'),
      )
      if (openAudience) {
        window.open('https://console.cloud.google.com/auth/audience', '_blank', 'noopener,noreferrer')
        flash('Add your Gmail as a Test user, then click Sign in again')
        return
      }
      flash('Opening Google sign-in…')
      const result = await signInWithGoogle()
      if (result === 'pending-redirect') return
      setSignedIn(true)
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
    setSignedIn(false)
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

  async function onSaveEvent(draft: Omit<CalendarEvent, 'id'> & { id?: string }) {
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
      (draft.source === 'google' || cal?.source === 'google')
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
          .catch((e) => {
            continuumLogger.error('Local events push failed', e)
            flash(e instanceof Error ? e.message : 'Saved locally — peer push failed')
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
      flash('CalDAV account added')
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
            {conflicts.length ? ` · ${conflicts.length} conflict(s)` : ''}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            className={`rounded px-2 py-1 text-sm ${view === 'agenda' ? 'bg-[var(--cc-accent)] text-white' : 'border border-[var(--cc-border)]'}`}
            onClick={() => setView('agenda')}
          >
            Agenda (event list)
          </button>
          <button
            type="button"
            className={`rounded px-2 py-1 text-sm ${view === 'rolling' ? 'bg-[var(--cc-accent)] text-white' : 'border border-[var(--cc-border)]'}`}
            onClick={() => setView('rolling')}
          >
            Rolling week
          </button>
          <button
            type="button"
            className="rounded border border-[var(--cc-border)] px-2 py-1 text-sm disabled:opacity-50"
            disabled={syncing}
            title="Refresh calendars and settings"
            onClick={() => void onRefresh()}
          >
            {syncing ? 'Refreshing…' : 'Refresh'}
          </button>
          <div className="relative">
            <button
              type="button"
              className="rounded border border-[var(--cc-border)] px-2 py-1 text-sm"
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
              </div>
            ) : null}
          </div>
          {signedIn ? (
            <button
              type="button"
              className="rounded border border-[var(--cc-border)] px-2 py-1 text-sm"
              onClick={() => void onSignOut()}
            >
              Sign out
            </button>
          ) : (
            <button
              type="button"
              className="rounded bg-[var(--cc-accent)] px-2 py-1 text-sm text-white"
              title={
                isGoogleConfigured()
                  ? 'Connect your Google Calendar, Contacts, and Tasks'
                  : 'Continuum Sign in with Google (your account — Continuum never collects passwords)'
              }
              onClick={() => void onSignIn()}
            >
              Sign in with Google
            </button>
          )}
          <button
            type="button"
            className="rounded border border-[var(--cc-border)] px-2 py-1 text-sm"
            onClick={() => setShowSettings((s) => !s)}
          >
            Settings
          </button>
        </div>
      </header>

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
          <main className="min-h-0 min-w-0 flex-1">
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
                onSave={(e) => void onSaveEvent(e)}
                onDelete={editing.id ? () => requestDeleteEvent(editing) : undefined}
              />
            ) : view === 'agenda' ? (
              <AgendaView
                events={visibleEvents}
                calendars={displayCalendars}
                showEmptyDays={settings.showEmptyDaysInAgenda}
                rangeDays={settings.agendaRangeDays}
                redactTitles={settings.redactTitlesInScreenshots}
                density={settings.agendaDensity}
                use24HourFormat={settings.use24HourFormat}
                workingHours={settings.workingHours}
                conflictIds={new Set(conflicts.flatMap((c) => [eventOccurrenceKey(c.a), eventOccurrenceKey(c.b)]))}
                onSelectEvent={(ev) => setEditing(ev)}
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
                events={visibleEvents}
                calendars={displayCalendars}
                rollingWeekFromToday={settings.rollingWeekFromToday}
                redactTitles={settings.redactTitlesInScreenshots}
                use24HourFormat
                firstDayOfWeek={settings.firstDayOfWeek}
                weeklyViewDays={settings.weeklyViewDays}
                conflictIds={new Set(conflicts.flatMap((c) => [eventOccurrenceKey(c.a), eventOccurrenceKey(c.b)]))}
                onSelectEvent={(ev) => setEditing(ev)}
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
          <aside className="w-72 shrink-0 space-y-3 overflow-auto rounded-xl border border-[var(--cc-border)] bg-[var(--cc-surface)] p-3 text-sm">
            <h2 className="font-semibold">Continuum</h2>
            <input
              type="search"
              placeholder="Search settings…"
              value={settingsQuery}
              onChange={(e) => setSettingsQuery(e.target.value)}
              className="w-full rounded border border-[var(--cc-border)] bg-transparent px-2 py-1 text-sm"
              aria-label="Search settings"
            />
            <p className="text-xs text-[var(--cc-muted)]">
              {signedIn ? 'Signed in with Google' : 'Not signed in'}
              {syncInfo.lastSyncedAt
                ? ` · last sync ${new Date(syncInfo.lastSyncedAt).toLocaleTimeString()}`
                : ''}
            </p>
            {getSettingsSyncError() ? (
              <p className="text-xs text-red-500">Settings sync: {getSettingsSyncError()}</p>
            ) : signedIn ? (
              <p className="text-xs text-[var(--cc-muted)]">
                Peer remote: Continuum settings sync both ways with Android (Drive App Data)
              </p>
            ) : (
              <p className="text-xs text-[var(--cc-muted)]">
                Sign in to publish/pull Continuum settings with Android
              </p>
            )}
            {!signedIn ? (
              <button
                type="button"
                className="w-full rounded bg-[var(--cc-accent)] px-2 py-1 text-white"
                onClick={() => void onSignIn()}
              >
                Sign in with Google
              </button>
            ) : null}
            {settingsMatch('Theme', 'appearance', 'dark', 'light') ? (
            <label className="flex items-center justify-between gap-2">
              Theme
              <select
                value={settings.themeMode}
                onChange={(e) => void persistSettings({ themeMode: e.target.value as ThemeMode })}
                className="rounded border border-[var(--cc-border)] bg-transparent px-1"
              >
                <option value="light">Light</option>
                <option value="dark">Dark</option>
                <option value="system">System ({resolved})</option>
              </select>
            </label>
            ) : null}
            {settingsMatch('Use Google Calendar', 'Google', 'privacy') ? (
            <label
              className="flex items-center justify-between gap-2"
              title="When off, Continuum uses local calendars only (still peer-syncs via Drive App Data). Google Calendar sync is skipped."
            >
              Use Google Calendar
              <input
                type="checkbox"
                checked={settings.useGoogleCalendar}
                onChange={(e) => void persistSettings({ useGoogleCalendar: e.target.checked })}
              />
            </label>
            ) : null}
            {settingsMatch('24-hour time', 'time', 'clock') ? (
            <label className="flex items-center justify-between gap-2">
              24-hour time
              <input
                type="checkbox"
                checked={settings.use24HourFormat}
                onChange={(e) => void persistSettings({ use24HourFormat: e.target.checked })}
              />
            </label>
            ) : null}
            {settingsMatch('First day of week', 'week') ? (
            <label className="flex items-center justify-between gap-2">
              First day of week
              <select
                value={settings.firstDayOfWeek}
                onChange={(e) =>
                  void persistSettings({ firstDayOfWeek: Number(e.target.value) || 0 })
                }
                className="rounded border border-[var(--cc-border)] bg-transparent px-1"
              >
                <option value={0}>Sunday</option>
                <option value={1}>Monday</option>
                <option value={2}>Tuesday</option>
                <option value={3}>Wednesday</option>
                <option value={4}>Thursday</option>
                <option value={5}>Friday</option>
                <option value={6}>Saturday</option>
              </select>
            </label>
            ) : null}
            {settingsMatch('Weekly view days', 'week') ? (
            <label className="flex items-center justify-between gap-2">
              Weekly view days
              <input
                type="number"
                min={1}
                max={14}
                className="w-16 rounded border border-[var(--cc-border)] bg-transparent px-1"
                value={settings.weeklyViewDays}
                onChange={(e) =>
                  void persistSettings({
                    weeklyViewDays: Math.min(14, Math.max(1, Number(e.target.value) || 7)),
                  })
                }
              />
            </label>
            ) : null}
            {settingsMatch('Default snooze', 'snooze') ? (
            <label className="flex items-center justify-between gap-2">
              Default snooze (min)
              <input
                type="number"
                min={1}
                className="w-16 rounded border border-[var(--cc-border)] bg-transparent px-1"
                value={settings.defaultSnoozeMinutes}
                onChange={(e) =>
                  void persistSettings({
                    defaultSnoozeMinutes: Math.max(1, Number(e.target.value) || 10),
                  })
                }
              />
            </label>
            ) : null}
            {settingsMatch('Default calendar', 'calendar') ? (
            <label className="flex flex-col gap-1">
              Default calendar for new events
              <select
                value={settings.defaultWriteCalendarId}
                onChange={(e) => void persistSettings({ defaultWriteCalendarId: e.target.value })}
                className="rounded border border-[var(--cc-border)] bg-transparent px-1 py-1"
              >
                {displayCalendars
                  .filter((c) => c.writable !== false && c.source !== 'holidays')
                  .map((c) => (
                    <option key={c.logicalId} value={c.logicalId}>
                      {c.displayName} ({c.source})
                    </option>
                  ))}
              </select>
            </label>
            ) : null}
            {settingsMatch('Show empty days', 'agenda', 'open') ? (
            <label className="flex items-center justify-between gap-2">
              Show empty days in agenda
              <input
                type="checkbox"
                checked={settings.showEmptyDaysInAgenda}
                onChange={(e) => void persistSettings({ showEmptyDaysInAgenda: e.target.checked })}
              />
            </label>
            ) : null}
            {settingsMatch('birthdays', 'Google', 'contacts') ? (
            <label className="flex items-center justify-between gap-2" title="Hides Google Contacts automated birthday events only. Manual yearly birthday events stay.">
              Show Google automated birthdays
              <input
                type="checkbox"
                checked={settings.showContactBirthdays}
                onChange={(e) => {
                  const show = e.target.checked
                  const nextCals = calendars.map((c) =>
                    isBirthdayCalendarEntry(c) ? { ...c, visible: show } : c,
                  )
                  setCalendars(nextCals)
                  saveCalendars(nextCals)
                  void persistSettings({ showContactBirthdays: show })
                }}
              />
            </label>
            ) : null}
            {settingsMatch('Working hours', 'hours') ? (
            <label className="flex items-center justify-between gap-2">
              Working hours
              <span className="flex items-center gap-1">
                <input
                  type="time"
                  className="rounded border border-[var(--cc-border)] bg-transparent px-1"
                  value={settings.workingHours.start}
                  onChange={(e) =>
                    void persistSettings({
                      workingHours: { ...settings.workingHours, start: e.target.value || '09:00' },
                    })
                  }
                />
                –
                <input
                  type="time"
                  className="rounded border border-[var(--cc-border)] bg-transparent px-1"
                  value={settings.workingHours.end}
                  onChange={(e) =>
                    void persistSettings({
                      workingHours: { ...settings.workingHours, end: e.target.value || '17:00' },
                    })
                  }
                />
              </span>
            </label>
            ) : null}
            {settingsMatch('Travel buffer', 'travel') ? (
            <label className="flex items-center justify-between gap-2">
              Travel buffer (min)
              <input
                type="number"
                min={0}
                className="w-16 rounded border border-[var(--cc-border)] bg-transparent px-1"
                value={settings.travelBufferMinutes}
                onChange={(e) =>
                  void persistSettings({
                    travelBufferMinutes: Math.max(0, Number(e.target.value) || 0),
                  })
                }
              />
            </label>
            ) : null}
            {settingsMatch('Min free slot', 'slot') ? (
            <label className="flex items-center justify-between gap-2">
              Min free slot (min)
              <input
                type="number"
                min={1}
                className="w-16 rounded border border-[var(--cc-border)] bg-transparent px-1"
                value={settings.slotMinMinutes}
                onChange={(e) =>
                  void persistSettings({
                    slotMinMinutes: Math.max(1, Number(e.target.value) || 30),
                  })
                }
              />
            </label>
            ) : null}
            {settingsMatch('Agenda range', 'agenda') ? (
            <label className="flex items-center justify-between gap-2">
              Agenda range (days)
              <input
                type="number"
                min={1}
                max={90}
                className="w-16 rounded border border-[var(--cc-border)] bg-transparent px-1"
                value={settings.agendaRangeDays}
                onChange={(e) =>
                  void persistSettings({
                    agendaRangeDays: Math.min(90, Math.max(1, Number(e.target.value) || 30)),
                  })
                }
              />
            </label>
            ) : null}
            {settingsMatch('Agenda density', 'agenda', 'compact') ? (
            <label className="flex items-center justify-between gap-2">
              Agenda density
              <select
                value={settings.agendaDensity}
                onChange={(e) =>
                  void persistSettings({
                    agendaDensity: e.target.value as ContinuumSettings['agendaDensity'],
                  })
                }
                className="rounded border border-[var(--cc-border)] bg-transparent px-1"
              >
                <option value="comfortable">Comfortable</option>
                <option value="compact">Compact</option>
              </select>
            </label>
            ) : null}
            {settingsMatch('Default reminder', 'reminder') ? (
            <label className="flex items-center justify-between gap-2">
              Default reminder (min)
              <input
                type="number"
                min={0}
                className="w-16 rounded border border-[var(--cc-border)] bg-transparent px-1"
                value={settings.defaultReminderMinutes}
                onChange={(e) =>
                  void persistSettings({
                    defaultReminderMinutes: Math.max(0, Number(e.target.value) || 0),
                  })
                }
              />
            </label>
            ) : null}
            {settingsMatch('Rolling week', 'week') ? (
            <label className="flex items-center justify-between gap-2">
              Rolling week from today
              <input
                type="checkbox"
                checked={settings.rollingWeekFromToday}
                onChange={(e) => void persistSettings({ rollingWeekFromToday: e.target.checked })}
              />
            </label>
            ) : null}
            {settingsMatch('Notifications', 'notify', 'permission') ? (
              <label className="flex items-center justify-between gap-2">
                Notifications
                <input
                  type="checkbox"
                  checked={settings.notificationEnabled}
                  onChange={(e) => void onToggleNotifications(e.target.checked)}
                />
              </label>
            ) : null}
            {settingsMatch('Redact titles', 'screenshots', 'privacy') ? (
            <label className="flex items-center justify-between gap-2">
              Redact titles in screenshots
              <input
                type="checkbox"
                checked={settings.redactTitlesInScreenshots}
                onChange={(e) => void persistSettings({ redactTitlesInScreenshots: e.target.checked })}
              />
            </label>
            ) : null}
            {settingsMatch('Export', 'Import', 'ICS', 'CalDAV', 'settings', 'error log', 'Reset') ? (
            <div className="flex flex-col gap-1">
              <button
                type="button"
                className="rounded border border-[var(--cc-border)] px-2 py-1"
                onClick={() => void downloadIcsFile(visibleEvents)}
              >
                Export ICS
              </button>
              <label className="rounded border border-[var(--cc-border)] px-2 py-1 text-center">
                Import ICS
                <input
                  type="file"
                  accept=".ics,text/calendar"
                  className="hidden"
                  onChange={(e) => {
                    const f = e.target.files?.[0]
                    if (f) onImportIcs(f)
                  }}
                />
              </label>
              <button
                type="button"
                className="rounded border border-[var(--cc-border)] px-2 py-1"
                onClick={() => void onOpenCalendarLink()}
              >
                Open calendar link…
              </button>
              <button
                type="button"
                className="rounded border border-[var(--cc-border)] px-2 py-1"
                onClick={() => void onAddCalDav()}
              >
                Add CalDAV account
              </button>
              <button
                type="button"
                className="rounded border border-[var(--cc-border)] px-2 py-1"
                onClick={() => {
                  const blob = new Blob([exportSettingsJson()], { type: 'application/json' })
                  const url = URL.createObjectURL(blob)
                  const a = document.createElement('a')
                  a.href = url
                  a.download = 'continuum-settings.json'
                  a.click()
                }}
              >
                Export settings JSON
              </button>
              <label className="rounded border border-[var(--cc-border)] px-2 py-1 text-center">
                Import settings JSON
                <input
                  type="file"
                  accept="application/json"
                  className="hidden"
                  onChange={(e) => {
                    const f = e.target.files?.[0]
                    if (!f) return
                    void f.text().then((t) => {
                      try {
                        applySettings(importSettingsJson(t), 'Settings imported')
                      } catch (err) {
                        flash(err instanceof Error ? err.message : 'Import failed')
                      }
                    })
                  }}
                />
              </label>
              <button
                type="button"
                className="rounded border border-[var(--cc-border)] px-2 py-1"
                onClick={() => void persistSettings(defaultContinuumSettings(), 'Reset to Continuum defaults')}
              >
                Reset Continuum defaults
              </button>
              <button
                type="button"
                className="rounded border border-[var(--cc-border)] px-2 py-1"
                onClick={() => {
                  continuumLogger.downloadLog()
                  flash('Error log downloaded')
                }}
              >
                Download error log
              </button>
            </div>
            ) : null}
            {syncInfo.lastError ? <p className="text-xs text-red-500">{syncInfo.lastError}</p> : null}
          </aside>
        ) : null}
      </div>

      {!editing ? (
        <button
          type="button"
          className="cc-fab absolute bottom-12 right-6 z-30 flex h-14 w-14 items-center justify-center rounded-full bg-[var(--cc-accent)] text-2xl font-light text-white shadow-lg"
          aria-label="New event"
          onClick={() => openNewEvent()}
        >
          +
        </button>
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
                  .map((e) => e.title || '(No title)')
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

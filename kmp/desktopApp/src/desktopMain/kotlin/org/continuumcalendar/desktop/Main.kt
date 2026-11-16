package org.continuumcalendar.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.continuumcalendar.data.DesktopCrashCapture
import org.continuumcalendar.data.GitHubUpdateCheck
import org.continuumcalendar.data.UpdateCheckResult
import org.continuumcalendar.data.google.GoogleAuthRefresh
import org.continuumcalendar.data.google.GoogleCalendarFetch
import org.continuumcalendar.data.google.GoogleCalendarWrite
import org.continuumcalendar.data.google.GoogleDesktopSignIn
import org.continuumcalendar.data.google.PeerDriveOverlay
import org.continuumcalendar.data.google.GoogleTasksFetch
import org.continuumcalendar.data.settings.DesktopCalDavStore
import org.continuumcalendar.data.settings.continuumSettingsToJson
import org.continuumcalendar.data.settings.parseContinuumSettingsJson
import org.continuumcalendar.domain.holiday.mergeHolidayEvents
import org.continuumcalendar.domain.holiday.parseHolidayPackId
import org.continuumcalendar.domain.ics.exportEventsToIcs
import org.continuumcalendar.domain.ics.importEventsFromIcs
import org.continuumcalendar.domain.model.FreeSlot
import org.continuumcalendar.domain.schedule.formatProposedSlot
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.JFileChooser

import org.continuumcalendar.data.google.TauriTokenVault
import org.continuumcalendar.data.google.loadDesktopEnvCredentials
import org.continuumcalendar.data.maps.PhotonGeocode
import org.continuumcalendar.data.settings.DesktopAutostart
import org.continuumcalendar.data.settings.DesktopChromePrefs
import org.continuumcalendar.data.settings.DesktopChromeStore
import org.continuumcalendar.data.settings.DesktopPeerMetaStore
import org.continuumcalendar.data.settings.DesktopSettingsStore
import org.continuumcalendar.ui.maps.GeocodeHit
import org.jetbrains.skia.Image
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import org.continuumcalendar.domain.calendar.applyBirthdayCalendarVisibility
import org.continuumcalendar.domain.calendar.calendarsWithVisibility
import org.continuumcalendar.domain.calendar.filterContactBirthdays
import org.continuumcalendar.domain.calendar.toggleCalendarVisible
import org.continuumcalendar.domain.calendar.visibleEvents
import org.continuumcalendar.domain.event.newEventAtHalfHour
import org.continuumcalendar.domain.event.newEventAtNow
import org.continuumcalendar.domain.event.newEventOnOpenDay
import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.CalendarListEntry
import org.continuumcalendar.domain.model.ContinuumSettings
import org.continuumcalendar.domain.model.GoogleOAuthTokens
import org.continuumcalendar.domain.remind.dueReminders
import org.continuumcalendar.platform.DesktopTrayNotifier
import org.continuumcalendar.platform.SystemContinuumClock
import org.continuumcalendar.ui.demo.demoCalendars
import org.continuumcalendar.ui.demo.demoEvents
import org.continuumcalendar.ui.event.EventDraft
import org.continuumcalendar.ui.event.toEvent
import org.continuumcalendar.ui.settings.SettingsCategory
import org.continuumcalendar.ui.settings.WindowStartupUi
import org.continuumcalendar.ui.settings.CONTINUUM_VENMO_DONATE_URL
import org.continuumcalendar.ui.shell.ContinuumMainView
import org.continuumcalendar.ui.shell.ContinuumShell
import java.awt.Desktop
import java.io.File
import java.net.URI

@OptIn(FlowPreview::class)
fun main() = application {
    DesktopCrashCapture.installDefaultHandler()
    val initialChrome = DesktopChromeStore.load()
    val windowState = rememberWindowState(
        size = DpSize(initialChrome.windowWidth.dp, initialChrome.windowHeight.dp),
        position = WindowPosition.Absolute(
            x = initialChrome.windowX.dp,
            y = initialChrome.windowY.dp,
        ),
    )
    var chrome by remember { mutableStateOf(initialChrome) }
    var windowVisible by remember { mutableStateOf(true) }
    val notifier = remember {
        DesktopTrayNotifier(
            onActivate = { windowVisible = true },
        )
    }
    if (!windowVisible) {
        // Keep process alive in tray; click tray icon restores via onActivate.
        androidx.compose.runtime.SideEffect { notifier.ensureTray() }
    }
    if (windowVisible) {
    Window(
        onCloseRequest = {
            if (chrome.closeTo == "tray") {
                notifier.ensureTray()
                windowVisible = false
            } else {
                exitApplication()
            }
        },
        title = "Continuum Calendar",
        state = windowState,
    ) {
        val clock = remember { SystemContinuumClock() }
        val scope = rememberCoroutineScope()
        var settings by remember { mutableStateOf(DesktopSettingsStore.load()) }

        LaunchedEffect(windowState.isMinimized, chrome.minimizeTo) {
            if (windowState.isMinimized && chrome.minimizeTo == "tray") {
                notifier.ensureTray()
                windowState.isMinimized = false
                windowVisible = false
            }
        }

        LaunchedEffect(windowState) {
            snapshotFlow {
                val pos = windowState.position
                val size = windowState.size
                val x = if (pos is WindowPosition.Absolute) pos.x.value.toInt() else chrome.windowX
                val y = if (pos is WindowPosition.Absolute) pos.y.value.toInt() else chrome.windowY
                DesktopChromePrefs(
                    openAtStartup = chrome.openAtStartup,
                    minimizeTo = chrome.minimizeTo,
                    closeTo = chrome.closeTo,
                    windowX = x,
                    windowY = y,
                    windowWidth = size.width.value.toInt().coerceIn(640, 10000),
                    windowHeight = size.height.value.toInt().coerceIn(480, 10000),
                    settingsCategory = chrome.settingsCategory,
                )
            }
                .distinctUntilChanged()
                .debounce(400)
                .collect { next ->
                    chrome = next
                    withContext(Dispatchers.IO) { DesktopChromeStore.save(next) }
                }
        }
        var allEvents by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
        var allCalendars by remember { mutableStateOf<List<CalendarListEntry>>(emptyList()) }
        var tokens by remember { mutableStateOf<GoogleOAuthTokens?>(null) }
        var authStatus by remember { mutableStateOf("Starting…") }
        var statusMessage by remember { mutableStateOf("") }
        var startupHint by remember { mutableStateOf("") }
        var updateStatus by remember { mutableStateOf("") }
        var nowMs by remember { mutableStateOf(clock.nowEpochMs()) }
        val firedReminders = remember { mutableSetOf<String>() }
        val appVersion = "0.1.0"

        LaunchedEffect(Unit) {
            val crash = withContext(Dispatchers.IO) { DesktopCrashCapture.readPending() }
            if (crash != null) {
                statusMessage = "Last crash saved locally — use Settings → About → Send feedback"
            }
        }

        // Same workaround as shipping desktop: force birthday calendar visibility from the toggle,
        // then hide eventType=birthday (manual yearly birthdays stay).
        val displayCalendars = applyBirthdayCalendarVisibility(
            calendarsWithVisibility(allCalendars, settings),
            settings.showContactBirthdays,
        )
        val displayEvents = filterContactBirthdays(
            visibleEvents(allEvents, displayCalendars),
            displayCalendars,
            settings.showContactBirthdays,
        )

        fun persistSettings(next: ContinuumSettings) {
            val cals = applyBirthdayCalendarVisibility(allCalendars, next.showContactBirthdays)
            allCalendars = cals
            val synced = next.copy(
                visibleCalendarIds = cals.filter { it.visible }.map { it.logicalId },
            )
            settings = synced
            DesktopSettingsStore.save(synced)
            DesktopPeerMetaStore.markSettingsPending()
        }

        fun persistChrome(next: DesktopChromePrefs, hint: String = "") {
            chrome = next
            DesktopChromeStore.save(next)
            startupHint = hint
        }

        fun loadData(forceRefresh: Boolean = false) {
            scope.launch {
                statusMessage = if (forceRefresh) "Refreshing…" else "Loading…"
                nowMs = clock.nowEpochMs()
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val stored = TauriTokenVault.load()
                        if (stored == null || !settings.useGoogleCalendar) {
                            return@runCatching LoadResult(
                                demoEvents(nowMs),
                                demoCalendars(),
                                null,
                                if (stored == null) "Local demo (no Google vault)" else "Local demo (Google off)",
                            )
                        }
                        val (envId, envSecret) = resolveDesktopCredentials()
                        val withId = if (stored.clientId.isNullOrBlank() && !envId.isNullOrBlank()) {
                            stored.copy(clientId = envId)
                        } else {
                            stored
                        }
                        val fresh = GoogleAuthRefresh.ensureFresh(withId, envSecret)
                        val snap = GoogleCalendarFetch.syncVisibleRange(
                            fresh,
                            nowMs,
                            settings.agendaRangeDays,
                        )
                        val peer = PeerDriveOverlay.reconcile(
                            tokens = fresh,
                            settings = settings,
                            calendars = snap.calendars,
                            events = snap.events,
                            nowIso = java.time.Instant.ofEpochMilli(nowMs).toString(),
                        )
                        val year = java.time.Year.now().value
                        var events = mergeHolidayEvents(
                            peer.events,
                            parseHolidayPackId(peer.settings.holidayPack),
                            year,
                        )
                        var calendars = peer.calendars
                        if (peer.settings.showGoogleTasks && GoogleTasksFetch.hasTasksScope(fresh.scope)) {
                            val tasks = runCatching { GoogleTasksFetch.listDueTasks(fresh.accessToken) }.getOrDefault(emptyList())
                            if (tasks.isNotEmpty()) {
                                events = events.filterNot { it.calendarId == GoogleTasksFetch.TASKS_CALENDAR_ID } + tasks
                                if (calendars.none { it.id == GoogleTasksFetch.TASKS_CALENDAR_ID }) {
                                    calendars = calendars + GoogleTasksFetch.tasksCalendarEntry()
                                }
                            }
                        }
                        LoadResult(
                            events,
                            calendars,
                            fresh,
                            "Signed in with Google",
                            peer.settings,
                            peer.note,
                        )
                    }
                }
                result.fold(
                    onSuccess = { loaded ->
                        loaded.peerSettings?.let { peerSettings ->
                            if (peerSettings != settings) {
                                settings = peerSettings
                                DesktopSettingsStore.save(peerSettings)
                            }
                        }
                        allEvents = loaded.events
                        allCalendars = applyBirthdayCalendarVisibility(
                            loaded.calendars,
                            settings.showContactBirthdays,
                        )
                        tokens = loaded.tokens
                        authStatus = loaded.auth
                        if (settings.visibleCalendarIds.isEmpty() && loaded.calendars.isNotEmpty()) {
                            persistSettings(
                                settings.copy(
                                    visibleCalendarIds = allCalendars.filter { it.visible }.map { it.logicalId },
                                ),
                            )
                        }
                        val peerNote = loaded.peerNote?.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
                        statusMessage =
                            "${loaded.events.size} events · ${allCalendars.count { it.visible }} calendars$peerNote"
                    },
                    onFailure = { err ->
                        allEvents = demoEvents(nowMs)
                        allCalendars = demoCalendars()
                        tokens = null
                        val detail = err.message ?: "error"
                        authStatus = if (detail.contains("refresh", ignoreCase = true) || detail.contains("401") || detail.contains("403")) {
                            "Needs reauth"
                        } else {
                            "Local demo"
                        }
                        statusMessage = "Google sync failed — using demo ($detail)"
                    },
                )
            }
        }

        fun saveEvent(draft: EventDraft) {
            scope.launch {
                statusMessage = "Saving…"
                val outcome = withContext(Dispatchers.IO) {
                    runCatching {
                        val access = tokens?.let { GoogleAuthRefresh.ensureFresh(it, resolveDesktopCredentials().second) }
                        val event = draft.toEvent()
                        if (access != null) {
                            val saved = if (draft.id == null) {
                                GoogleCalendarWrite.create(access.accessToken, event)
                            } else {
                                GoogleCalendarWrite.update(access.accessToken, event)
                            }
                            tokens = access
                            saved
                        } else {
                            event
                        }
                    }
                }
                outcome.fold(
                    onSuccess = { saved ->
                        allEvents = allEvents.filterNot { it.id == saved.id } + saved
                        statusMessage = "Saved “${saved.title}”"
                    },
                    onFailure = { err ->
                        statusMessage = "Save failed: ${err.message ?: "error"}"
                    },
                )
            }
        }

        fun deleteEvent(draft: EventDraft) {
            val id = draft.id ?: return
            scope.launch {
                statusMessage = "Deleting…"
                val outcome = withContext(Dispatchers.IO) {
                    runCatching {
                        val access = tokens?.let { GoogleAuthRefresh.ensureFresh(it, resolveDesktopCredentials().second) }
                        if (access != null) {
                            GoogleCalendarWrite.delete(access.accessToken, draft.calendarId, id)
                            tokens = access
                        }
                    }
                }
                outcome.fold(
                    onSuccess = {
                        allEvents = allEvents.filterNot { it.id == id }
                        statusMessage = "Deleted"
                    },
                    onFailure = { err ->
                        statusMessage = "Delete failed: ${err.message ?: "error"}"
                    },
                )
            }
        }

        androidx.compose.runtime.LaunchedEffect(Unit) { loadData() }

        androidx.compose.runtime.LaunchedEffect(
            allEvents,
            allCalendars,
            settings.notificationEnabled,
            settings.defaultReminderMinutes,
            settings.visibleCalendarIds,
        ) {
            while (isActive) {
                nowMs = clock.nowEpochMs()
                val visible = visibleEvents(
                    allEvents,
                    calendarsWithVisibility(allCalendars, settings),
                )
                val due = dueReminders(
                    events = visible,
                    settings = settings,
                    nowMs = nowMs,
                    alreadyFired = firedReminders,
                )
                for (r in due) {
                    firedReminders += r.eventId
                    notifier.notify("Upcoming: ${r.title}", "Starts soon")
                    statusMessage = "Reminder: ${r.title}"
                }
                delay(30_000)
            }
        }

        ContinuumShell(
            events = displayEvents,
            calendars = displayCalendars,
            settings = settings,
            nowEpochMs = nowMs,
            authStatus = authStatus,
            statusMessage = statusMessage,
            initialView = ContinuumMainView.ROLLING,
            onSettingsChange = { persistSettings(it) },
            onToggleCalendar = { id, visible ->
                persistSettings(toggleCalendarVisible(settings, displayCalendars, id, visible))
            },
            onRefresh = { loadData(forceRefresh = true) },
            onCreateDraft = {
                val t = newEventAtNow(settings, displayCalendars, clock.nowEpochMs())
                EventDraft(calendarId = t.calendarId, start = t.startIso, end = t.endIso)
            },
            onOpenDayDraft = { dateKey ->
                val t = newEventOnOpenDay(settings, displayCalendars, dateKey)
                EventDraft(calendarId = t.calendarId, start = t.startIso, end = t.endIso)
            },
            onSlotDraft = { dateKey, hour, minute ->
                val t = newEventAtHalfHour(settings, displayCalendars, dateKey, hour, minute)
                EventDraft(calendarId = t.calendarId, start = t.startIso, end = t.endIso)
            },
            onSaveEvent = { saveEvent(it) },
            onDeleteEvent = { deleteEvent(it) },
            onSignOut = {
                scope.launch {
                    withContext(Dispatchers.IO) { TauriTokenVault.save(null) }
                    tokens = null
                    allEvents = demoEvents(clock.nowEpochMs())
                    allCalendars = demoCalendars()
                    authStatus = "Signed out — local demo"
                    statusMessage = "Google tokens cleared. Use Sign in with Google in Settings."
                }
            },
            onSignIn = {
                scope.launch {
                    statusMessage = "Opening Google sign-in…"
                    authStatus = "Signing in…"
                    val outcome = withContext(Dispatchers.IO) {
                        runCatching {
                            val (id, secret) = resolveDesktopCredentials()
                            val clientId = id?.takeIf { it.isNotBlank() }
                                ?: error("Missing VITE_GOOGLE_CLIENT_ID in apps/desktop/.env")
                            GoogleDesktopSignIn.signIn(
                                clientId,
                                secret,
                                includeTasks = settings.showGoogleTasks,
                            )
                        }
                    }
                    outcome.fold(
                        onSuccess = {
                            tokens = it
                            statusMessage = "Signed in with Google"
                            loadData(forceRefresh = true)
                        },
                        onFailure = { err ->
                            authStatus = "Needs reauth"
                            statusMessage = "Sign-in failed: ${err.message ?: "error"}"
                        },
                    )
                }
            },
            windowStartup = WindowStartupUi(
                openAtStartup = chrome.openAtStartup,
                minimizeTo = chrome.minimizeTo,
                closeTo = chrome.closeTo,
                canRegisterStartup = DesktopAutostart.canRegisterNow(),
                startupHint = startupHint,
            ),
            onWindowStartupChange = { ui ->
                val next = chrome.copy(
                    openAtStartup = ui.openAtStartup,
                    minimizeTo = ui.minimizeTo,
                    closeTo = ui.closeTo,
                )
                if (ui.openAtStartup != chrome.openAtStartup) {
                    val result = DesktopAutostart.setEnabled(ui.openAtStartup)
                    result.fold(
                        onSuccess = {
                            persistChrome(next)
                            statusMessage = if (ui.openAtStartup) {
                                "Open at startup enabled"
                            } else {
                                "Open at startup disabled"
                            }
                        },
                        onFailure = { err ->
                            // Keep preference when gradle/java can't write Run key; still save intent.
                            persistChrome(
                                next,
                                hint = err.message ?: "Could not update Open at startup",
                            )
                            statusMessage = err.message ?: "Could not update Open at startup"
                        },
                    )
                } else {
                    persistChrome(next)
                }
            },
            initialSettingsCategory = SettingsCategory.fromStoredId(chrome.settingsCategory),
            onSettingsCategoryChange = { cat ->
                persistChrome(chrome.copy(settingsCategory = cat.storedId))
            },
            appVersion = appVersion,
            onOpenDonate = {
                openExternalUrl(CONTINUUM_VENMO_DONATE_URL) {
                    statusMessage = "Could not open donate link"
                }
            },
            onCheckUpdates = {
                scope.launch {
                    updateStatus = "Checking…"
                    val result = withContext(Dispatchers.IO) { GitHubUpdateCheck.check(appVersion) }
                    updateStatus = when (result) {
                        is UpdateCheckResult.UpToDate -> "You’re on the latest release ($appVersion)"
                        is UpdateCheckResult.Available -> {
                            openExternalUrl(result.url) {
                                statusMessage = "Update available: ${result.version}"
                            }
                            "Update available: ${result.version}"
                        }
                        is UpdateCheckResult.Failed -> "Update check failed: ${result.message}"
                    }
                }
            },
            onSendFeedback = {
                val crash = DesktopCrashCapture.readPending()
                openExternalUrl(DesktopCrashCapture.feedbackIssueUrl(crash)) {
                    statusMessage = "Could not open feedback link"
                }
            },
            updateStatus = updateStatus,
            onOpenUrl = { url ->
                openExternalUrl(url) {
                    statusMessage = "Could not open link"
                }
            },
            onExportIcs = {
                val ics = exportEventsToIcs(displayEvents)
                val chooser = JFileChooser().apply {
                    selectedFile = java.io.File("continuum-export.ics")
                }
                if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    chooser.selectedFile.writeText(ics, Charsets.UTF_8)
                    statusMessage = "Exported ${displayEvents.size} events"
                }
            },
            onImportIcs = {
                val chooser = JFileChooser()
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    val text = chooser.selectedFile.readText(Charsets.UTF_8)
                    val imported = importEventsFromIcs(text)
                    allEvents = allEvents.filterNot { it.source == org.continuumcalendar.domain.model.CalendarSource.ICS_IMPORT } + imported
                    statusMessage = "Imported ${imported.size} ICS events"
                }
            },
            onExportSettings = {
                val body = continuumSettingsToJson(settings)
                val chooser = JFileChooser().apply {
                    selectedFile = java.io.File("continuum-settings.json")
                }
                if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    chooser.selectedFile.writeText(body, Charsets.UTF_8)
                    statusMessage = "Settings exported"
                }
            },
            onImportSettings = {
                val chooser = JFileChooser()
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    val parsed = parseContinuumSettingsJson(chooser.selectedFile.readText(Charsets.UTF_8))
                    if (parsed != null) {
                        persistSettings(parsed)
                        statusMessage = "Settings imported"
                        loadData(forceRefresh = true)
                    } else {
                        statusMessage = "Invalid settings JSON"
                    }
                }
            },
            onSaveCalDav = { url, user, pass ->
                DesktopCalDavStore.save(
                    org.continuumcalendar.data.settings.DesktopCalDavAccount(url, user, pass),
                )
                statusMessage = if (url.isBlank()) "CalDAV account cleared" else "CalDAV account saved — sync on Refresh when reachable"
            },
            onCopyText = { text ->
                runCatching {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
                    statusMessage = "Copied free slots"
                }.onFailure { statusMessage = "Clipboard unavailable" }
            },
            onProposeSlot = { slot: FreeSlot ->
                statusMessage = "Proposed ${formatProposedSlot(slot, settings.use24HourFormat)}"
            },
            geocodeLocation = { query ->
                withContext(Dispatchers.IO) {
                    PhotonGeocode.geocode(query)?.let {
                        GeocodeHit(label = it.label, lat = it.lat, lon = it.lon)
                    }
                }
            },
            loadMapImage = { url ->
                withContext(Dispatchers.IO) { loadImageBitmapFromUrl(url) }
            },
        )
    }
    } // if windowVisible
}

private fun openExternalUrl(url: String, onFailure: () -> Unit) {
    runCatching {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(url))
        } else {
            error("Desktop browse unsupported")
        }
    }.onFailure { onFailure() }
}

private val mapHttp: HttpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(8))
    .build()

private fun loadImageBitmapFromUrl(url: String): androidx.compose.ui.graphics.ImageBitmap? {
    return runCatching {
        val req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .header("User-Agent", "ContinuumCalendar/0.1 (FOSS desktop)")
            .GET()
            .build()
        val res = mapHttp.send(req, HttpResponse.BodyHandlers.ofByteArray())
        if (res.statusCode() !in 200..299) return null
        Image.makeFromEncoded(res.body()).toComposeImageBitmap()
    }.getOrNull()
}

private data class LoadResult(
    val events: List<CalendarEvent>,
    val calendars: List<CalendarListEntry>,
    val tokens: GoogleOAuthTokens?,
    val auth: String,
    val peerSettings: ContinuumSettings? = null,
    val peerNote: String? = null,
)

private fun resolveDesktopCredentials(): Pair<String?, String?> {
    val seeds = listOf(
        File(System.getProperty("user.dir") ?: "."),
        File(".."),
        File("../.."),
        File("../../.."),
    )
    for (seed in seeds) {
        val (id, secret) = loadDesktopEnvCredentials(seed)
        if (!id.isNullOrBlank() || !secret.isNullOrBlank()) return id to secret
    }
    return null to null
}

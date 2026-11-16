package org.continuumcalendar.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.ImageBitmap
import org.continuumcalendar.domain.agenda.formatEventTimeRange
import org.continuumcalendar.domain.agenda.localDateKey
import org.continuumcalendar.domain.conflict.conflictedEventIds
import org.continuumcalendar.domain.event.RepeatFreq
import org.continuumcalendar.domain.event.formatRepeatFreq
import org.continuumcalendar.domain.event.freqFromRrule
import org.continuumcalendar.domain.hotkey.ShellHotkeyAction
import org.continuumcalendar.domain.hotkey.resolveShellHotkey
import org.continuumcalendar.domain.maps.MapOpenTarget
import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.CalendarListEntry
import org.continuumcalendar.domain.model.ContinuumSettings
import org.continuumcalendar.domain.model.FreeSlot
import org.continuumcalendar.domain.remind.formatReminderMinutes
import org.continuumcalendar.domain.schedule.formatProposedSlot
import org.continuumcalendar.domain.search.filterEventsByQuery
import org.continuumcalendar.domain.search.parseGoToDateKey
import org.continuumcalendar.ui.agenda.AgendaScreen
import org.continuumcalendar.ui.event.EventDraft
import org.continuumcalendar.ui.event.EventEditorSheet
import org.continuumcalendar.ui.event.toDraft
import org.continuumcalendar.ui.maps.GeocodeHit
import org.continuumcalendar.ui.maps.LocationMapPreview
import org.continuumcalendar.ui.maps.MapOpenChooser
import org.continuumcalendar.ui.month.MonthScreen
import org.continuumcalendar.ui.month.YearScreen
import org.continuumcalendar.ui.settings.SettingsCategory
import org.continuumcalendar.ui.settings.SettingsPanel
import org.continuumcalendar.ui.settings.WindowStartupUi
import org.continuumcalendar.ui.theme.ContinuumTheme
import org.continuumcalendar.ui.theme.LocalContinuumPalette
import org.continuumcalendar.ui.week.RollingWeekScreen

enum class ContinuumMainView { AGENDA, ROLLING, MONTH, YEAR }

@Composable
fun ContinuumShell(
    events: List<CalendarEvent>,
    calendars: List<CalendarListEntry>,
    settings: ContinuumSettings,
    nowEpochMs: Long,
    authStatus: String = "Local demo",
    statusMessage: String = "",
    initialView: ContinuumMainView = ContinuumMainView.ROLLING,
    onSettingsChange: (ContinuumSettings) -> Unit = {},
    onToggleCalendar: (calendarId: String, visible: Boolean) -> Unit = { _, _ -> },
    onRefresh: () -> Unit = {},
    onCreateDraft: () -> EventDraft? = { null },
    onOpenDayDraft: (dateKey: String) -> EventDraft? = { null },
    onSlotDraft: (dateKey: String, hour: Int, minute: Int) -> EventDraft? = { _, _, _ -> null },
    onSaveEvent: (EventDraft) -> Unit = {},
    onDeleteEvent: (EventDraft) -> Unit = {},
    onSignOut: (() -> Unit)? = null,
    onSignIn: (() -> Unit)? = null,
    windowStartup: WindowStartupUi = WindowStartupUi(),
    onWindowStartupChange: (WindowStartupUi) -> Unit = {},
    initialSettingsCategory: SettingsCategory = SettingsCategory.CALENDAR,
    onSettingsCategoryChange: (SettingsCategory) -> Unit = {},
    appVersion: String = "0.1.0",
    onOpenDonate: (() -> Unit)? = null,
    onCheckUpdates: (() -> Unit)? = null,
    onSendFeedback: (() -> Unit)? = null,
    updateStatus: String = "",
    showSplash: Boolean = true,
    onOpenUrl: (String) -> Unit = {},
    onExportIcs: (() -> Unit)? = null,
    onImportIcs: (() -> Unit)? = null,
    onExportSettings: (() -> Unit)? = null,
    onImportSettings: (() -> Unit)? = null,
    onSaveCalDav: ((String, String, String) -> Unit)? = null,
    onCopyText: (String) -> Unit = {},
    onProposeSlot: (FreeSlot) -> Unit = {},
    geocodeLocation: (suspend (String) -> GeocodeHit?)? = null,
    loadMapImage: (suspend (String) -> ImageBitmap?)? = null,
) {
    ContinuumTheme(themeMode = settings.themeMode) {
        val palette = LocalContinuumPalette.current
        val today = localDateKey(nowEpochMs)
        var splash by remember { mutableStateOf(showSplash) }
        var view by remember { mutableStateOf(initialView) }
        var showSettings by remember { mutableStateOf(false) }
        var selected by remember { mutableStateOf<CalendarEvent?>(null) }
        var editing by remember { mutableStateOf<EventDraft?>(null) }
        var searchQuery by remember { mutableStateOf("") }
        var goToText by remember { mutableStateOf("") }
        var focusDateKey by remember(today) { mutableStateOf(today) }
        var weekShiftDays by remember { mutableStateOf(0) }
        var conflictBannerDismissed by remember { mutableStateOf(false) }
        var mapTarget by remember { mutableStateOf<MapOpenTarget?>(null) }
        var showPalette by remember { mutableStateOf(false) }
        var showSlots by remember { mutableStateOf(false) }
        var searchFocusTick by remember { mutableStateOf(0) }
        var goToFocusTick by remember { mutableStateOf(0) }
        val searchFocus = remember { FocusRequester() }
        val goToFocus = remember { FocusRequester() }
        var typingInField by remember { mutableStateOf(false) }

        val filtered = remember(events, searchQuery) { filterEventsByQuery(events, searchQuery) }
        val conflictIds = remember(filtered) { conflictedEventIds(filtered) }
        val conflictCount = conflictIds.size

        fun jumpToday() {
            focusDateKey = localDateKey(nowEpochMs)
            weekShiftDays = 0
            goToText = ""
        }

        fun applyGoTo() {
            val key = parseGoToDateKey(goToText) ?: return
            focusDateKey = key
            weekShiftDays = 0
        }

        fun handleHotkey(action: ShellHotkeyAction) {
            when (action) {
                ShellHotkeyAction.NEW_EVENT -> {
                    editing = onCreateDraft()
                    selected = null
                    showSettings = false
                }
                ShellHotkeyAction.TODAY -> {
                    jumpToday()
                    if (view != ContinuumMainView.YEAR && view != ContinuumMainView.MONTH) {
                        view = ContinuumMainView.ROLLING
                    }
                }
                ShellHotkeyAction.VIEW_AGENDA -> view = ContinuumMainView.AGENDA
                ShellHotkeyAction.VIEW_WEEK -> view = ContinuumMainView.ROLLING
                ShellHotkeyAction.VIEW_MONTH -> view = ContinuumMainView.MONTH
                ShellHotkeyAction.VIEW_YEAR -> view = ContinuumMainView.YEAR
                ShellHotkeyAction.FOCUS_SEARCH -> searchFocusTick++
                ShellHotkeyAction.FOCUS_GOTO -> goToFocusTick++
                ShellHotkeyAction.COMMAND_PALETTE -> showPalette = true
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(palette.background)
                .padding(16.dp)
                .onPreviewKeyEvent { ev ->
                    if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val ch = ev.utf16CodePoint.toChar().toString()
                    val keyName = when {
                        ch.isNotBlank() && !ch[0].isISOControl() -> ch
                        ev.key == Key.DirectionUp -> ""
                        else -> ""
                    }
                    val raw = if (keyName.isNotBlank()) keyName else when (ev.key) {
                        Key.One -> "1"
                        Key.Two -> "2"
                        Key.Three -> "3"
                        Key.Four -> "4"
                        Key.Slash -> "/"
                        Key.F -> "f"
                        Key.G -> "g"
                        Key.N -> "n"
                        Key.T -> "t"
                        Key.K -> "k"
                        else -> ""
                    }
                    val action = resolveShellHotkey(
                        key = raw,
                        ctrl = ev.isCtrlPressed,
                        meta = ev.isMetaPressed,
                        alt = false,
                        typingInField = typingInField && !(ev.isCtrlPressed || ev.isMetaPressed),
                    ) ?: return@onPreviewKeyEvent false
                    handleHotkey(action)
                    true
                },
        ) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppTitle()
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = authStatus,
                        color = palette.muted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    TextButton(onClick = onRefresh) {
                        Text("Refresh", color = palette.accent)
                    }
                    TextButton(onClick = { showSlots = true }) {
                        Text("Free slots", color = palette.accent)
                    }
                    TextButton(onClick = { showSettings = !showSettings; editing = null }) {
                        Text("Settings", color = palette.accent)
                    }
                }
                CalendarToolbar(
                    view = view,
                    searchQuery = searchQuery,
                    goToText = goToText,
                    onSelectView = { view = it },
                    onSearchChange = {
                        typingInField = true
                        searchQuery = it
                    },
                    onGoToChange = {
                        typingInField = true
                        goToText = it
                    },
                    onGoToSubmit = { applyGoTo() },
                    onToday = {
                        jumpToday()
                        if (view == ContinuumMainView.YEAR || view == ContinuumMainView.MONTH) {
                            // stay
                        } else {
                            view = ContinuumMainView.ROLLING
                        }
                    },
                    searchFocusRequester = searchFocus,
                    goToFocusRequester = goToFocus,
                    requestSearchFocus = searchFocusTick > 0,
                    requestGoToFocus = goToFocusTick > 0,
                )
                val needsReauth = onSignIn != null && (
                    authStatus.contains("reauth", ignoreCase = true) ||
                        authStatus.contains("expired", ignoreCase = true) ||
                        authStatus.contains("refresh failed", ignoreCase = true) ||
                        authStatus.contains("invalid_grant", ignoreCase = true)
                    )
                if (needsReauth) {
                    AuthReconnectBanner(onSignIn = onSignIn!!)
                }
                if (conflictCount > 0 && !conflictBannerDismissed) {
                    ConflictBanner(
                        count = conflictCount,
                        onJump = {
                            val id = conflictIds.firstOrNull()
                            selected = filtered.firstOrNull { it.id == id }
                            view = ContinuumMainView.AGENDA
                        },
                        onDismiss = { conflictBannerDismissed = true },
                    )
                }
                Row(
                    Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        when (view) {
                            ContinuumMainView.AGENDA -> AgendaScreen(
                                events = filtered,
                                calendars = calendars,
                                settings = settings,
                                nowEpochMs = nowEpochMs,
                                focusDateKey = focusDateKey,
                                conflictIds = conflictIds,
                                onSelectEvent = {
                                    selected = it
                                    editing = null
                                    showSettings = false
                                },
                                onOpenDay = { dateKey ->
                                    editing = onOpenDayDraft(dateKey)
                                    selected = null
                                    showSettings = false
                                },
                            )
                            ContinuumMainView.ROLLING -> RollingWeekScreen(
                                events = filtered,
                                calendars = calendars,
                                settings = settings,
                                nowEpochMs = nowEpochMs,
                                focusDateKey = focusDateKey,
                                weekShiftDays = weekShiftDays,
                                conflictIds = conflictIds,
                                onShiftWeek = { weekShiftDays += it },
                                onWeekModeChange = { mode ->
                                    onSettingsChange(settings.copy(weekHourDisplay = mode))
                                },
                                onSelectEvent = {
                                    selected = it
                                    editing = null
                                    showSettings = false
                                },
                                onAddDay = { dateKey ->
                                    editing = onOpenDayDraft(dateKey)
                                    selected = null
                                    showSettings = false
                                },
                                onSlotClick = { dateKey, hour, minute ->
                                    editing = onSlotDraft(dateKey, hour, minute)
                                    selected = null
                                    showSettings = false
                                },
                                onEventMove = { ev, newStart, newEnd ->
                                    onSaveEvent(ev.toDraft().copy(start = newStart, end = newEnd))
                                },
                            )
                            ContinuumMainView.MONTH -> MonthScreen(
                                events = filtered,
                                settings = settings,
                                nowEpochMs = nowEpochMs,
                                focusDateKey = focusDateKey,
                                onFocusDate = { focusDateKey = it },
                                onSelectEvent = {
                                    selected = it
                                    editing = null
                                },
                            )
                            ContinuumMainView.YEAR -> YearScreen(
                                events = filtered,
                                settings = settings,
                                nowEpochMs = nowEpochMs,
                                focusDateKey = focusDateKey,
                                onFocusDate = { focusDateKey = it },
                                onOpenMonth = {
                                    focusDateKey = it
                                    view = ContinuumMainView.MONTH
                                },
                            )
                        }
                    }
                    editing?.let { draft ->
                        EventEditorSheet(
                            draft = draft,
                            calendars = calendars,
                            defaultWriteCalendarId = settings.defaultWriteCalendarId,
                            defaultReminderMinutes = settings.defaultReminderMinutes,
                            onSave = {
                                onSaveEvent(it)
                                editing = null
                            },
                            onDelete = if (draft.id != null) {
                                {
                                    onDeleteEvent(it)
                                    editing = null
                                    selected = null
                                }
                            } else {
                                null
                            },
                            onClose = { editing = null },
                        )
                    } ?: selected?.let { ev ->
                        EventSideSheet(
                            event = ev,
                            calendars = calendars,
                            use24Hour = settings.use24HourFormat,
                            defaultReminderMinutes = settings.defaultReminderMinutes,
                            conflict = conflictIds.contains(ev.id),
                            geocodeLocation = geocodeLocation,
                            loadMapImage = loadMapImage,
                            onOpenMaps = { mapTarget = it },
                            onEdit = {
                                editing = ev.toDraft()
                                selected = null
                            },
                            onClose = { selected = null },
                        )
                    }
                    if (showSettings) {
                        SettingsPanel(
                            settings = settings,
                            calendars = calendars,
                            authStatus = authStatus,
                            onSettingsChange = onSettingsChange,
                            onToggleCalendar = onToggleCalendar,
                            onSignOut = onSignOut,
                            onSignIn = onSignIn,
                            windowStartup = windowStartup,
                            onWindowStartupChange = onWindowStartupChange,
                            initialCategory = initialSettingsCategory,
                            onCategoryChange = onSettingsCategoryChange,
                            appVersion = appVersion,
                            onOpenDonate = onOpenDonate,
                            onCheckUpdates = onCheckUpdates,
                            onSendFeedback = onSendFeedback,
                            updateStatus = updateStatus,
                            onExportIcs = onExportIcs,
                            onImportIcs = onImportIcs,
                            onExportSettings = onExportSettings,
                            onImportSettings = onImportSettings,
                            onSaveCalDav = onSaveCalDav,
                            onClose = { showSettings = false },
                        )
                    }
                }
                if (statusMessage.isNotBlank()) {
                    Text(
                        text = statusMessage,
                        color = palette.muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            FloatingActionButton(
                onClick = {
                    editing = onCreateDraft()
                    selected = null
                    showSettings = false
                },
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(56.dp),
                shape = CircleShape,
                containerColor = palette.accent,
                contentColor = palette.onAccent,
            ) {
                Text("+", fontSize = 24.sp, fontWeight = FontWeight.Medium)
            }
            mapTarget?.let { target ->
                MapOpenChooser(
                    target = target,
                    onOpenUrl = onOpenUrl,
                    onClose = { mapTarget = null },
                )
            }
            if (splash) {
                SplashOverlay(onFinished = { splash = false })
            }
            if (showPalette) {
                CommandPalette(
                    onAction = { id ->
                        when (id) {
                            "new" -> handleHotkey(ShellHotkeyAction.NEW_EVENT)
                            "today" -> handleHotkey(ShellHotkeyAction.TODAY)
                            "agenda" -> handleHotkey(ShellHotkeyAction.VIEW_AGENDA)
                            "week" -> handleHotkey(ShellHotkeyAction.VIEW_WEEK)
                            "month" -> handleHotkey(ShellHotkeyAction.VIEW_MONTH)
                            "year" -> handleHotkey(ShellHotkeyAction.VIEW_YEAR)
                            "search" -> handleHotkey(ShellHotkeyAction.FOCUS_SEARCH)
                            "goto" -> handleHotkey(ShellHotkeyAction.FOCUS_GOTO)
                            "slots" -> showSlots = true
                            "settings" -> showSettings = true
                        }
                    },
                    onClose = { showPalette = false },
                )
            }
            if (showSlots) {
                FreeSlotsMenu(
                    events = filtered,
                    settings = settings,
                    nowEpochMs = nowEpochMs,
                    onPick = { slot ->
                        onProposeSlot(slot)
                        val startIso = kotlinx.datetime.Instant.fromEpochMilliseconds(slot.startEpochMs).toString()
                        val endIso = kotlinx.datetime.Instant.fromEpochMilliseconds(slot.endEpochMs).toString()
                        val dateKey = startIso.take(10)
                        val hour = startIso.substringAfter("T").take(2).toIntOrNull() ?: 9
                        val minute = startIso.substringAfter("T").drop(3).take(2).toIntOrNull() ?: 0
                        editing = onSlotDraft(dateKey, hour, minute)
                            ?: EventDraft(
                                calendarId = settings.defaultWriteCalendarId.removePrefix("google:").ifBlank { "primary" },
                                start = startIso,
                                end = endIso,
                            )
                        selected = null
                        showSettings = false
                    },
                    onCopyAll = { slots ->
                        onCopyText(
                            slots.joinToString("\n") {
                                formatProposedSlot(it, settings.use24HourFormat)
                            },
                        )
                    },
                    onClose = { showSlots = false },
                )
            }
        }
    }
}

@Composable
private fun AuthReconnectBanner(onSignIn: () -> Unit) {
    val palette = LocalContinuumPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(palette.surface)
            .border(2.dp, palette.now, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Google sign-in expired. Sign in again to sync.",
            color = palette.text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            "Sign in again",
            color = palette.accent,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onSignIn).padding(8.dp),
        )
    }
}

@Composable
private fun ConflictBanner(count: Int, onJump: () -> Unit, onDismiss: () -> Unit) {
    val palette = LocalContinuumPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(palette.warn.copy(alpha = 0.35f))
            .border(1.dp, palette.warn, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$count overlapping events",
            color = palette.text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onJump) { Text("Jump", color = palette.accent) }
        TextButton(onClick = onDismiss) { Text("Dismiss", color = palette.muted) }
    }
}

@Composable
private fun AppTitle() {
    val palette = LocalContinuumPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Continuum",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.02).sp,
            ),
            color = palette.accent,
        )
        Text(
            text = " Calendar",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.02).sp,
            ),
            color = palette.text,
        )
    }
}

@Composable
private fun EventSideSheet(
    event: CalendarEvent,
    calendars: List<CalendarListEntry>,
    use24Hour: Boolean,
    defaultReminderMinutes: Int,
    conflict: Boolean,
    geocodeLocation: (suspend (String) -> GeocodeHit?)?,
    loadMapImage: (suspend (String) -> ImageBitmap?)?,
    onOpenMaps: (MapOpenTarget) -> Unit,
    onEdit: () -> Unit,
    onClose: () -> Unit,
) {
    val palette = LocalContinuumPalette.current
    val cal = calendars.firstOrNull { it.id == event.calendarId }
    val calName = cal?.displayName ?: event.calendarId
    val canEdit = !event.readOnly && (cal?.writable != false)
    val freq = freqFromRrule(event.recurrence)
    Column(
        Modifier
            .width(320.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surface)
            .border(1.dp, palette.border, RoundedCornerShape(12.dp))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Event", style = MaterialTheme.typography.titleMedium, color = palette.accent)
            Spacer(Modifier.weight(1f))
            if (canEdit) {
                Text("Edit", color = palette.accent, modifier = Modifier.clickable(onClick = onEdit).padding(end = 12.dp))
            }
            Text("Close", color = palette.muted, modifier = Modifier.clickable(onClick = onClose))
        }
        if (conflict) {
            Text("Overlaps another event", color = palette.now, style = MaterialTheme.typography.bodySmall)
        }
        Text(event.title, style = MaterialTheme.typography.headlineMedium, color = palette.text)
        Text(calName, style = MaterialTheme.typography.bodySmall, color = palette.muted)
        Text(
            text = formatEventTimeRange(event, use24Hour = use24Hour),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.text,
        )
        if (freq != RepeatFreq.NONE) {
            Text(formatRepeatFreq(freq), style = MaterialTheme.typography.bodySmall, color = palette.muted)
        }
        event.location?.takeIf { it.isNotBlank() }?.let { loc ->
            Text(
                loc,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.accent,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onOpenMaps(MapOpenTarget(query = loc)) },
            )
            if (geocodeLocation != null && loadMapImage != null) {
                LocationMapPreview(
                    location = loc,
                    geocode = geocodeLocation,
                    loadImage = loadMapImage,
                    onOpenMaps = onOpenMaps,
                )
            }
        }
        event.description?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = palette.muted)
        }
        if (event.attendees.isNotEmpty()) {
            Text("Guests", style = MaterialTheme.typography.bodySmall, color = palette.muted)
            event.attendees.forEach { a ->
                val name = a.displayName?.takeIf { it.isNotBlank() } ?: a.email
                Text(
                    "$name · ${responseLabel(a.responseStatus)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.text,
                )
            }
        }
        Text("Reminders", style = MaterialTheme.typography.bodySmall, color = palette.muted)
        if (event.reminders.isEmpty()) {
            Text(
                "Default · ${formatReminderMinutes(defaultReminderMinutes)}",
                style = MaterialTheme.typography.bodySmall,
                color = palette.text,
            )
        } else {
            event.reminders.forEach { r ->
                Text(
                    formatReminderMinutes(r.minutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.text,
                )
            }
        }
    }
}

private fun responseLabel(status: String?): String = when (status) {
    "accepted" -> "Accepted"
    "declined" -> "Declined"
    "tentative" -> "Maybe"
    else -> "Needs action"
}

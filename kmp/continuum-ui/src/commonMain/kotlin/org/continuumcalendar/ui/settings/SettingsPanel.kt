package org.continuumcalendar.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.continuumcalendar.domain.model.CalendarListEntry
import org.continuumcalendar.domain.model.ContinuumSettings
import org.continuumcalendar.domain.model.WorkingHours
import org.continuumcalendar.domain.model.defaultCalendarNotifyPrefs
import org.continuumcalendar.ui.theme.LocalContinuumPalette

data class WindowStartupUi(
    val openAtStartup: Boolean = false,
    val minimizeTo: String = "taskbar",
    val closeTo: String = "quit",
    val canRegisterStartup: Boolean = false,
    val startupHint: String = "",
)

@Composable
fun SettingsPanel(
    settings: ContinuumSettings,
    calendars: List<CalendarListEntry>,
    authStatus: String,
    onSettingsChange: (ContinuumSettings) -> Unit,
    onToggleCalendar: (calendarId: String, visible: Boolean) -> Unit,
    onSignOut: (() -> Unit)? = null,
    onSignIn: (() -> Unit)? = null,
    windowStartup: WindowStartupUi = WindowStartupUi(),
    onWindowStartupChange: (WindowStartupUi) -> Unit = {},
    initialCategory: SettingsCategory = SettingsCategory.CALENDAR,
    onCategoryChange: (SettingsCategory) -> Unit = {},
    appVersion: String = "0.1.0",
    onOpenDonate: (() -> Unit)? = null,
    onCheckUpdates: (() -> Unit)? = null,
    onSendFeedback: (() -> Unit)? = null,
    updateStatus: String = "",
    onExportIcs: (() -> Unit)? = null,
    onImportIcs: (() -> Unit)? = null,
    onExportSettings: (() -> Unit)? = null,
    onImportSettings: (() -> Unit)? = null,
    onSaveCalDav: ((String, String, String) -> Unit)? = null,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalContinuumPalette.current
    var category by remember(initialCategory) { mutableStateOf(initialCategory) }
    var query by remember { mutableStateOf("") }

    fun selectCategory(cat: SettingsCategory) {
        category = cat
        onCategoryChange(cat)
    }
    val visibleCats = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) SettingsCategory.entries
        else SettingsCategory.entries.filter {
            it.title.lowercase().contains(q) || it.blurb.lowercase().contains(q) || it.name.lowercase().contains(q)
        }
    }
    Row(
        modifier
            .width(560.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surface)
            .border(1.dp, palette.border, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            Modifier.width(160.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Settings", style = MaterialTheme.typography.titleMedium, color = palette.accent)
                Spacer(Modifier.weight(1f))
                Text("Close", color = palette.muted, modifier = Modifier.clickable(onClick = onClose))
            }
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(color = palette.text),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(palette.background)
                    .border(1.dp, palette.border, RoundedCornerShape(6.dp))
                    .padding(8.dp),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text("Search settings", color = palette.muted, style = MaterialTheme.typography.bodySmall)
                    }
                    inner()
                },
            )
            visibleCats.forEach { cat ->
                val on = category == cat
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (on) palette.accent.copy(alpha = 0.12f) else palette.surface)
                        .clickable { selectCategory(cat) }
                        .padding(8.dp),
                ) {
                    Text(
                        cat.title,
                        color = if (on) palette.accent else palette.text,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(cat.blurb, color = palette.muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Column(
            Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            when (category) {
                SettingsCategory.ACCOUNT -> AccountSection(
                    settings, authStatus, onSettingsChange, onSignOut, onSignIn,
                )
                SettingsCategory.APPEARANCE -> AppearanceSection(settings, onSettingsChange)
                SettingsCategory.CALENDAR -> CalendarSection(settings, calendars, onSettingsChange, onToggleCalendar)
                SettingsCategory.REMINDERS -> RemindersSection(settings, onSettingsChange)
                SettingsCategory.SCHEDULING -> SchedulingSection(settings, onSettingsChange)
                SettingsCategory.WINDOW -> WindowSection(windowStartup, onWindowStartupChange)
                SettingsCategory.FILES -> FilesSection(
                    onExportIcs = onExportIcs,
                    onImportIcs = onImportIcs,
                    onExportSettings = onExportSettings,
                    onImportSettings = onImportSettings,
                    onSaveCalDav = onSaveCalDav,
                )
                SettingsCategory.ABOUT -> AboutSection(
                    appVersion = appVersion,
                    onOpenDonate = onOpenDonate,
                    onCheckUpdates = onCheckUpdates,
                    onSendFeedback = onSendFeedback,
                    updateStatus = updateStatus,
                )
            }
        }
    }
}

@Composable
private fun ComingSoon(message: String) {
    SettingsHint(message)
}

@Composable
private fun AboutSection(
    appVersion: String,
    onOpenDonate: (() -> Unit)?,
    onCheckUpdates: (() -> Unit)?,
    onSendFeedback: (() -> Unit)?,
    updateStatus: String,
) {
    val palette = LocalContinuumPalette.current
    SettingsSectionTitle("About")
    Text("Continuum Calendar", color = palette.text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    Text("Version $appVersion", color = palette.muted, style = MaterialTheme.typography.bodySmall)
    SettingsHint("FOSS calendar under the MIT license. No proprietary SDKs on the production path.")
    if (onCheckUpdates != null) {
        Text(
            "Check for updates",
            color = palette.accent,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onCheckUpdates)
                .padding(8.dp),
        )
    }
    if (updateStatus.isNotBlank()) SettingsHint(updateStatus)
    if (onSendFeedback != null) {
        Text(
            "Send feedback",
            color = palette.accent,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onSendFeedback)
                .padding(8.dp),
        )
        SettingsHint("Opens GitHub with the last local crash (if any). Never auto-sends.")
    }
    SettingsSectionTitle("Support development")
    SettingsHint("If Continuum Calendar helps you, consider supporting development.")
    if (onOpenDonate != null) {
        Text(
            "Donate via Venmo",
            color = palette.accent,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onOpenDonate)
                .padding(8.dp),
        )
        SettingsHint(CONTINUUM_VENMO_DONATE_URL)
    } else {
        SettingsHint(CONTINUUM_VENMO_DONATE_URL)
    }
}

@Composable
private fun AccountSection(
    settings: ContinuumSettings,
    authStatus: String,
    onChange: (ContinuumSettings) -> Unit,
    onSignOut: (() -> Unit)?,
    onSignIn: (() -> Unit)?,
) {
    SettingsSectionTitle("Account & Google")
    SettingsHint(authStatus)
    val signedIn = authStatus.contains("Signed in", ignoreCase = true)
    if (onSignIn != null && !signedIn) {
        Text(
            "Sign in with Google",
            color = LocalContinuumPalette.current.accent,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onSignIn)
                .padding(8.dp),
        )
    }
    if (onSignOut != null && signedIn) {
        Text(
            "Sign out (clears local Google tokens)",
            color = LocalContinuumPalette.current.now,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onSignOut)
                .padding(8.dp),
        )
    } else if (
        onSignIn != null &&
        (authStatus.contains("failed", ignoreCase = true) || authStatus.contains("reauth", ignoreCase = true))
    ) {
        Text(
            "Sign in again",
            color = LocalContinuumPalette.current.accent,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onSignIn)
                .padding(8.dp),
        )
    }
    SettingsCheckRow("Use Google Calendar", settings.useGoogleCalendar) {
        onChange(settings.copy(useGoogleCalendar = !settings.useGoogleCalendar))
    }
    SettingsCheckRow("Show Google automated birthdays", settings.showContactBirthdays) {
        onChange(settings.copy(showContactBirthdays = !settings.showContactBirthdays))
    }
    SettingsHint("Birthdays use Google Calendar birthday events. People/Contacts suggest stays deferred.")
    SettingsCheckRow("Show Google Tasks (due dates)", settings.showGoogleTasks) {
        onChange(settings.copy(showGoogleTasks = !settings.showGoogleTasks))
    }
    SettingsHint("Requires Tasks scope on the next Sign in with Google.")
}

@Composable
private fun FilesSection(
    onExportIcs: (() -> Unit)?,
    onImportIcs: (() -> Unit)?,
    onExportSettings: (() -> Unit)?,
    onImportSettings: (() -> Unit)?,
    onSaveCalDav: ((String, String, String) -> Unit)?,
) {
    var calDavUrl by remember { mutableStateOf("") }
    var calDavUser by remember { mutableStateOf("") }
    var calDavPass by remember { mutableStateOf("") }
    SettingsSectionTitle("ICS")
    if (onExportIcs != null) {
        Text(
            "Export visible Continuum events (.ics)",
            color = LocalContinuumPalette.current.accent,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable(onClick = onExportIcs).padding(8.dp),
        )
    }
    if (onImportIcs != null) {
        Text(
            "Import .ics file",
            color = LocalContinuumPalette.current.accent,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable(onClick = onImportIcs).padding(8.dp),
        )
    }
    SettingsSectionTitle("Settings backup")
    if (onExportSettings != null) {
        Text(
            "Export settings JSON",
            color = LocalContinuumPalette.current.accent,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable(onClick = onExportSettings).padding(8.dp),
        )
    }
    if (onImportSettings != null) {
        Text(
            "Import settings JSON",
            color = LocalContinuumPalette.current.accent,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable(onClick = onImportSettings).padding(8.dp),
        )
    }
    SettingsSectionTitle("CalDAV")
    SettingsHint("Stores account locally and syncs on next Refresh when reachable.")
    androidx.compose.foundation.text.BasicTextField(
        value = calDavUrl,
        onValueChange = { calDavUrl = it },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall.copy(color = LocalContinuumPalette.current.text),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(LocalContinuumPalette.current.background).border(1.dp, LocalContinuumPalette.current.border, RoundedCornerShape(6.dp)).padding(8.dp),
        decorationBox = { inner ->
            if (calDavUrl.isEmpty()) Text("CalDAV URL", color = LocalContinuumPalette.current.muted, style = MaterialTheme.typography.bodySmall)
            inner()
        },
    )
    androidx.compose.foundation.text.BasicTextField(
        value = calDavUser,
        onValueChange = { calDavUser = it },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall.copy(color = LocalContinuumPalette.current.text),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(LocalContinuumPalette.current.background).border(1.dp, LocalContinuumPalette.current.border, RoundedCornerShape(6.dp)).padding(8.dp),
        decorationBox = { inner ->
            if (calDavUser.isEmpty()) Text("Username", color = LocalContinuumPalette.current.muted, style = MaterialTheme.typography.bodySmall)
            inner()
        },
    )
    androidx.compose.foundation.text.BasicTextField(
        value = calDavPass,
        onValueChange = { calDavPass = it },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall.copy(color = LocalContinuumPalette.current.text),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(LocalContinuumPalette.current.background).border(1.dp, LocalContinuumPalette.current.border, RoundedCornerShape(6.dp)).padding(8.dp),
        decorationBox = { inner ->
            if (calDavPass.isEmpty()) Text("Password / app password", color = LocalContinuumPalette.current.muted, style = MaterialTheme.typography.bodySmall)
            inner()
        },
    )
    if (onSaveCalDav != null) {
        Text(
            "Save CalDAV account",
            color = LocalContinuumPalette.current.accent,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable {
                onSaveCalDav(calDavUrl.trim(), calDavUser.trim(), calDavPass)
            }.padding(8.dp),
        )
    }
}

@Composable
private fun WindowSection(
    prefs: WindowStartupUi,
    onChange: (WindowStartupUi) -> Unit,
) {
    SettingsSectionTitle("Window & startup")
    SettingsChoiceRow(
        label = "Minimize to",
        options = listOf("taskbar" to "Taskbar", "tray" to "Notification area"),
        selected = prefs.minimizeTo,
        onSelect = { onChange(prefs.copy(minimizeTo = it)) },
    )
    SettingsChoiceRow(
        label = "Close button",
        options = listOf("tray" to "Notification area", "quit" to "Quit Continuum"),
        selected = prefs.closeTo,
        onSelect = { onChange(prefs.copy(closeTo = it)) },
    )
    SettingsCheckRow("Open at startup", prefs.openAtStartup) {
        onChange(prefs.copy(openAtStartup = !prefs.openAtStartup))
    }
    if (prefs.startupHint.isNotBlank()) {
        SettingsHint(prefs.startupHint)
    } else if (!prefs.canRegisterStartup && prefs.openAtStartup) {
        SettingsHint("Preference saved. Registry registration applies when running the installed CMP app.")
    }
}

@Composable
private fun AppearanceSection(settings: ContinuumSettings, onChange: (ContinuumSettings) -> Unit) {
    SettingsSectionTitle("Appearance")
    SettingsChoiceRow(
        label = "Theme",
        options = listOf("system" to "System", "light" to "Light", "dark" to "Dark"),
        selected = settings.themeMode,
        onSelect = { onChange(settings.copy(themeMode = it)) },
    )
    SettingsCheckRow("24-hour clock", settings.use24HourFormat) {
        onChange(settings.copy(use24HourFormat = !settings.use24HourFormat))
    }
    SettingsChoiceRow(
        label = "Week hours",
        options = listOf(
            "scroll" to "Scroll",
            "fit24" to "Fit 24h",
            "agenda" to "Agenda",
        ),
        selected = settings.weekHourDisplay,
        onSelect = { onChange(settings.copy(weekHourDisplay = it)) },
    )
    SettingsCheckRow("Redact titles in screenshots", settings.redactTitlesInScreenshots) {
        onChange(settings.copy(redactTitlesInScreenshots = !settings.redactTitlesInScreenshots))
    }
}

@Composable
private fun CalendarSection(
    settings: ContinuumSettings,
    calendars: List<CalendarListEntry>,
    onChange: (ContinuumSettings) -> Unit,
    onToggleCalendar: (String, Boolean) -> Unit,
) {
    val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    SettingsSectionTitle("Calendars")
    if (calendars.isEmpty()) {
        SettingsHint("No calendars loaded yet — Refresh after sign-in.")
    } else {
        calendars.forEach { cal ->
            val prefs = settings.calendarNotifyPrefs[cal.logicalId] ?: defaultCalendarNotifyPrefs(cal.logicalId)
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                SettingsCheckRow(cal.displayName, cal.visible) {
                    onToggleCalendar(cal.id, !cal.visible)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 24.dp)) {
                    val isDefault = settings.defaultWriteCalendarId == cal.logicalId
                    Text(
                        if (isDefault) "★ Default" else "Set default",
                        color = LocalContinuumPalette.current.accent,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable {
                            onChange(settings.copy(defaultWriteCalendarId = cal.logicalId))
                        },
                    )
                    Text(
                        if (prefs.newEvent) "New ✓" else "New",
                        color = LocalContinuumPalette.current.muted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable {
                            onChange(settings.copy(calendarNotifyPrefs = settings.calendarNotifyPrefs + (cal.logicalId to prefs.copy(newEvent = !prefs.newEvent))))
                        },
                    )
                    Text(
                        if (prefs.reminder) "Reminder ✓" else "Reminder",
                        color = LocalContinuumPalette.current.muted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable {
                            onChange(settings.copy(calendarNotifyPrefs = settings.calendarNotifyPrefs + (cal.logicalId to prefs.copy(reminder = !prefs.reminder))))
                        },
                    )
                }
            }
        }
    }
    SettingsSectionTitle("Holiday pack")
    SettingsChoiceRow(
        label = "Public holidays",
        options = listOf(
            "none" to "None",
            "us" to "United States",
            "ca" to "Canada",
            "gb" to "United Kingdom",
            "de" to "Germany",
        ),
        selected = settings.holidayPack,
        onSelect = { onChange(settings.copy(holidayPack = it)) },
    )
    SettingsSectionTitle("Views")
    SettingsChoiceRow(
        label = "First day of week",
        options = weekdays.mapIndexed { i, n -> i.toString() to n },
        selected = settings.firstDayOfWeek.toString(),
        onSelect = { onChange(settings.copy(firstDayOfWeek = it.toIntOrNull() ?: 0)) },
    )
    SettingsStepperRow("Weekly view days", settings.weeklyViewDays, 1, 14) {
        onChange(settings.copy(weeklyViewDays = it))
    }
    SettingsCheckRow("Rolling week from today", settings.rollingWeekFromToday) {
        onChange(settings.copy(rollingWeekFromToday = !settings.rollingWeekFromToday))
    }
    SettingsCheckRow("Show empty days in agenda", settings.showEmptyDaysInAgenda) {
        onChange(settings.copy(showEmptyDaysInAgenda = !settings.showEmptyDaysInAgenda))
    }
    SettingsStepperRow("Agenda range (days)", settings.agendaRangeDays, 1, 90) {
        onChange(settings.copy(agendaRangeDays = it))
    }
    SettingsChoiceRow(
        label = "Agenda density",
        options = listOf("comfortable" to "Comfortable", "compact" to "Compact"),
        selected = settings.agendaDensity,
        onSelect = { onChange(settings.copy(agendaDensity = it)) },
    )
}

@Composable
private fun RemindersSection(settings: ContinuumSettings, onChange: (ContinuumSettings) -> Unit) {
    SettingsSectionTitle("Reminders")
    SettingsCheckRow("Enable notifications", settings.notificationEnabled) {
        onChange(settings.copy(notificationEnabled = !settings.notificationEnabled))
    }
    SettingsStepperRow("Default reminder (minutes)", settings.defaultReminderMinutes, 0, 10080) {
        onChange(settings.copy(defaultReminderMinutes = it))
    }
    SettingsStepperRow("Default snooze (minutes)", settings.defaultSnoozeMinutes, 1, 1440) {
        onChange(settings.copy(defaultSnoozeMinutes = it))
    }
    SettingsHint("OS notification delivery lands with P1-REMIND.")
}

@Composable
private fun SchedulingSection(settings: ContinuumSettings, onChange: (ContinuumSettings) -> Unit) {
    val hours = (0..23).map { h ->
        val label = h.toString().padStart(2, '0') + ":00"
        label to label
    }
    SettingsSectionTitle("Scheduling")
    SettingsChoiceRow(
        label = "Work day start",
        options = hours,
        selected = settings.workingHours.start.take(5),
        onSelect = { onChange(settings.copy(workingHours = WorkingHours(it, settings.workingHours.end))) },
    )
    SettingsChoiceRow(
        label = "Work day end",
        options = hours,
        selected = settings.workingHours.end.take(5),
        onSelect = { onChange(settings.copy(workingHours = WorkingHours(settings.workingHours.start, it))) },
    )
    SettingsStepperRow("Travel buffer (minutes)", settings.travelBufferMinutes, 0, 180) {
        onChange(settings.copy(travelBufferMinutes = it))
    }
    SettingsStepperRow("Minimum free slot (minutes)", settings.slotMinMinutes, 5, 240) {
        onChange(settings.copy(slotMinMinutes = it))
    }
}

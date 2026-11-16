package org.continuumcalendar.domain.model

data class WorkingHours(val start: String = "09:00", val end: String = "17:00")

data class CalendarNotifyPrefs(val newEvent: Boolean = true, val reminder: Boolean = true)

data class ContinuumSettings(
    val themeMode: String = "system",
    val showEmptyDaysInAgenda: Boolean = true,
    val rollingWeekFromToday: Boolean = true,
    val weeklyViewDays: Int = 7,
    val workingHours: WorkingHours = WorkingHours(),
    val defaultReminderMinutes: Int = 10,
    val notificationEnabled: Boolean = true,
    val defaultSnoozeMinutes: Int = 10,
    val visibleCalendarIds: List<String> = listOf("google:primary"),
    val defaultWriteCalendarId: String = "google:primary",
    val agendaRangeDays: Int = 30,
    val slotMinMinutes: Int = 30,
    val redactTitlesInScreenshots: Boolean = false,
    val travelBufferMinutes: Int = 0,
    val agendaDensity: String = "comfortable",
    val secondaryTimeZone: String? = null,
    val showContactBirthdays: Boolean = true,
    val use24HourFormat: Boolean = false,
    val firstDayOfWeek: Int = 0,
    val useGoogleCalendar: Boolean = true,
    val calendarNotifyPrefs: Map<String, CalendarNotifyPrefs> = emptyMap(),
    val weekHourDisplay: String = "scroll",
    val holidayPack: String = "none",
    val showGoogleTasks: Boolean = false,
)

data class SettingsUpdatedBy(
    val platform: String,
    val deviceId: String,
    val appVersion: String,
)

data class ContinuumSettingsEnvelope(
    val schemaVersion: Int = ContinuumConsts.SETTINGS_SCHEMA_VERSION,
    val revision: Long,
    val updatedAt: String,
    val updatedBy: SettingsUpdatedBy,
    val contentHash: String,
    val settings: ContinuumSettings,
)

data class LocalEventTombstone(
    val id: String,
    val calendarId: String,
    val deletedAt: String,
)

data class ContinuumLocalEventsEnvelope(
    val schemaVersion: Int = ContinuumConsts.LOCAL_EVENTS_SCHEMA_VERSION,
    val revision: Long,
    val updatedAt: String,
    val updatedBy: SettingsUpdatedBy,
    val contentHash: String,
    val calendars: List<CalendarListEntry>,
    val events: List<CalendarEvent>,
    val deletedIds: List<LocalEventTombstone>,
)

object ContinuumConsts {
    const val SETTINGS_SCHEMA_VERSION = 1
    const val SETTINGS_APP_DATA_FILENAME = "continuum-settings.json"
    const val SETTINGS_APP_DATA_PREV_FILENAME = "continuum-settings.prev.json"
    const val LOCAL_EVENTS_APP_DATA_FILENAME = "continuum-local-events.json"
    const val LOCAL_EVENTS_SCHEMA_VERSION = 1
}

fun defaultContinuumSettings(): ContinuumSettings = ContinuumSettings()

fun defaultCalendarNotifyPrefs(logicalId: String): CalendarNotifyPrefs {
    val isHoliday = logicalId.startsWith("holidays:")
    return CalendarNotifyPrefs(newEvent = !isHoliday, reminder = true)
}

fun normalizeFirstDayOfWeek(value: Int): Int = if (value in 0..6) value else 0

fun normalizeWeekHourDisplay(value: String?): String =
    when (value) {
        "fit24", "scroll", "agenda" -> value
        else -> "scroll"
    }

package org.fossify.calendar.continuum

import org.joda.time.DateTimeConstants

enum class CalendarSource {
    GOOGLE, CALDAV, LOCAL, ICS_IMPORT, HOLIDAYS;

    fun wire(): String = name.lowercase()
}

data class EventReminder(
    val minutes: Int,
    val method: String = "popup",
)

data class ContinuumWorkingHours(
    val start: String = "09:00",
    val end: String = "17:00",
)

data class CalendarNotifyPrefs(
    val newEvent: Boolean = true,
    val reminder: Boolean = true,
)

data class ContinuumSettings(
    val themeMode: String = "system",
    val showEmptyDaysInAgenda: Boolean = true,
    val rollingWeekFromToday: Boolean = true,
    val weeklyViewDays: Int = 7,
    val workingHours: ContinuumWorkingHours = ContinuumWorkingHours(),
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
    /** JS / FullCalendar: 0=Sunday … 6=Saturday */
    val firstDayOfWeek: Int = 0,
    val useGoogleCalendar: Boolean = true,
    val calendarNotifyPrefs: Map<String, CalendarNotifyPrefs> = emptyMap(),
)

data class SettingsUpdatedBy(
    val platform: String,
    val deviceId: String,
    val appVersion: String,
)

data class ContinuumSettingsEnvelope(
    val schemaVersion: Int = ContinuumConsts.SCHEMA_VERSION,
    val revision: Long = 1,
    val updatedAt: String,
    val updatedBy: SettingsUpdatedBy,
    val contentHash: String,
    val settings: ContinuumSettings,
)

object ContinuumConsts {
    const val SCHEMA_VERSION = 1
    const val APP_DATA_FILENAME = "continuum-settings.json"
    const val APP_DATA_PREV_FILENAME = "continuum-settings.prev.json"
    const val LOCAL_EVENTS_APP_DATA_FILENAME = "continuum-local-events.json"
    const val LOCAL_EVENTS_SCHEMA_VERSION = 1
    const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    const val CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar"
    const val CONTACTS_SCOPE = "https://www.googleapis.com/auth/contacts.readonly"
    const val TASKS_SCOPE = "https://www.googleapis.com/auth/tasks"

    fun logicalId(source: CalendarSource, calendarId: String): String =
        "${source.wire()}:$calendarId"

    /** Joda DateTimeConstants (Mon=1…Sun=7) → Continuum JS (Sun=0…Sat=6). */
    fun jodaToJsFirstDay(joda: Int): Int = when (joda) {
        DateTimeConstants.SUNDAY -> 0
        in DateTimeConstants.MONDAY..DateTimeConstants.SATURDAY -> joda
        else -> 0
    }

    /** Continuum JS (Sun=0…Sat=6) → Joda DateTimeConstants. */
    fun jsToJodaFirstDay(js: Int): Int = when (js) {
        0 -> DateTimeConstants.SUNDAY
        in 1..6 -> js
        else -> DateTimeConstants.SUNDAY
    }
}

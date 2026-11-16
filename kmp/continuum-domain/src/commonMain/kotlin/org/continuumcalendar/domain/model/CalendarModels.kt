package org.continuumcalendar.domain.model

enum class CalendarSource {
    GOOGLE, CALDAV, LOCAL, ICS_IMPORT, HOLIDAYS;

    fun wire(): String = name.lowercase()

    companion object {
        fun fromWire(value: String): CalendarSource =
            entries.firstOrNull { it.wire() == value.lowercase() } ?: LOCAL
    }
}

data class CalendarAttendee(
    val email: String,
    val displayName: String? = null,
    val responseStatus: String? = null,
    val optional: Boolean = false,
)

data class EventReminder(
    val minutes: Int,
    val method: String = "popup",
)

data class CalendarEvent(
    val id: String,
    val calendarId: String,
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val start: String,
    val end: String,
    val allDay: Boolean = false,
    val attendees: List<CalendarAttendee> = emptyList(),
    val reminders: List<EventReminder> = emptyList(),
    val source: CalendarSource? = null,
    val etag: String? = null,
    val updated: String? = null,
    val htmlLink: String? = null,
    val busy: Boolean = true,
    val readOnly: Boolean = false,
    val eventType: String? = null,
    val recurrence: List<String> = emptyList(),
    val recurringEventId: String? = null,
    val recurrenceExceptions: List<String> = emptyList(),
    val occurrenceStart: String? = null,
    val timeZone: String? = null,
    val visibility: String? = null,
    val color: String? = null,
)

data class CalendarListEntry(
    val id: String,
    val accountId: String,
    val displayName: String,
    val color: String,
    val visible: Boolean,
    val writable: Boolean,
    val source: CalendarSource,
    val logicalId: String,
)

data class FreeSlot(val startEpochMs: Long, val endEpochMs: Long)

data class AgendaDaySection(
    val date: String,
    val events: List<CalendarEvent>,
    val isEmpty: Boolean,
    val longestFreeSlot: FreeSlot? = null,
)

fun logicalCalendarId(source: CalendarSource, calendarId: String): String =
    "${source.wire()}:$calendarId"

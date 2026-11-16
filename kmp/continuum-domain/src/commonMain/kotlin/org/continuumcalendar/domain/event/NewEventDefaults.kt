package org.continuumcalendar.domain.event

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.continuumcalendar.domain.model.CalendarListEntry
import org.continuumcalendar.domain.model.ContinuumSettings

data class NewEventTimes(
    val calendarId: String,
    val startIso: String,
    val endIso: String,
    val allDay: Boolean = false,
)

fun defaultWriteCalendarId(settings: ContinuumSettings, calendars: List<CalendarListEntry>): String {
    val preferred = settings.defaultWriteCalendarId
    calendars.firstOrNull { it.logicalId == preferred && it.writable }?.let { return it.id }
    calendars.firstOrNull { it.writable && it.visible }?.let { return it.id }
    calendars.firstOrNull { it.writable }?.let { return it.id }
    return calendars.firstOrNull()?.id ?: "primary"
}

fun newEventAtNow(
    settings: ContinuumSettings,
    calendars: List<CalendarListEntry>,
    nowEpochMs: Long,
    durationMinutes: Int = 60,
): NewEventTimes {
    val calId = defaultWriteCalendarId(settings, calendars)
    val tz = TimeZone.currentSystemDefault()
    val local = Instant.fromEpochMilliseconds(nowEpochMs).toLocalDateTime(tz)
    val startMin = ((local.minute / 15) * 15).coerceAtMost(45)
    val start = LocalDateTime(local.date, LocalTime(local.hour, startMin))
    val endMs = start.toInstant(tz).toEpochMilliseconds() + durationMinutes * 60_000L
    val end = Instant.fromEpochMilliseconds(endMs).toLocalDateTime(tz)
    return NewEventTimes(calId, start.toIso(), end.toIso())
}

fun newEventOnOpenDay(
    settings: ContinuumSettings,
    calendars: List<CalendarListEntry>,
    dateKey: String,
    durationMinutes: Int = 60,
): NewEventTimes {
    val startHm = settings.workingHours.start.split(":")
    val hour = startHm.getOrNull(0)?.toIntOrNull() ?: 9
    val minute = startHm.getOrNull(1)?.toIntOrNull() ?: 0
    return newEventAtHalfHour(settings, calendars, dateKey, hour, minute, durationMinutes)
}

/** [minute] should be 0 or 30 for half-hour slots. */
fun newEventAtHalfHour(
    settings: ContinuumSettings,
    calendars: List<CalendarListEntry>,
    dateKey: String,
    hour: Int,
    minute: Int,
    durationMinutes: Int = 30,
): NewEventTimes {
    val calId = defaultWriteCalendarId(settings, calendars)
    val date = LocalDate.parse(dateKey)
    val tz = TimeZone.currentSystemDefault()
    val start = LocalDateTime(
        date,
        LocalTime(hour.coerceIn(0, 23), minute.coerceIn(0, 59)),
    )
    val endMs = start.toInstant(tz).toEpochMilliseconds() + durationMinutes * 60_000L
    val end = Instant.fromEpochMilliseconds(endMs).toLocalDateTime(tz)
    return NewEventTimes(calId, start.toIso(), end.toIso())
}

private fun LocalDateTime.toIso(): String {
    val h = hour.toString().padStart(2, '0')
    val m = minute.toString().padStart(2, '0')
    val s = second.toString().padStart(2, '0')
    return "${date}T$h:$m:$s"
}

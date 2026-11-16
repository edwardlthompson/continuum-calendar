package org.continuumcalendar.domain.event

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

data class EventTimeBounds(
    val start: String,
    val end: String,
    val allDay: Boolean,
)

/**
 * Convert between all-day (YYYY-MM-DD) and timed (local ISO datetime) start/end shapes.
 * Timed → all-day keeps the date portion; all-day → timed uses 09:00–10:00 local on that date.
 */
fun convertAllDayBounds(
    start: String,
    end: String,
    allDay: Boolean,
    targetAllDay: Boolean,
): EventTimeBounds {
    if (allDay == targetAllDay) {
        return EventTimeBounds(start = start, end = end, allDay = allDay)
    }
    return if (targetAllDay) {
        val startDate = start.take(10)
        val endDate = end.take(10).ifBlank { startDate }
        EventTimeBounds(start = startDate, end = endDate, allDay = true)
    } else {
        val startDate = runCatching { LocalDate.parse(start.take(10)) }.getOrNull()
            ?: return EventTimeBounds(start = start, end = end, allDay = false)
        val endDate = runCatching { LocalDate.parse(end.take(10)) }.getOrNull() ?: startDate
        val startDt = LocalDateTime(startDate, LocalTime(9, 0))
        val endDt = LocalDateTime(endDate, LocalTime(10, 0))
        EventTimeBounds(start = startDt.toIsoLocal(), end = endDt.toIsoLocal(), allDay = false)
    }
}

private fun LocalDateTime.toIsoLocal(): String {
    val h = hour.toString().padStart(2, '0')
    val m = minute.toString().padStart(2, '0')
    val s = second.toString().padStart(2, '0')
    return "${date}T$h:$m:$s"
}

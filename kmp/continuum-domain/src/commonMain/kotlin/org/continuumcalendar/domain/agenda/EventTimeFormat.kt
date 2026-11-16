package org.continuumcalendar.domain.agenda

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.continuumcalendar.domain.model.CalendarEvent

fun formatEventTimeRange(
    event: CalendarEvent,
    use24Hour: Boolean,
): String {
    if (isAllDayEvent(event)) return "All day"
    val start = formatClock(event.start, use24Hour) ?: return event.start
    val end = formatClock(event.end, use24Hour) ?: return start
    return "$start – $end"
}

private fun formatClock(iso: String, use24Hour: Boolean): String? {
    val trimmed = iso.trim()
    if (Regex("""^\d{4}-\d{2}-\d{2}$""").matches(trimmed)) return null
    val instant = runCatching { Instant.parse(trimmed) }.getOrNull() ?: return null
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val h = local.hour
    val m = local.minute.toString().padStart(2, '0')
    if (use24Hour) return "${h.toString().padStart(2, '0')}:$m"
    val ampm = if (h >= 12) "PM" else "AM"
    val h12 = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
    }
    return "$h12:$m $ampm"
}

package org.fossify.calendar.continuum

import android.provider.CalendarContract
import org.fossify.calendar.models.Attendee
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

/**
 * Pure formatting for Google-style Android event preview (view mode).
 * Keeps EventActivity thin; unit-tested without Activity.
 */
object ContinuumEventPreviewFormat {
    private const val REMINDER_OFF = -1

    data class OrganizerRow(val primary: String, val secondary: String?)

    /** "Thursday, Oct 15 • 10:30–15:30" or "Tomorrow • 13:00–19:00" or all-day. */
    fun formatWhenLine(
        startTsSeconds: Long,
        endTsSeconds: Long,
        allDay: Boolean,
        use24Hour: Boolean,
        now: DateTime = DateTime.now(),
        zone: DateTimeZone = DateTimeZone.getDefault(),
    ): String {
        val start = DateTime(startTsSeconds * 1000L, zone)
        val end = DateTime(endTsSeconds * 1000L, zone)
        val datePart = formatRelativeDate(start, now, zone)
        if (allDay) {
            val startDay = start.toLocalDate()
            val endDay = end.toLocalDate()
            val exclusiveEndNextMorning =
                endDay == startDay.plusDays(1) && end.millisOfDay == 0
            val sameDay = startDay == endDay || exclusiveEndNextMorning
            return if (sameDay) {
                "$datePart • All day"
            } else {
                val endDate = formatRelativeDate(
                    if (end.millisOfDay == 0) end.minusMillis(1) else end,
                    now,
                    zone,
                )
                "$datePart – $endDate • All day"
            }
        }
        val startTime = formatTime(start, use24Hour)
        val endTime = formatTime(end, use24Hour)
        return "$datePart • $startTime–$endTime"
    }

    fun formatReminderLabel(minutes: Int): String? {
        if (minutes == REMINDER_OFF) return null
        return when {
            minutes == 0 -> "At time of event"
            minutes < 60 -> "$minutes minutes before"
            minutes % 60 == 0 -> {
                val hours = minutes / 60
                if (hours == 1) "1 hour before" else "$hours hours before"
            }
            else -> "$minutes minutes before"
        }
    }

    fun reminderLabels(minutes: List<Int>): List<String> =
        minutes.mapNotNull { formatReminderLabel(it) }

    fun organizerRow(attendees: List<Attendee>): OrganizerRow? {
        val organizer = attendees.firstOrNull {
            it.relationship == CalendarContract.Attendees.RELATIONSHIP_ORGANIZER
        } ?: return null
        val name = organizer.getPublicName().trim()
        if (name.isEmpty()) return null
        val email = organizer.email.trim().takeIf { it.isNotEmpty() && it != name }
        return OrganizerRow(
            primary = "Created by $name",
            secondary = email,
        )
    }

    private fun formatRelativeDate(instant: DateTime, now: DateTime, zone: DateTimeZone): String {
        val day = instant.withZone(zone).toLocalDate()
        val today = now.withZone(zone).toLocalDate()
        return when (day) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            today.minusDays(1) -> "Yesterday"
            else -> {
                val pattern = if (day.year == today.year) "EEEE, MMM d" else "EEEE, MMM d, yyyy"
                instant.withZone(zone).toString(pattern)
            }
        }
    }

    private fun formatTime(instant: DateTime, use24Hour: Boolean): String {
        val pattern = if (use24Hour) "HH:mm" else "h:mm a"
        return instant.toString(pattern)
    }

    /** Visible when non-blank after trim. */
    fun nonBlank(value: String?): String? = value?.trim()?.takeIf { it.isNotEmpty() }

    fun calendarSubtitle(email: String?): String? = nonBlank(email)
}

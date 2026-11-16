package org.continuumcalendar.domain.remind

/** Human label for an event reminder offset (minutes before start). */
fun formatReminderMinutes(minutes: Int): String {
    if (minutes <= 0) return "At time of event"
    if (minutes < 60) return "$minutes min before"
    if (minutes % 60 == 0) return "${minutes / 60} hr before"
    return "$minutes min before"
}

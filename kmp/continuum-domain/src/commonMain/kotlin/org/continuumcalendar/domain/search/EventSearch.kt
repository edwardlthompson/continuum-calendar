package org.continuumcalendar.domain.search

import org.continuumcalendar.domain.model.CalendarEvent

fun filterEventsByQuery(events: List<CalendarEvent>, query: String): List<CalendarEvent> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return events
    return events.filter { ev ->
        ev.title.lowercase().contains(q) ||
            (ev.location?.lowercase()?.contains(q) == true) ||
            (ev.description?.lowercase()?.contains(q) == true)
    }
}

/** Accepts YYYY-MM-DD or partial; returns normalized date key or null. */
fun parseGoToDateKey(raw: String): String? {
    val t = raw.trim()
    if (Regex("""^\d{4}-\d{2}-\d{2}$""").matches(t)) return t
    if (Regex("""^\d{4}/\d{2}/\d{2}$""").matches(t)) return t.replace('/', '-')
    return null
}

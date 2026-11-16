package org.continuumcalendar.domain.conflict

import org.continuumcalendar.domain.agenda.eventEndMs
import org.continuumcalendar.domain.agenda.isAllDayEvent
import org.continuumcalendar.domain.agenda.toDateKey
import org.continuumcalendar.domain.model.CalendarEvent
import kotlinx.datetime.Instant

data class EventOverlap(
    val left: CalendarEvent,
    val right: CalendarEvent,
)

fun eventStartMs(event: CalendarEvent): Long {
    if (isAllDayEvent(event)) {
        val key = toDateKey(event.start)
        return org.continuumcalendar.domain.agenda.workingHoursEndMs(key, "00:00")
    }
    return runCatching {
        Instant.parse(event.start).toEpochMilliseconds()
    }.getOrDefault(0L)
}

/** Timed busy overlaps only (all-day ignored), same calendar day preferred. */
fun findOverlappingPairs(events: List<CalendarEvent>): List<EventOverlap> {
    val timed = events.filter { !isAllDayEvent(it) && it.busy }
        .sortedBy { eventStartMs(it) }
    val out = mutableListOf<EventOverlap>()
    for (i in timed.indices) {
        val a = timed[i]
        val aStart = eventStartMs(a)
        val aEnd = eventEndMs(a)
        if (aEnd <= aStart) continue
        for (j in i + 1 until timed.size) {
            val b = timed[j]
            val bStart = eventStartMs(b)
            if (bStart >= aEnd) break
            val bEnd = eventEndMs(b)
            if (bEnd <= bStart) continue
            if (aStart < bEnd && bStart < aEnd) {
                out += EventOverlap(a, b)
            }
        }
    }
    return out
}

fun conflictedEventIds(events: List<CalendarEvent>): Set<String> {
    val ids = mutableSetOf<String>()
    for (pair in findOverlappingPairs(events)) {
        ids += pair.left.id
        ids += pair.right.id
    }
    return ids
}

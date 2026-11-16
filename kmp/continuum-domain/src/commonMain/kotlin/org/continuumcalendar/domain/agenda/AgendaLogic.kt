package org.continuumcalendar.domain.agenda

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.continuumcalendar.domain.model.AgendaDaySection
import org.continuumcalendar.domain.model.CalendarEvent

enum class TodayAgendaPhase { ACTIVE, OPEN, EMPTY }

private val DATE_ONLY = Regex("""^\d{4}-\d{2}-\d{2}$""")

fun isAllDayEvent(event: CalendarEvent): Boolean {
    if (event.allDay) return true
    return DATE_ONLY.matches(event.start.trim())
}

fun dayHasTimedEvents(events: List<CalendarEvent>): Boolean =
    events.any { !isAllDayEvent(it) }

fun dayShouldShowOpen(events: List<CalendarEvent>): Boolean =
    events.isEmpty() || !dayHasTimedEvents(events)

fun buildAgendaSections(
    events: List<CalendarEvent>,
    rangeStart: String,
    rangeEnd: String,
    showEmptyDays: Boolean,
): List<AgendaDaySection> {
    val byDay = linkedMapOf<String, MutableList<CalendarEvent>>()
    for (ev in events) {
        val key = toDateKey(ev.start)
        byDay.getOrPut(key) { mutableListOf() }.add(ev)
    }
    val sections = mutableListOf<AgendaDaySection>()
    var cursor = rangeStart
    while (cursor <= rangeEnd) {
        val dayEvents = (byDay[cursor] ?: emptyList()).sortedWith(
            compareBy({ it.start }, { it.end }),
        )
        val isEmpty = dayEvents.isEmpty()
        if (!isEmpty || showEmptyDays) {
            sections += AgendaDaySection(date = cursor, events = dayEvents, isEmpty = isEmpty)
        }
        cursor = addDays(cursor, 1)
    }
    return sections
}

fun todayAgendaPhase(
    events: List<CalendarEvent>,
    nowMs: Long,
    workingHoursEnd: String,
    todayKey: String = localDateKey(nowMs),
): TodayAgendaPhase {
    val future = events.filter { !isEventPast(it, nowMs) }
    if (future.isNotEmpty()) return TodayAgendaPhase.ACTIVE
    return if (nowMs < workingHoursEndMs(todayKey, workingHoursEnd)) {
        TodayAgendaPhase.OPEN
    } else {
        TodayAgendaPhase.EMPTY
    }
}

fun isEventPast(event: CalendarEvent, nowMs: Long): Boolean = eventEndMs(event) < nowMs

fun eventEndMs(event: CalendarEvent): Long {
    if (isAllDayEvent(event)) {
        val endKey = toDateKey(event.end.ifBlank { event.start })
        return workingHoursEndMs(endKey, "23:59") + 999
    }
    return runCatching {
        Instant.parse(event.end).toEpochMilliseconds()
    }.getOrDefault(0L)
}

fun workingHoursEndMs(dateKey: String, workingHoursEnd: String): Long {
    val m = Regex("""^(\d{1,2}):(\d{2})$""").find(workingHoursEnd.trim().ifBlank { "17:00" })
    val h = m?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(0, 23) ?: 17
    val min = m?.groupValues?.get(2)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    val date = LocalDate.parse(dateKey)
    val local = LocalDateTime(date.year, date.monthNumber, date.dayOfMonth, h, min, 0, 0)
    return local.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}

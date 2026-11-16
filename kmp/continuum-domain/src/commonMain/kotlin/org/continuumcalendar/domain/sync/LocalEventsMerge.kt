package org.continuumcalendar.domain.sync

import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.CalendarListEntry
import org.continuumcalendar.domain.model.CalendarSource
import org.continuumcalendar.domain.model.LocalEventTombstone

fun eventKey(calendarId: String, id: String): String = "$calendarId:$id"

fun filterContinuumOwnedCalendars(cals: List<CalendarListEntry>): List<CalendarListEntry> =
    cals.filter { it.source == CalendarSource.LOCAL || it.source == CalendarSource.ICS_IMPORT }

fun filterContinuumOwnedEvents(events: List<CalendarEvent>): List<CalendarEvent> =
    events.filter {
        it.source == null ||
            it.source == CalendarSource.LOCAL ||
            it.source == CalendarSource.ICS_IMPORT
    }

private fun eventUpdatedMs(ev: CalendarEvent): Long {
    ev.updated?.let { updated ->
        runCatching {
            kotlinx.datetime.Instant.parse(updated).toEpochMilliseconds()
        }.getOrNull()?.let { return it }
    }
    return runCatching {
        kotlinx.datetime.Instant.parse(ev.start).toEpochMilliseconds()
    }.getOrDefault(0L)
}

private fun tombstoneMs(t: LocalEventTombstone): Long =
    runCatching {
        kotlinx.datetime.Instant.parse(t.deletedAt).toEpochMilliseconds()
    }.getOrDefault(0L)

data class LocalEventsMergeResult(
    val calendars: List<CalendarListEntry>,
    val events: List<CalendarEvent>,
    val deletedIds: List<LocalEventTombstone>,
)

/** LWW by `updated`; tombstones win when newer. */
fun mergeLocalEventsPayload(
    baseCalendars: List<CalendarListEntry>,
    baseEvents: List<CalendarEvent>,
    baseDeleted: List<LocalEventTombstone>,
    incomingCalendars: List<CalendarListEntry>,
    incomingEvents: List<CalendarEvent>,
    incomingDeleted: List<LocalEventTombstone>,
): LocalEventsMergeResult {
    val calMap = linkedMapOf<String, CalendarListEntry>()
    for (c in filterContinuumOwnedCalendars(baseCalendars) +
        filterContinuumOwnedCalendars(incomingCalendars)
    ) {
        calMap[c.logicalId] = c
    }

    val tombMap = linkedMapOf<String, LocalEventTombstone>()
    for (t in baseDeleted + incomingDeleted) {
        val key = eventKey(t.calendarId, t.id)
        val prev = tombMap[key]
        if (prev == null || tombstoneMs(t) >= tombstoneMs(prev)) {
            tombMap[key] = t
        }
    }

    val evMap = linkedMapOf<String, CalendarEvent>()
    for (ev in filterContinuumOwnedEvents(baseEvents) + filterContinuumOwnedEvents(incomingEvents)) {
        val key = eventKey(ev.calendarId, ev.id)
        val tomb = tombMap[key]
        if (tomb != null && tombstoneMs(tomb) >= eventUpdatedMs(ev)) continue
        val prev = evMap[key]
        if (prev == null || eventUpdatedMs(ev) >= eventUpdatedMs(prev)) {
            evMap[key] = ev
            tombMap.remove(key)
        }
    }

    return LocalEventsMergeResult(
        calendars = calMap.values.toList(),
        events = evMap.values.toList(),
        deletedIds = tombMap.values.toList(),
    )
}

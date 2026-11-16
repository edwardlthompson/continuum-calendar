package org.continuumcalendar.domain.calendar

import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.CalendarListEntry
import org.continuumcalendar.domain.model.ContinuumSettings

/** Apply settings.visibleCalendarIds onto calendar entries (empty list = all visible). */
fun calendarsWithVisibility(
    calendars: List<CalendarListEntry>,
    settings: ContinuumSettings,
): List<CalendarListEntry> {
    val ids = settings.visibleCalendarIds
    if (ids.isEmpty()) return calendars.map { it.copy(visible = true) }
    val set = ids.toSet()
    return calendars.map { cal ->
        cal.copy(visible = cal.logicalId in set || cal.id in set)
    }
}

fun visibleEvents(
    events: List<CalendarEvent>,
    calendars: List<CalendarListEntry>,
): List<CalendarEvent> {
    val visibleIds = calendars.filter { it.visible }.map { it.id }.toSet()
    if (visibleIds.isEmpty()) return emptyList()
    return events.filter { it.calendarId in visibleIds }
}

fun toggleCalendarVisible(
    settings: ContinuumSettings,
    calendars: List<CalendarListEntry>,
    calendarId: String,
    visible: Boolean,
): ContinuumSettings {
    val nextCals = calendars.map { c ->
        if (c.id == calendarId || c.logicalId == calendarId) c.copy(visible = visible) else c
    }
    return settings.copy(
        visibleCalendarIds = nextCals.filter { it.visible }.map { it.logicalId },
    )
}

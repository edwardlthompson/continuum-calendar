package org.continuumcalendar.ui.demo

import org.continuumcalendar.domain.agenda.addDays
import org.continuumcalendar.domain.agenda.localDateKey
import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.CalendarListEntry
import org.continuumcalendar.domain.model.CalendarSource
import org.continuumcalendar.domain.model.logicalCalendarId

fun demoCalendars(): List<CalendarListEntry> {
    val id = "primary"
    return listOf(
        CalendarListEntry(
            id = id,
            accountId = "local",
            displayName = "Personal",
            color = "#0F6E8C",
            visible = true,
            writable = true,
            source = CalendarSource.LOCAL,
            logicalId = logicalCalendarId(CalendarSource.LOCAL, id),
        ),
    )
}

fun demoEvents(nowEpochMs: Long): List<CalendarEvent> {
    val today = localDateKey(nowEpochMs)
    val cal = "primary"
    val day2 = addDays(today, 2)
    val day3 = addDays(today, 3)
    return listOf(
        CalendarEvent(
            id = "1",
            calendarId = cal,
            title = "Welcome to Continuum",
            start = "${today}T09:00:00",
            end = "${today}T09:30:00",
            source = CalendarSource.LOCAL,
        ),
        CalendarEvent(
            id = "2",
            calendarId = cal,
            title = "Deep work",
            start = "${today}T14:00:00",
            end = "${today}T15:30:00",
            source = CalendarSource.LOCAL,
        ),
        CalendarEvent(
            id = "3",
            calendarId = cal,
            title = "Team sync",
            start = "${day2}T11:00:00",
            end = "${day2}T12:00:00",
            source = CalendarSource.LOCAL,
        ),
        CalendarEvent(
            id = "4",
            calendarId = cal,
            title = "All-day focus",
            start = day3,
            end = day3,
            allDay = true,
            source = CalendarSource.LOCAL,
        ),
    )
}

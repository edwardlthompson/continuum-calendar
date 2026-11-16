package org.continuumcalendar.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.continuumcalendar.domain.calendar.calendarsWithVisibility
import org.continuumcalendar.domain.calendar.toggleCalendarVisible
import org.continuumcalendar.domain.calendar.visibleEvents
import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.CalendarListEntry
import org.continuumcalendar.domain.model.CalendarSource
import org.continuumcalendar.domain.model.ContinuumSettings
import org.continuumcalendar.domain.model.logicalCalendarId

class VisibleCalendarsTest {
    private val primary = CalendarListEntry(
        id = "primary",
        accountId = "google",
        displayName = "Primary",
        color = "#0F6E8C",
        visible = true,
        writable = true,
        source = CalendarSource.GOOGLE,
        logicalId = logicalCalendarId(CalendarSource.GOOGLE, "primary"),
    )
    private val work = primary.copy(
        id = "work@x",
        displayName = "Work",
        logicalId = logicalCalendarId(CalendarSource.GOOGLE, "work@x"),
    )

    @Test
    fun emptyVisibleIdsMeansAllVisible() {
        val out = calendarsWithVisibility(listOf(primary, work), ContinuumSettings(visibleCalendarIds = emptyList()))
        assertTrue(out.all { it.visible })
    }

    @Test
    fun filtersByLogicalId() {
        val settings = ContinuumSettings(visibleCalendarIds = listOf(primary.logicalId))
        val out = calendarsWithVisibility(listOf(primary, work), settings)
        assertTrue(out.first { it.id == "primary" }.visible)
        assertFalse(out.first { it.id == "work@x" }.visible)
        val events = listOf(
            CalendarEvent("1", "primary", "A", start = "2026-11-16T09:00:00Z", end = "2026-11-16T10:00:00Z"),
            CalendarEvent("2", "work@x", "B", start = "2026-11-16T11:00:00Z", end = "2026-11-16T12:00:00Z"),
        )
        assertEquals(listOf("1"), visibleEvents(events, out).map { it.id })
    }

    @Test
    fun toggleUpdatesVisibleIds() {
        val settings = ContinuumSettings(visibleCalendarIds = listOf(primary.logicalId, work.logicalId))
        val next = toggleCalendarVisible(settings, listOf(primary, work), "work@x", false)
        assertEquals(listOf(primary.logicalId), next.visibleCalendarIds)
    }
}

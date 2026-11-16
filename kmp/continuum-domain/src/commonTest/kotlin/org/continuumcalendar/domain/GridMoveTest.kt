package org.continuumcalendar.domain

import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.CalendarListEntry
import org.continuumcalendar.domain.model.CalendarSource
import org.continuumcalendar.domain.week.applyVerticalDrag
import org.continuumcalendar.domain.week.applyVerticalResize
import org.continuumcalendar.domain.week.formatLocalIso
import org.continuumcalendar.domain.week.isGridEventEditable
import org.continuumcalendar.domain.week.pixelDeltaToMinutes
import org.continuumcalendar.domain.week.snapToStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GridMoveTest {
    private val writable = CalendarListEntry(
        id = "primary",
        accountId = "google",
        displayName = "Primary",
        color = "#0F6E8C",
        visible = true,
        writable = true,
        source = CalendarSource.GOOGLE,
        logicalId = "google:primary",
    )

    @Test
    fun editableRequiresWritableCalendar() {
        val ev = CalendarEvent(
            id = "1",
            calendarId = "primary",
            title = "Meet",
            start = "2026-11-16T12:00:00",
            end = "2026-11-16T13:00:00",
        )
        assertTrue(isGridEventEditable(ev, listOf(writable)))
        assertFalse(
            isGridEventEditable(
                ev.copy(readOnly = true),
                listOf(writable),
            ),
        )
        assertFalse(
            isGridEventEditable(
                ev,
                listOf(writable.copy(writable = false)),
            ),
        )
        assertFalse(
            isGridEventEditable(
                ev.copy(start = "2026-11-16", end = "2026-11-17", allDay = true),
                listOf(writable),
            ),
        )
    }

    @Test
    fun snapsAndMovesPreservingDuration() {
        assertEquals(45, snapToStep(40, 15))
        assertEquals(60, pixelDeltaToMinutes(dayHeightPx = 1440f, deltaYPx = 60f))
        val ev = CalendarEvent(
            id = "1",
            calendarId = "primary",
            title = "Meet",
            start = "2026-11-16T10:00:00",
            end = "2026-11-16T11:00:00",
        )
        val moved = applyVerticalDrag(ev, deltaMinutes = 30, snapMinutes = 15)
        assertNotNull(moved)
        assertEquals("2026-11-16T10:30:00", moved.start)
        assertEquals("2026-11-16T11:30:00", moved.end)
    }

    @Test
    fun zeroDeltaReturnsNull() {
        val ev = CalendarEvent(
            id = "1",
            calendarId = "primary",
            title = "Meet",
            start = "2026-11-16T10:00:00",
            end = "2026-11-16T11:00:00",
        )
        assertNull(applyVerticalDrag(ev, deltaMinutes = 0, snapMinutes = 15))
    }

    @Test
    fun formatLocalIsoPads() {
        assertEquals("2026-11-16T09:05:00", formatLocalIso("2026-11-16", 9 * 60 + 5))
    }

    @Test
    fun resizeMovesEndOnly() {
        val ev = CalendarEvent(
            id = "1",
            calendarId = "primary",
            title = "Meet",
            start = "2026-11-16T10:00:00",
            end = "2026-11-16T11:00:00",
        )
        val resized = applyVerticalResize(ev, deltaMinutes = 30, snapMinutes = 15)
        assertNotNull(resized)
        assertEquals("2026-11-16T10:00:00", resized.start)
        assertEquals("2026-11-16T11:30:00", resized.end)
    }
}

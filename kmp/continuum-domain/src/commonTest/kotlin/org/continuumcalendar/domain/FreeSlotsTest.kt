package org.continuumcalendar.domain

import kotlinx.datetime.TimeZone
import org.continuumcalendar.domain.agenda.formatFreeSlotHint
import org.continuumcalendar.domain.agenda.longestFreeSlotForDay
import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.CalendarSource
import org.continuumcalendar.domain.model.ContinuumSettings
import org.continuumcalendar.domain.model.WorkingHours
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FreeSlotsTest {
    private val tz = TimeZone.of("America/New_York")
    private val settings = ContinuumSettings(
        workingHours = WorkingHours("09:00", "17:00"),
        slotMinMinutes = 30,
        travelBufferMinutes = 15,
    )

    @Test
    fun emptyDayYieldsFullWorkingWindow() {
        val slot = longestFreeSlotForDay("2026-11-16", emptyList(), settings, tz)
        assertNotNull(slot)
        val mins = (slot.endEpochMs - slot.startEpochMs) / 60_000L
        assertEquals(8 * 60L, mins)
        assertEquals("Free 8h", formatFreeSlotHint(slot, use24 = true))
    }

    @Test
    fun busyBlockShrinksSlotAndRespectsBufferAndMin() {
        val events = listOf(
            CalendarEvent(
                id = "1",
                calendarId = "c",
                title = "Meet",
                start = "2026-11-16T14:00:00-05:00",
                end = "2026-11-16T15:00:00-05:00",
                source = CalendarSource.LOCAL,
            ),
        )
        val slot = longestFreeSlotForDay("2026-11-16", events, settings, tz)
        assertNotNull(slot)
        // Morning free: 09:00–13:45 (buffer before 14:00) = 4h45m beats afternoon 15:15–17:00
        val mins = (slot.endEpochMs - slot.startEpochMs) / 60_000L
        assertEquals(4 * 60L + 45L, mins)
        assertTrue(formatFreeSlotHint(slot, use24 = true)!!.contains("4h"))
    }

    @Test
    fun slotBelowMinimumReturnsNullWhenPacked() {
        val packed = ContinuumSettings(
            workingHours = WorkingHours("09:00", "10:00"),
            slotMinMinutes = 90,
            travelBufferMinutes = 0,
        )
        val events = listOf(
            CalendarEvent(
                id = "1",
                calendarId = "c",
                title = "Block",
                start = "2026-11-16T09:00:00-05:00",
                end = "2026-11-16T10:00:00-05:00",
                source = CalendarSource.LOCAL,
            ),
        )
        assertNull(longestFreeSlotForDay("2026-11-16", events, packed, tz))
    }
}

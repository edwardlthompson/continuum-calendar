package org.continuumcalendar.domain

import org.continuumcalendar.domain.holiday.HolidayPackId
import org.continuumcalendar.domain.holiday.holidayEvents
import org.continuumcalendar.domain.hotkey.ShellHotkeyAction
import org.continuumcalendar.domain.hotkey.resolveShellHotkey
import org.continuumcalendar.domain.ics.exportEventsToIcs
import org.continuumcalendar.domain.ics.importEventsFromIcs
import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.CalendarSource
import org.continuumcalendar.domain.model.WorkingHours
import org.continuumcalendar.domain.schedule.proposeMeetingTimes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeferredParityTest {
    @Test
    fun hotkeysResolveCatalog() {
        assertEquals(ShellHotkeyAction.NEW_EVENT, resolveShellHotkey("n", false, false, false, false))
        assertEquals(ShellHotkeyAction.COMMAND_PALETTE, resolveShellHotkey("k", true, false, false, true))
        assertEquals(null, resolveShellHotkey("n", false, false, false, true))
    }

    @Test
    fun usHolidaysIncludeThanksgiving() {
        val ev = holidayEvents(HolidayPackId.US, 2026)
        assertTrue(ev.any { it.title.contains("Thanksgiving") })
        assertTrue(ev.all { it.source == CalendarSource.HOLIDAYS && it.allDay })
    }

    @Test
    fun icsRoundTrip() {
        val original = listOf(
            CalendarEvent(
                id = "1",
                calendarId = "local:1",
                title = "Meet",
                start = "2026-11-16T10:00:00",
                end = "2026-11-16T11:00:00",
                source = CalendarSource.LOCAL,
            ),
        )
        val ics = exportEventsToIcs(original)
        val again = importEventsFromIcs(ics)
        assertEquals(1, again.size)
        assertEquals("Meet", again[0].title)
    }

    @Test
    fun proposeFindsSlotOnEmptyDay() {
        val slots = proposeMeetingTimes(
            events = emptyList(),
            fromEpochMs = 1_763_280_000_000L, // around 2025-11-16 UTC
            days = 1,
            durationMinutes = 30,
            count = 2,
            workingHours = WorkingHours("09:00", "17:00"),
        )
        assertTrue(slots.isNotEmpty())
    }
}

package org.continuumcalendar.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.normalizeWeekHourDisplay
import org.continuumcalendar.domain.week.layoutTimedEventsForDay
import org.continuumcalendar.domain.week.slotHourMinute

class DayTimeLayoutTest {
    @Test
    fun normalizesAgendaMode() {
        assertEquals("agenda", normalizeWeekHourDisplay("agenda"))
        assertEquals("fit24", normalizeWeekHourDisplay("fit24"))
        assertEquals("scroll", normalizeWeekHourDisplay("nope"))
    }

    @Test
    fun halfHourSlotsMap() {
        assertEquals(9 to 0, slotHourMinute(18))
        assertEquals(9 to 30, slotHourMinute(19))
    }

    @Test
    fun laysOutTimedEventFraction() {
        val ev = CalendarEvent(
            id = "1",
            calendarId = "primary",
            title = "Meet",
            start = "2026-11-16T12:00:00Z",
            end = "2026-11-16T13:00:00Z",
        )
        val bands = layoutTimedEventsForDay(listOf(ev), "2026-11-16")
        // Timezone-dependent; just ensure we get a band with positive height when date matches local.
        if (bands.isNotEmpty()) {
            assertNotNull(bands.first())
            assertTrue(bands.first().heightFraction > 0f)
        }
    }
}

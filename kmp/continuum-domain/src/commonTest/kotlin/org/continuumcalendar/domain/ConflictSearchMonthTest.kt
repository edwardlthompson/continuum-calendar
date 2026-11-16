package org.continuumcalendar.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.continuumcalendar.domain.conflict.conflictedEventIds
import org.continuumcalendar.domain.conflict.findOverlappingPairs
import org.continuumcalendar.domain.month.monthCellKeys
import org.continuumcalendar.domain.month.shiftYearMonth
import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.search.filterEventsByQuery
import org.continuumcalendar.domain.search.parseGoToDateKey

class ConflictSearchMonthTest {
    private fun ev(id: String, start: String, end: String) = CalendarEvent(
        id = id,
        calendarId = "primary",
        title = "E$id",
        start = start,
        end = end,
    )

    @Test
    fun detectsTimedOverlap() {
        val events = listOf(
            ev("a", "2026-11-16T09:00:00Z", "2026-11-16T10:00:00Z"),
            ev("b", "2026-11-16T09:30:00Z", "2026-11-16T10:30:00Z"),
            ev("c", "2026-11-16T11:00:00Z", "2026-11-16T12:00:00Z"),
        )
        assertEquals(1, findOverlappingPairs(events).size)
        assertEquals(setOf("a", "b"), conflictedEventIds(events))
    }

    @Test
    fun searchFiltersTitleAndLocation() {
        val events = listOf(
            ev("1", "2026-11-16T09:00:00Z", "2026-11-16T10:00:00Z").copy(title = "Standup"),
            ev("2", "2026-11-16T11:00:00Z", "2026-11-16T12:00:00Z").copy(title = "Lunch", location = "Cafe"),
        )
        assertEquals(listOf("2"), filterEventsByQuery(events, "cafe").map { it.id })
        assertEquals(listOf("1"), filterEventsByQuery(events, "stand").map { it.id })
    }

    @Test
    fun goToParsesIsoDate() {
        assertEquals("2026-11-16", parseGoToDateKey("2026-11-16"))
        assertEquals("2026-11-16", parseGoToDateKey("2026/11/16"))
        assertNull(parseGoToDateKey("next monday"))
    }

    @Test
    fun monthGridHas42Cells() {
        val cells = monthCellKeys(2026, 11, firstDayOfWeek = 0)
        assertEquals(42, cells.size)
        assertTrue(cells.count { it != null } in 28..31)
        assertEquals("2027-01", shiftYearMonth("2026-12", 1))
    }
}

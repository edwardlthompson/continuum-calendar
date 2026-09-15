package org.fossify.calendar.continuum

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RollingWeekTest {

    @Test
    fun rollingStartsToday() {
        assertEquals(
            listOf("20260911", "20260912", "20260913", "20260914", "20260915", "20260916", "20260917"),
            RollingWeek.dayCodes("20260911", rolling = true),
        )
    }

    @Test
    fun calendarWeekStartsMonday() {
        val friday = LocalDate.of(2026, 9, 11)
        assertEquals(LocalDate.of(2026, 9, 7), RollingWeek.startDate(friday, rolling = false, isoWeekStart = 1))
    }

    @Test
    fun cellLabelUsesOpenWhenEmpty() {
        assertEquals("Open", RollingWeek.cellLabel(0, "Open"))
        assertEquals("3", RollingWeek.cellLabel(3, "Open"))
    }

    @Test
    fun cellCountSizeKeepsOpenOnOneLine() {
        assertEquals(16f, RollingWeek.cellCountSizeSp("1"), 0f)
        assertEquals(16f, RollingWeek.cellCountSizeSp("12"), 0f)
        assertEquals(10f, RollingWeek.cellCountSizeSp("Open"), 0f)
        assertEquals(8f, RollingWeek.cellCountSizeSp("Libre"), 0f)
    }
}

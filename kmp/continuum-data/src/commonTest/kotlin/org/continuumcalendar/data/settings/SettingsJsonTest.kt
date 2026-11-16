package org.continuumcalendar.data.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.continuumcalendar.domain.model.ContinuumSettings
import org.continuumcalendar.domain.model.WorkingHours

class SettingsJsonTest {
    @Test
    fun roundTripCoreFields() {
        val s = ContinuumSettings(
            themeMode = "dark",
            use24HourFormat = true,
            weeklyViewDays = 5,
            agendaRangeDays = 14,
            workingHours = WorkingHours("08:00", "18:00"),
            visibleCalendarIds = listOf("google:primary"),
            firstDayOfWeek = 1,
            weekHourDisplay = "fit24",
        )
        val again = assertNotNull(parseContinuumSettingsJson(continuumSettingsToJson(s)))
        assertEquals("dark", again.themeMode)
        assertEquals(true, again.use24HourFormat)
        assertEquals(5, again.weeklyViewDays)
        assertEquals(14, again.agendaRangeDays)
        assertEquals("08:00", again.workingHours.start)
        assertEquals(listOf("google:primary"), again.visibleCalendarIds)
        assertEquals(1, again.firstDayOfWeek)
        assertEquals("fit24", again.weekHourDisplay)
    }
}

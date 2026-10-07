package org.fossify.calendar.continuum

import org.fossify.calendar.helpers.HOLIDAY_EVENT
import org.fossify.calendar.helpers.OTHER_EVENT
import org.fossify.calendar.helpers.SOURCE_CONTACT_ANNIVERSARY
import org.fossify.calendar.helpers.SOURCE_CONTACT_BIRTHDAY
import org.fossify.calendar.helpers.SOURCE_SIMPLE_CALENDAR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContinuumEventViewModeTest {

    @Test
    fun parseModeDefaultsToEdit() {
        assertEquals(ContinuumEventViewMode.MODE_EDIT, ContinuumEventViewMode.parseMode(null))
        assertEquals(ContinuumEventViewMode.MODE_EDIT, ContinuumEventViewMode.parseMode(""))
        assertEquals(ContinuumEventViewMode.MODE_EDIT, ContinuumEventViewMode.parseMode("nope"))
    }

    @Test
    fun parseModeAcceptsViewAndEdit() {
        assertEquals(ContinuumEventViewMode.MODE_VIEW, ContinuumEventViewMode.parseMode("view"))
        assertEquals(ContinuumEventViewMode.MODE_VIEW, ContinuumEventViewMode.parseMode(" VIEW "))
        assertEquals(ContinuumEventViewMode.MODE_EDIT, ContinuumEventViewMode.parseMode("edit"))
        assertTrue(ContinuumEventViewMode.isViewMode("view"))
        assertFalse(ContinuumEventViewMode.isViewMode("edit"))
    }

    @Test
    fun canShowEditActionBlocksHolidaysAndContactSources() {
        assertTrue(
            ContinuumEventViewMode.canShowEditAction(
                eventSource = SOURCE_SIMPLE_CALENDAR,
                calendarType = OTHER_EVENT,
            )
        )
        assertFalse(
            ContinuumEventViewMode.canShowEditAction(
                eventSource = SOURCE_SIMPLE_CALENDAR,
                calendarType = HOLIDAY_EVENT,
            )
        )
        assertFalse(
            ContinuumEventViewMode.canShowEditAction(
                eventSource = SOURCE_CONTACT_BIRTHDAY,
                calendarType = OTHER_EVENT,
            )
        )
        assertFalse(
            ContinuumEventViewMode.canShowEditAction(
                eventSource = SOURCE_CONTACT_ANNIVERSARY,
                calendarType = OTHER_EVENT,
            )
        )
        assertFalse(
            ContinuumEventViewMode.canShowEditAction(
                eventSource = SOURCE_SIMPLE_CALENDAR,
                calendarType = OTHER_EVENT,
                calendarEmail = "en.usa#wendy.h@example.net",
            )
        )
    }

    @Test
    fun osmStaticMapUrlMatchesDesktopPattern() {
        val url = ContinuumEventViewMode.osmStaticMapUrl(38.9, -77.0, 400, 180, 14)
        assertTrue(url.contains("staticmap.openstreetmap.de"))
        assertTrue(url.contains("center=38.9,-77.0"))
        assertTrue(url.contains("size=400x180"))
        assertTrue(url.contains("markers=38.9,-77.0,red-pushpin"))
    }

    @Test
    fun parsePhotonCoordsReadsLonLatOrder() {
        val json = """
            {"features":[{"geometry":{"type":"Point","coordinates":[-77.0365,38.8977]},
              "properties":{"name":"White House"}}]}
        """.trimIndent()
        val coords = ContinuumEventViewMode.parsePhotonCoords(json)
        assertEquals(38.8977, coords!!.first, 0.0001)
        assertEquals(-77.0365, coords.second, 0.0001)
        assertNull(ContinuumEventViewMode.parsePhotonCoords("""{"features":[]}"""))
    }
}

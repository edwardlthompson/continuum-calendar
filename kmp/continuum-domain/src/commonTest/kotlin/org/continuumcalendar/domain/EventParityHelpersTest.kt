package org.continuumcalendar.domain

import org.continuumcalendar.domain.event.RepeatFreq
import org.continuumcalendar.domain.event.convertAllDayBounds
import org.continuumcalendar.domain.event.freqFromRrule
import org.continuumcalendar.domain.event.formatRepeatFreq
import org.continuumcalendar.domain.maps.MapOpenTarget
import org.continuumcalendar.domain.maps.MapProviderId
import org.continuumcalendar.domain.maps.googleMapsUrl
import org.continuumcalendar.domain.maps.mapProviderUrl
import org.continuumcalendar.domain.maps.osmMapsUrl
import org.continuumcalendar.domain.maps.osmStaticMapUrl
import org.continuumcalendar.domain.remind.formatReminderMinutes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventParityHelpersTest {
    @Test
    fun formatReminderMinutesLabels() {
        assertEquals("At time of event", formatReminderMinutes(0))
        assertEquals("15 min before", formatReminderMinutes(15))
        assertEquals("1 hr before", formatReminderMinutes(60))
        assertEquals("90 min before", formatReminderMinutes(90))
    }

    @Test
    fun freqFromRruleReadsIcs() {
        assertEquals(RepeatFreq.NONE, freqFromRrule(null))
        assertEquals(RepeatFreq.WEEKLY, freqFromRrule(listOf("RRULE:FREQ=WEEKLY;BYDAY=MO")))
        assertEquals(RepeatFreq.DAILY, freqFromRrule(listOf("RRULE:FREQ=DAILY")))
        assertEquals("Weekly", formatRepeatFreq(RepeatFreq.WEEKLY))
    }

    @Test
    fun convertAllDayRoundTripShapes() {
        val toAllDay = convertAllDayBounds(
            start = "2026-11-15T14:30:00",
            end = "2026-11-15T15:30:00",
            allDay = false,
            targetAllDay = true,
        )
        assertEquals(true, toAllDay.allDay)
        assertEquals("2026-11-15", toAllDay.start)
        assertEquals("2026-11-15", toAllDay.end)

        val toTimed = convertAllDayBounds(
            start = "2026-11-16",
            end = "2026-11-16",
            allDay = true,
            targetAllDay = false,
        )
        assertEquals(false, toTimed.allDay)
        assertEquals("2026-11-16T09:00:00", toTimed.start)
        assertEquals("2026-11-16T10:00:00", toTimed.end)
    }

    @Test
    fun mapUrlsQueryAndCoords() {
        val q = MapOpenTarget("Austin TX")
        assertTrue(osmMapsUrl(q).contains("openstreetmap.org/search"))
        assertTrue(googleMapsUrl(q).contains("google.com/maps"))
        assertEquals(
            osmMapsUrl(q),
            mapProviderUrl(MapProviderId.OSM, q),
        )
        val c = MapOpenTarget("Austin", lat = 30.2672, lon = -97.7431)
        assertTrue(osmMapsUrl(c).contains("mlat=30.2672"))
        val static = osmStaticMapUrl(30.2672, -97.7431, 400, 180, 14)
        assertTrue(static.contains("staticmap.openstreetmap.de"))
        assertTrue(static.contains("30.2672,-97.7431"))
    }
}

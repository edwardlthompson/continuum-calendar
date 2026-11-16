package org.continuumcalendar.data.maps

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PhotonGeocodeTest {
    @Test
    fun parseFirstPlaceReadsLonLat() {
        val raw = """
            {"features":[{"geometry":{"coordinates":[-97.7431,30.2672],"type":"Point"},
            "properties":{"name":"Austin","city":"Austin"}}]}
        """.trimIndent()
        val place = PhotonGeocode.parseFirstPlace(raw)
        assertNotNull(place)
        assertEquals(30.2672, place.lat)
        assertEquals(-97.7431, place.lon)
        assertEquals("Austin, Austin", place.label)
    }
}

package org.fossify.calendar.continuum

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContinuumLocationSearchTest {

    @Test
    fun parsePhotonFormatsAddress() {
        val json = """
            {"features":[{"properties":{
              "name":"Union Station","city":"Washington","state":"DC","country":"United States"
            }}]}
        """.trimIndent()
        val lines = ContinuumLocationSearch.parsePhoton(json)
        assertEquals(listOf("Union Station, Washington, DC, United States"), lines)
    }

    @Test
    fun parsePhotonEmptyFeatures() {
        assertTrue(ContinuumLocationSearch.parsePhoton("""{"features":[]}""").isEmpty())
    }
}

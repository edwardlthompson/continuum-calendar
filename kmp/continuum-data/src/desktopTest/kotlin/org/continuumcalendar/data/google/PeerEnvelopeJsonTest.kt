package org.continuumcalendar.data.google

import org.continuumcalendar.domain.model.ContinuumSettings
import org.continuumcalendar.domain.model.WorkingHours
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PeerEnvelopeJsonTest {
    @Test
    fun roundTripSettingsEnvelope() {
        val settings = ContinuumSettings(
            workingHours = WorkingHours("08:00", "18:00"),
            slotMinMinutes = 45,
            travelBufferMinutes = 10,
        )
        val body = PeerEnvelopeJson.settingsEnvelopeJson(settings, revision = 3, updatedAt = "2026-11-16T00:00:00Z")
        assertEquals(3L, PeerEnvelopeJson.parseRevision(body))
        val parsed = PeerEnvelopeJson.extractSettings(body)
        assertNotNull(parsed)
        assertEquals("08:00", parsed.workingHours.start)
        assertEquals(45, parsed.slotMinMinutes)
        assertEquals(10, parsed.travelBufferMinutes)
    }

    @Test
    fun parseOwnedLocalEvents() {
        val body = """
            {"schemaVersion":1,"revision":2,"events":[
              {"id":"a","calendarId":"local:1","title":"Note","start":"2026-11-16T10:00:00","end":"2026-11-16T11:00:00","allDay":false}
            ]}
        """.trimIndent()
        val events = PeerEnvelopeJson.parseOwnedEvents(body)
        assertEquals(1, events.size)
        assertEquals("a", events[0].id)
        assertEquals("local:1", events[0].calendarId)
    }
}

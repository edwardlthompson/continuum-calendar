package org.continuumcalendar.domain

import org.continuumcalendar.domain.event.isoDatePart
import org.continuumcalendar.domain.event.isoTimePart
import org.continuumcalendar.domain.event.joinDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class EventDateTimeEditTest {
    @Test
    fun splitsAndJoins() {
        assertEquals("2026-11-16", isoDatePart("2026-11-16T14:30:00"))
        assertEquals("14:30", isoTimePart("2026-11-16T14:30:00"))
        assertEquals("2026-11-16T14:30:00", joinDateTime("2026-11-16", "14:30", allDay = false))
        assertEquals("2026-11-16", joinDateTime("2026-11-16", "14:30", allDay = true))
    }
}

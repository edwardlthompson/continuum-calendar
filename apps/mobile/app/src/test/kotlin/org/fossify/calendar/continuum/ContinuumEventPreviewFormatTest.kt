package org.fossify.calendar.continuum

import android.provider.CalendarContract
import org.fossify.calendar.models.Attendee
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContinuumEventPreviewFormatTest {

    private val zone = DateTimeZone.forID("UTC")

    @Test
    fun formatWhenLineTimedUsesRelativeDay() {
        val now = DateTime(2026, 10, 16, 12, 0, zone)
        val start = DateTime(2026, 10, 17, 13, 0, zone).millis / 1000
        val end = DateTime(2026, 10, 17, 19, 0, zone).millis / 1000
        val line = ContinuumEventPreviewFormat.formatWhenLine(
            startTsSeconds = start,
            endTsSeconds = end,
            allDay = false,
            use24Hour = true,
            now = now,
            zone = zone,
        )
        assertEquals("Tomorrow • 13:00–19:00", line)
    }

    @Test
    fun formatWhenLineAllDaySingleDay() {
        val now = DateTime(2026, 10, 16, 12, 0, zone)
        val start = DateTime(2026, 10, 16, 0, 0, zone).millis / 1000
        val end = DateTime(2026, 10, 17, 0, 0, zone).millis / 1000
        val line = ContinuumEventPreviewFormat.formatWhenLine(
            startTsSeconds = start,
            endTsSeconds = end,
            allDay = true,
            use24Hour = true,
            now = now,
            zone = zone,
        )
        assertEquals("Today • All day", line)
    }

    @Test
    fun formatReminderLabel() {
        assertNull(ContinuumEventPreviewFormat.formatReminderLabel(-1))
        assertEquals("At time of event", ContinuumEventPreviewFormat.formatReminderLabel(0))
        assertEquals("30 minutes before", ContinuumEventPreviewFormat.formatReminderLabel(30))
        assertEquals("1 hour before", ContinuumEventPreviewFormat.formatReminderLabel(60))
        assertEquals("2 hours before", ContinuumEventPreviewFormat.formatReminderLabel(120))
    }

    @Test
    fun organizerRowUsesRelationship() {
        val guests = listOf(
            Attendee(1, "Guest", "g@x.com", 0, "", false, CalendarContract.Attendees.RELATIONSHIP_ATTENDEE),
            Attendee(
                2,
                "Edward Thompson",
                "edward@x.com",
                0,
                "",
                false,
                CalendarContract.Attendees.RELATIONSHIP_ORGANIZER,
            ),
        )
        val row = ContinuumEventPreviewFormat.organizerRow(guests)!!
        assertEquals("Created by Edward Thompson", row.primary)
        assertEquals("edward@x.com", row.secondary)
        assertNull(ContinuumEventPreviewFormat.organizerRow(emptyList()))
    }

    @Test
    fun reminderLabelsSkipsOff() {
        val labels = ContinuumEventPreviewFormat.reminderLabels(listOf(-1, 30, -1))
        assertEquals(listOf("30 minutes before"), labels)
        assertTrue(ContinuumEventPreviewFormat.nonBlank("  hi  ") == "hi")
        assertNull(ContinuumEventPreviewFormat.nonBlank("   "))
    }
}

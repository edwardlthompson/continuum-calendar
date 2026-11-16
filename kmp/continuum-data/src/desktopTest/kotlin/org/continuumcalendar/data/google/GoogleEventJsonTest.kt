package org.continuumcalendar.data.google

import org.continuumcalendar.domain.model.CalendarAttendee
import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.CalendarSource
import org.continuumcalendar.domain.model.EventReminder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GoogleEventJsonTest {
    @Test
    fun mapsAttendeesRemindersRecurrence() {
        val raw = """
            {
              "id":"ev1",
              "summary":"Sync",
              "start":{"dateTime":"2026-11-15T10:00:00-05:00","timeZone":"America/New_York"},
              "end":{"dateTime":"2026-11-15T11:00:00-05:00"},
              "attendees":[{"email":"a@example.com","displayName":"Ada","responseStatus":"accepted"}],
              "reminders":{"useDefault":false,"overrides":[{"method":"popup","minutes":30}]},
              "recurrence":["RRULE:FREQ=WEEKLY"],
              "recurringEventId":"master1",
              "transparency":"opaque",
              "visibility":"private"
            }
        """.trimIndent()
        val ev = GoogleEventJson.mapEvent(raw, "primary")!!
        assertEquals("Sync", ev.title)
        assertEquals(1, ev.attendees.size)
        assertEquals("a@example.com", ev.attendees[0].email)
        assertEquals("accepted", ev.attendees[0].responseStatus)
        assertEquals(listOf(EventReminder(30, "popup")), ev.reminders)
        assertEquals(listOf("RRULE:FREQ=WEEKLY"), ev.recurrence)
        assertEquals("master1", ev.recurringEventId)
        assertEquals("private", ev.visibility)
        assertEquals("America/New_York", ev.timeZone)
    }

    @Test
    fun eventBodyIncludesRemindersAndGuests() {
        val body = GoogleEventJson.eventBody(
            CalendarEvent(
                id = "x",
                calendarId = "primary",
                title = "Meet",
                start = "2026-11-15T10:00:00",
                end = "2026-11-15T11:00:00",
                attendees = listOf(CalendarAttendee(email = "b@example.com")),
                reminders = listOf(EventReminder(15)),
                recurrence = listOf("RRULE:FREQ=DAILY"),
                source = CalendarSource.GOOGLE,
            ),
        )
        assertTrue(body.contains("\"useDefault\":false"))
        assertTrue(body.contains("\"minutes\":15"))
        assertTrue(body.contains("b@example.com"))
        assertTrue(body.contains("RRULE:FREQ=DAILY"))
    }
}

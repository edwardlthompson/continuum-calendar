package org.continuumcalendar.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.ContinuumSettings
import org.continuumcalendar.domain.model.EventReminder
import org.continuumcalendar.domain.remind.dueReminders

class UpcomingRemindersTest {
    @Test
    fun firesWhenInsideWindow() {
        val now = 1_000_000_000_000L
        val start = now + 10 * 60_000L
        val ev = CalendarEvent(
            id = "1",
            calendarId = "primary",
            title = "Meet",
            start = kotlinx.datetime.Instant.fromEpochMilliseconds(start).toString(),
            end = kotlinx.datetime.Instant.fromEpochMilliseconds(start + 3_600_000).toString(),
            reminders = listOf(EventReminder(10)),
        )
        val due = dueReminders(
            listOf(ev),
            ContinuumSettings(notificationEnabled = true, defaultReminderMinutes = 10),
            nowMs = now,
            lookAheadMs = 60_000,
        )
        assertEquals(listOf("1"), due.map { it.eventId })
    }

    @Test
    fun respectsDisabledNotifications() {
        val now = 1_000_000_000_000L
        val start = now + 10 * 60_000L
        val ev = CalendarEvent(
            id = "1",
            calendarId = "primary",
            title = "Meet",
            start = kotlinx.datetime.Instant.fromEpochMilliseconds(start).toString(),
            end = kotlinx.datetime.Instant.fromEpochMilliseconds(start + 3_600_000).toString(),
        )
        val due = dueReminders(
            listOf(ev),
            ContinuumSettings(notificationEnabled = false),
            nowMs = now,
        )
        assertEquals(emptyList(), due)
    }
}

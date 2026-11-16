package org.continuumcalendar.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.continuumcalendar.domain.calendar.applyBirthdayCalendarVisibility
import org.continuumcalendar.domain.calendar.filterContactBirthdays
import org.continuumcalendar.domain.calendar.isBirthdayCalendarEntry
import org.continuumcalendar.domain.calendar.isContactBirthdayEvent
import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.CalendarListEntry
import org.continuumcalendar.domain.model.CalendarSource
import org.continuumcalendar.domain.model.logicalCalendarId

class BirthdaysTest {
    private val bdayCal = CalendarListEntry(
        id = "addressbook#group.v.calendar.google.com",
        accountId = "google",
        displayName = "Birthdays",
        color = "#0F6E8C",
        visible = true,
        writable = false,
        source = CalendarSource.GOOGLE,
        logicalId = logicalCalendarId(CalendarSource.GOOGLE, "addressbook#group.v.calendar.google.com"),
    )
    private val primary = bdayCal.copy(
        id = "primary",
        displayName = "Primary",
        logicalId = logicalCalendarId(CalendarSource.GOOGLE, "primary"),
        writable = true,
    )

    @Test
    fun detectsBirthdayCalendarNotHoliday() {
        assertTrue(isBirthdayCalendarEntry(bdayCal))
        assertFalse(
            isBirthdayCalendarEntry(
                bdayCal.copy(
                    id = "en.usa#wendy.h@example.net",
                    displayName = "Holidays in United States",
                    logicalId = "google:en.usa#wendy.h@example.net",
                ),
            ),
        )
    }

    @Test
    fun onlyAutomatedEventTypeIsContactBirthday() {
        val auto = CalendarEvent(
            id = "a",
            calendarId = "primary",
            title = "Alice",
            start = "2026-11-16",
            end = "2026-11-17",
            allDay = true,
            eventType = "birthday",
        )
        val manual = auto.copy(id = "b", title = "Sofia's Birthday", eventType = "default")
        assertTrue(isContactBirthdayEvent(auto))
        assertFalse(isContactBirthdayEvent(manual))
    }

    @Test
    fun filterHidesAutoAndBirthdayCalWhenOff() {
        val auto = CalendarEvent(
            id = "a",
            calendarId = "primary",
            title = "Alice",
            start = "2026-11-16",
            end = "2026-11-17",
            allDay = true,
            eventType = "birthday",
        )
        val onBdayCal = auto.copy(id = "c", calendarId = bdayCal.id, eventType = "default", title = "Bob")
        val manual = auto.copy(id = "b", title = "Sofia's Birthday", eventType = "default")
        val cals = listOf(primary, bdayCal)
        val hidden = filterContactBirthdays(listOf(auto, onBdayCal, manual), cals, showContactBirthdays = false)
        assertEquals(listOf("b"), hidden.map { it.id })
        val shown = filterContactBirthdays(listOf(auto, onBdayCal, manual), cals, showContactBirthdays = true)
        assertEquals(3, shown.size)
    }

    @Test
    fun applyVisibilityForcesBirthdayCals() {
        val next = applyBirthdayCalendarVisibility(listOf(bdayCal.copy(visible = true)), false)
        assertFalse(next.first().visible)
    }
}

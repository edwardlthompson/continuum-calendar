package org.continuumcalendar.domain.calendar

import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.CalendarListEntry

/** Detect Google automated birthday calendars / events — not manual birthday parties. */

private val NAME_HINTS = listOf(
    "birthday",
    "birthdays",
    "geburtstag",
    "geburtstage",
    "anniversaire",
    "cumpleaños",
    "compleanno",
    "verjaardag",
    "urodziny",
)

/** Contacts Birthdays only — not Holidays (`en.usa#holiday@group…`). */
private val GOOGLE_BIRTHDAY_ID_HINTS = listOf(
    "addressbook#group.v.calendar.google.com",
    "addressbook#group.calendar.google.com",
)

private val HOLIDAY_ID_HINTS = listOf("holiday@group", "#wendy.h@example.net", "holidays")

fun isBirthdayCalendarLabel(nameOrId: String?): Boolean {
    val value = nameOrId?.trim()?.lowercase().orEmpty()
    if (value.isEmpty()) return false
    if (HOLIDAY_ID_HINTS.any { value.contains(it) }) return false
    if (NAME_HINTS.any { value == it }) return true
    if (GOOGLE_BIRTHDAY_ID_HINTS.any { value.contains(it) }) return true
    return false
}

fun isBirthdayCalendarEntry(entry: CalendarListEntry): Boolean =
    isBirthdayCalendarLabel(entry.displayName) ||
        isBirthdayCalendarLabel(entry.id) ||
        isBirthdayCalendarLabel(entry.logicalId)

/**
 * Google Calendar API automated contact birthdays (`eventType === 'birthday'`).
 * Manual yearly “Sofia's Birthday” events are `default` and must stay visible.
 */
fun isContactBirthdayEvent(event: CalendarEvent): Boolean =
    (event.eventType ?: "").lowercase() == "birthday"

/** Align birthday calendar checkbox visibility with Continuum `showContactBirthdays`. */
fun applyBirthdayCalendarVisibility(
    calendars: List<CalendarListEntry>,
    showContactBirthdays: Boolean,
): List<CalendarListEntry> =
    calendars.map { c ->
        if (!isBirthdayCalendarEntry(c)) c
        else if (c.visible == showContactBirthdays) c
        else c.copy(visible = showContactBirthdays)
    }

/**
 * When birthdays are off: hide birthday calendars' events and eventType=birthday.
 * Manual yearly birthday events (default type) stay.
 */
fun filterContactBirthdays(
    events: List<CalendarEvent>,
    calendars: List<CalendarListEntry>,
    showContactBirthdays: Boolean,
): List<CalendarEvent> {
    if (showContactBirthdays) return events
    val birthdayCalIds = calendars.filter { isBirthdayCalendarEntry(it) }.map { it.id }.toSet()
    return events.filter { ev ->
        if (isContactBirthdayEvent(ev)) return@filter false
        if (ev.calendarId in birthdayCalIds) return@filter false
        if (isBirthdayCalendarLabel(ev.calendarId)) return@filter false
        true
    }
}

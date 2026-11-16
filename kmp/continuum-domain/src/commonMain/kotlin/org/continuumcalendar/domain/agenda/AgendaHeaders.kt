package org.continuumcalendar.domain.agenda

import kotlinx.datetime.LocalDate

const val CONTINUUM_OPEN_DAY_COLOR = "#0F6E8C"

private val WEEKDAYS_LONG = listOf(
    "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday",
)
private val WEEKDAYS_SHORT = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
private val MONTHS_LONG = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

/** Match packages/shared agendaHeaders.ts / Android Formatter.getAgendaSectionTitle. */
fun formatAgendaSectionTitle(dateKey: String, todayKey: String): String {
    val d = LocalDate.parse(dateKey)
    val jsDow = (d.dayOfWeek.ordinal + 1) % 7 // Sun=0
    val weekdayLong = WEEKDAYS_LONG[jsDow]
    val weekdayShort = WEEKDAYS_SHORT[jsDow]
    val month = MONTHS_LONG[d.monthNumber - 1]
    val day = d.dayOfMonth
    return if (dateKey == todayKey) {
        "Today · $weekdayLong, $month $day"
    } else {
        "$weekdayShort · $month $day"
    }
}

fun weekdayShort(dateKey: String): String {
    val d = LocalDate.parse(dateKey)
    val jsDow = (d.dayOfWeek.ordinal + 1) % 7
    return WEEKDAYS_SHORT[jsDow]
}

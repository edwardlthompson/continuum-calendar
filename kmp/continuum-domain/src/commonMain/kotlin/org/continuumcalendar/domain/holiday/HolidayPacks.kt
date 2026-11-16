package org.continuumcalendar.domain.holiday

import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.CalendarSource

enum class HolidayPackId { NONE, US, CA, GB, DE }

data class HolidayPackOption(val id: HolidayPackId, val label: String)

val HOLIDAY_PACK_OPTIONS = listOf(
    HolidayPackOption(HolidayPackId.NONE, "No holiday pack"),
    HolidayPackOption(HolidayPackId.US, "United States"),
    HolidayPackOption(HolidayPackId.CA, "Canada"),
    HolidayPackOption(HolidayPackId.GB, "United Kingdom"),
    HolidayPackOption(HolidayPackId.DE, "Germany"),
)

fun parseHolidayPackId(raw: String?): HolidayPackId =
    when (raw?.trim()?.lowercase()) {
        "us" -> HolidayPackId.US
        "ca" -> HolidayPackId.CA
        "gb" -> HolidayPackId.GB
        "de" -> HolidayPackId.DE
        else -> HolidayPackId.NONE
    }

fun holidayPackStorageId(id: HolidayPackId): String =
    when (id) {
        HolidayPackId.NONE -> "none"
        HolidayPackId.US -> "us"
        HolidayPackId.CA -> "ca"
        HolidayPackId.GB -> "gb"
        HolidayPackId.DE -> "de"
    }

fun holidayEventsForYear(pack: HolidayPackId, year: Int): List<CalendarEvent> {
    if (pack == HolidayPackId.NONE) return emptyList()
    val rows = mutableListOf<Triple<String, String, String>>()
    rows += Triple("nyd", "New Year's Day", iso(year, 1, 1))
    rows += Triple("xmas", "Christmas Day", iso(year, 12, 25))
    when (pack) {
        HolidayPackId.US -> {
            rows += Triple("mlk", "Martin Luther King Jr. Day", nthWeekday(year, 1, 1, 3))
            rows += Triple("pres", "Presidents' Day", nthWeekday(year, 2, 1, 3))
            rows += Triple("mem", "Memorial Day", lastWeekday(year, 5, 1))
            rows += Triple("jun", "Juneteenth", iso(year, 6, 19))
            rows += Triple("jul4", "Independence Day", iso(year, 7, 4))
            rows += Triple("labor", "Labor Day", nthWeekday(year, 9, 1, 1))
            rows += Triple("thanks", "Thanksgiving", nthWeekday(year, 11, 4, 4))
        }
        HolidayPackId.CA -> {
            rows += Triple("canada", "Canada Day", iso(year, 7, 1))
            rows += Triple("labour", "Labour Day", nthWeekday(year, 9, 1, 1))
            rows += Triple("thanks", "Thanksgiving", nthWeekday(year, 10, 1, 2))
            rows += Triple("boxing", "Boxing Day", iso(year, 12, 26))
        }
        HolidayPackId.GB -> rows += Triple("boxing", "Boxing Day", iso(year, 12, 26))
        HolidayPackId.DE -> {
            rows += Triple("unity", "German Unity Day", iso(year, 10, 3))
            rows += Triple("boxing", "Second Christmas Day", iso(year, 12, 26))
        }
        HolidayPackId.NONE -> Unit
    }
    val packKey = holidayPackStorageId(pack)
    return rows.map { (id, title, date) ->
        CalendarEvent(
            id = "holiday-$packKey-$year-$id",
            calendarId = "holidays",
            title = title,
            start = date,
            end = date,
            allDay = true,
            source = CalendarSource.HOLIDAYS,
            busy = false,
            readOnly = true,
        )
    }
}

fun holidayEvents(pack: HolidayPackId, year: Int): List<CalendarEvent> =
    holidayEventsForYear(pack, year) + holidayEventsForYear(pack, year + 1)

fun mergeHolidayEvents(existing: List<CalendarEvent>, pack: HolidayPackId, year: Int): List<CalendarEvent> =
    existing.filter { it.source != CalendarSource.HOLIDAYS } + holidayEvents(pack, year)

private fun iso(year: Int, month: Int, day: Int): String =
    "%04d-%02d-%02d".format(year, month, day)

/** [dow]: 0=Sun … 6=Sat (JS Date.getDay). Month is 1–12. */
private fun nthWeekday(year: Int, month: Int, dow: Int, nth: Int): String {
    val firstDow = weekday(year, month, 1)
    val day = 1 + ((dow - firstDow + 7) % 7) + (nth - 1) * 7
    return iso(year, month, day)
}

private fun lastWeekday(year: Int, month: Int, dow: Int): String {
    val lastDay = daysInMonth(year, month)
    val lastDow = weekday(year, month, lastDay)
    val day = lastDay - ((lastDow - dow + 7) % 7)
    return iso(year, month, day)
}

private fun daysInMonth(year: Int, month: Int): Int =
    when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        else -> 30
    }

private fun weekday(year: Int, month: Int, day: Int): Int {
    // Sakamoto — returns 0=Sun … 6=Sat
    var y = year
    val t = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
    if (month < 3) y -= 1
    return (y + y / 4 - y / 100 + y / 400 + t[month - 1] + day) % 7
}

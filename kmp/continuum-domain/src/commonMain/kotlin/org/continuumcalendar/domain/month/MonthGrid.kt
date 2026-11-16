package org.continuumcalendar.domain.month

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.continuumcalendar.domain.model.normalizeFirstDayOfWeek

/** Six weeks of date keys (null = padding outside month). */
fun monthCellKeys(year: Int, month: Int, firstDayOfWeek: Int): List<String?> {
    val fd = normalizeFirstDayOfWeek(firstDayOfWeek)
    val first = LocalDate(year, month, 1)
    val jsDow = (first.dayOfWeek.ordinal + 1) % 7
    val lead = (jsDow - fd + 7) % 7
    val start = first.plus(-lead, DateTimeUnit.DAY)
    return (0 until 42).map { i ->
        val d = start.plus(i, DateTimeUnit.DAY)
        if (d.monthNumber == month) d.toString() else null
    }
}

fun yearMonthLabel(year: Int, month: Int): String =
    LocalDate(year, month, 1).toString().take(7)

fun shiftYearMonth(yearMonth: String, deltaMonths: Int): String {
    val parts = yearMonth.split("-")
    val y = parts.getOrNull(0)?.toIntOrNull() ?: return yearMonth
    val m = parts.getOrNull(1)?.toIntOrNull() ?: return yearMonth
    val idx = y * 12 + (m - 1) + deltaMonths
    val ny = idx / 12
    val nm = idx % 12 + 1
    return "%04d-%02d".format(ny, nm)
}

fun parseYearMonth(dateKey: String): String = dateKey.take(7)

fun daysInMonthCount(year: Int, month: Int): Int {
    val first = LocalDate(year, month, 1)
    val next = if (month == 12) LocalDate(year + 1, 1, 1) else LocalDate(year, month + 1, 1)
    return next.toEpochDays() - first.toEpochDays()
}

/** Convenience: event counts keyed by YYYY-MM-DD. */
fun eventCountsByDay(dateKeys: Iterable<String>): Map<String, Int> {
    val map = mutableMapOf<String, Int>()
    for (k in dateKeys) {
        map[k] = (map[k] ?: 0) + 1
    }
    return map
}

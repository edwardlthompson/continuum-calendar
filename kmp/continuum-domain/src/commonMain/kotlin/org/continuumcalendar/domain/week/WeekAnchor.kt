package org.continuumcalendar.domain.week

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.continuumcalendar.domain.model.normalizeFirstDayOfWeek

/**
 * Leftmost column for the rolling week grid.
 * When [fromToday] is true, anchor is today; else week start from [firstDayOfWeek] (0=Sun).
 */
fun weekViewAnchorKey(
    fromToday: Boolean,
    firstDayOfWeek: Int = 0,
    epochMs: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val today = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(timeZone).date
    if (fromToday) return today.toString()
    val fd = normalizeFirstDayOfWeek(firstDayOfWeek)
    // kotlinx DayOfWeek ordinal: Mon=0 … Sun=6. JS firstDay: Sun=0 … Sat=6.
    val jsDow = (today.dayOfWeek.ordinal + 1) % 7
    val offset = (jsDow - fd + 7) % 7
    return today.plus(-offset, DateTimeUnit.DAY).toString()
}

fun shiftDateKey(dateKey: String, days: Int): String {
    val date = LocalDate.parse(dateKey)
    return date.plus(days, DateTimeUnit.DAY).toString()
}

fun weekViewAnchorKeyFromNow(
    fromToday: Boolean,
    firstDayOfWeek: Int = 0,
): String = weekViewAnchorKey(
    fromToday = fromToday,
    firstDayOfWeek = firstDayOfWeek,
    epochMs = org.continuumcalendar.domain.agenda.SystemEpoch.nowMs(),
)

package org.continuumcalendar.domain.agenda

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val DATE_ONLY = Regex("""^\d{4}-\d{2}-\d{2}$""")

fun localDateKey(epochMs: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val local = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(timeZone).date
    return local.toString()
}

fun toDateKey(iso: String, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val trimmed = iso.trim()
    if (DATE_ONLY.matches(trimmed)) return trimmed
    val instant = runCatching { Instant.parse(trimmed) }.getOrNull()
        ?: return localDateKey(epochMs = SystemEpoch.nowMs(), timeZone)
    return localDateKey(instant.toEpochMilliseconds(), timeZone)
}

fun addDays(dateKey: String, n: Int): String {
    val date = LocalDate.parse(dateKey)
    return LocalDate.fromEpochDays(date.toEpochDays() + n).toString()
}

/** Injectable clock for tests. */
object SystemEpoch {
    var nowMsProvider: () -> Long = { Clock.System.now().toEpochMilliseconds() }
    fun nowMs(): Long = nowMsProvider()
}

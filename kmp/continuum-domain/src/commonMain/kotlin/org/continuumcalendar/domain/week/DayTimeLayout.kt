package org.continuumcalendar.domain.week

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.continuumcalendar.domain.agenda.isAllDayEvent
import org.continuumcalendar.domain.agenda.toDateKey
import org.continuumcalendar.domain.model.CalendarEvent

private const val DAY_MINUTES = 24 * 60

/** Minutes from local midnight for a timed ISO instant; null if all-day / unparseable. */
fun minutesFromMidnightLocal(iso: String, timeZone: TimeZone = TimeZone.currentSystemDefault()): Int? {
    if (Regex("""^\d{4}-\d{2}-\d{2}$""").matches(iso.trim())) return null
    val instant = runCatching { Instant.parse(iso.trim()) }.getOrNull() ?: return null
    val local = instant.toLocalDateTime(timeZone)
    return local.hour * 60 + local.minute
}

data class DayEventBand(
    val event: CalendarEvent,
    /** 0..1 from top of day */
    val topFraction: Float,
    /** 0..1 height of day */
    val heightFraction: Float,
)

fun layoutTimedEventsForDay(
    events: List<CalendarEvent>,
    dateKey: String,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): List<DayEventBand> {
    val out = mutableListOf<DayEventBand>()
    for (ev in events) {
        if (isAllDayEvent(ev)) continue
        if (toDateKey(ev.start, timeZone) != dateKey) continue
        val startMin = minutesFromMidnightLocal(ev.start, timeZone) ?: continue
        val endMinRaw = minutesFromMidnightLocal(ev.end, timeZone)
            ?: (startMin + 30)
        val endMin = endMinRaw.coerceAtLeast(startMin + 15)
        val top = (startMin.toFloat() / DAY_MINUTES).coerceIn(0f, 1f)
        val height = ((endMin - startMin).toFloat() / DAY_MINUTES).coerceIn(1f / DAY_MINUTES, 1f - top)
        out += DayEventBand(ev, top, height)
    }
    return out
}

fun nowFractionOfDay(
    nowEpochMs: Long,
    dateKey: String,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): Float? {
    val local = Instant.fromEpochMilliseconds(nowEpochMs).toLocalDateTime(timeZone)
    if (local.date.toString() != dateKey) return null
    val mins = local.hour * 60 + local.minute + local.second / 60f
    return (mins / DAY_MINUTES).coerceIn(0f, 1f)
}

fun halfHourSlotIndex(hour: Int, half: Int): Int =
    hour.coerceIn(0, 23) * 2 + half.coerceIn(0, 1)

fun slotHourMinute(slotIndex: Int): Pair<Int, Int> {
    val i = slotIndex.coerceIn(0, 47)
    return i / 2 to if (i % 2 == 0) 0 else 30
}

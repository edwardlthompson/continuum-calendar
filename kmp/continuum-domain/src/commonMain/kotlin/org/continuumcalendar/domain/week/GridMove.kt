package org.continuumcalendar.domain.week

import kotlin.math.roundToInt
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import org.continuumcalendar.domain.agenda.isAllDayEvent
import org.continuumcalendar.domain.agenda.toDateKey
import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.CalendarListEntry
import org.continuumcalendar.domain.model.CalendarSource

private const val DAY_MINUTES = 24 * 60

fun isGridEventEditable(
    event: CalendarEvent,
    calendars: List<CalendarListEntry>,
): Boolean {
    if (event.id.isBlank() || event.readOnly) return false
    if (isAllDayEvent(event)) return false
    val cal = calendars.firstOrNull { it.id == event.calendarId }
        ?: return true
    return cal.writable && cal.source != CalendarSource.HOLIDAYS
}

/** Convert a vertical pixel drag into whole minutes of a 24h day column. */
fun pixelDeltaToMinutes(deltaYPx: Float, dayHeightPx: Float): Int {
    if (dayHeightPx <= 0f) return 0
    return ((deltaYPx / dayHeightPx) * DAY_MINUTES).roundToInt()
}

fun snapToStep(minutes: Int, snapMinutes: Int = 15): Int {
    val step = snapMinutes.coerceAtLeast(1)
    return ((minutes.toFloat() / step).roundToInt() * step).coerceIn(0, DAY_MINUTES)
}

/**
 * Move a timed event by [deltaMinutes], snapping start to [snapMinutes].
 * Preserves duration; clamps within the local calendar day.
 */
fun applyVerticalDrag(
    event: CalendarEvent,
    deltaMinutes: Int,
    snapMinutes: Int = 15,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): CalendarEvent? {
    if (isAllDayEvent(event)) return null
    val startMin = minutesOfIso(event.start, timeZone) ?: return null
    val endMinRaw = minutesOfIso(event.end, timeZone) ?: (startMin + 30)
    val duration = (endMinRaw - startMin).coerceAtLeast(snapMinutes.coerceAtLeast(15))
    var newStart = snapToStep(startMin + deltaMinutes, snapMinutes)
    var newEnd = newStart + duration
    if (newStart < 0) {
        newStart = 0
        newEnd = duration.coerceAtMost(DAY_MINUTES)
    }
    if (newEnd > DAY_MINUTES) {
        newEnd = DAY_MINUTES
        newStart = (newEnd - duration).coerceAtLeast(0)
        newStart = snapToStep(newStart, snapMinutes)
        newEnd = (newStart + duration).coerceAtMost(DAY_MINUTES)
    }
    if (newStart == startMin && newEnd == endMinRaw) return null
    val dateKey = eventDateKey(event.start, timeZone) ?: return null
    return event.copy(
        start = formatLocalIso(dateKey, newStart),
        end = formatLocalIso(dateKey, newEnd),
        allDay = false,
    )
}

/**
 * Resize a timed event by moving the end edge [deltaMinutes] (15-min snap).
 * Keeps start fixed; enforces minimum duration of [snapMinutes].
 */
fun applyVerticalResize(
    event: CalendarEvent,
    deltaMinutes: Int,
    snapMinutes: Int = 15,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): CalendarEvent? {
    if (isAllDayEvent(event)) return null
    val startMin = minutesOfIso(event.start, timeZone) ?: return null
    val endMinRaw = minutesOfIso(event.end, timeZone) ?: (startMin + 30)
    val minDur = snapMinutes.coerceAtLeast(15)
    var newEnd = snapToStep(endMinRaw + deltaMinutes, snapMinutes).coerceIn(startMin + minDur, DAY_MINUTES)
    if (newEnd == endMinRaw) return null
    val dateKey = eventDateKey(event.start, timeZone) ?: return null
    return event.copy(
        start = formatLocalIso(dateKey, startMin),
        end = formatLocalIso(dateKey, newEnd),
        allDay = false,
    )
}

/** Prefer Instant+TZ calendar day; for offset-less local ISO use the date prefix. */
internal fun eventDateKey(iso: String, timeZone: TimeZone): String? {
    val trimmed = iso.trim()
    if (runCatching { kotlinx.datetime.Instant.parse(trimmed) }.getOrNull() != null) {
        return toDateKey(trimmed, timeZone)
    }
    val local = runCatching {
        LocalDateTime.parse(trimmed.removeSuffix("Z").take(19))
    }.getOrNull()
    return local?.date?.toString()
}

internal fun minutesOfIso(iso: String, timeZone: TimeZone): Int? {
    minutesFromMidnightLocal(iso, timeZone)?.let { return it }
    val trimmed = iso.trim()
    val local = runCatching {
        val core = trimmed.removeSuffix("Z").take(19)
        LocalDateTime.parse(core)
    }.getOrNull() ?: return null
    return local.hour * 60 + local.minute
}

internal fun formatLocalIso(dateKey: String, minutes: Int): String {
    val m = minutes.coerceIn(0, DAY_MINUTES)
    val h = (m / 60).coerceIn(0, 23)
    val min = if (m >= DAY_MINUTES) 59 else m % 60
    return "$dateKey" + "T" +
        h.toString().padStart(2, '0') + ":" +
        min.toString().padStart(2, '0') + ":00"
}

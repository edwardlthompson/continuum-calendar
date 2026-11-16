package org.continuumcalendar.domain.schedule

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.continuumcalendar.domain.agenda.isAllDayEvent
import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.FreeSlot
import org.continuumcalendar.domain.model.WorkingHours

/** Propose up to [count] free windows of [durationMinutes] across the next [days] days. */
fun proposeMeetingTimes(
    events: List<CalendarEvent>,
    fromEpochMs: Long,
    days: Int = 7,
    durationMinutes: Int = 30,
    count: Int = 5,
    workingHours: WorkingHours = WorkingHours(),
    travelBufferMinutes: Int = 0,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): List<FreeSlot> {
    val durationMs = durationMinutes.coerceAtLeast(5) * 60_000L
    val bufferMs = travelBufferMinutes.coerceAtLeast(0) * 60_000L
    val from = Instant.fromEpochMilliseconds(fromEpochMs)
    val busy = events
        .filter { !isAllDayEvent(it) && it.busy }
        .mapNotNull { ev ->
            val s = parseMs(ev.start) ?: return@mapNotNull null
            val e = parseMs(ev.end) ?: return@mapNotNull null
            (s - bufferMs) to (e + bufferMs)
        }
        .sortedBy { it.first }
    val proposals = mutableListOf<FreeSlot>()
    val startLocal = from.toLocalDateTime(timeZone).date
    for (d in 0 until days.coerceAtLeast(1)) {
        if (proposals.size >= count) break
        val day = startLocal.plusDays(d) ?: break
        val winStart = hmEpoch(day, workingHours.start, timeZone) ?: continue
        val winEnd = hmEpoch(day, workingHours.end, timeZone) ?: continue
        var t = maxOf(winStart, fromEpochMs)
        while (t + durationMs <= winEnd && proposals.size < count) {
            val slotEnd = t + durationMs
            val blocker = busy.firstOrNull { t < it.second && slotEnd > it.first }
            if (blocker == null) {
                proposals += FreeSlot(t, slotEnd)
                t = slotEnd
            } else {
                t = blocker.second
            }
        }
    }
    return proposals
}

fun formatProposedSlot(slot: FreeSlot, use24: Boolean, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val start = Instant.fromEpochMilliseconds(slot.startEpochMs).toLocalDateTime(timeZone)
    val end = Instant.fromEpochMilliseconds(slot.endEpochMs).toLocalDateTime(timeZone)
    fun hm(t: LocalDateTime): String {
        val h = t.hour
        val m = t.minute.toString().padStart(2, '0')
        if (use24) return "%02d:%s".format(h, m)
        val am = h < 12
        val h12 = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return "$h12:$m ${if (am) "AM" else "PM"}"
    }
    return "${start.date} · ${hm(start)}–${hm(end)}"
}

private fun LocalDate.plusDays(n: Int): LocalDate? =
    runCatching {
        val epoch = toEpochDays() + n
        LocalDate.fromEpochDays(epoch)
    }.getOrNull()

private fun parseMs(iso: String): Long? =
    runCatching { Instant.parse(iso.trim()).toEpochMilliseconds() }.getOrNull()
        ?: runCatching {
            val core = iso.trim().removeSuffix("Z").take(19)
            LocalDateTime.parse(core).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        }.getOrNull()

private fun hmEpoch(date: LocalDate, hm: String, timeZone: TimeZone): Long? {
    val m = Regex("""^(\d{1,2}):(\d{2})""").find(hm.trim()) ?: return null
    val h = m.groupValues[1].toIntOrNull()?.coerceIn(0, 23) ?: return null
    val min = m.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: return null
    return LocalDateTime(date, LocalTime(h, min)).toInstant(timeZone).toEpochMilliseconds()
}

package org.continuumcalendar.domain.agenda

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.ContinuumSettings
import org.continuumcalendar.domain.model.FreeSlot

/**
 * Longest gap between busy timed events within working hours, after travel buffer,
 * that is at least [ContinuumSettings.slotMinMinutes] long.
 */
fun longestFreeSlotForDay(
    dateKey: String,
    events: List<CalendarEvent>,
    settings: ContinuumSettings,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): FreeSlot? {
    val date = runCatching { LocalDate.parse(dateKey) }.getOrNull() ?: return null
    val dayStart = hmToEpoch(date, settings.workingHours.start, timeZone) ?: return null
    val dayEnd = hmToEpoch(date, settings.workingHours.end, timeZone) ?: return null
    if (dayEnd <= dayStart) return null
    val bufferMs = settings.travelBufferMinutes.coerceAtLeast(0) * 60_000L
    val minMs = settings.slotMinMinutes.coerceAtLeast(5) * 60_000L
    val busy = events
        .filter { !isAllDayEvent(it) && it.busy }
        .mapNotNull { ev ->
            val s = parseMs(ev.start) ?: return@mapNotNull null
            val e = parseMs(ev.end) ?: return@mapNotNull null
            (s - bufferMs).coerceAtLeast(dayStart) to (e + bufferMs).coerceAtMost(dayEnd)
        }
        .filter { it.second > it.first }
        .sortedBy { it.first }
    val merged = mutableListOf<Pair<Long, Long>>()
    for ((s, e) in busy) {
        if (merged.isEmpty() || s > merged.last().second) {
            merged += s to e
        } else {
            val last = merged.removeLast()
            merged += last.first to maxOf(last.second, e)
        }
    }
    var cursor = dayStart
    var best: FreeSlot? = null
    fun consider(start: Long, end: Long) {
        if (end - start < minMs) return
        if (best == null || end - start > best!!.endEpochMs - best!!.startEpochMs) {
            best = FreeSlot(start, end)
        }
    }
    for ((s, e) in merged) {
        if (s > cursor) consider(cursor, s)
        cursor = maxOf(cursor, e)
    }
    if (cursor < dayEnd) consider(cursor, dayEnd)
    return best
}

fun formatFreeSlotHint(slot: FreeSlot?, use24: Boolean): String? {
    if (slot == null) return null
    val mins = ((slot.endEpochMs - slot.startEpochMs) / 60_000L).toInt()
    if (mins <= 0) return null
    val hours = mins / 60
    val rem = mins % 60
    val dur = when {
        hours > 0 && rem > 0 -> "${hours}h ${rem}m"
        hours > 0 -> "${hours}h"
        else -> "${rem}m"
    }
    return "Free $dur"
}

private fun parseMs(iso: String): Long? =
    runCatching { Instant.parse(iso.trim()).toEpochMilliseconds() }.getOrNull()
        ?: runCatching {
            val core = iso.trim().removeSuffix("Z").take(19)
            val local = LocalDateTime.parse(core)
            local.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        }.getOrNull()

private fun hmToEpoch(date: LocalDate, hm: String, timeZone: TimeZone): Long? {
    val m = Regex("""^(\d{1,2}):(\d{2})""").find(hm.trim()) ?: return null
    val h = m.groupValues[1].toIntOrNull()?.coerceIn(0, 23) ?: return null
    val min = m.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: return null
    return LocalDateTime(date, LocalTime(h, min)).toInstant(timeZone).toEpochMilliseconds()
}

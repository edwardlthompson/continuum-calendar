package org.fossify.calendar.continuum

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

data class FreeSlot(val startTs: Long, val endTs: Long)

object FreeSlots {
    /** All timestamps are Unix seconds (Fossify event TS convention). */
    fun compute(
        busyRanges: List<Pair<Long, Long>>,
        dayStartTs: Long,
        dayEndTs: Long,
        minMinutes: Int = 30,
        travelBufferMinutes: Int = 0,
    ): List<FreeSlot> {
        val buffer = travelBufferMinutes.coerceAtLeast(0) * 60L
        val minSec = minMinutes.coerceAtLeast(1) * 60L
        val busy = busyRanges
            .map { (it.first - buffer) to (it.second + buffer) }
            .filter { it.second > dayStartTs && it.first < dayEndTs }
            .sortedBy { it.first }
        val slots = mutableListOf<FreeSlot>()
        var freeStart = dayStartTs
        for ((bStart, bEnd) in busy) {
            val start = maxOf(bStart, dayStartTs)
            if (start > freeStart && (start - freeStart) >= minSec) {
                slots.add(FreeSlot(freeStart, start))
            }
            freeStart = maxOf(freeStart, minOf(bEnd, dayEndTs))
        }
        if (dayEndTs > freeStart && (dayEndTs - freeStart) >= minSec) {
            slots.add(FreeSlot(freeStart, dayEndTs))
        }
        return slots
    }

    fun formatPlainText(slots: List<FreeSlot>, heading: String = "Available times"): String {
        if (slots.isEmpty()) return "$heading\n(none in range)"
        val dayFmt = DateTimeFormat.forPattern("EEE, MMM d h:mm a")
        val endFmt = DateTimeFormat.forPattern("h:mm a")
        return buildString {
            appendLine(heading)
            slots.forEach { slot ->
                val start = DateTime(slot.startTs * 1000L)
                val end = DateTime(slot.endTs * 1000L)
                appendLine("• ${dayFmt.print(start)} – ${endFmt.print(end)}")
            }
        }.trimEnd()
    }
}

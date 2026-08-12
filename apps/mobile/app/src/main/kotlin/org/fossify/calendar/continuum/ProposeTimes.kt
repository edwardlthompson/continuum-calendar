package org.fossify.calendar.continuum

import org.joda.time.DateTime

/**
 * Mirror of packages/shared proposeMeetingTimes — timestamps in Unix seconds.
 */
object ProposeTimes {
    fun proposeMeetingTimes(
        busyRanges: List<Pair<Long, Long>>,
        fromTs: Long = DateTime.now().millis / 1000L,
        days: Int = 7,
        durationMinutes: Int = 30,
        count: Int = 5,
        workingHours: ContinuumWorkingHours = ContinuumWorkingHours(),
        travelBufferMinutes: Int = 0,
    ): List<FreeSlot> {
        val durationSec = durationMinutes.coerceAtLeast(1) * 60L
        val buffer = travelBufferMinutes.coerceAtLeast(0) * 60L
        val busy = busyRanges
            .map { (it.first - buffer) to (it.second + buffer) }
            .sortedBy { it.first }
        val proposals = mutableListOf<FreeSlot>()
        var cursor = DateTime(fromTs * 1000L).withTimeAtStartOfDay()

        var d = 0
        while (d < days && proposals.size < count) {
            val day = cursor.plusDays(d)
            val (winStart, winEnd) = dayWindow(day, workingHours)
            var t = maxOf(winStart, fromTs)
            while (t + durationSec <= winEnd && proposals.size < count) {
                val slotEnd = t + durationSec
                val blocker = busy.firstOrNull { t < it.second && slotEnd > it.first }
                if (blocker == null) {
                    proposals.add(FreeSlot(t, slotEnd))
                    t = slotEnd
                } else {
                    t = maxOf(blocker.second, t + 15 * 60L)
                }
            }
            d++
        }
        return proposals
    }

    private fun dayWindow(day: DateTime, hours: ContinuumWorkingHours): Pair<Long, Long> {
        val (sh, sm) = parseHm(hours.start)
        val (eh, em) = parseHm(hours.end)
        val start = day.withTime(sh, sm, 0, 0).millis / 1000L
        val end = day.withTime(eh, em, 0, 0).millis / 1000L
        return start to end
    }

    private fun parseHm(hm: String): Pair<Int, Int> {
        val parts = hm.split(':')
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h.coerceIn(0, 23) to m.coerceIn(0, 59)
    }
}

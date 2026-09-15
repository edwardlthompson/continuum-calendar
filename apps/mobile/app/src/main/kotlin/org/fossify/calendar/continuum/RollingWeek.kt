package org.fossify.calendar.continuum

import java.time.LocalDate

/** Homescreen week strip: today-first when rolling, else ISO week-start. */
object RollingWeek {
    fun startDate(today: LocalDate, rolling: Boolean, isoWeekStart: Int): LocalDate {
        if (rolling) return today
        val start = isoWeekStart.coerceIn(1, 7)
        val delta = (today.dayOfWeek.value - start + 7) % 7
        return today.minusDays(delta.toLong())
    }

    fun dayCodes(todayYmd: String, count: Int = 7, rolling: Boolean = true, isoWeekStart: Int = 1): List<String> {
        val today = LocalDate.parse(
            "${todayYmd.substring(0, 4)}-${todayYmd.substring(4, 6)}-${todayYmd.substring(6, 8)}",
        )
        val start = startDate(today, rolling, isoWeekStart)
        return (0 until count).map { offset ->
            val day = start.plusDays(offset.toLong())
            "%04d%02d%02d".format(day.year, day.monthValue, day.dayOfMonth)
        }
    }

    /** Empty days show Open; busy days show the event count. */
    fun cellLabel(count: Int, openLabel: String): String =
        if (count <= 0) openLabel else count.toString()

    /**
     * Count-row size for a 4×1 seven-day strip. Digits stay 16sp;
     * "Open" (and longer locale labels) shrink so they do not wrap.
     */
    fun cellCountSizeSp(label: String): Float = when {
        label.length <= 2 -> 16f
        label.length <= 4 -> 10f
        else -> 8f
    }
}

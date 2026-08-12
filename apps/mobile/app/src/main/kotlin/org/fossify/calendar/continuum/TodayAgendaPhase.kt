package org.fossify.calendar.continuum

import org.fossify.calendar.helpers.Formatter
import org.fossify.calendar.models.Event
import org.fossify.calendar.models.ListEvent
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

enum class TodayAgendaPhase {
    ACTIVE,
    OPEN,
    EMPTY,
}

object TodayAgendaLogic {
    /** Fossify stores one-day all-day ends at local noon — treat those as end-of-day too. */
    private fun looksLikeAllDaySpan(startTS: Long, endTS: Long, flaggedAllDay: Boolean): Boolean {
        if (flaggedAllDay) return true
        val duration = endTS - startTS
        return duration >= 12L * 60L * 60L
    }

    private fun endTsForPastCheck(startTS: Long, endTS: Long, flaggedAllDay: Boolean): Long {
        if (!looksLikeAllDaySpan(startTS, endTS, flaggedAllDay)) return endTS
        // Inclusive last day (noon end → that calendar day).
        return Formatter.getDayEndTS(Formatter.getDayCodeFromTS(endTS))
    }

    fun eventEnded(ev: ListEvent, nowTs: Long): Boolean {
        return endTsForPastCheck(ev.startTS, ev.endTS, ev.isAllDay) < nowTs
    }

    fun eventEnded(ev: Event, nowTs: Long): Boolean {
        return endTsForPastCheck(ev.startTS, ev.endTS, ev.getIsAllDay()) < nowTs
    }

    fun workingHoursEndTs(dayCode: String, workingHoursEnd: String): Long {
        val m = Regex("""^(\d{1,2}):(\d{2})$""").find(workingHoursEnd.trim().ifEmpty { "17:00" })
        val h = m?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(0, 23) ?: 17
        val min = m?.groupValues?.get(2)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val day = DateTime.parse(dayCode, DateTimeFormat.forPattern("yyyyMMdd"))
            .withTime(h, min, 0, 0)
        return day.seconds()
    }

    fun phase(
        events: List<ListEvent>,
        nowTs: Long,
        dayCode: String,
        workingHoursEnd: String,
    ): TodayAgendaPhase {
        val real = events.filter { it.id > 0 }
        if (real.any { !eventEnded(it, nowTs) }) return TodayAgendaPhase.ACTIVE
        return if (nowTs < workingHoursEndTs(dayCode, workingHoursEnd)) {
            TodayAgendaPhase.OPEN
        } else {
            TodayAgendaPhase.EMPTY
        }
    }

    fun phaseForEvents(
        events: List<Event>,
        nowTs: Long,
        dayCode: String,
        workingHoursEnd: String,
    ): TodayAgendaPhase {
        if (events.any { !eventEnded(it, nowTs) }) return TodayAgendaPhase.ACTIVE
        return if (nowTs < workingHoursEndTs(dayCode, workingHoursEnd)) {
            TodayAgendaPhase.OPEN
        } else {
            TodayAgendaPhase.EMPTY
        }
    }

    private fun DateTime.seconds(): Long = millis / 1000
}

package org.continuumcalendar.domain.agenda

import org.continuumcalendar.domain.model.AgendaDaySection

private const val DEFAULT_MAX_EMPTY = 7

data class AgendaFoldOpts(
    val todayKey: String,
    val maxEmpty: Int = DEFAULT_MAX_EMPTY,
    val nowMs: Long? = null,
    val workingHoursEnd: String = "17:00",
)

data class AgendaFoldResult(
    val visible: List<AgendaDaySection>,
    val foldedCount: Int,
)

private fun looksOpen(section: AgendaDaySection, opts: AgendaFoldOpts): Boolean {
    if (section.date == opts.todayKey && opts.nowMs != null) {
        return todayAgendaPhase(
            section.events,
            opts.nowMs,
            opts.workingHoursEnd,
            opts.todayKey,
        ) != TodayAgendaPhase.ACTIVE
    }
    return dayShouldShowOpen(section.events)
}

/** Cap empty/Open days so Agenda stays scannable (shared agendaFold.ts). */
fun collapseEmptyAgendaSections(
    sections: List<AgendaDaySection>,
    opts: AgendaFoldOpts,
): AgendaFoldResult {
    val maxEmpty = if (opts.maxEmpty <= 0) DEFAULT_MAX_EMPTY else opts.maxEmpty
    val visible = mutableListOf<AgendaDaySection>()
    var emptyKept = 0
    var foldedCount = 0
    for (section in sections) {
        if (section.date.isBlank()) continue
        val isToday = section.date == opts.todayKey
        val openDay = looksOpen(section, opts)
        if (!openDay || isToday) {
            visible += section
            if (openDay) emptyKept += 1
            continue
        }
        if (emptyKept < maxEmpty) {
            visible += section
            emptyKept += 1
        } else {
            foldedCount += 1
        }
    }
    return AgendaFoldResult(visible = visible, foldedCount = foldedCount)
}

fun splitDayEventsAtNow(
    events: List<org.continuumcalendar.domain.model.CalendarEvent>,
    nowMs: Long,
): Pair<List<org.continuumcalendar.domain.model.CalendarEvent>, List<org.continuumcalendar.domain.model.CalendarEvent>> {
    val sorted = events.sortedWith(compareBy({ it.start }, { it.end }))
    val past = mutableListOf<org.continuumcalendar.domain.model.CalendarEvent>()
    val future = mutableListOf<org.continuumcalendar.domain.model.CalendarEvent>()
    for (ev in sorted) {
        if (isEventPast(ev, nowMs)) past += ev else future += ev
    }
    return past to future
}

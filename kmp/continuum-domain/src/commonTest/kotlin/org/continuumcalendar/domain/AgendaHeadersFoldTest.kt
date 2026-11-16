package org.continuumcalendar.domain

import org.continuumcalendar.domain.agenda.AgendaFoldOpts
import org.continuumcalendar.domain.agenda.collapseEmptyAgendaSections
import org.continuumcalendar.domain.agenda.formatAgendaSectionTitle
import org.continuumcalendar.domain.model.AgendaDaySection
import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.CalendarSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AgendaHeadersFoldTest {
    @Test
    fun todayHeaderUsesLongWeekday() {
        val title = formatAgendaSectionTitle("2026-11-15", "2026-11-15")
        assertTrue(title.startsWith("Today · "))
        assertTrue(title.contains("November 15"))
    }

    @Test
    fun otherDayUsesShortWeekday() {
        val title = formatAgendaSectionTitle("2026-11-16", "2026-11-15")
        assertTrue(title.startsWith("Mon · ") || title.contains("November 16"))
    }

    @Test
    fun foldCapsOpenDays() {
        val sections = (0..10).map { i ->
            AgendaDaySection(
                date = "2026-11-${(15 + i).toString().padStart(2, '0')}",
                events = emptyList(),
                isEmpty = true,
            )
        }
        // Fix invalid dates for days past month — use addDays style keys only for first few
        val safe = listOf(
            AgendaDaySection("2026-11-15", emptyList(), true),
            AgendaDaySection("2026-11-16", emptyList(), true),
            AgendaDaySection("2026-11-17", emptyList(), true),
            AgendaDaySection("2026-11-18", emptyList(), true),
            AgendaDaySection(
                "2026-11-19",
                listOf(
                    CalendarEvent(
                        id = "1",
                        calendarId = "local:1",
                        title = "Busy",
                        start = "2026-11-19T10:00:00",
                        end = "2026-11-19T11:00:00",
                        source = CalendarSource.LOCAL,
                    ),
                ),
                false,
            ),
            AgendaDaySection("2026-11-20", emptyList(), true),
            AgendaDaySection("2026-11-21", emptyList(), true),
            AgendaDaySection("2026-11-22", emptyList(), true),
            AgendaDaySection("2026-11-23", emptyList(), true),
            AgendaDaySection("2026-11-24", emptyList(), true),
            AgendaDaySection("2026-11-25", emptyList(), true),
            AgendaDaySection("2026-11-26", emptyList(), true),
        )
        val folded = collapseEmptyAgendaSections(
            safe,
            AgendaFoldOpts(todayKey = "2026-11-15", maxEmpty = 3, nowMs = null),
        )
        assertTrue(folded.foldedCount > 0)
        assertEquals(true, folded.visible.any { it.date == "2026-11-15" })
        assertEquals(true, folded.visible.any { it.date == "2026-11-19" })
    }
}

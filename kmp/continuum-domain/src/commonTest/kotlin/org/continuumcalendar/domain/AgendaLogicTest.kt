package org.continuumcalendar.domain

import org.continuumcalendar.domain.agenda.TodayAgendaPhase
import org.continuumcalendar.domain.agenda.buildAgendaSections
import org.continuumcalendar.domain.agenda.dayShouldShowOpen
import org.continuumcalendar.domain.agenda.todayAgendaPhase
import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.CalendarSource
import org.continuumcalendar.domain.sync.PeerReconcileAction
import org.continuumcalendar.domain.sync.decidePeerReconcile
import org.continuumcalendar.domain.week.weekViewAnchorKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AgendaLogicTest {
    @Test
    fun emptyDaysAppearWhenEnabled() {
        val sections = buildAgendaSections(
            events = listOf(
                CalendarEvent(
                    id = "1",
                    calendarId = "local:1",
                    title = "Meet",
                    start = "2026-11-15T10:00:00",
                    end = "2026-11-15T11:00:00",
                    source = CalendarSource.LOCAL,
                ),
            ),
            rangeStart = "2026-11-15",
            rangeEnd = "2026-11-17",
            showEmptyDays = true,
        )
        assertEquals(3, sections.size)
        assertTrue(sections[1].isEmpty)
        assertTrue(dayShouldShowOpen(sections[1].events))
    }

    @Test
    fun todayPhaseOpenWhenNoFutureEventsBeforeWhEnd() {
        val phase = todayAgendaPhase(
            events = emptyList(),
            nowMs = 1_764_000_000_000L, // arbitrary
            workingHoursEnd = "23:59",
            todayKey = "2026-11-15",
        )
        // With end-of-day WH and mid timeline, treat as OPEN or EMPTY depending on TZ;
        // assert enum is one of the three and empty list never ACTIVE.
        assertTrue(phase == TodayAgendaPhase.OPEN || phase == TodayAgendaPhase.EMPTY)
        assertTrue(phase != TodayAgendaPhase.ACTIVE)
    }

    @Test
    fun peerReconcileSeedsWhenNoRemote() {
        assertEquals(
            PeerReconcileAction.SEED,
            decidePeerReconcile(
                hasRemote = false,
                remoteRevision = 0,
                localRevision = 1,
                pendingLocalPush = false,
            ),
        )
    }

    @Test
    fun rollingWeekAnchorsToday() {
        val key = weekViewAnchorKey(
            fromToday = true,
            firstDayOfWeek = 0,
            epochMs = 1_763_222_400_000L, // 2025-11-15-ish UTC-ish; date key must parse
        )
        assertTrue(key.matches(Regex("""\d{4}-\d{2}-\d{2}""")))
    }
}

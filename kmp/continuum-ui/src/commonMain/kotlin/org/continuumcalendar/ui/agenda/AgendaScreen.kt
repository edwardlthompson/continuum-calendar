package org.continuumcalendar.ui.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.continuumcalendar.domain.agenda.AgendaFoldOpts
import org.continuumcalendar.domain.agenda.CONTINUUM_OPEN_DAY_COLOR
import org.continuumcalendar.domain.agenda.TodayAgendaPhase
import org.continuumcalendar.domain.agenda.addDays
import org.continuumcalendar.domain.agenda.buildAgendaSections
import org.continuumcalendar.domain.agenda.collapseEmptyAgendaSections
import org.continuumcalendar.domain.agenda.dayShouldShowOpen
import org.continuumcalendar.domain.agenda.formatAgendaSectionTitle
import org.continuumcalendar.domain.agenda.formatEventTimeRange
import org.continuumcalendar.domain.agenda.formatFreeSlotHint
import org.continuumcalendar.domain.agenda.isEventPast
import org.continuumcalendar.domain.agenda.localDateKey
import org.continuumcalendar.domain.agenda.longestFreeSlotForDay
import org.continuumcalendar.domain.agenda.splitDayEventsAtNow
import org.continuumcalendar.domain.agenda.todayAgendaPhase
import org.continuumcalendar.domain.model.AgendaDaySection
import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.CalendarListEntry
import org.continuumcalendar.domain.model.ContinuumSettings
import org.continuumcalendar.ui.illustrations.OpenMark
import org.continuumcalendar.ui.theme.LocalContinuumPalette

@Composable
fun AgendaScreen(
    events: List<CalendarEvent>,
    calendars: List<CalendarListEntry>,
    settings: ContinuumSettings,
    nowEpochMs: Long,
    modifier: Modifier = Modifier,
    focusDateKey: String? = null,
    conflictIds: Set<String> = emptySet(),
    onOpenDay: ((String) -> Unit)? = null,
    onSelectEvent: ((CalendarEvent) -> Unit)? = null,
) {
    val palette = LocalContinuumPalette.current
    val today = localDateKey(nowEpochMs)
    val rangeStart = focusDateKey?.takeIf { it.isNotBlank() } ?: today
    val rangeEnd = addDays(rangeStart, settings.agendaRangeDays.coerceAtLeast(1) - 1)
    val workEnd = settings.workingHours.end
    val sections = remember(events, settings.showEmptyDaysInAgenda, rangeStart, rangeEnd) {
        buildAgendaSections(events, rangeStart, rangeEnd, settings.showEmptyDaysInAgenda)
    }
    val folded = remember(sections, today, nowEpochMs, workEnd) {
        collapseEmptyAgendaSections(
            sections,
            AgendaFoldOpts(todayKey = today, nowMs = nowEpochMs, workingHoursEnd = workEnd),
        )
    }
    var showFolded by remember { mutableStateOf(false) }
    val visible = if (showFolded) sections else folded.visible
    val colorByCal = remember(calendars) {
        calendars.associate { it.id to it.color }
    }
    val densityPad = if (settings.agendaDensity == "compact") 4.dp else 8.dp

    Box(
        modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surface)
            .padding(12.dp),
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(visible, key = { it.date }) { section ->
                AgendaDayBlock(
                    section = section,
                    settings = settings,
                    todayKey = today,
                    nowMs = nowEpochMs,
                    workEnd = workEnd,
                    showEmptyDays = settings.showEmptyDaysInAgenda,
                    redact = settings.redactTitlesInScreenshots,
                    use24 = settings.use24HourFormat,
                    colorByCal = colorByCal,
                    densityPad = densityPad,
                    conflictIds = conflictIds,
                    onOpenDay = onOpenDay,
                    onSelectEvent = onSelectEvent,
                )
            }
            if (!showFolded && folded.foldedCount > 0) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clickable { showFolded = true }
                            .padding(vertical = densityPad),
                    ) {
                        Text(
                            text = "Show ${folded.foldedCount} Open days",
                            color = palette.open,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaDayBlock(
    section: AgendaDaySection,
    settings: ContinuumSettings,
    todayKey: String,
    nowMs: Long,
    workEnd: String,
    showEmptyDays: Boolean,
    redact: Boolean,
    use24: Boolean,
    colorByCal: Map<String, String>,
    densityPad: androidx.compose.ui.unit.Dp,
    conflictIds: Set<String>,
    onOpenDay: ((String) -> Unit)?,
    onSelectEvent: ((CalendarEvent) -> Unit)?,
) {
    val palette = LocalContinuumPalette.current
    val isToday = section.date == todayKey
    val header = formatAgendaSectionTitle(section.date, todayKey)
    val phase = if (isToday) todayAgendaPhase(section.events, nowMs, workEnd, todayKey) else null
    val showOpen = showEmptyDays &&
        !(isToday && phase == TodayAgendaPhase.EMPTY) &&
        dayShouldShowOpen(section.events)

    Column(Modifier.fillMaxWidth()) {
        Text(
            text = header,
            style = MaterialTheme.typography.titleMedium,
            color = if (isToday) palette.accent else palette.text,
        )
        when {
            isToday && (phase == TodayAgendaPhase.EMPTY || phase == TodayAgendaPhase.OPEN) ->
                OpenDayRow(section.date, section.events, settings, use24, densityPad, onOpenDay)
            !isToday && section.isEmpty ->
                OpenDayRow(section.date, section.events, settings, use24, densityPad, onOpenDay)
            isToday && phase == TodayAgendaPhase.ACTIVE -> {
                val (past, future) = splitDayEventsAtNow(section.events, nowMs)
                past.forEach { ev ->
                    EventRow(
                        ev, colorByCal[ev.calendarId] ?: CONTINUUM_OPEN_DAY_COLOR,
                        redact, use24, true, densityPad, conflictIds.contains(ev.id), onSelectEvent,
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(palette.now),
                )
                future.forEach { ev ->
                    EventRow(
                        ev, colorByCal[ev.calendarId] ?: CONTINUUM_OPEN_DAY_COLOR,
                        redact, use24, false, densityPad, conflictIds.contains(ev.id), onSelectEvent,
                    )
                }
                if (showOpen) OpenDayRow(section.date, section.events, settings, use24, densityPad, onOpenDay)
            }
            else -> {
                section.events.forEach { ev ->
                    EventRow(
                        ev, colorByCal[ev.calendarId] ?: CONTINUUM_OPEN_DAY_COLOR,
                        redact, use24, isEventPast(ev, nowMs), densityPad, conflictIds.contains(ev.id), onSelectEvent,
                    )
                }
                if (showOpen) OpenDayRow(section.date, section.events, settings, use24, densityPad, onOpenDay)
            }
        }
    }
}

@Composable
private fun OpenDayRow(
    dateKey: String,
    events: List<CalendarEvent>,
    settings: ContinuumSettings,
    use24: Boolean,
    densityPad: androidx.compose.ui.unit.Dp,
    onOpenDay: ((String) -> Unit)?,
) {
    val palette = LocalContinuumPalette.current
    val hint = remember(dateKey, events, settings.slotMinMinutes, settings.travelBufferMinutes, settings.workingHours) {
        formatFreeSlotHint(longestFreeSlotForDay(dateKey, events, settings), use24)
    }
    Row(Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onOpenDay?.invoke(dateKey) }
            .padding(horizontal = 4.dp, vertical = densityPad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OpenMark(color = palette.open)
        Text(text = "Open", color = palette.open, style = MaterialTheme.typography.labelLarge)
        if (hint != null) {
            Text(
                text = "· $hint",
                color = palette.open.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun EventRow(
    ev: CalendarEvent,
    barHex: String,
    redact: Boolean,
    use24: Boolean,
    dimmed: Boolean,
    densityPad: androidx.compose.ui.unit.Dp,
    conflict: Boolean,
    onSelectEvent: ((CalendarEvent) -> Unit)?,
) {
    val palette = LocalContinuumPalette.current
    val bar = parseHexColor(barHex) ?: palette.open
    val title = if (redact) "••••••••" else ev.title
    Row(Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .then(if (conflict) Modifier.background(palette.warn.copy(alpha = 0.25f)) else Modifier)
            .clickable { onSelectEvent?.invoke(ev) }
            .padding(horizontal = 4.dp, vertical = densityPad)
            .then(if (dimmed) Modifier else Modifier),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(bar)
                .align(Alignment.CenterVertically),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.text.copy(alpha = if (dimmed) 0.45f else 1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatEventTimeRange(ev, use24),
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted.copy(alpha = if (dimmed) 0.45f else 1f),
            )
        }
    }
}

fun parseHexColor(hex: String): Color? {
    val h = hex.trim().removePrefix("#")
    if (h.length != 6) return null
    val v = h.toLongOrNull(16) ?: return null
    return Color(0xFF000000 or v)
}

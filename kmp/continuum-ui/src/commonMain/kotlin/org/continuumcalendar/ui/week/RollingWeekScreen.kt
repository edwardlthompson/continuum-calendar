package org.continuumcalendar.ui.week

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.continuumcalendar.domain.agenda.addDays
import org.continuumcalendar.domain.agenda.dayShouldShowOpen
import org.continuumcalendar.domain.agenda.formatEventTimeRange
import org.continuumcalendar.domain.agenda.isAllDayEvent
import org.continuumcalendar.domain.agenda.localDateKey
import org.continuumcalendar.domain.agenda.toDateKey
import org.continuumcalendar.domain.agenda.weekdayShort
import org.continuumcalendar.domain.agenda.CONTINUUM_OPEN_DAY_COLOR
import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.CalendarListEntry
import org.continuumcalendar.domain.model.ContinuumSettings
import org.continuumcalendar.domain.model.normalizeWeekHourDisplay
import org.continuumcalendar.domain.week.shiftDateKey
import org.continuumcalendar.domain.week.weekViewAnchorKey
import org.continuumcalendar.ui.agenda.parseHexColor
import org.continuumcalendar.ui.illustrations.OpenMark
import org.continuumcalendar.ui.theme.LocalContinuumPalette

/** Scroll mode: ~48dp per hour so ~06:00–18:00 fills a typical pane; night hours via scroll. */
private val ScrollHourHeight = 48.dp
private val ScrollFocusStartHour = 6

@Composable
fun RollingWeekScreen(
    events: List<CalendarEvent>,
    calendars: List<CalendarListEntry>,
    settings: ContinuumSettings,
    nowEpochMs: Long,
    modifier: Modifier = Modifier,
    focusDateKey: String? = null,
    weekShiftDays: Int = 0,
    conflictIds: Set<String> = emptySet(),
    onShiftWeek: ((Int) -> Unit)? = null,
    onWeekModeChange: ((String) -> Unit)? = null,
    onSelectEvent: ((CalendarEvent) -> Unit)? = null,
    onAddDay: ((dateKey: String) -> Unit)? = null,
    onSlotClick: ((dateKey: String, hour: Int, minute: Int) -> Unit)? = null,
    onEventMove: ((CalendarEvent, newStartIso: String, newEndIso: String) -> Unit)? = null,
) {
    val palette = LocalContinuumPalette.current
    val today = localDateKey(nowEpochMs)
    val mode = normalizeWeekHourDisplay(settings.weekHourDisplay)
    val colorByCal = remember(calendars) { calendars.associate { it.id to it.color } }
    val days = settings.weeklyViewDays.coerceIn(1, 14)
    val baseAnchor = when {
        focusDateKey != null && !settings.rollingWeekFromToday -> focusDateKey
        focusDateKey != null && weekShiftDays != 0 -> focusDateKey
        else -> weekViewAnchorKey(
            fromToday = settings.rollingWeekFromToday,
            firstDayOfWeek = settings.firstDayOfWeek,
            epochMs = nowEpochMs,
        )
    }
    val anchor = shiftDateKey(baseAnchor, weekShiftDays)
    val keys = remember(anchor, days) { (0 until days).map { addDays(anchor, it) } }
    val byDay = remember(events) { events.groupBy { toDateKey(it.start) } }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (onShiftWeek != null) {
                TextButton(onClick = { onShiftWeek(-1) }) { Text("‹", color = palette.accent) }
            }
            Text(
                text = "${keys.first()} → ${keys.last()}",
                color = palette.muted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            if (onWeekModeChange != null) {
                WeekModeChip("Scroll", mode == "scroll") { onWeekModeChange("scroll") }
                WeekModeChip("Fit 24h", mode == "fit24") { onWeekModeChange("fit24") }
                WeekModeChip("Agenda", mode == "agenda") { onWeekModeChange("agenda") }
            }
            if (onShiftWeek != null) {
                TextButton(onClick = { onShiftWeek(1) }) { Text("›", color = palette.accent) }
            }
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(palette.surface)
                .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                .padding(8.dp),
        ) {
            when (mode) {
                "fit24", "scroll" -> TimeGridWeek(
                    mode = mode,
                    keys = keys,
                    today = today,
                    byDay = byDay,
                    calendars = calendars,
                    settings = settings,
                    nowEpochMs = nowEpochMs,
                    conflictIds = conflictIds,
                    colorByCal = colorByCal,
                    onSelectEvent = onSelectEvent,
                    onSlotClick = onSlotClick,
                    onEventMove = onEventMove,
                )
                else -> AgendaWeek(
                    keys = keys,
                    today = today,
                    byDay = byDay,
                    settings = settings,
                    conflictIds = conflictIds,
                    colorByCal = colorByCal,
                    onSelectEvent = onSelectEvent,
                    onAddDay = onAddDay,
                )
            }
        }
    }
}

@Composable
private fun TimeGridWeek(
    mode: String,
    keys: List<String>,
    today: String,
    byDay: Map<String, List<CalendarEvent>>,
    calendars: List<CalendarListEntry>,
    settings: ContinuumSettings,
    nowEpochMs: Long,
    conflictIds: Set<String>,
    colorByCal: Map<String, String>,
    onSelectEvent: ((CalendarEvent) -> Unit)?,
    onSlotClick: ((dateKey: String, hour: Int, minute: Int) -> Unit)?,
    onEventMove: ((CalendarEvent, newStartIso: String, newEndIso: String) -> Unit)?,
) {
    val scroll = mode == "scroll"
    Column(Modifier.fillMaxSize()) {
        // Day headers aligned under gutter spacer
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Spacer(Modifier.width(WeekTimeGutterWidth))
            keys.forEach { dateKey ->
                DayHeader(
                    dateKey = dateKey,
                    isToday = dateKey == today,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val fitHeight = maxHeight
            val dayHeight: Dp = if (scroll) ScrollHourHeight * 24 else fitHeight
            val scrollState = rememberScrollState()
            val density = LocalDensity.current
            LaunchedEffect(scroll, dayHeight) {
                if (scroll) {
                    val px = with(density) { (ScrollHourHeight * ScrollFocusStartHour).toPx() }.toInt()
                    scrollState.scrollTo(px.coerceAtLeast(0))
                }
            }
            val bodyModifier = if (scroll) {
                Modifier.fillMaxSize().verticalScroll(scrollState)
            } else {
                Modifier.fillMaxSize()
            }
            Row(
                bodyModifier,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                WeekHourGutter(
                    dayHeight = dayHeight,
                    use24Hour = settings.use24HourFormat,
                )
                keys.forEach { dateKey ->
                    val isToday = dateKey == today
                    val dayEvents = byDay[dateKey].orEmpty().sortedBy { it.start }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(dayHeight)
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (isToday) {
                                    Modifier
                                        .border(2.dp, LocalContinuumPalette.current.now, RoundedCornerShape(8.dp))
                                        .background(LocalContinuumPalette.current.accent.copy(alpha = 0.08f))
                                } else {
                                    Modifier.background(LocalContinuumPalette.current.grid.copy(alpha = 0.25f))
                                },
                            ),
                    ) {
                        TimeGridDayColumn(
                            dateKey = dateKey,
                            events = dayEvents,
                            calendars = calendars,
                            nowEpochMs = nowEpochMs,
                            dayHeight = dayHeight,
                            conflictIds = conflictIds,
                            colorByCal = colorByCal,
                            onSelectEvent = onSelectEvent,
                            onSlotClick = { h, m -> onSlotClick?.invoke(dateKey, h, m) },
                            onEventMove = onEventMove,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaWeek(
    keys: List<String>,
    today: String,
    byDay: Map<String, List<CalendarEvent>>,
    settings: ContinuumSettings,
    conflictIds: Set<String>,
    colorByCal: Map<String, String>,
    onSelectEvent: ((CalendarEvent) -> Unit)?,
    onAddDay: ((dateKey: String) -> Unit)?,
) {
    Row(
        Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        keys.forEach { dateKey ->
            val isToday = dateKey == today
            val dayEvents = byDay[dateKey].orEmpty().sortedBy { it.start }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (isToday) {
                            Modifier
                                .border(2.dp, LocalContinuumPalette.current.now, RoundedCornerShape(8.dp))
                                .background(LocalContinuumPalette.current.accent.copy(alpha = 0.12f))
                        } else {
                            Modifier.background(LocalContinuumPalette.current.grid.copy(alpha = 0.35f))
                        },
                    )
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                DayHeader(dateKey = dateKey, isToday = isToday)
                AgendaDayStack(
                    dayEvents = dayEvents,
                    use24 = settings.use24HourFormat,
                    conflictIds = conflictIds,
                    colorByCal = colorByCal,
                    showOpen = dayShouldShowOpen(dayEvents) && settings.showEmptyDaysInAgenda,
                    onSelectEvent = onSelectEvent,
                    onAdd = { onAddDay?.invoke(dateKey) },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun DayHeader(dateKey: String, isToday: Boolean, modifier: Modifier = Modifier) {
    val palette = LocalContinuumPalette.current
    Column(modifier.padding(bottom = 4.dp)) {
        Text(
            text = weekdayShort(dateKey),
            style = MaterialTheme.typography.titleMedium,
            color = if (isToday) palette.accent else palette.text,
        )
        Text(
            text = dateKey.takeLast(2).trimStart('0').ifEmpty { dateKey.takeLast(2) },
            style = MaterialTheme.typography.bodySmall,
            color = if (isToday) palette.now else palette.muted,
        )
    }
}

@Composable
private fun WeekModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalContinuumPalette.current
    Text(
        text = label,
        color = if (selected) palette.onAccent else palette.text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) palette.accent else palette.grid.copy(alpha = 0.4f))
            .border(1.dp, if (selected) palette.accent else palette.border, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun AgendaDayStack(
    dayEvents: List<CalendarEvent>,
    use24: Boolean,
    conflictIds: Set<String>,
    colorByCal: Map<String, String>,
    showOpen: Boolean,
    onSelectEvent: ((CalendarEvent) -> Unit)?,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalContinuumPalette.current
    Column(
        modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        dayEvents.forEach { ev ->
            WeekChip(
                ev = ev,
                use24 = use24,
                conflict = conflictIds.contains(ev.id),
                calColorHex = colorByCal[ev.calendarId] ?: CONTINUUM_OPEN_DAY_COLOR,
                onSelect = onSelectEvent,
            )
        }
        if (showOpen && dayEvents.isEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                OpenMark(color = palette.open)
                Text("Open", color = palette.open, style = MaterialTheme.typography.bodySmall)
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, palette.accent, RoundedCornerShape(4.dp))
                .clickable(onClick = onAdd)
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("+", color = palette.accent, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun WeekChip(
    ev: CalendarEvent,
    use24: Boolean,
    conflict: Boolean,
    calColorHex: String,
    onSelect: ((CalendarEvent) -> Unit)?,
) {
    val palette = LocalContinuumPalette.current
    val time = formatEventTimeRange(ev, use24)
    val calColor = parseHexColor(calColorHex) ?: palette.accent
    val fill = if (conflict) {
        palette.warn.copy(alpha = 0.35f)
    } else {
        calColor.copy(alpha = 0.28f)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(fill)
            .border(1.dp, calColor.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
            .clickable { onSelect?.invoke(ev) }
            .padding(horizontal = 4.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = if (isAllDayEvent(ev)) "All day" else time,
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = ev.title,
            style = MaterialTheme.typography.bodySmall,
            color = palette.text,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

package org.continuumcalendar.ui.week

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.continuumcalendar.domain.agenda.CONTINUUM_OPEN_DAY_COLOR
import org.continuumcalendar.domain.agenda.isAllDayEvent
import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.CalendarListEntry
import org.continuumcalendar.domain.week.applyVerticalDrag
import org.continuumcalendar.domain.week.applyVerticalResize
import org.continuumcalendar.domain.week.isGridEventEditable
import org.continuumcalendar.domain.week.layoutTimedEventsForDay
import org.continuumcalendar.domain.week.nowFractionOfDay
import org.continuumcalendar.domain.week.pixelDeltaToMinutes
import org.continuumcalendar.domain.week.slotHourMinute
import org.continuumcalendar.ui.agenda.parseHexColor
import org.continuumcalendar.ui.theme.LocalContinuumPalette

val WeekTimeGutterWidth = 48.dp

@Composable
fun WeekHourGutter(
    dayHeight: Dp,
    use24Hour: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = LocalContinuumPalette.current
    val density = LocalDensity.current
    val totalPx = with(density) { dayHeight.toPx() }.coerceAtLeast(1f)
    Box(modifier.width(WeekTimeGutterWidth).height(dayHeight)) {
        for (h in 0 until 24) {
            val y = with(density) { (totalPx * (h / 24f)).toDp() }
            val label = if (use24Hour) {
                h.toString().padStart(2, '0') + ":00"
            } else {
                val h12 = when {
                    h == 0 -> 12
                    h > 12 -> h - 12
                    else -> h
                }
                val ampm = if (h < 12) "a" else "p"
                "$h12$ampm"
            }
            Text(
                text = label,
                color = palette.muted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = y - 6.dp)
                    .padding(end = 4.dp),
            )
        }
    }
}

@Composable
fun TimeGridDayColumn(
    dateKey: String,
    events: List<CalendarEvent>,
    calendars: List<CalendarListEntry>,
    nowEpochMs: Long,
    dayHeight: Dp,
    conflictIds: Set<String>,
    colorByCal: Map<String, String>,
    onSelectEvent: ((CalendarEvent) -> Unit)?,
    onSlotClick: (hour: Int, minute: Int) -> Unit,
    onEventMove: ((CalendarEvent, newStartIso: String, newEndIso: String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val palette = LocalContinuumPalette.current
    val allDay = events.filter { isAllDayEvent(it) }
    val bands = remember(events, dateKey) { layoutTimedEventsForDay(events, dateKey) }
    val nowFrac = remember(nowEpochMs, dateKey) { nowFractionOfDay(nowEpochMs, dateKey) }
    val density = LocalDensity.current
    val totalPx = with(density) { dayHeight.toPx() }.coerceAtLeast(1f)

    Box(modifier.fillMaxWidth().height(dayHeight)) {
        for (slot in 0 until 48) {
            val (hour, minute) = slotHourMinute(slot)
            val top = with(density) { (totalPx * (slot / 48f)).toDp() }
            val h = with(density) { (totalPx / 48f).toDp() }
            Box(
                Modifier
                    .fillMaxWidth()
                    .offset(y = top)
                    .height(h)
                    .clickable { onSlotClick(hour, minute) },
            )
        }
        for (h in 0..24) {
            val y = with(density) { (totalPx * (h / 24f)).toDp() }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .offset(y = y)
                    .background(palette.border),
            )
            if (h < 24) {
                val halfY = with(density) { (totalPx * ((h + 0.5f) / 24f)).toDp() }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .offset(y = halfY)
                        .background(palette.grid),
                )
            }
        }
        bands.forEach { band ->
            val top = with(density) { (totalPx * band.topFraction).toDp() }
            val h = with(density) { (totalPx * band.heightFraction).toDp().coerceAtLeast(14.dp) }
            val conflict = conflictIds.contains(band.event.id)
            val calColor = parseHexColor(
                colorByCal[band.event.calendarId] ?: CONTINUUM_OPEN_DAY_COLOR,
            ) ?: palette.accent
            val fill = if (conflict) palette.warn.copy(alpha = 0.45f) else calColor.copy(alpha = 0.32f)
            val editable = onEventMove != null && isGridEventEditable(band.event, calendars)
            var dragOffsetPx by remember(band.event.id) { mutableFloatStateOf(0f) }
            var resizeDeltaPx by remember(band.event.id) { mutableFloatStateOf(0f) }
            val dragOffsetDp = with(density) { dragOffsetPx.toDp() }
            val resizeDeltaDp = with(density) { resizeDeltaPx.toDp() }
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
                    .offset(y = top + dragOffsetDp)
                    .height((h + resizeDeltaDp).coerceAtLeast(14.dp))
                    .clip(RoundedCornerShape(3.dp))
                    .background(fill)
                    .border(1.dp, calColor.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                    .then(
                        if (editable) {
                            Modifier
                                .pointerInput(band.event.id) {
                                    detectTapGestures(
                                        onTap = { onSelectEvent?.invoke(band.event) },
                                    )
                                }
                                .pointerInput(band.event.id, totalPx) {
                                    detectDragGestures(
                                        onDragStart = { dragOffsetPx = 0f },
                                        onDragEnd = {
                                            val deltaMin = pixelDeltaToMinutes(dragOffsetPx, totalPx)
                                            val moved = applyVerticalDrag(band.event, deltaMin)
                                            dragOffsetPx = 0f
                                            if (moved != null) {
                                                onEventMove?.invoke(band.event, moved.start, moved.end)
                                            }
                                        },
                                        onDragCancel = { dragOffsetPx = 0f },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetPx += dragAmount.y
                                        },
                                    )
                                }
                        } else {
                            Modifier.clickable { onSelectEvent?.invoke(band.event) }
                        },
                    )
                    .padding(horizontal = 3.dp, vertical = 1.dp),
            ) {
                Text(
                    band.event.title,
                    color = palette.text,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (editable) {
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(10.dp)
                            .pointerInput(band.event.id, totalPx) {
                                detectDragGestures(
                                    onDragStart = { resizeDeltaPx = 0f },
                                    onDragEnd = {
                                        val deltaMin = pixelDeltaToMinutes(resizeDeltaPx, totalPx)
                                        val resized = applyVerticalResize(band.event, deltaMin)
                                        resizeDeltaPx = 0f
                                        if (resized != null) {
                                            onEventMove?.invoke(band.event, resized.start, resized.end)
                                        }
                                    },
                                    onDragCancel = { resizeDeltaPx = 0f },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        resizeDeltaPx += dragAmount.y
                                    },
                                )
                            }
                            .background(calColor.copy(alpha = 0.45f)),
                    )
                }
            }
        }
        nowFrac?.let { frac ->
            val y = with(density) { (totalPx * frac).toDp() }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .offset(y = y)
                    .background(palette.now),
            )
        }
        if (allDay.isNotEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(2.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(palette.accent.copy(alpha = 0.18f))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            ) {
                Text(
                    allDay.joinToString(" · ") { it.title },
                    color = palette.text,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

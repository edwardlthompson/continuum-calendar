package org.continuumcalendar.ui.month

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.continuumcalendar.domain.agenda.localDateKey
import org.continuumcalendar.domain.agenda.toDateKey
import org.continuumcalendar.domain.agenda.weekdayShort
import org.continuumcalendar.domain.month.monthCellKeys
import org.continuumcalendar.domain.month.parseYearMonth
import org.continuumcalendar.domain.month.shiftYearMonth
import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.ContinuumSettings
import org.continuumcalendar.ui.theme.LocalContinuumPalette

private val DOW = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

@Composable
fun MonthScreen(
    events: List<CalendarEvent>,
    settings: ContinuumSettings,
    nowEpochMs: Long,
    focusDateKey: String,
    onFocusDate: (String) -> Unit,
    onSelectEvent: ((CalendarEvent) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val palette = LocalContinuumPalette.current
    val today = localDateKey(nowEpochMs)
    val yearMonth = parseYearMonth(focusDateKey)
    val year = yearMonth.take(4).toIntOrNull() ?: 2026
    val month = yearMonth.takeLast(2).toIntOrNull() ?: 1
    val cells = remember(yearMonth, settings.firstDayOfWeek) {
        monthCellKeys(year, month, settings.firstDayOfWeek)
    }
    val byDay = remember(events) { events.groupBy { toDateKey(it.start) } }
    val weekdays = remember(settings.firstDayOfWeek, cells) {
        (0 until 7).map { i ->
            cells[i]?.let { weekdayShort(it) } ?: DOW[(settings.firstDayOfWeek + i) % 7]
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surface)
            .border(1.dp, palette.border, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onFocusDate("${shiftYearMonth(yearMonth, -1)}-01") }) {
                Text("‹", color = palette.accent)
            }
            Text(
                yearMonth,
                style = MaterialTheme.typography.titleMedium,
                color = palette.text,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { onFocusDate("${shiftYearMonth(yearMonth, 1)}-01") }) {
                Text("›", color = palette.accent)
            }
        }
        Row(Modifier.fillMaxWidth()) {
            weekdays.forEach { label ->
                Text(label, color = palette.muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            cells.chunked(7).forEach { week ->
                Row(
                    Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    week.forEach { key ->
                        val dayEvents = key?.let { byDay[it].orEmpty() }.orEmpty()
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        key == null -> palette.grid.copy(alpha = 0.2f)
                                        key == today -> palette.accent.copy(alpha = 0.15f)
                                        key == focusDateKey -> palette.grid.copy(alpha = 0.55f)
                                        else -> palette.grid.copy(alpha = 0.35f)
                                    },
                                )
                                .then(
                                    if (key == today) Modifier.border(2.dp, palette.now, RoundedCornerShape(6.dp))
                                    else Modifier,
                                )
                                .clickable(enabled = key != null) {
                                    key?.let {
                                        onFocusDate(it)
                                        dayEvents.firstOrNull()?.let { ev -> onSelectEvent?.invoke(ev) }
                                    }
                                }
                                .padding(4.dp),
                        ) {
                            if (key != null) {
                                Column {
                                    Text(
                                        key.takeLast(2).trimStart('0').ifEmpty { key.takeLast(2) },
                                        color = if (key == today) palette.now else palette.text,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    dayEvents.take(3).forEach { ev ->
                                        Text(
                                            ev.title,
                                            color = palette.text,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    if (dayEvents.size > 3) {
                                        Text(
                                            "+${dayEvents.size - 3}",
                                            color = palette.muted,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

package org.continuumcalendar.ui.month

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.continuumcalendar.domain.agenda.localDateKey
import org.continuumcalendar.domain.agenda.toDateKey
import org.continuumcalendar.domain.month.monthCellKeys
import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.ContinuumSettings
import org.continuumcalendar.ui.theme.LocalContinuumPalette

private val MONTH_NAMES = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

@Composable
fun YearScreen(
    events: List<CalendarEvent>,
    settings: ContinuumSettings,
    nowEpochMs: Long,
    focusDateKey: String,
    onFocusDate: (String) -> Unit,
    onOpenMonth: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalContinuumPalette.current
    val today = localDateKey(nowEpochMs)
    val year = focusDateKey.take(4).toIntOrNull() ?: today.take(4).toInt()
    val counts = remember(events) {
        events.groupingBy { toDateKey(it.start).take(7) }.eachCount()
    }

    Column(
        modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surface)
            .border(1.dp, palette.border, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onFocusDate("${year - 1}-01-01") }) {
                Text("‹", color = palette.accent)
            }
            Text(
                "$year",
                style = MaterialTheme.typography.titleMedium,
                color = palette.text,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { onFocusDate("${year + 1}-01-01") }) {
                Text("›", color = palette.accent)
            }
        }
        (0 until 4).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                (0 until 3).forEach { col ->
                    val month = row * 3 + col + 1
                    val ym = "%04d-%02d".format(year, month)
                    val cells = monthCellKeys(year, month, settings.firstDayOfWeek)
                    val hit = counts[ym] ?: 0
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.grid.copy(alpha = 0.35f))
                            .clickable {
                                onFocusDate("$ym-01")
                                onOpenMonth("$ym-01")
                            }
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            MONTH_NAMES[month - 1],
                            color = palette.accent,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        cells.chunked(7).take(6).forEach { week ->
                            Row(Modifier.fillMaxWidth()) {
                                week.forEach { key ->
                                    val isToday = key == today
                                    Text(
                                        text = key?.takeLast(2)?.trimStart('0')?.ifEmpty { "0" } ?: " ",
                                        color = when {
                                            key == null -> palette.muted.copy(alpha = 0.3f)
                                            isToday -> palette.now
                                            else -> palette.muted
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                        if (hit > 0) {
                            Text("$hit events", color = palette.muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

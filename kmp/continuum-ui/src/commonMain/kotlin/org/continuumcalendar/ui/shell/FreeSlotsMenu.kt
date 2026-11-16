package org.continuumcalendar.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.ContinuumSettings
import org.continuumcalendar.domain.model.FreeSlot
import org.continuumcalendar.domain.schedule.formatProposedSlot
import org.continuumcalendar.domain.schedule.proposeMeetingTimes
import org.continuumcalendar.ui.theme.LocalContinuumPalette

@Composable
fun FreeSlotsMenu(
    events: List<CalendarEvent>,
    settings: ContinuumSettings,
    nowEpochMs: Long,
    onPick: (FreeSlot) -> Unit,
    onCopyAll: (List<FreeSlot>) -> Unit,
    onClose: () -> Unit,
) {
    val palette = LocalContinuumPalette.current
    val slots = remember(events, settings, nowEpochMs) {
        proposeMeetingTimes(
            events = events,
            fromEpochMs = nowEpochMs,
            days = 7,
            durationMinutes = settings.slotMinMinutes.coerceAtLeast(15),
            count = 8,
            workingHours = settings.workingHours,
            travelBufferMinutes = settings.travelBufferMinutes,
        )
    }
    Dialog(onDismissRequest = onClose) {
        Column(
            Modifier
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.surface)
                .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Propose free slots", color = palette.accent, fontWeight = FontWeight.SemiBold)
            Text(
                "Next openings of ${settings.slotMinMinutes}m within working hours",
                color = palette.muted,
                style = MaterialTheme.typography.bodySmall,
            )
            if (slots.isEmpty()) {
                Text("No free slots found in the next week.", color = palette.text)
            } else {
                slots.forEach { slot ->
                    Text(
                        formatProposedSlot(slot, settings.use24HourFormat),
                        color = palette.text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onPick(slot); onClose() }
                            .padding(8.dp),
                    )
                }
                Text(
                    "Copy all",
                    color = palette.accent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCopyAll(slots); onClose() }
                        .padding(8.dp),
                )
            }
            Text("Close", color = palette.muted, modifier = Modifier.clickable(onClick = onClose).padding(8.dp))
        }
    }
}

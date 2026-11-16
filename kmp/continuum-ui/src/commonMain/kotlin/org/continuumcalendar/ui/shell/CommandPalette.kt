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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.continuumcalendar.domain.hotkey.DESKTOP_HOTKEY_CATALOG
import org.continuumcalendar.ui.theme.LocalContinuumPalette

@Composable
fun CommandPalette(
    onAction: (String) -> Unit,
    onClose: () -> Unit,
) {
    val palette = LocalContinuumPalette.current
    val actions = listOf(
        "new" to "New event",
        "today" to "Go to today",
        "agenda" to "Agenda view",
        "week" to "Week view",
        "month" to "Month view",
        "year" to "Year view",
        "search" to "Focus search",
        "goto" to "Go to date",
        "slots" to "Propose free slots",
        "settings" to "Open settings",
    )
    Dialog(onDismissRequest = onClose) {
        Column(
            Modifier
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.surface)
                .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Command palette", color = palette.accent, fontWeight = FontWeight.SemiBold)
            actions.forEach { (id, label) ->
                Text(
                    label,
                    color = palette.text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            onAction(id)
                            onClose()
                        }
                        .padding(8.dp),
                )
            }
            Text("Shortcuts", color = palette.muted, style = MaterialTheme.typography.labelMedium)
            DESKTOP_HOTKEY_CATALOG.forEach { entry ->
                Text(
                    "${entry.label} · ${entry.hint}",
                    color = palette.muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

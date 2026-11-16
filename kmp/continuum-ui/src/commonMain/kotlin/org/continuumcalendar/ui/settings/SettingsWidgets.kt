package org.continuumcalendar.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.continuumcalendar.ui.theme.LocalContinuumPalette

enum class SettingsCategory(
    val title: String,
    val blurb: String,
) {
    ACCOUNT("Account & Google", "Sign-in, Calendar sync, birthdays"),
    APPEARANCE("Appearance", "Theme, clock, screenshots"),
    CALENDAR("Calendar & views", "Calendars, week, agenda"),
    REMINDERS("Reminders", "Notifications, default alert, snooze"),
    SCHEDULING("Scheduling", "Work hours, travel, free slots"),
    WINDOW("Window & startup", "Minimize, close, open at startup"),
    FILES("Import & export", "ICS, CalDAV, settings backup"),
    ABOUT("About", "Version, license, donate"),
    ;

    val storedId: String get() = name.lowercase()

    companion object {
        fun fromStoredId(id: String?): SettingsCategory {
            if (id.isNullOrBlank()) return CALENDAR
            return entries.find { it.storedId.equals(id.trim(), ignoreCase = true) } ?: CALENDAR
        }
    }
}

/** Quiet Venmo donate — same URL as shipping desktop About. */
const val CONTINUUM_VENMO_DONATE_URL =
    "https://venmo.com/code?user_id=1857304970395648420"

@Composable
fun SettingsSectionTitle(text: String) {
    val palette = LocalContinuumPalette.current
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = palette.accent,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
fun SettingsHint(text: String) {
    val palette = LocalContinuumPalette.current
    Text(text, style = MaterialTheme.typography.bodySmall, color = palette.muted)
}

@Composable
fun SettingsCheckRow(label: String, checked: Boolean, onClick: () -> Unit) {
    val palette = LocalContinuumPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = if (checked) "☑" else "☐",
            color = if (checked) palette.accent else palette.muted,
        )
        Text(label, color = palette.text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SettingsChoiceRow(label: String, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    val palette = LocalContinuumPalette.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = palette.muted)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { (value, title) ->
                val on = selected == value
                Text(
                    text = title,
                    color = if (on) palette.onAccent else palette.text,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (on) palette.accent else palette.grid.copy(alpha = 0.35f))
                        .border(1.dp, if (on) palette.accent else palette.border, RoundedCornerShape(6.dp))
                        .clickable { onSelect(value) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
fun SettingsStepperRow(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    val palette = LocalContinuumPalette.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, color = palette.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text("−", color = palette.accent, modifier = Modifier.clickable { onChange((value - 1).coerceAtLeast(min)) }.padding(8.dp))
        Text("$value", color = palette.text, fontWeight = FontWeight.SemiBold)
        Text("+", color = palette.accent, modifier = Modifier.clickable { onChange((value + 1).coerceAtMost(max)) }.padding(8.dp))
    }
}

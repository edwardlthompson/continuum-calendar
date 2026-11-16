package org.continuumcalendar.ui.maps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import org.continuumcalendar.domain.maps.MAP_PROVIDERS
import org.continuumcalendar.domain.maps.MapOpenTarget
import org.continuumcalendar.domain.maps.mapProviderUrl
import org.continuumcalendar.ui.theme.LocalContinuumPalette

@Composable
fun MapOpenChooser(
    target: MapOpenTarget,
    onOpenUrl: (String) -> Unit,
    onClose: () -> Unit,
) {
    val palette = LocalContinuumPalette.current
    Box(
        Modifier
            .fillMaxSize()
            .background(palette.text.copy(alpha = 0.35f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.surface)
                .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                .clickable(enabled = false) {}
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Open in maps", style = MaterialTheme.typography.titleMedium, color = palette.accent)
            Text(
                target.query.trim().ifBlank { "this location" },
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
                maxLines = 2,
            )
            MAP_PROVIDERS.forEach { provider ->
                Text(
                    provider.label,
                    color = palette.text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                        .clickable {
                            onOpenUrl(mapProviderUrl(provider.id, target))
                            onClose()
                        }
                        .padding(12.dp),
                )
            }
            Text(
                "Cancel",
                color = palette.muted,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable(onClick = onClose)
                    .padding(8.dp),
            )
        }
    }
}

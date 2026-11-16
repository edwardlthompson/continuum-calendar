package org.continuumcalendar.ui.maps

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.continuumcalendar.domain.maps.MapOpenTarget
import org.continuumcalendar.domain.maps.osmStaticMapUrl
import org.continuumcalendar.ui.theme.LocalContinuumPalette

data class GeocodeHit(
    val label: String,
    val lat: Double,
    val lon: Double,
)

@Composable
fun LocationMapPreview(
    location: String,
    geocode: suspend (String) -> GeocodeHit?,
    loadImage: suspend (String) -> ImageBitmap?,
    onOpenMaps: (MapOpenTarget) -> Unit,
) {
    val palette = LocalContinuumPalette.current
    val query = location.trim()
    if (query.isEmpty()) return

    var status by remember(query) { mutableStateOf("loading") }
    var hit by remember(query) { mutableStateOf<GeocodeHit?>(null) }
    var bitmap by remember(query) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(query) {
        status = "loading"
        hit = null
        bitmap = null
        val place = withContext(Dispatchers.Default) { geocode(query) }
        if (place == null) {
            status = "missing"
            return@LaunchedEffect
        }
        hit = place
        val url = osmStaticMapUrl(place.lat, place.lon, width = 400, height = 180)
        val img = withContext(Dispatchers.Default) { loadImage(url) }
        bitmap = img
        status = if (img != null) "ready" else "missing"
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        when {
            status == "loading" -> {
                Text("Loading map…", color = palette.muted, style = MaterialTheme.typography.bodySmall)
            }
            bitmap != null && hit != null -> {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                        .clickable {
                            onOpenMaps(
                                MapOpenTarget(
                                    query = hit!!.label.ifBlank { query },
                                    lat = hit!!.lat,
                                    lon = hit!!.lon,
                                ),
                            )
                        },
                ) {
                    Image(
                        bitmap = bitmap!!,
                        contentDescription = "Map of ${hit!!.label}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                    )
                }
                Text(
                    "© OpenStreetMap contributors",
                    color = palette.muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            else -> {
                Text(
                    "Open in maps…",
                    color = palette.accent,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .clickable { onOpenMaps(MapOpenTarget(query = query)) }
                        .padding(vertical = 4.dp),
                )
            }
        }
    }
}

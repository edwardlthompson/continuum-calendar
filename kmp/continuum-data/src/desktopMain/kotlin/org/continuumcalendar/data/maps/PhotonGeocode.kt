package org.continuumcalendar.data.maps

import org.continuumcalendar.domain.maps.MapOpenTarget
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

data class GeocodedPlace(
    val label: String,
    val lat: Double,
    val lon: Double,
) {
    fun toMapTarget(fallbackQuery: String = label): MapOpenTarget =
        MapOpenTarget(query = fallbackQuery.ifBlank { label }, lat = lat, lon = lon)
}

/** Photon (Komoot) geocode — FOSS, same endpoint as shipping desktop. */
object PhotonGeocode {
    private val http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8))
        .build()

    fun geocode(query: String): GeocodedPlace? {
        val q = query.trim()
        if (q.isEmpty()) return null
        return runCatching {
            val url = "https://photon.komoot.io/api/?q=${URLEncoder.encode(q, StandardCharsets.UTF_8)}&limit=1"
            val req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "ContinuumCalendar/0.1 (FOSS desktop)")
                .GET()
                .build()
            val res = http.send(req, HttpResponse.BodyHandlers.ofString())
            if (res.statusCode() !in 200..299) return null
            parseFirstPlace(res.body())
        }.getOrNull()
    }

    internal fun parseFirstPlace(raw: String): GeocodedPlace? {
        // Photon GeoJSON: features[0].geometry.coordinates = [lon, lat]
        val featuresIdx = raw.indexOf("\"features\"")
        if (featuresIdx < 0) return null
        val coordsMatch = Regex(""""coordinates"\s*:\s*\[\s*(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)""")
            .find(raw, featuresIdx) ?: return null
        val lon = coordsMatch.groupValues[1].toDoubleOrNull() ?: return null
        val lat = coordsMatch.groupValues[2].toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        val name = Regex(""""name"\s*:\s*"((?:\\.|[^"\\])*)"""").find(raw, featuresIdx)
            ?.groupValues?.get(1)
            ?.replace("\\\"", "\"")
        val city = Regex(""""city"\s*:\s*"((?:\\.|[^"\\])*)"""").find(raw, featuresIdx)
            ?.groupValues?.get(1)
            ?.replace("\\\"", "\"")
        val label = listOfNotNull(name, city).joinToString(", ").ifBlank { "$lat,$lon" }
        return GeocodedPlace(label = label, lat = lat, lon = lon)
    }
}

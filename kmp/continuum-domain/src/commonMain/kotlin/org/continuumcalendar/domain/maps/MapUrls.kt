package org.continuumcalendar.domain.maps

data class MapOpenTarget(
    val query: String,
    val lat: Double? = null,
    val lon: Double? = null,
)

enum class MapProviderId {
    OSM,
    GOOGLE,
    APPLE,
}

data class MapProvider(
    val id: MapProviderId,
    val label: String,
)

val MAP_PROVIDERS: List<MapProvider> = listOf(
    MapProvider(MapProviderId.OSM, "OpenStreetMap"),
    MapProvider(MapProviderId.GOOGLE, "Google Maps"),
    MapProvider(MapProviderId.APPLE, "Apple Maps"),
)

fun googleMapsUrl(target: MapOpenTarget): String {
    val q = if (target.lat != null && target.lon != null) {
        "${target.lat},${target.lon}"
    } else {
        target.query.trim()
    }
    return "https://www.google.com/maps/search/?api=1&query=${enc(q)}"
}

fun appleMapsUrl(target: MapOpenTarget): String {
    val q = enc(target.query.trim().ifBlank { "Location" })
    return if (target.lat != null && target.lon != null) {
        "https://maps.apple.com/?ll=${target.lat},${target.lon}&q=$q"
    } else {
        "https://maps.apple.com/?q=$q"
    }
}

fun osmMapsUrl(target: MapOpenTarget): String {
    return if (target.lat != null && target.lon != null) {
        val lat = target.lat
        val lon = target.lon
        "https://www.openstreetmap.org/?mlat=$lat&mlon=$lon#map=15/$lat/$lon"
    } else {
        "https://www.openstreetmap.org/search?query=${enc(target.query.trim())}"
    }
}

fun mapProviderUrl(provider: MapProviderId, target: MapOpenTarget): String = when (provider) {
    MapProviderId.GOOGLE -> googleMapsUrl(target)
    MapProviderId.APPLE -> appleMapsUrl(target)
    MapProviderId.OSM -> osmMapsUrl(target)
}

/** FOSS static map preview (OpenStreetMap.de staticmap). */
fun osmStaticMapUrl(
    lat: Double,
    lon: Double,
    width: Int = 400,
    height: Int = 180,
    zoom: Int = 14,
): String {
    val w = width.coerceIn(100, 800)
    val h = height.coerceIn(80, 600)
    return "https://staticmap.openstreetmap.de/staticmap.php?center=$lat,$lon&zoom=$zoom&size=${w}x$h&markers=$lat,$lon,red-pushpin"
}

/** RFC 3986 query-component percent-encode (ASCII-safe for map URLs). */
internal fun enc(s: String): String = buildString(s.length + 8) {
    for (ch in s) {
        when {
            ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9' ||
                ch == '-' || ch == '_' || ch == '.' || ch == '~' -> append(ch)
            ch == ' ' -> append("%20")
            else -> {
                val code = ch.code
                if (code <= 0x7F) {
                    append('%')
                    append(hexDigit(code ushr 4))
                    append(hexDigit(code and 0xF))
                } else {
                    // UTF-8 encode non-ASCII
                    val bytes = ch.toString().encodeToByteArray()
                    for (b in bytes) {
                        val u = b.toInt() and 0xFF
                        append('%')
                        append(hexDigit(u ushr 4))
                        append(hexDigit(u and 0xF))
                    }
                }
            }
        }
    }
}

private fun hexDigit(n: Int): Char = "0123456789ABCDEF"[n]

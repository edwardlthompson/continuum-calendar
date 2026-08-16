package org.fossify.calendar.continuum

import android.content.Context
import android.location.Geocoder
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/** Live place suggestions for the event location field (no Play Services SDK). */
object ContinuumLocationSearch {
    private const val MIN_QUERY = 2
    private const val LIMIT = 8

    fun suggest(context: Context, query: String): List<String> {
        val q = query.trim()
        if (q.length < MIN_QUERY) return emptyList()
        val geo = fromGeocoder(context, q)
        if (geo.isNotEmpty()) return geo
        return fromPhoton(q)
    }

    private fun fromGeocoder(context: Context, query: String): List<String> {
        if (!Geocoder.isPresent()) return emptyList()
        return try {
            @Suppress("DEPRECATION")
            val list = Geocoder(context, Locale.getDefault())
                .getFromLocationName(query, LIMIT)
                .orEmpty()
            list.mapNotNull { addr ->
                addr.getAddressLine(0)?.trim()?.takeIf { it.isNotEmpty() }
                    ?: listOfNotNull(
                        addr.featureName,
                        addr.locality,
                        addr.adminArea,
                        addr.countryName,
                    ).joinToString(", ").takeIf { it.isNotEmpty() }
            }.distinct()
        } catch (_: Exception) {
            emptyList()
        }
    }

    internal fun parsePhoton(json: String): List<String> {
        val out = ArrayList<String>()
        var idx = 0
        while (true) {
            val start = json.indexOf("\"properties\"", idx)
            if (start < 0) break
            val objStart = json.indexOf('{', start)
            val objEnd = json.indexOf('}', objStart + 1)
            if (objStart < 0 || objEnd < 0) break
            val block = json.substring(objStart, objEnd + 1)
            val name = jsonString(block, "name")
            val street = listOf(jsonString(block, "housenumber"), jsonString(block, "street"))
                .filter { it.isNotBlank() }
                .joinToString(" ")
            val city = jsonString(block, "city").ifBlank { jsonString(block, "locality") }
            val line = listOf(
                name.ifBlank { street },
                city,
                jsonString(block, "state"),
                jsonString(block, "country"),
            ).map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString(", ")
            if (line.isNotEmpty()) out.add(line)
            idx = objEnd + 1
        }
        return out.distinct()
    }

    private fun jsonString(obj: String, key: String): String {
        val needle = "\"$key\""
        val k = obj.indexOf(needle)
        if (k < 0) return ""
        val colon = obj.indexOf(':', k + needle.length)
        val q1 = obj.indexOf('"', colon + 1)
        val q2 = if (q1 >= 0) obj.indexOf('"', q1 + 1) else -1
        if (q1 < 0 || q2 < 0) return ""
        return obj.substring(q1 + 1, q2)
    }

    private fun fromPhoton(query: String): List<String> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://photon.komoot.io/api/?q=$encoded&limit=$LIMIT"
        return try {
            parsePhoton(httpGet(url) ?: return emptyList())
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun httpGet(url: String): String? {
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = 4_000
            conn.readTimeout = 4_000
            conn.setRequestProperty("User-Agent", "ContinuumCalendar/1.10.7 (location-suggest)")
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}

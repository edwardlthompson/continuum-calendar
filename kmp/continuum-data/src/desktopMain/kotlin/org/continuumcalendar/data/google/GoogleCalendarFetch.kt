package org.continuumcalendar.data.google

import org.continuumcalendar.domain.agenda.addDays
import org.continuumcalendar.domain.agenda.localDateKey
import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.CalendarListEntry
import org.continuumcalendar.domain.model.CalendarSource
import org.continuumcalendar.domain.model.GoogleOAuthTokens
import org.continuumcalendar.domain.model.logicalCalendarId
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

data class GoogleSyncSnapshot(
    val calendars: List<CalendarListEntry>,
    val events: List<CalendarEvent>,
)

object GoogleCalendarFetch {
    private const val BASE = "https://www.googleapis.com/calendar/v3"
    private val http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    fun syncVisibleRange(tokens: GoogleOAuthTokens, nowMs: Long, rangeDays: Int = 30): GoogleSyncSnapshot {
        val calendars = listCalendars(tokens.accessToken)
        val visible = calendars.filter { it.visible }
        val timeMin = localDateKey(nowMs) + "T00:00:00Z"
        val timeMax = addDays(localDateKey(nowMs), rangeDays) + "T23:59:59Z"
        val events = mutableListOf<CalendarEvent>()
        for (cal in visible.take(12)) {
            runCatching {
                events += listEvents(tokens.accessToken, cal.id, timeMin, timeMax)
            }
        }
        return GoogleSyncSnapshot(calendars = calendars, events = events)
    }

    private fun listCalendars(accessToken: String): List<CalendarListEntry> {
        val body = get(accessToken, "$BASE/users/me/calendarList?maxResults=250")
        val items = objectArray(body, "items")
        val out = mutableListOf<CalendarListEntry>()
        for (item in items) {
            val id = stringField(item, "id")?.trim() ?: continue
            val primary = item.contains("\"primary\":true")
            val selected = !item.contains("\"selected\":false")
            val role = stringField(item, "accessRole")
            val writable = role == "owner" || role == "writer"
            val color = stringField(item, "backgroundColor") ?: "#0F6E8C"
            out += CalendarListEntry(
                id = id,
                accountId = "google",
                displayName = stringField(item, "summary")?.trim()?.ifBlank { id } ?: id,
                color = color,
                visible = selected,
                writable = writable,
                source = CalendarSource.GOOGLE,
                logicalId = if (primary) {
                    logicalCalendarId(CalendarSource.GOOGLE, "primary")
                } else {
                    logicalCalendarId(CalendarSource.GOOGLE, id)
                },
            )
        }
        if (out.isEmpty()) {
            out += CalendarListEntry(
                id = "primary",
                accountId = "google",
                displayName = "Google Primary",
                color = "#0F6E8C",
                visible = true,
                writable = true,
                source = CalendarSource.GOOGLE,
                logicalId = logicalCalendarId(CalendarSource.GOOGLE, "primary"),
            )
        }
        return out
    }

    private fun listEvents(
        accessToken: String,
        calendarId: String,
        timeMin: String,
        timeMax: String,
    ): List<CalendarEvent> {
        val q = buildString {
            append("singleEvents=true&orderBy=startTime&maxResults=250")
            append("&timeMin=").append(enc(timeMin))
            append("&timeMax=").append(enc(timeMax))
        }
        val url = "$BASE/calendars/${enc(calendarId)}/events?$q"
        val body = get(accessToken, url)
        val items = objectArray(body, "items")
        return items.mapNotNull { GoogleEventJson.mapEvent(it, calendarId) }
    }

    private fun get(accessToken: String, url: String): String {
        val req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(45))
            .header("Authorization", "Bearer $accessToken")
            .GET()
            .build()
        val res = http.send(req, HttpResponse.BodyHandlers.ofString())
        if (res.statusCode() !in 200..299) {
            error("Google API HTTP ${res.statusCode()}")
        }
        return res.body()
    }

    private fun enc(s: String): String =
        URLEncoder.encode(s, StandardCharsets.UTF_8)

    private fun stringField(json: String, key: String): String? {
        val re = Regex(""""$key"\s*:\s*"((?:\\.|[^"\\])*)"""")
        return re.find(json)?.groupValues?.get(1)
            ?.replace("\\\"", "\"")
            ?.replace("\\\\", "\\")
    }

    private fun objectField(json: String, key: String): String? {
        val re = Regex(""""$key"\s*:\s*\{""")
        val m = re.find(json) ?: return null
        val start = m.range.last
        var depth = 1
        var i = start + 1
        while (i < json.length && depth > 0) {
            when (json[i]) {
                '{' -> depth++
                '}' -> depth--
            }
            i++
        }
        return json.substring(start, i)
    }

    private fun objectArray(json: String, key: String): List<String> {
        val re = Regex(""""$key"\s*:\s*\[""")
        val m = re.find(json) ?: return emptyList()
        var i = m.range.last + 1
        val out = mutableListOf<String>()
        while (i < json.length) {
            while (i < json.length && json[i].isWhitespace()) i++
            if (i >= json.length || json[i] == ']') break
            if (json[i] != '{') break
            val start = i
            var depth = 0
            while (i < json.length) {
                when (json[i]) {
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) {
                            i++
                            out += json.substring(start, i)
                            break
                        }
                    }
                }
                i++
            }
            while (i < json.length && (json[i].isWhitespace() || json[i] == ',')) i++
        }
        return out
    }
}

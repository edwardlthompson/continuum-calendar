package org.continuumcalendar.data.google

import org.continuumcalendar.domain.model.CalendarEvent
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

object GoogleCalendarWrite {
    private const val BASE = "https://www.googleapis.com/calendar/v3"
    private val http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    fun create(accessToken: String, event: CalendarEvent): CalendarEvent {
        val body = GoogleEventJson.eventBody(event)
        val url = "$BASE/calendars/${enc(event.calendarId)}/events"
        val raw = send(accessToken, "POST", url, body)
        return GoogleEventJson.mapEvent(raw, event.calendarId)
            ?: error("No event id")
    }

    fun update(accessToken: String, event: CalendarEvent): CalendarEvent {
        val body = GoogleEventJson.eventBody(event)
        val url = "$BASE/calendars/${enc(event.calendarId)}/events/${enc(event.id)}"
        val raw = send(accessToken, "PUT", url, body, event.etag)
        return GoogleEventJson.mapEvent(raw, event.calendarId)
            ?: error("No event id")
    }

    fun delete(accessToken: String, calendarId: String, eventId: String) {
        val url = "$BASE/calendars/${enc(calendarId)}/events/${enc(eventId)}"
        val req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(45))
            .header("Authorization", "Bearer $accessToken")
            .DELETE()
            .build()
        val res = http.send(req, HttpResponse.BodyHandlers.ofString())
        if (res.statusCode() !in listOf(200, 204, 404, 410)) {
            error("Google delete HTTP ${res.statusCode()}")
        }
    }

    private fun send(
        accessToken: String,
        method: String,
        url: String,
        body: String,
        etag: String? = null,
    ): String {
        val builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(45))
            .header("Authorization", "Bearer $accessToken")
            .header("Content-Type", "application/json")
        if (!etag.isNullOrBlank()) builder.header("If-Match", etag)
        val publisher = HttpRequest.BodyPublishers.ofString(body)
        when (method) {
            "POST" -> builder.POST(publisher)
            "PUT" -> builder.PUT(publisher)
            else -> error("unsupported $method")
        }
        val res = http.send(builder.build(), HttpResponse.BodyHandlers.ofString())
        if (res.statusCode() !in 200..299) {
            error("Google $method HTTP ${res.statusCode()}")
        }
        return res.body()
    }

    private fun enc(s: String): String = URLEncoder.encode(s, StandardCharsets.UTF_8)
}

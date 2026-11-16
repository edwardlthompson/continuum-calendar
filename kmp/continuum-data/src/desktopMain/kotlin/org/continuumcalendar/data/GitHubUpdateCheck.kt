package org.continuumcalendar.data

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

sealed class UpdateCheckResult {
    data object UpToDate : UpdateCheckResult()
    data class Available(val version: String, val url: String) : UpdateCheckResult()
    data class Failed(val message: String) : UpdateCheckResult()
}

object GitHubUpdateCheck {
    private const val API =
        "https://api.github.com/repos/edwardlthompson/continuum-calendar/releases/latest"
    const val RELEASES_PAGE =
        "https://github.com/edwardlthompson/continuum-calendar/releases/latest"

    private val http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    fun check(currentVersion: String): UpdateCheckResult {
        return runCatching {
            val req = HttpRequest.newBuilder()
                .uri(URI.create(API))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "ContinuumCalendar-CMP")
                .GET()
                .build()
            val res = http.send(req, HttpResponse.BodyHandlers.ofString())
            if (res.statusCode() !in 200..299) {
                return UpdateCheckResult.Failed("HTTP ${res.statusCode()}")
            }
            val tag = Regex(""""tag_name"\s*:\s*"([^"]+)"""").find(res.body())
                ?.groupValues?.get(1)
                ?.removePrefix("v")
                ?.trim()
                ?: return UpdateCheckResult.Failed("missing tag")
            val html = Regex(""""html_url"\s*:\s*"([^"]+)"""").find(res.body())
                ?.groupValues?.get(1)
                ?: RELEASES_PAGE
            if (normalize(tag) == normalize(currentVersion)) {
                UpdateCheckResult.UpToDate
            } else {
                UpdateCheckResult.Available(tag, html)
            }
        }.getOrElse { UpdateCheckResult.Failed(it.message ?: "update check failed") }
    }

    private fun normalize(v: String): String =
        v.trim().removePrefix("v").lowercase()
}

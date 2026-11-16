package org.continuumcalendar.data.google

import org.continuumcalendar.domain.model.GoogleOAuthTokens
import org.continuumcalendar.domain.model.SystemEpochMs
import org.continuumcalendar.domain.model.isExpired
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

object GoogleAuthRefresh {
    private val http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    fun ensureFresh(tokens: GoogleOAuthTokens, clientSecret: String?): GoogleOAuthTokens {
        if (!tokens.isExpired()) return tokens
        val refresh = tokens.refreshToken ?: error("No refresh token")
        val clientId = tokens.clientId?.takeIf { it.isNotBlank() }
            ?: System.getenv("VITE_GOOGLE_CLIENT_ID")?.trim().orEmpty().ifBlank {
                error("Missing Google client id for refresh")
            }
        val secret = clientSecret?.takeIf { it.isNotBlank() }
            ?: System.getenv("VITE_GOOGLE_CLIENT_SECRET")?.trim().orEmpty()
        val form = buildString {
            append("client_id=").append(enc(clientId))
            append("&refresh_token=").append(enc(refresh))
            append("&grant_type=refresh_token")
            if (secret.isNotBlank()) append("&client_secret=").append(enc(secret))
        }
        val req = HttpRequest.newBuilder()
            .uri(URI.create("https://oauth2.googleapis.com/token"))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build()
        val res = http.send(req, HttpResponse.BodyHandlers.ofString())
        if (res.statusCode() !in 200..299) {
            error("Token refresh failed: HTTP ${res.statusCode()}")
        }
        val body = res.body()
        val access = stringField(body, "access_token") ?: error("No access_token in refresh")
        val expiresIn = longField(body, "expires_in") ?: 3600L
        val next = tokens.copy(
            accessToken = access,
            expiresAt = SystemEpochMs.now() + expiresIn * 1000,
            scope = stringField(body, "scope") ?: tokens.scope,
            tokenType = stringField(body, "token_type") ?: tokens.tokenType,
            clientId = clientId,
        )
        TauriTokenVault.save(next)
        return next
    }

    private fun enc(s: String): String =
        URLEncoder.encode(s, StandardCharsets.UTF_8)

    private fun stringField(json: String, key: String): String? {
        val re = Regex(""""$key"\s*:\s*"((?:\\.|[^"\\])*)"""")
        return re.find(json)?.groupValues?.get(1)
    }

    private fun longField(json: String, key: String): Long? {
        val re = Regex(""""$key"\s*:\s*(\d+)""")
        return re.find(json)?.groupValues?.get(1)?.toLongOrNull()
    }
}

/** Load client id/secret from apps/desktop/.env (never log values). */
fun loadDesktopEnvCredentials(workingDir: File = File(System.getProperty("user.dir") ?: ".")): Pair<String?, String?> {
    val candidates = listOf(
        File(workingDir, "apps/desktop/.env"),
        File(workingDir, "../apps/desktop/.env"),
        File(workingDir, "../../apps/desktop/.env"),
    )
    for (env in candidates) {
        if (!env.isFile) continue
        var id: String? = null
        var secret: String? = null
        env.readLines(Charsets.UTF_8).forEach { line ->
            val t = line.trim()
            if (t.startsWith("#") || !t.contains("=")) return@forEach
            val i = t.indexOf('=')
            val k = t.substring(0, i).trim()
            val v = t.substring(i + 1).trim().trim('"')
            when (k) {
                "VITE_GOOGLE_CLIENT_ID" -> id = v
                "VITE_GOOGLE_CLIENT_SECRET" -> secret = v
            }
        }
        return id to secret
    }
    return System.getenv("VITE_GOOGLE_CLIENT_ID") to System.getenv("VITE_GOOGLE_CLIENT_SECRET")
}

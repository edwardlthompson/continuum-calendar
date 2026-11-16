package org.continuumcalendar.data.google

import org.continuumcalendar.domain.model.GoogleOAuthTokens
import org.continuumcalendar.domain.model.SystemEpochMs
import org.continuumcalendar.domain.oauth.GoogleOAuthScopes
import java.awt.Desktop
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

object GoogleDesktopSignIn {
    private val http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    /**
     * Full desktop sign-in: loopback → browser → code exchange → vault.
     * Blocking; call from Dispatchers.IO.
     */
    fun signIn(
        clientId: String,
        clientSecret: String?,
        includeTasks: Boolean = false,
        openBrowser: (String) -> Unit = ::defaultBrowse,
    ): GoogleOAuthTokens {
        require(clientId.isNotBlank()) { "Missing Google client id" }
        val (server, redirectUri) = GoogleLoopbackOAuth.openServer()
        val pkce = GooglePkce.newSession()
        val authUrl = buildAuthUrl(clientId, redirectUri, pkce, includeTasks)
        openBrowser(authUrl)
        val redirect = GoogleLoopbackOAuth.awaitCode(server, redirectUri)
        if (redirect.state != pkce.state) error("OAuth state mismatch")
        val tokens = exchangeCode(
            clientId = clientId,
            clientSecret = clientSecret,
            redirectUri = redirectUri,
            code = redirect.code,
            verifier = pkce.verifier,
        )
        TauriTokenVault.save(tokens)
        return tokens
    }

    fun buildAuthUrl(
        clientId: String,
        redirectUri: String,
        pkce: PkceSession,
        includeTasks: Boolean = false,
    ): String {
        val q = listOf(
            "client_id" to clientId,
            "redirect_uri" to redirectUri,
            "response_type" to "code",
            "scope" to GoogleOAuthScopes.desktopSignIn(includeTasks),
            "access_type" to "offline",
            "include_granted_scopes" to "true",
            "prompt" to "consent",
            "state" to pkce.state,
            "code_challenge" to pkce.challenge,
            "code_challenge_method" to "S256",
        ).joinToString("&") { (k, v) -> "${enc(k)}=${enc(v)}" }
        return "https://accounts.google.com/o/oauth2/v2/auth?$q"
    }

    fun exchangeCode(
        clientId: String,
        clientSecret: String?,
        redirectUri: String,
        code: String,
        verifier: String,
    ): GoogleOAuthTokens {
        val form = buildString {
            append("client_id=").append(enc(clientId))
            append("&code=").append(enc(code))
            append("&code_verifier=").append(enc(verifier))
            append("&grant_type=authorization_code")
            append("&redirect_uri=").append(enc(redirectUri))
            val secret = clientSecret?.takeIf { it.isNotBlank() }.orEmpty()
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
            error("Token exchange failed: HTTP ${res.statusCode()}")
        }
        val body = res.body()
        val access = stringField(body, "access_token") ?: error("No access_token")
        val expiresIn = longField(body, "expires_in") ?: 3600L
        return GoogleOAuthTokens(
            accessToken = access,
            refreshToken = stringField(body, "refresh_token"),
            expiresAt = SystemEpochMs.now() + expiresIn * 1000,
            scope = stringField(body, "scope").orEmpty(),
            tokenType = stringField(body, "token_type") ?: "Bearer",
            clientId = clientId,
        )
    }

    private fun defaultBrowse(url: String) {
        if (!Desktop.isDesktopSupported()) error("Desktop browse unsupported")
        Desktop.getDesktop().browse(URI(url))
    }

    private fun enc(s: String): String = URLEncoder.encode(s, StandardCharsets.UTF_8)

    private fun stringField(json: String, key: String): String? =
        Regex(""""$key"\s*:\s*"((?:\\.|[^"\\])*)"""").find(json)?.groupValues?.get(1)

    private fun longField(json: String, key: String): Long? =
        Regex(""""$key"\s*:\s*(\d+)""").find(json)?.groupValues?.get(1)?.toLongOrNull()
}

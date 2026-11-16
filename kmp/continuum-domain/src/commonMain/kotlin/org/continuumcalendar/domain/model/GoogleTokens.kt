package org.continuumcalendar.domain.model

/** Matches packages/shared GoogleOAuthTokens (Tauri google-tokens.json). */
data class GoogleOAuthTokens(
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresAt: Long,
    val scope: String = "",
    val tokenType: String? = "Bearer",
    val clientId: String? = null,
)

fun GoogleOAuthTokens.isExpired(nowMs: Long = SystemEpochMs.now(), skewMs: Long = 60_000): Boolean =
    nowMs >= expiresAt - skewMs

object SystemEpochMs {
    var provider: () -> Long = { kotlinx.datetime.Clock.System.now().toEpochMilliseconds() }
    fun now(): Long = provider()
}

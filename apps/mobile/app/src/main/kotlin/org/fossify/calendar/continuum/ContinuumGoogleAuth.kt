package org.fossify.calendar.continuum

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

data class ContinuumTokens(
    val accessToken: String,
    val refreshToken: String?,
    val expiresAt: Long,
    val scope: String,
)

/**
 * Google OAuth (Authorization Code + PKCE) for Continuum — browser/Custom Tabs flow,
 * FOSS-friendly (no Play Services Sign-In SDK).
 */
class ContinuumGoogleAuth(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isSignedIn(): Boolean = loadTokens()?.accessToken?.isNotBlank() == true

    fun loadTokens(): ContinuumTokens? {
        val access = prefs.getString(KEY_ACCESS, null) ?: return null
        return ContinuumTokens(
            accessToken = access,
            refreshToken = prefs.getString(KEY_REFRESH, null),
            expiresAt = prefs.getLong(KEY_EXPIRES, 0L),
            scope = prefs.getString(KEY_SCOPE, ContinuumConsts.CALENDAR_SCOPE) ?: "",
        )
    }

    fun saveTokens(tokens: ContinuumTokens?) {
        prefs.edit().apply {
            if (tokens == null) {
                remove(KEY_ACCESS).remove(KEY_REFRESH).remove(KEY_EXPIRES).remove(KEY_SCOPE)
            } else {
                putString(KEY_ACCESS, tokens.accessToken)
                putString(KEY_REFRESH, tokens.refreshToken)
                putLong(KEY_EXPIRES, tokens.expiresAt)
                putString(KEY_SCOPE, tokens.scope)
            }
        }.apply()
    }

    fun signOut() = saveTokens(null)

    /**
     * Return a non-expired access token, refreshing with the refresh_token when needed.
     * Returns null when unsigned or refresh fails.
     */
    fun ensureFreshTokens(skewMs: Long = 60_000L): ContinuumTokens? {
        val tokens = loadTokens() ?: return null
        if (tokens.expiresAt > System.currentTimeMillis() + skewMs) return tokens
        val refresh = tokens.refreshToken ?: return null
        return try {
            refreshAccessToken(refresh, previous = tokens)
        } catch (e: Exception) {
            ContinuumDiagnostics.w("Token refresh failed", e)
            null
        }
    }

    fun refreshAccessToken(refreshToken: String, previous: ContinuumTokens? = loadTokens()): ContinuumTokens {
        val clientId = ContinuumSettingsOAuth.clientId()
        if (clientId.isBlank()) error("Missing Continuum Google Client ID")
        val params = linkedMapOf(
            "client_id" to clientId,
            "refresh_token" to refreshToken,
            "grant_type" to "refresh_token",
        )
        val secret = ContinuumSettingsOAuth.clientSecretForExchange()
        if (secret.isNotBlank()) params["client_secret"] = secret
        val body = params.entries.joinToString("&") { "${it.key}=${Uri.encode(it.value)}" }
        var lastError: Exception? = null
        repeat(3) { attempt ->
            try {
                val resp = ContinuumHttpsPost.postForm(
                    context,
                    "https://oauth2.googleapis.com/token",
                    body,
                )
                if (resp.code !in 200..299) {
                    throw IllegalStateException(
                        "Token refresh failed (${resp.code}): ${resp.body.take(240)}",
                    )
                }
                val json = JSONObject(resp.body)
                val next = ContinuumTokens(
                    accessToken = json.getString("access_token"),
                    refreshToken = if (json.has("refresh_token")) {
                        json.getString("refresh_token")
                    } else {
                        refreshToken
                    },
                    expiresAt = System.currentTimeMillis() + json.getLong("expires_in") * 1000L,
                    scope = json.optString("scope", previous?.scope ?: ContinuumConsts.CALENDAR_SCOPE),
                )
                saveTokens(next)
                return next
            } catch (e: Exception) {
                lastError = e
                ContinuumDiagnostics.w("Token refresh attempt ${attempt + 1}/3 failed", e)
                if (attempt < 2) Thread.sleep(600L * (attempt + 1))
            }
        }
        throw lastError ?: IllegalStateException("Token refresh failed")
    }

    fun buildAuthorizationUri(clientId: String, redirectUri: String): Uri {
        val verifier = randomUrlSafe(32)
        val challenge = sha256UrlSafe(verifier)
        val state = randomUrlSafe(16)
        prefs.edit()
            .putString(KEY_VERIFIER, verifier)
            .putString(KEY_STATE, state)
            .putString(KEY_CLIENT_ID, clientId)
            .putString(KEY_REDIRECT, redirectUri)
            .apply()
        return Uri.parse("https://accounts.google.com/o/oauth2/v2/auth").buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter(
                "scope",
                listOf(
                    ContinuumConsts.CALENDAR_SCOPE,
                    ContinuumConsts.CONTACTS_SCOPE,
                    ContinuumConsts.DRIVE_APPDATA_SCOPE,
                    ContinuumConsts.TASKS_SCOPE,
                ).joinToString(" "),
            )
            .appendQueryParameter("access_type", "offline")
            .appendQueryParameter("prompt", "consent")
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
    }

    fun exchangeCode(
        clientId: String,
        redirectUri: String,
        code: String,
        state: String,
        clientSecret: String = "",
    ): ContinuumTokens {
        val expected = prefs.getString(KEY_STATE, null)
        val verifier = prefs.getString(KEY_VERIFIER, null)
            ?: error("Missing PKCE verifier")
        if (expected != state) error("OAuth state mismatch")
        val params = linkedMapOf(
            "client_id" to clientId,
            "code" to code,
            "code_verifier" to verifier,
            "grant_type" to "authorization_code",
            "redirect_uri" to redirectUri,
        )
        // Desktop-type OAuth clients require client_secret even with PKCE.
        if (clientSecret.isNotBlank()) {
            params["client_secret"] = clientSecret
        }
        val body = params.entries.joinToString("&") { "${it.key}=${Uri.encode(it.value)}" }
        // After Custom Tabs, process DNS can be on IMS (no internet). Bind to validated network.
        var lastError: Exception? = null
        repeat(4) { attempt ->
            try {
                val resp = ContinuumHttpsPost.postForm(
                    context,
                    "https://oauth2.googleapis.com/token",
                    body,
                )
                if (resp.code !in 200..299) {
                    throw IllegalStateException(
                        "Token exchange failed (${resp.code}): ${resp.body.take(240)}",
                    )
                }
                val json = JSONObject(resp.body)
                val tokens = ContinuumTokens(
                    accessToken = json.getString("access_token"),
                    refreshToken = if (json.has("refresh_token")) json.getString("refresh_token") else null,
                    expiresAt = System.currentTimeMillis() + json.getLong("expires_in") * 1000L,
                    scope = json.optString("scope", ""),
                )
                saveTokens(tokens)
                prefs.edit()
                    .remove(KEY_VERIFIER)
                    .remove(KEY_STATE)
                    .remove(KEY_CLIENT_ID)
                    .remove(KEY_REDIRECT)
                    .apply()
                return tokens
            } catch (e: Exception) {
                lastError = e
                ContinuumDiagnostics.w("Token exchange attempt ${attempt + 1}/4 failed", e)
                if (attempt < 3) Thread.sleep(800L * (attempt + 1))
            }
        }
        throw lastError ?: IllegalStateException("Token exchange failed")
    }

    /** Client id / redirect used for the in-flight PKCE authorize (paste-URL recovery). */
    fun pendingClientId(): String? = prefs.getString(KEY_CLIENT_ID, null)
    fun pendingRedirectUri(): String? = prefs.getString(KEY_REDIRECT, null)

    data class PendingExchange(
        val code: String,
        val state: String,
        val clientId: String,
        val redirectUri: String,
        val clientSecret: String,
    )

    fun savePendingExchange(pending: PendingExchange) {
        prefs.edit()
            .putString(KEY_PENDING_CODE, pending.code)
            .putString(KEY_PENDING_STATE, pending.state)
            .putString(KEY_PENDING_CLIENT, pending.clientId)
            .putString(KEY_PENDING_REDIRECT, pending.redirectUri)
            .putString(KEY_PENDING_SECRET, pending.clientSecret)
            .putLong(KEY_PENDING_AT, System.currentTimeMillis())
            .apply()
    }

    fun loadPendingExchange(): PendingExchange? {
        val code = prefs.getString(KEY_PENDING_CODE, null) ?: return null
        val state = prefs.getString(KEY_PENDING_STATE, null) ?: return null
        val clientId = prefs.getString(KEY_PENDING_CLIENT, null) ?: return null
        val redirect = prefs.getString(KEY_PENDING_REDIRECT, null) ?: return null
        val at = prefs.getLong(KEY_PENDING_AT, 0L)
        // Auth codes expire quickly (~10 min); drop stale.
        if (at > 0 && System.currentTimeMillis() - at > 8 * 60_000L) {
            clearPendingExchange()
            return null
        }
        return PendingExchange(
            code = code,
            state = state,
            clientId = clientId,
            redirectUri = redirect,
            clientSecret = prefs.getString(KEY_PENDING_SECRET, "") ?: "",
        )
    }

    fun clearPendingExchange() {
        prefs.edit()
            .remove(KEY_PENDING_CODE)
            .remove(KEY_PENDING_STATE)
            .remove(KEY_PENDING_CLIENT)
            .remove(KEY_PENDING_REDIRECT)
            .remove(KEY_PENDING_SECRET)
            .remove(KEY_PENDING_AT)
            .apply()
    }

    companion object {
        private const val PREFS = "continuum_google_auth"
        private const val KEY_ACCESS = "access"
        private const val KEY_REFRESH = "refresh"
        private const val KEY_EXPIRES = "expires"
        private const val KEY_SCOPE = "scope"
        private const val KEY_VERIFIER = "verifier"
        private const val KEY_STATE = "state"
        private const val KEY_CLIENT_ID = "client_id"
        private const val KEY_REDIRECT = "redirect_uri"
        private const val KEY_PENDING_CODE = "pending_code"
        private const val KEY_PENDING_STATE = "pending_state"
        private const val KEY_PENDING_CLIENT = "pending_client"
        private const val KEY_PENDING_REDIRECT = "pending_redirect"
        private const val KEY_PENDING_SECRET = "pending_secret"
        private const val KEY_PENDING_AT = "pending_at"

        private fun randomUrlSafe(bytes: Int): String {
            val buf = ByteArray(bytes)
            SecureRandom().nextBytes(buf)
            return Base64.encodeToString(buf, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        }

        private fun sha256UrlSafe(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        }
    }
}

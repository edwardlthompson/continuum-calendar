package org.fossify.calendar.continuum

import android.app.Activity
import android.content.Context
import org.fossify.calendar.BuildConfig

/** Starts Continuum Google API OAuth (Drive App Data) for cross-device peer sync. */
object ContinuumSettingsOAuth {
    /** Desktop client (same GCP project) — used when Android client is absent. */
    fun desktopClientId(): String = BuildConfig.CONTINUUM_GOOGLE_CLIENT_ID.trim()

    /** Preferred Android-type client for Custom Tabs reverse-DNS redirect. */
    fun androidClientId(): String = BuildConfig.CONTINUUM_GOOGLE_ANDROID_CLIENT_ID.trim()

    fun clientId(): String = androidClientId().ifBlank { desktopClientId() }

    fun isConfigured(): Boolean = clientId().isNotBlank()

    fun usesAndroidClient(): Boolean = androidClientId().isNotBlank()

    fun clientSecretForExchange(): String =
        if (usesAndroidClient()) "" else BuildConfig.CONTINUUM_GOOGLE_CLIENT_SECRET.trim()

    /**
     * Android client: com.googleusercontent.apps.<prefix>:/oauth2redirect
     * Fallback package scheme (blocked by Google for Desktop clients — paste-URL recovery).
     */
    fun redirectUri(context: Context): String {
        val androidId = androidClientId()
        if (androidId.contains(".apps.googleusercontent.com")) {
            val prefix = androidId.substringBefore(".apps.googleusercontent.com")
            return "com.googleusercontent.apps.$prefix:/oauth2redirect"
        }
        return "${context.packageName}://oauth2redirect"
    }

    fun redirectScheme(context: Context? = null): String {
        val androidId = androidClientId()
        if (androidId.contains(".apps.googleusercontent.com")) {
            val prefix = androidId.substringBefore(".apps.googleusercontent.com")
            return "com.googleusercontent.apps.$prefix"
        }
        return context?.packageName ?: "org.continuumcalendar.app.debug"
    }

    /** @return true if the auth activity was started */
    fun startIfConfigured(activity: Activity): Boolean {
        if (!isConfigured()) return false
        ContinuumAuthActivity.start(activity, clientId(), redirectUri(activity))
        return true
    }
}

package org.fossify.calendar.continuum

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import org.fossify.calendar.BuildConfig
import org.fossify.calendar.R

/**
 * Continuum Google OAuth via Custom Tabs + Android OAuth client reverse-DNS redirect
 * (`com.googleusercontent.apps.<prefix>:/oauth2redirect`).
 *
 * Deep-link return and paste-URL recovery both land here with `code` + `state`.
 */
class ContinuumAuthActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent?.data
        if (data != null && (!data.getQueryParameter("code").isNullOrBlank() || !data.getQueryParameter("error").isNullOrBlank())) {
            handleRedirect(data)
            return
        }

        // Explicit paste recovery: EXTRA_PASTE_URL
        val paste = intent?.getStringExtra(EXTRA_PASTE_URL)
        if (!paste.isNullOrBlank()) {
            handleRedirect(Uri.parse(paste.trim()))
            return
        }

        val clientId = ContinuumSettingsOAuth.clientId()
        if (clientId.isBlank()) {
            Toast.makeText(this, R.string.continuum_google_client_id_missing, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        if (!ContinuumSettingsOAuth.usesAndroidClient()) {
            Toast.makeText(this, R.string.continuum_peer_sync_need_android_client, Toast.LENGTH_LONG).show()
            // Still open auth — paste-URL recovery may work after Console is fixed.
        }

        val redirect = ContinuumSettingsOAuth.redirectUri(this)
        val auth = ContinuumGoogleAuth(this)
        val authUri = auth.buildAuthorizationUri(clientId, redirect)
        ContinuumDiagnostics.i(
            "OAuth start package=${packageName} clientId=$clientId redirect=$redirect authHost=${authUri.host}",
        )
        try {
            val tabs = CustomTabsIntent.Builder().build()
            // Prefer Chrome — Firefox Custom Tabs on Oppo leaves the process unable to open sockets.
            val chrome = "com.android.chrome"
            if (packageManager.getLaunchIntentForPackage(chrome) != null) {
                tabs.intent.setPackage(chrome)
            }
            tabs.launchUrl(this, authUri)
            Toast.makeText(this, R.string.continuum_peer_sync_opening_browser, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            ContinuumDiagnostics.e("Custom Tabs launch failed — falling back to VIEW", e)
            startActivity(Intent(Intent.ACTION_VIEW, authUri))
        }
        // Finish this instance; redirect starts a new ContinuumAuthActivity via intent-filter.
        finish()
    }

    private fun handleRedirect(uri: Uri) {
        val error = uri.getQueryParameter("error")
        if (!error.isNullOrBlank()) {
            val desc = uri.getQueryParameter("error_description") ?: error
            ContinuumDiagnostics.e("OAuth redirect error: $desc")
            Toast.makeText(
                this,
                getString(R.string.continuum_peer_sync_sign_in_failed, desc),
                Toast.LENGTH_LONG,
            ).show()
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        val code = uri.getQueryParameter("code")
        val state = uri.getQueryParameter("state") ?: ""
        if (code.isNullOrBlank()) {
            Toast.makeText(this, R.string.continuum_peer_sync_paste_invalid, Toast.LENGTH_LONG).show()
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        val auth = ContinuumGoogleAuth(this)
        val redirect = auth.pendingRedirectUri() ?: ContinuumSettingsOAuth.redirectUri(this)
        val clientId = auth.pendingClientId() ?: ContinuumSettingsOAuth.clientId()
        val secret =
            if (clientId == ContinuumSettingsOAuth.androidClientId() &&
                ContinuumSettingsOAuth.androidClientId().isNotBlank()
            ) {
                ""
            } else {
                ContinuumSettingsOAuth.clientSecretForExchange().ifBlank {
                    BuildConfig.CONTINUUM_GOOGLE_CLIENT_SECRET.trim()
                }
            }
        // Do NOT exchange here — Oppo refuses all sockets while AuthActivity/Custom Tab is active.
        // Persist + WorkManager after MainActivity is foreground with CONNECTED network.
        auth.savePendingExchange(
            ContinuumGoogleAuth.PendingExchange(
                code = code,
                state = state,
                clientId = clientId,
                redirectUri = redirect,
                clientSecret = secret,
            ),
        )
        ContinuumDiagnostics.i("OAuth code received — scheduling deferred token exchange")
        Toast.makeText(this, R.string.continuum_peer_sync_exchanging_token, Toast.LENGTH_LONG).show()
        ContinuumTokenExchangeWorker.enqueue(this)
        startActivity(
            Intent(this, org.fossify.calendar.activities.MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        setResult(RESULT_OK)
        finish()
    }

    companion object {
        const val EXTRA_CLIENT_ID = "client_id"
        const val EXTRA_REDIRECT = "redirect_uri"
        const val EXTRA_PASTE_URL = "paste_url"

        fun start(activity: Activity, clientId: String, redirectUri: String) {
            activity.startActivity(
                Intent(activity, ContinuumAuthActivity::class.java)
                    .putExtra(EXTRA_CLIENT_ID, clientId)
                    .putExtra(EXTRA_REDIRECT, redirectUri),
            )
        }

        fun startWithPasteUrl(activity: Activity, url: String) {
            activity.startActivity(
                Intent(activity, ContinuumAuthActivity::class.java)
                    .putExtra(EXTRA_PASTE_URL, url),
            )
        }
    }
}

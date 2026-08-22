package org.fossify.calendar.continuum

import android.content.Context
import org.fossify.calendar.R

/** Map Google OAuth error query params to Testing-mode guidance. */
object ContinuumOAuthErrors {
    fun isTestingMode(error: String, description: String): Boolean {
        val blob = "$error $description"
        return blob.contains("access_denied", ignoreCase = true) ||
            blob.contains("unknown error", ignoreCase = true) ||
            blob.contains("verification", ignoreCase = true)
    }

    /** Refresh token dead — clear local tokens; do not retry. */
    fun isInvalidGrant(blob: String): Boolean {
        return blob.contains("invalid_grant", ignoreCase = true) ||
            blob.contains("expired or revoked", ignoreCase = true)
    }

    fun message(context: Context, error: String, description: String): String {
        return if (isTestingMode(error, description)) {
            context.getString(R.string.continuum_oauth_testing_mode)
        } else {
            context.getString(R.string.continuum_peer_sync_sign_in_failed, description.ifBlank { error })
        }
    }
}

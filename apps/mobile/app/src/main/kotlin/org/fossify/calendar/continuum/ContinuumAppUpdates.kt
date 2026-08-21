package org.fossify.calendar.continuum

import org.fossify.calendar.BuildConfig
import org.fossify.calendar.R
import org.fossify.calendar.activities.SimpleActivity
import org.fossify.commons.dialogs.ConfirmationAdvancedDialog
import org.fossify.commons.extensions.launchViewIntent
import org.fossify.commons.helpers.ensureBackgroundThread

object ContinuumAppUpdates {
    fun onLaunch(activity: SimpleActivity) {
        val prefs = ContinuumUpdatePrefs(activity)
        val current = BuildConfig.VERSION_NAME
        if (ContinuumProductUpdate.shouldNudgeDonate(prefs.lastSeenVersion(), current)) {
            showDonate(activity) { prefs.markVersionSeen(current) }
            return
        }
        prefs.markVersionSeen(current)
        if (!ContinuumProductUpdate.shouldCheckDaily(prefs.lastCheckAt(), System.currentTimeMillis())) {
            return
        }
        ensureBackgroundThread {
            val release = try {
                ContinuumGithubRelease.fetchLatest(activity)
            } catch (_: Exception) {
                null
            }
            val now = System.currentTimeMillis()
            prefs.markChecked(now)
            val asset = release?.let { ContinuumProductUpdate.selectApkAsset(it.assets) }
            val latest = asset?.version
            if (
                !activity.isFinishing &&
                ContinuumProductUpdate.shouldPromptUpdate(current, latest, prefs.dismissedVersion()) &&
                latest != null
            ) {
                val url = asset?.url?.ifBlank { null } ?: release?.htmlUrl ?: ContinuumProductUpdate.RELEASES_PAGE
                activity.runOnUiThread {
                    if (!activity.isFinishing) showUpdate(activity, prefs, latest, url)
                }
            }
        }
    }

    private fun showDonate(activity: SimpleActivity, onDone: () -> Unit) {
        ConfirmationAdvancedDialog(
            activity = activity,
            message = activity.getString(R.string.about_donate_nudge_message),
            messageId = 0,
            positive = R.string.about_donate,
            negative = R.string.about_not_now,
        ) { donate ->
            onDone()
            if (donate) activity.launchViewIntent(ContinuumAboutLinks.DONATION_URL)
        }
    }

    private fun showUpdate(
        activity: SimpleActivity,
        prefs: ContinuumUpdatePrefs,
        version: String,
        url: String,
    ) {
        ConfirmationAdvancedDialog(
            activity = activity,
            message = activity.getString(R.string.about_update_message, version),
            messageId = 0,
            positive = R.string.about_install,
            negative = R.string.about_not_now,
        ) { install ->
            prefs.markChecked(System.currentTimeMillis(), version)
            if (install) activity.launchViewIntent(url)
        }
    }
}

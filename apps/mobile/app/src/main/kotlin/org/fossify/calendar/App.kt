package org.fossify.calendar

import android.widget.Toast
import org.fossify.calendar.continuum.ContinuumDiagnostics
import org.fossify.calendar.continuum.ContinuumSyncWorker
import org.fossify.calendar.continuum.HighRefreshDisplay
import org.fossify.calendar.extensions.hasDummyAlarm
import org.fossify.calendar.jobs.AppStartupWorker
import org.fossify.commons.FossifyApp
import org.fossify.commons.extensions.baseConfig
import org.fossify.commons.helpers.SIDELOADING_FALSE

class App : FossifyApp() {
    override fun onCreate() {
        super.onCreate()
        // Continuum uses applicationId org.continuumcalendar.app — not a pirate Fossify build.
        // Commons anti-repackaging (ic_camera_vector probe) can false-positive; skip the scare dialog.
        baseConfig.appSideloadingStatus = SIDELOADING_FALSE
        ContinuumDiagnostics.install(this)
        HighRefreshDisplay.install(this)
        ContinuumDiagnostics.lastCrashSummary(this)?.let { summary ->
            ContinuumDiagnostics.e("Previous crash detected:\n$summary")
            Toast.makeText(
                this,
                getString(R.string.continuum_previous_crash_notice),
                Toast.LENGTH_LONG,
            ).show()
            ContinuumDiagnostics.clearCrashMarker(this)
        }
        if (!hasDummyAlarm()) {
            AppStartupWorker.start(this)
        }
        ContinuumSyncWorker.enqueue(this)
    }
}

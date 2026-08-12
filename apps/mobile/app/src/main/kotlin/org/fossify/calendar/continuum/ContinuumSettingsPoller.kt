package org.fossify.calendar.continuum

import android.content.Context
import android.os.Handler
import android.os.Looper

/** Foreground ~1s settings poll while Continuum UI is active. */
class ContinuumSettingsPoller(
    private val context: Context,
    private val onUpdate: (ContinuumSettings) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var lastNoAuthLogAt = 0L
    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            Thread {
                try {
                    val sync = ContinuumSettingsSync(context)
                    if (!sync.hasDriveAuth()) {
                        // Throttle: once per minute (avoid flooding continuum-errors.log).
                        val now = System.currentTimeMillis()
                        if (now - lastNoAuthLogAt > 60_000L) {
                            lastNoAuthLogAt = now
                            ContinuumDiagnostics.w(
                                "Peer sync idle — Continuum Google API not signed in " +
                                    "(Settings → Continuum → Sync with desktop)",
                            )
                        }
                        if (running) handler.postDelayed(this, 5000L)
                        return@Thread
                    }
                    val before = sync.lastRevision()
                    val beforeBirthdays = sync.loadLocal().showContactBirthdays
                    // Peer reconcile: pull desktop writes, flush pending Android writes.
                    val settings = sync.reconcilePeerRemote()
                    var localEventsChanged = false
                    try {
                        localEventsChanged = ContinuumLocalEventsSync(context).reconcilePeerRemote()
                    } catch (e: Exception) {
                        ContinuumDiagnostics.w("Peer local-events poll failed", e)
                    }
                    val changed =
                        sync.lastRevision() > before ||
                            settings.showContactBirthdays != beforeBirthdays ||
                            localEventsChanged
                    if (changed) {
                        handler.post { onUpdate(settings) }
                    }
                } catch (e: Exception) {
                    ContinuumDiagnostics.w("Peer settings poll failed", e)
                }
                if (running) handler.postDelayed(this, 1000L)
            }.start()
        }
    }

    fun start() {
        if (running) return
        running = true
        handler.post(tick)
    }

    fun stop() {
        running = false
        handler.removeCallbacks(tick)
    }
}

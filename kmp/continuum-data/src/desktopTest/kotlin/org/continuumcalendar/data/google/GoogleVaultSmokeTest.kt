package org.continuumcalendar.data.google

import kotlin.test.Test
import kotlin.test.assertTrue
import org.continuumcalendar.domain.model.SystemEpochMs

/**
 * Local-only smoke: reuses Tauri vault + apps/desktop/.env.
 * Skips when vault file is absent (CI / clean machines).
 */
class GoogleVaultSmokeTest {
    @Test
    fun loadRefreshAndFetchWhenVaultPresent() {
        val stored = TauriTokenVault.load() ?: return
        val (_, secret) = loadDesktopEnvCredentials(
            java.io.File("..").resolve("apps/desktop").parentFile?.parentFile
                ?: java.io.File("."),
        )
        val also = loadDesktopEnvCredentials(java.io.File("../.."))
        val fromKmp = loadDesktopEnvCredentials(java.io.File("."))
        val clientSecret = secret ?: also.second ?: fromKmp.second
        val withId = if (stored.clientId.isNullOrBlank()) {
            stored.copy(clientId = also.first ?: fromKmp.first)
        } else {
            stored
        }
        val fresh = GoogleAuthRefresh.ensureFresh(withId, clientSecret)
        assertTrue(fresh.accessToken.isNotBlank())
        val snap = GoogleCalendarFetch.syncVisibleRange(fresh, SystemEpochMs.now(), 14)
        assertTrue(snap.calendars.isNotEmpty(), "expected at least one calendar")
        // Events may be empty for sparse calendars; calendars prove auth worked.
        println("Google vault smoke: ${snap.calendars.size} calendars, ${snap.events.size} events")
    }
}

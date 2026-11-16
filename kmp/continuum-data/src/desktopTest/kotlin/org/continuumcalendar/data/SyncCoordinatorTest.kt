package org.continuumcalendar.data

import kotlinx.coroutines.runBlocking
import org.continuumcalendar.platform.ContinuumClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncCoordinatorTest {
    @Test
    fun seedsWhenRemoteMissing() = runBlocking {
        val settings = InMemorySettingsRepository()
        val drive = InMemoryDriveAppDataPort(settingsJson = null)
        val sync = SyncCoordinator(settings, drive, ContinuumClock { 1_000L })
        val result = sync.reconcileSettings()
        assertEquals(SyncResult.Seeded, result)
        assertTrue(drive.settingsJson != null)
    }

    @Test
    fun pullsWhenRemoteNewer() = runBlocking {
        val settings = InMemorySettingsRepository(revision = 1)
        val drive = InMemoryDriveAppDataPort(
            settingsJson = """{"schemaVersion":1,"revision":5,"updatedAt":"x"}""",
        )
        val sync = SyncCoordinator(settings, drive, ContinuumClock { 1_000L })
        assertEquals(SyncResult.Pulled, sync.reconcileSettings())
        assertEquals(5, settings.localRevision())
    }
}

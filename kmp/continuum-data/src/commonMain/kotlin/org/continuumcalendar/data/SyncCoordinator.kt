package org.continuumcalendar.data

import org.continuumcalendar.domain.model.ContinuumSettings
import org.continuumcalendar.domain.model.defaultContinuumSettings
import org.continuumcalendar.domain.sync.PeerReconcileAction
import org.continuumcalendar.domain.sync.decidePeerReconcile
import org.continuumcalendar.platform.ContinuumClock

/** Peer ports — fakes in tests; Drive REST adapters later. */
interface DriveAppDataPort {
    suspend fun readSettingsJson(): String?
    suspend fun writeSettingsJson(body: String): Boolean
}

interface SettingsRepository {
    suspend fun load(): ContinuumSettings
    suspend fun save(settings: ContinuumSettings)
    suspend fun localRevision(): Long
    suspend fun setLocalRevision(revision: Long)
    suspend fun pendingLocalPush(): Boolean
    suspend fun setPendingLocalPush(pending: Boolean)
}

class InMemorySettingsRepository(
    private var settings: ContinuumSettings = defaultContinuumSettings(),
    private var revision: Long = 1,
    private var pending: Boolean = false,
) : SettingsRepository {
    override suspend fun load(): ContinuumSettings = settings
    override suspend fun save(settings: ContinuumSettings) {
        this.settings = settings
    }

    override suspend fun localRevision(): Long = revision
    override suspend fun setLocalRevision(revision: Long) {
        this.revision = revision
    }

    override suspend fun pendingLocalPush(): Boolean = pending
    override suspend fun setPendingLocalPush(pending: Boolean) {
        this.pending = pending
    }
}

sealed class SyncResult {
    data object Noop : SyncResult()
    data object Seeded : SyncResult()
    data object Pulled : SyncResult()
    data object Pushed : SyncResult()
    data class Failed(val message: String) : SyncResult()
}

/**
 * Single-writer peer reconcile for settings (Drive App Data).
 * Network I/O only through [DriveAppDataPort]; pure decide via domain.
 */
class SyncCoordinator(
    private val settingsRepo: SettingsRepository,
    private val drive: DriveAppDataPort,
    private val clock: ContinuumClock,
) {
    private var busy = false

    suspend fun reconcileSettings(): SyncResult {
        if (busy) return SyncResult.Failed("reconcile already running")
        busy = true
        return try {
            val remoteBody = drive.readSettingsJson()
            val hasRemote = remoteBody != null
            val remoteRevision = parseRevision(remoteBody) ?: 0L
            val action = decidePeerReconcile(
                hasRemote = hasRemote,
                remoteRevision = remoteRevision,
                localRevision = settingsRepo.localRevision(),
                pendingLocalPush = settingsRepo.pendingLocalPush(),
            )
            when (action) {
                PeerReconcileAction.NOOP -> SyncResult.Noop
                PeerReconcileAction.SEED -> {
                    val ok = drive.writeSettingsJson(
                        """{"schemaVersion":1,"revision":${settingsRepo.localRevision()},"updatedAt":"${clock.nowEpochMs()}"}""",
                    )
                    if (ok) SyncResult.Seeded else SyncResult.Failed("seed write failed")
                }
                PeerReconcileAction.PULL -> {
                    settingsRepo.setLocalRevision(remoteRevision)
                    settingsRepo.setPendingLocalPush(false)
                    SyncResult.Pulled
                }
                PeerReconcileAction.PUSH_PENDING -> {
                    val next = settingsRepo.localRevision().coerceAtLeast(remoteRevision) + 1
                    val ok = drive.writeSettingsJson(
                        """{"schemaVersion":1,"revision":$next,"updatedAt":"${clock.nowEpochMs()}"}""",
                    )
                    if (!ok) return SyncResult.Failed("push write failed")
                    settingsRepo.setLocalRevision(next)
                    settingsRepo.setPendingLocalPush(false)
                    SyncResult.Pushed
                }
            }
        } catch (t: Throwable) {
            SyncResult.Failed(t.message ?: "reconcile error")
        } finally {
            busy = false
        }
    }

    private fun parseRevision(body: String?): Long? {
        if (body.isNullOrBlank()) return null
        val m = Regex(""""revision"\s*:\s*(\d+)""").find(body) ?: return null
        return m.groupValues[1].toLongOrNull()
    }
}

class InMemoryDriveAppDataPort(
    var settingsJson: String? = null,
) : DriveAppDataPort {
    override suspend fun readSettingsJson(): String? = settingsJson
    override suspend fun writeSettingsJson(body: String): Boolean {
        settingsJson = body
        return true
    }
}

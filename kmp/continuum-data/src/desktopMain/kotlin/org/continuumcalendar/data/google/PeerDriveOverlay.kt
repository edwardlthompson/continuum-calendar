package org.continuumcalendar.data.google

import org.continuumcalendar.data.settings.DesktopPeerMeta
import org.continuumcalendar.data.settings.DesktopPeerMetaStore
import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.CalendarListEntry
import org.continuumcalendar.domain.model.CalendarSource
import org.continuumcalendar.domain.model.ContinuumConsts
import org.continuumcalendar.domain.model.ContinuumSettings
import org.continuumcalendar.domain.model.GoogleOAuthTokens
import org.continuumcalendar.domain.sync.PeerReconcileAction
import org.continuumcalendar.domain.sync.decidePeerReconcile
import org.continuumcalendar.domain.sync.filterContinuumOwnedCalendars
import org.continuumcalendar.domain.sync.filterContinuumOwnedEvents
import org.continuumcalendar.domain.sync.mergeLocalEventsPayload

data class PeerOverlayResult(
    val settings: ContinuumSettings,
    val calendars: List<CalendarListEntry>,
    val events: List<CalendarEvent>,
    val note: String,
)

/**
 * Drive App Data peer overlays: settings envelope + local-events merge on top of Google Calendar.
 * Skips quietly when vault scope lacks drive.appdata.
 */
object PeerDriveOverlay {
    fun hasDriveScope(tokens: GoogleOAuthTokens): Boolean =
        tokens.scope.contains("drive.appdata")

    fun reconcile(
        tokens: GoogleOAuthTokens,
        settings: ContinuumSettings,
        calendars: List<CalendarListEntry>,
        events: List<CalendarEvent>,
        nowIso: String,
    ): PeerOverlayResult {
        if (!hasDriveScope(tokens)) {
            return PeerOverlayResult(settings, calendars, events, "peer skipped (no Drive scope)")
        }
        var meta = DesktopPeerMetaStore.load()
        val nextSettings = runCatching {
            reconcileSettings(tokens.accessToken, settings, meta, nowIso).also {
                meta = DesktopPeerMetaStore.load()
            }
        }.getOrElse { settings }
        val settingsNote = if (nextSettings !== settings) "settings pulled" else "settings ok"

        val overlay = runCatching {
            overlayLocalEvents(tokens.accessToken, calendars, events, meta)
        }.getOrElse {
            return PeerOverlayResult(nextSettings, calendars, events, "$settingsNote · local-events: ${it.message}")
        }
        return PeerOverlayResult(nextSettings, overlay.first, overlay.second, "$settingsNote · local-events ok")
    }

    private fun reconcileSettings(
        accessToken: String,
        local: ContinuumSettings,
        meta: DesktopPeerMeta,
        nowIso: String,
    ): ContinuumSettings {
        val remote = runCatching {
            GoogleDriveAppData.findAndDownload(accessToken, ContinuumConsts.SETTINGS_APP_DATA_FILENAME)
        }.getOrNull()
        val remoteRev = PeerEnvelopeJson.parseRevision(remote?.body) ?: 0L
        val action = decidePeerReconcile(
            hasRemote = remote != null,
            remoteRevision = remoteRev,
            localRevision = meta.settingsRevision,
            pendingLocalPush = meta.settingsPending,
        )
        return when (action) {
            PeerReconcileAction.NOOP -> local
            PeerReconcileAction.PULL -> {
                val remoteSettings = PeerEnvelopeJson.extractSettings(remote?.body) ?: local
                DesktopPeerMetaStore.save(
                    meta.copy(
                        settingsRevision = remoteRev,
                        settingsPending = false,
                        settingsEtag = remote?.etag,
                    ),
                )
                remoteSettings
            }
            PeerReconcileAction.SEED, PeerReconcileAction.PUSH_PENDING -> {
                val nextRev = maxOf(meta.settingsRevision, remoteRev) + 1
                val envelope = PeerEnvelopeJson.settingsEnvelopeJson(local, nextRev, nowIso)
                val uploaded = GoogleDriveAppData.upload(
                    accessToken,
                    ContinuumConsts.SETTINGS_APP_DATA_FILENAME,
                    envelope,
                    remote?.id,
                    meta.settingsEtag ?: remote?.etag,
                )
                DesktopPeerMetaStore.save(
                    meta.copy(
                        settingsRevision = nextRev,
                        settingsPending = false,
                        settingsEtag = uploaded.etag,
                    ),
                )
                local
            }
        }
    }

    private fun overlayLocalEvents(
        accessToken: String,
        calendars: List<CalendarListEntry>,
        events: List<CalendarEvent>,
        meta: DesktopPeerMeta,
    ): Pair<List<CalendarListEntry>, List<CalendarEvent>> {
        val remote = runCatching {
            GoogleDriveAppData.findAndDownload(accessToken, ContinuumConsts.LOCAL_EVENTS_APP_DATA_FILENAME)
        }.getOrNull() ?: return calendars to events
        val remoteRev = PeerEnvelopeJson.parseRevision(remote.body) ?: 0L
        if (remoteRev <= meta.localEventsRevision && !meta.localEventsPending) {
            return calendars to events
        }
        val incomingEvents = PeerEnvelopeJson.parseOwnedEvents(remote.body)
        val merged = mergeLocalEventsPayload(
            baseCalendars = filterContinuumOwnedCalendars(calendars),
            baseEvents = filterContinuumOwnedEvents(events),
            baseDeleted = emptyList(),
            incomingCalendars = emptyList(),
            incomingEvents = incomingEvents,
            incomingDeleted = emptyList(),
        )
        val nonOwnedCals = calendars.filter {
            it.source != CalendarSource.LOCAL && it.source != CalendarSource.ICS_IMPORT
        }
        val nonOwnedEv = events.filter {
            it.source == CalendarSource.GOOGLE ||
                it.source == CalendarSource.CALDAV ||
                it.source == CalendarSource.HOLIDAYS
        }
        DesktopPeerMetaStore.save(
            meta.copy(
                localEventsRevision = remoteRev,
                localEventsPending = false,
                localEventsEtag = remote.etag,
            ),
        )
        return (merged.calendars + nonOwnedCals) to (merged.events + nonOwnedEv)
    }
}

package org.continuumcalendar.ios

import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.runBlocking
import org.continuumcalendar.data.InMemoryEventRepository
import org.continuumcalendar.data.InMemorySettingsRepository
import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.CalendarSource
import org.continuumcalendar.platform.SystemContinuumClock
import org.continuumcalendar.ui.agenda.AgendaScreen
import org.continuumcalendar.ui.theme.ContinuumTheme
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val clock = SystemContinuumClock()
    val settings = runBlocking { InMemorySettingsRepository().load() }
    val events = runBlocking {
        val repo = InMemoryEventRepository()
        val today = org.continuumcalendar.domain.agenda.localDateKey(clock.nowEpochMs())
        repo.upsert(
            CalendarEvent(
                id = "demo",
                calendarId = "local:1",
                title = "Welcome to Continuum CMP",
                start = "${today}T09:00:00",
                end = "${today}T09:30:00",
                source = CalendarSource.LOCAL,
            ),
        )
        repo.listAll()
    }
    return ComposeUIViewController {
        ContinuumTheme(themeMode = settings.themeMode) {
            AgendaScreen(
                events = events,
                settings = settings,
                nowEpochMs = clock.nowEpochMs(),
            )
        }
    }
}

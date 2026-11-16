package org.continuumcalendar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.continuumcalendar.domain.model.defaultContinuumSettings
import org.continuumcalendar.platform.SystemContinuumClock
import org.continuumcalendar.ui.demo.demoCalendars
import org.continuumcalendar.ui.demo.demoEvents
import org.continuumcalendar.ui.shell.ContinuumMainView
import org.continuumcalendar.ui.shell.ContinuumShell

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val clock = SystemContinuumClock()
        val now = clock.nowEpochMs()
        setContent {
            ContinuumShell(
                events = demoEvents(now),
                calendars = demoCalendars(),
                settings = defaultContinuumSettings(),
                nowEpochMs = now,
                initialView = ContinuumMainView.ROLLING,
            )
        }
    }
}

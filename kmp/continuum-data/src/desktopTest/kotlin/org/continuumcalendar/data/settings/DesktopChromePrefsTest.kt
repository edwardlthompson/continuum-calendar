package org.continuumcalendar.data.settings

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopChromePrefsTest {
    @Test
    fun roundTripsSettingsCategory() {
        val dir = Files.createTempDirectory("cc-chrome").toFile()
        val file = java.io.File(dir, "continuum-chrome.local.json")
        val prefs = DesktopChromePrefs(
            openAtStartup = true,
            minimizeTo = "tray",
            closeTo = "tray",
            windowX = 10,
            windowY = 20,
            windowWidth = 900,
            windowHeight = 700,
            settingsCategory = "about",
        )
        DesktopChromeStore.save(prefs, file)
        val loaded = DesktopChromeStore.load(file)
        assertEquals("about", loaded.settingsCategory)
        assertTrue(loaded.openAtStartup)
        assertEquals("tray", loaded.minimizeTo)
        assertEquals(900, loaded.windowWidth)
    }

    @Test
    fun defaultsCategoryWhenMissing() {
        val dir = Files.createTempDirectory("cc-chrome-missing").toFile()
        val file = java.io.File(dir, "legacy.json")
        file.writeText(
            """{"openAtStartup":false,"minimizeTo":"taskbar","closeTo":"quit","windowX":1,"windowY":2,"windowWidth":800,"windowHeight":600}""",
            Charsets.UTF_8,
        )
        assertEquals("calendar", DesktopChromeStore.load(file).settingsCategory)
    }
}

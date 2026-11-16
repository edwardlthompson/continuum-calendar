package org.continuumcalendar.data.settings

import java.io.File

data class DesktopChromePrefs(
    val openAtStartup: Boolean = false,
    val minimizeTo: String = "taskbar", // taskbar | tray
    val closeTo: String = "quit", // tray | quit
    val windowX: Int = 80,
    val windowY: Int = 60,
    val windowWidth: Int = 1280,
    val windowHeight: Int = 800,
    /** Last Settings category id (lowercase enum name), e.g. "calendar", "about". */
    val settingsCategory: String = "calendar",
)

object DesktopChromeStore {
    const val APP_ID = "org.continuumcalendar.app"
    const val FILE_NAME = "continuum-chrome.local.json"

    fun storeFile(): File {
        val appData = System.getenv("APPDATA")
            ?: (System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Roaming")
        return File(File(appData, APP_ID), FILE_NAME)
    }

    fun load(file: File = storeFile()): DesktopChromePrefs {
        if (!file.isFile) return DesktopChromePrefs()
        val raw = runCatching { file.readText(Charsets.UTF_8) }.getOrNull() ?: return DesktopChromePrefs()
        return DesktopChromePrefs(
            openAtStartup = boolField(raw, "openAtStartup") ?: false,
            minimizeTo = stringField(raw, "minimizeTo") ?: "taskbar",
            closeTo = stringField(raw, "closeTo") ?: "quit",
            windowX = intField(raw, "windowX") ?: 80,
            windowY = intField(raw, "windowY") ?: 60,
            windowWidth = (intField(raw, "windowWidth") ?: 1280).coerceIn(640, 10000),
            windowHeight = (intField(raw, "windowHeight") ?: 800).coerceIn(480, 10000),
            settingsCategory = stringField(raw, "settingsCategory") ?: "calendar",
        )
    }

    fun save(prefs: DesktopChromePrefs, file: File = storeFile()) {
        file.parentFile?.mkdirs()
        val safeCat = prefs.settingsCategory.replace("\"", "")
        val json = buildString {
            append('{')
            append("\"openAtStartup\":${prefs.openAtStartup},")
            append("\"minimizeTo\":\"${prefs.minimizeTo}\",")
            append("\"closeTo\":\"${prefs.closeTo}\",")
            append("\"windowX\":${prefs.windowX},")
            append("\"windowY\":${prefs.windowY},")
            append("\"windowWidth\":${prefs.windowWidth},")
            append("\"windowHeight\":${prefs.windowHeight},")
            append("\"settingsCategory\":\"$safeCat\"")
            append('}')
        }
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(json, Charsets.UTF_8)
        if (file.exists()) file.delete()
        tmp.renameTo(file)
    }

    private fun stringField(json: String, key: String): String? =
        Regex(""""$key"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.get(1)

    private fun intField(json: String, key: String): Int? =
        Regex(""""$key"\s*:\s*(-?\d+)""").find(json)?.groupValues?.get(1)?.toIntOrNull()

    private fun boolField(json: String, key: String): Boolean? =
        Regex(""""$key"\s*:\s*(true|false)""").find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull()
}

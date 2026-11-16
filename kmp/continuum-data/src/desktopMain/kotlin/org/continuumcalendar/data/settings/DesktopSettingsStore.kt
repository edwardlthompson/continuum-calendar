package org.continuumcalendar.data.settings

import org.continuumcalendar.domain.model.ContinuumSettings
import org.continuumcalendar.domain.model.defaultContinuumSettings
import java.io.File

/**
 * Local settings next to the Tauri vault:
 * `%APPDATA%/org.continuumcalendar.app/continuum-settings.local.json`
 */
object DesktopSettingsStore {
    const val APP_ID = "org.continuumcalendar.app"
    const val FILE_NAME = "continuum-settings.local.json"

    fun storeFile(): File {
        val appData = System.getenv("APPDATA")
            ?: (System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Roaming")
        return File(File(appData, APP_ID), FILE_NAME)
    }

    fun load(): ContinuumSettings {
        val file = storeFile()
        if (!file.isFile) return defaultContinuumSettings()
        val raw = runCatching { file.readText(Charsets.UTF_8) }.getOrNull() ?: return defaultContinuumSettings()
        return parseContinuumSettingsJson(raw) ?: defaultContinuumSettings()
    }

    fun save(settings: ContinuumSettings) {
        val file = storeFile()
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, "$FILE_NAME.tmp")
        tmp.writeText(continuumSettingsToJson(settings), Charsets.UTF_8)
        if (file.exists()) file.delete()
        tmp.renameTo(file)
    }
}

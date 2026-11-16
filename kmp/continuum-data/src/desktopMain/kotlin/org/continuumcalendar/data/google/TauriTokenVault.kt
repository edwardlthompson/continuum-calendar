package org.continuumcalendar.data.google

import org.continuumcalendar.domain.model.GoogleOAuthTokens
import java.io.File

/**
 * Reads/writes the same vault as Tauri desktop:
 * `%APPDATA%/org.continuumcalendar.app/google-tokens.json`
 */
object TauriTokenVault {
    const val APP_ID = "org.continuumcalendar.app"
    const val FILE_NAME = "google-tokens.json"

    fun vaultFile(): File {
        val appData = System.getenv("APPDATA")
            ?: (System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Roaming")
        return File(File(appData, APP_ID), FILE_NAME)
    }

    fun load(): GoogleOAuthTokens? {
        val file = vaultFile()
        if (!file.isFile) return null
        val raw = runCatching { file.readText(Charsets.UTF_8) }.getOrNull() ?: return null
        if (raw.length > 16_384) return null
        return parseGoogleTokensJson(raw)
    }

    fun save(tokens: GoogleOAuthTokens?) {
        val file = vaultFile()
        file.parentFile?.mkdirs()
        if (tokens == null) {
            if (file.exists()) file.delete()
            return
        }
        val tmp = File(file.parentFile, "$FILE_NAME.tmp")
        tmp.writeText(googleTokensToJson(tokens), Charsets.UTF_8)
        if (file.exists()) file.delete()
        tmp.renameTo(file)
    }
}

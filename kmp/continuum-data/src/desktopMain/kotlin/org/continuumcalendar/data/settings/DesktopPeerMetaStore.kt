package org.continuumcalendar.data.settings

import java.io.File

/** Peer sync bookkeeping beside the Tauri vault. */
data class DesktopPeerMeta(
    val settingsRevision: Long = 0,
    val settingsPending: Boolean = false,
    val settingsEtag: String? = null,
    val localEventsRevision: Long = 0,
    val localEventsPending: Boolean = false,
    val localEventsEtag: String? = null,
)

object DesktopPeerMetaStore {
    const val APP_ID = "org.continuumcalendar.app"
    const val FILE_NAME = "continuum-peer-meta.json"

    fun storeFile(): File {
        val appData = System.getenv("APPDATA")
            ?: (System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Roaming")
        return File(File(appData, APP_ID), FILE_NAME)
    }

    fun load(): DesktopPeerMeta {
        val raw = runCatching { storeFile().readText(Charsets.UTF_8) }.getOrNull() ?: return DesktopPeerMeta()
        return DesktopPeerMeta(
            settingsRevision = longField(raw, "settingsRevision") ?: 0L,
            settingsPending = boolField(raw, "settingsPending") ?: false,
            settingsEtag = stringField(raw, "settingsEtag"),
            localEventsRevision = longField(raw, "localEventsRevision") ?: 0L,
            localEventsPending = boolField(raw, "localEventsPending") ?: false,
            localEventsEtag = stringField(raw, "localEventsEtag"),
        )
    }

    fun save(meta: DesktopPeerMeta) {
        val file = storeFile()
        file.parentFile?.mkdirs()
        val body = buildString {
            append('{')
            append("\"settingsRevision\":${meta.settingsRevision},")
            append("\"settingsPending\":${meta.settingsPending},")
            meta.settingsEtag?.let { append("\"settingsEtag\":${jsonString(it)},") }
            append("\"localEventsRevision\":${meta.localEventsRevision},")
            append("\"localEventsPending\":${meta.localEventsPending}")
            meta.localEventsEtag?.let { append(",\"localEventsEtag\":${jsonString(it)}") }
            append('}')
        }
        val tmp = File(file.parentFile, "$FILE_NAME.tmp")
        tmp.writeText(body, Charsets.UTF_8)
        if (file.exists()) file.delete()
        tmp.renameTo(file)
    }

    fun markSettingsPending() {
        val cur = load()
        save(cur.copy(settingsPending = true, settingsRevision = cur.settingsRevision + 1))
    }

    private fun jsonString(s: String): String =
        "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private fun stringField(json: String, key: String): String? {
        val re = Regex(""""$key"\s*:\s*"((?:\\.|[^"\\])*)"""")
        return re.find(json)?.groupValues?.get(1)
            ?.replace("\\\"", "\"")
            ?.replace("\\\\", "\\")
    }

    private fun longField(json: String, key: String): Long? {
        val re = Regex(""""$key"\s*:\s*(-?\d+)""")
        return re.find(json)?.groupValues?.get(1)?.toLongOrNull()
    }

    private fun boolField(json: String, key: String): Boolean? {
        val re = Regex(""""$key"\s*:\s*(true|false)""")
        return re.find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull()
    }
}

package org.continuumcalendar.data.settings

import java.io.File

data class DesktopCalDavAccount(
    val url: String = "",
    val username: String = "",
    val password: String = "",
)

object DesktopCalDavStore {
    const val APP_ID = "org.continuumcalendar.app"
    const val FILE_NAME = "continuum-caldav.json"

    fun storeFile(): File {
        val appData = System.getenv("APPDATA")
            ?: (System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Roaming")
        return File(File(appData, APP_ID), FILE_NAME)
    }

    fun load(): DesktopCalDavAccount {
        val raw = runCatching { storeFile().readText(Charsets.UTF_8) }.getOrNull() ?: return DesktopCalDavAccount()
        fun field(key: String): String {
            val re = Regex(""""$key"\s*:\s*"((?:\\.|[^"\\])*)"""")
            return re.find(raw)?.groupValues?.get(1)
                ?.replace("\\\"", "\"")
                ?.replace("\\\\", "\\")
                .orEmpty()
        }
        return DesktopCalDavAccount(url = field("url"), username = field("username"), password = field("password"))
    }

    fun save(account: DesktopCalDavAccount) {
        val file = storeFile()
        file.parentFile?.mkdirs()
        fun esc(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")
        val body =
            """{"url":"${esc(account.url)}","username":"${esc(account.username)}","password":"${esc(account.password)}"}"""
        val tmp = File(file.parentFile, "$FILE_NAME.tmp")
        tmp.writeText(body, Charsets.UTF_8)
        if (file.exists()) file.delete()
        tmp.renameTo(file)
    }
}

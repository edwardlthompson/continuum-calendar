package org.fossify.calendar.continuum

import android.content.Context
import org.fossify.calendar.BuildConfig
import org.json.JSONObject

object ContinuumGithubRelease {
    fun fetchLatest(context: Context): Parsed? {
        val conn = ContinuumHttp.openConnection(context, ContinuumProductUpdate.RELEASES_API)
        return try {
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "Continuum-Calendar/${BuildConfig.VERSION_NAME}")
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            if (conn.responseCode != 200) return null
            parse(conn.inputStream.bufferedReader().use { it.readText() })
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
            ContinuumHttp.unbindProcess(context)
        }
    }

    fun parse(json: String): Parsed? {
        return try {
            val root = JSONObject(json)
            val htmlUrl = root.optString("html_url", ContinuumProductUpdate.RELEASES_PAGE)
            val assets = mutableListOf<ContinuumProductUpdate.NamedAsset>()
            val arr = root.optJSONArray("assets") ?: return Parsed(htmlUrl, assets)
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val name = item.optString("name")
                val url = item.optString("browser_download_url")
                if (name.isNotBlank() && url.isNotBlank()) {
                    assets.add(ContinuumProductUpdate.NamedAsset(name, url))
                }
            }
            Parsed(htmlUrl, assets)
        } catch (_: Exception) {
            null
        }
    }

    data class Parsed(val htmlUrl: String, val assets: List<ContinuumProductUpdate.NamedAsset>)
}

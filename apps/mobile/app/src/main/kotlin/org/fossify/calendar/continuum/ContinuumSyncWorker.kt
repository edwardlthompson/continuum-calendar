package org.fossify.calendar.continuum

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/** Pull Google Calendar events via REST when Continuum SSO is signed in. */
class ContinuumSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val auth = ContinuumGoogleAuth(applicationContext)
        val tokens = auth.ensureFreshTokens() ?: return Result.retry()
        return try {
            val settingsSync = ContinuumSettingsSync(applicationContext)
            val settings = settingsSync.loadLocal()
            if (settings.useGoogleCalendar) {
                val timeMin = java.time.Instant.now().minusSeconds(30L * 24 * 3600).toString()
                val timeMax = java.time.Instant.now().plusSeconds(90L * 24 * 3600).toString()
                val url =
                    "https://www.googleapis.com/calendar/v3/calendars/primary/events" +
                        "?singleEvents=true&orderBy=startTime&maxResults=2500" +
                        "&timeMin=${java.net.URLEncoder.encode(timeMin, "UTF-8")}" +
                        "&timeMax=${java.net.URLEncoder.encode(timeMax, "UTF-8")}"
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.setRequestProperty("Authorization", "Bearer ${tokens.accessToken}")
                val body = conn.inputStream.bufferedReader().readText()
                val items = JSONObject(body).optJSONArray("items") ?: JSONArray()
                applicationContext
                    .getSharedPreferences("continuum_sync", Context.MODE_PRIVATE)
                    .edit()
                    .putString("last_primary_events", items.toString())
                    .putLong("last_synced_at", System.currentTimeMillis())
                    .apply()
            } else {
                applicationContext
                    .getSharedPreferences("continuum_sync", Context.MODE_PRIVATE)
                    .edit()
                    .remove("last_primary_events")
                    .apply()
            }
            settingsSync.pullAndApply()
            ContinuumLocalEventsSync(applicationContext).reconcilePeerRemote()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE = "continuum_google_sync"

        fun enqueue(context: Context) {
            val req = PeriodicWorkRequestBuilder<ContinuumSyncWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE,
                ExistingPeriodicWorkPolicy.KEEP,
                req,
            )
        }
    }
}

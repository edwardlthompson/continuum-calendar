package org.fossify.calendar.continuum

import android.content.Context
import android.widget.Toast
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fossify.calendar.R
import java.util.concurrent.TimeUnit

/**
 * Completes OAuth token exchange after Custom Tabs closes.
 * On Oppo/OnePlus, sockets often ECONNREFUSED while AuthActivity/Firefox is still top —
 * WorkManager runs later with a CONNECTED network constraint.
 */
class ContinuumTokenExchangeWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val auth = ContinuumGoogleAuth(applicationContext)
        val pending = auth.loadPendingExchange() ?: return@withContext Result.success()
        try {
            ContinuumHttp.waitForInternetNetwork(applicationContext, timeoutMs = 20_000L)
            // Brief settle after OEM network rebind.
            Thread.sleep(1_500)
            auth.exchangeCode(
                pending.clientId,
                pending.redirectUri,
                pending.code,
                pending.state,
                pending.clientSecret,
            )
            auth.clearPendingExchange()
            ContinuumSyncWorker.enqueue(applicationContext)
            ContinuumSettingsSync(applicationContext).pushLocalAfterAuth()
            ContinuumLocalEventsSync(applicationContext).reconcilePeerRemote()
            ContinuumDiagnostics.i("Deferred OAuth token exchange succeeded")
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    applicationContext,
                    R.string.continuum_peer_sync_connected_toast,
                    Toast.LENGTH_LONG,
                ).show()
            }
            Result.success()
        } catch (e: Exception) {
            ContinuumDiagnostics.e("Deferred OAuth token exchange failed (attempt $runAttemptCount)", e)
            if (runAttemptCount >= 4) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        applicationContext,
                        applicationContext.getString(
                            R.string.continuum_peer_sync_sign_in_failed,
                            e.message ?: "error",
                        ),
                        Toast.LENGTH_LONG,
                    ).show()
                }
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }

    companion object {
        private const val UNIQUE = "continuum_oauth_token_exchange"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val req = OneTimeWorkRequestBuilder<ContinuumTokenExchangeWorker>()
                .setConstraints(constraints)
                .setInitialDelay(2, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE,
                ExistingWorkPolicy.REPLACE,
                req,
            )
            ContinuumDiagnostics.i("Enqueued deferred OAuth token exchange")
        }
    }
}

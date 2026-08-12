package org.fossify.calendar.continuum

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * HTTP helpers that bind to a real INTERNET network (not IMS).
 * Oppo/OnePlus often leave the default route on IMS after Custom Tabs, so DNS fails
 * for oauth2.googleapis.com even though Wi‑Fi works in the shell.
 */
object ContinuumHttp {
    fun waitForInternetNetwork(context: Context, timeoutMs: Long = 20_000L): Network? {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
        pickBestInternet(cm)?.let { return it }

        val holder = AtomicReference<Network?>(null)
        val latch = CountDownLatch(1)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (isUsableInternet(cm, network)) {
                    holder.compareAndSet(null, network)
                    latch.countDown()
                }
            }

            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                if (isUsableCaps(caps)) {
                    holder.compareAndSet(null, network)
                    latch.countDown()
                }
            }
        }
        try {
            val req = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                .build()
            cm.requestNetwork(req, callback)
            pickBestInternet(cm)?.let {
                holder.compareAndSet(null, it)
                latch.countDown()
            }
            latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            ContinuumDiagnostics.w("waitForInternetNetwork failed", e)
        } finally {
            try {
                cm.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
        return holder.get() ?: pickBestInternet(cm)
    }

    fun openConnection(context: Context, url: String): HttpURLConnection {
        val u = URL(url)
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val network = waitForInternetNetwork(context)
        if (cm != null && network != null) {
            try {
                // Force this process onto Wi‑Fi/cellular data for DNS + TLS.
                cm.bindProcessToNetwork(network)
                ContinuumDiagnostics.i("Bound process to internet network for $url")
            } catch (e: Exception) {
                ContinuumDiagnostics.w("bindProcessToNetwork failed", e)
            }
        }
        val conn = try {
            if (network != null) {
                network.openConnection(u) as HttpURLConnection
            } else {
                u.openConnection() as HttpURLConnection
            }
        } catch (e: Exception) {
            ContinuumDiagnostics.w("network.openConnection failed — falling back", e)
            u.openConnection() as HttpURLConnection
        }
        conn.connectTimeout = 20_000
        conn.readTimeout = 20_000
        return conn
    }

    fun unbindProcess(context: Context) {
        try {
            context.getSystemService(ConnectivityManager::class.java)
                ?.bindProcessToNetwork(null)
        } catch (_: Exception) {
        }
    }

    private fun pickBestInternet(cm: ConnectivityManager): Network? {
        val candidates = cm.allNetworks.mapNotNull { n ->
            val caps = cm.getNetworkCapabilities(n) ?: return@mapNotNull null
            if (!isUsableCaps(caps)) return@mapNotNull null
            val score = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> 3
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> 2
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> 1
                else -> 0
            }
            n to score
        }
        return candidates.maxByOrNull { it.second }?.first
    }

    private fun isUsableInternet(cm: ConnectivityManager, network: Network): Boolean {
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return isUsableCaps(caps)
    }

    private fun isUsableCaps(caps: NetworkCapabilities): Boolean {
        // IMS is VALIDATED but has no INTERNET — must reject.
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return false
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)) return false
        // Prefer validated; allow briefly-unvalidated Wi‑Fi as last resort.
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}

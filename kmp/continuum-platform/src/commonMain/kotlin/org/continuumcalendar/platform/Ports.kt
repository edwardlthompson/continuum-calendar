package org.continuumcalendar.platform

/** Epoch clock (injectable in tests). */
fun interface ContinuumClock {
    fun nowEpochMs(): Long
}

/** Secure OAuth token persistence. */
interface SecureTokenStore {
    suspend fun save(json: String)
    suspend fun load(): String?
    suspend fun clear()
}

/** OS notification surface. */
interface Notifier {
    suspend fun notify(title: String, body: String)
}

/** Browser / Custom Tabs OAuth loop. */
interface BrowserAuth {
    suspend fun openAuthUrl(url: String)
}

/** Limited file read for ICS import. */
interface FileAccess {
    suspend fun readTextLimited(path: String, maxBytes: Int): String?
}

/** Factory for platform HTTP clients (timeouts applied by implementers). */
interface HttpClientFactory {
    fun create(connectTimeoutMs: Long = 15_000, readTimeoutMs: Long = 30_000): PlatformHttpClient
}

interface PlatformHttpClient {
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): HttpTextResponse
    suspend fun put(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
    ): HttpTextResponse
}

data class HttpTextResponse(val status: Int, val body: String)

class SystemContinuumClock : ContinuumClock {
    override fun nowEpochMs(): Long =
        kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
}

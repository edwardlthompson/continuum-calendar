package org.continuumcalendar.platform

class InMemoryTokenStore : SecureTokenStore {
    private var value: String? = null
    override suspend fun save(json: String) {
        value = json
    }

    override suspend fun load(): String? = value

    override suspend fun clear() {
        value = null
    }
}

class RecordingNotifier : Notifier {
    val messages = mutableListOf<Pair<String, String>>()
    override suspend fun notify(title: String, body: String) {
        messages += title to body
    }
}

class NoOpBrowserAuth : BrowserAuth {
    override suspend fun openAuthUrl(url: String) = Unit
}

class NoOpFileAccess : FileAccess {
    override suspend fun readTextLimited(path: String, maxBytes: Int): String? = null
}

class FakeHttpClientFactory(
    private val handler: (method: String, url: String) -> HttpTextResponse =
        { _, _ -> HttpTextResponse(404, "") },
) : HttpClientFactory {
    override fun create(connectTimeoutMs: Long, readTimeoutMs: Long): PlatformHttpClient =
        object : PlatformHttpClient {
            override suspend fun get(url: String, headers: Map<String, String>) =
                handler("GET", url)

            override suspend fun put(url: String, body: String, headers: Map<String, String>) =
                handler("PUT", url)
        }
}

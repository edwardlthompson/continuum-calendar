package org.continuumcalendar.data.google

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.continuumcalendar.domain.model.GoogleOAuthTokens

class GoogleTokenJsonTest {
    @Test
    fun parseRoundTripPreservesFields() {
        val raw = """
            {"accessToken":"tok-a","refreshToken":"tok-r","expiresAt":1794795670152,
            "scope":"calendar","tokenType":"Bearer","clientId":"cid.apps.googleusercontent.com"}
        """.trimIndent()
        val parsed = assertNotNull(parseGoogleTokensJson(raw))
        assertEquals("tok-a", parsed.accessToken)
        assertEquals("tok-r", parsed.refreshToken)
        assertEquals(1794795670152L, parsed.expiresAt)
        assertEquals("cid.apps.googleusercontent.com", parsed.clientId)
        val again = assertNotNull(parseGoogleTokensJson(googleTokensToJson(parsed)))
        assertEquals(parsed, again)
    }

    @Test
    fun parseRejectsBlankAccess() {
        assertNull(parseGoogleTokensJson("""{"accessToken":"","expiresAt":1}"""))
    }

    @Test
    fun serializeOmitsNullOptionals() {
        val json = googleTokensToJson(
            GoogleOAuthTokens(accessToken = "a", expiresAt = 2L),
        )
        assertEquals(true, json.contains("\"accessToken\""))
        assertEquals(false, json.contains("refreshToken"))
        assertEquals(false, json.contains("clientId"))
    }
}

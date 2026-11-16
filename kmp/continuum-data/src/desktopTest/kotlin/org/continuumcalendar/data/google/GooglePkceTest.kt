package org.continuumcalendar.data.google

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GooglePkceTest {
    @Test
    fun challengeMatchesKnownVector() {
        // RFC 7636 appendix B
        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        val challenge = GooglePkce.sha256UrlSafe(verifier)
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", challenge)
    }

    @Test
    fun newSessionHasDistinctParts() {
        val s = GooglePkce.newSession()
        assertTrue(s.verifier.length >= 32)
        assertTrue(s.challenge.isNotBlank())
        assertTrue(s.state.isNotBlank())
        assertEquals(GooglePkce.sha256UrlSafe(s.verifier), s.challenge)
    }
}

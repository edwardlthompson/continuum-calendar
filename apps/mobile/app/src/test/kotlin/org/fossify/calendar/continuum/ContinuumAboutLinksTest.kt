package org.fossify.calendar.continuum

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContinuumAboutLinksTest {

    @Test
    fun aboutMenuShowsContinuumVenmoNotFossifySocial() {
        assertTrue(ContinuumAboutLinks.SHOW_DONATE)
        assertFalse(ContinuumAboutLinks.SHOW_SOCIAL)
        assertEquals(
            "https://venmo.com/code?user_id=1857304970395648420",
            ContinuumAboutLinks.DONATION_URL,
        )
        assertFalse(ContinuumAboutLinks.isFossifyPromo(ContinuumAboutLinks.DONATION_URL))
    }

    @Test
    fun blocksFossifyPromoDestinations() {
        assertTrue(ContinuumAboutLinks.isFossifyPromo("https://github.com/FossifyOrg"))
        assertTrue(ContinuumAboutLinks.isFossifyPromo("https://www.reddit.com/r/Fossify"))
        assertTrue(ContinuumAboutLinks.isFossifyPromo("https://t.me/Fossify"))
        assertTrue(ContinuumAboutLinks.isFossifyPromo("https://www.fossify.org"))
        assertTrue(ContinuumAboutLinks.isFossifyPromo("https://fossify.org/more-apps"))
        assertTrue(ContinuumAboutLinks.isFossifyPromo("mailto:hello@fossify.org"))
        assertFalse(ContinuumAboutLinks.isFossifyPromo("https://github.com/edwardlthompson/continuum-calendar"))
    }

    @Test
    fun privacyOpensDocsPrivacyMd() {
        assertTrue(ContinuumAboutLinks.PRIVACY_URL.endsWith("docs/PRIVACY.md"))
        assertFalse(ContinuumAboutLinks.isFossifyPromo(ContinuumAboutLinks.PRIVACY_URL))
    }

    @Test
    fun englishStringsOmitFossifyOrgCtas() {
        val xml = java.io.File("src/main/res/values/strings.xml").readText()
        assertFalse(xml.contains("https://www.fossify.org"))
        assertFalse(xml.contains("hello@fossify.org"))
    }
}

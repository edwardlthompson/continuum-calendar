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
        assertTrue(ContinuumAboutLinks.isFossifyPromo("https://www.fossify.org/donate"))
        assertTrue(ContinuumAboutLinks.isFossifyPromo("mailto:hello@fossify.org"))
        assertFalse(ContinuumAboutLinks.isFossifyPromo("https://github.com/edwardlthompson/continuum-calendar"))
    }
}

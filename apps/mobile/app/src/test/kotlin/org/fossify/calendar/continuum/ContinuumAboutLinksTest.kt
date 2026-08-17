package org.fossify.calendar.continuum

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContinuumAboutLinksTest {

    @Test
    fun aboutMenuOmitsFossifyDonateAndSocial() {
        assertFalse(ContinuumAboutLinks.SHOW_DONATE)
        assertFalse(ContinuumAboutLinks.SHOW_SOCIAL)
    }

    @Test
    fun blocksFossifyPromoDestinations() {
        assertTrue(ContinuumAboutLinks.isFossifyPromo("https://github.com/FossifyOrg"))
        assertTrue(ContinuumAboutLinks.isFossifyPromo("https://www.reddit.com/r/Fossify"))
        assertTrue(ContinuumAboutLinks.isFossifyPromo("https://t.me/Fossify"))
        assertFalse(ContinuumAboutLinks.isFossifyPromo("https://github.com/edwardlthompson/continuum-calendar"))
    }
}

package org.fossify.calendar.continuum

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContinuumAboutGuardTest {

    @Test
    fun redirectsCommonsAboutOnly() {
        assertTrue(ContinuumAboutGuard.shouldRedirect(ContinuumAboutGuard.COMMONS_ABOUT))
        assertFalse(
            ContinuumAboutGuard.shouldRedirect(
                "org.fossify.calendar.continuum.ContinuumAboutActivity",
            ),
        )
        assertFalse(ContinuumAboutGuard.shouldRedirect("org.fossify.calendar.activities.MainActivity"))
    }
}

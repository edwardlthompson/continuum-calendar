package org.fossify.calendar.continuum

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContinuumOAuthErrorsTest {

    @Test
    fun accessDeniedIsTestingMode() {
        assertTrue(ContinuumOAuthErrors.isTestingMode("access_denied", "Access denied"))
    }

    @Test
    fun unknownErrorIsTestingMode() {
        assertTrue(ContinuumOAuthErrors.isTestingMode("unknown_error", "An unknown error has occurred"))
    }

    @Test
    fun invalidGrantIsNotTestingMode() {
        assertFalse(ContinuumOAuthErrors.isTestingMode("invalid_grant", "code expired"))
    }
}

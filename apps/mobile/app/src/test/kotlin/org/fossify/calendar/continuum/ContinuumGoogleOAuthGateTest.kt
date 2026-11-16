package org.fossify.calendar.continuum

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContinuumGoogleOAuthGateTest {

    @Test
    fun fossFlavorWithoutClientIdIsDisabled() {
        assertFalse(ContinuumSettingsOAuth.oauthUsable(flavorEnabled = false, clientId = ""))
        assertFalse(ContinuumSettingsOAuth.oauthUsable(flavorEnabled = false, clientId = "cid"))
    }

    @Test
    fun gplayFlavorNeedsClientId() {
        assertFalse(ContinuumSettingsOAuth.oauthUsable(flavorEnabled = true, clientId = ""))
        assertFalse(ContinuumSettingsOAuth.oauthUsable(flavorEnabled = true, clientId = "   "))
        assertTrue(ContinuumSettingsOAuth.oauthUsable(flavorEnabled = true, clientId = "cid.apps.googleusercontent.com"))
    }
}

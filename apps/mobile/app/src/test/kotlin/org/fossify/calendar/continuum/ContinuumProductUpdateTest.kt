package org.fossify.calendar.continuum

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContinuumProductUpdateTest {

    @Test
    fun dailyCheckWaitsAFullDay() {
        assertTrue(ContinuumProductUpdate.shouldCheckDaily(null, 0L))
        assertFalse(ContinuumProductUpdate.shouldCheckDaily(0L, ContinuumProductUpdate.MS_DAY - 1))
        assertTrue(ContinuumProductUpdate.shouldCheckDaily(0L, ContinuumProductUpdate.MS_DAY))
    }

    @Test
    fun apkVersionIgnoresTemplateTags() {
        assertEquals("1.10.8", ContinuumProductUpdate.parseApkVersion("continuum-calendar-1.10.8-foss.apk"))
        assertEquals(null, ContinuumProductUpdate.parseApkVersion("v0.22.1"))
    }

    @Test
    fun donateNudgeOnlyAfterVersionChange() {
        assertFalse(ContinuumProductUpdate.shouldNudgeDonate(null, "1.10.7"))
        assertFalse(ContinuumProductUpdate.shouldNudgeDonate("1.10.7", "1.10.7"))
        assertTrue(ContinuumProductUpdate.shouldNudgeDonate("1.10.7", "1.10.8"))
    }

    @Test
    fun selectApkAssetReadsFossFilename() {
        val picked = ContinuumProductUpdate.selectApkAsset(
            listOf(
                ContinuumProductUpdate.NamedAsset("sbom.cyclonedx.json", "https://example.com/sbom"),
                ContinuumProductUpdate.NamedAsset(
                    "continuum-calendar-1.10.8-foss.apk",
                    "https://example.com/a.apk",
                ),
            ),
        )
        assertEquals("1.10.8", picked?.version)
        assertEquals("https://example.com/a.apk", picked?.url)
    }

    @Test
    fun updatePromptSkipsDismissedVersion() {
        assertTrue(ContinuumProductUpdate.shouldPromptUpdate("1.10.7", "1.10.8", null))
        assertFalse(ContinuumProductUpdate.shouldPromptUpdate("1.10.7", "1.10.8", "1.10.8"))
        assertFalse(ContinuumProductUpdate.shouldPromptUpdate("1.10.8", "1.10.8", null))
    }
}

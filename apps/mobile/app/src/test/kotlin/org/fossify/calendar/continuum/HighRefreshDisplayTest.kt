package org.fossify.calendar.continuum

import org.junit.Assert.assertEquals
import org.junit.Test

class HighRefreshDisplayTest {

    @Test
    fun picksFastestModeAtCurrentResolution() {
        val current = HighRefreshDisplay.ModeSpec(1, 1080, 2412, 60f)
        val supported = listOf(
            current,
            HighRefreshDisplay.ModeSpec(2, 1080, 2412, 90f),
            HighRefreshDisplay.ModeSpec(3, 1080, 2412, 120f),
            HighRefreshDisplay.ModeSpec(4, 1440, 3216, 60f),
        )
        assertEquals(3, HighRefreshDisplay.fastestSameResolutionModeId(current, supported))
    }

    @Test
    fun ignoresHigherRefreshAtOtherResolution() {
        val current = HighRefreshDisplay.ModeSpec(1, 1440, 3216, 60f)
        val supported = listOf(
            current,
            HighRefreshDisplay.ModeSpec(2, 1080, 2412, 120f),
        )
        assertEquals(1, HighRefreshDisplay.fastestSameResolutionModeId(current, supported))
    }

    @Test
    fun fallsBackToCurrentWhenListHasNoMatch() {
        val current = HighRefreshDisplay.ModeSpec(7, 1080, 1920, 60f)
        assertEquals(7, HighRefreshDisplay.fastestSameResolutionModeId(current, emptyList()))
    }
}

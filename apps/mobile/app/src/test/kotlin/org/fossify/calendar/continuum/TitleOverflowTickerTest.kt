package org.fossify.calendar.continuum

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TitleOverflowTickerTest {

    private fun ticker(mode: TitleOverflowMode) = TitleOverflowTicker(
        mode = mode,
        overflow = 100f,
        textWidth = 200f,
        gap = 48f,
        speed = 100f,
        pauseSeconds = 1f,
    ).also { it.reset() }

    @Test
    fun loopCycleIsTextWidthPlusGap() {
        val t = ticker(TitleOverflowMode.LOOP)
        assertEquals(248f, t.loopCycle(), 0.01f)
        assertTrue(t.loopCycle() > t.textWidth)
    }

    @Test
    fun loopWrapsWithoutOverlap() {
        val t = ticker(TitleOverflowMode.LOOP)
        t.step(2.5f)
        assertEquals(2f, t.offset, 0.01f)
    }

    @Test
    fun bouncePausesThenReachesEndThenReturns() {
        val t = ticker(TitleOverflowMode.BOUNCE)
        t.step(0.5f)
        assertEquals(0f, t.offset, 0.01f)
        t.step(0.5f)
        t.step(1f)
        assertEquals(100f, t.offset, 0.01f)
        t.step(1f)
        t.step(1f)
        assertEquals(0f, t.offset, 0.01f)
    }

    @Test
    fun resetJumpsToStartAfterEndPause() {
        val t = ticker(TitleOverflowMode.RESET)
        t.step(1f)
        t.step(1f)
        assertEquals(100f, t.offset, 0.01f)
        t.step(1f)
        assertEquals(0f, t.offset, 0.01f)
    }

    @Test
    fun shrinkDoesNotTick() {
        val t = ticker(TitleOverflowMode.SHRINK)
        assertFalse(t.shouldTick())
        t.step(1f)
        assertEquals(0f, t.offset, 0.01f)
        assertEquals(0.5f, t.shrinkScale(100f), 0.01f)
    }
}

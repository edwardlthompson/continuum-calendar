package org.fossify.calendar.continuum

/** How overflowing in-app event titles move. Widget stays on end-ellipsis. */
enum class TitleOverflowMode(val pref: Int) {
    BOUNCE(1),
    LOOP(2),
    RESET(3),
    SHRINK(4);

    companion object {
        fun fromPref(value: Int): TitleOverflowMode =
            entries.firstOrNull { it.pref == value } ?: BOUNCE
    }
}

/**
 * Choreographer-free stepper for long titles.
 * Loop cycle is text width + gap so the two copies never overlap.
 */
class TitleOverflowTicker(
    var mode: TitleOverflowMode = TitleOverflowMode.BOUNCE,
    var overflow: Float = 0f,
    var textWidth: Float = 0f,
    var gap: Float = 0f,
    var speed: Float = 0f,
    var pauseSeconds: Float = 1f,
) {
    var offset = 0f
        private set

    private var phase = Phase.PAUSE_START
    private var pauseLeft = pauseSeconds

    fun reset() {
        offset = 0f
        phase = Phase.PAUSE_START
        pauseLeft = pauseSeconds
    }

    fun shouldTick(): Boolean = overflow > 0f && mode != TitleOverflowMode.SHRINK

    fun loopCycle(): Float = textWidth + gap

    fun shrinkScale(avail: Float): Float =
        if (textWidth <= 0f || avail <= 0f) 1f else (avail / textWidth).coerceIn(0.35f, 1f)

    fun step(dt: Float) {
        if (!shouldTick() || dt <= 0f) return
        when (mode) {
            TitleOverflowMode.LOOP -> {
                val cycle = loopCycle()
                if (cycle > 0f) offset = (offset + speed * dt) % cycle
            }
            TitleOverflowMode.BOUNCE -> stepTravel(dt, reverse = true)
            TitleOverflowMode.RESET -> stepTravel(dt, reverse = false)
            TitleOverflowMode.SHRINK -> Unit
        }
    }

    private fun stepTravel(dt: Float, reverse: Boolean) {
        var remain = dt
        var guard = 0
        while (remain > 0f && speed > 0f && guard++ < 6) {
            when (phase) {
                Phase.PAUSE_START, Phase.PAUSE_END -> {
                    if (pauseLeft > remain) {
                        pauseLeft -= remain
                        return
                    }
                    remain -= pauseLeft.coerceAtLeast(0f)
                    if (phase == Phase.PAUSE_END && !reverse) {
                        offset = 0f
                        phase = Phase.PAUSE_START
                        pauseLeft = pauseSeconds
                    } else {
                        phase = if (phase == Phase.PAUSE_START) {
                            Phase.SCROLL_LEFT
                        } else {
                            Phase.SCROLL_RIGHT
                        }
                    }
                }
                Phase.SCROLL_LEFT -> {
                    val need = (overflow - offset) / speed
                    if (need > remain) {
                        offset += speed * remain
                        return
                    }
                    offset = overflow
                    remain -= need.coerceAtLeast(0f)
                    phase = Phase.PAUSE_END
                    pauseLeft = pauseSeconds
                }
                Phase.SCROLL_RIGHT -> {
                    val need = offset / speed
                    if (need > remain) {
                        offset -= speed * remain
                        return
                    }
                    offset = 0f
                    remain -= need.coerceAtLeast(0f)
                    phase = Phase.PAUSE_START
                    pauseLeft = pauseSeconds
                }
            }
        }
    }

    private enum class Phase { PAUSE_START, SCROLL_LEFT, PAUSE_END, SCROLL_RIGHT }
}

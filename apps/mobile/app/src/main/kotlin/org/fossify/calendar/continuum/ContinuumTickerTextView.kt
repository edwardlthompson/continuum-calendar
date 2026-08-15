package org.fossify.calendar.continuum

import android.content.Context
import android.graphics.Canvas
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Choreographer
import androidx.appcompat.widget.AppCompatTextView
import org.fossify.calendar.extensions.config

/**
 * Scrolls overflowing titles with Choreographer (not ValueAnimator), so the
 * ticker still moves when the device animator duration scale is 0.
 * In-app only — do not use in widget RemoteViews.
 */
class ContinuumTickerTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle,
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val ticker = TitleOverflowTicker(
        gap = resources.displayMetrics.density * 48f,
        speed = resources.displayMetrics.density * 36f,
    )
    private val choreographer = Choreographer.getInstance()
    private var lastNs = 0L
    private var ticking = false
    private val frame = Choreographer.FrameCallback { ns -> onFrame(ns) }

    init {
        ellipsize = TextUtils.TruncateAt.END
        isSingleLine = true
        maxLines = 1
        isHorizontalFadingEdgeEnabled = true
    }

    fun restartTicker() {
        ellipsize = null
        isSingleLine = true
        maxLines = 1
        textScaleX = 1f
        ticker.mode = TitleOverflowMode.fromPref(context.config.titleOverflowMode)
        ticker.reset()
        post {
            recompute()
            startTick()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recompute()
        startTick()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startTick()
    }

    override fun onDetachedFromWindow() {
        stopTick()
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) startTick() else stopTick()
    }

    private fun recompute() {
        val avail = (width - paddingLeft - paddingRight).toFloat()
        ticker.textWidth = paint.measureText(text?.toString().orEmpty())
        ticker.overflow = if (avail > 0f) (ticker.textWidth - avail).coerceAtLeast(0f) else 0f
        if (ticker.overflow <= 0f) {
            ticker.reset()
            textScaleX = 1f
        } else if (ticker.mode == TitleOverflowMode.SHRINK) {
            textScaleX = ticker.shrinkScale(avail)
        } else {
            textScaleX = 1f
        }
    }

    private fun startTick() {
        if (ticking || !ticker.shouldTick() || windowVisibility != VISIBLE) return
        ticking = true
        lastNs = 0L
        choreographer.postFrameCallback(frame)
    }

    private fun stopTick() {
        ticking = false
        lastNs = 0L
        choreographer.removeFrameCallback(frame)
    }

    private fun onFrame(ns: Long) {
        if (!ticking) return
        if (lastNs != 0L && ticker.shouldTick()) {
            val dt = (ns - lastNs).coerceIn(0L, 50_000_000L) / 1_000_000_000f
            ticker.step(dt)
            invalidate()
        }
        lastNs = ns
        if (ticker.shouldTick() && windowVisibility == VISIBLE) {
            choreographer.postFrameCallback(frame)
        } else {
            ticking = false
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (ticker.overflow <= 0f || ticker.mode == TitleOverflowMode.SHRINK) {
            if (ticker.overflow <= 0f) ellipsize = TextUtils.TruncateAt.END
            super.onDraw(canvas)
            return
        }
        val content = text?.toString().orEmpty()
        val x = paddingLeft.toFloat()
        val y = baseline.toFloat()
        canvas.save()
        canvas.clipRect(paddingLeft, 0, width - paddingRight, height)
        canvas.drawText(content, x - ticker.offset, y, paint)
        if (ticker.mode == TitleOverflowMode.LOOP) {
            canvas.drawText(content, x - ticker.offset + ticker.loopCycle(), y, paint)
        }
        canvas.restore()
    }
}

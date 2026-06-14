package io.legado.app.ui.watch

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import io.legado.app.utils.dpToPx

class EdgeSwipeBackLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val detector = EdgeSwipeBackDetector(
        edgeWidthPx = 32.dpToPx().toFloat(),
        minDistancePx = 64.dpToPx().toFloat(),
        maxVerticalDriftPx = 48.dpToPx().toFloat()
    )
    private var startX = 0f
    private var startY = 0f
    private var onBack: (() -> Unit)? = null

    fun setOnEdgeBack(action: () -> Unit) {
        onBack = action
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = ev.x
                startY = ev.y
            }

            MotionEvent.ACTION_UP -> {
                if (detector.shouldBack(startX, startY, ev.x, ev.y)) {
                    onBack?.invoke()
                    return true
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}

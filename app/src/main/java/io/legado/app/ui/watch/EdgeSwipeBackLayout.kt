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

    private val edgeWidthPx = 48.dpToPx().toFloat()
    private val detector = EdgeSwipeBackDetector(
        edgeWidthPx = edgeWidthPx,
        minDistancePx = 48.dpToPx().toFloat(),
        maxVerticalDriftPx = 48.dpToPx().toFloat()
    )
    private var startX = 0f
    private var startY = 0f
    private var isTrackingEdgeSwipe = false
    private var hasTriggeredBack = false
    private var onBack: (() -> Unit)? = null

    fun setOnEdgeBack(action: () -> Unit) {
        onBack = action
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = ev.x
                startY = ev.y
                isTrackingEdgeSwipe = startX <= edgeWidthPx
                hasTriggeredBack = false
                val handled = super.dispatchTouchEvent(ev)
                return handled || isTrackingEdgeSwipe
            }

            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                if (isTrackingEdgeSwipe && !hasTriggeredBack && detector.shouldBack(startX, startY, ev.x, ev.y)) {
                    hasTriggeredBack = true
                    onBack?.invoke()
                    return true
                }
                if (ev.actionMasked == MotionEvent.ACTION_UP) {
                    isTrackingEdgeSwipe = false
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                isTrackingEdgeSwipe = false
                hasTriggeredBack = false
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}

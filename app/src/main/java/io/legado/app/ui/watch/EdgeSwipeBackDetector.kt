package io.legado.app.ui.watch

import kotlin.math.abs

class EdgeSwipeBackDetector(
    private val edgeWidthPx: Float,
    private val minDistancePx: Float,
    private val maxVerticalDriftPx: Float
) {

    fun shouldBack(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float
    ): Boolean {
        val dx = endX - startX
        val dy = endY - startY
        return startX <= edgeWidthPx &&
            dx >= minDistancePx &&
            abs(dy) <= maxVerticalDriftPx &&
            dx > abs(dy)
    }
}

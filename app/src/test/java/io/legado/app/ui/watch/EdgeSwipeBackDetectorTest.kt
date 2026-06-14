package io.legado.app.ui.watch

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeSwipeBackDetectorTest {

    private val detector = EdgeSwipeBackDetector(
        edgeWidthPx = 32f,
        minDistancePx = 64f,
        maxVerticalDriftPx = 48f
    )

    @Test
    fun detectsRightSwipeStartingAtLeftEdge() {
        assertTrue(detector.shouldBack(startX = 12f, startY = 100f, endX = 96f, endY = 112f))
    }

    @Test
    fun ignoresSwipeThatStartsAwayFromLeftEdge() {
        assertFalse(detector.shouldBack(startX = 40f, startY = 100f, endX = 140f, endY = 100f))
    }

    @Test
    fun ignoresShortHorizontalMovement() {
        assertFalse(detector.shouldBack(startX = 12f, startY = 100f, endX = 60f, endY = 100f))
    }

    @Test
    fun ignoresMostlyVerticalMovement() {
        assertFalse(detector.shouldBack(startX = 12f, startY = 100f, endX = 120f, endY = 180f))
    }

    @Test
    fun ignoresLeftwardMovement() {
        assertFalse(detector.shouldBack(startX = 12f, startY = 100f, endX = 2f, endY = 100f))
    }
}

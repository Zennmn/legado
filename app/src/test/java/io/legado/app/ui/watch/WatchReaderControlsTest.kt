package io.legado.app.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchReaderControlsTest {

    @Test
    fun textSizeIsClampedToWatchRange() {
        assertEquals(14, WatchReaderControls.nextTextSize(14, -1))
        assertEquals(22, WatchReaderControls.nextTextSize(20, 2))
        assertEquals(28, WatchReaderControls.nextTextSize(28, 1))
    }

    @Test
    fun lineSpacingIsClampedToWatchRange() {
        assertEquals(10, WatchReaderControls.nextLineSpacing(10, -2))
        assertEquals(12, WatchReaderControls.nextLineSpacing(10, 2))
        assertEquals(16, WatchReaderControls.nextLineSpacing(16, 4))
        assertEquals(10, WatchReaderControls.nextLineSpacing(2, 0))
    }

    @Test
    fun roundDisplayDefaultsFitOppoWatchXPanel() {
        val defaults = WatchReaderControls.roundDisplayLayoutDefaults(
            widthPixels = 466,
            heightPixels = 466,
            density = 2f
        )

        assertEquals(36, defaults.contentPaddingDp)
        assertEquals(18, defaults.textSize)
        assertEquals(12, defaults.lineSpacing)
        assertEquals(1, defaults.paragraphSpacing)
    }

    @Test
    fun roundDisplayPaddingFallsBackForInvalidMetrics() {
        val defaults = WatchReaderControls.roundDisplayLayoutDefaults(
            widthPixels = 0,
            heightPixels = 466,
            density = 0f
        )

        assertEquals(36, defaults.contentPaddingDp)
    }

    @Test
    fun brightnessIsClampedToReadableRange() {
        assertEquals(8, WatchReaderControls.nextBrightness(8, -16))
        assertEquals(144, WatchReaderControls.nextBrightness(128, 16))
        assertEquals(255, WatchReaderControls.nextBrightness(248, 16))
    }

    @Test
    fun windowBrightnessFollowsSystemWhenEnabled() {
        assertEquals(-1f, WatchReaderControls.windowBrightness(128, followSystem = true))
        assertEquals(128 / 255f, WatchReaderControls.windowBrightness(128, followSystem = false))
    }

    @Test
    fun progressTextHandlesEmptyChapterLists() {
        assertEquals("第 1/1 章", WatchReaderControls.chapterProgressText(0, 0))
        assertEquals("第 3/12 章", WatchReaderControls.chapterProgressText(2, 12))
    }

    @Test
    fun watchSafeClickActionKeepsReaderActionsAndReplacesPhoneActions() {
        assertEquals(10, WatchReaderControls.watchSafeClickAction(10, fallback = 0))
        assertEquals(2, WatchReaderControls.watchSafeClickAction(8, fallback = 2))
        assertEquals(0, WatchReaderControls.watchSafeClickAction(12, fallback = 0))
        assertEquals(1, WatchReaderControls.watchSafeClickAction(-1, fallback = 1))
    }
}

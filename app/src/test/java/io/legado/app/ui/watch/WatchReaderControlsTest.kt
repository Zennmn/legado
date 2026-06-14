package io.legado.app.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchReaderControlsTest {

    @Test
    fun textSizeIsClampedToWatchRange() {
        assertEquals(16, WatchReaderControls.nextTextSize(16, -1))
        assertEquals(22, WatchReaderControls.nextTextSize(20, 2))
        assertEquals(32, WatchReaderControls.nextTextSize(32, 1))
    }

    @Test
    fun lineSpacingIsClampedToWatchRange() {
        assertEquals(2, WatchReaderControls.nextLineSpacing(2, -2))
        assertEquals(10, WatchReaderControls.nextLineSpacing(8, 2))
        assertEquals(20, WatchReaderControls.nextLineSpacing(20, 4))
    }

    @Test
    fun brightnessIsClampedToReadableRange() {
        assertEquals(8, WatchReaderControls.nextBrightness(8, -16))
        assertEquals(144, WatchReaderControls.nextBrightness(128, 16))
        assertEquals(255, WatchReaderControls.nextBrightness(248, 16))
    }

    @Test
    fun progressTextHandlesEmptyChapterLists() {
        assertEquals("第 1/1 章", WatchReaderControls.chapterProgressText(0, 0))
        assertEquals("第 3/12 章", WatchReaderControls.chapterProgressText(2, 12))
    }
}

package io.legado.app.ui.watch

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class WatchBookshelfLayoutTest {

    @Test
    fun bookshelfLayoutKeepsContentInsideRoundWatchSafeArea() {
        val xml = readFile("src/main/res/layout/activity_watch_bookshelf.xml")

        assertTrue(xml.contains("android:paddingStart=\"30dp\""))
        assertTrue(xml.contains("android:paddingEnd=\"30dp\""))
        assertTrue(xml.contains("android:paddingTop=\"24dp\""))
        assertTrue(xml.contains("android:paddingBottom=\"24dp\""))
        assertFalse(xml.contains("android:minWidth=\"64dp\""))
    }

    @Test
    fun edgeSwipeBackLayoutHandlesSwipeBeforeChildConsumesUpEvent() {
        val source = readFile("src/main/java/io/legado/app/ui/watch/EdgeSwipeBackLayout.kt")

        assertTrue(source.contains("MotionEvent.ACTION_MOVE"))
        assertTrue(source.contains("hasTriggeredBack"))
    }

    @Test
    fun edgeSwipeBackLayoutDoesNotConsumePlainEdgeTaps() {
        val source = readFile("src/main/java/io/legado/app/ui/watch/EdgeSwipeBackLayout.kt")

        assertTrue(source.contains("isTrackingEdgeSwipe = startX <= edgeWidthPx"))
        assertFalse(source.contains("if (isTrackingEdgeSwipe) return true"))
    }

    @Test
    fun edgeSwipeBackLayoutKeepsBlankEdgeSwipeStreamsAlive() {
        val source = readFile("src/main/java/io/legado/app/ui/watch/EdgeSwipeBackLayout.kt")

        assertTrue(source.contains("val handled = super.dispatchTouchEvent(ev)"))
        assertTrue(source.contains("return handled || isTrackingEdgeSwipe"))
    }

    @Test
    fun readerEdgeSwipeClosesWatchOverlaysBeforeFinishing() {
        val source = readFile("src/main/java/io/legado/app/ui/book/read/ReadBookActivity.kt")

        assertTrue(source.contains("closeWatchOverlays()"))
        assertTrue(source.contains("binding.root.setOnEdgeBack"))
    }

    @Test
    fun watchReaderSettingsUsesFullscreenMenuInsteadOfPhoneDialog() {
        val source = readFile("src/main/java/io/legado/app/ui/book/read/ReadBookActivity.kt")
        val layout = readFile("src/main/res/layout/activity_book_read.xml")

        assertFalse(source.contains("showDialogFragment<WatchReaderSettingsDialog>"))
        assertTrue(layout.contains("WatchReadSettingsMenu"))
    }

    private fun readFile(path: String): String {
        val file = listOf(Paths.get(path), Paths.get("app", path)).first(Files::exists)
        return String(Files.readAllBytes(file))
    }
}

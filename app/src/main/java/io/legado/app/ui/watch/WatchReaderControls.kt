package io.legado.app.ui.watch

object WatchReaderControls {

    const val MIN_TEXT_SIZE = 16
    const val MAX_TEXT_SIZE = 32
    const val MIN_LINE_SPACING = 2
    const val MAX_LINE_SPACING = 20
    const val MIN_BRIGHTNESS = 8
    const val MAX_BRIGHTNESS = 255

    fun nextTextSize(current: Int, delta: Int): Int {
        return (current + delta).coerceIn(MIN_TEXT_SIZE, MAX_TEXT_SIZE)
    }

    fun nextLineSpacing(current: Int, delta: Int): Int {
        return (current + delta).coerceIn(MIN_LINE_SPACING, MAX_LINE_SPACING)
    }

    fun nextBrightness(current: Int, delta: Int): Int {
        return (current + delta).coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
    }

    fun chapterProgressText(index: Int, total: Int): String {
        val safeTotal = total.coerceAtLeast(1)
        val safeIndex = index.coerceIn(0, safeTotal - 1) + 1
        return "第 $safeIndex/$safeTotal 章"
    }
}

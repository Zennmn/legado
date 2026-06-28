package io.legado.app.ui.watch

import android.view.WindowManager
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

object WatchReaderControls {

    const val MIN_TEXT_SIZE = 14
    const val MAX_TEXT_SIZE = 28
    const val MIN_LINE_SPACING = 10
    const val MAX_LINE_SPACING = 16
    const val MIN_BRIGHTNESS = 8
    const val MAX_BRIGHTNESS = 255
    const val DEFAULT_TEXT_SIZE = 18
    const val DEFAULT_LINE_SPACING = 12
    const val DEFAULT_PARAGRAPH_SPACING = 1
    private const val DEFAULT_ROUND_PADDING_DP = 36
    private const val MIN_ROUND_PADDING_DP = 26
    private const val MAX_ROUND_PADDING_DP = 42
    private val WATCH_SAFE_CLICK_ACTIONS = setOf(0, 1, 2, 3, 4, 10)

    data class RoundDisplayLayoutDefaults(
        val contentPaddingDp: Int,
        val textSize: Int,
        val lineSpacing: Int,
        val paragraphSpacing: Int
    )

    fun nextTextSize(current: Int, delta: Int): Int {
        return (current + delta).coerceIn(MIN_TEXT_SIZE, MAX_TEXT_SIZE)
    }

    fun nextLineSpacing(current: Int, delta: Int): Int {
        return (current + delta).coerceIn(MIN_LINE_SPACING, MAX_LINE_SPACING)
    }

    fun nextBrightness(current: Int, delta: Int): Int {
        return (current + delta).coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
    }

    fun windowBrightness(readBrightness: Int, followSystem: Boolean): Float {
        return if (followSystem) {
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        } else {
            (readBrightness / 255f).coerceAtLeast(0.004f).coerceAtMost(1f)
        }
    }

    fun chapterProgressText(index: Int, total: Int): String {
        val safeTotal = total.coerceAtLeast(1)
        val safeIndex = index.coerceIn(0, safeTotal - 1) + 1
        return "第 $safeIndex/$safeTotal 章"
    }

    fun watchSafeClickAction(action: Int, fallback: Int = 0): Int {
        return if (action in WATCH_SAFE_CLICK_ACTIONS) action else fallback
    }

    fun roundDisplayLayoutDefaults(
        widthPixels: Int,
        heightPixels: Int,
        density: Float
    ): RoundDisplayLayoutDefaults {
        return RoundDisplayLayoutDefaults(
            contentPaddingDp = roundDisplayContentPaddingDp(widthPixels, heightPixels, density),
            textSize = DEFAULT_TEXT_SIZE,
            lineSpacing = DEFAULT_LINE_SPACING,
            paragraphSpacing = DEFAULT_PARAGRAPH_SPACING
        )
    }

    private fun roundDisplayContentPaddingDp(
        widthPixels: Int,
        heightPixels: Int,
        density: Float
    ): Int {
        if (widthPixels <= 0 || heightPixels <= 0 || density <= 0f) {
            return DEFAULT_ROUND_PADDING_DP
        }
        val sidePixels = min(widthPixels, heightPixels).toDouble()
        val marginPixels = (sidePixels - sidePixels / sqrt(2.0)) / 2.0
        return (marginPixels / density).roundToInt()
            .plus(2)
            .coerceIn(MIN_ROUND_PADDING_DP, MAX_ROUND_PADDING_DP)
    }
}

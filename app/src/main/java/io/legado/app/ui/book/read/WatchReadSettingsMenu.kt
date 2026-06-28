package io.legado.app.ui.book.read

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.FrameLayout
import io.legado.app.R
import io.legado.app.constant.EventBus
import io.legado.app.databinding.ViewWatchReadSettingsMenuBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.ui.watch.WatchReaderControls
import io.legado.app.ui.watch.WatchReaderDefaults
import io.legado.app.utils.activity
import io.legado.app.utils.gone
import io.legado.app.utils.postEvent
import io.legado.app.utils.visible

class WatchReadSettingsMenu @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val binding = ViewWatchReadSettingsMenuBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        isClickable = true
        isFocusable = true
        bindEvent()
        gone()
    }

    fun runMenuIn() {
        upValues()
        visible()
    }

    fun runMenuOut() {
        gone()
    }

    private fun bindEvent() = binding.run {
        watchSettingsRoot.setOnClickListener { runMenuOut() }
        settingsContent.setOnClickListener { }
        btnTextDown.setOnClickListener { changeTextSize(-1) }
        btnTextUp.setOnClickListener { changeTextSize(1) }
        btnLineDown.setOnClickListener { changeLineSpacing(-1) }
        btnLineUp.setOnClickListener { changeLineSpacing(1) }
        btnBrightnessFollowSystem.setOnClickListener { toggleBrightnessFollowSystem() }
        btnBrightnessDown.setOnClickListener { changeBrightness(-16) }
        btnBrightnessUp.setOnClickListener { changeBrightness(16) }
        btnResetBlack.setOnClickListener {
            WatchReaderDefaults.resetAllToBlack()
            postEvent(EventBus.UP_CONFIG, arrayListOf(0, 1, 2, 6, 9, 11))
            applyBrightnessToWindow()
            upValues()
        }
    }

    private fun changeTextSize(delta: Int) {
        val config = ReadBookConfig.durConfig
        config.textSize = WatchReaderControls.nextTextSize(config.textSize, delta)
        ReadBookConfig.save()
        postEvent(EventBus.UP_CONFIG, arrayListOf(2, 6, 9, 11))
        upValues()
    }

    private fun changeLineSpacing(delta: Int) {
        val config = ReadBookConfig.durConfig
        config.lineSpacingExtra = WatchReaderControls.nextLineSpacing(config.lineSpacingExtra, delta)
        ReadBookConfig.save()
        postEvent(EventBus.UP_CONFIG, arrayListOf(2, 6, 9, 11))
        upValues()
    }

    private fun changeBrightness(delta: Int) {
        AppConfig.readBrightnessFollowSystem = false
        AppConfig.readBrightness = WatchReaderControls.nextBrightness(AppConfig.readBrightness, delta)
        applyBrightnessToWindow()
        upValues()
    }

    private fun toggleBrightnessFollowSystem() {
        AppConfig.readBrightnessFollowSystem = !AppConfig.readBrightnessFollowSystem
        applyBrightnessToWindow()
        upValues()
    }

    private fun applyBrightnessToWindow() {
        activity?.window?.let { window ->
            val params = window.attributes
            params.screenBrightness = WatchReaderControls.windowBrightness(
                AppConfig.readBrightness,
                AppConfig.readBrightnessFollowSystem
            )
            window.attributes = params
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun upValues() = binding.run {
        tvTextSize.text = ReadBookConfig.durConfig.textSize.toString()
        tvLineSpacing.text = ReadBookConfig.durConfig.lineSpacingExtra.toString()
        btnBrightnessFollowSystem.text = if (AppConfig.readBrightnessFollowSystem) {
            context.getString(R.string.watch_follow_system_on)
        } else {
            context.getString(R.string.watch_follow_system_off)
        }
        tvBrightness.text = if (AppConfig.readBrightnessFollowSystem) {
            context.getString(R.string.watch_auto)
        } else {
            AppConfig.readBrightness.toString()
        }
    }
}

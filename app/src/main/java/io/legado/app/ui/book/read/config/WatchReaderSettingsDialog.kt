package io.legado.app.ui.book.read.config

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.constant.EventBus
import io.legado.app.databinding.DialogWatchReaderSettingsBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.ui.watch.WatchReaderControls
import io.legado.app.ui.watch.WatchReaderDefaults
import io.legado.app.utils.dpToPx
import io.legado.app.utils.postEvent
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlin.math.min

class WatchReaderSettingsDialog : BaseDialogFragment(R.layout.dialog_watch_reader_settings) {

    private val binding by viewBinding(DialogWatchReaderSettingsBinding::bind)

    override fun onStart() {
        super.onStart()
        dialog?.window?.run {
            setBackgroundDrawableResource(R.color.transparent)
            decorView.setPadding(0, 0, 0, 0)
            val attr = attributes
            attr.gravity = Gravity.CENTER
            attributes = attr
            val width = min(resources.displayMetrics.widthPixels - 48.dpToPx(), 260.dpToPx())
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        bindEvent()
        upValues()
    }

    private fun bindEvent() = binding.run {
        btnTextDown.setOnClickListener { changeTextSize(-1) }
        btnTextUp.setOnClickListener { changeTextSize(1) }
        btnLineDown.setOnClickListener { changeLineSpacing(-1) }
        btnLineUp.setOnClickListener { changeLineSpacing(1) }
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
        AppConfig.readBrightness = WatchReaderControls.nextBrightness(AppConfig.readBrightness, delta)
        applyBrightnessToWindow()
        upValues()
    }

    private fun applyBrightnessToWindow() {
        activity?.window?.let { window ->
            val params = window.attributes
            params.screenBrightness = (AppConfig.readBrightness / 255f)
                .coerceAtLeast(0.004f)
                .coerceAtMost(1f)
            window.attributes = params
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun upValues() = binding.run {
        tvTextSize.text = ReadBookConfig.durConfig.textSize.toString()
        tvLineSpacing.text = ReadBookConfig.durConfig.lineSpacingExtra.toString()
        tvBrightness.text = AppConfig.readBrightness.toString()
    }
}

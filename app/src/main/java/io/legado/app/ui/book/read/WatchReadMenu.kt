package io.legado.app.ui.book.read

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.FrameLayout
import io.legado.app.constant.EventBus
import io.legado.app.databinding.ViewWatchReadMenuBinding
import io.legado.app.help.book.simulatedTotalChapterNum
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.model.ReadBook
import io.legado.app.ui.watch.WatchReaderControls
import io.legado.app.ui.watch.WatchReaderDefaults
import io.legado.app.utils.activity
import io.legado.app.utils.gone
import io.legado.app.utils.postEvent
import io.legado.app.utils.visible

class WatchReadMenu @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var canShowMenu: Boolean = false
    private val binding = ViewWatchReadMenuBinding.inflate(LayoutInflater.from(context), this, true)
    private val callBack: CallBack get() = activity as CallBack
    private var onMenuOutEnd: (() -> Unit)? = null

    init {
        isClickable = true
        isFocusable = true
        bindEvent()
        gone()
    }

    fun runMenuIn(anim: Boolean = false) {
        callBack.onMenuShow()
        upBookView()
        upNumbers()
        visible()
        canShowMenu = true
        callBack.upSystemUiVisibility()
    }

    fun runMenuOut(anim: Boolean = false, onMenuOutEnd: (() -> Unit)? = null) {
        this.onMenuOutEnd = onMenuOutEnd
        gone()
        canShowMenu = false
        callBack.onMenuHide()
        callBack.upSystemUiVisibility()
        this.onMenuOutEnd?.invoke()
        this.onMenuOutEnd = null
    }

    fun reset() {
        upBookView()
        upNumbers()
    }

    fun refreshMenuColorFilter() = Unit

    fun upBrightnessState() {
        upNumbers()
        applyBrightnessToWindow()
    }

    fun upBookView() = binding.run {
        val book = ReadBook.book
        tvBookName.text = book?.name.orEmpty().ifBlank { book?.originName.orEmpty() }
        tvProgress.text = WatchReaderControls.chapterProgressText(
            index = ReadBook.durChapterIndex,
            total = book?.simulatedTotalChapterNum() ?: ReadBook.simulatedChapterSize
        )
    }

    fun upSeekBar() {
        upBookView()
    }

    fun setAutoPage(autoPage: Boolean) = Unit

    fun setSeekPage(seek: Int) = Unit

    private fun bindEvent() = binding.run {
        watchMenuRoot.setOnClickListener { runMenuOut() }
        btnExit.setOnClickListener {
            runMenuOut {
                activity?.finish()
            }
        }
        btnToc.setOnClickListener {
            runMenuOut {
                callBack.openChapterList()
            }
        }
        btnSettings.setOnClickListener {
            runMenuOut {
                callBack.showMoreSetting()
            }
        }
        btnTextDown.setOnClickListener { changeTextSize(-1) }
        btnTextUp.setOnClickListener { changeTextSize(1) }
        btnBrightnessDown.setOnClickListener { changeBrightness(-16) }
        btnBrightnessUp.setOnClickListener { changeBrightness(16) }
        btnOled.setOnClickListener {
            WatchReaderDefaults.resetAllToBlack()
            postEvent(EventBus.UP_CONFIG, arrayListOf(0, 1, 2, 6, 9, 11))
            upNumbers()
            applyBrightnessToWindow()
        }
    }

    private fun changeTextSize(delta: Int) {
        val config = ReadBookConfig.durConfig
        config.textSize = WatchReaderControls.nextTextSize(config.textSize, delta)
        ReadBookConfig.save()
        postEvent(EventBus.UP_CONFIG, arrayListOf(2, 6, 9, 11))
        upNumbers()
    }

    private fun changeBrightness(delta: Int) {
        AppConfig.readBrightnessFollowSystem = false
        AppConfig.readBrightness = WatchReaderControls.nextBrightness(AppConfig.readBrightness, delta)
        applyBrightnessToWindow()
        upNumbers()
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

    private fun upNumbers() = binding.run {
        tvTextSize.text = ReadBookConfig.durConfig.textSize.toString()
        tvBrightness.text = if (AppConfig.readBrightnessFollowSystem) "自动" else AppConfig.readBrightness.toString()
    }

    interface CallBack {
        fun autoPage()
        fun openReplaceRule()
        fun openChapterList()
        fun openSearchActivity(searchWord: String?)
        fun openSourceEditActivity()
        fun openBookInfoActivity()
        fun showReadStyle()
        fun showMoreSetting()
        fun showReadAloudDialog()
        fun upSystemUiVisibility()
        fun onClickReadAloud()
        fun showHelp()
        fun showLogin()
        fun payAction()
        fun disableSource()
        fun skipToChapter(index: Int)
        fun onMenuShow()
        fun onMenuHide()
    }
}

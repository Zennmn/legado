package io.legado.app.ui.watch

import android.graphics.Color
import io.legado.app.constant.PageAnim
import io.legado.app.constant.PreferKey
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.utils.putPrefBoolean
import splitties.init.appCtx

object WatchReaderDefaults {

    fun apply() {
        AppConfig.isNightTheme = true
        AppConfig.isEInkMode = false
        appCtx.putPrefBoolean(PreferKey.showMangaUi, false)
        appCtx.putPrefBoolean(PreferKey.showDiscovery, false)
        appCtx.putPrefBoolean(PreferKey.showRss, false)
        appCtx.putPrefBoolean(PreferKey.textSelectAble, false)
        appCtx.putPrefBoolean(PreferKey.autoRefresh, false)
        appCtx.putPrefBoolean(PreferKey.autoCheckNewBackup, false)

        applyThemeDefaults()
    }

    /**
     * Reset all configs to OLED-black watch defaults.
     * Called from the "纯黑" button in the reader menu / settings dialog.
     */
    fun resetAllToBlack() {
        ReadBookConfig.configList.forEach(::applyConfig)
        applyConfig(ReadBookConfig.shareConfig)
        ReadBookConfig.save()
    }

    /**
     * Apply only theme/background settings without touching user-adjusted
     * values like textSize, lineSpacing, padding.
     */
    private fun applyThemeDefaults() {
        ReadBookConfig.configList.forEach(::applyTheme)
        applyTheme(ReadBookConfig.shareConfig)
        ReadBookConfig.save()
    }

    private fun applyTheme(config: ReadBookConfig.Config) {
        config.bgType = 0
        config.bgTypeNight = 0
        config.bgTypeEInk = 0
        config.bgStr = "#000000"
        config.bgStrNight = "#000000"
        config.bgStrEInk = "#000000"
        config.setCurBg(0, "#000000")
        config.setCurTextColor(Color.rgb(234, 234, 234))
        config.setCurTextAccentColor(Color.rgb(150, 220, 180))
        config.setCurPageAnim(PageAnim.noAnim)
        config.showHeaderLine = false
        config.showFooterLine = false
    }

    private fun applyConfig(config: ReadBookConfig.Config) {
        applyTheme(config)
        config.textSize = 22
        config.lineSpacingExtra = 8
        config.paragraphSpacing = 1
        config.paddingLeft = 18
        config.paddingRight = 18
        config.paddingTop = 14
        config.paddingBottom = 14
        config.headerPaddingLeft = 18
        config.headerPaddingRight = 18
        config.footerPaddingLeft = 18
        config.footerPaddingRight = 18
    }
}

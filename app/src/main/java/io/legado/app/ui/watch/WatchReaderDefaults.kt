package io.legado.app.ui.watch

import android.graphics.Color
import io.legado.app.constant.PageAnim
import io.legado.app.constant.PreferKey
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.help.config.ReadTipConfig
import io.legado.app.utils.putPrefBoolean
import io.legado.app.utils.putPrefInt
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
        applyReaderSurfaceDefaults()
        applyWatchSafeClickActions()
        applyThemeDefaults()
        applyLayoutDefaults()
        ReadBookConfig.save()
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

    private fun applyThemeDefaults() {
        ReadBookConfig.configList.forEach(::applyTheme)
        applyTheme(ReadBookConfig.shareConfig)
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
        applyLayout(
            config,
            WatchReaderControls.roundDisplayLayoutDefaults(
                widthPixels = appCtx.resources.displayMetrics.widthPixels,
                heightPixels = appCtx.resources.displayMetrics.heightPixels,
                density = appCtx.resources.displayMetrics.density
            ),
            resetTypography = true
        )
    }

    private fun applyReaderSurfaceDefaults() {
        ReadBookConfig.hideStatusBar = true
        ReadBookConfig.hideNavigationBar = true
        appCtx.putPrefBoolean(PreferKey.hideStatusBar, true)
        appCtx.putPrefBoolean(PreferKey.hideNavigationBar, true)
        appCtx.putPrefBoolean(PreferKey.textFullJustify, false)
        appCtx.putPrefBoolean(PreferKey.textBottomJustify, false)
    }

    private fun applyLayoutDefaults() {
        val metrics = appCtx.resources.displayMetrics
        val defaults = WatchReaderControls.roundDisplayLayoutDefaults(
            widthPixels = metrics.widthPixels,
            heightPixels = metrics.heightPixels,
            density = metrics.density
        )
        ReadBookConfig.configList.forEach { applyLayout(it, defaults) }
        applyLayout(ReadBookConfig.shareConfig, defaults)
    }

    private fun applyLayout(
        config: ReadBookConfig.Config,
        defaults: WatchReaderControls.RoundDisplayLayoutDefaults,
        resetTypography: Boolean = false
    ) {
        config.textSize = if (resetTypography) {
            defaults.textSize
        } else {
            WatchReaderControls.nextTextSize(config.textSize, delta = 0)
        }
        config.lineSpacingExtra = if (resetTypography) {
            defaults.lineSpacing
        } else {
            WatchReaderControls.nextLineSpacing(config.lineSpacingExtra, delta = 0)
        }
        config.paragraphSpacing = defaults.paragraphSpacing
        config.letterSpacing = 0f
        config.paragraphIndent = ""
        config.titleMode = 2
        config.titleSize = 0
        config.titleTopSpacing = 0
        config.titleBottomSpacing = 0
        config.paddingLeft = defaults.contentPaddingDp
        config.paddingRight = defaults.contentPaddingDp
        config.paddingTop = defaults.contentPaddingDp
        config.paddingBottom = defaults.contentPaddingDp
        config.headerPaddingLeft = defaults.contentPaddingDp
        config.headerPaddingRight = defaults.contentPaddingDp
        config.headerPaddingTop = 0
        config.headerPaddingBottom = 0
        config.footerPaddingLeft = defaults.contentPaddingDp
        config.footerPaddingRight = defaults.contentPaddingDp
        config.footerPaddingTop = 0
        config.footerPaddingBottom = 0
        config.showHeaderLine = false
        config.showFooterLine = false
        config.tipHeaderLeft = ReadTipConfig.none
        config.tipHeaderMiddle = ReadTipConfig.none
        config.tipHeaderRight = ReadTipConfig.none
        config.tipFooterLeft = ReadTipConfig.none
        config.tipFooterMiddle = ReadTipConfig.none
        config.tipFooterRight = ReadTipConfig.none
        config.headerMode = 2
        config.footerMode = 1
    }

    private fun applyWatchSafeClickActions() {
        AppConfig.clickActionTL = saveClickAction(
            PreferKey.clickActionTL,
            WatchReaderControls.watchSafeClickAction(AppConfig.clickActionTL, fallback = 2)
        )
        AppConfig.clickActionTC = saveClickAction(
            PreferKey.clickActionTC,
            WatchReaderControls.watchSafeClickAction(AppConfig.clickActionTC, fallback = 2)
        )
        AppConfig.clickActionTR = saveClickAction(
            PreferKey.clickActionTR,
            WatchReaderControls.watchSafeClickAction(AppConfig.clickActionTR, fallback = 1)
        )
        AppConfig.clickActionML = saveClickAction(
            PreferKey.clickActionML,
            WatchReaderControls.watchSafeClickAction(AppConfig.clickActionML, fallback = 2)
        )
        AppConfig.clickActionMC = saveClickAction(
            PreferKey.clickActionMC,
            WatchReaderControls.watchSafeClickAction(AppConfig.clickActionMC, fallback = 0)
        )
        AppConfig.clickActionMR = saveClickAction(
            PreferKey.clickActionMR,
            WatchReaderControls.watchSafeClickAction(AppConfig.clickActionMR, fallback = 1)
        )
        AppConfig.clickActionBL = saveClickAction(
            PreferKey.clickActionBL,
            WatchReaderControls.watchSafeClickAction(AppConfig.clickActionBL, fallback = 2)
        )
        AppConfig.clickActionBC = saveClickAction(
            PreferKey.clickActionBC,
            WatchReaderControls.watchSafeClickAction(AppConfig.clickActionBC, fallback = 1)
        )
        AppConfig.clickActionBR = saveClickAction(
            PreferKey.clickActionBR,
            WatchReaderControls.watchSafeClickAction(AppConfig.clickActionBR, fallback = 1)
        )
    }

    private fun saveClickAction(key: String, value: Int): Int {
        appCtx.putPrefInt(key, value)
        return value
    }
}

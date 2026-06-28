package io.legado.app

import android.app.Application
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import com.github.liuyueyi.quick.transfer.constants.TransType
import com.jeremyliao.liveeventbus.LiveEventBus
import com.jeremyliao.liveeventbus.logger.DefaultLogger
import io.legado.app.base.AppContextWrapper
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.AppFreezeMonitor
import io.legado.app.help.CrashHandler
import io.legado.app.help.DefaultData
import io.legado.app.help.DispatchersMonitor
import io.legado.app.help.LifecycleHelp
import io.legado.app.help.book.BookHelp
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.help.config.ThemeConfig.applyDayNight
import io.legado.app.help.config.ThemeConfig.applyDayNightInit
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.utils.ChineseUtils
import io.legado.app.utils.LogUtils
import io.legado.app.utils.defaultSharedPreferences
import java.util.logging.Level

class App : Application() {

    private lateinit var oldConfig: Configuration

    override fun onCreate() {
        super.onCreate()
        CrashHandler(this)
        oldConfig = Configuration(resources.configuration)
        applyDayNightInit(this)
        registerActivityLifecycleCallbacks(LifecycleHelp)
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(AppConfig)
        Coroutine.async {
            LogUtils.init(this@App)
            LogUtils.d("App", "onCreate")
            LogUtils.logDeviceInfo()
            LiveEventBus.config()
                .lifecycleObserverAlwaysActive(true)
                .autoClear(false)
                .enableLogger(BuildConfig.DEBUG || AppConfig.recordLog)
                .setLogger(EventLogger())
            DefaultData.upVersion()
            AppFreezeMonitor.init(this@App)
            DispatchersMonitor.init()
            BookHelp.clearInvalidCache()
            ReadBookConfig.clearBgAndCache()
            when (AppConfig.chineseConverterType) {
                1 -> {
                    ChineseUtils.fixT2sDict()
                    ChineseUtils.preLoad(true, TransType.TRADITIONAL_TO_SIMPLE)
                }

                2 -> ChineseUtils.preLoad(true, TransType.SIMPLE_TO_TRADITIONAL)
            }
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppContextWrapper.wrap(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val diff = newConfig.diff(oldConfig)
        if ((diff and ActivityInfo.CONFIG_UI_MODE) != 0) {
            applyDayNight(this)
        }
        oldConfig = Configuration(newConfig)
    }

    class EventLogger : DefaultLogger() {

        override fun log(level: Level, msg: String) {
            super.log(level, msg)
            LogUtils.d(TAG, msg)
        }

        override fun log(level: Level, msg: String, th: Throwable?) {
            super.log(level, msg, th)
            LogUtils.d(TAG, "$msg\n${th?.stackTraceToString()}")
        }

        companion object {
            private const val TAG = "[LiveEventBus]"
        }
    }

    companion object {
        init {
            if (BuildConfig.DEBUG) {
                System.setProperty("kotlinx.coroutines.debug", "on")
            }
        }
    }

}

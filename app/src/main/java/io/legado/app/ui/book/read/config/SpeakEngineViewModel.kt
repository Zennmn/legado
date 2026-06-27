package io.legado.app.ui.book.read.config

import android.app.Application
import android.speech.tts.TextToSpeech
import io.legado.app.base.BaseViewModel

class SpeakEngineViewModel(application: Application) : BaseViewModel(application) {

    val sysEngines: List<TextToSpeech.EngineInfo> by lazy {
        val tts = TextToSpeech(context, null)
        val engines = tts.engines
        tts.shutdown()
        engines
    }

}
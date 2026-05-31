package com.shenghui.localvibe.core.tts

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class BookTtsController(
    context: Context,
    private val onReady: () -> Unit,
    private val onError: (String) -> Unit,
    private val onDone: () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var isReady = false
    private var utteranceCounter = 0

    init {
        val appContext = context.applicationContext
        tts = TextToSpeech(appContext) { status ->
            if (status != TextToSpeech.SUCCESS) {
                mainHandler.post { onError("系统 TTS 初始化失败") }
                return@TextToSpeech
            }

            val engine = tts ?: return@TextToSpeech
            val languageResult = engine.setLanguage(Locale.SIMPLIFIED_CHINESE)
            if (
                languageResult == TextToSpeech.LANG_MISSING_DATA ||
                languageResult == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                mainHandler.post { onError("系统 TTS 不支持中文，请安装中文语音引擎") }
                return@TextToSpeech
            }

            engine.setOnUtteranceProgressListener(
                object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit

                    override fun onDone(utteranceId: String?) {
                        mainHandler.post { onDone() }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        mainHandler.post { onError("朗读失败") }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        mainHandler.post { onError("朗读失败") }
                    }
                }
            )
            isReady = true
            mainHandler.post { onReady() }
        }
    }

    fun speak(text: String, speechRate: Float, pitch: Float): Boolean {
        val engine = tts ?: return false
        if (!isReady || text.isBlank()) return false
        utteranceCounter += 1
        engine.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))
        engine.setPitch(pitch.coerceIn(0.5f, 2.0f))
        val params = Bundle()
        val result = engine.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            params,
            "book-paragraph-$utteranceCounter"
        )
        return result == TextToSpeech.SUCCESS
    }

    fun pause() {
        tts?.stop()
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}

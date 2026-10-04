package com.shenghui.localvibe.core.tts

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale

data class BookTtsSpeakResult(
    val success: Boolean,
    val message: String? = null
)

class BookTtsController(
    context: Context,
    private val onReady: () -> Unit,
    private val onError: (String, Long?) -> Unit,
    private val onWarning: (String) -> Unit = {},
    private val onStart: () -> Unit = {},
    private val onDone: (Long?) -> Unit,
    private val onStop: (Long?) -> Unit = {}
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var isReady = false
    private var utteranceCounter = 0
    private var selectedVoiceName: String? = null
    private val utteranceRegistry = BookTtsUtteranceRegistry()

    init {
        val appContext = context.applicationContext
        runCatching {
            tts = TextToSpeech(appContext) { status ->
                if (status != TextToSpeech.SUCCESS) {
                    mainHandler.post { onError("系统 TTS 初始化失败，请安装或启用系统语音引擎", null) }
                    return@TextToSpeech
                }

                val engine = tts
                if (engine == null) {
                    mainHandler.post { onError("当前系统没有可用语音引擎", null) }
                    return@TextToSpeech
                }
                val selectedLocale = applyBestLanguage(engine)
                if (selectedLocale == null) {
                    mainHandler.post { onError("当前系统缺少可用语音数据，请安装或启用系统语音引擎", null) }
                    return@TextToSpeech
                }

                engine.setOnUtteranceProgressListener(
                    object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            val event = utteranceRegistry.onStart(utteranceId)
                            if (event != null) {
                                mainHandler.post { onStart() }
                            }
                        }

                        override fun onDone(utteranceId: String?) {
                            val event = utteranceRegistry.onDone(utteranceId)
                            event?.let {
                                mainHandler.post { onDone(event.playbackSessionId) }
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            val event = utteranceRegistry.onError(utteranceId)
                            event?.let {
                                mainHandler.post { onError("朗读失败", event.playbackSessionId) }
                            }
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            val event = utteranceRegistry.onError(utteranceId)
                            event?.let {
                                mainHandler.post { onError("朗读失败：$errorCode", event.playbackSessionId) }
                            }
                        }
                    }
                )
                isReady = true
                mainHandler.post {
                    onReady()
                    if (selectedLocale.language != Locale.CHINESE.language) {
                        onWarning("系统语音可用，但中文支持可能不完整")
                    }
                }
            }
        }.onFailure {
            mainHandler.post { onError("当前系统没有可用语音引擎", null) }
        }
    }

    private fun isLanguageUsable(result: Int): Boolean {
        return result != TextToSpeech.LANG_MISSING_DATA &&
            result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    private fun languageCandidates(): List<Locale> {
        return listOf(
            Locale.SIMPLIFIED_CHINESE,
            Locale.CHINESE,
            Locale.CHINA,
            Locale.getDefault(),
            Locale.US
        )
            .distinctBy { it.toLanguageTag() }
    }

    private fun applyBestLanguage(engine: TextToSpeech): Locale? {
        return languageCandidates().firstOrNull { locale ->
            isLanguageUsable(engine.setLanguage(locale))
        }
    }

    fun speakSentence(
        text: String,
        speechRate: Float,
        pitch: Float,
        utteranceId: String? = null,
        playbackSessionId: Long? = null
    ): BookTtsSpeakResult {
        val engine = tts ?: return BookTtsSpeakResult(false, "当前系统没有可用语音引擎")
        if (!isReady) return BookTtsSpeakResult(false, "系统语音仍在初始化或不可用")
        val ttsText = BookTtsTextNormalizer.normalize(text)
        if (ttsText.spokenText.isBlank()) return BookTtsSpeakResult(false, "朗读内容为空")
        utteranceCounter += 1
        selectedVoiceName?.let { voiceName ->
            val voice = engine.voices?.firstOrNull { it.name == voiceName }
            if (voice == null) {
                return BookTtsSpeakResult(false, "当前声线不可用")
            }
            if (engine.setVoice(voice) != TextToSpeech.SUCCESS) {
                return BookTtsSpeakResult(false, "当前声线不可用")
            }
        }
        engine.setSpeechRate(BookSpeechRate.fromUserMultiplier(speechRate).systemTtsSpeechRate)
        engine.setPitch(pitch.coerceIn(0.5f, 2.0f))
        val params = Bundle()
        val actualUtteranceId = utteranceId ?: "book-sentence-$utteranceCounter"
        utteranceRegistry.register(actualUtteranceId, playbackSessionId)
        val result = engine.speak(
            ttsText.spokenText,
            TextToSpeech.QUEUE_FLUSH,
            params,
            actualUtteranceId
        )
        return if (result == TextToSpeech.SUCCESS) {
            BookTtsSpeakResult(true)
        } else {
            utteranceRegistry.remove(actualUtteranceId)
            BookTtsSpeakResult(false, "speak 调用失败")
        }
    }

    fun speak(text: String, speechRate: Float, pitch: Float): Boolean {
        return speakSentence(text, speechRate, pitch).success
    }

    fun getAvailableVoices(): List<BookTtsVoice> {
        val voices = tts?.voices.orEmpty()
        return voices
            .sortedWith(
                compareByDescending<Voice> { it.locale?.language == Locale.CHINESE.language }
                    .thenBy { it.locale?.toLanguageTag().orEmpty() }
                    .thenBy { it.name }
            )
            .map { voice ->
                BookTtsVoice(
                    name = voice.name,
                    localeTag = voice.locale?.toLanguageTag().orEmpty().ifBlank { "未知语言" },
                    isNetworkConnectionRequired = voice.isNetworkConnectionRequired
                )
            }
    }

    fun selectVoice(voiceName: String?): Boolean {
        val engine = tts ?: return false
        if (!isReady) return false
        if (voiceName == null) {
            applyBestLanguage(engine) ?: return false
            selectedVoiceName = null
            return true
        }
        val voice = engine.voices?.firstOrNull { it.name == voiceName } ?: return false
        val result = engine.setVoice(voice)
        if (result == TextToSpeech.SUCCESS) {
            selectedVoiceName = voiceName
            return true
        }
        return false
    }

    fun pause() {
        val stopped = utteranceRegistry.onStop()
        tts?.stop()
        stopped.forEach { event ->
            mainHandler.post { onStop(event.playbackSessionId) }
        }
    }

    fun stop() {
        val stopped = utteranceRegistry.onStop()
        tts?.stop()
        stopped.forEach { event ->
            mainHandler.post { onStop(event.playbackSessionId) }
        }
    }

    fun shutdown() {
        utteranceRegistry.onStop()
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}

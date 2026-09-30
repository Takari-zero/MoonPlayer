package com.shenghui.localvibe.feature.book

import android.content.Context
import com.shenghui.localvibe.core.tts.MatchaAuditionSynthesizer
import com.shenghui.localvibe.core.tts.PcmAudioChunk
import com.shenghui.localvibe.core.tts.StreamingPcmAudioPlayer
import com.shenghui.localvibe.core.tts.StreamingTtsResult
import java.io.File

/** Small screen-scoped controller for the experimental Matcha audition only. */
internal class BookMatchaAuditionController(context: Context) {
    private val synthesizer = MatchaAuditionSynthesizer(context.applicationContext)
    private var player: StreamingPcmAudioPlayer? = null
    private var sessionId = 0L

    val isAvailable: Boolean
        get() = REQUIRED_FILES.all { File(root, it).isFile }

    suspend fun play(speed: Float, onStarted: () -> Unit): StreamingTtsResult {
        stop()
        val nextSessionId = ++sessionId
        var started = false
        return synthesizer.synthesize(
            text = "你好，这是离线语音试听。清晨时分，我们继续阅读这本书。",
            speed = speed
        ) { chunk: PcmAudioChunk ->
            if (!started) {
                val nextPlayer = StreamingPcmAudioPlayer()
                val startResult = nextPlayer.startSession(nextSessionId, chunk.format)
                if (startResult.isFailure) {
                    nextPlayer.release()
                    error(startResult.exceptionOrNull()?.message ?: "AudioTrack 启动失败")
                }
                player = nextPlayer
                started = true
                onStarted()
            }
            player?.write(nextSessionId, chunk)?.getOrThrow()
        }
    }

    fun stop() {
        sessionId += 1
        val currentPlayer = player
        player = null
        synthesizer.stop()
        currentPlayer?.stop()
        currentPlayer?.release()
    }

    fun release() {
        stop()
        synthesizer.release()
    }

    private val root: File
        get() = File(appContext.filesDir, "bench_models/matcha-icefall-zh-baker")

    private val appContext = context.applicationContext

    private companion object {
        val REQUIRED_FILES = listOf(
            "model-steps-3.onnx",
            "vocos-22khz-univ.onnx",
            "tokens.txt",
            "lexicon.txt",
            "phone.fst",
            "date.fst",
            "number.fst"
        )
    }
}

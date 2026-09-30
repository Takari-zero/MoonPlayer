package com.shenghui.localvibe.core.tts

import android.content.Context
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsMatchaModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/** Debug-only Matcha audition adapter. It never participates in正文 playback. */
class MatchaAuditionSynthesizer(context: Context) {
    private val root = File(
        context.applicationContext.filesDir,
        "bench_models/matcha-icefall-zh-baker"
    )
    private var tts: OfflineTts? = null
    @Volatile
    private var stopped = false

    suspend fun synthesize(
        text: String,
        speed: Float = 1f,
        onChunk: suspend (PcmAudioChunk) -> Unit
    ): StreamingTtsResult = withContext(Dispatchers.IO) {
        stopped = false
        try {
            requireCompleteModel()
            val offlineTts = tts ?: OfflineTts(
                assetManager = null,
                config = createConfig()
            ).also { tts = it }
            if (stopped) return@withContext StreamingTtsResult.Stopped
            val audio = offlineTts.generate(text, sid = 0, speed = speed)
            if (stopped) return@withContext StreamingTtsResult.Stopped
            onChunk(PcmAudioChunk(
                data = floatSamplesToPcm16(audio.samples),
                format = PcmAudioFormat(sampleRate = audio.sampleRate, channelCount = 1),
                isFinal = true
            ))
            StreamingTtsResult.Success
        } catch (error: Throwable) {
            StreamingTtsResult.Error(error.message ?: error::class.java.simpleName)
        }
    }

    fun stop() {
        stopped = true
    }

    fun release() {
        tts?.release()
        tts = null
    }

    private fun requireCompleteModel() {
        val required = listOf(
            "model-steps-3.onnx",
            "vocos-22khz-univ.onnx",
            "tokens.txt",
            "lexicon.txt",
            "phone.fst",
            "date.fst",
            "number.fst"
        )
        val missing = required.filterNot { File(root, it).isFile }
        require(missing.isEmpty()) { "Matcha audition model missing: ${missing.joinToString()}" }
    }

    private fun createConfig(): OfflineTtsConfig {
        val acoustic = File(root, "model-steps-3.onnx").absolutePath
        val vocoder = File(root, "vocos-22khz-univ.onnx").absolutePath
        val tokens = File(root, "tokens.txt").absolutePath
        val lexicon = File(root, "lexicon.txt").absolutePath
        val rules = listOf("phone.fst", "date.fst", "number.fst")
            .joinToString(",") { File(root, it).absolutePath }
        return OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                matcha = OfflineTtsMatchaModelConfig(
                    acousticModel = acoustic,
                    vocoder = vocoder,
                    tokens = tokens,
                    lexicon = lexicon,
                    dataDir = root.absolutePath,
                    dictDir = File(root, "dict").absolutePath
                ),
                numThreads = 2,
                debug = true,
                provider = "cpu"
            ),
            ruleFsts = rules,
            ruleFars = "",
            maxNumSentences = 1,
            silenceScale = 0.2f
        )
    }

    private fun floatSamplesToPcm16(samples: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { sample ->
            val clamped = sample.coerceIn(-1f, 1f)
            buffer.putShort((clamped * 32767f).roundToInt().toShort())
        }
        return buffer.array()
    }
}

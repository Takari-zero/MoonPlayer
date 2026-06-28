package com.shenghui.localvibe.core.tts

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

class Aishell3SegmentedStreamingTtsEngine(
    context: Context
) : StreamingTtsEngine {
    override val name: String = "sherpa-aishell3-segmented"
    override var isReady: Boolean = false
        private set

    private val appContext = context.applicationContext
    private var tts: OfflineTts? = null

    @Volatile
    private var stopRequested = false

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        if (isReady) {
            return@withContext Result.success(Unit)
        }

        runCatching {
            stopRequested = false
            val startedAt = System.currentTimeMillis()
            val modelDir = prepareModelFiles()
            val ruleFsts = RULE_FST_FILES.joinToString(",") {
                File(modelDir, it).absolutePath
            }

            Log.d(TAG, "modelDir=${modelDir.absolutePath}")
            Log.d(TAG, "ruleFsts=$ruleFsts")
            Log.d(TAG, "ruleFars=")

            val config = OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    vits = OfflineTtsVitsModelConfig(
                        model = File(modelDir, "model.onnx").absolutePath,
                        lexicon = File(modelDir, "lexicon.txt").absolutePath,
                        tokens = File(modelDir, "tokens.txt").absolutePath,
                        dataDir = "",
                        dictDir = ""
                    ),
                    numThreads = 4,
                    debug = true,
                    provider = "cpu"
                ),
                ruleFsts = ruleFsts,
                ruleFars = "",
                maxNumSentences = 1,
                silenceScale = 0.2f
            )

            tts = OfflineTts(assetManager = null, config = config)
            isReady = true
            val initCostMs = System.currentTimeMillis() - startedAt
            Log.d(TAG, "initCostMs=$initCostMs")
            Log.d(TAG, "sampleRate=${tts?.sampleRate()}")
            Log.d(TAG, "numSpeakers=${tts?.numSpeakers()}")
            Unit
        }.onFailure { error ->
            Log.e(TAG, "initialize error", error)
            release()
        }
    }

    override suspend fun speak(
        text: String,
        params: StreamingTtsParams,
        onStart: () -> Unit,
        onChunk: suspend (PcmAudioChunk) -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ): StreamingTtsResult = withContext(Dispatchers.IO) {
        val initResult = initialize()
        if (initResult.isFailure) {
            val message = initResult.exceptionOrNull()?.message ?: "aishell3 初始化失败"
            onError(message)
            return@withContext StreamingTtsResult.Error(message)
        }

        val offlineTts = tts
        if (offlineTts == null) {
            val message = "aishell3 引擎不可用"
            onError(message)
            return@withContext StreamingTtsResult.Error(message)
        }

        val segments = splitForPreview(text)
        if (segments.isEmpty()) {
            val message = "试听文本为空"
            onError(message)
            return@withContext StreamingTtsResult.Error(message)
        }

        stopRequested = false
        var started = false
        val clickStartMs = System.currentTimeMillis()
        var totalSynthesizeCostMs = 0L

        try {
            for ((index, segmentText) in segments.withIndex()) {
                if (stopRequested) {
                    Log.d(TAG, "stopped=true segmentIndex=$index")
                    return@withContext StreamingTtsResult.Stopped
                }

                val segmentStartMs = System.currentTimeMillis()
                val audio = offlineTts.generate(
                    text = segmentText,
                    sid = DEFAULT_SPEAKER_ID,
                    speed = params.speed.coerceIn(0.5f, 2.0f)
                )
                val segmentSynthesizeCostMs = System.currentTimeMillis() - segmentStartMs
                totalSynthesizeCostMs += segmentSynthesizeCostMs

                val sampleRate = audio.sampleRate.takeIf { it > 0 } ?: DEFAULT_SAMPLE_RATE
                val pcm = floatSamplesToPcm16(audio.samples, params.volume)
                val audioDurationMs = if (sampleRate > 0) {
                    (audio.samples.size * 1000L) / sampleRate
                } else {
                    0L
                }

                Log.d(TAG, "segmentIndex=$index")
                Log.d(TAG, "segmentText=$segmentText")
                Log.d(TAG, "segmentSynthesizeCostMs=$segmentSynthesizeCostMs")
                Log.d(TAG, "segmentAudioDurationMs=$audioDurationMs")
                Log.d(TAG, "segmentSampleRate=$sampleRate")
                Log.d(TAG, "segmentPcmBytes=${pcm.size}")

                if (stopRequested) {
                    Log.d(TAG, "stopped=true after synth segmentIndex=$index")
                    return@withContext StreamingTtsResult.Stopped
                }

                if (!started) {
                    started = true
                    val clickToFirstChunkMs = System.currentTimeMillis() - clickStartMs
                    Log.d(TAG, "clickToFirstChunkMs=$clickToFirstChunkMs")
                    onStart()
                }

                onChunk(
                    PcmAudioChunk(
                        data = pcm,
                        format = PcmAudioFormat(
                            sampleRate = sampleRate,
                            channelCount = 1,
                            encoding = PcmAudioEncoding.PCM_16BIT
                        ),
                        isFinal = index == segments.lastIndex
                    )
                )
            }

            Log.d(TAG, "totalSynthesizeCostMs=$totalSynthesizeCostMs")
            onDone()
            StreamingTtsResult.Success
        } catch (error: Throwable) {
            val message = error.message ?: error::class.java.simpleName
            Log.e(TAG, "error=$message", error)
            onError(message)
            StreamingTtsResult.Error(message)
        }
    }

    override fun stop() {
        stopRequested = true
        Log.d(TAG, "stop")
    }

    override fun release() {
        stopRequested = true
        isReady = false
        tts?.release()
        tts = null
        Log.d(TAG, "release")
    }

    private fun prepareModelFiles(): File {
        val targetDir = File(appContext.filesDir, "offline_tts/aishell3")
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        MODEL_FILES.forEach { fileName ->
            copyAssetIfNeeded(
                assetPath = "$ASSET_DIR/$fileName",
                targetFile = File(targetDir, fileName)
            )
        }

        return targetDir
    }

    private fun copyAssetIfNeeded(assetPath: String, targetFile: File) {
        targetFile.parentFile?.mkdirs()
        val assetLength = appContext.assets.open(assetPath).use { it.available().toLong() }
        if (targetFile.exists() && targetFile.length() == assetLength) {
            return
        }

        appContext.assets.open(assetPath).use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun splitForPreview(text: String): List<String> {
        return if (text == PREVIEW_TEXT) {
            PREVIEW_SEGMENTS
        } else {
            text.split(Regex("[，。！？；、\\s]+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .ifEmpty { listOf(text.trim()) }
        }
    }

    private fun floatSamplesToPcm16(samples: FloatArray, volume: Float): ByteArray {
        val gain = volume.coerceIn(0f, 1.5f)
        val bytes = ByteArray(samples.size * BYTES_PER_SAMPLE)
        samples.forEachIndexed { index, sample ->
            val clamped = (sample * gain).coerceIn(-1f, 1f)
            val shortValue = (clamped * Short.MAX_VALUE).roundToInt().toShort()
            val byteIndex = index * BYTES_PER_SAMPLE
            bytes[byteIndex] = (shortValue.toInt() and 0xFF).toByte()
            bytes[byteIndex + 1] = ((shortValue.toInt() shr 8) and 0xFF).toByte()
        }
        return bytes
    }

    companion object {
        private const val TAG = "Aishell3StreamingTts"
        private const val ASSET_DIR = "offline_tts/aishell3"
        private const val DEFAULT_SAMPLE_RATE = 8000
        private const val DEFAULT_SPEAKER_ID = 10
        private const val BYTES_PER_SAMPLE = 2
        const val PREVIEW_TEXT = "这是一段自研离线流式语音试听。"

        private val PREVIEW_SEGMENTS = listOf(
            "这是一段",
            "自研离线",
            "流式语音试听"
        )

        private val MODEL_FILES = listOf(
            "model.onnx",
            "tokens.txt",
            "lexicon.txt",
            "phone.fst",
            "number.fst",
            "date.fst",
            "new_heteronym.fst",
            "speakers.txt"
        )

        private val RULE_FST_FILES = listOf(
            "phone.fst",
            "date.fst",
            "number.fst",
            "new_heteronym.fst"
        )
    }
}

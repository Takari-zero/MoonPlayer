package com.shenghui.localvibe.core.tts

import android.content.Context
import android.os.Looper
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
            val resourceConfig = OfflineTtsResourceConfigs.AISHELL3
            val ruleFsts = resourceConfig.ruleFstFiles.joinToString(",") {
                File(modelDir, it).absolutePath
            }

            Log.d(TAG, "modelDir=${modelDir.absolutePath}")
            Log.d(TAG, "ruleFsts=$ruleFsts")
            Log.d(TAG, "ruleFars=")

            val config = OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    vits = OfflineTtsVitsModelConfig(
                        model = File(modelDir, resourceConfig.modelFile).absolutePath,
                        lexicon = File(modelDir, resourceConfig.lexiconFile).absolutePath,
                        tokens = File(modelDir, resourceConfig.tokensFile).absolutePath,
                        dataDir = "",
                        dictDir = resourceConfig.dictDir
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

            tts = synchronized(NATIVE_TTS_LOCK) {
                OfflineTts(assetManager = null, config = config)
            }
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

        val segments = splitTextSegments(text)
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
                Log.i(
                    BOOK_HOT_TTS_TAG,
                    "native synth enter session=speak segmentIndex=$index main=${isMainThread()} " +
                        "thread=${Thread.currentThread().name}"
                )
                val audio = synchronized(NATIVE_TTS_LOCK) {
                    val speechRate = BookSpeechRate.fromUserMultiplier(params.speed)
                    offlineTts.generate(
                        text = segmentText,
                        sid = DEFAULT_SPEAKER_ID,
                        speed = speechRate.sherpaGenerateSpeed
                    )
                }
                Log.i(
                    BOOK_HOT_TTS_TAG,
                    "native synth exit session=speak segmentIndex=$index thread=${Thread.currentThread().name}"
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

    suspend fun synthesizeToChunks(
        text: String,
        params: StreamingTtsParams
    ): Result<List<PcmAudioChunk>> = withContext(Dispatchers.IO) {
        val segments = splitTextSegments(text)
        if (segments.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("text is blank"))
        }

        runCatching {
            segments.mapIndexed { index, segmentText ->
                synthesizeSegmentToChunk(
                    segmentText = segmentText,
                    params = params,
                    sessionLabel = "prewarm",
                    segmentIndex = index,
                    isFinal = index == segments.lastIndex
                ).getOrThrow()
            }
        }
    }

    suspend fun synthesizeSegmentToChunk(
        segmentText: String,
        params: StreamingTtsParams,
        sessionLabel: String,
        segmentIndex: Int,
        isFinal: Boolean
    ): Result<PcmAudioChunk> = withContext(Dispatchers.IO) {
        val initResult = initialize()
        if (initResult.isFailure) {
            return@withContext Result.failure(
                initResult.exceptionOrNull() ?: IllegalStateException("aishell3 initialize failed")
            )
        }

        val offlineTts = tts
            ?: return@withContext Result.failure(IllegalStateException("aishell3 engine unavailable"))

        val ttsText = BookTtsTextNormalizer.normalize(segmentText)
        if (ttsText.spokenText.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("text is blank"))
        }

        runCatching {
            val startedAt = System.currentTimeMillis()
            Log.i(
                BOOK_HOT_TTS_TAG,
                "native synth enter session=$sessionLabel segmentIndex=$segmentIndex main=${isMainThread()} " +
                    "thread=${Thread.currentThread().name}"
            )
            val audio = synchronized(NATIVE_TTS_LOCK) {
                val speechRate = BookSpeechRate.fromUserMultiplier(params.speed)
                offlineTts.generate(
                    text = ttsText.spokenText,
                    sid = DEFAULT_SPEAKER_ID,
                    speed = speechRate.sherpaGenerateSpeed
                )
            }
            Log.i(
                BOOK_HOT_TTS_TAG,
                "native synth exit session=$sessionLabel segmentIndex=$segmentIndex thread=${Thread.currentThread().name}"
            )
            val sampleRate = audio.sampleRate.takeIf { it > 0 } ?: DEFAULT_SAMPLE_RATE
            val pcm = floatSamplesToPcm16(audio.samples, params.volume)
            Log.d(
                TAG,
                "$sessionLabel segmentIndex=$segmentIndex costMs=${System.currentTimeMillis() - startedAt} " +
                    "sampleRate=$sampleRate pcmBytes=${pcm.size}"
            )
            PcmAudioChunk(
                data = pcm,
                format = PcmAudioFormat(
                    sampleRate = sampleRate,
                    channelCount = 1,
                    encoding = PcmAudioEncoding.PCM_16BIT
                ),
                isFinal = isFinal
            )
        }
    }

    override fun stop() {
        stopRequested = true
        Log.d(TAG, "stop")
    }

    override fun release() {
        stopRequested = true
        isReady = false
        synchronized(NATIVE_TTS_LOCK) {
            tts?.release()
        }
        tts = null
        Log.d(TAG, "release")
    }

    private fun isMainThread(): Boolean {
        return Looper.myLooper() == Looper.getMainLooper()
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

    fun splitTextSegments(text: String): List<String> {
        val ttsText = BookTtsTextNormalizer.normalize(text)
        return if (ttsText.spokenText == PREVIEW_TEXT) {
            PREVIEW_SEGMENTS
        } else {
            splitIntoShortSegments(ttsText.spokenText)
        }
    }

    private fun splitIntoShortSegments(text: String): List<String> {
        val normalized = text.trim()
        if (normalized.isBlank()) return emptyList()
        val segments = mutableListOf<String>()
        val builder = StringBuilder()

        fun flush() {
            val segment = builder.toString().trim()
            if (segment.isNotEmpty()) {
                segments += segment
            }
            builder.clear()
        }

        normalized.forEach { char ->
            if (char.isWhitespace() && char != '\n' && char != '\r') {
                if (builder.isNotEmpty() && builder.last() != ' ') {
                    builder.append(' ')
                }
            } else {
                builder.append(char)
            }

            if (isSegmentBoundary(char) || builder.length >= MAX_SEGMENT_CHARS) {
                flush()
            }
        }
        flush()
        return segments.ifEmpty { listOf(normalized) }
    }

    private fun isSegmentBoundary(char: Char): Boolean {
        return char == '\uFF0C' ||
            char == '\u3002' ||
            char == '\uFF01' ||
            char == '\uFF1F' ||
            char == '\uFF1B' ||
            char == '\uFF1A' ||
            char == '\u3001' ||
            char == ',' ||
            char == '.' ||
            char == '!' ||
            char == '?' ||
            char == ';' ||
            char == ':' ||
            char == '\n' ||
            char == '\r'
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
        private const val BOOK_HOT_TTS_TAG = "BookListenHot"
        private val NATIVE_TTS_LOCK = Any()
        private const val ASSET_DIR = "offline_tts/aishell3"
        private const val DEFAULT_SAMPLE_RATE = 8000
        private const val DEFAULT_SPEAKER_ID = 10
        private const val BYTES_PER_SAMPLE = 2
        private const val MAX_SEGMENT_CHARS = 24
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

    }
}

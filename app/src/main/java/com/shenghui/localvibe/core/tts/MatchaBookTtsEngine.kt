package com.shenghui.localvibe.core.tts

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsMatchaModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt

/** Debug-only whole-sentence Matcha provider for real-book listening experiments. */
internal class MatchaRequestLifecycle {
    private val generation = AtomicLong(0L)

    @Synchronized
    fun start(): Request {
        return Request(generation.incrementAndGet())
    }

    @Synchronized
    fun stop(): Long {
        return generation.incrementAndGet() - 1L
    }

    fun isActive(request: Request, stage: String): Boolean {
        val currentGeneration = generation.get()
        return request.id == currentGeneration
    }

    fun currentGeneration(): Long = generation.get()

    data class Request(val id: Long)
}

class MatchaBookTtsEngine(context: Context) : StreamingTtsEngine {
    override val name: String = "matcha-experimental"
    override var isReady: Boolean = false
        private set

    private val appContext = context.applicationContext
    private val root = File(appContext.filesDir, MODEL_RELATIVE_PATH)
    private var tts: OfflineTts? = null

    private val requestLifecycle = MatchaRequestLifecycle()

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        if (isReady) return@withContext Result.success(Unit)
        runCatching {
            requireCompleteModel()
            tts = synchronized(OfflineTtsNativeLock) {
                OfflineTts(assetManager = null, config = createConfig())
            }
            isReady = true
            Log.i(TAG, "initialized model=matcha-icefall-zh-baker threads=2 sampleRate=${tts?.sampleRate()}")
            Unit
        }.onFailure {
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
        val request = requestLifecycle.start()
        Log.i(TAG, "MATCHA_REQUEST_START generation=${request.id}")
        val normalized = BookTtsTextNormalizer.normalize(text)
        if (normalized.spokenText.isBlank()) {
            val error = "text is blank"
            onError(error)
            return@withContext StreamingTtsResult.Error(error)
        }
        val initResult = initialize()
        if (initResult.isFailure) {
            val error = initResult.exceptionOrNull()?.message ?: "Matcha initialize failed"
            onError(error)
            return@withContext StreamingTtsResult.Error(error)
        }
        if (!isRequestActive(request, "after_initialize")) {
            return@withContext StreamingTtsResult.Stopped
        }

        val requestStartedAt = SystemClock.elapsedRealtime()
        return@withContext runCatching {
            if (!isRequestActive(request, "before_start")) {
                return@runCatching StreamingTtsResult.Stopped
            }
            onStart()
            var lockWaitMs = 0L
            var nativeGenerateMs = 0L
            val lockRequestedAt = SystemClock.elapsedRealtime()
            val generated = synchronized(OfflineTtsNativeLock) {
                lockWaitMs = SystemClock.elapsedRealtime() - lockRequestedAt
                if (!isRequestActive(request, "before_generate")) {
                    return@runCatching StreamingTtsResult.Stopped
                }
                val nativeGenerateStartedAt = SystemClock.elapsedRealtime()
                val result = tts?.generate(
                    text = normalized.spokenText,
                    sid = 0,
                    speed = BookSpeechRate.fromUserMultiplier(params.speed).sherpaGenerateSpeed
                ) ?: error("Matcha engine unavailable")
                nativeGenerateMs = SystemClock.elapsedRealtime() - nativeGenerateStartedAt
                result
            }
            if (!isRequestActive(request, "after_generate")) {
                return@runCatching StreamingTtsResult.Stopped
            }
            val sampleRate = generated.sampleRate.takeIf { it > 0 } ?: DEFAULT_SAMPLE_RATE
            val pcm = floatSamplesToPcm16(generated.samples, params.volume)
            if (!isRequestActive(request, "after_pcm_conversion")) {
                return@runCatching StreamingTtsResult.Stopped
            }
            val audioDurationMs = generated.samples.size * 1000L / sampleRate
            val generateMs = SystemClock.elapsedRealtime() - requestStartedAt
            val rtf = if (audioDurationMs > 0) generateMs.toDouble() / audioDurationMs else 0.0
            Log.i(
                TAG,
                "MATCHA_BOOK_SYNTHESIS textLength=${normalized.spokenText.length} " +
                    "generateMs=$generateMs audioDurationMs=$audioDurationMs " +
                    "rtf=${"%.3f".format(java.util.Locale.US, rtf)} sampleRate=$sampleRate " +
                    "generation=${request.id} lockWaitMs=$lockWaitMs nativeGenerateMs=$nativeGenerateMs"
            )
            if (!isRequestActive(request, "before_chunk")) {
                return@runCatching StreamingTtsResult.Stopped
            }
            onChunk(
                PcmAudioChunk(
                    data = pcm,
                    format = PcmAudioFormat(sampleRate = sampleRate, channelCount = 1),
                    isFinal = true
                )
            )
            if (!isRequestActive(request, "before_completion")) {
                return@runCatching StreamingTtsResult.Stopped
            }
            onDone()
            Log.i(TAG, "MATCHA_REQUEST_COMPLETE generation=${request.id}")
            StreamingTtsResult.Success
        }.getOrElse { error ->
            val message = error.message ?: error::class.java.simpleName
            onError(message)
            StreamingTtsResult.Error(message)
        }
    }

    /** Synthesis-only entry point used by the single next-target prewarm slot. */
    suspend fun prepare(
        text: String,
        params: StreamingTtsParams
    ): Result<MatchaPreparedAudio> = withContext(Dispatchers.IO) {
        val normalized = BookTtsTextNormalizer.normalize(text)
        if (normalized.spokenText.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("text is blank"))
        }
        val initResult = initialize()
        if (initResult.isFailure) {
            return@withContext Result.failure(
                initResult.exceptionOrNull() ?: IllegalStateException("Matcha initialize failed")
            )
        }
        runCatching {
            var lockWaitMs = 0L
            val startedAt = SystemClock.elapsedRealtime()
            val lockRequestedAt = SystemClock.elapsedRealtime()
            val generated = synchronized(OfflineTtsNativeLock) {
                lockWaitMs = SystemClock.elapsedRealtime() - lockRequestedAt
                val nativeStartedAt = SystemClock.elapsedRealtime()
                val result = tts?.generate(
                    text = normalized.spokenText,
                    sid = 0,
                    speed = BookSpeechRate.fromUserMultiplier(params.speed).sherpaGenerateSpeed
                ) ?: error("Matcha engine unavailable")
                Log.i(TAG, "MATCHA_PREWARM_NATIVE_GENERATE ms=${SystemClock.elapsedRealtime() - nativeStartedAt}")
                result
            }
            val sampleRate = generated.sampleRate.takeIf { it > 0 } ?: DEFAULT_SAMPLE_RATE
            val pcm = floatSamplesToPcm16(generated.samples, params.volume)
            val audioDurationMs = generated.samples.size * 1000L / sampleRate
            val generateMs = SystemClock.elapsedRealtime() - startedAt
            val mappedSpeed = BookSpeechRate.fromUserMultiplier(params.speed).sherpaGenerateSpeed
            val rtf = if (audioDurationMs > 0) generateMs.toDouble() / audioDurationMs else 0.0
            Log.i(
                TAG,
                "MATCHA_BOOK_SYNTHESIS purpose=prewarm textLength=${normalized.spokenText.length} " +
                    "generateMs=$generateMs audioDurationMs=$audioDurationMs " +
                    "rtf=${"%.3f".format(java.util.Locale.US, rtf)} sampleRate=$sampleRate " +
                    "lockWaitMs=$lockWaitMs"
            )
            MatchaPreparedAudio(
                chunk = PcmAudioChunk(
                    data = pcm,
                    format = PcmAudioFormat(sampleRate = sampleRate, channelCount = 1),
                    isFinal = true
                ),
                audioDurationMs = audioDurationMs,
                generateMs = generateMs,
                lockWaitMs = lockWaitMs,
                provider = name,
                speed = params.speed,
                textHash = normalized.spokenText.hashCode()
            )
        }
    }

    override fun stop() {
        val stoppedGeneration = requestLifecycle.stop()
        Log.i(TAG, "MATCHA_REQUEST_STOPPED generation=$stoppedGeneration reason=stop")
    }

    override fun release() {
        requestLifecycle.stop()
        isReady = false
        synchronized(OfflineTtsNativeLock) {
            tts?.release()
        }
        tts = null
    }

    fun isModelAvailable(): Boolean {
        return REQUIRED_FILES.all { File(root, it).isFile }
    }

    private fun requireCompleteModel() {
        val missing = REQUIRED_FILES.filterNot { File(root, it).isFile }
        require(missing.isEmpty()) { "Matcha model missing: ${missing.joinToString()}" }
    }

    private fun isRequestActive(request: MatchaRequestLifecycle.Request, stage: String): Boolean {
        val active = requestLifecycle.isActive(request, stage)
        if (!active) {
            Log.i(
                TAG,
                "MATCHA_REQUEST_STALE generation=${request.id} " +
                    "currentGeneration=${requestLifecycle.currentGeneration()} stage=$stage"
            )
        }
        return active
    }

    private fun createConfig(): OfflineTtsConfig {
        return OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                matcha = OfflineTtsMatchaModelConfig(
                    acousticModel = File(root, "model-steps-3.onnx").absolutePath,
                    vocoder = File(root, "vocos-22khz-univ.onnx").absolutePath,
                    tokens = File(root, "tokens.txt").absolutePath,
                    lexicon = File(root, "lexicon.txt").absolutePath,
                    dataDir = root.absolutePath,
                    dictDir = File(root, "dict").absolutePath
                ),
                numThreads = 2,
                debug = true,
                provider = "cpu"
            ),
            ruleFsts = listOf("phone.fst", "date.fst", "number.fst")
                .joinToString(",") { File(root, it).absolutePath },
            ruleFars = "",
            maxNumSentences = 1,
            silenceScale = 0.2f
        )
    }

    private fun floatSamplesToPcm16(samples: FloatArray, volume: Float): ByteArray {
        val buffer = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        val safeVolume = volume.coerceIn(0f, 1f)
        samples.forEach { sample ->
            buffer.putShort((sample.coerceIn(-1f, 1f) * safeVolume * 32767f).roundToInt().toShort())
        }
        return buffer.array()
    }

    companion object {
        const val MODEL_RELATIVE_PATH = "bench_models/matcha-icefall-zh-baker"
        private const val DEFAULT_SAMPLE_RATE = 22050
        private const val TAG = "MatchaBookTts"
        private val REQUIRED_FILES = listOf(
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

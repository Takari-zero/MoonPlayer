package com.shenghui.localvibe.core.tts.fastspeech2

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.benjaminwan.chinesettstflite.tts.FastSpeech2
import com.benjaminwan.chinesettstflite.tts.MBMelGan
import com.benjaminwan.chinesettstflite.utils.ZhProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

class FastSpeech2BookTtsEngine(
    private val assetDir: String = FastSpeech2ModelPaths.DEFAULT_ASSET_DIR,
) {
    private val synthMutex = Mutex()
    private var appContext: Context? = null
    private var zhProcessor: ZhProcessor? = null
    private var fastSpeech2: FastSpeech2? = null
    private var melGan: MBMelGan? = null

    @Volatile
    private var initialized = false

    suspend fun initialize(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        if (initialized) return@withContext Result.success(Unit)
        runCatching {
            val startedAt = SystemClock.elapsedRealtime()
            val applicationContext = context.applicationContext
            appContext = applicationContext
            val modelDir = File(applicationContext.filesDir, "fastspeech2/chinese-tts-tflite")
            modelDir.mkdirs()
            val fastSpeechFile = copyAssetIfNeeded(applicationContext, assetDir, FastSpeech2ModelPaths.FASTSPEECH2_MODEL, modelDir)
            val melGanFile = copyAssetIfNeeded(applicationContext, assetDir, FastSpeech2ModelPaths.MB_MELGAN_MODEL, modelDir)
            ensureAssetExists(applicationContext, assetDir, FastSpeech2ModelPaths.BAKER_MAPPER)
            zhProcessor = ZhProcessor(applicationContext, "$assetDir/${FastSpeech2ModelPaths.BAKER_MAPPER}")
            fastSpeech2 = FastSpeech2(fastSpeechFile)
            melGan = MBMelGan(melGanFile)
            initialized = true
            Log.i(TAG, "engine init costMs=${SystemClock.elapsedRealtime() - startedAt} sampleRate=$SAMPLE_RATE")
            Unit
        }.onFailure { error ->
            Log.e(TAG, "FastSpeech2 initialize failed", error)
            release()
        }
    }

    suspend fun synthesizeToPcm(
        text: String,
        sessionId: Long,
        paragraphIndex: Int,
        sentenceIndex: Int,
        chapterIndex: Int?,
        chapterTitle: String?,
    ): Result<FastSpeech2SynthesisResult> = withContext(Dispatchers.IO) {
        runCatching {
            val context = requireNotNull(appContext) { "FastSpeech2BookTtsEngine is not initialized" }
            initialize(context).getOrThrow()
            val processor = requireNotNull(zhProcessor) { "ZhProcessor unavailable" }
            val fastSpeech = requireNotNull(fastSpeech2) { "FastSpeech2 model unavailable" }
            val vocoder = requireNotNull(melGan) { "MB-MelGAN model unavailable" }
            val clauses = FastSpeech2TextSplitter.split(text)
            require(clauses.isNotEmpty()) { "Text is empty" }

            val requestStartMs = SystemClock.elapsedRealtime()
            val allPcm = mutableListOf<ByteArray>()
            val clauseResults = mutableListOf<FastSpeech2SynthesisClause>()

            for ((index, clause) in clauses.withIndex()) {
                val clauseStartedAt = SystemClock.elapsedRealtime()
                val synthesis = synthMutex.withLock {
                    synthesizeClause(processor, fastSpeech, vocoder, clause, index)
                }
                allPcm += synthesis.pcm16
                clauseResults += synthesis.toClauseResult(
                    clauseIndex = index,
                    text = clause,
                    durationMs = pcmDurationMs(synthesis.pcm16.size),
                )
                Log.i(
                    TAG,
                    "clause index=$index sessionId=$sessionId paragraphIndex=$paragraphIndex sentenceIndex=$sentenceIndex " +
                        "chapterIndex=$chapterIndex pcmBytes=${synthesis.pcm16.size} preview=${clause.take(20)} " +
                        "costMs=${SystemClock.elapsedRealtime() - clauseStartedAt}"
                )
            }

            val pcm = ByteArray(allPcm.sumOf { it.size })
            var offset = 0
            allPcm.forEach { bytes ->
                bytes.copyInto(pcm, offset)
                offset += bytes.size
            }
            val fullDoneMs = SystemClock.elapsedRealtime() - requestStartMs
            val durationMs = pcmDurationMs(pcm.size)
            val rtf = if (durationMs > 0) fullDoneMs.toDouble() / durationMs.toDouble() else -1.0
            Log.i(
                TAG,
                "fullPcmDoneCostMs=$fullDoneMs RTF=$rtf totalPcmBytes=${pcm.size} " +
                    "sessionId=$sessionId chapterIndex=$chapterIndex chapterTitle=${chapterTitle.orEmpty()}"
            )
            FastSpeech2SynthesisResult(
                sampleRate = SAMPLE_RATE,
                channelCount = 1,
                pcm16 = pcm,
                clauses = clauseResults,
                durationMs = durationMs,
                rtf = rtf,
            )
        }
    }

    fun release() {
        initialized = false
        zhProcessor = null
        fastSpeech2 = null
        melGan = null
        appContext = null
        Log.i(TAG, "release")
    }

    private fun synthesizeClause(
        processor: ZhProcessor,
        fastSpeech: FastSpeech2,
        vocoder: MBMelGan,
        clause: String,
        clauseIndex: Int,
    ): ClauseSynthesis {
        val preprocessStart = SystemClock.elapsedRealtime()
        val inputIds = processor.text2ids(clause) ?: IntArray(0)
        val preprocessMs = SystemClock.elapsedRealtime() - preprocessStart

        val fastStart = SystemClock.elapsedRealtime()
        val mel: TensorBuffer = fastSpeech.getMelSpectrogram(inputIds, DEFAULT_SPEED)
        val fastMs = SystemClock.elapsedRealtime() - fastStart

        val vocoderStart = SystemClock.elapsedRealtime()
        val audio = vocoder.getAudio(mel)
        val melGanMs = SystemClock.elapsedRealtime() - vocoderStart

        val pcm = floatSamplesToPcm16(audio, DEFAULT_VOLUME)
        val isMainThread = Thread.currentThread().name == "main"
        Log.i(
            TAG,
            "clause index=$clauseIndex preprocess costMs=$preprocessMs FastSpeech2 inference costMs=$fastMs " +
                "MB-MelGAN inference costMs=$melGanMs inference main=$isMainThread samples=${audio.size} pcmBytes=${pcm.size}"
        )
        return ClauseSynthesis(
            pcm16 = pcm,
            preprocessCostMs = preprocessMs,
            fastSpeech2CostMs = fastMs,
            mbMelGanCostMs = melGanMs,
        )
    }

    private fun floatSamplesToPcm16(samples: FloatArray, volume: Float): ByteArray {
        val output = ByteBuffer.allocate(samples.size * BYTES_PER_SAMPLE).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { sample ->
            val scaled = (sample * volume).coerceIn(-1f, 1f)
            output.putShort((scaled * Short.MAX_VALUE).roundToInt().toShort())
        }
        return output.array()
    }

    private fun copyAssetIfNeeded(context: Context, assetDir: String, fileName: String, targetDir: File): File {
        val target = File(targetDir, fileName)
        val assetPath = "$assetDir/$fileName"
        val assetBytes = context.assets.open(assetPath).use { input -> input.readBytes() }
        if (!target.exists() || target.length() != assetBytes.size.toLong()) {
            target.outputStream().use { output -> output.write(assetBytes) }
        }
        return target
    }

    private fun ensureAssetExists(context: Context, assetDir: String, fileName: String) {
        context.assets.open("$assetDir/$fileName").use { /* existence check */ }
    }

    private fun pcmDurationMs(byteCount: Int): Long = byteCount / BYTES_PER_SAMPLE * 1000L / SAMPLE_RATE

    private data class ClauseSynthesis(
        val pcm16: ByteArray,
        val preprocessCostMs: Long,
        val fastSpeech2CostMs: Long,
        val mbMelGanCostMs: Long,
    ) {
        fun toClauseResult(
            clauseIndex: Int,
            text: String,
            durationMs: Long,
        ): FastSpeech2SynthesisClause = FastSpeech2SynthesisClause(
            clauseIndex = clauseIndex,
            text = text,
            pcmByteCount = pcm16.size,
            durationMs = durationMs,
            preprocessCostMs = preprocessCostMs,
            fastSpeech2CostMs = fastSpeech2CostMs,
            mbMelGanCostMs = mbMelGanCostMs,
        )
    }

    companion object {
        const val TAG = "FastSpeech2Boundary"
        const val SAMPLE_RATE = 24000
        private const val BYTES_PER_SAMPLE = 2
        private const val DEFAULT_SPEED = 1.0f
        private const val DEFAULT_VOLUME = 1.0f
    }
}

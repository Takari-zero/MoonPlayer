package com.shenghui.localvibe.core.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class BuiltInOfflineTtsEngine {
    var isReady: Boolean = false
        private set
    var isInitializing: Boolean = false
        private set
    var lastError: String? = null
        private set

    private var tts: OfflineTts? = null
    private var preparedModel: PreparedModel? = null

    @Volatile
    private var stopped: Boolean = true

    suspend fun initialize(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (isReady) return@withContext true
        if (isInitializing) return@withContext false

        isInitializing = true
        lastError = null
        Log.i(TAG, "initialize start")

        try {
            val appContext = context.applicationContext
            val model = prepareModelFiles(appContext)
            preparedModel = model

            val modelPath = File(model.modelDir, MODEL_NAME).absolutePath
            val tokensPath = File(model.modelDir, "tokens.txt").absolutePath
            val lexiconPath = File(model.modelDir, "lexicon.txt").absolutePath
            val dictPath = File(model.modelDir, "dict").absolutePath
            val ruleFsts = RULE_FST_FILES.joinToString(",") { File(model.modelDir, it).absolutePath }

            Log.i(TAG, "using assetManager or filesDir=filesDir")
            Log.i(TAG, "config model path=$modelPath")
            Log.i(TAG, "config tokens path=$tokensPath")
            Log.i(TAG, "config lexicon path=$lexiconPath")
            Log.i(TAG, "config rule fsts=$ruleFsts")
            Log.i(TAG, "config dict/data dir dataDir= dictDir=")
            Log.i(TAG, "dict path=$dictPath")
            Log.i(TAG, "all required files exist result=true")
            Log.i(TAG, "dict file count=${model.dictFileCount}")
            Log.i(TAG, "OfflineTts config summary:")
            Log.i(TAG, "model=$modelPath exists=${File(modelPath).exists()} size=${File(modelPath).length()}")
            Log.i(TAG, "tokens=$tokensPath exists=${File(tokensPath).exists()} size=${File(tokensPath).length()}")
            Log.i(TAG, "lexicon=$lexiconPath exists=${File(lexiconPath).exists()} size=${File(lexiconPath).length()}")
            REQUIRED_FST_FILES.forEach { name ->
                val file = File(model.modelDir, name)
                Log.i(TAG, "$name=${file.absolutePath} exists=${file.exists()} size=${file.length()}")
            }
            Log.i(TAG, "dataDir=")
            Log.i(TAG, "dictDir=")
            Log.i(TAG, "provider=cpu")
            Log.i(TAG, "numThreads=2")
            Log.i(TAG, "debug=true")
            Log.i(TAG, "sid=0")
            Log.i(TAG, "speed=runtime")

            val config = OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    vits = OfflineTtsVitsModelConfig(
                        model = modelPath,
                        lexicon = lexiconPath,
                        tokens = tokensPath,
                        dataDir = "",
                        dictDir = ""
                    ),
                    numThreads = 2,
                    debug = true,
                    provider = "cpu"
                ),
                ruleFsts = ruleFsts,
                ruleFars = "",
                maxNumSentences = 1,
                silenceScale = 0.2f
            )

            tts = OfflineTts(assetManager = null, config = config)
            val sampleRate = tts?.sampleRate() ?: 0
            Log.i(TAG, "OfflineTts init success sampleRate=$sampleRate")
            isReady = true
            true
        } catch (error: Throwable) {
            Log.e(TAG, "initialize failed", error)
            lastError = error.message ?: error::class.java.simpleName
            release()
            false
        } finally {
            isInitializing = false
        }
    }

    suspend fun speak(
        text: String,
        speechRate: BookSpeechRate = BookSpeechRate.fromUserMultiplier(1f),
        onPlaybackStarted: suspend () -> Unit = {}
    ): BuiltInOfflineTtsResult = withContext(Dispatchers.IO) {
        val offlineTts = tts
        if (!isReady || offlineTts == null) {
            return@withContext fail("内置离线语音尚未初始化")
        }
        val ttsText = BookTtsTextNormalizer.normalize(text)
        if (ttsText.spokenText.isBlank()) {
            return@withContext fail("试听文本为空")
        }

        try {
            stopped = false
            val english = synthesize(offlineTts, "hello", "hello", speechRate)
            val chineseShort = synthesize(offlineTts, "你好", "你好", speechRate)
            val chinese = synthesize(offlineTts, "中文试听", ttsText.spokenText, speechRate)
            val selected = when {
                chinese.isUsable -> chinese
                chineseShort.isUsable -> chineseShort
                english.isUsable -> english
                else -> {
                    val reason = buildString {
                        append("内置语音合成失败：")
                        append("hello=${english.samples.size} samples; ")
                        append("你好=${chineseShort.samples.size} samples; ")
                        append("中文试听=${chinese.samples.size} samples; ")
                        append("请检查模型配置")
                    }
                    Log.e(TAG, "final selected audio source=none reason=$reason")
                    return@withContext fail(reason)
                }
            }
            Log.i(TAG, "final selected audio source=${selected.label}")

            val finalSampleRate = if (selected.rawSampleRate > 0) {
                selected.rawSampleRate
            } else {
                Log.w(TAG, "generated sampleRate invalid, fallback to $FALLBACK_SAMPLE_RATE")
                FALLBACK_SAMPLE_RATE
            }
            playFloatSamples(
                samples = selected.samples,
                sampleRate = finalSampleRate,
                rawSampleRate = selected.rawSampleRate,
                source = "built-in-tts-${selected.label}",
                onPlaybackStarted = onPlaybackStarted
            )
        } catch (error: Throwable) {
            Log.e(TAG, "exception stacktrace", error)
            fail("内置语音失败：${error.message ?: error::class.java.simpleName}")
        }
    }

    suspend fun playAudioChannelTest(
        onPlaybackStarted: suspend () -> Unit = {}
    ): BuiltInOfflineTtsResult = withContext(Dispatchers.IO) {
        try {
            stopped = false
            val sampleRate = FALLBACK_SAMPLE_RATE
            val sampleCount = sampleRate
            val samples = FloatArray(sampleCount) { index ->
                (sin(2.0 * PI * 440.0 * index / sampleRate) * 0.35).toFloat()
            }
            Log.i(TAG, "audio test start")
            Log.i(TAG, "audio test sampleRate=$sampleRate")
            Log.i(TAG, "audio test samples size=${samples.size}")
            playFloatSamples(
                samples = samples,
                sampleRate = sampleRate,
                rawSampleRate = sampleRate,
                source = "audio-test",
                onPlaybackStarted = onPlaybackStarted
            )
        } catch (error: Throwable) {
            Log.e(TAG, "exception stacktrace", error)
            fail("测试音播放失败：${error.message ?: error::class.java.simpleName}")
        }
    }

    fun stop() {
        stopped = true
        Log.i(TAG, "stop requested")
    }

    fun release() {
        stop()
        tts?.release()
        tts = null
        isReady = false
        isInitializing = false
    }

    private fun prepareModelFiles(context: Context): PreparedModel {
        val missing = REQUIRED_FILES.filterNot { assetExists(context, "$ASSET_ROOT/$it") }
        val dictFiles = listAssetFiles(context, "$ASSET_ROOT/dict")
        if (dictFiles.isEmpty()) {
            throw IllegalStateException("内置语音模型文件缺失：dict/")
        }
        if (missing.isNotEmpty()) {
            throw IllegalStateException("内置语音模型文件缺失：${missing.joinToString()}")
        }

        val targetRoot = File(context.filesDir, "offline_tts/zh_mvp/vits-melo-tts-zh_en")
        Log.i(TAG, "prepare model files target=${targetRoot.absolutePath}")
        copyAssetTreeIfNeeded(context, ASSET_ROOT, targetRoot)

        val missingCopied = REQUIRED_FILES.filterNot { File(targetRoot, it).isFile }
        if (missingCopied.isNotEmpty()) {
            throw IllegalStateException("内置语音模型复制失败：${missingCopied.joinToString()}")
        }
        val copiedDictCount = File(targetRoot, "dict").walkTopDown().count { it.isFile }
        if (copiedDictCount <= 0) {
            throw IllegalStateException("内置语音模型复制失败：dict 为空")
        }
        Log.i(TAG, "dict file count=$copiedDictCount")
        return PreparedModel(targetRoot, copiedDictCount)
    }

    private fun assetExists(context: Context, path: String): Boolean {
        return try {
            context.assets.open(path).use { true }
        } catch (_: Throwable) {
            false
        }
    }

    private fun listAssetFiles(context: Context, path: String): List<String> {
        val children = context.assets.list(path).orEmpty()
        if (children.isEmpty()) {
            return if (assetExists(context, path)) listOf(path) else emptyList()
        }
        return children.flatMap { child -> listAssetFiles(context, "$path/$child") }
    }

    private fun copyAssetTreeIfNeeded(context: Context, assetPath: String, target: File) {
        val children = context.assets.list(assetPath).orEmpty()
        if (children.isEmpty()) {
            copyAssetFileIfNeeded(context, assetPath, target)
            return
        }
        if (!target.exists()) target.mkdirs()
        children.forEach { child ->
            copyAssetTreeIfNeeded(context, "$assetPath/$child", File(target, child))
        }
    }

    private fun copyAssetFileIfNeeded(context: Context, assetPath: String, target: File) {
        val expectedSize = context.assets.open(assetPath).use { it.available().toLong() }
        if (target.isFile && expectedSize > 0 && target.length() == expectedSize) {
            Log.i(TAG, "asset copy skip path=$assetPath size=${target.length()}")
            return
        }
        target.parentFile?.mkdirs()
        context.assets.open(assetPath).use { input ->
            FileOutputStream(target, false).use { output ->
                input.copyTo(output)
            }
        }
        Log.i(TAG, "asset copied path=$assetPath target=${target.absolutePath} size=${target.length()}")
    }

    private fun synthesize(
        offlineTts: OfflineTts,
        label: String,
        text: String,
        speechRate: BookSpeechRate
    ): SynthesisOutput {
        Log.i(TAG, "synthesize $label start")
        Log.i(TAG, "$label text=$text")
        Log.i(TAG, "$label text length=${text.length}")
        Log.i(TAG, "$label sid=0")
        Log.i(TAG, "$label speed=${speechRate.sherpaGenerateSpeed}")
        val startMs = System.currentTimeMillis()
        val audio = offlineTts.generate(text = text, sid = 0, speed = speechRate.sherpaGenerateSpeed)
        val costMs = System.currentTimeMillis() - startMs
        val samples = audio.samples
        val rawSampleRate = audio.sampleRate
        val stats = sampleStats(samples)
        val durationSeconds = if (rawSampleRate > 0) samples.size.toDouble() / rawSampleRate else 0.0

        Log.i(TAG, "$label synthesize cost ms=$costMs")
        Log.i(TAG, "$label generated audio object null=false")
        Log.i(TAG, "$label samples size=${samples.size}")
        Log.i(TAG, "$label sampleRate raw=$rawSampleRate")
        Log.i(TAG, "$label sampleRate raw value=$rawSampleRate")
        Log.i(TAG, "$label duration seconds=$durationSeconds")
        Log.i(TAG, "$label maxAbsAmplitude=${stats.maxAbsAmplitude}")
        Log.i(TAG, "$label rms=${stats.rms}")

        val failureReason = when {
            samples.isEmpty() -> "未生成音频"
            rawSampleRate <= 0 -> "采样率无效 raw=$rawSampleRate"
            stats.maxAbsAmplitude < SILENCE_THRESHOLD || stats.rms < SILENCE_THRESHOLD -> "合成结果接近静音"
            else -> null
        }
        if (failureReason != null) {
            Log.e(TAG, "$label failure reason=$failureReason")
        }
        return SynthesisOutput(
            label = label,
            samples = samples,
            rawSampleRate = rawSampleRate,
            stats = stats,
            failureReason = failureReason
        )
    }

    private suspend fun playFloatSamples(
        samples: FloatArray,
        sampleRate: Int,
        rawSampleRate: Int,
        source: String,
        onPlaybackStarted: suspend () -> Unit
    ): BuiltInOfflineTtsResult {
        val stats = sampleStats(samples)
        val durationSeconds = if (sampleRate > 0) samples.size.toDouble() / sampleRate else 0.0
        Log.i(TAG, "$source final sampleRate used=$sampleRate")
        Log.i(TAG, "$source samples size=${samples.size}")
        Log.i(TAG, "$source duration seconds=$durationSeconds")
        Log.i(TAG, "$source maxAbsAmplitude=${stats.maxAbsAmplitude}")
        Log.i(TAG, "$source minAbsAmplitude=${stats.minAbsAmplitude}")
        Log.i(TAG, "$source rms=${stats.rms}")

        if (samples.isEmpty()) {
            return fail("${sourceFailurePrefix(source)}：未生成音频")
        }
        if (sampleRate <= 0) {
            return fail("${sourceFailurePrefix(source)}：采样率无效")
        }
        if (stats.maxAbsAmplitude < SILENCE_THRESHOLD || stats.rms < SILENCE_THRESHOLD) {
            return fail("${sourceFailurePrefix(source)}：合成结果接近静音")
        }

        val pcm = floatToPcm16(samples)
        return playPcm16(
            samples = pcm,
            sampleRate = sampleRate,
            rawSampleRate = rawSampleRate,
            durationSeconds = durationSeconds,
            maxAbsAmplitude = stats.maxAbsAmplitude,
            rms = stats.rms,
            source = source,
            onPlaybackStarted = onPlaybackStarted
        )
    }

    private suspend fun playPcm16(
        samples: ShortArray,
        sampleRate: Int,
        rawSampleRate: Int,
        durationSeconds: Double,
        maxAbsAmplitude: Float,
        rms: Float,
        source: String,
        onPlaybackStarted: suspend () -> Unit
    ): BuiltInOfflineTtsResult {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        Log.i(TAG, "$source getMinBufferSize result=$minBufferSize")
        if (minBufferSize <= 0) {
            return fail("${sourceFailurePrefix(source)}：getMinBufferSize=$minBufferSize")
        }

        val bufferSizeBytes = minBufferSize.coerceAtLeast(4096)
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .setSampleRate(sampleRate)
            .build()

        var track: AudioTrack? = null
        return try {
            track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSizeBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setSessionId(AudioManager.AUDIO_SESSION_ID_GENERATE)
                .build()
            Log.i(TAG, "$source AudioTrack state after build=${track.state}")
            if (track.state != AudioTrack.STATE_INITIALIZED) {
                return fail("${sourceFailurePrefix(source)}：AudioTrack 初始化失败 state=${track.state}")
            }

            track.play()
            Log.i(TAG, "$source AudioTrack playState after play=${track.playState}")
            if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                return fail("${sourceFailurePrefix(source)}：AudioTrack 未开始播放 playState=${track.playState}")
            }

            withContext(Dispatchers.Main) { onPlaybackStarted() }

            var totalWritten = 0
            while (!stopped && totalWritten < samples.size) {
                val writeCount = minOf(WRITE_CHUNK_SAMPLES, samples.size - totalWritten)
                val written = track.write(samples, totalWritten, writeCount)
                Log.i(TAG, "$source write result=$written offset=$totalWritten requested=$writeCount")
                if (written <= 0) {
                    return fail("${sourceFailurePrefix(source)}：音频写入失败 write=$written")
                }
                totalWritten += written
            }
            Log.i(TAG, "$source total written=$totalWritten")
            if (totalWritten <= 0) {
                return fail("${sourceFailurePrefix(source)}：音频写入为空")
            }

            waitForPlaybackCompletion(track, totalWritten)
            Log.i(TAG, "$source playback completed")
            lastError = null
            BuiltInOfflineTtsResult.Success(
                sourceLabel = source.removePrefix("built-in-tts-"),
                sampleRate = sampleRate,
                rawSampleRate = rawSampleRate,
                samplesSize = samples.size,
                durationSeconds = durationSeconds,
                maxAbsAmplitude = maxAbsAmplitude,
                rms = rms,
                audioTrackWrite = totalWritten,
                playState = track.playState,
                minBufferSize = minBufferSize
            )
        } finally {
            track?.stopQuietly()
            track?.release()
        }
    }

    private fun waitForPlaybackCompletion(track: AudioTrack, targetFrames: Int) {
        while (!stopped && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
            if (track.playbackHeadPosition >= targetFrames) break
            Thread.sleep(40L)
        }
    }

    private fun floatToPcm16(samples: FloatArray): ShortArray {
        return ShortArray(samples.size) { index ->
            (samples[index].coerceIn(-1f, 1f) * Short.MAX_VALUE).roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
    }

    private fun sourceFailurePrefix(source: String): String {
        return if (source == "audio-test") "测试音播放失败" else "内置语音播放失败"
    }

    private fun fail(message: String): BuiltInOfflineTtsResult.Failure {
        lastError = message
        Log.e(TAG, message)
        return BuiltInOfflineTtsResult.Failure(message)
    }

    private fun sampleStats(samples: FloatArray): SampleStats {
        if (samples.isEmpty()) return SampleStats(0f, 0f, 0f)
        var maxAbs = 0f
        var minAbs = Float.MAX_VALUE
        var squareSum = 0.0
        for (sample in samples) {
            val value = abs(sample)
            if (value > maxAbs) maxAbs = value
            if (value < minAbs) minAbs = value
            squareSum += sample.toDouble() * sample.toDouble()
        }
        return SampleStats(
            maxAbsAmplitude = maxAbs,
            minAbsAmplitude = if (minAbs == Float.MAX_VALUE) 0f else minAbs,
            rms = sqrt(squareSum / samples.size).toFloat()
        )
    }

    private fun AudioTrack.stopQuietly() {
        try {
            if (playState != AudioTrack.PLAYSTATE_STOPPED) stop()
        } catch (error: Throwable) {
            Log.w(TAG, "AudioTrack stop failed", error)
        }
    }

    private data class PreparedModel(
        val modelDir: File,
        val dictFileCount: Int
    )

    private data class SynthesisOutput(
        val label: String,
        val samples: FloatArray,
        val rawSampleRate: Int,
        val stats: SampleStats,
        val failureReason: String?
    ) {
        val isUsable: Boolean
            get() = failureReason == null
    }

    private data class SampleStats(
        val maxAbsAmplitude: Float,
        val minAbsAmplitude: Float,
        val rms: Float
    )

    private companion object {
        const val TAG = "OfflineTts"
        const val ASSET_ROOT = "offline_tts/zh_mvp/vits-melo-tts-zh_en"
        const val MODEL_NAME = "model.int8.onnx"
        const val FALLBACK_SAMPLE_RATE = 44_100
        const val SILENCE_THRESHOLD = 0.001f
        const val WRITE_CHUNK_SAMPLES = 4096

        val REQUIRED_FILES = listOf(
            MODEL_NAME,
            "tokens.txt",
            "lexicon.txt",
            "phone.fst",
            "number.fst",
            "date.fst",
            "new_heteronym.fst"
        )
        val REQUIRED_FST_FILES = listOf(
            "phone.fst",
            "number.fst",
            "date.fst",
            "new_heteronym.fst"
        )
        val RULE_FST_FILES = listOf(
            "phone.fst",
            "date.fst",
            "number.fst"
        )
    }
}

sealed class BuiltInOfflineTtsResult {
    data class Success(
        val sourceLabel: String,
        val sampleRate: Int,
        val rawSampleRate: Int,
        val samplesSize: Int,
        val durationSeconds: Double,
        val maxAbsAmplitude: Float,
        val rms: Float,
        val audioTrackWrite: Int,
        val playState: Int,
        val minBufferSize: Int
    ) : BuiltInOfflineTtsResult()

    data class Failure(val message: String) : BuiltInOfflineTtsResult()
}

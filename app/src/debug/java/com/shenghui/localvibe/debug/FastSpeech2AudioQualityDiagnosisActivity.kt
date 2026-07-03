package com.shenghui.localvibe.debug

import android.app.Activity
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.TextView
import com.benjaminwan.chinesettstflite.tts.FastSpeech2
import com.benjaminwan.chinesettstflite.tts.MBMelGan
import com.benjaminwan.chinesettstflite.utils.ZhProcessor
import com.shenghui.localvibe.core.tts.fastspeech2.FastSpeech2AssetContract
import com.shenghui.localvibe.core.tts.fastspeech2.FastSpeech2AssetVerifier
import com.shenghui.localvibe.core.tts.fastspeech2.FastSpeech2BookTtsEngine
import com.shenghui.localvibe.debug.audio.Pcm16WavWriter
import com.shenghui.localvibe.debug.audio.PcmStats
import com.shenghui.localvibe.feature.book.playback.BookReaderAudioTrackSink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.security.MessageDigest
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

class FastSpeech2AudioQualityDiagnosisActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val engine = FastSpeech2BookTtsEngine(assetDir = FastSpeech2AssetContract.ASSET_DIR)
    private val formalSink = BookReaderAudioTrackSink()
    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        statusView = TextView(this).apply {
            textSize = 16f
            setPadding(32, 32, 32, 32)
            text = "FastSpeech2 audio quality diagnosis starting..."
        }
        setContentView(statusView)
        scope.launch { runDiagnosis() }
    }

    override fun onDestroy() {
        formalSink.release()
        engine.release()
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun runDiagnosis() {
        val phase4fReport = JSONObject()
        val phase4f1Report = JSONObject()
        val phase4f2Report = JSONObject()
        val casesJson = JSONArray()
        val rootCasesJson = JSONArray()
        try {
            Log.i(TAG, "start phase4f1")
            appendStatus("checking assets")
            val assets = FastSpeech2AssetVerifier.checkAssets(this)
            Log.i(TAG, "assets complete=${assets.isComplete} missing=${assets.missingFiles}")
            if (!assets.isComplete) error("missing assets: ${assets.missingFiles}")

            val outputDir = File(requireNotNull(getExternalFilesDir(null)), OUTPUT_DIR_NAME)
            outputDir.mkdirs()
            Log.i(TAG, "outputDir=${outputDir.absolutePath}")

            val initStart = SystemClock.elapsedRealtime()
            engine.initialize(this).getOrThrow()
            Log.i(TAG, "engine init costMs=${SystemClock.elapsedRealtime() - initStart} declaredSampleRate=${FastSpeech2BookTtsEngine.SAMPLE_RATE}")

            val assetInfo = buildAssetInfo(outputDir)
            phase4fReport.put("phase", "4F")
                .put("engineDeclaredSampleRate", FastSpeech2BookTtsEngine.SAMPLE_RATE)
                .put("formalSink", "BookReaderAudioTrackSink")
                .put("pcmFormat", "mono pcm16 little_endian")
                .put("cases", casesJson)

            phase4f1Report.put("phase", "4F.1")
                .put("engineSampleRate", FastSpeech2BookTtsEngine.SAMPLE_RATE)
                .put("modelSampleRateSource", "FastSpeech2BookTtsEngine.SAMPLE_RATE constant")
                .put("mapperPath", FastSpeech2AssetContract.MAPPER_JSON)
                .put("modelFileSizes", assetInfo.sizes)
                .put("modelSha256", assetInfo.sha256)
                .put("byteOrder", "little_endian")
                .put("formalUsesFloat32RawBytesAsPcm16", false)
                .put("rootCauseCandidate", "ZhProcessor must load mapperAssetPath; empty mapper makes ids fall back to unknown=2 and degrades speech intelligibility")
                 .put("cases", rootCasesJson)

            phase4f2Report.put("phase", "4F.2")
                .put("engineSampleRate", FastSpeech2BookTtsEngine.SAMPLE_RATE)
                .put("formalMapperPath", FastSpeech2AssetContract.MAPPER_JSON)
                .put("referenceMapperPath", FastSpeech2AssetContract.MAPPER_JSON)
                .put("formalMapperSha256", assetInfo.sha256.getString(FastSpeech2AssetContract.MAPPER_JSON))
                .put("referenceMapperSha256", assetInfo.sha256.getString(FastSpeech2AssetContract.MAPPER_JSON))
                .put("formalModelSha256", assetInfo.sha256)
                .put("referenceModelSha256", assetInfo.sha256)
                .put("cases", rootCasesJson)

            DIAGNOSIS_CASES.forEachIndexed { index, testCase ->
                val formalCase = runCase(index, testCase, outputDir)
                casesJson.put(formalCase.phase4fJson)
                rootCasesJson.put(formalCase.phase4f1Json)
            }

            val legacyReportFile = File(outputDir, REPORT_FILE_NAME)
            legacyReportFile.writeText(phase4fReport.toString(2), Charsets.UTF_8)
            val rootReportFile = File(outputDir, ROOT_CAUSE_REPORT_FILE_NAME)
            rootReportFile.writeText(phase4f1Report.toString(2), Charsets.UTF_8)
            val mapperValidationReportFile = File(outputDir, MAPPER_VALIDATION_REPORT_FILE_NAME)
            mapperValidationReportFile.writeText(phase4f2Report.toString(2), Charsets.UTF_8)
            Log.i(TAG, "report path=${legacyReportFile.absolutePath}")
            Log.i(TAG, "root cause report path=${rootReportFile.absolutePath}")
            Log.i(TAG, "mapper validation report path=${mapperValidationReportFile.absolutePath}")
            appendStatus("complete report=${mapperValidationReportFile.absolutePath}")
        } catch (error: Throwable) {
            Log.e(TAG, "error type=${error::class.java.simpleName} message=${error.message}", error)
            appendStatus("error: ${error.message}")
            phase4f1Report.put("error", error.message ?: error::class.java.simpleName)
            phase4f2Report.put("error", error.message ?: error::class.java.simpleName)
        }
    }

    private suspend fun runCase(
        index: Int,
        testCase: DiagnosisCase,
        outputDir: File,
    ): CaseReports {
        appendStatus("synth ${testCase.name}")
        Log.i(TAG, "case start name=${testCase.name} text=${testCase.text} textLength=${testCase.text.length}")
        val synthStart = SystemClock.elapsedRealtime()
        val result = engine.synthesizeToPcm(
            text = testCase.text,
            sessionId = 4000L + index,
            paragraphIndex = index,
            sentenceIndex = 0,
            chapterIndex = 0,
            chapterTitle = "Phase4F",
        ).getOrThrow()
        val synthCostMs = SystemClock.elapsedRealtime() - synthStart
        Log.i(
            TAG,
            "synth done name=${testCase.name} costMs=$synthCostMs sampleRate=${result.sampleRate} " +
                "pcmBytes=${result.pcm16.size} durationMs=${result.durationMs} rtf=${result.rtf} clauses=${result.clauses.size}"
        )

        val phase4fJson = JSONObject()
            .put("name", testCase.name)
            .put("text", testCase.text)
            .put("textLength", testCase.text.length)
            .put("synthCostMs", synthCostMs)
            .put("engineSampleRate", result.sampleRate)
            .put("engineDurationMs", result.durationMs)
            .put("rtf", result.rtf)
            .put("clauses", JSONArray(result.clauses.map { clause ->
                JSONObject()
                    .put("clauseIndex", clause.clauseIndex)
                    .put("text", clause.text)
                    .put("pcmByteCount", clause.pcmByteCount)
                    .put("durationMs", clause.durationMs)
            }))

        val statsArray = JSONArray()
        val filesArray = JSONArray()
        SAMPLE_RATE_CANDIDATES.forEach { candidate ->
            val rate = if (candidate == ENGINE_RATE_SENTINEL) result.sampleRate else candidate
            val suffix = if (candidate == ENGINE_RATE_SENTINEL) "engine_rate" else candidate.toString()
            val file = File(outputDir, "${testCase.name}_${suffix}.wav")
            Pcm16WavWriter.writeMono16(file, rate, result.pcm16)
            val stats = PcmStats.calculate("${testCase.name}_$suffix", testCase.text, rate, result.pcm16)
            Log.i(
                TAG,
                "stats name=${stats.name} sampleRate=${stats.sampleRateCandidate} pcmBytes=${stats.pcmBytes} " +
                    "durationMs=${stats.durationMs} minSample=${stats.minSample} maxSample=${stats.maxSample} " +
                    "peakAbs=${stats.peakAbs} rms=${stats.rms} rmsDb=${stats.rmsDb} zeroRatio=${stats.zeroRatio} " +
                    "clippingRatio=${stats.clippingRatio} leadingSilenceMs=${stats.leadingSilenceMs} trailingSilenceMs=${stats.trailingSilenceMs}"
            )
            statsArray.put(stats.toJson().put("wavPath", file.absolutePath))
            filesArray.put(file.absolutePath)
        }
        phase4fJson.put("stats", statsArray)
        phase4fJson.put("wavFiles", filesArray)

        val rootCaseJson = runRootCauseCase(index, testCase, result.pcm16, result.sampleRate, outputDir)

        if (index == 0) {
            withContext(Dispatchers.IO) {
                Log.i(TAG, "formal sink playback start name=${testCase.name} sampleRate=${result.sampleRate} pcmBytes=${result.pcm16.size}")
                formalSink.playPcm(5000L + index, result.sampleRate, result.pcm16)
                Log.i(TAG, "formal sink playback done name=${testCase.name}")
            }

            withContext(Dispatchers.IO) {
                Log.i(TAG, "reference playback start sampleRate=${result.sampleRate} name=${testCase.name}")
                playReferencePcm(result.sampleRate, result.pcm16)
                Log.i(TAG, "reference playback done sampleRate=${result.sampleRate} name=${testCase.name}")
            }
        }

        appendStatus("done ${testCase.name}: wav=${filesArray.length()}")
        return CaseReports(phase4fJson, rootCaseJson)
    }

    private suspend fun runRootCauseCase(
        index: Int,
        testCase: DiagnosisCase,
        formalPcm16: ByteArray,
        sampleRate: Int,
        outputDir: File,
    ): JSONObject = withContext(Dispatchers.IO) {
        val direct = synthesizeDirect(testCase.text)
        val formal = synthesizeFormalFrontend(testCase.text)
        val idSequenceEqual = formal.ids.contentEquals(direct.ids)
        val caseJson = JSONObject()
            .put("name", testCase.name)
            .put("text", testCase.text)
            .put("normalizedText", direct.normalizedText)
            .put("pinyinSequencePreview", direct.pinyinPreview)
            .put("idSequenceLength", direct.ids.size)
            .put("idSequencePreview", JSONArray(direct.ids.take(ID_PREVIEW_COUNT)))
            .put("melShape", JSONArray(direct.melShape.toList()))
            .put("vocoderOutputShape", JSONArray(listOf(direct.floatAudio.size)))
            .put("vocoderOutputType", "FloatArray/FLOAT32")
            .put("floatMin", jsonNumber(direct.floatStats.min))
            .put("floatMax", jsonNumber(direct.floatStats.max))
            .put("floatRms", jsonNumber(direct.floatStats.rms))
            .put("floatNaNCount", direct.floatStats.nanCount)
            .put("floatInfCount", direct.floatStats.infCount)
            .put("byteOrder", "little_endian")
            .put("formalMapperPath", FastSpeech2AssetContract.MAPPER_JSON)
            .put("referenceMapperPath", FastSpeech2AssetContract.MAPPER_JSON)
            .put("formalNormalizedText", formal.normalizedText)
            .put("referenceNormalizedText", direct.normalizedText)
            .put("formalIdSequenceLength", formal.ids.size)
            .put("referenceIdSequenceLength", direct.ids.size)
            .put("formalIdSequencePreview", JSONArray(formal.ids.take(ID_PREVIEW_COUNT)))
            .put("referenceIdSequencePreview", JSONArray(direct.ids.take(ID_PREVIEW_COUNT)))
            .put("idSequenceEqual", idSequenceEqual)
            .put("formalMelShape", JSONArray(formal.melShapes.map { JSONArray(it.toList()) }))
            .put("referenceMelShape", JSONArray(direct.melShape.toList()))
            .put("formalVocoderOutputShape", JSONArray(formal.vocoderOutputShapes.map { JSONArray(it.toList()) }))
            .put("referenceVocoderOutputShape", JSONArray(listOf(direct.floatAudio.size)))
            .put("formalPcmStats", PcmStats.calculate("after_mapper_formal_${testCase.name}", testCase.text, sampleRate, formalPcm16).toJson())
            .put("referencePcmStats", PcmStats.calculate("after_mapper_reference_${testCase.name}", testCase.text, sampleRate, direct.referencePcm16).toJson())
            .put("formalClauses", JSONArray(direct.formalClauses))
            .put("formalCurrentPcm16Stats", PcmStats.calculate("formal_${testCase.name}", testCase.text, sampleRate, formalPcm16).toJson())
            .put("referencePcm16Stats", PcmStats.calculate("reference_${testCase.name}", testCase.text, sampleRate, direct.referencePcm16).toJson())
            .put("rawFloatBytesAsPcm16Stats", PcmStats.calculate("raw_float_${testCase.name}", testCase.text, sampleRate, direct.rawFloatBytes).toJson())

        val formalAbFile = File(outputDir, "ab_formal_${testCase.name}.wav")
        val referenceAbFile = File(outputDir, "ab_reference_${testCase.name}.wav")
        val afterMapperFormalFile = File(outputDir, "after_mapper_formal_${testCase.name}.wav")
        val afterMapperReferenceFile = File(outputDir, "after_mapper_reference_${testCase.name}.wav")
        Pcm16WavWriter.writeMono16(formalAbFile, sampleRate, formalPcm16)
        Pcm16WavWriter.writeMono16(referenceAbFile, sampleRate, direct.referencePcm16)
        Pcm16WavWriter.writeMono16(afterMapperFormalFile, sampleRate, formalPcm16)
        Pcm16WavWriter.writeMono16(afterMapperReferenceFile, sampleRate, direct.referencePcm16)
        caseJson.put("abFormalWav", formalAbFile.absolutePath)
        caseJson.put("abReferenceWav", referenceAbFile.absolutePath)
        caseJson.put("afterMapperFormalWav", afterMapperFormalFile.absolutePath)
        caseJson.put("afterMapperReferenceWav", afterMapperReferenceFile.absolutePath)

        if (index == 0) {
            val formalCurrentFile = File(outputDir, "formal_current_pcm16.wav")
            val floatConvertedFile = File(outputDir, "formal_float_converted_pcm16.wav")
            val rawFloatFile = File(outputDir, "formal_raw_bytes_interpreted_as_pcm16.wav")
            Pcm16WavWriter.writeMono16(formalCurrentFile, sampleRate, formalPcm16)
            Pcm16WavWriter.writeMono16(floatConvertedFile, sampleRate, direct.referencePcm16)
            Pcm16WavWriter.writeMono16(rawFloatFile, sampleRate, direct.rawFloatBytes)
            caseJson.put("formalCurrentPcm16Wav", formalCurrentFile.absolutePath)
            caseJson.put("formalFloatConvertedPcm16Wav", floatConvertedFile.absolutePath)
            caseJson.put("formalRawBytesInterpretedAsPcm16Wav", rawFloatFile.absolutePath)
        }

        Log.i(
            TAG,
            "after_mapper name=${testCase.name} mapperPath=${FastSpeech2AssetContract.MAPPER_JSON} " +
                "formalNormalizedText=${formal.normalizedText} referenceNormalizedText=${direct.normalizedText} " +
                "formalIdSequenceLength=${formal.ids.size} referenceIdSequenceLength=${direct.ids.size} " +
                "idSequenceEqual=$idSequenceEqual formalMelShape=${formal.melShapes.map { it.contentToString() }} " +
                "referenceMelShape=${direct.melShape.contentToString()} formalVocoderOutputShape=${formal.vocoderOutputShapes.map { it.contentToString() }} " +
                "referenceVocoderOutputShape=[${direct.floatAudio.size}] pcm16RmsDb=${caseJson.getJSONObject("referencePcm16Stats").getDouble("rmsDb")}"
        )
        caseJson
    }
    private fun synthesizeFormalFrontend(text: String): FormalFrontendResult {
        val clauses = com.shenghui.localvibe.core.tts.fastspeech2.FastSpeech2TextSplitter.split(text)
        val modelDir = File(filesDir, "fastspeech2/phase4f2-formal-frontend").apply { mkdirs() }
        val fastSpeechFile = copyAssetToFile(FastSpeech2AssetContract.FASTSPEECH2_MODEL, modelDir)
        val melGanFile = copyAssetToFile(FastSpeech2AssetContract.MB_MELGAN_MODEL, modelDir)
        val processor = ZhProcessor(applicationContext, FastSpeech2AssetContract.MAPPER_JSON)
        val fastSpeech = FastSpeech2(fastSpeechFile)
        val melGan = MBMelGan(melGanFile)
        val ids = mutableListOf<Int>()
        val normalized = mutableListOf<String>()
        val melShapes = mutableListOf<IntArray>()
        val vocoderShapes = mutableListOf<IntArray>()
        clauses.forEach { clause ->
            normalized += reflectNormalizedText(processor, clause) ?: clause
            val clauseIds = processor.text2ids(clause) ?: IntArray(0)
            ids += clauseIds.toList()
            val mel = fastSpeech.getMelSpectrogram(clauseIds, 1.0f)
            melShapes += mel.shape
            val audio = melGan.getAudio(mel)
            vocoderShapes += intArrayOf(audio.size)
        }
        return FormalFrontendResult(
            normalizedText = normalized.joinToString("|"),
            ids = ids.toIntArray(),
            melShapes = melShapes,
            vocoderOutputShapes = vocoderShapes,
        )
    }
    private fun synthesizeDirect(text: String): DirectSynthesisResult {
        val modelDir = File(filesDir, "fastspeech2/phase4f1-direct").apply { mkdirs() }
        val fastSpeechFile = copyAssetToFile(FastSpeech2AssetContract.FASTSPEECH2_MODEL, modelDir)
        val melGanFile = copyAssetToFile(FastSpeech2AssetContract.MB_MELGAN_MODEL, modelDir)
        val processor = ZhProcessor(applicationContext, FastSpeech2AssetContract.MAPPER_JSON)
        val ids = processor.text2ids(text) ?: IntArray(0)
        val fastSpeech = FastSpeech2(fastSpeechFile)
        val melGan = MBMelGan(melGanFile)
        val mel: TensorBuffer = fastSpeech.getMelSpectrogram(ids, 1.0f)
        val audio = melGan.getAudio(mel)
        val pcm16 = floatSamplesToPcm16(audio)
        return DirectSynthesisResult(
            normalizedText = reflectNormalizedText(processor, text) ?: text,
            pinyinPreview = reflectPinyinPreview(reflectNormalizedText(processor, text) ?: text).orEmpty(),
            ids = ids,
            melShape = mel.shape,
            floatAudio = audio,
            floatStats = FloatStats.calculate(audio),
            referencePcm16 = pcm16,
            rawFloatBytes = floatSamplesToRawBytes(audio),
            formalClauses = com.shenghui.localvibe.core.tts.fastspeech2.FastSpeech2TextSplitter.split(text),
        )
    }

    private fun copyAssetToFile(assetPath: String, targetDir: File): File {
        val target = File(targetDir, File(assetPath).name)
        assets.open(assetPath).use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        return target
    }

    private fun buildAssetInfo(outputDir: File): AssetInfo {
        val sizes = JSONObject()
        val sha256 = JSONObject()
        FastSpeech2AssetContract.REQUIRED_FILES.forEach { path ->
            val bytes = assets.open(path).use { it.readBytes() }
            sizes.put(path, bytes.size)
            sha256.put(path, sha256Hex(bytes))
        }
        val assetInfoFile = File(outputDir, "phase4f1_asset_hashes.json")
        assetInfoFile.writeText(JSONObject().put("sizes", sizes).put("sha256", sha256).toString(2), Charsets.UTF_8)
        Log.i(TAG, "modelSha256=$sha256")
        return AssetInfo(sizes, sha256)
    }

    private suspend fun playReferencePcm(sampleRate: Int, pcm16: ByteArray) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufferSize = max(minBufferSize, sampleRate / 2)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        try {
            track.play()
            var offset = 0
            while (offset < pcm16.size) {
                val size = min(REFERENCE_WRITE_CHUNK_BYTES, pcm16.size - offset)
                val buffer = ByteBuffer.allocateDirect(size)
                buffer.put(pcm16, offset, size)
                buffer.flip()
                val written = track.write(buffer, size, AudioTrack.WRITE_BLOCKING)
                Log.i(TAG, "reference write result sampleRate=$sampleRate offset=$offset bytes=$size written=$written")
                if (written <= 0) break
                offset += written
            }
            val durationMs = pcm16.size / 2L * 1000L / sampleRate
            delay(durationMs.coerceAtLeast(1L))
        } finally {
            runCatching {
                track.pause()
                track.flush()
                track.release()
            }
        }
    }

    private fun floatSamplesToPcm16(samples: FloatArray): ByteArray {
        val output = ByteBuffer.allocate(samples.size * BYTES_PER_SAMPLE).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { sample ->
            val clipped = sample.coerceIn(-1f, 1f)
            output.putShort((clipped * Short.MAX_VALUE).roundToInt().toShort())
        }
        return output.array()
    }

    private fun floatSamplesToRawBytes(samples: FloatArray): ByteArray {
        val output = ByteBuffer.allocate(samples.size * Float.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { output.putFloat(it) }
        return output.array()
    }

    private fun reflectNormalizedText(processor: ZhProcessor, text: String): String? = runCatching {
        val method = ZhProcessor::class.java.getDeclaredMethod("parseText", String::class.java)
        method.isAccessible = true
        method.invoke(processor, text) as String
    }.getOrNull()

    private fun reflectPinyinPreview(normalizedText: String): String? = runCatching {
        val method = ZhProcessor::class.java.getDeclaredMethod("convert2Pinyin", String::class.java)
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val pinyin = method.invoke(null, normalizedText) as Array<String>
        pinyin.take(PINYIN_PREVIEW_COUNT).joinToString(" ")
    }.getOrNull()

    private suspend fun appendStatus(line: String) {
        withContext(Dispatchers.Main.immediate) {
            if (!isDestroyed) statusView.append("\n$line")
        }
    }

    private data class DiagnosisCase(
        val name: String,
        val text: String,
    )

    private data class CaseReports(
        val phase4fJson: JSONObject,
        val phase4f1Json: JSONObject,
    )

    private data class FormalFrontendResult(
        val normalizedText: String,
        val ids: IntArray,
        val melShapes: List<IntArray>,
        val vocoderOutputShapes: List<IntArray>,
    )

    private data class DirectSynthesisResult(
        val normalizedText: String,
        val pinyinPreview: String,
        val ids: IntArray,
        val melShape: IntArray,
        val floatAudio: FloatArray,
        val floatStats: FloatStats,
        val referencePcm16: ByteArray,
        val rawFloatBytes: ByteArray,
        val formalClauses: List<String>,
    )

    private data class AssetInfo(
        val sizes: JSONObject,
        val sha256: JSONObject,
    )

    private data class FloatStats(
        val min: Double,
        val max: Double,
        val rms: Double,
        val nanCount: Int,
        val infCount: Int,
    ) {
        companion object {
            fun calculate(samples: FloatArray): FloatStats {
                var minValue = Double.POSITIVE_INFINITY
                var maxValue = Double.NEGATIVE_INFINITY
                var sumSquares = 0.0
                var finiteCount = 0
                var nanCount = 0
                var infCount = 0
                samples.forEach { sample ->
                    val value = sample.toDouble()
                    when {
                        value.isNaN() -> nanCount += 1
                        value.isInfinite() -> infCount += 1
                        else -> {
                            minValue = minOf(minValue, value)
                            maxValue = maxOf(maxValue, value)
                            sumSquares += value * value
                            finiteCount += 1
                        }
                    }
                }
                val rms = if (finiteCount > 0) sqrt(sumSquares / finiteCount.toDouble()) else 0.0
                return FloatStats(
                    min = if (finiteCount > 0) minValue else 0.0,
                    max = if (finiteCount > 0) maxValue else 0.0,
                    rms = rms,
                    nanCount = nanCount,
                    infCount = infCount,
                )
            }
        }
    }

    companion object {
        private const val TAG = "LV_BOOK_AUDIO_QUALITY"
        private const val OUTPUT_DIR_NAME = "tts_quality"
        private const val REPORT_FILE_NAME = "phase4f_audio_quality_report.json"
        private const val ROOT_CAUSE_REPORT_FILE_NAME = "phase4f1_audio_root_cause_report.json"
        private const val MAPPER_VALIDATION_REPORT_FILE_NAME = "phase4f2_mapper_fix_validation_report.json"
        private const val ENGINE_RATE_SENTINEL = -1
        private const val REFERENCE_WRITE_CHUNK_BYTES = 4096
        private const val BYTES_PER_SAMPLE = 2
        private const val ID_PREVIEW_COUNT = 32
        private const val PINYIN_PREVIEW_COUNT = 32
        private val SAMPLE_RATE_CANDIDATES = listOf(ENGINE_RATE_SENTINEL, 22050, 24000)
        private val DIAGNOSIS_CASES = listOf(
            DiagnosisCase("baseline_01", "\u8fd9\u662f\u4e00\u6bb5\u79bb\u7ebf\u8bed\u97f3\u8bd5\u542c\u3002"),
            DiagnosisCase("baseline_02", "\u8bb8\u9752\u89c2\u5bdf\u4e4b\u4e0b\uff0c\u5fc3\u5e95\u6709\u4e86\u5224\u65ad\u3002"),
            DiagnosisCase("baseline_03", "\u4ed6\u4eec\u4e0d\u662f\u4fee\u58eb\uff0c\u4f46\u51fa\u624b\u7684\u72e0\u8fa3\u4e0e\u65f6\u673a\u7684\u628a\u63e1\uff0c\u8fd8\u6709\u90a3\u5173\u952e\u65f6\u523b\u4f3c\u4e0d\u5728\u4e4e\u6b7b\u4ea1\u7684\u6001\u5ea6\uff0c\u4f7f\u4ed6\u5fc3\u5e95\u6709\u4e86\u5224\u65ad\u3002"),
        )

        private fun sha256Hex(bytes: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            return digest.joinToString("") { "%02x".format(it) }
        }

        private fun jsonNumber(value: Double): Any = if (value.isFinite()) value else JSONObject.NULL

        @Suppress("unused")
        private fun dbFromFloatRms(rms: Double): Double = if (rms > 0.0) 20.0 * log10(rms) else -120.0
    }
}






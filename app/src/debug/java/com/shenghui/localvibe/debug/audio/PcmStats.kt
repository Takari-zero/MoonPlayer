package com.shenghui.localvibe.debug.audio

import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

data class PcmStats(
    val name: String,
    val text: String,
    val textLength: Int,
    val sampleRateCandidate: Int,
    val pcmBytes: Int,
    val frameCount: Int,
    val durationMs: Long,
    val minSample: Int,
    val maxSample: Int,
    val peakAbs: Int,
    val rms: Double,
    val rmsDb: Double,
    val meanAbs: Double,
    val zeroRatio: Double,
    val clippingRatio: Double,
    val firstNonZeroFrame: Int,
    val lastNonZeroFrame: Int,
    val leadingSilenceMs: Long,
    val trailingSilenceMs: Long,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("name", name)
        .put("text", text)
        .put("textLength", textLength)
        .put("sampleRateCandidate", sampleRateCandidate)
        .put("pcmBytes", pcmBytes)
        .put("frameCount", frameCount)
        .put("durationMs", durationMs)
        .put("minSample", minSample)
        .put("maxSample", maxSample)
        .put("peakAbs", peakAbs)
        .put("rms", rms)
        .put("rmsDb", rmsDb)
        .put("meanAbs", meanAbs)
        .put("zeroRatio", zeroRatio)
        .put("clippingRatio", clippingRatio)
        .put("firstNonZeroFrame", firstNonZeroFrame)
        .put("lastNonZeroFrame", lastNonZeroFrame)
        .put("leadingSilenceMs", leadingSilenceMs)
        .put("trailingSilenceMs", trailingSilenceMs)

    companion object {
        fun calculate(name: String, text: String, sampleRateCandidate: Int, pcm16: ByteArray): PcmStats {
            val frames = pcm16.size / BYTES_PER_SAMPLE
            if (frames <= 0 || sampleRateCandidate <= 0) {
                return PcmStats(
                    name = name,
                    text = text,
                    textLength = text.length,
                    sampleRateCandidate = sampleRateCandidate,
                    pcmBytes = pcm16.size,
                    frameCount = frames,
                    durationMs = 0,
                    minSample = 0,
                    maxSample = 0,
                    peakAbs = 0,
                    rms = 0.0,
                    rmsDb = MIN_DB,
                    meanAbs = 0.0,
                    zeroRatio = 1.0,
                    clippingRatio = 0.0,
                    firstNonZeroFrame = -1,
                    lastNonZeroFrame = -1,
                    leadingSilenceMs = 0,
                    trailingSilenceMs = 0,
                )
            }

            var minSample = Int.MAX_VALUE
            var maxSample = Int.MIN_VALUE
            var peakAbs = 0
            var zeroCount = 0
            var clippingCount = 0
            var firstNonZero = -1
            var lastNonZero = -1
            var sumAbs = 0.0
            var sumSquares = 0.0

            for (frame in 0 until frames) {
                val sample = readLittleEndianSample(pcm16, frame * BYTES_PER_SAMPLE)
                minSample = minOf(minSample, sample)
                maxSample = maxOf(maxSample, sample)
                val absSample = abs(sample)
                peakAbs = maxOf(peakAbs, absSample)
                sumAbs += absSample.toDouble()
                sumSquares += sample.toDouble() * sample.toDouble()
                if (sample == 0) {
                    zeroCount += 1
                } else {
                    if (firstNonZero < 0) firstNonZero = frame
                    lastNonZero = frame
                }
                if (absSample >= Short.MAX_VALUE) {
                    clippingCount += 1
                }
            }

            val rms = sqrt(sumSquares / frames.toDouble())
            val rmsDb = if (rms > 0.0) 20.0 * log10(rms / Short.MAX_VALUE.toDouble()) else MIN_DB
            val durationMs = frames * 1000L / sampleRateCandidate
            val leadingSilenceMs = if (firstNonZero >= 0) firstNonZero * 1000L / sampleRateCandidate else durationMs
            val trailingSilenceMs = if (lastNonZero >= 0) (frames - lastNonZero - 1) * 1000L / sampleRateCandidate else durationMs

            return PcmStats(
                name = name,
                text = text,
                textLength = text.length,
                sampleRateCandidate = sampleRateCandidate,
                pcmBytes = pcm16.size,
                frameCount = frames,
                durationMs = durationMs,
                minSample = minSample,
                maxSample = maxSample,
                peakAbs = peakAbs,
                rms = rms,
                rmsDb = rmsDb,
                meanAbs = sumAbs / frames.toDouble(),
                zeroRatio = zeroCount.toDouble() / frames.toDouble(),
                clippingRatio = clippingCount.toDouble() / frames.toDouble(),
                firstNonZeroFrame = firstNonZero,
                lastNonZeroFrame = lastNonZero,
                leadingSilenceMs = leadingSilenceMs,
                trailingSilenceMs = trailingSilenceMs,
            )
        }

        private fun readLittleEndianSample(bytes: ByteArray, offset: Int): Int {
            val low = bytes[offset].toInt() and 0xFF
            val high = bytes[offset + 1].toInt()
            return ((high shl 8) or low).toShort().toInt()
        }

        private const val BYTES_PER_SAMPLE = 2
        private const val MIN_DB = -120.0
    }
}

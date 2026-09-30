package com.shenghui.localvibe.core.tts

import java.security.MessageDigest
import kotlin.math.abs
import kotlin.math.sqrt

data class TtsAudioQualityMetrics(
    val sampleCount: Int,
    val durationMs: Long,
    val peakAbs: Float,
    val rms: Float,
    val clippedSampleCount: Int,
    val clippedRatio: Float,
    val fingerprint: String
) {
    companion object {
        fun from(samples: FloatArray, sampleRate: Int, pcmBytes: ByteArray): TtsAudioQualityMetrics {
            if (samples.isEmpty() || sampleRate <= 0) {
                return TtsAudioQualityMetrics(
                    sampleCount = samples.size,
                    durationMs = 0L,
                    peakAbs = 0f,
                    rms = 0f,
                    clippedSampleCount = 0,
                    clippedRatio = 0f,
                    fingerprint = fingerprint(pcmBytes)
                )
            }

            var peak = 0f
            var sumSquares = 0.0
            var clipped = 0
            samples.forEach { sample ->
                val absolute = abs(sample)
                if (absolute > peak) peak = absolute
                if (absolute >= 0.999f) clipped++
                sumSquares += sample.toDouble() * sample.toDouble()
            }
            return TtsAudioQualityMetrics(
                sampleCount = samples.size,
                durationMs = samples.size * 1000L / sampleRate,
                peakAbs = peak,
                rms = sqrt(sumSquares / samples.size).toFloat(),
                clippedSampleCount = clipped,
                clippedRatio = clipped.toFloat() / samples.size,
                fingerprint = fingerprint(pcmBytes)
            )
        }

        fun fingerprint(bytes: ByteArray): String {
            return MessageDigest.getInstance("SHA-256")
                .digest(bytes)
                .joinToString("") { "%02x".format(it) }
                .take(12)
        }
    }
}

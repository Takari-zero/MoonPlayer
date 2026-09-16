package com.shenghui.localvibe.core.tts

import kotlin.math.roundToLong

data class TtsSynthesisMetrics(
    val textLength: Int,
    val synthesisRequestStartElapsedMs: Long,
    val synthesisStartElapsedMs: Long?,
    val firstPcmElapsedMs: Long?,
    val generationCompletedElapsedMs: Long?,
    val generatedSamples: Long,
    val sampleRate: Int
) {
    val requestToSynthesisMs: Long?
        get() = elapsedDelta(synthesisRequestStartElapsedMs, synthesisStartElapsedMs)

    val firstPcmLatencyMs: Long?
        get() = elapsedDelta(synthesisRequestStartElapsedMs, firstPcmElapsedMs)

    val synthesisDurationMs: Long?
        get() = elapsedDelta(synthesisStartElapsedMs, generationCompletedElapsedMs)

    val generatedAudioDurationMs: Long?
        get() {
            if (generatedSamples <= 0L || sampleRate <= 0) return null
            return (generatedSamples.toDouble() * 1000.0 / sampleRate.toDouble()).roundToLong()
        }

    val realTimeFactor: Double?
        get() {
            val synthesisMs = synthesisDurationMs ?: return null
            val audioMs = generatedAudioDurationMs ?: return null
            if (synthesisMs < 0L || audioMs <= 0L) return null
            return synthesisMs.toDouble() / audioMs.toDouble()
        }

    private fun elapsedDelta(startMs: Long?, endMs: Long?): Long? {
        if (startMs == null || endMs == null || endMs < startMs) return null
        return endMs - startMs
    }
}

class TtsFirstEventLatch {
    private var firstElapsedMs: Long? = null

    fun record(elapsedMs: Long): Long? {
        if (elapsedMs >= 0L && firstElapsedMs == null) {
            firstElapsedMs = elapsedMs
        }
        return firstElapsedMs
    }

    fun value(): Long? = firstElapsedMs
}

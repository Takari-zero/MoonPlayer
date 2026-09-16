package com.shenghui.localvibe.core.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TtsSynthesisMetricsTest {
    @Test
    fun calculatesAudioDurationFromSamplesAndSampleRate() {
        val metrics = metrics(
            synthesisStart = 100L,
            firstPcm = 250L,
            generationCompleted = 600L,
            generatedSamples = 16_000L,
            sampleRate = 16_000
        )

        assertEquals(1_000L, metrics.generatedAudioDurationMs)
    }

    @Test
    fun calculatesRtfAsSynthesisTimeOverGeneratedAudioTime() {
        val metrics = metrics(
            synthesisStart = 100L,
            firstPcm = 250L,
            generationCompleted = 600L,
            generatedSamples = 20_000L,
            sampleRate = 20_000
        )

        assertEquals(0.5, metrics.realTimeFactor ?: -1.0, 0.0001)
        assertEquals(250L, metrics.firstPcmLatencyMs)
        assertEquals(500L, metrics.synthesisDurationMs)
    }

    @Test
    fun rejectsEmptyAudioInvalidSampleRateAndNegativeTiming() {
        assertNull(metrics(generatedSamples = 0L, sampleRate = 16_000).generatedAudioDurationMs)
        assertNull(metrics(generatedSamples = 16_000L, sampleRate = 0).generatedAudioDurationMs)
        assertNull(
            metrics(
                synthesisStart = 500L,
                generationCompleted = 100L,
                generatedSamples = 16_000L,
                sampleRate = 16_000
            ).realTimeFactor
        )
    }

    @Test
    fun firstEventLatchKeepsOnlyTheFirstValidEvent() {
        val latch = TtsFirstEventLatch()

        assertNull(latch.record(-1L))
        assertEquals(320L, latch.record(320L))
        assertEquals(320L, latch.record(500L))
        assertEquals(320L, latch.value())
    }

    private fun metrics(
        synthesisStart: Long? = 100L,
        firstPcm: Long? = 250L,
        generationCompleted: Long? = 600L,
        generatedSamples: Long = 16_000L,
        sampleRate: Int = 16_000
    ): TtsSynthesisMetrics {
        return TtsSynthesisMetrics(
            textLength = 20,
            synthesisRequestStartElapsedMs = 0L,
            synthesisStartElapsedMs = synthesisStart,
            firstPcmElapsedMs = firstPcm,
            generationCompletedElapsedMs = generationCompleted,
            generatedSamples = generatedSamples,
            sampleRate = sampleRate
        )
    }
}

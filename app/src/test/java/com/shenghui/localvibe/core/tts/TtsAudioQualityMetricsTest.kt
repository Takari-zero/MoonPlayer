package com.shenghui.localvibe.core.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsAudioQualityMetricsTest {
    @Test
    fun calculatesPeakRmsClippingAndDuration() {
        val metrics = TtsAudioQualityMetrics.from(
            samples = floatArrayOf(0f, 0.5f, -1f, 0.25f),
            sampleRate = 2,
            pcmBytes = byteArrayOf(1, 2)
        )

        assertEquals(4, metrics.sampleCount)
        assertEquals(2_000L, metrics.durationMs)
        assertEquals(1f, metrics.peakAbs, 0f)
        assertEquals(1, metrics.clippedSampleCount)
        assertEquals(0.25f, metrics.clippedRatio, 0f)
        assertTrue(metrics.rms > 0.5f)
    }

    @Test
    fun fingerprintsDifferentPcm() {
        val first = TtsAudioQualityMetrics.fingerprint(byteArrayOf(1, 2, 3))
        val second = TtsAudioQualityMetrics.fingerprint(byteArrayOf(1, 2, 4))

        assertNotEquals(first, second)
    }

    @Test
    fun emptySamplesAreSafe() {
        val metrics = TtsAudioQualityMetrics.from(FloatArray(0), 8_000, ByteArray(0))

        assertEquals(0, metrics.sampleCount)
        assertEquals(0L, metrics.durationMs)
        assertEquals(0f, metrics.rms, 0f)
        assertEquals(0, metrics.clippedSampleCount)
    }
}

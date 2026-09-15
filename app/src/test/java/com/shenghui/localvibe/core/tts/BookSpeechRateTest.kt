package com.shenghui.localvibe.core.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookSpeechRateTest {
    @Test
    fun recommendedPresetsMatchProductBaseline() {
        assertEquals(
            listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f),
            BookSpeechRate.recommendedPresets.map { it.multiplier }
        )
    }

    @Test
    fun currentUiRateIsClampedToExistingSliderRange() {
        assertEquals(0.6f, BookSpeechRate.fromCurrentUiMultiplier(0.1f).multiplier)
        assertEquals(1.8f, BookSpeechRate.fromCurrentUiMultiplier(3.0f).multiplier)
        assertEquals(1.0f, BookSpeechRate.fromCurrentUiMultiplier(Float.NaN).multiplier)
    }

    @Test
    fun engineRateIsClampedToEngineSafetyRange() {
        assertEquals(0.5f, BookSpeechRate.fromUserMultiplier(0.1f).multiplier)
        assertEquals(2.0f, BookSpeechRate.fromUserMultiplier(3.0f).multiplier)
        assertEquals(1.25f, BookSpeechRate.fromUserMultiplier(1.251f).multiplier)
    }

    @Test
    fun systemAndSherpaUseEngineLevelSpeedButPcmPlaybackStaysNeutral() {
        val rate = BookSpeechRate.fromUserMultiplier(1.5f)

        assertEquals(1.5f, rate.systemTtsSpeechRate)
        assertEquals(1.5f, rate.sherpaGenerateSpeed)
        assertEquals(1.0f, rate.pcmPlaybackSpeed)
    }

    @Test
    fun streamingParamsUseSherpaSpeedWithoutTouchingPcmFormat() {
        val params = BookSpeechRate.fromUserMultiplier(1.75f)
            .asStreamingParams(voiceId = "aishell3-speaker-10", pitch = 3f)

        assertEquals("aishell3-speaker-10", params.voiceId)
        assertEquals(1.75f, params.speed)
        assertEquals(2.0f, params.pitch)
        assertEquals(1.0f, params.volume)
    }

    @Test
    fun realTimeFactorUsesSynthesisDurationOverGeneratedDuration() {
        val metrics = TtsQualityMetrics(
            firstAudioLatencyMs = 320,
            synthesisDurationMs = 500,
            generatedAudioDurationMs = 1000
        )

        assertEquals(0.5, metrics.realTimeFactor ?: -1.0, 0.0001)
        assertNull(
            TtsQualityMetrics(
                firstAudioLatencyMs = 0,
                synthesisDurationMs = 500,
                generatedAudioDurationMs = 0
            ).realTimeFactor
        )
    }
}

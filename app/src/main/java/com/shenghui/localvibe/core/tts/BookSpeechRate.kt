package com.shenghui.localvibe.core.tts

import kotlin.math.round

data class BookSpeechRate(
    val multiplier: Float
) {
    val systemTtsSpeechRate: Float
        get() = multiplier

    val sherpaGenerateSpeed: Float
        get() = multiplier

    val pcmPlaybackSpeed: Float
        get() = DEFAULT_MULTIPLIER

    fun asStreamingParams(
        voiceId: String,
        pitch: Float = DEFAULT_MULTIPLIER,
        volume: Float = DEFAULT_MULTIPLIER
    ): StreamingTtsParams {
        return StreamingTtsParams(
            voiceId = voiceId,
            speed = sherpaGenerateSpeed,
            pitch = pitch.coerceIn(MIN_ENGINE_MULTIPLIER, MAX_ENGINE_MULTIPLIER),
            volume = volume
        )
    }

    companion object {
        const val DEFAULT_MULTIPLIER = 1.0f
        const val MIN_RECOMMENDED_MULTIPLIER = 0.75f
        const val MAX_RECOMMENDED_MULTIPLIER = 2.0f
        const val MIN_CURRENT_UI_MULTIPLIER = 0.6f
        const val MAX_CURRENT_UI_MULTIPLIER = 1.8f
        const val MIN_ENGINE_MULTIPLIER = 0.5f
        const val MAX_ENGINE_MULTIPLIER = 2.0f

        val recommendedPresets: List<BookSpeechRate> = listOf(
            0.75f,
            1.0f,
            1.25f,
            1.5f,
            1.75f,
            2.0f
        ).map(::BookSpeechRate)

        fun fromUserMultiplier(value: Float): BookSpeechRate {
            val safeValue = if (value.isNaN()) DEFAULT_MULTIPLIER else value
            return BookSpeechRate(
                multiplier = roundToStep(
                    safeValue.coerceIn(MIN_ENGINE_MULTIPLIER, MAX_ENGINE_MULTIPLIER)
                )
            )
        }

        fun fromCurrentUiMultiplier(value: Float): BookSpeechRate {
            val safeValue = if (value.isNaN()) DEFAULT_MULTIPLIER else value
            return BookSpeechRate(
                multiplier = roundToStep(
                    safeValue.coerceIn(MIN_CURRENT_UI_MULTIPLIER, MAX_CURRENT_UI_MULTIPLIER)
                )
            )
        }

        private fun roundToStep(value: Float): Float {
            return round(value * 100f) / 100f
        }
    }
}

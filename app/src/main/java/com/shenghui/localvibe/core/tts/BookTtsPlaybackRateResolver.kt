package com.shenghui.localvibe.core.tts

/** Resolves the rate for a newly synthesized target without freezing an old UI value. */
object BookTtsPlaybackRateResolver {
    fun resolveLatest(uiRate: Float): BookSpeechRate =
        BookSpeechRate.fromCurrentUiMultiplier(uiRate)

    fun acceptsPreparedRate(entryRate: Float, currentRate: Float): Boolean =
        entryRate.isFinite() && currentRate.isFinite() &&
            kotlin.math.abs(entryRate - currentRate) < 0.001f
}

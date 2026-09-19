package com.shenghui.localvibe.core.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookTtsPlaybackRateResolverTest {
    @Test
    fun latestUiRateIsUsedForNextTarget() {
        val playbackStartedRate = 1.0f
        val latestUiRate = 1.5f

        val resolved = BookTtsPlaybackRateResolver.resolveLatest(latestUiRate)

        assertEquals(1.5f, resolved.multiplier)
        assertEquals(1.5f, resolved.sherpaGenerateSpeed)
        assertFalse(resolved.multiplier == playbackStartedRate)
    }

    @Test
    fun preparedAudioAtOldRateIsRejected() {
        assertFalse(BookTtsPlaybackRateResolver.acceptsPreparedRate(1.0f, 1.5f))
    }

    @Test
    fun preparedAudioAtCurrentRateIsAccepted() {
        assertTrue(BookTtsPlaybackRateResolver.acceptsPreparedRate(1.5f, 1.5f))
    }
}

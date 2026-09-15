package com.shenghui.localvibe.core.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PcmPlaybackCompletionTest {
    private val mono16kFormat = PcmAudioFormat(
        sampleRate = 16_000,
        channelCount = 1,
        encoding = PcmAudioEncoding.PCM_16BIT
    )

    @Test
    fun frameCountConvertsPcm16MonoBytes() {
        assertEquals(0L, PcmPlaybackCompletion.frameCount(bytes = 0, format = mono16kFormat))
        assertEquals(1L, PcmPlaybackCompletion.frameCount(bytes = 2, format = mono16kFormat))
        assertEquals(16_000L, PcmPlaybackCompletion.frameCount(bytes = 32_000, format = mono16kFormat))
    }

    @Test
    fun playbackIsNotCompleteBeforeAllQueuedFramesAreWritten() {
        assertFalse(
            PcmPlaybackCompletion.isComplete(
                queuedFrames = 16_000,
                writtenFrames = 15_999,
                playbackHeadFrames = 16_000
            )
        )
    }

    @Test
    fun playbackIsNotCompleteBeforeAudioTrackHeadReachesQueuedFrames() {
        assertFalse(
            PcmPlaybackCompletion.isComplete(
                queuedFrames = 16_000,
                writtenFrames = 16_000,
                playbackHeadFrames = 15_999
            )
        )
    }

    @Test
    fun playbackIsCompleteOnlyAfterQueuedWrittenAndPlayedFramesMatch() {
        assertTrue(
            PcmPlaybackCompletion.isComplete(
                queuedFrames = 16_000,
                writtenFrames = 16_000,
                playbackHeadFrames = 16_000
            )
        )
    }
}

package com.shenghui.localvibe.core.tts

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookTtsCacheKeyTest {
    @Test
    fun speakerIdentitySeparatesPreparedAndSegmentKeys() {
        val voices = Aishell3VoiceRegistry.fromSpeakerLabels((0..25).map { "SSB${it}" }).voices
        val first = voices[10]
        val second = voices[25]
        val prepared10 = BookTtsCacheKey.aishell3("book", 4, 99, 1.0f, 1.0f, first)
        val prepared25 = BookTtsCacheKey.aishell3("book", 4, 99, 1.0f, 1.0f, second)
        val segment10 = BookTtsCacheKey.aishell3Segment("book", 4, 0, 99, 1.0f, 1.0f, first)
        val segment25 = BookTtsCacheKey.aishell3Segment("book", 4, 0, 99, 1.0f, 1.0f, second)

        assertNotEquals(prepared10, prepared25)
        assertNotEquals(segment10, segment25)
        assertTrue(prepared10.contains("aishell3-speaker-10"))
    }
}

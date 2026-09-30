package com.shenghui.localvibe.core.tts

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MatchaPrewarmTest {
    private fun key(epoch: Long = 0L, speed: Float = 1f, target: Int = 76) = MatchaPrewarmKey(
        chapterSentenceIndex = target,
        paragraphIndex = 10,
        sentenceIndexInParagraph = 2,
        textHash = 76,
        provider = "MATCHA_EXPERIMENTAL",
        speed = speed,
        epoch = epoch
    )

    private fun audio() = MatchaPreparedAudio(
        chunk = PcmAudioChunk(ByteArray(4), PcmAudioFormat(22050, 1), true),
        audioDurationMs = 1_000,
        generateMs = 100,
        lockWaitMs = 0,
        provider = "MATCHA_EXPERIMENTAL",
        speed = 1f,
        textHash = 76
    )

    @Test
    fun exactReadyAudioIsConsumedOnce() {
        val slot = MatchaPrewarmSlot()
        val request = slot.begin(key(), 1L)!!
        assertEquals(true, slot.complete(request, audio()))
        assertNotNull(slot.consume(key()))
        assertNull(slot.consume(key()))
    }

    @Test
    fun wrongKeyDoesNotConsume() {
        val slot = MatchaPrewarmSlot()
        val request = slot.begin(key(), 1L)!!
        slot.complete(request, audio())
        assertNull(slot.consume(key(target = 77)))
    }

    @Test
    fun epochInvalidationDiscardsReadyAndInFlight() = runBlocking {
        val slot = MatchaPrewarmSlot()
        val request = slot.begin(key(), 1L)!!
        slot.invalidate()
        assertFalse(slot.complete(request, audio()))
        assertNull(request.result.await())
        assertNull(slot.consume(key()))
    }

    @Test
    fun exactInFlightRequestCanBeReused() {
        val slot = MatchaPrewarmSlot()
        val request = slot.begin(key(), 1L)!!
        assertNotNull(slot.inFlight(key()))
        assertNull(slot.begin(key(), 2L))
        slot.clearIf(request)
        assertNull(slot.inFlight(key()))
    }

    @Test
    fun mismatchedReadyTargetIsDroppedAndSlotCanBeReused() {
        val slot = MatchaPrewarmSlot()
        val request = slot.begin(key(target = 92), 1L)!!
        slot.complete(request, audio())
        assertEquals(92, slot.dropIfTargetMismatch(93)?.chapterSentenceIndex)
        assertNull(slot.consume(key(target = 92)))
        assertNotNull(slot.begin(key(target = 93), 2L))
    }
}

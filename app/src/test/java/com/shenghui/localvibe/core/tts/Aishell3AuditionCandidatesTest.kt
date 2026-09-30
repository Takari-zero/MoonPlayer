package com.shenghui.localvibe.core.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Aishell3AuditionCandidatesTest {
    @Test
    fun returnsTwelveDistributedCandidatesIncludingDefaultSpeaker() {
        val registry = Aishell3VoiceRegistry.fromSpeakerLabels((0 until 174).map { "SSB$it" })
        val candidates = Aishell3AuditionCandidates.from(registry)

        assertEquals(12, candidates.size)
        assertEquals(listOf(0, 10, 20, 35, 50, 65, 80, 95, 110, 125, 145, 165), candidates.map { it.voice.speakerId })
        assertTrue(candidates.any { it.voice.speakerId == Aishell3VoiceRegistry.DEFAULT_AISHELL3_SPEAKER_ID })
        assertEquals((1..12).toList(), candidates.map { it.slot })
    }

    @Test
    fun unavailableCandidateRemainsVisibleAndDisabled() {
        val registry = Aishell3VoiceRegistry.fromSpeakerLabels((0 until 100).map { "SSB$it" })
        val candidates = Aishell3AuditionCandidates.from(registry)

        assertEquals(12, candidates.size)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12), candidates.map { it.slot })
        assertTrue(candidates.last().voice.available.not())
    }

    @Test
    fun replacementAcceptsOnlyLatestSessionCallbacks() {
        assertTrue(Aishell3AuditionCandidates.acceptsCallback(2L, 2L))
        assertTrue(!Aishell3AuditionCandidates.acceptsCallback(2L, 1L))
    }

    @Test
    fun missingRegistryStillKeepsAllCandidatesVisibleAsUnavailable() {
        val candidates = Aishell3AuditionCandidates.from(null)

        assertEquals(12, candidates.size)
        assertTrue(candidates.all { it.voice.available.not() })
    }
}

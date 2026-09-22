package com.shenghui.localvibe.core.tts

import org.junit.Assert.assertEquals
import org.junit.Test

class Aishell3VoiceRegistryTest {
    private val labels = listOf("SSB0005", "SSB0009", "SSB0073", "SSB1956")

    @Test
    fun parsesOneLabelPerLineWithZeroBasedSid() {
        val registry = Aishell3VoiceRegistry.fromSpeakerLabels(labels)
        assertEquals(4, registry.numSpeakers)
        assertEquals("SSB0073", registry.voices[2].speakerLabel)
        assertEquals(2, registry.voices[2].speakerId)
        assertEquals("aishell3-speaker-2", registry.voices[2].voiceId)
    }

    @Test
    fun ignoresBlankLines() {
        assertEquals(2, Aishell3VoiceRegistry.parseSpeakerLabels(listOf("A", "", "  ", "B" )).size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDuplicateLabels() {
        Aishell3VoiceRegistry.parseSpeakerLabels(listOf("A", "A"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMalformedLabel() {
        Aishell3VoiceRegistry.parseSpeakerLabels(listOf("SSB 10"))
    }

    @Test
    fun invalidPersistedVoiceFallsBackToDefault() {
        val registry = Aishell3VoiceRegistry.fromSpeakerLabels((0..10).map { "SSB${it}" })
        listOf(
            "aishell3-speaker--1",
            "aishell3-speaker-11",
            "aishell3-speaker-174",
            "aishell3-speaker-999",
            "aishell3-speaker-abc",
            "speaker-10",
            ""
        ).forEach { invalidVoiceId ->
            assertEquals(10, registry.resolveSpeakerIdOrDefault(invalidVoiceId))
        }
        assertEquals(10, registry.resolveSpeakerIdOrDefault(null))
    }
}

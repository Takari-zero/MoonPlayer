package com.shenghui.localvibe.core.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineTtsResourceConfigTest {
    @Test
    fun aishell3ConfigUsesExpectedResources() {
        val config = OfflineTtsResourceConfigs.AISHELL3

        assertEquals("model.onnx", config.modelFile)
        assertEquals("tokens.txt", config.tokensFile)
        assertEquals("lexicon.txt", config.lexiconFile)
        assertEquals("", config.dictDir)
        assertEquals(EXPECTED_RULE_FSTS, config.ruleFstFiles)
    }

    @Test
    fun builtInOfflineConfigUsesExpectedResources() {
        val config = OfflineTtsResourceConfigs.BUILT_IN_OFFLINE

        assertEquals("model.int8.onnx", config.modelFile)
        assertEquals("tokens.txt", config.tokensFile)
        assertEquals("lexicon.txt", config.lexiconFile)
        assertEquals("", config.dictDir)
        assertEquals(EXPECTED_RULE_FSTS, config.ruleFstFiles)
        assertTrue("heteronym FST must stay configured", "new_heteronym.fst" in config.ruleFstFiles)
    }

    private companion object {
        val EXPECTED_RULE_FSTS: List<String> = listOf(
            "phone.fst",
            "date.fst",
            "number.fst",
            "new_heteronym.fst"
        )
    }
}

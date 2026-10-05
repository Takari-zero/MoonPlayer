package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookAishell3PrewarmGateTest {
    @Test
    fun `matcha does not enqueue aishell3 prewarm`() {
        assertFalse(BookAishell3PrewarmGate.allows(BookPlaybackEngine.MATCHA_EXPERIMENTAL, true))
    }

    @Test
    fun `aishell3 keeps prewarm enabled`() {
        assertTrue(BookAishell3PrewarmGate.allows(BookPlaybackEngine.AISHELL3, true))
    }

    @Test
    fun `system tts does not enqueue aishell3 prewarm`() {
        assertFalse(BookAishell3PrewarmGate.allows(BookPlaybackEngine.SYSTEM_TTS, true))
    }

    @Test
    fun `unrestored provider selection rejects aishell3 prewarm`() {
        assertFalse(BookAishell3PrewarmGate.allows(BookPlaybackEngine.AISHELL3, false))
    }

    @Test
    fun `restored matcha rejects aishell3 prewarm`() {
        assertFalse(BookAishell3PrewarmGate.allows(BookPlaybackEngine.MATCHA_EXPERIMENTAL, true))
    }
}

package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookMatchaPlaybackPauseStateTest {
    @Test
    fun pausedSessionCanResumeOnlyOnTheSameCanonicalSession() {
        val state = BookMatchaPlaybackPauseState()

        state.pause(41L)

        assertTrue(state.isPaused(41L))
        assertFalse(state.canResume(42L))
        assertTrue(state.canResume(41L))
        assertTrue(state.resume(41L))
        assertFalse(state.isPaused(41L))
        assertFalse(state.resume(41L))
    }

    @Test
    fun terminalClearInvalidatesPausedSession() {
        val state = BookMatchaPlaybackPauseState()

        state.pause(41L)
        state.clear()

        assertFalse(state.canResume(41L))
        assertFalse(state.resume(41L))
    }
}

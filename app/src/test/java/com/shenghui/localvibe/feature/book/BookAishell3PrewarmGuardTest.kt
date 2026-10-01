package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookAishell3PrewarmGuardTest {
    @Test
    fun `active aishell3 allows segment chain and next target`() {
        val state = State()
        val request = state.begin()!!
        var generated = 0
        var published = 0
        var nextTargets = 0
        repeat(2) {
            if (request.isActive()) generated++
            request.runIfActive { published++ }
        }
        request.runIfActive { if (state.begin() != null) nextTargets++ }
        assertEquals(2, generated)
        assertEquals(2, published)
        assertEquals(1, nextTargets)
        assertTrue(state.discards.isEmpty())
    }

    @Test
    fun `provider changes during native call discard completion without starting another segment`() {
        val state = State()
        val request = state.begin()!!
        var nativeCalls = 1
        var cachePuts = 0
        var nextTargets = 0
        state.provider = BookPlaybackEngine.MATCHA_EXPERIMENTAL

        request.runIfActive {
            cachePuts++
            nextTargets++
        }
        if (request.isActive()) nativeCalls++

        assertEquals(1, nativeCalls)
        assertEquals(0, cachePuts)
        assertEquals(0, nextTargets)
        assertEquals(listOf("PROVIDER_INACTIVE"), state.discards)
    }

    @Test
    fun `restore blocks captured callback even when playback session stays unchanged`() {
        val state = State()
        val request = state.begin()!!
        var queued = 0
        val oldCallback = { request.runIfActive { queued++ } }
        state.provider = BookPlaybackEngine.MATCHA_EXPERIMENTAL
        oldCallback()
        assertEquals(1L, state.session)
        assertEquals(0, queued)
        assertFalse(request.isActive())
    }

    @Test
    fun `returning to aishell3 cannot revive an old session callback`() {
        val state = State()
        val oldRequest = state.begin()!!
        var queued = 0
        state.provider = BookPlaybackEngine.MATCHA_EXPERIMENTAL
        state.session++
        state.provider = BookPlaybackEngine.AISHELL3
        state.session++

        oldRequest.runIfActive { queued++ }
        assertEquals(0, queued)
        assertFalse(oldRequest.isActive())
        assertEquals(listOf("STALE_SESSION"), state.discards)
        assertTrue(state.begin()!!.isActive())
    }

    @Test
    fun `matcha entry skips native synthesis entirely`() {
        val state = State()
        state.provider = BookPlaybackEngine.MATCHA_EXPERIMENTAL
        var nativeCalls = 0
        state.begin()?.runIfActive { nativeCalls++ }
        assertEquals(0, nativeCalls)
        assertNull(state.begin())
    }

    @Test
    fun `session replacement blocks old prewarm even with the same provider`() {
        val state = State()
        val request = state.begin()!!
        state.session++
        assertFalse(request.isActive())
        assertEquals(listOf("STALE_SESSION"), state.discards)
    }

    @Test
    fun `disposed book blocks completion callback and new entry`() {
        val state = State()
        val request = state.begin()!!
        var cachePuts = 0
        state.disposed = true
        request.runIfActive { cachePuts++ }
        assertEquals(0, cachePuts)
        assertFalse(request.isActive())
        assertNull(state.begin())
        assertEquals(1, state.discards.size)
    }

    @Test
    fun `discarded request remains invalid and reports only once`() {
        val state = State()
        val request = state.begin()!!
        assertNotNull(request)
        state.provider = BookPlaybackEngine.SYSTEM_TTS
        assertFalse(request.isActive())
        state.provider = BookPlaybackEngine.AISHELL3
        repeat(3) { assertFalse(request.isActive()) }
        assertEquals(listOf("PROVIDER_INACTIVE"), state.discards)
    }

    private class State {
        var provider = BookPlaybackEngine.AISHELL3
        var session = 1L
        var disposed = false
        val discards = mutableListOf<String>()

        fun begin() = BookAishell3PrewarmGuard.begin(
            latestProvider = { BookPlaybackEngineSnapshot(provider, provider, matchaAvailable = true) },
            latestSessionId = { session },
            screenDisposed = { disposed },
            onDiscard = { reason, _, _ -> discards.add(reason) }
        )
    }
}

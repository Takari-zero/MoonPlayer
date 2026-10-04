package com.shenghui.localvibe.core.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookTtsUtteranceRegistryTest {
    @Test
    fun doneKeepsTheCapturedSessionAndCleansTheMapping() {
        val registry = BookTtsUtteranceRegistry()
        registry.register("u10", 10L)

        val event = registry.onDone("u10")

        assertEquals(10L, event?.playbackSessionId)
        assertEquals(0, registry.size())
        assertNull(registry.onDone("u10"))
    }

    @Test
    fun staleDoneAndErrorDoNotMatchTheCurrentSession() {
        val registry = BookTtsUtteranceRegistry()
        registry.register("done10", 10L)
        registry.register("error10", 10L)

        val done = registry.onDone("done10")
        val error = registry.onError("error10")

        assertTrue(done?.playbackSessionId != 11L)
        assertTrue(error?.playbackSessionId != 11L)
    }

    @Test
    fun outOfOrderCallbacksPreserveOriginalSessionIds() {
        val registry = BookTtsUtteranceRegistry()
        registry.register("u10", 10L)
        registry.register("u11", 11L)

        assertEquals(11L, registry.onDone("u11")?.playbackSessionId)
        assertEquals(10L, registry.onError("u10")?.playbackSessionId)
    }

    @Test
    fun unknownUtteranceDoesNotFallbackToLatestSession() {
        val registry = BookTtsUtteranceRegistry()
        registry.register("u10", 10L)

        assertNull(registry.onDone("unknown"))
        assertNull(registry.onError("unknown"))
        assertEquals(1, registry.size())
    }

    @Test
    fun stopAndReleaseClearMappingsAndIgnoreLateCallbacks() {
        val registry = BookTtsUtteranceRegistry()
        registry.register("u10", 10L)
        registry.register("u11", 11L)

        assertEquals(setOf(10L, 11L), registry.onStop().map { it.playbackSessionId }.toSet())
        assertEquals(0, registry.size())
        assertNull(registry.onDone("u10"))
        assertNull(registry.onError("u11"))
    }

    @Test
    fun uncorrelatedAuditionEventHasNoPlaybackSession() {
        val registry = BookTtsUtteranceRegistry()
        registry.register("audition", null)

        assertNull(registry.onDone("audition")?.playbackSessionId)
    }
}

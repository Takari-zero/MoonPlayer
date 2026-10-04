package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BookSentencePlaybackDispatcherTest {
    private val target = BookPlaybackTargetId(
        chapterSentenceIndex = 8,
        paragraphIndex = 3,
        sentenceIndexInParagraph = 2
    )
    private val request = BookSentencePlaybackRequest(
        target = target,
        text = "typed sentence",
        provider = BookPlaybackEngine.MATCHA_EXPERIMENTAL,
        speechRate = 1.25f,
        playbackSessionId = 17L,
        preparedSnapshot = BookSequentialTargetSnapshot(emptyList()),
        allowNextPrewarm = true,
        playbackIntentPlaying = true
    )

    @Test
    fun requestRetainsTargetTextProviderRateAndCanonicalSession() {
        assertEquals(target, request.target)
        assertEquals("typed sentence", request.text)
        assertEquals(BookPlaybackEngine.MATCHA_EXPERIMENTAL, request.provider)
        assertEquals(1.25f, request.speechRate)
        assertEquals(17L, request.playbackSessionId)
    }

    @Test
    fun routesAllThreeProvidersWithoutOwningContinuation() {
        val routed = mutableListOf<BookPlaybackEngine>()
        val handlers = BookSentencePlaybackProviderHandlers(
            matcha = recordHandler(routed, BookPlaybackEngine.MATCHA_EXPERIMENTAL),
            aishell3 = recordHandler(routed, BookPlaybackEngine.AISHELL3),
            systemTts = recordHandler(routed, BookPlaybackEngine.SYSTEM_TTS)
        )

        listOf(
            BookPlaybackEngine.MATCHA_EXPERIMENTAL,
            BookPlaybackEngine.AISHELL3,
            BookPlaybackEngine.SYSTEM_TTS
        )
            .forEach { provider ->
                BookSentencePlaybackDispatcher().dispatch(
                    request.copy(provider = provider),
                    BookSentencePlaybackCallbacks(),
                    handlers
                )
            }

        assertEquals(
            listOf(
                BookPlaybackEngine.MATCHA_EXPERIMENTAL,
                BookPlaybackEngine.AISHELL3,
                BookPlaybackEngine.SYSTEM_TTS
            ),
            routed
        )
    }

    @Test
    fun systemTtsDrainReturnsOriginalSessionAndDoesNotNext() {
        val dispatcher = BookSentencePlaybackDispatcher()
        val drained = mutableListOf<Long>()
        var handlerSawRequest: BookSentencePlaybackRequest? = null
        val handlers = BookSentencePlaybackProviderHandlers(
            matcha = recordHandler(mutableListOf(), BookPlaybackEngine.MATCHA_EXPERIMENTAL),
            aishell3 = recordHandler(mutableListOf(), BookPlaybackEngine.AISHELL3),
            systemTts = BookSentencePlaybackProviderHandler { request, callbacks ->
                handlerSawRequest = request
                callbacks.onStarted(request.playbackSessionId)
            }
        )

        dispatcher.dispatch(
            request.copy(provider = BookPlaybackEngine.SYSTEM_TTS),
            BookSentencePlaybackCallbacks(onDrained = { drained += it }),
            handlers
        )
        dispatcher.notifySystemTtsDrained(request.playbackSessionId)

        assertEquals(request.playbackSessionId, handlerSawRequest?.playbackSessionId)
        assertEquals(listOf(request.playbackSessionId), drained)
        assertEquals(0, dispatcher.pendingSystemTtsCallbackCount())
    }

    @Test
    fun aishell3CallbacksPropagateOriginalSessionExactlyOnce() {
        val dispatcher = BookSentencePlaybackDispatcher()
        val started = mutableListOf<Long>()
        val drained = mutableListOf<Long>()
        val failed = mutableListOf<Pair<Long, String>>()
        var handlerSawRequest: BookSentencePlaybackRequest? = null
        val handlers = BookSentencePlaybackProviderHandlers(
            matcha = recordHandler(mutableListOf(), BookPlaybackEngine.MATCHA_EXPERIMENTAL),
            aishell3 = BookSentencePlaybackProviderHandler { request, callbacks ->
                handlerSawRequest = request
                callbacks.onStarted(request.playbackSessionId)
                callbacks.onDrained(request.playbackSessionId)
                callbacks.onFailed(request.playbackSessionId, "after-drain failure")
            },
            systemTts = recordHandler(mutableListOf(), BookPlaybackEngine.SYSTEM_TTS)
        )

        dispatcher.dispatch(
            request.copy(provider = BookPlaybackEngine.AISHELL3),
            BookSentencePlaybackCallbacks(
                onStarted = { started += it },
                onDrained = { drained += it },
                onFailed = { sessionId, reason -> failed += sessionId to reason }
            ),
            handlers
        )

        assertEquals(request.playbackSessionId, handlerSawRequest?.playbackSessionId)
        assertEquals(listOf(request.playbackSessionId), started)
        assertEquals(listOf(request.playbackSessionId), drained)
        assertEquals(listOf(request.playbackSessionId to "after-drain failure"), failed)
    }

    @Test
    fun staleAishell3DrainDoesNotReachFormalContinuation() {
        val dispatcher = BookSentencePlaybackDispatcher()
        var handlerCallbacks: BookSentencePlaybackCallbacks? = null
        val drained = mutableListOf<Long>()
        val handlers = BookSentencePlaybackProviderHandlers(
            matcha = recordHandler(mutableListOf(), BookPlaybackEngine.MATCHA_EXPERIMENTAL),
            aishell3 = BookSentencePlaybackProviderHandler { _, callbacks -> handlerCallbacks = callbacks },
            systemTts = recordHandler(mutableListOf(), BookPlaybackEngine.SYSTEM_TTS)
        )

        dispatcher.dispatch(
            request.copy(provider = BookPlaybackEngine.AISHELL3),
            BookSentencePlaybackCallbacks(onDrained = { drained += it }),
            handlers
        )
        handlerCallbacks?.onDrained(request.playbackSessionId + 1L)

        assertEquals(emptyList<Long>(), drained)
    }

    @Test
    fun dispatcherFailureDoesNotChangePendingTarget() {
        val dispatcher = BookSentencePlaybackDispatcher()
        var failedSessionId: Long? = null
        val handlers = BookSentencePlaybackProviderHandlers(
            matcha = recordHandler(mutableListOf(), BookPlaybackEngine.MATCHA_EXPERIMENTAL),
            aishell3 = recordHandler(mutableListOf(), BookPlaybackEngine.AISHELL3),
            systemTts = BookSentencePlaybackProviderHandler { request, callbacks ->
                callbacks.onFailed(request.playbackSessionId, "provider failed")
            }
        )

        dispatcher.dispatch(
            request.copy(provider = BookPlaybackEngine.SYSTEM_TTS),
            BookSentencePlaybackCallbacks(onFailed = { sessionId, _ -> failedSessionId = sessionId }),
            handlers
        )

        assertEquals(request.playbackSessionId, failedSessionId)
        assertEquals(0, dispatcher.pendingSystemTtsCallbackCount())
    }

    @Test
    fun staleCallbackSessionIsIgnoredAndFailureDoesNotTouchPending() {
        val dispatcher = BookSentencePlaybackDispatcher()
        var started = false
        var failed = false
        lateinit var handlerCallbacks: BookSentencePlaybackCallbacks
        val handlers = BookSentencePlaybackProviderHandlers(
            matcha = BookSentencePlaybackProviderHandler { _, callbacks -> handlerCallbacks = callbacks },
            aishell3 = recordHandler(mutableListOf(), BookPlaybackEngine.AISHELL3),
            systemTts = recordHandler(mutableListOf(), BookPlaybackEngine.SYSTEM_TTS)
        )

        dispatcher.dispatch(
            request,
            BookSentencePlaybackCallbacks(
                onStarted = { started = true },
                onFailed = { _, _ -> failed = true }
            ),
            handlers
        )
        handlerCallbacks.onStarted(request.playbackSessionId + 1L)
        handlerCallbacks.onFailed(request.playbackSessionId + 1L, "stale")

        assertFalse(started)
        assertFalse(failed)
    }

    @Test
    fun futureLocalShapedRequestAllowsNullSnapshotAndDisablesPrewarm() {
        val futureLocal = request.copy(
            source = BookSentencePlaybackSource.CACHED_PREVIEW,
            preparedSnapshot = null,
            allowNextPrewarm = false
        )

        assertEquals(BookSentencePlaybackSource.CACHED_PREVIEW, futureLocal.source)
        assertEquals(null, futureLocal.preparedSnapshot)
        assertFalse(futureLocal.allowNextPrewarm)
    }

    private fun recordHandler(
        routed: MutableList<BookPlaybackEngine>,
        provider: BookPlaybackEngine
    ) = BookSentencePlaybackProviderHandler { _, _ -> routed += provider }
}

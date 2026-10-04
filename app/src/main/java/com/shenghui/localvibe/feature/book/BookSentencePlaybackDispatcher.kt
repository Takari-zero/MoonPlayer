package com.shenghui.localvibe.feature.book

import java.util.concurrent.ConcurrentHashMap

internal enum class BookSentencePlaybackSource {
    FORMAL_PREPARED,
    CACHED_PREVIEW,
    FUTURE_LOCAL
}

internal data class BookSentencePlaybackRequest(
    val target: BookPlaybackTargetId,
    val text: String,
    val provider: BookPlaybackEngine,
    val speechRate: Float,
    val playbackSessionId: Long,
    val preparedSnapshot: BookSequentialTargetSnapshot? = null,
    val allowNextPrewarm: Boolean = true,
    val playbackIntentPlaying: Boolean = true,
    val source: BookSentencePlaybackSource = BookSentencePlaybackSource.FORMAL_PREPARED
)

internal data class BookSentencePlaybackCallbacks(
    val onStarted: (Long) -> Unit = {},
    val onDrained: (Long) -> Unit = {},
    val onFailed: (Long, String) -> Unit = { _, _ -> }
)

internal fun interface BookSentencePlaybackProviderHandler {
    fun dispatch(
        request: BookSentencePlaybackRequest,
        callbacks: BookSentencePlaybackCallbacks
    )
}

internal data class BookSentencePlaybackProviderHandlers(
    val matcha: BookSentencePlaybackProviderHandler,
    val aishell3: BookSentencePlaybackProviderHandler,
    val systemTts: BookSentencePlaybackProviderHandler
)

/**
 * Provider routing only. It does not own pending targets, continuation, readiness, or LOCAL playback.
 */
internal class BookSentencePlaybackDispatcher {
    private val systemTtsCallbacks = ConcurrentHashMap<Long, BookSentencePlaybackCallbacks>()

    fun dispatch(
        request: BookSentencePlaybackRequest,
        callbacks: BookSentencePlaybackCallbacks,
        handlers: BookSentencePlaybackProviderHandlers
    ) {
        val scopedCallbacks = callbacks.scopedTo(request.playbackSessionId)
        if (request.provider == BookPlaybackEngine.SYSTEM_TTS) {
            systemTtsCallbacks[request.playbackSessionId] = scopedCallbacks
        }
        val handler = when (request.provider) {
            BookPlaybackEngine.MATCHA_EXPERIMENTAL -> handlers.matcha
            BookPlaybackEngine.AISHELL3 -> handlers.aishell3
            BookPlaybackEngine.SYSTEM_TTS -> handlers.systemTts
            BookPlaybackEngine.NONE -> null
        }
        if (handler == null) {
            systemTtsCallbacks.remove(request.playbackSessionId)
            scopedCallbacks.onFailed(request.playbackSessionId, "unsupported playback provider")
            return
        }
        runCatching { handler.dispatch(request, scopedCallbacks) }
            .onFailure { error ->
                systemTtsCallbacks.remove(request.playbackSessionId)
                scopedCallbacks.onFailed(
                    request.playbackSessionId,
                    error.message ?: error::class.java.simpleName
                )
            }
    }

    fun notifySystemTtsDrained(playbackSessionId: Long) {
        systemTtsCallbacks.remove(playbackSessionId)?.onDrained(playbackSessionId)
    }

    fun notifySystemTtsFailed(playbackSessionId: Long, reason: String) {
        systemTtsCallbacks.remove(playbackSessionId)?.onFailed(playbackSessionId, reason)
    }

    fun clearSystemTts(playbackSessionId: Long) {
        systemTtsCallbacks.remove(playbackSessionId)
    }

    internal fun pendingSystemTtsCallbackCount(): Int = systemTtsCallbacks.size

    private fun BookSentencePlaybackCallbacks.scopedTo(expectedSessionId: Long) =
        BookSentencePlaybackCallbacks(
            onStarted = { callbackSessionId ->
                if (callbackSessionId == expectedSessionId) onStarted(callbackSessionId)
            },
            onDrained = { callbackSessionId ->
                if (callbackSessionId == expectedSessionId) {
                    systemTtsCallbacks.remove(expectedSessionId)
                    onDrained(callbackSessionId)
                }
            },
            onFailed = { callbackSessionId, reason ->
                if (callbackSessionId == expectedSessionId) {
                    systemTtsCallbacks.remove(expectedSessionId)
                    onFailed(callbackSessionId, reason)
                }
            }
        )
}

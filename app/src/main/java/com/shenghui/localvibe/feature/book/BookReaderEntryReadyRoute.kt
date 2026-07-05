package com.shenghui.localvibe.feature.book

import com.shenghui.localvibe.core.scanner.LocalMediaFile
import com.shenghui.localvibe.feature.book.playback.BookReaderEntryLoadRequest
import com.shenghui.localvibe.feature.book.playback.BookReaderEntryReadyState

data class BookReaderEntryReadyRouteInput(
    val bookFile: LocalMediaFile?,
    val initialParagraphIndex: Int,
    val speechRate: Float = 1f,
) {
    fun toLoadRequest(): BookReaderEntryLoadRequest? {
        val file = bookFile ?: return null
        return BookReaderEntryLoadRequest(
            bookId = file.uri,
            bookTitle = file.name,
            chapterStartIndex = initialParagraphIndex.coerceAtLeast(0),
            speechRate = speechRate,
        )
    }
}

sealed interface BookReaderEntryReadyRouteState {
    val input: BookReaderEntryReadyRouteInput

    data class Preparing(
        override val input: BookReaderEntryReadyRouteInput,
    ) : BookReaderEntryReadyRouteState

    data class Ready(
        override val input: BookReaderEntryReadyRouteInput,
        val readyState: BookReaderEntryReadyState,
    ) : BookReaderEntryReadyRouteState

    data class Failed(
        override val input: BookReaderEntryReadyRouteInput,
        val message: String,
        val cause: Throwable? = null,
    ) : BookReaderEntryReadyRouteState
}

sealed interface BookReaderEntryReadyRouteSlot {
    val exposesStableSnapshotAsReaderContent: Boolean
        get() = false

    fun readyStateOrNull(): BookReaderEntryReadyState? = null

    data class Preparing(
        val input: BookReaderEntryReadyRouteInput,
    ) : BookReaderEntryReadyRouteSlot

    data class ReadyContent(
        val readyState: BookReaderEntryReadyState,
    ) : BookReaderEntryReadyRouteSlot {
        override fun readyStateOrNull(): BookReaderEntryReadyState = readyState
    }

    data class Failed(
        val input: BookReaderEntryReadyRouteInput,
        val message: String,
        val cause: Throwable? = null,
    ) : BookReaderEntryReadyRouteSlot
}

object BookReaderEntryReadyRouteHost {
    fun resolveSlot(state: BookReaderEntryReadyRouteState): BookReaderEntryReadyRouteSlot {
        return when (state) {
            is BookReaderEntryReadyRouteState.Preparing -> {
                BookReaderEntryReadyRouteSlot.Preparing(state.input)
            }
            is BookReaderEntryReadyRouteState.Ready -> {
                BookReaderEntryReadyRouteSlot.ReadyContent(state.readyState)
            }
            is BookReaderEntryReadyRouteState.Failed -> {
                BookReaderEntryReadyRouteSlot.Failed(
                    input = state.input,
                    message = state.message,
                    cause = state.cause,
                )
            }
        }
    }
}

sealed interface BookReaderEntryReadyRouteShadowPlan {
    val shouldRunLoader: Boolean
        get() = false

    fun loadRequestOrNull(): BookReaderEntryLoadRequest? = null

    data class Disabled(
        val input: BookReaderEntryReadyRouteInput,
    ) : BookReaderEntryReadyRouteShadowPlan

    data class Enabled(
        val input: BookReaderEntryReadyRouteInput,
        val loadRequest: BookReaderEntryLoadRequest,
    ) : BookReaderEntryReadyRouteShadowPlan {
        override val shouldRunLoader: Boolean = true

        override fun loadRequestOrNull(): BookReaderEntryLoadRequest = loadRequest
    }
}

object BookReaderEntryReadyRouteShadow {
    fun plan(
        enabled: Boolean,
        input: BookReaderEntryReadyRouteInput,
    ): BookReaderEntryReadyRouteShadowPlan {
        val loadRequest = input.toLoadRequest()
        return if (enabled && loadRequest != null) {
            BookReaderEntryReadyRouteShadowPlan.Enabled(
                input = input,
                loadRequest = loadRequest,
            )
        } else {
            BookReaderEntryReadyRouteShadowPlan.Disabled(input)
        }
    }
}

sealed interface BookReaderEntryReadyPathPlan {
    val usesReadyState: Boolean
        get() = false

    fun entryReadyStateForScreen(): BookReaderEntryReadyState? = null

    data object Legacy : BookReaderEntryReadyPathPlan

    data class Ready(
        val readyState: BookReaderEntryReadyState,
    ) : BookReaderEntryReadyPathPlan {
        override val usesReadyState: Boolean = true

        override fun entryReadyStateForScreen(): BookReaderEntryReadyState = readyState
    }
}

object BookReaderEntryReadyPath {
    fun plan(
        enabled: Boolean,
        routeState: BookReaderEntryReadyRouteState?,
    ): BookReaderEntryReadyPathPlan {
        return if (enabled && routeState is BookReaderEntryReadyRouteState.Ready) {
            BookReaderEntryReadyPathPlan.Ready(routeState.readyState)
        } else {
            BookReaderEntryReadyPathPlan.Legacy
        }
    }
}

fun interface BookReaderEntryReadyRouteReadyStateLoader {
    fun load(request: BookReaderEntryLoadRequest): Result<BookReaderEntryReadyState>
}

object BookReaderEntryReadyRouteLoader {
    fun loadIfEnabled(
        enabled: Boolean,
        input: BookReaderEntryReadyRouteInput,
        loader: BookReaderEntryReadyRouteReadyStateLoader,
    ): BookReaderEntryReadyRouteState? {
        if (!enabled) return null
        val request = input.toLoadRequest()
            ?: return BookReaderEntryReadyRouteState.Failed(
                input = input,
                message = "book file unavailable",
            )
        return loader.load(request).fold(
            onSuccess = { readyState ->
                BookReaderEntryReadyRouteState.Ready(
                    input = input,
                    readyState = readyState,
                )
            },
            onFailure = { error ->
                BookReaderEntryReadyRouteState.Failed(
                    input = input,
                    message = error.message ?: "reader entry ready state unavailable",
                    cause = error,
                )
            },
        )
    }
}

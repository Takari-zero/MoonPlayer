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

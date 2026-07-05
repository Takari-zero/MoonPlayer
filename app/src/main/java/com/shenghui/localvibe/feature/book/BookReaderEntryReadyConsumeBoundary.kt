package com.shenghui.localvibe.feature.book

import com.shenghui.localvibe.feature.book.playback.BookReaderEntryCanonicalTarget
import com.shenghui.localvibe.feature.book.playback.BookReaderEntryPlaybackSeed
import com.shenghui.localvibe.feature.book.playback.BookReaderEntryProgressSnapshot
import com.shenghui.localvibe.feature.book.playback.BookReaderEntryReadyState
import com.shenghui.localvibe.feature.book.playback.BookReaderEntrySentence

sealed interface BookReaderEntryReadyConsumeBoundary {
    val usesReadyState: Boolean
        get() = false

    fun readyStateOrNull(): BookReaderEntryReadyState? = null

    fun firstFrameOrNull(): BookReaderEntryReadyFirstFrame? = null

    data object Legacy : BookReaderEntryReadyConsumeBoundary

    data class Ready(
        val readyState: BookReaderEntryReadyState,
        val firstFrame: BookReaderEntryReadyFirstFrame,
    ) : BookReaderEntryReadyConsumeBoundary {
        override val usesReadyState: Boolean = true

        override fun readyStateOrNull(): BookReaderEntryReadyState = readyState

        override fun firstFrameOrNull(): BookReaderEntryReadyFirstFrame = firstFrame
    }

    companion object {
        fun from(readyState: BookReaderEntryReadyState?): BookReaderEntryReadyConsumeBoundary {
            return if (readyState == null) {
                Legacy
            } else {
                Ready(
                    readyState = readyState,
                    firstFrame = BookReaderEntryReadyFirstFrame.from(readyState),
                )
            }
        }
    }
}

data class BookReaderEntryReadyFirstFrame(
    val paragraphs: List<String>,
    val chapterSentences: List<BookReaderEntrySentence>,
    val firstFrameTarget: BookReaderEntryCanonicalTarget,
    val progressSnapshot: BookReaderEntryProgressSnapshot,
    val lazyListInitialIndex: Int,
    val lazyListInitialOffset: Int,
    val playbackSeed: BookReaderEntryPlaybackSeed,
) {
    companion object {
        fun from(readyState: BookReaderEntryReadyState): BookReaderEntryReadyFirstFrame {
            return BookReaderEntryReadyFirstFrame(
                paragraphs = readyState.paragraphs,
                chapterSentences = readyState.chapterSentences,
                firstFrameTarget = readyState.canonicalTarget,
                progressSnapshot = readyState.progressSnapshot,
                lazyListInitialIndex = readyState.lazyListInitialIndex,
                lazyListInitialOffset = readyState.lazyListInitialOffset,
                playbackSeed = readyState.playbackSeed,
            )
        }
    }
}

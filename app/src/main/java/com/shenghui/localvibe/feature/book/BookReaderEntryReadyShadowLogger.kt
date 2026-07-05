package com.shenghui.localvibe.feature.book

import android.util.Log
import com.shenghui.localvibe.feature.book.playback.BookReaderEntryReadyStateWiringAdapter

private const val BOOK_READER_ENTRY_READY_SHADOW_TAG = "LV_BOOK_FORMAL"

internal fun logBookReaderEntryReadyShadow(
    bookId: String,
    bookTitle: String,
    paragraphs: List<String>,
    restoreSnapshot: BookReaderEntryReadyStateWiringAdapter.RestoreSnapshotInput?,
    chapterIndex: Int,
    chapterTitle: String,
    chapterStartIndex: Int,
    chapterEndExclusive: Int,
    speechRate: Float,
    currentParagraphIndex: Int,
    currentSentenceIndex: Int,
    currentChapterIndex: Int,
    currentListenedTimeLabel: String,
    currentRemainingTimeLabel: String,
) {
    val readyStateResult = BookReaderEntryReadyStateWiringAdapter.load(
        BookReaderEntryReadyStateWiringAdapter.Input(
            bookId = bookId,
            bookTitle = bookTitle,
            paragraphs = paragraphs,
            restoreSnapshot = restoreSnapshot,
            chapterIndex = chapterIndex,
            chapterTitle = chapterTitle,
            chapterStartIndex = chapterStartIndex,
            chapterEndExclusive = chapterEndExclusive,
            speechRate = speechRate,
        )
    )
    val currentStateResult = BookReaderEntryReadyStateWiringAdapter.load(
        BookReaderEntryReadyStateWiringAdapter.Input(
            bookId = bookId,
            bookTitle = bookTitle,
            paragraphs = paragraphs,
            restoreSnapshot = BookReaderEntryReadyStateWiringAdapter.RestoreSnapshotInput(
                paragraphIndex = currentParagraphIndex,
                sentenceIndex = currentSentenceIndex,
            ),
            chapterIndex = chapterIndex,
            chapterTitle = chapterTitle,
            chapterStartIndex = chapterStartIndex,
            chapterEndExclusive = chapterEndExclusive,
            speechRate = speechRate,
        )
    )
    val readyState = readyStateResult.getOrNull()
    if (readyState == null) {
        Log.d(
            BOOK_READER_ENTRY_READY_SHADOW_TAG,
            "phase5b read-only entry ready shadow skipped reason=${readyStateResult.exceptionOrNull()?.message.orEmpty()}"
        )
        return
    }

    val currentState = currentStateResult.getOrNull()
    val canonicalTarget = readyState.canonicalTarget
    val currentTarget = currentState?.canonicalTarget
    val progress = readyState.progressSnapshot
    val playbackSeed = readyState.playbackSeed
    Log.d(
        BOOK_READER_ENTRY_READY_SHADOW_TAG,
        "phase5b read-only entry ready shadow " +
            "currentParagraphIndex=$currentParagraphIndex currentSentenceIndex=$currentSentenceIndex " +
            "currentChapterSentenceIndex=${currentTarget?.chapterSentenceIndex ?: -1} " +
            "canonicalParagraphIndex=${canonicalTarget.paragraphIndex} canonicalSentenceIndex=${canonicalTarget.sentenceIndex} " +
            "canonicalChapterSentenceIndex=${canonicalTarget.chapterSentenceIndex} " +
            "playbackParagraphIndex=$currentParagraphIndex " +
            "playbackSentenceIndex=$currentSentenceIndex " +
            "playbackChapterIndex=$currentChapterIndex " +
            "seedParagraphIndex=${playbackSeed.paragraphIndex} seedSentenceIndex=${playbackSeed.sentenceIndex} " +
            "progressValue=${progress.progressValue} progressMax=${progress.progressMaxValue} " +
            "progressChapterSentenceIndex=${progress.chapterSentenceIndex} total=${progress.totalChapterSentenceCount} " +
            "readyListened=${progress.listenedTimeLabel} readyRemaining=${progress.remainingTimeLabel} " +
            "currentListened=$currentListenedTimeLabel currentRemaining=$currentRemainingTimeLabel " +
            "lazyListInitialIndex=${readyState.lazyListInitialIndex} lazyListInitialOffset=${readyState.lazyListInitialOffset}"
    )
}

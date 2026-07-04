package com.shenghui.localvibe.feature.book.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class BookReaderEntryReadyStateWiringAdapterTest {
    @Test
    fun adapterOutputsReadyStateFromLoadedParagraphsAndRestoreSnapshot() {
        val state = BookReaderEntryReadyStateWiringAdapter.load(
            input(
                restoreSnapshot = restoreSnapshot(paragraphIndex = 2, sentenceIndex = 1),
            )
        ).getOrThrow()

        assertEquals(2, state.canonicalTarget.paragraphIndex)
        assertEquals(1, state.canonicalTarget.sentenceIndex)
        assertEquals(4, state.canonicalTarget.chapterSentenceIndex)
        assertEquals(5, state.lazyListInitialIndex)
    }

    @Test
    fun firstFrameProgressAndPlaybackTargetsShareCanonicalTarget() {
        val state = BookReaderEntryReadyStateWiringAdapter.load(
            input(
                restoreSnapshot = restoreSnapshot(paragraphIndex = 2, sentenceIndex = 1),
            )
        ).getOrThrow()

        assertEquals(state.canonicalTarget.chapterSentenceIndex, state.progressSnapshot.chapterSentenceIndex)
        assertEquals(state.canonicalTarget.paragraphIndex, state.playbackSeed.paragraphIndex)
        assertEquals(state.canonicalTarget.sentenceIndex, state.playbackSeed.sentenceIndex)
        assertEquals("第五句。", state.playbackSeed.sentenceText)
    }

    @Test
    fun staleCachedViewportDoesNotOverrideCanonicalTarget() {
        val state = BookReaderEntryReadyStateWiringAdapter.load(
            input(
                restoreSnapshot = restoreSnapshot(
                    paragraphIndex = 2,
                    sentenceIndex = 1,
                    cachedViewport = BookReaderEntryReadyStateWiringAdapter.CachedViewportInput(
                        firstVisibleParagraphIndex = 0,
                        firstVisibleSentenceIndex = 0,
                        firstVisibleChapterSentenceIndex = 0,
                        firstVisibleItemScrollOffset = 96,
                    ),
                ),
            )
        ).getOrThrow()

        assertEquals(2, state.canonicalTarget.paragraphIndex)
        assertEquals(1, state.canonicalTarget.sentenceIndex)
        assertEquals(4, state.canonicalTarget.chapterSentenceIndex)
        assertEquals(5, state.lazyListInitialIndex)
        assertEquals(0, state.lazyListInitialOffset)
    }

    @Test
    fun matchingCachedViewportOnlyRestoresOffset() {
        val state = BookReaderEntryReadyStateWiringAdapter.load(
            input(
                restoreSnapshot = restoreSnapshot(
                    paragraphIndex = 2,
                    sentenceIndex = 1,
                    cachedViewport = BookReaderEntryReadyStateWiringAdapter.CachedViewportInput(
                        firstVisibleParagraphIndex = 2,
                        firstVisibleSentenceIndex = 1,
                        firstVisibleChapterSentenceIndex = 4,
                        firstVisibleItemScrollOffset = 44,
                    ),
                ),
            )
        ).getOrThrow()

        assertEquals(4, state.canonicalTarget.chapterSentenceIndex)
        assertEquals(5, state.lazyListInitialIndex)
        assertEquals(44, state.lazyListInitialOffset)
        assertEquals(state.canonicalTarget.paragraphIndex, state.playbackSeed.paragraphIndex)
        assertEquals(state.canonicalTarget.sentenceIndex, state.playbackSeed.sentenceIndex)
    }

    @Test
    fun missingRestoreTargetFallsBackToStart() {
        val state = BookReaderEntryReadyStateWiringAdapter.load(
            input(restoreSnapshot = null)
        ).getOrThrow()

        assertEquals(0, state.canonicalTarget.paragraphIndex)
        assertEquals(0, state.canonicalTarget.sentenceIndex)
        assertEquals(0, state.canonicalTarget.chapterSentenceIndex)
        assertEquals(1, state.lazyListInitialIndex)
        assertEquals(0, state.playbackSeed.paragraphIndex)
        assertEquals(0, state.playbackSeed.sentenceIndex)
    }

    private fun input(
        restoreSnapshot: BookReaderEntryReadyStateWiringAdapter.RestoreSnapshotInput?,
    ): BookReaderEntryReadyStateWiringAdapter.Input {
        return BookReaderEntryReadyStateWiringAdapter.Input(
            bookId = "book://demo",
            bookTitle = "Demo Book",
            paragraphs = paragraphs(),
            restoreSnapshot = restoreSnapshot,
            chapterTitle = "正文",
            speechRate = 1f,
        )
    }

    private fun restoreSnapshot(
        paragraphIndex: Int,
        sentenceIndex: Int,
        cachedViewport: BookReaderEntryReadyStateWiringAdapter.CachedViewportInput? = null,
    ): BookReaderEntryReadyStateWiringAdapter.RestoreSnapshotInput {
        return BookReaderEntryReadyStateWiringAdapter.RestoreSnapshotInput(
            paragraphIndex = paragraphIndex,
            sentenceIndex = sentenceIndex,
            cachedViewport = cachedViewport,
        )
    }

    private fun paragraphs(): List<String> = listOf(
        "第一句。第二句。",
        "第三句。",
        "第四句。第五句。",
    )
}

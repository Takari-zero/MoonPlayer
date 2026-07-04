package com.shenghui.localvibe.feature.book.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class BookReaderEntryReadyStateFactoryTest {
    @Test
    fun createsReadyStateFromLoadedParagraphsAndSavedTarget() {
        val state = BookReaderEntryReadyStateFactory.create(
            BookReaderEntryReadyStateFactory.Input(
                bookId = "book://demo",
                bookTitle = "Demo Book",
                paragraphs = paragraphs(),
                savedParagraphIndex = 2,
                savedSentenceIndex = 1,
                chapterTitle = "正文",
                speechRate = 1f,
            )
        )

        assertEquals(2, state.canonicalTarget.paragraphIndex)
        assertEquals(1, state.canonicalTarget.sentenceIndex)
        assertEquals(4, state.canonicalTarget.chapterSentenceIndex)
        assertEquals(4, state.progressSnapshot.chapterSentenceIndex)
        assertEquals(5, state.lazyListInitialIndex)
        assertEquals(0, state.lazyListInitialOffset)
        assertEquals(2, state.playbackSeed.paragraphIndex)
        assertEquals(1, state.playbackSeed.sentenceIndex)
        assertEquals("第五句。", state.playbackSeed.sentenceText)
    }

    @Test
    fun staleCachedViewportDoesNotOverrideCanonicalPlaybackTarget() {
        val state = BookReaderEntryReadyStateFactory.create(
            BookReaderEntryReadyStateFactory.Input(
                bookId = "book://demo",
                bookTitle = "Demo Book",
                paragraphs = paragraphs(),
                savedParagraphIndex = 2,
                savedSentenceIndex = 1,
                chapterTitle = "正文",
                cachedViewport = BookReaderEntryReadyStateFactory.CachedViewport(
                    firstVisibleParagraphIndex = 0,
                    firstVisibleSentenceIndex = 0,
                    firstVisibleChapterSentenceIndex = 0,
                    firstVisibleItemScrollOffset = 88,
                ),
            )
        )

        assertEquals(2, state.canonicalTarget.paragraphIndex)
        assertEquals(1, state.canonicalTarget.sentenceIndex)
        assertEquals(4, state.canonicalTarget.chapterSentenceIndex)
        assertEquals(5, state.lazyListInitialIndex)
        assertEquals(0, state.lazyListInitialOffset)
        assertEquals(2, state.playbackSeed.paragraphIndex)
        assertEquals(1, state.playbackSeed.sentenceIndex)
    }

    @Test
    fun matchingCachedViewportOnlyRestoresOffsetWithoutChangingTarget() {
        val state = BookReaderEntryReadyStateFactory.create(
            BookReaderEntryReadyStateFactory.Input(
                bookId = "book://demo",
                bookTitle = "Demo Book",
                paragraphs = paragraphs(),
                savedParagraphIndex = 2,
                savedSentenceIndex = 1,
                chapterTitle = "正文",
                cachedViewport = BookReaderEntryReadyStateFactory.CachedViewport(
                    firstVisibleParagraphIndex = 2,
                    firstVisibleSentenceIndex = 1,
                    firstVisibleChapterSentenceIndex = 4,
                    firstVisibleItemScrollOffset = 24,
                ),
            )
        )

        assertEquals(4, state.canonicalTarget.chapterSentenceIndex)
        assertEquals(5, state.lazyListInitialIndex)
        assertEquals(24, state.lazyListInitialOffset)
        assertEquals(4, state.progressSnapshot.progressValue.toInt())
        assertEquals(4, state.playbackSeed.sentenceIndex + 3)
    }

    @Test
    fun outOfRangeSavedTargetFallsBackToNearestReadableSentence() {
        val state = BookReaderEntryReadyStateFactory.create(
            BookReaderEntryReadyStateFactory.Input(
                bookId = "book://demo",
                bookTitle = "Demo Book",
                paragraphs = paragraphs(),
                savedParagraphIndex = 50,
                savedSentenceIndex = 9,
                chapterTitle = "正文",
            )
        )

        assertEquals(2, state.canonicalTarget.paragraphIndex)
        assertEquals(1, state.canonicalTarget.sentenceIndex)
        assertEquals(4, state.canonicalTarget.chapterSentenceIndex)
        assertEquals("第五句。", state.playbackSeed.sentenceText)
    }

    private fun paragraphs(): List<String> = listOf(
        "第一句。第二句。",
        "第三句。",
        "第四句。第五句。",
    )
}

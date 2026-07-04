package com.shenghui.localvibe.feature.book.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookReaderEntryReadyStateLoaderTest {
    @Test
    fun loadReturnsReadyStateWhenParagraphsAndRestoreTargetAreReady() {
        val loader = loader(
            restoreTarget = restoreTarget(paragraphIndex = 2, sentenceIndex = 1),
        )

        val state = loader.load(request()).getOrThrow()

        assertEquals(2, state.canonicalTarget.paragraphIndex)
        assertEquals(1, state.canonicalTarget.sentenceIndex)
        assertEquals(4, state.canonicalTarget.chapterSentenceIndex)
        assertEquals(5, state.lazyListInitialIndex)
        assertEquals(2, state.playbackSeed.paragraphIndex)
        assertEquals(1, state.playbackSeed.sentenceIndex)
    }

    @Test
    fun loadFallsBackToStartWhenRestoreTargetIsMissing() {
        val loader = loader(restoreTarget = null)

        val state = loader.load(request()).getOrThrow()

        assertEquals(0, state.canonicalTarget.paragraphIndex)
        assertEquals(0, state.canonicalTarget.sentenceIndex)
        assertEquals(0, state.canonicalTarget.chapterSentenceIndex)
        assertEquals(1, state.lazyListInitialIndex)
        assertEquals(0, state.playbackSeed.paragraphIndex)
        assertEquals(0, state.playbackSeed.sentenceIndex)
    }

    @Test
    fun staleCachedViewportDoesNotOverrideCanonicalPlayTarget() {
        val loader = loader(
            restoreTarget = restoreTarget(paragraphIndex = 2, sentenceIndex = 1),
            cachedViewport = BookReaderEntryReadyStateFactory.CachedViewport(
                firstVisibleParagraphIndex = 0,
                firstVisibleSentenceIndex = 0,
                firstVisibleChapterSentenceIndex = 0,
                firstVisibleItemScrollOffset = 88,
            ),
        )

        val state = loader.load(request()).getOrThrow()

        assertEquals(2, state.canonicalTarget.paragraphIndex)
        assertEquals(1, state.canonicalTarget.sentenceIndex)
        assertEquals(4, state.canonicalTarget.chapterSentenceIndex)
        assertEquals(5, state.lazyListInitialIndex)
        assertEquals(0, state.lazyListInitialOffset)
        assertEquals(state.canonicalTarget.paragraphIndex, state.playbackSeed.paragraphIndex)
        assertEquals(state.canonicalTarget.sentenceIndex, state.playbackSeed.sentenceIndex)
    }

    @Test
    fun matchingCachedViewportOnlyRestoresOffsetAfterCanonicalTargetMatches() {
        val loader = loader(
            restoreTarget = restoreTarget(paragraphIndex = 2, sentenceIndex = 1),
            cachedViewport = BookReaderEntryReadyStateFactory.CachedViewport(
                firstVisibleParagraphIndex = 2,
                firstVisibleSentenceIndex = 1,
                firstVisibleChapterSentenceIndex = 4,
                firstVisibleItemScrollOffset = 32,
            ),
        )

        val state = loader.load(request()).getOrThrow()

        assertEquals(2, state.canonicalTarget.paragraphIndex)
        assertEquals(1, state.canonicalTarget.sentenceIndex)
        assertEquals(4, state.progressSnapshot.chapterSentenceIndex)
        assertEquals(5, state.lazyListInitialIndex)
        assertEquals(32, state.lazyListInitialOffset)
    }

    @Test
    fun loadReturnsFailureWhenParagraphSourceFails() {
        val loader = BookReaderEntryReadyStateLoader(
            paragraphSource = BookReaderEntryParagraphSource { Result.failure(IllegalStateException("read failed")) },
            restoreSource = BookReaderEntryRestoreSource { null },
        )

        val result = loader.load(request())

        assertTrue(result.isFailure)
        assertEquals("read failed", result.exceptionOrNull()?.message)
    }

    private fun loader(
        restoreTarget: BookReaderEntryRestoreTarget?,
        cachedViewport: BookReaderEntryReadyStateFactory.CachedViewport? = null,
    ): BookReaderEntryReadyStateLoader {
        return BookReaderEntryReadyStateLoader(
            paragraphSource = BookReaderEntryParagraphSource { Result.success(paragraphs()) },
            restoreSource = BookReaderEntryRestoreSource {
                restoreTarget?.copy(cachedViewport = cachedViewport)
            },
        )
    }

    private fun request(): BookReaderEntryLoadRequest {
        return BookReaderEntryLoadRequest(
            bookId = "book://demo",
            bookTitle = "Demo Book",
            chapterTitle = "正文",
            speechRate = 1f,
        )
    }

    private fun restoreTarget(
        paragraphIndex: Int,
        sentenceIndex: Int,
    ): BookReaderEntryRestoreTarget {
        return BookReaderEntryRestoreTarget(
            paragraphIndex = paragraphIndex,
            sentenceIndex = sentenceIndex,
        )
    }

    private fun paragraphs(): List<String> = listOf(
        "第一句。第二句。",
        "第三句。",
        "第四句。第五句。",
    )
}

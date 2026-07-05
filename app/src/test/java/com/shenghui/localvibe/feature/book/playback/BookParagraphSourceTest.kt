package com.shenghui.localvibe.feature.book.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookParagraphSourceTest {
    @Test
    fun paragraphSourceSuccessReturnsParagraphs() {
        val key = BookDocumentCacheKey("file://demo.txt")
        val source = BookParagraphSource { request ->
            BookParagraphLoadResult.success(
                key = request.key,
                paragraphs = listOf("Chapter 1", "First sentence."),
            )
        }

        val result = source.load(BookParagraphLoadRequest(key))

        assertTrue(result.isSuccess)
        assertEquals(listOf("Chapter 1", "First sentence."), result.paragraphsOrNull())
    }

    @Test
    fun emptyParagraphsReturnFailedRepositoryResult() {
        val key = BookDocumentCacheKey("file://empty.txt")
        val result = BookParagraphLoadResult.success(
            key = key,
            paragraphs = emptyList(),
        )

        assertTrue(result is BookParagraphLoadResult.Empty)
        assertFalse(result.isSuccess)
        assertTrue(result.errorOrNull() is IllegalArgumentException)
    }

    @Test
    fun exceptionReturnsFailedResult() {
        val key = BookDocumentCacheKey("file://broken.txt")
        val error = IllegalStateException("read failed")
        val result = BookParagraphLoadResult.failed(
            key = key,
            message = "read failed",
            cause = error,
        )

        assertTrue(result is BookParagraphLoadResult.Failed)
        assertFalse(result.isSuccess)
        assertEquals(error, result.errorOrNull())
    }

    @Test
    fun repositoryCanConsumeParagraphSourceResult() {
        val key = BookDocumentCacheKey("file://demo.txt")
        val source = BookParagraphSource { request ->
            BookParagraphLoadResult.success(
                key = request.key,
                paragraphs = listOf("Chapter 1", "First sentence."),
            )
        }
        val repository = BookDocumentRepository(
            paragraphSource = source,
            clock = IncrementingBookDocumentClock(start = 100L),
        )

        val entry = repository.preload(
            BookDocumentPreloadRequest(
                key = key,
                title = "Demo Book",
                savedParagraphIndex = 1,
                savedSentenceIndex = 0,
                chapterTitle = "Chapter 1",
            )
        ).getOrThrow()

        assertTrue(entry.isReady)
        assertTrue(entry.snapshotOrNull()?.firstFrameTargetEqualsPlaybackTarget == true)
    }
}

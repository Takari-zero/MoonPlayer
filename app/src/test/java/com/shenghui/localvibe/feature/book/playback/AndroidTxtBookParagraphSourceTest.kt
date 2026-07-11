package com.shenghui.localvibe.feature.book.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidTxtBookParagraphSourceTest {
    @Test
    fun readerSuccessReturnsParagraphSourceSuccess() {
        val source = AndroidTxtBookParagraphSource(
            reader = AndroidTxtBookParagraphReader { bookId ->
                assertEquals("content://book/demo.txt", bookId)
                Result.success(listOf("Chapter 1", "First sentence."))
            }
        )

        val result = source.load(BookParagraphLoadRequest(BookDocumentCacheKey("content://book/demo.txt")))

        assertTrue(result.isSuccess)
        assertEquals(listOf("Chapter 1", "First sentence."), result.paragraphsOrNull())
    }

    @Test
    fun readerEmptyResultIsNotReady() {
        val source = AndroidTxtBookParagraphSource(
            reader = AndroidTxtBookParagraphReader { Result.success(emptyList()) }
        )

        val result = source.load(BookParagraphLoadRequest(BookDocumentCacheKey("content://book/empty.txt")))

        assertTrue(result is BookParagraphLoadResult.Empty)
        assertFalse(result.isSuccess)
        assertTrue(result.errorOrNull() is IllegalArgumentException)
    }

    @Test
    fun readerExceptionReturnsFailedResult() {
        val error = IllegalStateException("read failed")
        val source = AndroidTxtBookParagraphSource(
            reader = AndroidTxtBookParagraphReader { Result.failure(error) }
        )

        val result = source.load(BookParagraphLoadRequest(BookDocumentCacheKey("content://book/broken.txt")))

        assertTrue(result is BookParagraphLoadResult.Failed)
        assertFalse(result.isSuccess)
        assertEquals(error, result.errorOrNull())
    }

    @Test
    fun sourceResultCanBeConsumedByBookDocumentRepository() {
        val source = AndroidTxtBookParagraphSource(
            reader = AndroidTxtBookParagraphReader {
                Result.success(listOf("Chapter 1", "First sentence."))
            }
        )
        val repository = BookDocumentRepository(
            paragraphSource = source,
            clock = IncrementingBookDocumentClock(start = 100L),
        )
        val request = BookDocumentPreloadRequest(
            key = BookDocumentCacheKey("content://book/demo.txt"),
            title = "Demo Book",
            savedParagraphIndex = 1,
            savedSentenceIndex = 0,
            chapterTitle = "Chapter 1",
            chapterStartIndex = 0,
            speechRate = 1f,
        )

        val result = repository.preload(request)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isReady)
        assertTrue(result.getOrThrow().snapshotOrNull()?.firstFrameTargetEqualsPlaybackTarget == true)
    }
}

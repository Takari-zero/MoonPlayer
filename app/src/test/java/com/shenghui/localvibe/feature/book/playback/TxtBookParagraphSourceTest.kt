package com.shenghui.localvibe.feature.book.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TxtBookParagraphSourceTest {
    @Test
    fun loaderSuccessReturnsParagraphs() {
        val source = TxtBookParagraphSource(
            loader = TxtBookParagraphLoader { bookId ->
                assertEquals("file://demo.txt", bookId)
                Result.success(listOf("Chapter 1", "First sentence."))
            }
        )

        val result = source.load(BookParagraphLoadRequest(BookDocumentCacheKey("file://demo.txt")))

        assertTrue(result.isSuccess)
        assertEquals(listOf("Chapter 1", "First sentence."), result.paragraphsOrNull())
    }

    @Test
    fun loaderEmptyResultIsNotReady() {
        val source = TxtBookParagraphSource(
            loader = TxtBookParagraphLoader { Result.success(emptyList()) }
        )

        val result = source.load(BookParagraphLoadRequest(BookDocumentCacheKey("file://empty.txt")))

        assertTrue(result is BookParagraphLoadResult.Empty)
        assertFalse(result.isSuccess)
        assertTrue(result.errorOrNull() is IllegalArgumentException)
    }

    @Test
    fun loaderFailureReturnsFailedResult() {
        val error = IllegalStateException("read failed")
        val source = TxtBookParagraphSource(
            loader = TxtBookParagraphLoader { Result.failure(error) }
        )

        val result = source.load(BookParagraphLoadRequest(BookDocumentCacheKey("file://broken.txt")))

        assertTrue(result is BookParagraphLoadResult.Failed)
        assertFalse(result.isSuccess)
        assertEquals(error, result.errorOrNull())
    }
}

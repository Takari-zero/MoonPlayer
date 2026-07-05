package com.shenghui.localvibe.feature.book.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookReaderPreloadManagerTest {
    @Test
    fun preloadSuccessReturnsReadyStatusAndCachesEntry() {
        val repository = repository(paragraphs = listOf("Chapter 1", "First sentence."))
        val manager = BookReaderPreloadManager(repository)
        val request = request()

        val result = manager.preload(request)

        assertTrue(result.isReady)
        assertEquals(BookDocumentPreloadStatus.Ready, result.status)
        assertTrue(repository.get(request.key)?.isReady == true)
        assertTrue(result.readySnapshot?.firstFrameTargetEqualsPlaybackTarget == true)
    }

    @Test
    fun preloadFailureReturnsFailedStatusWithoutReadySnapshot() {
        val manager = BookReaderPreloadManager(
            repository = repository(
                paragraphSource = BookParagraphSource { request ->
                    BookParagraphLoadResult.failed(
                        key = request.key,
                        message = "parse failed",
                        cause = IllegalStateException("parse failed"),
                    )
                }
            )
        )

        val result = manager.preload(request())

        assertFalse(result.isReady)
        assertEquals(BookDocumentPreloadStatus.Failed, result.status)
        assertTrue(result.readySnapshot == null)
    }

    @Test
    fun preloadManySkipsDuplicateBookKeys() {
        var loadCalls = 0
        val repository = repository(
            paragraphSource = BookParagraphSource { request ->
                loadCalls++
                BookParagraphLoadResult.success(
                    key = request.key,
                    paragraphs = listOf("Chapter 1", "First sentence."),
                )
            }
        )
        val manager = BookReaderPreloadManager(repository)
        val request = request()

        val results = manager.preloadAll(listOf(request, request))

        assertEquals(1, loadCalls)
        assertEquals(1, results.size)
        assertTrue(results.single().isReady)
    }

    private fun repository(
        paragraphs: List<String> = listOf("Chapter 1", "First sentence."),
        paragraphSource: BookParagraphSource = BookParagraphSource { request ->
            BookParagraphLoadResult.success(
                key = request.key,
                paragraphs = paragraphs,
            )
        },
    ): BookDocumentRepository {
        return BookDocumentRepository(
            paragraphSource = paragraphSource,
            clock = IncrementingBookDocumentClock(start = 100L),
        )
    }

    private fun request(): BookDocumentPreloadRequest {
        return BookDocumentPreloadRequest(
            key = BookDocumentCacheKey("file://demo.txt"),
            title = "Demo Book",
            savedParagraphIndex = 1,
            savedSentenceIndex = 0,
            chapterTitle = "Chapter 1",
            chapterStartIndex = 0,
            speechRate = 1f,
        )
    }
}

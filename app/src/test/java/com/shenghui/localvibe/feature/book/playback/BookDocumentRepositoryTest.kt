package com.shenghui.localvibe.feature.book.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BookDocumentRepositoryTest {
    @Test
    fun preloadSuccessCachesReadyDocument() {
        val repository = repository(paragraphs = listOf("Chapter 1", "First sentence."))
        val request = request()

        val result = repository.preload(request)

        assertTrue(result.isSuccess)
        val entry = result.getOrThrow()
        assertTrue(entry.isReady)
        assertEquals(BookDocumentPreloadStatus.Ready, entry.preloadStatus)
        assertEquals(listOf("Chapter 1", "First sentence."), entry.snapshotOrNull()?.document?.paragraphs)
        assertEquals(0, entry.snapshotOrNull()?.canonicalTarget?.chapterSentenceIndex)
        assertTrue(entry.snapshotOrNull()?.firstFrameTargetEqualsPlaybackTarget == true)
        assertSame(entry, repository.get(request.key))
    }

    @Test
    fun repeatedPreloadUsesReadyCacheWithoutParsingAgain() {
        var loadCalls = 0
        val repository = repository(
            paragraphSource = BookDocumentParagraphSource {
                loadCalls++
                Result.success(listOf("Chapter 1", "First sentence."))
            }
        )
        val request = request()

        val first = repository.preload(request).getOrThrow()
        val second = repository.preload(request).getOrThrow()

        assertEquals(1, loadCalls)
        assertSame(first, second)
        assertTrue(second.isReady)
    }

    @Test
    fun failedPreloadIsCachedAsFailedAndNotReady() {
        val repository = repository(
            paragraphSource = BookDocumentParagraphSource {
                Result.failure(IllegalStateException("parse failed"))
            }
        )
        val request = request()

        val result = repository.preload(request)

        assertTrue(result.isFailure)
        val entry = repository.get(request.key)
        assertTrue(entry?.state is BookDocumentCacheState.Failed)
        assertFalse(entry?.isReady == true)
    }

    @Test
    fun emptyDocumentIsCachedAsFailedAndNotReady() {
        val repository = repository(paragraphs = emptyList())
        val request = request()

        val result = repository.preload(request)

        assertTrue(result.isFailure)
        val entry = repository.get(request.key)
        assertTrue(entry?.state is BookDocumentCacheState.Failed)
        assertFalse(entry?.isReady == true)
    }

    @Test
    fun invalidateMarksReadyCacheStale() {
        val repository = repository(paragraphs = listOf("Chapter 1", "First sentence."))
        val request = request()
        repository.preload(request).getOrThrow()

        val stale = repository.invalidate(request.key, nowMillis = 300L)

        assertEquals(BookDocumentPreloadStatus.Stale, stale?.preloadStatus)
        assertFalse(stale?.isReady == true)
        assertEquals(BookDocumentPreloadStatus.Stale, repository.get(request.key)?.preloadStatus)
    }

    private fun repository(
        paragraphs: List<String> = listOf("Chapter 1", "First sentence."),
        paragraphSource: BookDocumentParagraphSource = BookDocumentParagraphSource { Result.success(paragraphs) },
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

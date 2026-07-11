package com.shenghui.localvibe.feature.book.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
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
    fun preloadSuccessStoresReadyStateForLaterClick() {
        val store = BookReaderEntryReadyStateStore()
        val repository = repository(paragraphs = listOf("Chapter 1", "First sentence."))
        val manager = BookReaderPreloadManager(
            repository = repository,
            readyStateStore = store,
        )
        val request = request()

        val result = manager.preload(request)
        val readyState = store.getReadyState(request.key)

        assertTrue(result.isReady)
        assertTrue(store.get(request.key).isReady)
        assertTrue(readyState != null)
        assertEquals(result.readySnapshot?.canonicalTarget, readyState?.canonicalTarget)
        assertEquals(readyState?.canonicalTarget?.paragraphIndex, readyState?.playbackSeed?.paragraphIndex)
        assertEquals(readyState?.canonicalTarget?.sentenceIndex, readyState?.playbackSeed?.sentenceIndex)
        assertEquals(readyState?.canonicalTarget?.chapterSentenceIndex, readyState?.progressSnapshot?.chapterSentenceIndex)
    }

    @Test
    fun preloadFailureStoresFailedStateWithoutReadyState() {
        val store = BookReaderEntryReadyStateStore()
        val manager = BookReaderPreloadManager(
            repository = repository(
                paragraphSource = BookParagraphSource { request ->
                    BookParagraphLoadResult.failed(
                        key = request.key,
                        message = "parse failed",
                        cause = IllegalStateException("parse failed"),
                    )
                }
            ),
            readyStateStore = store,
        )
        val request = request()

        val result = manager.preload(request)

        assertFalse(result.isReady)
        assertEquals(BookDocumentPreloadStatus.Failed, result.status)
        assertTrue(store.get(request.key).state is BookReaderEntryReadyStateStoreState.Failed)
        assertFalse(store.get(request.key).isReady)
        assertTrue(store.getReadyState(request.key) == null)
    }

    @Test
    fun emptyDocumentDoesNotStoreReadyState() {
        val store = BookReaderEntryReadyStateStore()
        val manager = BookReaderPreloadManager(
            repository = repository(paragraphs = emptyList()),
            readyStateStore = store,
        )
        val request = request()

        val result = manager.preload(request)

        assertFalse(result.isReady)
        assertEquals(BookDocumentPreloadStatus.Failed, result.status)
        assertFalse(store.get(request.key).isReady)
        assertTrue(store.getReadyState(request.key) == null)
    }

    @Test
    fun repeatedPreloadHitsCacheAndKeepsReadyStateWithoutReparsing() {
        var loadCalls = 0
        val store = BookReaderEntryReadyStateStore()
        val repository = repository(
            paragraphSource = BookParagraphSource { request ->
                loadCalls++
                BookParagraphLoadResult.success(
                    key = request.key,
                    paragraphs = listOf("Chapter 1", "First sentence."),
                )
            }
        )
        val manager = BookReaderPreloadManager(
            repository = repository,
            readyStateStore = store,
        )
        val request = request()

        val first = manager.preload(request)
        val firstReadyState = store.getReadyState(request.key)
        val second = manager.preload(request)
        val secondReadyState = store.getReadyState(request.key)

        assertEquals(1, loadCalls)
        assertTrue(first.isReady)
        assertTrue(second.isReady)
        assertSame(firstReadyState, secondReadyState)
        assertTrue(secondReadyState != null)
    }

    @Test
    fun invalidateClearsDocumentCacheAndReadyStateStore() {
        val store = BookReaderEntryReadyStateStore()
        val repository = repository(paragraphs = listOf("Chapter 1", "First sentence."))
        val manager = BookReaderPreloadManager(
            repository = repository,
            readyStateStore = store,
        )
        val request = request()
        manager.preload(request)

        manager.invalidate(request.key, nowMillis = 300L)

        assertEquals(BookDocumentPreloadStatus.Stale, repository.get(request.key)?.preloadStatus)
        assertFalse(repository.get(request.key)?.isReady == true)
        assertTrue(store.get(request.key).state is BookReaderEntryReadyStateStoreState.Stale)
        assertTrue(store.getReadyState(request.key) == null)
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

package com.shenghui.localvibe.feature.book.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BookReaderPreloadRuntimeFactoryTest {
    @Test
    fun factoryConstructionDoesNotCreateSourceOrReadParagraphs() {
        var sourceCreations = 0
        var readCalls = 0

        BookReaderPreloadRuntimeFactory(
            paragraphSourceFactory = {
                sourceCreations++
                BookParagraphSource { request ->
                    readCalls++
                    BookParagraphLoadResult.success(
                        key = request.key,
                        paragraphs = listOf("Chapter 1", "First sentence."),
                    )
                }
            },
            clock = IncrementingBookDocumentClock(start = 100L),
        )

        assertEquals(0, sourceCreations)
        assertEquals(0, readCalls)
    }

    @Test
    fun createRuntimeComposesDependenciesWithoutReadingParagraphs() {
        var sourceCreations = 0
        var readCalls = 0
        val store = BookReaderEntryReadyStateStore()
        val factory = BookReaderPreloadRuntimeFactory(
            paragraphSourceFactory = {
                sourceCreations++
                BookParagraphSource { request ->
                    readCalls++
                    BookParagraphLoadResult.success(
                        key = request.key,
                        paragraphs = listOf("Chapter 1", "First sentence."),
                    )
                }
            },
            readyStateStoreFactory = { store },
            clock = IncrementingBookDocumentClock(start = 100L),
        )

        val runtime = factory.create()

        val request = request()
        val plan = runtime.bookshelfPreloadCoordinator.plan(
            candidates = listOf(BookReaderBookshelfPreloadCandidate(request)),
            recentBookKeys = listOf(request.key),
            currentBookKey = null,
            maxPreloadCount = 1,
        )

        assertEquals(1, sourceCreations)
        assertEquals(0, readCalls)
        assertSame(store, runtime.readyStateStore)
        assertEquals(listOf(request.key), plan.map { it.request.key })
    }

    @Test
    fun explicitPreloadReadsSourceAndStoresReadyState() {
        var readCalls = 0
        val runtime = BookReaderPreloadRuntimeFactory(
            paragraphSourceFactory = {
                BookParagraphSource { request ->
                    readCalls++
                    BookParagraphLoadResult.success(
                        key = request.key,
                        paragraphs = listOf("Chapter 1", "First sentence."),
                    )
                }
            },
            clock = IncrementingBookDocumentClock(start = 100L),
        ).create()
        val request = request()

        val result = runtime.preloadManager.preload(request)

        assertEquals(1, readCalls)
        assertTrue(result.isReady)
        assertTrue(runtime.readyStateStore.get(request.key).isReady)
        assertTrue(runtime.readyStateStore.getReadyState(request.key) != null)
    }

    @Test
    fun failedPreloadDoesNotMarkReady() {
        val runtime = BookReaderPreloadRuntimeFactory(
            paragraphSourceFactory = {
                BookParagraphSource { request ->
                    BookParagraphLoadResult.failed(
                        key = request.key,
                        message = "parse failed",
                        cause = IllegalStateException("parse failed"),
                    )
                }
            },
            clock = IncrementingBookDocumentClock(start = 100L),
        ).create()
        val request = request()

        val result = runtime.preloadManager.preload(request)

        assertFalse(result.isReady)
        assertFalse(runtime.readyStateStore.get(request.key).isReady)
        assertTrue(runtime.readyStateStore.getReadyState(request.key) == null)
    }

    @Test
    fun repeatedPreloadCanHitRuntimeCacheAndStore() {
        var readCalls = 0
        val runtime = BookReaderPreloadRuntimeFactory(
            paragraphSourceFactory = {
                BookParagraphSource { request ->
                    readCalls++
                    BookParagraphLoadResult.success(
                        key = request.key,
                        paragraphs = listOf("Chapter 1", "First sentence."),
                    )
                }
            },
            clock = IncrementingBookDocumentClock(start = 100L),
        ).create()
        val request = request()

        runtime.preloadManager.preload(request)
        val firstReadyState = runtime.readyStateStore.getReadyState(request.key)
        runtime.preloadManager.preload(request)
        val secondReadyState = runtime.readyStateStore.getReadyState(request.key)

        assertEquals(1, readCalls)
        assertSame(firstReadyState, secondReadyState)
        assertTrue(secondReadyState != null)
    }

    private fun request(): BookDocumentPreloadRequest {
        return BookDocumentPreloadRequest(
            key = BookDocumentCacheKey("content://book/demo.txt"),
            title = "Demo Book",
            savedParagraphIndex = 1,
            savedSentenceIndex = 0,
            chapterTitle = "Chapter 1",
            chapterStartIndex = 0,
            speechRate = 1f,
        )
    }
}

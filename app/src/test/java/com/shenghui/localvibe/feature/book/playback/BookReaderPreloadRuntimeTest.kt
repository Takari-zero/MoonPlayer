package com.shenghui.localvibe.feature.book.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BookReaderPreloadRuntimeTest {
    @Test
    fun runtimeComposesRepositoryPreloadManagerStoreAndCoordinator() {
        val paragraphSource = BookParagraphSource { request ->
            BookParagraphLoadResult.success(
                key = request.key,
                paragraphs = listOf("Chapter 1", "First sentence."),
            )
        }
        val store = BookReaderEntryReadyStateStore()

        val runtime = BookReaderPreloadRuntime.create(
            paragraphSource = paragraphSource,
            clock = IncrementingBookDocumentClock(start = 100L),
            readyStateStore = store,
        )
        val request = request()
        val plan = runtime.bookshelfPreloadCoordinator.plan(
            candidates = listOf(BookReaderBookshelfPreloadCandidate(request)),
            recentBookKeys = emptyList(),
            currentBookKey = null,
            maxPreloadCount = 1,
        )

        assertSame(paragraphSource, runtime.paragraphSource)
        assertSame(store, runtime.readyStateStore)
        assertEquals(listOf(request.key), plan.map { it.request.key })
    }

    @Test
    fun constructingRuntimeDoesNotReadParagraphSource() {
        var readCalls = 0
        val paragraphSource = BookParagraphSource { request ->
            readCalls++
            BookParagraphLoadResult.success(
                key = request.key,
                paragraphs = listOf("Chapter 1", "First sentence."),
            )
        }

        BookReaderPreloadRuntime.create(
            paragraphSource = paragraphSource,
            clock = IncrementingBookDocumentClock(start = 100L),
        )

        assertEquals(0, readCalls)
    }

    @Test
    fun explicitPreloadReadsSourceAndStoresReadyState() {
        var readCalls = 0
        val runtime = BookReaderPreloadRuntime.create(
            paragraphSource = BookParagraphSource { request ->
                readCalls++
                BookParagraphLoadResult.success(
                    key = request.key,
                    paragraphs = listOf("Chapter 1", "First sentence."),
                )
            },
            clock = IncrementingBookDocumentClock(start = 100L),
        )
        val request = request()

        val result = runtime.preloadManager.preload(request)

        assertEquals(1, readCalls)
        assertTrue(result.isReady)
        assertTrue(runtime.readyStateStore.get(request.key).isReady)
        assertTrue(runtime.readyStateStore.getReadyState(request.key) != null)
    }

    @Test
    fun failedPreloadDoesNotMarkReadyStateReady() {
        val runtime = BookReaderPreloadRuntime.create(
            paragraphSource = BookParagraphSource { request ->
                BookParagraphLoadResult.failed(
                    key = request.key,
                    message = "parse failed",
                    cause = IllegalStateException("parse failed"),
                )
            },
            clock = IncrementingBookDocumentClock(start = 100L),
        )
        val request = request()

        val result = runtime.preloadManager.preload(request)

        assertFalse(result.isReady)
        assertFalse(runtime.readyStateStore.get(request.key).isReady)
        assertTrue(runtime.readyStateStore.getReadyState(request.key) == null)
    }

    @Test
    fun repeatedPreloadUsesCacheAndKeepsSameReadyState() {
        var readCalls = 0
        val runtime = BookReaderPreloadRuntime.create(
            paragraphSource = BookParagraphSource { request ->
                readCalls++
                BookParagraphLoadResult.success(
                    key = request.key,
                    paragraphs = listOf("Chapter 1", "First sentence."),
                )
            },
            clock = IncrementingBookDocumentClock(start = 100L),
        )
        val request = request()

        runtime.preloadManager.preload(request)
        val firstReadyState = runtime.readyStateStore.getReadyState(request.key)
        runtime.preloadManager.preload(request)
        val secondReadyState = runtime.readyStateStore.getReadyState(request.key)

        assertEquals(1, readCalls)
        assertSame(firstReadyState, secondReadyState)
        assertTrue(secondReadyState != null)
    }

    @Test
    fun bookshelfCoordinatorUsesRuntimeDependencies() {
        val runtime = BookReaderPreloadRuntime.create(
            paragraphSource = BookParagraphSource { request ->
                BookParagraphLoadResult.success(
                    key = request.key,
                    paragraphs = listOf("Chapter 1", "First sentence."),
                )
            },
            clock = IncrementingBookDocumentClock(start = 100L),
        )
        val request = request()
        val plan = runtime.bookshelfPreloadCoordinator.plan(
            candidates = listOf(BookReaderBookshelfPreloadCandidate(request)),
            recentBookKeys = listOf(request.key),
            currentBookKey = null,
            maxPreloadCount = 1,
        )

        val results = runtime.bookshelfPreloadCoordinator.preload(plan)

        assertTrue(results.single().isReady)
        assertTrue(runtime.readyStateStore.get(request.key).isReady)
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

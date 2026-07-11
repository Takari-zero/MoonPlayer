package com.shenghui.localvibe.feature.book.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookReaderBookshelfPreloadCoordinatorTest {
    @Test
    fun recentBookIsFirstInPreloadPlan() {
        val store = BookReaderEntryReadyStateStore()
        val coordinator = coordinator(store = store)
        val plan = coordinator.plan(
            candidates = listOf(candidate("file://a.txt"), candidate("file://recent.txt"), candidate("file://b.txt")),
            recentBookKeys = listOf(key("file://recent.txt")),
            currentBookKey = null,
            maxPreloadCount = 3,
        )

        assertEquals("file://recent.txt", plan.first().request.key.bookId)
    }

    @Test
    fun duplicatedBookKeysAreDeduped() {
        val coordinator = coordinator()

        val plan = coordinator.plan(
            candidates = listOf(candidate("file://a.txt"), candidate("file://a.txt"), candidate("file://b.txt")),
            recentBookKeys = emptyList(),
            currentBookKey = null,
            maxPreloadCount = 3,
        )

        assertEquals(listOf("file://a.txt", "file://b.txt"), plan.map { it.request.key.bookId })
    }

    @Test
    fun alreadyReadyBookIsNotPlannedAgain() {
        val store = BookReaderEntryReadyStateStore()
        store.putReady(
            key = key("file://ready.txt"),
            readyState = readyState("file://ready.txt"),
            updatedAtMillis = 100L,
        )
        val coordinator = coordinator(store = store)

        val plan = coordinator.plan(
            candidates = listOf(candidate("file://ready.txt"), candidate("file://missing.txt")),
            recentBookKeys = emptyList(),
            currentBookKey = null,
            maxPreloadCount = 3,
        )

        assertEquals(listOf("file://missing.txt"), plan.map { it.request.key.bookId })
    }

    @Test
    fun missingAndStaleBooksArePreloadCandidates() {
        val store = BookReaderEntryReadyStateStore()
        store.putReady(
            key = key("file://stale.txt"),
            readyState = readyState("file://stale.txt"),
            updatedAtMillis = 100L,
        )
        store.invalidate(key = key("file://stale.txt"), updatedAtMillis = 200L)
        val coordinator = coordinator(store = store)

        val plan = coordinator.plan(
            candidates = listOf(candidate("file://stale.txt"), candidate("file://missing.txt")),
            recentBookKeys = emptyList(),
            currentBookKey = null,
            maxPreloadCount = 3,
        )

        assertEquals(listOf("file://stale.txt", "file://missing.txt"), plan.map { it.request.key.bookId })
    }

    @Test
    fun maxPreloadCountLimitsPlanSize() {
        val coordinator = coordinator()

        val plan = coordinator.plan(
            candidates = listOf(candidate("file://a.txt"), candidate("file://b.txt"), candidate("file://c.txt")),
            recentBookKeys = emptyList(),
            currentBookKey = null,
            maxPreloadCount = 2,
        )

        assertEquals(listOf("file://a.txt", "file://b.txt"), plan.map { it.request.key.bookId })
    }

    @Test
    fun preloadSuccessStoresReadyState() {
        val store = BookReaderEntryReadyStateStore()
        val manager = manager(store = store)
        val coordinator = BookReaderBookshelfPreloadCoordinator(
            preloadManager = manager,
            readyStateStore = store,
        )

        val results = coordinator.preload(
            listOf(BookReaderBookshelfPreloadPlanItem(candidate("file://demo.txt").request))
        )

        assertTrue(results.single().isReady)
        assertTrue(store.get(key("file://demo.txt")).isReady)
        assertTrue(store.getReadyState(key("file://demo.txt")) != null)
        val readyState = store.getReadyState(key("file://demo.txt"))
        assertEquals(readyState?.canonicalTarget?.paragraphIndex, readyState?.playbackSeed?.paragraphIndex)
        assertEquals(readyState?.canonicalTarget?.sentenceIndex, readyState?.playbackSeed?.sentenceIndex)
    }

    @Test
    fun preloadFailureIsNotReady() {
        val store = BookReaderEntryReadyStateStore()
        val manager = manager(
            store = store,
            paragraphSource = BookParagraphSource { request ->
                BookParagraphLoadResult.failed(
                    key = request.key,
                    message = "parse failed",
                    cause = IllegalStateException("parse failed"),
                )
            },
        )
        val coordinator = BookReaderBookshelfPreloadCoordinator(
            preloadManager = manager,
            readyStateStore = store,
        )

        val results = coordinator.preload(
            listOf(BookReaderBookshelfPreloadPlanItem(candidate("file://demo.txt").request))
        )

        assertFalse(results.single().isReady)
        assertFalse(store.get(key("file://demo.txt")).isReady)
        assertTrue(store.getReadyState(key("file://demo.txt")) == null)
    }

    private fun coordinator(
        store: BookReaderEntryReadyStateStore = BookReaderEntryReadyStateStore(),
    ): BookReaderBookshelfPreloadCoordinator {
        return BookReaderBookshelfPreloadCoordinator(
            preloadManager = manager(store = store),
            readyStateStore = store,
        )
    }

    private fun manager(
        store: BookReaderEntryReadyStateStore,
        paragraphSource: BookParagraphSource = BookParagraphSource { request ->
            BookParagraphLoadResult.success(
                key = request.key,
                paragraphs = listOf("Chapter 1", "First sentence."),
            )
        },
    ): BookReaderPreloadManager {
        return BookReaderPreloadManager(
            repository = BookDocumentRepository(
                paragraphSource = paragraphSource,
                clock = IncrementingBookDocumentClock(start = 100L),
            ),
            readyStateStore = store,
        )
    }

    private fun candidate(bookId: String): BookReaderBookshelfPreloadCandidate {
        return BookReaderBookshelfPreloadCandidate(
            request = BookDocumentPreloadRequest(
                key = key(bookId),
                title = "Demo Book",
                savedParagraphIndex = 1,
                savedSentenceIndex = 0,
                chapterTitle = "Chapter 1",
                chapterStartIndex = 0,
                speechRate = 1f,
            ),
        )
    }

    private fun key(bookId: String): BookDocumentCacheKey {
        return BookDocumentCacheKey(bookId)
    }

    private fun readyState(bookId: String): BookReaderEntryReadyState {
        val sentence = BookReaderEntrySentence(
            text = "First sentence.",
            paragraphIndex = 0,
            sentenceIndex = 0,
            chapterSentenceIndex = 0,
        )
        return BookReaderEntryReadyState(
            bookId = bookId,
            bookTitle = "Demo Book",
            paragraphs = listOf("First sentence."),
            chapterTitle = "Chapter 1",
            chapterSentences = listOf(sentence),
            canonicalTarget = BookReaderEntryCanonicalTarget(
                paragraphIndex = 0,
                sentenceIndex = 0,
                chapterSentenceIndex = 0,
            ),
            progressSnapshot = BookReaderEntryProgressSnapshot(
                chapterSentenceIndex = 0,
                totalChapterSentenceCount = 1,
                progressValue = 0f,
                progressMaxValue = 1f,
                listenedTimeLabel = "00:00",
                remainingTimeLabel = "00:01",
            ),
            lazyListInitialIndex = 1,
            lazyListInitialOffset = 0,
            playbackSeed = BookReaderEntryPlaybackSeed(
                bookId = bookId,
                bookTitle = "Demo Book",
                chapterIndex = 0,
                chapterTitle = "Chapter 1",
                paragraphIndex = 0,
                sentenceIndex = 0,
                sentenceText = "First sentence.",
            ),
        )
    }
}

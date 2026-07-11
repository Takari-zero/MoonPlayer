package com.shenghui.localvibe.feature.book.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BookDocumentCacheTest {
    @Test
    fun parsedDocumentCanHoldParagraphsAndChapterSentences() {
        val document = document()

        assertEquals("file://demo.txt", document.key.bookId)
        assertEquals("Demo Book", document.title)
        assertEquals(listOf("Chapter 1", "First sentence."), document.paragraphs)
        assertEquals(1, document.chapterSentences.size)
        assertTrue(document.hasReadyText)
    }

    @Test
    fun readySnapshotCarriesCanonicalTargetProgressAndPlaybackSeed() {
        val snapshot = readySnapshot()

        assertEquals(0, snapshot.canonicalTarget.paragraphIndex)
        assertEquals(0, snapshot.progressSnapshot.chapterSentenceIndex)
        assertEquals(0, snapshot.playbackSeed.paragraphIndex)
        assertTrue(snapshot.firstFrameTargetEqualsPlaybackTarget)
    }

    @Test
    fun cacheEntryCanRepresentLoadingReadyAndFailed() {
        val key = BookDocumentCacheKey(bookId = "file://demo.txt")
        val loading = BookDocumentCacheEntry.loading(key = key, updatedAtMillis = 100L)
        val ready = BookDocumentCacheEntry.ready(
            key = key,
            snapshot = readySnapshot(),
            createdAtMillis = 100L,
            updatedAtMillis = 200L,
        )
        val failed = BookDocumentCacheEntry.failed(
            key = key,
            message = "parse failed",
            updatedAtMillis = 300L,
        )

        assertTrue(loading.state is BookDocumentCacheState.Loading)
        assertFalse(loading.isReady)
        assertTrue(ready.state is BookDocumentCacheState.Ready)
        assertTrue(ready.isReady)
        assertSame(ready.snapshotOrNull(), (ready.state as BookDocumentCacheState.Ready).snapshot)
        assertTrue(failed.state is BookDocumentCacheState.Failed)
        assertFalse(failed.isReady)
    }

    @Test
    fun staleOrEmptyDocumentIsNotReady() {
        val key = BookDocumentCacheKey(bookId = "file://demo.txt")
        val stale = BookDocumentCacheEntry.ready(
            key = key,
            snapshot = readySnapshot(isStale = true),
            createdAtMillis = 100L,
            updatedAtMillis = 200L,
        )
        val emptyDocumentState = BookDocumentCacheState.Parsed(
            document = document(paragraphs = emptyList(), chapterSentences = emptyList()),
        )
        val emptyDocument = BookDocumentCacheEntry(
            key = key,
            state = emptyDocumentState,
            createdAtMillis = 100L,
            updatedAtMillis = 200L,
        )

        assertFalse(stale.isReady)
        assertFalse(emptyDocument.isReady)
    }

    private fun document(
        paragraphs: List<String> = listOf("Chapter 1", "First sentence."),
        chapterSentences: List<BookReaderEntrySentence> = listOf(sentence()),
    ): BookDocument {
        return BookDocument(
            key = BookDocumentCacheKey(bookId = "file://demo.txt"),
            title = "Demo Book",
            paragraphs = paragraphs,
            chapterTitle = "Chapter 1",
            chapterSentences = chapterSentences,
            parsedAtMillis = 100L,
        )
    }

    private fun readySnapshot(isStale: Boolean = false): BookDocumentReadySnapshot {
        return BookDocumentReadySnapshot(
            document = document(),
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
                bookId = "file://demo.txt",
                bookTitle = "Demo Book",
                chapterIndex = 0,
                chapterTitle = "Chapter 1",
                paragraphIndex = 0,
                sentenceIndex = 0,
                sentenceText = "First sentence.",
            ),
            preloadStatus = if (isStale) {
                BookDocumentPreloadStatus.Stale
            } else {
                BookDocumentPreloadStatus.Ready
            },
            createdAtMillis = 100L,
            updatedAtMillis = 200L,
        )
    }

    private fun sentence(): BookReaderEntrySentence {
        return BookReaderEntrySentence(
            text = "First sentence.",
            paragraphIndex = 0,
            sentenceIndex = 0,
            chapterSentenceIndex = 0,
        )
    }
}

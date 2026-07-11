package com.shenghui.localvibe.feature.book.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BookReaderEntryReadyStateStoreTest {
    @Test
    fun putReadyStateCanBeReadBack() {
        val store = BookReaderEntryReadyStateStore()
        val key = key()
        val readyState = readyState()

        val entry = store.putReady(
            key = key,
            readyState = readyState,
            updatedAtMillis = 100L,
        )

        assertTrue(entry.state is BookReaderEntryReadyStateStoreState.Ready)
        assertTrue(entry.isReady)
        assertSame(readyState, entry.readyStateOrNull())
        assertSame(readyState, store.getReadyState(key))
    }

    @Test
    fun missingBookReturnsMissingAndNotReady() {
        val store = BookReaderEntryReadyStateStore()
        val entry = store.get(key())

        assertTrue(entry.state is BookReaderEntryReadyStateStoreState.Missing)
        assertFalse(entry.isReady)
        assertNull(entry.readyStateOrNull())
        assertNull(store.getReadyState(key()))
    }

    @Test
    fun failedStateCannotBeUsedAsReady() {
        val store = BookReaderEntryReadyStateStore()
        val key = key()

        val entry = store.putFailed(
            key = key,
            message = "parse failed",
            updatedAtMillis = 100L,
        )

        assertTrue(entry.state is BookReaderEntryReadyStateStoreState.Failed)
        assertFalse(entry.isReady)
        assertNull(entry.readyStateOrNull())
        assertNull(store.getReadyState(key))
    }

    @Test
    fun loadingStateDoesNotExposeReadyState() {
        val store = BookReaderEntryReadyStateStore()
        val key = key()

        val entry = store.putLoading(
            key = key,
            updatedAtMillis = 100L,
        )

        assertTrue(entry.state is BookReaderEntryReadyStateStoreState.Loading)
        assertFalse(entry.isReady)
        assertNull(entry.readyStateOrNull())
        assertNull(store.getReadyState(key))
    }

    @Test
    fun invalidateRemovesReadyStateFromFreshLookup() {
        val store = BookReaderEntryReadyStateStore()
        val key = key()
        store.putReady(
            key = key,
            readyState = readyState(),
            updatedAtMillis = 100L,
        )

        val entry = store.invalidate(
            key = key,
            updatedAtMillis = 200L,
        )

        assertTrue(entry?.state is BookReaderEntryReadyStateStoreState.Stale)
        assertNull(store.getReadyState(key))
    }

    @Test
    fun clearRemovesAllReadyStates() {
        val store = BookReaderEntryReadyStateStore()
        store.putReady(
            key = key("file://a.txt"),
            readyState = readyState(bookId = "file://a.txt"),
            updatedAtMillis = 100L,
        )
        store.putReady(
            key = key("file://b.txt"),
            readyState = readyState(bookId = "file://b.txt"),
            updatedAtMillis = 100L,
        )

        store.clear()

        assertNull(store.getReadyState(key("file://a.txt")))
        assertNull(store.getReadyState(key("file://b.txt")))
    }

    @Test
    fun oldEntryIsNotFreshReadyState() {
        val store = BookReaderEntryReadyStateStore()
        val key = key()
        val readyState = readyState()
        store.putReady(
            key = key,
            readyState = readyState,
            updatedAtMillis = 100L,
        )

        assertSame(
            readyState,
            store.getReadyState(
                key = key,
                nowMillis = 150L,
                maxAgeMillis = 100L,
            ),
        )
        assertNull(
            store.getReadyState(
                key = key,
                nowMillis = 250L,
                maxAgeMillis = 100L,
            ),
        )
    }

    @Test
    fun firstFrameTargetMustEqualPlaybackTarget() {
        val readyState = readyState()
        val store = BookReaderEntryReadyStateStore()
        val entry = store.putReady(
            key = key(),
            readyState = readyState,
            updatedAtMillis = 100L,
        )

        assertTrue(entry.firstFrameTargetEqualsPlaybackTarget)
        assertEquals(readyState.canonicalTarget.paragraphIndex, readyState.playbackSeed.paragraphIndex)
        assertEquals(readyState.canonicalTarget.sentenceIndex, readyState.playbackSeed.sentenceIndex)
        assertEquals(readyState.canonicalTarget.chapterSentenceIndex, readyState.progressSnapshot.chapterSentenceIndex)
    }

    private fun key(bookId: String = "file://demo.txt"): BookDocumentCacheKey {
        return BookDocumentCacheKey(bookId = bookId)
    }

    private fun readyState(bookId: String = "file://demo.txt"): BookReaderEntryReadyState {
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

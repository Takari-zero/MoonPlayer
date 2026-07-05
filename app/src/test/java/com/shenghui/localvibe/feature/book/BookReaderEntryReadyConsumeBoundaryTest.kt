package com.shenghui.localvibe.feature.book

import com.shenghui.localvibe.feature.book.playback.BookReaderEntryCanonicalTarget
import com.shenghui.localvibe.feature.book.playback.BookReaderEntryPlaybackSeed
import com.shenghui.localvibe.feature.book.playback.BookReaderEntryProgressSnapshot
import com.shenghui.localvibe.feature.book.playback.BookReaderEntryReadyState
import com.shenghui.localvibe.feature.book.playback.BookReaderEntrySentence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BookReaderEntryReadyConsumeBoundaryTest {
    @Test
    fun nullReadyStateKeepsLegacyPath() {
        val boundary = BookReaderEntryReadyConsumeBoundary.from(null)

        assertTrue(boundary is BookReaderEntryReadyConsumeBoundary.Legacy)
        assertFalse(boundary.usesReadyState)
        assertNull(boundary.firstFrameOrNull())
    }

    @Test
    fun readyStateMapsToFirstFrameTargetsWithoutComposeOrFileIo() {
        val readyState = readyState()
        val boundary = BookReaderEntryReadyConsumeBoundary.from(readyState)

        assertTrue(boundary is BookReaderEntryReadyConsumeBoundary.Ready)
        assertTrue(boundary.usesReadyState)
        assertSame(readyState, boundary.readyStateOrNull())

        val firstFrame = requireNotNull(boundary.firstFrameOrNull())
        assertEquals(readyState.paragraphs, firstFrame.paragraphs)
        assertEquals(readyState.chapterSentences, firstFrame.chapterSentences)
        assertEquals(readyState.canonicalTarget, firstFrame.firstFrameTarget)
        assertEquals(readyState.progressSnapshot, firstFrame.progressSnapshot)
        assertEquals(readyState.lazyListInitialIndex, firstFrame.lazyListInitialIndex)
        assertEquals(readyState.lazyListInitialOffset, firstFrame.lazyListInitialOffset)
        assertEquals(readyState.playbackSeed, firstFrame.playbackSeed)
    }

    @Test
    fun firstFrameTargetProgressAndLazyListAllSharePlaybackTarget() {
        val firstFrame = requireNotNull(
            BookReaderEntryReadyConsumeBoundary.from(readyState()).firstFrameOrNull()
        )

        assertEquals(firstFrame.firstFrameTarget.paragraphIndex, firstFrame.playbackSeed.paragraphIndex)
        assertEquals(firstFrame.firstFrameTarget.sentenceIndex, firstFrame.playbackSeed.sentenceIndex)
        assertEquals(firstFrame.firstFrameTarget.chapterSentenceIndex, firstFrame.progressSnapshot.chapterSentenceIndex)
        assertEquals(firstFrame.firstFrameTarget.chapterSentenceIndex + 1, firstFrame.lazyListInitialIndex)
        assertEquals(24, firstFrame.lazyListInitialOffset)
    }

    private fun readyState(): BookReaderEntryReadyState {
        val sentences = listOf(
            BookReaderEntrySentence(
                text = "第一句。",
                paragraphIndex = 0,
                sentenceIndex = 0,
                chapterSentenceIndex = 0,
            ),
            BookReaderEntrySentence(
                text = "第二句。",
                paragraphIndex = 1,
                sentenceIndex = 0,
                chapterSentenceIndex = 1,
            ),
        )
        return BookReaderEntryReadyState(
            bookId = "file://demo.txt",
            bookTitle = "Demo Book",
            paragraphs = listOf("第一句。", "第二句。"),
            chapterTitle = "正文",
            chapterSentences = sentences,
            canonicalTarget = BookReaderEntryCanonicalTarget(
                paragraphIndex = 1,
                sentenceIndex = 0,
                chapterSentenceIndex = 1,
            ),
            progressSnapshot = BookReaderEntryProgressSnapshot(
                chapterSentenceIndex = 1,
                totalChapterSentenceCount = 2,
                progressValue = 1f,
                progressMaxValue = 1f,
                listenedTimeLabel = "00:01",
                remainingTimeLabel = "00:01",
            ),
            lazyListInitialIndex = 2,
            lazyListInitialOffset = 24,
            playbackSeed = BookReaderEntryPlaybackSeed(
                bookId = "file://demo.txt",
                bookTitle = "Demo Book",
                chapterIndex = 0,
                chapterTitle = "正文",
                paragraphIndex = 1,
                sentenceIndex = 0,
                sentenceText = "第二句。",
            ),
        )
    }
}

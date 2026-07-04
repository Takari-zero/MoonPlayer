package com.shenghui.localvibe.feature.book.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookReaderEntryReadyStateTest {
    @Test
    fun readyStateKeepsCanonicalDisplayAndPlaybackTargetsTogether() {
        val state = readyState()

        assertEquals(10, state.canonicalTarget.paragraphIndex)
        assertEquals(1, state.canonicalTarget.sentenceIndex)
        assertEquals(2, state.canonicalTarget.chapterSentenceIndex)
        assertEquals(2, state.progressSnapshot.chapterSentenceIndex)
        assertEquals(2, state.lazyListInitialIndex)
        assertEquals(10, state.playbackSeed.paragraphIndex)
        assertEquals(1, state.playbackSeed.sentenceIndex)
        assertEquals("第三句", state.playbackSeed.sentenceText)
    }

    @Test
    fun readyStateRejectsPlaybackSeedThatDoesNotMatchCanonicalTarget() {
        val error = expectIllegalArgument {
            readyState(
                playbackSeed = playbackSeed(paragraphIndex = 11, sentenceIndex = 0, sentenceText = "错位句子")
            )
        }

        assertTrue(error.message.orEmpty().contains("playbackSeed"))
    }

    @Test
    fun readyStateRejectsLazyListIndexOutsideReaderRange() {
        val error = expectIllegalArgument {
            readyState(lazyListInitialIndex = 99)
        }

        assertTrue(error.message.orEmpty().contains("lazyListInitialIndex"))
    }

    private fun readyState(
        lazyListInitialIndex: Int = 2,
        playbackSeed: BookReaderEntryPlaybackSeed = playbackSeed(),
    ): BookReaderEntryReadyState {
        val sentences = listOf(
            BookReaderEntrySentence(text = "第一句", paragraphIndex = 9, sentenceIndex = 0, chapterSentenceIndex = 0),
            BookReaderEntrySentence(text = "第二句", paragraphIndex = 10, sentenceIndex = 0, chapterSentenceIndex = 1),
            BookReaderEntrySentence(text = "第三句", paragraphIndex = 10, sentenceIndex = 1, chapterSentenceIndex = 2),
        )
        return BookReaderEntryReadyState(
            bookId = "book://demo",
            bookTitle = "Demo Book",
            paragraphs = listOf("第一段", "第二段"),
            chapterTitle = "第一章",
            chapterSentences = sentences,
            canonicalTarget = BookReaderEntryCanonicalTarget(
                paragraphIndex = 10,
                sentenceIndex = 1,
                chapterSentenceIndex = 2,
            ),
            progressSnapshot = BookReaderEntryProgressSnapshot(
                chapterSentenceIndex = 2,
                totalChapterSentenceCount = 3,
                progressValue = 2f,
                progressMaxValue = 2f,
                listenedTimeLabel = "01:00",
                remainingTimeLabel = "00:30",
            ),
            lazyListInitialIndex = lazyListInitialIndex,
            lazyListInitialOffset = 12,
            playbackSeed = playbackSeed,
        )
    }

    private fun playbackSeed(
        paragraphIndex: Int = 10,
        sentenceIndex: Int = 1,
        sentenceText: String = "第三句",
    ): BookReaderEntryPlaybackSeed {
        return BookReaderEntryPlaybackSeed(
            bookId = "book://demo",
            bookTitle = "Demo Book",
            chapterIndex = 0,
            chapterTitle = "第一章",
            paragraphIndex = paragraphIndex,
            sentenceIndex = sentenceIndex,
            clauseIndex = 0,
            sentenceText = sentenceText,
        )
    }

    private fun expectIllegalArgument(block: () -> Unit): IllegalArgumentException {
        return try {
            block()
            throw AssertionError("Expected IllegalArgumentException")
        } catch (error: IllegalArgumentException) {
            error
        }
    }
}

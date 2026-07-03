package com.shenghui.localvibe.feature.book.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookReaderAutoAdvanceCoordinatorTest {
    @Test
    fun completedEventsAdvanceThroughNextTargets() {
        val coordinator = BookReaderAutoAdvanceCoordinator()
        val targets = listOf(
            target(paragraphIndex = 10, sentenceIndex = 0),
            target(paragraphIndex = 10, sentenceIndex = 1),
            target(paragraphIndex = 11, sentenceIndex = 0),
            target(paragraphIndex = 12, sentenceIndex = 0),
        )
        coordinator.onPlayRequested()

        val first = coordinator.decide(
            completedTarget = targets[0],
            currentTarget = targets[0],
            isPlaybackPaused = false,
            isReaderPlaying = true,
            activePlaybackEngineName = FASTSPEECH2,
            expectedPlaybackEngineName = FASTSPEECH2,
            nextTargetProvider = { targets[1] },
        )
        val second = coordinator.decide(
            completedTarget = targets[1],
            currentTarget = targets[1],
            isPlaybackPaused = false,
            isReaderPlaying = true,
            activePlaybackEngineName = FASTSPEECH2,
            expectedPlaybackEngineName = FASTSPEECH2,
            nextTargetProvider = { targets[2] },
        )
        val third = coordinator.decide(
            completedTarget = targets[2],
            currentTarget = targets[2],
            isPlaybackPaused = false,
            isReaderPlaying = true,
            activePlaybackEngineName = FASTSPEECH2,
            expectedPlaybackEngineName = FASTSPEECH2,
            nextTargetProvider = { targets[3] },
        )

        assertEquals(targets[1], (first as BookReaderAutoAdvanceDecision.Advance).target)
        assertEquals(targets[2], (second as BookReaderAutoAdvanceDecision.Advance).target)
        assertEquals(targets[3], (third as BookReaderAutoAdvanceDecision.Advance).target)
    }

    @Test
    fun pauseDisablesAutoAdvance() {
        val coordinator = BookReaderAutoAdvanceCoordinator()
        val current = target()

        coordinator.onPlayRequested()
        coordinator.onPauseRequested()
        val decision = coordinator.decide(
            completedTarget = current,
            currentTarget = current,
            isPlaybackPaused = true,
            isReaderPlaying = false,
            activePlaybackEngineName = FASTSPEECH2,
            expectedPlaybackEngineName = FASTSPEECH2,
            nextTargetProvider = { target(sentenceIndex = 1) },
        )

        assertEquals(BookReaderAutoAdvanceDecision.Skip("auto_advance_disabled"), decision)
    }

    @Test
    fun resumeOrPlayEnablesAutoAdvanceAgain() {
        val coordinator = BookReaderAutoAdvanceCoordinator()
        val current = target()
        val next = target(sentenceIndex = 1)

        coordinator.onPauseRequested()
        coordinator.onPlayRequested()
        val decision = coordinator.decide(
            completedTarget = current,
            currentTarget = current,
            isPlaybackPaused = false,
            isReaderPlaying = true,
            activePlaybackEngineName = FASTSPEECH2,
            expectedPlaybackEngineName = FASTSPEECH2,
            nextTargetProvider = { next },
        )

        assertEquals(next, (decision as BookReaderAutoAdvanceDecision.Advance).target)
    }

    @Test
    fun staleTargetIsIgnored() {
        val coordinator = BookReaderAutoAdvanceCoordinator()
        coordinator.onPlayRequested()

        val decision = coordinator.decide(
            completedTarget = target(paragraphIndex = 10),
            currentTarget = target(paragraphIndex = 11),
            isPlaybackPaused = false,
            isReaderPlaying = true,
            activePlaybackEngineName = FASTSPEECH2,
            expectedPlaybackEngineName = FASTSPEECH2,
            nextTargetProvider = { target(paragraphIndex = 12) },
        )

        assertEquals(BookReaderAutoAdvanceDecision.Skip("stale_target"), decision)
    }

    @Test
    fun chapterEndStopsWhenNoNextTargetExists() {
        val coordinator = BookReaderAutoAdvanceCoordinator()
        coordinator.onPlayRequested()
        val current = target(paragraphIndex = 99)

        val decision = coordinator.decide(
            completedTarget = current,
            currentTarget = current,
            isPlaybackPaused = false,
            isReaderPlaying = true,
            activePlaybackEngineName = FASTSPEECH2,
            expectedPlaybackEngineName = FASTSPEECH2,
            nextTargetProvider = { null },
        )

        assertTrue(decision is BookReaderAutoAdvanceDecision.StopAtEnd)
    }

    private fun target(
        chapterIndex: Int = 0,
        paragraphIndex: Int = 10,
        sentenceIndex: Int = 0,
    ) = BookReaderPlaybackTarget(
        bookId = "book",
        bookTitle = "Book",
        chapterIndex = chapterIndex,
        chapterTitle = "Chapter",
        paragraphIndex = paragraphIndex,
        sentenceIndex = sentenceIndex,
        sentenceText = "test sentence",
        sentencePreview = "test sentence",
    )

    private companion object {
        const val FASTSPEECH2 = "FASTSPEECH2"
    }
}

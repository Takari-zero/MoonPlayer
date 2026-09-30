package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookPlaybackIntentTest {
    @Test
    fun playingIntentKeepsRapidSentenceReplacementPlaying() {
        assertTrue(BookPlaybackIntent.shouldAutoPlayAfterSentenceTap(playbackIntentPlaying = true))
    }

    @Test
    fun pausedIntentDoesNotStartWhenSelectingAnotherSentence() {
        assertFalse(BookPlaybackIntent.shouldAutoPlayAfterSentenceTap(playbackIntentPlaying = false))
    }

    @Test
    fun executionStateDoesNotChangeTheIntentDecision() {
        val playingIntent = true
        val executionStatesDuringReplacement = listOf(true, false, true)

        executionStatesDuringReplacement.forEach { executionPlaying ->
            assertTrue(
                "executionPlaying=$executionPlaying must not clear the playing intent",
                BookPlaybackIntent.shouldAutoPlayAfterSentenceTap(playingIntent)
            )
        }
    }

    @Test
    fun completedTargetContinuesFromIntentEvenWhenCurrentPlayerIsNoLongerPlaying() {
        assertTrue(
            shouldContinuePlaybackAfterTarget(
                moved = true,
                playbackIntentPlaying = true,
                sessionValid = true
            )
        )
    }

    @Test
    fun pausedIntentDoesNotAutoAdvanceAfterCurrentTargetCompletes() {
        assertFalse(
            shouldContinuePlaybackAfterTarget(
                moved = true,
                playbackIntentPlaying = false,
                sessionValid = true
            )
        )
    }

    @Test
    fun staleSessionDoesNotAutoAdvanceEvenWhenIntentIsStillTrue() {
        assertFalse(
            shouldContinuePlaybackAfterTarget(
                moved = true,
                playbackIntentPlaying = true,
                sessionValid = false
            )
        )
    }

    @Test
    fun missingNextTargetDoesNotContinue() {
        assertFalse(
            shouldContinuePlaybackAfterTarget(
                moved = false,
                playbackIntentPlaying = true,
                sessionValid = true
            )
        )
    }
}

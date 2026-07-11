package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookReaderProgressSeekInteractionTest {
    @Test
    fun progressDragPreviewDoesNotCommitSeek() {
        val interaction = BookReaderProgressSeekInteraction()

        assertNull(interaction.preview(12))
        assertNull(interaction.preview(24))
        assertNull(interaction.preview(36))
    }

    @Test
    fun progressDragPreviewDoesNotChangeCommittedTimeLabels() {
        val interaction = BookReaderProgressSeekInteraction()

        interaction.preview(12)
        interaction.preview(24)
        interaction.preview(36)

        val labels = resolveBookReaderProgressDisplayLabels(
            listenedTimeLabel = "02:03",
            remainingTimeLabel = "02:07",
            listenedTimeLabelOverride = null,
            remainingTimeLabelOverride = null,
        )

        assertEquals("02:03", labels.listenedTimeLabel)
        assertEquals("02:07", labels.remainingTimeLabel)
    }

    @Test
    fun progressDragPreviewKeepsCommittedOverrideLabels() {
        val interaction = BookReaderProgressSeekInteraction()

        interaction.preview(12)
        interaction.preview(24)

        val labels = resolveBookReaderProgressDisplayLabels(
            listenedTimeLabel = "02:03",
            remainingTimeLabel = "02:07",
            listenedTimeLabelOverride = "04:10",
            remainingTimeLabelOverride = "00:30",
        )

        assertEquals("04:10", labels.listenedTimeLabel)
        assertEquals("00:30", labels.remainingTimeLabel)
    }

    @Test
    fun manualViewportPreviewDoesNotCommitReaderTarget() {
        val interaction = BookReaderManualViewportPreviewInteraction()
        val firstPreview = BookReaderProgressSeekUiTarget.fromReaderSentenceFields(
            localIndex = 12,
            paragraphIndex = 750,
            sentenceIndexInParagraph = 0,
            chapterSentenceIndex = 12,
        )
        val secondPreview = BookReaderProgressSeekUiTarget.fromReaderSentenceFields(
            localIndex = 24,
            paragraphIndex = 758,
            sentenceIndexInParagraph = 1,
            chapterSentenceIndex = 24,
        )

        assertNull(interaction.preview(firstPreview))
        assertNull(interaction.preview(secondPreview))
    }

    @Test
    fun manualViewportPreviewReturnsToPlaybackTargetWithoutCommit() {
        val interaction = BookReaderManualViewportPreviewInteraction()
        val preview = BookReaderProgressSeekUiTarget.fromReaderSentenceFields(
            localIndex = 24,
            paragraphIndex = 758,
            sentenceIndexInParagraph = 1,
            chapterSentenceIndex = 24,
        )
        val playbackTarget = BookReaderProgressSeekUiTarget.fromReaderSentenceFields(
            localIndex = 66,
            paragraphIndex = 764,
            sentenceIndexInParagraph = 0,
            chapterSentenceIndex = 66,
        )

        interaction.preview(preview)

        assertEquals(playbackTarget, interaction.playbackReturnTarget(playbackTarget))
    }

    @Test
    fun sentenceClickCommitsAfterManualViewportPreview() {
        val interaction = BookReaderManualViewportPreviewInteraction()
        val preview = BookReaderProgressSeekUiTarget.fromReaderSentenceFields(
            localIndex = 24,
            paragraphIndex = 758,
            sentenceIndexInParagraph = 1,
            chapterSentenceIndex = 24,
        )
        val sentenceClickTarget = BookReaderProgressSeekUiTarget.fromReaderSentenceFields(
            localIndex = 28,
            paragraphIndex = 760,
            sentenceIndexInParagraph = 0,
            chapterSentenceIndex = 28,
        )

        interaction.preview(preview)

        assertEquals(sentenceClickTarget, interaction.sentenceClickCommit(sentenceClickTarget))
    }

    @Test
    fun staleResumeTargetIsIgnoredAfterSeekCommit() {
        val decision = resolveBookReaderPlayResumeDecision(
            isPaused = true,
            canResume = true,
            targetMatches = false,
        )

        assertFalse(decision.shouldResume)
        assertTrue(decision.shouldFreshPlay)
        assertTrue(decision.ignoreStaleResume)
    }

    @Test
    fun matchingPausedTargetCanResumeNormally() {
        val decision = resolveBookReaderPlayResumeDecision(
            isPaused = true,
            canResume = true,
            targetMatches = true,
        )

        assertTrue(decision.shouldResume)
        assertFalse(decision.shouldFreshPlay)
        assertFalse(decision.ignoreStaleResume)
    }

    @Test
    fun nonResumablePlaybackUsesFreshPlay() {
        val decision = resolveBookReaderPlayResumeDecision(
            isPaused = true,
            canResume = false,
            targetMatches = true,
        )

        assertFalse(decision.shouldResume)
        assertTrue(decision.shouldFreshPlay)
        assertFalse(decision.ignoreStaleResume)
    }

    @Test
    fun pauseInvalidatesActiveSessionAndCapturesCommittedTarget() {
        val boundary = resolveBookReaderPauseSessionBoundary(
            hasActivePlayback = true,
            committedParagraphIndex = 781,
            committedSentenceIndex = 3,
        )

        assertTrue(boundary.hasActivePlayback)
        assertEquals(781, boundary.committedParagraphIndex)
        assertEquals(3, boundary.committedSentenceIndex)
        assertTrue(boundary.invalidateSession)
        assertTrue(boundary.resumeShouldFreshPlay)
    }

    @Test
    fun pauseWithoutActivePlaybackStillInvalidatesStaleSessionBoundary() {
        val boundary = resolveBookReaderPauseSessionBoundary(
            hasActivePlayback = false,
            committedParagraphIndex = 781,
            committedSentenceIndex = 3,
        )

        assertFalse(boundary.hasActivePlayback)
        assertEquals(781, boundary.committedParagraphIndex)
        assertEquals(3, boundary.committedSentenceIndex)
        assertTrue(boundary.invalidateSession)
        assertTrue(boundary.resumeShouldFreshPlay)
    }

    @Test
    fun continueAfterPauseInvalidationUsesFreshCommittedTarget() {
        val boundary = resolveBookReaderPauseSessionBoundary(
            hasActivePlayback = true,
            committedParagraphIndex = 781,
            committedSentenceIndex = 3,
        )
        val decision = resolveBookReaderPlayResumeDecision(
            isPaused = false,
            canResume = false,
            targetMatches = true,
        )

        assertTrue(boundary.resumeShouldFreshPlay)
        assertEquals(781, boundary.committedParagraphIndex)
        assertEquals(3, boundary.committedSentenceIndex)
        assertFalse(decision.shouldResume)
        assertTrue(decision.shouldFreshPlay)
    }

    @Test
    fun progressDragFinishedCommitsLastPreviewOnce() {
        val interaction = BookReaderProgressSeekInteraction()

        interaction.preview(12)
        interaction.preview(24)
        interaction.preview(36)

        assertEquals(36, interaction.finish())
        assertNull(interaction.finish())
    }

    @Test
    fun progressDragCanceledDoesNotCommitSeek() {
        val interaction = BookReaderProgressSeekInteraction()

        interaction.preview(12)
        interaction.preview(24)
        interaction.cancel()

        assertNull(interaction.finish())
    }

    @Test
    fun progressTapCommitsImmediately() {
        val interaction = BookReaderProgressSeekInteraction()

        assertEquals(42, interaction.tap(42))
    }

    @Test
    fun seekCommitSuppressesStaleViewportUntilTargetAligns() {
        val pending = BookReaderProgressSeekUiTarget.fromReaderSentenceFields(
            localIndex = 27,
            paragraphIndex = 743,
            sentenceIndexInParagraph = 1,
            chapterSentenceIndex = 27,
        )
        val staleViewport = BookReaderProgressSeekUiTarget.fromViewportFields(
            paragraphIndex = 758,
            sentenceIndexInParagraph = 1,
            chapterSentenceIndex = 77,
        )
        val alignedViewport = BookReaderProgressSeekUiTarget.fromViewportFields(
            paragraphIndex = 743,
            sentenceIndexInParagraph = 1,
            chapterSentenceIndex = 27,
        )

        assertTrue(BookReaderProgressSeekUiTarget.shouldSuppressViewport(pending, staleViewport))
        assertFalse(BookReaderProgressSeekUiTarget.shouldSuppressViewport(pending, alignedViewport))
    }

    @Test
    fun noPendingSeekDoesNotSuppressViewport() {
        val viewport = BookReaderProgressSeekUiTarget.fromViewportFields(
            paragraphIndex = 758,
            sentenceIndexInParagraph = 1,
            chapterSentenceIndex = 77,
        )

        assertFalse(BookReaderProgressSeekUiTarget.shouldSuppressViewport(null, viewport))
    }
}

package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PreviewPlaybackContinuationPlannerTest {
    private val baseline = LocalPlaybackLifecycleSnapshot(
        localTargetGeneration = 8L,
        localSessionId = 21L,
        currentReaderGeneration = 8L,
        currentPlaybackSessionId = 21L,
        playbackIntentPlaying = true,
        localSentenceDrained = false,
        fullReaderStatus = FullReaderStatus.LOADING,
        preparedVerification = PreparedTargetVerificationStatus.UNKNOWN
    )

    @Test
    fun fullReadyFirstKeepsMatchedLocalSentencePlaying() {
        assertDecision(
            PreviewPlaybackContinuationDecision.KEEP_CURRENT_PLAYBACK,
            baseline.copy(
                fullReaderStatus = FullReaderStatus.READY,
                preparedVerification = PreparedTargetVerificationStatus.MATCHED
            )
        )
    }

    @Test
    fun drainedAfterMatchedFullReadyAllowsFormalNext() {
        assertDecision(
            PreviewPlaybackContinuationDecision.ALLOW_FORMAL_NEXT,
            baseline.copy(
                localSentenceDrained = true,
                fullReaderStatus = FullReaderStatus.READY,
                preparedVerification = PreparedTargetVerificationStatus.MATCHED
            )
        )
    }

    @Test
    fun localDrainedFirstWaitsForFullReader() {
        assertDecision(
            PreviewPlaybackContinuationDecision.WAIT_FOR_FULL_READY,
            baseline.copy(localSentenceDrained = true)
        )
    }

    @Test
    fun fullReadyAfterDrainedAllowsFormalNextWhenMatched() {
        val drainedWaiting = baseline.copy(localSentenceDrained = true)

        assertDecision(
            PreviewPlaybackContinuationDecision.ALLOW_FORMAL_NEXT,
            drainedWaiting.copy(
                fullReaderStatus = FullReaderStatus.READY,
                preparedVerification = PreparedTargetVerificationStatus.MATCHED
            )
        )
    }

    @Test
    fun pauseWhileWaitingStopsAutoContinueAfterFullReady() {
        assertDecision(
            PreviewPlaybackContinuationDecision.STOP_AUTO_CONTINUE,
            baseline.copy(
                playbackIntentPlaying = false,
                localSentenceDrained = true,
                fullReaderStatus = FullReaderStatus.READY,
                preparedVerification = PreparedTargetVerificationStatus.MATCHED
            )
        )
    }

    @Test
    fun sessionMismatchIgnoresStaleLifecycle() {
        assertDecision(
            PreviewPlaybackContinuationDecision.IGNORE_STALE,
            baseline.copy(currentPlaybackSessionId = baseline.localSessionId + 1L)
        )
    }

    @Test
    fun generationMismatchIgnoresStaleLifecycle() {
        assertDecision(
            PreviewPlaybackContinuationDecision.IGNORE_STALE,
            baseline.copy(currentReaderGeneration = baseline.localTargetGeneration + 1L)
        )
    }

    @Test
    fun generationStaleWinsOverPausedIntentAndRejectedVerification() {
        assertDecision(
            PreviewPlaybackContinuationDecision.IGNORE_STALE,
            baseline.copy(
                currentReaderGeneration = baseline.localTargetGeneration + 1L,
                playbackIntentPlaying = false,
                fullReaderStatus = FullReaderStatus.READY,
                preparedVerification = PreparedTargetVerificationStatus.REJECTED
            )
        )
    }

    @Test
    fun rejectedVerificationStopsFutureContinuationWhileLocalIsPlaying() {
        assertDecision(
            PreviewPlaybackContinuationDecision.STOP_AUTO_CONTINUE,
            baseline.copy(
                fullReaderStatus = FullReaderStatus.READY,
                preparedVerification = PreparedTargetVerificationStatus.REJECTED
            )
        )
    }

    @Test
    fun rejectedVerificationStopsContinuationAfterLocalDrains() {
        assertDecision(
            PreviewPlaybackContinuationDecision.STOP_AUTO_CONTINUE,
            baseline.copy(
                localSentenceDrained = true,
                fullReaderStatus = FullReaderStatus.READY,
                preparedVerification = PreparedTargetVerificationStatus.REJECTED
            )
        )
    }

    @Test
    fun falsePlaybackIntentNeverAllowsFormalNext() {
        val decision = plan(
            baseline.copy(
                playbackIntentPlaying = false,
                localSentenceDrained = true,
                fullReaderStatus = FullReaderStatus.READY,
                preparedVerification = PreparedTargetVerificationStatus.MATCHED
            )
        )

        assertEquals(PreviewPlaybackContinuationDecision.STOP_AUTO_CONTINUE, decision)
        assertFalse(decision == PreviewPlaybackContinuationDecision.ALLOW_FORMAL_NEXT)
    }

    @Test
    fun staleSessionWinsEvenWhenContinuationWouldOtherwiseBeAllowed() {
        assertDecision(
            PreviewPlaybackContinuationDecision.IGNORE_STALE,
            baseline.copy(
                currentPlaybackSessionId = baseline.localSessionId + 1L,
                localSentenceDrained = true,
                fullReaderStatus = FullReaderStatus.READY,
                preparedVerification = PreparedTargetVerificationStatus.MATCHED
            )
        )
    }

    @Test
    fun decisionApiHasNoCurrentTargetReplayAction() {
        assertEquals(
            setOf(
                PreviewPlaybackContinuationDecision.KEEP_CURRENT_PLAYBACK,
                PreviewPlaybackContinuationDecision.WAIT_FOR_FULL_READY,
                PreviewPlaybackContinuationDecision.WAIT_FOR_PREPARED_VERIFICATION,
                PreviewPlaybackContinuationDecision.ALLOW_FORMAL_NEXT,
                PreviewPlaybackContinuationDecision.STOP_AUTO_CONTINUE,
                PreviewPlaybackContinuationDecision.IGNORE_STALE
            ),
            PreviewPlaybackContinuationDecision.entries.toSet()
        )
    }

    @Test
    fun fullLoadFailureDoesNotInterruptCurrentLocalSentence() {
        assertDecision(
            PreviewPlaybackContinuationDecision.KEEP_CURRENT_PLAYBACK,
            baseline.copy(fullReaderStatus = FullReaderStatus.FAILED)
        )
    }

    @Test
    fun fullLoadFailureStopsAfterLocalSentenceDrains() {
        assertDecision(
            PreviewPlaybackContinuationDecision.STOP_AUTO_CONTINUE,
            baseline.copy(
                localSentenceDrained = true,
                fullReaderStatus = FullReaderStatus.FAILED
            )
        )
    }

    @Test
    fun drainedReadyTargetWaitsForVerificationBeforeContinuing() {
        assertDecision(
            PreviewPlaybackContinuationDecision.WAIT_FOR_PREPARED_VERIFICATION,
            baseline.copy(
                localSentenceDrained = true,
                fullReaderStatus = FullReaderStatus.READY
            )
        )
    }

    @Test
    fun playingReadyTargetWaitsForVerificationWithoutPlaybackAction() {
        assertDecision(
            PreviewPlaybackContinuationDecision.WAIT_FOR_PREPARED_VERIFICATION,
            baseline.copy(fullReaderStatus = FullReaderStatus.READY)
        )
    }

    @Test
    fun loadingStateIgnoresPrematureMatchedVerification() {
        assertDecision(
            PreviewPlaybackContinuationDecision.WAIT_FOR_FULL_READY,
            baseline.copy(
                localSentenceDrained = true,
                preparedVerification = PreparedTargetVerificationStatus.MATCHED
            )
        )
    }

    @Test
    fun loadingStateIgnoresPrematureRejectedVerification() {
        assertDecision(
            PreviewPlaybackContinuationDecision.KEEP_CURRENT_PLAYBACK,
            baseline.copy(preparedVerification = PreparedTargetVerificationStatus.REJECTED)
        )
    }

    private fun assertDecision(
        expected: PreviewPlaybackContinuationDecision,
        snapshot: LocalPlaybackLifecycleSnapshot
    ) {
        assertEquals(expected, plan(snapshot))
    }

    private fun plan(snapshot: LocalPlaybackLifecycleSnapshot): PreviewPlaybackContinuationDecision {
        return PreviewPlaybackContinuationPlanner.plan(snapshot)
    }
}

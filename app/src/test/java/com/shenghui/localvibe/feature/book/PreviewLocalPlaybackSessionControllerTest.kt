package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewLocalPlaybackSessionControllerTest {
    @Test
    fun initialStateIsIdleWithoutOwnership() {
        val snapshot = PreviewLocalPlaybackSessionController().snapshot()

        assertEquals(PreviewLocalPlaybackState.IDLE, snapshot.state)
        assertEquals(PreviewLocalPlaybackOwnership.NONE, snapshot.ownership)
        assertFalse(snapshot.localSentenceDrained)
    }

    @Test
    fun beginLocalEntersStartingAndSuppressesSameTargetPending() {
        val result = begin()

        assertEquals(PreviewLocalPlaybackState.LOCAL_STARTING, result.snapshot.state)
        assertEquals(PreviewLocalPlaybackOwnership.LOCAL, result.snapshot.ownership)
        assertEquals(
            PreviewLocalOwnershipDirective.SUPPRESS_SAME_TARGET_PENDING,
            result.ownershipDirective
        )
        assertEquals(10L, result.snapshot.playbackSessionId)
    }

    @Test
    fun localStartSuccessEntersActive() {
        val controller = PreviewLocalPlaybackSessionController()
        begin(controller)

        val result = controller.onLocalStarted(1L, 10L)

        assertEquals(PreviewLocalPlaybackState.LOCAL_ACTIVE, result.snapshot.state)
        assertEquals(PreviewLocalPlaybackOwnership.LOCAL, result.snapshot.ownership)
        assertEquals(
            PreviewPlaybackContinuationDecision.KEEP_CURRENT_PLAYBACK,
            result.continuationDecision
        )
    }

    @Test
    fun localStartFailureRestoresPreparedFallbackOwnership() {
        val controller = PreviewLocalPlaybackSessionController()
        begin(controller)

        val result = controller.onLocalStartFailed(1L, 10L)

        assertEquals(PreviewLocalPlaybackState.PREPARED_FALLBACK_WAITING, result.snapshot.state)
        assertEquals(PreviewLocalPlaybackOwnership.PREPARED_FALLBACK, result.snapshot.ownership)
        assertEquals(
            PreviewLocalOwnershipDirective.RESTORE_PREPARED_FALLBACK,
            result.ownershipDirective
        )
        assertEquals(
            PreviewPlaybackContinuationDecision.WAIT_FOR_FULL_READY,
            result.continuationDecision
        )
    }

    @Test
    fun localAndPreparedFallbackOwnershipAreMutuallyExclusive() {
        val local = startedController().snapshot()
        val fallbackController = PreviewLocalPlaybackSessionController()
        begin(fallbackController)
        val fallback = fallbackController.onLocalStartFailed(1L, 10L).snapshot

        assertEquals(PreviewLocalPlaybackOwnership.LOCAL, local.ownership)
        assertEquals(PreviewLocalPlaybackOwnership.PREPARED_FALLBACK, fallback.ownership)
        assertFalse(local.ownership == PreviewLocalPlaybackOwnership.PREPARED_FALLBACK)
        assertFalse(fallback.ownership == PreviewLocalPlaybackOwnership.LOCAL)
    }

    @Test
    fun fullReadyBeforeLocalStartDoesNotLoseStartingCallback() {
        val controller = PreviewLocalPlaybackSessionController()
        begin(controller)

        val ready = controller.onFullReaderReady(1L, 10L, PreparedTargetVerificationStatus.MATCHED)
        val started = controller.onLocalStarted(1L, 10L)

        assertEquals(PreviewLocalPlaybackState.LOCAL_STARTING, ready.snapshot.state)
        assertEquals(PreviewLocalPlaybackState.PREPARED_VERIFIED_WAITING_DRAIN, started.snapshot.state)
        assertEquals(
            PreviewPlaybackContinuationDecision.KEEP_CURRENT_PLAYBACK,
            started.continuationDecision
        )
    }

    @Test
    fun localDrainedBeforeFullReadyWaitsWithoutAllowingFormalNext() {
        val controller = startedController()

        val result = controller.onLocalDrained(1L, 10L)

        assertEquals(PreviewLocalPlaybackState.LOCAL_DRAINED_WAITING_PREPARED, result.snapshot.state)
        assertEquals(
            PreviewPlaybackContinuationDecision.WAIT_FOR_FULL_READY,
            result.continuationDecision
        )
    }

    @Test
    fun fullReadyUnknownAfterLocalDrainWaitsForVerification() {
        val controller = drainedController()

        val result = controller.onFullReaderReady(1L, 10L, PreparedTargetVerificationStatus.UNKNOWN)

        assertEquals(PreviewLocalPlaybackState.LOCAL_DRAINED_WAITING_PREPARED, result.snapshot.state)
        assertEquals(
            PreviewPlaybackContinuationDecision.WAIT_FOR_PREPARED_VERIFICATION,
            result.continuationDecision
        )
    }

    @Test
    fun fullReadyMatchedBeforeLocalDrainKeepsCurrentPlayback() {
        val controller = startedController()

        val result = controller.onFullReaderReady(1L, 10L, PreparedTargetVerificationStatus.MATCHED)

        assertEquals(PreviewLocalPlaybackState.PREPARED_VERIFIED_WAITING_DRAIN, result.snapshot.state)
        assertEquals(
            PreviewPlaybackContinuationDecision.KEEP_CURRENT_PLAYBACK,
            result.continuationDecision
        )
    }

    @Test
    fun fullReadyMatchedAfterLocalDrainAllowsFormalNext() {
        val controller = drainedController()

        val result = controller.onFullReaderReady(1L, 10L, PreparedTargetVerificationStatus.MATCHED)

        assertTrue(result.snapshot.localSentenceDrained)
        assertEquals(
            PreviewPlaybackContinuationDecision.ALLOW_FORMAL_NEXT,
            result.continuationDecision
        )
    }

    @Test
    fun rejectedVerificationDoesNotStopActiveLocalSentence() {
        val controller = startedController()

        val result = controller.onFullReaderReady(1L, 10L, PreparedTargetVerificationStatus.REJECTED)

        assertEquals(PreviewLocalPlaybackState.LOCAL_ACTIVE, result.snapshot.state)
        assertEquals(
            PreviewPlaybackContinuationDecision.KEEP_CURRENT_PLAYBACK,
            result.continuationDecision
        )
    }

    @Test
    fun rejectedVerificationStopsAfterLocalDrain() {
        val controller = drainedController()

        val result = controller.onFullReaderReady(1L, 10L, PreparedTargetVerificationStatus.REJECTED)

        assertEquals(
            PreviewPlaybackContinuationDecision.STOP_AUTO_CONTINUE,
            result.continuationDecision
        )
    }

    @Test
    fun fullLoadFailureKeepsActiveLocalSentence() {
        val controller = startedController()

        val result = controller.onFullLoadFailed(1L, 10L)

        assertEquals(PreviewLocalPlaybackState.LOCAL_ACTIVE, result.snapshot.state)
        assertEquals(
            PreviewPlaybackContinuationDecision.KEEP_CURRENT_PLAYBACK,
            result.continuationDecision
        )
    }

    @Test
    fun fullLoadFailureStopsAfterLocalDrain() {
        val controller = drainedController()

        val result = controller.onFullLoadFailed(1L, 10L)

        assertEquals(
            PreviewPlaybackContinuationDecision.STOP_AUTO_CONTINUE,
            result.continuationDecision
        )
    }

    @Test
    fun fallbackCannotAllowFormalNextAfterFullReady() {
        val controller = PreviewLocalPlaybackSessionController()
        begin(controller)
        controller.onLocalStartFailed(1L, 10L)

        val result = controller.onFullReaderReady(1L, 10L, PreparedTargetVerificationStatus.MATCHED)

        assertEquals(PreviewLocalPlaybackOwnership.PREPARED_FALLBACK, result.snapshot.ownership)
        assertEquals(
            PreviewPlaybackContinuationDecision.KEEP_CURRENT_PLAYBACK,
            result.continuationDecision
        )
        assertFalse(result.continuationDecision == PreviewPlaybackContinuationDecision.ALLOW_FORMAL_NEXT)
    }

    @Test
    fun fallbackStopsWhenFullLoadFails() {
        val controller = PreviewLocalPlaybackSessionController()
        begin(controller)
        controller.onLocalStartFailed(1L, 10L)

        val result = controller.onFullLoadFailed(1L, 10L)

        assertEquals(PreviewLocalPlaybackOwnership.PREPARED_FALLBACK, result.snapshot.ownership)
        assertEquals(
            PreviewPlaybackContinuationDecision.STOP_AUTO_CONTINUE,
            result.continuationDecision
        )
    }

    @Test
    fun playbackIntentPauseStopsAutoContinueWithoutReleasingLocalOwnership() {
        val controller = startedController()

        val result = controller.onPlaybackIntentChanged(1L, 10L, false)

        assertEquals(PreviewLocalPlaybackOwnership.LOCAL, result.snapshot.ownership)
        assertEquals(
            PreviewPlaybackContinuationDecision.STOP_AUTO_CONTINUE,
            result.continuationDecision
        )
    }

    @Test
    fun newSessionReplacesOldTargetAndOldDrainIsIgnored() {
        val controller = startedController()
        val targetB = target(generation = 1L, chapterSentenceIndex = 28)

        val beginB = controller.beginLocalOwnership(targetB, 1L, 11L, true)
        val oldDrain = controller.onLocalDrained(1L, 10L)

        assertEquals(28, beginB.snapshot.target?.chapterSentenceIndex)
        assertTrue(oldDrain.stale)
        assertEquals(11L, oldDrain.snapshot.playbackSessionId)
        assertFalse(oldDrain.snapshot.localSentenceDrained)
    }

    @Test
    fun staleStartedCallbackCannotReviveNewSession() {
        val controller = startedController()
        controller.beginLocalOwnership(target(generation = 1L, chapterSentenceIndex = 28), 1L, 11L, true)

        val result = controller.onLocalStarted(1L, 10L)

        assertTrue(result.stale)
        assertEquals(PreviewLocalPlaybackState.LOCAL_STARTING, result.snapshot.state)
        assertEquals(11L, result.snapshot.playbackSessionId)
    }

    @Test
    fun staleStartFailureCallbackCannotRestoreFallbackForNewSession() {
        val controller = startedController()
        controller.beginLocalOwnership(target(generation = 1L, chapterSentenceIndex = 28), 1L, 11L, true)

        val result = controller.onLocalStartFailed(1L, 10L)

        assertTrue(result.stale)
        assertEquals(PreviewLocalPlaybackOwnership.LOCAL, result.snapshot.ownership)
    }

    @Test
    fun staleFullReadyCallbackCannotChangeNewSession() {
        val controller = startedController()
        controller.beginLocalOwnership(target(generation = 1L, chapterSentenceIndex = 28), 1L, 11L, true)

        val result = controller.onFullReaderReady(1L, 10L, PreparedTargetVerificationStatus.MATCHED)

        assertTrue(result.stale)
        assertEquals(PreviewLocalPlaybackState.LOCAL_STARTING, result.snapshot.state)
    }

    @Test
    fun staleGenerationCallbackIsIgnored() {
        val controller = startedController()

        val result = controller.onLocalDrained(2L, 10L)

        assertTrue(result.stale)
        assertEquals(PreviewLocalPlaybackState.LOCAL_ACTIVE, result.snapshot.state)
    }

    @Test
    fun sameBookNewGenerationMakesOldCallbackStale() {
        val controller = startedController()
        controller.beginLocalOwnership(target(generation = 2L), 2L, 11L, true)

        val result = controller.onLocalDrained(1L, 10L)

        assertTrue(result.stale)
        assertEquals(2L, result.snapshot.readerGeneration)
    }

    @Test
    fun bookAToBToADoesNotReviveFirstGeneration() {
        val controller = startedController(target(generation = 1L, bookUri = "content://a"))
        controller.beginLocalOwnership(target(generation = 2L, bookUri = "content://b"), 2L, 11L, true)
        controller.beginLocalOwnership(target(generation = 3L, bookUri = "content://a"), 3L, 12L, true)

        val result = controller.onLocalDrained(1L, 10L)

        assertTrue(result.stale)
        assertEquals(3L, result.snapshot.readerGeneration)
        assertEquals(12L, result.snapshot.playbackSessionId)
    }

    @Test
    fun releaseStopsAndClearsOwnership() {
        val controller = startedController()

        val result = controller.release()

        assertEquals(PreviewLocalPlaybackState.STOPPED, result.snapshot.state)
        assertEquals(PreviewLocalPlaybackOwnership.NONE, result.snapshot.ownership)
        assertEquals(
            PreviewLocalOwnershipDirective.CLEAR_LOCAL_OWNERSHIP,
            result.ownershipDirective
        )
    }

    @Test
    fun releaseIsIdempotentAndLaterCallbacksAreIgnored() {
        val controller = startedController()
        controller.release()

        val secondRelease = controller.release()
        val callback = controller.onLocalStarted(1L, 10L)

        assertEquals(PreviewLocalPlaybackState.STOPPED, secondRelease.snapshot.state)
        assertTrue(callback.stale)
        assertEquals(PreviewPlaybackContinuationDecision.IGNORE_STALE, callback.continuationDecision)
    }

    private fun startedController(
        target: LocalPlaybackTarget = target()
    ): PreviewLocalPlaybackSessionController {
        return PreviewLocalPlaybackSessionController().also {
            begin(it, target)
            it.onLocalStarted(target.generation, 10L)
        }
    }

    private fun drainedController(): PreviewLocalPlaybackSessionController {
        return startedController().also { it.onLocalDrained(1L, 10L) }
    }

    private fun begin(
        controller: PreviewLocalPlaybackSessionController = PreviewLocalPlaybackSessionController(),
        target: LocalPlaybackTarget = target()
    ): PreviewLocalSessionTransitionResult {
        return controller.beginLocalOwnership(target, target.generation, 10L, true)
    }

    private fun target(
        generation: Long = 1L,
        bookUri: String = "content://book",
        chapterSentenceIndex: Int = 27
    ): LocalPlaybackTarget = LocalPlaybackTarget(
        bookUri = bookUri,
        paragraphIndex = 12,
        sentenceIndexInParagraph = 3,
        chapterSentenceIndex = chapterSentenceIndex,
        text = "cached sentence",
        stableTextHash = "cached sentence".hashCode(),
        generation = generation,
        source = LocalPlaybackTargetSource.CACHED_PREVIEW
    )
}

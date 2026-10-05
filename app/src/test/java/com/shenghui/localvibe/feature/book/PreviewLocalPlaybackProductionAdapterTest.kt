package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewLocalPlaybackProductionAdapterTest {
    @Test
    fun beginBuildsProviderNeutralCachedPreviewRequest() {
        val adapter = adapter()

        val dispatch = adapter.begin(target(), BookPlaybackEngine.MATCHA_EXPERIMENTAL, 1.15f, 41L)

        requireNotNull(dispatch)
        assertEquals(BookSentencePlaybackSource.CACHED_PREVIEW, dispatch.request.source)
        assertEquals(BookPlaybackEngine.MATCHA_EXPERIMENTAL, dispatch.request.provider)
        assertEquals(41L, dispatch.request.playbackSessionId)
        assertEquals(1.15f, dispatch.request.speechRate)
        assertNull(dispatch.request.preparedSnapshot)
        assertFalse(dispatch.request.allowNextPrewarm)
        assertTrue(dispatch.request.playbackIntentPlaying)
    }

    @Test
    fun beginAcquiresLocalOwnershipAndSuppressesPendingTarget() {
        val dispatch = requireNotNull(adapter().begin(target(), BookPlaybackEngine.MATCHA_EXPERIMENTAL, 1f, 41L))

        assertEquals(
            PreviewLocalOwnershipDirective.SUPPRESS_SAME_TARGET_PENDING,
            dispatch.transition.ownershipDirective
        )
        assertEquals(PreviewLocalPlaybackOwnership.LOCAL, dispatch.transition.snapshot.ownership)
        assertEquals(PreviewLocalPlaybackState.LOCAL_STARTING, dispatch.transition.snapshot.state)
    }

    @Test
    fun startAndDrainKeepOneSessionAndWaitForPreparedReader() {
        val adapter = adapter()
        adapter.begin(target(), BookPlaybackEngine.MATCHA_EXPERIMENTAL, 1f, 41L)

        assertEquals(
            PreviewLocalPlaybackState.LOCAL_ACTIVE,
            adapter.onLocalStarted(7L, 41L).snapshot.state
        )
        val drained = adapter.onLocalDrained(7L, 41L)

        assertEquals(41L, drained.snapshot.playbackSessionId)
        assertEquals(PreviewLocalPlaybackState.LOCAL_DRAINED_WAITING_PREPARED, drained.snapshot.state)
        assertEquals(PreviewPlaybackContinuationDecision.WAIT_FOR_FULL_READY, drained.continuationDecision)
    }

    @Test
    fun fullReadyAfterDrainAllowsFormalNextOnlyAfterVerification() {
        val adapter = adapter()
        adapter.begin(target(), BookPlaybackEngine.MATCHA_EXPERIMENTAL, 1f, 41L)
        adapter.onLocalStarted(7L, 41L)
        adapter.onLocalDrained(7L, 41L)

        val ready = adapter.onFullReaderReady(7L, 41L, PreparedTargetVerificationStatus.MATCHED)

        assertEquals(PreviewPlaybackContinuationDecision.ALLOW_FORMAL_NEXT, ready.continuationDecision)
        assertEquals(PreviewLocalPlaybackState.LOCAL_DRAINED_WAITING_PREPARED, ready.snapshot.state)
        assertEquals(41L, ready.snapshot.playbackSessionId)
    }

    @Test
    fun pausePreservesOwnershipAndSessionWithoutAllowingContinuation() {
        val adapter = adapter()
        adapter.begin(target(), BookPlaybackEngine.MATCHA_EXPERIMENTAL, 1f, 41L)
        adapter.onLocalStarted(7L, 41L)

        val paused = adapter.onPlaybackIntentChanged(7L, 41L, false)

        assertEquals(PreviewLocalPlaybackOwnership.LOCAL, paused.snapshot.ownership)
        assertEquals(41L, paused.snapshot.playbackSessionId)
        assertFalse(paused.snapshot.playbackIntentPlaying)
        assertEquals(PreviewPlaybackContinuationDecision.STOP_AUTO_CONTINUE, paused.continuationDecision)
    }

    @Test
    fun resumePreservesOwnershipAndSession() {
        val adapter = adapter()
        adapter.begin(target(), BookPlaybackEngine.MATCHA_EXPERIMENTAL, 1f, 41L)
        adapter.onLocalStarted(7L, 41L)
        adapter.onPlaybackIntentChanged(7L, 41L, false)

        val resumed = adapter.onPlaybackIntentChanged(7L, 41L, true)

        assertEquals(PreviewLocalPlaybackOwnership.LOCAL, resumed.snapshot.ownership)
        assertEquals(41L, resumed.snapshot.playbackSessionId)
        assertTrue(resumed.snapshot.playbackIntentPlaying)
        assertEquals(PreviewPlaybackContinuationDecision.KEEP_CURRENT_PLAYBACK, resumed.continuationDecision)
    }

    @Test
    fun staleCallbacksCannotChangeTheCurrentLocalSession() {
        val adapter = adapter()
        adapter.begin(target(generation = 7L), BookPlaybackEngine.MATCHA_EXPERIMENTAL, 1f, 41L)
        adapter.begin(target(generation = 8L, chapterSentenceIndex = 28), BookPlaybackEngine.MATCHA_EXPERIMENTAL, 1f, 42L)

        val staleStarted = adapter.onLocalStarted(7L, 41L)
        val staleDrained = adapter.onLocalDrained(7L, 41L)

        assertTrue(staleStarted.stale)
        assertTrue(staleDrained.stale)
        assertEquals(42L, adapter.snapshot().playbackSessionId)
        assertEquals(8L, adapter.snapshot().readerGeneration)
    }

    @Test
    fun localStartFailureMovesToPreparedFallbackWithoutLocalOwnership() {
        val adapter = adapter()
        adapter.begin(target(), BookPlaybackEngine.MATCHA_EXPERIMENTAL, 1f, 41L)

        val failed = adapter.onLocalStartFailed(7L, 41L)

        assertEquals(PreviewLocalPlaybackOwnership.PREPARED_FALLBACK, failed.snapshot.ownership)
        assertEquals(PreviewLocalPlaybackState.PREPARED_FALLBACK_WAITING, failed.snapshot.state)
        assertEquals(PreviewLocalOwnershipDirective.RESTORE_PREPARED_FALLBACK, failed.ownershipDirective)
        assertEquals(PreviewPlaybackContinuationDecision.WAIT_FOR_FULL_READY, failed.continuationDecision)
    }

    @Test
    fun preparedFallbackNeverAllowsFormalNext() {
        val adapter = adapter()
        adapter.begin(target(), BookPlaybackEngine.MATCHA_EXPERIMENTAL, 1f, 41L)
        adapter.onLocalStartFailed(7L, 41L)

        val ready = adapter.onFullReaderReady(7L, 41L, PreparedTargetVerificationStatus.MATCHED)

        assertEquals(PreviewLocalPlaybackOwnership.PREPARED_FALLBACK, ready.snapshot.ownership)
        assertEquals(PreviewPlaybackContinuationDecision.KEEP_CURRENT_PLAYBACK, ready.continuationDecision)
        assertFalse(ready.continuationDecision == PreviewPlaybackContinuationDecision.ALLOW_FORMAL_NEXT)
    }

    @Test
    fun resetClearsDispatchAndInvalidatesCallbacks() {
        val adapter = adapter()
        adapter.begin(target(), BookPlaybackEngine.MATCHA_EXPERIMENTAL, 1f, 41L)

        adapter.reset()

        assertNull(adapter.currentRequest())
        assertEquals(PreviewLocalPlaybackState.IDLE, adapter.snapshot().state)
        assertEquals(PreviewLocalPlaybackOwnership.NONE, adapter.snapshot().ownership)
    }

    private fun adapter(): PreviewLocalPlaybackProductionAdapter =
        PreviewLocalPlaybackProductionAdapter(logger = {})

    private fun target(
        generation: Long = 7L,
        chapterSentenceIndex: Int = 27
    ): LocalPlaybackTarget = LocalPlaybackTarget(
        bookUri = "content://book",
        paragraphIndex = 12,
        sentenceIndexInParagraph = 3,
        chapterSentenceIndex = chapterSentenceIndex,
        text = "cached sentence",
        stableTextHash = "cached sentence".hashCode(),
        generation = generation,
        source = LocalPlaybackTargetSource.CACHED_PREVIEW
    )
}

package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertEquals
import org.junit.Test

class BookPlaybackToggleDecisionTest {
    @Test
    fun previewPendingPlayCancelsWithoutStartingAnotherSession() {
        assertEquals(
            BookPlaybackToggleDecision.CANCEL_PREVIEW_PENDING,
            decideBookPlaybackToggle(facts(hasPending = true, playbackIntentPlaying = true))
        )
    }

    @Test
    fun localOwnershipPausesBeforeFormalReadiness() {
        assertEquals(
            BookPlaybackToggleDecision.PAUSE_LOCAL,
            decideBookPlaybackToggle(facts(hasLocalOwnership = true))
        )
    }

    @Test
    fun pausedLocalOwnershipResumesSameSession() {
        assertEquals(
            BookPlaybackToggleDecision.RESUME_LOCAL,
            decideBookPlaybackToggle(facts(hasLocalOwnership = true, localPlaybackPaused = true))
        )
    }

    @Test
    fun formalReadyPlayingPausesAndIdleFormalPlaybackStarts() {
        assertEquals(
            BookPlaybackToggleDecision.PAUSE_FORMAL,
            decideBookPlaybackToggle(facts(readiness = BookContentReadiness.PLAYBACK_READY, formalIsPlaying = true))
        )
        assertEquals(
            BookPlaybackToggleDecision.RESUME_OR_START_FORMAL,
            decideBookPlaybackToggle(facts(readiness = BookContentReadiness.PLAYBACK_READY))
        )
    }

    @Test
    fun previewWithoutPendingPlaybackStartsPreview() {
        assertEquals(
            BookPlaybackToggleDecision.START_PREVIEW_OR_NOOP,
            decideBookPlaybackToggle(facts())
        )
    }

    private fun facts(
        hasLocalOwnership: Boolean = false,
        localPlaybackPaused: Boolean = false,
        formalIsPlaying: Boolean = false,
        playbackIntentPlaying: Boolean = false,
        readiness: BookContentReadiness = BookContentReadiness.PREVIEW,
        hasPending: Boolean = false
    ): BookPlaybackToggleFacts = BookPlaybackToggleFacts(
        hasCurrentLocalOwnership = hasLocalOwnership,
        localPlaybackPaused = localPlaybackPaused,
        formalIsPlaying = formalIsPlaying,
        playbackIntentPlaying = playbackIntentPlaying,
        contentReadiness = readiness,
        hasPendingPlaybackTarget = hasPending,
        playbackPreparing = hasPending
    )
}

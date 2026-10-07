package com.shenghui.localvibe.feature.book

internal enum class BookPlaybackToggleDecision {
    PAUSE_LOCAL,
    RESUME_LOCAL,
    PAUSE_FORMAL,
    RESUME_OR_START_FORMAL,
    CANCEL_PREVIEW_PENDING,
    START_PREVIEW_OR_NOOP
}

internal data class BookPlaybackToggleFacts(
    val hasCurrentLocalOwnership: Boolean,
    val localPlaybackPaused: Boolean,
    val formalIsPlaying: Boolean,
    val playbackIntentPlaying: Boolean,
    val contentReadiness: BookContentReadiness,
    val hasPendingPlaybackTarget: Boolean,
    val playbackPreparing: Boolean
)

internal fun decideBookPlaybackToggle(
    facts: BookPlaybackToggleFacts
): BookPlaybackToggleDecision {
    if (facts.hasCurrentLocalOwnership) {
        return if (facts.localPlaybackPaused) {
            BookPlaybackToggleDecision.RESUME_LOCAL
        } else {
            BookPlaybackToggleDecision.PAUSE_LOCAL
        }
    }
    if (facts.contentReadiness == BookContentReadiness.PLAYBACK_READY) {
        return if (facts.formalIsPlaying || facts.playbackIntentPlaying) {
            BookPlaybackToggleDecision.PAUSE_FORMAL
        } else {
            BookPlaybackToggleDecision.RESUME_OR_START_FORMAL
        }
    }
    return if (facts.playbackIntentPlaying && facts.hasPendingPlaybackTarget) {
        BookPlaybackToggleDecision.CANCEL_PREVIEW_PENDING
    } else {
        BookPlaybackToggleDecision.START_PREVIEW_OR_NOOP
    }
}

internal fun shouldShowPlaybackPause(facts: BookPlaybackToggleFacts): Boolean {
    if (facts.hasCurrentLocalOwnership) return !facts.localPlaybackPaused
    if (facts.contentReadiness == BookContentReadiness.PREVIEW) {
        return facts.playbackPreparing
    }
    return facts.formalIsPlaying
}

internal fun applyBookPlaybackToggleDecision(
    decision: BookPlaybackToggleDecision,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancelPreviewPending: () -> Unit,
    onStartPreview: () -> Unit
) {
    when (decision) {
        BookPlaybackToggleDecision.PAUSE_LOCAL,
        BookPlaybackToggleDecision.PAUSE_FORMAL -> onPause()

        BookPlaybackToggleDecision.RESUME_LOCAL,
        BookPlaybackToggleDecision.RESUME_OR_START_FORMAL -> onResume()

        BookPlaybackToggleDecision.CANCEL_PREVIEW_PENDING -> onCancelPreviewPending()
        BookPlaybackToggleDecision.START_PREVIEW_OR_NOOP -> onStartPreview()
    }
}

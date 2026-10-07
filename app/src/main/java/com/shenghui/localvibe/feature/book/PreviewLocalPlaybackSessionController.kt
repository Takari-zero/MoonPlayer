package com.shenghui.localvibe.feature.book

/** Ownership of the current sentence is exclusive: LOCAL or prepared fallback, never both. */
enum class PreviewLocalPlaybackOwnership {
    NONE,
    LOCAL,
    PREPARED_FALLBACK
}

enum class PreviewLocalPlaybackState {
    IDLE,
    LOCAL_STARTING,
    LOCAL_ACTIVE,
    LOCAL_PAUSED,
    LOCAL_DRAINED_WAITING_PREPARED,
    PREPARED_VERIFIED_WAITING_DRAIN,
    PREPARED_FALLBACK_WAITING,
    STOPPED
}

enum class PreviewLocalOwnershipDirective {
    NONE,
    SUPPRESS_SAME_TARGET_PENDING,
    RESTORE_PREPARED_FALLBACK,
    CLEAR_LOCAL_OWNERSHIP
}

data class PreviewLocalSessionSnapshot(
    val state: PreviewLocalPlaybackState,
    val ownership: PreviewLocalPlaybackOwnership,
    val target: LocalPlaybackTarget?,
    val readerGeneration: Long?,
    val playbackSessionId: Long?,
    val playbackIntentPlaying: Boolean,
    val localPlaybackPaused: Boolean,
    val fullReaderStatus: FullReaderStatus,
    val preparedVerification: PreparedTargetVerificationStatus,
    val localSentenceDrained: Boolean,
    val formalContinuationClaimed: Boolean
)

data class PreviewLocalSessionTransitionResult(
    val snapshot: PreviewLocalSessionSnapshot,
    val ownershipDirective: PreviewLocalOwnershipDirective = PreviewLocalOwnershipDirective.NONE,
    val continuationDecision: PreviewPlaybackContinuationDecision? = null,
    val stale: Boolean = false
)

/** Pure lifecycle/ownership state machine for one preview sentence. */
class PreviewLocalPlaybackSessionController {
    private var state = PreviewLocalPlaybackState.IDLE
    private var target: LocalPlaybackTarget? = null
    private var readerGeneration: Long? = null
    private var playbackSessionId: Long? = null
    private var playbackIntentPlaying = false
    private var localPlaybackPaused = false
    private var fullReaderStatus = FullReaderStatus.LOADING
    private var preparedVerification = PreparedTargetVerificationStatus.UNKNOWN
    private var formalContinuationClaimed = false
    private var released = false

    fun snapshot(): PreviewLocalSessionSnapshot = PreviewLocalSessionSnapshot(
        state = state,
        ownership = state.ownership(),
        target = target,
        readerGeneration = readerGeneration,
        playbackSessionId = playbackSessionId,
        playbackIntentPlaying = playbackIntentPlaying,
        localPlaybackPaused = localPlaybackPaused,
        fullReaderStatus = fullReaderStatus,
        preparedVerification = preparedVerification,
        localSentenceDrained = state == PreviewLocalPlaybackState.LOCAL_DRAINED_WAITING_PREPARED,
        formalContinuationClaimed = formalContinuationClaimed
    )

    fun beginLocalOwnership(
        localTarget: LocalPlaybackTarget,
        readerGeneration: Long,
        playbackSessionId: Long,
        playbackIntentPlaying: Boolean
    ): PreviewLocalSessionTransitionResult {
        if (released || localTarget.generation != readerGeneration) {
            return staleResult()
        }
        target = localTarget
        this.readerGeneration = readerGeneration
        this.playbackSessionId = playbackSessionId
        this.playbackIntentPlaying = playbackIntentPlaying
        localPlaybackPaused = false
        fullReaderStatus = FullReaderStatus.LOADING
        preparedVerification = PreparedTargetVerificationStatus.UNKNOWN
        formalContinuationClaimed = false
        state = PreviewLocalPlaybackState.LOCAL_STARTING
        return result(PreviewLocalOwnershipDirective.SUPPRESS_SAME_TARGET_PENDING)
    }

    fun onLocalStarted(
        readerGeneration: Long,
        playbackSessionId: Long
    ): PreviewLocalSessionTransitionResult {
        if (!isCurrent(readerGeneration, playbackSessionId)) return staleResult()
        if (state != PreviewLocalPlaybackState.LOCAL_STARTING) return result()
        state = stateAfterLocalStart()
        return result(continuationDecision = planLocalContinuation())
    }

    fun onLocalStartFailed(
        readerGeneration: Long,
        playbackSessionId: Long
    ): PreviewLocalSessionTransitionResult {
        if (!isCurrent(readerGeneration, playbackSessionId)) return staleResult()
        if (state != PreviewLocalPlaybackState.LOCAL_STARTING) return result()
        state = PreviewLocalPlaybackState.PREPARED_FALLBACK_WAITING
        return result(
            ownershipDirective = PreviewLocalOwnershipDirective.RESTORE_PREPARED_FALLBACK,
            continuationDecision = fallbackDecision()
        )
    }

    fun onLocalDrained(
        readerGeneration: Long,
        playbackSessionId: Long
    ): PreviewLocalSessionTransitionResult {
        if (!isCurrent(readerGeneration, playbackSessionId)) return staleResult()
        if (state != PreviewLocalPlaybackState.LOCAL_STARTING &&
            state != PreviewLocalPlaybackState.LOCAL_ACTIVE &&
            state != PreviewLocalPlaybackState.PREPARED_VERIFIED_WAITING_DRAIN
        ) {
            return result()
        }
        state = PreviewLocalPlaybackState.LOCAL_DRAINED_WAITING_PREPARED
        return result(continuationDecision = planLocalContinuation())
    }

    fun onLocalPaused(
        readerGeneration: Long,
        playbackSessionId: Long
    ): PreviewLocalSessionTransitionResult {
        if (!isCurrent(readerGeneration, playbackSessionId)) return staleResult()
        if (state != PreviewLocalPlaybackState.LOCAL_STARTING &&
            state != PreviewLocalPlaybackState.LOCAL_ACTIVE &&
            state != PreviewLocalPlaybackState.PREPARED_VERIFIED_WAITING_DRAIN
        ) {
            return result()
        }
        localPlaybackPaused = true
        playbackIntentPlaying = false
        state = PreviewLocalPlaybackState.LOCAL_PAUSED
        return result(continuationDecision = PreviewPlaybackContinuationDecision.KEEP_CURRENT_PLAYBACK)
    }

    fun onLocalResumed(
        readerGeneration: Long,
        playbackSessionId: Long
    ): PreviewLocalSessionTransitionResult {
        if (!isCurrent(readerGeneration, playbackSessionId)) return staleResult()
        if (state != PreviewLocalPlaybackState.LOCAL_PAUSED) return result()
        localPlaybackPaused = false
        playbackIntentPlaying = true
        state = stateAfterLocalResume()
        return result(continuationDecision = planLocalContinuation())
    }

    fun onFullReaderReady(
        readerGeneration: Long,
        playbackSessionId: Long,
        verification: PreparedTargetVerificationStatus
    ): PreviewLocalSessionTransitionResult {
        if (!isCurrent(readerGeneration, playbackSessionId)) return staleResult()
        fullReaderStatus = FullReaderStatus.READY
        preparedVerification = verification
        if (state == PreviewLocalPlaybackState.PREPARED_FALLBACK_WAITING) {
            return if (verification == PreparedTargetVerificationStatus.MATCHED) {
                result(
                    ownershipDirective = PreviewLocalOwnershipDirective.RESTORE_PREPARED_FALLBACK,
                    continuationDecision = PreviewPlaybackContinuationDecision.KEEP_CURRENT_PLAYBACK
                )
            } else {
                result(continuationDecision = fallbackDecision())
            }
        }
        state = stateAfterPreparedUpdate()
        return result(continuationDecision = planLocalContinuation())
    }

    fun onFullLoadFailed(
        readerGeneration: Long,
        playbackSessionId: Long
    ): PreviewLocalSessionTransitionResult {
        if (!isCurrent(readerGeneration, playbackSessionId)) return staleResult()
        fullReaderStatus = FullReaderStatus.FAILED
        preparedVerification = PreparedTargetVerificationStatus.REJECTED
        if (state == PreviewLocalPlaybackState.PREPARED_FALLBACK_WAITING) {
            return result(continuationDecision = PreviewPlaybackContinuationDecision.STOP_AUTO_CONTINUE)
        }
        state = stateAfterPreparedUpdate()
        return result(continuationDecision = planLocalContinuation())
    }

    fun onPlaybackIntentChanged(
        readerGeneration: Long,
        playbackSessionId: Long,
        playbackIntentPlaying: Boolean
    ): PreviewLocalSessionTransitionResult {
        if (!isCurrent(readerGeneration, playbackSessionId)) return staleResult()
        this.playbackIntentPlaying = playbackIntentPlaying
        val decision = if (state == PreviewLocalPlaybackState.PREPARED_FALLBACK_WAITING) {
            fallbackDecision()
        } else {
            planLocalContinuation()
        }
        return result(continuationDecision = decision)
    }

    fun claimFormalContinuation(readerGeneration: Long, playbackSessionId: Long): Boolean {
        if (!isCurrent(readerGeneration, playbackSessionId) || formalContinuationClaimed) return false
        if (planLocalContinuation() != PreviewPlaybackContinuationDecision.ALLOW_FORMAL_NEXT) return false
        formalContinuationClaimed = true
        return true
    }

    fun release(): PreviewLocalSessionTransitionResult {
        if (released) return result()
        released = true
        state = PreviewLocalPlaybackState.STOPPED
        target = null
        readerGeneration = null
        playbackSessionId = null
        playbackIntentPlaying = false
        localPlaybackPaused = false
        return result(PreviewLocalOwnershipDirective.CLEAR_LOCAL_OWNERSHIP)
    }

    private fun isCurrent(callbackGeneration: Long, callbackSessionId: Long): Boolean {
        return !released &&
            callbackGeneration == readerGeneration &&
            callbackSessionId == playbackSessionId
    }

    private fun stateAfterPreparedUpdate(): PreviewLocalPlaybackState {
        if (state == PreviewLocalPlaybackState.LOCAL_STARTING) {
            return PreviewLocalPlaybackState.LOCAL_STARTING
        }
        if (state == PreviewLocalPlaybackState.LOCAL_DRAINED_WAITING_PREPARED) {
            return PreviewLocalPlaybackState.LOCAL_DRAINED_WAITING_PREPARED
        }
        if (state == PreviewLocalPlaybackState.LOCAL_PAUSED) {
            return PreviewLocalPlaybackState.LOCAL_PAUSED
        }
        return if (
            fullReaderStatus == FullReaderStatus.READY &&
            preparedVerification == PreparedTargetVerificationStatus.MATCHED
        ) {
            PreviewLocalPlaybackState.PREPARED_VERIFIED_WAITING_DRAIN
        } else {
            PreviewLocalPlaybackState.LOCAL_ACTIVE
        }
    }

    private fun stateAfterLocalStart(): PreviewLocalPlaybackState {
        return if (
            fullReaderStatus == FullReaderStatus.READY &&
            preparedVerification == PreparedTargetVerificationStatus.MATCHED
        ) {
            PreviewLocalPlaybackState.PREPARED_VERIFIED_WAITING_DRAIN
        } else {
            PreviewLocalPlaybackState.LOCAL_ACTIVE
        }
    }

    private fun stateAfterLocalResume(): PreviewLocalPlaybackState {
        return if (
            fullReaderStatus == FullReaderStatus.READY &&
            preparedVerification == PreparedTargetVerificationStatus.MATCHED
        ) {
            PreviewLocalPlaybackState.PREPARED_VERIFIED_WAITING_DRAIN
        } else {
            PreviewLocalPlaybackState.LOCAL_ACTIVE
        }
    }

    private fun planLocalContinuation(): PreviewPlaybackContinuationDecision {
        val localGeneration = readerGeneration ?: return PreviewPlaybackContinuationDecision.IGNORE_STALE
        val localSession = playbackSessionId ?: return PreviewPlaybackContinuationDecision.IGNORE_STALE
        if (localPlaybackPaused) {
            return PreviewPlaybackContinuationDecision.KEEP_CURRENT_PLAYBACK
        }
        return PreviewPlaybackContinuationPlanner.plan(
            LocalPlaybackLifecycleSnapshot(
                localTargetGeneration = localGeneration,
                localSessionId = localSession,
                currentReaderGeneration = localGeneration,
                currentPlaybackSessionId = localSession,
                playbackIntentPlaying = playbackIntentPlaying,
                localSentenceDrained = state == PreviewLocalPlaybackState.LOCAL_DRAINED_WAITING_PREPARED,
                fullReaderStatus = fullReaderStatus,
                preparedVerification = preparedVerification
            )
        )
    }

    private fun fallbackDecision(): PreviewPlaybackContinuationDecision {
        return when {
            !playbackIntentPlaying -> PreviewPlaybackContinuationDecision.STOP_AUTO_CONTINUE
            fullReaderStatus == FullReaderStatus.READY &&
                preparedVerification == PreparedTargetVerificationStatus.MATCHED ->
                PreviewPlaybackContinuationDecision.KEEP_CURRENT_PLAYBACK
            fullReaderStatus == FullReaderStatus.FAILED ||
                preparedVerification == PreparedTargetVerificationStatus.REJECTED ->
                PreviewPlaybackContinuationDecision.STOP_AUTO_CONTINUE
            else -> PreviewPlaybackContinuationDecision.WAIT_FOR_FULL_READY
        }
    }

    private fun staleResult(): PreviewLocalSessionTransitionResult {
        return PreviewLocalSessionTransitionResult(
            snapshot = snapshot(),
            continuationDecision = PreviewPlaybackContinuationDecision.IGNORE_STALE,
            stale = true
        )
    }

    private fun result(
        ownershipDirective: PreviewLocalOwnershipDirective = PreviewLocalOwnershipDirective.NONE,
        continuationDecision: PreviewPlaybackContinuationDecision? = null
    ): PreviewLocalSessionTransitionResult {
        return PreviewLocalSessionTransitionResult(
            snapshot = snapshot(),
            ownershipDirective = ownershipDirective,
            continuationDecision = continuationDecision
        )
    }

    private fun PreviewLocalPlaybackState.ownership(): PreviewLocalPlaybackOwnership {
        return when (this) {
            PreviewLocalPlaybackState.LOCAL_STARTING,
            PreviewLocalPlaybackState.LOCAL_ACTIVE,
            PreviewLocalPlaybackState.LOCAL_PAUSED,
            PreviewLocalPlaybackState.LOCAL_DRAINED_WAITING_PREPARED,
            PreviewLocalPlaybackState.PREPARED_VERIFIED_WAITING_DRAIN ->
                PreviewLocalPlaybackOwnership.LOCAL

            PreviewLocalPlaybackState.PREPARED_FALLBACK_WAITING ->
                PreviewLocalPlaybackOwnership.PREPARED_FALLBACK

            PreviewLocalPlaybackState.IDLE,
            PreviewLocalPlaybackState.STOPPED -> PreviewLocalPlaybackOwnership.NONE
        }
    }
}

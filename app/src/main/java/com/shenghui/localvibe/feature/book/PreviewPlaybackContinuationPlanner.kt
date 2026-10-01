package com.shenghui.localvibe.feature.book

enum class FullReaderStatus {
    LOADING,
    READY,
    FAILED
}

enum class PreparedTargetVerificationStatus {
    UNKNOWN,
    MATCHED,
    REJECTED
}

data class LocalPlaybackLifecycleSnapshot(
    val localTargetGeneration: Long,
    val localSessionId: Long,
    val currentReaderGeneration: Long,
    val currentPlaybackSessionId: Long,
    val playbackIntentPlaying: Boolean,
    val localSentenceDrained: Boolean,
    val fullReaderStatus: FullReaderStatus,
    val preparedVerification: PreparedTargetVerificationStatus
)

enum class PreviewPlaybackContinuationDecision {
    KEEP_CURRENT_PLAYBACK,
    WAIT_FOR_FULL_READY,
    WAIT_FOR_PREPARED_VERIFICATION,
    ALLOW_FORMAL_NEXT,
    STOP_AUTO_CONTINUE,
    IGNORE_STALE
}

object PreviewPlaybackContinuationPlanner {
    fun plan(snapshot: LocalPlaybackLifecycleSnapshot): PreviewPlaybackContinuationDecision {
        if (snapshot.localTargetGeneration != snapshot.currentReaderGeneration) {
            return PreviewPlaybackContinuationDecision.IGNORE_STALE
        }
        if (snapshot.localSessionId != snapshot.currentPlaybackSessionId) {
            return PreviewPlaybackContinuationDecision.IGNORE_STALE
        }
        if (!snapshot.playbackIntentPlaying) {
            return PreviewPlaybackContinuationDecision.STOP_AUTO_CONTINUE
        }
        return when (snapshot.fullReaderStatus) {
            FullReaderStatus.FAILED -> if (snapshot.localSentenceDrained) {
                PreviewPlaybackContinuationDecision.STOP_AUTO_CONTINUE
            } else {
                PreviewPlaybackContinuationDecision.KEEP_CURRENT_PLAYBACK
            }

            FullReaderStatus.LOADING -> if (snapshot.localSentenceDrained) {
                PreviewPlaybackContinuationDecision.WAIT_FOR_FULL_READY
            } else {
                PreviewPlaybackContinuationDecision.KEEP_CURRENT_PLAYBACK
            }

            FullReaderStatus.READY -> when (snapshot.preparedVerification) {
                PreparedTargetVerificationStatus.UNKNOWN -> {
                    PreviewPlaybackContinuationDecision.WAIT_FOR_PREPARED_VERIFICATION
                }

                PreparedTargetVerificationStatus.REJECTED -> {
                    PreviewPlaybackContinuationDecision.STOP_AUTO_CONTINUE
                }

                PreparedTargetVerificationStatus.MATCHED -> if (snapshot.localSentenceDrained) {
                    PreviewPlaybackContinuationDecision.ALLOW_FORMAL_NEXT
                } else {
                    PreviewPlaybackContinuationDecision.KEEP_CURRENT_PLAYBACK
                }
            }
        }
    }
}

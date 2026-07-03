package com.shenghui.localvibe.feature.book.playback

class BookReaderAutoAdvanceCoordinator {
    var autoAdvanceEnabled: Boolean = false
        private set

    fun onPlayRequested() {
        autoAdvanceEnabled = true
    }

    fun onPauseRequested() {
        autoAdvanceEnabled = false
    }

    fun onStopRequested() {
        autoAdvanceEnabled = false
    }

    fun decide(
        completedTarget: BookReaderPlaybackTarget,
        currentTarget: BookReaderPlaybackTarget?,
        isPlaybackPaused: Boolean,
        isReaderPlaying: Boolean,
        activePlaybackEngineName: String,
        expectedPlaybackEngineName: String,
        nextTargetProvider: () -> BookReaderPlaybackTarget?,
    ): BookReaderAutoAdvanceDecision {
        if (!autoAdvanceEnabled) {
            return BookReaderAutoAdvanceDecision.Skip("auto_advance_disabled")
        }
        if (isPlaybackPaused) {
            return BookReaderAutoAdvanceDecision.Skip("paused")
        }
        if (!isReaderPlaying || activePlaybackEngineName != expectedPlaybackEngineName) {
            return BookReaderAutoAdvanceDecision.Skip("stopped")
        }
        if (currentTarget != null && currentTarget != completedTarget) {
            return BookReaderAutoAdvanceDecision.Skip("stale_target")
        }

        val nextTarget = nextTargetProvider()
        return if (nextTarget == null) {
            BookReaderAutoAdvanceDecision.StopAtEnd
        } else {
            BookReaderAutoAdvanceDecision.Advance(nextTarget)
        }
    }
}

sealed interface BookReaderAutoAdvanceDecision {
    data class Advance(val target: BookReaderPlaybackTarget) : BookReaderAutoAdvanceDecision
    data class Skip(val reason: String) : BookReaderAutoAdvanceDecision
    data object StopAtEnd : BookReaderAutoAdvanceDecision
}

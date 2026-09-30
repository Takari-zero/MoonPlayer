package com.shenghui.localvibe.feature.book

/** Distinguishes cached display availability from the metadata required for playback. */
enum class BookContentReadiness {
    PREVIEW,
    PLAYBACK_READY
}

/** Stable sentence identity captured while the full playback mapping is still loading. */
data class PendingBookPlaybackTarget(
    val paragraphIndex: Int,
    val sentenceIndexInParagraph: Int,
    val stableTextHash: Int,
    val source: PendingBookPlaybackSource,
    val playRequested: Boolean
)

enum class PendingBookPlaybackSource {
    USER_SENTENCE_TAP,
    PLAY_BUTTON,
    RESTORE
}

object PendingBookPlaybackTargetResolver {
    data class ConsumeResult(
        val pendingTarget: PendingBookPlaybackTarget?,
        val playbackIntentPlaying: Boolean,
        val resolvedTarget: BookPlaybackSentenceTarget?,
        val preparedNotReady: Boolean,
        val terminalRejected: Boolean
    )

    fun replace(
        current: PendingBookPlaybackTarget?,
        next: PendingBookPlaybackTarget
    ): PendingBookPlaybackTarget = next

    fun setPlayRequested(
        current: PendingBookPlaybackTarget,
        playRequested: Boolean
    ): PendingBookPlaybackTarget = current.copy(playRequested = playRequested)

    fun resolve(
        pending: PendingBookPlaybackTarget,
        chapterTargets: List<BookPlaybackSentenceTarget>
    ): BookPlaybackSentenceTarget? {
        return chapterTargets.firstOrNull {
            it.paragraphIndex == pending.paragraphIndex &&
                it.sentenceIndexInParagraph == pending.sentenceIndexInParagraph
        }
    }

    fun consume(
        pending: PendingBookPlaybackTarget?,
        readiness: BookContentReadiness,
        preparedAvailable: Boolean,
        preparedTargets: List<BookPlaybackSentenceTarget>,
        currentPlaybackIntentPlaying: Boolean
    ): ConsumeResult {
        if (pending == null) {
            return ConsumeResult(
                pendingTarget = null,
                playbackIntentPlaying = currentPlaybackIntentPlaying,
                resolvedTarget = null,
                preparedNotReady = false,
                terminalRejected = false
            )
        }
        if (readiness != BookContentReadiness.PLAYBACK_READY || !preparedAvailable) {
            return ConsumeResult(
                pendingTarget = pending,
                playbackIntentPlaying = currentPlaybackIntentPlaying,
                resolvedTarget = null,
                preparedNotReady = true,
                terminalRejected = false
            )
        }
        val resolved = resolve(pending, preparedTargets)
        if (resolved == null) {
            return ConsumeResult(
                pendingTarget = null,
                playbackIntentPlaying = false,
                resolvedTarget = null,
                preparedNotReady = false,
                terminalRejected = true
            )
        }
        return ConsumeResult(
            pendingTarget = null,
            playbackIntentPlaying = pending.playRequested,
            resolvedTarget = resolved,
            preparedNotReady = false,
            terminalRejected = false
        )
    }
}

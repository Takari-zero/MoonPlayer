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
}

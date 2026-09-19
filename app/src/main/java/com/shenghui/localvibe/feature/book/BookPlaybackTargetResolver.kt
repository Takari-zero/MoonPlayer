package com.shenghui.localvibe.feature.book

/** Metadata carried by a displayed sentence before it enters playback. */
data class BookPlaybackSentenceTarget(
    val paragraphIndex: Int,
    val sentenceIndexInParagraph: Int,
    val chapterSentenceIndex: Int
)

/** Resolves display identity to the chapter-local index required by playback. */
object BookPlaybackTargetResolver {
    fun resolveChapterSentenceIndex(
        paragraphIndex: Int,
        sentenceIndexInParagraph: Int,
        cachedTargets: List<BookPlaybackSentenceTarget>,
        chapterTargets: List<BookPlaybackSentenceTarget>
    ): Int? {
        cachedTargets.firstOrNull {
            it.paragraphIndex == paragraphIndex &&
                it.sentenceIndexInParagraph == sentenceIndexInParagraph
        }?.let { return it.chapterSentenceIndex }

        return chapterTargets.firstOrNull {
            it.paragraphIndex == paragraphIndex &&
                it.sentenceIndexInParagraph == sentenceIndexInParagraph
        }?.chapterSentenceIndex
    }
}

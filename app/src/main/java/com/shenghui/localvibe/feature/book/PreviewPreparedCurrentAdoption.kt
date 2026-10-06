package com.shenghui.localvibe.feature.book

internal data class PreviewPreparedCurrentAdoptionPlan(
    val paragraphIndex: Int,
    val sentenceIndexInParagraph: Int,
    val chapterSentenceIndex: Int
)

internal object PreviewPreparedCurrentAdoption {
    fun plan(preparedCurrent: ReaderSentence?): PreviewPreparedCurrentAdoptionPlan? {
        return preparedCurrent?.takeIf {
            it.paragraphIndex >= 0 && it.sentenceIndexInParagraph >= 0 && it.chapterSentenceIndex >= 0
        }?.let {
            PreviewPreparedCurrentAdoptionPlan(
                paragraphIndex = it.paragraphIndex,
                sentenceIndexInParagraph = it.sentenceIndexInParagraph,
                chapterSentenceIndex = it.chapterSentenceIndex
            )
        }
    }
}

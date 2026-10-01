package com.shenghui.localvibe.feature.book

data class PreparedPlaybackTargetIdentity(
    val paragraphIndex: Int,
    val sentenceIndexInParagraph: Int,
    val chapterSentenceIndex: Int,
    val text: String,
    val stableTextHash: Int
)

enum class PreviewPreparedRejectionReason {
    BOOK_URI_MISMATCH,
    GENERATION_MISMATCH,
    TARGET_MISSING,
    PARAGRAPH_INDEX_MISMATCH,
    SENTENCE_INDEX_MISMATCH,
    CHAPTER_SENTENCE_INDEX_MISMATCH,
    TEXT_HASH_MISMATCH,
    TEXT_MISMATCH
}

sealed interface PreviewPreparedVerification {
    data object Matched : PreviewPreparedVerification

    data class Rejected(
        val reason: PreviewPreparedRejectionReason
    ) : PreviewPreparedVerification
}

object PreviewToPreparedTargetVerifier {
    fun findPreparedTarget(
        localTarget: LocalPlaybackTarget,
        preparedTargets: List<PreparedPlaybackTargetIdentity>
    ): PreparedPlaybackTargetIdentity? {
        return preparedTargets.firstOrNull { prepared ->
            prepared.paragraphIndex == localTarget.paragraphIndex &&
                prepared.sentenceIndexInParagraph == localTarget.sentenceIndexInParagraph
        }
    }

    fun verify(
        localTarget: LocalPlaybackTarget,
        currentReaderGeneration: Long,
        preparedBookUri: String,
        preparedTarget: PreparedPlaybackTargetIdentity?
    ): PreviewPreparedVerification {
        if (localTarget.bookUri != preparedBookUri) {
            return rejected(PreviewPreparedRejectionReason.BOOK_URI_MISMATCH)
        }
        if (localTarget.generation != currentReaderGeneration) {
            return rejected(PreviewPreparedRejectionReason.GENERATION_MISMATCH)
        }
        if (preparedTarget == null) {
            return rejected(PreviewPreparedRejectionReason.TARGET_MISSING)
        }
        if (localTarget.paragraphIndex != preparedTarget.paragraphIndex) {
            return rejected(PreviewPreparedRejectionReason.PARAGRAPH_INDEX_MISMATCH)
        }
        if (localTarget.sentenceIndexInParagraph != preparedTarget.sentenceIndexInParagraph) {
            return rejected(PreviewPreparedRejectionReason.SENTENCE_INDEX_MISMATCH)
        }
        if (localTarget.chapterSentenceIndex != preparedTarget.chapterSentenceIndex) {
            return rejected(PreviewPreparedRejectionReason.CHAPTER_SENTENCE_INDEX_MISMATCH)
        }
        if (localTarget.stableTextHash != preparedTarget.stableTextHash) {
            return rejected(PreviewPreparedRejectionReason.TEXT_HASH_MISMATCH)
        }
        if (localTarget.text != preparedTarget.text) {
            return rejected(PreviewPreparedRejectionReason.TEXT_MISMATCH)
        }
        return PreviewPreparedVerification.Matched
    }

    private fun rejected(reason: PreviewPreparedRejectionReason): PreviewPreparedVerification {
        return PreviewPreparedVerification.Rejected(reason)
    }
}

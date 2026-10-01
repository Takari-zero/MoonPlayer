package com.shenghui.localvibe.feature.book

data class PreviewContentIdentity(
    val bookUri: String,
    val size: Long?,
    val modifiedAt: Long?
)

data class CachedPreviewPlaybackCandidate(
    val paragraphIndex: Int,
    val sentenceIndexInParagraph: Int,
    val chapterSentenceIndex: Int,
    val text: String,
    val stableTextHash: Int,
    val originReaderGeneration: Long
)

enum class LocalPlaybackTargetSource {
    CACHED_PREVIEW
}

data class LocalPlaybackTarget(
    val bookUri: String,
    val paragraphIndex: Int,
    val sentenceIndexInParagraph: Int,
    val chapterSentenceIndex: Int,
    val text: String,
    val stableTextHash: Int,
    val generation: Long,
    val source: LocalPlaybackTargetSource
)

enum class PreviewPlaybackRejectionReason {
    BOOK_URI_MISMATCH,
    CONTENT_VERSION_UNAVAILABLE,
    CONTENT_SIZE_MISMATCH,
    CONTENT_MODIFIED_AT_MISMATCH,
    GENERATION_MISMATCH,
    TEXT_HASH_MISMATCH,
    TEXT_MISMATCH,
    INVALID_COORDINATES,
    PLAY_NOT_REQUESTED
}

sealed interface PreviewPlaybackResolution {
    data class Allowed(val target: LocalPlaybackTarget) : PreviewPlaybackResolution
    data class Rejected(val reason: PreviewPlaybackRejectionReason) : PreviewPlaybackResolution
}

object PreviewPlaybackTargetResolver {
    fun resolve(
        candidate: CachedPreviewPlaybackCandidate,
        cachedIdentity: PreviewContentIdentity,
        currentIdentity: PreviewContentIdentity,
        currentReaderGeneration: Long,
        expectedText: String,
        expectedStableTextHash: Int,
        playRequested: Boolean
    ): PreviewPlaybackResolution {
        if (!playRequested) {
            return PreviewPlaybackResolution.Rejected(PreviewPlaybackRejectionReason.PLAY_NOT_REQUESTED)
        }
        if (
            candidate.paragraphIndex < 0 ||
            candidate.sentenceIndexInParagraph < 0 ||
            candidate.chapterSentenceIndex < 0
        ) {
            return PreviewPlaybackResolution.Rejected(PreviewPlaybackRejectionReason.INVALID_COORDINATES)
        }
        if (cachedIdentity.bookUri != currentIdentity.bookUri) {
            return PreviewPlaybackResolution.Rejected(PreviewPlaybackRejectionReason.BOOK_URI_MISMATCH)
        }
        val cachedSize = cachedIdentity.size
        val currentSize = currentIdentity.size
        val cachedModifiedAt = cachedIdentity.modifiedAt
        val currentModifiedAt = currentIdentity.modifiedAt
        if (
            cachedSize == null ||
            currentSize == null ||
            cachedModifiedAt == null ||
            currentModifiedAt == null
        ) {
            return PreviewPlaybackResolution.Rejected(
                PreviewPlaybackRejectionReason.CONTENT_VERSION_UNAVAILABLE
            )
        }
        if (cachedSize != currentSize) {
            return PreviewPlaybackResolution.Rejected(PreviewPlaybackRejectionReason.CONTENT_SIZE_MISMATCH)
        }
        if (cachedModifiedAt != currentModifiedAt) {
            return PreviewPlaybackResolution.Rejected(
                PreviewPlaybackRejectionReason.CONTENT_MODIFIED_AT_MISMATCH
            )
        }
        if (candidate.originReaderGeneration != currentReaderGeneration) {
            return PreviewPlaybackResolution.Rejected(PreviewPlaybackRejectionReason.GENERATION_MISMATCH)
        }
        if (candidate.stableTextHash != expectedStableTextHash) {
            return PreviewPlaybackResolution.Rejected(PreviewPlaybackRejectionReason.TEXT_HASH_MISMATCH)
        }
        if (candidate.text != expectedText) {
            return PreviewPlaybackResolution.Rejected(PreviewPlaybackRejectionReason.TEXT_MISMATCH)
        }
        return PreviewPlaybackResolution.Allowed(
            LocalPlaybackTarget(
                bookUri = currentIdentity.bookUri,
                paragraphIndex = candidate.paragraphIndex,
                sentenceIndexInParagraph = candidate.sentenceIndexInParagraph,
                chapterSentenceIndex = candidate.chapterSentenceIndex,
                text = candidate.text,
                stableTextHash = candidate.stableTextHash,
                generation = currentReaderGeneration,
                source = LocalPlaybackTargetSource.CACHED_PREVIEW
            )
        )
    }
}

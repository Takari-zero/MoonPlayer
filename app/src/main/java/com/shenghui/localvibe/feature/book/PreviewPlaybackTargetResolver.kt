package com.shenghui.localvibe.feature.book

data class PreviewContentIdentity(
    val bookUri: String,
    val version: PreviewContentVersion
) {
    constructor(bookUri: String, size: Long?, modifiedAt: Long?) : this(
        bookUri = bookUri,
        version = PreviewContentVersion.metadata(size, modifiedAt)
    )
}

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
    CONTENT_VERSION_KIND_MISMATCH,
    CONTENT_MODIFIED_AT_MISMATCH,
    CONTENT_FINGERPRINT_MISMATCH,
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
        PreviewContentVersionComparator.compare(cachedIdentity.version, currentIdentity.version)?.let {
            return PreviewPlaybackResolution.Rejected(it)
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

package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewPlaybackTargetResolverTest {
    private val text = "cached sentence"
    private val textHash = text.hashCode()
    private val candidate = CachedPreviewPlaybackCandidate(
        paragraphIndex = 12,
        sentenceIndexInParagraph = 3,
        chapterSentenceIndex = 27,
        text = text,
        stableTextHash = textHash,
        originReaderGeneration = 8L
    )
    private val identity = PreviewContentIdentity(
        bookUri = "content://books/current",
        size = 4096L,
        modifiedAt = 123456L
    )

    @Test
    fun matchingIdentityVersionAndTextAreAllowed() {
        assertTrue(resolve() is PreviewPlaybackResolution.Allowed)
    }

    @Test
    fun rejectsBookUriMismatch() {
        assertRejected(
            PreviewPlaybackRejectionReason.BOOK_URI_MISMATCH,
            currentIdentity = identity.copy(bookUri = "content://books/other")
        )
    }

    @Test
    fun rejectsUnavailableCachedSize() {
        assertRejected(
            PreviewPlaybackRejectionReason.CONTENT_VERSION_UNAVAILABLE,
            cachedIdentity = identity.copy(size = null)
        )
    }

    @Test
    fun rejectsUnavailableCachedModifiedAt() {
        assertRejected(
            PreviewPlaybackRejectionReason.CONTENT_VERSION_UNAVAILABLE,
            cachedIdentity = identity.copy(modifiedAt = null)
        )
    }

    @Test
    fun rejectsContentSizeMismatch() {
        assertRejected(
            PreviewPlaybackRejectionReason.CONTENT_SIZE_MISMATCH,
            currentIdentity = identity.copy(size = identity.size!! + 1L)
        )
    }

    @Test
    fun rejectsContentModifiedAtMismatch() {
        assertRejected(
            PreviewPlaybackRejectionReason.CONTENT_MODIFIED_AT_MISMATCH,
            currentIdentity = identity.copy(modifiedAt = identity.modifiedAt!! + 1L)
        )
    }

    @Test
    fun rejectsGenerationMismatch() {
        assertRejected(
            PreviewPlaybackRejectionReason.GENERATION_MISMATCH,
            currentReaderGeneration = candidate.originReaderGeneration + 1L
        )
    }

    @Test
    fun persistentIdentityWithoutGenerationCanHydrateIntoCurrentReaderGeneration() {
        val hydratedCandidate = candidate.copy(originReaderGeneration = 41L)

        val result = resolve(
            candidate = hydratedCandidate,
            currentReaderGeneration = 41L
        ) as PreviewPlaybackResolution.Allowed

        assertEquals(41L, result.target.generation)
    }

    @Test
    fun hydratedCandidateIsRejectedAfterReaderGenerationAdvances() {
        val hydratedCandidate = candidate.copy(originReaderGeneration = 41L)

        assertRejected(
            PreviewPlaybackRejectionReason.GENERATION_MISMATCH,
            candidate = hydratedCandidate,
            currentReaderGeneration = 42L
        )
    }

    @Test
    fun rejectsStableTextHashMismatch() {
        assertRejected(
            PreviewPlaybackRejectionReason.TEXT_HASH_MISMATCH,
            expectedStableTextHash = textHash + 1
        )
    }

    @Test
    fun rejectsCompleteTextMismatch() {
        assertRejected(
            PreviewPlaybackRejectionReason.TEXT_MISMATCH,
            expectedText = "different sentence"
        )
    }

    @Test
    fun rejectsNegativeParagraphIndex() {
        assertRejected(
            PreviewPlaybackRejectionReason.INVALID_COORDINATES,
            candidate = candidate.copy(paragraphIndex = -1)
        )
    }

    @Test
    fun rejectsNegativeSentenceIndex() {
        assertRejected(
            PreviewPlaybackRejectionReason.INVALID_COORDINATES,
            candidate = candidate.copy(sentenceIndexInParagraph = -1)
        )
    }

    @Test
    fun rejectsWhenPlayWasNotRequested() {
        assertRejected(PreviewPlaybackRejectionReason.PLAY_NOT_REQUESTED, playRequested = false)
    }

    @Test
    fun allowedTargetPreservesSentenceIdentity() {
        val result = resolve() as PreviewPlaybackResolution.Allowed

        assertEquals(identity.bookUri, result.target.bookUri)
        assertEquals(candidate.paragraphIndex, result.target.paragraphIndex)
        assertEquals(candidate.sentenceIndexInParagraph, result.target.sentenceIndexInParagraph)
        assertEquals(candidate.chapterSentenceIndex, result.target.chapterSentenceIndex)
        assertEquals(candidate.text, result.target.text)
        assertEquals(candidate.stableTextHash, result.target.stableTextHash)
        assertEquals(candidate.originReaderGeneration, result.target.generation)
    }

    @Test
    fun allowedTargetIsExplicitlyFromCachedPreview() {
        val result = resolve() as PreviewPlaybackResolution.Allowed

        assertEquals(LocalPlaybackTargetSource.CACHED_PREVIEW, result.target.source)
    }

    private fun resolve(
        candidate: CachedPreviewPlaybackCandidate = this.candidate,
        cachedIdentity: PreviewContentIdentity = identity,
        currentIdentity: PreviewContentIdentity = identity,
        currentReaderGeneration: Long = candidate.originReaderGeneration,
        expectedText: String = text,
        expectedStableTextHash: Int = textHash,
        playRequested: Boolean = true
    ): PreviewPlaybackResolution {
        return PreviewPlaybackTargetResolver.resolve(
            candidate = candidate,
            cachedIdentity = cachedIdentity,
            currentIdentity = currentIdentity,
            currentReaderGeneration = currentReaderGeneration,
            expectedText = expectedText,
            expectedStableTextHash = expectedStableTextHash,
            playRequested = playRequested
        )
    }

    private fun assertRejected(
        expected: PreviewPlaybackRejectionReason,
        candidate: CachedPreviewPlaybackCandidate = this.candidate,
        cachedIdentity: PreviewContentIdentity = identity,
        currentIdentity: PreviewContentIdentity = identity,
        currentReaderGeneration: Long = candidate.originReaderGeneration,
        expectedText: String = text,
        expectedStableTextHash: Int = textHash,
        playRequested: Boolean = true
    ) {
        val result = resolve(
            candidate = candidate,
            cachedIdentity = cachedIdentity,
            currentIdentity = currentIdentity,
            currentReaderGeneration = currentReaderGeneration,
            expectedText = expectedText,
            expectedStableTextHash = expectedStableTextHash,
            playRequested = playRequested
        )

        assertEquals(PreviewPlaybackResolution.Rejected(expected), result)
    }
}

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
        version = PreviewContentVersion.metadata(4096L, 123456L)
    )
    private val fingerprint = PreviewContentVersion.fingerprint(4096L, "a".repeat(64))

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
    fun rejectsUnavailableCachedVersion() {
        assertRejected(
            PreviewPlaybackRejectionReason.CONTENT_VERSION_UNAVAILABLE,
            cachedIdentity = identity.copy(version = PreviewContentVersion.Unavailable)
        )
    }

    @Test
    fun rejectsUnavailableCurrentVersion() {
        assertRejected(
            PreviewPlaybackRejectionReason.CONTENT_VERSION_UNAVAILABLE,
            currentIdentity = identity.copy(version = PreviewContentVersion.Unavailable)
        )
    }

    @Test
    fun rejectsContentSizeMismatch() {
        assertRejected(
            PreviewPlaybackRejectionReason.CONTENT_SIZE_MISMATCH,
            currentIdentity = identity.copy(version = PreviewContentVersion.metadata(4097L, 123456L))
        )
    }

    @Test
    fun rejectsContentModifiedAtMismatch() {
        assertRejected(
            PreviewPlaybackRejectionReason.CONTENT_MODIFIED_AT_MISMATCH,
            currentIdentity = identity.copy(version = PreviewContentVersion.metadata(4096L, 123457L))
        )
    }

    @Test
    fun matchingFingerprintVersionsAreAllowed() {
        val fingerprintIdentity = identity.copy(version = fingerprint)

        assertTrue(
            resolve(
                cachedIdentity = fingerprintIdentity,
                currentIdentity = fingerprintIdentity
            ) is PreviewPlaybackResolution.Allowed
        )
    }

    @Test
    fun rejectsFingerprintSizeMismatch() {
        assertRejected(
            PreviewPlaybackRejectionReason.CONTENT_SIZE_MISMATCH,
            cachedIdentity = identity.copy(version = fingerprint),
            currentIdentity = identity.copy(
                version = PreviewContentVersion.fingerprint(4097L, "a".repeat(64))
            )
        )
    }

    @Test
    fun rejectsFingerprintDigestMismatch() {
        assertRejected(
            PreviewPlaybackRejectionReason.CONTENT_FINGERPRINT_MISMATCH,
            cachedIdentity = identity.copy(version = fingerprint),
            currentIdentity = identity.copy(
                version = PreviewContentVersion.fingerprint(4096L, "b".repeat(64))
            )
        )
    }

    @Test
    fun rejectsContentVersionKindMismatch() {
        assertRejected(
            PreviewPlaybackRejectionReason.CONTENT_VERSION_KIND_MISMATCH,
            cachedIdentity = identity,
            currentIdentity = identity.copy(version = fingerprint)
        )
    }

    @Test
    fun uriMismatchPrecedesUnavailableVersion() {
        assertRejected(
            PreviewPlaybackRejectionReason.BOOK_URI_MISMATCH,
            cachedIdentity = identity.copy(version = PreviewContentVersion.Unavailable),
            currentIdentity = identity.copy(bookUri = "content://books/other")
        )
    }

    @Test
    fun unavailableVersionPrecedesGenerationMismatch() {
        assertRejected(
            PreviewPlaybackRejectionReason.CONTENT_VERSION_UNAVAILABLE,
            cachedIdentity = identity.copy(version = PreviewContentVersion.Unavailable),
            currentReaderGeneration = candidate.originReaderGeneration + 1L
        )
    }

    @Test
    fun sizeMismatchPrecedesVersionKindMismatch() {
        assertRejected(
            PreviewPlaybackRejectionReason.CONTENT_SIZE_MISMATCH,
            currentIdentity = identity.copy(
                version = PreviewContentVersion.fingerprint(4097L, "a".repeat(64))
            )
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

    @Test
    fun playIntentPrecedesCoordinatesAndVersion() {
        assertRejected(PreviewPlaybackRejectionReason.PLAY_NOT_REQUESTED,
            candidate = candidate.copy(paragraphIndex = -1),
            cachedIdentity = identity.copy(version = PreviewContentVersion.Unavailable),
            playRequested = false)
    }

    @Test
    fun coordinatesPrecedeUriAndVersion() {
        assertRejected(PreviewPlaybackRejectionReason.INVALID_COORDINATES,
            candidate = candidate.copy(paragraphIndex = -1),
            currentIdentity = identity.copy(bookUri = "other", version = PreviewContentVersion.Unavailable))
    }

    @Test
    fun fingerprintMismatchPrecedesGenerationAndText() {
        assertRejected(PreviewPlaybackRejectionReason.CONTENT_FINGERPRINT_MISMATCH,
            cachedIdentity = identity.copy(version = fingerprint),
            currentIdentity = identity.copy(version = PreviewContentVersion.fingerprint(4096L, "b".repeat(64))),
            currentReaderGeneration = 9L, expectedText = "other", expectedStableTextHash = 0)
    }

    @Test
    fun generationPrecedesTextHashAndText() {
        assertRejected(PreviewPlaybackRejectionReason.GENERATION_MISMATCH,
            currentReaderGeneration = 9L, expectedText = "other", expectedStableTextHash = 0)
    }

    @Test
    fun hashPrecedesText() {
        assertRejected(PreviewPlaybackRejectionReason.TEXT_HASH_MISMATCH,
            expectedText = "other", expectedStableTextHash = 0)
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

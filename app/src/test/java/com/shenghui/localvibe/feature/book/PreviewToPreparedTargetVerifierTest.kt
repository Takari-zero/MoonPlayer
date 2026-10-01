package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PreviewToPreparedTargetVerifierTest {
    private val text = "prepared sentence"
    private val textHash = text.hashCode()
    private val localTarget = LocalPlaybackTarget(
        bookUri = "content://books/current",
        paragraphIndex = 12,
        sentenceIndexInParagraph = 3,
        chapterSentenceIndex = 27,
        text = text,
        stableTextHash = textHash,
        generation = 8L,
        source = LocalPlaybackTargetSource.CACHED_PREVIEW
    )
    private val preparedTarget = PreparedPlaybackTargetIdentity(
        paragraphIndex = 12,
        sentenceIndexInParagraph = 3,
        chapterSentenceIndex = 27,
        text = text,
        stableTextHash = textHash
    )

    @Test
    fun completelyMatchingIdentityIsMatched() {
        assertEquals(PreviewPreparedVerification.Matched, verify())
    }

    @Test
    fun rejectsBookUriMismatch() {
        assertRejected(
            PreviewPreparedRejectionReason.BOOK_URI_MISMATCH,
            preparedBookUri = "content://books/other"
        )
    }

    @Test
    fun rejectsReaderGenerationMismatch() {
        assertRejected(
            PreviewPreparedRejectionReason.GENERATION_MISMATCH,
            currentReaderGeneration = localTarget.generation + 1L
        )
    }

    @Test
    fun rejectsMissingPreparedTarget() {
        assertRejected(PreviewPreparedRejectionReason.TARGET_MISSING, preparedTarget = null)
    }

    @Test
    fun rejectsParagraphIndexMismatch() {
        assertRejected(
            PreviewPreparedRejectionReason.PARAGRAPH_INDEX_MISMATCH,
            preparedTarget = preparedTarget.copy(paragraphIndex = preparedTarget.paragraphIndex + 1)
        )
    }

    @Test
    fun rejectsSentenceIndexMismatch() {
        assertRejected(
            PreviewPreparedRejectionReason.SENTENCE_INDEX_MISMATCH,
            preparedTarget = preparedTarget.copy(
                sentenceIndexInParagraph = preparedTarget.sentenceIndexInParagraph + 1
            )
        )
    }

    @Test
    fun rejectsChapterSentenceIndexMismatch() {
        assertRejected(
            PreviewPreparedRejectionReason.CHAPTER_SENTENCE_INDEX_MISMATCH,
            preparedTarget = preparedTarget.copy(
                chapterSentenceIndex = preparedTarget.chapterSentenceIndex + 1
            )
        )
    }

    @Test
    fun rejectsStableTextHashMismatch() {
        assertRejected(
            PreviewPreparedRejectionReason.TEXT_HASH_MISMATCH,
            preparedTarget = preparedTarget.copy(stableTextHash = preparedTarget.stableTextHash + 1)
        )
    }

    @Test
    fun matchingHashDoesNotHideFullTextMismatch() {
        assertRejected(
            PreviewPreparedRejectionReason.TEXT_MISMATCH,
            preparedTarget = preparedTarget.copy(text = "different sentence")
        )
    }

    @Test
    fun lookupUsesExactParagraphAndSentenceCoordinates() {
        val expected = preparedTarget.copy(text = "exact", stableTextHash = "exact".hashCode())
        val targets = listOf(
            preparedTarget.copy(paragraphIndex = 11),
            expected,
            preparedTarget.copy(sentenceIndexInParagraph = 4)
        )

        assertEquals(
            expected,
            PreviewToPreparedTargetVerifier.findPreparedTarget(localTarget, targets)
        )
    }

    @Test
    fun sameTextAtAnotherPositionIsNotSelected() {
        val targets = listOf(
            preparedTarget.copy(paragraphIndex = 99),
            preparedTarget.copy(sentenceIndexInParagraph = 99)
        )

        assertNull(PreviewToPreparedTargetVerifier.findPreparedTarget(localTarget, targets))
    }

    @Test
    fun cachedPreviewSourceDoesNotChangeIdentityVerification() {
        assertEquals(LocalPlaybackTargetSource.CACHED_PREVIEW, localTarget.source)
        assertEquals(PreviewPreparedVerification.Matched, verify())
    }

    @Test
    fun bookMismatchWinsBeforeGenerationMismatch() {
        assertRejected(
            PreviewPreparedRejectionReason.BOOK_URI_MISMATCH,
            currentReaderGeneration = localTarget.generation + 1L,
            preparedBookUri = "content://books/other",
            preparedTarget = null
        )
    }

    @Test
    fun generationMismatchWinsBeforeMissingTarget() {
        assertRejected(
            PreviewPreparedRejectionReason.GENERATION_MISMATCH,
            currentReaderGeneration = localTarget.generation + 1L,
            preparedTarget = null
        )
    }

    @Test
    fun paragraphMismatchWinsBeforeLaterCandidateMismatches() {
        assertRejected(
            PreviewPreparedRejectionReason.PARAGRAPH_INDEX_MISMATCH,
            preparedTarget = preparedTarget.copy(
                paragraphIndex = preparedTarget.paragraphIndex + 1,
                sentenceIndexInParagraph = preparedTarget.sentenceIndexInParagraph + 1,
                chapterSentenceIndex = preparedTarget.chapterSentenceIndex + 1,
                stableTextHash = preparedTarget.stableTextHash + 1,
                text = "different sentence"
            )
        )
    }

    private fun verify(
        localTarget: LocalPlaybackTarget = this.localTarget,
        currentReaderGeneration: Long = localTarget.generation,
        preparedBookUri: String = localTarget.bookUri,
        preparedTarget: PreparedPlaybackTargetIdentity? = this.preparedTarget
    ): PreviewPreparedVerification {
        return PreviewToPreparedTargetVerifier.verify(
            localTarget = localTarget,
            currentReaderGeneration = currentReaderGeneration,
            preparedBookUri = preparedBookUri,
            preparedTarget = preparedTarget
        )
    }

    private fun assertRejected(
        expected: PreviewPreparedRejectionReason,
        localTarget: LocalPlaybackTarget = this.localTarget,
        currentReaderGeneration: Long = localTarget.generation,
        preparedBookUri: String = localTarget.bookUri,
        preparedTarget: PreparedPlaybackTargetIdentity? = this.preparedTarget
    ) {
        assertEquals(
            PreviewPreparedVerification.Rejected(expected),
            verify(
                localTarget = localTarget,
                currentReaderGeneration = currentReaderGeneration,
                preparedBookUri = preparedBookUri,
                preparedTarget = preparedTarget
            )
        )
    }
}

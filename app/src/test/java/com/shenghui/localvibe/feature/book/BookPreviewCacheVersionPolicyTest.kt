package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class BookPreviewCacheVersionPolicyTest {
    private val cycle = ReaderContentCycle(1L, "content://books/a")
    private val fingerprint = PreviewContentVersion.fingerprint(42L, "ab".repeat(32))

    @Test
    fun completeMatchingMetadataUsesMetadata() {
        assertEquals(PreviewContentVersion.metadata(42L, 100L), prepare(42L, 100L).contentVersion)
    }

    @Test
    fun missingModifiedAtUsesSharedFingerprint() {
        assertSame(fingerprint, prepare(42L, null).contentVersion)
    }

    @Test
    fun missingSizeUsesSharedFingerprint() {
        assertSame(fingerprint, prepare(null, 100L).contentVersion)
    }

    @Test
    fun sizeMismatchUsesSharedFingerprintWithDiagnostic() {
        val result = prepare(43L, 100L)
        assertSame(fingerprint, result.contentVersion)
        assertEquals("METADATA_SIZE_MISMATCH_FALLBACK_FINGERPRINT", result.diagnosticReason)
    }

    @Test
    fun zeroByteMatchingMetadataIsValid() {
        val empty = PreviewContentVersion.fingerprint(0L, "ab".repeat(32))
        assertEquals(
            PreviewContentVersion.metadata(0L, 100L),
            BookPreviewCacheVersionPolicy.prepare(cycle, empty, 0L, 100L).contentVersion
        )
    }

    @Test
    fun zeroByteFingerprintFallbackIsValid() {
        val empty = PreviewContentVersion.fingerprint(0L, "ab".repeat(32))
        assertSame(empty, BookPreviewCacheVersionPolicy.prepare(cycle, empty, 0L, null).contentVersion)
    }

    @Test
    fun cachedUnavailableIsNotUpgradedByReadyCurrentFingerprint() {
        val currentLoaderResult = CurrentPreviewContentVersionResult(fingerprint)
        assertEquals(true, currentLoaderResult.isReady)
        val old = state(PreviewContentVersion.Unavailable)
        val saved = old.copy(lastParagraphIndex = 9,
            contentVersion = BookPreviewCacheVersionPolicy.preserveCached(old.contentVersion))
        assertEquals(PreviewContentVersion.Unavailable, saved.contentVersion)
        assertEquals(9, saved.lastParagraphIndex)
    }

    @Test
    fun cachedMetadataPositionSavePreservesVersion() {
        val old = state(PreviewContentVersion.metadata(42L, 100L))
        val saved = old.copy(lastParagraphIndex = 9,
            contentVersion = BookPreviewCacheVersionPolicy.preserveCached(old.contentVersion))
        assertSame(old.contentVersion, saved.contentVersion)
    }

    @Test
    fun cachedFingerprintPositionSavePreservesVersion() {
        val old = state(fingerprint)
        val saved = old.copy(lastParagraphIndex = 9,
            contentVersion = BookPreviewCacheVersionPolicy.preserveCached(old.contentVersion))
        assertSame(fingerprint, saved.contentVersion)
    }

    @Test
    fun fullContentSaveUsesSharedFingerprint() {
        val saved = fullSave(prepare(42L, null), cycle)
        assertSame(fingerprint, saved?.contentVersion)
    }

    @Test
    fun fullContentSaveUsesCompatibleMetadata() {
        val saved = fullSave(prepare(42L, 100L), cycle)
        assertEquals(PreviewContentVersion.metadata(42L, 100L), saved?.contentVersion)
    }

    @Test
    fun oldCallbackCannotWriteAfterNextGeneration() {
        assertNull(fullSave(prepare(42L, null), ReaderContentCycle(2L, "content://books/b")))
    }

    @Test
    fun sameBookReloadRejectsOldCallback() {
        val current = ReaderContentCycle(2L, cycle.bookUri)
        val currentCache = state(PreviewContentVersion.fingerprint(99L, "cd".repeat(32)))
        val retained = fullSave(prepare(42L, null), current) ?: currentCache
        assertSame(currentCache, retained)
    }

    @Test
    fun firstACallbackCannotOverwriteThirdCycleA() {
        assertNull(fullSave(prepare(42L, null), ReaderContentCycle(3L, cycle.bookUri)))
    }

    @Test
    fun selectedFullFingerprintRoundTripsThroughCanonicalCodec() {
        val saved = requireNotNull(fullSave(prepare(42L, null), cycle))
        val encoded = BookPreviewCacheMetadataCodec.encode("test_",
            BookPreviewCacheMetadata(saved.bookUri, saved.contentVersion, "window"))
        val decoded = BookPreviewCacheMetadataCodec.decode("test_", saved.bookUri,
            encoded.filterValues { it != null })
        assertEquals(fingerprint, decoded?.contentVersion)
        assertNull(encoded["test_content_modified_at"])
        assertEquals("fingerprint", encoded["test_content_version_kind"])
    }

    @Test
    fun releasedCycleAndMissingProvenanceCannotWrite() {
        assertNull(fullSave(prepare(42L, null), null))
        assertNull(BookPreviewCacheVersionPolicy.forFullSave(null, cycle))
    }

    @Test
    fun invalidMetadataValuesKeepSharedFingerprint() {
        assertSame(fingerprint, prepare(-1L, 100L).contentVersion)
        assertSame(fingerprint, prepare(42L, 0L).contentVersion)
    }

    private fun prepare(size: Long?, modifiedAt: Long?) =
        BookPreviewCacheVersionPolicy.prepare(cycle, fingerprint, size, modifiedAt)

    private fun fullSave(provenance: PreparedContentProvenance, current: ReaderContentCycle?) =
        BookPreviewCacheVersionPolicy.forFullSave(provenance, current)?.let(::state)

    private fun state(version: PreviewContentVersion) = BookReadStateCache(
        bookUri = cycle.bookUri, contentVersion = version, bookTitle = "Test",
        lastParagraphIndex = 0, lastSentenceIndexInParagraph = 0, lastChapterSentenceIndex = 0,
        lastReadingTargetName = "SENTENCE", lastChapterTitle = "Chapter",
        lastVisibleFirstItemIndex = 0, lastVisibleFirstChapterSentenceIndex = 0,
        lastVisibleFirstItemScrollOffset = 0, cachedStartChapterSentenceIndex = 0,
        cachedElapsedSeconds = 0, cachedRemainingSeconds = 1, cachedProgressFraction = 0f,
        cachedChapterEstimatedDurationSeconds = 1, cachedSentences = emptyList(),
        cachedVisibleText = emptyList(), updatedAt = 1L
    )
}

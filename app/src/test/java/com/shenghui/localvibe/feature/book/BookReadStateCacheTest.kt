package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertEquals
import org.junit.Test

class BookReadStateCacheTest {
    private val prefix = "book_42_"
    private val bookUri = "content://books/current"
    private val fingerprint = "ab".repeat(32)

    @Test
    fun metadataVersionRoundTripsWithoutChangingReaderState() {
        val state = fixture(PreviewContentVersion.metadata(4096L, 123456L))

        assertEquals(state, roundTripVersion(state))
    }

    @Test
    fun fingerprintVersionRoundTripsWithoutDroppingSha256() {
        val state = fixture(PreviewContentVersion.fingerprint(4096L, fingerprint))

        assertEquals(state, roundTripVersion(state))
    }

    @Test
    fun unavailableVersionRoundTripsWithoutChangingReaderState() {
        val state = fixture(PreviewContentVersion.Unavailable)

        assertEquals(state, roundTripVersion(state))
    }

    private fun roundTripVersion(state: BookReadStateCache): BookReadStateCache {
        val encoded = BookPreviewCacheMetadataCodec.encode(
            prefix = prefix,
            metadata = BookPreviewCacheMetadata(
                bookUri = state.bookUri,
                contentVersion = state.contentVersion,
                encodedSentenceWindow = "window"
            )
        )
        val stored = encoded.filterValues { it != null }
        val decoded = requireNotNull(
            BookPreviewCacheMetadataCodec.decode(prefix, bookUri, stored)
        )
        return state.copy(
            bookUri = decoded.bookUri,
            contentVersion = decoded.contentVersion
        )
    }

    private fun fixture(version: PreviewContentVersion): BookReadStateCache {
        return BookReadStateCache(
            bookUri = bookUri,
            contentVersion = version,
            bookTitle = "Current book",
            lastParagraphIndex = 4,
            lastSentenceIndexInParagraph = 2,
            lastChapterSentenceIndex = 18,
            lastReadingTargetName = "SENTENCE",
            lastChapterTitle = "Chapter 1",
            lastVisibleFirstItemIndex = 3,
            lastVisibleFirstChapterSentenceIndex = 16,
            lastVisibleFirstItemScrollOffset = 24,
            cachedStartChapterSentenceIndex = 15,
            cachedElapsedSeconds = 8,
            cachedRemainingSeconds = 12,
            cachedProgressFraction = 0.4f,
            cachedChapterEstimatedDurationSeconds = 20,
            cachedSentences = listOf(
                BookCachedSentence(
                    text = "Hello, reader.",
                    paragraphIndex = 4,
                    sentenceIndexInParagraph = 2,
                    chapterSentenceIndex = 18,
                    isCurrent = true
                ),
                BookCachedSentence(
                    text = "下一句。",
                    paragraphIndex = 4,
                    sentenceIndexInParagraph = 3,
                    chapterSentenceIndex = 19,
                    isCurrent = false
                )
            ),
            cachedVisibleText = listOf("Hello, reader.", "下一句。"),
            updatedAt = 987654321L
        )
    }
}

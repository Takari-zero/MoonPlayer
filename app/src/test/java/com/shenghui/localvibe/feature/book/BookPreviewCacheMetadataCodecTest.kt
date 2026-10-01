package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookPreviewCacheMetadataCodecTest {
    private val prefix = "book_42_"
    private val bookUri = "content://books/current"

    @Test
    fun newCacheRoundTripsUriSizeAndModifiedAt() {
        val metadata = metadata(contentSize = 4096L, contentModifiedAt = 123456L)

        assertEquals(metadata, roundTrip(metadata))
    }

    @Test
    fun legacyCacheWithoutProvenanceStillLoadsForDisplay() {
        val stored = mutableMapOf<String, Any>(
            prefix + "uri" to bookUri,
            prefix + "sentence_window" to "legacy-window"
        )

        val decoded = BookPreviewCacheMetadataCodec.decode(prefix, bookUri, stored)

        assertEquals(bookUri, decoded?.bookUri)
        assertEquals("legacy-window", decoded?.encodedSentenceWindow)
        assertNull(decoded?.contentSize)
        assertNull(decoded?.contentModifiedAt)
    }

    @Test
    fun legacyCacheCannotEstablishLocalPlaybackProvenance() {
        val result = PreviewPlaybackTargetResolver.resolve(
            candidate = CachedPreviewPlaybackCandidate(
                paragraphIndex = 1,
                sentenceIndexInParagraph = 0,
                chapterSentenceIndex = 1,
                text = "sentence",
                stableTextHash = "sentence".hashCode(),
                originReaderGeneration = 3L
            ),
            cachedIdentity = PreviewContentIdentity(bookUri, null, null),
            currentIdentity = PreviewContentIdentity(bookUri, 4096L, 123456L),
            currentReaderGeneration = 3L,
            expectedText = "sentence",
            expectedStableTextHash = "sentence".hashCode(),
            playRequested = true
        )

        assertEquals(
            PreviewPlaybackResolution.Rejected(
                PreviewPlaybackRejectionReason.CONTENT_VERSION_UNAVAILABLE
            ),
            result
        )
    }

    @Test
    fun zeroContentSizeIsPersistedAsARealValue() {
        val encoded = BookPreviewCacheMetadataCodec.encode(
            prefix,
            metadata(contentSize = 0L, contentModifiedAt = 123456L)
        )

        assertTrue(encoded.containsKey(prefix + "content_size"))
        assertEquals(0L, roundTrip(metadata(contentSize = 0L, contentModifiedAt = 123456L))?.contentSize)
    }

    @Test
    fun unknownModifiedAtRemainsNullWithoutMagicValue() {
        val encoded = BookPreviewCacheMetadataCodec.encode(
            prefix,
            metadata(contentSize = 4096L, contentModifiedAt = null)
        )
        val stored = applyUpdate(encoded)

        assertFalse(stored.containsKey(prefix + "content_modified_at"))
        assertNull(BookPreviewCacheMetadataCodec.decode(prefix, bookUri, stored)?.contentModifiedAt)
    }

    @Test
    fun explicitlyStoredZeroModifiedAtRemainsPresent() {
        val decoded = roundTrip(metadata(contentSize = 4096L, contentModifiedAt = 0L))

        assertEquals(0L, decoded?.contentModifiedAt)
    }

    @Test
    fun savingNullProvenanceRemovesPreviouslyStoredValues() {
        val knownValues = BookPreviewCacheMetadataCodec.encode(
            prefix,
            metadata(contentSize = 4096L, contentModifiedAt = 123456L)
        )
        val stored = applyUpdate(knownValues).toMutableMap()

        applyUpdate(
            update = BookPreviewCacheMetadataCodec.encode(
                prefix,
                metadata(contentSize = null, contentModifiedAt = null)
            ),
            stored = stored
        )

        assertFalse(stored.containsKey(prefix + "content_size"))
        assertFalse(stored.containsKey(prefix + "content_modified_at"))
        val decoded = BookPreviewCacheMetadataCodec.decode(prefix, bookUri, stored)
        assertNull(decoded?.contentSize)
        assertNull(decoded?.contentModifiedAt)
    }

    @Test
    fun differentBookUriCannotReuseCacheEntry() {
        val stored = applyUpdate(
            BookPreviewCacheMetadataCodec.encode(
                prefix,
                metadata(contentSize = 4096L, contentModifiedAt = 123456L)
            )
        )

        assertNull(
            BookPreviewCacheMetadataCodec.decode(
                prefix,
                "content://books/other",
                stored
            )
        )
    }

    @Test
    fun sentenceWindowRoundTripsWithoutChangingItsEncoding() {
        val encodedWindow = "12\u001F3\u001F0\u001F1\u001Fhello%20world\u001D13\u001F4\u001F0\u001F0\u001Fnext"

        val decoded = roundTrip(
            metadata(
                contentSize = 4096L,
                contentModifiedAt = 123456L,
                encodedSentenceWindow = encodedWindow
            )
        )

        assertEquals(encodedWindow, decoded?.encodedSentenceWindow)
    }

    @Test
    fun readerGenerationIsNeverPersisted() {
        val encoded = BookPreviewCacheMetadataCodec.encode(
            prefix,
            metadata(contentSize = 4096L, contentModifiedAt = 123456L)
        )

        assertTrue(encoded.keys.none { it.contains("generation", ignoreCase = true) })
    }

    private fun metadata(
        contentSize: Long?,
        contentModifiedAt: Long?,
        encodedSentenceWindow: String = "window"
    ): BookPreviewCacheMetadata {
        return BookPreviewCacheMetadata(
            bookUri = bookUri,
            contentSize = contentSize,
            contentModifiedAt = contentModifiedAt,
            encodedSentenceWindow = encodedSentenceWindow
        )
    }

    private fun roundTrip(metadata: BookPreviewCacheMetadata): BookPreviewCacheMetadata? {
        val stored = applyUpdate(BookPreviewCacheMetadataCodec.encode(prefix, metadata))
        return BookPreviewCacheMetadataCodec.decode(prefix, bookUri, stored)
    }

    private fun applyUpdate(
        update: Map<String, Any?>,
        stored: MutableMap<String, Any> = mutableMapOf()
    ): Map<String, Any> {
        update.forEach { (key, value) ->
            if (value == null) {
                stored.remove(key)
            } else {
                stored[key] = value
            }
        }
        return stored
    }
}

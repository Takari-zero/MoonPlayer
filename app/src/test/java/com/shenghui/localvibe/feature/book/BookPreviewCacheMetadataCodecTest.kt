package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookPreviewCacheMetadataCodecTest {
    private val prefix = "book_42_"
    private val bookUri = "content://books/current"
    private val fingerprint = "ab".repeat(32)

    @Test
    fun metadataRoundTrips() {
        val metadata = metadata(PreviewContentVersion.metadata(4096L, 123456L))
        assertEquals(metadata, roundTrip(metadata))
    }

    @Test
    fun fingerprintRoundTrips() {
        val metadata = metadata(PreviewContentVersion.fingerprint(4096L, fingerprint))
        assertEquals(metadata, roundTrip(metadata))
    }

    @Test
    fun legacyMetadataWithoutKindDecodesAsMetadata() {
        val stored = mutableMapOf<String, Any>(
            prefix + "uri" to bookUri,
            prefix + "content_size" to 4096L,
            prefix + "content_modified_at" to 123456L,
            prefix + "sentence_window" to "legacy-window"
        )

        val decoded = BookPreviewCacheMetadataCodec.decode(prefix, bookUri, stored)

        assertEquals(PreviewContentVersion.metadata(4096L, 123456L), decoded?.contentVersion)
        assertEquals("legacy-window", decoded?.encodedSentenceWindow)
    }

    @Test
    fun legacySizeOnlyDecodesAsUnavailable() {
        val stored = mutableMapOf<String, Any>(
            prefix + "uri" to bookUri,
            prefix + "content_size" to 4096L
        )

        assertEquals(
            PreviewContentVersion.Unavailable,
            BookPreviewCacheMetadataCodec.decode(prefix, bookUri, stored)?.contentVersion
        )
    }

    @Test
    fun fingerprintKindWithoutShaDecodesAsUnavailable() {
        assertDecodedUnavailable(storedVersion("fingerprint", size = 4096L))
    }

    @Test
    fun metadataKindWithoutModifiedAtDecodesAsUnavailable() {
        assertDecodedUnavailable(storedVersion("metadata", size = 4096L))
    }

    @Test
    fun unknownKindDecodesAsUnavailable() {
        assertDecodedUnavailable(storedVersion("future", size = 4096L))
    }

    @Test
    fun metadataOverwriteRemovesOldFingerprint() {
        val stored = applyUpdate(
            BookPreviewCacheMetadataCodec.encode(
                prefix,
                metadata(PreviewContentVersion.fingerprint(4096L, fingerprint))
            )
        ).toMutableMap()

        applyUpdate(
            BookPreviewCacheMetadataCodec.encode(
                prefix,
                metadata(PreviewContentVersion.metadata(4096L, 123456L))
            ),
            stored
        )

        assertFalse(stored.containsKey(prefix + "content_sha256"))
        assertEquals(123456L, stored[prefix + "content_modified_at"])
    }

    @Test
    fun fingerprintOverwriteRemovesOldModifiedAt() {
        val stored = applyUpdate(
            BookPreviewCacheMetadataCodec.encode(
                prefix,
                metadata(PreviewContentVersion.metadata(4096L, 123456L))
            )
        ).toMutableMap()

        applyUpdate(
            BookPreviewCacheMetadataCodec.encode(
                prefix,
                metadata(PreviewContentVersion.fingerprint(4096L, fingerprint))
            ),
            stored
        )

        assertFalse(stored.containsKey(prefix + "content_modified_at"))
        assertEquals(fingerprint, stored[prefix + "content_sha256"])
    }

    @Test
    fun unavailableRemovesAllVersionKeys() {
        val stored = applyUpdate(
            BookPreviewCacheMetadataCodec.encode(
                prefix,
                metadata(PreviewContentVersion.fingerprint(4096L, fingerprint))
            )
        ).toMutableMap()

        applyUpdate(
            BookPreviewCacheMetadataCodec.encode(
                prefix,
                metadata(PreviewContentVersion.Unavailable)
            ),
            stored
        )

        assertFalse(stored.containsKey(prefix + "content_version_kind"))
        assertFalse(stored.containsKey(prefix + "content_size"))
        assertFalse(stored.containsKey(prefix + "content_modified_at"))
        assertFalse(stored.containsKey(prefix + "content_sha256"))
    }

    @Test
    fun zeroByteFingerprintIsPreserved() {
        val decoded = roundTrip(metadata(PreviewContentVersion.fingerprint(0L, fingerprint)))

        assertEquals(0L, decoded?.contentVersion?.size)
        assertTrue(decoded?.contentVersion is PreviewContentVersion.Fingerprint)
    }

    @Test
    fun malformedPersistedFingerprintDecodesAsUnavailable() {
        assertDecodedUnavailable(
            storedVersion("fingerprint", size = 4096L, sha256 = "not-a-sha256")
        )
    }

    @Test
    fun uppercasePersistedFingerprintIsCanonicalized() {
        val stored = storedVersion(
            kind = "fingerprint",
            size = 4096L,
            sha256 = fingerprint.uppercase()
        )

        val version = BookPreviewCacheMetadataCodec.decode(prefix, bookUri, stored)?.contentVersion
            as PreviewContentVersion.Fingerprint

        assertEquals(fingerprint, version.sha256Hex)
    }

    @Test
    fun legacyConstructorMapsInvalidMetadataToUnavailable() {
        val metadata = BookPreviewCacheMetadata(
            bookUri = bookUri,
            contentSize = 4096L,
            contentModifiedAt = 0L,
            encodedSentenceWindow = "window"
        )

        assertEquals(PreviewContentVersion.Unavailable, metadata.contentVersion)
        assertNull(metadata.contentSize)
        assertNull(metadata.contentModifiedAt)
    }

    @Test
    fun sentenceWindowRoundTripsWithoutChangingEncoding() {
        val window = "12\u001F3\u001F0\u001F1\u001Fhello%20world"

        assertEquals(
            window,
            roundTrip(
                metadata(
                    PreviewContentVersion.metadata(4096L, 123456L),
                    encodedSentenceWindow = window
                )
            )?.encodedSentenceWindow
        )
    }

    @Test
    fun differentBookUriCannotReuseCacheEntry() {
        val stored = applyUpdate(
            BookPreviewCacheMetadataCodec.encode(
                prefix,
                metadata(PreviewContentVersion.metadata(4096L, 123456L))
            )
        )

        assertNull(BookPreviewCacheMetadataCodec.decode(prefix, "content://books/other", stored))
    }

    @Test
    fun readerGenerationIsNeverPersisted() {
        val encoded = BookPreviewCacheMetadataCodec.encode(
            prefix,
            metadata(PreviewContentVersion.metadata(4096L, 123456L))
        )

        assertTrue(encoded.keys.none { it.contains("generation", ignoreCase = true) })
    }

    private fun assertDecodedUnavailable(stored: Map<String, Any>) {
        assertEquals(
            PreviewContentVersion.Unavailable,
            BookPreviewCacheMetadataCodec.decode(prefix, bookUri, stored)?.contentVersion
        )
    }

    private fun metadata(
        version: PreviewContentVersion,
        encodedSentenceWindow: String = "window"
    ): BookPreviewCacheMetadata {
        return BookPreviewCacheMetadata(
            bookUri = bookUri,
            contentVersion = version,
            encodedSentenceWindow = encodedSentenceWindow
        )
    }

    private fun storedVersion(
        kind: String,
        size: Long,
        sha256: String? = null
    ): MutableMap<String, Any> {
        return mutableMapOf<String, Any>(
            prefix + "uri" to bookUri,
            prefix + "content_version_kind" to kind,
            prefix + "content_size" to size
        ).also { stored ->
            sha256?.let { stored[prefix + "content_sha256"] = it }
        }
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

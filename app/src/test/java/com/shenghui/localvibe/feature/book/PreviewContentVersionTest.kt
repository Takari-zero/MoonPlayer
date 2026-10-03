package com.shenghui.localvibe.feature.book

import com.shenghui.localvibe.core.book.BookContentFingerprint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewContentVersionTest {
    @Test
    fun equalMetadataValuesAreEqual() {
        assertEquals(
            PreviewContentVersion.metadata(4096L, 123456L),
            PreviewContentVersion.metadata(4096L, 123456L)
        )
    }

    @Test
    fun equalFingerprintValuesAreEqual() {
        assertEquals(
            PreviewContentVersion.fingerprint(4096L, "a".repeat(64)),
            PreviewContentVersion.fingerprint(4096L, "a".repeat(64))
        )
    }

    @Test
    fun zeroByteFingerprintIsValid() {
        val version = PreviewContentVersion.fingerprint(0L, "a".repeat(64))

        assertTrue(version is PreviewContentVersion.Fingerprint)
        assertEquals(0L, version.size)
    }

    @Test
    fun malformedFingerprintLengthIsUnavailable() {
        assertEquals(
            PreviewContentVersion.Unavailable,
            PreviewContentVersion.fingerprint(4096L, "a".repeat(63))
        )
    }

    @Test
    fun nonHexFingerprintIsUnavailable() {
        assertEquals(
            PreviewContentVersion.Unavailable,
            PreviewContentVersion.fingerprint(4096L, "z".repeat(64))
        )
    }

    @Test
    fun uppercaseFingerprintIsCanonicalizedToLowercase() {
        val version = PreviewContentVersion.fingerprint(4096L, "AB".repeat(32))
            as PreviewContentVersion.Fingerprint

        assertEquals("ab".repeat(32), version.sha256Hex)
    }

    @Test
    fun nonPositiveModifiedAtIsUnavailable() {
        assertEquals(
            PreviewContentVersion.Unavailable,
            PreviewContentVersion.metadata(4096L, 0L)
        )
    }

    @Test
    fun coreFingerprintMapsWithoutChangingSizeOrHash() {
        val fingerprint = BookContentFingerprint(4096L, "a".repeat(64))

        assertEquals(
            PreviewContentVersion.fingerprint(4096L, "a".repeat(64)),
            fingerprint.toPreviewContentVersion()
        )
    }
}

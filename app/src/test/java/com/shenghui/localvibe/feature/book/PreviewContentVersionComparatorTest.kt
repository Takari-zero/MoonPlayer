package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PreviewContentVersionComparatorTest {
    private val fingerprint = PreviewContentVersion.fingerprint(4L, "a".repeat(64))
    private val metadata = PreviewContentVersion.metadata(4L, 123L)

    @Test fun identicalFingerprintsMatch() {
        assertNull(PreviewContentVersionComparator.compare(fingerprint, fingerprint))
    }

    @Test fun differentDigestsReject() {
        assertEquals(PreviewPlaybackRejectionReason.CONTENT_FINGERPRINT_MISMATCH,
            PreviewContentVersionComparator.compare(fingerprint, PreviewContentVersion.fingerprint(4L, "b".repeat(64))))
    }

    @Test fun differentSizesRejectBeforeDigest() {
        assertEquals(PreviewPlaybackRejectionReason.CONTENT_SIZE_MISMATCH,
            PreviewContentVersionComparator.compare(fingerprint, PreviewContentVersion.fingerprint(5L, "b".repeat(64))))
    }

    @Test fun identicalMetadataMatches() {
        assertNull(PreviewContentVersionComparator.compare(metadata, metadata))
    }

    @Test fun differentModifiedAtRejects() {
        assertEquals(PreviewPlaybackRejectionReason.CONTENT_MODIFIED_AT_MISMATCH,
            PreviewContentVersionComparator.compare(metadata, PreviewContentVersion.metadata(4L, 124L)))
    }

    @Test fun differentKindsReject() {
        assertEquals(PreviewPlaybackRejectionReason.CONTENT_VERSION_KIND_MISMATCH,
            PreviewContentVersionComparator.compare(metadata, fingerprint))
        assertEquals(PreviewPlaybackRejectionReason.CONTENT_VERSION_KIND_MISMATCH,
            PreviewContentVersionComparator.compare(fingerprint, metadata))
    }

    @Test fun unavailableCacheRejects() {
        assertEquals(PreviewPlaybackRejectionReason.CONTENT_VERSION_UNAVAILABLE,
            PreviewContentVersionComparator.compare(PreviewContentVersion.Unavailable, fingerprint))
    }

    @Test fun unavailableCurrentRejects() {
        assertEquals(PreviewPlaybackRejectionReason.CONTENT_VERSION_UNAVAILABLE,
            PreviewContentVersionComparator.compare(fingerprint, PreviewContentVersion.Unavailable))
    }

    @Test fun twoUnavailableVersionsDoNotMatch() {
        assertEquals(PreviewPlaybackRejectionReason.CONTENT_VERSION_UNAVAILABLE,
            PreviewContentVersionComparator.compare(PreviewContentVersion.Unavailable, PreviewContentVersion.Unavailable))
    }

    @Test fun sizePrecedesKind() {
        assertEquals(PreviewPlaybackRejectionReason.CONTENT_SIZE_MISMATCH,
            PreviewContentVersionComparator.compare(metadata, PreviewContentVersion.fingerprint(5L, "a".repeat(64))))
    }
}

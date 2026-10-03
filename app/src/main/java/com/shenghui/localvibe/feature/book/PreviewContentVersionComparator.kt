package com.shenghui.localvibe.feature.book

internal object PreviewContentVersionComparator {
    // A null rejection means that both available versions match.
    fun compare(
        cached: PreviewContentVersion,
        current: PreviewContentVersion
    ): PreviewPlaybackRejectionReason? {
        if (cached == PreviewContentVersion.Unavailable || current == PreviewContentVersion.Unavailable) {
            return PreviewPlaybackRejectionReason.CONTENT_VERSION_UNAVAILABLE
        }
        if (cached.size != current.size) {
            return PreviewPlaybackRejectionReason.CONTENT_SIZE_MISMATCH
        }
        if (cached::class != current::class) {
            return PreviewPlaybackRejectionReason.CONTENT_VERSION_KIND_MISMATCH
        }
        return when {
            cached is PreviewContentVersion.Metadata && current is PreviewContentVersion.Metadata &&
                cached.modifiedAt != current.modifiedAt -> PreviewPlaybackRejectionReason.CONTENT_MODIFIED_AT_MISMATCH
            cached is PreviewContentVersion.Fingerprint && current is PreviewContentVersion.Fingerprint &&
                cached.sha256Hex != current.sha256Hex -> PreviewPlaybackRejectionReason.CONTENT_FINGERPRINT_MISMATCH
            else -> null
        }
    }
}

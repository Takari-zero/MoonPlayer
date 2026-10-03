package com.shenghui.localvibe.feature.book

internal data class PreparedContentProvenance(
    val cycle: ReaderContentCycle,
    val contentVersion: PreviewContentVersion,
    val diagnosticReason: String? = null
)

internal object BookPreviewCacheVersionPolicy {
    fun prepare(
        cycle: ReaderContentCycle,
        sharedReadFingerprint: PreviewContentVersion,
        providerSize: Long?,
        providerModifiedAt: Long?
    ): PreparedContentProvenance {
        require(sharedReadFingerprint is PreviewContentVersion.Fingerprint)
        val metadata = PreviewContentVersion.metadata(providerSize, providerModifiedAt)
        val sizeMismatch = providerSize != null && providerSize >= 0L &&
            providerSize != sharedReadFingerprint.size
        return PreparedContentProvenance(
            cycle = cycle,
            contentVersion = if (metadata is PreviewContentVersion.Metadata && !sizeMismatch) {
                metadata
            } else {
                sharedReadFingerprint
            },
            diagnosticReason = if (sizeMismatch) {
                "METADATA_SIZE_MISMATCH_FALLBACK_FINGERPRINT"
            } else {
                null
            }
        )
    }

    fun preserveCached(version: PreviewContentVersion): PreviewContentVersion = version

    fun forFullSave(
        provenance: PreparedContentProvenance?,
        currentCycle: ReaderContentCycle?
    ): PreviewContentVersion? = provenance?.takeIf { it.cycle == currentCycle }?.contentVersion
}

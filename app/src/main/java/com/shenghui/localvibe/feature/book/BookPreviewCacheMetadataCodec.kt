package com.shenghui.localvibe.feature.book

internal data class BookPreviewCacheMetadata(
    val bookUri: String,
    val contentVersion: PreviewContentVersion,
    val encodedSentenceWindow: String
) {
    val contentSize: Long?
        get() = contentVersion.size

    val contentModifiedAt: Long?
        get() = (contentVersion as? PreviewContentVersion.Metadata)?.modifiedAt

    companion object {
        fun fromLegacyMetadata(
            bookUri: String,
            contentSize: Long?,
            contentModifiedAt: Long?,
            encodedSentenceWindow: String
        ): BookPreviewCacheMetadata = BookPreviewCacheMetadata(
            bookUri = bookUri,
            contentVersion = PreviewContentVersion.metadata(contentSize, contentModifiedAt),
            encodedSentenceWindow = encodedSentenceWindow
        )
    }
}

internal object BookPreviewCacheMetadataCodec {
    private const val URI_SUFFIX = "uri"
    private const val CONTENT_VERSION_KIND_SUFFIX = "content_version_kind"
    private const val CONTENT_SIZE_SUFFIX = "content_size"
    private const val CONTENT_MODIFIED_AT_SUFFIX = "content_modified_at"
    private const val CONTENT_SHA256_SUFFIX = "content_sha256"
    private const val SENTENCE_WINDOW_SUFFIX = "sentence_window"
    private const val VERSION_KIND_METADATA = "metadata"
    private const val VERSION_KIND_FINGERPRINT = "fingerprint"

    fun encode(prefix: String, metadata: BookPreviewCacheMetadata): Map<String, Any?> {
        val version = metadata.contentVersion
        return linkedMapOf(
            prefix + URI_SUFFIX to metadata.bookUri,
            prefix + CONTENT_VERSION_KIND_SUFFIX to when (version) {
                is PreviewContentVersion.Metadata -> VERSION_KIND_METADATA
                is PreviewContentVersion.Fingerprint -> VERSION_KIND_FINGERPRINT
                PreviewContentVersion.Unavailable -> null
            },
            prefix + CONTENT_SIZE_SUFFIX to version.size,
            prefix + CONTENT_MODIFIED_AT_SUFFIX to
                (version as? PreviewContentVersion.Metadata)?.modifiedAt,
            prefix + CONTENT_SHA256_SUFFIX to
                (version as? PreviewContentVersion.Fingerprint)?.sha256Hex,
            prefix + SENTENCE_WINDOW_SUFFIX to metadata.encodedSentenceWindow
        )
    }

    fun decode(
        prefix: String,
        expectedBookUri: String,
        storedValues: Map<String, *>
    ): BookPreviewCacheMetadata? {
        val storedUri = storedValues[prefix + URI_SUFFIX] as? String ?: return null
        if (storedUri != expectedBookUri) return null

        val size = (storedValues[prefix + CONTENT_SIZE_SUFFIX] as? Number)?.toLong()
        val modifiedAt = (storedValues[prefix + CONTENT_MODIFIED_AT_SUFFIX] as? Number)?.toLong()
        val sha256 = storedValues[prefix + CONTENT_SHA256_SUFFIX] as? String
        val version = when (storedValues[prefix + CONTENT_VERSION_KIND_SUFFIX] as? String) {
            null -> PreviewContentVersion.metadata(size, modifiedAt)
            VERSION_KIND_METADATA -> PreviewContentVersion.metadata(size, modifiedAt)
            VERSION_KIND_FINGERPRINT -> PreviewContentVersion.fingerprint(size, sha256)
            else -> PreviewContentVersion.Unavailable
        }

        return BookPreviewCacheMetadata(
            bookUri = storedUri,
            contentVersion = version,
            encodedSentenceWindow = storedValues[prefix + SENTENCE_WINDOW_SUFFIX] as? String ?: ""
        )
    }
}

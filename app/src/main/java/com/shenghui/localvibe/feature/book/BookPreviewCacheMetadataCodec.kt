package com.shenghui.localvibe.feature.book

internal data class BookPreviewCacheMetadata(
    val bookUri: String,
    val contentSize: Long?,
    val contentModifiedAt: Long?,
    val encodedSentenceWindow: String
)

internal object BookPreviewCacheMetadataCodec {
    private const val URI_SUFFIX = "uri"
    private const val CONTENT_SIZE_SUFFIX = "content_size"
    private const val CONTENT_MODIFIED_AT_SUFFIX = "content_modified_at"
    private const val SENTENCE_WINDOW_SUFFIX = "sentence_window"

    fun encode(prefix: String, metadata: BookPreviewCacheMetadata): Map<String, Any?> {
        return linkedMapOf(
            prefix + URI_SUFFIX to metadata.bookUri,
            prefix + CONTENT_SIZE_SUFFIX to metadata.contentSize,
            prefix + CONTENT_MODIFIED_AT_SUFFIX to metadata.contentModifiedAt,
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

        return BookPreviewCacheMetadata(
            bookUri = storedUri,
            contentSize = (storedValues[prefix + CONTENT_SIZE_SUFFIX] as? Number)?.toLong(),
            contentModifiedAt = (storedValues[prefix + CONTENT_MODIFIED_AT_SUFFIX] as? Number)?.toLong(),
            encodedSentenceWindow = storedValues[prefix + SENTENCE_WINDOW_SUFFIX] as? String ?: ""
        )
    }
}

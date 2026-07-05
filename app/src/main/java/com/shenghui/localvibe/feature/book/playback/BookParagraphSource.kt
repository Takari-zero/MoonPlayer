package com.shenghui.localvibe.feature.book.playback

data class BookParagraphLoadRequest(
    val key: BookDocumentCacheKey,
) {
    init {
        require(key.bookId.isNotBlank()) { "bookId must not be blank" }
    }
}

sealed interface BookParagraphLoadResult {
    val key: BookDocumentCacheKey
    val isSuccess: Boolean
        get() = false

    fun paragraphsOrNull(): List<String>? = null
    fun errorOrNull(): Throwable? = null

    data class Success(
        override val key: BookDocumentCacheKey,
        val paragraphs: List<String>,
    ) : BookParagraphLoadResult {
        override val isSuccess: Boolean = true

        override fun paragraphsOrNull(): List<String> = paragraphs
    }

    data class Empty(
        override val key: BookDocumentCacheKey,
        val message: String,
    ) : BookParagraphLoadResult {
        private val error = IllegalArgumentException(message)

        override fun errorOrNull(): Throwable = error
    }

    data class Failed(
        override val key: BookDocumentCacheKey,
        val message: String,
        val cause: Throwable? = null,
    ) : BookParagraphLoadResult {
        private val error = cause ?: IllegalStateException(message)

        override fun errorOrNull(): Throwable = error
    }

    companion object {
        fun success(
            key: BookDocumentCacheKey,
            paragraphs: List<String>,
        ): BookParagraphLoadResult {
            return if (paragraphs.any { it.isNotBlank() }) {
                Success(
                    key = key,
                    paragraphs = paragraphs,
                )
            } else {
                Empty(
                    key = key,
                    message = "paragraphs must not be empty",
                )
            }
        }

        fun empty(
            key: BookDocumentCacheKey,
            message: String = "paragraphs must not be empty",
        ): BookParagraphLoadResult {
            return Empty(
                key = key,
                message = message,
            )
        }

        fun failed(
            key: BookDocumentCacheKey,
            message: String,
            cause: Throwable? = null,
        ): BookParagraphLoadResult {
            return Failed(
                key = key,
                message = message,
                cause = cause,
            )
        }
    }
}

fun interface BookParagraphSource {
    fun load(request: BookParagraphLoadRequest): BookParagraphLoadResult
}

fun BookParagraphSource.toDocumentParagraphSource(): BookDocumentParagraphSource {
    return BookDocumentParagraphSource { key ->
        when (val result = load(BookParagraphLoadRequest(key))) {
            is BookParagraphLoadResult.Success -> Result.success(result.paragraphs)
            is BookParagraphLoadResult.Empty -> Result.failure(
                result.errorOrNull() ?: IllegalArgumentException(result.message)
            )
            is BookParagraphLoadResult.Failed -> Result.failure(
                result.errorOrNull() ?: IllegalStateException(result.message)
            )
        }
    }
}

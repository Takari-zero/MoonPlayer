package com.shenghui.localvibe.feature.book.playback

fun interface TxtBookParagraphLoader {
    fun readParagraphs(bookId: String): Result<List<String>>
}

class TxtBookParagraphSource(
    private val loader: TxtBookParagraphLoader,
) : BookParagraphSource {
    override fun load(request: BookParagraphLoadRequest): BookParagraphLoadResult {
        return loader.readParagraphs(request.key.bookId).fold(
            onSuccess = { paragraphs ->
                BookParagraphLoadResult.success(
                    key = request.key,
                    paragraphs = paragraphs,
                )
            },
            onFailure = { error ->
                BookParagraphLoadResult.failed(
                    key = request.key,
                    message = error.message ?: "txt paragraphs unavailable",
                    cause = error,
                )
            },
        )
    }
}

package com.shenghui.localvibe.feature.book.playback

class BookReaderEntryReadyStateLoader(
    private val paragraphSource: BookReaderEntryParagraphSource,
    private val restoreSource: BookReaderEntryRestoreSource,
    private val factory: BookReaderEntryReadyStateFactoryAdapter = BookReaderEntryReadyStateFactoryAdapter { input ->
        BookReaderEntryReadyStateFactory.create(input)
    },
) {
    fun load(request: BookReaderEntryLoadRequest): Result<BookReaderEntryReadyState> {
        return paragraphSource.loadParagraphs(request.bookId).mapCatching { paragraphs ->
            val restoreTarget = restoreSource.loadRestoreTarget(request.bookId)
            factory.create(
                BookReaderEntryReadyStateFactory.Input(
                    bookId = request.bookId,
                    bookTitle = request.bookTitle,
                    paragraphs = paragraphs,
                    savedParagraphIndex = restoreTarget?.paragraphIndex ?: 0,
                    savedSentenceIndex = restoreTarget?.sentenceIndex ?: 0,
                    chapterIndex = request.chapterIndex,
                    chapterTitle = request.chapterTitle,
                    chapterStartIndex = request.chapterStartIndex,
                    chapterEndExclusive = request.chapterEndExclusive ?: paragraphs.size,
                    speechRate = request.speechRate,
                    cachedViewport = restoreTarget?.cachedViewport,
                )
            )
        }
    }
}

data class BookReaderEntryLoadRequest(
    val bookId: String,
    val bookTitle: String,
    val chapterIndex: Int = 0,
    val chapterTitle: String = "正文",
    val chapterStartIndex: Int = 0,
    val chapterEndExclusive: Int? = null,
    val speechRate: Float = 1f,
)

data class BookReaderEntryRestoreTarget(
    val paragraphIndex: Int,
    val sentenceIndex: Int,
    val cachedViewport: BookReaderEntryReadyStateFactory.CachedViewport? = null,
)

fun interface BookReaderEntryParagraphSource {
    fun loadParagraphs(bookId: String): Result<List<String>>
}

fun interface BookReaderEntryRestoreSource {
    fun loadRestoreTarget(bookId: String): BookReaderEntryRestoreTarget?
}

fun interface BookReaderEntryReadyStateFactoryAdapter {
    fun create(input: BookReaderEntryReadyStateFactory.Input): BookReaderEntryReadyState
}

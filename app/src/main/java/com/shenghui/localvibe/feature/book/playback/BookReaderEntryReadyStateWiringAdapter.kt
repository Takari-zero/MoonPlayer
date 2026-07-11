package com.shenghui.localvibe.feature.book.playback

object BookReaderEntryReadyStateWiringAdapter {
    data class Input(
        val bookId: String,
        val bookTitle: String,
        val paragraphs: List<String>,
        val restoreSnapshot: RestoreSnapshotInput?,
        val chapterIndex: Int = 0,
        val chapterTitle: String = "正文",
        val chapterStartIndex: Int = 0,
        val chapterEndExclusive: Int? = null,
        val speechRate: Float = 1f,
    )

    data class RestoreSnapshotInput(
        val paragraphIndex: Int,
        val sentenceIndex: Int,
        val cachedViewport: CachedViewportInput? = null,
    )

    data class CachedViewportInput(
        val firstVisibleParagraphIndex: Int,
        val firstVisibleSentenceIndex: Int,
        val firstVisibleChapterSentenceIndex: Int,
        val firstVisibleItemScrollOffset: Int,
    )

    fun load(input: Input): Result<BookReaderEntryReadyState> {
        val loader = BookReaderEntryReadyStateLoader(
            paragraphSource = BookReaderEntryParagraphSource { requestedBookId ->
                if (requestedBookId == input.bookId) {
                    Result.success(input.paragraphs)
                } else {
                    Result.failure(IllegalArgumentException("bookId does not match adapter input"))
                }
            },
            restoreSource = BookReaderEntryRestoreSource { requestedBookId ->
                input.restoreSnapshot
                    ?.takeIf { requestedBookId == input.bookId }
                    ?.toRestoreTarget()
            },
        )
        return loader.load(input.toLoadRequest())
    }

    private fun Input.toLoadRequest(): BookReaderEntryLoadRequest {
        return BookReaderEntryLoadRequest(
            bookId = bookId,
            bookTitle = bookTitle,
            chapterIndex = chapterIndex,
            chapterTitle = chapterTitle,
            chapterStartIndex = chapterStartIndex,
            chapterEndExclusive = chapterEndExclusive,
            speechRate = speechRate,
        )
    }

    private fun RestoreSnapshotInput.toRestoreTarget(): BookReaderEntryRestoreTarget {
        return BookReaderEntryRestoreTarget(
            paragraphIndex = paragraphIndex,
            sentenceIndex = sentenceIndex,
            cachedViewport = cachedViewport?.toCachedViewport(),
        )
    }

    private fun CachedViewportInput.toCachedViewport(): BookReaderEntryReadyStateFactory.CachedViewport {
        return BookReaderEntryReadyStateFactory.CachedViewport(
            firstVisibleParagraphIndex = firstVisibleParagraphIndex,
            firstVisibleSentenceIndex = firstVisibleSentenceIndex,
            firstVisibleChapterSentenceIndex = firstVisibleChapterSentenceIndex,
            firstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
        )
    }
}

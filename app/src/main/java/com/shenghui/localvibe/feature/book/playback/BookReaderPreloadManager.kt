package com.shenghui.localvibe.feature.book.playback

data class BookReaderPreloadResult(
    val key: BookDocumentCacheKey,
    val status: BookDocumentPreloadStatus,
    val readySnapshot: BookDocumentReadySnapshot? = null,
    val message: String? = null,
) {
    val isReady: Boolean
        get() = status == BookDocumentPreloadStatus.Ready && readySnapshot?.isReady == true
}

class BookReaderPreloadManager(
    private val repository: BookDocumentRepository,
    private val readyStateStore: BookReaderEntryReadyStateStore? = null,
) {
    fun preload(request: BookDocumentPreloadRequest): BookReaderPreloadResult {
        return repository.preload(request).fold(
            onSuccess = { entry ->
                val result = entry.toPreloadResult()
                writeReadyStateIfAvailable(result)
                result
            },
            onFailure = { error ->
                val cachedResult = repository.get(request.key)?.toPreloadResult()
                val result = cachedResult ?: BookReaderPreloadResult(
                    key = request.key,
                    status = BookDocumentPreloadStatus.Failed,
                    message = error.message,
                )
                writeFailureIfNeeded(
                    key = request.key,
                    message = result.message ?: error.message ?: "book reader preload failed",
                    cause = error,
                )
                result
            },
        )
    }

    fun preloadAll(requests: List<BookDocumentPreloadRequest>): List<BookReaderPreloadResult> {
        return requests
            .distinctBy { request -> request.key }
            .map { request -> preload(request) }
    }

    fun invalidate(
        key: BookDocumentCacheKey,
        nowMillis: Long,
    ): BookDocumentCacheEntry? {
        val documentEntry = repository.invalidate(key = key, nowMillis = nowMillis)
        readyStateStore?.invalidate(key = key, updatedAtMillis = nowMillis)
        return documentEntry
    }

    private fun writeReadyStateIfAvailable(result: BookReaderPreloadResult) {
        val snapshot = result.readySnapshot?.takeIf { it.isReady } ?: return
        if (readyStateStore?.getReadyState(result.key) != null) return
        readyStateStore?.putReady(
            key = result.key,
            readyState = snapshot.toEntryReadyState(),
            updatedAtMillis = snapshot.updatedAtMillis,
        )
    }

    private fun writeFailureIfNeeded(
        key: BookDocumentCacheKey,
        message: String,
        cause: Throwable,
    ) {
        readyStateStore?.putFailed(
            key = key,
            message = message,
            updatedAtMillis = repository.get(key)?.updatedAtMillis ?: 0L,
            cause = cause,
        )
    }

    private fun BookDocumentCacheEntry.toPreloadResult(): BookReaderPreloadResult {
        val snapshot = snapshotOrNull()?.takeIf { it.isReady }
        return BookReaderPreloadResult(
            key = key,
            status = preloadStatus,
            readySnapshot = snapshot,
            message = (state as? BookDocumentCacheState.Failed)?.message,
        )
    }

    private fun BookDocumentReadySnapshot.toEntryReadyState(): BookReaderEntryReadyState {
        return BookReaderEntryReadyState(
            bookId = document.key.bookId,
            bookTitle = document.title,
            paragraphs = document.paragraphs,
            chapterTitle = document.chapterTitle,
            chapterSentences = document.chapterSentences,
            canonicalTarget = canonicalTarget,
            progressSnapshot = progressSnapshot,
            lazyListInitialIndex = lazyListInitialIndex,
            lazyListInitialOffset = lazyListInitialOffset,
            playbackSeed = playbackSeed,
        )
    }
}

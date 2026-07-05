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
) {
    fun preload(request: BookDocumentPreloadRequest): BookReaderPreloadResult {
        return repository.preload(request).fold(
            onSuccess = { entry -> entry.toPreloadResult() },
            onFailure = { error ->
                repository.get(request.key)?.toPreloadResult()
                    ?: BookReaderPreloadResult(
                        key = request.key,
                        status = BookDocumentPreloadStatus.Failed,
                        message = error.message,
                    )
            },
        )
    }

    fun preloadAll(requests: List<BookDocumentPreloadRequest>): List<BookReaderPreloadResult> {
        return requests
            .distinctBy { request -> request.key }
            .map { request -> preload(request) }
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
}

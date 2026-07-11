package com.shenghui.localvibe.feature.book.playback

data class BookReaderBookshelfPreloadCandidate(
    val request: BookDocumentPreloadRequest,
)

data class BookReaderBookshelfPreloadPlanItem(
    val request: BookDocumentPreloadRequest,
)

class BookReaderBookshelfPreloadCoordinator(
    private val preloadManager: BookReaderPreloadManager,
    private val readyStateStore: BookReaderEntryReadyStateStore,
) {
    fun plan(
        candidates: List<BookReaderBookshelfPreloadCandidate>,
        recentBookKeys: List<BookDocumentCacheKey>,
        currentBookKey: BookDocumentCacheKey?,
        maxPreloadCount: Int,
    ): List<BookReaderBookshelfPreloadPlanItem> {
        require(maxPreloadCount >= 0) { "maxPreloadCount must be non-negative" }
        if (maxPreloadCount == 0) return emptyList()

        val recentPriority = recentBookKeys
            .mapIndexed { index, key -> key to index }
            .toMap()
        return candidates
            .withIndex()
            .distinctBy { indexed -> indexed.value.request.key }
            .filterNot { indexed -> readyStateStore.getReadyState(indexed.value.request.key) != null }
            .sortedWith(
                compareBy<IndexedValue<BookReaderBookshelfPreloadCandidate>> { indexed ->
                    priorityRank(
                        key = indexed.value.request.key,
                        currentBookKey = currentBookKey,
                        recentPriority = recentPriority,
                    )
                }.thenBy { indexed -> indexed.index }
            )
            .take(maxPreloadCount)
            .map { indexed -> BookReaderBookshelfPreloadPlanItem(indexed.value.request) }
    }

    fun preload(plan: List<BookReaderBookshelfPreloadPlanItem>): List<BookReaderPreloadResult> {
        return plan.map { item -> preloadManager.preload(item.request) }
    }

    private fun priorityRank(
        key: BookDocumentCacheKey,
        currentBookKey: BookDocumentCacheKey?,
        recentPriority: Map<BookDocumentCacheKey, Int>,
    ): Int {
        if (currentBookKey == key) return 0
        val recentIndex = recentPriority[key]
        if (recentIndex != null) return 1 + recentIndex
        return Int.MAX_VALUE
    }
}

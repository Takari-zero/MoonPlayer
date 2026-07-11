package com.shenghui.localvibe.feature.book.playback

data class BookReaderPreloadRuntime(
    val paragraphSource: BookParagraphSource,
    val documentRepository: BookDocumentRepository,
    val preloadManager: BookReaderPreloadManager,
    val readyStateStore: BookReaderEntryReadyStateStore,
    val bookshelfPreloadCoordinator: BookReaderBookshelfPreloadCoordinator,
) {
    companion object {
        fun create(
            paragraphSource: BookParagraphSource,
            clock: BookDocumentClock = SystemBookDocumentClock,
            readyStateStore: BookReaderEntryReadyStateStore = BookReaderEntryReadyStateStore(),
        ): BookReaderPreloadRuntime {
            val repository = BookDocumentRepository(
                paragraphSource = paragraphSource,
                clock = clock,
            )
            val preloadManager = BookReaderPreloadManager(
                repository = repository,
                readyStateStore = readyStateStore,
            )
            val coordinator = BookReaderBookshelfPreloadCoordinator(
                preloadManager = preloadManager,
                readyStateStore = readyStateStore,
            )
            return BookReaderPreloadRuntime(
                paragraphSource = paragraphSource,
                documentRepository = repository,
                preloadManager = preloadManager,
                readyStateStore = readyStateStore,
                bookshelfPreloadCoordinator = coordinator,
            )
        }
    }
}

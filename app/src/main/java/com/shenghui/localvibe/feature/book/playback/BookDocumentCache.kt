package com.shenghui.localvibe.feature.book.playback

data class BookDocumentCacheKey(
    val bookId: String,
) {
    init {
        require(bookId.isNotBlank()) { "bookId must not be blank" }
    }
}

data class BookDocument(
    val key: BookDocumentCacheKey,
    val title: String,
    val paragraphs: List<String>,
    val chapterTitle: String,
    val chapterSentences: List<BookReaderEntrySentence>,
    val parsedAtMillis: Long,
) {
    val hasReadyText: Boolean
        get() = paragraphs.any { it.isNotBlank() } && chapterSentences.isNotEmpty()

    init {
        require(title.isNotBlank()) { "title must not be blank" }
        require(parsedAtMillis >= 0L) { "parsedAtMillis must be non-negative" }
    }
}

enum class BookDocumentPreloadStatus {
    NotStarted,
    Loading,
    Parsed,
    Ready,
    Failed,
    Stale,
}

data class BookDocumentReadySnapshot(
    val document: BookDocument,
    val canonicalTarget: BookReaderEntryCanonicalTarget,
    val progressSnapshot: BookReaderEntryProgressSnapshot,
    val lazyListInitialIndex: Int,
    val lazyListInitialOffset: Int,
    val playbackSeed: BookReaderEntryPlaybackSeed,
    val preloadStatus: BookDocumentPreloadStatus,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
) {
    val firstFrameTargetEqualsPlaybackTarget: Boolean
        get() = playbackSeed.paragraphIndex == canonicalTarget.paragraphIndex &&
            playbackSeed.sentenceIndex == canonicalTarget.sentenceIndex &&
            progressSnapshot.chapterSentenceIndex == canonicalTarget.chapterSentenceIndex

    val isStale: Boolean
        get() = preloadStatus == BookDocumentPreloadStatus.Stale

    val isReady: Boolean
        get() = preloadStatus == BookDocumentPreloadStatus.Ready &&
            document.hasReadyText &&
            firstFrameTargetEqualsPlaybackTarget

    init {
        require(lazyListInitialIndex >= 0) { "lazyListInitialIndex must be non-negative" }
        require(lazyListInitialOffset >= 0) { "lazyListInitialOffset must be non-negative" }
        require(createdAtMillis >= 0L) { "createdAtMillis must be non-negative" }
        require(updatedAtMillis >= createdAtMillis) { "updatedAtMillis must not be older than createdAtMillis" }
        require(canonicalTarget.chapterSentenceIndex in document.chapterSentences.indices) {
            "canonicalTarget.chapterSentenceIndex must point into document chapterSentences"
        }
        require(progressSnapshot.chapterSentenceIndex == canonicalTarget.chapterSentenceIndex) {
            "progressSnapshot must use the canonical target"
        }
        require(playbackSeed.bookId == document.key.bookId) {
            "playbackSeed bookId must match document key"
        }
        require(firstFrameTargetEqualsPlaybackTarget) {
            "first frame target must match playback target"
        }
    }
}

sealed interface BookDocumentCacheState {
    data object Missing : BookDocumentCacheState

    data class Loading(
        val status: BookDocumentPreloadStatus = BookDocumentPreloadStatus.Loading,
    ) : BookDocumentCacheState

    data class Parsed(
        val document: BookDocument,
    ) : BookDocumentCacheState

    data class Ready(
        val snapshot: BookDocumentReadySnapshot,
    ) : BookDocumentCacheState

    data class Failed(
        val message: String,
        val cause: Throwable? = null,
    ) : BookDocumentCacheState {
        init {
            require(message.isNotBlank()) { "message must not be blank" }
        }
    }
}

data class BookDocumentCacheEntry(
    val key: BookDocumentCacheKey,
    val state: BookDocumentCacheState,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
) {
    val preloadStatus: BookDocumentPreloadStatus
        get() = when (val currentState = state) {
            BookDocumentCacheState.Missing -> BookDocumentPreloadStatus.NotStarted
            is BookDocumentCacheState.Loading -> currentState.status
            is BookDocumentCacheState.Parsed -> BookDocumentPreloadStatus.Parsed
            is BookDocumentCacheState.Ready -> currentState.snapshot.preloadStatus
            is BookDocumentCacheState.Failed -> BookDocumentPreloadStatus.Failed
        }

    val isReady: Boolean
        get() = snapshotOrNull()?.isReady == true

    fun snapshotOrNull(): BookDocumentReadySnapshot? {
        return (state as? BookDocumentCacheState.Ready)?.snapshot
    }

    init {
        require(createdAtMillis >= 0L) { "createdAtMillis must be non-negative" }
        require(updatedAtMillis >= createdAtMillis) { "updatedAtMillis must not be older than createdAtMillis" }
    }

    companion object {
        fun missing(
            key: BookDocumentCacheKey,
            nowMillis: Long,
        ): BookDocumentCacheEntry {
            return BookDocumentCacheEntry(
                key = key,
                state = BookDocumentCacheState.Missing,
                createdAtMillis = nowMillis,
                updatedAtMillis = nowMillis,
            )
        }

        fun loading(
            key: BookDocumentCacheKey,
            updatedAtMillis: Long,
            createdAtMillis: Long = updatedAtMillis,
        ): BookDocumentCacheEntry {
            return BookDocumentCacheEntry(
                key = key,
                state = BookDocumentCacheState.Loading(),
                createdAtMillis = createdAtMillis,
                updatedAtMillis = updatedAtMillis,
            )
        }

        fun ready(
            key: BookDocumentCacheKey,
            snapshot: BookDocumentReadySnapshot,
            createdAtMillis: Long,
            updatedAtMillis: Long,
        ): BookDocumentCacheEntry {
            require(snapshot.document.key == key) { "snapshot document key must match cache key" }
            return BookDocumentCacheEntry(
                key = key,
                state = BookDocumentCacheState.Ready(snapshot),
                createdAtMillis = createdAtMillis,
                updatedAtMillis = updatedAtMillis,
            )
        }

        fun failed(
            key: BookDocumentCacheKey,
            message: String,
            updatedAtMillis: Long,
            createdAtMillis: Long = updatedAtMillis,
            cause: Throwable? = null,
        ): BookDocumentCacheEntry {
            return BookDocumentCacheEntry(
                key = key,
                state = BookDocumentCacheState.Failed(
                    message = message,
                    cause = cause,
                ),
                createdAtMillis = createdAtMillis,
                updatedAtMillis = updatedAtMillis,
            )
        }
    }
}

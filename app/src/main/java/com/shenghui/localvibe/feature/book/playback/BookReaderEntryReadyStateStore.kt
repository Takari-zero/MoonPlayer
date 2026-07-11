package com.shenghui.localvibe.feature.book.playback

sealed interface BookReaderEntryReadyStateStoreState {
    data object Missing : BookReaderEntryReadyStateStoreState
    data object Loading : BookReaderEntryReadyStateStoreState

    data class Ready(
        val readyState: BookReaderEntryReadyState,
    ) : BookReaderEntryReadyStateStoreState

    data class Failed(
        val message: String,
        val cause: Throwable? = null,
    ) : BookReaderEntryReadyStateStoreState {
        init {
            require(message.isNotBlank()) { "message must not be blank" }
        }
    }

    data object Stale : BookReaderEntryReadyStateStoreState
}

data class BookReaderEntryReadyStateStoreEntry(
    val key: BookDocumentCacheKey,
    val state: BookReaderEntryReadyStateStoreState,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
) {
    val firstFrameTargetEqualsPlaybackTarget: Boolean
        get() {
            val readyState = readyStateOrNull() ?: return false
            return readyState.playbackSeed.paragraphIndex == readyState.canonicalTarget.paragraphIndex &&
                readyState.playbackSeed.sentenceIndex == readyState.canonicalTarget.sentenceIndex &&
                readyState.progressSnapshot.chapterSentenceIndex == readyState.canonicalTarget.chapterSentenceIndex
        }

    val isReady: Boolean
        get() = state is BookReaderEntryReadyStateStoreState.Ready && firstFrameTargetEqualsPlaybackTarget

    fun readyStateOrNull(): BookReaderEntryReadyState? {
        return (state as? BookReaderEntryReadyStateStoreState.Ready)?.readyState
    }

    fun isFresh(
        nowMillis: Long,
        maxAgeMillis: Long,
    ): Boolean {
        require(nowMillis >= 0L) { "nowMillis must be non-negative" }
        require(maxAgeMillis >= 0L) { "maxAgeMillis must be non-negative" }
        return nowMillis >= updatedAtMillis && nowMillis - updatedAtMillis <= maxAgeMillis
    }

    init {
        require(createdAtMillis >= 0L) { "createdAtMillis must be non-negative" }
        require(updatedAtMillis >= createdAtMillis) { "updatedAtMillis must not be older than createdAtMillis" }
        val readyState = readyStateOrNull()
        require(readyState == null || readyState.bookId == key.bookId) {
            "readyState bookId must match cache key"
        }
    }
}

class BookReaderEntryReadyStateStore {
    private val entries = linkedMapOf<BookDocumentCacheKey, BookReaderEntryReadyStateStoreEntry>()

    fun get(key: BookDocumentCacheKey): BookReaderEntryReadyStateStoreEntry {
        return entries[key] ?: BookReaderEntryReadyStateStoreEntry(
            key = key,
            state = BookReaderEntryReadyStateStoreState.Missing,
            createdAtMillis = 0L,
            updatedAtMillis = 0L,
        )
    }

    fun getReadyState(
        key: BookDocumentCacheKey,
        nowMillis: Long? = null,
        maxAgeMillis: Long? = null,
    ): BookReaderEntryReadyState? {
        val entry = entries[key] ?: return null
        if (!entry.isReady) return null
        if (nowMillis != null || maxAgeMillis != null) {
            require(nowMillis != null && maxAgeMillis != null) {
                "nowMillis and maxAgeMillis must be provided together"
            }
            if (!entry.isFresh(nowMillis = nowMillis, maxAgeMillis = maxAgeMillis)) return null
        }
        return entry.readyStateOrNull()
    }

    fun putLoading(
        key: BookDocumentCacheKey,
        updatedAtMillis: Long,
    ): BookReaderEntryReadyStateStoreEntry {
        return put(
            key = key,
            state = BookReaderEntryReadyStateStoreState.Loading,
            updatedAtMillis = updatedAtMillis,
        )
    }

    fun putReady(
        key: BookDocumentCacheKey,
        readyState: BookReaderEntryReadyState,
        updatedAtMillis: Long,
    ): BookReaderEntryReadyStateStoreEntry {
        require(readyState.bookId == key.bookId) { "readyState bookId must match cache key" }
        return put(
            key = key,
            state = BookReaderEntryReadyStateStoreState.Ready(readyState = readyState),
            updatedAtMillis = updatedAtMillis,
        )
    }

    fun putFailed(
        key: BookDocumentCacheKey,
        message: String,
        updatedAtMillis: Long,
        cause: Throwable? = null,
    ): BookReaderEntryReadyStateStoreEntry {
        return put(
            key = key,
            state = BookReaderEntryReadyStateStoreState.Failed(
                message = message,
                cause = cause,
            ),
            updatedAtMillis = updatedAtMillis,
        )
    }

    fun invalidate(
        key: BookDocumentCacheKey,
        updatedAtMillis: Long,
    ): BookReaderEntryReadyStateStoreEntry? {
        val existing = entries[key] ?: return null
        val entry = BookReaderEntryReadyStateStoreEntry(
            key = key,
            state = BookReaderEntryReadyStateStoreState.Stale,
            createdAtMillis = existing.createdAtMillis,
            updatedAtMillis = updatedAtMillis,
        )
        entries[key] = entry
        return entry
    }

    fun clear() {
        entries.clear()
    }

    private fun put(
        key: BookDocumentCacheKey,
        state: BookReaderEntryReadyStateStoreState,
        updatedAtMillis: Long,
    ): BookReaderEntryReadyStateStoreEntry {
        val createdAtMillis = entries[key]?.createdAtMillis ?: updatedAtMillis
        val entry = BookReaderEntryReadyStateStoreEntry(
            key = key,
            state = state,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = updatedAtMillis,
        )
        entries[key] = entry
        return entry
    }
}

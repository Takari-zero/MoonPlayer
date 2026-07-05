package com.shenghui.localvibe.feature.book.playback

data class BookDocumentPreloadRequest(
    val key: BookDocumentCacheKey,
    val title: String,
    val savedParagraphIndex: Int = 0,
    val savedSentenceIndex: Int = 0,
    val chapterIndex: Int = 0,
    val chapterTitle: String = "??",
    val chapterStartIndex: Int = 0,
    val chapterEndExclusive: Int? = null,
    val speechRate: Float = 1f,
) {
    init {
        require(title.isNotBlank()) { "title must not be blank" }
        require(savedParagraphIndex >= 0) { "savedParagraphIndex must be non-negative" }
        require(savedSentenceIndex >= 0) { "savedSentenceIndex must be non-negative" }
        require(chapterIndex >= 0) { "chapterIndex must be non-negative" }
        require(chapterStartIndex >= 0) { "chapterStartIndex must be non-negative" }
        require(speechRate > 0f) { "speechRate must be positive" }
    }
}

fun interface BookDocumentParagraphSource {
    fun loadParagraphs(key: BookDocumentCacheKey): Result<List<String>>
}

fun interface BookDocumentClock {
    fun nowMillis(): Long
}

object SystemBookDocumentClock : BookDocumentClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}

class IncrementingBookDocumentClock(
    start: Long = 0L,
) : BookDocumentClock {
    private var next = start

    override fun nowMillis(): Long {
        val current = next
        next += 1L
        return current
    }
}

class BookDocumentRepository(
    private val paragraphSource: BookDocumentParagraphSource,
    private val clock: BookDocumentClock = SystemBookDocumentClock,
) {
    private val entries = linkedMapOf<BookDocumentCacheKey, BookDocumentCacheEntry>()

    fun get(key: BookDocumentCacheKey): BookDocumentCacheEntry? = entries[key]

    fun put(entry: BookDocumentCacheEntry): BookDocumentCacheEntry {
        entries[entry.key] = entry
        return entry
    }

    fun invalidate(
        key: BookDocumentCacheKey,
        nowMillis: Long = clock.nowMillis(),
    ): BookDocumentCacheEntry? {
        val current = entries[key] ?: return null
        val staleEntry = when (val state = current.state) {
            is BookDocumentCacheState.Ready -> current.copy(
                state = BookDocumentCacheState.Ready(
                    state.snapshot.copy(
                        preloadStatus = BookDocumentPreloadStatus.Stale,
                        updatedAtMillis = nowMillis,
                    )
                ),
                updatedAtMillis = nowMillis,
            )
            else -> current.copy(updatedAtMillis = nowMillis)
        }
        entries[key] = staleEntry
        return staleEntry
    }

    fun preload(request: BookDocumentPreloadRequest): Result<BookDocumentCacheEntry> {
        val cached = entries[request.key]
        if (cached?.isReady == true) {
            return Result.success(cached)
        }

        val loadingTime = clock.nowMillis()
        entries[request.key] = BookDocumentCacheEntry.loading(
            key = request.key,
            updatedAtMillis = loadingTime,
            createdAtMillis = cached?.createdAtMillis ?: loadingTime,
        )

        val paragraphs = paragraphSource.loadParagraphs(request.key).getOrElse { error ->
            val failed = BookDocumentCacheEntry.failed(
                key = request.key,
                message = error.message ?: "book document preload failed",
                createdAtMillis = cached?.createdAtMillis ?: loadingTime,
                updatedAtMillis = clock.nowMillis(),
                cause = error,
            )
            entries[request.key] = failed
            return Result.failure(error)
        }
        if (paragraphs.none { it.isNotBlank() }) {
            val error = IllegalArgumentException("paragraphs must not be empty")
            val failed = BookDocumentCacheEntry.failed(
                key = request.key,
                message = error.message ?: "paragraphs must not be empty",
                createdAtMillis = cached?.createdAtMillis ?: loadingTime,
                updatedAtMillis = clock.nowMillis(),
                cause = error,
            )
            entries[request.key] = failed
            return Result.failure(error)
        }

        return runCatching {
            val readyState = BookReaderEntryReadyStateFactory.create(
                BookReaderEntryReadyStateFactory.Input(
                    bookId = request.key.bookId,
                    bookTitle = request.title,
                    paragraphs = paragraphs,
                    savedParagraphIndex = request.savedParagraphIndex,
                    savedSentenceIndex = request.savedSentenceIndex,
                    chapterIndex = request.chapterIndex,
                    chapterTitle = request.chapterTitle,
                    chapterStartIndex = request.chapterStartIndex,
                    chapterEndExclusive = request.chapterEndExclusive ?: paragraphs.size,
                    speechRate = request.speechRate,
                )
            )
            val preparedAt = clock.nowMillis()
            val document = BookDocument(
                key = request.key,
                title = request.title,
                paragraphs = readyState.paragraphs,
                chapterTitle = readyState.chapterTitle,
                chapterSentences = readyState.chapterSentences,
                parsedAtMillis = preparedAt,
            )
            val snapshot = BookDocumentReadySnapshot(
                document = document,
                canonicalTarget = readyState.canonicalTarget,
                progressSnapshot = readyState.progressSnapshot,
                lazyListInitialIndex = readyState.lazyListInitialIndex,
                lazyListInitialOffset = readyState.lazyListInitialOffset,
                playbackSeed = readyState.playbackSeed,
                preloadStatus = BookDocumentPreloadStatus.Ready,
                createdAtMillis = cached?.createdAtMillis ?: loadingTime,
                updatedAtMillis = preparedAt,
            )
            BookDocumentCacheEntry.ready(
                key = request.key,
                snapshot = snapshot,
                createdAtMillis = cached?.createdAtMillis ?: loadingTime,
                updatedAtMillis = preparedAt,
            )
        }.fold(
            onSuccess = { entry ->
                entries[request.key] = entry
                Result.success(entry)
            },
            onFailure = { error ->
                val failed = BookDocumentCacheEntry.failed(
                    key = request.key,
                    message = error.message ?: "book document preload failed",
                    createdAtMillis = cached?.createdAtMillis ?: loadingTime,
                    updatedAtMillis = clock.nowMillis(),
                    cause = error,
                )
                entries[request.key] = failed
                Result.failure(error)
            },
        )
    }
}

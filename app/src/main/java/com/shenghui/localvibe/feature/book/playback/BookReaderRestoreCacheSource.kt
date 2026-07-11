package com.shenghui.localvibe.feature.book.playback

import android.content.Context

data class BookReaderRestoreCache(
    val bookUri: String,
    val bookTitle: String,
    val lastParagraphIndex: Int,
    val lastSentenceIndexInParagraph: Int,
    val lastChapterSentenceIndex: Int,
    val lastReadingTargetName: String,
    val lastChapterTitle: String,
    val cachedVisibleText: List<String>,
    val cachedVisibleParagraphIndexes: List<Int> = emptyList(),
    val cachedVisibleSentenceIndexes: List<Int> = emptyList(),
    val cachedVisibleChapterSentenceIndexes: List<Int> = emptyList(),
    val cachedChapterSentenceCount: Int = 0,
    val cachedProgressValue: Float = -1f,
    val cachedProgressMaxValue: Float = -1f,
    val cachedListenedTimeLabel: String = "",
    val cachedRemainingTimeLabel: String = "",
    val viewportFirstVisibleItemIndex: Int = 0,
    val viewportFirstVisibleItemScrollOffset: Int = 0,
    val updatedAt: Long,
) {
    fun hasViewportSnapshot(): Boolean {
        return cachedVisibleText.isNotEmpty() &&
            cachedVisibleParagraphIndexes.size == cachedVisibleText.size &&
            cachedVisibleSentenceIndexes.size == cachedVisibleText.size &&
            cachedVisibleChapterSentenceIndexes.size == cachedVisibleText.size
    }
}

object BookReaderRestoreCacheSource {
    const val PREFS_NAME = "book_read_state_cache"
    const val SEPARATOR = "\u001E"

    private const val DEFAULT_READING_TARGET_NAME = "SENTENCE"

    data class ValueReader(
        val getString: (String) -> String?,
        val getInt: (String, Int) -> Int,
        val getFloat: (String, Float) -> Float,
        val getLong: (String, Long) -> Long,
    )

    data class ValueWriter(
        val putString: (String, String?) -> Unit,
        val putInt: (String, Int) -> Unit,
        val putFloat: (String, Float) -> Unit,
        val putLong: (String, Long) -> Unit,
    )

    fun keyPrefix(bookUri: String): String {
        return "book_${bookUri.hashCode()}_"
    }

    fun decode(bookUri: String?, reader: ValueReader): BookReaderRestoreCache? {
        if (bookUri.isNullOrBlank()) return null
        val prefix = keyPrefix(bookUri)
        val storedUri = reader.getString(prefix + "uri") ?: return null
        if (storedUri != bookUri) return null
        val cachedText = reader
            .getString(prefix + "text")
            ?.split(SEPARATOR)
            ?.filter { it.isNotBlank() }
            .orEmpty()
        return BookReaderRestoreCache(
            bookUri = storedUri,
            bookTitle = reader.getString(prefix + "title").orEmpty(),
            lastParagraphIndex = reader.getInt(prefix + "paragraph", 0),
            lastSentenceIndexInParagraph = reader.getInt(prefix + "sentence", 0),
            lastChapterSentenceIndex = reader.getInt(prefix + "chapter_sentence", 0),
            lastReadingTargetName = reader.getString(prefix + "reading_target")
                ?: DEFAULT_READING_TARGET_NAME,
            lastChapterTitle = reader.getString(prefix + "chapter_title").orEmpty(),
            cachedVisibleText = cachedText,
            cachedVisibleParagraphIndexes = parseIntList(reader.getString(prefix + "visible_paragraphs")),
            cachedVisibleSentenceIndexes = parseIntList(reader.getString(prefix + "visible_sentences")),
            cachedVisibleChapterSentenceIndexes = parseIntList(reader.getString(prefix + "visible_chapter_sentences")),
            cachedChapterSentenceCount = reader.getInt(prefix + "chapter_sentence_count", 0),
            cachedProgressValue = reader.getFloat(prefix + "progress_value", -1f),
            cachedProgressMaxValue = reader.getFloat(prefix + "progress_max_value", -1f),
            cachedListenedTimeLabel = reader.getString(prefix + "listened_time_label").orEmpty(),
            cachedRemainingTimeLabel = reader.getString(prefix + "remaining_time_label").orEmpty(),
            viewportFirstVisibleItemIndex = reader.getInt(prefix + "first_visible_item", 0),
            viewportFirstVisibleItemScrollOffset = reader.getInt(prefix + "first_visible_offset", 0),
            updatedAt = reader.getLong(prefix + "updated_at", 0L),
        )
    }

    fun encode(state: BookReaderRestoreCache, writer: ValueWriter) {
        if (state.bookUri.isBlank()) return
        val prefix = keyPrefix(state.bookUri)
        writer.putString(prefix + "uri", state.bookUri)
        writer.putString(prefix + "title", state.bookTitle)
        writer.putInt(prefix + "paragraph", state.lastParagraphIndex.coerceAtLeast(0))
        writer.putInt(prefix + "sentence", state.lastSentenceIndexInParagraph.coerceAtLeast(0))
        writer.putInt(prefix + "chapter_sentence", state.lastChapterSentenceIndex.coerceAtLeast(0))
        writer.putString(prefix + "reading_target", state.lastReadingTargetName)
        writer.putString(prefix + "chapter_title", state.lastChapterTitle)
        writer.putString(prefix + "text", state.cachedVisibleText.joinToString(SEPARATOR))
        writer.putString(prefix + "visible_paragraphs", state.cachedVisibleParagraphIndexes.joinToString(SEPARATOR))
        writer.putString(prefix + "visible_sentences", state.cachedVisibleSentenceIndexes.joinToString(SEPARATOR))
        writer.putString(prefix + "visible_chapter_sentences", state.cachedVisibleChapterSentenceIndexes.joinToString(SEPARATOR))
        writer.putInt(prefix + "chapter_sentence_count", state.cachedChapterSentenceCount.coerceAtLeast(0))
        writer.putFloat(prefix + "progress_value", state.cachedProgressValue)
        writer.putFloat(prefix + "progress_max_value", state.cachedProgressMaxValue)
        writer.putString(prefix + "listened_time_label", state.cachedListenedTimeLabel)
        writer.putString(prefix + "remaining_time_label", state.cachedRemainingTimeLabel)
        writer.putInt(prefix + "first_visible_item", state.viewportFirstVisibleItemIndex.coerceAtLeast(0))
        writer.putInt(prefix + "first_visible_offset", state.viewportFirstVisibleItemScrollOffset.coerceAtLeast(0))
        writer.putLong(prefix + "updated_at", state.updatedAt)
    }

    fun load(context: Context, bookUri: String?): BookReaderRestoreCache? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return decode(
            bookUri = bookUri,
            reader = ValueReader(
                getString = { key -> prefs.getString(key, null) },
                getInt = { key, default -> prefs.getInt(key, default) },
                getFloat = { key, default -> prefs.getFloat(key, default) },
                getLong = { key, default -> prefs.getLong(key, default) },
            )
        )
    }

    fun save(context: Context, state: BookReaderRestoreCache) {
        if (state.bookUri.isBlank()) return
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        encode(
            state = state,
            writer = ValueWriter(
                putString = { key, value -> editor.putString(key, value) },
                putInt = { key, value -> editor.putInt(key, value) },
                putFloat = { key, value -> editor.putFloat(key, value) },
                putLong = { key, value -> editor.putLong(key, value) },
            )
        )
        editor.apply()
    }

    fun parseIntList(raw: String?): List<Int> {
        return raw
            ?.split(SEPARATOR)
            ?.mapNotNull { it.toIntOrNull() }
            .orEmpty()
    }
}

fun BookReaderRestoreCache.stableUiSnapshotSource(): String {
    return when {
        cachedProgressValue >= 0f &&
            cachedProgressMaxValue > 0f &&
            cachedListenedTimeLabel.isNotBlank() &&
            cachedRemainingTimeLabel.isNotBlank() -> "stable_snapshot"
        hasViewportSnapshot() -> "legacy_viewport_backfill"
        else -> "recent_record_backfill"
    }
}

fun BookReaderRestoreCache.firstVisibleParagraphIndex(): Int? =
    cachedVisibleParagraphIndexes.firstOrNull()

fun BookReaderRestoreCache.firstVisibleSentenceIndex(): Int? =
    cachedVisibleSentenceIndexes.firstOrNull()

fun BookReaderRestoreCache.firstVisibleChapterSentenceIndex(): Int? =
    cachedVisibleChapterSentenceIndexes.firstOrNull()

fun BookReaderRestoreCache.toEntryRestoreSnapshotInput(): BookReaderEntryReadyStateWiringAdapter.RestoreSnapshotInput {
    return BookReaderEntryReadyStateWiringAdapter.RestoreSnapshotInput(
        paragraphIndex = lastParagraphIndex,
        sentenceIndex = lastSentenceIndexInParagraph,
        cachedViewport = BookReaderEntryReadyStateWiringAdapter.CachedViewportInput(
            firstVisibleParagraphIndex = firstVisibleParagraphIndex() ?: lastParagraphIndex,
            firstVisibleSentenceIndex = firstVisibleSentenceIndex() ?: lastSentenceIndexInParagraph,
            firstVisibleChapterSentenceIndex = firstVisibleChapterSentenceIndex() ?: lastChapterSentenceIndex,
            firstVisibleItemScrollOffset = viewportFirstVisibleItemScrollOffset,
        )
    )
}

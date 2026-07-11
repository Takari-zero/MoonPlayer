package com.shenghui.localvibe.feature.book.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookReaderRestoreCacheSourceTest {
    @Test
    fun decodesStoredRestoreCacheWithViewportAndProgressFields() {
        val bookUri = "file://demo-book.txt"
        val values = mutableMapOf<String, Any?>()
        val prefix = BookReaderRestoreCacheSource.keyPrefix(bookUri)
        values[prefix + "uri"] = bookUri
        values[prefix + "title"] = "Demo Book"
        values[prefix + "paragraph"] = 8
        values[prefix + "sentence"] = 2
        values[prefix + "chapter_sentence"] = 21
        values[prefix + "reading_target"] = "SENTENCE"
        values[prefix + "chapter_title"] = "Chapter 5"
        values[prefix + "text"] = listOf("line one", "line two").joinToString(BookReaderRestoreCacheSource.SEPARATOR)
        values[prefix + "visible_paragraphs"] = listOf(8, 9).joinToString(BookReaderRestoreCacheSource.SEPARATOR)
        values[prefix + "visible_sentences"] = listOf(2, 0).joinToString(BookReaderRestoreCacheSource.SEPARATOR)
        values[prefix + "visible_chapter_sentences"] = listOf(21, 22).joinToString(BookReaderRestoreCacheSource.SEPARATOR)
        values[prefix + "chapter_sentence_count"] = 40
        values[prefix + "progress_value"] = 21f
        values[prefix + "progress_max_value"] = 39f
        values[prefix + "listened_time_label"] = "08:33"
        values[prefix + "remaining_time_label"] = "02:25"
        values[prefix + "first_visible_item"] = 22
        values[prefix + "first_visible_offset"] = 64
        values[prefix + "updated_at"] = 1234L

        val cache = BookReaderRestoreCacheSource.decode(bookUri, mapReader(values))

        requireNotNull(cache)
        assertEquals(bookUri, cache.bookUri)
        assertEquals("Demo Book", cache.bookTitle)
        assertEquals(8, cache.lastParagraphIndex)
        assertEquals(2, cache.lastSentenceIndexInParagraph)
        assertEquals(21, cache.lastChapterSentenceIndex)
        assertEquals("SENTENCE", cache.lastReadingTargetName)
        assertEquals("Chapter 5", cache.lastChapterTitle)
        assertEquals(listOf("line one", "line two"), cache.cachedVisibleText)
        assertEquals(listOf(8, 9), cache.cachedVisibleParagraphIndexes)
        assertEquals(listOf(2, 0), cache.cachedVisibleSentenceIndexes)
        assertEquals(listOf(21, 22), cache.cachedVisibleChapterSentenceIndexes)
        assertEquals(40, cache.cachedChapterSentenceCount)
        assertEquals(21f, cache.cachedProgressValue)
        assertEquals(39f, cache.cachedProgressMaxValue)
        assertEquals("08:33", cache.cachedListenedTimeLabel)
        assertEquals("02:25", cache.cachedRemainingTimeLabel)
        assertEquals(22, cache.viewportFirstVisibleItemIndex)
        assertEquals(64, cache.viewportFirstVisibleItemScrollOffset)
        assertEquals(1234L, cache.updatedAt)
        assertTrue(cache.hasViewportSnapshot())
    }

    @Test
    fun returnsNullWhenCacheIsMissingOrForDifferentBook() {
        val bookUri = "file://demo-book.txt"
        val prefix = BookReaderRestoreCacheSource.keyPrefix(bookUri)

        assertNull(BookReaderRestoreCacheSource.decode(bookUri, mapReader(emptyMap())))
        assertNull(BookReaderRestoreCacheSource.decode(null, mapReader(emptyMap())))
        assertNull(BookReaderRestoreCacheSource.decode("", mapReader(emptyMap())))
        assertNull(
            BookReaderRestoreCacheSource.decode(
                bookUri,
                mapReader(mapOf(prefix + "uri" to "file://other.txt")),
            )
        )
    }

    @Test
    fun safelyFallsBackWhenOptionalFieldsAreMissingOrMalformed() {
        val bookUri = "file://demo-book.txt"
        val prefix = BookReaderRestoreCacheSource.keyPrefix(bookUri)
        val cache = BookReaderRestoreCacheSource.decode(
            bookUri,
            mapReader(
                mapOf(
                    prefix + "uri" to bookUri,
                    prefix + "visible_paragraphs" to "1${BookReaderRestoreCacheSource.SEPARATOR}bad${BookReaderRestoreCacheSource.SEPARATOR}3",
                )
            ),
        )

        requireNotNull(cache)
        assertEquals("", cache.bookTitle)
        assertEquals(0, cache.lastParagraphIndex)
        assertEquals(0, cache.lastSentenceIndexInParagraph)
        assertEquals(0, cache.lastChapterSentenceIndex)
        assertEquals("SENTENCE", cache.lastReadingTargetName)
        assertEquals(emptyList<String>(), cache.cachedVisibleText)
        assertEquals(listOf(1, 3), cache.cachedVisibleParagraphIndexes)
        assertEquals(-1f, cache.cachedProgressValue)
        assertEquals(-1f, cache.cachedProgressMaxValue)
        assertEquals("", cache.cachedListenedTimeLabel)
        assertEquals("", cache.cachedRemainingTimeLabel)
        assertFalse(cache.hasViewportSnapshot())
    }

    @Test
    fun encodesRestoreCacheUsingTheExistingPreferenceKeys() {
        val bookUri = "file://demo-book.txt"
        val values = mutableMapOf<String, Any?>()
        val cache = BookReaderRestoreCache(
            bookUri = bookUri,
            bookTitle = "Demo Book",
            lastParagraphIndex = 3,
            lastSentenceIndexInParagraph = 1,
            lastChapterSentenceIndex = 9,
            lastReadingTargetName = "PARAGRAPH",
            lastChapterTitle = "Chapter",
            cachedVisibleText = listOf("a", "b"),
            cachedVisibleParagraphIndexes = listOf(3, 4),
            cachedVisibleSentenceIndexes = listOf(1, 0),
            cachedVisibleChapterSentenceIndexes = listOf(9, 10),
            cachedChapterSentenceCount = 12,
            cachedProgressValue = 9f,
            cachedProgressMaxValue = 11f,
            cachedListenedTimeLabel = "01:00",
            cachedRemainingTimeLabel = "00:20",
            viewportFirstVisibleItemIndex = 10,
            viewportFirstVisibleItemScrollOffset = 24,
            updatedAt = 55L,
        )

        BookReaderRestoreCacheSource.encode(cache, mapWriter(values))
        val restored = BookReaderRestoreCacheSource.decode(bookUri, mapReader(values))

        assertEquals(cache, restored)
    }

    @Test
    fun restoreSnapshotInputUsesCanonicalRestoreTargetAndOnlyCarriesCachedViewportAsInput() {
        val cache = BookReaderRestoreCache(
            bookUri = "file://demo-book.txt",
            bookTitle = "Demo Book",
            lastParagraphIndex = 2,
            lastSentenceIndexInParagraph = 1,
            lastChapterSentenceIndex = 4,
            lastReadingTargetName = "SENTENCE",
            lastChapterTitle = "Chapter",
            cachedVisibleText = listOf("stale first line"),
            cachedVisibleParagraphIndexes = listOf(0),
            cachedVisibleSentenceIndexes = listOf(0),
            cachedVisibleChapterSentenceIndexes = listOf(0),
            viewportFirstVisibleItemScrollOffset = 96,
            updatedAt = 55L,
        )

        val restoreSnapshot = cache.toEntryRestoreSnapshotInput()
        val state = BookReaderEntryReadyStateWiringAdapter.load(
            BookReaderEntryReadyStateWiringAdapter.Input(
                bookId = cache.bookUri,
                bookTitle = cache.bookTitle,
                paragraphs = listOf(
                    "第一句。第二句。",
                    "第三句。",
                    "第四句。第五句。",
                ),
                restoreSnapshot = restoreSnapshot,
                chapterTitle = "正文",
            )
        ).getOrThrow()

        assertEquals(2, restoreSnapshot.paragraphIndex)
        assertEquals(1, restoreSnapshot.sentenceIndex)
        assertEquals(0, restoreSnapshot.cachedViewport?.firstVisibleParagraphIndex)
        assertEquals(2, state.canonicalTarget.paragraphIndex)
        assertEquals(1, state.canonicalTarget.sentenceIndex)
        assertEquals(4, state.canonicalTarget.chapterSentenceIndex)
        assertEquals(0, state.lazyListInitialOffset)
    }

    private fun mapReader(values: Map<String, Any?>): BookReaderRestoreCacheSource.ValueReader {
        return BookReaderRestoreCacheSource.ValueReader(
            getString = { key -> values[key] as? String },
            getInt = { key, default -> values[key] as? Int ?: default },
            getFloat = { key, default -> values[key] as? Float ?: default },
            getLong = { key, default -> values[key] as? Long ?: default },
        )
    }

    private fun mapWriter(values: MutableMap<String, Any?>): BookReaderRestoreCacheSource.ValueWriter {
        return BookReaderRestoreCacheSource.ValueWriter(
            putString = { key, value -> values[key] = value },
            putInt = { key, value -> values[key] = value },
            putFloat = { key, value -> values[key] = value },
            putLong = { key, value -> values[key] = value },
        )
    }
}

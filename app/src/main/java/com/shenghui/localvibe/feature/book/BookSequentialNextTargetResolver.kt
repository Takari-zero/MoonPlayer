package com.shenghui.localvibe.feature.book

import com.shenghui.localvibe.core.book.BookChapter

data class BookSequentialTarget(
    val paragraphIndex: Int,
    val sentenceIndexInParagraph: Int,
    val isChapterTitle: Boolean = false
)

data class BookMatchaNextPlayableTarget(
    val paragraphIndex: Int,
    val sentenceIndexInParagraph: Int,
    val chapterSentenceIndex: Int,
    val text: String
)

/** Pure next-target resolution shared by formal playback and Matcha prewarm. */
object BookSequentialNextTargetResolver {
    fun resolve(
        current: BookSequentialTarget,
        snapshot: BookSequentialTargetSnapshot
    ): BookSequentialTarget? {
        if (snapshot.chapters.isEmpty()) return null
        val chapterIndex = snapshot.chapters.indexOfLast {
            it.paragraphIndex <= current.paragraphIndex
        }
        val chapter = snapshot.chapters.getOrNull(chapterIndex) ?: return null

        if (current.isChapterTitle) {
            return chapter.sentences.firstOrNull { isReadableBookTtsText(it.text) }?.let {
                BookSequentialTarget(it.paragraphIndex, it.sentenceIndexInParagraph)
            }
        }

        val currentSentenceIndex = chapter.sentences.indexOfFirst {
            it.paragraphIndex == current.paragraphIndex &&
                it.sentenceIndexInParagraph == current.sentenceIndexInParagraph
        }
        if (currentSentenceIndex < 0) return null
        chapter.sentences.drop(currentSentenceIndex + 1)
            .firstOrNull { isReadableBookTtsText(it.text) }
            ?.let {
            return BookSequentialTarget(it.paragraphIndex, it.sentenceIndexInParagraph)
        }

        val nextChapter = snapshot.chapters.getOrNull(chapterIndex + 1) ?: return null
        return BookSequentialTarget(
            paragraphIndex = nextChapter.paragraphIndex,
            sentenceIndexInParagraph = 0,
            isChapterTitle = true
        )
    }

    fun resolve(
        current: BookSequentialTarget,
        paragraphs: List<String>,
        paragraphSentences: List<List<String>>,
        chapters: List<BookChapter>
    ): BookSequentialTarget? {
        if (paragraphSentences.isEmpty()) return null

        if (current.isChapterTitle) {
            val chapterIndex = chapters.indexOfLast { it.paragraphIndex <= current.paragraphIndex }
            val chapter = chapters.getOrNull(chapterIndex) ?: return null
            val chapterEnd = chapters.getOrNull(chapterIndex + 1)?.paragraphIndex
                ?.coerceIn(0, paragraphSentences.size)
                ?: paragraphSentences.size
            for (paragraphIndex in chapter.paragraphIndex.coerceAtLeast(0) until chapterEnd) {
                val isExactChapterTitle = paragraphIndex == chapter.paragraphIndex &&
                    paragraphs.getOrNull(paragraphIndex)?.trim() == chapter.title.trim()
                if (isExactChapterTitle) continue
                val sentenceIndex = paragraphSentences[paragraphIndex]
                    .indexOfFirst(::isReadableBookTtsText)
                if (sentenceIndex >= 0) {
                    return BookSequentialTarget(paragraphIndex, sentenceIndex)
                }
            }
            return null
        }

        val currentSentences = paragraphSentences.getOrNull(current.paragraphIndex).orEmpty()
        for (sentenceIndex in (current.sentenceIndexInParagraph + 1) until currentSentences.size) {
            if (isReadableBookTtsText(currentSentences[sentenceIndex])) {
                return BookSequentialTarget(current.paragraphIndex, sentenceIndex)
            }
        }

        for (paragraphIndex in (current.paragraphIndex + 1) until paragraphSentences.size) {
            val chapter = chapters.firstOrNull { it.paragraphIndex == paragraphIndex }
            if (chapter != null) {
                return BookSequentialTarget(paragraphIndex, 0, isChapterTitle = true)
            }
            val sentenceIndex = paragraphSentences[paragraphIndex]
                .indexOfFirst(::isReadableBookTtsText)
            if (sentenceIndex >= 0) {
                return BookSequentialTarget(paragraphIndex, sentenceIndex)
            }
        }
        return null
    }
}

fun isReadableBookTtsText(text: String): Boolean {
    val trimmed = text.trim()
    if (trimmed.isBlank()) return false
    return trimmed.any { it.isLetterOrDigit() }
}

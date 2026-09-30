package com.shenghui.localvibe.feature.book

import com.shenghui.localvibe.core.book.BookChapter

data class BookSequentialSentenceSnapshot(
    val paragraphIndex: Int,
    val sentenceIndexInParagraph: Int,
    val chapterSentenceIndex: Int,
    val text: String
)

data class BookSequentialChapterSnapshot(
    val chapterIndex: Int,
    val title: String,
    val paragraphIndex: Int,
    val endParagraphExclusive: Int,
    val sentences: List<BookSequentialSentenceSnapshot>
)

data class BookSequentialTargetSnapshot(
    val chapters: List<BookSequentialChapterSnapshot>
)

fun buildBookSequentialTargetSnapshot(
    paragraphs: List<String>,
    chapters: List<BookChapter>,
    splitParagraph: (String) -> List<String>
): BookSequentialTargetSnapshot {
    if (paragraphs.isEmpty()) return BookSequentialTargetSnapshot(emptyList())
    val effectiveChapters = if (chapters.isEmpty()) {
        listOf(BookChapter(title = "", paragraphIndex = 0))
    } else {
        chapters
    }
    val snapshots = effectiveChapters.mapIndexed { chapterIndex, chapter ->
        val endExclusive = effectiveChapters.getOrNull(chapterIndex + 1)?.paragraphIndex
            ?.coerceIn(chapter.paragraphIndex, paragraphs.size)
            ?: paragraphs.size
        val safeStart = chapter.paragraphIndex.coerceIn(0, paragraphs.size)
        val safeEnd = endExclusive.coerceIn(safeStart, paragraphs.size)
        val title = chapter.title.trim()
        val sentences = buildList {
            for (paragraphIndex in safeStart until safeEnd) {
                val paragraph = paragraphs[paragraphIndex].trim()
                if (paragraph.isBlank()) continue
                if (paragraphIndex == safeStart && title.isNotBlank() && paragraph == title) continue
                splitParagraph(paragraph).forEachIndexed { sentenceIndex, sentence ->
                    if (sentence.isNotBlank()) {
                        add(
                            BookSequentialSentenceSnapshot(
                                paragraphIndex = paragraphIndex,
                                sentenceIndexInParagraph = sentenceIndex,
                                chapterSentenceIndex = size,
                                text = sentence
                            )
                        )
                    }
                }
            }
        }
        BookSequentialChapterSnapshot(
            chapterIndex = chapterIndex,
            title = chapter.title,
            paragraphIndex = chapter.paragraphIndex,
            endParagraphExclusive = endExclusive,
            sentences = sentences
        )
    }
    return BookSequentialTargetSnapshot(snapshots)
}

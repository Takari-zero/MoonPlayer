package com.shenghui.localvibe.core.book

data class BookChapter(
    val title: String,
    val paragraphIndex: Int
)

object BookChapterDetector {
    private val whitespaceRegex = Regex("\\s+")
    private val chapterTitleRegex = Regex(
        pattern = """^\s*第[\s　]*[0-9零〇一二三四五六七八九十百千万两]+[\s　]*[章节回卷部集][\s　\S]*$"""
    )
    private val volumeTitleRegex = Regex(
        pattern = """^\s*卷[\s　]*[0-9零〇一二三四五六七八九十百千万两]+[\s　]+[\s　\S]+$"""
    )

    fun detect(paragraphs: List<String>): List<BookChapter> {
        return paragraphs.mapIndexedNotNull { index, paragraph ->
            val title = paragraph.replace(whitespaceRegex, " ").trim()
            if (isLikelyChapterTitle(title)) {
                BookChapter(title = title, paragraphIndex = index)
            } else {
                null
            }
        }
    }

    private fun isLikelyChapterTitle(title: String): Boolean {
        if (title.isBlank()) return false
        if (title.length > 40) return false
        return chapterTitleRegex.matches(title) || volumeTitleRegex.matches(title)
    }
}

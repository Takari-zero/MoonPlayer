package com.shenghui.localvibe.feature.book

internal data class BookCachedSentence(
    val text: String,
    val paragraphIndex: Int,
    val sentenceIndexInParagraph: Int,
    val chapterSentenceIndex: Int,
    val isCurrent: Boolean
)

internal data class BookReadStateCache(
    val bookUri: String,
    val contentVersion: PreviewContentVersion,
    val bookTitle: String,
    val lastParagraphIndex: Int,
    val lastSentenceIndexInParagraph: Int,
    val lastChapterSentenceIndex: Int,
    val lastReadingTargetName: String,
    val lastChapterTitle: String,
    val lastVisibleFirstItemIndex: Int,
    val lastVisibleFirstChapterSentenceIndex: Int,
    val lastVisibleFirstItemScrollOffset: Int,
    val cachedStartChapterSentenceIndex: Int,
    val cachedElapsedSeconds: Int,
    val cachedRemainingSeconds: Int,
    val cachedProgressFraction: Float,
    val cachedChapterEstimatedDurationSeconds: Int,
    val cachedSentences: List<BookCachedSentence>,
    val cachedVisibleText: List<String>,
    val updatedAt: Long
)

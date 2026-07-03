package com.shenghui.localvibe.feature.book.playback

data class BookReaderPlaybackTarget(
    val bookId: String,
    val bookTitle: String,
    val chapterIndex: Int,
    val chapterTitle: String,
    val paragraphIndex: Int,
    val sentenceIndex: Int,
    val clauseIndex: Int = 0,
    val sentenceText: String,
    val sentencePreview: String = sentenceText.replace('\n', ' ').trim().take(40),
)
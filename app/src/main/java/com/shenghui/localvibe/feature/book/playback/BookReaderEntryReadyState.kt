package com.shenghui.localvibe.feature.book.playback

data class BookReaderEntryReadyState(
    val bookId: String,
    val bookTitle: String,
    val paragraphs: List<String>,
    val chapterTitle: String,
    val chapterSentences: List<BookReaderEntrySentence>,
    val canonicalTarget: BookReaderEntryCanonicalTarget,
    val progressSnapshot: BookReaderEntryProgressSnapshot,
    val lazyListInitialIndex: Int,
    val lazyListInitialOffset: Int,
    val playbackSeed: BookReaderEntryPlaybackSeed,
) {
    init {
        require(bookId.isNotBlank()) { "bookId must not be blank" }
        require(paragraphs.isNotEmpty()) { "paragraphs must be ready before BookListenScreen entry" }
        require(chapterSentences.isNotEmpty()) { "chapterSentences must be ready before BookListenScreen entry" }
        require(canonicalTarget.chapterSentenceIndex in chapterSentences.indices) {
            "canonicalTarget.chapterSentenceIndex must point into chapterSentences"
        }
        require(lazyListInitialIndex in 0..chapterSentences.size) {
            "lazyListInitialIndex must point to the chapter title or a sentence item"
        }
        require(lazyListInitialOffset >= 0) { "lazyListInitialOffset must be non-negative" }
        require(progressSnapshot.chapterSentenceIndex == canonicalTarget.chapterSentenceIndex) {
            "progressSnapshot must use the canonical target"
        }
        require(playbackSeed.paragraphIndex == canonicalTarget.paragraphIndex &&
            playbackSeed.sentenceIndex == canonicalTarget.sentenceIndex) {
            "playbackSeed must use the canonical target"
        }
    }
}

data class BookReaderEntryCanonicalTarget(
    val paragraphIndex: Int,
    val sentenceIndex: Int,
    val chapterSentenceIndex: Int,
) {
    init {
        require(paragraphIndex >= 0) { "paragraphIndex must be non-negative" }
        require(sentenceIndex >= 0) { "sentenceIndex must be non-negative" }
        require(chapterSentenceIndex >= 0) { "chapterSentenceIndex must be non-negative" }
    }
}

data class BookReaderEntryProgressSnapshot(
    val chapterSentenceIndex: Int,
    val totalChapterSentenceCount: Int,
    val progressValue: Float,
    val progressMaxValue: Float,
    val listenedTimeLabel: String,
    val remainingTimeLabel: String,
) {
    init {
        require(chapterSentenceIndex >= 0) { "chapterSentenceIndex must be non-negative" }
        require(totalChapterSentenceCount > 0) { "totalChapterSentenceCount must be positive" }
        require(progressValue >= 0f) { "progressValue must be non-negative" }
        require(progressMaxValue > 0f) { "progressMaxValue must be positive" }
        require(progressValue <= progressMaxValue) { "progressValue must not exceed progressMaxValue" }
    }
}

data class BookReaderEntrySentence(
    val text: String,
    val paragraphIndex: Int,
    val sentenceIndex: Int,
    val chapterSentenceIndex: Int,
) {
    init {
        require(text.isNotBlank()) { "text must not be blank" }
        require(paragraphIndex >= 0) { "paragraphIndex must be non-negative" }
        require(sentenceIndex >= 0) { "sentenceIndex must be non-negative" }
        require(chapterSentenceIndex >= 0) { "chapterSentenceIndex must be non-negative" }
    }
}

data class BookReaderEntryPlaybackSeed(
    val bookId: String,
    val bookTitle: String,
    val chapterIndex: Int,
    val chapterTitle: String,
    val paragraphIndex: Int,
    val sentenceIndex: Int,
    val clauseIndex: Int = 0,
    val sentenceText: String,
    val sentencePreview: String = sentenceText.replace('\n', ' ').trim().take(40),
) {
    init {
        require(bookId.isNotBlank()) { "bookId must not be blank" }
        require(chapterIndex >= 0) { "chapterIndex must be non-negative" }
        require(paragraphIndex >= 0) { "paragraphIndex must be non-negative" }
        require(sentenceIndex >= 0) { "sentenceIndex must be non-negative" }
        require(clauseIndex >= 0) { "clauseIndex must be non-negative" }
        require(sentenceText.isNotBlank()) { "sentenceText must not be blank" }
    }
}

package com.shenghui.localvibe.feature.book.playback

import kotlin.math.roundToInt

object BookReaderEntryReadyStateFactory {
    data class Input(
        val bookId: String,
        val bookTitle: String,
        val paragraphs: List<String>,
        val savedParagraphIndex: Int,
        val savedSentenceIndex: Int,
        val chapterIndex: Int = 0,
        val chapterTitle: String = "正文",
        val chapterStartIndex: Int = 0,
        val chapterEndExclusive: Int = paragraphs.size,
        val speechRate: Float = 1f,
        val cachedViewport: CachedViewport? = null,
    )

    data class CachedViewport(
        val firstVisibleParagraphIndex: Int,
        val firstVisibleSentenceIndex: Int,
        val firstVisibleChapterSentenceIndex: Int,
        val firstVisibleItemScrollOffset: Int,
    )

    fun create(input: Input): BookReaderEntryReadyState {
        require(input.bookId.isNotBlank()) { "bookId must not be blank" }
        require(input.paragraphs.isNotEmpty()) { "paragraphs must be ready" }

        val start = input.chapterStartIndex.coerceIn(0, input.paragraphs.size)
        val end = input.chapterEndExclusive.coerceIn(start, input.paragraphs.size)
        val chapterSentences = buildEntrySentences(
            paragraphs = input.paragraphs,
            startIndex = start,
            endExclusive = end,
            chapterTitle = input.chapterTitle,
        )
        require(chapterSentences.isNotEmpty()) { "chapterSentences must be ready" }

        val safeParagraphIndex = input.savedParagraphIndex.coerceIn(start, (end - 1).coerceAtLeast(start))
        val requestedSentenceIndex = input.savedSentenceIndex.coerceAtLeast(0)
        val paragraphSentenceIndexes = chapterSentences
            .mapIndexedNotNull { index, sentence ->
                index.takeIf { sentence.paragraphIndex == safeParagraphIndex }
            }
        val canonicalIndex = chapterSentences.indexOfFirst { sentence ->
            sentence.paragraphIndex == safeParagraphIndex && sentence.sentenceIndex == requestedSentenceIndex
        }.takeIf { it >= 0 }
            ?: paragraphSentenceIndexes.getOrNull(requestedSentenceIndex)
            ?: paragraphSentenceIndexes.lastOrNull()
            ?: chapterSentences.lastIndex

        val canonicalSentence = chapterSentences[canonicalIndex]
        val canonicalTarget = BookReaderEntryCanonicalTarget(
            paragraphIndex = canonicalSentence.paragraphIndex,
            sentenceIndex = canonicalSentence.sentenceIndex,
            chapterSentenceIndex = canonicalSentence.chapterSentenceIndex,
        )
        val progressSnapshot = BookReaderEntryProgressSnapshot(
            chapterSentenceIndex = canonicalTarget.chapterSentenceIndex,
            totalChapterSentenceCount = chapterSentences.size,
            progressValue = canonicalTarget.chapterSentenceIndex.toFloat(),
            progressMaxValue = (chapterSentences.size - 1).coerceAtLeast(1).toFloat(),
            listenedTimeLabel = estimateSentenceTimeLabel(
                sentences = chapterSentences,
                fromIndex = 0,
                toIndex = canonicalTarget.chapterSentenceIndex,
                speechRate = input.speechRate,
            ),
            remainingTimeLabel = estimateSentenceTimeLabel(
                sentences = chapterSentences,
                fromIndex = canonicalTarget.chapterSentenceIndex,
                toIndex = chapterSentences.size,
                speechRate = input.speechRate,
            ),
        )
        val matchingCachedViewport = input.cachedViewport?.takeIf { viewport ->
            viewport.firstVisibleParagraphIndex == canonicalTarget.paragraphIndex &&
                viewport.firstVisibleSentenceIndex == canonicalTarget.sentenceIndex &&
                viewport.firstVisibleChapterSentenceIndex == canonicalTarget.chapterSentenceIndex
        }
        val lazyListInitialIndex = canonicalTarget.chapterSentenceIndex + 1
        val lazyListInitialOffset = matchingCachedViewport
            ?.firstVisibleItemScrollOffset
            ?.coerceAtLeast(0)
            ?: 0

        return BookReaderEntryReadyState(
            bookId = input.bookId,
            bookTitle = input.bookTitle,
            paragraphs = input.paragraphs,
            chapterTitle = input.chapterTitle,
            chapterSentences = chapterSentences,
            canonicalTarget = canonicalTarget,
            progressSnapshot = progressSnapshot,
            lazyListInitialIndex = lazyListInitialIndex,
            lazyListInitialOffset = lazyListInitialOffset,
            playbackSeed = BookReaderEntryPlaybackSeed(
                bookId = input.bookId,
                bookTitle = input.bookTitle,
                chapterIndex = input.chapterIndex.coerceAtLeast(0),
                chapterTitle = input.chapterTitle,
                paragraphIndex = canonicalTarget.paragraphIndex,
                sentenceIndex = canonicalTarget.sentenceIndex,
                clauseIndex = 0,
                sentenceText = canonicalSentence.text,
            ),
        )
    }

    private fun buildEntrySentences(
        paragraphs: List<String>,
        startIndex: Int,
        endExclusive: Int,
        chapterTitle: String,
    ): List<BookReaderEntrySentence> {
        val title = chapterTitle.trim()
        val result = mutableListOf<BookReaderEntrySentence>()
        for (paragraphIndex in startIndex until endExclusive) {
            val paragraph = paragraphs[paragraphIndex].trim()
            if (paragraph.isBlank()) continue
            if (paragraphIndex == startIndex && title.isNotBlank() && paragraph == title) continue
            splitParagraphIntoSentences(paragraph).forEachIndexed { sentenceIndex, sentence ->
                if (sentence.isNotBlank()) {
                    result += BookReaderEntrySentence(
                        text = sentence,
                        paragraphIndex = paragraphIndex,
                        sentenceIndex = sentenceIndex,
                        chapterSentenceIndex = result.size,
                    )
                }
            }
        }
        return result
    }

    private fun splitParagraphIntoSentences(paragraph: String): List<String> {
        val trimmed = paragraph.trim()
        if (trimmed.isBlank()) return emptyList()
        val result = mutableListOf<String>()
        val builder = StringBuilder()
        trimmed.forEach { char ->
            builder.append(char)
            if (char in sentenceBreakChars) {
                val sentence = builder.toString().trim()
                if (sentence.isNotBlank()) result += sentence
                builder.clear()
            }
        }
        val tail = builder.toString().trim()
        if (tail.isNotBlank()) result += tail
        return result
    }

    private fun estimateSentenceTimeLabel(
        sentences: List<BookReaderEntrySentence>,
        fromIndex: Int,
        toIndex: Int,
        speechRate: Float,
    ): String {
        if (sentences.isEmpty()) return "00:00"
        val start = fromIndex.coerceIn(0, sentences.size)
        val end = toIndex.coerceIn(start, sentences.size)
        val chars = sentences
            .subList(start, end)
            .sumOf { sentence -> sentence.text.count { !it.isWhitespace() } }
        val safeSpeechRate = if (speechRate > 0f) speechRate else 1f
        val effectiveRate = (300f * safeSpeechRate).coerceAtLeast(60f)
        val seconds = ((chars * 60f) / effectiveRate).roundToInt().coerceAtLeast(0)
        return formatDuration(seconds)
    }

    private fun formatDuration(totalSeconds: Int): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    private val sentenceBreakChars = setOf('，', '。', '！', '？', '；', '：', '、', ',', '.', '!', '?', ';', ':', '\n')
}

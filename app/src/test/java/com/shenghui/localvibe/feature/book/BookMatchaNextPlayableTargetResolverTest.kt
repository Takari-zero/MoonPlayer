package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookMatchaNextPlayableTargetResolverTest {
    private fun snapshot(): BookSequentialTargetSnapshot {
        return BookSequentialTargetSnapshot(
            chapters = listOf(
                BookSequentialChapterSnapshot(
                    chapterIndex = 0,
                    title = "第一章",
                    paragraphIndex = 0,
                    endParagraphExclusive = 3,
                    sentences = listOf(
                        BookSequentialSentenceSnapshot(0, 0, 0, "a"),
                        BookSequentialSentenceSnapshot(2, 0, 1, "b")
                    )
                ),
                BookSequentialChapterSnapshot(
                    chapterIndex = 1,
                    title = "第二章",
                    paragraphIndex = 3,
                    endParagraphExclusive = 5,
                    sentences = listOf(
                        BookSequentialSentenceSnapshot(4, 0, 0, "c")
                    )
                )
            )
        )
    }

    @Test
    fun snapshotResolverPreservesChapterLocalSequenceAndBoundary() {
        val target = snapshot()
        assertEquals(
            BookSequentialTarget(2, 0),
            BookSequentialNextTargetResolver.resolve(BookSequentialTarget(0, 0), target)
        )
        assertEquals(
            BookSequentialTarget(3, 0, isChapterTitle = true),
            BookSequentialNextTargetResolver.resolve(BookSequentialTarget(2, 0), target)
        )
        assertEquals(
            BookSequentialTarget(4, 0),
            BookSequentialNextTargetResolver.resolve(
                BookSequentialTarget(3, 0, isChapterTitle = true),
                target
            )
        )
    }

    @Test
    fun repeatedSnapshotResolveDoesNotRebuildInput() {
        val target = snapshot()
        val resolved = (0..20).map {
            BookSequentialNextTargetResolver.resolve(BookSequentialTarget(0, 0), target)
        }
        assertEquals(List(21) { BookSequentialTarget(2, 0) }, resolved)
    }

    @Test
    fun snapshotResolverSkipsUnplayableSentence() {
        val target = BookSequentialTargetSnapshot(
            chapters = listOf(
                BookSequentialChapterSnapshot(
                    chapterIndex = 0,
                    title = "",
                    paragraphIndex = 0,
                    endParagraphExclusive = 1,
                    sentences = listOf(
                        BookSequentialSentenceSnapshot(0, 0, 0, "first"),
                        BookSequentialSentenceSnapshot(0, 1, 1, "—"),
                        BookSequentialSentenceSnapshot(0, 2, 2, "third")
                    )
                )
            )
        )
        assertEquals(
            BookSequentialTarget(0, 2),
            BookSequentialNextTargetResolver.resolve(BookSequentialTarget(0, 0), target)
        )
    }

    @Test
    fun skipsUnplayableSentenceAndUsesNextPlayable() {
        val result = BookSequentialNextTargetResolver.resolve(
            current = BookSequentialTarget(10, 0),
            paragraphs = List(11) { "" },
            paragraphSentences = List(11) { index ->
                if (index == 10) listOf("91", "—", "93") else emptyList()
            },
            chapters = emptyList()
        )
        assertEquals(2, result?.sentenceIndexInParagraph)
    }

    @Test
    fun returnsNullAtEnd() {
        assertNull(
            BookSequentialNextTargetResolver.resolve(
                current = BookSequentialTarget(0, 0),
                paragraphs = listOf("text"),
                paragraphSentences = listOf(listOf("text")),
                chapters = emptyList()
            )
        )
    }

    @Test
    fun formalAndPrewarmResolveTheSameNonAdjacentTarget() {
        val paragraphs = listOf("91", "—", "93")
        val sentenceLists = paragraphs.map { listOf(it) }
        val formal = BookSequentialNextTargetResolver.resolve(
            current = BookSequentialTarget(0, 0),
            paragraphs = paragraphs,
            paragraphSentences = sentenceLists,
            chapters = emptyList()
        )
        val prewarm = BookSequentialNextTargetResolver.resolve(
            current = BookSequentialTarget(0, 0),
            paragraphs = paragraphs,
            paragraphSentences = sentenceLists,
            chapters = emptyList()
        )
        assertEquals(formal, prewarm)
        assertEquals(BookSequentialTarget(2, 0), formal)
    }

    @Test
    fun chapterTitleBoundaryMatchesFormalSemantics() {
        val paragraphs = listOf("body", "第二章 新章", "next")
        val chapters = listOf(
            com.shenghui.localvibe.core.book.BookChapter("第二章 新章", 1)
        )
        val result = BookSequentialNextTargetResolver.resolve(
            current = BookSequentialTarget(0, 0),
            paragraphs = paragraphs,
            paragraphSentences = paragraphs.map { listOf(it) },
            chapters = chapters
        )
        assertEquals(BookSequentialTarget(1, 0, isChapterTitle = true), result)
    }

    @Test
    fun matchaPrewarmPreservesChapterTitleTargetSemantics() {
        val result = resolveMatchaNextPrewarmTarget(
            currentTarget = BookPlaybackTargetId(
                chapterSentenceIndex = 1,
                paragraphIndex = 2,
                sentenceIndexInParagraph = 0,
                isChapterTitle = false
            ),
            snapshot = snapshot()
        )

        assertEquals(BookSequentialTarget(3, 0, isChapterTitle = true), result)
    }

    @Test
    fun matchaPrewarmResolvesFirstBodyAfterChapterTitle() {
        val result = resolveMatchaNextPrewarmTarget(
            currentTarget = BookPlaybackTargetId(
                chapterSentenceIndex = 0,
                paragraphIndex = 3,
                sentenceIndexInParagraph = 0,
                isChapterTitle = true
            ),
            snapshot = snapshot()
        )

        assertEquals(BookSequentialTarget(4, 0), result)
    }

    @Test
    fun matchaPrewarmStopsAtBookEndWithoutWrapAround() {
        val target = BookSequentialTargetSnapshot(
            chapters = listOf(
                BookSequentialChapterSnapshot(
                    chapterIndex = 0,
                    title = "第一章",
                    paragraphIndex = 0,
                    endParagraphExclusive = 1,
                    sentences = listOf(BookSequentialSentenceSnapshot(0, 0, 0, "last"))
                )
            )
        )

        assertNull(
            resolveMatchaNextPrewarmTarget(
                currentTarget = BookPlaybackTargetId(0, 0, 0),
                snapshot = target
            )
        )
    }
}

package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookPlaybackTargetResolverTest {
    @Test
    fun cachedTargetWinsOverGlobalDisplayIndex() {
        val resolved = BookPlaybackTargetResolver.resolveChapterSentenceIndex(
            paragraphIndex = 103788,
            sentenceIndexInParagraph = 0,
            cachedTargets = listOf(
                BookPlaybackSentenceTarget(103788, 0, 235)
            ),
            chapterTargets = listOf(
                BookPlaybackSentenceTarget(103788, 0, 135048)
            )
        )

        assertEquals(235, resolved)
    }

    @Test
    fun chapterTargetIsUsedWhenPrewarmCacheHasNoEntry() {
        val resolved = BookPlaybackTargetResolver.resolveChapterSentenceIndex(
            paragraphIndex = 120,
            sentenceIndexInParagraph = 2,
            cachedTargets = emptyList(),
            chapterTargets = listOf(
                BookPlaybackSentenceTarget(120, 2, 286)
            )
        )

        assertEquals(286, resolved)
    }

    @Test
    fun invalidParagraphAndSentenceAreRejected() {
        val resolved = BookPlaybackTargetResolver.resolveChapterSentenceIndex(
            paragraphIndex = 999,
            sentenceIndexInParagraph = 0,
            cachedTargets = listOf(BookPlaybackSentenceTarget(100, 0, 4)),
            chapterTargets = listOf(BookPlaybackSentenceTarget(100, 0, 4))
        )

        assertNull(resolved)
    }

    @Test
    fun chapterBoundaryDoesNotReuseAnotherParagraphTarget() {
        val resolved = BookPlaybackTargetResolver.resolveChapterSentenceIndex(
            paragraphIndex = 201,
            sentenceIndexInParagraph = 0,
            cachedTargets = listOf(BookPlaybackSentenceTarget(200, 0, 12)),
            chapterTargets = listOf(BookPlaybackSentenceTarget(202, 0, 0))
        )

        assertNull(resolved)
    }
}

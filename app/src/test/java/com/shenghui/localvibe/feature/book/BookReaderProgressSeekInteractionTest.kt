package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookReaderProgressSeekInteractionTest {
    @Test
    fun progressDragPreviewDoesNotCommitSeek() {
        val interaction = BookReaderProgressSeekInteraction()

        assertNull(interaction.preview(12))
        assertNull(interaction.preview(24))
        assertNull(interaction.preview(36))
    }

    @Test
    fun progressDragFinishedCommitsLastPreviewOnce() {
        val interaction = BookReaderProgressSeekInteraction()

        interaction.preview(12)
        interaction.preview(24)
        interaction.preview(36)

        assertEquals(36, interaction.finish())
        assertNull(interaction.finish())
    }

    @Test
    fun progressDragCanceledDoesNotCommitSeek() {
        val interaction = BookReaderProgressSeekInteraction()

        interaction.preview(12)
        interaction.preview(24)
        interaction.cancel()

        assertNull(interaction.finish())
    }

    @Test
    fun progressTapCommitsImmediately() {
        val interaction = BookReaderProgressSeekInteraction()

        assertEquals(42, interaction.tap(42))
    }

    @Test
    fun seekCommitSuppressesStaleViewportUntilTargetAligns() {
        val pending = BookReaderProgressSeekUiTarget.fromReaderSentenceFields(
            localIndex = 27,
            paragraphIndex = 743,
            sentenceIndexInParagraph = 1,
            chapterSentenceIndex = 27,
        )
        val staleViewport = BookReaderProgressSeekUiTarget.fromViewportFields(
            paragraphIndex = 758,
            sentenceIndexInParagraph = 1,
            chapterSentenceIndex = 77,
        )
        val alignedViewport = BookReaderProgressSeekUiTarget.fromViewportFields(
            paragraphIndex = 743,
            sentenceIndexInParagraph = 1,
            chapterSentenceIndex = 27,
        )

        assertTrue(BookReaderProgressSeekUiTarget.shouldSuppressViewport(pending, staleViewport))
        assertFalse(BookReaderProgressSeekUiTarget.shouldSuppressViewport(pending, alignedViewport))
    }

    @Test
    fun noPendingSeekDoesNotSuppressViewport() {
        val viewport = BookReaderProgressSeekUiTarget.fromViewportFields(
            paragraphIndex = 758,
            sentenceIndexInParagraph = 1,
            chapterSentenceIndex = 77,
        )

        assertFalse(BookReaderProgressSeekUiTarget.shouldSuppressViewport(null, viewport))
    }
}

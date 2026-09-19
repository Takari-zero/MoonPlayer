package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookContentReadinessTest {
    @Test
    fun previewTapStoresOnlyLatestIntent() {
        val first = PendingBookPlaybackTarget(
            10, 0, "A".hashCode(), PendingBookPlaybackSource.USER_SENTENCE_TAP, true
        )
        val latest = PendingBookPlaybackTarget(
            12, 2, "C".hashCode(), PendingBookPlaybackSource.USER_SENTENCE_TAP, true
        )

        val pending = PendingBookPlaybackTargetResolver.replace(
            PendingBookPlaybackTargetResolver.replace(null, first),
            latest
        )

        assertEquals(latest, pending)
    }

    @Test
    fun pendingTargetResolvesToFullChapterSentence() {
        val pending = PendingBookPlaybackTarget(
            103846, 0, "target".hashCode(), PendingBookPlaybackSource.USER_SENTENCE_TAP, true
        )

        val resolved = PendingBookPlaybackTargetResolver.resolve(
            pending,
            listOf(BookPlaybackSentenceTarget(103846, 0, 7))
        )

        assertEquals(7, resolved?.chapterSentenceIndex)
    }

    @Test
    fun pendingTargetIsRejectedWhenFullMappingDoesNotContainIt() {
        val pending = PendingBookPlaybackTarget(
            103846, 0, "target".hashCode(), PendingBookPlaybackSource.USER_SENTENCE_TAP, true
        )

        val resolved = PendingBookPlaybackTargetResolver.resolve(
            pending,
            listOf(BookPlaybackSentenceTarget(103847, 0, 8))
        )

        assertNull(resolved)
    }

    @Test
    fun userSentenceTapRequestsPlaybackEvenWhenNothingWasPlaying() {
        val pending = PendingBookPlaybackTarget(
            12, 2, "C".hashCode(), PendingBookPlaybackSource.USER_SENTENCE_TAP, true
        )

        assertEquals(true, pending.playRequested)
    }

    @Test
    fun pauseBeforeReadyCancelsPendingPlaybackIntent() {
        val pending = PendingBookPlaybackTarget(
            12, 2, "C".hashCode(), PendingBookPlaybackSource.USER_SENTENCE_TAP, true
        )

        val paused = PendingBookPlaybackTargetResolver.setPlayRequested(pending, false)

        assertEquals(false, paused.playRequested)
        assertEquals(pending.paragraphIndex, paused.paragraphIndex)
    }

    @Test
    fun playAfterPauseRestoresPendingPlaybackIntent() {
        val pending = PendingBookPlaybackTarget(
            12, 2, "C".hashCode(), PendingBookPlaybackSource.USER_SENTENCE_TAP, false
        )

        val resumed = PendingBookPlaybackTargetResolver.setPlayRequested(pending, true)

        assertEquals(true, resumed.playRequested)
        assertEquals(pending.paragraphIndex, resumed.paragraphIndex)
    }

    @Test
    fun restoreTargetDoesNotRequestPlayback() {
        val restored = PendingBookPlaybackTarget(
            12, 2, "C".hashCode(), PendingBookPlaybackSource.RESTORE, false
        )

        assertEquals(false, restored.playRequested)
    }
}

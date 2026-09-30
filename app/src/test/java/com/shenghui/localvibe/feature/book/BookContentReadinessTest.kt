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

    @Test
    fun previewPendingTargetResolvesFromPreparedContentWhenActiveListIsStale() {
        val pending = PendingBookPlaybackTarget(
            104210, 0, "target".hashCode(), PendingBookPlaybackSource.USER_SENTENCE_TAP, true
        )

        val result = PendingBookPlaybackTargetResolver.consume(
            pending = pending,
            readiness = BookContentReadiness.PLAYBACK_READY,
            preparedAvailable = true,
            preparedTargets = listOf(BookPlaybackSentenceTarget(104210, 0, 163)),
            currentPlaybackIntentPlaying = false
        )

        assertEquals(163, result.resolvedTarget?.chapterSentenceIndex)
        assertEquals(true, result.playbackIntentPlaying)
        assertEquals(false, result.terminalRejected)
    }

    @Test
    fun pendingTargetIsPreservedUntilPreparedContentIsReady() {
        val pending = PendingBookPlaybackTarget(
            104210, 0, "target".hashCode(), PendingBookPlaybackSource.USER_SENTENCE_TAP, true
        )

        val result = PendingBookPlaybackTargetResolver.consume(
            pending = pending,
            readiness = BookContentReadiness.PREVIEW,
            preparedAvailable = false,
            preparedTargets = emptyList(),
            currentPlaybackIntentPlaying = false
        )

        assertEquals(pending, result.pendingTarget)
        assertEquals(true, result.preparedNotReady)
        assertEquals(false, result.terminalRejected)
        assertEquals(false, result.playbackIntentPlaying)
    }

    @Test
    fun readyMissingPendingTargetClearsIntentForNextPlay() {
        val pending = PendingBookPlaybackTarget(
            104210, 0, "target".hashCode(), PendingBookPlaybackSource.USER_SENTENCE_TAP, true
        )

        val result = PendingBookPlaybackTargetResolver.consume(
            pending = pending,
            readiness = BookContentReadiness.PLAYBACK_READY,
            preparedAvailable = true,
            preparedTargets = emptyList(),
            currentPlaybackIntentPlaying = true
        )

        assertNull(result.pendingTarget)
        assertEquals(false, result.playbackIntentPlaying)
        assertEquals(true, result.terminalRejected)
    }

    @Test
    fun preparedPendingTargetRequestsExistingPlaybackDispatch() {
        val pending = PendingBookPlaybackTarget(
            12, 2, "target".hashCode(), PendingBookPlaybackSource.USER_SENTENCE_TAP, true
        )

        val result = PendingBookPlaybackTargetResolver.consume(
            pending = pending,
            readiness = BookContentReadiness.PLAYBACK_READY,
            preparedAvailable = true,
            preparedTargets = listOf(BookPlaybackSentenceTarget(12, 2, 7)),
            currentPlaybackIntentPlaying = false
        )

        assertEquals(BookPlaybackSentenceTarget(12, 2, 7), result.resolvedTarget)
        assertEquals(true, result.playbackIntentPlaying)
        assertEquals(false, result.preparedNotReady)
    }
}

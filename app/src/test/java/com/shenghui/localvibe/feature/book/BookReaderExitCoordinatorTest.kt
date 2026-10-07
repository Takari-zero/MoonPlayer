package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookReaderExitCoordinatorTest {
    @Test
    fun invalidationPrecedesNavigation() {
        val events = mutableListOf<String>()
        val coordinator = BookReaderExitCoordinator()

        if (coordinator.tryStartExit()) {
            events += "invalidate:back"
            events += "navigate"
        }

        assertEquals(listOf("invalidate:back", "navigate"), events)
        assertTrue(coordinator.hasExited)
    }

    @Test
    fun toolbarAndHardwareBackShareOneIdempotentExit() {
        val events = mutableListOf<String>()
        val coordinator = BookReaderExitCoordinator()

        if (coordinator.tryStartExit()) {
            events += "invalidate:back"
            events += "navigate"
        }
        if (coordinator.tryStartExit()) {
            events += "invalidate:hardware_back"
            events += "navigate"
        }

        assertEquals(listOf("invalidate:back", "navigate"), events)
    }

    @Test
    fun exitWithoutNavigationCallbackStillPreventsSecondInvalidation() {
        var invalidationCount = 0
        var navigationCount = 0
        val coordinator = BookReaderExitCoordinator()

        if (coordinator.tryStartExit()) {
            invalidationCount += 1
            navigationCount += 1
        }
        if (coordinator.tryStartExit()) {
            invalidationCount += 1
            navigationCount += 1
        }

        assertEquals(1, invalidationCount)
        assertEquals(1, navigationCount)
    }

    @Test
    fun exitInvalidatesLocalSessionBeforeNavigationAndRejectsOldDrain() {
        val events = mutableListOf<String>()
        val coordinator = BookReaderExitCoordinator()
        val adapter = PreviewLocalPlaybackProductionAdapter(logger = {})
        val target = LocalPlaybackTarget(
            bookUri = "content://book",
            paragraphIndex = 12,
            sentenceIndexInParagraph = 3,
            chapterSentenceIndex = 27,
            text = "cached sentence",
            stableTextHash = "cached sentence".hashCode(),
            generation = 7L,
            source = LocalPlaybackTargetSource.CACHED_PREVIEW
        )

        requireNotNull(
            adapter.begin(
                target = target,
                provider = BookPlaybackEngine.MATCHA_EXPERIMENTAL,
                speechRate = 1f,
                playbackSessionId = 41L
            )
        )
        adapter.onLocalStarted(readerGeneration = 7L, playbackSessionId = 41L)
        assertTrue(adapter.hasCurrentLocalOwnership(readerGeneration = 7L, playbackSessionId = 41L))

        if (coordinator.tryStartExit()) {
            adapter.reset()
            events += "invalidate:back"
            events += "navigate"
        }

        val oldDrain = adapter.onLocalDrained(readerGeneration = 7L, playbackSessionId = 41L)

        assertEquals(listOf("invalidate:back", "navigate"), events)
        assertTrue(oldDrain.stale)
        assertEquals(
            PreviewPlaybackContinuationDecision.IGNORE_STALE,
            oldDrain.continuationDecision
        )
        assertEquals(PreviewLocalPlaybackState.IDLE, adapter.snapshot().state)
        assertEquals(PreviewLocalPlaybackOwnership.NONE, adapter.snapshot().ownership)
        assertFalse(adapter.hasCurrentLocalOwnership(readerGeneration = 7L, playbackSessionId = 41L))
        assertFalse(adapter.claimFormalContinuation(readerGeneration = 7L, playbackSessionId = 41L))
    }
}

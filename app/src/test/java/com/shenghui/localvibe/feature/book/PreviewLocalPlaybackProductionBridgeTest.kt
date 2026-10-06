package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewLocalPlaybackProductionBridgeTest {
    @Test
    fun drainedBeforeFullReadyIsReevaluatedAndContinuesOnce() {
        val adapter = PreviewLocalPlaybackProductionAdapter(logger = {})
        val host = PreviewLocalPlaybackHost()
        val prepared = PreparedReaderContent(
            activeSentences = listOf(
                ReaderSentence("cached sentence", 12, 3, 27),
                ReaderSentence("formal next", 12, 4, 28)
            )
        )
        var callbacks: BookSentencePlaybackCallbacks? = null
        var adopted: PreviewPreparedCurrentAdoptionPlan? = null
        var continuationCount = 0
        host.currentReaderGeneration = { 7L }
        host.currentPlaybackSessionId = { 41L }
        host.playbackIntentPlaying = { true }
        host.preparedBookUri = { "content://book" }
        host.preparedContent = { prepared }
        host.dispatchPlayback = { _, dispatched -> callbacks = dispatched }
        host.adoptPreparedCurrent = { plan -> adopted = plan; true }
        host.onContinueFormalNext = { continuationCount += 1 }
        val bridge = PreviewLocalPlaybackProductionBridge(adapter, host)

        val dispatch = requireNotNull(
            adapter.begin(target(), BookPlaybackEngine.AISHELL3, 1f, 41L)
        )
        bridge.dispatch(dispatch)
        callbacks?.onDrained?.invoke(41L)

        assertEquals(0, continuationCount)
        assertTrue(bridge.handleFullReaderReady(prepared))
        assertEquals(PreviewPreparedCurrentAdoptionPlan(12, 3, 27), adopted)
        assertEquals(1, continuationCount)
        assertEquals(PreviewLocalPlaybackOwnership.NONE, adapter.snapshot().ownership)
    }

    @Test
    fun readyFirstThenDrainAdoptsPreparedCurrentAndContinuesOnce() {
        val adapter = PreviewLocalPlaybackProductionAdapter(logger = {})
        val host = PreviewLocalPlaybackHost()
        val prepared = PreparedReaderContent(
            activeSentences = listOf(
                ReaderSentence("cached sentence", 12, 3, 27),
                ReaderSentence("formal next", 12, 4, 28)
            )
        )
        var callbacks: BookSentencePlaybackCallbacks? = null
        var adopted: PreviewPreparedCurrentAdoptionPlan? = null
        var continuationCount = 0
        host.currentReaderGeneration = { 7L }
        host.currentPlaybackSessionId = { 41L }
        host.playbackIntentPlaying = { true }
        host.preparedBookUri = { "content://book" }
        host.preparedContent = { prepared }
        host.dispatchPlayback = { _, dispatched -> callbacks = dispatched }
        host.adoptPreparedCurrent = { plan -> adopted = plan; true }
        host.onContinueFormalNext = { continuationCount += 1 }
        val bridge = PreviewLocalPlaybackProductionBridge(adapter, host)

        val dispatch = requireNotNull(
            adapter.begin(target(), BookPlaybackEngine.MATCHA_EXPERIMENTAL, 1f, 41L)
        )
        bridge.dispatch(dispatch)
        callbacks?.onStarted?.invoke(41L)
        assertTrue(bridge.handleFullReaderReady(prepared))
        callbacks?.onDrained?.invoke(41L)
        callbacks?.onDrained?.invoke(41L)

        assertEquals(PreviewPreparedCurrentAdoptionPlan(12, 3, 27), adopted)
        assertEquals(1, continuationCount)
        assertEquals(PreviewLocalPlaybackOwnership.NONE, adapter.snapshot().ownership)
    }

    private fun target(): LocalPlaybackTarget = LocalPlaybackTarget(
        bookUri = "content://book",
        paragraphIndex = 12,
        sentenceIndexInParagraph = 3,
        chapterSentenceIndex = 27,
        text = "cached sentence",
        stableTextHash = "cached sentence".hashCode(),
        generation = 7L,
        source = LocalPlaybackTargetSource.CACHED_PREVIEW
    )
}

package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewLocalPlaybackPauseResumeBridgeTest {
    @Test
    fun pausedLocalDoesNotAdoptUntilResumeAndDrain() {
        val adapter = PreviewLocalPlaybackProductionAdapter(logger = {})
        val host = PreviewLocalPlaybackHost()
        val prepared = PreparedReaderContent(
            activeSentences = listOf(
                ReaderSentence("cached sentence", 12, 3, 27),
                ReaderSentence("formal next", 12, 4, 28)
            )
        )
        var callbacks: BookSentencePlaybackCallbacks? = null
        var hostIntent = true
        var adopted: PreviewPreparedCurrentAdoptionPlan? = null
        var continuationCount = 0
        host.currentReaderGeneration = { 7L }
        host.currentPlaybackSessionId = { 41L }
        host.playbackIntentPlaying = { hostIntent }
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
        hostIntent = false
        adapter.onLocalPaused(7L, 41L)

        assertTrue(bridge.handleFullReaderReady(prepared))
        assertEquals(null, adopted)
        assertEquals(0, continuationCount)

        hostIntent = true
        adapter.onLocalResumed(7L, 41L)
        callbacks?.onDrained?.invoke(41L)

        assertEquals(PreviewPreparedCurrentAdoptionPlan(12, 3, 27), adopted)
        assertEquals(1, continuationCount)
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

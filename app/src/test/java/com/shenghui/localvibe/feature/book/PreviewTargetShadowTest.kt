package com.shenghui.localvibe.feature.book

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewTargetShadowTest {
    private val text = "cached sentence"
    private val version = PreviewContentVersion.fingerprint(4L, "a".repeat(64))
    private val identity = PreviewContentIdentity("content://book/a", version)
    private val cycle = ReaderContentCycle(7L, identity.bookUri)
    private val sentence = PreviewCachedSentence(12, 3, 27, text, text.hashCode())

    @Test fun exactFingerprintAndSentenceAreAllowed() {
        assertTrue(resolution(evaluate()) is PreviewPlaybackResolution.Allowed)
    }

    @Test fun notReadyDoesNotRunResolverEvenWithInvalidCandidate() {
        assertEquals(PreviewTargetShadowResult.ProvenancePending,
            evaluate(current = null, sentence = sentence.copy(paragraphIndex = -1)))
    }

    @Test fun unavailableCachedVersionRejects() {
        assertRejected(PreviewPlaybackRejectionReason.CONTENT_VERSION_UNAVAILABLE,
            evaluate(cached = identity.copy(version = PreviewContentVersion.Unavailable)))
    }

    @Test fun differentFingerprintRejects() {
        assertRejected(PreviewPlaybackRejectionReason.CONTENT_FINGERPRINT_MISMATCH,
            evaluate(current = PreviewContentVersion.fingerprint(4L, "b".repeat(64))))
    }

    @Test fun differentSizeRejects() {
        assertRejected(PreviewPlaybackRejectionReason.CONTENT_SIZE_MISMATCH,
            evaluate(current = PreviewContentVersion.fingerprint(5L, "a".repeat(64))))
    }

    @Test fun differentUriRejects() {
        assertRejected(PreviewPlaybackRejectionReason.BOOK_URI_MISMATCH,
            evaluate(cached = identity.copy(bookUri = "content://book/b")))
    }

    @Test fun differentStableHashRejects() {
        assertRejected(PreviewPlaybackRejectionReason.TEXT_HASH_MISMATCH,
            evaluate(sentence = sentence.copy(stableTextHash = 0)))
    }

    @Test fun differentFullTextWithMatchingHashRejects() {
        assertRejected(PreviewPlaybackRejectionReason.TEXT_MISMATCH,
            evaluate(sentence = sentence.copy(text = "other")))
    }

    @Test fun allowedCoordinatesAndGenerationArePreserved() {
        val target = (resolution(evaluate()) as PreviewPlaybackResolution.Allowed).target
        assertEquals(sentence.paragraphIndex, target.paragraphIndex)
        assertEquals(sentence.sentenceIndexInParagraph, target.sentenceIndexInParagraph)
        assertEquals(sentence.chapterSentenceIndex, target.chapterSentenceIndex)
        assertEquals(cycle.generation, target.generation)
    }

    @Test fun allowedSourceIsCachedPreview() {
        val target = (resolution(evaluate()) as PreviewPlaybackResolution.Allowed).target
        assertEquals(LocalPlaybackTargetSource.CACHED_PREVIEW, target.source)
    }

    @Test fun playIntentIsNotBypassed() {
        assertRejected(PreviewPlaybackRejectionReason.PLAY_NOT_REQUESTED, evaluate(playRequested = false))
    }

    @Test fun invalidCoordinatesReject() {
        assertRejected(PreviewPlaybackRejectionReason.INVALID_COORDINATES,
            evaluate(sentence = sentence.copy(sentenceIndexInParagraph = -1)))
    }

    @Test fun readyClickIsSkippedBeforeVersionComparison() {
        assertEquals(PreviewTargetShadowResult.NotPreview,
            evaluate(readiness = BookContentReadiness.PLAYBACK_READY, current = null))
    }

    @Test fun missingEntrySentenceIsStaleNotFabricated() {
        assertEquals(PreviewTargetShadowResult.Stale, evaluate(sentence = null))
    }

    @Test fun oldCandidateRemainsStaleOnSameBookReload() {
        assertOldCandidateRejected(listOf(identity.bookUri, identity.bookUri))
    }

    @Test fun firstACandidateCannotReviveAfterABA() {
        assertOldCandidateRejected(listOf(identity.bookUri, "content://book/b", identity.bookUri))
    }

    @Test(timeout = 10_000) fun allowedObserverReturnsUnitAndLegacyContinuesOnce() {
        observeAndContinue(ready = true, rejected = false, expected = "result=ALLOWED")
    }

    @Test(timeout = 10_000) fun rejectedObserverDoesNotPreventLegacyContinuation() {
        observeAndContinue(ready = true, rejected = true, expected = "result=REJECTED")
    }

    @Test(timeout = 10_000) fun pendingObserverDoesNotStartReadOrPreventLegacyContinuation() {
        observeAndContinue(ready = false, rejected = false, expected = "result=PROVENANCE_PENDING")
    }

    private fun assertOldCandidateRejected(uris: List<String>) {
        val controller = PreviewLocalPlaybackController()
        controller.beginContentCycle(cycle, identity)
        val old = controller.hydrateCandidate(sentence, identity, true)
        uris.drop(1).forEachIndexed { index, uri ->
            controller.beginContentCycle(ReaderContentCycle(cycle.generation + index + 1, uri),
                PreviewContentIdentity(uri, version))
        }
        assertEquals(PreviewPlaybackResolution.Rejected(PreviewPlaybackRejectionReason.GENERATION_MISMATCH),
            controller.resolve(old, text, text.hashCode()))
    }

    private fun observeAndContinue(ready: Boolean, rejected: Boolean, expected: String) {
        var streamOpens = 0
        val scope = CoroutineScope(Dispatchers.Default)
        val coordinator = ReaderContentCycleCoordinator(scope,
            CurrentPreviewContentVersionLoader(scope, PreviewContentInputStreamOpener { streamOpens++; null }),
            shadowLogger = {}, cacheVersionShadowLogger = {})
        val metadata = PreviewContentVersion.metadata(4L, 123L)
        val active = coordinator.beginContentCycle(identity.bookUri, 4L, if (ready) 123L else null, metadata)
        try {
            if (ready) {
                coordinator.startCurrentVersionShadow(active)
                val deadline = System.nanoTime() + 5_000_000_000L
                while (coordinator.currentVersionSnapshot(active) == null) {
                    check(System.nanoTime() < deadline)
                    Thread.sleep(5)
                }
            }
            val cache = cache(if (rejected) PreviewContentVersion.Unavailable else metadata)
            val logs = mutableListOf<String>()
            var legacyCalls = 0
            val result: Unit = PreviewTargetShadow.observeClick(coordinator, cache,
                ReaderSentence(text, 12, 3, 27), BookContentReadiness.PREVIEW, true, logs::add)
            legacyCalls++
            assertEquals(Unit, result)
            assertEquals(1, legacyCalls)
            assertEquals(0, streamOpens)
            assertTrue(logs.single().contains(expected))
            assertFalse(logs.single().contains(text))
            assertFalse(logs.single().contains(identity.bookUri))
            assertTrue(logs.single().contains("paragraphIndex=12 sentenceIndexInParagraph=3 chapterSentenceIndex=27"))
        } finally {
            coordinator.release()
        }
    }

    private fun cache(version: PreviewContentVersion) = BookReadStateCache(
        bookUri = identity.bookUri, contentVersion = version, bookTitle = "test",
        lastParagraphIndex = 12, lastSentenceIndexInParagraph = 3, lastChapterSentenceIndex = 27,
        lastReadingTargetName = "SENTENCE", lastChapterTitle = "test",
        lastVisibleFirstItemIndex = 0, lastVisibleFirstChapterSentenceIndex = 27,
        lastVisibleFirstItemScrollOffset = 0, cachedStartChapterSentenceIndex = 27,
        cachedElapsedSeconds = 0, cachedRemainingSeconds = 1, cachedProgressFraction = 0f,
        cachedChapterEstimatedDurationSeconds = 1,
        cachedSentences = listOf(BookCachedSentence(text, 12, 3, 27, true)),
        cachedVisibleText = listOf(text), updatedAt = 1L
    )

    private fun evaluate(
        cached: PreviewContentIdentity = identity,
        sentence: PreviewCachedSentence? = this.sentence,
        current: PreviewContentVersion? = version,
        readiness: BookContentReadiness = BookContentReadiness.PREVIEW,
        playRequested: Boolean = true
    ) = PreviewTargetShadow.evaluate(cycle, cached, sentence, current, readiness, text, text.hashCode(), playRequested)

    private fun resolution(result: PreviewTargetShadowResult) = (result as PreviewTargetShadowResult.Resolved).resolution

    private fun assertRejected(reason: PreviewPlaybackRejectionReason, result: PreviewTargetShadowResult) {
        assertEquals(PreviewPlaybackResolution.Rejected(reason), resolution(result))
    }
}

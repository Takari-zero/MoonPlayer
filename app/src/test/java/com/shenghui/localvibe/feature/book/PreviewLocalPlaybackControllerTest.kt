package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewLocalPlaybackControllerTest {
    private val text = "cached sentence"
    private val textHash = text.hashCode()
    private val sentence = PreviewCachedSentence(
        paragraphIndex = 12,
        sentenceIndexInParagraph = 3,
        chapterSentenceIndex = 27,
        text = text,
        stableTextHash = textHash
    )
    private val identity = PreviewContentIdentity(
        bookUri = "content://books/current",
        version = PreviewContentVersion.metadata(4096L, 123456L)
    )
    private val fingerprintIdentity = identity.copy(
        version = PreviewContentVersion.fingerprint(4096L, "a".repeat(64))
    )

    @Test
    fun matchingCurrentCycleIsAllowed() {
        val controller = startedController()

        assertTrue(resolve(controller) is PreviewPlaybackResolution.Allowed)
    }

    @Test
    fun unavailableBeforeContentCycleBegins() {
        val controller = PreviewLocalPlaybackController()
        val hydration = controller.hydrateCandidate(sentence, identity, playRequested = true)

        assertEquals(PreviewContentCycleUnavailable, hydration)
        assertEquals(
            PreviewContentCycleUnavailable,
            controller.resolve(hydration, text, textHash)
        )
        assertEquals(null, controller.currentReaderGeneration)
    }

    @Test
    fun hydratedCandidateBecomesStaleAfterNewGeneration() {
        val controller = startedController()
        val hydration = hydrate(controller)

        controller.beginContentCycle(identity)

        assertRejected(
            PreviewPlaybackRejectionReason.GENERATION_MISMATCH,
            controller.resolve(hydration, text, textHash)
        )
    }

    @Test
    fun sameBookReloadAdvancesGenerationAndStalesOldCandidate() {
        val controller = PreviewLocalPlaybackController()
        val firstGeneration = controller.beginContentCycle(identity)
        val hydration = hydrate(controller)
        val secondGeneration = controller.beginContentCycle(identity)

        assertEquals(firstGeneration + 1L, secondGeneration)
        assertRejected(
            PreviewPlaybackRejectionReason.GENERATION_MISMATCH,
            controller.resolve(hydration, text, textHash)
        )
    }

    @Test
    fun firstBookACandidateCannotReviveAfterBookAToBToA() {
        val controller = PreviewLocalPlaybackController()
        val firstGeneration = controller.beginContentCycle(identity)
        val hydration = hydrate(controller)
        controller.beginContentCycle(identity.copy(bookUri = "content://books/other"))
        val thirdGeneration = controller.beginContentCycle(identity)

        assertEquals(firstGeneration + 2L, thirdGeneration)
        assertRejected(
            PreviewPlaybackRejectionReason.GENERATION_MISMATCH,
            controller.resolve(hydration, text, textHash)
        )
    }

    @Test
    fun unavailableCachedVersionIsRejectedByTargetResolver() {
        val controller = startedController()

        assertRejected(
            PreviewPlaybackRejectionReason.CONTENT_VERSION_UNAVAILABLE,
            resolve(controller, cachedIdentity = identity.copy(version = PreviewContentVersion.Unavailable))
        )
    }

    @Test
    fun currentFileSizeMismatchIsRejectedByTargetResolver() {
        val controller = startedController(
            identity.copy(version = PreviewContentVersion.metadata(4097L, 123456L))
        )

        assertRejected(
            PreviewPlaybackRejectionReason.CONTENT_SIZE_MISMATCH,
            resolve(controller)
        )
    }

    @Test
    fun currentModifiedAtMismatchIsRejectedByTargetResolver() {
        val controller = startedController(
            identity.copy(version = PreviewContentVersion.metadata(4096L, 123457L))
        )

        assertRejected(
            PreviewPlaybackRejectionReason.CONTENT_MODIFIED_AT_MISMATCH,
            resolve(controller)
        )
    }

    @Test
    fun currentBookUriMismatchIsRejectedByTargetResolver() {
        val controller = startedController(identity.copy(bookUri = "content://books/other"))

        assertRejected(
            PreviewPlaybackRejectionReason.BOOK_URI_MISMATCH,
            resolve(controller)
        )
    }

    @Test
    fun matchingFingerprintCandidateIsAllowed() {
        val controller = startedController(fingerprintIdentity)

        assertTrue(
            resolve(controller, cachedIdentity = fingerprintIdentity) is PreviewPlaybackResolution.Allowed
        )
    }

    @Test
    fun fingerprintMismatchIsRejectedByTargetResolver() {
        val controller = startedController(
            fingerprintIdentity.copy(
                version = PreviewContentVersion.fingerprint(4096L, "b".repeat(64))
            )
        )

        assertRejected(
            PreviewPlaybackRejectionReason.CONTENT_FINGERPRINT_MISMATCH,
            resolve(controller, cachedIdentity = fingerprintIdentity)
        )
    }

    @Test
    fun versionKindMismatchIsRejectedByTargetResolver() {
        val controller = startedController(fingerprintIdentity)

        assertRejected(
            PreviewPlaybackRejectionReason.CONTENT_VERSION_KIND_MISMATCH,
            resolve(controller, cachedIdentity = identity)
        )
    }

    @Test
    fun expectedHashMismatchIsRejectedByTargetResolver() {
        val controller = startedController()

        assertRejected(
            PreviewPlaybackRejectionReason.TEXT_HASH_MISMATCH,
            resolve(controller, expectedHash = textHash + 1)
        )
    }

    @Test
    fun expectedTextMismatchIsRejectedByTargetResolver() {
        val controller = startedController()

        assertRejected(
            PreviewPlaybackRejectionReason.TEXT_MISMATCH,
            resolve(controller, expectedText = "different sentence")
        )
    }

    @Test
    fun playNotRequestedIsRejectedByTargetResolver() {
        val controller = startedController()

        assertRejected(
            PreviewPlaybackRejectionReason.PLAY_NOT_REQUESTED,
            resolve(controller, playRequested = false)
        )
    }

    @Test
    fun invalidCoordinatesAreRejectedByTargetResolver() {
        val controller = startedController()

        assertRejected(
            PreviewPlaybackRejectionReason.INVALID_COORDINATES,
            resolve(controller, cachedSentence = sentence.copy(paragraphIndex = -1))
        )
    }

    @Test
    fun allowedTargetUsesCurrentControllerGeneration() {
        val controller = startedController()

        val result = resolve(controller) as PreviewPlaybackResolution.Allowed

        assertEquals(controller.currentReaderGeneration, result.target.generation)
    }

    @Test
    fun allowedTargetDoesNotCaptureProviderRateOrSnapshot() {
        val controller = startedController()
        val target = (resolve(controller) as PreviewPlaybackResolution.Allowed).target
        val fieldNames = target.javaClass.declaredFields.map { it.name }.toSet()

        assertTrue(fieldNames.intersect(setOf("provider", "speechRate", "rate", "snapshot")).isEmpty())
    }

    private fun startedController(
        currentIdentity: PreviewContentIdentity = identity
    ): PreviewLocalPlaybackController {
        return PreviewLocalPlaybackController().also {
            it.beginContentCycle(currentIdentity)
        }
    }

    private fun hydrate(
        controller: PreviewLocalPlaybackController,
        cachedSentence: PreviewCachedSentence = sentence,
        cachedIdentity: PreviewContentIdentity = identity,
        playRequested: Boolean = true
    ): PreviewCandidateHydration {
        return controller.hydrateCandidate(cachedSentence, cachedIdentity, playRequested)
    }

    private fun resolve(
        controller: PreviewLocalPlaybackController,
        cachedSentence: PreviewCachedSentence = sentence,
        cachedIdentity: PreviewContentIdentity = identity,
        playRequested: Boolean = true,
        expectedText: String = text,
        expectedHash: Int = textHash
    ): PreviewPlaybackResolution {
        return controller.resolve(
            hydration = hydrate(controller, cachedSentence, cachedIdentity, playRequested),
            expectedText = expectedText,
            expectedStableTextHash = expectedHash
        )
    }

    private fun assertRejected(
        expected: PreviewPlaybackRejectionReason,
        actual: PreviewPlaybackResolution
    ) {
        assertEquals(PreviewPlaybackResolution.Rejected(expected), actual)
    }
}

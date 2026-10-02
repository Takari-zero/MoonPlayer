package com.shenghui.localvibe.feature.book

data class PreviewCachedSentence(
    val paragraphIndex: Int,
    val sentenceIndexInParagraph: Int,
    val chapterSentenceIndex: Int,
    val text: String,
    val stableTextHash: Int
)

sealed interface PreviewCandidateHydration

data class HydratedPreviewPlaybackCandidate(
    val candidate: CachedPreviewPlaybackCandidate,
    val cachedIdentity: PreviewContentIdentity,
    val playRequested: Boolean
) : PreviewCandidateHydration

data object PreviewContentCycleUnavailable :
    PreviewCandidateHydration,
    PreviewPlaybackResolution

class PreviewLocalPlaybackController {
    private data class ContentCycle(
        val identity: PreviewContentIdentity,
        val generation: Long
    )

    private var currentCycle: ContentCycle? = null

    val currentReaderGeneration: Long?
        get() = currentCycle?.generation

    fun beginContentCycle(cycle: ReaderContentCycle, identity: PreviewContentIdentity) {
        require(cycle.bookUri == identity.bookUri) { "Reader cycle and identity must use the same book URI" }
        currentCycle = ContentCycle(identity, cycle.generation)
    }

    fun hydrateCandidate(
        sentence: PreviewCachedSentence,
        cachedIdentity: PreviewContentIdentity,
        playRequested: Boolean
    ): PreviewCandidateHydration {
        val cycle = currentCycle ?: return PreviewContentCycleUnavailable
        return HydratedPreviewPlaybackCandidate(
            candidate = CachedPreviewPlaybackCandidate(
                paragraphIndex = sentence.paragraphIndex,
                sentenceIndexInParagraph = sentence.sentenceIndexInParagraph,
                chapterSentenceIndex = sentence.chapterSentenceIndex,
                text = sentence.text,
                stableTextHash = sentence.stableTextHash,
                originReaderGeneration = cycle.generation
            ),
            cachedIdentity = cachedIdentity,
            playRequested = playRequested
        )
    }

    fun resolve(
        hydration: PreviewCandidateHydration,
        expectedText: String,
        expectedStableTextHash: Int
    ): PreviewPlaybackResolution {
        val cycle = currentCycle ?: return PreviewContentCycleUnavailable
        return when (hydration) {
            PreviewContentCycleUnavailable -> PreviewContentCycleUnavailable
            is HydratedPreviewPlaybackCandidate -> PreviewPlaybackTargetResolver.resolve(
                candidate = hydration.candidate,
                cachedIdentity = hydration.cachedIdentity,
                currentIdentity = cycle.identity,
                currentReaderGeneration = cycle.generation,
                expectedText = expectedText,
                expectedStableTextHash = expectedStableTextHash,
                playRequested = hydration.playRequested
            )
        }
    }
}

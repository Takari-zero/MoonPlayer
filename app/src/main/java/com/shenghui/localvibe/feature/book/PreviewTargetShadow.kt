package com.shenghui.localvibe.feature.book

import android.util.Log

internal sealed interface PreviewTargetShadowResult {
    data class Resolved(val resolution: PreviewPlaybackResolution) : PreviewTargetShadowResult
    data object ProvenancePending : PreviewTargetShadowResult
    data object NotPreview : PreviewTargetShadowResult
    data object Stale : PreviewTargetShadowResult
}

internal object PreviewTargetShadow {
    fun observeClick(
        coordinator: ReaderContentCycleCoordinator,
        entryCache: BookReadStateCache?,
        clicked: ReaderSentence,
        readiness: BookContentReadiness,
        playRequested: Boolean,
        logger: (String) -> Unit = { Log.i("PREVIEW_TARGET_SHADOW", it) }
    ): PreviewTargetShadowResult {
        val cycle = coordinator.currentCycleSnapshot()
        val currentVersion = cycle?.let(coordinator::currentVersionSnapshot)
        val cachedSentence = entryCache?.cachedSentences?.firstOrNull {
            it.paragraphIndex == clicked.paragraphIndex &&
                it.sentenceIndexInParagraph == clicked.sentenceIndexInParagraph &&
                it.chapterSentenceIndex == clicked.chapterSentenceIndex
        }?.let {
            PreviewCachedSentence(it.paragraphIndex, it.sentenceIndexInParagraph,
                it.chapterSentenceIndex, it.text, it.text.hashCode())
        }
        val cachedIdentity = entryCache?.let { PreviewContentIdentity(it.bookUri, it.contentVersion) }
        val evaluated = evaluate(cycle, cachedIdentity, cachedSentence, currentVersion, readiness,
            clicked.text, clicked.text.hashCode(), playRequested)
        val result = if (coordinator.currentCycleSnapshot() == cycle) evaluated else PreviewTargetShadowResult.Stale
        val outcome = when (result) {
            PreviewTargetShadowResult.NotPreview -> "result=SKIPPED_NOT_PREVIEW"
            PreviewTargetShadowResult.ProvenancePending -> "result=PROVENANCE_PENDING"
            PreviewTargetShadowResult.Stale -> "result=STALE"
            is PreviewTargetShadowResult.Resolved -> when (val resolution = result.resolution) {
                is PreviewPlaybackResolution.Allowed -> "result=ALLOWED"
                is PreviewPlaybackResolution.Rejected -> "result=REJECTED reason=${resolution.reason}"
                PreviewContentCycleUnavailable -> "result=STALE"
            }
        }
        logger("generation=${cycle?.generation ?: -1L} $outcome source=CACHED_PREVIEW " +
            "paragraphIndex=${clicked.paragraphIndex} sentenceIndexInParagraph=${clicked.sentenceIndexInParagraph} " +
            "chapterSentenceIndex=${clicked.chapterSentenceIndex} " +
            "cachedVersionKind=${cachedIdentity?.version.kind()} currentVersionKind=${currentVersion.kind()} " +
            "contentReadiness=$readiness")
        return result
    }

    // No await, retry, playback callbacks or pending mutations: the caller always continues its legacy path.
    fun evaluate(
        cycle: ReaderContentCycle?,
        cachedIdentity: PreviewContentIdentity?,
        cachedSentence: PreviewCachedSentence?,
        currentVersion: PreviewContentVersion?,
        readiness: BookContentReadiness,
        expectedText: String,
        expectedHash: Int,
        playRequested: Boolean
    ): PreviewTargetShadowResult {
        if (readiness != BookContentReadiness.PREVIEW) return PreviewTargetShadowResult.NotPreview
        if (cycle == null || cachedIdentity == null || cachedSentence == null) return PreviewTargetShadowResult.Stale
        if (currentVersion == null) return PreviewTargetShadowResult.ProvenancePending
        val controller = PreviewLocalPlaybackController()
        controller.beginContentCycle(cycle, PreviewContentIdentity(cycle.bookUri, currentVersion))
        val hydration = controller.hydrateCandidate(cachedSentence, cachedIdentity, playRequested)
        return PreviewTargetShadowResult.Resolved(controller.resolve(hydration, expectedText, expectedHash))
    }

    private fun PreviewContentVersion?.kind(): String = when (this) {
        is PreviewContentVersion.Fingerprint -> "FINGERPRINT"
        is PreviewContentVersion.Metadata -> "METADATA"
        PreviewContentVersion.Unavailable -> "UNAVAILABLE"
        null -> "NOT_READY"
    }
}

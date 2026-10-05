package com.shenghui.localvibe.feature.book

import android.util.Log

internal data class PreviewLocalPlaybackDispatch(
    val request: BookSentencePlaybackRequest,
    val transition: PreviewLocalSessionTransitionResult
)

/** Production bridge for one cached-preview sentence. It owns no Compose or provider state. */
internal class PreviewLocalPlaybackProductionAdapter(
    private val logger: (String) -> Unit = { Log.i("PREVIEW_LOCAL_PLAYBACK", it) }
) {
    private var controller = PreviewLocalPlaybackSessionController()
    private var lastDispatch: BookSentencePlaybackRequest? = null

    fun snapshot(): PreviewLocalSessionSnapshot = controller.snapshot()

    fun begin(
        target: LocalPlaybackTarget,
        provider: BookPlaybackEngine,
        speechRate: Float,
        playbackSessionId: Long,
        playbackIntentPlaying: Boolean = true
    ): PreviewLocalPlaybackDispatch? {
        val transition = controller.beginLocalOwnership(
            localTarget = target,
            readerGeneration = target.generation,
            playbackSessionId = playbackSessionId,
            playbackIntentPlaying = playbackIntentPlaying
        )
        if (transition.stale) {
            log("DISPATCH_REJECTED generation=${target.generation} session=$playbackSessionId " +
                "paragraph=${target.paragraphIndex} sentence=${target.sentenceIndexInParagraph}")
            return null
        }
        log("OWNERSHIP_ACQUIRED generation=${target.generation} session=$playbackSessionId " +
            "provider=${provider.name} paragraph=${target.paragraphIndex} " +
            "sentence=${target.sentenceIndexInParagraph} state=${transition.snapshot.state}")
        if (transition.ownershipDirective == PreviewLocalOwnershipDirective.SUPPRESS_SAME_TARGET_PENDING) {
            log("PENDING_SUPPRESSED generation=${target.generation} session=$playbackSessionId " +
                "paragraph=${target.paragraphIndex} sentence=${target.sentenceIndexInParagraph}")
        }
        return PreviewLocalPlaybackDispatch(
            request = BookSentencePlaybackRequest(
                target = BookPlaybackTargetId(
                    chapterSentenceIndex = target.chapterSentenceIndex,
                    paragraphIndex = target.paragraphIndex,
                    sentenceIndexInParagraph = target.sentenceIndexInParagraph
                ),
                text = target.text,
                provider = provider,
                speechRate = speechRate,
                playbackSessionId = playbackSessionId,
                preparedSnapshot = null,
                allowNextPrewarm = false,
                playbackIntentPlaying = playbackIntentPlaying,
                source = BookSentencePlaybackSource.CACHED_PREVIEW
            ),
            transition = transition
        ).also { lastDispatch = it.request }
    }

    fun currentRequest(): BookSentencePlaybackRequest? = lastDispatch

    fun onLocalStarted(
        readerGeneration: Long,
        playbackSessionId: Long
    ): PreviewLocalSessionTransitionResult = controller.onLocalStarted(readerGeneration, playbackSessionId)
        .also { logCallback("STARTED", readerGeneration, playbackSessionId, it) }

    fun onLocalStartFailed(
        readerGeneration: Long,
        playbackSessionId: Long
    ): PreviewLocalSessionTransitionResult = controller.onLocalStartFailed(readerGeneration, playbackSessionId)
        .also { logCallback("START_FAILED", readerGeneration, playbackSessionId, it) }

    fun onLocalDrained(
        readerGeneration: Long,
        playbackSessionId: Long
    ): PreviewLocalSessionTransitionResult = controller.onLocalDrained(readerGeneration, playbackSessionId)
        .also { logCallback("DRAINED", readerGeneration, playbackSessionId, it) }

    fun onFullReaderReady(
        readerGeneration: Long,
        playbackSessionId: Long,
        verification: PreparedTargetVerificationStatus
    ): PreviewLocalSessionTransitionResult = controller.onFullReaderReady(
        readerGeneration,
        playbackSessionId,
        verification
    ).also {
        log("FULL_READY_VERIFY_${if (verification == PreparedTargetVerificationStatus.MATCHED) "MATCH" else "REJECT"} " +
            "generation=$readerGeneration session=$playbackSessionId state=${it.snapshot.state}")
    }

    fun onFullLoadFailed(
        readerGeneration: Long,
        playbackSessionId: Long
    ): PreviewLocalSessionTransitionResult = controller.onFullLoadFailed(readerGeneration, playbackSessionId)

    fun onPlaybackIntentChanged(
        readerGeneration: Long,
        playbackSessionId: Long,
        playbackIntentPlaying: Boolean
    ): PreviewLocalSessionTransitionResult = controller.onPlaybackIntentChanged(
        readerGeneration,
        playbackSessionId,
        playbackIntentPlaying
    )

    fun reset() {
        controller.release()
        controller = PreviewLocalPlaybackSessionController()
        lastDispatch = null
    }

    private fun logCallback(
        event: String,
        generation: Long,
        session: Long,
        result: PreviewLocalSessionTransitionResult
    ) {
        log("$event generation=$generation session=$session state=${result.snapshot.state} " +
            "decision=${result.continuationDecision ?: "NONE"} stale=${result.stale}")
    }

    private fun log(message: String) {
        logger(message)
    }
}

package com.shenghui.localvibe.feature.book

/**
 * Production bridge for the PREVIEW LOCAL lifecycle. Provider routing remains
 * outside this class; the bridge only translates typed callbacks and phase
 * directives into host state callbacks.
 */
internal class PreviewLocalPlaybackProductionBridge(
    private val adapter: PreviewLocalPlaybackProductionAdapter,
    private val host: PreviewLocalPlaybackHost
) {
    fun dispatch(
        dispatch: PreviewLocalPlaybackDispatch
    ) {
        val request = dispatch.request
        val generation = adapter.snapshot().readerGeneration ?: return
        val callbacks = BookSentencePlaybackCallbacks(
            onStarted = { callbackSessionId ->
                if (host.screenDisposed()) return@BookSentencePlaybackCallbacks
                val transition = adapter.onLocalStarted(generation, callbackSessionId)
                if (transition.stale) return@BookSentencePlaybackCallbacks
                host.onStarted(request.provider, request.target.paragraphIndex)
            },
            onDrained = { callbackSessionId ->
                if (host.screenDisposed()) return@BookSentencePlaybackCallbacks
                val transition = adapter.onLocalDrained(generation, callbackSessionId)
                if (transition.stale) return@BookSentencePlaybackCallbacks
                host.onStopped()
                handleContinuationDecision(
                    generation = generation,
                    session = callbackSessionId,
                    transition = transition,
                    prepared = host.preparedContent()
                )
            },
            onFailed = { callbackSessionId, reason ->
                if (host.screenDisposed()) return@BookSentencePlaybackCallbacks
                val before = adapter.snapshot()
                val transition = if (before.state == PreviewLocalPlaybackState.LOCAL_STARTING) {
                    adapter.onLocalStartFailed(generation, callbackSessionId)
                } else {
                    null
                }
                if (transition?.stale == true) return@BookSentencePlaybackCallbacks
                host.onStopped()
                if (transition?.ownershipDirective == PreviewLocalOwnershipDirective.RESTORE_PREPARED_FALLBACK) {
                    if (host.fullReaderReady()) {
                        restorePreparedFallback(
                            target = transition.snapshot.target,
                            prepared = host.preparedContent(),
                            onRestore = host.onRestorePreparedFallback,
                            onUnavailable = host.onFallbackUnavailable
                        )
                    }
                } else {
                    host.onStopAutoContinue(reason)
                }
            }
        )
        host.dispatchPlayback(request, callbacks)
    }

    fun handleFullReaderReady(
        prepared: PreparedReaderContent
    ): Boolean {
        val localSnapshot = adapter.snapshot()
        val localStates = setOf(
            PreviewLocalPlaybackState.LOCAL_STARTING,
            PreviewLocalPlaybackState.LOCAL_ACTIVE,
            PreviewLocalPlaybackState.LOCAL_DRAINED_WAITING_PREPARED,
            PreviewLocalPlaybackState.PREPARED_VERIFIED_WAITING_DRAIN,
            PreviewLocalPlaybackState.PREPARED_FALLBACK_WAITING
        )
        if (localSnapshot.state !in localStates) return false
        val target = localSnapshot.target ?: return true
        val preparedTarget = PreviewToPreparedTargetVerifier.findPreparedTarget(
            localTarget = target,
            preparedTargets = prepared.activeSentences.map {
                PreparedPlaybackTargetIdentity(
                    paragraphIndex = it.paragraphIndex,
                    sentenceIndexInParagraph = it.sentenceIndexInParagraph,
                    chapterSentenceIndex = it.chapterSentenceIndex,
                    text = it.text,
                    stableTextHash = it.text.hashCode()
                )
            }
        )
        val currentReaderGeneration = host.currentReaderGeneration()
        val preparedBookUri = host.preparedBookUri()
        val verification = if (currentReaderGeneration != null && preparedBookUri != null) {
            when (PreviewToPreparedTargetVerifier.verify(
                localTarget = target,
                currentReaderGeneration = currentReaderGeneration,
                preparedBookUri = preparedBookUri,
                preparedTarget = preparedTarget
            )) {
                PreviewPreparedVerification.Matched -> PreparedTargetVerificationStatus.MATCHED
                is PreviewPreparedVerification.Rejected -> PreparedTargetVerificationStatus.REJECTED
            }
        } else {
            PreparedTargetVerificationStatus.REJECTED
        }
        val session = localSnapshot.playbackSessionId ?: return true
        val transition = adapter.onFullReaderReady(
            currentReaderGeneration ?: -1L,
            session,
            verification
        )
        if (transition.stale) return true
        handleContinuationDecision(
            generation = currentReaderGeneration ?: -1L,
            session = session,
            transition = transition,
            prepared = prepared
        )
        if (
            transition.ownershipDirective == PreviewLocalOwnershipDirective.RESTORE_PREPARED_FALLBACK &&
            verification == PreparedTargetVerificationStatus.MATCHED
        ) {
            restorePreparedFallback(
                target = target,
                prepared = prepared,
                onRestore = host.onRestorePreparedFallback,
                onUnavailable = host.onFallbackUnavailable
            )
        }
        return true
    }

    private fun handleContinuationDecision(
        generation: Long,
        session: Long,
        transition: PreviewLocalSessionTransitionResult,
        prepared: PreparedReaderContent
    ) {
        when (transition.continuationDecision) {
            PreviewPlaybackContinuationDecision.ALLOW_FORMAL_NEXT -> {
                continueFormalNextIfCurrent(generation, session, transition, prepared)
            }
            PreviewPlaybackContinuationDecision.STOP_AUTO_CONTINUE -> {
                host.onStopAutoContinue("preview_continuation_rejected")
            }
            else -> Unit
        }
    }

    private fun continueFormalNextIfCurrent(
        generation: Long,
        session: Long,
        transition: PreviewLocalSessionTransitionResult,
        prepared: PreparedReaderContent
    ) {
        val target = transition.snapshot.target ?: run {
            host.onStopAutoContinue("preview_adoption_target_missing")
            return
        }
        if (
            transition.snapshot.ownership != PreviewLocalPlaybackOwnership.LOCAL ||
            transition.snapshot.preparedVerification != PreparedTargetVerificationStatus.MATCHED ||
            !transition.snapshot.playbackIntentPlaying ||
            host.currentReaderGeneration() != generation ||
            host.currentPlaybackSessionId() != session ||
            !host.playbackIntentPlaying()
        ) {
            return
        }
        val preparedCurrent = prepared.activeSentences.firstOrNull {
            it.paragraphIndex == target.paragraphIndex &&
                it.sentenceIndexInParagraph == target.sentenceIndexInParagraph &&
                it.chapterSentenceIndex == target.chapterSentenceIndex &&
                it.text == target.text &&
                it.text.hashCode() == target.stableTextHash
        }
        val adoption = PreviewPreparedCurrentAdoption.plan(preparedCurrent) ?: run {
            host.onStopAutoContinue("preview_adoption_target_rejected")
            return
        }
        if (!adapter.claimFormalContinuation(generation, session)) return
        if (!host.adoptPreparedCurrent(adoption)) {
            host.onStopAutoContinue("preview_adoption_failed")
            return
        }
        adapter.release()
        host.onContinueFormalNext(session)
    }

    private fun restorePreparedFallback(
        target: LocalPlaybackTarget?,
        prepared: PreparedReaderContent,
        onRestore: (ReaderSentence, PreparedReaderContent) -> Unit,
        onUnavailable: () -> Unit
    ) {
        val preparedSentence = target?.let { localTarget ->
            prepared.activeSentences.firstOrNull {
                it.paragraphIndex == localTarget.paragraphIndex &&
                    it.sentenceIndexInParagraph == localTarget.sentenceIndexInParagraph &&
                    it.chapterSentenceIndex == localTarget.chapterSentenceIndex &&
                    it.text == localTarget.text &&
                    it.text.hashCode() == localTarget.stableTextHash
            }
        }
        if (preparedSentence == null) {
            onUnavailable()
            return
        }
        onRestore(preparedSentence, prepared)
    }
}

internal class PreviewLocalPlaybackHost {
    var screenDisposed: () -> Boolean = { false }
    var fullReaderReady: () -> Boolean = { false }
    var preparedContent: () -> PreparedReaderContent = { PreparedReaderContent() }
    var currentReaderGeneration: () -> Long? = { null }
    var preparedBookUri: () -> String? = { null }
    var currentPlaybackSessionId: () -> Long? = { null }
    var playbackIntentPlaying: () -> Boolean = { false }
    var dispatchPlayback: (BookSentencePlaybackRequest, BookSentencePlaybackCallbacks) -> Unit = { _, _ -> }
    var onStarted: (BookPlaybackEngine, Int) -> Unit = { _, _ -> }
    var onStopped: () -> Unit = {}
    var adoptPreparedCurrent: (PreviewPreparedCurrentAdoptionPlan) -> Boolean = { false }
    var onContinueFormalNext: (Long) -> Unit = {}
    var onRestorePreparedFallback: (ReaderSentence, PreparedReaderContent) -> Unit = { _, _ -> }
    var onFallbackUnavailable: () -> Unit = {}
    var onStopAutoContinue: (String) -> Unit = {}
}

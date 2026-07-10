package com.shenghui.localvibe.feature.book

import com.shenghui.localvibe.feature.book.playback.BookReaderEntryCanonicalTarget
import com.shenghui.localvibe.feature.book.playback.BookReaderEntryPlaybackSeed
import com.shenghui.localvibe.feature.book.playback.BookReaderEntryProgressSnapshot
import com.shenghui.localvibe.feature.book.playback.BookReaderEntryReadyState
import com.shenghui.localvibe.feature.book.playback.BookReaderEntrySentence

sealed interface BookReaderEntryReadyConsumeBoundary {
    val usesReadyState: Boolean
        get() = false

    fun readyStateOrNull(): BookReaderEntryReadyState? = null

    fun firstFrameOrNull(): BookReaderEntryReadyFirstFrame? = null

    fun screenPlan(): BookReaderEntryReadyScreenPlan = BookReaderEntryReadyScreenPlan.Legacy

    data object Legacy : BookReaderEntryReadyConsumeBoundary

    data class Ready(
        val readyState: BookReaderEntryReadyState,
        val firstFrame: BookReaderEntryReadyFirstFrame,
    ) : BookReaderEntryReadyConsumeBoundary {
        override val usesReadyState: Boolean = true

        override fun readyStateOrNull(): BookReaderEntryReadyState = readyState

        override fun firstFrameOrNull(): BookReaderEntryReadyFirstFrame = firstFrame

        override fun screenPlan(): BookReaderEntryReadyScreenPlan = BookReaderEntryReadyScreenPlan.Ready
    }

    companion object {
        fun from(readyState: BookReaderEntryReadyState?): BookReaderEntryReadyConsumeBoundary {
            return if (readyState == null) {
                Legacy
            } else {
                Ready(
                    readyState = readyState,
                    firstFrame = BookReaderEntryReadyFirstFrame.from(readyState),
                )
            }
        }
    }
}

sealed interface BookReaderEntryReadyScreenPlan {
    val skipLegacyRestoreLoad: Boolean
    val skipStableSnapshot: Boolean
    val skipRestorePrompt: Boolean
    val allowLegacyViewportCacheWrite: Boolean
    val allowInitialRestoreScroll: Boolean

    data object Legacy : BookReaderEntryReadyScreenPlan {
        override val skipLegacyRestoreLoad: Boolean = false
        override val skipStableSnapshot: Boolean = false
        override val skipRestorePrompt: Boolean = false
        override val allowLegacyViewportCacheWrite: Boolean = true
        override val allowInitialRestoreScroll: Boolean = true
    }

    data object Ready : BookReaderEntryReadyScreenPlan {
        override val skipLegacyRestoreLoad: Boolean = true
        override val skipStableSnapshot: Boolean = true
        override val skipRestorePrompt: Boolean = true
        override val allowLegacyViewportCacheWrite: Boolean = false
        override val allowInitialRestoreScroll: Boolean = false
    }
}

data class BookReaderEntryReadyFirstFrame(
    val paragraphs: List<String>,
    val chapterSentences: List<BookReaderEntrySentence>,
    val firstFrameTarget: BookReaderEntryCanonicalTarget,
    val progressSnapshot: BookReaderEntryProgressSnapshot,
    val lazyListInitialIndex: Int,
    val lazyListInitialOffset: Int,
    val playbackSeed: BookReaderEntryPlaybackSeed,
) {
    fun uiListPlan(
        readerListSize: Int,
        targetLocalIndex: Int,
        interactionState: BookReaderEntryReadyInteractionState? = null,
    ): BookReaderEntryReadyUiListPlan {
        val safeReaderListSize = readerListSize.coerceAtLeast(0)
        val safeLocalIndex = if (safeReaderListSize == 0) {
            0
        } else {
            targetLocalIndex.coerceIn(0, safeReaderListSize - 1)
        }
        return BookReaderEntryReadyUiListPlan(
            totalReadySentenceCount = chapterSentences.size,
            readerListSize = safeReaderListSize,
            targetGlobalIndex = firstFrameTarget.chapterSentenceIndex,
            targetLocalIndex = safeLocalIndex,
            exposesFullReadyList = safeReaderListSize == chapterSentences.size,
            firstFrameEqualsPlayTarget = firstFrameTarget.paragraphIndex == playbackSeed.paragraphIndex &&
                firstFrameTarget.sentenceIndex == playbackSeed.sentenceIndex &&
                firstFrameTarget.chapterSentenceIndex == progressSnapshot.chapterSentenceIndex,
            shouldApplyInitialSeed = interactionState?.shouldApplyInitialSeed ?: true,
            shouldLogRestoreScrollSkipped = interactionState?.shouldLogRestoreScrollSkipped ?: true,
        )
    }

    companion object {
        fun from(readyState: BookReaderEntryReadyState): BookReaderEntryReadyFirstFrame {
            return BookReaderEntryReadyFirstFrame(
                paragraphs = readyState.paragraphs,
                chapterSentences = readyState.chapterSentences,
                firstFrameTarget = readyState.canonicalTarget,
                progressSnapshot = readyState.progressSnapshot,
                lazyListInitialIndex = readyState.lazyListInitialIndex,
                lazyListInitialOffset = readyState.lazyListInitialOffset,
                playbackSeed = readyState.playbackSeed,
            )
        }
    }
}

data class BookReaderEntryReadyUiListPlan(
    val totalReadySentenceCount: Int,
    val readerListSize: Int,
    val targetGlobalIndex: Int,
    val targetLocalIndex: Int,
    val exposesFullReadyList: Boolean,
    val firstFrameEqualsPlayTarget: Boolean,
    val shouldApplyInitialSeed: Boolean,
    val shouldLogRestoreScrollSkipped: Boolean,
)

internal data class BookReaderReadyWindowMappingPlan(
    val windowStartInclusive: Int,
    val windowEndExclusive: Int,
    val targetIndexInWindow: Int,
    val windowSize: Int,
)

internal fun planBookReaderReadyWindowMapping(
    totalSentenceCount: Int,
    targetGlobalSentenceIndex: Int,
    maxWindowSize: Int = 133,
): BookReaderReadyWindowMappingPlan {
    val safeTotal: Int = totalSentenceCount.coerceAtLeast(0)
    if (safeTotal == 0) {
        return BookReaderReadyWindowMappingPlan(
            windowStartInclusive = 0,
            windowEndExclusive = 0,
            targetIndexInWindow = 0,
            windowSize = 0,
        )
    }

    val safeWindowSize: Int = maxWindowSize.coerceAtLeast(1).coerceAtMost(safeTotal)
    val safeTargetIndex: Int = targetGlobalSentenceIndex.coerceIn(0, safeTotal - 1)
    val sentencesBeforeTarget: Int = safeWindowSize / 2
    val preferredStart: Int = (safeTargetIndex - sentencesBeforeTarget).coerceAtLeast(0)
    val maxStart: Int = (safeTotal - safeWindowSize).coerceAtLeast(0)
    val start: Int = preferredStart.coerceAtMost(maxStart)
    val end: Int = (start + safeWindowSize).coerceAtMost(safeTotal)

    return BookReaderReadyWindowMappingPlan(
        windowStartInclusive = start,
        windowEndExclusive = end,
        targetIndexInWindow = safeTargetIndex - start,
        windowSize = end - start,
    )
}

class BookReaderEntryReadyInteractionState {
    var initialSeedConsumed: Boolean = false
        private set

    var manualViewportTakeover: Boolean = false
        private set

    var restoreScrollSkipLogged: Boolean = false
        private set

    val shouldApplyInitialSeed: Boolean
        get() = !initialSeedConsumed && !manualViewportTakeover

    val shouldLogRestoreScrollSkipped: Boolean
        get() = !restoreScrollSkipLogged && !manualViewportTakeover

    fun markInitialSeedConsumed() {
        initialSeedConsumed = true
    }

    fun markManualViewportTakeover() {
        manualViewportTakeover = true
    }

    fun markRestoreScrollSkipLogged() {
        restoreScrollSkipLogged = true
    }
}

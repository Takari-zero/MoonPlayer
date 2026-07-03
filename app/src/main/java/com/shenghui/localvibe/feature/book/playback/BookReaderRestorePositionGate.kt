package com.shenghui.localvibe.feature.book.playback

import kotlin.math.abs

object BookReaderRestorePositionGate {
    const val INITIAL_RENDER_STABLE_UI_SNAPSHOT = "stable_ui_snapshot"
    const val INITIAL_RENDER_SNAPSHOT_WINDOW = "snapshot_window"
    const val INITIAL_RENDER_SNAPSHOT = INITIAL_RENDER_STABLE_UI_SNAPSHOT
    const val INITIAL_RENDER_PLACEHOLDER = "placeholder"
    const val INITIAL_RENDER_REAL_CONTENT = "real_content"

    fun initialRenderMode(
        hasSavedPosition: Boolean,
        hasSnapshotContent: Boolean,
        isLoading: Boolean,
        hasLoadError: Boolean,
    ): String {
        return when {
            hasLoadError -> INITIAL_RENDER_REAL_CONTENT
            isLoading && hasSavedPosition && hasSnapshotContent -> INITIAL_RENDER_STABLE_UI_SNAPSHOT
            isLoading && hasSavedPosition -> INITIAL_RENDER_PLACEHOLDER
            else -> INITIAL_RENDER_REAL_CONTENT
        }
    }

    fun shouldHideContent(
        hasRestorePosition: Boolean,
        hasSettledPosition: Boolean,
        isLoading: Boolean,
        hasLoadError: Boolean,
        hasSnapshotContent: Boolean = false,
    ): Boolean {
        return hasRestorePosition && !hasSnapshotContent && !hasSettledPosition && !hasLoadError
    }

    fun restoreKey(
        bookId: String?,
        paragraphIndex: Int?,
        sentenceIndex: Int?,
    ): String {
        return listOf(
            bookId.orEmpty(),
            paragraphIndex?.coerceAtLeast(0)?.toString().orEmpty(),
            sentenceIndex?.coerceAtLeast(0)?.toString().orEmpty(),
        ).joinToString(separator = "#")
    }

    fun shouldResetForTarget(currentKey: String?, nextKey: String): Boolean {
        return currentKey != nextKey
    }

    fun shouldBlockRealContentForSnapshotHandoff(
        hasSnapshotContent: Boolean,
        hasCompletedSnapshotHandoff: Boolean,
        hasLoadError: Boolean,
    ): Boolean {
        return hasSnapshotContent && !hasCompletedSnapshotHandoff && !hasLoadError
    }

    data class SentenceIdentity(
        val paragraphIndex: Int,
        val sentenceIndex: Int,
        val chapterSentenceIndex: Int,
    )

    fun findRealContentIndexForSnapshotFirstVisible(
        realSentences: List<SentenceIdentity>,
        snapshotFirstVisibleParagraphIndex: Int?,
        snapshotFirstVisibleSentenceIndex: Int?,
        snapshotFirstVisibleChapterSentenceIndex: Int?,
        hasChapterTitle: Boolean,
    ): Int? {
        val sentenceIndex = realSentences.indexOfFirst { identity ->
            snapshotFirstVisibleParagraphIndex != null &&
                snapshotFirstVisibleSentenceIndex != null &&
                identity.paragraphIndex == snapshotFirstVisibleParagraphIndex &&
                identity.sentenceIndex == snapshotFirstVisibleSentenceIndex
        }.takeIf { it >= 0 } ?: realSentences.indexOfFirst { identity ->
            snapshotFirstVisibleChapterSentenceIndex != null &&
                identity.chapterSentenceIndex == snapshotFirstVisibleChapterSentenceIndex
        }.takeIf { it >= 0 }

        return sentenceIndex?.let { if (hasChapterTitle) it + 1 else it }
    }


    enum class ProgressHandoffMode {
        SNAPSHOT_ABSOLUTE,
        REAL_ABSOLUTE,
        HIDDEN,
    }

    data class ProgressHandoffState(
        val mode: ProgressHandoffMode,
        val chapterSentenceIndex: Int?,
        val totalChapterSentenceCount: Int?,
        val value: Float?,
        val maxValue: Float?,
    )

    fun resolveSnapshotProgress(
        chapterSentenceIndex: Int?,
        totalChapterSentenceCount: Int?,
    ): ProgressHandoffState {
        return resolveAbsoluteProgress(
            mode = ProgressHandoffMode.SNAPSHOT_ABSOLUTE,
            chapterSentenceIndex = chapterSentenceIndex,
            totalChapterSentenceCount = totalChapterSentenceCount,
        )
    }

    fun resolveRealProgress(
        chapterSentenceIndex: Int,
        totalChapterSentenceCount: Int,
    ): ProgressHandoffState {
        return resolveAbsoluteProgress(
            mode = ProgressHandoffMode.REAL_ABSOLUTE,
            chapterSentenceIndex = chapterSentenceIndex,
            totalChapterSentenceCount = totalChapterSentenceCount,
        )
    }

    fun progressDelta(snapshotValue: Float?, realValue: Float?): Float? {
        if (snapshotValue == null || realValue == null) return null
        return abs(snapshotValue - realValue)
    }


    enum class ProgressTimeHandoffMode {
        SAVED_STABLE,
        DERIVED_ABSOLUTE,
        REAL_ABSOLUTE,
        KEEP_SAVED,
        DEFAULT_INITIAL,
    }

    data class ProgressTimeHandoffState(
        val mode: ProgressTimeHandoffMode,
        val progress: ProgressHandoffState?,
        val listenedTimeLabel: String? = null,
        val remainingTimeLabel: String? = null,
        val shouldShowProgress: Boolean = true,
        val normalizedDelta: Float? = null,
    )

    fun resolveProgressTimeForSnapshotOrPendingHandoff(
        savedProgressValue: Float?,
        savedProgressMaxValue: Float?,
        savedListenedTimeLabel: String?,
        savedRemainingTimeLabel: String?,
        chapterSentenceIndex: Int?,
        totalChapterSentenceCount: Int?,
    ): ProgressTimeHandoffState {
        val canonicalProgress = resolveSnapshotProgress(
            chapterSentenceIndex = chapterSentenceIndex,
            totalChapterSentenceCount = totalChapterSentenceCount,
        )
        if (canonicalProgress.value != null && canonicalProgress.maxValue != null) {
            val labels = resolveCanonicalTimeLabels(
                progress = canonicalProgress,
                primaryListenedTimeLabel = null,
                primaryRemainingTimeLabel = null,
                fallbackListenedTimeLabel = savedListenedTimeLabel,
                fallbackRemainingTimeLabel = savedRemainingTimeLabel,
            )
            return ProgressTimeHandoffState(
                mode = ProgressTimeHandoffMode.DERIVED_ABSOLUTE,
                progress = canonicalProgress,
                listenedTimeLabel = labels?.first,
                remainingTimeLabel = labels?.second,
                normalizedDelta = normalizedProgressDelta(
                    resolveSavedProgress(
                        savedProgressValue = savedProgressValue,
                        savedProgressMaxValue = savedProgressMaxValue,
                        chapterSentenceIndex = chapterSentenceIndex,
                        totalChapterSentenceCount = totalChapterSentenceCount,
                    ),
                    canonicalProgress,
                ),
            )
        }

        return ProgressTimeHandoffState(
            mode = ProgressTimeHandoffMode.DEFAULT_INITIAL,
            progress = null,
            listenedTimeLabel = savedListenedTimeLabel?.takeIf { it.isNotBlank() },
            remainingTimeLabel = savedRemainingTimeLabel?.takeIf { it.isNotBlank() },
        )
    }

    fun resolveProgressTimeForRealContent(
        savedState: ProgressTimeHandoffState?,
        handoffDone: Boolean,
        chapterSentenceIndex: Int,
        totalChapterSentenceCount: Int,
        realListenedTimeLabel: String?,
        realRemainingTimeLabel: String?,
        progressDeltaThreshold: Float = 0.02f,
    ): ProgressTimeHandoffState {
        if (!handoffDone) {
            return savedState ?: resolveProgressTimeForSnapshotOrPendingHandoff(
                savedProgressValue = null,
                savedProgressMaxValue = null,
                savedListenedTimeLabel = null,
                savedRemainingTimeLabel = null,
                chapterSentenceIndex = chapterSentenceIndex,
                totalChapterSentenceCount = totalChapterSentenceCount,
            )
        }

        val realProgress = resolveRealProgress(
            chapterSentenceIndex = chapterSentenceIndex,
            totalChapterSentenceCount = totalChapterSentenceCount,
        )
        val delta = normalizedProgressDelta(savedState?.progress, realProgress)
        val labels = resolveCanonicalTimeLabels(
            progress = realProgress,
            primaryListenedTimeLabel = savedState?.listenedTimeLabel,
            primaryRemainingTimeLabel = savedState?.remainingTimeLabel,
            fallbackListenedTimeLabel = realListenedTimeLabel,
            fallbackRemainingTimeLabel = realRemainingTimeLabel,
        )

        return ProgressTimeHandoffState(
            mode = ProgressTimeHandoffMode.REAL_ABSOLUTE,
            progress = realProgress,
            listenedTimeLabel = labels?.first ?: realListenedTimeLabel?.takeIf { it.isNotBlank() },
            remainingTimeLabel = labels?.second ?: realRemainingTimeLabel?.takeIf { it.isNotBlank() },
            normalizedDelta = delta,
        )
    }

    private fun resolveCanonicalTimeLabels(
        progress: ProgressHandoffState,
        primaryListenedTimeLabel: String?,
        primaryRemainingTimeLabel: String?,
        fallbackListenedTimeLabel: String?,
        fallbackRemainingTimeLabel: String?,
    ): Pair<String, String>? {
        val totalSeconds = totalSecondsFromLabels(primaryListenedTimeLabel, primaryRemainingTimeLabel)
            ?: totalSecondsFromLabels(fallbackListenedTimeLabel, fallbackRemainingTimeLabel)
            ?: return null
        val value = progress.value ?: return null
        val maxValue = progress.maxValue?.takeIf { it > 0f } ?: return null
        val listenedSeconds = ((value / maxValue) * totalSeconds).toInt().coerceIn(0, totalSeconds)
        val remainingSeconds = (totalSeconds - listenedSeconds).coerceAtLeast(0)
        return formatDuration(listenedSeconds) to formatDuration(remainingSeconds)
    }

    private fun totalSecondsFromLabels(listenedTimeLabel: String?, remainingTimeLabel: String?): Int? {
        val listened = parseDurationSeconds(listenedTimeLabel) ?: return null
        val remaining = parseDurationSeconds(remainingTimeLabel) ?: return null
        return (listened + remaining).takeIf { it > 0 }
    }

    private fun parseDurationSeconds(value: String?): Int? {
        val parts = value
            ?.takeIf { it.isNotBlank() }
            ?.split(':')
            ?.mapNotNull { part -> part.toIntOrNull() }
            ?: return null
        return when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> null
        }
    }

    private fun formatDuration(totalSeconds: Int): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    fun normalizedProgressDelta(savedProgress: ProgressHandoffState?, realProgress: ProgressHandoffState?): Float? {
        val savedValue = savedProgress?.value ?: return null
        val savedMax = savedProgress.maxValue?.takeIf { it > 0f } ?: return null
        val realValue = realProgress?.value ?: return null
        val realMax = realProgress.maxValue?.takeIf { it > 0f } ?: return null
        return abs((savedValue / savedMax) - (realValue / realMax))
    }

    private fun resolveSavedProgress(
        savedProgressValue: Float?,
        savedProgressMaxValue: Float?,
        chapterSentenceIndex: Int?,
        totalChapterSentenceCount: Int?,
    ): ProgressHandoffState? {
        val value = savedProgressValue?.takeIf { it >= 0f } ?: return null
        val maxValue = savedProgressMaxValue?.takeIf { it > 0f } ?: return null
        val total = totalChapterSentenceCount?.takeIf { it > 0 }
        val safeValue = value.coerceIn(0f, maxValue)
        return ProgressHandoffState(
            mode = ProgressHandoffMode.SNAPSHOT_ABSOLUTE,
            chapterSentenceIndex = chapterSentenceIndex?.coerceAtLeast(0),
            totalChapterSentenceCount = total,
            value = safeValue,
            maxValue = maxValue,
        )
    }

    private fun resolveAbsoluteProgress(
        mode: ProgressHandoffMode,
        chapterSentenceIndex: Int?,
        totalChapterSentenceCount: Int?,
    ): ProgressHandoffState {
        val total = totalChapterSentenceCount?.takeIf { it > 0 }
        val index = chapterSentenceIndex?.takeIf { it >= 0 }
        if (total == null || index == null) {
            return ProgressHandoffState(
                mode = ProgressHandoffMode.HIDDEN,
                chapterSentenceIndex = chapterSentenceIndex,
                totalChapterSentenceCount = totalChapterSentenceCount,
                value = null,
                maxValue = null,
            )
        }
        val safeIndex = index.coerceAtMost(total - 1)
        return ProgressHandoffState(
            mode = mode,
            chapterSentenceIndex = safeIndex,
            totalChapterSentenceCount = total,
            value = safeIndex.toFloat(),
            maxValue = (total - 1).coerceAtLeast(1).toFloat(),
        )
    }

}

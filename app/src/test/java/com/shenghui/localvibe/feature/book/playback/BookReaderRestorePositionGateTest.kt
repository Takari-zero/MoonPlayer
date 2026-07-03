package com.shenghui.localvibe.feature.book.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookReaderRestorePositionGateTest {
    @Test
    fun hidesContentOnlyWhileRestorePositionIsPending() {
        assertTrue(
            BookReaderRestorePositionGate.shouldHideContent(
                hasRestorePosition = true,
                hasSettledPosition = false,
                isLoading = false,
                hasLoadError = false,
            )
        )

        assertFalse(
            BookReaderRestorePositionGate.shouldHideContent(
                hasRestorePosition = true,
                hasSettledPosition = true,
                isLoading = false,
                hasLoadError = false,
            )
        )
    }

    @Test
    fun keepsContentHiddenWhileContentLoadsUntilScrollSettles() {
        assertTrue(
            BookReaderRestorePositionGate.shouldHideContent(
                hasRestorePosition = true,
                hasSettledPosition = false,
                isLoading = true,
                hasLoadError = false,
            )
        )
    }

    @Test
    fun snapshotContentIsVisibleImmediatelyWhileFullContentLoads() {
        assertFalse(
            BookReaderRestorePositionGate.shouldHideContent(
                hasRestorePosition = true,
                hasSettledPosition = false,
                isLoading = true,
                hasLoadError = false,
                hasSnapshotContent = true,
            )
        )
    }

    @Test
    fun doesNotHideContentWithoutRestorePositionOrWhenLoadFailed() {
        assertFalse(
            BookReaderRestorePositionGate.shouldHideContent(
                hasRestorePosition = false,
                hasSettledPosition = false,
                isLoading = false,
                hasLoadError = false,
            )
        )
        assertFalse(
            BookReaderRestorePositionGate.shouldHideContent(
                hasRestorePosition = true,
                hasSettledPosition = false,
                isLoading = false,
                hasLoadError = true,
            )
        )
    }


    @Test
    fun usesStableUiSnapshotRenderModeWhenSavedPositionHasViewportSnapshotWhileLoading() {
        assertEquals(
            BookReaderRestorePositionGate.INITIAL_RENDER_STABLE_UI_SNAPSHOT,
            BookReaderRestorePositionGate.initialRenderMode(
                hasSavedPosition = true,
                hasSnapshotContent = true,
                isLoading = true,
                hasLoadError = false,
            )
        )
    }

    @Test
    fun usesPlaceholderRenderModeWhenSavedPositionHasNoSnapshotWhileLoading() {
        assertEquals(
            BookReaderRestorePositionGate.INITIAL_RENDER_PLACEHOLDER,
            BookReaderRestorePositionGate.initialRenderMode(
                hasSavedPosition = true,
                hasSnapshotContent = false,
                isLoading = true,
                hasLoadError = false,
            )
        )
    }

    @Test
    fun usesRealContentRenderModeWithoutSavedPositionOrAfterLoadError() {
        assertEquals(
            BookReaderRestorePositionGate.INITIAL_RENDER_REAL_CONTENT,
            BookReaderRestorePositionGate.initialRenderMode(
                hasSavedPosition = false,
                hasSnapshotContent = false,
                isLoading = true,
                hasLoadError = false,
            )
        )
        assertEquals(
            BookReaderRestorePositionGate.INITIAL_RENDER_REAL_CONTENT,
            BookReaderRestorePositionGate.initialRenderMode(
                hasSavedPosition = true,
                hasSnapshotContent = false,
                isLoading = true,
                hasLoadError = true,
            )
        )
    }

    @Test
    fun restoreKeyResetsWhenBookOrTargetChanges() {
        val first = BookReaderRestorePositionGate.restoreKey(
            bookId = "book-a",
            paragraphIndex = 10,
            sentenceIndex = 2,
        )
        val same = BookReaderRestorePositionGate.restoreKey(
            bookId = "book-a",
            paragraphIndex = 10,
            sentenceIndex = 2,
        )
        val nextSentence = BookReaderRestorePositionGate.restoreKey(
            bookId = "book-a",
            paragraphIndex = 10,
            sentenceIndex = 3,
        )
        val nextBook = BookReaderRestorePositionGate.restoreKey(
            bookId = "book-b",
            paragraphIndex = 10,
            sentenceIndex = 2,
        )

        assertFalse(BookReaderRestorePositionGate.shouldResetForTarget(first, same))
        assertTrue(BookReaderRestorePositionGate.shouldResetForTarget(first, nextSentence))
        assertTrue(BookReaderRestorePositionGate.shouldResetForTarget(first, nextBook))
    }

    @Test
    fun mapsSnapshotFirstVisibleIdentityToRealContentIndex() {
        val realSentences = listOf(
            BookReaderRestorePositionGate.SentenceIdentity(paragraphIndex = 7, sentenceIndex = 0, chapterSentenceIndex = 4),
            BookReaderRestorePositionGate.SentenceIdentity(paragraphIndex = 8, sentenceIndex = 0, chapterSentenceIndex = 5),
            BookReaderRestorePositionGate.SentenceIdentity(paragraphIndex = 9, sentenceIndex = 0, chapterSentenceIndex = 6),
        )

        val realIndex = BookReaderRestorePositionGate.findRealContentIndexForSnapshotFirstVisible(
            realSentences = realSentences,
            snapshotFirstVisibleParagraphIndex = 8,
            snapshotFirstVisibleSentenceIndex = 0,
            snapshotFirstVisibleChapterSentenceIndex = 5,
            hasChapterTitle = true,
        )

        assertEquals(2, realIndex)
    }

    @Test
    fun missingSnapshotFirstVisibleIdentityDoesNotFallBackToTop() {
        val realSentences = listOf(
            BookReaderRestorePositionGate.SentenceIdentity(paragraphIndex = 7, sentenceIndex = 0, chapterSentenceIndex = 4),
            BookReaderRestorePositionGate.SentenceIdentity(paragraphIndex = 8, sentenceIndex = 0, chapterSentenceIndex = 5),
        )

        val realIndex = BookReaderRestorePositionGate.findRealContentIndexForSnapshotFirstVisible(
            realSentences = realSentences,
            snapshotFirstVisibleParagraphIndex = 99,
            snapshotFirstVisibleSentenceIndex = 0,
            snapshotFirstVisibleChapterSentenceIndex = 99,
            hasChapterTitle = true,
        )

        assertEquals(null, realIndex)
    }

    @Test
    fun blocksRealContentUntilSnapshotHandoffIsComplete() {
        assertTrue(
            BookReaderRestorePositionGate.shouldBlockRealContentForSnapshotHandoff(
                hasSnapshotContent = true,
                hasCompletedSnapshotHandoff = false,
                hasLoadError = false,
            )
        )

        assertFalse(
            BookReaderRestorePositionGate.shouldBlockRealContentForSnapshotHandoff(
                hasSnapshotContent = true,
                hasCompletedSnapshotHandoff = true,
                hasLoadError = false,
            )
        )
    }


    @Test
    fun snapshotProgressUsesAbsoluteChapterCoordinatesWhenTotalCountExists() {
        val state = BookReaderRestorePositionGate.resolveSnapshotProgress(
            chapterSentenceIndex = 42,
            totalChapterSentenceCount = 100,
        )

        assertEquals(BookReaderRestorePositionGate.ProgressHandoffMode.SNAPSHOT_ABSOLUTE, state.mode)
        assertEquals(42, state.chapterSentenceIndex)
        assertEquals(100, state.totalChapterSentenceCount)
        assertEquals(42f, state.value ?: -1f, 0.0001f)
        assertEquals(99f, state.maxValue ?: -1f, 0.0001f)
    }

    @Test
    fun snapshotProgressWithoutTotalCountIsHiddenInsteadOfRelativeWindowProgress() {
        val state = BookReaderRestorePositionGate.resolveSnapshotProgress(
            chapterSentenceIndex = 42,
            totalChapterSentenceCount = 0,
        )

        assertEquals(BookReaderRestorePositionGate.ProgressHandoffMode.HIDDEN, state.mode)
        assertNull(state.value)
        assertNull(state.maxValue)
    }

    @Test
    fun realProgressUsesAbsoluteChapterCoordinates() {
        val state = BookReaderRestorePositionGate.resolveRealProgress(
            chapterSentenceIndex = 42,
            totalChapterSentenceCount = 100,
        )

        assertEquals(BookReaderRestorePositionGate.ProgressHandoffMode.REAL_ABSOLUTE, state.mode)
        assertEquals(42f, state.value ?: -1f, 0.0001f)
        assertEquals(99f, state.maxValue ?: -1f, 0.0001f)
    }

    @Test
    fun snapshotAndRealProgressDeltaCanStayWithinTolerance() {
        val snapshot = BookReaderRestorePositionGate.resolveSnapshotProgress(
            chapterSentenceIndex = 42,
            totalChapterSentenceCount = 100,
        )
        val real = BookReaderRestorePositionGate.resolveRealProgress(
            chapterSentenceIndex = 42,
            totalChapterSentenceCount = 100,
        )

        assertEquals(0f, BookReaderRestorePositionGate.progressDelta(snapshot.value, real.value) ?: -1f, 0.0001f)
    }



    @Test
    fun snapshotHandoffUsesCanonicalPositionEvenWhenSavedProgressDiffers() {
        val state = BookReaderRestorePositionGate.resolveProgressTimeForSnapshotOrPendingHandoff(
            savedProgressValue = 20f,
            savedProgressMaxValue = 99f,
            savedListenedTimeLabel = "01:12",
            savedRemainingTimeLabel = "02:34",
            chapterSentenceIndex = 42,
            totalChapterSentenceCount = 100,
        )

        assertEquals(BookReaderRestorePositionGate.ProgressTimeHandoffMode.DERIVED_ABSOLUTE, state.mode)
        assertTrue(state.shouldShowProgress)
        assertEquals(42f, state.progress?.value ?: -1f, 0.0001f)
        assertEquals(99f, state.progress?.maxValue ?: -1f, 0.0001f)
        assertEquals("01:35", state.listenedTimeLabel)
        assertEquals("02:11", state.remainingTimeLabel)
        assertTrue((state.normalizedDelta ?: 0f) > 0.02f)
    }

    @Test
    fun snapshotHandoffDerivesAbsoluteProgressWhenSavedProgressMissing() {
        val state = BookReaderRestorePositionGate.resolveProgressTimeForSnapshotOrPendingHandoff(
            savedProgressValue = null,
            savedProgressMaxValue = null,
            savedListenedTimeLabel = null,
            savedRemainingTimeLabel = null,
            chapterSentenceIndex = 42,
            totalChapterSentenceCount = 100,
        )

        assertEquals(BookReaderRestorePositionGate.ProgressTimeHandoffMode.DERIVED_ABSOLUTE, state.mode)
        assertTrue(state.shouldShowProgress)
        assertEquals(42f, state.progress?.value ?: -1f, 0.0001f)
        assertEquals(99f, state.progress?.maxValue ?: -1f, 0.0001f)
    }

    @Test
    fun snapshotHandoffFallsBackToDefaultOnlyWithoutSavedPosition() {
        val state = BookReaderRestorePositionGate.resolveProgressTimeForSnapshotOrPendingHandoff(
            savedProgressValue = null,
            savedProgressMaxValue = null,
            savedListenedTimeLabel = null,
            savedRemainingTimeLabel = null,
            chapterSentenceIndex = null,
            totalChapterSentenceCount = null,
        )

        assertEquals(BookReaderRestorePositionGate.ProgressTimeHandoffMode.DEFAULT_INITIAL, state.mode)
        assertTrue(state.shouldShowProgress)
        assertNull(state.progress)
    }

    @Test
    fun pendingRealContentKeepsCanonicalSnapshotProgressTime() {
        val saved = BookReaderRestorePositionGate.resolveProgressTimeForSnapshotOrPendingHandoff(
            savedProgressValue = 20f,
            savedProgressMaxValue = 99f,
            savedListenedTimeLabel = "01:12",
            savedRemainingTimeLabel = "02:34",
            chapterSentenceIndex = 42,
            totalChapterSentenceCount = 100,
        )
        val state = BookReaderRestorePositionGate.resolveProgressTimeForRealContent(
            savedState = saved,
            handoffDone = false,
            chapterSentenceIndex = 0,
            totalChapterSentenceCount = 100,
            realListenedTimeLabel = "00:00",
            realRemainingTimeLabel = "03:46",
        )

        assertEquals(BookReaderRestorePositionGate.ProgressTimeHandoffMode.DERIVED_ABSOLUTE, state.mode)
        assertEquals(42f, state.progress?.value ?: -1f, 0.0001f)
        assertEquals("01:35", state.listenedTimeLabel)
    }

    @Test
    fun sameCanonicalPositionGivesSameSnapshotAndRealProgressTime() {
        val saved = BookReaderRestorePositionGate.resolveProgressTimeForSnapshotOrPendingHandoff(
            savedProgressValue = 20f,
            savedProgressMaxValue = 99f,
            savedListenedTimeLabel = "01:12",
            savedRemainingTimeLabel = "02:34",
            chapterSentenceIndex = 42,
            totalChapterSentenceCount = 100,
        )
        val state = BookReaderRestorePositionGate.resolveProgressTimeForRealContent(
            savedState = saved,
            handoffDone = true,
            chapterSentenceIndex = 42,
            totalChapterSentenceCount = 100,
            realListenedTimeLabel = "01:13",
            realRemainingTimeLabel = "02:33",
        )

        assertEquals(BookReaderRestorePositionGate.ProgressTimeHandoffMode.REAL_ABSOLUTE, state.mode)
        assertEquals(saved.progress?.value ?: -1f, state.progress?.value ?: -2f, 0.0001f)
        assertEquals(saved.listenedTimeLabel, state.listenedTimeLabel)
        assertEquals(saved.remainingTimeLabel, state.remainingTimeLabel)
    }

    @Test
    fun differentCanonicalPositionUsesRealPositionInsteadOfKeepingSavedLabel() {
        val saved = BookReaderRestorePositionGate.resolveProgressTimeForSnapshotOrPendingHandoff(
            savedProgressValue = 42f,
            savedProgressMaxValue = 99f,
            savedListenedTimeLabel = "01:12",
            savedRemainingTimeLabel = "02:34",
            chapterSentenceIndex = 42,
            totalChapterSentenceCount = 100,
        )
        val state = BookReaderRestorePositionGate.resolveProgressTimeForRealContent(
            savedState = saved,
            handoffDone = true,
            chapterSentenceIndex = 4,
            totalChapterSentenceCount = 100,
            realListenedTimeLabel = "00:07",
            realRemainingTimeLabel = "03:39",
        )

        assertEquals(BookReaderRestorePositionGate.ProgressTimeHandoffMode.REAL_ABSOLUTE, state.mode)
        assertEquals(4f, state.progress?.value ?: -1f, 0.0001f)
        assertEquals("00:09", state.listenedTimeLabel)
        assertEquals("03:37", state.remainingTimeLabel)
        assertTrue((state.normalizedDelta ?: 0f) > 0.02f)
    }

}

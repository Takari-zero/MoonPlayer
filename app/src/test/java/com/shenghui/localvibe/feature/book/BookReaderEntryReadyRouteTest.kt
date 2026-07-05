package com.shenghui.localvibe.feature.book

import com.shenghui.localvibe.core.scanner.LocalMediaFile
import com.shenghui.localvibe.core.scanner.LocalMediaType
import com.shenghui.localvibe.feature.book.playback.BookReaderEntryCanonicalTarget
import com.shenghui.localvibe.feature.book.playback.BookReaderEntryLoadRequest
import com.shenghui.localvibe.feature.book.playback.BookReaderEntryPlaybackSeed
import com.shenghui.localvibe.feature.book.playback.BookReaderEntryProgressSnapshot
import com.shenghui.localvibe.feature.book.playback.BookReaderEntryReadyState
import com.shenghui.localvibe.feature.book.playback.BookReaderEntrySentence
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BookReaderEntryReadyRouteTest {
    @Test
    fun formalPackageCanEnableReadyPathWhenExplicitFlagIsTrue() {
        assertTrue(
            BookReaderEntryReadyPackageGate.isFormalPackage(
                packageName = "com.shenghui.localvibe.fastspeech2formal",
            )
        )
        assertTrue(
            BookReaderEntryReadyPackageGate.shouldEnable(
                packageName = "com.shenghui.localvibe.fastspeech2formal",
                explicitFlag = true,
            )
        )
    }

    @Test
    fun mainPackageCannotEnableReadyPathEvenWhenExplicitFlagIsTrue() {
        assertFalse(
            BookReaderEntryReadyPackageGate.isFormalPackage(
                packageName = "com.shenghui.localvibe",
            )
        )
        assertFalse(
            BookReaderEntryReadyPackageGate.shouldEnable(
                packageName = "com.shenghui.localvibe",
                explicitFlag = true,
            )
        )
    }

    @Test
    fun formalPackageStaysDisabledWhenExplicitFlagIsFalse() {
        assertFalse(
            BookReaderEntryReadyPackageGate.shouldEnable(
                packageName = "com.shenghui.localvibe.fastspeech2formal",
                explicitFlag = false,
            )
        )
    }

    @Test
    fun routeInputBuildsLoadRequestFromBookFileWithoutLoadingParagraphs() {
        val input = BookReaderEntryReadyRouteInput(
            bookFile = bookFile(),
            initialParagraphIndex = 12,
            speechRate = 1.25f,
        )

        val request = input.toLoadRequest()

        assertEquals("file://demo.txt", request?.bookId)
        assertEquals("Demo Book", request?.bookTitle)
        assertEquals(12, request?.chapterStartIndex)
        assertEquals(1.25f, request?.speechRate)
    }

    @Test
    fun readyStateIsTheOnlyStateThatReachesReaderContentSlot() {
        val readyState = readyState()
        val decision = BookReaderEntryReadyRouteHost.resolveSlot(
            BookReaderEntryReadyRouteState.Ready(
                input = input(),
                readyState = readyState,
            )
        )

        assertTrue(decision is BookReaderEntryReadyRouteSlot.ReadyContent)
        assertSame(readyState, decision.readyStateOrNull())
        assertFalse(decision.exposesStableSnapshotAsReaderContent)
    }

    @Test
    fun preparingStateDoesNotGenerateFakeReaderContent() {
        val decision = BookReaderEntryReadyRouteHost.resolveSlot(
            BookReaderEntryReadyRouteState.Preparing(input())
        )

        assertTrue(decision is BookReaderEntryReadyRouteSlot.Preparing)
        assertNull(decision.readyStateOrNull())
        assertFalse(decision.exposesStableSnapshotAsReaderContent)
    }

    @Test
    fun failedStateDoesNotGenerateFakeReaderContent() {
        val decision = BookReaderEntryReadyRouteHost.resolveSlot(
            BookReaderEntryReadyRouteState.Failed(
                input = input(),
                message = "paragraphs unavailable",
            )
        )

        assertTrue(decision is BookReaderEntryReadyRouteSlot.Failed)
        assertNull(decision.readyStateOrNull())
        assertFalse(decision.exposesStableSnapshotAsReaderContent)
    }

    @Test
    fun disabledRouteShadowDoesNotCreateLoadRequest() {
        val plan = BookReaderEntryReadyRouteShadow.plan(
            enabled = false,
            input = input(),
        )

        assertTrue(plan is BookReaderEntryReadyRouteShadowPlan.Disabled)
        assertNull(plan.loadRequestOrNull())
        assertFalse(plan.shouldRunLoader)
    }

    @Test
    fun enabledRouteShadowCreatesReadOnlyLoadRequest() {
        val plan = BookReaderEntryReadyRouteShadow.plan(
            enabled = true,
            input = BookReaderEntryReadyRouteInput(
                bookFile = bookFile(),
                initialParagraphIndex = 7,
                speechRate = 1.5f,
            ),
        )

        assertTrue(plan is BookReaderEntryReadyRouteShadowPlan.Enabled)
        assertTrue(plan.shouldRunLoader)
        assertEquals("file://demo.txt", plan.loadRequestOrNull()?.bookId)
        assertEquals(7, plan.loadRequestOrNull()?.chapterStartIndex)
        assertEquals(1.5f, plan.loadRequestOrNull()?.speechRate)
    }

    @Test
    fun disabledReadyPathDoesNotPassReadyStateToBookListenScreen() {
        val plan = BookReaderEntryReadyPath.plan(
            enabled = false,
            routeState = BookReaderEntryReadyRouteState.Ready(
                input = input(),
                readyState = readyState(),
            ),
        )

        assertTrue(plan is BookReaderEntryReadyPathPlan.Legacy)
        assertFalse(plan.usesReadyState)
        assertNull(plan.entryReadyStateForScreen())
    }

    @Test
    fun disabledRouteLoaderDoesNotLoadReadyState() = runBlocking {
        var loadCalls = 0

        val state = BookReaderEntryReadyRouteLoader.loadIfEnabled(
            enabled = false,
            input = input(),
            loader = BookReaderEntryReadyRouteReadyStateLoader {
                loadCalls++
                Result.success(readyState())
            },
        )

        assertNull(state)
        assertEquals(0, loadCalls)
    }

    @Test
    fun enabledRouteLoaderBuildsReadyStateFromInjectedLoader() = runBlocking {
        val readyState = readyState()

        val state = BookReaderEntryReadyRouteLoader.loadIfEnabled(
            enabled = true,
            input = input(),
            loader = BookReaderEntryReadyRouteReadyStateLoader { request ->
                assertEquals("file://demo.txt", request.bookId)
                assertEquals(0, request.chapterStartIndex)
                Result.success(readyState)
            },
        )

        assertTrue(state is BookReaderEntryReadyRouteState.Ready)
        val loaded = (state as BookReaderEntryReadyRouteState.Ready).readyState
        assertSame(readyState, loaded)
        assertEquals(loaded.canonicalTarget.paragraphIndex, loaded.playbackSeed.paragraphIndex)
        assertEquals(loaded.canonicalTarget.sentenceIndex, loaded.playbackSeed.sentenceIndex)
        assertEquals(loaded.canonicalTarget.chapterSentenceIndex, loaded.progressSnapshot.chapterSentenceIndex)
    }

    @Test
    fun routeLoaderFailureFallsBackToFailedStateWithoutReadyPath() = runBlocking {
        val state = BookReaderEntryReadyRouteLoader.loadIfEnabled(
            enabled = true,
            input = input(),
            loader = BookReaderEntryReadyRouteReadyStateLoader {
                Result.failure(IllegalStateException("paragraphs unavailable"))
            },
        )

        assertTrue(state is BookReaderEntryReadyRouteState.Failed)
        val plan = BookReaderEntryReadyPath.plan(
            enabled = true,
            routeState = state,
        )
        assertFalse(plan.usesReadyState)
        assertNull(plan.entryReadyStateForScreen())
    }

    private fun input(): BookReaderEntryReadyRouteInput {
        return BookReaderEntryReadyRouteInput(
            bookFile = bookFile(),
            initialParagraphIndex = 0,
        )
    }

    private fun bookFile(): LocalMediaFile {
        return LocalMediaFile(
            id = "book-1",
            name = "Demo Book",
            uri = "file://demo.txt",
            type = LocalMediaType.BOOK,
            extension = "txt",
            size = 128L,
            parentFolderName = "Books",
        )
    }

    private fun readyState(): BookReaderEntryReadyState {
        val sentence = BookReaderEntrySentence(
            text = "第一句。",
            paragraphIndex = 0,
            sentenceIndex = 0,
            chapterSentenceIndex = 0,
        )
        return BookReaderEntryReadyState(
            bookId = "file://demo.txt",
            bookTitle = "Demo Book",
            paragraphs = listOf("第一句。"),
            chapterTitle = "正文",
            chapterSentences = listOf(sentence),
            canonicalTarget = BookReaderEntryCanonicalTarget(
                paragraphIndex = 0,
                sentenceIndex = 0,
                chapterSentenceIndex = 0,
            ),
            progressSnapshot = BookReaderEntryProgressSnapshot(
                chapterSentenceIndex = 0,
                totalChapterSentenceCount = 1,
                progressValue = 0f,
                progressMaxValue = 1f,
                listenedTimeLabel = "00:00",
                remainingTimeLabel = "00:01",
            ),
            lazyListInitialIndex = 1,
            lazyListInitialOffset = 0,
            playbackSeed = BookReaderEntryPlaybackSeed(
                bookId = "file://demo.txt",
                bookTitle = "Demo Book",
                chapterIndex = 0,
                chapterTitle = "正文",
                paragraphIndex = 0,
                sentenceIndex = 0,
                sentenceText = "第一句。",
            ),
        )
    }
}

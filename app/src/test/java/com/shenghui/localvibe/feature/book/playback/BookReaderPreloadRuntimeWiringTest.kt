package com.shenghui.localvibe.feature.book.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookReaderPreloadRuntimeWiringTest {
    @Test
    fun formalPackageCanEnterObservePathWhenExplicitFlagIsFalse() {
        val decision = BookReaderPreloadRuntimeWiring.observeDecision(
            packageName = "com.shenghui.localvibe.fastspeech2formal",
            explicitPreloadFlag = false,
        )

        assertTrue(decision.formalPackage)
        assertTrue(decision.observeEnabled)
        assertFalse(decision.preloadEnabled)
        assertFalse(decision.preloadStarted)
        assertFalse(decision.txtReadTriggered)
    }

    @Test
    fun mainPackageKeepsObservePathDisabled() {
        val decision = BookReaderPreloadRuntimeWiring.observeDecision(
            packageName = "com.shenghui.localvibe",
            explicitPreloadFlag = false,
        )

        assertFalse(decision.formalPackage)
        assertFalse(decision.observeEnabled)
        assertFalse(decision.preloadEnabled)
        assertFalse(decision.preloadStarted)
        assertFalse(decision.txtReadTriggered)
    }

    @Test
    fun mainPackageCannotEnableBookshelfPreloadEvenWhenFlagIsTrue() {
        val decision = BookReaderPreloadRuntimeWiring.observeDecision(
            packageName = "com.shenghui.localvibe",
            explicitPreloadFlag = true,
        )

        assertFalse(decision.formalPackage)
        assertFalse(decision.observeEnabled)
        assertFalse(decision.preloadEnabled)
        assertFalse(decision.preloadStarted)
        assertFalse(decision.txtReadTriggered)
    }

    @Test
    fun formalPackageCanEnableBookshelfPreloadOnlyWhenFlagIsTrue() {
        val decision = BookReaderPreloadRuntimeWiring.observeDecision(
            packageName = "com.shenghui.localvibe.fastspeech2formal",
            explicitPreloadFlag = true,
        )

        assertTrue(decision.formalPackage)
        assertTrue(decision.observeEnabled)
        assertTrue(decision.preloadEnabled)
        assertFalse(decision.preloadStarted)
        assertFalse(decision.txtReadTriggered)
    }

    @Test
    fun disabledWiringDoesNotCreateRuntimeFactoryOrStartPreload() {
        var factoryCreations = 0
        var sourceCreations = 0
        var readCalls = 0

        val plan = BookReaderPreloadRuntimeWiring.plan(
            enabled = false,
            runtimeFactory = {
                factoryCreations++
                BookReaderPreloadRuntimeFactory(
                    paragraphSourceFactory = {
                        sourceCreations++
                        BookParagraphSource { request ->
                            readCalls++
                            BookParagraphLoadResult.success(
                                key = request.key,
                                paragraphs = listOf("Chapter 1", "First sentence."),
                            )
                        }
                    },
                    clock = IncrementingBookDocumentClock(start = 100L),
                )
            },
        )

        assertTrue(plan is BookReaderPreloadRuntimeWiringPlan.Disabled)
        assertEquals(0, factoryCreations)
        assertEquals(0, sourceCreations)
        assertEquals(0, readCalls)
        assertFalse(plan.preloadStarted)
        assertTrue(plan.clickPlan() is BookReaderBookshelfClickPlan.LegacyOpen)
    }

    @Test
    fun disabledWiringKeepsLegacyBookClickPlanEvenWhenReadyStateCouldExist() {
        val plan = BookReaderPreloadRuntimeWiring.plan(
            enabled = false,
            runtimeFactory = {
                BookReaderPreloadRuntimeFactory(
                    paragraphSourceFactory = {
                        BookParagraphSource { request ->
                            BookParagraphLoadResult.success(
                                key = request.key,
                                paragraphs = listOf("Chapter 1", "First sentence."),
                            )
                        }
                    },
                )
            },
        )

        val clickPlan = plan.clickPlan(readyStateAvailable = true)

        assertTrue(clickPlan is BookReaderBookshelfClickPlan.LegacyOpen)
    }

    @Test
    fun enabledWiringCreatesRuntimeOnlyWhenExplicitlyRequested() {
        var factoryCreations = 0
        var sourceCreations = 0
        var readCalls = 0
        val plan = BookReaderPreloadRuntimeWiring.plan(
            enabled = true,
            runtimeFactory = {
                factoryCreations++
                BookReaderPreloadRuntimeFactory(
                    paragraphSourceFactory = {
                        sourceCreations++
                        BookParagraphSource { request ->
                            readCalls++
                            BookParagraphLoadResult.success(
                                key = request.key,
                                paragraphs = listOf("Chapter 1", "First sentence."),
                            )
                        }
                    },
                    clock = IncrementingBookDocumentClock(start = 100L),
                )
            },
        )

        assertTrue(plan is BookReaderPreloadRuntimeWiringPlan.Enabled)
        assertEquals(0, factoryCreations)
        assertEquals(0, sourceCreations)
        assertEquals(0, readCalls)

        val runtime = (plan as BookReaderPreloadRuntimeWiringPlan.Enabled).createRuntime()

        assertEquals(1, factoryCreations)
        assertEquals(1, sourceCreations)
        assertEquals(0, readCalls)
        assertFalse(runtime.readyStateStore.get(request().key).isReady)
    }

    @Test
    fun enabledWiringCanPlanBookshelfPreparingOrReadyOpenWithoutTriggeringRead() {
        var readCalls = 0
        val plan = BookReaderPreloadRuntimeWiring.plan(
            enabled = true,
            runtimeFactory = {
                BookReaderPreloadRuntimeFactory(
                    paragraphSourceFactory = {
                        BookParagraphSource { request ->
                            readCalls++
                            BookParagraphLoadResult.success(
                                key = request.key,
                                paragraphs = listOf("Chapter 1", "First sentence."),
                            )
                        }
                    },
                    clock = IncrementingBookDocumentClock(start = 100L),
                )
            },
        )

        assertTrue(plan.clickPlan(readyStateAvailable = false) is BookReaderBookshelfClickPlan.PrepareOnBookshelf)
        assertTrue(plan.clickPlan(readyStateAvailable = true) is BookReaderBookshelfClickPlan.OpenReadyReader)
        assertEquals(0, readCalls)
    }

    @Test
    fun explicitRuntimePreloadIsTheOnlyPathThatReadsSource() {
        var readCalls = 0
        val plan = BookReaderPreloadRuntimeWiring.plan(
            enabled = true,
            runtimeFactory = {
                BookReaderPreloadRuntimeFactory(
                    paragraphSourceFactory = {
                        BookParagraphSource { request ->
                            readCalls++
                            BookParagraphLoadResult.success(
                                key = request.key,
                                paragraphs = listOf("Chapter 1", "First sentence."),
                            )
                        }
                    },
                    clock = IncrementingBookDocumentClock(start = 100L),
                )
            },
        ) as BookReaderPreloadRuntimeWiringPlan.Enabled
        val runtime = plan.createRuntime()

        val result = runtime.preloadManager.preload(request())

        assertEquals(1, readCalls)
        assertTrue(result.isReady)
        assertTrue(runtime.readyStateStore.get(request().key).isReady)
    }

    private fun request(): BookDocumentPreloadRequest {
        return BookDocumentPreloadRequest(
            key = BookDocumentCacheKey("content://book/demo.txt"),
            title = "Demo Book",
            savedParagraphIndex = 1,
            savedSentenceIndex = 0,
            chapterTitle = "Chapter 1",
            chapterStartIndex = 0,
            speechRate = 1f,
        )
    }
}

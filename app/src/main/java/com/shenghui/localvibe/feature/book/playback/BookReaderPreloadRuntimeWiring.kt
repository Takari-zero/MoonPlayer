package com.shenghui.localvibe.feature.book.playback

private const val FORMAL_READER_PACKAGE_NAME = "com.shenghui.localvibe.fastspeech2formal"

data class BookReaderPreloadObserveDecision(
    val formalPackage: Boolean,
    val observeEnabled: Boolean,
    val preloadEnabled: Boolean,
    val preloadStarted: Boolean = false,
    val txtReadTriggered: Boolean = false,
)

data class BookReaderPreloadScheduleResult(
    val scheduled: Boolean,
    val results: List<BookReaderPreloadResult> = emptyList(),
    val uiBlocked: Boolean = false,
    val txtReadOnMain: Boolean = false,
)

data class BookReaderBookshelfClickResult(
    val clickPlan: BookReaderBookshelfClickPlan,
    val readyState: BookReaderEntryReadyState? = null,
    val syncPreloadStarted: Boolean = false,
    val txtReadTriggered: Boolean = false,
)

sealed interface BookReaderBookshelfClickPlan {
    data object LegacyOpen : BookReaderBookshelfClickPlan
    data object PrepareOnBookshelf : BookReaderBookshelfClickPlan
    data object OpenReadyReader : BookReaderBookshelfClickPlan
}

sealed interface BookReaderPreloadRuntimeWiringPlan {
    val preloadStarted: Boolean

    fun clickPlan(readyStateAvailable: Boolean = false): BookReaderBookshelfClickPlan

    data object Disabled : BookReaderPreloadRuntimeWiringPlan {
        override val preloadStarted: Boolean = false

        override fun clickPlan(readyStateAvailable: Boolean): BookReaderBookshelfClickPlan {
            return BookReaderBookshelfClickPlan.LegacyOpen
        }
    }

    class Enabled(
        private val runtimeFactory: () -> BookReaderPreloadRuntimeFactory,
    ) : BookReaderPreloadRuntimeWiringPlan {
        override val preloadStarted: Boolean = false

        fun createRuntime(): BookReaderPreloadRuntime {
            return runtimeFactory().create()
        }

        override fun clickPlan(readyStateAvailable: Boolean): BookReaderBookshelfClickPlan {
            return if (readyStateAvailable) {
                BookReaderBookshelfClickPlan.OpenReadyReader
            } else {
                BookReaderBookshelfClickPlan.PrepareOnBookshelf
            }
        }
    }
}

object BookReaderPreloadRuntimeWiring {
    fun observeDecision(
        packageName: String,
        explicitPreloadFlag: Boolean,
    ): BookReaderPreloadObserveDecision {
        val formalPackage = packageName == FORMAL_READER_PACKAGE_NAME
        return BookReaderPreloadObserveDecision(
            formalPackage = formalPackage,
            observeEnabled = formalPackage,
            preloadEnabled = formalPackage && explicitPreloadFlag,
        )
    }

    fun schedulePreload(
        runtime: BookReaderPreloadRuntime,
        candidates: List<BookReaderBookshelfPreloadCandidate>,
        recentBookKeys: List<BookDocumentCacheKey>,
        currentBookKey: BookDocumentCacheKey?,
        maxPreloadCount: Int,
    ): BookReaderPreloadScheduleResult {
        val plan = runtime.bookshelfPreloadCoordinator.plan(
            candidates = candidates,
            recentBookKeys = recentBookKeys,
            currentBookKey = currentBookKey,
            maxPreloadCount = maxPreloadCount,
        )
        if (plan.isEmpty()) {
            return BookReaderPreloadScheduleResult(scheduled = false)
        }
        return BookReaderPreloadScheduleResult(
            scheduled = true,
            results = runtime.bookshelfPreloadCoordinator.preload(plan),
        )
    }

    fun selectClickReadyState(
        plan: BookReaderPreloadRuntimeWiringPlan,
        key: BookDocumentCacheKey,
        readyStateStore: BookReaderEntryReadyStateStore,
    ): BookReaderBookshelfClickResult {
        if (plan !is BookReaderPreloadRuntimeWiringPlan.Enabled) {
            return BookReaderBookshelfClickResult(
                clickPlan = BookReaderBookshelfClickPlan.LegacyOpen,
            )
        }
        val readyState = readyStateStore.getReadyState(key)
        return if (readyState != null) {
            BookReaderBookshelfClickResult(
                clickPlan = BookReaderBookshelfClickPlan.OpenReadyReader,
                readyState = readyState,
            )
        } else {
            BookReaderBookshelfClickResult(
                clickPlan = BookReaderBookshelfClickPlan.LegacyOpen,
            )
        }
    }

    fun plan(
        enabled: Boolean,
        runtimeFactory: () -> BookReaderPreloadRuntimeFactory,
    ): BookReaderPreloadRuntimeWiringPlan {
        return if (enabled) {
            BookReaderPreloadRuntimeWiringPlan.Enabled(runtimeFactory)
        } else {
            BookReaderPreloadRuntimeWiringPlan.Disabled
        }
    }
}

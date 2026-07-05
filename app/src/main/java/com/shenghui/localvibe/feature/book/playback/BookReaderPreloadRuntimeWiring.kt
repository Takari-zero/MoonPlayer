package com.shenghui.localvibe.feature.book.playback

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

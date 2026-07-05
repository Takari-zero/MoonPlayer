package com.shenghui.localvibe.feature.book.playback

import android.content.Context

class BookReaderPreloadRuntimeFactory(
    private val paragraphSourceFactory: () -> BookParagraphSource,
    private val clock: BookDocumentClock = SystemBookDocumentClock,
    private val readyStateStoreFactory: () -> BookReaderEntryReadyStateStore = { BookReaderEntryReadyStateStore() },
) {
    fun create(): BookReaderPreloadRuntime {
        return BookReaderPreloadRuntime.create(
            paragraphSource = paragraphSourceFactory(),
            clock = clock,
            readyStateStore = readyStateStoreFactory(),
        )
    }

    companion object {
        fun fromContext(
            context: Context,
            clock: BookDocumentClock = SystemBookDocumentClock,
            readyStateStoreFactory: () -> BookReaderEntryReadyStateStore = { BookReaderEntryReadyStateStore() },
        ): BookReaderPreloadRuntimeFactory {
            val appContext = context.applicationContext
            return BookReaderPreloadRuntimeFactory(
                paragraphSourceFactory = { AndroidTxtBookParagraphSource.fromContext(appContext) },
                clock = clock,
                readyStateStoreFactory = readyStateStoreFactory,
            )
        }
    }
}

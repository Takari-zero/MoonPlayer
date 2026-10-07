package com.shenghui.localvibe.feature.book

import java.util.concurrent.atomic.AtomicBoolean

internal class BookReaderExitCoordinator {
    private val exitStarted = AtomicBoolean(false)

    val hasExited: Boolean
        get() = exitStarted.get()

    fun tryStartExit(): Boolean = exitStarted.compareAndSet(false, true)
}
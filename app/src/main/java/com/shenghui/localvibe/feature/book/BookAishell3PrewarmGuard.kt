package com.shenghui.localvibe.feature.book

import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

internal class BookAishell3PrewarmGuard private constructor(
    private val sessionId: Long,
    private val latestProvider: () -> BookPlaybackEngineSnapshot,
    private val latestSessionId: () -> Long,
    private val screenDisposed: () -> Boolean,
    private val onDiscard: (String, BookPlaybackEngine, Long) -> Unit
) {
    private val discarded = AtomicBoolean(false)

    fun isActive(): Boolean {
        if (discarded.get()) return false
        val provider = latestProvider().dispatchEngine()
        val reason = when {
            !BookPlaybackSessionGuard.isActive(sessionId, latestSessionId(), screenDisposed()) -> "STALE_SESSION"
            !BookAishell3PrewarmGate.allows(provider) -> "PROVIDER_INACTIVE"
            else -> return !discarded.get()
        }
        if (discarded.compareAndSet(false, true)) onDiscard(reason, provider, sessionId)
        return false
    }

    fun runIfActive(action: () -> Unit) {
        if (isActive()) action()
    }

    companion object {
        fun begin(
            latestProvider: () -> BookPlaybackEngineSnapshot,
            latestSessionId: () -> Long,
            screenDisposed: () -> Boolean,
            onDiscard: (String, BookPlaybackEngine, Long) -> Unit = { reason, provider, session ->
                Log.i("AISHELL3_PREWARM_GUARD", "event=STALE_DISCARD reason=$reason provider=$provider session=$session")
            }
        ): BookAishell3PrewarmGuard? {
            if (screenDisposed() || !BookAishell3PrewarmGate.allows(latestProvider().dispatchEngine())) return null
            return BookAishell3PrewarmGuard(latestSessionId(), latestProvider, latestSessionId, screenDisposed, onDiscard)
        }
    }
}

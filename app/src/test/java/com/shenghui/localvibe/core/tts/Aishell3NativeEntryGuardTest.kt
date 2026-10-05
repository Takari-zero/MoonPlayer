package com.shenghui.localvibe.core.tts

import com.shenghui.localvibe.feature.book.BookAishell3PrewarmGuard
import com.shenghui.localvibe.feature.book.BookPlaybackEngine
import com.shenghui.localvibe.feature.book.BookPlaybackEngineSnapshot
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Aishell3NativeEntryGuardTest {
    @Test
    fun `valid request generates exactly once under native lock`() {
        val lock = Any()
        val guard = Aishell3NativeEntryGuard({ true })
        var generated = 0
        assertTrue(guard.afterInitialization())
        assertEquals("pcm", guard.generate(lock) {
            assertTrue(Thread.holdsLock(lock))
            generated++
            "pcm"
        })
        assertEquals(1, generated)
    }

    @Test
    fun `request stale during initialization never reaches generate`() {
        val valid = AtomicBoolean(true)
        val initialized = CountDownLatch(1)
        val finishInitialization = CountDownLatch(1)
        val generated = AtomicInteger(0)
        val skipPhase = AtomicReference<String>()
        val error = AtomicReference<Throwable>()
        val guard = Aishell3NativeEntryGuard(valid::get, skipPhase::set)
        val worker = thread(isDaemon = true) {
            try {
                initialized.countDown()
                check(finishInitialization.await(5, TimeUnit.SECONDS))
                if (guard.afterInitialization()) guard.generate(Any()) { generated.incrementAndGet() }
            } catch (failure: Throwable) {
                error.set(failure)
            }
        }
        try {
            assertTrue(initialized.await(5, TimeUnit.SECONDS))
            valid.set(false)
        } finally {
            finishInitialization.countDown()
            worker.join(5_000)
        }
        assertFalse(worker.isAlive)
        assertNull(error.get())
        assertEquals(0, generated.get())
        assertEquals("POST_INIT", skipPhase.get())
    }

    @Test
    fun `request stale while blocked on native lock is checked after lock acquisition`() {
        val valid = AtomicBoolean(true)
        val lock = Any()
        val afterInit = CountDownLatch(1)
        val generated = AtomicInteger(0)
        val result = AtomicReference<Int?>()
        val skipPhase = AtomicReference<String>()
        val error = AtomicReference<Throwable>()
        val guard = Aishell3NativeEntryGuard(valid::get, skipPhase::set)
        lateinit var worker: Thread
        synchronized(lock) {
            worker = thread(isDaemon = true) {
                try {
                    check(guard.afterInitialization())
                    afterInit.countDown()
                    result.set(guard.generate(lock) { generated.incrementAndGet() })
                } catch (failure: Throwable) {
                    error.set(failure)
                }
            }
            assertTrue(afterInit.await(5, TimeUnit.SECONDS))
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (worker.state != Thread.State.BLOCKED && worker.isAlive && System.nanoTime() < deadline) {
                Thread.sleep(1)
            }
            assertEquals(Thread.State.BLOCKED, worker.state)
            valid.set(false)
        }
        worker.join(5_000)
        assertFalse(worker.isAlive)
        assertNull(error.get())
        assertNull(result.get())
        assertEquals(0, generated.get())
        assertEquals("NATIVE_LOCK", skipPhase.get())
    }

    @Test
    fun `provider round trip cannot revive the old prewarm request at native entry`() {
        val state = ProviderState()
        val request = state.begin()!!
        val guard = Aishell3NativeEntryGuard(request::isActive)
        state.provider.set(BookPlaybackEngine.MATCHA_EXPERIMENTAL)
        state.session.incrementAndGet()
        state.provider.set(BookPlaybackEngine.AISHELL3)
        state.session.incrementAndGet()
        var generated = 0
        assertFalse(guard.afterInitialization())
        assertNull(guard.generate(Any()) { generated++ })
        assertEquals(0, generated)
        assertTrue(state.begin()!!.isActive())
    }

    @Test
    fun `foreground default validity preserves generation`() {
        val guard = Aishell3NativeEntryGuard()
        var generated = 0
        assertTrue(guard.afterInitialization())
        assertEquals(1, guard.generate(Any()) { ++generated })
        assertEquals(1, generated)
    }

    @Test
    fun `stale abort cannot publish cache or continue segments or next target`() {
        val state = ProviderState()
        val request = state.begin()!!
        val guard = Aishell3NativeEntryGuard(request::isActive)
        state.provider.set(BookPlaybackEngine.MATCHA_EXPERIMENTAL)
        var generated = 0
        var cachePuts = 0
        var nextSegments = 0
        var nextTargets = 0
        val pcm = if (guard.afterInitialization()) guard.generate(Any()) { ++generated } else null
        if (pcm != null) request.runIfActive {
            cachePuts++
            nextSegments++
            nextTargets++
        }
        assertNull(pcm)
        assertEquals(0, generated)
        assertEquals(0, cachePuts)
        assertEquals(0, nextSegments)
        assertEquals(0, nextTargets)
    }

    @Test
    fun `disposed screen is rejected after initialization`() {
        val state = ProviderState()
        val request = state.begin()!!
        val guard = Aishell3NativeEntryGuard(request::isActive)
        state.disposed.set(true)
        assertFalse(guard.afterInitialization())
    }

    @Test
    fun `provider restore alone invalidates prewarm without changing session`() {
        val state = ProviderState()
        val request = state.begin()!!
        val guard = Aishell3NativeEntryGuard(request::isActive)
        state.provider.set(BookPlaybackEngine.MATCHA_EXPERIMENTAL)
        assertEquals(1L, state.session.get())
        assertFalse(guard.afterInitialization())
        assertNull(guard.generate(Any()) { "pcm" })
    }

    private class ProviderState {
        val provider = AtomicReference(BookPlaybackEngine.AISHELL3)
        val session = AtomicLong(1)
        val disposed = AtomicBoolean(false)

        fun begin() = BookAishell3PrewarmGuard.begin(
            latestProvider = { provider.get().let { BookPlaybackEngineSnapshot(it, it, matchaAvailable = true) } },
            providerSelectionRestored = { true },
            latestSessionId = session::get,
            screenDisposed = disposed::get,
            onDiscard = { _, _, _ -> }
        )
    }
}

package com.shenghui.localvibe.core.tts

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class MatchaEngineInitializationTest {
    private class Resource {
        val releases = AtomicInteger()
    }

    private class Factory {
        val constructions = AtomicInteger()
        val started = CompletableDeferred<Unit>()
        val finish = CountDownLatch(1)
        val resource = Resource()

        fun create(): Resource {
            constructions.incrementAndGet()
            started.complete(Unit)
            check(finish.await(5, TimeUnit.SECONDS)) { "test constructor timed out" }
            return resource
        }

        fun initialization() = MatchaEngineInitialization(
            create = ::create,
            destroy = { it.releases.incrementAndGet(); Unit }
        )
    }

    @Test(timeout = 10_000)
    fun sequentialCallsConstructOnce() = runBlocking {
        val factory = Factory().apply { finish.countDown() }
        val init = factory.initialization()

        assertTrue(init.ensureInitialized().isSuccess)
        assertTrue(init.ensureInitialized().isSuccess)
        assertEquals(1, factory.constructions.get())
        assertSame(factory.resource, init.readyResource())
        init.release()
    }

    @Test(timeout = 10_000)
    fun concurrentCallsShareInitialization() = runBlocking {
        val factory = Factory()
        val init = factory.initialization()
        val first = async(start = CoroutineStart.UNDISPATCHED) { init.ensureInitialized() }
        withTimeout(5_000) { factory.started.await() }
        val second = async(start = CoroutineStart.UNDISPATCHED) { init.ensureInitialized() }
        assertFalse(second.isCompleted)
        assertFalse(init.isReady)
        factory.finish.countDown()

        assertTrue(first.await().isSuccess)
        assertTrue(second.await().isSuccess)
        assertTrue(init.isReady)
        assertEquals(1, factory.constructions.get())
        assertSame(factory.resource, init.readyResource())
        init.release()
    }

    @Test(timeout = 10_000)
    fun backgroundPreparationAndPlaybackAndPrewarmShareOneConstructor() = runBlocking {
        val factory = Factory()
        val init = factory.initialization()
        val backgroundPrepare = async(start = CoroutineStart.UNDISPATCHED) { init.ensureInitialized() }
        withTimeout(5_000) { factory.started.await() }
        val playback = async(start = CoroutineStart.UNDISPATCHED) { init.ensureInitialized() }
        val prewarm = async(start = CoroutineStart.UNDISPATCHED) { init.ensureInitialized() }
        assertFalse(playback.isCompleted)
        assertFalse(prewarm.isCompleted)
        factory.finish.countDown()

        assertTrue(backgroundPrepare.await().isSuccess)
        assertTrue(playback.await().isSuccess)
        assertTrue(prewarm.await().isSuccess)
        assertEquals(1, factory.constructions.get())
        init.release()
    }

    @Test(timeout = 10_000)
    fun releaseDuringConstructionDoesNotWaitOrPublishAndReleasesLateResource() = runBlocking {
        val factory = Factory()
        val init = factory.initialization()
        val owner = async(start = CoroutineStart.UNDISPATCHED) { init.ensureInitialized() }
        withTimeout(5_000) { factory.started.await() }
        val waiter = async(start = CoroutineStart.UNDISPATCHED) { init.ensureInitialized() }

        init.release()
        assertFalse(init.isReady)
        assertNull(init.readyResource())
        assertTrue(withTimeout(1_000) { waiter.await() }.isFailure)
        assertEquals(1L, factory.finish.count)
        factory.finish.countDown()

        assertTrue(owner.await().isFailure)
        assertFalse(init.isReady)
        assertNull(init.readyResource())
        assertEquals(1, factory.resource.releases.get())
        assertTrue(init.ensureInitialized().isFailure)
        assertEquals(1, factory.constructions.get())
    }

    @Test(timeout = 10_000)
    fun readyCallsReturnWithoutConstructingAgain() = runBlocking {
        val factory = Factory().apply { finish.countDown() }
        val init = factory.initialization()
        assertTrue(init.ensureInitialized().isSuccess)

        withTimeout(1_000) {
            repeat(100) { assertTrue(init.ensureInitialized().isSuccess) }
        }
        assertEquals(1, factory.constructions.get())
        init.release()
    }

    @Test(timeout = 10_000)
    fun failedInitializationUnblocksAllWaitersAndCanRetry() = runBlocking {
        val factory = Factory()
        val failure = IllegalStateException("native constructor failed")
        val init = MatchaEngineInitialization(
            create = {
                val resource = factory.create()
                if (factory.constructions.get() == 1) throw failure
                resource
            },
            destroy = { resource -> resource.releases.incrementAndGet(); Unit }
        )
        val owner = async(start = CoroutineStart.UNDISPATCHED) { init.ensureInitialized() }
        withTimeout(5_000) { factory.started.await() }
        val waiter = async(start = CoroutineStart.UNDISPATCHED) { init.ensureInitialized() }
        factory.finish.countDown()

        assertSame(failure, owner.await().exceptionOrNull())
        assertSame(failure, waiter.await().exceptionOrNull())
        assertFalse(init.isReady)
        assertTrue(init.ensureInitialized().isSuccess)
        assertEquals(2, factory.constructions.get())
        init.release()
        assertEquals(1, factory.resource.releases.get())
    }

    @Test(timeout = 10_000)
    fun cancelledOwnerStillCleansUpLateConstructorAfterRelease() = runBlocking {
        val factory = Factory()
        val init = factory.initialization()
        val owner = async(start = CoroutineStart.UNDISPATCHED) { init.ensureInitialized() }
        withTimeout(5_000) { factory.started.await() }

        owner.cancel()
        init.release()
        factory.finish.countDown()
        owner.join()

        assertTrue(owner.isCancelled)
        assertFalse(init.isReady)
        assertNull(init.readyResource())
        assertEquals(1, factory.resource.releases.get())
    }

    @Test(timeout = 10_000)
    fun cancelledWaiterDoesNotCancelSharedCompletion() = runBlocking {
        val factory = Factory()
        val init = factory.initialization()
        val owner = async(start = CoroutineStart.UNDISPATCHED) { init.ensureInitialized() }
        withTimeout(5_000) { factory.started.await() }
        val waiter = async(start = CoroutineStart.UNDISPATCHED) { init.ensureInitialized() }

        waiter.cancelAndJoin()
        factory.finish.countDown()

        assertTrue(owner.await().isSuccess)
        assertEquals(1, factory.constructions.get())
        assertTrue(init.isReady)
        init.release()
    }

    @Test(timeout = 10_000)
    fun cancelledOwnerCanLeaveReadyEngineForTheLiveReader() = runBlocking {
        val factory = Factory()
        val init = factory.initialization()
        val owner = async(start = CoroutineStart.UNDISPATCHED) { init.ensureInitialized() }
        withTimeout(5_000) { factory.started.await() }
        val playback = async(start = CoroutineStart.UNDISPATCHED) { init.ensureInitialized() }

        owner.cancel()
        factory.finish.countDown()
        owner.join()

        assertTrue(playback.await().isSuccess)
        assertTrue(init.isReady)
        assertEquals(1, factory.constructions.get())
        init.release()
        assertEquals(1, factory.resource.releases.get())
    }

    @Test(timeout = 10_000)
    fun releaseIsTerminalAndReadyResourceIsDestroyedOnce() = runBlocking {
        val factory = Factory().apply { finish.countDown() }
        val init = factory.initialization()
        assertTrue(init.ensureInitialized().isSuccess)

        init.release()
        init.release()

        assertTrue(init.ensureInitialized().isFailure)
        assertFalse(init.isReady)
        assertNull(init.readyResource())
        assertEquals(1, factory.resource.releases.get())
        assertEquals(1, factory.constructions.get())
    }
}

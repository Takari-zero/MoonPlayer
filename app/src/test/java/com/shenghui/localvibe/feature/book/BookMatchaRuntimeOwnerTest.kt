package com.shenghui.localvibe.feature.book

import com.shenghui.localvibe.core.tts.MatchaPreparedAudio
import com.shenghui.localvibe.core.tts.PcmAudioChunk
import com.shenghui.localvibe.core.tts.StreamingTtsParams
import com.shenghui.localvibe.core.tts.StreamingTtsResult
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookMatchaRuntimeOwnerTest {
    @Test
    fun `root prepare only warms effective matcha`() {
        assertTrue(
            shouldPrepareMatchaRuntime(
                BookPlaybackEngine.MATCHA_EXPERIMENTAL,
                BookPlaybackEngine.MATCHA_EXPERIMENTAL,
                matchaAvailable = true
            )
        )
        assertFalse(
            shouldPrepareMatchaRuntime(
                BookPlaybackEngine.SYSTEM_TTS,
                BookPlaybackEngine.SYSTEM_TTS,
                matchaAvailable = true
            )
        )
        assertFalse(
            shouldPrepareMatchaRuntime(
                BookPlaybackEngine.AISHELL3,
                BookPlaybackEngine.AISHELL3,
                matchaAvailable = true
            )
        )
    }

    @Test
    fun `root and reader prepare share one initialization and reader release keeps runtime`() = runBlocking {
        val fake = FakeMatchaRuntime()
        val ownerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val owner = BookMatchaRuntimeOwner(fake, ownerScope)
        val readerA = BookMatchaPlaybackCoordinator(owner)
        val readerB = BookMatchaPlaybackCoordinator(owner)

        owner.prepareEngine()
        readerA.prepareEngine()
        readerB.prepareEngine()
        awaitReady(fake)

        assertEquals(1, fake.initializationCount.get())
        readerA.releasePlaybackState()
        assertTrue(fake.isReady)
        assertEquals(0, fake.releaseCount.get())

        owner.releaseEngineRuntime()
        owner.releaseEngineRuntime()
        assertEquals(1, fake.releaseCount.get())
        assertEquals(2, fake.stopCount.get())
    }

    @Test
    fun `late initialization survives reader disposal`() = runBlocking {
        val fake = FakeMatchaRuntime(initializationDelayMs = 40)
        val owner = BookMatchaRuntimeOwner(
            fake,
            CoroutineScope(SupervisorJob() + Dispatchers.Default)
        )
        val reader = BookMatchaPlaybackCoordinator(owner)

        owner.prepareEngine()
        reader.releasePlaybackState()
        awaitReady(fake)

        assertEquals(1, fake.initializationCount.get())
        assertEquals(0, fake.releaseCount.get())
        owner.releaseEngineRuntime()
    }

    private suspend fun awaitReady(fake: FakeMatchaRuntime) {
        withTimeout(1_000) {
            while (!fake.isReady) delay(5)
        }
    }

    private class FakeMatchaRuntime(
        private val initializationDelayMs: Long = 0
    ) : BookMatchaRuntime {
        private val ready = java.util.concurrent.atomic.AtomicBoolean(false)
        val initializationCount = AtomicInteger(0)
        val releaseCount = AtomicInteger(0)
        val stopCount = AtomicInteger(0)

        override val isReady: Boolean
            get() = ready.get()

        override fun isModelAvailable(): Boolean = true

        override suspend fun ensureInitialized(): Result<Unit> {
            initializationCount.incrementAndGet()
            if (initializationDelayMs > 0) delay(initializationDelayMs)
            ready.set(true)
            return Result.success(Unit)
        }

        override suspend fun speak(
            text: String,
            params: StreamingTtsParams,
            onStart: () -> Unit,
            onChunk: suspend (PcmAudioChunk) -> Unit,
            onDone: () -> Unit,
            onError: (String) -> Unit
        ): StreamingTtsResult = StreamingTtsResult.Stopped

        override suspend fun prepare(
            text: String,
            params: StreamingTtsParams
        ): Result<MatchaPreparedAudio> = Result.failure(UnsupportedOperationException())

        override fun stop() {
            stopCount.incrementAndGet()
        }

        override fun release() {
            releaseCount.incrementAndGet()
            ready.set(false)
        }
    }
}

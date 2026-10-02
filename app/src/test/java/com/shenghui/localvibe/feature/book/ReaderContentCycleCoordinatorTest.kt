package com.shenghui.localvibe.feature.book

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class ReaderContentCycleCoordinatorTest {
    @Test
    fun firstBeginUsesGenerationOne() {
        val fixture = fixture(CountingOpener { null })

        val cycle = fixture.coordinator.beginContentCycle(BOOK_A, null, null)

        assertEquals(1L, cycle.generation)
        assertEquals(BOOK_A, cycle.bookUri)
        fixture.coordinator.release()
    }

    @Test
    fun sameBookBeginTwiceAdvancesGeneration() {
        val fixture = fixture(CountingOpener { null })

        val first = fixture.coordinator.beginContentCycle(BOOK_A, null, null)
        val second = fixture.coordinator.beginContentCycle(BOOK_A, null, null)

        assertEquals(1L, first.generation)
        assertEquals(2L, second.generation)
        fixture.coordinator.release()
    }

    @Test
    fun aToBToAUsesThreeDistinctGenerations() {
        val fixture = fixture(CountingOpener { null })

        val firstA = fixture.coordinator.beginContentCycle(BOOK_A, null, null)
        val bookB = fixture.coordinator.beginContentCycle(BOOK_B, null, null)
        val secondA = fixture.coordinator.beginContentCycle(BOOK_A, null, null)

        assertEquals(listOf(1L, 2L, 3L), listOf(firstA.generation, bookB.generation, secondA.generation))
        fixture.coordinator.release()
    }

    @Test
    fun loaderReceivesExactCoordinatorGeneration() {
        val fixture = fixture(CountingOpener { null })

        val cycle = fixture.coordinator.beginContentCycle(BOOK_A, 7L, 1234L)
        val loaderState = fixture.loader.stateSnapshot() as CurrentPreviewContentVersionState.Ready

        assertEquals(cycle.generation, loaderState.readerGeneration)
        assertEquals(BOOK_A, loaderState.bookUri)
        fixture.coordinator.release()
    }

    @Test(timeout = 10_000)
    fun shadowStartReturnsWithoutWaitingForFingerprint() {
        val stream = BlockingInputStream("slow".encodeToByteArray())
        val fixture = fixture(CountingOpener { stream })
        val cycle = fixture.coordinator.beginContentCycle(BOOK_A, 4L, null)

        assertTrue(fixture.coordinator.startCurrentVersionShadow(cycle))
        assertTrue(stream.readStarted.await(5, TimeUnit.SECONDS))
        assertTrue(fixture.coordinator.shadowStateSnapshot() is CurrentVersionShadowState.InFlight)

        fixture.coordinator.release()
        stream.finishRead.countDown()
    }

    @Test(timeout = 10_000)
    fun repeatedShadowStartForSameCycleOpensStreamOnce() = runBlocking {
        val stream = BlockingInputStream("once".encodeToByteArray())
        val opener = CountingOpener { stream }
        val fixture = fixture(opener)
        val cycle = fixture.coordinator.beginContentCycle(BOOK_A, 4L, null)

        assertTrue(fixture.coordinator.startCurrentVersionShadow(cycle))
        assertFalse(fixture.coordinator.startCurrentVersionShadow(cycle))
        assertTrue(stream.readStarted.await(5, TimeUnit.SECONDS))
        assertEquals(1, opener.opens.get())
        stream.finishRead.countDown()
        awaitState { fixture.coordinator.shadowStateSnapshot() is CurrentVersionShadowState.Ready }

        assertEquals(1, opener.opens.get())
        fixture.coordinator.release()
    }

    @Test(timeout = 10_000)
    fun lateOldCycleResultCannotReplaceCurrentShadow() = runBlocking {
        val oldStream = BlockingInputStream("old".encodeToByteArray(), ignoreClose = true)
        val opener = SequenceOpener(
            listOf(oldStream, ByteArrayInputStream("new".encodeToByteArray()))
        )
        val fixture = fixture(opener)
        val first = fixture.coordinator.beginContentCycle(BOOK_A, 3L, null)
        assertTrue(fixture.coordinator.startCurrentVersionShadow(first))
        assertTrue(oldStream.readStarted.await(5, TimeUnit.SECONDS))

        val second = fixture.coordinator.beginContentCycle(BOOK_B, 3L, null)
        assertTrue(fixture.coordinator.startCurrentVersionShadow(second))
        oldStream.finishRead.countDown()
        awaitState {
            val state = fixture.coordinator.shadowStateSnapshot()
            state is CurrentVersionShadowState.Ready && state.cycle == second
        }

        assertTrue(fixture.logs.any { it.contains("event=STALE generation=${first.generation}") })
        assertEquals(second, fixture.coordinator.currentCycleSnapshot())
        fixture.coordinator.release()
    }

    @Test(timeout = 10_000)
    fun releaseDuringInFlightCannotPublishReady() {
        val stream = BlockingInputStream("late".encodeToByteArray(), ignoreClose = true)
        val fixture = fixture(CountingOpener { stream })
        val cycle = fixture.coordinator.beginContentCycle(BOOK_A, 4L, null)
        assertTrue(fixture.coordinator.startCurrentVersionShadow(cycle))
        assertTrue(stream.readStarted.await(5, TimeUnit.SECONDS))

        fixture.coordinator.release()
        fixture.coordinator.release()
        stream.finishRead.countDown()

        assertEquals(CurrentVersionShadowState.Released, fixture.coordinator.shadowStateSnapshot())
        assertFalse(fixture.logs.any { it.contains("event=READY") })
    }

    @Test(timeout = 10_000)
    fun metadataFastPathShadowDoesNotOpenStream() = runBlocking {
        val opener = CountingOpener { ByteArrayInputStream(byteArrayOf(1)) }
        val fixture = fixture(opener)
        val cycle = fixture.coordinator.beginContentCycle(BOOK_A, 7L, 1234L)

        assertTrue(fixture.coordinator.startCurrentVersionShadow(cycle))
        awaitState { fixture.coordinator.shadowStateSnapshot() is CurrentVersionShadowState.Ready }

        assertEquals(0, opener.opens.get())
        assertTrue(fixture.logs.any { it.contains("versionKind=METADATA") })
        fixture.coordinator.release()
    }

    private fun fixture(opener: PreviewContentInputStreamOpener): Fixture {
        val scope = CoroutineScope(Dispatchers.Default)
        val loader = CurrentPreviewContentVersionLoader(
            ownerScope = scope,
            streamOpener = opener,
            fingerprintDispatcher = Dispatchers.IO
        )
        val logs = Collections.synchronizedList(mutableListOf<String>())
        val coordinator = ReaderContentCycleCoordinator(
            ownerScope = scope,
            currentVersionLoader = loader,
            shadowLogger = logs::add
        )
        return Fixture(coordinator, loader, logs)
    }

    private fun awaitState(predicate: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (!predicate()) {
            check(System.nanoTime() < deadline) { "Timed out waiting for shadow state" }
            Thread.sleep(10L)
        }
    }

    private data class Fixture(
        val coordinator: ReaderContentCycleCoordinator,
        val loader: CurrentPreviewContentVersionLoader,
        val logs: List<String>
    )

    private class CountingOpener(
        private val stream: () -> InputStream?
    ) : PreviewContentInputStreamOpener {
        val opens = AtomicInteger()

        override fun open(bookUri: String): InputStream? {
            opens.incrementAndGet()
            return stream()
        }
    }

    private class SequenceOpener(streams: List<InputStream?>) : PreviewContentInputStreamOpener {
        private val queue = ArrayDeque(streams)

        @Synchronized
        override fun open(bookUri: String): InputStream? = queue.removeFirst()
    }

    private class BlockingInputStream(
        private val bytes: ByteArray,
        private val ignoreClose: Boolean = false
    ) : InputStream() {
        val readStarted = CountDownLatch(1)
        val finishRead = CountDownLatch(1)
        private val closed = AtomicBoolean(false)
        private var emitted = false

        override fun read(): Int {
            val one = ByteArray(1)
            val count = read(one, 0, 1)
            return if (count < 0) -1 else one[0].toInt() and 0xFF
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (emitted) return -1
            readStarted.countDown()
            check(finishRead.await(5, TimeUnit.SECONDS)) { "test stream timed out" }
            if (closed.get() && !ignoreClose) error("stream closed")
            bytes.copyInto(buffer, offset, 0, bytes.size)
            emitted = true
            return bytes.size
        }

        override fun close() {
            closed.set(true)
        }
    }

    private companion object {
        const val BOOK_A = "content://books/a"
        const val BOOK_B = "content://books/b"
    }
}

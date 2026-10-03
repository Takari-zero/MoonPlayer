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

    @Test(timeout = 10_000)
    fun comparisonKeepsEntryVersionAfterCacheReplacement() {
        val stream = BlockingInputStream("same".encodeToByteArray())
        val fixture = fixture(CountingOpener { stream })
        var savedVersion = PreviewContentVersion.fingerprint(4L, "a".repeat(64))
        val cycle = fixture.coordinator.beginContentCycle(BOOK_A, 4L, null, savedVersion)
        fixture.coordinator.startCurrentVersionShadow(cycle)
        assertTrue(stream.readStarted.await(5, TimeUnit.SECONDS))
        savedVersion = fingerprint("same")
        stream.finishRead.countDown()
        awaitState { fixture.coordinator.shadowStateSnapshot() is CurrentVersionShadowState.Ready }

        assertEquals(fingerprint("same"), savedVersion)
        assertTrue(fixture.comparisons.single().contains("reason=CONTENT_FINGERPRINT_MISMATCH"))
        fixture.coordinator.release()
    }

    @Test(timeout = 10_000)
    fun sameGenerationPublishesOneMatchingComparison() {
        val fixture = fixture(CountingOpener { ByteArrayInputStream("same".encodeToByteArray()) })
        val cycle = fixture.coordinator.beginContentCycle(BOOK_A, 4L, null, fingerprint("same"))
        assertTrue(fixture.coordinator.startCurrentVersionShadow(cycle))
        repeat(5) { assertFalse(fixture.coordinator.startCurrentVersionShadow(cycle)) }
        awaitState { fixture.coordinator.shadowStateSnapshot() is CurrentVersionShadowState.Ready }
        assertFalse(fixture.coordinator.startCurrentVersionShadow(cycle))
        assertTrue(fixture.comparisons.single().contains("result=MATCH"))
        assertFalse(fixture.comparisons.single().contains(BOOK_A))
        assertFalse(fixture.comparisons.single().contains((fingerprint("same") as PreviewContentVersion.Fingerprint).sha256Hex))
        fixture.coordinator.release()
    }

    @Test(timeout = 10_000)
    fun lateOtherBookCannotPublishComparison() = assertLateComparisonRejected(listOf(BOOK_A, BOOK_B))

    @Test(timeout = 10_000)
    fun lateSameBookReloadCannotPublishComparison() = assertLateComparisonRejected(listOf(BOOK_A, BOOK_A))

    @Test(timeout = 10_000)
    fun firstACannotPublishForThirdA() = assertLateComparisonRejected(listOf(BOOK_A, BOOK_B, BOOK_A))

    @Test(timeout = 10_000)
    fun failedCurrentLoaderCannotMatchOrFallbackToCache() {
        val fixture = fixture(CountingOpener { null })
        val cycle = fixture.coordinator.beginContentCycle(BOOK_A, 4L, null, fingerprint("same"))
        fixture.coordinator.startCurrentVersionShadow(cycle)
        awaitState { fixture.coordinator.shadowStateSnapshot() is CurrentVersionShadowState.Failed }
        val message = fixture.comparisons.single()
        assertTrue(message.contains("result=UNAVAILABLE"))
        assertTrue(message.contains("currentKind=UNAVAILABLE"))
        assertTrue(message.contains("diagnosticReason=STREAM_OPEN_FAILED"))
        assertFalse(message.contains("result=MATCH"))
        fixture.coordinator.release()
    }

    @Test(timeout = 10_000)
    fun unavailableEntryCacheCannotBecomeMatch() {
        val fixture = fixture(CountingOpener { null })
        val cycle = fixture.coordinator.beginContentCycle(BOOK_A, 4L, 123L)
        fixture.coordinator.startCurrentVersionShadow(cycle)
        awaitState { fixture.coordinator.shadowStateSnapshot() is CurrentVersionShadowState.Ready }
        assertTrue(fixture.comparisons.single().contains("result=UNAVAILABLE"))
        fixture.coordinator.release()
    }

    @Test(timeout = 10_000)
    fun currentVersionSnapshotIsReadyOnlyAndDoesNotStartRead() {
        val opener = CountingOpener { null }
        val fixture = fixture(opener)
        val first = fixture.coordinator.beginContentCycle(BOOK_A, 4L, null)
        assertEquals(null, fixture.coordinator.currentVersionSnapshot(first))
        assertEquals(0, opener.opens.get())
        fixture.coordinator.startCurrentVersionShadow(first)
        awaitState { fixture.coordinator.shadowStateSnapshot() is CurrentVersionShadowState.Failed }
        assertEquals(null, fixture.coordinator.currentVersionSnapshot(first))
        fixture.coordinator.release()
    }

    @Test(timeout = 10_000)
    fun currentVersionSnapshotRejectsReloadAndReleasedCycle() {
        val fixture = fixture(CountingOpener { null })
        val first = fixture.coordinator.beginContentCycle(BOOK_A, 4L, 123L)
        fixture.coordinator.startCurrentVersionShadow(first)
        awaitState { fixture.coordinator.currentVersionSnapshot(first) != null }
        val second = fixture.coordinator.beginContentCycle(BOOK_A, 4L, 123L)
        fixture.coordinator.startCurrentVersionShadow(second)
        awaitState { fixture.coordinator.currentVersionSnapshot(second) != null }
        assertEquals(null, fixture.coordinator.currentVersionSnapshot(first))
        fixture.coordinator.release()
        assertEquals(null, fixture.coordinator.currentVersionSnapshot(second))
    }

    private fun assertLateComparisonRejected(books: List<String>) {
        val old = BlockingInputStream("old".encodeToByteArray(), ignoreClose = true)
        val fixture = fixture(SequenceOpener(listOf(old, ByteArrayInputStream("new".encodeToByteArray()))))
        val first = fixture.coordinator.beginContentCycle(books.first(), 3L, null, fingerprint("old"))
        fixture.coordinator.startCurrentVersionShadow(first)
        assertTrue(old.readStarted.await(5, TimeUnit.SECONDS))
        var latest = first
        for (book in books.drop(1)) {
            latest = fixture.coordinator.beginContentCycle(book, 3L, null, fingerprint("new"))
        }
        fixture.coordinator.startCurrentVersionShadow(latest)
        old.finishRead.countDown()
        awaitState {
            val state = fixture.coordinator.shadowStateSnapshot()
            state is CurrentVersionShadowState.Ready && state.cycle == latest
        }
        awaitState { fixture.logs.any { it.contains("event=STALE generation=1 ") } }
        assertEquals(1, fixture.comparisons.size)
        assertTrue(fixture.comparisons.single().contains("generation=${latest.generation} "))
        assertTrue(fixture.comparisons.single().contains("result=MATCH"))
        fixture.coordinator.release()
    }

    private fun fingerprint(text: String): PreviewContentVersion = PreviewContentVersion.fingerprint(
        text.encodeToByteArray().size.toLong(),
        java.security.MessageDigest.getInstance("SHA-256").digest(text.encodeToByteArray())
            .joinToString("") { "%02x".format(it) }
    )

    private fun fixture(opener: PreviewContentInputStreamOpener): Fixture {
        val scope = CoroutineScope(Dispatchers.Default)
        val loader = CurrentPreviewContentVersionLoader(
            ownerScope = scope,
            streamOpener = opener,
            fingerprintDispatcher = Dispatchers.IO
        )
        val logs = Collections.synchronizedList(mutableListOf<String>())
        val comparisons = Collections.synchronizedList(mutableListOf<String>())
        val coordinator = ReaderContentCycleCoordinator(
            ownerScope = scope,
            currentVersionLoader = loader,
            shadowLogger = logs::add,
            cacheVersionShadowLogger = comparisons::add
        )
        return Fixture(coordinator, loader, logs, comparisons)
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
        val logs: List<String>,
        val comparisons: List<String>
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

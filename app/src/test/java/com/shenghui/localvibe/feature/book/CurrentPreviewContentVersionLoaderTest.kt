package com.shenghui.localvibe.feature.book

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class CurrentPreviewContentVersionLoaderTest {
    @Test(timeout = 10_000)
    fun metadataFastPathDoesNotOpenStream() = runBlocking {
        val opener = CountingOpener { ByteArrayInputStream(byteArrayOf(1)) }
        val loader = loader(opener)
        loader.beginContentCycle(BOOK_A, 1L, 7L, 1234L)

        val result = loader.ensureCurrentVersion()

        assertEquals(PreviewContentVersion.metadata(7L, 1234L), result.version)
        assertTrue(result.isReady)
        assertEquals(0, opener.opens.get())
        loader.release()
    }

    @Test(timeout = 10_000)
    fun fingerprintSuccessUsesActualBytesAndExpectedDigest() = runBlocking {
        val input = CloseTrackingInputStream("abc".encodeToByteArray())
        val loader = loader(CountingOpener { input })
        loader.beginContentCycle(BOOK_A, 1L, 3L, null)

        val result = loader.ensureCurrentVersion()

        assertEquals(
            PreviewContentVersion.fingerprint(
                3L,
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
            ),
            result.version
        )
        assertTrue(input.closed.get())
        loader.release()
    }

    @Test(timeout = 10_000)
    fun emptyFileProducesValidFingerprint() = runBlocking {
        val loader = loader(CountingOpener { ByteArrayInputStream(byteArrayOf()) })
        loader.beginContentCycle(BOOK_A, 1L, 0L, null)

        val result = loader.ensureCurrentVersion()

        assertEquals(
            PreviewContentVersion.fingerprint(
                0L,
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
            ),
            result.version
        )
        loader.release()
    }

    @Test(timeout = 10_000)
    fun concurrentCallersShareOneInFlightRead() = runBlocking {
        val stream = BlockingInputStream("shared".encodeToByteArray())
        val opener = CountingOpener { stream }
        val loader = loader(opener)
        loader.beginContentCycle(BOOK_A, 1L, 6L, null)
        val callers = List(8) {
            async(start = CoroutineStart.UNDISPATCHED) { loader.ensureCurrentVersion() }
        }
        assertTrue(stream.readStarted.await(5, TimeUnit.SECONDS))
        assertEquals(1, opener.opens.get())
        stream.finishRead.countDown()

        val results = callers.map { it.await() }

        results.drop(1).forEach { assertSame(results.first(), it) }
        assertEquals(1, opener.opens.get())
        loader.release()
    }

    @Test(timeout = 10_000)
    fun readyResultIsReusedWithoutSecondRead() = runBlocking {
        val opener = CountingOpener { ByteArrayInputStream("ready".encodeToByteArray()) }
        val loader = loader(opener)
        loader.beginContentCycle(BOOK_A, 1L, 5L, null)

        val first = loader.ensureCurrentVersion()
        val second = loader.ensureCurrentVersion()

        assertSame(first, second)
        assertEquals(1, opener.opens.get())
        loader.release()
    }

    @Test(timeout = 10_000)
    fun nullStreamFailsOnceAndIsReused() = runBlocking {
        val opener = CountingOpener { null }
        val loader = loader(opener)
        loader.beginContentCycle(BOOK_A, 1L, 5L, null)

        val first = loader.ensureCurrentVersion()
        val second = loader.ensureCurrentVersion()

        assertEquals(CurrentPreviewContentVersionFailureReason.STREAM_OPEN_FAILED, first.diagnosticReason)
        assertEquals(PreviewContentVersion.Unavailable, first.version)
        assertSame(first, second)
        assertEquals(1, opener.opens.get())
        loader.release()
    }

    @Test(timeout = 10_000)
    fun readFailureClosesStreamAndBecomesFailed() = runBlocking {
        val stream = ThrowingInputStream()
        val loader = loader(CountingOpener { stream })
        loader.beginContentCycle(BOOK_A, 1L, null, null)

        val result = loader.ensureCurrentVersion()

        assertEquals(CurrentPreviewContentVersionFailureReason.READ_FAILED, result.diagnosticReason)
        assertTrue(stream.closed.get())
        assertTrue(loader.stateSnapshot() is CurrentPreviewContentVersionState.Failed)
        loader.release()
    }

    @Test(timeout = 10_000)
    fun expectedSizeMismatchDoesNotPublishFingerprint() = runBlocking {
        val loader = loader(CountingOpener { ByteArrayInputStream("abc".encodeToByteArray()) })
        loader.beginContentCycle(BOOK_A, 1L, 4L, null)

        val result = loader.ensureCurrentVersion()

        assertEquals(
            CurrentPreviewContentVersionFailureReason.SIZE_CHANGED_DURING_READ,
            result.diagnosticReason
        )
        assertEquals(PreviewContentVersion.Unavailable, result.version)
        loader.release()
    }

    @Test(timeout = 10_000)
    fun cycleChangeMakesOldCompletionStaleWithoutAffectingNewCycle() = runBlocking {
        val oldStream = BlockingInputStream("old".encodeToByteArray(), ignoreClose = true)
        val opener = SequenceOpener(listOf(oldStream, ByteArrayInputStream("new".encodeToByteArray())))
        val loader = loader(opener)
        loader.beginContentCycle(BOOK_A, 1L, 3L, null)
        val old = async(start = CoroutineStart.UNDISPATCHED) { loader.ensureCurrentVersion() }
        assertTrue(oldStream.readStarted.await(5, TimeUnit.SECONDS))

        loader.beginContentCycle(BOOK_B, 2L, 3L, null)
        val new = loader.ensureCurrentVersion()
        oldStream.finishRead.countDown()

        assertEquals(CurrentPreviewContentVersionFailureReason.STALE, old.await().diagnosticReason)
        assertTrue(new.isReady)
        val state = loader.stateSnapshot() as CurrentPreviewContentVersionState.Ready
        assertEquals(BOOK_B, state.bookUri)
        assertEquals(2L, state.readerGeneration)
        loader.release()
    }

    @Test(timeout = 10_000)
    fun sameBookReloadRejectsOldGeneration() = runBlocking {
        val oldStream = BlockingInputStream("old".encodeToByteArray(), ignoreClose = true)
        val loader = loader(SequenceOpener(listOf(oldStream, ByteArrayInputStream("new".encodeToByteArray()))))
        loader.beginContentCycle(BOOK_A, 1L, 3L, null)
        val old = async(start = CoroutineStart.UNDISPATCHED) { loader.ensureCurrentVersion() }
        assertTrue(oldStream.readStarted.await(5, TimeUnit.SECONDS))

        loader.beginContentCycle(BOOK_A, 2L, 3L, null)
        val new = loader.ensureCurrentVersion()
        oldStream.finishRead.countDown()

        assertEquals(CurrentPreviewContentVersionFailureReason.STALE, old.await().diagnosticReason)
        assertTrue(new.isReady)
        assertEquals(2L, (loader.stateSnapshot() as CurrentPreviewContentVersionState.Ready).readerGeneration)
        loader.release()
    }

    @Test(timeout = 10_000)
    fun aToBToADoesNotAcceptFirstACompletion() = runBlocking {
        val firstA = BlockingInputStream("old".encodeToByteArray(), ignoreClose = true)
        val opener = SequenceOpener(
            listOf(
                firstA,
                ByteArrayInputStream("bbb".encodeToByteArray()),
                ByteArrayInputStream("new".encodeToByteArray())
            )
        )
        val loader = loader(opener)
        loader.beginContentCycle(BOOK_A, 1L, 3L, null)
        val old = async(start = CoroutineStart.UNDISPATCHED) { loader.ensureCurrentVersion() }
        assertTrue(firstA.readStarted.await(5, TimeUnit.SECONDS))

        loader.beginContentCycle(BOOK_B, 2L, 3L, null)
        assertTrue(loader.ensureCurrentVersion().isReady)
        loader.beginContentCycle(BOOK_A, 3L, 3L, null)
        val latest = loader.ensureCurrentVersion()
        firstA.finishRead.countDown()

        assertEquals(CurrentPreviewContentVersionFailureReason.STALE, old.await().diagnosticReason)
        assertEquals(
            PreviewContentVersion.fingerprint(
                3L,
                "11507a0e2f5e69d5dfa40a62a1bd7b6ee57e6bcd85c67c9b8431b36fff21c437"
            ),
            latest.version
        )
        assertEquals(3L, (loader.stateSnapshot() as CurrentPreviewContentVersionState.Ready).readerGeneration)
        loader.release()
    }

    @Test(timeout = 10_000)
    fun releaseDuringBlockingReadClosesStreamAndRejectsLateResult() = runBlocking {
        val stream = BlockingInputStream("late".encodeToByteArray(), ignoreClose = true)
        val loader = loader(CountingOpener { stream })
        loader.beginContentCycle(BOOK_A, 1L, 4L, null)
        val result = async(start = CoroutineStart.UNDISPATCHED) { loader.ensureCurrentVersion() }
        assertTrue(stream.readStarted.await(5, TimeUnit.SECONDS))

        loader.release()

        assertEquals(
            CurrentPreviewContentVersionFailureReason.CANCELLED,
            withTimeout(1_000) { result.await() }.diagnosticReason
        )
        assertTrue(stream.closed.get())
        firstFinish(stream)
        assertEquals(CurrentPreviewContentVersionState.Released, loader.stateSnapshot())
    }

    @Test(timeout = 10_000)
    fun failedCycleDoesNotRetryButNewCycleCanRetry() = runBlocking {
        val opener = SequenceOpener(listOf(null, ByteArrayInputStream("retry".encodeToByteArray())))
        val loader = loader(opener)
        loader.beginContentCycle(BOOK_A, 1L, 5L, null)

        val failed = loader.ensureCurrentVersion()
        val repeated = loader.ensureCurrentVersion()
        loader.beginContentCycle(BOOK_A, 2L, 5L, null)
        val retried = loader.ensureCurrentVersion()

        assertEquals(CurrentPreviewContentVersionFailureReason.STREAM_OPEN_FAILED, failed.diagnosticReason)
        assertSame(failed, repeated)
        assertTrue(retried.isReady)
        assertEquals(2, opener.opens.get())
        loader.release()
    }

    @Test(timeout = 10_000)
    fun fingerprintHexIsCanonicalLowercaseAnd64Characters() = runBlocking {
        val loader = loader(CountingOpener { ByteArrayInputStream("hex".encodeToByteArray()) })
        loader.beginContentCycle(BOOK_A, 1L, null, null)

        val version = loader.ensureCurrentVersion().version as PreviewContentVersion.Fingerprint

        assertEquals(64, version.sha256Hex.length)
        assertEquals(version.sha256Hex.lowercase(), version.sha256Hex)
        assertTrue(version.sha256Hex.matches(Regex("[0-9a-f]{64}")))
        loader.release()
    }

    @Test(timeout = 10_000)
    fun modifiedAtWithUnavailableSizeFallsBackToFingerprint() = runBlocking {
        val stream = CloseTrackingInputStream("fallback".encodeToByteArray())
        val opener = CountingOpener { stream }
        val loader = loader(opener)
        loader.beginContentCycle(BOOK_A, 1L, null, 1234L)

        val result = loader.ensureCurrentVersion()

        assertTrue(result.isReady)
        assertTrue(result.version is PreviewContentVersion.Fingerprint)
        assertEquals(8L, result.version.size)
        assertEquals(1, opener.opens.get())
        assertTrue(stream.closed.get())
        loader.release()
    }

    @Test(timeout = 10_000)
    fun unavailableMetadataFallsBackToFingerprintUsingActualSize() = runBlocking {
        val opener = CountingOpener { ByteArrayInputStream("actual".encodeToByteArray()) }
        val loader = loader(opener)
        loader.beginContentCycle(BOOK_A, 1L, null, null)

        val result = loader.ensureCurrentVersion()

        assertTrue(result.isReady)
        assertTrue(result.version is PreviewContentVersion.Fingerprint)
        assertEquals(6L, result.version.size)
        assertEquals(1, opener.opens.get())
        loader.release()
    }

    @Test(timeout = 10_000)
    fun zeroByteKnownMetadataUsesMetadataFastPath() = runBlocking {
        val opener = CountingOpener { ByteArrayInputStream(byteArrayOf()) }
        val loader = loader(opener)
        loader.beginContentCycle(BOOK_A, 1L, 0L, 1234L)

        val result = loader.ensureCurrentVersion()

        assertEquals(PreviewContentVersion.metadata(0L, 1234L), result.version)
        assertTrue(result.isReady)
        assertEquals(0, opener.opens.get())
        loader.release()
    }

    private fun loader(opener: PreviewContentInputStreamOpener): CurrentPreviewContentVersionLoader {
        return CurrentPreviewContentVersionLoader(
            ownerScope = thisScope,
            streamOpener = opener,
            fingerprintDispatcher = Dispatchers.IO
        )
    }

    private val thisScope
        get() = kotlinx.coroutines.CoroutineScope(Dispatchers.Default)

    private fun firstFinish(stream: BlockingInputStream) {
        stream.finishRead.countDown()
    }

    private class CountingOpener(
        private val stream: () -> InputStream?
    ) : PreviewContentInputStreamOpener {
        val opens = AtomicInteger()

        override fun open(bookUri: String): InputStream? {
            opens.incrementAndGet()
            return stream()
        }
    }

    private class SequenceOpener(
        streams: List<InputStream?>
    ) : PreviewContentInputStreamOpener {
        private val queue = ArrayDeque(streams)
        val opens = AtomicInteger()

        @Synchronized
        override fun open(bookUri: String): InputStream? {
            opens.incrementAndGet()
            return queue.removeFirst()
        }
    }

    private open class CloseTrackingInputStream(bytes: ByteArray) : ByteArrayInputStream(bytes) {
        val closed = AtomicBoolean(false)

        override fun close() {
            closed.set(true)
            super.close()
        }
    }

    private class ThrowingInputStream : InputStream() {
        val closed = AtomicBoolean(false)

        override fun read(): Int = throw IOException("read failed")

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            throw IOException("read failed")

        override fun close() {
            closed.set(true)
        }
    }

    private class BlockingInputStream(
        private val bytes: ByteArray,
        private val ignoreClose: Boolean = false
    ) : InputStream() {
        val readStarted = CountDownLatch(1)
        val finishRead = CountDownLatch(1)
        val closed = AtomicBoolean(false)
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
            if (closed.get() && !ignoreClose) throw IOException("stream closed")
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

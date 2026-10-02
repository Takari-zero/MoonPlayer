package com.shenghui.localvibe.feature.book

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.io.InputStream
import java.security.MessageDigest

internal fun interface PreviewContentInputStreamOpener {
    fun open(bookUri: String): InputStream?
}

internal enum class CurrentPreviewContentVersionFailureReason {
    NO_ACTIVE_CYCLE,
    STREAM_OPEN_FAILED,
    READ_FAILED,
    SIZE_CHANGED_DURING_READ,
    DIGEST_FAILED,
    CANCELLED,
    STALE
}

internal data class CurrentPreviewContentVersionResult(
    val version: PreviewContentVersion,
    val diagnosticReason: CurrentPreviewContentVersionFailureReason? = null
) {
    val isReady: Boolean
        get() = diagnosticReason == null && version != PreviewContentVersion.Unavailable
}

internal sealed interface CurrentPreviewContentVersionState {
    data object NotStarted : CurrentPreviewContentVersionState

    data class InFlight(
        val bookUri: String,
        val readerGeneration: Long
    ) : CurrentPreviewContentVersionState

    data class Ready(
        val bookUri: String,
        val readerGeneration: Long,
        val result: CurrentPreviewContentVersionResult
    ) : CurrentPreviewContentVersionState

    data class Failed(
        val bookUri: String,
        val readerGeneration: Long,
        val result: CurrentPreviewContentVersionResult
    ) : CurrentPreviewContentVersionState

    data object Released : CurrentPreviewContentVersionState
}

internal class CurrentPreviewContentVersionLoader(
    ownerScope: CoroutineScope,
    private val streamOpener: PreviewContentInputStreamOpener,
    private val fingerprintDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private data class ActiveCycle(
        val bookUri: String,
        val readerGeneration: Long,
        val expectedSize: Long?,
        val modifiedAt: Long?,
        val token: Any = Any()
    )

    private class FingerprintRequest(
        val cycle: ActiveCycle,
        val completion: CompletableDeferred<CurrentPreviewContentVersionResult> = CompletableDeferred()
    ) {
        var worker: Job? = null
        var stream: InputStream? = null
    }

    private val stateLock = Any()
    private val ownerJob = SupervisorJob(ownerScope.coroutineContext[Job])
    private val loaderScope = CoroutineScope(ownerScope.coroutineContext + ownerJob)
    private var activeCycle: ActiveCycle? = null
    private var activeRequest: FingerprintRequest? = null
    private var released = false
    private var state: CurrentPreviewContentVersionState = CurrentPreviewContentVersionState.NotStarted

    fun beginContentCycle(
        bookUri: String,
        readerGeneration: Long,
        expectedSize: Long?,
        modifiedAt: Long?
    ) {
        val normalizedSize = expectedSize?.takeIf { it >= 0L }
        val normalizedModifiedAt = modifiedAt?.takeIf { it > 0L }
        val cycle = ActiveCycle(
            bookUri = bookUri,
            readerGeneration = readerGeneration,
            expectedSize = normalizedSize,
            modifiedAt = normalizedModifiedAt
        )
        val initialState = if (normalizedSize != null && normalizedModifiedAt != null) {
            CurrentPreviewContentVersionState.Ready(
                bookUri = bookUri,
                readerGeneration = readerGeneration,
                result = CurrentPreviewContentVersionResult(
                    PreviewContentVersion.Metadata(normalizedSize, normalizedModifiedAt)
                )
            )
        } else {
            CurrentPreviewContentVersionState.NotStarted
        }
        val previousRequest = synchronized(stateLock) {
            check(!released) { "CurrentPreviewContentVersionLoader is released" }
            val previous = activeRequest
            activeCycle = cycle
            activeRequest = null
            state = initialState
            previous
        }
        previousRequest?.let {
            invalidateRequest(it, CurrentPreviewContentVersionFailureReason.STALE)
        }
    }

    suspend fun ensureCurrentVersion(): CurrentPreviewContentVersionResult {
        var requestToStart: FingerprintRequest? = null
        val completion = synchronized(stateLock) {
            when (val snapshot = state) {
                CurrentPreviewContentVersionState.Released -> return failed(
                    CurrentPreviewContentVersionFailureReason.CANCELLED
                )

                is CurrentPreviewContentVersionState.Ready -> return snapshot.result
                is CurrentPreviewContentVersionState.Failed -> return snapshot.result
                is CurrentPreviewContentVersionState.InFlight -> {
                    checkNotNull(activeRequest).completion
                }

                CurrentPreviewContentVersionState.NotStarted -> {
                    val cycle = activeCycle ?: return failed(
                        CurrentPreviewContentVersionFailureReason.NO_ACTIVE_CYCLE
                    )
                    FingerprintRequest(cycle).also { request ->
                        activeRequest = request
                        state = CurrentPreviewContentVersionState.InFlight(
                            bookUri = cycle.bookUri,
                            readerGeneration = cycle.readerGeneration
                        )
                        requestToStart = request
                    }.completion
                }
            }
        }
        requestToStart?.let(::startRequest)
        return completion.await()
    }

    fun stateSnapshot(): CurrentPreviewContentVersionState = synchronized(stateLock) { state }

    fun release() {
        val request = synchronized(stateLock) {
            if (released) return
            released = true
            activeCycle = null
            val detached = activeRequest
            activeRequest = null
            state = CurrentPreviewContentVersionState.Released
            detached
        }
        request?.let {
            invalidateRequest(it, CurrentPreviewContentVersionFailureReason.CANCELLED)
        }
        ownerJob.cancel()
    }

    private fun startRequest(request: FingerprintRequest) {
        val worker = loaderScope.launch(
            context = fingerprintDispatcher,
            start = CoroutineStart.LAZY
        ) {
            publishResult(request, loadFingerprint(request))
        }
        worker.invokeOnCompletion { error ->
            if (error != null) {
                publishResult(
                    request,
                    failed(CurrentPreviewContentVersionFailureReason.CANCELLED)
                )
            }
        }
        val shouldStart = synchronized(stateLock) {
            if (isActive(request)) {
                request.worker = worker
                true
            } else {
                false
            }
        }
        if (shouldStart) {
            worker.start()
        } else {
            worker.cancel()
        }
    }

    private suspend fun loadFingerprint(
        request: FingerprintRequest
    ): CurrentPreviewContentVersionResult {
        val digest = try {
            MessageDigest.getInstance("SHA-256")
        } catch (_: Throwable) {
            return failed(CurrentPreviewContentVersionFailureReason.DIGEST_FAILED)
        }
        currentCoroutineContext().ensureActive()
        val stream = try {
            streamOpener.open(request.cycle.bookUri)
        } catch (_: Throwable) {
            return failed(CurrentPreviewContentVersionFailureReason.STREAM_OPEN_FAILED)
        } ?: return failed(CurrentPreviewContentVersionFailureReason.STREAM_OPEN_FAILED)

        if (!registerStream(request, stream)) {
            runCatching { stream.close() }
            return failed(staleReason())
        }

        return try {
            var bytesRead = 0L
            stream.use { input ->
                val buffer = ByteArray(FINGERPRINT_BUFFER_SIZE_BYTES)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = input.read(buffer)
                    currentCoroutineContext().ensureActive()
                    if (read < 0) break
                    if (read == 0) continue
                    digest.update(buffer, 0, read)
                    bytesRead += read
                }
            }
            val expectedSize = request.cycle.expectedSize
            if (expectedSize != null && bytesRead != expectedSize) {
                failed(CurrentPreviewContentVersionFailureReason.SIZE_CHANGED_DURING_READ)
            } else {
                val version = PreviewContentVersion.fingerprint(
                    size = bytesRead,
                    sha256Hex = digest.digest().toLowerHex()
                )
                if (version is PreviewContentVersion.Fingerprint) {
                    CurrentPreviewContentVersionResult(version)
                } else {
                    failed(CurrentPreviewContentVersionFailureReason.DIGEST_FAILED)
                }
            }
        } catch (_: kotlinx.coroutines.CancellationException) {
            failed(staleReason())
        } catch (_: Throwable) {
            failed(CurrentPreviewContentVersionFailureReason.READ_FAILED)
        } finally {
            clearStream(request, stream)
        }
    }

    private fun publishResult(
        request: FingerprintRequest,
        result: CurrentPreviewContentVersionResult
    ) {
        val accepted = synchronized(stateLock) {
            if (!isActive(request)) {
                false
            } else {
                activeRequest = null
                state = if (result.isReady) {
                    CurrentPreviewContentVersionState.Ready(
                        bookUri = request.cycle.bookUri,
                        readerGeneration = request.cycle.readerGeneration,
                        result = result
                    )
                } else {
                    CurrentPreviewContentVersionState.Failed(
                        bookUri = request.cycle.bookUri,
                        readerGeneration = request.cycle.readerGeneration,
                        result = result
                    )
                }
                true
            }
        }
        request.completion.complete(if (accepted) result else failed(staleReason()))
    }

    private fun registerStream(request: FingerprintRequest, stream: InputStream): Boolean {
        return synchronized(stateLock) {
            if (isActive(request)) {
                request.stream = stream
                true
            } else {
                false
            }
        }
    }

    private fun clearStream(request: FingerprintRequest, stream: InputStream) {
        synchronized(stateLock) {
            if (request.stream === stream) {
                request.stream = null
            }
        }
    }

    private fun invalidateRequest(
        request: FingerprintRequest,
        reason: CurrentPreviewContentVersionFailureReason
    ) {
        val detached = synchronized(stateLock) {
            val worker = request.worker
            val stream = request.stream
            request.worker = null
            request.stream = null
            worker to stream
        }
        request.completion.complete(failed(reason))
        detached.first?.cancel()
        runCatching { detached.second?.close() }
    }

    private fun isActive(request: FingerprintRequest): Boolean {
        return !released &&
            activeRequest === request &&
            activeCycle?.token === request.cycle.token
    }

    private fun staleReason(): CurrentPreviewContentVersionFailureReason = synchronized(stateLock) {
        if (released) {
            CurrentPreviewContentVersionFailureReason.CANCELLED
        } else {
            CurrentPreviewContentVersionFailureReason.STALE
        }
    }

    private fun failed(
        reason: CurrentPreviewContentVersionFailureReason
    ): CurrentPreviewContentVersionResult {
        return CurrentPreviewContentVersionResult(
            version = PreviewContentVersion.Unavailable,
            diagnosticReason = reason
        )
    }

    private fun ByteArray.toLowerHex(): String {
        val result = CharArray(size * 2)
        forEachIndexed { index, byte ->
            val value = byte.toInt() and 0xFF
            result[index * 2] = LOWER_HEX[value ushr 4]
            result[index * 2 + 1] = LOWER_HEX[value and 0x0F]
        }
        return result.concatToString()
    }

    private companion object {
        const val FINGERPRINT_BUFFER_SIZE_BYTES = 64 * 1024
        val LOWER_HEX = "0123456789abcdef".toCharArray()
    }
}

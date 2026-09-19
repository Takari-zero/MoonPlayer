package com.shenghui.localvibe.core.tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

class StreamingPcmAudioPlayer(
    private val onStateChanged: (String) -> Unit = {}
) {
    private val lock = Any()
    private val audioOperationLock = Any()
    private var audioTrack: AudioTrack? = null
    private var activeFormat: PcmAudioFormat? = null
    private var startTimeMs: Long = 0L
    private var firstChunkWritten = false
    private var queuedFrames: Long = 0L
    private var writtenFrames: Long = 0L
    private val writerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val audioQueue = Channel<QueuedPcmChunk>(capacity = Channel.UNLIMITED)
    @Volatile
    private var currentSessionId: Long = 0L
    @Volatile
    private var stopped = true
    @Volatile
    private var paused = false
    @Volatile
    private var released = false

    init {
        writerScope.launch {
            writerLoop()
        }
    }

    fun start(format: PcmAudioFormat): Result<Unit> {
        return startSession(currentSessionId + 1L, format)
    }

    fun startSession(sessionId: Long, format: PcmAudioFormat): Result<Unit> = synchronized(audioOperationLock) {
        synchronized(lock) {
            runCatching {
                released = false
                currentSessionId = sessionId
                clearQueueLocked()
                logMainThreadErrorIfNeeded("prepared audio start")
                Log.i(BOOK_HOT_TTS_TAG, "audio queue start session=$sessionId")

                require(format.encoding == PcmAudioEncoding.PCM_16BIT) {
                    "Only PCM_16BIT is supported"
                }
                require(format.channelCount == 1) {
                    "Only mono PCM is supported"
                }
                require(format.sampleRate in SUPPORTED_SAMPLE_RATES) {
                    "Unsupported sampleRate=${format.sampleRate}"
                }

                if (audioTrack != null && activeFormat != format) {
                    releaseLocked()
                } else {
                    audioTrack?.let { track ->
                        runCatching {
                            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                                track.pause()
                            }
                            track.flush()
                        }.onFailure { error ->
                            Log.w(TAG, "flush old track failed", error)
                            releaseLocked()
                        }
                    }
                }

                val channelMask = AudioFormat.CHANNEL_OUT_MONO
                val encoding = AudioFormat.ENCODING_PCM_16BIT
                val track = audioTrack ?: run {
                    val minBufferSize = AudioTrack.getMinBufferSize(
                        format.sampleRate,
                        channelMask,
                        encoding
                    )
                    require(minBufferSize > 0) {
                        "AudioTrack.getMinBufferSize failed: $minBufferSize"
                    }

                    val bufferSize = max(minBufferSize, format.sampleRate * BYTES_PER_MONO_16BIT_SAMPLE / 2)
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(encoding)
                                .setSampleRate(format.sampleRate)
                                .setChannelMask(channelMask)
                                .build()
                        )
                        .setBufferSizeInBytes(bufferSize)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()
                        .also { newTrack ->
                            Log.d(TAG, "sampleRate=${format.sampleRate}")
                            Log.d(TAG, "bufferSize=$bufferSize minBufferSize=$minBufferSize")
                            Log.d(TAG, "AudioTrack state=${newTrack.state}")
                        }
                }


                require(track.state == AudioTrack.STATE_INITIALIZED) {
                    "AudioTrack initialization failed state=${track.state}"
                }

                track.play()
                Log.d(TAG, "AudioTrack playState=${track.playState}")

                audioTrack = track
                activeFormat = format
                startTimeMs = System.currentTimeMillis()
                firstChunkWritten = false
                queuedFrames = 0L
                writtenFrames = 0L
                stopped = false
                paused = false
                onStateChanged("started")
            }.onFailure { error ->
                Log.e(TAG, "start failed", error)
                releaseLocked()
                onStateChanged("error:${error.message}")
            }
        }
    }

    fun write(chunk: PcmAudioChunk): Result<Int> {
        return write(currentSessionId, chunk)
    }

    fun write(sessionId: Long, chunk: PcmAudioChunk): Result<Int> {
        if (sessionId != currentSessionId) {
            Log.i(BOOK_HOT_TTS_TAG, "audio queue drop old session=$sessionId current=$currentSessionId")
            return Result.success(0)
        }
        return runCatching {
            require(!released) { "Audio player is released" }
            require(!stopped) { "Audio player is stopped" }
            synchronized(lock) {
                val format = requireNotNull(activeFormat) { "No active audio format" }
                require(chunk.format == format) {
                    "Chunk format changed from $format to ${chunk.format}"
                }
                queuedFrames += PcmPlaybackCompletion.frameCount(
                    bytes = chunk.data.size,
                    format = chunk.format
                )
            }
            Log.i(BOOK_HOT_TTS_TAG, "audio queue enqueue session=$sessionId size=${chunk.data.size}")
            val sendResult = audioQueue.trySend(QueuedPcmChunk(sessionId, chunk))
            if (sendResult.isFailure) {
                synchronized(lock) {
                    queuedFrames -= PcmPlaybackCompletion.frameCount(
                        bytes = chunk.data.size,
                        format = chunk.format
                    )
                }
            }
            sendResult.getOrThrow()
            chunk.data.size
        }.onFailure { error ->
            Log.e(TAG, "enqueue failed", error)
            onStateChanged("error:${error.message}")
        }
    }

    private fun writeNow(queued: QueuedPcmChunk): Result<Int> {
        val sessionId = queued.sessionId
        val chunk = queued.chunk
        if (sessionId != currentSessionId) {
            Log.i(BOOK_HOT_TTS_TAG, "audio queue drop old session=$sessionId current=$currentSessionId")
            return Result.success(0)
        }
        try {
            logMainThreadErrorIfNeeded("prepared audio write")
            if (chunk.data.isEmpty()) {
                Log.d(TAG, "write empty chunk isFinal=${chunk.isFinal}")
                return Result.success(0)
            }

            var totalWritten = 0
            var offset = 0
            while (offset < chunk.data.size) {
                if (sessionId != currentSessionId || stopped || released) {
                    Log.i(BOOK_HOT_TTS_TAG, "audio queue write aborted stale session=$sessionId current=$currentSessionId")
                    return Result.success(totalWritten)
                }
                if (paused) {
                    Thread.sleep(PAUSED_WRITE_WAIT_MS)
                    continue
                }

                val byteCount = minOf(MAX_WRITE_BYTES, chunk.data.size - offset)
                val writeResult = synchronized(audioOperationLock) {
                    synchronized(lock) {
                        if (sessionId != currentSessionId || stopped || released || paused) {
                            return@synchronized 0
                        }
                        val track = requireNotNull(audioTrack) { "AudioTrack is not started" }
                        val format = requireNotNull(activeFormat) { "No active audio format" }
                        require(chunk.format == format) {
                            "Chunk format changed from $format to ${chunk.format}"
                        }
                        Log.i(
                            BOOK_HOT_TTS_TAG,
                            "audio queue write session=$sessionId main=${isMainThread()} size=$byteCount"
                        )
                        track.write(chunk.data, offset, byteCount, AudioTrack.WRITE_NON_BLOCKING)
                    }
                }

                if (sessionId != currentSessionId || stopped || released) {
                    Log.i(BOOK_HOT_TTS_TAG, "audio queue write result ignored stale session=$sessionId current=$currentSessionId")
                    return Result.success(totalWritten)
                }
                Log.d(TAG, "write bytes=$writeResult requested=$byteCount")
                if (writeResult < 0) {
                    throw IllegalStateException("AudioTrack.write failed: $writeResult")
                }
                if (writeResult == 0) {
                    Thread.sleep(NON_BLOCKING_WRITE_RETRY_MS)
                    continue
                }

                totalWritten += writeResult
                offset += writeResult
                synchronized(lock) {
                    writtenFrames += PcmPlaybackCompletion.frameCount(
                        bytes = writeResult,
                        format = chunk.format
                    )
                }

                if (!firstChunkWritten) {
                    firstChunkWritten = true
                    val firstChunkToPlayMs = System.currentTimeMillis() - startTimeMs
                    Log.d(TAG, "firstChunkToPlayMs=$firstChunkToPlayMs")
                    onStateChanged("first_chunk")
                }
            }

            if (chunk.isFinal) {
                Log.d(TAG, "final chunk written")
                onStateChanged("final_chunk")
            }

            return Result.success(totalWritten)
        } catch (error: Throwable) {
            Log.e(TAG, "write failed", error)
            onStateChanged("error:${error.message}")
            return Result.failure<Int>(error)
        }
    }

    suspend fun awaitSessionPlaybackComplete(sessionId: Long): Boolean {
        val startedAt = System.currentTimeMillis()
        while (true) {
            val track = synchronized(lock) {
                if (sessionId != currentSessionId || stopped || released) {
                    return false
                }
                audioTrack ?: return false
            }
            val playbackHeadFrames = runCatching {
                track.playbackHeadPosition.toLong().coerceAtLeast(0L)
            }.getOrDefault(0L)
            val snapshot = synchronized(lock) {
                PlaybackSnapshot(
                    queuedFrames = queuedFrames,
                    writtenFrames = writtenFrames,
                    playbackHeadFrames = playbackHeadFrames,
                    paused = paused
                )
            }
            if (PcmPlaybackCompletion.isComplete(
                    queuedFrames = snapshot.queuedFrames,
                    writtenFrames = snapshot.writtenFrames,
                    playbackHeadFrames = snapshot.playbackHeadFrames
                )
            ) {
                delay(PLAYBACK_COMPLETE_SETTLE_MS)
                return synchronized(lock) {
                    sessionId == currentSessionId && !stopped && !released
                }
            }
            if (!snapshot.paused && System.currentTimeMillis() - startedAt > PLAYBACK_COMPLETE_TIMEOUT_MS) {
                Log.w(
                    TAG,
                    "await playback timeout session=$sessionId queued=${snapshot.queuedFrames} " +
                        "written=${snapshot.writtenFrames} head=${snapshot.playbackHeadFrames}"
                )
                return false
            }
            delay(if (snapshot.paused) PAUSED_WRITE_WAIT_MS else PLAYBACK_COMPLETE_POLL_MS)
        }
    }

    fun stop() = synchronized(audioOperationLock) {
        synchronized(lock) {
            Log.d(TAG, "stop")
            currentSessionId += 1L
            clearQueueLocked()
            Log.i(BOOK_HOT_TTS_TAG, "audio queue stop requested session=$currentSessionId")
            Log.i(BOOK_HOT_TTS_TAG, "prepared audio stop start main=${isMainThread()}")
            logMainThreadErrorIfNeeded("prepared audio stop")
            stopped = true
            paused = false
            audioTrack?.let { track ->
                runCatching {
                    if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        track.pause()
                    }
                    track.flush()
                    track.stop()
                }.onFailure { error ->
                    Log.w(TAG, "stop failed", error)
                }
            }
            onStateChanged("stopped")
        }
    }

    fun pause() = synchronized(audioOperationLock) {
        synchronized(lock) {
            Log.d(TAG, "pause")
            paused = true
            audioTrack?.let { track ->
                runCatching {
                    if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        track.pause()
                    }
                    onStateChanged("paused")
                }.onFailure { error ->
                    Log.w(TAG, "pause failed", error)
                    onStateChanged("error:${error.message}")
                }
            }
        }
    }

    fun resume() = synchronized(audioOperationLock) {
        synchronized(lock) {
            Log.d(TAG, "resume")
            val track = audioTrack
            if (track == null || stopped) {
                onStateChanged("error:AudioTrack is not paused")
                return@synchronized Result.failure(IllegalStateException("AudioTrack is not paused"))
            }
            runCatching {
                paused = false
                track.play()
                Log.d(TAG, "AudioTrack playState=${track.playState}")
                onStateChanged("resumed")
            }.onFailure { error ->
                Log.w(TAG, "resume failed", error)
                onStateChanged("error:${error.message}")
            }
        }
    }

    fun release() = synchronized(audioOperationLock) {
        synchronized(lock) {
            Log.d(TAG, "release")
            Log.i(BOOK_HOT_TTS_TAG, "prepared audio release start main=${isMainThread()}")
            logMainThreadErrorIfNeeded("prepared audio release")
            released = true
            currentSessionId += 1L
            clearQueueLocked()
            releaseLocked()
            writerScope.cancel()
            Log.i(BOOK_HOT_TTS_TAG, "prepared audio release done main=${isMainThread()}")
            Log.i(BOOK_HOT_TTS_TAG, "audio queue release done main=${isMainThread()}")
            onStateChanged("released")
        }
    }

    private suspend fun writerLoop() {
        for (queued in audioQueue) {
            if (queued.sessionId != currentSessionId) {
                Log.i(BOOK_HOT_TTS_TAG, "audio queue drop old session=${queued.sessionId} current=$currentSessionId")
                continue
            }
            writeNow(queued)
        }
    }

    private fun clearQueueLocked() {
        var dropped = 0
        while (true) {
            val item = audioQueue.tryReceive().getOrNull() ?: break
            dropped += 1
            Log.i(BOOK_HOT_TTS_TAG, "audio queue drop old session=${item.sessionId} current=$currentSessionId")
        }
        Log.i(BOOK_HOT_TTS_TAG, "audio queue clear old session=$currentSessionId dropped=$dropped")
    }

    private fun releaseLocked() {
        stopped = true
        paused = false
        audioTrack?.let { track ->
            runCatching {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.pause()
                }
                track.flush()
                track.release()
            }.onFailure { error ->
                Log.w(TAG, "release failed", error)
            }
        }
        audioTrack = null
        activeFormat = null
        firstChunkWritten = false
        startTimeMs = 0L
        queuedFrames = 0L
        writtenFrames = 0L
    }

    private fun isMainThread(): Boolean {
        return Looper.myLooper() == Looper.getMainLooper()
    }

    private fun logMainThreadErrorIfNeeded(operation: String) {
        if (isMainThread()) {
            Log.e(BOOK_HOT_TTS_TAG, "ERROR $operation on main thread")
        }
    }

    private data class QueuedPcmChunk(
        val sessionId: Long,
        val chunk: PcmAudioChunk
    )

    private data class PlaybackSnapshot(
        val queuedFrames: Long,
        val writtenFrames: Long,
        val playbackHeadFrames: Long,
        val paused: Boolean
    )

    companion object {
        private const val TAG = "StreamingPcmAudioPlayer"
        private const val BOOK_HOT_TTS_TAG = "BookListenHot"
        private const val BYTES_PER_MONO_16BIT_SAMPLE = 2
        private const val MAX_WRITE_BYTES = 2048
        private const val PAUSED_WRITE_WAIT_MS = 20L
        private const val NON_BLOCKING_WRITE_RETRY_MS = 4L
        private const val PLAYBACK_COMPLETE_POLL_MS = 40L
        private const val PLAYBACK_COMPLETE_SETTLE_MS = 80L
        private const val PLAYBACK_COMPLETE_TIMEOUT_MS = 60_000L
        private val SUPPORTED_SAMPLE_RATES = setOf(8000, 16000, 24000, 44100)
    }
}

internal object PcmPlaybackCompletion {
    fun frameCount(bytes: Int, format: PcmAudioFormat): Long {
        if (bytes <= 0) return 0L
        val bytesPerSample = when (format.encoding) {
            PcmAudioEncoding.PCM_16BIT -> 2
        }
        val bytesPerFrame = (bytesPerSample * format.channelCount).coerceAtLeast(1)
        return bytes / bytesPerFrame.toLong()
    }

    fun isComplete(
        queuedFrames: Long,
        writtenFrames: Long,
        playbackHeadFrames: Long
    ): Boolean {
        if (queuedFrames <= 0L) return false
        return writtenFrames >= queuedFrames && playbackHeadFrames >= queuedFrames
    }
}

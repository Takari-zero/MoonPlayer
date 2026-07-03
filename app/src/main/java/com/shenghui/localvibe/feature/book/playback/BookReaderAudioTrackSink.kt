package com.shenghui.localvibe.feature.book.playback

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

class BookReaderAudioTrackSink : BookReaderAudioSink {
    private val lock = Any()

    @Volatile
    private var activeSessionId: Long = 0L
    private var audioTrack: AudioTrack? = null

    override suspend fun playPcm(
        sessionId: Long,
        sampleRate: Int,
        pcm16: ByteArray,
    ) = withContext(Dispatchers.IO) {
        val startedAtMs = SystemClock.elapsedRealtime()
        val durationMs = calculatePcmDurationMs(pcm16.size, sampleRate)
        Log.d(TAG, "AudioTrackSink playPcm start sessionId=$sessionId bytes=${pcm16.size} sampleRate=$sampleRate")
        val track = synchronized(lock) {
            if (sessionId < activeSessionId) {
                Log.d(TAG, "AudioTrackSink playPcm cancelled reason=stale_session sessionId=$sessionId active=$activeSessionId")
                return@withContext
            }

            releaseTrackLocked(sessionId = activeSessionId)
            activeSessionId = sessionId
            createTrack(sampleRate).also { created ->
                audioTrack = created
                Log.d(TAG, "AudioTrackSink create sessionId=$sessionId bufferSize=${created.bufferSizeInFrames} sampleRate=$sampleRate")
            }
        }

        if (sessionId != activeSessionId) {
            Log.d(TAG, "AudioTrackSink playPcm cancelled reason=stale_session sessionId=$sessionId active=$activeSessionId")
            return@withContext
        }

        runCatching { track.play() }
            .onSuccess { Log.d(TAG, "AudioTrackSink play sessionId=$sessionId") }
            .onFailure { error -> Log.w(TAG, "AudioTrackSink play failed sessionId=$sessionId", error) }

        var offset = 0
        val chunkSize = DEFAULT_WRITE_CHUNK_BYTES
        var lastProgressAtMs = SystemClock.elapsedRealtime()
        while (offset < pcm16.size) {
            if (sessionId != activeSessionId) {
                Log.d(TAG, "AudioTrackSink drop old pcm sessionId=$sessionId active=$activeSessionId bytes=${pcm16.size - offset}")
                return@withContext
            }

            val size = min(chunkSize, pcm16.size - offset)
            val onMainThread = Looper.myLooper() == Looper.getMainLooper()
            val buffer = ByteBuffer.allocateDirect(size)
            buffer.put(pcm16, offset, size)
            buffer.flip()
            Log.d(TAG, "AudioTrackSink write attempt sessionId=$sessionId offset=$offset bytes=$size main=$onMainThread")
            val written = track.write(buffer, size, AudioTrack.WRITE_NON_BLOCKING)
            Log.d(TAG, "AudioTrackSink write result sessionId=$sessionId written=$written offset=$offset")
            if (written < 0) {
                Log.w(TAG, "AudioTrackSink write failed sessionId=$sessionId result=$written")
                releaseSessionTrack(sessionId)
                return@withContext
            }
            if (written == 0) {
                delay(NON_BLOCKING_WRITE_RETRY_DELAY_MS)
                val now = SystemClock.elapsedRealtime()
                if (now - lastProgressAtMs > WRITE_NO_PROGRESS_TIMEOUT_MS) {
                    Log.w(TAG, "AudioTrackSink write timeout sessionId=$sessionId offset=$offset totalBytes=${pcm16.size}")
                    releaseSessionTrack(sessionId)
                    return@withContext
                }
                continue
            }
            offset += written
            lastProgressAtMs = SystemClock.elapsedRealtime()
            Log.d(TAG, "AudioTrackSink write complete sessionId=$sessionId writtenBytes=$written")
        }

        if (sessionId != activeSessionId) {
            Log.d(TAG, "AudioTrackSink playPcm cancelled reason=stale_session sessionId=$sessionId active=$activeSessionId")
            return@withContext
        }

        val elapsedMs = SystemClock.elapsedRealtime() - startedAtMs
        val remainingDrainMs = (durationMs - elapsedMs).coerceAtLeast(1L)
        Log.d(TAG, "AudioTrackSink wait playback drain start sessionId=$sessionId durationMs=$remainingDrainMs")
        delay(remainingDrainMs)

        if (sessionId != activeSessionId) {
            Log.d(TAG, "AudioTrackSink playPcm cancelled reason=stale_session sessionId=$sessionId active=$activeSessionId")
            return@withContext
        }

        Log.d(TAG, "AudioTrackSink wait playback drain done sessionId=$sessionId")
        releaseSessionTrack(sessionId)
        Log.d(TAG, "AudioTrackSink playPcm return sessionId=$sessionId")
    }

    override fun pause() {
        val sessionId = activeSessionId
        synchronized(lock) {
            runCatching { audioTrack?.pause() }
                .onFailure { error -> Log.w(TAG, "AudioTrackSink pause failed sessionId=$sessionId", error) }
        }
        Log.d(TAG, "AudioTrackSink pause sessionId=$sessionId")
    }

    override fun resume() {
        val sessionId = activeSessionId
        synchronized(lock) {
            runCatching { audioTrack?.play() }
                .onFailure { error -> Log.w(TAG, "AudioTrackSink resume failed sessionId=$sessionId", error) }
        }
        Log.d(TAG, "AudioTrackSink resume sessionId=$sessionId")
    }

    override fun stop(reason: String) {
        val stoppedSessionId = synchronized(lock) {
            activeSessionId += 1
            val stopped = activeSessionId
            runCatching {
                audioTrack?.pause()
                audioTrack?.flush()
            }.onFailure { error -> Log.w(TAG, "AudioTrackSink stop failed sessionId=$stopped", error) }
            stopped
        }
        Log.d(TAG, "AudioTrackSink stop reason=$reason sessionId=$stoppedSessionId")
    }

    override fun release() {
        synchronized(lock) {
            activeSessionId += 1
            releaseTrackLocked(sessionId = activeSessionId)
        }
        Log.d(TAG, "AudioTrackSink release")
    }

    private fun createTrack(sampleRate: Int): AudioTrack {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufferSize = max(minBufferSize, sampleRate / 2)
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private fun releaseSessionTrack(sessionId: Long) {
        synchronized(lock) {
            if (sessionId == activeSessionId) {
                releaseTrackLocked(sessionId)
            }
        }
    }

    private fun releaseTrackLocked(sessionId: Long) {
        runCatching {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        }.onFailure { error -> Log.w(TAG, "AudioTrackSink release track failed", error) }
        audioTrack = null
        Log.d(TAG, "AudioTrackSink release sessionId=$sessionId")
    }

    companion object {
        private const val TAG = "LV_BOOK_PLAYBACK"
        private const val DEFAULT_WRITE_CHUNK_BYTES = 4096
        private const val NON_BLOCKING_WRITE_RETRY_DELAY_MS = 5L
        private const val WRITE_NO_PROGRESS_TIMEOUT_MS = 1500L

        internal fun calculatePcmDurationMs(pcmByteCount: Int, sampleRate: Int): Long {
            if (sampleRate <= 0 || pcmByteCount <= 0) return 1L
            val frameCount = pcmByteCount / 2L
            return (frameCount * 1000L / sampleRate).coerceAtLeast(1L)
        }
    }
}

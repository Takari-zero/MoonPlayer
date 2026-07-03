package com.shenghui.localvibe.core.tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.math.max

class StreamingPcmAudioPlayer(
    private val onStateChanged: (String) -> Unit = {}
) {
    private val lock = Any()
    private var audioTrack: AudioTrack? = null
    private var activeFormat: PcmAudioFormat? = null
    private var startTimeMs: Long = 0L
    private var firstChunkWritten = false
    @Volatile
    private var stopped = true
    @Volatile
    private var paused = false

    fun start(format: PcmAudioFormat): Result<Unit> = synchronized(lock) {
        runCatching {
            releaseLocked()

            require(format.encoding == PcmAudioEncoding.PCM_16BIT) {
                "Only PCM_16BIT is supported"
            }
            require(format.channelCount == 1) {
                "Only mono PCM is supported"
            }
            require(format.sampleRate in SUPPORTED_SAMPLE_RATES) {
                "Unsupported sampleRate=${format.sampleRate}"
            }

            val channelMask = AudioFormat.CHANNEL_OUT_MONO
            val encoding = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioTrack.getMinBufferSize(
                format.sampleRate,
                channelMask,
                encoding
            )
            require(minBufferSize > 0) {
                "AudioTrack.getMinBufferSize failed: $minBufferSize"
            }

            val bufferSize = max(minBufferSize, format.sampleRate * BYTES_PER_MONO_16BIT_SAMPLE / 2)
            val track = AudioTrack.Builder()
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

            Log.d(TAG, "sampleRate=${format.sampleRate}")
            Log.d(TAG, "bufferSize=$bufferSize minBufferSize=$minBufferSize")
            Log.d(TAG, "AudioTrack state=${track.state}")

            require(track.state == AudioTrack.STATE_INITIALIZED) {
                "AudioTrack initialization failed state=${track.state}"
            }

            track.play()
            Log.d(TAG, "AudioTrack playState=${track.playState}")

            audioTrack = track
            activeFormat = format
            startTimeMs = System.currentTimeMillis()
            firstChunkWritten = false
            stopped = false
            paused = false
            onStateChanged("started")
        }.onFailure { error ->
            Log.e(TAG, "start failed", error)
            releaseLocked()
            onStateChanged("error:${error.message}")
        }
    }

    fun write(chunk: PcmAudioChunk): Result<Int> {
        val track: AudioTrack
        val format: PcmAudioFormat
        synchronized(lock) {
            if (stopped) {
                Log.d(TAG, "write ignored because player is stopped")
                return Result.success(0)
            }

            track = requireNotNull(audioTrack) { "AudioTrack is not started" }
            format = requireNotNull(activeFormat) { "No active audio format" }
            require(chunk.format == format) {
                "Chunk format changed from $format to ${chunk.format}"
            }
        }

        return runCatching {
            if (chunk.data.isEmpty()) {
                Log.d(TAG, "write empty chunk isFinal=${chunk.isFinal}")
                return Result.success(0)
            }

            var totalWritten = 0
            var offset = 0
            while (offset < chunk.data.size) {
                if (stopped) {
                    Log.d(TAG, "write stopped totalWritten=$totalWritten requested=${chunk.data.size}")
                    return Result.success(totalWritten)
                }
                while (paused && !stopped) {
                    Thread.sleep(PAUSED_WRITE_WAIT_MS)
                }
                if (stopped) {
                    Log.d(TAG, "write stopped after pause totalWritten=$totalWritten requested=${chunk.data.size}")
                    return Result.success(totalWritten)
                }

                val byteCount = minOf(MAX_WRITE_BYTES, chunk.data.size - offset)
                val written = track.write(chunk.data, offset, byteCount)
                Log.d(TAG, "write bytes=$written requested=$byteCount")
                require(written > 0) {
                    "AudioTrack.write failed: $written"
                }

                totalWritten += written
                offset += written

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

            totalWritten
        }.onFailure { error ->
            Log.e(TAG, "write failed", error)
            onStateChanged("error:${error.message}")
        }
    }

    fun stop() = synchronized(lock) {
        Log.d(TAG, "stop")
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

    fun pause() = synchronized(lock) {
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

    fun resume() = synchronized(lock) {
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

    fun release() = synchronized(lock) {
        Log.d(TAG, "release")
        releaseLocked()
        onStateChanged("released")
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
    }

    companion object {
        private const val TAG = "StreamingPcmAudioPlayer"
        private const val BYTES_PER_MONO_16BIT_SAMPLE = 2
        private const val MAX_WRITE_BYTES = 2048
        private const val PAUSED_WRITE_WAIT_MS = 20L
        private val SUPPORTED_SAMPLE_RATES = setOf(8000, 16000, 24000, 44100)
    }
}

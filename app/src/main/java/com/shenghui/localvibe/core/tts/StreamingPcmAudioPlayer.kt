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
    private var stopped = true

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
            onStateChanged("started")
        }.onFailure { error ->
            Log.e(TAG, "start failed", error)
            releaseLocked()
            onStateChanged("error:${error.message}")
        }
    }

    fun write(chunk: PcmAudioChunk): Result<Int> = synchronized(lock) {
        runCatching {
            if (stopped) {
                Log.d(TAG, "write ignored because player is stopped")
                return Result.success(0)
            }

            val track = requireNotNull(audioTrack) { "AudioTrack is not started" }
            val format = requireNotNull(activeFormat) { "No active audio format" }
            require(chunk.format == format) {
                "Chunk format changed from $format to ${chunk.format}"
            }

            if (chunk.data.isEmpty()) {
                Log.d(TAG, "write empty chunk isFinal=${chunk.isFinal}")
                return Result.success(0)
            }

            val written = track.write(chunk.data, 0, chunk.data.size)
            Log.d(TAG, "write bytes=$written requested=${chunk.data.size}")
            require(written > 0) {
                "AudioTrack.write failed: $written"
            }

            if (!firstChunkWritten) {
                firstChunkWritten = true
                val firstChunkToPlayMs = System.currentTimeMillis() - startTimeMs
                Log.d(TAG, "firstChunkToPlayMs=$firstChunkToPlayMs")
                onStateChanged("first_chunk")
            }

            if (chunk.isFinal) {
                Log.d(TAG, "final chunk written")
                onStateChanged("final_chunk")
            }

            written
        }.onFailure { error ->
            Log.e(TAG, "write failed", error)
            onStateChanged("error:${error.message}")
        }
    }

    fun stop() = synchronized(lock) {
        Log.d(TAG, "stop")
        stopped = true
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

    fun release() = synchronized(lock) {
        Log.d(TAG, "release")
        releaseLocked()
        onStateChanged("released")
    }

    private fun releaseLocked() {
        stopped = true
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
        private val SUPPORTED_SAMPLE_RATES = setOf(8000, 16000, 24000, 44100)
    }
}

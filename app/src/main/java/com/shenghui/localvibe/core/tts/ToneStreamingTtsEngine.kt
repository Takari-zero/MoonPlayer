package com.shenghui.localvibe.core.tts

import android.util.Log
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

class ToneStreamingTtsEngine : StreamingTtsEngine {
    override val name: String = "ToneStreamingTtsEngine"
    override var isReady: Boolean = false
        private set

    @Volatile
    private var stopRequested = false

    override suspend fun initialize(): Result<Unit> {
        isReady = true
        stopRequested = false
        Log.d(TAG, "initialize success")
        return Result.success(Unit)
    }

    override suspend fun speak(
        text: String,
        params: StreamingTtsParams,
        onStart: () -> Unit,
        onChunk: suspend (PcmAudioChunk) -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ): StreamingTtsResult {
        if (!isReady) {
            val result = initialize()
            if (result.isFailure) {
                val message = result.exceptionOrNull()?.message ?: "Tone engine init failed"
                onError(message)
                return StreamingTtsResult.Error(message)
            }
        }

        stopRequested = false
        onStart()
        Log.d(TAG, "speak start textLength=${text.length} speed=${params.speed}")

        val format = PcmAudioFormat(
            sampleRate = SAMPLE_RATE,
            channelCount = 1,
            encoding = PcmAudioEncoding.PCM_16BIT
        )
        val totalChunks = DURATION_MS / CHUNK_MS

        for (chunkIndex in 0 until totalChunks) {
            if (stopRequested) {
                Log.d(TAG, "speak stopped at chunk=$chunkIndex")
                return StreamingTtsResult.Stopped
            }

            val pcm = makeToneChunk(chunkIndex)
            onChunk(
                PcmAudioChunk(
                    data = pcm,
                    format = format,
                    isFinal = chunkIndex == totalChunks - 1
                )
            )
            delay(CHUNK_MS.toLong())
        }

        onDone()
        Log.d(TAG, "speak done")
        return StreamingTtsResult.Success
    }

    override fun stop() {
        stopRequested = true
        Log.d(TAG, "stop")
    }

    override fun release() {
        stopRequested = true
        isReady = false
        Log.d(TAG, "release")
    }

    private fun makeToneChunk(chunkIndex: Int): ByteArray {
        val sampleCount = SAMPLE_RATE * CHUNK_MS / 1000
        val bytes = ByteArray(sampleCount * BYTES_PER_SAMPLE)
        val startSample = chunkIndex * sampleCount

        for (i in 0 until sampleCount) {
            val sample = sin(2.0 * PI * FREQUENCY_HZ * (startSample + i) / SAMPLE_RATE)
            val shortValue = (sample * Short.MAX_VALUE * AMPLITUDE).roundToInt().toShort()
            val byteIndex = i * BYTES_PER_SAMPLE
            bytes[byteIndex] = (shortValue.toInt() and 0xFF).toByte()
            bytes[byteIndex + 1] = ((shortValue.toInt() shr 8) and 0xFF).toByte()
        }

        return bytes
    }

    companion object {
        private const val TAG = "ToneStreamingTtsEngine"
        private const val SAMPLE_RATE = 16000
        private const val FREQUENCY_HZ = 440
        private const val DURATION_MS = 1000
        private const val CHUNK_MS = 100
        private const val BYTES_PER_SAMPLE = 2
        private const val AMPLITUDE = 0.35
    }
}

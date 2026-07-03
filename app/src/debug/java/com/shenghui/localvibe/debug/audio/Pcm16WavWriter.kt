package com.shenghui.localvibe.debug.audio

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

object Pcm16WavWriter {
    fun writeMono16(file: File, sampleRate: Int, pcm16: ByteArray) {
        file.parentFile?.mkdirs()
        file.outputStream().use { output ->
            output.write(header(sampleRate, pcm16.size))
            output.write(pcm16)
        }
    }

    private fun header(sampleRate: Int, dataSize: Int): ByteArray {
        val byteRate = sampleRate * CHANNELS * BYTES_PER_SAMPLE
        val blockAlign = CHANNELS * BYTES_PER_SAMPLE
        return ByteBuffer.allocate(WAV_HEADER_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putAscii("RIFF")
            .putInt(WAV_HEADER_BYTES - 8 + dataSize)
            .putAscii("WAVE")
            .putAscii("fmt ")
            .putInt(16)
            .putShort(1.toShort())
            .putShort(CHANNELS.toShort())
            .putInt(sampleRate)
            .putInt(byteRate)
            .putShort(blockAlign.toShort())
            .putShort(16)
            .putAscii("data")
            .putInt(dataSize)
            .array()
    }

    private fun ByteBuffer.putAscii(value: String): ByteBuffer {
        put(value.toByteArray(Charsets.US_ASCII))
        return this
    }

    private const val CHANNELS = 1
    private const val BYTES_PER_SAMPLE = 2
    private const val WAV_HEADER_BYTES = 44
}


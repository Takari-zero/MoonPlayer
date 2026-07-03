package com.benjaminwan.chinesettstflite.tts

import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MBMelGan(file: File) : BaseInference(file) {
    override val interpreter: Interpreter = Interpreter(file, options)
    private var reusableOutputBuffer: ByteBuffer? = null

    init {
        printTensorInfo(LOG_TAG)
    }

    fun getAudio(input: TensorBuffer): FloatArray {
        interpreter.resizeInput(0, input.shape)
        interpreter.allocateTensors()

        val melFrames = detectMelFrameCount(input)
        val estimatedSamples = (melFrames * MELGAN_HOP_SIZE) + MELGAN_OUTPUT_PADDING_SAMPLES
        val estimatedBytes = (estimatedSamples * Float.SIZE_BYTES).coerceAtLeast(MIN_MELGAN_OUTPUT_BYTES)
        val tensorBytesBeforeRun = interpreter.getOutputTensor(0).numBytes()
        var outputBuffer = ensureOutputBuffer(estimatedBytes)
        Log.i(
            LOG_TAG,
            "MBMelGAN output estimatedBytes=$estimatedBytes tensorBytesBeforeRun=$tensorBytesBeforeRun " +
                "bufferCapacity=${outputBuffer.capacity()} expanded=${outputBuffer.capacity() == estimatedBytes} melFrames=$melFrames"
        )

        var retry = 0
        while (true) {
            outputBuffer.clear()
            try {
                interpreter.run(input.buffer, outputBuffer)
                break
            } catch (error: IllegalArgumentException) {
                val requiredBytes = parseRequiredBytes(error.message)
                if (requiredBytes == null || retry >= MAX_MELGAN_RETRY) {
                    throw error
                }
                val oldCapacity = outputBuffer.capacity()
                val newCapacity = maxOf(requiredBytes, oldCapacity * 2, MIN_MELGAN_OUTPUT_BYTES)
                Log.i(
                    LOG_TAG,
                    "MBMelGAN retry expand requiredBytes=$requiredBytes oldCapacity=$oldCapacity " +
                        "newCapacity=$newCapacity retry=${retry + 1}"
                )
                outputBuffer = allocateOutputBuffer(newCapacity)
                reusableOutputBuffer = outputBuffer
                retry++
            }
        }

        val tensor = interpreter.getOutputTensor(0)
        val tensorBytesAfterRun = tensor.numBytes()
        val shapeBytesAfterRun = tensor.shape().fold(1) { acc, value -> acc * value.coerceAtLeast(1) } * Float.SIZE_BYTES
        val actualBytesAfterRun = when {
            tensorBytesAfterRun > Float.SIZE_BYTES -> tensorBytesAfterRun
            shapeBytesAfterRun > Float.SIZE_BYTES -> shapeBytesAfterRun
            else -> estimatedBytes
        }.coerceAtMost(outputBuffer.capacity())
        val samples = actualBytesAfterRun / Float.SIZE_BYTES
        Log.i(
            LOG_TAG,
            "MBMelGAN output actualBytesAfterRun=$actualBytesAfterRun tensorBytesAfterRun=$tensorBytesAfterRun " +
                "shape=${tensor.shape().contentToString()} samples=$samples"
        )

        val readBuffer = outputBuffer.duplicate().order(ByteOrder.nativeOrder())
        readBuffer.position(0)
        readBuffer.limit(samples * Float.SIZE_BYTES)
        val floatBuffer = readBuffer.asFloatBuffer()
        val audioArray = FloatArray(samples)
        floatBuffer.get(audioArray)
        return audioArray
    }

    private fun ensureOutputBuffer(requiredBytes: Int): ByteBuffer {
        val existing = reusableOutputBuffer
        val expanded = existing == null || existing.capacity() < requiredBytes
        return if (expanded) {
            allocateOutputBuffer(requiredBytes).also { reusableOutputBuffer = it }
        } else {
            existing
        }
    }

    private fun allocateOutputBuffer(capacityBytes: Int): ByteBuffer = ByteBuffer
        .allocateDirect(capacityBytes.coerceAtLeast(MIN_MELGAN_OUTPUT_BYTES))
        .order(ByteOrder.nativeOrder())

    private fun detectMelFrameCount(input: TensorBuffer): Int {
        val shape = input.shape
        val melBinIndex = shape.indexOfFirst { it == MEL_BIN_COUNT }
        return when {
            melBinIndex > 0 -> shape[melBinIndex - 1]
            shape.size >= 3 -> shape[1]
            shape.isNotEmpty() -> shape.maxOrNull() ?: 1
            else -> 1
        }.coerceAtLeast(1)
    }

    private fun parseRequiredBytes(message: String?): Int? {
        if (message.isNullOrBlank()) return null
        return REQUIRED_BYTES_REGEX.find(message)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private companion object {
        private const val LOG_TAG = "FastSpeech2Boundary"
        private const val MEL_BIN_COUNT = 80
        private const val MELGAN_HOP_SIZE = 256
        private const val MELGAN_OUTPUT_PADDING_SAMPLES = 4096
        private const val MIN_MELGAN_OUTPUT_BYTES = 2 * 1024 * 1024
        private const val MAX_MELGAN_RETRY = 2
        private val REQUIRED_BYTES_REGEX = Regex("with\\s+(\\d+)\\s+bytes\\s+to\\s+a\\s+Java\\s+Buffer\\s+with\\s+(\\d+)\\s+bytes")
    }
}
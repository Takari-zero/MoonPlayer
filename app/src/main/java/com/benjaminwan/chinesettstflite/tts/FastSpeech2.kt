package com.benjaminwan.chinesettstflite.tts

import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.nio.FloatBuffer
import java.util.HashMap

class FastSpeech2(file: File) : BaseInference(file) {
    override val interpreter: Interpreter = Interpreter(file, options)

    init {
        printTensorInfo()
    }

    fun getMelSpectrogram(inputIds: IntArray, speed: Float): TensorBuffer {
        interpreter.resizeInput(0, intArrayOf(1, inputIds.size))
        interpreter.allocateTensors()

        val outputBuffer = FloatBuffer.allocate(MAX_MEL_FLOATS)
        val outputMap: MutableMap<Int, Any> = HashMap()
        outputMap[0] = outputBuffer

        val inputs = Array(1) { IntArray(inputIds.size) }
        inputs[0] = inputIds

        interpreter.runForMultipleInputsOutputs(
            arrayOf<Any>(inputs, intArrayOf(0), floatArrayOf(speed), floatArrayOf(1f), floatArrayOf(1f)),
            outputMap
        )

        val melBins = interpreter.getOutputTensor(0).shape()[2]
        val shape = intArrayOf(1, outputBuffer.position() / melBins, melBins)
        val spectrogram = TensorBuffer.createFixedSize(shape, DataType.FLOAT32)
        val outputArray = FloatArray(outputBuffer.position())
        outputBuffer.rewind()
        outputBuffer.get(outputArray)
        spectrogram.loadArray(outputArray)
        return spectrogram
    }

    private companion object {
        private const val MAX_MEL_FLOATS = 350000
    }
}
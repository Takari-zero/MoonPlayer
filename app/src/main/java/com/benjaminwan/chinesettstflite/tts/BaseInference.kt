package com.benjaminwan.chinesettstflite.tts

import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.File

abstract class BaseInference(private val modelFile: File) {
    abstract val interpreter: Interpreter

    protected val options: Interpreter.Options
        get() = Interpreter.Options().apply { setNumThreads(DEFAULT_THREAD_COUNT) }

    protected fun printTensorInfo(tag: String = LOG_TAG) {
        runCatching {
            val inputInfo = buildString {
                appendLine(modelFile.name)
                for (index in 0 until interpreter.inputTensorCount) {
                    val tensor = interpreter.getInputTensor(index)
                    appendLine("inputTensor[$index]: name=${tensor.name()} shape=${tensor.shape().contentToString()} dataType=${tensor.dataType()}")
                }
            }
            val outputInfo = buildString {
                appendLine(modelFile.name)
                for (index in 0 until interpreter.outputTensorCount) {
                    val tensor = interpreter.getOutputTensor(index)
                    appendLine("outputTensor[$index]: name=${tensor.name()} shape=${tensor.shape().contentToString()} dataType=${tensor.dataType()}")
                }
            }
            Log.d(tag, inputInfo)
            Log.d(tag, outputInfo)
        }
    }

    companion object {
        private const val DEFAULT_THREAD_COUNT = 4
        private const val LOG_TAG = "FastSpeech2Boundary"
    }
}
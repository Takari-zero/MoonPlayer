package com.shenghui.localvibe.core.tts

import android.content.Context
import android.os.Build

data class OfflineTtsResourceState(
    val nativeRuntimeLoadable: Boolean,
    val supportedAbiPresent: Boolean,
    val aishell3ModelAvailable: Boolean,
    val builtInModelAvailable: Boolean
)

data class OfflineTtsEngineAvailability(
    val nativeRuntimeAvailable: Boolean,
    val supportedAbiPresent: Boolean,
    val aishell3ModelAvailable: Boolean,
    val builtInModelAvailable: Boolean,
    val aishell3Available: Boolean,
    val builtInOfflineAvailable: Boolean
) {
    val aishell3UnavailableReason: String?
        get() = unavailableReason(
            modelAvailable = aishell3ModelAvailable,
            modelMissingMessage = "Aishell3 离线语音包未安装"
        )

    val builtInOfflineUnavailableReason: String?
        get() = unavailableReason(
            modelAvailable = builtInModelAvailable,
            modelMissingMessage = "内置离线语音包未安装"
        )

    private fun unavailableReason(
        modelAvailable: Boolean,
        modelMissingMessage: String
    ): String? {
        return when {
            !nativeRuntimeAvailable -> "离线语音运行库未安装或不支持当前设备"
            !modelAvailable -> modelMissingMessage
            else -> null
        }
    }
}

object OfflineTtsAvailability {
    private val packagedNativeAbis: Set<String> = setOf("arm64-v8a")

    fun evaluate(state: OfflineTtsResourceState): OfflineTtsEngineAvailability {
        val nativeAvailable = state.nativeRuntimeLoadable && state.supportedAbiPresent
        return OfflineTtsEngineAvailability(
            nativeRuntimeAvailable = nativeAvailable,
            supportedAbiPresent = state.supportedAbiPresent,
            aishell3ModelAvailable = state.aishell3ModelAvailable,
            builtInModelAvailable = state.builtInModelAvailable,
            aishell3Available = nativeAvailable && state.aishell3ModelAvailable,
            builtInOfflineAvailable = nativeAvailable && state.builtInModelAvailable
        )
    }

    fun check(context: Context): OfflineTtsEngineAvailability {
        val appContext = context.applicationContext
        return evaluate(
            OfflineTtsResourceState(
                nativeRuntimeLoadable = NativeRuntimeProbe.isLoadable(),
                supportedAbiPresent = hasPackagedAbi(Build.SUPPORTED_ABIS.orEmpty().toList()),
                aishell3ModelAvailable = requiredAssetsExist(appContext, AISHELL3_REQUIRED_ASSETS),
                builtInModelAvailable = requiredAssetsExist(appContext, BUILT_IN_REQUIRED_ASSETS)
            )
        )
    }

    fun hasPackagedAbi(supportedAbis: List<String>): Boolean {
        return supportedAbis.any { it in packagedNativeAbis }
    }

    private fun requiredAssetsExist(context: Context, paths: List<String>): Boolean {
        return paths.all { path -> assetExists(context, path) }
    }

    private fun assetExists(context: Context, path: String): Boolean {
        return try {
            context.assets.open(path).use { true }
        } catch (_: Throwable) {
            false
        }
    }

    private object NativeRuntimeProbe {
        private val loadResult: Result<Unit> by lazy {
            runCatching {
                System.loadLibrary("sherpa-onnx-jni")
            }
        }

        fun isLoadable(): Boolean {
            return loadResult.isSuccess
        }
    }

    private val AISHELL3_REQUIRED_ASSETS = listOf(
        "offline_tts/aishell3/model.onnx",
        "offline_tts/aishell3/tokens.txt"
    )

    private val BUILT_IN_REQUIRED_ASSETS = listOf(
        "offline_tts/zh_mvp/vits-melo-tts-zh_en/model.int8.onnx",
        "offline_tts/zh_mvp/vits-melo-tts-zh_en/tokens.txt"
    )
}

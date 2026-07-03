package com.shenghui.localvibe.core.tts.fastspeech2

import android.content.Context
import android.util.Log

object FastSpeech2AssetVerifier {
    private const val TAG = "LV_BOOK_FORMAL"

    fun checkAssets(context: Context): FastSpeech2AssetCheckResult {
        val result = verifyForTest { path -> context.assetExists(path) }
        Log.d(
            TAG,
            "fastspeech2 assets check mapper=${result.hasMapper} " +
                "fastspeech2=${result.hasFastSpeech2Model} melgan=${result.hasMbMelGanModel}"
        )
        result.missingFiles.forEach { missing ->
            Log.d(TAG, "fastspeech2 assets missing file=$missing")
        }
        return result
    }

    internal fun verifyForTest(assetExists: (String) -> Boolean): FastSpeech2AssetCheckResult {
        val available = FastSpeech2AssetContract.REQUIRED_FILES.associateWith(assetExists)
        return FastSpeech2AssetCheckResult(
            hasMapper = available.getValue(FastSpeech2AssetContract.MAPPER_JSON),
            hasFastSpeech2Model = available.getValue(FastSpeech2AssetContract.FASTSPEECH2_MODEL),
            hasMbMelGanModel = available.getValue(FastSpeech2AssetContract.MB_MELGAN_MODEL),
            missingFiles = available.filterValues { exists -> !exists }.keys.toList(),
        )
    }

    private fun Context.assetExists(path: String): Boolean = runCatching {
        assets.open(path).use { }
    }.isSuccess
}

data class FastSpeech2AssetCheckResult(
    val hasMapper: Boolean,
    val hasFastSpeech2Model: Boolean,
    val hasMbMelGanModel: Boolean,
    val missingFiles: List<String>,
) {
    val isComplete: Boolean = missingFiles.isEmpty()
}

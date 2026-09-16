package com.shenghui.localvibe.core.tts

data class OfflineTtsResourceConfig(
    val modelFile: String,
    val tokensFile: String,
    val lexiconFile: String,
    val dictDir: String,
    val ruleFstFiles: List<String>
)

object OfflineTtsResourceConfigs {
    val AISHELL3: OfflineTtsResourceConfig = OfflineTtsResourceConfig(
        modelFile = "model.onnx",
        tokensFile = "tokens.txt",
        lexiconFile = "lexicon.txt",
        dictDir = "",
        ruleFstFiles = listOf(
            "phone.fst",
            "date.fst",
            "number.fst",
            "new_heteronym.fst"
        )
    )

    val BUILT_IN_OFFLINE: OfflineTtsResourceConfig = OfflineTtsResourceConfig(
        modelFile = "model.int8.onnx",
        tokensFile = "tokens.txt",
        lexiconFile = "lexicon.txt",
        dictDir = "",
        ruleFstFiles = listOf(
            "phone.fst",
            "date.fst",
            "number.fst",
            "new_heteronym.fst"
        )
    )
}

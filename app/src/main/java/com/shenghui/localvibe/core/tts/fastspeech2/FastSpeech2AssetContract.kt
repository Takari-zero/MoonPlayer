package com.shenghui.localvibe.core.tts.fastspeech2

object FastSpeech2AssetContract {
    const val ASSET_DIR = "fastspeech2"
    const val MAPPER_JSON = "fastspeech2/baker_mapper.json"
    const val FASTSPEECH2_MODEL = "fastspeech2/fastspeech2_quan.tflite"
    const val MB_MELGAN_MODEL = "fastspeech2/mb_melgan.tflite"

    val REQUIRED_FILES: List<String> = listOf(
        MAPPER_JSON,
        FASTSPEECH2_MODEL,
        MB_MELGAN_MODEL,
    )
}

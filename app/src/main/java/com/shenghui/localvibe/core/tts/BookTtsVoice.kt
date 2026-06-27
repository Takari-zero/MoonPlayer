package com.shenghui.localvibe.core.tts

data class BookTtsVoice(
    val name: String,
    val localeTag: String,
    val isNetworkConnectionRequired: Boolean
)

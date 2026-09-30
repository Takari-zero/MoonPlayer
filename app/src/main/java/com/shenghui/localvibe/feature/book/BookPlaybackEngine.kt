package com.shenghui.localvibe.feature.book

enum class BookPlaybackEngine {
    NONE,
    AISHELL3,
    MATCHA_EXPERIMENTAL,
    SYSTEM_TTS
}

fun BookPlaybackEngine.displayName(): String {
    return when (this) {
        BookPlaybackEngine.AISHELL3 -> "自研离线"
        BookPlaybackEngine.MATCHA_EXPERIMENTAL -> "Matcha 离线（实验）"
        BookPlaybackEngine.SYSTEM_TTS -> "系统语音"
        BookPlaybackEngine.NONE -> "无"
    }
}

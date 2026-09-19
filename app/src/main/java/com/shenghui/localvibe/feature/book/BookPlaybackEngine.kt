package com.shenghui.localvibe.feature.book

enum class BookPlaybackEngine {
    NONE,
    AISHELL3,
    SYSTEM_TTS
}

fun BookPlaybackEngine.displayName(): String {
    return when (this) {
        BookPlaybackEngine.AISHELL3 -> "自研离线"
        BookPlaybackEngine.SYSTEM_TTS -> "系统语音"
        BookPlaybackEngine.NONE -> "无"
    }
}

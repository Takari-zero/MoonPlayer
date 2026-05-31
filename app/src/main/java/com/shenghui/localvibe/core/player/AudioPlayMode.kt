package com.shenghui.localvibe.core.player

enum class AudioPlayMode(
    val label: String
) {
    NORMAL("顺序"),
    SHUFFLE("随机"),
    REPEAT_ONE("单曲"),
    REPEAT_ALL("列表");

    fun next(): AudioPlayMode {
        return when (this) {
            NORMAL -> SHUFFLE
            SHUFFLE -> REPEAT_ONE
            REPEAT_ONE -> REPEAT_ALL
            REPEAT_ALL -> NORMAL
        }
    }
}

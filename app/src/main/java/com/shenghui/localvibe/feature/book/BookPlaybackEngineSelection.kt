package com.shenghui.localvibe.feature.book

object BookPlaybackEngineSelection {
    data class Option(
        val engine: BookPlaybackEngine,
        val title: String,
        val enabled: Boolean,
        val unavailableReason: String? = null
    )

    fun parse(value: String?, default: BookPlaybackEngine): BookPlaybackEngine {
        return value
            ?.let { runCatching { BookPlaybackEngine.valueOf(it) }.getOrNull() }
            ?.takeIf { it == BookPlaybackEngine.AISHELL3 || it == BookPlaybackEngine.SYSTEM_TTS }
            ?: default
    }

    fun effective(
        preferred: BookPlaybackEngine,
        aishell3Available: Boolean
    ): BookPlaybackEngine {
        return if (preferred == BookPlaybackEngine.AISHELL3 && aishell3Available) {
            BookPlaybackEngine.AISHELL3
        } else {
            BookPlaybackEngine.SYSTEM_TTS
        }
    }

    fun options(
        preferred: BookPlaybackEngine,
        aishell3Available: Boolean,
        aishell3UnavailableReason: String?
    ): List<Option> {
        return listOf(
            Option(
                engine = BookPlaybackEngine.SYSTEM_TTS,
                title = "系统语音引擎",
                enabled = true
            ),
            Option(
                engine = BookPlaybackEngine.AISHELL3,
                title = "自研离线",
                enabled = aishell3Available,
                unavailableReason = aishell3UnavailableReason
            )
        )
    }
}

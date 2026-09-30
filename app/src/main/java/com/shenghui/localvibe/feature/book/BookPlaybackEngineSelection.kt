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
            ?.takeIf {
                it == BookPlaybackEngine.AISHELL3 ||
                    it == BookPlaybackEngine.MATCHA_EXPERIMENTAL ||
                    it == BookPlaybackEngine.SYSTEM_TTS
            }
            ?: default
    }

    fun effective(
        preferred: BookPlaybackEngine,
        aishell3Available: Boolean,
        matchaAvailable: Boolean = false
    ): BookPlaybackEngine {
        return when {
            preferred == BookPlaybackEngine.AISHELL3 && aishell3Available -> BookPlaybackEngine.AISHELL3
            preferred == BookPlaybackEngine.MATCHA_EXPERIMENTAL && matchaAvailable -> BookPlaybackEngine.MATCHA_EXPERIMENTAL
            else -> BookPlaybackEngine.SYSTEM_TTS
        }
    }

    fun options(
        preferred: BookPlaybackEngine,
        aishell3Available: Boolean,
        aishell3UnavailableReason: String?,
        matchaAvailable: Boolean = false,
        includeExperimentalMatcha: Boolean = false
    ): List<Option> {
        val options = mutableListOf(
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
        if (includeExperimentalMatcha) {
            options += Option(
                engine = BookPlaybackEngine.MATCHA_EXPERIMENTAL,
                title = "Matcha 离线（实验）",
                enabled = matchaAvailable,
                unavailableReason = if (matchaAvailable) null else "Matcha benchmark 模型未安装"
            )
        }
        return options
    }
}

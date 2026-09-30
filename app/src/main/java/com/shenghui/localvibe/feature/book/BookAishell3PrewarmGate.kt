package com.shenghui.localvibe.feature.book

internal object BookAishell3PrewarmGate {
    fun allows(engine: BookPlaybackEngine): Boolean {
        return engine == BookPlaybackEngine.AISHELL3
    }
}

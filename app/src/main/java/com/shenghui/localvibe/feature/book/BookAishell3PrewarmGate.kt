package com.shenghui.localvibe.feature.book

internal object BookAishell3PrewarmGate {
    fun allows(engine: BookPlaybackEngine, providerSelectionRestored: Boolean): Boolean {
        return providerSelectionRestored && engine == BookPlaybackEngine.AISHELL3
    }
}

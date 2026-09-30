package com.shenghui.localvibe.feature.book

internal data class BookPlaybackEngineSnapshot(
    val preferred: BookPlaybackEngine,
    val effective: BookPlaybackEngine,
    val matchaAvailable: Boolean
)

internal fun BookPlaybackEngineSnapshot.dispatchEngine(): BookPlaybackEngine = effective

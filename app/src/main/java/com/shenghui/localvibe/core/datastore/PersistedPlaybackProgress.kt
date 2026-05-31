package com.shenghui.localvibe.core.datastore

data class PersistedPlaybackProgress(
    val mediaUri: String,
    val mediaType: String,
    val positionMs: Long,
    val updatedAt: Long = System.currentTimeMillis()
)

package com.shenghui.localvibe.core.datastore

data class PersistedBookFile(
    val id: String,
    val name: String,
    val uri: String,
    val addedAt: Long = System.currentTimeMillis(),
    val progressPercent: Int = 0,
    val source: String = "FILE"
)

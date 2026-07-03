package com.shenghui.localvibe.core.datastore

data class PersistedBookProgress(
    val uri: String,
    val paragraphIndex: Int,
    val totalParagraphs: Int,
    val updatedAt: Long = System.currentTimeMillis()
)

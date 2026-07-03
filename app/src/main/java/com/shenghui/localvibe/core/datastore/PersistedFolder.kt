package com.shenghui.localvibe.core.datastore

data class PersistedFolder(
    val folderId: String,
    val name: String,
    val uri: String,
    val targetType: String,
    val source: String = "MANUAL",
    val addedAt: Long = System.currentTimeMillis(),
    val lastScannedAt: Long = 0L
)

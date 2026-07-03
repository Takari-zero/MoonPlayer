package com.shenghui.localvibe.core.datastore

data class PersistedVideoVisibilityRecord(
    val id: String,
    val name: String,
    val uri: String,
    val path: String?,
    val recordedAt: Long
)

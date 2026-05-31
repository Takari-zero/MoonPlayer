package com.shenghui.localvibe.core.scanner

data class LocalMediaFile(
    val id: String,
    val name: String,
    val uri: String,
    val type: LocalMediaType,
    val extension: String,
    val size: Long,
    val parentFolderName: String?
)

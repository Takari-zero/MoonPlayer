package com.shenghui.localvibe.feature.home.model

data class MediaFolderUiModel(
    val id: String,
    val name: String,
    val uri: String,
    val videoCount: Int,
    val audioCount: Int,
    val bookCount: Int,
    val isScanning: Boolean
)

package com.shenghui.localvibe.feature.video.model

enum class VideoVisibilityRecordType {
    HIDDEN_FOLDER,
    REMOVED_VIDEO,
    UNAVAILABLE_FILE
}

data class VideoVisibilityRecordUiModel(
    val id: String,
    val type: VideoVisibilityRecordType,
    val name: String,
    val path: String?,
    val recordedAt: Long
)

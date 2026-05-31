package com.shenghui.localvibe.feature.video.model

import com.shenghui.localvibe.core.scanner.LocalMediaFile
import com.shenghui.localvibe.feature.home.model.MediaFolderUiModel

data class VideoFolderUiModel(
    val folder: MediaFolderUiModel,
    val videos: List<LocalMediaFile>,
    val source: String = "手动添加"
)

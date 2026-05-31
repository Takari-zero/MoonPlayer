package com.shenghui.localvibe.feature.library

import com.shenghui.localvibe.core.scanner.LocalMediaFile
import com.shenghui.localvibe.feature.home.model.MediaFolderUiModel

data class MediaFolderGroupUiModel(
    val folder: MediaFolderUiModel,
    val files: List<LocalMediaFile>,
    val source: String = "手动添加"
)

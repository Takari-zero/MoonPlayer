package com.shenghui.localvibe.core.media

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

fun resolveDocumentTreeName(
    context: Context,
    uri: Uri
): String {
    val name = DocumentFile.fromTreeUri(context, uri)?.name
    return name?.takeIf { it.isNotBlank() } ?: "未命名文件夹"
}

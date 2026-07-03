package com.shenghui.localvibe.core.media

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

fun deleteUri(context: Context, uriString: String): Boolean {
    val uri = Uri.parse(uriString)
    val contentResolverDeleted = runCatching {
        context.contentResolver.delete(uri, null, null) > 0
    }.getOrDefault(false)
    if (contentResolverDeleted) {
        return true
    }

    return runCatching {
        DocumentFile.fromSingleUri(context, uri)?.delete() == true
    }.getOrDefault(false)
}

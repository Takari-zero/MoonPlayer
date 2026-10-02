package com.shenghui.localvibe.feature.book

import android.content.ContentResolver
import android.net.Uri
import java.io.InputStream

internal class ContentResolverPreviewContentStreamOpener(
    private val contentResolver: ContentResolver
) : PreviewContentInputStreamOpener {
    override fun open(bookUri: String): InputStream? {
        return contentResolver.openInputStream(Uri.parse(bookUri))
    }
}

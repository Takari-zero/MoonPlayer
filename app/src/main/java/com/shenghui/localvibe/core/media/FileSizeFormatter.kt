package com.shenghui.localvibe.core.media

import java.util.Locale

fun formatFileSize(size: Long): String {
    if (size <= 0L) {
        return "0 B"
    }

    val kb = size / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
        else -> "$size B"
    }
}

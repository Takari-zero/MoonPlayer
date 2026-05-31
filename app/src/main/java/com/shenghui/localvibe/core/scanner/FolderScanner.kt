package com.shenghui.localvibe.core.scanner

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

object FolderScanner {
    fun scanFolder(context: Context, treeUri: Uri): List<LocalMediaFile> {
        val root = runCatching {
            DocumentFile.fromTreeUri(context, treeUri)
        }.getOrNull() ?: return emptyList()

        val canScanRoot = runCatching {
            root.exists() && root.canRead()
        }.getOrDefault(false)
        if (!canScanRoot) {
            return emptyList()
        }

        val result = mutableListOf<LocalMediaFile>()
        scanDocumentFile(
            documentFile = root,
            parentFolderName = runCatching { root.name }.getOrNull(),
            result = result
        )
        return result
    }

    private fun scanDocumentFile(
        documentFile: DocumentFile,
        parentFolderName: String?,
        result: MutableList<LocalMediaFile>
    ) {
        val isDirectory = runCatching { documentFile.isDirectory }.getOrDefault(false)
        if (isDirectory) {
            val children = runCatching { documentFile.listFiles() }.getOrDefault(emptyArray())
            children.forEach { child ->
                val nextParentName = if (runCatching { child.isDirectory }.getOrDefault(false)) {
                    runCatching { child.name }.getOrNull()
                } else {
                    parentFolderName
                }
                scanDocumentFile(
                    documentFile = child,
                    parentFolderName = nextParentName,
                    result = result
                )
            }
            return
        }

        val isFile = runCatching { documentFile.isFile }.getOrDefault(false)
        if (!isFile) {
            return
        }

        val name = runCatching { documentFile.name.orEmpty() }.getOrDefault("")
        val extension = name.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase()
        val type = extension.toLocalMediaType()
        if (type == LocalMediaType.UNKNOWN) {
            return
        }

        val uriText = runCatching { documentFile.uri.toString() }.getOrNull() ?: return
        result.add(
            LocalMediaFile(
                id = uriText,
                name = name.ifBlank { "未命名文件" },
                uri = uriText,
                type = type,
                extension = extension,
                size = runCatching { documentFile.length() }.getOrDefault(0L),
                parentFolderName = parentFolderName
            )
        )
    }

    private fun String.toLocalMediaType(): LocalMediaType {
        return when (this) {
            "mp4", "mkv", "avi", "mov", "webm", "m4v", "3gp" -> LocalMediaType.VIDEO
            "mp3", "flac", "m4a", "aac", "wav", "ogg" -> LocalMediaType.AUDIO
            "txt" -> LocalMediaType.BOOK
            "srt", "ass", "ssa", "vtt" -> LocalMediaType.SUBTITLE
            else -> LocalMediaType.UNKNOWN
        }
    }
}

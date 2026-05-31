package com.shenghui.localvibe.core.scanner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.shenghui.localvibe.feature.home.model.MediaFolderUiModel

data class ScannedMediaFolder(
    val folder: MediaFolderUiModel,
    val files: List<LocalMediaFile>
)

object MediaStoreScanner {
    fun scanVideos(context: Context): List<ScannedMediaFolder> {
        return scanMediaStore(
            context = context,
            collectionUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            type = LocalMediaType.VIDEO,
            folderPrefix = "auto:video",
            fallbackFolderName = "视频"
        )
    }

    fun scanAudios(context: Context): List<ScannedMediaFolder> {
        return scanMediaStore(
            context = context,
            collectionUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            type = LocalMediaType.AUDIO,
            folderPrefix = "auto:audio",
            fallbackFolderName = "音乐"
        )
    }

    private fun scanMediaStore(
        context: Context,
        collectionUri: Uri,
        type: LocalMediaType,
        folderPrefix: String,
        fallbackFolderName: String
    ): List<ScannedMediaFolder> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.RELATIVE_PATH
        )
        val groupedFiles = linkedMapOf<String, MutableList<LocalMediaFile>>()

        runCatching {
            context.contentResolver.query(
                collectionUri,
                projection,
                null,
                null,
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)

                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn).orEmpty()
                    val size = cursor.getLong(sizeColumn)
                    val relativePath = cursor.getString(pathColumn).orEmpty()
                    val folderName = relativePath
                        .trimEnd('/')
                        .substringAfterLast('/', missingDelimiterValue = "")
                        .ifBlank { fallbackFolderName }
                    val folderId = "$folderPrefix:$relativePath"
                    val uri = ContentUris.withAppendedId(collectionUri, mediaId)
                    val extension = name.substringAfterLast('.', missingDelimiterValue = "")
                        .lowercase()

                    groupedFiles.getOrPut(folderId) { mutableListOf() }.add(
                        LocalMediaFile(
                            id = uri.toString(),
                            name = name.ifBlank { "未命名文件" },
                            uri = uri.toString(),
                            type = type,
                            extension = extension,
                            size = size,
                            parentFolderName = folderName
                        )
                    )
                }
            }
        }

        return groupedFiles.map { (folderId, files) ->
            val folderName = files.firstOrNull()?.parentFolderName ?: fallbackFolderName
            ScannedMediaFolder(
                folder = MediaFolderUiModel(
                    id = folderId,
                    name = folderName,
                    uri = folderId,
                    videoCount = files.count { it.type == LocalMediaType.VIDEO },
                    audioCount = files.count { it.type == LocalMediaType.AUDIO },
                    bookCount = files.count { it.type == LocalMediaType.BOOK },
                    isScanning = false
                ),
                files = files
            )
        }
    }
}

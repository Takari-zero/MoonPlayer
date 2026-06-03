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
        val includeVideoBucketColumns = type == LocalMediaType.VIDEO
        val projection = buildList {
            add(MediaStore.MediaColumns._ID)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.SIZE)
            add(MediaStore.MediaColumns.RELATIVE_PATH)
            add(MediaStore.MediaColumns.DATA)
            if (includeVideoBucketColumns) {
                add(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                add(MediaStore.Video.Media.BUCKET_ID)
            }
        }.toTypedArray()
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
                val dataColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                val bucketNameColumn = if (includeVideoBucketColumns) {
                    cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                } else {
                    -1
                }

                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn).orEmpty()
                    val size = cursor.getLong(sizeColumn)
                    val relativePath = cursor.getString(pathColumn).orEmpty()
                    val dataPath = dataColumn
                        .takeIf { it >= 0 }
                        ?.let { cursor.getString(it).orEmpty() }
                        .orEmpty()
                    val bucketDisplayName = bucketNameColumn
                        .takeIf { it >= 0 }
                        ?.let { cursor.getString(it).orEmpty() }
                        .orEmpty()
                    val folderKey = relativePath.toFolderKey()
                        .ifBlank { dataPath.toParentFolderKey() }
                        .ifBlank { fallbackFolderName }
                    val folderName = resolveFolderDisplayName(
                        bucketDisplayName = bucketDisplayName,
                        relativePath = relativePath,
                        dataPath = dataPath,
                        fallbackFolderName = fallbackFolderName
                    )
                    val folderId = "$folderPrefix:$folderKey"
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

    private fun String.toFolderKey(): String {
        return trim()
            .replace('\\', '/')
            .trimEnd('/')
    }

    private fun String.toParentFolderKey(): String {
        return trim()
            .replace('\\', '/')
            .substringBeforeLast('/', missingDelimiterValue = "")
            .trimEnd('/')
    }

    private fun String.toFolderName(): String {
        return trim()
            .replace('\\', '/')
            .trimEnd('/')
            .substringAfterLast('/', missingDelimiterValue = "")
    }

    private fun resolveFolderDisplayName(
        bucketDisplayName: String,
        relativePath: String,
        dataPath: String,
        fallbackFolderName: String
    ): String {
        return bucketDisplayName.trim()
            .ifBlank { relativePath.toFolderName() }
            .ifBlank { dataPath.toParentFolderKey().toFolderName() }
            .ifBlank { fallbackFolderName }
    }
}

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
                    val rawFolderKey = relativePath.toFolderKey()
                        .ifBlank { dataPath.toParentFolderKey() }
                        .ifBlank { fallbackFolderName }
                    val folderKey = if (type == LocalMediaType.VIDEO) {
                        rawFolderKey.toCanonicalFolderKey().ifBlank { rawFolderKey }
                    } else {
                        rawFolderKey
                    }
                    val folderName = resolveFolderDisplayName(
                        bucketDisplayName = bucketDisplayName,
                        relativePath = relativePath,
                        dataPath = dataPath,
                        fallbackFolderName = fallbackFolderName,
                        preferSpecificVideoFolderName = type == LocalMediaType.VIDEO
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

        val scannedFolders = groupedFiles.map { (folderId, files) ->
            val folderName = files.toBestFolderDisplayName(fallbackFolderName)
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

        return if (type == LocalMediaType.VIDEO) {
            scannedFolders.mergeCaseVariantVideoFolders(fallbackFolderName)
        } else {
            scannedFolders
        }
    }

    private fun String.toFolderKey(): String {
        return trim()
            .replace('\\', '/')
            .trimEnd('/')
    }

    private fun String.toCanonicalFolderKey(): String {
        return trim()
            .replace('\\', '/')
            .split('/')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("/")
            .lowercase()
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

    private fun List<LocalMediaFile>.toBestFolderDisplayName(fallbackFolderName: String): String {
        val names = mapNotNull { it.parentFolderName?.trim()?.takeIf { name -> name.isNotBlank() } }
        if (names.isEmpty()) return fallbackFolderName

        val dominantKey = names
            .groupingBy { it.lowercase() }
            .eachCount()
            .maxWithOrNull(
                compareBy<Map.Entry<String, Int>> { it.value }
                    .thenBy { it.key }
            )
            ?.key
            ?: return names.first()

        return names
            .filter { it.equals(dominantKey, ignoreCase = true) }
            .maxWithOrNull(
                compareBy<String> { it.folderDisplayNameScore(fallbackFolderName) }
                    .thenBy { it }
            )
            ?: names.first()
    }

    private fun String.folderDisplayNameScore(fallbackFolderName: String): Int {
        var score = 0
        if (isNotBlank()) score += 1
        if (!isGenericVideoFolderName(fallbackFolderName)) score += 2
        if (firstOrNull()?.isUpperCase() == true) score += 1
        return score
    }

    private fun List<ScannedMediaFolder>.mergeCaseVariantVideoFolders(
        fallbackFolderName: String
    ): List<ScannedMediaFolder> {
        return groupBy { it.folder.name.trim().lowercase() }
            .flatMap { (_, folders) ->
                val displayNames = folders
                    .map { it.folder.name.trim() }
                    .filter { it.isNotBlank() }
                    .toSet()
                val shouldMerge = folders.size > 1 &&
                    displayNames.size > 1 &&
                    displayNames.none { it.isGenericVideoFolderName(fallbackFolderName) }

                if (!shouldMerge) {
                    folders
                } else {
                    val files = folders
                        .flatMap { it.files }
                        .distinctBy { it.uri }
                    val baseFolder = folders.first().folder
                    val folderName = files.toBestFolderDisplayName(fallbackFolderName)

                    listOf(
                        ScannedMediaFolder(
                            folder = baseFolder.copy(
                                name = folderName,
                                videoCount = files.count { it.type == LocalMediaType.VIDEO },
                                audioCount = files.count { it.type == LocalMediaType.AUDIO },
                                bookCount = files.count { it.type == LocalMediaType.BOOK }
                            ),
                            files = files
                        )
                    )
                }
            }
    }

    private fun resolveFolderDisplayName(
        bucketDisplayName: String,
        relativePath: String,
        dataPath: String,
        fallbackFolderName: String,
        preferSpecificVideoFolderName: Boolean
    ): String {
        val dataFolderName = dataPath.toParentFolderKey().toFolderName()
        val pathFolderName = relativePath.toFolderName()
            .toSpecificFolderName(
                fallbackFolderName = fallbackFolderName,
                dataFolderName = dataFolderName,
                preferSpecificVideoFolderName = preferSpecificVideoFolderName
            )
            .ifBlank { dataFolderName }

        return bucketDisplayName.toMeaningfulBucketName(
            fallbackFolderName = fallbackFolderName,
            pathFolderName = pathFolderName
        )
            .ifBlank { pathFolderName }
            .ifBlank { fallbackFolderName }
    }

    private fun String.toSpecificFolderName(
        fallbackFolderName: String,
        dataFolderName: String,
        preferSpecificVideoFolderName: Boolean
    ): String {
        val folderName = trim()
        if (folderName.isBlank()) return ""
        if (!preferSpecificVideoFolderName || dataFolderName.isBlank()) return folderName
        return if (
            folderName.isGenericVideoFolderName(fallbackFolderName) &&
            !folderName.equals(dataFolderName, ignoreCase = true)
        ) {
            ""
        } else {
            folderName
        }
    }

    private fun String.toMeaningfulBucketName(
        fallbackFolderName: String,
        pathFolderName: String
    ): String {
        val bucketName = trim()
        if (bucketName.isBlank()) return ""
        if (pathFolderName.isBlank()) return bucketName

        val lowerBucketName = bucketName.lowercase()
        val lowerFallbackName = fallbackFolderName.lowercase()
        val genericVideoNames = setOf("video", "videos", "movie", "movies", "影片", "视频")

        return when {
            lowerBucketName == lowerFallbackName && !bucketName.equals(pathFolderName, ignoreCase = true) -> ""
            lowerBucketName in genericVideoNames && !bucketName.equals(pathFolderName, ignoreCase = true) -> ""
            else -> bucketName
        }
    }

    private fun String.isGenericVideoFolderName(fallbackFolderName: String): Boolean {
        val normalizedName = trim().lowercase()
        val fallbackName = fallbackFolderName.trim().lowercase()
        return normalizedName == fallbackName ||
            normalizedName in setOf("video", "videos", "movie", "movies", "影片", "视频")
    }
}

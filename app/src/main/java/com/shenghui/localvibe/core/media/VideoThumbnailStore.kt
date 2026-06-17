package com.shenghui.localvibe.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.shenghui.localvibe.core.scanner.LocalMediaFile
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.max
import kotlin.math.sqrt

object VideoThumbnailStore {
    private const val CACHE_DIR = "video_thumbnails"
    private const val JPEG_QUALITY = 86
    private const val MAX_THUMBNAIL_CACHE_BYTES = 300L * 1024L * 1024L
    private const val TRIM_THUMBNAIL_CACHE_TO_BYTES = 260L * 1024L * 1024L
    private const val DARK_LUMA = 18.0
    private const val BRIGHT_LUMA = 238.0
    private const val LOW_DETAIL_STD_DEV = 8.0
    private const val BLANK_PIXEL_RATIO = 0.92
    private const val PURE_COLOR_STD_DEV = 4.0
    private val cacheTrimLock = Any()

    fun cacheKey(file: LocalMediaFile): String {
        val uriHash = sha256(file.uri.trim())
        val modifiedAt = file.modifiedAt?.takeIf { it > 0L } ?: 0L
        val size = file.size.coerceAtLeast(0L)
        return "${uriHash}_${modifiedAt}_${size}"
    }

    fun loadOrCreate(
        context: Context,
        file: LocalMediaFile,
        durationMs: Long? = file.durationMs
    ): Bitmap? {
        val appContext = context.applicationContext
        if (!isReadable(appContext, file.uri)) {
            deleteForUri(appContext, file.uri)
            return null
        }

        val key = cacheKey(file)
        cleanupOldVersions(appContext, file, key)
        val cacheFile = cacheFile(appContext, key)
        if (cacheFile.isFile) {
            BitmapFactory.decodeFile(cacheFile.absolutePath)?.let { cached ->
                if (!isInvalidThumbnail(cached)) {
                    touch(cacheFile)
                    return cached
                }
            }
            cacheFile.delete()
        }

        val bitmap = createValidThumbnail(appContext, file.uri, durationMs) ?: return null
        runCatching {
            cacheFile.parentFile?.mkdirs()
            cacheFile.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            }
            touch(cacheFile)
            enforceCacheLimit(appContext, protectedFile = cacheFile)
        }.onFailure {
            cacheFile.delete()
        }
        return bitmap
    }

    fun hasValidCache(context: Context, file: LocalMediaFile): Boolean {
        val appContext = context.applicationContext
        val cacheFile = cacheFile(appContext, cacheKey(file))
        if (!cacheFile.isFile) return false
        val cached = BitmapFactory.decodeFile(cacheFile.absolutePath)
        if (cached != null && !isInvalidThumbnail(cached)) {
            touch(cacheFile)
            return true
        }
        cacheFile.delete()
        return false
    }

    fun isSourceReadable(context: Context, file: LocalMediaFile): Boolean {
        return isReadable(context.applicationContext, file.uri)
    }

    fun delete(context: Context, file: LocalMediaFile) {
        deleteForUri(context.applicationContext, file.uri)
    }

    fun delete(context: Context, files: Collection<LocalMediaFile>) {
        files.forEach { delete(context, it) }
    }

    fun deleteForUris(context: Context, uris: Collection<String>) {
        uris.forEach { deleteForUri(context.applicationContext, it) }
    }

    fun deleteForUri(context: Context, uri: String) {
        val prefix = "${sha256(uri.trim())}_"
        thumbnailDir(context.applicationContext)
            .listFiles { file -> file.isFile && file.name.startsWith(prefix) }
            .orEmpty()
            .forEach { it.delete() }
    }

    private fun enforceCacheLimit(context: Context, protectedFile: File? = null) {
        synchronized(cacheTrimLock) {
            val cacheDir = thumbnailDir(context.applicationContext)
            val files = cacheDir
                .listFiles { file -> file.isFile }
                .orEmpty()
                .toList()
            var totalBytes = files.sumOf { it.length().coerceAtLeast(0L) }
            if (totalBytes <= MAX_THUMBNAIL_CACHE_BYTES) return

            val protectedPath = protectedFile?.absolutePath
            files
                .sortedWith(compareBy<File> { it.lastModified() }.thenBy { it.name })
                .forEach { candidate ->
                    if (totalBytes <= TRIM_THUMBNAIL_CACHE_TO_BYTES) return
                    if (candidate.absolutePath == protectedPath) return@forEach
                    val size = candidate.length().coerceAtLeast(0L)
                    val deleted = runCatching { candidate.delete() }.getOrDefault(false)
                    if (deleted) {
                        totalBytes -= size
                    }
                }
        }
    }

    private fun touch(file: File) {
        runCatching {
            file.setLastModified(System.currentTimeMillis())
        }
    }

    private fun createValidThumbnail(
        context: Context,
        uri: String,
        knownDurationMs: Long?
    ): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, Uri.parse(uri))
            val durationMs = knownDurationMs
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                ?: 0L
            thumbnailTimesUs(durationMs).firstNotNullOfOrNull { timeUs ->
                retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?.takeUnless(::isInvalidThumbnail)
            }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun thumbnailTimesUs(durationMs: Long): List<Long> {
        val safeDuration = durationMs.coerceAtLeast(0L)
        return listOf(
            1_000_000L,
            3_000_000L,
            (safeDuration * 100L).coerceAtLeast(0L),
            (safeDuration * 250L).coerceAtLeast(0L),
            (safeDuration * 500L).coerceAtLeast(0L),
            0L
        ).map { timeUs ->
            if (safeDuration > 0L) {
                timeUs.coerceIn(0L, safeDuration * 1_000L)
            } else {
                timeUs
            }
        }.distinct()
    }

    private fun isInvalidThumbnail(bitmap: Bitmap): Boolean {
        if (bitmap.width <= 0 || bitmap.height <= 0) return true
        val sampleX = 12
        val sampleY = 12
        var totalLuma = 0.0
        var totalLumaSquared = 0.0
        var totalRed = 0.0
        var totalGreen = 0.0
        var totalBlue = 0.0
        var totalRedSquared = 0.0
        var totalGreenSquared = 0.0
        var totalBlueSquared = 0.0
        var darkCount = 0
        var whiteCount = 0
        var count = 0
        repeat(sampleY) { yIndex ->
            val y = ((yIndex + 0.5f) * bitmap.height / sampleY).toInt().coerceIn(0, bitmap.height - 1)
            repeat(sampleX) { xIndex ->
                val x = ((xIndex + 0.5f) * bitmap.width / sampleX).toInt().coerceIn(0, bitmap.width - 1)
                val color = bitmap.getPixel(x, y)
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF
                val luma = 0.299 * r + 0.587 * g + 0.114 * b
                totalLuma += luma
                totalLumaSquared += luma * luma
                totalRed += r
                totalGreen += g
                totalBlue += b
                totalRedSquared += r * r
                totalGreenSquared += g * g
                totalBlueSquared += b * b
                if (luma < DARK_LUMA) darkCount += 1
                if (luma > BRIGHT_LUMA && r > 232 && g > 232 && b > 232) whiteCount += 1
                count += 1
            }
        }
        val safeCount = max(count, 1)
        val averageLuma = totalLuma / safeCount
        val lumaStdDev = sqrt((totalLumaSquared / safeCount - averageLuma * averageLuma).coerceAtLeast(0.0))
        val redStdDev = channelStdDev(totalRed, totalRedSquared, safeCount)
        val greenStdDev = channelStdDev(totalGreen, totalGreenSquared, safeCount)
        val blueStdDev = channelStdDev(totalBlue, totalBlueSquared, safeCount)
        val averageColorStdDev = (redStdDev + greenStdDev + blueStdDev) / 3.0
        val darkRatio = darkCount.toDouble() / safeCount
        val whiteRatio = whiteCount.toDouble() / safeCount

        val isBlackFrame = averageLuma < 12.0 ||
            (darkRatio > BLANK_PIXEL_RATIO && lumaStdDev < LOW_DETAIL_STD_DEV)
        val isWhiteFrame = (averageLuma > 245.0 && lumaStdDev < LOW_DETAIL_STD_DEV) ||
            (whiteRatio > BLANK_PIXEL_RATIO && lumaStdDev < 10.0)
        val isLowInformationSolidFrame = lumaStdDev < 3.0 && averageColorStdDev < PURE_COLOR_STD_DEV
        return isBlackFrame || isWhiteFrame || isLowInformationSolidFrame
    }

    private fun channelStdDev(total: Double, squaredTotal: Double, count: Int): Double {
        val average = total / count
        return sqrt((squaredTotal / count - average * average).coerceAtLeast(0.0))
    }

    private fun cleanupOldVersions(context: Context, file: LocalMediaFile, currentKey: String) {
        val prefix = "${sha256(file.uri.trim())}_"
        thumbnailDir(context)
            .listFiles { candidate ->
                candidate.isFile &&
                    candidate.name.startsWith(prefix) &&
                    candidate.name != "$currentKey.jpg"
            }
            .orEmpty()
            .forEach { it.delete() }
    }

    private fun isReadable(context: Context, uri: String): Boolean {
        return try {
            context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { true } == true
        } catch (_: Exception) {
            false
        }
    }

    private fun cacheFile(context: Context, key: String): File {
        return File(thumbnailDir(context), "$key.jpg")
    }

    private fun thumbnailDir(context: Context): File {
        return File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(Locale.US, it.toInt() and 0xFF) }
    }
}

fun videoMetadataCacheKey(file: LocalMediaFile): String = VideoThumbnailStore.cacheKey(file)

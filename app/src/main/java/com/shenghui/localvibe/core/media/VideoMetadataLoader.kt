package com.shenghui.localvibe.core.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.shenghui.localvibe.core.scanner.LocalMediaFile

data class VideoMetadata(
    val thumbnail: Bitmap?,
    val durationMs: Long?
)

fun loadVideoMetadata(
    context: Context,
    uri: String
): VideoMetadata {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, Uri.parse(uri))
        val duration = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()
        val thumbnail = retriever.getFrameAtTime(
            0L,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
        )
        VideoMetadata(
            thumbnail = thumbnail,
            durationMs = duration
        )
    } catch (_: Exception) {
        VideoMetadata(
            thumbnail = null,
            durationMs = null
        )
    } finally {
        runCatching { retriever.release() }
    }
}

fun loadVideoMetadata(
    context: Context,
    file: LocalMediaFile
): VideoMetadata {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, Uri.parse(file.uri))
        val duration = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()
        val thumbnail = VideoThumbnailStore.loadOrCreate(context, file, duration)
        VideoMetadata(
            thumbnail = thumbnail,
            durationMs = duration
        )
    } catch (_: Exception) {
        VideoThumbnailStore.delete(context, file)
        VideoMetadata(
            thumbnail = null,
            durationMs = null
        )
    } finally {
        runCatching { retriever.release() }
    }
}

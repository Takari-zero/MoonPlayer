package com.shenghui.localvibe.core.media

import android.content.Context
import com.shenghui.localvibe.core.scanner.LocalMediaFile
import com.shenghui.localvibe.core.scanner.LocalMediaType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VideoThumbnailPrewarmer(
    context: Context,
    private val scope: CoroutineScope,
    private val maxPerRun: Int = 100,
    private val delayBetweenItemsMs: Long = 90L
) {
    private val appContext = context.applicationContext
    private var job: Job? = null

    fun start(files: Collection<LocalMediaFile>) {
        val queue = files
            .asSequence()
            .filter { it.type == LocalMediaType.VIDEO }
            .filter { it.uri.isNotBlank() }
            .distinctBy { VideoThumbnailStore.cacheKey(it) }
            .toList()

        job?.cancel()
        if (queue.isEmpty()) {
            job = null
            return
        }
        job = scope.launch(Dispatchers.IO) {
            var attemptedCount = 0
            for (file in queue) {
                if (!isActive || attemptedCount >= maxPerRun) break
                if (!VideoThumbnailStore.isSourceReadable(appContext, file)) continue
                if (VideoThumbnailStore.hasValidCache(appContext, file)) continue

                attemptedCount += 1
                runCatching {
                    VideoThumbnailStore.loadOrCreate(appContext, file)
                }
                delay(delayBetweenItemsMs)
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}

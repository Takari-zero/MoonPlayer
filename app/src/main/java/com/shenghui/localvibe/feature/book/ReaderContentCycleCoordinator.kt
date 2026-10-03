package com.shenghui.localvibe.feature.book

import android.content.ContentResolver
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

data class ReaderContentCycle(
    val generation: Long,
    val bookUri: String
)

internal sealed interface CurrentVersionShadowState {
    data object Idle : CurrentVersionShadowState

    data class NotStarted(val cycle: ReaderContentCycle) : CurrentVersionShadowState

    data class InFlight(val cycle: ReaderContentCycle) : CurrentVersionShadowState

    data class Ready(
        val cycle: ReaderContentCycle,
        val result: CurrentPreviewContentVersionResult
    ) : CurrentVersionShadowState

    data class Failed(
        val cycle: ReaderContentCycle,
        val result: CurrentPreviewContentVersionResult
    ) : CurrentVersionShadowState

    data object Released : CurrentVersionShadowState
}

internal class ReaderContentCycleCoordinator(
    ownerScope: CoroutineScope,
    private val currentVersionLoader: CurrentPreviewContentVersionLoader,
    private val shadowLogger: (String) -> Unit = { message ->
        Log.i(PREVIEW_VERSION_SHADOW_TAG, message)
    },
    private val elapsedRealtimeMs: () -> Long = { System.nanoTime() / 1_000_000L },
    private val cacheVersionShadowLogger: (String) -> Unit = { message ->
        Log.i("PREVIEW_CACHE_VERSION_SHADOW", message)
    }
) {
    constructor(
        ownerScope: CoroutineScope,
        contentResolver: ContentResolver
    ) : this(
        ownerScope = ownerScope,
        currentVersionLoader = CurrentPreviewContentVersionLoader(
            ownerScope = ownerScope,
            streamOpener = ContentResolverPreviewContentStreamOpener(contentResolver)
        )
    )

    private val stateLock = Any()
    private val shadowJob = SupervisorJob(ownerScope.coroutineContext[Job])
    private val shadowScope = CoroutineScope(ownerScope.coroutineContext + shadowJob)
    private val startedShadowGenerations = mutableSetOf<Long>()
    private val activeShadowJobs = mutableSetOf<Job>()
    private var generationCounter = 0L
    private var currentCycle: ReaderContentCycle? = null
    private var released = false
    private var shadowState: CurrentVersionShadowState = CurrentVersionShadowState.Idle
    private val cacheWriteDiagnostics = mutableSetOf<String>()
    private var entryCachedVersion: PreviewContentVersion = PreviewContentVersion.Unavailable
    private var cycleBeginMs = 0L
    private var comparedGeneration: Long? = null

    fun beginContentCycle(
        bookUri: String,
        expectedSize: Long?,
        modifiedAt: Long?,
        cachedVersion: PreviewContentVersion = PreviewContentVersion.Unavailable
    ): ReaderContentCycle {
        val cycle = synchronized(stateLock) {
            check(!released) { "ReaderContentCycleCoordinator is released" }
            check(generationCounter < Long.MAX_VALUE) { "Reader generation exhausted" }
            ReaderContentCycle(
                generation = ++generationCounter,
                bookUri = bookUri
            ).also {
                currentCycle = it
                shadowState = CurrentVersionShadowState.NotStarted(it)
                cacheWriteDiagnostics.clear()
                entryCachedVersion = cachedVersion
                cycleBeginMs = elapsedRealtimeMs()
            }
        }
        currentVersionLoader.beginContentCycle(
            bookUri = cycle.bookUri,
            readerGeneration = cycle.generation,
            expectedSize = expectedSize,
            modifiedAt = modifiedAt
        )
        shadowLogger("event=BEGIN generation=${cycle.generation}")
        return cycle
    }

    fun startCurrentVersionShadow(cycle: ReaderContentCycle): Boolean {
        val startedAtMs = elapsedRealtimeMs()
        val shouldStart = synchronized(stateLock) {
            if (
                released ||
                currentCycle != cycle ||
                !startedShadowGenerations.add(cycle.generation)
            ) {
                false
            } else {
                shadowState = CurrentVersionShadowState.InFlight(cycle)
                true
            }
        }
        if (!shouldStart) return false

        val job = shadowScope.launch(start = CoroutineStart.LAZY) {
            publishShadowResult(
                cycle = cycle,
                result = currentVersionLoader.ensureCurrentVersion(),
                elapsedMs = (elapsedRealtimeMs() - startedAtMs).coerceAtLeast(0L)
            )
        }
        val registered = synchronized(stateLock) {
            if (released) {
                false
            } else {
                activeShadowJobs += job
                true
            }
        }
        if (!registered) {
            job.cancel()
            return false
        }
        job.invokeOnCompletion {
            synchronized(stateLock) {
                activeShadowJobs -= job
            }
        }
        job.start()
        return true
    }

    fun currentCycleSnapshot(): ReaderContentCycle? = synchronized(stateLock) { currentCycle }

    fun shadowStateSnapshot(): CurrentVersionShadowState = synchronized(stateLock) { shadowState }

    fun recordCacheWrite(
        version: PreviewContentVersion,
        provenance: PreparedContentProvenance? = null
    ) {
        val message = synchronized(stateLock) {
            if (released) return
            val source = if (provenance == null) "CACHED_PREVIEW" else "FULL_CONTENT"
            val generation = provenance?.cycle?.generation ?: currentCycle?.generation ?: -1L
            val detail = if (provenance == null) "versionPreserved=true" else "authority=SHARED_RAW_READ"
            val reason = provenance?.diagnosticReason?.let { " reason=$it" }.orEmpty()
            "source=$source generation=$generation kind=${version.logKind()} " +
                "size=${version.size ?: -1L} $detail$reason"
        }
        val first = synchronized(stateLock) { cacheWriteDiagnostics.add(message) }
        if (first) Log.i("BOOK_CACHE_VERSION_WRITE", message)
    }

    fun release() {
        val shouldRelease = synchronized(stateLock) {
            if (released) {
                false
            } else {
                released = true
                currentCycle = null
                shadowState = CurrentVersionShadowState.Released
                true
            }
        }
        if (!shouldRelease) return
        shadowJob.cancel()
        currentVersionLoader.release()
    }

    private fun publishShadowResult(
        cycle: ReaderContentCycle,
        result: CurrentPreviewContentVersionResult,
        elapsedMs: Long
    ) {
        val message = synchronized(stateLock) {
            if (released) {
                null
            } else {
                val current = currentCycle
                if (current != cycle) {
                    "event=STALE generation=${cycle.generation} " +
                        "currentGeneration=${current?.generation ?: -1L}"
                } else if (result.isReady) {
                    shadowState = CurrentVersionShadowState.Ready(cycle, result)
                    publishCacheComparisonLocked(cycle, result)
                    "event=READY generation=${cycle.generation} " +
                        "versionKind=${result.version.logKind()} " +
                        "size=${result.version.size ?: -1L} elapsedMs=$elapsedMs"
                } else {
                    shadowState = CurrentVersionShadowState.Failed(cycle, result)
                    publishCacheComparisonLocked(cycle, result)
                    "event=FAILED generation=${cycle.generation} " +
                        "diagnosticReason=${result.diagnosticReason}"
                }
            }
        }
        message?.let(shadowLogger)
    }

    // Called under stateLock so a newer cycle cannot overtake comparison publication.
    private fun publishCacheComparisonLocked(
        cycle: ReaderContentCycle,
        result: CurrentPreviewContentVersionResult
    ) {
        if (comparedGeneration == cycle.generation ||
            result.diagnosticReason == CurrentPreviewContentVersionFailureReason.STALE
        ) return
        comparedGeneration = cycle.generation
        val current = if (result.isReady) result.version else PreviewContentVersion.Unavailable
        val reason = PreviewContentVersionComparator.compare(entryCachedVersion, current)
        val outcome = when (reason) {
            null -> "MATCH"
            PreviewPlaybackRejectionReason.CONTENT_VERSION_UNAVAILABLE -> "UNAVAILABLE"
            else -> "REJECT"
        }
        val detail = reason?.let { " reason=$it" }.orEmpty()
        val failure = result.diagnosticReason?.let { " diagnosticReason=$it" }.orEmpty()
        cacheVersionShadowLogger(
            "generation=${cycle.generation} cachedKind=${entryCachedVersion.logKind()} " +
                "currentKind=${current.logKind()} cachedSize=${entryCachedVersion.size ?: -1L} " +
                "currentSize=${current.size ?: -1L} result=$outcome$detail$failure " +
                "elapsedFromCycleBeginMs=${(elapsedRealtimeMs() - cycleBeginMs).coerceAtLeast(0L)}"
        )
    }

    private fun PreviewContentVersion.logKind(): String = when (this) {
        is PreviewContentVersion.Metadata -> "METADATA"
        is PreviewContentVersion.Fingerprint -> "FINGERPRINT"
        PreviewContentVersion.Unavailable -> "UNAVAILABLE"
    }

    private companion object {
        const val PREVIEW_VERSION_SHADOW_TAG = "PREVIEW_VERSION_SHADOW"
    }
}

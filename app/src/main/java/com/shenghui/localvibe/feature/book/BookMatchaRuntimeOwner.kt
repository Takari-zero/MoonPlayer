package com.shenghui.localvibe.feature.book

import android.content.Context
import android.util.Log
import com.shenghui.localvibe.core.tts.MatchaBookTtsEngine
import com.shenghui.localvibe.core.tts.MatchaPreparedAudio
import com.shenghui.localvibe.core.tts.PcmAudioChunk
import com.shenghui.localvibe.core.tts.StreamingTtsParams
import com.shenghui.localvibe.core.tts.StreamingTtsResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal interface BookMatchaRuntime {
    val isReady: Boolean
    fun isModelAvailable(): Boolean
    suspend fun ensureInitialized(): Result<Unit>
    suspend fun speak(
        text: String,
        params: StreamingTtsParams,
        onStart: () -> Unit,
        onChunk: suspend (PcmAudioChunk) -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ): StreamingTtsResult
    suspend fun prepare(text: String, params: StreamingTtsParams): Result<MatchaPreparedAudio>
    fun stop()
    fun release()
}
private class MatchaBookTtsRuntime(
    context: Context
) : BookMatchaRuntime {
    private val engine = MatchaBookTtsEngine(context.applicationContext)

    override val isReady: Boolean
        get() = engine.isReady

    override fun isModelAvailable(): Boolean = engine.isModelAvailable()

    override suspend fun ensureInitialized(): Result<Unit> = engine.ensureInitialized()

    override suspend fun speak(
        text: String,
        params: StreamingTtsParams,
        onStart: () -> Unit,
        onChunk: suspend (PcmAudioChunk) -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ): StreamingTtsResult = engine.speak(text, params, onStart, onChunk, onDone, onError)

    override suspend fun prepare(
        text: String,
        params: StreamingTtsParams
    ): Result<MatchaPreparedAudio> = engine.prepare(text, params)

    override fun stop() = engine.stop()

    override fun release() = engine.release()
}

class BookMatchaRuntimeOwner internal constructor(
    private val runtime: BookMatchaRuntime,
    private val scope: CoroutineScope
) {
    constructor(context: Context) : this(
        runtime = MatchaBookTtsRuntime(context.applicationContext),
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    )

    private val prepareLock = Any()
    private var prepareJob: Job? = null
    private var released = false

    internal val playbackRuntime: BookMatchaRuntime
        get() = runtime

    val isReady: Boolean
        get() = !released && runtime.isReady

    val isAvailable: Boolean
        get() = !released && runtime.isModelAvailable()

    fun prepareEngine() {
        synchronized(prepareLock) {
            if (released || runtime.isReady || prepareJob?.isActive == true || !runtime.isModelAvailable()) {
                return
            }
            prepareJob = scope.launch {
                runCatching { Log.i("MATCHA_ENGINE_PREPARE", "event=REQUESTED") }
                runtime.ensureInitialized().onFailure {
                    runCatching { Log.w(TAG, "engine preparation failed", it) }
                }
            }
        }
    }

    fun releaseEngineRuntime() {
        synchronized(prepareLock) {
            if (released) return
            released = true
        }
        scope.cancel()
        runtime.stop()
        runtime.release()
    }

    private companion object {
        const val TAG = "BookMatchaRuntimeOwner"
    }
}

internal fun shouldPrepareMatchaRuntime(
    preferred: BookPlaybackEngine,
    effective: BookPlaybackEngine,
    matchaAvailable: Boolean
): Boolean {
    return preferred == BookPlaybackEngine.MATCHA_EXPERIMENTAL &&
        effective == BookPlaybackEngine.MATCHA_EXPERIMENTAL &&
        matchaAvailable
}

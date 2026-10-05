package com.shenghui.localvibe.feature.book

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.shenghui.localvibe.core.tts.BookTtsController
import com.shenghui.localvibe.core.tts.PcmAudioChunk
import com.shenghui.localvibe.core.tts.StreamingTtsResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private typealias PreparedAishell3Playback = (
    Long,
    String,
    List<PcmAudioChunk>,
    Int,
    (Long) -> Unit,
    (Long) -> Unit,
    (Long, String) -> Unit
) -> Unit

private typealias SegmentedAishell3Playback = (
    Long,
    String,
    Int,
    String,
    Int,
    (String) -> Unit,
    (Long) -> Unit,
    (Long) -> Unit,
    (Long, String) -> Unit
) -> Unit

internal class BookSentencePlaybackProviderRouter(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val dispatcher: BookSentencePlaybackDispatcher,
    private val matchaPlaybackCoordinator: BookMatchaPlaybackCoordinator,
    private val ttsController: () -> BookTtsController?,
    private val pitch: () -> Float,
    private val isTtsReady: () -> Boolean,
    private val currentPlaybackSessionId: () -> Long,
    private val preparedKey: (Int, String) -> String,
    private val isPrewarming: (String) -> Boolean,
    private val getPrepared: (String) -> List<PcmAudioChunk>?,
    private val waitPrepared: suspend (String) -> List<PcmAudioChunk>?,
    private val playPrepared: PreparedAishell3Playback,
    private val playSegmented: SegmentedAishell3Playback
) {
    fun dispatch(
        request: BookSentencePlaybackRequest,
        callbacks: BookSentencePlaybackCallbacks,
        formalCallbacksFor: ((BookSentencePlaybackRequest) -> BookSentencePlaybackCallbacks)? = null,
        skipPreparedWait: Boolean = false,
        onPreparedWaitTimeout: (() -> Unit)? = null
    ) {
        lateinit var providerHandlers: BookSentencePlaybackProviderHandlers
        providerHandlers = BookSentencePlaybackProviderHandlers(
            matcha = BookSentencePlaybackProviderHandler { dispatchRequest, dispatchCallbacks ->
                coroutineScope.launch {
                    val matchaRequest = MatchaPlaybackRequest(
                        targetId = dispatchRequest.target,
                        text = dispatchRequest.text,
                        speechRate = dispatchRequest.speechRate,
                        sessionId = dispatchRequest.playbackSessionId,
                        playbackIntentPlaying = dispatchRequest.playbackIntentPlaying,
                        snapshot = dispatchRequest.preparedSnapshot
                    )
                    val result = matchaPlaybackCoordinator.play(
                        request = matchaRequest,
                        onStarted = { dispatchCallbacks.onStarted(dispatchRequest.playbackSessionId) },
                        onFirstWrite = {
                            if (dispatchRequest.allowNextPrewarm) {
                                dispatchRequest.preparedSnapshot?.let { snapshot ->
                                    matchaPlaybackCoordinator.scheduleNextPrewarm(
                                        MatchaPrewarmScheduleRequest(
                                            dispatchRequest.target,
                                            snapshot,
                                            dispatchRequest.speechRate,
                                            dispatchRequest.playbackSessionId
                                        )
                                    )
                                }
                            }
                        }
                    )
                    withContext(Dispatchers.Main.immediate) {
                        if (result is StreamingTtsResult.Success) {
                            dispatchCallbacks.onDrained(dispatchRequest.playbackSessionId)
                        } else {
                            dispatchCallbacks.onFailed(dispatchRequest.playbackSessionId, "Matcha playback stopped")
                        }
                    }
                }
            },
            systemTts = BookSentencePlaybackProviderHandler { dispatchRequest, dispatchCallbacks ->
                val result = ttsController()?.speakSentence(
                    text = dispatchRequest.text,
                    speechRate = dispatchRequest.speechRate,
                    pitch = pitch(),
                    playbackSessionId = dispatchRequest.playbackSessionId
                )
                if (result?.success == true) {
                    dispatchCallbacks.onStarted(dispatchRequest.playbackSessionId)
                } else {
                    dispatchCallbacks.onFailed(
                        dispatchRequest.playbackSessionId,
                        result?.message ?: "系统语音不可用，请安装或启用系统语音引擎"
                    )
                    Toast.makeText(context, "请先安装或启用系统语音", Toast.LENGTH_SHORT).show()
                }
            },
            aishell3 = BookSentencePlaybackProviderHandler { dispatchRequest, dispatchCallbacks ->
                val requestSessionId = dispatchRequest.playbackSessionId
                val requestTarget = dispatchRequest.target
                val requestPreparedKey = preparedKey(requestTarget.chapterSentenceIndex, dispatchRequest.text)
                Log.i("BookListenHot", "play prepared path enter key=$requestPreparedKey")
                getPrepared(requestPreparedKey)?.let { chunks ->
                    playPrepared(
                        requestSessionId,
                        requestPreparedKey,
                        chunks,
                        requestTarget.paragraphIndex,
                        dispatchCallbacks.onStarted,
                        dispatchCallbacks.onDrained,
                        dispatchCallbacks.onFailed
                    )
                    return@BookSentencePlaybackProviderHandler
                }
                val missReason = if (isPrewarming(requestPreparedKey)) {
                    "prewarm_in_progress"
                } else {
                    "cache_empty_or_key_not_ready"
                }
                if (!skipPreparedWait && missReason == "prewarm_in_progress") {
                    coroutineScope.launch {
                        val waitedChunks = waitPrepared(requestPreparedKey)
                        if (requestSessionId != currentPlaybackSessionId()) return@launch
                        if (waitedChunks != null) {
                            playPrepared(
                                requestSessionId,
                                requestPreparedKey,
                                waitedChunks,
                                requestTarget.paragraphIndex,
                                dispatchCallbacks.onStarted,
                                dispatchCallbacks.onDrained,
                                dispatchCallbacks.onFailed
                            )
                        } else {
                            onPreparedWaitTimeout?.invoke()
                        }
                    }
                    return@BookSentencePlaybackProviderHandler
                }
                fun fallbackToSystemTts(reason: String) {
                    Log.d("BookReaderPlayback", "fallback reason=$reason sessionId=$requestSessionId")
                    if (requestSessionId != currentPlaybackSessionId()) return
                    if (dispatchRequest.source == BookSentencePlaybackSource.CACHED_PREVIEW ||
                        formalCallbacksFor == null
                    ) {
                        dispatchCallbacks.onFailed(requestSessionId, reason)
                        return
                    }
                    val systemRequest = dispatchRequest.copy(provider = BookPlaybackEngine.SYSTEM_TTS)
                    if (!isTtsReady()) {
                        formalCallbacksFor.invoke(systemRequest).onFailed(requestSessionId, "请先安装或启用系统语音")
                        Toast.makeText(context, "请先安装或启用系统语音", Toast.LENGTH_SHORT).show()
                        return
                    }
                    dispatcher.dispatch(
                        systemRequest,
                        formalCallbacksFor.invoke(systemRequest),
                        providerHandlers
                    )
                }
                playSegmented(
                    requestSessionId,
                    requestPreparedKey,
                    requestTarget.chapterSentenceIndex,
                    dispatchRequest.text,
                    requestTarget.paragraphIndex,
                    ::fallbackToSystemTts,
                    dispatchCallbacks.onStarted,
                    dispatchCallbacks.onDrained,
                    dispatchCallbacks.onFailed
                )
            }
        )
        dispatcher.dispatch(request, callbacks, providerHandlers)
    }
}

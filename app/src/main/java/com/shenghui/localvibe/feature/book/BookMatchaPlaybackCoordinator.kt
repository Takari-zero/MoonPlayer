package com.shenghui.localvibe.feature.book

import android.util.Log
import com.shenghui.localvibe.core.tts.MatchaPrewarmKey
import com.shenghui.localvibe.core.tts.MatchaPrewarmSlot
import com.shenghui.localvibe.core.tts.PcmAudioChunk
import com.shenghui.localvibe.core.tts.StreamingPcmAudioPlayer
import com.shenghui.localvibe.core.tts.StreamingTtsParams
import com.shenghui.localvibe.core.tts.StreamingTtsResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class BookPlaybackTargetId(
    val chapterSentenceIndex: Int,
    val paragraphIndex: Int,
    val sentenceIndexInParagraph: Int,
    val isChapterTitle: Boolean = false
)

internal data class MatchaPlaybackRequest(
    val targetId: BookPlaybackTargetId,
    val text: String,
    val speechRate: Float,
    val sessionId: Long,
    val playbackIntentPlaying: Boolean,
    val snapshot: BookSequentialTargetSnapshot? = null
)

internal data class MatchaPrewarmScheduleRequest(
    val currentTarget: BookPlaybackTargetId,
    val snapshot: BookSequentialTargetSnapshot,
    val speechRate: Float,
    val sessionId: Long
)

internal class BookMatchaPlaybackCoordinator(
    private val runtimeOwner: BookMatchaRuntimeOwner
) {
    private val engine = runtimeOwner.playbackRuntime
    private val prewarmSlot = MatchaPrewarmSlot()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prepareLock = Any()
    private var released = false
    private var player: StreamingPcmAudioPlayer? = null

    val isAvailable: Boolean
        get() = engine.isModelAvailable()

    fun prepareEngine() {
        if (!released) runtimeOwner.prepareEngine()
    }

    suspend fun play(
        request: MatchaPlaybackRequest,
        onStarted: suspend () -> Unit,
        onFirstWrite: suspend () -> Unit
    ): StreamingTtsResult = withContext(Dispatchers.IO) {
        if (!request.playbackIntentPlaying) return@withContext StreamingTtsResult.Stopped
        replacePlayerOutput()
        val prepared = request.snapshot?.let { snapshot ->
            val key = keyFor(request.targetId, request.text, request.speechRate, prewarmSlot.currentEpoch())
            prewarmSlot.awaitAndConsume(key, PREWARM_WAIT_MS)
        }
        if (prepared != null) {
            Log.i(TAG, "MATCHA_PREWARM_CONSUMED target=${request.targetId.chapterSentenceIndex}")
            return@withContext playChunk(request, prepared.chunk, onStarted, onFirstWrite)
        }
        return@withContext speakFresh(request, onStarted, onFirstWrite)
    }

    fun scheduleNextPrewarm(request: MatchaPrewarmScheduleRequest) {
        val next = resolveMatchaNextPrewarmTarget(request.currentTarget, request.snapshot) ?: return
        if (next.isChapterTitle) return
        val sentence = request.snapshot.chapters.asSequence()
            .flatMap { it.sentences.asSequence() }
            .firstOrNull { it.paragraphIndex == next.paragraphIndex && it.sentenceIndexInParagraph == next.sentenceIndexInParagraph }
            ?: return
        val target = BookPlaybackTargetId(sentence.chapterSentenceIndex, sentence.paragraphIndex, sentence.sentenceIndexInParagraph)
        val key = keyFor(target, sentence.text, request.speechRate, prewarmSlot.currentEpoch())
        val pending = prewarmSlot.begin(key, request.sessionId) ?: return
        scope.launch {
            val audio = engine.prepare(
                text = sentence.text,
                params = StreamingTtsParams("matcha-default", request.speechRate, 1f, 1f)
            ).getOrNull()
            if (!prewarmSlot.complete(pending, audio)) {
                Log.i(TAG, "MATCHA_PREWARM_STALE target=${target.chapterSentenceIndex}")
            } else if (audio == null) {
                Log.i(TAG, "MATCHA_PREWARM_MISS target=${target.chapterSentenceIndex}")
            } else {
                Log.i(TAG, "MATCHA_PREWARM_READY target=${target.chapterSentenceIndex}")
            }
        }
    }

    fun invalidatePrewarm(reason: String) {
        val epoch = prewarmSlot.invalidate()
        runCatching { Log.i(TAG, "MATCHA_PREWARM_INVALIDATED reason=$reason epoch=$epoch") }
    }

    fun pause(): Boolean {
        invalidatePrewarm("pause")
        val currentPlayer = player ?: return false
        currentPlayer.pause()
        return true
    }

    fun resume(): Boolean {
        val currentPlayer = player ?: return false
        currentPlayer.resume()
        return true
    }

    fun stop() {
        invalidatePrewarm("stop")
        engine.stop()
        val currentPlayer = player
        player = null
        currentPlayer?.stop()
        currentPlayer?.release()
    }

    fun releasePlaybackState() {
        synchronized(prepareLock) {
            if (released) return
            released = true
        }
        invalidatePrewarm("release")
        scope.coroutineContext.cancel()
        engine.stop()
        val currentPlayer = player
        player = null
        currentPlayer?.stop()
        currentPlayer?.release()
    }

    private suspend fun speakFresh(
        request: MatchaPlaybackRequest,
        onStarted: suspend () -> Unit,
        onFirstWrite: suspend () -> Unit
    ): StreamingTtsResult {
        val nextPlayer = StreamingPcmAudioPlayer()
        player = nextPlayer
        var firstWrite = false
        val result = engine.speak(
            text = request.text,
            params = StreamingTtsParams("matcha-default", request.speechRate, 1f, 1f),
            onStart = {},
            onChunk = { chunk ->
                if (!firstWrite) {
                    firstWrite = true
                    nextPlayer.startSession(request.sessionId, chunk.format).getOrThrow()
                    withContext(Dispatchers.Main.immediate) { onStarted() }
                    nextPlayer.write(request.sessionId, chunk).getOrThrow()
                    withContext(Dispatchers.Main.immediate) { onFirstWrite() }
                } else {
                    nextPlayer.write(request.sessionId, chunk).getOrThrow()
                }
            },
            onDone = {},
            onError = {}
        )
        return finish(request, nextPlayer, result)
    }

    private suspend fun playChunk(
        request: MatchaPlaybackRequest,
        chunk: PcmAudioChunk,
        onStarted: suspend () -> Unit,
        onFirstWrite: suspend () -> Unit
    ): StreamingTtsResult {
        val nextPlayer = StreamingPcmAudioPlayer()
        player = nextPlayer
        withContext(Dispatchers.Main.immediate) { onStarted() }
        nextPlayer.startSession(request.sessionId, chunk.format).getOrThrow()
        nextPlayer.write(request.sessionId, chunk).getOrThrow()
        withContext(Dispatchers.Main.immediate) { onFirstWrite() }
        return finish(request, nextPlayer, StreamingTtsResult.Success)
    }

    private suspend fun finish(
        request: MatchaPlaybackRequest,
        nextPlayer: StreamingPcmAudioPlayer,
        result: StreamingTtsResult
    ): StreamingTtsResult {
        if (result !is StreamingTtsResult.Success) return result
        return if (nextPlayer.awaitSessionPlaybackComplete(request.sessionId)) result else StreamingTtsResult.Stopped
    }

    private fun keyFor(
        target: BookPlaybackTargetId,
        text: String,
        speed: Float,
        epoch: Long
    ): MatchaPrewarmKey {
        return MatchaPrewarmKey(
            chapterSentenceIndex = target.chapterSentenceIndex,
            paragraphIndex = target.paragraphIndex,
            sentenceIndexInParagraph = target.sentenceIndexInParagraph,
            textHash = com.shenghui.localvibe.core.tts.BookTtsTextNormalizer.normalize(text).spokenText.hashCode(),
            provider = BookPlaybackEngine.MATCHA_EXPERIMENTAL.name,
            speed = speed,
            epoch = epoch
        )
    }

    private fun replacePlayerOutput() {
        val currentPlayer = player
        player = null
        currentPlayer?.stop()
        currentPlayer?.release()
    }

    private companion object {
        const val PREWARM_WAIT_MS = 10_000L
        const val TAG = "BookMatchaCoordinator"
    }

}

internal fun resolveMatchaNextPrewarmTarget(
    currentTarget: BookPlaybackTargetId,
    snapshot: BookSequentialTargetSnapshot
): BookSequentialTarget? = BookSequentialNextTargetResolver.resolve(
    current = BookSequentialTarget(
        paragraphIndex = currentTarget.paragraphIndex,
        sentenceIndexInParagraph = currentTarget.sentenceIndexInParagraph,
        isChapterTitle = currentTarget.isChapterTitle
    ),
    snapshot = snapshot
)

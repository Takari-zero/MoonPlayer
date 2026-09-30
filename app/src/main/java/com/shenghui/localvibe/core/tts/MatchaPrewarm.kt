package com.shenghui.localvibe.core.tts

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

data class MatchaPreparedAudio(
    val chunk: PcmAudioChunk,
    val audioDurationMs: Long,
    val generateMs: Long,
    val lockWaitMs: Long,
    val provider: String,
    val speed: Float,
    val textHash: Int
)

data class MatchaPrewarmKey(
    val chapterSentenceIndex: Int,
    val paragraphIndex: Int,
    val sentenceIndexInParagraph: Int,
    val textHash: Int,
    val provider: String,
    val speed: Float,
    val epoch: Long
)

data class MatchaPrewarmRequest(
    val key: MatchaPrewarmKey,
    val originSessionId: Long,
    val result: CompletableDeferred<MatchaPreparedAudio?>
)

/** Single-slot state for Matcha prewarm; stale results cannot become playable. */
class MatchaPrewarmSlot {
    private val lock = Any()
    private var epoch = 0L
    private var request: MatchaPrewarmRequest? = null
    private var ready: MatchaPreparedAudio? = null
    private var readyKey: MatchaPrewarmKey? = null
    private var consumed = false

    fun currentEpoch(): Long = synchronized(lock) { epoch }

    fun invalidate(): Long = synchronized(lock) {
        epoch += 1L
        request?.result?.complete(null)
        request = null
        ready = null
        readyKey = null
        consumed = false
        epoch
    }

    fun begin(key: MatchaPrewarmKey, originSessionId: Long): MatchaPrewarmRequest? = synchronized(lock) {
        if (key.epoch != epoch) return null
        if (request != null || ready != null) return null
        val next = MatchaPrewarmRequest(key, originSessionId, CompletableDeferred())
        request = next
        consumed = false
        next
    }

    fun inFlight(key: MatchaPrewarmKey): Deferred<MatchaPreparedAudio?>? = synchronized(lock) {
        request?.takeIf { it.key == key }?.result
    }

    fun complete(requestToComplete: MatchaPrewarmRequest, audio: MatchaPreparedAudio?): Boolean = synchronized(lock) {
        val current = request
        val accepted = current === requestToComplete && requestToComplete.key.epoch == epoch
        if (accepted) {
            request = null
            if (audio != null) {
                ready = audio
                readyKey = requestToComplete.key
                consumed = false
            }
        }
        requestToComplete.result.complete(if (accepted) audio else null)
        accepted
    }

    fun consume(key: MatchaPrewarmKey): MatchaPreparedAudio? = synchronized(lock) {
        if (consumed || readyKey != key || key.epoch != epoch) return null
        consumed = true
        val result = ready
        ready = null
        readyKey = null
        result
    }

    fun dropIfTargetMismatch(formalTarget: Int): MatchaPrewarmKey? = synchronized(lock) {
        val currentKey = readyKey ?: return@synchronized null
        if (currentKey.chapterSentenceIndex == formalTarget) return@synchronized null
        ready = null
        readyKey = null
        consumed = false
        currentKey
    }

    fun clearIf(requestToClear: MatchaPrewarmRequest) = synchronized(lock) {
        if (request === requestToClear) {
            request = null
            requestToClear.result.complete(null)
        }
    }
}

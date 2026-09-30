package com.shenghui.localvibe.core.tts

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.withTimeoutOrNull

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
    private var readyRequest: MatchaPrewarmRequest? = null
    private var consumed = false

    fun currentEpoch(): Long = synchronized(lock) { epoch }

    fun invalidate(): Long = synchronized(lock) {
        epoch += 1L
        request?.result?.complete(null)
        request = null
        clearReadyLocked()
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

    /** Waits for the exact request, then atomically releases its prepared audio. */
    suspend fun awaitAndConsume(
        key: MatchaPrewarmKey,
        timeoutMs: Long
    ): MatchaPreparedAudio? {
        val direct = synchronized(lock) { consumeReadyLocked(key) }
        if (direct != null) return direct

        val pending = synchronized(lock) {
            if (key.epoch != epoch) null else request?.takeIf { it.key == key }
        } ?: return null

        return try {
            val completed = withTimeoutOrNull(timeoutMs) { pending.result.await() }
            if (completed == null) {
                null
            } else {
                synchronized(lock) {
                    if (epoch != key.epoch || readyKey != key || readyRequest !== pending) {
                        null
                    } else {
                        consumeReadyLocked(key)
                    }
                }
            }
        } finally {
            abandon(pending)
        }
    }

    fun complete(requestToComplete: MatchaPrewarmRequest, audio: MatchaPreparedAudio?): Boolean = synchronized(lock) {
        val current = request
        val accepted = current === requestToComplete && requestToComplete.key.epoch == epoch
        if (accepted) {
            request = null
            if (audio != null) {
                ready = audio
                readyKey = requestToComplete.key
                readyRequest = requestToComplete
                consumed = false
            } else {
                clearReadyLocked()
            }
        }
        requestToComplete.result.complete(if (accepted) audio else null)
        accepted
    }

    /** Abandons only this exact request and any READY result produced by it. */
    fun abandon(requestToAbandon: MatchaPrewarmRequest): Boolean = synchronized(lock) {
        var changed = false
        if (request === requestToAbandon) {
            request = null
            changed = true
        }
        if (readyRequest === requestToAbandon) {
            clearReadyLocked()
            changed = true
        }
        requestToAbandon.result.complete(null)
        changed
    }

    fun consume(key: MatchaPrewarmKey): MatchaPreparedAudio? = synchronized(lock) {
        if (consumed || readyKey != key || key.epoch != epoch) return null
        consumed = true
        val result = ready
        clearReadyLocked()
        result
    }

    fun dropIfTargetMismatch(formalTarget: Int): MatchaPrewarmKey? = synchronized(lock) {
        val currentKey = readyKey ?: return@synchronized null
        if (currentKey.chapterSentenceIndex == formalTarget) return@synchronized null
        clearReadyLocked()
        currentKey
    }

    fun clearIf(requestToClear: MatchaPrewarmRequest) = synchronized(lock) {
        abandon(requestToClear)
    }

    private fun clearReadyLocked() {
        ready = null
        readyKey = null
        readyRequest = null
        consumed = false
    }

    private fun consumeReadyLocked(key: MatchaPrewarmKey): MatchaPreparedAudio? {
        if (consumed || readyKey != key || key.epoch != epoch) return null
        consumed = true
        val result = ready
        clearReadyLocked()
        return result
    }
}

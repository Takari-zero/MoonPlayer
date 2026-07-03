package com.shenghui.localvibe.feature.book.playback

import kotlin.system.measureTimeMillis

class BookReaderPlaybackController(
    private val engine: BookTtsEngine,
    private val audioSink: BookReaderAudioSink = NoopBookReaderAudioSink(),
    private val chapterNavigator: BookReaderChapterNavigator = NoopBookReaderChapterNavigator,
    private val logger: (String) -> Unit = {},
    private val eventSink: (BookReaderPlaybackEvent) -> Unit = {},
) {
    var state: BookReaderPlaybackState = BookReaderPlaybackState()
        private set

    private var nextSessionId = 0L

    suspend fun play(target: BookReaderPlaybackTarget) {
        val sessionId = nextSessionId()
        state = state.copy(
            isPlaying = false,
            isPaused = false,
            activeSessionId = sessionId,
            target = target,
            lastError = null,
        )
        log("play sessionId=$sessionId chapterIndex=${target.chapterIndex} paragraphIndex=${target.paragraphIndex}")

        val synthText = target.sentenceText
        val synthPreview = synthText.replace('\n', ' ').trim().take(40)
        log("synth text preview sessionId=$sessionId length=${synthText.length} preview=$synthPreview")
        var result: Result<BookTtsPcmResult>
        val synthCostMs = measureTimeMillis {
            log("synth start sessionId=$sessionId")
            result = engine.synthesizeToPcm(
                target = target,
                text = synthText,
                sessionId = sessionId,
            )
        }
        log("synth done sessionId=$sessionId costMs=$synthCostMs success=${result.isSuccess}")

        if (isStale(sessionId)) return

        result
            .onSuccess { pcm ->
                state = state.copy(isPlaying = true, isPaused = false, canResume = true, lastError = null)
                eventSink(BookReaderPlaybackEvent.PlaybackStarted(sessionId, target))
                audioSink.playPcm(
                    sessionId = sessionId,
                    sampleRate = pcm.sampleRate,
                    pcm16 = pcm.pcm16,
                )
                if (isStale(sessionId)) {
                    log("playback complete ignored reason=stale_session sessionId=$sessionId active=${state.activeSessionId}")
                    return
                }
                if (state.isPaused) {
                    state = state.copy(canResume = false)
                    log("playback complete ignored reason=paused sessionId=$sessionId canResume=false")
                    return
                }
                if (!state.isPlaying) {
                    log("playback complete ignored reason=stopped sessionId=$sessionId")
                    return
                }
                log(
                    "playback completed sessionId=$sessionId chapterIndex=${target.chapterIndex} " +
                        "paragraphIndex=${target.paragraphIndex} sentenceIndex=${target.sentenceIndex}"
                )
                eventSink(BookReaderPlaybackEvent.PlaybackCompleted(sessionId, target))
            }
            .onFailure { error ->
                state = state.copy(isPlaying = false, isPaused = false, canResume = false, lastError = error.message)
                eventSink(BookReaderPlaybackEvent.Error(sessionId, error.message ?: "unknown"))
            }
    }

    fun pause() {
        val pausedSessionId = state.activeSessionId
        if (!state.canResume) {
            state = state.copy(isPlaying = false, isPaused = false, activeSessionId = pausedSessionId)
            log("pause skipped reason=no_resumable_audio sessionId=$pausedSessionId")
            return
        }
        audioSink.pause()
        state = state.copy(isPlaying = false, isPaused = true, canResume = true, activeSessionId = pausedSessionId)
        log("pause sessionId=$pausedSessionId")
    }

    suspend fun resume() {
        val resumedSessionId = state.activeSessionId
        if (state.target == null || !state.canResume) {
            state = state.copy(isPlaying = false, isPaused = false, activeSessionId = resumedSessionId)
            log("resume skipped reason=no_resumable_audio sessionId=$resumedSessionId")
            return
        }
        audioSink.resume()
        state = state.copy(isPlaying = true, isPaused = false)
        log("resume started sessionId=$resumedSessionId")
    }

    fun stop(reason: String) {
        val stoppedSessionId = invalidateSession()
        audioSink.stop(reason)
        state = state.copy(isPlaying = false, isPaused = false, canResume = false, activeSessionId = stoppedSessionId)
        log("stop reason=$reason sessionId=$stoppedSessionId")
        eventSink(BookReaderPlaybackEvent.PlaybackStopped(stoppedSessionId, reason))
    }

    suspend fun seekTo(target: BookReaderPlaybackTarget, resumeIfPlaying: Boolean) {
        jumpToTarget(target = target, resumeIfPlaying = resumeIfPlaying, reason = "seek")
    }

    suspend fun jumpToSentence(target: BookReaderPlaybackTarget, resumeIfPlaying: Boolean) {
        jumpToTarget(target = target, resumeIfPlaying = resumeIfPlaying, reason = "jump_to_sentence")
    }

    suspend fun jumpToChapter(target: BookReaderPlaybackTarget, resumeIfPlaying: Boolean) {
        jumpToTarget(target = target, resumeIfPlaying = resumeIfPlaying, reason = "jump_to_chapter")
    }

    suspend fun nextChapter() {
        val current = state.target ?: return
        val target = chapterNavigator.nextChapter(current)
        if (target == null) {
            log("chapter jump no-op reason=no_next_chapter currentChapter=${current.chapterIndex}")
            return
        }
        jumpToChapter(target, resumeIfPlaying = state.isPlaying)
    }

    suspend fun previousChapter() {
        val current = state.target ?: return
        val target = chapterNavigator.previousChapter(current)
        if (target == null) {
            log("chapter jump no-op reason=no_previous_chapter currentChapter=${current.chapterIndex}")
            return
        }
        jumpToChapter(target, resumeIfPlaying = state.isPlaying)
    }

    fun setMode(mode: BookReaderPlaybackMode) {
        val previous = state.mode
        state = state.copy(mode = mode)
        log("mode switch from=$previous to=$mode")
        eventSink(BookReaderPlaybackEvent.ModeChanged(previous, mode))
    }

    suspend fun onChapterEnd(
        currentChapterFirstTarget: BookReaderPlaybackTarget?,
        nextChapterFirstTarget: BookReaderPlaybackTarget?,
        sessionId: Long = state.activeSessionId,
    ) {
        if (isStale(sessionId)) return

        when (state.mode) {
            BookReaderPlaybackMode.SEQUENTIAL -> {
                if (nextChapterFirstTarget != null) {
                    log("chapter end mode=SEQUENTIAL action=next")
                    play(nextChapterFirstTarget)
                } else {
                    log("chapter end mode=SEQUENTIAL action=stop")
                    stop("chapter_end")
                }
            }

            BookReaderPlaybackMode.CHAPTER_LOOP -> {
                if (currentChapterFirstTarget != null) {
                    log("chapter end mode=CHAPTER_LOOP action=loop")
                    play(currentChapterFirstTarget)
                } else {
                    log("chapter end mode=CHAPTER_LOOP action=stop")
                    stop("chapter_end_missing_current")
                }
            }

            BookReaderPlaybackMode.STOP_AFTER_CHAPTER -> {
                val stoppedSessionId = invalidateSession()
                audioSink.stop("chapter_end_stop_after_chapter")
                state = state.copy(
                    isPlaying = false,
                    isPaused = true,
                    activeSessionId = stoppedSessionId,
                )
                log("chapter end mode=STOP_AFTER_CHAPTER action=stop")
            }
        }
    }

    fun release() {
        val releasedSessionId = invalidateSession()
        audioSink.release()
        engine.release()
        state = state.copy(isPlaying = false, isPaused = false, canResume = false, activeSessionId = releasedSessionId)
        log("release sessionId=$releasedSessionId")
    }

    private suspend fun jumpToTarget(
        target: BookReaderPlaybackTarget,
        resumeIfPlaying: Boolean,
        reason: String,
    ) {
        stop(reason)
        state = state.copy(target = target, canResume = false)
        if (resumeIfPlaying) {
            play(target)
        } else {
            state = state.copy(isPlaying = false, isPaused = false, canResume = false, target = target)
            log("target selected reason=$reason resumeIfPlaying=false canResume=false")
        }
    }

    private fun isStale(sessionId: Long): Boolean {
        val activeSessionId = state.activeSessionId
        if (sessionId == activeSessionId) return false

        log("stale session ignored sessionId=$sessionId active=$activeSessionId")
        eventSink(BookReaderPlaybackEvent.StaleSessionIgnored(sessionId, activeSessionId))
        return true
    }

    private fun nextSessionId(): Long {
        nextSessionId += 1
        return nextSessionId
    }

    private fun invalidateSession(): Long = nextSessionId()

    private fun log(message: String) {
        logger("LV_BOOK_PLAYBACK $message")
    }
}

interface BookTtsEngine {
    suspend fun synthesizeToPcm(
        target: BookReaderPlaybackTarget,
        text: String,
        sessionId: Long,
    ): Result<BookTtsPcmResult>

    fun release()
}

data class BookTtsPcmResult(
    val sampleRate: Int,
    val pcm16: ByteArray,
    val channelCount: Int = 1,
    val clauseIndex: Int = 0,
    val durationMs: Long = 0L,
    val rtf: Double = 0.0,
)

interface BookReaderChapterNavigator {
    fun nextChapter(current: BookReaderPlaybackTarget): BookReaderPlaybackTarget?
    fun previousChapter(current: BookReaderPlaybackTarget): BookReaderPlaybackTarget?
}

object NoopBookReaderChapterNavigator : BookReaderChapterNavigator {
    override fun nextChapter(current: BookReaderPlaybackTarget): BookReaderPlaybackTarget? = null
    override fun previousChapter(current: BookReaderPlaybackTarget): BookReaderPlaybackTarget? = null
}


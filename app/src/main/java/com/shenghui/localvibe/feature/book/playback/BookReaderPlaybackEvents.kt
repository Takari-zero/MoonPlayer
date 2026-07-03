package com.shenghui.localvibe.feature.book.playback

sealed interface BookReaderPlaybackEvent {
    data class PlaybackStarted(
        val sessionId: Long,
        val target: BookReaderPlaybackTarget,
    ) : BookReaderPlaybackEvent

    data class PlaybackCompleted(
        val sessionId: Long,
        val target: BookReaderPlaybackTarget,
    ) : BookReaderPlaybackEvent

    data class PlaybackStopped(
        val sessionId: Long,
        val reason: String,
    ) : BookReaderPlaybackEvent

    data class StaleSessionIgnored(
        val sessionId: Long,
        val activeSessionId: Long,
    ) : BookReaderPlaybackEvent

    data class ModeChanged(
        val from: BookReaderPlaybackMode,
        val to: BookReaderPlaybackMode,
    ) : BookReaderPlaybackEvent

    data class Error(
        val sessionId: Long,
        val message: String,
    ) : BookReaderPlaybackEvent
}

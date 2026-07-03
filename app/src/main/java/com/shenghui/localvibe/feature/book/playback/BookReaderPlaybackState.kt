package com.shenghui.localvibe.feature.book.playback

data class BookReaderPlaybackState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val canResume: Boolean = false,
    val activeSessionId: Long = 0L,
    val target: BookReaderPlaybackTarget? = null,
    val mode: BookReaderPlaybackMode = BookReaderPlaybackMode.SEQUENTIAL,
    val engineName: String = "FastSpeech2",
    val lastError: String? = null,
)

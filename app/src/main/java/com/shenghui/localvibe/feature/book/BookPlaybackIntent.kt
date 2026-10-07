package com.shenghui.localvibe.feature.book

internal object BookPlaybackIntent {
    fun shouldAutoPlayAfterSentenceTap(playbackIntentPlaying: Boolean): Boolean {
        return playbackIntentPlaying
    }

    fun afterPlaybackAdvance(moved: Boolean, playbackIntentPlaying: Boolean): Boolean {
        return moved && playbackIntentPlaying
    }
}

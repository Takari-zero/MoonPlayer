package com.shenghui.localvibe.core.player

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class MusicPlaybackService : MediaSessionService() {
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val audioPlayer = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            setHandleAudioBecomingNoisy(true)
            addListener(
                object : Player.Listener {
                    override fun onAudioSessionIdChanged(audioSessionId: Int) {
                        currentAudioSessionId = audioSessionId
                    }
                }
            )
        }
        player = audioPlayer
        mediaSession = MediaSession.Builder(this, audioPlayer).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        val currentPlayer = player
        if (currentPlayer == null || !currentPlayer.playWhenReady) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        currentAudioSessionId = C.AUDIO_SESSION_ID_UNSET
        super.onDestroy()
    }

    companion object {
        @Volatile
        var currentAudioSessionId: Int = C.AUDIO_SESSION_ID_UNSET
            private set
    }
}

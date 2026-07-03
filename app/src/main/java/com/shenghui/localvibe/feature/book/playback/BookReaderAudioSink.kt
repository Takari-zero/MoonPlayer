package com.shenghui.localvibe.feature.book.playback

interface BookReaderAudioSink {
    suspend fun playPcm(
        sessionId: Long,
        sampleRate: Int,
        pcm16: ByteArray,
    )

    fun pause()
    fun resume()
    fun stop(reason: String)
    fun release()
}

class NoopBookReaderAudioSink : BookReaderAudioSink {
    override suspend fun playPcm(sessionId: Long, sampleRate: Int, pcm16: ByteArray) = Unit
    override fun pause() = Unit
    override fun resume() = Unit
    override fun stop(reason: String) = Unit
    override fun release() = Unit
}

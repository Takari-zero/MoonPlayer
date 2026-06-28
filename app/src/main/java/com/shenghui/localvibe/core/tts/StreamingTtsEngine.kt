package com.shenghui.localvibe.core.tts

interface StreamingTtsEngine {
    val name: String
    val isReady: Boolean

    suspend fun initialize(): Result<Unit>

    suspend fun speak(
        text: String,
        params: StreamingTtsParams,
        onStart: () -> Unit,
        onChunk: suspend (PcmAudioChunk) -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ): StreamingTtsResult

    fun stop()

    fun release()
}

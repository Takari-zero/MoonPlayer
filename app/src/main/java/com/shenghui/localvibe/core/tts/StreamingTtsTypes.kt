package com.shenghui.localvibe.core.tts

enum class PcmAudioEncoding {
    PCM_16BIT
}

data class PcmAudioFormat(
    val sampleRate: Int,
    val channelCount: Int,
    val encoding: PcmAudioEncoding = PcmAudioEncoding.PCM_16BIT
)

data class PcmAudioChunk(
    val data: ByteArray,
    val format: PcmAudioFormat,
    val isFinal: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PcmAudioChunk

        if (!data.contentEquals(other.data)) return false
        if (format != other.format) return false
        if (isFinal != other.isFinal) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + format.hashCode()
        result = 31 * result + isFinal.hashCode()
        return result
    }
}

data class StreamingTtsParams(
    val voiceId: String,
    val speed: Float = 1f,
    val pitch: Float = 1f,
    val volume: Float = 1f
)

sealed class StreamingTtsResult {
    data object Success : StreamingTtsResult()
    data object Stopped : StreamingTtsResult()
    data class Error(val message: String) : StreamingTtsResult()
}

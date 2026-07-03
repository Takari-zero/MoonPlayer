package com.shenghui.localvibe.core.tts.fastspeech2

data class FastSpeech2SynthesisResult(
    val sampleRate: Int = FastSpeech2BookTtsEngine.SAMPLE_RATE,
    val channelCount: Int = 1,
    val pcm16: ByteArray,
    val clauses: List<FastSpeech2SynthesisClause>,
    val durationMs: Long,
    val rtf: Double,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FastSpeech2SynthesisResult) return false
        return sampleRate == other.sampleRate &&
            channelCount == other.channelCount &&
            pcm16.contentEquals(other.pcm16) &&
            clauses == other.clauses &&
            durationMs == other.durationMs &&
            rtf == other.rtf
    }

    override fun hashCode(): Int {
        var result = sampleRate
        result = 31 * result + channelCount
        result = 31 * result + pcm16.contentHashCode()
        result = 31 * result + clauses.hashCode()
        result = 31 * result + durationMs.hashCode()
        result = 31 * result + rtf.hashCode()
        return result
    }
}

data class FastSpeech2SynthesisClause(
    val clauseIndex: Int,
    val text: String,
    val pcmByteCount: Int,
    val durationMs: Long,
    val preprocessCostMs: Long,
    val fastSpeech2CostMs: Long,
    val mbMelGanCostMs: Long,
)
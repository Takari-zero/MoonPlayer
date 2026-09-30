package com.shenghui.localvibe.core.tts

data class Aishell3AuditionCandidate(
    val slot: Int,
    val voice: BookOfflineVoiceDescriptor
)

object Aishell3AuditionCandidates {
    private val CANDIDATE_SPEAKER_IDS = listOf(0, 10, 20, 35, 50, 65, 80, 95, 110, 125, 145, 165)
    const val SAMPLE_TEXT = "你好，这是离线语音试听。清晨时分，我们继续阅读这本书。"

    fun from(registry: Aishell3VoiceRegistrySnapshot?): List<Aishell3AuditionCandidate> {
        return CANDIDATE_SPEAKER_IDS.mapIndexed { index, speakerId ->
            val voice = registry?.voices?.getOrNull(speakerId)
                ?: BookOfflineVoiceDescriptor(
                    voiceId = Aishell3VoiceRegistry.voiceIdFor(speakerId),
                    providerId = Aishell3VoiceRegistry.PROVIDER_ID,
                    modelId = Aishell3VoiceRegistry.MODEL_ID,
                    displayName = "自研离线",
                    localeTag = "zh-CN",
                    speakerId = speakerId,
                    speakerLabel = null,
                    available = false,
                    unavailableReason = if (registry == null) {
                        "音色元数据不可用"
                    } else {
                        "当前模型不包含该 speaker"
                    }
                )
            Aishell3AuditionCandidate(slot = index + 1, voice = voice)
        }
    }

    fun candidateSpeakerIds(): List<Int> = CANDIDATE_SPEAKER_IDS

    fun acceptsCallback(activeSessionId: Long, callbackSessionId: Long): Boolean =
        activeSessionId == callbackSessionId
}

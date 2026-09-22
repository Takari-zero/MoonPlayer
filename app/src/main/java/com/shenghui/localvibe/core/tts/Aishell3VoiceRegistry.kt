package com.shenghui.localvibe.core.tts

import android.content.res.AssetManager

object Aishell3VoiceRegistry {
    const val PROVIDER_ID = "AISHELL3"
    const val MODEL_ID = "aishell3"
    const val DEFAULT_AISHELL3_SPEAKER_ID = 10
    const val DEFAULT_AISHELL3_VOICE_ID = "aishell3-speaker-10"

    fun parseSpeakerLabels(lines: Iterable<String>): List<String> {
        val labels = lines.map { it.trim() }.filter { it.isNotEmpty() }.toList()
        require(labels.none { it.contains(Regex("\\s")) }) { "speaker labels must not contain whitespace" }
        require(labels.distinct().size == labels.size) { "speaker labels must be unique" }
        return labels
    }

    fun fromSpeakerLabels(
        lines: Iterable<String>,
        available: Boolean = true,
        unavailableReason: String? = null
    ): Aishell3VoiceRegistrySnapshot {
        val labels = parseSpeakerLabels(lines)
        val voices = labels.mapIndexed { speakerId, label ->
            BookOfflineVoiceDescriptor(
                voiceId = voiceIdFor(speakerId),
                providerId = PROVIDER_ID,
                modelId = MODEL_ID,
                displayName = "自研离线 $label",
                localeTag = "zh-CN",
                speakerId = speakerId,
                speakerLabel = label,
                available = available,
                unavailableReason = unavailableReason
            )
        }
        return Aishell3VoiceRegistrySnapshot(voices)
    }

    fun fromAsset(assetManager: AssetManager): Aishell3VoiceRegistrySnapshot {
        return fromSpeakerLabels(
            assetManager.open("offline_tts/aishell3/speakers.txt")
                .bufferedReader()
                .use { it.readLines() }
        )
    }

    fun defaultDescriptor(): BookOfflineVoiceDescriptor {
        return BookOfflineVoiceDescriptor(
            voiceId = DEFAULT_AISHELL3_VOICE_ID,
            providerId = PROVIDER_ID,
            modelId = MODEL_ID,
            displayName = "自研离线",
            localeTag = "zh-CN",
            speakerId = DEFAULT_AISHELL3_SPEAKER_ID,
            speakerLabel = null,
            available = true
        )
    }

    fun voiceIdFor(speakerId: Int): String = "aishell3-speaker-$speakerId"
}

class Aishell3VoiceRegistrySnapshot internal constructor(
    val voices: List<BookOfflineVoiceDescriptor>
) {
    val numSpeakers: Int get() = voices.size

    fun resolve(voiceId: String?): BookOfflineVoiceDescriptor? =
        voices.firstOrNull { it.voiceId == voiceId }

    fun resolveOrDefault(voiceId: String?): BookOfflineVoiceDescriptor? {
        return resolve(voiceId) ?: resolve(Aishell3VoiceRegistry.DEFAULT_AISHELL3_VOICE_ID)
    }

    fun resolveSpeakerIdOrDefault(voiceId: String?): Int? =
        resolveOrDefault(voiceId)?.takeIf { it.speakerId in voices.indices }?.speakerId
}

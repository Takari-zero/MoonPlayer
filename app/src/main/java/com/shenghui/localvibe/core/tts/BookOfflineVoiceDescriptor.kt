package com.shenghui.localvibe.core.tts

data class BookOfflineVoiceDescriptor(
    val voiceId: String,
    val providerId: String,
    val modelId: String,
    val displayName: String,
    val localeTag: String,
    val speakerId: Int,
    val speakerLabel: String?,
    val available: Boolean,
    val unavailableReason: String? = null
)

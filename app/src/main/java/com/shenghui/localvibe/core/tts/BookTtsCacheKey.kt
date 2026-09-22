package com.shenghui.localvibe.core.tts

object BookTtsCacheKey {
    fun aishell3(
        bookUri: String,
        chapterSentenceIndex: Int,
        textIdentity: Int,
        speed: Float,
        pitch: Float,
        voice: BookOfflineVoiceDescriptor
    ): String {
        return listOf(
            voice.providerId,
            voice.modelId,
            voice.voiceId,
            voice.speakerId,
            bookUri,
            chapterSentenceIndex,
            textIdentity,
            speed,
            pitch
        ).joinToString("|")
    }

    fun aishell3Segment(
        bookUri: String,
        chapterSentenceIndex: Int,
        segmentIndex: Int,
        textIdentity: Int,
        speed: Float,
        pitch: Float,
        voice: BookOfflineVoiceDescriptor
    ): String {
        return aishell3(bookUri, chapterSentenceIndex, textIdentity, speed, pitch, voice) +
            "|seg=$segmentIndex"
    }
}

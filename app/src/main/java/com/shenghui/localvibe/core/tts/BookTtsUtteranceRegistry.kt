package com.shenghui.localvibe.core.tts

data class BookTtsUtteranceEvent(
    val utteranceId: String,
    val playbackSessionId: Long?
)

class BookTtsUtteranceRegistry {
    private val events = LinkedHashMap<String, BookTtsUtteranceEvent>()

    @Synchronized
    fun register(utteranceId: String, playbackSessionId: Long?): BookTtsUtteranceEvent {
        return BookTtsUtteranceEvent(utteranceId, playbackSessionId).also { events[utteranceId] = it }
    }

    @Synchronized
    fun onStart(utteranceId: String?): BookTtsUtteranceEvent? {
        return utteranceId?.let(events::get)
    }

    @Synchronized
    fun onDone(utteranceId: String?): BookTtsUtteranceEvent? {
        return utteranceId?.let(events::remove)
    }

    @Synchronized
    fun onError(utteranceId: String?): BookTtsUtteranceEvent? {
        return utteranceId?.let(events::remove)
    }

    @Synchronized
    fun onStop(): List<BookTtsUtteranceEvent> {
        return events.values.toList().also { events.clear() }
    }

    @Synchronized
    fun remove(utteranceId: String): BookTtsUtteranceEvent? {
        return events.remove(utteranceId)
    }

    @Synchronized
    fun size(): Int = events.size
}

package com.shenghui.localvibe.feature.book

internal data class BookSequentialPlaybackBridge(
    val snapshot: BookSequentialTargetSnapshot
) {
    companion object {
        val Empty = BookSequentialPlaybackBridge(BookSequentialTargetSnapshot(emptyList()))

        fun build(
            paragraphs: List<String>,
            chapters: List<com.shenghui.localvibe.core.book.BookChapter>,
            splitParagraph: (String) -> List<String>
        ): BookSequentialPlaybackBridge = BookSequentialPlaybackBridge(
            buildBookSequentialTargetSnapshot(paragraphs, chapters, splitParagraph)
        )
    }
}

internal fun BookSequentialPlaybackBridge?.createMatchaPlaybackRequest(
    chapterSentenceIndex: Int,
    paragraphIndex: Int,
    sentenceIndexInParagraph: Int,
    text: String,
    speechRate: Float,
    sessionId: Long,
    playbackIntentPlaying: Boolean
): MatchaPlaybackRequest = MatchaPlaybackRequest(
    targetId = BookPlaybackTargetId(
        chapterSentenceIndex = chapterSentenceIndex,
        paragraphIndex = paragraphIndex,
        sentenceIndexInParagraph = sentenceIndexInParagraph
    ),
    text = text,
    speechRate = speechRate,
    sessionId = sessionId,
    playbackIntentPlaying = playbackIntentPlaying,
    snapshot = this?.snapshot
)

internal fun BookSequentialPlaybackBridge?.createNextPrewarmRequest(
    request: MatchaPlaybackRequest
): MatchaPrewarmScheduleRequest? = this?.let { bridge ->
    MatchaPrewarmScheduleRequest(
            currentTarget = request.targetId,
            snapshot = bridge.snapshot,
            speechRate = request.speechRate,
            sessionId = request.sessionId
        )
}

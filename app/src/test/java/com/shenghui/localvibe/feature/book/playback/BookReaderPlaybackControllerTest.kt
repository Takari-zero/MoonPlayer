package com.shenghui.localvibe.feature.book.playback

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookReaderPlaybackControllerTest {
    @Test
    fun playCreatesActiveSessionAndWritesPcmToSink() = runBlocking {
        val engine = FakeBookTtsEngine()
        val sink = RecordingAudioSink()
        val controller = BookReaderPlaybackController(engine = engine, audioSink = sink)
        val target = target(chapterIndex = 0, paragraphIndex = 10, sentenceIndex = 2)

        controller.play(target)

        assertEquals(1L, controller.state.activeSessionId)
        assertTrue(controller.state.isPlaying)
        assertFalse(controller.state.isPaused)
        assertEquals(target, controller.state.target)
        assertEquals(listOf("play:1:24000:4"), sink.events)
        assertEquals(listOf(1L), engine.sessionIds)
    }

    @Test
    fun jumpToChapterWhilePlayingInvalidatesOldSessionAndStartsTargetChapter() = runBlocking {
        val engine = FakeBookTtsEngine()
        val sink = RecordingAudioSink()
        val controller = BookReaderPlaybackController(engine = engine, audioSink = sink)
        val first = target(chapterIndex = 0, paragraphIndex = 10, sentenceIndex = 0)
        val second = target(chapterIndex = 1, paragraphIndex = 70, sentenceIndex = 0)

        controller.play(first)
        controller.jumpToChapter(second, resumeIfPlaying = true)

        assertEquals(3L, controller.state.activeSessionId)
        assertEquals(second, controller.state.target)
        assertTrue(controller.state.isPlaying)
        assertEquals(
            listOf(
                "play:1:24000:4",
                "stop:jump_to_chapter",
                "play:3:24000:4"
            ),
            sink.events
        )
        assertEquals(listOf(1L, 3L), engine.sessionIds)
    }

    @Test
    fun jumpToSentenceWithoutResumeSelectsTargetWithoutPausedState() = runBlocking {
        val engine = FakeBookTtsEngine()
        val sink = RecordingAudioSink()
        val logs = mutableListOf<String>()
        val controller = BookReaderPlaybackController(
            engine = engine,
            audioSink = sink,
            logger = logs::add
        )
        val target = target(chapterIndex = 0, paragraphIndex = 10, sentenceIndex = 2)

        controller.jumpToSentence(target, resumeIfPlaying = false)

        assertEquals(target, controller.state.target)
        assertFalse(controller.state.isPlaying)
        assertFalse(controller.state.isPaused)
        assertFalse(controller.state.canResume)
        assertEquals(emptyList<String>(), engine.sessionIds)
        assertEquals(listOf("stop:jump_to_sentence"), sink.events)
        assertTrue(logs.any { it.contains("target selected reason=jump_to_sentence resumeIfPlaying=false") })
    }

    @Test
    fun resumeWithoutStartedPlaybackDoesNotStartAudioSink() = runBlocking {
        val engine = FakeBookTtsEngine()
        val sink = RecordingAudioSink()
        val logs = mutableListOf<String>()
        val controller = BookReaderPlaybackController(
            engine = engine,
            audioSink = sink,
            logger = logs::add
        )
        val target = target(chapterIndex = 0, paragraphIndex = 10, sentenceIndex = 2)

        controller.jumpToSentence(target, resumeIfPlaying = false)
        controller.resume()

        assertFalse(controller.state.isPlaying)
        assertFalse(controller.state.isPaused)
        assertFalse(controller.state.canResume)
        assertEquals(listOf("stop:jump_to_sentence"), sink.events)
        assertTrue(logs.any { it.contains("resume skipped reason=no_resumable_audio") })
    }
    @Test
    fun activeSessionPlaybackCompletedEmitsEvent() = runBlocking {
        val events = mutableListOf<BookReaderPlaybackEvent>()
        val controller = BookReaderPlaybackController(
            engine = FakeBookTtsEngine(),
            audioSink = RecordingAudioSink(),
            eventSink = events::add
        )
        val target = target(chapterIndex = 0, paragraphIndex = 10, sentenceIndex = 2)

        controller.play(target)

        assertTrue(events.any { it == BookReaderPlaybackEvent.PlaybackCompleted(1L, target) })
    }

    @Test
    fun multipleSequentialPlaysEmitCompletedForEachTarget() = runBlocking {
        val events = mutableListOf<BookReaderPlaybackEvent>()
        val controller = BookReaderPlaybackController(
            engine = FakeBookTtsEngine(),
            audioSink = RecordingAudioSink(),
            eventSink = events::add
        )
        val first = target(chapterIndex = 0, paragraphIndex = 10, sentenceIndex = 0)
        val second = target(chapterIndex = 0, paragraphIndex = 11, sentenceIndex = 0)
        val third = target(chapterIndex = 0, paragraphIndex = 12, sentenceIndex = 0)

        controller.play(first)
        controller.play(second)
        controller.play(third)

        val completedEvents = events.filterIsInstance<BookReaderPlaybackEvent.PlaybackCompleted>()
        assertEquals(listOf(1L, 2L, 3L), completedEvents.map { it.sessionId })
        assertEquals(listOf(first, second, third), completedEvents.map { it.target })
    }

    @Test
    fun stoppedSessionPlaybackCompletionDoesNotEmitCompletedEvent() = runBlocking {
        val events = mutableListOf<BookReaderPlaybackEvent>()
        lateinit var controller: BookReaderPlaybackController
        val sink = CallbackAudioSink { controller.stop("test_stop_during_write") }
        controller = BookReaderPlaybackController(
            engine = FakeBookTtsEngine(),
            audioSink = sink,
            eventSink = events::add
        )

        controller.play(target(chapterIndex = 0, paragraphIndex = 10, sentenceIndex = 2))

        assertFalse(events.any { it is BookReaderPlaybackEvent.PlaybackCompleted })
    }

    @Test
    fun pausedSessionPlaybackCompletionDoesNotEmitCompletedEvent() = runBlocking {
        val events = mutableListOf<BookReaderPlaybackEvent>()
        lateinit var controller: BookReaderPlaybackController
        val sink = CallbackAudioSink { controller.pause() }
        controller = BookReaderPlaybackController(
            engine = FakeBookTtsEngine(),
            audioSink = sink,
            eventSink = events::add
        )

        controller.play(target(chapterIndex = 0, paragraphIndex = 10, sentenceIndex = 2))

        assertFalse(events.any { it is BookReaderPlaybackEvent.PlaybackCompleted })
        assertTrue(controller.state.isPaused)
    }

    @Test
    fun pausedSessionPlaybackCompletionClearsResumeCapability() = runBlocking {
        lateinit var controller: BookReaderPlaybackController
        val sink = CallbackAudioSink { controller.pause() }
        controller = BookReaderPlaybackController(
            engine = FakeBookTtsEngine(),
            audioSink = sink,
        )

        controller.play(target(chapterIndex = 0, paragraphIndex = 10, sentenceIndex = 2))

        assertTrue(controller.state.isPaused)
        assertFalse(controller.state.canResume)
    }

    @Test
    fun stopAfterChapterModeKeepsTargetPausedInsteadOfLooping() = runBlocking {
        val engine = FakeBookTtsEngine()
        val sink = RecordingAudioSink()
        val logs = mutableListOf<String>()
        val controller = BookReaderPlaybackController(
            engine = engine,
            audioSink = sink,
            logger = logs::add
        )
        val target = target(chapterIndex = 1, paragraphIndex = 70, sentenceIndex = 5)

        controller.play(target)
        controller.setMode(BookReaderPlaybackMode.STOP_AFTER_CHAPTER)
        controller.onChapterEnd(currentChapterFirstTarget = target.copy(sentenceIndex = 0), nextChapterFirstTarget = null)

        assertFalse(controller.state.isPlaying)
        assertTrue(controller.state.isPaused)
        assertTrue(controller.state.canResume)
        assertEquals(target, controller.state.target)
        assertEquals(BookReaderPlaybackMode.STOP_AFTER_CHAPTER, controller.state.mode)
        assertTrue(logs.any { it.contains("chapter end mode=STOP_AFTER_CHAPTER action=stop") })
    }

    @Test
    fun audioTrackSinkDurationUsesPcm16MonoFrameCount() {
        assertEquals(1000L, BookReaderAudioTrackSink.calculatePcmDurationMs(48000, 24000))
        assertEquals(1L, BookReaderAudioTrackSink.calculatePcmDurationMs(0, 24000))
        assertEquals(1L, BookReaderAudioTrackSink.calculatePcmDurationMs(48000, 0))
    }
}

private fun target(
    chapterIndex: Int,
    paragraphIndex: Int,
    sentenceIndex: Int,
) = BookReaderPlaybackTarget(
    bookId = "book",
    bookTitle = "Book",
    chapterIndex = chapterIndex,
    chapterTitle = "Chapter $chapterIndex",
    paragraphIndex = paragraphIndex,
    sentenceIndex = sentenceIndex,
    sentenceText = "这是一段测试文本。",
    sentencePreview = "这是一段测试文本。"
)

private class FakeBookTtsEngine : BookTtsEngine {
    val sessionIds = mutableListOf<Long>()
    val texts = mutableListOf<String>()

    override suspend fun synthesizeToPcm(
        target: BookReaderPlaybackTarget,
        text: String,
        sessionId: Long,
    ): Result<BookTtsPcmResult> {
        sessionIds += sessionId
        texts += text
        return Result.success(
            BookTtsPcmResult(
                sampleRate = 24000,
                pcm16 = byteArrayOf(1, 2, 3, 4),
                durationMs = 1,
                rtf = 0.1,
            )
        )
    }

    override fun release() = Unit
}

private class RecordingAudioSink : BookReaderAudioSink {
    val events = mutableListOf<String>()

    override suspend fun playPcm(sessionId: Long, sampleRate: Int, pcm16: ByteArray) {
        events += "play:$sessionId:$sampleRate:${pcm16.size}"
    }

    override fun pause() {
        events += "pause"
    }

    override fun resume() {
        events += "resume"
    }

    override fun stop(reason: String) {
        events += "stop:$reason"
    }

    override fun release() {
        events += "release"
    }
}

private class CallbackAudioSink(
    private val onPlayPcm: suspend () -> Unit,
) : BookReaderAudioSink {
    override suspend fun playPcm(sessionId: Long, sampleRate: Int, pcm16: ByteArray) {
        onPlayPcm()
    }

    override fun pause() = Unit
    override fun resume() = Unit
    override fun stop(reason: String) = Unit
    override fun release() = Unit
}

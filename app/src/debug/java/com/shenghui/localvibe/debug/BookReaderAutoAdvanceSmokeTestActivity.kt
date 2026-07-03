package com.shenghui.localvibe.debug

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.shenghui.localvibe.core.tts.fastspeech2.FastSpeech2AssetVerifier
import com.shenghui.localvibe.core.tts.fastspeech2.FastSpeech2BookTtsEngineAdapter
import com.shenghui.localvibe.feature.book.playback.BookReaderAudioTrackSink
import com.shenghui.localvibe.feature.book.playback.BookReaderPlaybackController
import com.shenghui.localvibe.feature.book.playback.BookReaderPlaybackEvent
import com.shenghui.localvibe.feature.book.playback.BookReaderPlaybackTarget
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class BookReaderAutoAdvanceSmokeTestActivity : Activity() {
    private val smokeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val events = Channel<BookReaderPlaybackEvent>(Channel.UNLIMITED)
    private lateinit var statusView: TextView
    private lateinit var controller: BookReaderPlaybackController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        statusView = TextView(this).apply {
            textSize = 16f
            setPadding(32, 32, 32, 32)
            text = "BookReader auto advance smoke starting..."
        }
        setContentView(statusView)

        controller = BookReaderPlaybackController(
            engine = FastSpeech2BookTtsEngineAdapter(applicationContext),
            audioSink = BookReaderAudioTrackSink(),
            logger = { message -> Log.i(TAG, message) },
            eventSink = { event ->
                Log.i(TAG, "event=$event")
                events.trySend(event)
            },
        )

        smokeScope.launch { runSmoke() }
    }

    override fun onDestroy() {
        if (::controller.isInitialized) {
            controller.release()
        }
        events.close()
        smokeScope.cancel()
        super.onDestroy()
    }

    private suspend fun runSmoke() {
        try {
            Log.i(TAG, "start")
            appendStatus("checking assets")
            val assets = FastSpeech2AssetVerifier.checkAssets(this)
            Log.i(TAG, "assets complete=${assets.isComplete} missing=${assets.missingFiles}")
            if (!assets.isComplete) error("missing assets: ${assets.missingFiles}")

            runPauseGate()
            runAutoAdvance()

            Log.i(TAG, "complete=true count=5")
            appendStatus("complete=true count=5")
        } catch (error: Throwable) {
            Log.e(TAG, "error type=${error::class.java.simpleName} message=${error.message}", error)
            appendStatus("error: ${error.message}")
        }
    }

    private suspend fun runPauseGate() {
        val target = target(0, "暂停门控测试，暂停以后不应该自动进入下一句。")
        val started = CompletableDeferred<Unit>()
        val done = CompletableDeferred<Unit>()
        var completedWhilePaused = false

        val collector = smokeScope.launch {
            for (event in events) {
                when (event) {
                    is BookReaderPlaybackEvent.PlaybackStarted -> started.complete(Unit)
                    is BookReaderPlaybackEvent.PlaybackCompleted -> {
                        completedWhilePaused = controller.state.isPaused
                        done.complete(Unit)
                        break
                    }
                    else -> Unit
                }
            }
        }

        val playJob = smokeScope.launch { controller.play(target) }
        withTimeout(10_000) { started.await() }
        delay(120)
        controller.pause()
        Log.i(TAG, "pause requested")
        delay(350)
        if (done.isCompleted) error("pause gate failed: completed while paused=$completedWhilePaused")
        Log.i(TAG, "pause gate passed")
        appendStatus("pause gate passed")
        controller.resume()
        Log.i(TAG, "resume requested")
        playJob.join()
        done.await()
        collector.cancel()
        Log.i(TAG, "resume gate passed")
        appendStatus("resume gate passed")
    }

    private suspend fun runAutoAdvance() {
        val targets = listOf(
            target(1, "第一句自动续读测试。"),
            target(2, "第二句继续播放。"),
            target(3, "第三句检查完成事件。"),
            target(4, "第四句检查写入返回。"),
            target(5, "第五句结束自动测试。"),
        )
        val completed = CompletableDeferred<Unit>()
        var completedCount = 0

        smokeScope.launch {
            for (event in events) {
                if (event !is BookReaderPlaybackEvent.PlaybackCompleted) continue
                completedCount += 1
                Log.i(TAG, "sentence done index=$completedCount sessionId=${event.sessionId}")
                appendStatus("done $completedCount")
                if (completedCount >= targets.size) {
                    completed.complete(Unit)
                    break
                }
                val next = targets[completedCount]
                Log.i(TAG, "next index=${completedCount + 1}")
                controller.play(next)
            }
        }

        Log.i(TAG, "next index=1")
        controller.play(targets.first())
        completed.await()
    }

    private fun target(index: Int, text: String): BookReaderPlaybackTarget =
        BookReaderPlaybackTarget(
            bookId = "auto-smoke-book",
            bookTitle = "Auto Smoke Book",
            chapterIndex = 0,
            chapterTitle = "Auto Smoke",
            paragraphIndex = index,
            sentenceIndex = 0,
            clauseIndex = 0,
            sentenceText = text,
            sentencePreview = text,
        )

    private suspend fun appendStatus(line: String) {
        withContext(Dispatchers.Main.immediate) {
            if (!isDestroyed) {
                statusView.append("\n$line")
            }
        }
    }

    companion object {
        private const val TAG = "LV_BOOK_AUTO_SMOKE"
    }
}

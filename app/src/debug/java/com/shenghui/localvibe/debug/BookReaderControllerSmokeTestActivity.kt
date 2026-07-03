package com.shenghui.localvibe.debug

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.shenghui.localvibe.core.tts.fastspeech2.FastSpeech2AssetVerifier
import com.shenghui.localvibe.core.tts.fastspeech2.FastSpeech2BookTtsEngineAdapter
import com.shenghui.localvibe.feature.book.playback.BookReaderAudioTrackSink
import com.shenghui.localvibe.feature.book.playback.BookReaderPlaybackController
import com.shenghui.localvibe.feature.book.playback.BookReaderPlaybackTarget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookReaderControllerSmokeTestActivity : Activity() {
    private val smokeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var statusView: TextView
    private lateinit var controller: BookReaderPlaybackController

    @Volatile
    private var smokeFinished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        statusView = TextView(this).apply {
            textSize = 16f
            setPadding(32, 32, 32, 32)
            text = "BookReader controller smoke starting..."
        }
        setContentView(statusView)

        controller = BookReaderPlaybackController(
            engine = FastSpeech2BookTtsEngineAdapter(applicationContext),
            audioSink = BookReaderAudioTrackSink(),
            logger = { message -> Log.i(TAG, message) },
            eventSink = { event -> Log.i(TAG, "event=$event") },
        )

        smokeScope.launch { runSmokeTest() }
    }

    override fun onDestroy() {
        if (smokeFinished) {
            cleanup("activity_destroy_after_finish")
            smokeScope.cancel()
        } else {
            Log.w(TAG, "cancelled_by_lifecycle smokeFinished=false; smoke job continues for debug verification")
        }
        super.onDestroy()
    }

    private suspend fun runSmokeTest() {
        var completed = false
        try {
            val assets = FastSpeech2AssetVerifier.checkAssets(this)
            Log.i(TAG, "assets check complete=${assets.isComplete} missing=${assets.missingFiles}")
            if (!assets.isComplete) error("missing assets: ${assets.missingFiles}")

            val short = target(
                name = "short",
                paragraphIndex = 0,
                text = "这是一段离线语音试听。",
            )
            val medium = target(
                name = "medium",
                paragraphIndex = 1,
                text = "他缓缓抬起头，看向远处翻滚的云海，心中忽然有了答案。",
            )
            val long = target(
                name = "long",
                paragraphIndex = 2,
                text = "许青站在风雪里，望着远处渐渐暗下来的天色，心中没有太多波澜，只是默默握紧了手中的刀。",
            )
            val quickA = target(
                name = "quick-a",
                paragraphIndex = 3,
                text = "旧会话应该在新请求到来后失效，不能继续写入音频。",
            )
            val quickB = target(
                name = "quick-b",
                paragraphIndex = 4,
                text = "新会话应该正常播放。",
            )

            step("play_short") {
                val shortJob = smokeScope.launch { controller.play(short) }
                delay(350)
                step("pause_resume") {
                    Log.i(TAG, "controller pause request")
                    controller.pause()
                    appendStatus("pause")
                    delay(250)
                    Log.i(TAG, "controller resume request")
                    controller.resume()
                    appendStatus("resume")
                }
                delay(250)
                Log.i(TAG, "controller stop request")
                controller.stop("controller_smoke_stop")
                appendStatus("stop")
                shortJob.join()
                Log.i(TAG, "expected cancellation reason=play_short_stopped_session")
            }

            step("play_medium") {
                controller.play(medium)
                appendStatus("medium complete")
            }

            step("play_long") {
                controller.play(long)
                appendStatus("long complete")
            }

            step("quick_switch") {
                val quickAJob = smokeScope.launch { controller.play(quickA) }
                delay(30)
                controller.play(quickB)
                quickAJob.join()
                Log.i(TAG, "expected cancellation reason=quick_switch_old_session")
                appendStatus("quick switch complete")
            }

            completed = true
            smokeFinished = true
            Log.i(TAG, "controller smoke complete=true")
            appendStatus("controller smoke complete")
        } catch (error: CancellationException) {
            if (completed) {
                Log.i(TAG, "expected cancellation reason=after_complete_cleanup message=${error.message}")
            } else {
                Log.e(TAG, "controller smoke error type=${error::class.java.simpleName} message=${error.message}", error)
            }
        } catch (error: Throwable) {
            Log.e(TAG, "controller smoke error type=${error::class.java.simpleName} message=${error.message}", error)
            appendStatus("error: ${error.message}")
        } finally {
            if (completed) {
                withContext(NonCancellable) {
                    cleanup("smoke_complete")
                    smokeScope.cancel()
                }
            }
        }
    }

    private suspend fun step(name: String, block: suspend () -> Unit) {
        Log.i(TAG, "step start name=$name")
        appendStatus("start $name")
        block()
        Log.i(TAG, "step done name=$name")
        appendStatus("done $name")
    }

    private fun cleanup(reason: String) {
        if (::controller.isInitialized) {
            controller.release()
        }
        Log.i(TAG, "cleanup release done reason=$reason")
    }

    private fun target(
        name: String,
        paragraphIndex: Int,
        text: String,
    ): BookReaderPlaybackTarget = BookReaderPlaybackTarget(
        bookId = "smoke-book",
        bookTitle = "Smoke Book",
        chapterIndex = 0,
        chapterTitle = "第一章",
        paragraphIndex = paragraphIndex,
        sentenceIndex = 0,
        clauseIndex = 0,
        sentenceText = text,
        sentencePreview = text,
    ).also {
        Log.i(TAG, "target name=$name paragraphIndex=$paragraphIndex length=${text.length}")
    }

    private suspend fun appendStatus(line: String) {
        runCatching {
            withContext(Dispatchers.Main.immediate) {
                if (!isDestroyed) {
                    statusView.append("\n$line")
                }
            }
        }.onFailure { error ->
            Log.w(TAG, "status update skipped message=${error.message}")
        }
    }

    companion object {
        private const val TAG = "LV_BOOK_SMOKE"
    }
}

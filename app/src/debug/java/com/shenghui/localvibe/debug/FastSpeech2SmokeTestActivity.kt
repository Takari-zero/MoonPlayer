package com.shenghui.localvibe.debug

import android.app.Activity
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.TextView
import com.shenghui.localvibe.core.tts.fastspeech2.FastSpeech2AssetContract
import com.shenghui.localvibe.core.tts.fastspeech2.FastSpeech2AssetVerifier
import com.shenghui.localvibe.core.tts.fastspeech2.FastSpeech2BookTtsEngine
import com.shenghui.localvibe.feature.book.playback.BookReaderAudioTrackSink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FastSpeech2SmokeTestActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val engine = FastSpeech2BookTtsEngine(assetDir = FastSpeech2AssetContract.ASSET_DIR)
    private val audioSink = BookReaderAudioTrackSink()
    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        statusView = TextView(this).apply {
            textSize = 16f
            setPadding(32, 32, 32, 32)
            text = "FastSpeech2 smoke test starting..."
        }
        setContentView(statusView)
        scope.launch { runSmokeTest() }
    }

    override fun onDestroy() {
        scope.cancel()
        audioSink.release()
        engine.release()
        super.onDestroy()
    }

    private suspend fun runSmokeTest() {
        val cases = listOf(
            SmokeCase("short", "这是一段离线语音试听。"),
            SmokeCase("medium", "他缓缓抬起头，看向远处翻滚的云海，心中忽然有了答案。"),
            SmokeCase("long", "许青站在风雪里，望着远处渐渐暗下来的天色，心中没有太多波澜，只是默默握紧了手中的刀。"),
        )

        try {
            appendStatus("checking assets")
            val assets = FastSpeech2AssetVerifier.checkAssets(this)
            Log.i(TAG, "assets check complete=${assets.isComplete} missing=${assets.missingFiles}")
            if (!assets.isComplete) error("missing assets: ${assets.missingFiles}")

            val initStart = SystemClock.elapsedRealtime()
            engine.initialize(this).getOrThrow()
            Log.i(TAG, "engine init costMs=${SystemClock.elapsedRealtime() - initStart}")

            cases.forEachIndexed { index, smokeCase ->
                runSmokeCase(index + 1L, smokeCase)
            }

            appendStatus("smoke test complete")
        } catch (error: Throwable) {
            Log.e(TAG, "error ${error.message}", error)
            appendStatus("error: ${error.message}")
        }
    }

    private suspend fun runSmokeCase(sessionId: Long, smokeCase: SmokeCase) {
        appendStatus("synth ${smokeCase.name}")
        Log.i(TAG, "synth start case=${smokeCase.name} textLength=${smokeCase.text.length}")
        val startedAt = SystemClock.elapsedRealtime()
        val result = engine.synthesizeToPcm(
            text = smokeCase.text,
            sessionId = sessionId,
            paragraphIndex = smokeCase.ordinal,
            sentenceIndex = 0,
            chapterIndex = 0,
            chapterTitle = "Smoke",
        ).getOrThrow()
        val synthCostMs = SystemClock.elapsedRealtime() - startedAt
        Log.i(TAG, "first pcm ready costMs=$synthCostMs case=${smokeCase.name} bytes=${result.pcm16.size}")
        Log.i(TAG, "full pcm done costMs=$synthCostMs case=${smokeCase.name} durationMs=${result.durationMs} rtf=${result.rtf}")

        withContext(Dispatchers.IO) {
            Log.i(TAG, "audio sink write main=${Thread.currentThread().name == "main"} case=${smokeCase.name}")
            audioSink.playPcm(
                sessionId = sessionId,
                sampleRate = result.sampleRate,
                pcm16 = result.pcm16,
            )
        }
        Log.i(TAG, "playback complete case=${smokeCase.name}")
        appendStatus("complete ${smokeCase.name}: synth=${synthCostMs}ms pcm=${result.pcm16.size}")
    }

    private suspend fun appendStatus(line: String) = withContext(Dispatchers.Main.immediate) {
        statusView.append("\n$line")
    }

    private data class SmokeCase(
        val name: String,
        val text: String,
    ) {
        val ordinal: Int = when (name) {
            "short" -> 0
            "medium" -> 1
            else -> 2
        }
    }

    companion object {
        private const val TAG = "LV_BOOK_SMOKE"
    }
}

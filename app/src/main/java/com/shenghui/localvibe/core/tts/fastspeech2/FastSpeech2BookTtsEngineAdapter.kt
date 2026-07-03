package com.shenghui.localvibe.core.tts.fastspeech2

import android.content.Context
import com.shenghui.localvibe.feature.book.playback.BookReaderPlaybackTarget
import com.shenghui.localvibe.feature.book.playback.BookTtsEngine
import com.shenghui.localvibe.feature.book.playback.BookTtsPcmResult

class FastSpeech2BookTtsEngineAdapter(
    private val context: Context,
    private val engine: FastSpeech2BookTtsEngine = FastSpeech2BookTtsEngine(
        assetDir = FastSpeech2AssetContract.ASSET_DIR,
    ),
) : BookTtsEngine {
    override suspend fun synthesizeToPcm(
        target: BookReaderPlaybackTarget,
        text: String,
        sessionId: Long,
    ): Result<BookTtsPcmResult> {
        engine.initialize(context).getOrElse { error -> return Result.failure(error) }
        return engine.synthesizeToPcm(
            text = text,
            sessionId = sessionId,
            paragraphIndex = target.paragraphIndex,
            sentenceIndex = target.sentenceIndex,
            chapterIndex = target.chapterIndex,
            chapterTitle = target.chapterTitle,
        ).map { result ->
            BookTtsPcmResult(
                sampleRate = result.sampleRate,
                pcm16 = result.pcm16,
                channelCount = result.channelCount,
                clauseIndex = result.clauses.firstOrNull()?.clauseIndex ?: target.clauseIndex,
                durationMs = result.durationMs,
                rtf = result.rtf,
            )
        }
    }

    override fun release() {
        engine.release()
    }
}

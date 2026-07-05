package com.shenghui.localvibe.feature.book.playback

import android.content.Context
import com.shenghui.localvibe.core.book.TxtBookReader
import kotlinx.coroutines.runBlocking

fun interface AndroidTxtBookParagraphReader {
    fun readParagraphs(bookId: String): Result<List<String>>
}

class AndroidTxtBookParagraphSource(
    private val reader: AndroidTxtBookParagraphReader,
) : BookParagraphSource {
    override fun load(request: BookParagraphLoadRequest): BookParagraphLoadResult {
        return reader.readParagraphs(request.key.bookId).fold(
            onSuccess = { paragraphs ->
                BookParagraphLoadResult.success(
                    key = request.key,
                    paragraphs = paragraphs,
                )
            },
            onFailure = { error ->
                BookParagraphLoadResult.failed(
                    key = request.key,
                    message = error.message ?: "android txt paragraphs unavailable",
                    cause = error,
                )
            },
        )
    }

    companion object {
        fun fromContext(context: Context): AndroidTxtBookParagraphSource {
            val appContext = context.applicationContext
            return AndroidTxtBookParagraphSource(
                reader = AndroidTxtBookParagraphReader { bookId ->
                    runBlocking {
                        TxtBookReader.readParagraphs(appContext, bookId)
                    }
                }
            )
        }
    }
}

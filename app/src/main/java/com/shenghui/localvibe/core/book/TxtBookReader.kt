package com.shenghui.localvibe.core.book

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

object TxtBookReader {
    suspend fun readParagraphs(context: Context, uriString: String): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(uriString)
                val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes()
                } ?: error("Cannot open txt file")
                val text = decodeText(bytes)
                    .removePrefix("\uFEFF")
                    .replace("\r\n", "\n")
                    .replace('\r', '\n')
                splitParagraphs(text)
            }
        }
    }

    private fun decodeText(bytes: ByteArray): String {
        return try {
            decodeStrict(bytes, StandardCharsets.UTF_8)
        } catch (_: CharacterCodingException) {
            decodeStrict(bytes, Charset.forName("GB18030"))
        }
    }

    private fun decodeStrict(bytes: ByteArray, charset: Charset): String {
        val decoder = charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return decoder.decode(ByteBuffer.wrap(bytes)).toString()
    }

    private fun splitParagraphs(text: String): List<String> {
        return text
            .split(Regex("\\n\\s*\\n|\\n"))
            .flatMap { paragraph ->
                chunkParagraph(paragraph.trim())
            }
            .filter { it.isNotBlank() }
    }

    private fun chunkParagraph(paragraph: String, maxLength: Int = 420): List<String> {
        if (paragraph.isBlank()) return emptyList()
        if (paragraph.length <= maxLength) return listOf(paragraph)

        val chunks = mutableListOf<String>()
        var start = 0
        while (start < paragraph.length) {
            val endLimit = (start + maxLength).coerceAtMost(paragraph.length)
            val punctuationIndex = paragraph
                .lastIndexOfAny(charArrayOf('。', '！', '？', '.', '!', '?', '；', ';'), endLimit - 1)
                .takeIf { it >= start + 120 }
            val end = ((punctuationIndex ?: (endLimit - 1)) + 1).coerceAtMost(paragraph.length)
            chunks.add(paragraph.substring(start, end).trim())
            start = end
        }
        return chunks.filter { it.isNotBlank() }
    }
}

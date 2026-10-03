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
import java.security.MessageDigest

object TxtBookReader {
    suspend fun readParagraphs(context: Context, uriString: String): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            runCatching {
                parseParagraphs(readBytes(context, uriString))
            }
        }
    }

    suspend fun readBookWithFingerprint(
        context: Context,
        uriString: String
    ): Result<TxtBookReadResult> {
        return withContext(Dispatchers.IO) {
            runCatching {
                readBytesWithFingerprint(readBytes(context, uriString))
            }
        }
    }

    internal fun readBytesWithFingerprint(bytes: ByteArray): TxtBookReadResult {
        val fingerprint = BookContentFingerprint(
            size = bytes.size.toLong(),
            sha256Hex = sha256Hex(bytes)
        )
        return TxtBookReadResult(
            paragraphs = parseParagraphs(bytes),
            fingerprint = fingerprint
        )
    }

    internal fun parseParagraphs(bytes: ByteArray): List<String> {
        val text = decodeText(bytes)
            .removePrefix("\uFEFF")
            .replace("\r\n", "\n")
            .replace('\r', '\n')
        return splitParagraphs(text)
    }

    private fun readBytes(context: Context, uriString: String): ByteArray {
        val uri = Uri.parse(uriString)
        return context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: error("Cannot open txt file")
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return buildString(digest.size * 2) {
            digest.forEach { value ->
                val unsigned = value.toInt() and 0xFF
                append(HEX_DIGITS[unsigned ushr 4])
                append(HEX_DIGITS[unsigned and 0x0F])
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

    private const val HEX_DIGITS = "0123456789abcdef"
}

package com.shenghui.localvibe.core.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TxtBookReaderTest {
    @Test
    fun knownRawBytesProduceParagraphsSizeAndExpectedHash() {
        val result = TxtBookReader.readBytesWithFingerprint("abc".encodeToByteArray())

        assertEquals(listOf("abc"), result.paragraphs)
        assertEquals(3L, result.fingerprint.size)
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            result.fingerprint.sha256Hex
        )
    }

    @Test
    fun emptyBytesProduceEmptyParagraphsAndStandardDigest() {
        val result = TxtBookReader.readBytesWithFingerprint(byteArrayOf())

        assertEquals(emptyList<String>(), result.paragraphs)
        assertEquals(0L, result.fingerprint.size)
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            result.fingerprint.sha256Hex
        )
    }

    @Test
    fun chineseTextKeepsParagraphSemanticsAndHashesRawUtf8Bytes() {
        val bytes = "你好，世界\n第二段".encodeToByteArray()

        val result = TxtBookReader.readBytesWithFingerprint(bytes)

        assertEquals(listOf("你好，世界", "第二段"), result.paragraphs)
        assertEquals(25L, result.fingerprint.size)
        assertEquals(
            "9450a42d6d14e00e5b8737aebce5d7b0bbf6c8307c64d64a59e3eaf6d0322b5b",
            result.fingerprint.sha256Hex
        )
    }

    @Test
    fun utf8BomChangesFingerprintWithoutChangingVisibleText() {
        val plain = "正文".encodeToByteArray()
        val withBom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + plain

        val plainResult = TxtBookReader.readBytesWithFingerprint(plain)
        val bomResult = TxtBookReader.readBytesWithFingerprint(withBom)

        assertEquals(plainResult.paragraphs, bomResult.paragraphs)
        assertNotEquals(plainResult.fingerprint, bomResult.fingerprint)
    }

    @Test
    fun sameBytesProduceDeterministicFingerprint() {
        val bytes = "deterministic".encodeToByteArray()

        assertEquals(
            TxtBookReader.readBytesWithFingerprint(bytes).fingerprint,
            TxtBookReader.readBytesWithFingerprint(bytes).fingerprint
        )
    }

    @Test
    fun legacyParagraphParsingSemanticsRemainUnchanged() {
        val bytes = "\uFEFF第一段\r\n\r\n第二段\r第三段".encodeToByteArray()

        assertEquals(
            listOf("第一段", "第二段", "第三段"),
            TxtBookReader.parseParagraphs(bytes)
        )
    }

    @Test
    fun resultTypesDoNotRetainRawByteArray() {
        assertFalse(TxtBookReadResult::class.java.declaredFields.any { it.type == ByteArray::class.java })
        assertFalse(BookContentFingerprint::class.java.declaredFields.any { it.type == ByteArray::class.java })
    }
}

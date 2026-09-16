package com.shenghui.localvibe.core.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookTtsTextNormalizerTest {
    @Test
    fun keepsOriginalTextForDisplayAndHighlight() {
        val original = "今天下午我们继续完成播放器测试。"

        val normalized = BookTtsTextNormalizer.normalize(original)

        assertEquals(original, normalized.originalText)
        assertEquals(original, normalized.spokenText)
    }

    @Test
    fun preservesEnglishWordSpacing() {
        val original = "LocalVibe uses Android Media3 for local playback."

        val normalized = BookTtsTextNormalizer.normalize(original)

        assertEquals(original, normalized.originalText)
        assertEquals(original, normalized.spokenText)
    }

    @Test
    fun removesBomZeroWidthAndInvisibleControls() {
        val original = "\uFEFF今\u200B天\u0000继续\u200D测试。"

        val normalized = BookTtsTextNormalizer.normalize(original)

        assertEquals(original, normalized.originalText)
        assertEquals("今天继续测试。", normalized.spokenText)
    }

    @Test
    fun collapsesWhitespaceWithoutRemovingNeededSpaces() {
        val original = "LocalVibe\t使用\nAndroid  Media3\u3000播放。"

        val normalized = BookTtsTextNormalizer.normalize(original)

        assertEquals(original, normalized.originalText)
        assertEquals("LocalVibe 使用 Android Media3 播放。", normalized.spokenText)
    }

    @Test
    fun keepsNumbersDatesDecimalsAndPercentUnchanged() {
        val original = "计划在2026年9月16日上午8点30分验证，数值是3.14，进度50%。"

        val normalized = BookTtsTextNormalizer.normalize(original)

        assertEquals(original, normalized.originalText)
        assertEquals(original, normalized.spokenText)
    }

    @Test
    fun cautiouslyCollapsesOnlyLongRepeatedSamePunctuation() {
        val original = "你好。。。。真的吗？？？好？！继续！！"

        val normalized = BookTtsTextNormalizer.normalize(original)

        assertEquals(original, normalized.originalText)
        assertEquals("你好。。真的吗？？好？！继续！！", normalized.spokenText)
    }

    @Test
    fun normalizationIsDeterministicAndIdempotent() {
        val original = "\uFEFFLocalVibe\t测试。。。。2026年9月16日。"

        val first = BookTtsTextNormalizer.normalize(original)
        val second = BookTtsTextNormalizer.normalize(first.spokenText)

        assertEquals(first.spokenText, second.spokenText)
        assertEquals(second.spokenText, BookTtsTextNormalizer.normalize(original).spokenText)
    }

    @Test
    fun controlOnlyInputCanBecomeBlankWhileOriginalIsPreserved() {
        val original = "\uFEFF\u200B\u0000"

        val normalized = BookTtsTextNormalizer.normalize(original)

        assertEquals(original, normalized.originalText)
        assertTrue(normalized.spokenText.isBlank())
    }
}

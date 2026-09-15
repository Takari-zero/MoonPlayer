package com.shenghui.localvibe.core.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookTtsQualityCorpusTest {
    @Test
    fun baselineCorpusCoversRequiredCategories() {
        val categories = BookTtsQualityCorpus.samples.map { it.category }.toSet()

        assertTrue(TtsQualityCategory.PLAIN_CHINESE in categories)
        assertTrue(TtsQualityCategory.HETERONYM in categories)
        assertTrue(TtsQualityCategory.NUMBER in categories)
        assertTrue(TtsQualityCategory.DATE_TIME in categories)
        assertTrue(TtsQualityCategory.CHINESE_ENGLISH in categories)
        assertTrue(TtsQualityCategory.PUNCTUATION in categories)
        assertTrue(TtsQualityCategory.LONG_SENTENCE in categories)
    }

    @Test
    fun corpusUsesHandwrittenNonBookSamples() {
        assertEquals(7, BookTtsQualityCorpus.samples.size)
        assertTrue(BookTtsQualityCorpus.samples.all { it.id.isNotBlank() })
        assertTrue(BookTtsQualityCorpus.samples.all { it.text.isNotBlank() })
        assertTrue(BookTtsQualityCorpus.samples.any { it.text.length in 80..150 })
    }
}

private object BookTtsQualityCorpus {
    val samples: List<TtsQualitySample> = listOf(
        TtsQualitySample(
            id = "plain_zh_001",
            category = TtsQualityCategory.PLAIN_CHINESE,
            text = "今天下午我们继续完成播放器测试。"
        ),
        TtsQualitySample(
            id = "heteronym_001",
            category = TtsQualityCategory.HETERONYM,
            text = "我在银行门口行走，听见重庆的朋友说这件事很重要，音乐让人快乐，也陪我们慢慢长大。"
        ),
        TtsQualitySample(
            id = "number_001",
            category = TtsQualityCategory.NUMBER,
            text = "测试编号是一二三，版本是2026，圆周率近似3.14，当前进度是50%。"
        ),
        TtsQualitySample(
            id = "datetime_001",
            category = TtsQualityCategory.DATE_TIME,
            text = "计划在2026年9月16日上午8点30分开始验证。"
        ),
        TtsQualitySample(
            id = "zh_en_001",
            category = TtsQualityCategory.CHINESE_ENGLISH,
            text = "LocalVibe 使用 Android Media3 播放视频，也会在本地完成小说朗读。"
        ),
        TtsQualitySample(
            id = "punctuation_001",
            category = TtsQualityCategory.PUNCTUATION,
            text = "请注意：这里有逗号，句号。还有问题吗？当然有！我们继续。"
        ),
        TtsQualitySample(
            id = "long_001",
            category = TtsQualityCategory.LONG_SENTENCE,
            text = "为了验证长句朗读是否稳定，我们准备了一段完全手写的测试文本，它不会引用任何小说内容，只用于观察是否漏字、重复、截断、提前结束，以及高亮是否能跟随当前正在播放的语句继续移动。"
        )
    )
}

private data class TtsQualitySample(
    val id: String,
    val category: TtsQualityCategory,
    val text: String
)

private enum class TtsQualityCategory {
    PLAIN_CHINESE,
    HETERONYM,
    NUMBER,
    DATE_TIME,
    CHINESE_ENGLISH,
    PUNCTUATION,
    LONG_SENTENCE
}

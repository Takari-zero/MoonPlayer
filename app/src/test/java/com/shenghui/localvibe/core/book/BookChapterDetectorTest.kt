package com.shenghui.localvibe.core.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookChapterDetectorTest {
    @Test
    fun detectsSupportedChineseChapterClassifiers() {
        val paragraphs = listOf(
            "第1章",
            "第12章 标题",
            "第一章",
            "第十二章 标题",
            "第一节",
            "第一回",
            "第一卷",
            "第一部",
            "第一集"
        )

        val chapters = BookChapterDetector.detect(paragraphs)

        assertEquals(paragraphs, chapters.map(BookChapter::title))
        assertEquals(paragraphs.indices.toList(), chapters.map(BookChapter::paragraphIndex))
    }

    @Test
    fun detectsSupportedChineseNumeralCharacters() {
        val paragraphs = listOf(
            "第零章",
            "第〇章",
            "第一二三四五六七八九十章",
            "第一百二十三章",
            "第一千章",
            "第一万章",
            "第两章"
        )

        val chapters = BookChapterDetector.detect(paragraphs)

        assertEquals(paragraphs, chapters.map(BookChapter::title))
    }

    @Test
    fun detectsArabicChapterAndVolumeForms() {
        val paragraphs = listOf("第1章", "第123章", "卷1 标题", "卷12 标题")

        val chapters = BookChapterDetector.detect(paragraphs)

        assertEquals(paragraphs, chapters.map(BookChapter::title))
    }

    @Test
    fun volumeRequiresTitleAfterWhitespace() {
        val chapters = BookChapterDetector.detect(listOf("卷1", "卷1 标题"))

        assertEquals(listOf(BookChapter("卷1 标题", 1)), chapters)
    }

    @Test
    fun normalizesAsciiWhitespaceAndTabs() {
        val chapters = BookChapterDetector.detect(
            listOf(
                "   第1章    标题   ",
                "\t第\t12\t章\t标题\t",
                " \t第  3\t章   混合空白 \t"
            )
        )

        assertEquals(
            listOf("第1章 标题", "第 12 章 标题", "第 3 章 混合空白"),
            chapters.map(BookChapter::title)
        )
    }

    @Test
    fun preservesCurrentFullWidthSpaceBehavior() {
        val chapters = BookChapterDetector.detect(listOf("　第一　章　标题　", "卷　12　标题"))

        assertEquals(listOf("第一　章　标题", "卷　12　标题"), chapters.map(BookChapter::title))
    }

    @Test
    fun ignoresEmptyAndWhitespaceOnlyParagraphs() {
        val chapters = BookChapterDetector.detect(listOf("", "   ", "\t\t", "　　"))

        assertTrue(chapters.isEmpty())
    }

    @Test
    fun rejectsOrdinaryTextThatContainsChapterLikeCharacters() {
        val chapters = BookChapterDetector.detect(
            listOf(
                "这是一段普通中文正文。",
                "第一个问题不是章节标题。",
                "这卷书很好看。",
                "卷起来放好。",
                "123 开始的普通正文",
                "短句"
            )
        )

        assertTrue(chapters.isEmpty())
    }

    @Test
    fun appliesFortyCharacterLimitAfterNormalization() {
        val fortyCharacters = "第1章" + "题".repeat(37)
        val fortyOneCharacters = "第1章" + "题".repeat(38)

        val chapters = BookChapterDetector.detect(listOf(fortyCharacters, fortyOneCharacters))

        assertEquals(40, fortyCharacters.length)
        assertEquals(41, fortyOneCharacters.length)
        assertEquals(listOf(BookChapter(fortyCharacters, 0)), chapters)
    }

    @Test
    fun rawLongWhitespaceCanNormalizeIntoValidShortHeading() {
        val raw = "   第1章" + " ".repeat(60) + "标题   "

        val chapters = BookChapterDetector.detect(listOf(raw))

        assertTrue(raw.length > 40)
        assertEquals(listOf(BookChapter("第1章 标题", 0)), chapters)
    }

    @Test
    fun acceptsCurrentPunctuationSuffixSemantics() {
        val paragraphs = listOf("第1章：标题", "第1章 标题！")

        val chapters = BookChapterDetector.detect(paragraphs)

        assertEquals(paragraphs, chapters.map(BookChapter::title))
    }

    @Test
    fun rejectsCurrentlyUnsupportedHeadingForms() {
        val chapters = BookChapterDetector.detect(
            listOf(
                "Chapter 1",
                "第Ⅰ章",
                "第１章",
                "第廿章",
                "第卅章"
            )
        )

        assertTrue(chapters.isEmpty())
    }

    @Test
    fun preservesOriginalParagraphIndicesAndOrder() {
        val chapters = BookChapterDetector.detect(
            listOf(
                "普通正文",
                "第一章 开始",
                "另一段正文",
                "卷2 下卷",
                "结尾正文"
            )
        )

        assertEquals(
            listOf(
                BookChapter("第一章 开始", 1),
                BookChapter("卷2 下卷", 3)
            ),
            chapters
        )
    }
}

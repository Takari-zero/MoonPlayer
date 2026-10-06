package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PreviewPreparedCurrentAdoptionTest {
    @Test
    fun planCopiesAllFormalSentenceCoordinatesWithoutPlaying() {
        val plan = PreviewPreparedCurrentAdoption.plan(
            ReaderSentence(
                text = "prepared current",
                paragraphIndex = 4,
                sentenceIndexInParagraph = 2,
                chapterSentenceIndex = 17
            )
        )

        assertEquals(PreviewPreparedCurrentAdoptionPlan(4, 2, 17), plan)
    }

    @Test
    fun missingOrInvalidPreparedCurrentCannotBeAdopted() {
        assertNull(PreviewPreparedCurrentAdoption.plan(null))
        assertNull(PreviewPreparedCurrentAdoption.plan(ReaderSentence("invalid", -1, 0, 0)))
    }
}

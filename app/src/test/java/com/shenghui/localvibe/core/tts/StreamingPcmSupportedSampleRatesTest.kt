package com.shenghui.localvibe.core.tts

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamingPcmSupportedSampleRatesTest {
    @Test
    fun acceptsExistingAndMatchaRates() {
        listOf(8000, 16000, 22050, 24000, 44100).forEach {
            assertTrue(StreamingPcmSupportedSampleRates.isSupported(it))
        }
    }

    @Test
    fun rejectsUnapprovedRate() {
        assertFalse(StreamingPcmSupportedSampleRates.isSupported(12345))
    }
}

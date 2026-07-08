package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookReaderEntryReadyShadowLoggerTest {
    @Test
    fun legacyPathRunsEntryReadyShadowDiagnostics() {
        assertTrue(shouldRunBookReaderEntryReadyShadow(usesEntryReadyState = false))
    }

    @Test
    fun readyPathSkipsEntryReadyShadowDiagnostics() {
        assertFalse(shouldRunBookReaderEntryReadyShadow(usesEntryReadyState = true))
    }
}

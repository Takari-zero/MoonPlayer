package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookPlaybackSessionGuardTest {
    @Test
    fun replacementKeepsOnlyCurrentSessionActive() {
        assertFalse(BookPlaybackSessionGuard.isActive(1L, 3L, screenDisposed = false))
        assertFalse(BookPlaybackSessionGuard.isActive(2L, 3L, screenDisposed = false))
        assertTrue(BookPlaybackSessionGuard.isActive(3L, 3L, screenDisposed = false))
    }

    @Test
    fun disposedScreenRejectsEvenCurrentSession() {
        assertFalse(BookPlaybackSessionGuard.isActive(3L, 3L, screenDisposed = true))
    }
}

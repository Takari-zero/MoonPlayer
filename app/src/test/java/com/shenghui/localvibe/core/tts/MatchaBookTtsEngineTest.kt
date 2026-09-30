package com.shenghui.localvibe.core.tts

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchaBookTtsEngineTest {
    @Test
    fun `stop invalidates old request and next request is active`() {
        val lifecycle = newLifecycle()
        val first = lifecycle.start()

        lifecycle.stop()

        val second = lifecycle.start()

        assertFalse(lifecycle.isActive(first, "late_completion"))
        assertTrue(lifecycle.isActive(second, "new_request"))
    }

    @Test
    fun `latest request wins over a late completion`() {
        val lifecycle = newLifecycle()
        val first = lifecycle.start()
        val second = lifecycle.start()

        assertFalse(lifecycle.isActive(first, "late_completion"))
        assertTrue(lifecycle.isActive(second, "current_request"))
    }

    @Test
    fun `stop invalidates the current request`() {
        val lifecycle = newLifecycle()
        val request = lifecycle.start()

        lifecycle.stop()

        assertFalse(lifecycle.isActive(request, "after_stop"))
    }

    private fun newLifecycle(): MatchaRequestLifecycle {
        return MatchaRequestLifecycle()
    }
}

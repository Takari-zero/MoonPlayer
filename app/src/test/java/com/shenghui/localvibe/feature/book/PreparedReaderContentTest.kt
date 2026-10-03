package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertEquals
import org.junit.Test

class PreparedReaderContentTest {
    @Test
    fun carriesAuthoritativeContentVersion() {
        val version = PreviewContentVersion.fingerprint(4096L, "a".repeat(64))

        val prepared = PreparedReaderContent(authoritativeContentVersion = version)

        assertEquals(version, prepared.authoritativeContentVersion)
    }
}

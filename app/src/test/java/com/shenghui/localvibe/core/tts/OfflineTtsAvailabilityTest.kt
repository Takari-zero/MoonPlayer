package com.shenghui.localvibe.core.tts

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineTtsAvailabilityTest {
    @Test
    fun nativeUnavailableMakesOfflineEnginesUnavailable() {
        val availability = OfflineTtsAvailability.evaluate(
            OfflineTtsResourceState(
                nativeRuntimeLoadable = false,
                supportedAbiPresent = true,
                aishell3ModelAvailable = true,
                builtInModelAvailable = true
            )
        )

        assertFalse(availability.nativeRuntimeAvailable)
        assertFalse(availability.aishell3Available)
        assertFalse(availability.builtInOfflineAvailable)
    }

    @Test
    fun unsupportedAbiMakesOfflineEnginesUnavailable() {
        val availability = OfflineTtsAvailability.evaluate(
            OfflineTtsResourceState(
                nativeRuntimeLoadable = true,
                supportedAbiPresent = false,
                aishell3ModelAvailable = true,
                builtInModelAvailable = true
            )
        )

        assertFalse(availability.nativeRuntimeAvailable)
        assertFalse(availability.aishell3Available)
        assertFalse(availability.builtInOfflineAvailable)
    }

    @Test
    fun missingModelMakesOnlyThatEngineUnavailable() {
        val availability = OfflineTtsAvailability.evaluate(
            OfflineTtsResourceState(
                nativeRuntimeLoadable = true,
                supportedAbiPresent = true,
                aishell3ModelAvailable = false,
                builtInModelAvailable = true
            )
        )

        assertTrue(availability.nativeRuntimeAvailable)
        assertFalse(availability.aishell3Available)
        assertTrue(availability.builtInOfflineAvailable)
    }

    @Test
    fun nativeAndModelsAvailableMakesOfflineEnginesAvailable() {
        val availability = OfflineTtsAvailability.evaluate(
            OfflineTtsResourceState(
                nativeRuntimeLoadable = true,
                supportedAbiPresent = true,
                aishell3ModelAvailable = true,
                builtInModelAvailable = true
            )
        )

        assertTrue(availability.nativeRuntimeAvailable)
        assertTrue(availability.aishell3Available)
        assertTrue(availability.builtInOfflineAvailable)
    }

    @Test
    fun packagedAbiMatchesSupportedAbiList() {
        assertTrue(OfflineTtsAvailability.hasPackagedAbi(listOf("x86_64", "arm64-v8a")))
        assertFalse(OfflineTtsAvailability.hasPackagedAbi(listOf("armeabi-v7a", "x86")))
    }
}

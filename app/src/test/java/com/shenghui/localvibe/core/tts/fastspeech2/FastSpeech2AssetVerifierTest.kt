package com.shenghui.localvibe.core.tts.fastspeech2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FastSpeech2AssetVerifierTest {
    @Test
    fun contractUsesFormalAssetPaths() {
        assertEquals("fastspeech2", FastSpeech2AssetContract.ASSET_DIR)
        assertEquals("fastspeech2/baker_mapper.json", FastSpeech2AssetContract.MAPPER_JSON)
        assertEquals("fastspeech2/fastspeech2_quan.tflite", FastSpeech2AssetContract.FASTSPEECH2_MODEL)
        assertEquals("fastspeech2/mb_melgan.tflite", FastSpeech2AssetContract.MB_MELGAN_MODEL)
    }

    @Test
    fun verifierReportsAllAssetsPresent() {
        val result = FastSpeech2AssetVerifier.verifyForTest { true }

        assertTrue(result.isComplete)
        assertEquals(emptyList<String>(), result.missingFiles)
    }

    @Test
    fun verifierReportsMissingFilesByFormalPath() {
        val result = FastSpeech2AssetVerifier.verifyForTest { path ->
            path != FastSpeech2AssetContract.MB_MELGAN_MODEL
        }

        assertFalse(result.isComplete)
        assertEquals(listOf("fastspeech2/mb_melgan.tflite"), result.missingFiles)
    }
}

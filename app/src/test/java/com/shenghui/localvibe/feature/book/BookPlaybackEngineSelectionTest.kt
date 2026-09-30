package com.shenghui.localvibe.feature.book

import org.junit.Assert.assertEquals
import org.junit.Test

class BookPlaybackEngineSelectionTest {
    @Test
    fun systemPreferenceUsesSystemTts() {
        assertEquals(
            BookPlaybackEngine.SYSTEM_TTS,
            BookPlaybackEngineSelection.effective(BookPlaybackEngine.SYSTEM_TTS, aishell3Available = true)
        )
    }

    @Test
    fun availableAishell3PreferenceUsesAishell3() {
        assertEquals(
            BookPlaybackEngine.AISHELL3,
            BookPlaybackEngineSelection.effective(BookPlaybackEngine.AISHELL3, aishell3Available = true)
        )
    }

    @Test
    fun userSelectingAishell3FromSystemPreferenceUsesAishell3WhenAvailable() {
        val currentPreferred = BookPlaybackEngine.SYSTEM_TTS
        val requested = BookPlaybackEngine.AISHELL3

        assertEquals(
            BookPlaybackEngine.AISHELL3,
            BookPlaybackEngineSelection.effective(requested, aishell3Available = true)
        )
        assertEquals(BookPlaybackEngine.SYSTEM_TTS, currentPreferred)
    }

    @Test
    fun unavailableAishell3FallsBackWithoutChangingPreference() {
        val preferred = BookPlaybackEngine.AISHELL3

        assertEquals(
            BookPlaybackEngine.SYSTEM_TTS,
            BookPlaybackEngineSelection.effective(preferred, aishell3Available = false)
        )
        assertEquals(BookPlaybackEngine.AISHELL3, preferred)
    }

    @Test
    fun invalidPersistedValueUsesDefault() {
        assertEquals(
            BookPlaybackEngine.AISHELL3,
            BookPlaybackEngineSelection.parse("unknown", BookPlaybackEngine.AISHELL3)
        )
    }

    @Test
    fun optionsAlwaysRepresentAishell3WhenUnavailable() {
        val options = BookPlaybackEngineSelection.options(
            preferred = BookPlaybackEngine.SYSTEM_TTS,
            aishell3Available = false,
            aishell3UnavailableReason = "资源未安装"
        )

        assertEquals(
            listOf(BookPlaybackEngine.SYSTEM_TTS, BookPlaybackEngine.AISHELL3),
            options.map { it.engine }
        )
        assertEquals("系统语音引擎", options[0].title)
        assertEquals("自研离线", options[1].title)
        assertEquals(false, options[1].enabled)
        assertEquals("资源未安装", options[1].unavailableReason)
    }

    @Test
    fun availableAishell3OptionIsSelectableAndPreferredOptionIsSelected() {
        val options = BookPlaybackEngineSelection.options(
            preferred = BookPlaybackEngine.AISHELL3,
            aishell3Available = true,
            aishell3UnavailableReason = null
        )

        assertEquals(true, options[1].enabled)
        assertEquals(BookPlaybackEngine.AISHELL3, options.single { it.engine == BookPlaybackEngine.AISHELL3 }.engine)
    }

    @Test
    fun availableExperimentalMatchaIsSelectableWithoutChangingDefaultOptions() {
        assertEquals(
            BookPlaybackEngine.MATCHA_EXPERIMENTAL,
            BookPlaybackEngineSelection.effective(
                BookPlaybackEngine.MATCHA_EXPERIMENTAL,
                aishell3Available = false,
                matchaAvailable = true
            )
        )
        val options = BookPlaybackEngineSelection.options(
            preferred = BookPlaybackEngine.SYSTEM_TTS,
            aishell3Available = true,
            aishell3UnavailableReason = null,
            matchaAvailable = true,
            includeExperimentalMatcha = true
        )
        assertEquals(
            BookPlaybackEngine.MATCHA_EXPERIMENTAL,
            options.single { it.engine == BookPlaybackEngine.MATCHA_EXPERIMENTAL }.engine
        )
    }

    @Test
    fun unavailableExperimentalMatchaRemainsVisibleButDisabled() {
        val option = BookPlaybackEngineSelection.options(
            preferred = BookPlaybackEngine.SYSTEM_TTS,
            aishell3Available = true,
            aishell3UnavailableReason = null,
            matchaAvailable = false,
            includeExperimentalMatcha = true
        ).single { it.engine == BookPlaybackEngine.MATCHA_EXPERIMENTAL }

        assertEquals(false, option.enabled)
        assertEquals("Matcha benchmark 模型未安装", option.unavailableReason)
    }

    @Test
    fun pendingDispatchUsesLatestMatchaSnapshot() {
        val initial = BookPlaybackEngineSnapshot(
            preferred = BookPlaybackEngine.AISHELL3,
            effective = BookPlaybackEngine.AISHELL3,
            matchaAvailable = true
        )
        val latest = BookPlaybackEngineSnapshot(
            preferred = BookPlaybackEngine.MATCHA_EXPERIMENTAL,
            effective = BookPlaybackEngine.MATCHA_EXPERIMENTAL,
            matchaAvailable = true
        )

        assertEquals(BookPlaybackEngine.AISHELL3, initial.dispatchEngine())
        assertEquals(BookPlaybackEngine.MATCHA_EXPERIMENTAL, latest.dispatchEngine())
    }

    @Test
    fun pendingDispatchUsesLatestSystemTtsAfterProviderChange() {
        val latest = BookPlaybackEngineSnapshot(
            preferred = BookPlaybackEngine.SYSTEM_TTS,
            effective = BookPlaybackEngine.SYSTEM_TTS,
            matchaAvailable = true
        )

        assertEquals(BookPlaybackEngine.SYSTEM_TTS, latest.dispatchEngine())
    }

    @Test
    fun unavailableMatchaSnapshotDispatchesEffectiveAishell3Fallback() {
        val latest = BookPlaybackEngineSnapshot(
            preferred = BookPlaybackEngine.MATCHA_EXPERIMENTAL,
            effective = BookPlaybackEngine.AISHELL3,
            matchaAvailable = false
        )

        assertEquals(BookPlaybackEngine.AISHELL3, latest.dispatchEngine())
    }
}

# FastSpeech2 Formal Phase 3E Dry-Run And Debug Isolation

## Scope

Phase 3E adds debug package isolation and formal reader dry-run logs. It does not enable formal playback, load models, access assets, play real audio, or change the legacy reader path.

## Debug Package Isolation

`app/build.gradle.kts` now configures the debug build with:

```kotlin
debug {
    applicationIdSuffix = ".fastspeech2formal"
}
```

Expected debug package:

```text
com.shenghui.localvibe.fastspeech2formal
```

The release application id remains unchanged.

Local APK inspection confirmed:

```text
package: name='com.shenghui.localvibe.fastspeech2formal'
```

## Feature Flags

`BookListenScreen.kt` keeps formal playback disabled:

```kotlin
private const val USE_FORMAL_BOOK_PLAYBACK_CONTROLLER = false
private const val LOG_FORMAL_BOOK_PLAYBACK_DRY_RUN = true
```

Rules:

- The legacy reader playback path still executes.
- Formal dry-run only logs target construction.
- Dry-run does not return early.
- Dry-run does not call real FastSpeech2 playback.
- Dry-run does not load models, access assets, or touch AudioTrack.

## Dry-Run Coverage

Dry-run logging now covers:

- play target construction
- pause
- resume
- stop
- seek target construction
- sentence tap target construction
- reader next chapter target construction
- reader previous chapter target construction

Dry-run tag:

```text
LV_BOOK_FORMAL_DRYRUN
```

Example logs:

```text
LV_BOOK_FORMAL_DRYRUN play target chapterIndex=... chapterTitle=... paragraphIndex=... sentenceIndex=...
LV_BOOK_FORMAL_DRYRUN seek target chapterIndex=... paragraphIndex=... sentenceIndex=... resumeIfPlaying=...
LV_BOOK_FORMAL_DRYRUN sentence_tap target chapterIndex=... paragraphIndex=... sentenceIndex=... resumeIfPlaying=...
LV_BOOK_FORMAL_DRYRUN reader_next_chapter target chapterIndex=... chapterTitle=... paragraphIndex=... sentenceIndex=... resumeIfPlaying=...
LV_BOOK_FORMAL_DRYRUN reader_previous_chapter target chapterIndex=... chapterTitle=... paragraphIndex=... sentenceIndex=... resumeIfPlaying=...
LV_BOOK_FORMAL_DRYRUN target skipped reason=missing_chapter_index source=...
```

The target helpers never synthesize a `chapterIndex = -1` target. Missing metadata is logged as skipped and the legacy path continues.

## Not Connected In Phase 3E

- No mini console.
- No recent listen persistence.
- No playback mode wiring.
- No real FastSpeech2 model loading.
- No model assets.
- No local AAR files.
- No real AudioTrack sink.
- No system TTS gate changes.
- No Aishell3 fallback changes.
- No MediaSession, Foreground Service, notification, or lock-screen controls.

## Verification

Commands run from `E:\gitType\LocalVibe-fastspeech2-formal` with temporary `ANDROID_HOME=E:\Android`:

```powershell
.\gradlew.bat :app:testDebugUnitTest --console=plain
.\gradlew.bat :app:assembleDebug --console=plain
```

Results:

- `:app:testDebugUnitTest` passed.
- `:app:assembleDebug` passed.
- Local APK package inspection confirmed `com.shenghui.localvibe.fastspeech2formal`.

Install verification:

- ADB was checked with project-required `E:\Android\platform-tools\adb.exe -P 62001 devices`.
- No device was listed on port `62001`, so installation and runtime dry-run log verification were not performed.

## Phase 4 Recommendation

Before enabling formal playback, choose one of two paths:

1. Add a dry-run reader UI synchronization layer that proves controller target state, title, purple highlight, and scroll anchor remain aligned.
2. Define the formal `BookReaderAudioSink` and model asset strategy, including where FastSpeech2/MB-MelGAN models live and how they affect APK size.

Do not enable real FastSpeech2 playback until model packaging and AudioSink lifecycle are reviewed.

## Runtime Dry-Run Verification Result

Formal debug dry-run was installed as `com.shenghui.localvibe.fastspeech2formal` and verified with the formal feature flag still disabled:

```kotlin
private const val USE_FORMAL_BOOK_PLAYBACK_CONTROLLER = false
private const val LOG_FORMAL_BOOK_PLAYBACK_DRY_RUN = true
```

Observed log summary:

- `LV_BOOK_FORMAL_DRYRUN` appeared.
- Play target construction appeared with explicit `chapterIndex`, `chapterTitle`, `paragraphIndex`, and `sentenceIndex`.
- Reader next chapter target construction appeared.
- `chapterIndex=-1`: `0`.
- `missing_chapter_index`: `0`.
- The system TTS / voice settings page came from the legacy path after `fallback to legacy reason=feature_flag_disabled`; this is expected while the formal controller flag remains `false`.
- No formal debug package `FATAL EXCEPTION` or app crash was found in the screened log.

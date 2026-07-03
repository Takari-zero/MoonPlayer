# FastSpeech2 Formal Phase 4C Controller Smoke

## Scope

Phase 4C verifies the formal `BookReaderPlaybackController` with the real `FastSpeech2BookTtsEngineAdapter` and `BookReaderAudioTrackSink` through a debug-only smoke Activity.

This phase does not connect `BookListenScreen`, `MainActivity`, the novel-home mini console, recent records, playback mode UI, system TTS gate, or Aishell3 fallback.

## Retry Fix

The first Phase 4C run failed because the debug smoke Activity treated lifecycle/cleanup cancellation as a smoke failure:

```text
controller smoke error Job was cancelled
```

The retry changed only the debug smoke harness cancellation isolation:

- The smoke job uses an independent `SupervisorJob` on `Dispatchers.Default`.
- Activity lifecycle cancellation no longer marks expected session stale/cleanup as a smoke error.
- `controller smoke complete=true` is logged before cleanup.
- Cleanup runs after complete/error and logs `cleanup release done`.
- Expected stale-session behavior is logged as expected cancellation.

## Feature Flag State

The formal reader remains disabled:

```kotlin
private const val USE_FORMAL_BOOK_PLAYBACK_CONTROLLER = false
```

The smoke test runs only from:

```text
app/src/debug/java/com/shenghui/localvibe/debug/BookReaderControllerSmokeTestActivity.kt
```

## Build Verification

Commands run from `E:\gitType\LocalVibe-fastspeech2-formal`:

```powershell
$env:ANDROID_HOME="E:\Android"
$env:JAVA_HOME=""
.\gradlew.bat :app:testDebugUnitTest --console=plain
.\gradlew.bat :app:assembleDebug --console=plain
```

Results:

- `:app:testDebugUnitTest` passed.
- `:app:assembleDebug` passed.

## Install Verification

Installed with default ADB as explicitly approved for this verification round.

Package check:

```text
package:com.shenghui.localvibe
package:com.shenghui.localvibe.fastspeech2formal
```

The formal debug package did not overwrite the main app.

## Runtime Smoke Log

Log file:

```text
test-results/formal_controller_smoke_retry_log.txt
```

Summary counts:

```text
controller smoke complete=true = 1
controller smoke error = 0
step done name=play_short = 1
step done name=pause_resume = 1
step done name=play_medium = 1
step done name=play_long = 1
step done name=quick_switch = 1
expected cancellation reason=quick_switch_old_session = 1
LV_BOOK_PLAYBACK stale session ignored = 2
LV_BOOK_PLAYBACK pause sessionId = 1
LV_BOOK_PLAYBACK resume sessionId = 1
LV_BOOK_PLAYBACK stop reason = 1
AudioTrackSink write main=false = 100
bufferCapacity=4 = 0
Cannot copy from a TensorFlowLite tensor = 0
FATAL EXCEPTION = 0
ANR = 0
```

`AndroidRuntime` appeared only as other-process VM exit lines and was not a `com.shenghui.localvibe.fastspeech2formal` crash.

## Controller Behavior Verified

- `controller.play(target)` initializes/synthesizes/writes PCM through the real engine and sink.
- `controller.pause()` reaches the AudioSink and logs pause.
- `controller.resume()` reaches the AudioSink and logs resume.
- `controller.stop(reason)` stops the active session and makes the old session stale.
- Quick switch starts a new target and the old session is ignored as stale.
- AudioTrack writes occur off the main thread.

## Not Connected In Phase 4C

- No `BookListenScreen` integration.
- No `MainActivity` integration.
- No mini console.
- No recent record.
- No system TTS gate changes.
- No Aishell3 fallback changes.
- No release manifest changes.
- Model files remain ignored and were not added to Git status.

## Phase 4D Recommendation

Next phase should temporarily enable the formal reader path only in the debug package and verify a tiny reader surface, starting with play/pause only. Do not connect mini/recent/chapter mode until the debug reader play/pause path is stable.

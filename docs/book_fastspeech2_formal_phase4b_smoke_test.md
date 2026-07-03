# FastSpeech2 Formal Phase 4B Smoke Test

## Scope

Phase 4B adds a debug-only FastSpeech2 smoke test path. It verifies that the formal branch can load ignored FastSpeech2 assets, synthesize PCM with the formal `FastSpeech2BookTtsEngine`, and write PCM through the formal `BookReaderAudioTrackSink`.

This phase does not connect `BookListenScreen`, `MainActivity`, the novel-home mini console, recent record, playback mode, system TTS gate, or Aishell3 fallback.

## Patch Baseline

Saved Phase 4A WIP patch:

```text
localvibe_fastspeech2_formal_phase4a_assets_audiosink.patch
```

## Ignored Assets

The following files were copied from the benchmark staging directory into ignored formal assets:

```text
app/src/main/assets/fastspeech2/baker_mapper.json
app/src/main/assets/fastspeech2/fastspeech2_quan.tflite
app/src/main/assets/fastspeech2/mb_melgan.tflite
```

Not copied:

```text
tacotron2_quan.tflite
models-tf.7z
AAR files
other models
```

`git status --short` did not show files under `app/src/main/assets/fastspeech2/`, confirming the ignored model assets were not added to source control.

## Debug-Only Smoke Activity

Added debug-only files:

```text
app/src/debug/AndroidManifest.xml
app/src/debug/java/com/shenghui/localvibe/debug/FastSpeech2SmokeTestActivity.kt
```

The smoke activity:

- runs only in the debug source set;
- verifies assets with `FastSpeech2AssetVerifier`;
- initializes `FastSpeech2BookTtsEngine` with `FastSpeech2AssetContract.ASSET_DIR`;
- synthesizes short, medium, and long Chinese test cases;
- writes PCM through `BookReaderAudioTrackSink`;
- logs with `LV_BOOK_SMOKE`.

The third-party `ZhProcessor` was minimally adapted so the formal engine can pass an explicit mapper asset path. The default constructor keeps the old path for compatibility.

## Reader Flag State

The formal reader flag remains disabled:

```kotlin
private const val USE_FORMAL_BOOK_PLAYBACK_CONTROLLER = false
private const val LOG_FORMAL_BOOK_PLAYBACK_DRY_RUN = true
```

Phase 4B smoke test runs only through the debug Activity and does not replace the legacy reader playback chain.

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

Installed with default ADB as explicitly approved for this verification round:

```text
E:\Android\platform-tools\adb.exe
```

Package check showed both apps present:

```text
package:com.shenghui.localvibe
package:com.shenghui.localvibe.fastspeech2formal
```

The formal debug package did not overwrite the main app.

## Runtime Smoke Results

Log file:

```text
test-results/formal_fastspeech2_smoke_log.txt
```

Asset check:

```text
LV_BOOK_SMOKE assets check complete=true missing=[]
```

Engine init:

```text
LV_BOOK_SMOKE engine init costMs=301
```

Short case:

```text
first pcm ready costMs=1381 bytes=55200
full pcm done costMs=1381 durationMs=1150 rtf=1.1886956521739132
playback complete case=short
```

Medium case:

```text
first pcm ready costMs=952 bytes=142800
full pcm done costMs=952 durationMs=2975 rtf=0.3196638655462185
playback complete case=medium
```

Long case:

```text
first pcm ready costMs=488 bytes=205800
full pcm done costMs=488 durationMs=4287 rtf=0.11383251691159318
playback complete case=long
```

AudioTrack evidence:

```text
LV_BOOK_SMOKE audio sink write main=false
LV_BOOK_PLAYBACK AudioTrackSink write main=false
```

`LV_BOOK_PLAYBACK.*write main=false` appeared 100 times in the screened log.

## Error Screening

Screened results:

```text
bufferCapacity=4 = 0
Cannot copy from a TensorFlowLite tensor = 0
FATAL EXCEPTION = 0
ANR = 0
LV_BOOK_SMOKE error = 0
```

One `AndroidRuntime` line was present from another process VM exit and was not a LocalVibe formal crash.

## Not Connected In Phase 4B

- No `BookListenScreen` integration.
- No `MainActivity` or mini console integration.
- No recent record integration.
- No mode integration.
- No system TTS gate changes.
- No Aishell3 fallback changes.
- No release manifest change.
- No model or AAR submitted to Git.

## Phase 4C Recommendation

Next phase should connect the smoke-verified FastSpeech2 engine and `BookReaderAudioTrackSink` to `BookReaderPlaybackController` behind the existing feature flag. Keep `USE_FORMAL_BOOK_PLAYBACK_CONTROLLER=false` until a separate controlled verification explicitly enables it.

# FastSpeech2 Formal Phase 4A Assets And AudioSink Boundary

## Scope

Phase 4A defines the formal FastSpeech2 model asset contract and adds an AudioTrack-backed `BookReaderAudioSink` boundary. It does not enable real reader playback, connect `BookListenScreen`, connect the home mini console, or change the legacy reader path.

## Added Asset Contract

Added:

```text
app/src/main/java/com/shenghui/localvibe/core/tts/fastspeech2/FastSpeech2AssetContract.kt
app/src/main/java/com/shenghui/localvibe/core/tts/fastspeech2/FastSpeech2AssetVerifier.kt
```

Formal asset paths:

```text
fastspeech2/baker_mapper.json
fastspeech2/fastspeech2_quan.tflite
fastspeech2/mb_melgan.tflite
```

`FastSpeech2AssetVerifier` only checks asset existence. It does not load TensorFlow Lite models, initialize the TTS engine, or synthesize audio.

## Model And AAR Policy

No model files were copied in Phase 4A.

No local AAR files were added or submitted. The Maven TensorFlow Lite dependencies introduced in Phase 1 remain the active dependency strategy.

The repository ignore rules continue to protect local model/AAR staging paths:

```gitignore
app/src/main/assets/fastspeech2/
app/libs/tflite-local/
```

## Added AudioSink Boundary

Added:

```text
app/src/main/java/com/shenghui/localvibe/feature/book/playback/BookReaderAudioTrackSink.kt
```

Boundary behavior:

- Uses `AudioTrack` with `USAGE_MEDIA`, `CONTENT_TYPE_SPEECH`, mono, PCM 16-bit, and `MODE_STREAM`.
- Writes PCM from `Dispatchers.IO` and logs `main=false` during write calls.
- Tracks `sessionId` so stale PCM from old sessions is dropped.
- Supports `pause`, `resume`, `stop(reason)`, and `release`.
- Is not connected to `BookListenScreen` or any UI in Phase 4A.

## Not Enabled

- `USE_FORMAL_BOOK_PLAYBACK_CONTROLLER` remains `false`.
- No real FastSpeech2 playback is enabled.
- No model assets are bundled.
- No local AAR is submitted.
- No mini console, recent record, playback mode, MediaSession, Foreground Service, or notification integration is added.

## Verification

Commands to run from `E:\gitType\LocalVibe-fastspeech2-formal`:

```powershell
$env:ANDROID_HOME="E:\Android"
$env:JAVA_HOME=""
.\gradlew.bat :app:testDebugUnitTest --console=plain
.\gradlew.bat :app:assembleDebug --console=plain
```

Results:

- :app:testDebugUnitTest passed.
- :app:assembleDebug passed.

## Phase 4B Recommendation

Next phase should copy the ignored FastSpeech2 model files into debug assets for a formal smoke test, but still avoid replacing the legacy reader path until model packaging and AudioSink lifecycle are explicitly reviewed.


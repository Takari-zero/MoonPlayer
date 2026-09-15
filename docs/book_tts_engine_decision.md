# Book TTS Engine Decision

## Current state

The current Aishell3/Sherpa experiment has produced several useful pieces that should be preserved:

- Fully offline playback is possible.
- Spoken sentence and purple reader highlight are synchronized.
- Re-entering after playback no longer flashes the wrong reader position in the verified path.
- Seek / slider lifecycle has been guarded.
- Sentence tap playback has ANR protection.
- `AudioTrack.write` has been moved off the main thread.
- Native synthesis is serialized to avoid Sherpa/ONNX native crashes such as SIGSEGV.
- Prepared audio cache, segment cache, and a PCM queue have been prototyped.

These changes are valuable, but they do not remove the core Aishell3 latency limit.

Runtime status:

- Commit `d286b71` adds session/lifecycle safeguards for Aishell3 streaming TTS playback.
- JVM tests and Debug build passed for that commit.
- Android real-device runtime acceptance for `d286b71` has not been completed yet.

## Engine roles

### Android System TTS

Recommended positioning: fallback / system provider.

Advantages:

- Simple integration.
- Managed by the operating system.
- Does not require LocalVibe to bundle large voice models in the APK.

Limitations:

- Voice quality and capabilities depend on the device and installed engines.
- Vendor behavior varies.
- Offline availability cannot be fully controlled by LocalVibe.

### Sherpa ONNX / BuiltInOfflineTtsEngine

Recommended positioning: formal offline TTS candidate.

Advantages:

- Can be bundled with the app or delivered as an additional resource package.
- More controllable than device-provided system TTS.
- Fits LocalVibe's offline-first direction.

Open concerns:

- Model size.
- Runtime performance on low-end devices.
- Model license and redistribution rights.
- APK size and optional voice-pack strategy.

This path should not be treated as the final primary engine until those concerns are resolved with evidence.

### Aishell3 segmented streaming TTS

Recommended positioning: experimental streaming offline TTS provider.

What it currently proves:

- Segmented generation can reduce the first generated unit size.
- PCM chunks can be queued to `AudioTrack`.
- Session IDs can isolate old playback from current playback.
- Pause/resume lifecycle has been improved in the current WIP.

Current limitation:

- It still depends on synchronous `OfflineTts.generate()`.
- It is not production ready.
- It is not the official primary book-listening engine.
- Android real-device runtime acceptance is still pending.

## Aishell3 limitations

The main unresolved issue is first-audio latency.

`OfflineTts.generate()` is synchronous. Until it returns, LocalVibe has no PCM data to send to the audio queue. This means:

- First playback is still gated by synchronous native synthesis.
- Aishell3 cannot provide a true native first PCM callback like mature streaming TTS SDKs.
- Immediate play after opening the reader cannot reliably match iFlytek-level instant playback.
- Further patches around segmenting, caching, and scheduling add complexity and regression risk.

Segmenting can reduce the size of the first unit, and caching can help after the audio exists, but neither can produce audio before `OfflineTts.generate()` returns.

## iFlytek ReadAssistant black-box findings

Observed app:

- Package: `com.iflytek.readassistant`
- Version: `2.7.3212`
- ABI: `arm64-v8a`
- Native library of interest: `libmsc.so`

System-level observations:

- Uses `AudioTrack`.
- Playback format observed as 16 kHz mono PCM.
- Has active `MediaSession` while playing.
- Requests media audio focus.
- Declares TTS-related components:
  - `.biz.common.TtsReceiver`
  - `.biz.common.TtsStatusContentProvider`
  - `com.iflytek.readassistant.tts.provider`
- User-observed behavior:
  - Reader opens immediately.
  - Playback starts immediately.
  - Seek switches audio immediately without overlap.
  - Consecutive sentence switching has no overlap.
  - Returning and re-entering restores and resumes quickly.

No reverse engineering was performed. The APK was only inspected by package metadata, file names, and system runtime state.

These observations are only high-level architecture references. LocalVibe must not copy closed-source implementation details, closed-source binaries, APK contents, or private resources from the observed app. The third-party APK used during local investigation must not be committed to this repository.

## Technical judgment

iFlytek appears to rely on a mature TTS stack built around `libmsc.so`, fast PCM output, media/session integration, and robust playback control. The likely winning properties are:

- Long-lived TTS engine or early engine initialization.
- PCM output that is streaming or close to streaming.
- Audio writer pipeline based on `AudioTrack`.
- Fast stop / flush / session switching.
- MediaSession and playback-service style lifecycle.

LocalVibe's Aishell3 path is fundamentally different. It is based on synchronous offline generation. The audio queue can make output safer once PCM exists, but it cannot make synchronous native generation behave like a streaming first-packet engine.

## Decision

Aishell3 should not be treated as the final primary book-listening engine for iFlytek-level UX.

Recommended positioning:

- Keep Aishell3 as a fallback / experimental offline engine.
- Stop piling more patches onto the current reader playback WIP for first-audio latency.
- Evaluate a new primary engine that is faster, lighter, legally integrable, and closer to first-audio streaming.
- If using iFlytek, use only official SDKs, official resources, and valid authorization. Do not copy or reuse `libmsc.so` from the installed iFlytek app.

## Recommended next route

Create a new branch for TTS engine prototyping instead of continuing inside the current WIP.

Define a stable reader-facing interface first:

```kotlin
interface BookTtsEngine {
    val name: String
    val isReady: Boolean
    val supportsStreamingFirstAudio: Boolean

    suspend fun initialize(): Result<Unit>
    suspend fun prepareCurrentSentence(request: BookTtsRequest): Result<Unit>
    suspend fun speakStreaming(
        request: BookTtsRequest,
        onFirstAudio: () -> Unit,
        onAudioChunk: suspend (PcmAudioChunk) -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    )
    fun stop()
    fun release()
}
```

The reader should depend on this interface rather than directly depending on Aishell3.

## Future implementation notes

To approach the iFlytek experience, the next architecture should separate these concerns:

- Long-lived engine lifecycle.
- Long-lived PCM writer / `AudioTrack` queue.
- Current sentence preparation.
- First-audio streaming.
- Stop / seek / sentence switching by session ID.
- MediaSession / foreground playback service.

The playback service and MediaSession work should be done separately from the current reader bugfix WIP.

## What not to do

- Do not reverse engineer the iFlytek APK.
- Do not hook or modify iFlytek.
- Do not copy `libmsc.so` or any resources from the installed iFlytek app.
- Do not integrate unlicensed SDK files.
- Do not keep expanding the current Aishell3 WIP as if it can become a true first-packet streaming engine.

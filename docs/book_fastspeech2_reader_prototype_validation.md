# FastSpeech2 Reader Prototype Validation

## Final Conclusion

FastSpeech2 + MB-MelGAN is validated as the strongest current candidate for the LocalVibe novel listening primary engine. Aishell3 should remain as fallback / baseline rather than continuing to be optimized for instant playback as the main path.

The prototype package used for validation was `com.shenghui.localvibe.fastspeech2proto` in the benchmark worktree only. The main workspace `E:\gitType\LocalVibe` was not touched.

## Validated Capabilities

The prototype has passed the following real-device checks:

- First playback and warm playback.
- MB-MelGAN dynamic output buffer handling.
- Seek finish resume.
- Repeated sentence taps without tail-audio overlap.
- Persistent novel-home mini reader console.
- Recent listening record persistence.
- Direct playback from the home mini console.
- Mini play button does not navigate into the reader page.
- Mini info/body area opens the reader page.
- Mini previous chapter and next chapter.
- Reader-internal previous chapter and next chapter.
- Active session controls prevent `engine=NONE` / non-active reader instances from overriding playback controls.
- Reader restore sync from active mini state.
- Audio, chapter title, and purple highlight stay synchronized.
- Pause, resume, and stop behavior.
- No voice package / system TTS settings jump during FastSpeech2 playback.
- No MB-MelGAN buffer copy errors in final verification.
- No prototype ANR / FATAL / crash in final verification.

## Key Performance Data

Measured results across the upstream demo, isolated prototype, and LocalVibe reader prototype:

- ChineseTtsTflite FastSpeech2 demo first audio: about `399 ms - 489 ms`.
- Isolated LocalVibe prototype warm first PCM: about `80 ms - 120 ms`.
- Reader prototype first PCM: repeatedly below `500 ms` in verified paths.
- Representative delegated chapter-switch first PCM values: `388 ms`, `436 ms`, `568 ms`, `641 ms`.
- AudioTrack writes remained off the main thread: `AudioTrack write main=false`.
- FastSpeech2 + MB-MelGAN model size: about `22.72 MiB`.

## Downgraded / Rejected Options

- Kokoro int8: first audio was about `3.3 - 4.2 s`; extracted model size was about `205 MiB`. It is not recommended for the main reader engine.
- Aishell3: keep as fallback / experimental baseline. Its synchronous `OfflineTts.generate()` path makes it a poor fit for the primary instant-play listening experience.

## Final Verification Highlights

Final reader delegate-active-controls verification showed:

- Reader-internal next chapter delegated to active controls.
- Reader-internal previous chapter delegated to active controls.
- Local `reader next chapter local controls` and `reader previous chapter local controls` were not used in the active-session case.
- Old sessions were stopped/released or ignored after switching.
- Reader restore followed the active mini state after switching.
- No stale old-chapter session covered the new chapter UI.
- `chapterIndex=-1`: 0.
- `bufferCapacity=4`: 0.
- `Cannot copy from a TensorFlowLite tensor`: 0.
- Voice package/settings jump: 0.
- Prototype ANR/FATAL/crash: 0.

## Remaining Formalization Risks

Before production integration, these risks must be addressed:

- Apache-2.0 source attribution and license documentation for ChineseTtsTflite-derived implementation.
- APK size and asset packaging impact.
- Model and AAR files must remain ignored and must not be accidentally committed.
- Prototype logs, temporary feature flags, and recovery patches must be cleaned before formal merge.
- Decide separately whether to add Foreground Service, MediaSession, notification controls, and lock-screen controls.
- Long-duration continuous playback still needs a formal regression pass.
- Production code must not depend on ignored staging paths or prototype-only file locations.

## Preservation

The final validated prototype diff was saved as:

`localvibe_fastspeech2_reader_prototype_validated_final.patch`

# Book FastSpeech2 Formal Integration Status

## 1. Background

- Aishell3 / Sherpa paths had first-audio and stability limits for the reader main-engine goal.
- Kokoro int8 was benchmarked but did not fit the main-engine target because of size and first-audio latency.
- FastSpeech2 + MB-MelGAN is the current formal direction for LocalVibe novel listen mode.

## 2. Completed Phases

- Phase 1: engine boundary.
- Phase 2: playback controller.
- Phase 3A-3E: reader skeleton, dry-run wiring, and debug isolation.
- Phase 4A: assets contract and AudioSink boundary.
- Phase 4B: smoke activity.
- Phase 4C: controller + real engine smoke.
- Phase 4D: reader play/pause, text source, and resume guard.
- Phase 4E: auto advance.
- Phase 4F: mapper and audio quality diagnosis.
- Phase 4G: real BookListenScreen auto advance, resume, and restore gate.
- Phase 4H: instant entry, viewport snapshot, and TTS prewarm.

## 3. Current Passing Items

- Formal debug package is independent: `com.shenghui.localvibe.fastspeech2formal`.
- Main app package is not overwritten: `com.shenghui.localvibe`.
- FastSpeech2 produces audible and clear speech after mapper fix.
- Auto advance for 3-5 sentences works.
- Highlight follows playback.
- Pause works.
- Resume works.
- TTS prewarm improves first play.
- Viewport snapshot gives near-instant second entry.
- No system TTS / voice package settings jump.
- No `bufferCapacity=4` regression.
- No TensorFlowLite buffer copy error.
- No formal FATAL/crash in the validated logs.

## 4. Current Known Limits

- First entry after old cache may still show the restore placeholder because viewport snapshot metadata does not exist yet.
- Full real content restore still takes about 2.6-2.9s, but after viewport cache exists it is hidden behind the snapshot window.
- Models/assets are ignored and must not be committed accidentally.
- Debug-only smoke and diagnosis activities must remain debug-only.
- Mini/recent/mode/previous-next/seek are not fully formalized unless separately completed and validated.

## 5. Before Commit

- Do not commit model files under `app/src/main/assets/fastspeech2/`.
- Do not commit local AARs under `app/libs/tflite-local/`.
- Do not commit `test-results/` logs.
- Review patch files before deciding whether any docs-only archive patch should be kept.
- Treat `localvibe_fastspeech2_formal_phase4h1_before_true_instant_entry_fix.utf8.patch` as a temporary conversion artifact unless explicitly retained.

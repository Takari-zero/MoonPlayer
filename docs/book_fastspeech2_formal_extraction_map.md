# FastSpeech2 Formal Extraction Map

## Purpose

This document freezes the prototype extraction map for moving the validated FastSpeech2 reader work into a future clean formal integration branch.

Do not directly submit the current dirty benchmark worktree. The final validated prototype patch is a reference artifact, not a production-ready patch to apply wholesale.

Reference artifacts:

- Final validated patch: `localvibe_fastspeech2_reader_prototype_validated_final.patch`
- Prototype validation report: `docs/book_fastspeech2_reader_prototype_validation.md`
- Formal integration plan: `docs/book_fastspeech2_reader_formal_integration_plan.md`

## Production Candidate Code To Extract

### FastSpeech2 Engine Related

Candidate files:

- `app/src/main/java/com/shenghui/localvibe/core/tts/fastspeech2/FastSpeech2BookTtsEngine.kt`
- `app/src/main/java/com/shenghui/localvibe/core/tts/fastspeech2/README.md`

Candidate logic:

- FastSpeech2 model loading.
- MB-MelGAN vocoder invocation.
- Chinese text preprocessing bridge.
- Dynamic output buffer handling for MB-MelGAN.
- Clause splitting and PCM generation.
- Explicit failure logs for missing model/assets/inference errors.
- `main=false` inference guarantees.

Production cleanup before extraction:

- Convert prototype logging volume into targeted diagnostics.
- Ensure model paths are production-owned and not tied to ignored staging directories.
- Keep Aishell3 fallback as a separate engine path.

### MB-MelGAN / FastSpeech2 / ZhProcessor Source

Candidate source root:

- `app/src/main/java/com/benjaminwan/`

Expected contents to review and selectively migrate:

- FastSpeech2 TFLite wrapper.
- MB-MelGAN TFLite wrapper.
- Chinese processor / mapper logic, including `baker_mapper.json` usage.
- Any minimal utility classes needed by the wrappers.

Production cleanup before extraction:

- Remove Tacotron-only or demo-only paths if present.
- Keep only the FastSpeech2 + MB-MelGAN path.
- Add source attribution and license notes in the production package or README.

### Reader Mini State / Active Session / Controls

Candidate files:

- `app/src/main/java/com/shenghui/localvibe/feature/book/BookReaderPlaybackMiniState.kt`

Candidate logic:

- `BookReaderPlaybackMiniState`.
- `BookReaderPlaybackMiniControls`.
- Chapter snapshot state.
- Active controls owner id.
- Active playback session id.
- `hasActiveFastSpeech2Session`.
- Mini mode state labels and enum-name persistence bridge.

Production cleanup before extraction:

- Move shared state into a production controller-owned model if `BookReaderPlaybackController` is introduced.
- Keep a single active session source of truth.

### BookListenScreen Formal Logic

Candidate file:

- `app/src/main/java/com/shenghui/localvibe/feature/book/BookListenScreen.kt`

Candidate logic to extract selectively:

- FastSpeech2 playback request path.
- Explicit `chapterIndex`, `paragraphIndex`, `sentenceIndex`, and `clauseIndex` propagation.
- Seek start / seek finish resume behavior.
- Sentence tap stale-session protections.
- No-tail stop/switch behavior.
- Active mini restore priority over persisted reader state while an active session exists.
- Reader title/highlight/scroll sync from active mini state.
- Reader internal previous/next chapter delegating to active mini controls when the visible reader is not the active owner.
- Sequential / chapter loop / stop-after-chapter mode semantics.
- System TTS gate suppression for FastSpeech2 path.
- Aishell3 fallback hooks, but not as the primary engine.

Production cleanup before extraction:

- Do not move prototype code as one large block.
- Extract playback/session logic into `BookReaderPlaybackController` first.
- Reduce direct mutable state coupling in the Composable.
- Remove temporary recovery logs and prototype-only branches.

### MainActivity Mini Console Logic

Candidate file:

- `app/src/main/java/com/shenghui/localvibe/MainActivity.kt`

Candidate logic to extract selectively:

- Persistent novel-home mini console state.
- Mini play button that starts/resumes playback without opening reader.
- Mini info/body area opening the reader.
- Active owner filtering so `engine=NONE` controls do not replace active FastSpeech2 controls.
- Mini previous/next chapter controls.
- Mini mode toggle.
- Recent record restore into mini idle/ready state.
- Background active reader host only if still required after introducing a formal controller.

Production cleanup before extraction:

- Prefer moving mini playback orchestration out of `MainActivity` into controller/state holders.
- Keep UI style aligned with the existing music mini player.
- Avoid introducing a second source of playback truth.

### Recent Record / DataStore Logic

Candidate files:

- `app/src/main/java/com/shenghui/localvibe/core/datastore/AppStateStore.kt`
- `app/src/main/java/com/shenghui/localvibe/core/datastore/PersistedBookListenRecentRecord.kt`

Candidate logic:

- Persist recent book URI/title.
- Persist chapter index/title.
- Persist paragraph index, sentence index, and clause index.
- Persist current sentence preview and listen mode.
- Restore recent record into the mini console without requiring reader page entry.

Production cleanup before extraction:

- Confirm schema naming and backward compatibility.
- Add migration/default handling if production persisted state already exists.

### Gradle / Dependency Related Changes

Candidate files:

- `app/build.gradle.kts`
- `settings.gradle.kts`
- `.gitignore`

Candidate logic:

- TensorFlow Lite dependencies or local AAR wiring required by FastSpeech2 and MB-MelGAN.
- `.gitignore` entries for model/AAR staging and generated benchmark files.

Production cleanup before extraction:

- Prefer Maven dependencies for TensorFlow Lite if feasible.
- If local AARs are required, decide whether they are checked in, stored through an internal artifact repository, or handled separately.
- Do not accidentally commit large models or local staging assets.

## Prototype / Benchmark Content Not To Migrate Directly

Do not directly migrate these paths or file classes into the formal branch:

- `ttsBenchmarkApp/`
- `test-results/`
- `localvibe_*.txt`
- `iflytek_*.txt`
- `iflytek_*.apk`
- `*.pid`
- `*.err.txt`
- `*.patch` temporary backup files.
- `BookListenScreen.kt.corrupt-after-buffer-fix.bak`
- `BookListenScreen.bad-after-mini-play-mode-fix.bak`
- `BookListenScreen.corrupt-before-reapply-20260630_220149.bak`
- `BookListenScreen.kt.broken-by-seek-edit.bak`
- `MainActivity.bad-after-mini-play-mode-fix.bak`
- `MainActivity.corrupt-before-reapply-20260630_220149.bak`
- Ignored staging model directories such as `tts-benchmark-models/`.
- Excessive benchmark-only logs with tag `LV_FASTSPEECH2_READER`.
- Debug constants used only to force the prototype route.
- Any code that depends on ignored local paths or one-off staging layout.

## Third-Party Code And License

The FastSpeech2 path is based on ChineseTtsTflite / related TensorFlowTTS-style FastSpeech2 + MB-MelGAN implementation.

Known license posture:

- Source route: ChineseTtsTflite / FastSpeech2 TFLite Android implementation.
- License: Apache-2.0.
- Production integration must keep attribution.

Required production license handling:

- Preserve source headers where present.
- Add a README or NOTICE entry in the FastSpeech2 package explaining the source repo and Apache-2.0 license.
- Include third-party license text or link in the app's third-party notices if the project has a license screen or bundled notices.
- Clearly distinguish LocalVibe-owned wrapper/controller code from ported third-party inference/preprocessing code.

## Model And Dependency Strategy

Measured model size:

- FastSpeech2 + MB-MelGAN model files: about `22.72 MiB`.

Current prototype status:

- Model and AAR files are in ignored/local staging and must not be accidentally committed.
- Prototype build wiring is not a final production packaging decision.

Formal decisions required:

- Whether model files live under `app/src/main/assets`.
- Whether model files should be tracked in Git.
- Whether model files require Git LFS or an internal artifact download process.
- Whether TensorFlow Lite AARs should be Maven dependencies instead of local `libs/` AARs.
- Whether local AARs are license-compatible and redistributable in the app.
- APK size impact and store/distribution constraints.
- Whether model variants or quantization choices should be configurable.

## Recommended Formal Migration Order

1. Create a clean formal worktree from the intended production base.
2. Extract only FastSpeech2 engine + model loader.
3. Introduce `BookReaderPlaybackController` as the single active session owner.
4. Connect the reader page playback path to the controller.
5. Connect the novel-home persistent mini console.
6. Connect recent record persistence and restore.
7. Add previous/next chapter and listen mode controls.
8. Keep Aishell3 fallback behind the common engine interface.
9. Run full real-device regression before any merge.

## Risks If The Prototype Is Copied Directly

Do not directly apply the full final patch to the formal branch because:

- The benchmark worktree is dirty WIP.
- It contains temporary patches, backup files, and experimental scaffolding.
- It contains benchmark-only logging and prototype gate logic.
- It may include local staging assumptions for models/AARs.
- It mixes UI, playback, persistence, and engine work in large files.
- It risks creating two playback state sources instead of one.
- It risks allowing non-active `engine=NONE` reader instances to overwrite active playback controls if the active owner pattern is not ported carefully.

Formal integration must manually select core code and rebuild the architecture around one active session source of truth.

## Formal Regression Checklist

Required real-device checks before production merge:

- Cold first playback.
- Play after 0.5 seconds.
- Play after 8 seconds.
- Pause and resume.
- Seek / slider finish resume.
- Repeated sentence taps.
- Persistent mini console on novel home.
- Clear-background / restart recent restore.
- Mini direct playback from recent record.
- Mini previous chapter and next chapter.
- Reader previous chapter and next chapter.
- Sequential mode.
- Chapter loop mode.
- Stop-after-chapter mode.
- No voice package / system TTS settings jump.
- No tail-audio overlap.
- No MB-MelGAN buffer error.
- No TensorFlowLite tensor copy error.
- No `chapterIndex=-1` in normal playback paths.
- No app ANR / FATAL / crash.

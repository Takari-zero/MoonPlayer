# FastSpeech2 Formal Integration Phase 1 Tasks

## Scope

Phase 1 prepares a clean production engine boundary only. It must not connect FastSpeech2 to the novel reader UI, the home mini console, or any existing playback page behavior.

## Phase 1 Allowed Work

1. Introduce the FastSpeech2 engine source skeleton.
2. Organize Apache-2.0 attribution for the ChineseTtsTflite / TensorFlowTTS-derived code.
3. Decide and document the model and AAR placement strategy.
4. Define a clean engine boundary that can compile independently from reader UI wiring.
5. Keep Aishell3 available as the fallback engine path.
6. Keep model and AAR files out of Git unless the user explicitly confirms a packaging strategy.
7. Ensure production code does not depend on ignored benchmark staging paths.
8. Keep changes small and reviewable.

## Phase 1 Explicit Non-Goals

- Do not connect `BookListenScreen`.
- Do not connect the novel-home mini console.
- Do not change the reader page UI.
- Do not change music or video modules.
- Do not introduce ignored staging path dependencies.
- Do not submit model files or AAR files unless the user explicitly confirms.
- Do not add Foreground Service, MediaSession, notification controls, or lock-screen controls.

## Phase 1 Forbidden Shortcuts

- Do not directly apply `localvibe_fastspeech2_reader_prototype_validated_final.patch`.
- Do not copy the entire prototype `BookListenScreen.kt`.
- Do not copy the entire prototype `MainActivity.kt`.
- Do not commit `ttsBenchmarkApp/`.
- Do not commit benchmark logs, pid files, err files, temporary patches, or corrupt backup files.
- Do not commit model/AAR files from `tts-benchmark-models/` or any ignored staging directory.
- Do not change the novel reader UI or mini state machine in Phase 1.

## Candidate Phase 1 Deliverables

- A production package for FastSpeech2 engine code.
- A minimal `BookTtsEngine` boundary or equivalent interface proposal.
- License/NOTICE documentation for third-party source and model usage.
- A model/AAR packaging decision note.
- A buildable project without reader UI integration.

## Phase 1 Verification

Before Phase 1 is considered complete:

- Project builds successfully.
- No model/AAR files are staged accidentally.
- No benchmark-only directories are staged.
- FastSpeech2 code compiles behind a clean boundary.
- Existing reader, music, and video behavior is not changed.
- Git diff is limited to engine boundary, license/docs, and explicitly approved dependency scaffolding.

## Phase 1 Progress

Completed in the formal worktree:

- Added the production FastSpeech2 engine boundary under `app/src/main/java/com/shenghui/localvibe/core/tts/fastspeech2/`.
- Added `FastSpeech2BookTtsEngine`, model path definitions, synthesis result models, and the verified clause splitter.
- Added the required ChineseTtsTflite-derived FastSpeech2, MB-MelGAN, base inference, and Chinese text preprocessing source under `app/src/main/java/com/benjaminwan/chinesettstflite/`.
- Preserved the dynamic MB-MelGAN output buffer retry strategy so Phase 1 does not regress into fixed `bufferCapacity=4` behavior.
- Added `docs/book_fastspeech2_third_party_notice.md` and `FastSpeech2SourceNotice.md` as Phase 1 attribution placeholders.
- Added Maven dependencies for TensorFlow Lite, select TF ops, TensorFlow Lite support, and pinyin4j.
- Added `.gitignore` protection for future local model/AAR staging paths: `app/src/main/assets/fastspeech2/` and `app/libs/tflite-local/`.

Not done in Phase 1:

- No `BookListenScreen` wiring.
- No `MainActivity` or mini console wiring.
- No AudioTrack playback wiring.
- No Aishell3 fallback wiring.
- No model files or local AAR files copied into the repository.

Build result:

- `:app:assembleDebug` passed with `ANDROID_HOME=E:\Android`.
- The first build attempt without `ANDROID_HOME` failed because the clean worktree has no `local.properties`; no project file was added for that local SDK path.
- TensorFlow Lite Maven dependencies resolved from Gradle/Maven cache.

Remaining risks:

- The Apache-2.0 attribution and NOTICE text still need formal review before production merge.
- Model packaging is still undecided: assets vs. release artifact strategy vs. Git LFS.
- APK size impact is not measured in the formal branch yet because Phase 1 does not include model files.
- The engine boundary is compile-verified only; reader integration, playback controller, mini console, recent record, and long-running playback remain Phase 2+ work.

## Next Phase Gate

Only after Phase 1 is reviewed and accepted should Phase 2 introduce `BookReaderPlaybackController` and begin wiring reader playback behavior.

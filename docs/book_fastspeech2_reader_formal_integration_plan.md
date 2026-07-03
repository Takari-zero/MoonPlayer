# FastSpeech2 Reader Formal Integration Plan

## Formal Goal

- Use FastSpeech2 as the primary novel listening TTS engine.
- Keep Aishell3 as fallback / experimental engine.
- Do not let system TTS availability block FastSpeech2 playback.
- Keep the novel-home mini reader console persistent.
- Restore recent listening records from persisted state.
- Make reader and mini controls operate on one shared active playback session.

## Formal Architecture

Recommended production components:

```text
BookTtsEngine
FastSpeech2BookTtsEngine
BookReaderPlaybackController
BookReaderMiniState
BookReaderRecentStore
BookReaderAudioTrackPlayer
```

Responsibilities:

- `BookTtsEngine`: common TTS abstraction for prepare/speak/stop/release/capability queries.
- `FastSpeech2BookTtsEngine`: model loading, text preprocessing, FastSpeech2 inference, MB-MelGAN inference, and PCM generation.
- `BookReaderPlaybackController`: single active session owner, chapter/sentence targeting, mode handling, pause/resume/stop, stale-session rejection.
- `BookReaderMiniState`: stable UI state for home mini console and reader restore.
- `BookReaderRecentStore`: persistence for recent book URI, chapter, paragraph, sentence, clause, mode, and preview text.
- `BookReaderAudioTrackPlayer`: single output path for PCM writing, stop/release, and no-tail switch behavior.

## State Principles

- Use one active session source of truth.
- Mini and reader must control the same active session.
- `engine=NONE` / non-active reader instances must never override active playback controls.
- `chapterIndex`, `paragraphIndex`, `sentenceIndex`, and `clauseIndex` must be passed explicitly across playback, mini, reader, and persistence.
- Paragraph-to-chapter inference can be a fallback, but must not be the primary control path.
- Playback request reasons must be explicit, e.g. `play`, `sentence_tap`, `seek_finish_resume`, `mini_next_chapter`, `reader_next_chapter`.
- Old session callbacks must be ignored if their session id no longer matches the active session.

## Migration Stages

### Stage 1: Extract FastSpeech2 Engine And Model Loader

- Move the validated FastSpeech2 and MB-MelGAN code into production-owned packages.
- Keep Apache-2.0 attribution and source notes.
- Define model/AAR packaging strategy.
- Keep model files ignored until packaging is explicitly decided.

### Stage 2: Add Formal BookReaderPlaybackController

- Centralize active session ownership.
- Move stop/resume/seek/chapter/sentence switching into the controller.
- Preserve stale-session checks and `main=false` inference/write guarantees.

### Stage 3: Connect Reader Page

- Reader UI sends requests to the controller.
- Reader title/highlight/scroll restore from explicit active playback state.
- Reader internal previous/next chapter delegates to active controls when another owner is active.

### Stage 4: Connect Novel-Home Mini Console

- Mini console is persistent on the novel home page.
- Mini play/pause/previous/next/mode controls call the shared controller.
- Mini body/info area opens the reader page without stealing playback ownership.

### Stage 5: Add Recent Record Restore

- Persist recent book URI, title, chapter index/title, paragraph index, sentence index, clause index, preview, mode, and timestamp.
- Restore mini idle state when no active session exists.
- Allow mini play to continue from recent position without opening reader.

### Stage 6: Keep Aishell3 Fallback

- Aishell3 remains available as fallback or experimental engine.
- FastSpeech2 should not be blocked by missing or unavailable system TTS.
- Fallback must be explicit and logged; it must not silently show system voice settings.

### Stage 7: Formal Regression Test

Run the complete reader regression suite on a real device before merging.

## Formal Test Checklist

- Cold first playback.
- Play after 0.5 seconds on reader entry.
- Play after 8 seconds on reader entry.
- Pause and resume.
- Seek / slider finish resume.
- Repeated sentence taps.
- Persistent novel-home mini console.
- Clear-background / restart recent restore.
- Mini direct playback from recent record.
- Mini previous chapter and next chapter.
- Reader previous chapter and next chapter.
- Modes: sequential, chapter loop, stop after chapter.
- No voice package / system TTS settings jump.
- No tail-audio overlap.
- No MB-MelGAN buffer copy error.
- No `chapterIndex=-1` in normal playback paths.
- No ANR / FATAL / crash for the LocalVibe package.

## Cleanup Strategy Before Formal Integration

Do not directly submit the current dirty prototype worktree as production code. Before formal integration:

- Clean temporary patch files.
- Clean corrupt backup files.
- Clean log files and pid/err files.
- Keep model files and AARs ignored unless an explicit production packaging decision is made.
- Decide separately whether `ttsBenchmarkApp` should remain as a benchmark module or be removed.
- Remove prototype-only logs or downgrade them to targeted diagnostics.
- Remove ignored staging path dependencies.
- Rebuild the formal integration as small, reviewable changes on a clean branch.

## Recommended Next Step

Create a clean formal integration branch from the intended production base. Port only the validated architecture and required implementation pieces from the prototype patch, keeping benchmark artifacts and temporary recovery files out of the production diff.

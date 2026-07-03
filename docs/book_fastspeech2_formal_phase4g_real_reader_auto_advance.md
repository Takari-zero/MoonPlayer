# Phase 4G Real Reader Auto Advance

## Scope

Phase 4G focuses only on the real `BookListenScreen` state path:

- keep the Phase 4F mapper/audio-quality result closed
- do not change `ZhProcessor`, FastSpeech2, MB-MelGAN, or `BookReaderAudioTrackSink`
- fix real reader auto advance after `PlaybackCompleted`
- add diagnostics for delayed restore/scroll to the previous reading position

## Starting Observation

User validation confirmed:

- `after_mapper_formal_baseline_01.wav` is clear
- `after_mapper_reference_baseline_01.wav` is clear
- the real reader can play one sentence
- the real reader did not continue after the first sentence
- entering the reader may show an old/default position briefly before jumping to the previous reading position

## Root Cause

The debug smoke test owns a stable event loop and plays the next target directly after each
`PlaybackCompleted` event.

The real `BookListenScreen` collector lived inside Compose and was using state/function values captured
when the `LaunchedEffect` started. That made the collector vulnerable to stale values such as:

- old `isPlaying`
- old `activePlaybackEngineName`
- old `paragraphs` / `chapters`
- an old next-target builder closure

As a result, the real reader could synthesize and play the first sentence, but the completion event could
be treated as stopped/paused or fail to resolve the next target.

## Fix

Added `BookReaderAutoAdvanceCoordinator` as a small pure Kotlin state holder for the auto-advance gate.

The real reader now:

- keeps a stable `BookReaderPlaybackController`
- starts one controller-keyed collector
- logs collector start/dispose and controller identity
- uses `rememberUpdatedState` values for latest reader state
- resolves next targets from the latest `bookFile`, `chapters`, and `paragraphs`
- sets auto advance enabled on formal play/resume and playing seek/chapter jump
- disables auto advance on pause/stop
- updates highlight/progress before requesting the next session

Added logs:

- `real reader screen entered`
- `real reader controller identity`
- `auto advance collector started`
- `auto advance event received`
- `auto advance state`
- `auto advance next target`
- `auto advance play next session request`
- `auto advance skipped reason=...`
- `reader highlight from auto advance`

## Restore Position Diagnostics

Added minimal restore diagnostics:

- `restore position start`
- `restore position content ready`
- `restore position scroll start`
- `restore position scroll done`

The reader now tracks whether the initial restored position has settled. This gives the final real-page
log enough evidence to distinguish slow file loading from late scroll positioning.

## Tests

Added `BookReaderAutoAdvanceCoordinatorTest` covering:

- completed event 1 -> next target 1
- completed event 2 -> next target 2
- completed event 3 -> next target 3
- pause disables auto advance
- play/resume enables auto advance again
- stale target ignored
- chapter end stops when no next target exists

## Verification

- `:app:testDebugUnitTest`: passed
- `:app:assembleDebug`: passed
- debug APK installed successfully
- main app package remained installed and was not overwritten

Auto smoke regression:

- `complete=true count=5`: passed
- sentence completed count: 5
- pause gate: passed
- resume gate: passed
- `write timeout`: 0
- `bufferCapacity=4`: 0
- `Cannot copy from a TensorFlowLite tensor`: 0
- voice package / system TTS jump: 0
- formal package ANR/FATAL/crash: 0

## Final Manual Test

Use `Moon播放器 Formal`.

1. Enter the novel reader.
2. Confirm the reader does not visibly sit at the wrong/default position before restoring.
3. Tap a body sentence.
4. Tap play.
5. Confirm the first sentence plays.
6. Wait for the next 3-5 sentences.
7. Confirm playback continues without manual input.
8. Confirm title/highlight follow the spoken sentence.
9. Pause and confirm auto advance stops.
10. Resume/play and confirm auto advance continues.
11. Confirm no voice-package/settings page appears.
12. Confirm no crash, black screen, or freeze.

Final real-reader log is started at:

`test-results/formal_reader_phase4g_real_reader_final_log.txt`

PID file:

`test-results/formal_reader_phase4g_real_reader_final_log.pid`

## Phase 4G.1 Resume And Restore Hardening

### Real Log Analysis

The previous real-reader log confirmed that the real page had already reached the formal FastSpeech2
controller path and auto-advanced multiple sentences:

- `auto advance next target`: 6
- `auto advance play next session request`: 6
- `reader highlight from auto advance`: 6
- `playback completed`: 6
- `auto advance skipped`: 0

The remaining resume failure happened after a pause during an active `AudioTrackSink.playPcm` write:

- `AudioTrackSink write timeout sessionId=9`
- `AudioTrackSink release sessionId=9`
- `playback complete ignored reason=paused sessionId=9`
- next play button chose `action=resume canResume=true targetMatches=true`

Root cause: after a paused session returned from `playPcm`, the sink had already released that audio
session, but the controller still exposed `canResume=true`. The play button then performed a no-audio
resume instead of replaying the current target.

### Fix

- `BookReaderPlaybackController` now clears `canResume=false` when `playPcm` returns while the session
  is paused.
- `BookListenScreen` now falls back to `play(target)` when the page is paused but the controller reports
  no resumable audio.
- Added `BookReaderRestorePositionGate` so restored reader content stays hidden until the initial scroll
  has settled, preventing the old/default position from flashing before the saved position is restored.

### Added Tests

- `pausedSessionPlaybackCompletionClearsResumeCapability`
- `BookReaderRestorePositionGateTest`

### Verification

- `:app:testDebugUnitTest --no-build-cache --refresh-dependencies`: passed
- `:app:assembleDebug --no-build-cache`: passed
- debug APK installed successfully
- package check confirmed both:
  - `com.shenghui.localvibe`
  - `com.shenghui.localvibe.fastspeech2formal`

Phase 4G.1 auto smoke:

- `complete=true count=5`: passed
- `sentence done index=`: 5
- pause gate: passed
- resume gate: passed
- `write timeout`: 0
- `bufferCapacity=4`: 0
- `Cannot copy from a TensorFlowLite tensor`: 0
- voice package / system TTS jump: 0
- formal package FATAL / AndroidRuntime crash: 0

System log still contains unrelated device-level `ANR` text from other Android services/processes; no
formal package crash was found in the smoke log.

## Phase 4G.2 Restore-Only First Frame Fix

### Status Before This Phase

Phase 4G.1 manual validation passed the playback path:

- auto-read 3-5 sentences after play
- highlight followed audio
- speech was intelligible
- pause stopped auto advance
- resume continued reading
- no voice-package/settings jump
- no black screen, flash exit, or freeze

The remaining issue was limited to reader entry: the page could still show a wrong/default reading
position for a few seconds before jumping to the previous reading position.

### Root Cause

The Phase 4G.1 restore log showed this order:

1. `restore position start`
2. `restore position scroll start index=0`
3. `restore position scroll done`
4. `restore position visible content enabled`
5. `restore position content ready`

So the cached/empty early reader content was able to call `onPositionSettled()` before the real book
content was loaded. That made the gate visible too early, then the real content later jumped to the
saved position.

### Fix

- The restore gate now hides reader content whenever a saved position exists and the initial position
  has not settled, including while content is still loading.
- The cached reader preview is skipped while the restore gate is pending, so it cannot complete restore
  with index `0`.
- The restore state now uses a key made from `bookId + paragraphIndex + sentenceIndex`, so the gate
  resets for a new book or saved target and does not re-run for the same target.
- The final visible reader still uses `scrollToItem`, not `animateScrollToItem`, for the first restore.

Expected restore-only log order:

1. `restore position start ...`
2. `restore position gate hidden reason=pending_restore ...`
3. `restore position content ready ...`
4. `restore position scroll start index=...`
5. `restore position scroll done elapsedMs=...`
6. `restore position visible content enabled`

No saved position expected log:

- `restore position skipped reason=no_saved_position`
- `restore position visible content enabled reason=no_saved_position`

### Verification

- `:app:testDebugUnitTest --no-build-cache`: passed
- `:app:assembleDebug --no-build-cache`: passed
- debug APK installed successfully
- package check confirmed both:
  - `com.shenghui.localvibe`
  - `com.shenghui.localvibe.fastspeech2formal`

Final restore-only manual log:

`test-results/formal_reader_phase4g2_restore_only_final_log.txt`

PID file:

`test-results/formal_reader_phase4g2_restore_only_final_log.pid`

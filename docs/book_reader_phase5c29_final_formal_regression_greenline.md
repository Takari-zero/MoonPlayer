# Phase 5C.29 Final Formal Regression Greenline

## 1. Greenline Baseline

- Commit: `b54b578`
- Commit message: `fix(book): stop reader playback session on pause`
- Branch: `feature/book-phase5a-first-entry-flicker-polish`
- Local tag: `local-greenline-moonplayer-phase5c29-final-formal-regression-b54b578`
- Committed: yes
- Pushed: no

This local commit and tag are the validated starting point for any work after Phase 5C.29. They are local-only until a separate push is explicitly approved.

## 2. Phase 5C Goal

Phase 5C moved expensive reader preparation ahead of book entry:

- The Formal package preloads a `BookReaderEntryReadyState` in the background from the bookshelf path.
- A store hit on book click navigates directly to real, interactive reader content.
- The ready path does not use a restore prompt, stable snapshot, route loading room, or legacy reader loading path.
- Reader text, playback target, highlight, time, and progress share the same committed target.

The main package remains independent from the Formal-only preload and ready-path rollout.

## 3. Delivered Path, Phase 5C.17-5C.29

- Phase 5C.17 connected Formal book clicks to the preloaded ready-state store.
- Phase 5C.18 removed legacy restore/loading behavior from the ready screen path.
- Phase 5C.19 bounded the reader list so full-book sentence data was not exposed to Compose.
- Phase 5C.20 made the initial ready target one-shot and gave manual viewport interaction control after entry.
- Phase 5C.21.1 skipped read-only shadow diagnostics on the ready path.
- Phase 5C.21.2 measured full-book chapter detection and identified it as an entry-time cost.
- Phase 5C.22 changed progress dragging to preview plus one commit on drag end.
- Phase 5C.23.x bounded the ready window, fixed chapter mapping, and synchronized seek UI, playback, and highlight targets.
- Phase 5C.25 isolated progress-drag preview from committed time labels.
- Phase 5C.26 isolated manual text browsing from committed time/progress.
- Phase 5C.27 aligned pause/resume with the latest committed target after repeated seeks.
- Phase 5C.28 invalidated the active playback session on pause and dropped stale PCM.
- Phase 5C.29 completed the final Formal real-device regression.

## 4. Resolved Issues

- Restore prompts and reader-route waiting on the store-hit path.
- Stable snapshot and legacy loading behavior on the ready path.
- Main-thread lockup caused by exposing a full-book reader list.
- Manual text scrolling returning to an initial seed target.
- Progress-slider seek storms during drag.
- UI, playback, and highlight disagreement after progress seek.
- Manual text browsing incorrectly changing committed time/progress.
- Pause/resume returning to an old target after repeated seeks.
- Residual audio after pause from an active or stale playback session.

## 5. Phase 5C.29 Formal Verification

The validated Formal run used a ready-state store hit after bookshelf preload completed.

- `click readyState store hit`: yes
- `click readyState store miss legacy fallback`: no
- Ready screen path selected: yes
- `restorePromptReachable=false`
- `firstFrameEqualsPlayTarget=true`
- Ready reader list size: 133
- Full reader list exposure skipped: yes
- `targetChapterIndex=-1`: no
- `missing_chapter_index`: no
- `build target skipped`: no
- Restore prompt or `stable_ui_snapshot`: no
- Reader text immediately scrollable: yes
- Manual browsing did not rewrite committed time/progress: yes
- Manual browsing returned to the playback highlight: yes
- Progress drag used preview plus one final commit: yes
- Seek storm: no
- Seek target parity: true
- Pause stopped audio and invalidated the playback session: yes
- Stale PCM dropped and stale sessions ignored: yes
- Resume used the committed target: yes
- Audio, highlight, time, and progress remained aligned: yes
- Sentence-click seek: normal
- Auto advance and highlight follow: normal
- Pause/resume: normal
- Formal crash, ANR, signal 9, `APP_SCOUT_HANG`, or `inputdelaytimeout`: none

## 6. Remaining Notes

- The MIUI/Android system volume HUD is tracked separately. It is not part of the Phase 5C reader preload, ready-state, seek, or playback-target path.
- Model assets, local TFLite libraries, device logs, and `test-results` remain local and must not be committed accidentally.
- This greenline has not been pushed.
- Any follow-up phase should start from commit `b54b578` or local tag `local-greenline-moonplayer-phase5c29-final-formal-regression-b54b578`.

## 7. Scope Boundary

This archive contains only MoonPlayer / LocalVibe book-reader context. It does not include or imply changes to music, video, TTS internals, AudioTrack internals, FastSpeech2, MB-MelGAN, mapper, model assets, or any other project.

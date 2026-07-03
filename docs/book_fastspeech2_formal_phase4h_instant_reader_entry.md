# Phase 4H Instant Reader Entry

## Problem

Phase 4G.2 prevented the wrong reader position from flashing, but it did so by showing a restore/loading
placeholder. User validation showed this still felt slow: entering the listen page could wait roughly
5 seconds before showing the previous reading position.

## Log Finding

The Phase 4G.2 restore log showed the gate order was correct, but the visible content still depended on
the full reader content path:

- restore gate hidden immediately
- content ready after roughly 0.6-0.7 seconds
- visible content enabled after roughly 3.1-3.2 seconds

So the remaining bad experience was not the old wrong-position flash; it was that the cached reader
snapshot was no longer allowed to appear while the full content and initial scroll settled.

## Implementation

Phase 4H reuses the existing `BookReadStateCache.cachedVisibleText` as the first-frame snapshot.

- If a saved snapshot exists, the listen page can show the cached nearby sentences immediately.
- The cached snapshot no longer marks restore as settled.
- Full content still loads in the background.
- When full content is ready, the real list uses the saved initial item index and `scrollToItem`, not
  `animateScrollToItem`.
- If no snapshot exists, the restore placeholder remains.

Performance logs added:

- `perf screen_enter`
- `perf recent_snapshot_load`
- `perf first_visible_snapshot`
- `perf full_chapter_load done`
- `perf paragraph_parse done`
- `perf restore_initial_state`
- `perf restore_visible_content`

## TTS Prewarm

The formal FastSpeech2 engine is now prewarmed in the background when the listen page opens.

- It reuses the same formal engine instance as the controller.
- It synthesizes a short warmup sentence and discards the PCM.
- It does not autoplay.
- It does not touch `AudioTrack`, `MBMelGAN`, mapper, or audio quality logic.

Logs:

- `tts prewarm start`
- `tts prewarm done elapsedMs=...`
- `perf tts_prewarm done elapsedMs=...`
- `first play request elapsedSinceScreenEnterMs=...`

## Verification

- `:app:testDebugUnitTest --no-build-cache`: passed
- `:app:assembleDebug --no-build-cache`: passed
- debug APK installed successfully
- package check confirmed both:
  - `com.shenghui.localvibe`
  - `com.shenghui.localvibe.fastspeech2formal`

## Manual Test

Use `Moon播放器 Formal`.

1. Open the listen page for a book with a saved reading position.
2. Confirm the page shows the previous nearby text quickly, not a long restore placeholder.
3. Confirm it does not flash the wrong/default top position.
4. Tap play and confirm first audio starts without obvious cold-start delay.
5. Let it auto-read 3-5 sentences.
6. Confirm highlight follows audio.
7. Pause and resume.
8. Confirm no voice package/settings page appears.
9. Confirm no black screen, crash, or freeze.

Final log:

`test-results/formal_reader_phase4h_instant_entry_final_log.txt`


## Phase 4H.1 True Instant Entry Fix

Phase 4H validation showed the snapshot appeared quickly, but the first reader frame was still ambiguous: the cached snapshot path reused the same reader content component and logged an initial list index of `0`, while the real content later switched to the saved position. Phase 4H.1 separates the first-frame render decision into explicit modes.

Render modes:

- `snapshot`: saved position exists and cached nearby text is available while full content loads.
- `placeholder`: saved position exists but no cached snapshot is available; default real content is blocked until restore can settle.
- `real_content`: normal reader content after loading, using the saved paragraph/sentence target.

Added logs:

- `instant entry savedPosition exists=...`
- `instant entry snapshot available=...`
- `instant entry render mode=snapshot`
- `instant entry render mode=placeholder reason=no_snapshot_pending_restore`
- `instant entry blocked default content reason=pending_restore`
- `instant entry render mode=real_content initialIndex=... offset=0`
- `instant entry LazyListState created initialIndex=... offset=0 restoreKey=...`

Implementation notes:

- `BookReaderRestorePositionGate.initialRenderMode(...)` now owns the render-mode decision.
- Snapshot rendering is selected only through the explicit `snapshot` mode.
- No-snapshot saved-position entry uses placeholder mode and blocks default real content.
- Real content logs the initial index and restore key so device logs can prove it does not create default real content at index `0` before restore.
- This phase does not change playback, FastSpeech2, MB-MelGAN, mapper, AudioTrack, auto-advance, pause/resume, seek, mini, recent, music, or video logic.

Verification commands:

- `:app:compileDebugKotlin`: passed with temporary high-memory Gradle/Kotlin daemon args.
- `:app:testDebugUnitTest`: passed with temporary high-memory Gradle/Kotlin daemon args.
- `:app:assembleDebug`: passed.

Device validation still needs one final user pass with:

`test-results/formal_reader_phase4h1_true_instant_entry_final_log.txt`

## Phase 4H.2 Viewport Snapshot Fix

Phase 4H.1 still showed a visible second jump on device: the cached first frame could show the highlighted target quickly, but it was not the same viewport that the real reader later rendered. The root cause was the snapshot shape. It stored `chapterSentences.drop(currentChapterSentenceIndex).take(24)`, so the target sentence became the first cached visible sentence and paragraphs above the target were missing.

Phase 4H.2 changes the snapshot contract from target-neighborhood text to a viewport window:

- Snapshot availability now requires viewport metadata, not only cached text.
- Cached snapshot stores visible text plus explicit `paragraphIndex`, `sentenceIndexInParagraph`, and `chapterSentenceIndex` per visible sentence.
- Cached snapshot stores the real `LazyColumn` `firstVisibleItemIndex` and `firstVisibleItemScrollOffset`.
- First-frame render mode is now logged as `snapshot_window`.
- Older target-only snapshots without viewport metadata are treated as unavailable and use the placeholder path instead of fake instant content.
- Real content receives the cached viewport first index and offset before first display, so the snapshot-to-real handoff starts aligned instead of rendering default/top content first.
- The fallback target-neighborhood snapshot includes a few sentences above the highlighted sentence, preventing the highlighted line from being forced to the top when a true visible window has not been captured yet.

Added/updated logs:

- `instant entry render mode=snapshot_window firstVisible=... offset=... count=...`
- `instant entry viewport snapshot saved reason=target_neighborhood ...`
- `instant entry viewport snapshot saved reason=visible_window ...`
- `instant entry render mode=real_content initialIndex=... offset=...`
- `instant entry LazyListState created initialIndex=... offset=... restoreKey=...`
- `instant entry real content align done index=... offset=... renderMode=...`

Verification:

- `:app:testDebugUnitTest`: passed with high-memory Gradle/Kotlin daemon args.
- `:app:assembleDebug`: passed with high-memory Gradle/Kotlin daemon args.

Device validation target log:

`test-results/formal_reader_phase4h2_viewport_snapshot_final_log.txt`

## Phase 4H.2 Final Validation

Final real-device validation confirmed the viewport snapshot behavior:

- First entry may show placeholder because old cache lacks viewport metadata.
- After first restore, viewport snapshot is saved.
- Second entry hits `snapshot_window`.
- `first_visible_snapshot`: 78-143ms.
- `restore_visible_content`: 2.6-2.9s, but hidden behind the correct viewport snapshot after viewport cache exists.
- No first-frame `real_content initialIndex=0` on second entry.
- Real content aligns to the saved viewport before handoff.
- First play after prewarm is fast; test log showed `synth done success=true costMs=481`.
- No TFLite buffer error.
- No voice package / system TTS settings jump.
- No formal package FATAL/crash.



## Phase 4N Stable Reader UI Snapshot

Phase 4M/4M.1 improved the text viewport handoff, but device feedback showed the progress bar and listened/remaining time could still jump independently from the text. Phase 4M.2 tried hiding progress/time during pending handoff; that direction was rejected because it replaced the jump with an empty UI (`--:--`).

Phase 4N changes the first-frame contract: text, highlight, progress value, listened time, and remaining time must come from one stable reader UI snapshot.

Implementation notes:

- First-frame saved snapshot render mode is now `stable_ui_snapshot`.
- The saved reader state now stores stable progress value/max plus listened/remaining time labels alongside viewport text and highlight identity.
- Pending snapshot-to-real handoff keeps showing saved stable progress/time instead of hiding them.
- If saved progress is unavailable but absolute chapter sentence index and total count exist, progress is derived from absolute chapter coordinates, not the cached window length.
- After real content is aligned, real progress/time replaces the saved values only when the normalized progress delta is small.
- If real progress is far from the saved snapshot, progress/time keep the saved values until playback or user interaction updates the state.
- Normal paths no longer log or render `progress time handoff mode=hidden`, hide the progress bar, or display `--:--` when a saved reader snapshot exists.

Added/updated logs:

- `instant entry render mode=stable_ui_snapshot ... progress=... progressMax=... listened=... remaining=...`
- `stable ui snapshot saved reason=... progress=... progressMax=... listened=... remaining=...`
- `progress time handoff mode=saved_stable ...`
- `progress time handoff mode=derived_absolute ...`
- `progress time handoff mode=real_absolute ... reason=handoff_done ...`
- `progress time handoff keep_saved reason=large_delta ...`
- `progress time handoff stable before=... after=... delta=...`

Verification:

- `:app:testDebugUnitTest`: passed.
- `:app:assembleDebug`: passed.

Device validation target log:

`test-results/formal_phase4n_stable_ui_snapshot_final_log.txt`


## Phase 4N.1 First-Entry Stable Snapshot Backfill

Phase 4N real-device validation confirmed that the second entry is stable, but the first entry after legacy cache can still jump. The log showed the first entry had viewport text and absolute sentence identity, but no stable progress/time fields:

- `instant entry render mode=stable_ui_snapshot ... progress=-1.0 progressMax=-1.0 listened= remaining=`
- The complete progress/time fields were only saved after full content loaded.

Root cause: older viewport snapshots were text-only. They had enough identity to show the right text window, but not enough stable progress/time state for the first frame.

Phase 4N.1 adds first-entry backfill:

- Existing stable snapshots are used as-is and logged as `source=stable_snapshot`.
- Legacy viewport snapshots are backfilled to a stable UI snapshot before first-frame use and logged as `source=legacy_viewport_backfill`.
- Backfill derives progress from absolute `chapterSentenceIndex / totalChapterSentenceCount`, not from cached window length.
- Backfill derives time labels from the cached visible text average sentence length projected onto absolute chapter sentence coordinates when no saved labels exist.
- Hidden progress and `--:--` remain disallowed for normal saved-position paths.
- Saved progress protection is limited to restore/handoff; after the reader is settled, playback/user movement saves real progress again so old saved values do not pollute future snapshots.

Added/updated logs:

- `stable ui snapshot source=stable_snapshot ...`
- `stable ui snapshot backfilled source=legacy_viewport_backfill ...`
- `stable ui snapshot saved reason=visible_window ... progress=... listened=... remaining=...`

Verification:

- `:app:testDebugUnitTest`: passed.
- `:app:assembleDebug`: passed.

Device validation target log:

`test-results/formal_phase4n1_first_entry_snapshot_backfill_final_log.txt`


## Phase 4N.2 Single Canonical Progress/Time Source

Phase 4N.1 improved first-entry backfill, but device evidence showed one remaining jump: the first frame used old saved progress/time (`07:08 / 03:50`) while the real content used the same highlighted sentence's canonical chapter sentence index (`08:27 / 02:31`). The total duration stayed constant, which proved the text viewport was not the root cause. The problem was two progress/time authorities.

Phase 4N.2 changes progress/time display to a single canonical source:

- Progress value is always derived from `chapterSentenceIndex / totalChapterSentenceCount`.
- Saved progress labels are no longer authoritative.
- Saved labels may only provide a cached total duration; listened/remaining labels are recomputed from the canonical position.
- Snapshot and real content both call the same resolver path.
- If saved labels conflict with canonical position, the resolver uses canonical output and logs `progress resolver conflict ... action=use_canonical`.
- Saved snapshot protection is no longer allowed to force a different progress value during real-content handoff.

Added/updated logs:

- `progress resolver source=canonical_restore ...`
- `progress resolver source=real_content ...`
- `progress resolver conflict saved=... real=... action=use_canonical`

Verification:

- `:app:testDebugUnitTest`: passed.
- `:app:assembleDebug`: passed.

Device validation target log:

`test-results/formal_phase4n2_single_progress_source_final_log.txt`

# Phase 4E Formal Reader Auto Advance

## Scope

Phase 4E only connects formal reader auto advance inside the current chapter:

- Current sentence completed -> next sentence target -> update reader highlight state -> play next sentence.
- Chapter end stops playback for now.
- No next-chapter auto jump.
- No mini/recent/mode integration.
- No previous/next chapter work.
- No seek expansion.
- No MediaSession, notification, or background service.
- No voice quality/model changes.

## Implementation Notes

- `BookReaderPlaybackEvent.PlaybackCompleted` is emitted after the active session finishes writing PCM.
- Phase 4E.1 hardens `BookReaderAudioTrackSink.playPcm()` so the sink logs write completion, waits for playback drain, and returns deterministically to the controller.
- `AudioTrack.write` uses non-blocking writes with short retry delay to avoid hanging inside the final blocking write before the controller can emit completion.
- Phase 4E.2 switches the sink to per-session `AudioTrack` instances and the `ByteBuffer` `WRITE_NON_BLOCKING` overload.
- Each write now logs `write attempt` and `write result`, and the write loop has a no-progress timeout so it cannot spin forever without forward progress.
- Stale sessions do not emit active completion.
- Paused sessions do not emit active completion.
- Stopped sessions do not emit active completion.
- `BookListenScreen` listens for `PlaybackCompleted` only when the formal controller flag is enabled.
- The next target is built with the same real reader sentence resolver used by manual play.
- `chapterIndex`, `paragraphIndex`, and `sentenceIndex` remain explicit.
- `sentenceText` is the synthesis input; `sentencePreview` remains UI/log-only.

## Expected Logs

- `LV_BOOK_PLAYBACK playback completed sessionId=... chapterIndex=... paragraphIndex=... sentenceIndex=...`
- `LV_BOOK_PLAYBACK AudioTrackSink playPcm start sessionId=... bytes=... sampleRate=...`
- `LV_BOOK_PLAYBACK AudioTrackSink write attempt sessionId=... offset=... bytes=... main=false`
- `LV_BOOK_PLAYBACK AudioTrackSink write result sessionId=... written=... offset=...`
- `LV_BOOK_PLAYBACK AudioTrackSink write complete sessionId=... writtenBytes=...`
- `LV_BOOK_PLAYBACK AudioTrackSink write timeout sessionId=... offset=... totalBytes=...`
- `LV_BOOK_PLAYBACK AudioTrackSink wait playback drain done sessionId=...`
- `LV_BOOK_PLAYBACK AudioTrackSink playPcm return sessionId=...`
- `LV_BOOK_FORMAL auto advance next target chapterIndex=... paragraphIndex=... sentenceIndex=...`
- `LV_BOOK_FORMAL reader highlight from auto advance paragraphIndex=... sentenceIndex=...`
- `LV_BOOK_FORMAL auto advance stop reason=end_of_chapter`
- `LV_BOOK_FORMAL auto advance skipped reason=paused`
- `LV_BOOK_FORMAL auto advance skipped reason=stopped`

## Verification Status

- Phase 4E initial real-device log: first sentence completed and auto-advanced once, but the second sentence wrote PCM without a second `PlaybackCompleted`.
- Phase 4E.1 root cause target: the AudioSink completion boundary after PCM writes.
- Phase 4E.1 real-device log: the first auto-advanced sentence completed, but the next session stopped after `AudioTrackSink write start` with no `write complete`; root cause narrowed to `AudioTrack.write` not returning.
- Phase 4E.2 fix: use the `ByteBuffer` `WRITE_NON_BLOCKING` overload, per-session tracks, and a no-progress timeout.
- `:app:testDebugUnitTest` passed after adding controller completion tests.
- `:app:assembleDebug` passed.
- Real-device verification is pending user testing.

## Remaining Risks

- Voice quality/listening clarity remains a separate evaluation item.
- Long continuous playback still needs a later regression pass.
- This phase does not validate mini/recent/mode/chapter controls.

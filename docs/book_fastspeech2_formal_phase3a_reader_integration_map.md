# FastSpeech2 Formal Phase 3A Reader Integration Map

## Scope

Phase 3A is a read-only integration map for wiring the formal `BookReaderPlaybackController` into the reader later. This phase does not modify `BookListenScreen.kt`, `MainActivity.kt`, models, AARs, UI, mini console, or playback code.

## Current BookListenScreen Playback Chain

Current reader playback is self-contained inside `app/src/main/java/com/shenghui/localvibe/feature/book/BookListenScreen.kt`.

Important state:

- `isPlaying`
- `playbackSessionId`
- `activePlaybackEngineName`
- `currentParagraphIndex`
- `currentSentenceIndexInParagraph`
- `currentReadingTargetName`
- `playbackModeName`
- `showVoicePackageSheet`
- `ttsController`
- `aishell3TtsEngine`
- `aishell3Player`
- `isAishell3Paused`

Main chain:

1. UI play button calls `onPlayPause`.
2. `onPlayPause` calls `pauseReading()` when `isPlaying == true`, otherwise `speakCurrentSentence()`.
3. `speakCurrentSentence()` resolves the current text through `currentSpeakText()`.
4. It increments `playbackSessionId`, logs `playbackRequested`, stops existing Aishell3/system TTS playback, and calls `onBeforeSpeak()`.
5. The primary path creates `StreamingPcmAudioPlayer`, then calls `Aishell3SegmentedStreamingTtsEngine.speak(...)`.
6. Aishell3 `onChunk` starts/writes `StreamingPcmAudioPlayer`; stale chunks are dropped by comparing `sessionId == playbackSessionId`.
7. On success, `continuePlaybackAfterCurrentTarget(sessionId)` calls `moveAfterSpokenTarget()`.
8. If a next target exists and `isPlaying` is still true, `requestSpeakCurrent()` calls `speakCurrentSentence()` again.
9. If Aishell3 fails, `fallbackToSystemTts(reason)` may call `BookTtsController.speakSentence(...)` and may show the voice package sheet.

## Existing Stop/Pause/Seek/Jump Paths

- `stopCurrentPlayback(reason, invalidateSession)` invalidates `playbackSessionId`, stops Aishell3, stops system TTS, and clears `activePlaybackEngineName`.
- `pauseReading()` pauses Aishell3 if active; otherwise it invalidates the session, stops Aishell3/system TTS, clears the active engine, and saves progress.
- `stopReading()` stops playback, resets paragraph/sentence to 0, and saves progress.
- `jumpToParagraph(index, autoPlay)` stops current playback, updates paragraph/sentence, saves progress, then optionally calls `speakCurrentSentence()`.
- `jumpToChapter(offset)` stops current playback, moves to chapter title, saves progress, then optionally calls `speakCurrentSentence()`.
- `jumpToSentence(sentence, autoPlay)` stops current playback, updates paragraph/sentence, saves progress, then optionally calls `speakCurrentSentence()`.
- Slider movement currently calls `onSeekSentence`, which calls `jumpToSentence(sentence)`.
- Sentence tap calls `jumpToSentence(sentence)`.

## Current Highlight And Scroll Chain

The reader UI computes the visible chapter section from `currentParagraphIndex`:

- `chapterStartFor(currentParagraphIndex, chapters)`
- `chapterEndExclusiveFor(currentParagraphIndex, paragraphs.size, chapters)`
- `currentChapter = chapters.lastOrNull { it.paragraphIndex <= currentParagraphIndex }`
- `chapterTitle = currentChapter?.title ?: ...`
- `chapterSentences = buildReaderSentences(...)`
- `currentChapterSentenceIndex` is found by matching `currentParagraphIndex` and `currentSentenceIndexInParagraph`.

Highlight:

- A sentence is highlighted when its `paragraphIndex` and `sentenceIndexInParagraph` match current state, or when its list index equals `currentChapterSentenceIndex`.

Scroll:

- `BookListenContent` has a `LaunchedEffect(chapterTitle, currentChapterSentenceIndex, chapterSentences.size, isChapterTitleCurrent)` that scrolls to the current sentence.

Risk:

- `chapterIndex` is not an explicit primary UI state today. Most chapter UI is derived from `paragraphIndex`.

## Current System TTS Gate And Voice Package Triggers

System TTS is still initialized through `BookTtsController` in `DisposableEffect(ttsRetryKey)`.

Voice package/settings can be triggered by:

- `BookTtsController.onError` when active engine is `SYSTEM_TTS` and playback is active.
- `fallbackToSystemTts(reason)` when `isTtsReady == false`.
- `fallbackToSystemTts(reason)` when `ttsController.speakSentence(...)` fails.
- `previewSystemVoice()` when system TTS is not ready or preview fails.
- `TtsStatusStrip` actions through `onOpenVoicePackageSettings`.
- Top bar menu action `"语音设置"`.
- Voice package bottom sheet actions.

Phase 3B must ensure FastSpeech2 playback does not depend on system TTS readiness and does not show `VoicePackageSettingsBottomSheet` for FastSpeech2 failures.

## Current Aishell3 And AudioTrack Path

Files:

- `app/src/main/java/com/shenghui/localvibe/core/tts/Aishell3SegmentedStreamingTtsEngine.kt`
- `app/src/main/java/com/shenghui/localvibe/core/tts/StreamingPcmAudioPlayer.kt`

`Aishell3SegmentedStreamingTtsEngine` provides:

- `initialize()`
- `speak(text, params, onStart, onChunk, onDone, onError)`
- `stop()`
- `release()`

`StreamingPcmAudioPlayer` owns real `AudioTrack` behavior:

- `start(format)`
- `write(chunk)`
- `pause()`
- `resume()`
- `stop()`
- `release()`

Phase 3B should not reuse this player for FastSpeech2 unless a dedicated `BookReaderAudioSink` implementation is introduced intentionally. The existing Phase 2 controller currently uses only the `BookReaderAudioSink` boundary.

## Related Files

Existing:

- `app/src/main/java/com/shenghui/localvibe/MainActivity.kt`
- `app/src/main/java/com/shenghui/localvibe/core/datastore/AppStateStore.kt`
- `app/src/main/java/com/shenghui/localvibe/core/tts/Aishell3SegmentedStreamingTtsEngine.kt`
- `app/src/main/java/com/shenghui/localvibe/core/tts/StreamingPcmAudioPlayer.kt`

Not present in the formal branch as standalone files:

- `PersistedBookListenRecentRecord.kt`
- `BookReaderPlaybackMiniState.kt`

`MainActivity.kt` currently wires `BookListenScreen` at `LocalVibeRoute.BookListen` with:

- `bookFile`
- `initialParagraphIndex`
- `onProgressChanged`
- `onBeforeSpeak`
- `onBack`

`MainActivity.kt` already has a music `MiniAudioPlayerBar`, but Phase 3B should not touch mini behavior.

`AppStateStore.kt` has book progress persistence through `PersistedBookProgress`, but it stores only:

- `uri`
- `paragraphIndex`
- `totalParagraphs`
- `updatedAt`

No formal book listen recent/session record exists yet.

## Old Paths To Replace Later

Highest-risk old paths:

- `speakCurrentSentence()` as the current monolithic playback entry.
- `fallbackToSystemTts(reason)` because it may show voice package settings.
- `BookTtsController` readiness/error path because it can block or confuse FastSpeech2 playback.
- `Aishell3SegmentedStreamingTtsEngine.speak(...)` direct call inside reader.
- `StreamingPcmAudioPlayer` direct ownership inside reader.
- `pauseReading()` because it has Aishell3-specific pause behavior and system TTS fallback behavior.
- `stopCurrentPlayback(...)` because it currently controls multiple engines directly.
- `jumpToSentence(...)`, `jumpToChapter(...)`, and slider `onSeekSentence` because they stop/resume via local state and `speakCurrentSentence()`.
- `moveAfterSpokenTarget()` / `continuePlaybackAfterCurrentTarget()` because stale session and auto-advance behavior must move to the controller.
- `currentChapterIndexFor(paragraphIndex)` as the main route for chapter identity; formal FastSpeech2 should pass explicit `chapterIndex`.
- `LaunchedEffect` cached reader restore path because persisted old state could briefly override active playback state in later phases.
- Top bar `onBack`, which currently stops playback; this will matter when mini/background continuity is introduced in a later phase.

## Phase 3B Minimal Integration Range

Recommended Phase 3B scope:

- Add a local feature flag in `BookListenScreen.kt`, for example:
  `private const val USE_FORMAL_BOOK_PLAYBACK_CONTROLLER = true`
- Instantiate or remember a `BookReaderPlaybackController` inside `BookListenScreen`.
- Add a minimal adapter from `FastSpeech2BookTtsEngine` to Phase 2 `BookTtsEngine`.
- Add a minimal `BookReaderAudioSink` boundary implementation only if needed for compile-time wiring; real playback can remain out of scope if model assets are absent.
- Replace only the play/pause/resume surface for a narrow path.
- Emit basic logs for target creation and highlight synchronization.
- Keep the old path behind the feature flag for quick rollback.

Phase 3B should not include:

- Homepage mini console.
- Recent listen record persistence.
- Previous/next chapter controls.
- Playback mode switching.
- App restart recovery.
- Foreground Service.
- MediaSession.
- Notification controls.
- Lock-screen controls.
- Model/AAR copying.
- Aishell3 fallback implementation changes.

## Feature Flag Proposal

Use a file-local flag first:

```kotlin
private const val USE_FORMAL_BOOK_PLAYBACK_CONTROLLER = true
```

Reasons:

- Fast rollback during reader integration.
- No Gradle/BuildConfig churn in the first reader wiring step.
- No impact on music/video modules.
- No behavior change outside the novel reader route.

Later, after Phase 3B is stable, this can move to BuildConfig or app settings.

## Controller Call Mapping

Future reader calls should map as follows:

- Play current sentence:
  `controller.play(target)`
- Pause:
  `controller.pause()`
- Resume:
  `controller.resume()`
- Stop:
  `controller.stop(reason)`
- Slider seek:
  `controller.seekTo(target, resumeIfPlaying)`
- Sentence tap:
  `controller.jumpToSentence(target, resumeIfPlaying)`
- Chapter jump:
  `controller.jumpToChapter(target, resumeIfPlaying)`

`BookReaderPlaybackTarget` must be built with explicit values:

- `bookId`
- `bookTitle`
- `chapterIndex`
- `chapterTitle`
- `paragraphIndex`
- `sentenceIndex`
- `clauseIndex`
- `sentencePreview`

## UI State Synchronization Principles

Prototype validation showed these rules are non-negotiable:

- Sound, title, purple highlight, and scroll anchor must move together.
- `chapterIndex` must be explicitly passed with every playback target.
- Do not rely on paragraph-to-chapter reverse lookup as the primary path.
- Reader UI must not let old persisted state override active playback state.
- Stale sessions must not auto-advance.
- Old session callbacks must not publish current UI state.
- Seek and sentence tap must invalidate old session before target playback starts.
- If playback is paused before seek, seek updates the target without auto-playing.
- If playback is active before seek, seek resumes from the target.

## Phase 3B Risks

- `BookListenScreen.kt` is a large stateful composable with playback, UI, TTS gate, progress cache, sleep timer, and catalog logic mixed together.
- System TTS errors may still open the voice package sheet unless FastSpeech2 is clearly separated from system TTS readiness.
- Aishell3 fallback must not block or silently replace FastSpeech2 in the first formal reader path.
- Seek resume is easy to regress because slider updates currently call `jumpToSentence(...)` immediately.
- Highlight/scroll can lag if controller target state is not mirrored into `currentParagraphIndex` / `currentSentenceIndexInParagraph`.
- Back/onDispose behavior currently releases/stops reader engines; future mini/background work must change that carefully, but Phase 3B should not.
- Formal branch currently has no FastSpeech2 model assets, so Phase 3B can only promise compile-time wiring unless model packaging is explicitly approved.

## Phase 3B Verification Checklist

Minimum verification after Phase 3B:

- `:app:assembleDebug` passes.
- First entry into reader does not regress.
- First play click uses the formal controller path when the feature flag is enabled.
- Pause works.
- Resume works.
- Continuous sentence taps invalidate old session.
- Seek updates target and resumes only when expected.
- Voice package settings do not open for FastSpeech2 path.
- No TensorFlow Lite buffer error is introduced.
- No ANR / FATAL / crash is reported for app process.
- Old path can be restored quickly by disabling the feature flag.

Real-device audio verification should wait until model asset strategy is approved and the formal audio sink is introduced.

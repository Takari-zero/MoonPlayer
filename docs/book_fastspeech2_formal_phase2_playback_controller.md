# FastSpeech2 Formal Integration Phase 2 Playback Controller

## Scope

Phase 2 adds a production `BookReaderPlaybackController` boundary only. It does not connect the controller to Compose UI, `BookListenScreen`, the novel-home mini console, AudioTrack playback, Aishell3 fallback, or system TTS gate behavior.

## Added Controller Files

- `app/src/main/java/com/shenghui/localvibe/feature/book/playback/BookReaderPlaybackController.kt`
- `app/src/main/java/com/shenghui/localvibe/feature/book/playback/BookReaderPlaybackTarget.kt`
- `app/src/main/java/com/shenghui/localvibe/feature/book/playback/BookReaderPlaybackState.kt`
- `app/src/main/java/com/shenghui/localvibe/feature/book/playback/BookReaderPlaybackSession.kt`
- `app/src/main/java/com/shenghui/localvibe/feature/book/playback/BookReaderPlaybackMode.kt`
- `app/src/main/java/com/shenghui/localvibe/feature/book/playback/BookReaderAudioSink.kt`
- `app/src/main/java/com/shenghui/localvibe/feature/book/playback/BookReaderPlaybackEvents.kt`

## Added Test Coverage

- `app/src/test/java/com/shenghui/localvibe/feature/book/playback/BookReaderPlaybackControllerTest.kt`

Covered behavior:

- `play(target)` creates a new active session and writes synthesized PCM to the injected sink.
- `jumpToChapter(target, resumeIfPlaying = true)` invalidates the old session, stops the sink, and starts the target chapter.
- `STOP_AFTER_CHAPTER` keeps the current target paused instead of looping to the chapter start.

## Boundary Decisions

- `BookReaderPlaybackController` depends on an abstract `BookTtsEngine`.
- `BookReaderAudioSink` is only an interface in Phase 2.
- `NoopBookReaderAudioSink` exists only to keep the boundary buildable without real AudioTrack wiring.
- `BookReaderChapterNavigator` is abstract, so real chapter lists stay outside the controller.
- Logging uses the `LV_BOOK_PLAYBACK` prefix through an injected logger, avoiding direct Android logging in unit tests.
- Every new playback attempt uses a fresh `sessionId`.
- Pause, stop, seek, jump, and release invalidate the active session.
- Stale session completion is ignored through the active session check.

## Explicit Non-Goals

- No `BookListenScreen` integration.
- No `MainActivity` or mini console integration.
- No reader UI changes.
- No AudioTrack implementation.
- No model files or local AAR files.
- No Aishell3 fallback implementation.
- No system TTS gate changes.
- No Foreground Service, MediaSession, notification, or lock-screen controls.

## Verification

- Red test was observed first: `:app:testDebugUnitTest` failed because the controller boundary did not exist.
- Green test passed after adding the controller boundary: `:app:testDebugUnitTest`.
- App build passed: `:app:assembleDebug`.
- Build used temporary `ANDROID_HOME=E:\Android`; no `local.properties` was added.

## Next Phase

Phase 3 should connect the controller to the reader page in a narrow path, still avoiding mini console, recent record, and service work until the reader path is stable.

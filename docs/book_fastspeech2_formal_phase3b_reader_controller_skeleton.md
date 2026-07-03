# FastSpeech2 Formal Phase 3B Reader Controller Skeleton

## Scope

Phase 3B adds a minimal, disabled-by-default `BookListenScreen` skeleton for routing play, pause, resume, and stop through `BookReaderPlaybackController`.

This phase does not change default runtime behavior because the feature flag is `false`.

## Modified Files

- `app/src/main/java/com/shenghui/localvibe/feature/book/BookListenScreen.kt`
- `docs/book_fastspeech2_formal_phase3b_reader_controller_skeleton.md`

## Feature Flag

`BookListenScreen.kt` now contains:

```kotlin
private const val USE_FORMAL_BOOK_PLAYBACK_CONTROLLER = false
```

Rules:

- `false`: existing Aishell3/system TTS reader path remains active.
- `true`: play, pause, resume, and stop can enter the formal controller skeleton.
- The flag is local to `BookListenScreen.kt` so it can be rolled back quickly without affecting music, video, or the home shell.

## Skeleton Wiring

The skeleton adds:

- A remembered `BookReaderPlaybackController`.
- A disabled `BookTtsEngine` adapter that does not load models and does not access assets.
- A `NoopBookReaderAudioSink`.
- A `buildFormalBookReaderPlaybackTarget(...)` helper that logs and returns `null` when required target metadata is missing.
- `LV_BOOK_FORMAL` logs for play, pause, resume, stop, fallback, and target construction.

Skeleton branches were added near:

- `speakCurrentSentence()`
- `pauseReading()`
- `stopCurrentPlayback(...)`

## Target Construction

The formal target includes:

- `bookId`
- `bookTitle`
- `chapterIndex`
- `chapterTitle`
- `paragraphIndex`
- `sentenceIndex`
- `clauseIndex`
- `sentencePreview`

If no chapter index can be resolved, the helper logs:

```text
LV_BOOK_FORMAL build target skipped reason=missing_chapter_index
```

and the reader falls back to the legacy path.

## Not Connected In Phase 3B

- No real FastSpeech2 model loading.
- No model assets.
- No local AAR files.
- No real AudioTrack sink.
- No homepage mini console.
- No recent listen persistence.
- No seek integration.
- No sentence tap integration beyond existing legacy behavior.
- No previous/next chapter integration.
- No playback mode integration.
- No system TTS gate changes.
- No Aishell3 fallback changes.
- No MainActivity changes.

## Verification

Commands run from `E:\gitType\LocalVibe-fastspeech2-formal` with temporary `ANDROID_HOME=E:\Android`:

```powershell
.\gradlew.bat :app:testDebugUnitTest --console=plain
.\gradlew.bat :app:assembleDebug --console=plain
```

Results:

- `:app:testDebugUnitTest` passed.
- `:app:assembleDebug` passed.
- No APK was installed.

## Phase 3C Recommendation

Phase 3C should wire seek and sentence jump into `BookReaderPlaybackController` while keeping the same feature flag and still avoiding:

- homepage mini console
- recent record persistence
- previous/next chapter
- mode switching
- MediaSession
- Foreground Service
- notification controls

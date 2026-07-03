# FastSpeech2 Formal Phase 3C Seek And Sentence Jump Skeleton

## Scope

Phase 3C adds disabled-by-default skeleton wiring for reader seek and sentence tap paths to `BookReaderPlaybackController`.

The feature flag remains:

```kotlin
private const val USE_FORMAL_BOOK_PLAYBACK_CONTROLLER = false
```

Default behavior remains the existing legacy reader path.

## Modified Files

- `app/src/main/java/com/shenghui/localvibe/feature/book/BookListenScreen.kt`
- `docs/book_fastspeech2_formal_phase3c_seek_sentence_jump_skeleton.md`

## Skeleton Wiring

The existing `jumpToSentence(...)` function now accepts a source:

- `source = "seek"` for slider seek
- `source = "sentence_tap"` for text sentence tap

When the feature flag is enabled:

- It builds a `BookReaderPlaybackTarget` with explicit `chapterIndex`, `chapterTitle`, `paragraphIndex`, `sentenceIndex`, `clauseIndex`, and preview text.
- It logs either:
  - `LV_BOOK_FORMAL reader seek via controller ...`
  - `LV_BOOK_FORMAL reader sentence tap via controller ...`
- It calls:
  - `formalPlaybackController.seekTo(target, resumeIfPlaying = autoPlay)` for seek
  - `formalPlaybackController.jumpToSentence(target, resumeIfPlaying = autoPlay)` for sentence tap

If target construction fails, it logs:

```text
LV_BOOK_FORMAL reader jump fallback to legacy reason=missing_target source=...
```

and continues through the legacy implementation.

## Resume Rules

The skeleton preserves the prototype rule through the existing `autoPlay` parameter:

- Playing state seek/tap: `resumeIfPlaying = true`
- Paused state seek/tap: `resumeIfPlaying = false`

Because the feature flag is currently false, runtime behavior remains unchanged.

## Not Connected In Phase 3C

- No homepage mini console.
- No recent listen persistence.
- No previous/next chapter skeleton.
- No playback mode skeleton.
- No real FastSpeech2 model loading.
- No model assets.
- No local AAR files.
- No real AudioTrack sink.
- No system TTS gate changes.
- No Aishell3 fallback changes.
- No MediaSession, Foreground Service, notification, or lock-screen controls.

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

## Phase 3D Recommendation

Phase 3D should add reader previous/next chapter skeleton wiring to `BookReaderPlaybackController`, still keeping the feature flag disabled by default and still avoiding mini console, recent record, real model loading, and real AudioTrack playback.

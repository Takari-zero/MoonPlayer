# FastSpeech2 Formal Phase 3D Reader Chapter Jump Skeleton

## Scope

Phase 3D adds disabled-by-default skeleton wiring for reader previous/next chapter controls to `BookReaderPlaybackController`.

The feature flag remains:

```kotlin
private const val USE_FORMAL_BOOK_PLAYBACK_CONTROLLER = false
```

Default behavior remains the existing legacy reader previous/next chapter path.

## Modified Files

- `app/src/main/java/com/shenghui/localvibe/feature/book/BookListenScreen.kt`
- `docs/book_fastspeech2_formal_phase3d_reader_chapter_jump_skeleton.md`

## Skeleton Wiring

The existing `jumpToChapter(offset)` function now has a formal-controller branch guarded by the feature flag.

When enabled:

- `offset > 0` is logged as `reader_next_chapter`.
- `offset < 0` is logged as `reader_previous_chapter`.
- Invalid previous/next chapter requests are safe no-ops.
- A target chapter first readable sentence is resolved through `buildFormalBookReaderPlaybackTargetForChapter(...)`.
- The target is passed to `formalPlaybackController.jumpToChapter(target, resumeIfPlaying)`.

Expected logs include:

```text
LV_BOOK_FORMAL reader previous chapter via controller chapterIndex=... paragraphIndex=... sentenceIndex=... resumeIfPlaying=...
LV_BOOK_FORMAL reader next chapter via controller chapterIndex=... paragraphIndex=... sentenceIndex=... resumeIfPlaying=...
LV_BOOK_FORMAL reader chapter jump fallback to legacy reason=missing_target source=reader_next_chapter
LV_BOOK_FORMAL reader chapter jump no-op reason=no_previous_chapter
LV_BOOK_FORMAL reader chapter jump no-op reason=no_next_chapter
```

## Target Construction

`buildFormalBookReaderPlaybackTargetForChapter(...)` resolves:

- `bookId`
- `bookTitle`
- `chapterIndex`
- `chapterTitle`
- `paragraphIndex`
- `sentenceIndex`
- `clauseIndex = 0`
- `sentencePreview`

The helper skips chapter title paragraphs and chooses the first readable sentence in the target chapter. It never emits `chapterIndex = -1`.

If the chapter cannot produce a valid target, it logs a reason such as:

- `missing_chapter_index`
- `empty_book`
- `empty_chapter`

and returns `null`.

## Resume Rules

The skeleton preserves the prototype rule:

- If reader was playing, `resumeIfPlaying = true`.
- If reader was paused or stopped, `resumeIfPlaying = false`.

Because the feature flag is currently false, runtime behavior remains unchanged.

## Not Connected In Phase 3D

- No homepage mini console.
- No recent listen persistence.
- No playback mode wiring.
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

## Phase 3E Recommendation

Phase 3E should either:

- begin formal `BookReaderAudioSink` and model asset strategy, or
- add a dry-run reader UI state log layer that proves controller target state and reader title/highlight state would stay synchronized before enabling real audio.

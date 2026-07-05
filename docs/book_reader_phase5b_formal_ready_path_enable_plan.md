# Book Reader Phase 5B.20 Formal Ready Path Controlled Enable Plan

This document is a plan only. It does not enable the ready path, does not
change Kotlin, does not install an APK, does not start ADB, and does not change
runtime behavior.

## 1. Current State

- The ready state boundary already exists.
- The factory / loader / wiring adapter already exist.
- The restore cache source has already been extracted.
- The route loader path already exists.
- `BookListenScreen` already has the `entryReadyState` parameter.
- `MainActivity` still keeps the ready path disabled:
  - `ENABLE_BOOK_READER_ENTRY_READY_ROUTE_SHADOW = false`
  - `ENABLE_BOOK_READER_READY_PATH = false`
- Current reader runtime behavior remains on the legacy path.

## 2. Formal/debug Only Enablement

The first controlled ready path enablement must be limited to the Formal/debug
package:

```text
com.shenghui.localvibe.fastspeech2formal
```

Rules:

- Only verify in `com.shenghui.localvibe.fastspeech2formal`.
- Do not affect the main app package `com.shenghui.localvibe`.
- The main app must continue using the old path.
- The enablement must be one-switch reversible.
- Release and main app behavior must remain unchanged.

## 3. Recommended Enablement Method

- Use an explicit flag.
- The flag default must remain `false`.
- First enablement may only be a temporary Formal/debug package pass.
- Do not use snapshot, loading UI, overlay, alpha, or placeholder behavior to
  make the reader appear ready.
- Do not affect release or the main app package.
- Keep the switch easy to close so Formal/debug can immediately fall back to
  the old path.

## 4. Required Behavior After Ready Path Is Enabled

When the Formal/debug ready path is explicitly enabled:

1. `BookReaderEntryReadyRoute` prepares `readyState` before entering the reader.
2. The app enters `BookListenScreen` only after the route state is ready.
3. The screen call must pass:

```text
BookListenScreen(entryReadyState = readyState)
```

4. The first frame must show real, interactive reader body text.
5. The reader text must be scrollable immediately.
6. Tapping play must start playback immediately.
7. The screen must not show `正在恢复小说内容`.
8. The visible text, playback target, progress, and highlight must come from
   the same ready-state target.

## 5. Forbidden Items

The controlled ready path enablement must not use:

- snapshot fake正文
- fake snapshot reader content
- loading 遮挡
- loading mask
- entering the page and then doing scroll / align correction
- correcting position when the play button is tapped
- double reader swap
- dual reader instances
- full-screen alpha gate
- stable snapshot as reader body content
- reader-body placeholder as a substitute for readiness

If any forbidden item appears necessary, stop the enablement and fix route-level
readiness first.

## 6. Verification Plan

Automated verification before any manual device check:

```powershell
.\gradlew.bat :app:testDebugUnitTest --no-daemon --max-workers=1 --console=plain -Dorg.gradle.jvmargs="-Xmx6144m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8" -Dkotlin.daemon.jvm.options="-Xmx6144m"
.\gradlew.bat :app:assembleDebug --no-daemon --max-workers=1 --console=plain -Dorg.gradle.jvmargs="-Xmx6144m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8" -Dkotlin.daemon.jvm.options="-Xmx6144m"
git diff --check
```

Package and app-boundary checks:

- Confirm the APK package name is Formal:
  `com.shenghui.localvibe.fastspeech2formal`.
- Confirm the main app package `com.shenghui.localvibe` is not overwritten.
- Confirm the main app remains on the old path.

Manual real-device acceptance, only after explicit approval in a later phase:

- First glance shows the correct real reader body text.
- Text is immediately scrollable.
- Playback does not show `正在恢复小说内容`.
- After tapping play, text does not jump.
- After tapping play, progress does not jump.
- Auto-resume / continue reading works.
- Highlight works.
- Pause / continue works.

This plan does not execute those checks in this round.

## 7. Rollback Strategy

- Close the explicit ready-path flag.
- Return Formal/debug to the old path.
- Do not revert TTS, AudioTrack, playback controller, auto advance, pause /
  continue, model, mapper, music, or video paths.
- Do not affect the main app.
- Do not remove the ready-state boundary, route loader path, factory, loader,
  wiring adapter, or restore cache source.

Rollback must be a flag close, not a playback-stack rollback.

## 8. Stop Conditions

Stop immediately if:

- The ready path affects `com.shenghui.localvibe`.
- The main app is overwritten.
- The ready path enters `BookListenScreen` before `readyState` is ready.
- The first frame is snapshot fake正文.
- The reader is masked by loading UI.
- The page enters and then scrolls / aligns after visible content.
- Playback tap corrects position or progress.
- There is a double reader swap.
- There is a full-screen alpha gate.
- The screen shows `正在恢复小说内容`.

## 9. This Round Boundary

- No Kotlin changes.
- `ENABLE_BOOK_READER_READY_PATH` remains disabled.
- No APK install.
- No ADB startup.
- No commit.
- No push.

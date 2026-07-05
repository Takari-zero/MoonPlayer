# Book Reader Phase 5B.20 Formal Ready Path Enable Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Safely enable the reader ready path for the Formal/debug package first, while keeping the main app and legacy reader path protected.

**Architecture:** The route layer prepares `BookReaderEntryReadyState` before entering `BookListenScreen`. Only when the Formal/debug ready path flag is enabled and route state is `Ready` may `BookListenScreen(entryReadyState = readyState)` be used. Main/release behavior remains on the legacy path until Formal real-device evidence proves the ready path is safe.

**Tech Stack:** Kotlin, Jetpack Compose, JVM unit tests, Android debug package `com.shenghui.localvibe.fastspeech2formal`, existing reader ready-state foundation.

---

## 1. Current State

The reader ready-state foundation is in place:

- `BookReaderEntryReadyState` boundary exists.
- `BookReaderEntryReadyStateFactory` exists.
- `BookReaderEntryReadyStateLoader` exists.
- `BookReaderEntryReadyStateWiringAdapter` exists.
- `BookReaderRestoreCacheSource` has been extracted from the old BookListenScreen-private cache logic.
- `BookReaderEntryReadyRoute` route skeleton exists.
- `BookReaderEntryReadyRouteLoader` can call an injected loader and return `Ready` or `Failed`.
- `BookListenScreen` already accepts `entryReadyState: BookReaderEntryReadyState? = null`.
- `MainActivity` has disabled wiring:
  - `ENABLE_BOOK_READER_ENTRY_READY_ROUTE_SHADOW = false`
  - `ENABLE_BOOK_READER_READY_PATH = false`
  - `BookListenScreen(entryReadyState = null)` by default.

This means runtime behavior is still legacy. Ready path enablement has not happened yet.

## 2. Formal/debug Only Enablement Rule

The first enabled ready-path pass may only target the Formal/debug package:

```text
com.shenghui.localvibe.fastspeech2formal
```

It must not affect the main app package:

```text
com.shenghui.localvibe
```

Rules:

- Main app stays on the old path.
- Formal/debug gets the first controlled ready-path experiment.
- The ready path must be one switch away from disabled.
- If anything fails, close the flag and return to the legacy path.
- Do not change FastSpeech2, MB-MelGAN, mapper, AudioTrack, auto advance, pause/resume, music, video, or model files.

## 3. Recommended Enablement Mechanism

### Flag shape

Use an explicit flag whose default is still false:

```kotlin
private const val ENABLE_BOOK_READER_READY_PATH = false
```

When the controlled Formal/debug pass begins, change only the debug/Formal decision, not the main app package behavior. Prefer one of these approaches:

1. Keep the constant false and add a debug-only package guard helper.
2. Move the default to a debug-only source set later.
3. Add a helper that returns true only for the Formal application id.

The helper must make the package boundary visible:

```kotlin
internal fun shouldEnableBookReaderReadyPathForPackage(
    packageName: String,
    explicitFlag: Boolean,
): Boolean {
    return explicitFlag && packageName == "com.shenghui.localvibe.fastspeech2formal"
}
```

The first implementation should keep `explicitFlag = false`. The Formal trial can temporarily set it true only after tests pass.

### No visual workaround

The Formal enablement must not introduce:

- snapshot fake reader content
- loading / "恢复阅读位置" inside reader body
- overlay / alpha / placeholder
- dual reader swap
- post-entry scroll/align
- target/progress correction on play

The route must prepare real state first. The screen must render real content first.

## 4. Required Runtime Behavior When Enabled

When Formal ready path is enabled:

1. `BookReaderEntryReadyRoute` creates input from the resolved book and route paragraph hint.
2. Route-level source loads real paragraphs.
3. Route-level source reads restore cache.
4. `BookReaderEntryReadyStateLoader` / wiring adapter builds `BookReaderEntryReadyState`.
5. Only after `Ready`, route calls:

```kotlin
BookListenScreen(
    bookFile = resolvedBookFile,
    initialParagraphIndex = initialBookParagraphIndex,
    entryReadyState = readyState,
    ...
)
```

6. `BookListenScreen` ready path uses `entryReadyState` as the first-frame source for:
   - paragraphs
   - chapterSentences
   - canonical paragraph index
   - canonical sentence index
   - canonical chapter sentence index
   - progress/time labels
   - LazyList initial index/offset
   - playback seed

Immediate user-facing requirements:

- First frame is real reader text.
- Text can scroll immediately.
- Play starts immediately.
- No "正在恢复小说内容".
- Text does not refresh or jump after play.
- Progress/time do not jump after play.

## 5. Forbidden Items

The controlled Formal enablement must not use:

- snapshot fake text
- loading or "恢复阅读位置" masking
- post-entry `scrollToItem`, `requestScrollToItem`, `animateScrollToItem`, or align
- play-time target/progress correction
- dual reader instances
- full-screen alpha gate
- reader-body placeholder as a substitute for readiness

If any of these are needed to make the screen look acceptable, stop and fix the route-level readiness instead.

## 6. Implementation Tasks

### Task 1: Add package-guard tests

**Files:**
- Modify or create: `app/src/test/java/com/shenghui/localvibe/feature/book/BookReaderEntryReadyRouteTest.kt`
- Modify: `app/src/main/java/com/shenghui/localvibe/feature/book/BookReaderEntryReadyRoute.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
@Test
fun readyPathFlagCanOnlyEnableFormalPackage() {
    assertTrue(
        BookReaderEntryReadyPackageGate.shouldEnable(
            packageName = "com.shenghui.localvibe.fastspeech2formal",
            explicitFlag = true,
        )
    )
    assertFalse(
        BookReaderEntryReadyPackageGate.shouldEnable(
            packageName = "com.shenghui.localvibe",
            explicitFlag = true,
        )
    )
}

@Test
fun explicitFlagFalseKeepsFormalDisabled() {
    assertFalse(
        BookReaderEntryReadyPackageGate.shouldEnable(
            packageName = "com.shenghui.localvibe.fastspeech2formal",
            explicitFlag = false,
        )
    )
}
```

- [ ] **Step 2: Run test and confirm RED**

```powershell
.\gradlew.bat :app:testDebugUnitTest --no-daemon --max-workers=1 --console=plain -Dorg.gradle.jvmargs="-Xmx6144m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8" -Dkotlin.daemon.jvm.options="-Xmx6144m"
```

- [ ] **Step 3: Add minimal helper**

```kotlin
object BookReaderEntryReadyPackageGate {
    private const val FORMAL_PACKAGE_NAME = "com.shenghui.localvibe.fastspeech2formal"

    fun shouldEnable(
        packageName: String,
        explicitFlag: Boolean,
    ): Boolean {
        return explicitFlag && packageName == FORMAL_PACKAGE_NAME
    }
}
```

- [ ] **Step 4: Run tests and confirm GREEN**

Expected: PASS.

### Task 2: Wire Formal package guard into MainActivity

**Files:**
- Modify: `app/src/main/java/com/shenghui/localvibe/MainActivity.kt`
- Test: package-guard tests remain the proof for main-vs-formal behavior.

- [ ] **Step 1: Keep explicit flag false**

Leave:

```kotlin
private const val ENABLE_BOOK_READER_READY_PATH = false
```

- [ ] **Step 2: Derive runtime gate from application id**

Near the BookListen route:

```kotlin
val isBookReaderReadyPathEnabled = BookReaderEntryReadyPackageGate.shouldEnable(
    packageName = context.packageName,
    explicitFlag = ENABLE_BOOK_READER_READY_PATH,
)
```

- [ ] **Step 3: Use the derived gate for loader and ready path**

Use the derived boolean, not the raw flag:

```kotlin
val entryReadyRouteState = BookReaderEntryReadyRouteLoader.loadIfEnabled(
    enabled = isBookReaderReadyPathEnabled,
    input = entryReadyRouteInput,
    loader = routeReadyStateLoader,
)

val entryReadyPathPlan = BookReaderEntryReadyPath.plan(
    enabled = isBookReaderReadyPathEnabled,
    routeState = entryReadyRouteState,
)
```

With the explicit flag still false, this must keep `entryReadyState = null`.

### Task 3: Formal-only temporary enable pass

**Files:**
- Modify only after user approval:
  - `app/src/main/java/com/shenghui/localvibe/MainActivity.kt` or debug-only config.

- [ ] **Step 1: Temporarily enable for Formal only**

Change only the explicit ready-path flag or debug-only config after tests pass and user approves.

- [ ] **Step 2: Verify main package remains protected**

Before install:

```text
APK package must be com.shenghui.localvibe.fastspeech2formal
Main package com.shenghui.localvibe must remain installed separately
```

- [ ] **Step 3: Do not use release/main app for first enablement**

The first device test must only run Formal.

### Task 4: Ready-path screen behavior verification

**Files:**
- No code changes unless a test or log proves a specific architectural failure.

- [ ] **Step 1: Automated verification**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --no-daemon --max-workers=1 --console=plain -Dorg.gradle.jvmargs="-Xmx6144m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8" -Dkotlin.daemon.jvm.options="-Xmx6144m"
.\gradlew.bat :app:assembleDebug --no-daemon --max-workers=1 --console=plain -Dorg.gradle.jvmargs="-Xmx6144m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8" -Dkotlin.daemon.jvm.options="-Xmx6144m"
git diff --check
```

- [ ] **Step 2: APK package check**

Use `aapt dump badging` and require:

```text
package: name='com.shenghui.localvibe.fastspeech2formal'
```

- [ ] **Step 3: Install Formal only after user approval**

Use the user-approved device command:

```powershell
E:\Android\platform-tools\adb.exe -s GEY5UCAMQ8A6YPB6 install -r -g .\app\build\outputs\apk\debug\app-debug.apk
```

- [ ] **Step 4: Confirm both packages exist**

Require both:

```text
package:com.shenghui.localvibe
package:com.shenghui.localvibe.fastspeech2formal
```

## 7. Real-device Acceptance Checklist

Formal/debug ready path is acceptable only if all items pass:

- First view is the correct real reader text.
- Text scrolls immediately.
- No loading spinner.
- No "恢复阅读位置".
- Play does not show "正在恢复小说内容".
- After play, text does not refresh or jump.
- After play, progress/time do not suddenly jump.
- Playback starts quickly.
- Auto advance works.
- Highlight follows playback.
- Pause/resume works.
- No voice package/settings jump.
- No Formal crash, black screen, freeze, or TFLite buffer error.

Required log evidence:

```text
ready path package gate formal=true
entryReadyState consumed
firstFrameTarget == playTarget
progressTarget == playTarget
lazyListTarget == playTarget
snapshotReaderContent=false
restorePrompt=false
scrollAfterVisible=false
alignAfterVisible=false
playback completed
auto advance next target
reader highlight from auto advance
```

## 8. Rollback Strategy

Rollback must be boring:

1. Set the ready-path explicit flag back to false.
2. Keep route-ready types, tests, and loader helper in place.
3. Do not revert FastSpeech2, MB-MelGAN, mapper, AudioTrack, auto advance, pause/resume, or playback controller code.
4. Do not alter main package behavior.
5. If Formal fails, keep main app on the legacy path and continue diagnosing ready-state data mismatch.

Rollback must not require touching model files, APK artifacts, logs, or test-results.

## 9. Stop Conditions

Stop immediately if any of these are observed:

- Formal APK package is not `com.shenghui.localvibe.fastspeech2formal`.
- Main app package is overwritten.
- Ready path can enable for `com.shenghui.localvibe`.
- Ready path enters BookListenScreen before `BookReaderEntryReadyState` is ready.
- Reader text is fake snapshot, not real content.
- Reader text is visible but cannot scroll.
- Play shows "正在恢复小说内容".
- Play changes the visible position/progress.
- Any Formal `FATAL EXCEPTION`, ANR, black screen, or freeze appears.


# Book Reader Phase 5B.18 Ready Path Enabled Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enable the reader ready path only after route-level data is complete, so the first BookListenScreen frame is real, interactive, correctly positioned reader content.

**Architecture:** MainActivity/route code prepares a complete `BookReaderEntryReadyState` before entering the real reader path. `BookListenScreen` consumes that state as the single source for paragraphs, chapter sentences, canonical target, progress/time, LazyList initial position, and playback seed. The legacy path remains as fallback, while the ready path explicitly bypasses snapshot/loading/late-restore UI.

**Tech Stack:** Kotlin, Jetpack Compose, JVM unit tests, Android Gradle plugin, existing FastSpeech2 reader playback foundation.

---

## 1. Current State

The codebase already has the pieces needed to prepare a first-frame reader state without changing the playback engine:

- `BookReaderEntryReadyState`: ready-state boundary for paragraphs, chapter sentences, canonical target, progress/time, LazyList initial position, and playback seed.
- `BookReaderEntryReadyStateFactory`: pure resolver that builds canonical ready state from prepared inputs.
- `BookReaderEntryReadyStateLoader`: pure loader boundary that combines paragraph source, restore/cache source, and factory.
- `BookReaderEntryReadyStateWiringAdapter`: conversion layer from existing route/book inputs into loader inputs.
- `BookReaderRestoreCacheSource`: extracted restore/cache source from the old BookListenScreen-private logic.
- `BookReaderEntryReadyRoute`: route-level host/slot/plan skeleton, including disabled shadow and disabled ready-path decisions.
- `MainActivity`: currently has disabled route wiring:
  - `ENABLE_BOOK_READER_ENTRY_READY_ROUTE_SHADOW = false`
  - `ENABLE_BOOK_READER_READY_PATH = false`
  - `BookListenScreen(entryReadyState = null)` by default.
- `BookListenScreen`: already accepts `entryReadyState: BookReaderEntryReadyState? = null`, but the default legacy behavior is still active and the ready state does not drive UI.

No current code should be treated as a completed first-entry fix. It is only a safe boundary for the next implementation.

## 2. Enablement Principles

The ready path must solve the real root cause: BookListenScreen cannot create its first real frame before paragraphs, chapter sentences, restore target, progress/time, LazyList target, and playback target are all ready.

The following approaches remain forbidden:

- Do not use stable snapshot as fake reader content.
- Do not use loading, "恢复阅读位置", alpha, overlay, or placeholder to hide restore work.
- Do not render one reader position and then `scrollToItem`, `requestScrollToItem`, or align to another visible position.
- Do not use dual reader instances or snapshot-to-real swaps.
- Do not correct target/progress only after the play button is clicked.
- Do not show text that cannot scroll.
- Do not show text where play still reports "正在恢复小说内容".

If ready data is not available, do not enter the ready BookListenScreen path. The route layer may wait or fall back to legacy, but once the user sees the true reader path it must already be ready.

## 3. Minimum Implementation Steps

### Task 1: Add ready path route decision tests

**Files:**
- Modify: `app/src/test/java/com/shenghui/localvibe/feature/book/BookReaderEntryReadyRouteTest.kt`
- Modify: `app/src/main/java/com/shenghui/localvibe/feature/book/BookReaderEntryReadyRoute.kt`

- [ ] **Step 1: Write the failing tests**

Add tests that require:

```kotlin
@Test
fun enabledReadyPathPassesReadyStateOnlyWhenRouteStateIsReady() {
    val readyState = readyState()

    val plan = BookReaderEntryReadyPath.plan(
        enabled = true,
        routeState = BookReaderEntryReadyRouteState.Ready(
            input = input(),
            readyState = readyState,
        ),
    )

    assertTrue(plan is BookReaderEntryReadyPathPlan.Ready)
    assertTrue(plan.usesReadyState)
    assertSame(readyState, plan.entryReadyStateForScreen())
}

@Test
fun enabledReadyPathDoesNotEnterReaderWhenStateIsPreparingOrFailed() {
    val preparing = BookReaderEntryReadyPath.plan(
        enabled = true,
        routeState = BookReaderEntryReadyRouteState.Preparing(input()),
    )
    val failed = BookReaderEntryReadyPath.plan(
        enabled = true,
        routeState = BookReaderEntryReadyRouteState.Failed(
            input = input(),
            message = "paragraphs unavailable",
        ),
    )

    assertNull(preparing.entryReadyStateForScreen())
    assertNull(failed.entryReadyStateForScreen())
    assertFalse(preparing.usesReadyState)
    assertFalse(failed.usesReadyState)
}
```

- [ ] **Step 2: Run the tests and confirm RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --no-daemon --max-workers=1 --console=plain -Dorg.gradle.jvmargs="-Xmx6144m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8" -Dkotlin.daemon.jvm.options="-Xmx6144m"
```

Expected: FAIL until `BookReaderEntryReadyPath` explicitly protects preparing/failed states and returns ready only for `Ready`.

- [ ] **Step 3: Implement the minimal decision**

Keep `BookReaderEntryReadyPath.plan()` small:

```kotlin
object BookReaderEntryReadyPath {
    fun plan(
        enabled: Boolean,
        routeState: BookReaderEntryReadyRouteState?,
    ): BookReaderEntryReadyPathPlan {
        return if (enabled && routeState is BookReaderEntryReadyRouteState.Ready) {
            BookReaderEntryReadyPathPlan.Ready(routeState.readyState)
        } else {
            BookReaderEntryReadyPathPlan.Legacy
        }
    }
}
```

- [ ] **Step 4: Run the tests and confirm GREEN**

Expected: PASS.

### Task 2: Add route-level ready loader state without driving UI

**Files:**
- Modify: `app/src/main/java/com/shenghui/localvibe/MainActivity.kt`
- Test: `app/src/test/java/com/shenghui/localvibe/feature/book/BookReaderEntryReadyRouteTest.kt` or a small pure helper test if MainActivity logic is extracted.

- [ ] **Step 1: Write the failing contract test**

Extract a pure helper if needed, for example:

```kotlin
object BookReaderEntryReadyRouteDecision {
    fun shouldRenderReadyScreen(
        readyPathEnabled: Boolean,
        routeState: BookReaderEntryReadyRouteState?,
    ): Boolean
}
```

Test:

```kotlin
@Test
fun readyScreenCanRenderOnlyWhenFlagEnabledAndRouteStateReady() {
    assertFalse(BookReaderEntryReadyRouteDecision.shouldRenderReadyScreen(false, readyStateRoute()))
    assertFalse(BookReaderEntryReadyRouteDecision.shouldRenderReadyScreen(true, null))
    assertFalse(BookReaderEntryReadyRouteDecision.shouldRenderReadyScreen(true, BookReaderEntryReadyRouteState.Preparing(input())))
    assertTrue(BookReaderEntryReadyRouteDecision.shouldRenderReadyScreen(true, readyStateRoute()))
}
```

- [ ] **Step 2: Run and confirm RED**

Expected: FAIL because the helper does not exist.

- [ ] **Step 3: Add route state storage behind the existing flag**

In `MainActivity.kt`, keep `ENABLE_BOOK_READER_READY_PATH = false` initially. Prepare a nullable route state near the BookListen route:

```kotlin
var entryReadyRouteState by remember(resolvedBookFile?.uri, initialBookParagraphIndex) {
    mutableStateOf<BookReaderEntryReadyRouteState?>(null)
}
```

When the flag is false, do not start loading. When the flag is true in a later debug-only pass, route state may move through `Preparing -> Ready/Failed`.

- [ ] **Step 4: Keep the real BookListenScreen call legacy while flag is false**

The default call must remain equivalent:

```kotlin
val entryReadyPathPlan = BookReaderEntryReadyPath.plan(
    enabled = ENABLE_BOOK_READER_READY_PATH,
    routeState = entryReadyRouteState,
)

BookListenScreen(
    bookFile = resolvedBookFile,
    initialParagraphIndex = initialBookParagraphIndex,
    entryReadyState = entryReadyPathPlan.entryReadyStateForScreen(),
    ...
)
```

With the flag false, `entryReadyState` must be null.

### Task 3: Implement route-level ready loading behind the flag

**Files:**
- Modify: `app/src/main/java/com/shenghui/localvibe/MainActivity.kt`
- Modify or create: a focused route helper if MainActivity grows too much.
- Test: pure helper tests where possible.

- [ ] **Step 1: Write tests for source wiring**

Create tests around helper code, not Compose. The helper should prove:

```text
resolvedBookFile + initialParagraphIndex -> BookReaderEntryReadyRouteInput
route input -> BookReaderEntryLoadRequest
restore cache input remains advisory and cannot override canonical target
```

- [ ] **Step 2: Run and confirm RED**

Expected: FAIL until the helper exists.

- [ ] **Step 3: Add the route loader call only inside the enabled branch**

The route should load only when the flag is true:

```kotlin
if (ENABLE_BOOK_READER_READY_PATH) {
    LaunchedEffect(resolvedBookFile?.uri, initialBookParagraphIndex) {
        entryReadyRouteState = BookReaderEntryReadyRouteState.Preparing(entryReadyRouteInput)
        val request = entryReadyRouteInput.toLoadRequest()
        if (request == null) {
            entryReadyRouteState = BookReaderEntryReadyRouteState.Failed(
                input = entryReadyRouteInput,
                message = "book file unavailable",
            )
            return@LaunchedEffect
        }
        // Call a route-level loader/adapter here. Do not duplicate factory logic in MainActivity.
    }
}
```

Do not add reader-body loading. Do not show snapshot. If loading cannot complete, either keep legacy path selected or show a route-level non-reader wait state in a later controlled pass.

- [ ] **Step 4: Log ready-path state**

Use `LV_BOOK_FORMAL` with a clear prefix:

```text
phase5b ready path route load
phase5b ready path route ready
phase5b ready path route failed
```

Logs must include book id, route initial paragraph, canonical target, progress/time, LazyList initial target, and playback seed.

### Task 4: Make BookListenScreen ready path initialize from readyState

**Files:**
- Modify: `app/src/main/java/com/shenghui/localvibe/feature/book/BookListenScreen.kt`
- Modify: `app/src/main/java/com/shenghui/localvibe/feature/book/BookReaderEntryReadyConsumeBoundary.kt`
- Test: `app/src/test/java/com/shenghui/localvibe/feature/book/BookReaderEntryReadyConsumeBoundaryTest.kt`

- [ ] **Step 1: Write failing consume tests**

The ready first frame must be a single source:

```kotlin
@Test
fun readyFirstFrameUsesSameTargetForReaderProgressLazyListAndPlayback() {
    val firstFrame = requireNotNull(
        BookReaderEntryReadyConsumeBoundary.from(readyState()).firstFrameOrNull()
    )

    assertEquals(firstFrame.firstFrameTarget.paragraphIndex, firstFrame.playbackSeed.paragraphIndex)
    assertEquals(firstFrame.firstFrameTarget.sentenceIndex, firstFrame.playbackSeed.sentenceIndex)
    assertEquals(firstFrame.firstFrameTarget.chapterSentenceIndex, firstFrame.progressSnapshot.chapterSentenceIndex)
    assertEquals(firstFrame.lazyListInitialIndex, firstFrame.firstFrameTarget.chapterSentenceIndex + 1)
}
```

- [ ] **Step 2: Run and confirm RED for missing ready-path behavior**

Expected: FAIL until BookListenScreen actually initializes from `entryReadyState`.

- [ ] **Step 3: Initialize state from ready boundary only when non-null**

When `entryReadyState != null`, derive initial values from `BookReaderEntryReadyFirstFrame`:

```text
paragraphs = firstFrame.paragraphs
chapterSentences = firstFrame.chapterSentences
currentParagraphIndex = firstFrame.firstFrameTarget.paragraphIndex
currentSentenceIndex = firstFrame.firstFrameTarget.sentenceIndex
currentChapterSentenceIndex = firstFrame.firstFrameTarget.chapterSentenceIndex
progress/time = firstFrame.progressSnapshot
LazyListState initial index/offset = firstFrame.lazyListInitialIndex / firstFrame.lazyListInitialOffset
playback target seed = firstFrame.playbackSeed
```

For `entryReadyState == null`, keep all existing legacy initialization.

- [ ] **Step 4: Bypass old first-entry traps only on ready path**

When the ready path is active:

```text
isLoading initial false
stable_ui_snapshot not rendered
snapshot_to_real handoff not used
restore positioning overlay not used
reader body restore prompt not used
post-visible restore scroll/align not used
snapshot onPlayPause toast not reachable
```

Legacy path remains unchanged as fallback.

### Task 5: Add ready-path route gate before real reader entry

**Files:**
- Modify: `app/src/main/java/com/shenghui/localvibe/MainActivity.kt`
- Modify: `app/src/main/java/com/shenghui/localvibe/feature/book/BookReaderEntryReadyRoute.kt`
- Test: `app/src/test/java/com/shenghui/localvibe/feature/book/BookReaderEntryReadyRouteTest.kt`

- [ ] **Step 1: Write tests for "not ready means no ready reader"**

```kotlin
@Test
fun preparingAndFailedStatesDoNotCreateReaderContentOrSnapshot() {
    val preparingSlot = BookReaderEntryReadyRouteHost.resolveSlot(BookReaderEntryReadyRouteState.Preparing(input()))
    val failedSlot = BookReaderEntryReadyRouteHost.resolveSlot(BookReaderEntryReadyRouteState.Failed(input(), "failed"))

    assertNull(preparingSlot.readyStateOrNull())
    assertNull(failedSlot.readyStateOrNull())
    assertFalse(preparingSlot.exposesStableSnapshotAsReaderContent)
    assertFalse(failedSlot.exposesStableSnapshotAsReaderContent)
}
```

- [ ] **Step 2: Implement the gate**

When `ENABLE_BOOK_READER_READY_PATH == true`:

```text
Ready -> call BookListenScreen(entryReadyState = readyState)
Preparing -> do not call BookListenScreen ready path
Failed -> fall back to legacy or route-level failure UI, but do not create fake reader content
```

The first implementation may choose legacy fallback on failure. It must not show fake reader content.

### Task 6: Verification before real-device smoke

**Files:**
- No code changes unless tests reveal a gap.

- [ ] **Step 1: UTF-8 check**

Run:

```powershell
python -c "from pathlib import Path; files=[Path(r'app/src/main/java/com/shenghui/localvibe/MainActivity.kt'), Path(r'app/src/main/java/com/shenghui/localvibe/feature/book/BookListenScreen.kt')]; [p.read_bytes().decode('utf-8') for p in files if p.exists()]; print('utf-8 decode: PASS')"
```

- [ ] **Step 2: Run unit tests**

```powershell
.\gradlew.bat :app:testDebugUnitTest --no-daemon --max-workers=1 --console=plain -Dorg.gradle.jvmargs="-Xmx6144m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8" -Dkotlin.daemon.jvm.options="-Xmx6144m"
```

- [ ] **Step 3: Run debug build**

```powershell
.\gradlew.bat :app:assembleDebug --no-daemon --max-workers=1 --console=plain -Dorg.gradle.jvmargs="-Xmx6144m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8" -Dkotlin.daemon.jvm.options="-Xmx6144m"
```

- [ ] **Step 4: Run diff check**

```powershell
git diff --check
```

Expected: all PASS before any APK install.

## 4. Ready Path Must Bypass These Legacy Branches

The ready path must not execute these old first-entry behaviors:

- `isLoading` first frame true for reader body.
- `stable_ui_snapshot` as visible reader text.
- `snapshot_to_real` handoff.
- Restore positioning overlay.
- Visible-after-render `scrollToItem`, `requestScrollToItem`, align, or delayed correction.
- Snapshot `onPlayPause` path that shows "正在恢复小说内容".

These may remain in the legacy fallback path until the ready path has real-device evidence and is safe to simplify.

## 5. Risk Controls

- Keep internal flags default false until all tests pass.
- First enabled pass should be limited to Formal debug package behavior.
- If the route-level loader fails, use legacy fallback or a route-level non-reader failure state; never enter a fake reader.
- Do not affect the main package `com.shenghui.localvibe`.
- Do not change FastSpeech2, MB-MelGAN, mapper, AudioTrack, auto advance, pause/resume, music, video, or model files.
- Do not copy ready-state factory logic into MainActivity. MainActivity should orchestrate, not resolve canonical targets.

## 6. Test Plan

Required automated tests:

- Ready path route decision:
  - flag false -> legacy
  - flag true + `Ready` -> ready state passed
  - flag true + `Preparing`/`Failed`/null -> no ready state
- Ready route host:
  - `Preparing` and `Failed` do not create reader content.
  - no route state exposes stable snapshot as reader content.
- BookListenScreen ready consume boundary:
  - first frame target == playback target
  - progress target == playback target
  - LazyList initial target == playback target
  - ready first frame uses paragraphs/chapterSentences from ready state
- Restore/cache source:
  - cached viewport remains advisory
  - stale cached viewport cannot override canonical play target
- No restore prompt contract:
  - ready path cannot use snapshot onPlayPause fallback
- No visible-after-scroll contract:
  - ready path initial LazyList target comes from ready state before first render

## 7. Manual Smoke Acceptance

Only after automatic verification passes and the user approves install/logcat:

- First view after entering reader is correct real text.
- Text can scroll immediately.
- No spinner.
- No "恢复阅读位置".
- Play does not show "正在恢复小说内容".
- After play, text does not refresh or jump.
- After play, progress/time do not suddenly jump.
- Playback starts normally.
- Auto advance works.
- Highlight follows playback.
- Pause/resume works.
- No voice package/settings jump.
- No Formal crash, black screen, freeze, or TFLite buffer error.

Required log evidence:

```text
entryReadyState consumed
firstFrameTarget == playTarget
progressTarget == playTarget
lazyListTarget == playTarget
visibleAfterReady=true
restorePrompt=false
snapshotReaderContent=false
scrollAfterVisible=false
alignAfterVisible=false
playback completed
auto advance next target
reader highlight from auto advance
```

## 8. Stop Conditions

Stop immediately and do not continue patching UI if any of these appear:

- Ready state is not complete but the ready BookListenScreen path is entered.
- Ready path first frame has empty paragraphs.
- Ready path shows stable snapshot.
- Ready path shows loading or "恢复阅读位置".
- Ready path scrolls or aligns after visible content.
- Play changes target/progress instead of using the first-frame target.
- Text is visible but cannot scroll.
- Play shows "正在恢复小说内容".

These are architecture failures, not small visual bugs.


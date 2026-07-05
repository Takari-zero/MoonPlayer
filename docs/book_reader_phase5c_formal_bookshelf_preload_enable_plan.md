# Phase 5C Formal Bookshelf Preload Enable Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Safely enable bookshelf background reader preload only for the Formal debug package, while keeping the main app on the legacy path.

**Architecture:** The bookshelf layer owns background preparation. `BookReaderPreloadRuntime` prepares parsed document and `BookReaderEntryReadyStateStore` entries before the user opens the listener. `BookListenScreen` is entered only when a ready state exists, so the reader is never used as a loading room.

**Tech Stack:** Kotlin, Jetpack Compose route state in `MainActivity`, `BookReaderPreloadRuntime`, `BookReaderPreloadRuntimeFactory`, `BookReaderPreloadRuntimeWiring`, `BookReaderBookshelfPreloadCoordinator`, `BookReaderEntryReadyStateStore`, and Formal package gating via `BuildConfig.APPLICATION_ID`.

---

## 1. Current State

The mature reader preload foundation is present but disabled at runtime:

- `BookDocumentCache` exists and models parsed documents, ready snapshots, preload status, freshness, stale entries, and failed states.
- `BookDocumentRepository` exists and can consume `BookParagraphSource` to build a `BookDocumentReadySnapshot`.
- `BookReaderPreloadManager` exists and can preload documents, return preload results, invalidate cache entries, and write successful ready states into `BookReaderEntryReadyStateStore`.
- `BookReaderEntryReadyStateStore` exists and can store missing, loading, ready, failed, and stale states by `BookDocumentCacheKey`.
- `AndroidTxtBookParagraphSource` exists and wraps the Android TXT reader behind the `BookParagraphSource` boundary.
- `BookReaderPreloadRuntime` and `BookReaderPreloadRuntimeFactory` exist and can compose the paragraph source, repository, preload manager, ready state store, and bookshelf preload coordinator.
- `BookReaderPreloadRuntimeWiring` exists and currently supports a disabled no-op plan and a future enabled plan.
- `MainActivity` has a disabled bookshelf preload wiring slot.
- `ENABLE_BOOKSHELF_READER_PRELOAD` currently must remain `false`.
- Current `BookLibraryScreen.onOpenBook` still uses the legacy immediate navigation flow.
- No real bookshelf background preload should run until a later implementation phase explicitly enables it for the Formal package.

## 2. Formal-Only Enable Principles

Bookshelf preload must be enabled conservatively:

- Enable only for `com.shenghui.localvibe.fastspeech2formal` first.
- Keep the main app package `com.shenghui.localvibe` unaffected and on the legacy path.
- Keep a single explicit flag so the feature can be turned off immediately.
- Do not block the bookshelf UI while preparing reader data.
- Do not parse the whole book synchronously when the user clicks a novel.
- Do not use `BookListenScreen` as a loading state.
- Do not route to a black or blank listener screen while ready state is preparing.
- Do not use snapshot, loading, overlay, alpha, gate, or double-reader tricks to fake readiness.
- Do not correct reader position or progress after visible reader content appears.
- Do not correct target or progress when playback starts.

The allowed behavior is: prepare in the background at bookshelf level, store ready state, and only then let ready books enter the listener immediately.

## 3. Correct Preload Behavior

When the user reaches the bookshelf in the Formal package:

1. Build or retrieve a `BookReaderPreloadRuntime` from the disabled wiring slot.
2. Convert recent and visible bookshelf books into `BookReaderBookshelfPreloadCandidate` values.
3. Ask `BookReaderBookshelfPreloadCoordinator.plan(...)` for a bounded preload plan.
4. Run `BookReaderBookshelfPreloadCoordinator.preload(...)` from a background coroutine.
5. Write successful ready states into `BookReaderEntryReadyStateStore` through `BookReaderPreloadManager`.
6. Keep the bookshelf responsive while preload runs.
7. Log preload lifecycle events without exposing private document content.

When the user taps a book in the Formal package:

- If `BookReaderEntryReadyStateStore.getReadyState(key)` returns a fresh ready state, immediately navigate to `BookListenScreen(entryReadyState = readyState)`.
- If ready state is missing, loading, failed, or stale, stay on the bookshelf and show a lightweight bookshelf-level preparing or retry state.
- Do not enter `BookListenScreen` until ready state exists.

The main app package keeps the legacy click flow throughout the first Formal-only smoke cycle.

## 4. Explicitly Forbidden

Do not reintroduce any of the failed Phase 5A/5B strategies:

- Do not synchronously parse the whole novel when the user clicks it.
- Do not freeze the bookshelf after click while parsing.
- Do not route to a black reader screen.
- Do not show loading, `恢复阅读位置`, or any reader-level preparing text inside `BookListenScreen`.
- Do not render a stable snapshot as fake reader text.
- Do not use overlay, alpha, placeholder, or gate tricks to hide a jump.
- Do not use two reader instances and swap them.
- Do not scroll or align after visible reader content appears.
- Do not correct paragraph, progress, or playback target when play is tapped.
- Do not let text appear but be non-scrollable.
- Do not let play show `正在恢复小说内容` in the ready path.
- Do not affect music, video, FastSpeech2, MB-MelGAN, mapper, AudioTrack, auto advance, pause, or resume behavior.

## 5. Step-By-Step Implementation Plan

### Task 5C.15: Formal-Only Preload Flag And No-Op Runtime Observe

**Files:**

- Modify: `app/src/main/java/com/shenghui/localvibe/MainActivity.kt`
- Modify if needed: `app/src/main/java/com/shenghui/localvibe/feature/book/playback/BookReaderPreloadRuntimeWiring.kt`
- Test: `app/src/test/java/com/shenghui/localvibe/feature/book/playback/BookReaderPreloadRuntimeWiringTest.kt`

- [ ] **Step 1: Write the failing test for package gating**

Add tests proving the future enable decision is Formal-only:

```kotlin
@Test
fun formalPackageCanEnableBookshelfPreloadWhenExplicitFlagIsTrue() {
    val decision = BookReaderPreloadRuntimeWiring.shouldEnableForPackage(
        packageName = "com.shenghui.localvibe.fastspeech2formal",
        explicitFlag = true,
    )

    assertTrue(decision)
}

@Test
fun mainPackageCannotEnableBookshelfPreloadEvenWhenExplicitFlagIsTrue() {
    val decision = BookReaderPreloadRuntimeWiring.shouldEnableForPackage(
        packageName = "com.shenghui.localvibe",
        explicitFlag = true,
    )

    assertFalse(decision)
}
```

- [ ] **Step 2: Run the target test and verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.shenghui.localvibe.feature.book.playback.BookReaderPreloadRuntimeWiringTest --no-daemon --max-workers=1 --console=plain -Dorg.gradle.jvmargs="-Xmx6144m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8" -Dkotlin.daemon.jvm.options="-Xmx6144m"
```

Expected: FAIL because `shouldEnableForPackage` does not exist.

- [ ] **Step 3: Add minimal Formal-only gate**

Add to `BookReaderPreloadRuntimeWiring`:

```kotlin
private const val FORMAL_PACKAGE_NAME = "com.shenghui.localvibe.fastspeech2formal"

fun shouldEnableForPackage(
    packageName: String,
    explicitFlag: Boolean,
): Boolean {
    return explicitFlag && packageName == FORMAL_PACKAGE_NAME
}
```

- [ ] **Step 4: Keep MainActivity flag off**

Verify `MainActivity.kt` keeps:

```kotlin
private const val ENABLE_BOOKSHELF_READER_PRELOAD = false
```

The phase should only observe the disabled wiring and must not start preload.

- [ ] **Step 5: Run tests**

Run the same target test, then full verification:

```powershell
.\gradlew.bat :app:testDebugUnitTest --no-daemon --max-workers=1 --console=plain -Dorg.gradle.jvmargs="-Xmx6144m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8" -Dkotlin.daemon.jvm.options="-Xmx6144m"
.\gradlew.bat :app:assembleDebug --no-daemon --max-workers=1 --console=plain -Dorg.gradle.jvmargs="-Xmx6144m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8" -Dkotlin.daemon.jvm.options="-Xmx6144m"
git diff --check
```

Expected: PASS. Runtime behavior unchanged.

- [ ] **Step 6: Commit only after verification**

```powershell
git add app/src/main/java/com/shenghui/localvibe/MainActivity.kt
git add app/src/main/java/com/shenghui/localvibe/feature/book/playback/BookReaderPreloadRuntimeWiring.kt
git add app/src/test/java/com/shenghui/localvibe/feature/book/playback/BookReaderPreloadRuntimeWiringTest.kt
git commit -m "feat(book): gate bookshelf preload to formal package"
```

### Task 5C.16: Formal Bookshelf Background Preload Start

**Files:**

- Modify: `app/src/main/java/com/shenghui/localvibe/MainActivity.kt`
- Test: `app/src/test/java/com/shenghui/localvibe/feature/book/playback/BookReaderPreloadRuntimeWiringTest.kt`

- [ ] **Step 1: Write the failing test for bounded preload plan**

Add a test proving enabled wiring plans preload but still does not read source until explicit preload:

```kotlin
@Test
fun enabledFormalWiringCanCreateBoundedPreloadPlanWithoutReadingSource() {
    var readCalls = 0
    val plan = BookReaderPreloadRuntimeWiring.plan(
        enabled = true,
        runtimeFactory = {
            BookReaderPreloadRuntimeFactory(
                paragraphSourceFactory = {
                    BookParagraphSource { request ->
                        readCalls++
                        BookParagraphLoadResult.success(
                            key = request.key,
                            paragraphs = listOf("Chapter 1", "First sentence."),
                        )
                    }
                },
                clock = IncrementingBookDocumentClock(start = 100L),
            )
        },
    ) as BookReaderPreloadRuntimeWiringPlan.Enabled
    val runtime = plan.createRuntime()
    val request = BookDocumentPreloadRequest(
        key = BookDocumentCacheKey("content://book/demo.txt"),
        title = "Demo Book",
        savedParagraphIndex = 0,
        savedSentenceIndex = 0,
        chapterTitle = "Chapter 1",
        chapterStartIndex = 0,
        speechRate = 1f,
    )

    val preloadPlan = runtime.bookshelfPreloadCoordinator.plan(
        candidates = listOf(BookReaderBookshelfPreloadCandidate(request)),
        recentBookKeys = listOf(request.key),
        currentBookKey = null,
        maxPreloadCount = 1,
    )

    assertEquals(listOf(request.key), preloadPlan.map { it.request.key })
    assertEquals(0, readCalls)
}
```

- [ ] **Step 2: Run target tests and verify RED or new coverage**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.shenghui.localvibe.feature.book.playback.BookReaderPreloadRuntimeWiringTest --no-daemon --max-workers=1 --console=plain -Dorg.gradle.jvmargs="-Xmx6144m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8" -Dkotlin.daemon.jvm.options="-Xmx6144m"
```

Expected: PASS if current runtime already supports this, otherwise fail at the missing helper or plan shape. If it passes, it documents the existing foundation.

- [ ] **Step 3: Add Formal-only bookshelf preload launch in MainActivity**

In the `BookLibraryScreen` route, use `LaunchedEffect(bookFiles, BuildConfig.APPLICATION_ID)` only when the Formal gate is true. It must run in a coroutine and never in `onOpenBook` synchronously.

Implementation shape:

```kotlin
val isBookshelfPreloadEnabled = BookReaderPreloadRuntimeWiring.shouldEnableForPackage(
    packageName = BuildConfig.APPLICATION_ID,
    explicitFlag = ENABLE_BOOKSHELF_READER_PRELOAD,
)

if (isBookshelfPreloadEnabled) {
    LaunchedEffect(bookFiles.map { it.normalizedBookKey() }) {
        withContext(Dispatchers.IO) {
            // Build candidates and call coordinator.preload(plan).
            // Keep maxPreloadCount small for first Formal smoke.
        }
    }
}
```

Do not enable the flag in this task unless the user explicitly approves Formal-only smoke.

- [ ] **Step 4: Add logs**

Use a clear tag and redact content:

```kotlin
Log.d(BOOK_READER_ROUTE_SHADOW_TAG, "phase5c bookshelf preload start count=${plan.size}")
Log.d(BOOK_READER_ROUTE_SHADOW_TAG, "phase5c bookshelf preload success bookId=$bookId")
Log.w(BOOK_READER_ROUTE_SHADOW_TAG, "phase5c bookshelf preload failed bookId=$bookId reason=$reason")
```

Do not log paragraph text or raw document contents.

- [ ] **Step 5: Verify**

Run full unit/build/diff checks. No ADB is required in this implementation task unless a later smoke task asks for it.

### Task 5C.17: Click Uses ReadyState From Store

**Files:**

- Modify: `app/src/main/java/com/shenghui/localvibe/MainActivity.kt`
- Test: create or update a pure route decision helper test if the click logic becomes complex.

- [ ] **Step 1: Write the failing test for ready hit click decision**

Create a pure helper if needed:

```kotlin
sealed interface BookReaderBookshelfOpenDecision {
    data object LegacyOpen : BookReaderBookshelfOpenDecision
    data object StayOnBookshelfPreparing : BookReaderBookshelfOpenDecision
    data object OpenReadyReader : BookReaderBookshelfOpenDecision
}
```

Test:

```kotlin
@Test
fun formalReadyStateHitOpensReadyReader() {
    val decision = decideBookshelfOpen(
        isFormalPreloadEnabled = true,
        readyStateAvailable = true,
    )

    assertEquals(BookReaderBookshelfOpenDecision.OpenReadyReader, decision)
}
```

- [ ] **Step 2: Verify RED**

Run the new helper test and confirm it fails before implementation.

- [ ] **Step 3: Implement click logic**

In Formal path only:

```kotlin
val readyState = runtime.readyStateStore.getReadyState(bookKey)
if (readyState != null) {
    selectedMediaFile = file
    selectedBookUri = file.uri
    selectedBookEntryReadyState = readyState
    navController.navigate(LocalVibeRoute.BookListen)
} else {
    markBookPreparing(file)
    scheduleOrContinuePreload(file)
}
```

The main app branch remains:

```kotlin
selectedMediaFile = file
selectedBookUri = file.uri
navController.navigate(LocalVibeRoute.BookListen)
```

- [ ] **Step 4: Verify no click-time parsing**

Tests must prove the click decision reads only the store and does not call `preloadManager.preload(...)` synchronously inside `onOpenBook`.

- [ ] **Step 5: Verify firstFrame target equals play target before navigation**

Assert the selected ready state satisfies:

```kotlin
readyState.canonicalTarget.paragraphIndex == readyState.playbackSeed.paragraphIndex
readyState.canonicalTarget.sentenceIndex == readyState.playbackSeed.sentenceIndex
readyState.canonicalTarget.chapterSentenceIndex == readyState.progressSnapshot.chapterSentenceIndex
```

### Task 5C.18: Missing ReadyState Bookshelf-Level Preparing State

**Files:**

- Modify: `app/src/main/java/com/shenghui/localvibe/MainActivity.kt`
- Potentially modify: `app/src/main/java/com/shenghui/localvibe/feature/book/BookLibraryScreen.kt`
- Test: pure state helper tests before UI changes.

- [ ] **Step 1: Write the failing state test**

```kotlin
@Test
fun formalMissingReadyStateStaysOnBookshelfPreparing() {
    val decision = decideBookshelfOpen(
        isFormalPreloadEnabled = true,
        readyStateAvailable = false,
    )

    assertEquals(BookReaderBookshelfOpenDecision.StayOnBookshelfPreparing, decision)
}
```

- [ ] **Step 2: Implement lightweight bookshelf state**

Add a small state map in the bookshelf route:

```kotlin
val preparingBookKeys = remember { mutableStateMapOf<String, Boolean>() }
```

On Formal miss:

```kotlin
preparingBookKeys[file.normalizedBookKey()] = true
```

- [ ] **Step 3: Surface preparing status without entering reader**

Prefer a small card-level or top-area status in `BookLibraryScreen`. Do not open `BookListenScreen`; do not show reader-level loading.

- [ ] **Step 4: Keep UI responsive**

The preparing state update must happen immediately on click. Any preload work must run in background coroutine.

- [ ] **Step 5: Verify with unit/build checks**

Run `testDebugUnitTest`, `assembleDebug`, and `git diff --check`.

### Task 5C.19: Formal Smoke

**Files:**

- Do not change code during smoke unless the user starts a new fix task.

- [ ] **Step 1: Build Formal debug APK**

Run:

```powershell
.\gradlew.bat :app:assembleDebug --no-daemon --max-workers=1 --console=plain -Dorg.gradle.jvmargs="-Xmx6144m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8" -Dkotlin.daemon.jvm.options="-Xmx6144m"
```

- [ ] **Step 2: Confirm package name**

Use the project-approved ADB/package workflow for this worktree only when the user approves install/logcat.

Expected package:

```text
com.shenghui.localvibe.fastspeech2formal
```

- [ ] **Step 3: Manual smoke checklist**

The user must confirm:

- Opening the bookshelf does not freeze.
- Background preload does not affect scroll or taps.
- Clicking a preloaded book enters the listener immediately.
- If not ready, the app stays on bookshelf and shows preparing state.
- The listener first frame is correct real text.
- Text scrolls immediately.
- Tapping play does not show `正在恢复小说内容`.
- Tapping play does not make text or progress jump.
- Playback starts normally.
- Auto advance works.
- Highlight works.
- Pause/resume works.
- No Formal crash.

### Task 5C.20: Cleanup Old Ready-Route Failed Attempt

**Files:**

- Modify only after smoke PASS: `app/src/main/java/com/shenghui/localvibe/MainActivity.kt`
- Potentially modify after smoke PASS: `app/src/main/java/com/shenghui/localvibe/feature/book/BookListenScreen.kt`

- [ ] **Step 1: Confirm repeated Formal smoke PASS**

Do not clean up until at least one full user-confirmed smoke passes. Prefer two if the first pass is marginal.

- [ ] **Step 2: Remove old route blackscreen/preparing path**

Remove the failed 5B route-level ready loading path that waited inside the BookListen route.

- [ ] **Step 3: Keep main app legacy path**

Do not remove main package fallback before explicit approval.

- [ ] **Step 4: Keep playback untouched**

Do not modify FastSpeech2, MB-MelGAN, mapper, AudioTrack, auto advance, highlight, pause, or resume.

## 6. Verification Commands

For implementation tasks:

```powershell
git status --short
python -c "from pathlib import Path; p=Path(r'app/src/main/java/com/shenghui/localvibe/MainActivity.kt'); b=p.read_bytes(); b.decode('utf-8'); print('utf-8 decode: PASS'); print('nul_count:', b.count(b'\x00'))"
.\gradlew.bat :app:testDebugUnitTest --no-daemon --max-workers=1 --console=plain -Dorg.gradle.jvmargs="-Xmx6144m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8" -Dkotlin.daemon.jvm.options="-Xmx6144m"
.\gradlew.bat :app:assembleDebug --no-daemon --max-workers=1 --console=plain -Dorg.gradle.jvmargs="-Xmx6144m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8" -Dkotlin.daemon.jvm.options="-Xmx6144m"
git diff --check
```

For this docs-only phase:

```powershell
git status --short
git diff --check
```

## 7. Rollback Strategy

- Set `ENABLE_BOOKSHELF_READER_PRELOAD = false`.
- Keep main app on the legacy path.
- Keep foundation classes and tests unless they are directly causing compile failures.
- Do not roll back TTS, AudioTrack, playback, auto advance, highlight, pause, or resume code.
- If Formal smoke fails because preload blocks the shelf, disable the flag and return to the bookshelf-level design before any new code fix.

## 8. Self-Review

- Spec coverage: the plan covers current state, Formal-only principles, correct preload behavior, forbidden items, staged implementation, smoke criteria, and rollback.
- Placeholder scan: no TBD/TODO/fill-later placeholders are present.
- Type consistency: the plan uses existing names from the current codebase: `BookReaderPreloadRuntime`, `BookReaderPreloadRuntimeFactory`, `BookReaderPreloadRuntimeWiring`, `BookReaderBookshelfPreloadCoordinator`, `BookReaderEntryReadyStateStore`, and `ENABLE_BOOKSHELF_READER_PRELOAD`.

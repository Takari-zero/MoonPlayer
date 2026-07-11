# Phase 5C Bookshelf Preload Integration Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire mature reader preload into the bookshelf path so a ready book can enter `BookListenScreen` immediately with real interactive text.

**Architecture:** Keep heavy reader preparation outside `BookListenScreen`. The bookshelf layer starts background preload for recent or likely books, `BookReaderPreloadManager` writes parsed document snapshots and `BookReaderEntryReadyStateStore`, and clicks only navigate when a fresh ready state already exists.

**Tech Stack:** Kotlin, Jetpack Compose route state, existing `BookDocumentRepository`, `BookReaderPreloadManager`, `BookReaderEntryReadyStateStore`, `BookParagraphSource`, and Formal debug package gating.

---

## 1. Current State

The mature reader foundation is now in place but not yet connected to bookshelf runtime:

- `BookDocumentCache` exists and represents parsed documents, ready snapshots, preload status, freshness timestamps, stale state, and failed state.
- `BookDocumentRepository` exists and can consume injected `BookParagraphSource` results to build `BookDocumentReadySnapshot` through `BookReaderEntryReadyStateFactory`.
- `BookReaderPreloadManager` exists and can preload a document, return `BookReaderPreloadResult`, invalidate repository entries, and write successful ready states into `BookReaderEntryReadyStateStore`.
- `BookReaderEntryReadyStateStore` exists and can store loading, ready, failed, missing, and stale ready states by `BookDocumentCacheKey`.
- `BookParagraphSource` and `TxtBookParagraphSource` boundaries exist, but the real Android TXT runtime adapter is not connected to the bookshelf path.
- `MainActivity` still calls `BookLibraryScreen(onOpenBook = { selectedMediaFile = file; selectedBookUri = file.uri; navController.navigate(LocalVibeRoute.BookListen) })`.
- `BookListenScreen` still has legacy loading, restore gate, stable snapshot, and cached restore paths. Those paths must not be the solution for first-entry readiness.
- No bookshelf preload runtime is currently connected.
- No UI behavior should change until a Formal-only guarded wiring step is explicitly implemented and smoke-tested.

## 2. Target Architecture

Bookshelf display should quietly prepare likely reader entries before the user taps a book:

- When the bookshelf is shown, schedule background preload for recent reading books and visible candidate books.
- Preload reads paragraphs through a runtime `BookParagraphSource`, builds chapter sentences, resolves canonical restore target, computes progress/time, and builds playback seed.
- Successful preload writes `BookReaderEntryReadyState` into `BookReaderEntryReadyStateStore`.
- User clicks a book.
- If a fresh ready state exists in the store, the app immediately navigates to `BookListenScreen(entryReadyState = readyState)`.
- If no fresh ready state exists, the app stays on the bookshelf and shows a clear bookshelf-level preparing state for that book. It must not navigate into a black reader route.
- Once the book becomes ready, the user can tap again, or a deliberate user-approved auto-open behavior can be considered later. Do not auto-open by default in the first implementation.

The reader route is not a loading room. `BookListenScreen` should only be entered when the ready state is already available for the Formal path.

## 3. Recommended Integration Point

Use `MainActivity` / `BookLibraryScreen` around the existing `onOpenBook` path as the integration boundary.

Current click flow:

```kotlin
onOpenBook = { file ->
    selectedMediaFile = file
    selectedBookUri = file.uri
    navController.navigate(LocalVibeRoute.BookListen)
}
```

Future Formal-only flow:

```kotlin
onOpenBook = { file ->
    val key = BookDocumentCacheKey(file.normalizedBookKey())
    val readyState = readerReadyStateStore.getReadyState(key)
    if (isFormalPackage && readyState != null) {
        selectedMediaFile = file
        selectedBookUri = file.uri
        selectedBookEntryReadyState = readyState
        navController.navigate(LocalVibeRoute.BookListen)
    } else if (isFormalPackage) {
        markBookPreparing(file)
        scheduleBookPreload(file)
    } else {
        selectedMediaFile = file
        selectedBookUri = file.uri
        navController.navigate(LocalVibeRoute.BookListen)
    }
}
```

Do not put this responsibility inside `BookListenScreen`. Do not parse the whole book inside the route after navigation. Do not block the bookshelf click handler synchronously while parsing.

## 4. Phased Integration Plan

### 5C.9 Bookshelf Preload Wiring Skeleton

**Files:**

- Modify: `app/src/main/java/com/shenghui/localvibe/MainActivity.kt`
- Potentially create: `app/src/main/java/com/shenghui/localvibe/feature/book/BookReaderBookshelfPreloadState.kt`
- Test: `app/src/test/java/com/shenghui/localvibe/feature/book/BookReaderBookshelfPreloadStateTest.kt`

Steps:

- [ ] Add a Formal-only disabled flag or gate for bookshelf preload wiring.
- [ ] Add state shape for book preload status: missing, loading, ready, failed.
- [ ] Add pure helper tests proving a ready store hit selects immediate navigation and a missing store hit selects bookshelf preparing state.
- [ ] Do not call real TXT parsing yet.
- [ ] Verify `entryReadyState` remains null for main app legacy path.

Expected result: no runtime behavior change by default, but a tested decision boundary exists.

### 5C.10 Formal-Only Preload Smoke

**Files:**

- Modify: `app/src/main/java/com/shenghui/localvibe/MainActivity.kt`
- Potentially create: `app/src/main/java/com/shenghui/localvibe/feature/book/playback/AndroidTxtBookParagraphSource.kt`
- Test: focused JVM tests for decision logic only.

Steps:

- [ ] Enable background preload only for `com.shenghui.localvibe.fastspeech2formal`.
- [ ] Start preload from a coroutine when `BookLibraryScreen` receives a non-empty book list.
- [ ] Limit first smoke to recent book plus visible candidates.
- [ ] Log `phase5c bookshelf preload start/success/failed/cache_hit`.
- [ ] Keep main package `com.shenghui.localvibe` on the old path.
- [ ] Manually smoke that opening the bookshelf stays responsive.

Expected result: Formal package can prepare ready states in the background without blocking the shelf.

### 5C.11 Click Uses ReadyState From Store

**Files:**

- Modify: `app/src/main/java/com/shenghui/localvibe/MainActivity.kt`
- Test: route decision helper tests.

Steps:

- [ ] On Formal book click, check `BookReaderEntryReadyStateStore.getReadyState(key)` first.
- [ ] If present and fresh, assign selected book state and navigate immediately.
- [ ] Pass the ready state to the existing `BookListenScreen(entryReadyState = readyState)` boundary.
- [ ] Log `phase5c click readyState hit navigate` with canonical target and playback target.
- [ ] Prove in tests that firstFrame target equals playback target before navigation.

Expected result: ready books enter the listener immediately without route blackscreen or reader loading.

### 5C.12 Missing ReadyState Fallback At Bookshelf Level

**Files:**

- Modify: `app/src/main/java/com/shenghui/localvibe/MainActivity.kt`
- Potentially modify: `app/src/main/java/com/shenghui/localvibe/feature/book/BookLibraryScreen.kt`
- Test: state helper tests.

Steps:

- [ ] If Formal click misses ready state, do not navigate to `BookListenScreen`.
- [ ] Mark that book as preparing in bookshelf-level state.
- [ ] Start or continue background preload.
- [ ] Show a lightweight card/top-area preparing indicator on the bookshelf, not inside the reader.
- [ ] Keep the UI responsive while preload runs.
- [ ] If preload fails, show a bookshelf-level failure/retry state.

Expected result: users never see a black reader route or fake reader content when data is not ready.

### 5C.13 BookListenScreen Ready-Only Formal Smoke

**Files:**

- Modify only if needed after logs: `app/src/main/java/com/shenghui/localvibe/feature/book/BookListenScreen.kt`

Steps:

- [ ] Confirm Formal route passes non-null `entryReadyState` only after store hit.
- [ ] Confirm `BookListenScreen` skips legacy restore cache, stable snapshot, positioning overlay, and restore prompt when ready state is non-null.
- [ ] Confirm first frame uses ready paragraphs and ready canonical target.
- [ ] Confirm text scroll works immediately.
- [ ] Confirm play starts from the same target as first frame.

Expected result: first reader frame is real, interactive content.

### 5C.14 Cleanup Old Snapshot / Restore Path After Stable

**Files:**

- Modify: `app/src/main/java/com/shenghui/localvibe/feature/book/BookListenScreen.kt`
- Modify tests around restore gate only after repeated manual smoke passes.

Steps:

- [ ] Remove Formal-only dependency on `stable_ui_snapshot` once ready path has repeated smoke PASS.
- [ ] Remove reader-level restore loading prompt from Formal path.
- [ ] Keep main app legacy fallback until explicitly approved.
- [ ] Keep playback, auto advance, highlight, pause/resume unchanged.

Expected result: old traps are removed only after the mature path is proven.

## 5. Must Not Do

- Do not synchronously parse the whole book when the user clicks a novel.
- Do not enter `BookListenScreen` and then show loading.
- Do not use the route as a black waiting room.
- Do not render stable snapshot as fake??.
- Do not use overlay, alpha, gate, or placeholder tricks to hide jumps.
- Do not scroll or align after visible reader content is shown.
- Do not correct target/progress when playback starts.
- Do not let text appear but be non-scrollable.
- Do not let play show `????????` in the ready path.
- Do not affect the main package before Formal has passed real-device smoke.

## 6. Real-Device Acceptance Criteria

A Formal smoke can pass only when all of these are true:

- Opening the bookshelf does not freeze.
- Background preload does not block card scrolling, taps, or navigation elsewhere.
- A preloaded book click enters `BookListenScreen` immediately.
- If a book is not ready, the bookshelf shows preparing state and does not enter the reader.
- The first reader frame is real??, not snapshot text.
- Text can be scrolled immediately.
- Tapping play does not show `????????`.
- Tapping play starts audio from the same canonical target visible on first frame.
- Tapping play does not refresh or jump??.
- Progress/time does not jump on play.
- Auto advance works.
- Highlight follows playback.
- Pause/resume works.
- No Formal crash.

## 7. Risks And Rollback

- Risk: preloading too many books can make the bookshelf sluggish. Mitigation: preload recent/visible books first and cap concurrency.
- Risk: memory cache can grow. Mitigation: keep an LRU-style limit in a later phase before broad enablement.
- Risk: ready state can become stale after book deletion/reimport. Mitigation: invalidate `BookDocumentRepository` and `BookReaderEntryReadyStateStore` together.
- Risk: runtime TXT source may be slower than expected. Mitigation: start with Formal-only logs and keep click fallback at bookshelf level.
- Risk: main app behavior changes accidentally. Mitigation: guard by package and keep main package on legacy path.

Rollback strategy:

- Disable the Formal bookshelf preload flag.
- Leave foundation types and tests in place.
- Keep main app untouched.
- Do not roll back FastSpeech2, MB-MelGAN, mapper, AudioTrack, playback, auto advance, highlight, or pause/resume code.

## 8. Verification For Future Implementation

For code phases after this plan, run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --no-daemon --max-workers=1 --console=plain -Dorg.gradle.jvmargs="-Xmx6144m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8" -Dkotlin.daemon.jvm.options="-Xmx6144m"
.\gradlew.bat :app:assembleDebug --no-daemon --max-workers=1 --console=plain -Dorg.gradle.jvmargs="-Xmx6144m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8" -Dkotlin.daemon.jvm.options="-Xmx6144m"
git diff --check
```

For this docs-only phase, only `git status --short` and `git diff --check` are required.

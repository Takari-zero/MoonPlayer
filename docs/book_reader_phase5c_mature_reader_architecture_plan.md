# Phase 5C Mature Reader Architecture Plan

## 1. Current Failure Summary

Phase 5A tried to polish first-entry flicker inside `BookListenScreen` with stable snapshots, loading gates, overlays, alpha gates, and handoff timing. It failed because the screen still did not have the real paragraphs, chapter sentences, canonical target, progress, and playback seed before the first visible reader frame.

Phase 5B.21 moved toward a route-level ready path, but the ready load still happened after navigation into the BookListen route. The result was a black or blank reader route while paragraphs and restore state were prepared.

Phase 5B.23 moved the ready load before navigating to the reader route. That removed the reader-route black wait, but only moved the wait to the bookshelf click path: after tapping a book, the bookshelf stayed blocked while the app parsed and restored the book.

Root cause: heavy reader preparation still happens at click-to-enter time. Parsing the full text, building chapter sentences, resolving restore state, computing progress/time, and building playback seed are too late if they start only when the user taps the book.

## 2. Mature Reader Architecture Target

A mature reader should prepare reader data before the user enters the listening screen.

The expensive work should happen during import, bookshelf display, or background preload:

- Load the text document.
- Parse paragraphs.
- Build chapter sentences.
- Resolve the canonical restore target.
- Compute progress/time.
- Build the playback seed.
- Cache the resulting ready state.

When the user taps a book, the app should only read an already-prepared ready state and immediately navigate into the reader. The first frame in `BookListenScreen` must be real, interactive reader content.

The target experience:

- No black screen.
- No restore/loading screen in the reader.
- No fake snapshot text.
- No click-after-wait bookshelf freeze.
- No post-entry scroll correction.
- No playback-time target correction.

## 3. Recommended Modules

### BookRepository

Owns book-level access and identity. It should know how to resolve a book by URI or imported record, expose metadata needed by the bookshelf, and delegate document preparation to the cache/preload layers.

### BookDocumentCache

Caches parsed book document data:

- Book id / URI.
- Title.
- Paragraphs.
- Chapter sentence list.
- Parse metadata and version.
- Last prepared timestamp.

This cache can start as an in-memory cache and later grow into a persisted cache if needed.

### BookReaderPreloadManager

Coordinates background preparation:

- Preload recent books when the app opens.
- Preload visible or likely books when the bookshelf is shown.
- Coalesce duplicate loads for the same book.
- Avoid blocking the main thread.
- Expose lightweight status for the bookshelf.

### BookReaderEntryReadyStateStore

Stores and exposes the final entry state:

- Canonical paragraph index.
- Canonical sentence index.
- Canonical chapter sentence index.
- Canonical progress/time.
- LazyList initial index and offset.
- Playback seed.

The store should be the single source used by both click navigation and the reader screen.

### BookListenScreen Ready-Only Consume Path

`BookListenScreen` should consume an already-built ready state. It should not parse the full document, compute restore target, show restore loading, or use snapshots to fake content.

## 4. Data Flow

### Import / Open Book

1. User imports or adds a book.
2. `BookRepository` records the book identity and metadata.
3. `BookDocumentCache` parses and caches paragraphs when practical.
4. `BookReaderPreloadManager` schedules readiness work for recent or likely books.

### Parse And Cache

1. Read TXT content off the UI thread.
2. Split into paragraphs.
3. Build chapter sentences from paragraphs.
4. Store parsed document in `BookDocumentCache`.

### Build Ready State

1. Read restore/cache state.
2. Compute canonical paragraph index.
3. Compute canonical sentence index.
4. Compute canonical chapter sentence index.
5. Compute progress/time.
6. Build LazyList initial index/offset.
7. Build playback seed.
8. Store the result in `BookReaderEntryReadyStateStore`.

### User Clicks Book

If ready state is available:

1. Select the book.
2. Attach cached ready state.
3. Navigate immediately to `BookListenScreen`.
4. First reader frame is real content.

If ready state is not available:

1. Do not navigate into `BookListenScreen`.
2. Show a clear bookshelf-level preparing state on the book card or top area.
3. Continue background preparation.
4. Navigate only after ready state exists, or let the user retry when the card becomes ready.

The reader route should never be used as a loading room.

## 5. Explicitly Forbidden

- Do not synchronously parse the whole book when the user taps it.
- Do not enter `BookListenScreen` before ready state exists.
- Do not show loading or restore text inside `BookListenScreen`.
- Do not route to a black or blank reader screen.
- Do not use stable snapshots as fake reader content.
- Do not use overlays, alpha gates, or placeholders to hide jumps.
- Do not use a double-reader swap.
- Do not scroll or align after visible reader content is shown.
- Do not correct paragraph, progress, or playback target when the user taps play.

## 6. Phased Plan

### Phase 5C.1 Docs-Only Architecture

Document the mature reader architecture and stop further snapshot/loading/gate fixes.

### Phase 5C.2 BookDocumentCache Type Boundary

Add type boundaries for parsed book documents and cached readiness state. This should be pure Kotlin and covered by JVM tests.

### Phase 5C.3 Repository / Preload Manager Foundation

Add `BookRepository` and `BookReaderPreloadManager` foundations with injected sources. Keep runtime behavior disabled or read-only until verified.

### Phase 5C.4 Bookshelf Background Preload

Start preloading recent and visible books from the bookshelf layer. Surface lightweight readiness status without blocking clicks or freezing the page.

### Phase 5C.5 Click Uses Cached ReadyState

When a book card has cached ready state, clicking it immediately navigates to the reader with that state. If the state is missing, stay on the bookshelf and show a clear preparing status there.

### Phase 5C.6 BookListenScreen Ready-Only Path

Switch the Formal reader path so `BookListenScreen` only consumes ready state. It must not parse, restore, load, or fake text internally.

### Phase 5C.7 Remove Old Restore / Snapshot Path After Smoke Pass

After repeated real-device smoke passes, remove old restore snapshot traps, route loading rooms, and reader-level restore prompts from the Formal path. Keep legacy fallback only where explicitly needed.

## 7. Acceptance Criteria

- Opening the bookshelf does not freeze.
- Tapping a ready book enters immediately.
- If a book is not ready, the bookshelf clearly shows preparing status instead of entering the reader.
- Once in `BookListenScreen`, the first visible frame is real text.
- Text can be scrolled immediately.
- Tapping play never shows restore-in-progress text.
- Tapping play does not refresh or jump the text.
- Tapping play does not make progress/time jump.
- Playback starts normally.
- Auto advance works normally.
- Highlight follows playback.
- Pause and resume work normally.
- No black screen, crash, or fake reader content.

## 8. Implemented Greenline

Phase 5C.17 through Phase 5C.29 implemented and validated the Formal-only bookshelf preload and ready-state consumption path. The final Formal regression passed at commit `b54b578`, tagged locally as `local-greenline-moonplayer-phase5c29-final-formal-regression-b54b578`.

The validated path uses a ready-state store hit, renders a bounded reader window, skips restore prompts and stable snapshots, keeps seek and playback targets aligned, isolates manual browsing from committed progress, and invalidates stale audio sessions on pause.

The complete result and handoff are archived in `docs/book_reader_phase5c29_final_formal_regression_greenline.md`. The commit and tag remain local; pushed: no.

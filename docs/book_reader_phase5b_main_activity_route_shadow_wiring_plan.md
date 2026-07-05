# Book Reader Phase 5B MainActivity Route Shadow Wiring Plan

## 1. 当前 MainActivity BookListen route 流程

当前小说听书入口从 `BookLibraryScreen` 进入：

```kotlin
onOpenBook = { file ->
    selectedMediaFile = file
    selectedBookUri = file.uri
    navController.navigate(LocalVibeRoute.BookListen)
}
```

`LocalVibeRoute.BookListen` route 内部重新解析当前书本：

```kotlin
val allBookFiles = importedBookFiles
    .plus(folderBookFiles)
    .distinctBy { it.normalizedBookKey() }
val resolvedBookFile = selectedMediaFile
    ?.takeIf { it.type == LocalMediaType.BOOK }
    ?: selectedBookUri?.let { uri ->
        allBookFiles.firstOrNull { it.uri == uri }
    }
```

随后直接调用旧 `BookListenScreen`：

```kotlin
BookListenScreen(
    bookFile = resolvedBookFile,
    initialParagraphIndex = resolvedBookFile?.let { file ->
        bookProgressMap[file.uri]?.paragraphIndex ?: 0
    } ?: 0,
    onProgressChanged = { uri, paragraphIndex, totalParagraphs -> ... },
    onBeforeSpeak = { ... },
    onBack = { navController.popBackStack() }
)
```

当前参数含义：

```text
selectedMediaFile
- 用户点击书本时保存的当前媒体文件。

selectedBookUri
- 用户点击书本时保存的 uri。
- 当 selectedMediaFile 不可用时，用于从 importedBookFiles + folderBookFiles 中重新查找。

resolvedBookFile
- route 层最终传给 BookListenScreen 的 LocalMediaFile?。

initialParagraphIndex
- 只来自 bookProgressMap[file.uri]?.paragraphIndex。
- 不包含 sentenceIndex、chapterSentenceIndex、progress/time、LazyList offset 或 playback seed。

BookListenScreen 调用参数
- bookFile: resolvedBookFile
- initialParagraphIndex: paragraph-only progress fallback
- onProgressChanged: 写回 bookProgressMap 和 AppStateStore
- onBeforeSpeak: 播放小说前暂停音乐
- onBack: popBackStack
```

因此，MainActivity route 当前只提供 book file 与 paragraph-level progress。真实 paragraphs、restore cache、canonical target、LazyList initial target 和 playback target 仍在 `BookListenScreen` 内部晚于首帧生成。

## 2. 未来 shadow wiring 目标

Phase 5B.12 之后的下一步不是直接切换 UI，而是在 MainActivity route 层建立 disabled-by-default / read-only shadow path。

目标：

```text
在 MainActivity BookListen route 层创建 BookReaderEntryReadyRouteInput
用 route-level source 预备 paragraphs / restore cache / readyState
只把结果作为 shadow 日志输出
仍然渲染旧 BookListenScreen
不影响旧 UI、播放、progress、LazyList、snapshot/restore gate
```

shadow wiring 只回答一个问题：

```text
如果未来由 MainActivity route 先准备 BookReaderEntryReadyState，
它算出的 canonical target 是否与旧 BookListenScreen 当前 target / playback target / progress-time 完全一致？
```

日志应对比：

```text
routeShadow.canonicalTarget
routeShadow.progressSnapshot
routeShadow.lazyListInitialIndex / lazyListInitialOffset
routeShadow.playbackSeed
旧 BookListenScreen currentParagraphIndex / currentSentenceIndex / currentChapterSentenceIndex
旧播放 target paragraphIndex / sentenceIndex
旧 progress/time labels
```

这一步不允许让 readyState 驱动 UI。它只是给后续正式切换建立证据。

## 3. 最小接线步骤

### Step 1: 新增 disabled-by-default route shadow flag

建议在 MainActivity 或 book route 附近定义只读 flag：

```kotlin
private const val ENABLE_BOOK_READER_ENTRY_READY_ROUTE_SHADOW = false
```

默认必须是 `false`。首次接线只允许通过显式临时打开 debug 测试，不能默认影响用户路径。

### Step 2: 在 BookListen route 构造 route input

在 `resolvedBookFile` 和 `initialParagraphIndex` 计算后，构造：

```kotlin
val entryReadyInput = BookReaderEntryReadyRouteInput(
    bookFile = resolvedBookFile,
    initialParagraphIndex = resolvedBookFile?.let { file ->
        bookProgressMap[file.uri]?.paragraphIndex ?: 0
    } ?: 0,
    speechRate = 1f
)
```

注意：这一步只创建输入对象，不读取文件、不改变 UI。

### Step 3: 添加 route-level paragraph/restore source adapter

后续实现可创建一个薄 adapter，不直接塞进 `BookListenScreen`：

```text
BookReaderEntryParagraphSource
- 输入 bookId/bookUri
- 调用 TxtBookReader.readParagraphs(context.applicationContext, uri)
- 返回 Result<List<String>>

BookReaderEntryRestoreSource
- 输入 bookId/bookUri
- 调用 BookReaderRestoreCacheSource.load(context.applicationContext, uri)
- 转为 BookReaderEntryRestoreTarget
```

route 层 adapter 只负责喂给 `BookReaderEntryReadyStateLoader`。不要在 MainActivity 中复制 factory 逻辑。

### Step 4: shadow 生成 readyState

当 flag 打开时，用 `LaunchedEffect(resolvedBookFile?.uri, initialParagraphIndex)` 在 route 层 shadow 计算：

```text
BookReaderEntryReadyStateLoader.load(entryReadyInput.toLoadRequest())
```

输出只进入日志，不进入 `BookListenScreen` 参数。

### Step 5: 仍然渲染旧 BookListenScreen

无论 shadow 成功或失败，route 继续执行原调用：

```kotlin
BookListenScreen(
    bookFile = resolvedBookFile,
    initialParagraphIndex = initialParagraphIndex,
    onProgressChanged = ...,
    onBeforeSpeak = ...,
    onBack = ...
)
```

禁止修改：

```text
UI rendering
playback behavior
progress/time display
LazyListState initialization
snapshot/loading/restore gate
```

### Step 6: 只记录对比日志

日志 tag 建议继续使用：

```text
LV_BOOK_FORMAL
```

日志前缀建议：

```text
phase5b route entry ready shadow
```

建议字段：

```text
bookId
routeInitialParagraphIndex
shadowCanonicalParagraphIndex
shadowCanonicalSentenceIndex
shadowCanonicalChapterSentenceIndex
shadowPlaybackParagraphIndex
shadowPlaybackSentenceIndex
shadowProgressValue
shadowProgressMax
shadowListened
shadowRemaining
shadowLazyListInitialIndex
shadowLazyListInitialOffset
shadowLoadSuccess
shadowError
```

与 `BookListenScreen` 内现有 `phase5b read-only entry ready shadow` 日志一起看，才能确认 route-level ready target 与旧 runtime target 是否一致。

## 4. 后续正式切换条件

只有满足以下条件后，才允许进入 ready path 驱动 UI：

```text
shadow target == current target
shadow target == playback target
progress/time 一致
LazyList initial target 与 canonical/playback target 同源
真机无“正在恢复小说内容”提示
播放正常
自动续读正常
紫色高亮跟随正常
暂停/继续正常
连续 smoke 通过
```

建议连续至少两轮：

```text
1. route shadow flag=false，确认旧行为无变化。
2. route shadow flag=true，仅日志打开，确认 UI/播放无变化。
```

然后才允许下一阶段把 `BookReaderEntryReadyState` 传入 `BookListenScreen` 的 read-only consume path。

## 5. 风险

### MainActivity 文件较大，接线必须薄

`MainActivity.kt` 已经承载视频、音乐、小说、路由和状态存储。route shadow 接线必须保持很薄：

```text
只创建 input
只调用 adapter/loader
只打日志
不复制 ready-state factory 逻辑
不新增复杂 UI 分支
```

### 不要把 loader 直接塞进 BookListenScreen

如果继续让 `BookListenScreen` 首帧后自己读取 paragraphs/restore cache，就会回到 Phase 5A 的根因。loader 应在 route-level 或 route wrapper 中准备数据。

### 不要恢复 snapshot/loading 遮挡方案

route shadow 不是新的 snapshot 方案。它不应该产生：

```text
stable snapshot 伪正文
reader body loading
恢复阅读位置提示
整屏 alpha
双层 reader swap
visible 后 scroll/align
```

### 不要让 shadow path 影响真实 UI

shadow path 失败时只能记录日志，不能阻塞旧 `BookListenScreen`。否则会把 read-only 验证变成运行时行为变更。

## 6. 禁止项

本阶段明确禁止：

```text
不改 Kotlin
不接 UI
不安装 APK
不启动 ADB
不提交
不 push
不改变 BookListenScreen 调用参数
不改变播放逻辑
不改变 progress/time
不改变 LazyList
不改 FastSpeech2 / MB-MelGAN / mapper / AudioTrack
不改 auto advance / pause/resume
不改音乐 / 视频模块
```

## 7. 后续执行计划

### Phase 5B.13: route shadow adapter type

新增一个薄 adapter 类型，封装 MainActivity 需要的 paragraph/restore source wiring。

验证：

```text
JVM unit test
不接 MainActivity
不改变运行时行为
```

### Phase 5B.14: MainActivity disabled shadow wiring

在 `LocalVibeRoute.BookListen` 中加入 disabled-by-default shadow path。

验证：

```text
flag=false 时 runtime behavior unchanged
testDebugUnitTest
assembleDebug
git diff --check
```

### Phase 5B.15: debug-only shadow observation

只在 debug/formal 包临时打开 route shadow 日志，真机观察：

```text
route shadow target
BookListenScreen current target
playback target
progress/time
```

不改变 UI。

### Phase 5B.16: ready state drives BookListenScreen only after evidence

只有 shadow 证据连续通过后，才允许让 ready state 驱动 `BookListenScreen` 首帧。

## 8. 验收标准

Phase 5B route shadow wiring 通过条件：

```text
MainActivity route 旧 UI 行为不变
BookListenScreen 旧调用参数不变
shadow readyState 可成功生成
shadow target 与 BookListenScreen current target 一致
shadow target 与 playback target 一致
shadow progress/time 与旧 progress/time 一致
无“正在恢复小说内容”新增问题
播放、自动续读、高亮、暂停继续不回归
```

如果 shadow target 与旧 target 不一致，不允许切换 UI，只能继续分析数据源差异。

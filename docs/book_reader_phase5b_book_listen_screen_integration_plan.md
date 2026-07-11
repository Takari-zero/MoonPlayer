# Book Reader Phase 5B BookListenScreen Integration Plan

## 1. 当前 BookListenScreen 的旧入口问题

### paragraphs 首帧为空

当前 `BookListenScreen` 在 composition 开始时只同步读取 restore cache：

```text
initialReadStateCache = loadBookReadStateCache(...)
```

但正文内容不是同步 ready state。`paragraphs` 初始为：

```text
emptyList()
```

真实正文在 `LaunchedEffect(bookFile?.uri)` 中异步读取：

```text
TxtBookReader.readParagraphs(context.applicationContext, bookFile.uri)
```

因此进入页面第一帧时，`BookListenScreen` 没有真实 `paragraphs` / `chapterSentences`。

### isLoading 首帧为 true

当前 `isLoading` 初始为：

```text
bookFile != null
```

只要进入真实小说文件，首帧就是 loading 状态。后续等待 `TxtBookReader.readParagraphs(...)` 完成后才设置：

```text
paragraphs = loaded
isLoading = false
```

这天然制造了“reader 首帧”和“真实正文 ready”之间的间隙。

### stable snapshot / loading / restore gate 的旧流程

旧流程大致是：

```text
进入 BookListenScreen
-> loadBookReadStateCache 同步读取最近位置/cache
-> paragraphs 仍为空
-> isLoading=true
-> 如果有 cachedVisibleText，走 stable_ui_snapshot
-> 否则显示 loading / 恢复阅读位置
-> paragraphs 异步加载完成
-> 进入 real_content 分支
-> BookListenContent 创建 LazyListState
-> scrollToItem / restore align
-> onPositionSettled
-> snapshot_to_real handoff done
```

这里同时存在三套位置来源：

```text
1. restore cache 的 lastParagraphIndex / lastSentenceIndexInParagraph
2. cachedVisibleText / viewportFirstVisibleItemIndex 代表的 snapshot 窗口
3. real content 中由 paragraphs/chapterSentences 计算出的 currentChapterSentenceIndex
```

播放 target 又是在点击播放时通过 `buildFormalBookReaderPlaybackTarget(...)` 现场构造。

### 为什么旧流程会闪、跳、假正文、播放提示恢复中

旧流程的问题不是单一 UI 动画，而是数据 ready 时序错误：

```text
首帧没有真实正文
snapshot 不是完整真实 reader
real content 晚于首帧挂载
LazyListState 可能先用 snapshot/cached index，再被 real target 修正
播放 target 和 snapshot target 可以不一致
```

具体表现：

```text
闪：snapshot/loading/real_content 交接时，正文区域发生视觉替换。
跳：real content visible 后执行 scroll/align，或播放时 canonical target 生效。
假正文：stable snapshot 显示 cachedVisibleText，但不是可交互真实 reader。
播放提示恢复中：snapshot 分支的播放按钮只 toast “正在恢复小说内容”，不会触发真实播放。
```

## 2. 新接入目标

Phase 5B 的接入目标是让 `BookListenScreen` 首帧消费 `BookReaderEntryReadyState`。

首帧必须已经具备：

```text
paragraphs
chapterSentences
canonical paragraphIndex
canonical sentenceIndex
canonical chapterSentenceIndex
canonical progress/time
LazyList initial index/offset
playback seed
```

正确关系必须是：

```text
first frame visible text target
== LazyList initial target
== progress/time target
== playback seed target
== highlight target
```

用户第一眼看到的必须是真实可交互正文，不再用 snapshot 伪装正文。

## 3. 最小接入步骤

### Step 1: 新增 entry ready state 加载入口

在进入 reader 前增加一个小的 loading boundary。它负责调用已经存在的 loader/factory：

```text
BookReaderEntryReadyStateLoader.load(BookReaderEntryLoadRequest)
```

这个入口应在 `BookListenScreen` 外部完成，不要把“读取文件 + 恢复位置”的等待塞进 reader body 内。

### Step 2: 将旧 TxtBookReader.readParagraphs 逻辑迁移到 loader 外围

保留 `TxtBookReader.readParagraphs(...)` 的真实文件读取能力，但把调用位置从 `BookListenScreen` 内部 `LaunchedEffect` 迁到 entry loader adapter。

adapter 形态建议：

```text
BookReaderEntryParagraphSource:
- 输入 bookId/bookUri
- 调用 TxtBookReader.readParagraphs(...)
- 返回 Result<List<String>>

BookReaderEntryRestoreSource:
- 输入 bookId/bookUri
- 读取 BookReadStateCache / recent record
- 返回 BookReaderEntryRestoreTarget?
```

### Step 3: BookListenScreen 接收 ready state

给 `BookListenScreen` 增加 ready state 消费路径。初期可以并存旧参数，但新路径必须明确：

```text
BookListenScreen(entryReadyState = readyState, ...)
```

进入新路径时：

```text
paragraphs = readyState.paragraphs
chapterSentences = readyState.chapterSentences
currentParagraphIndex = readyState.canonicalTarget.paragraphIndex
currentSentenceIndexInParagraph = readyState.canonicalTarget.sentenceIndex
currentChapterSentenceIndex = readyState.canonicalTarget.chapterSentenceIndex
```

### Step 4: LazyList 初始位置来自 ready state

`BookListenContent` 的初始 LazyList index/offset 必须来自：

```text
readyState.lazyListInitialIndex
readyState.lazyListInitialOffset
```

禁止在 visible 后再用 restore gate 做 `scrollToItem` 修正。

### Step 5: 播放 target 来自 readyState.playbackSeed

首次播放 target 必须和首帧 canonical target 一致。接入时应把 `readyState.playbackSeed` 转换成正式 `BookReaderPlaybackTarget`，或直接用于构造 target。

目标：

```text
playbackSeed.paragraphIndex == canonicalTarget.paragraphIndex
playbackSeed.sentenceIndex == canonicalTarget.sentenceIndex
```

### Step 6: 进度/时间来自 readyState.progressSnapshot

首帧进度和时间显示来自：

```text
readyState.progressSnapshot
```

不能先显示 cached snapshot 的旧 progress，再在播放或 handoff 时切到 real progress。

## 4. 明确禁止

后续接入中不允许继续使用这些方案解决首屏问题：

```text
long snapshot
loading 遮挡
整屏 alpha
双层 reader swap
visible 后 scroll/align
播放时再修正位置
```

也不允许把 snapshot 当成最终交互 reader。snapshot 只可作为旧代码待删除对象，不应成为 Phase 5B 的新依赖。

## 5. 分小步接入计划

### 5B.6 loader wiring adapter

目标：新增 Android/LocalVibe adapter，把真实数据源接到纯 loader。

范围：

```text
TxtBookReader.readParagraphs -> BookReaderEntryParagraphSource
BookReadStateCache / recent record -> BookReaderEntryRestoreSource
BookReaderEntryReadyStateLoader -> ready state
```

验证：

```text
纯单测/必要的 JVM 测试
不接 BookListenScreen
不改变运行时行为
```

### 5B.7 BookListenScreen read-only consume path

目标：给 `BookListenScreen` 增加 ready state 参数和内部消费路径，但先用 feature flag 或未启用入口保护。

要求：

```text
ready state path 能编译
旧入口默认行为不变
不删除旧 snapshot/loading 代码
不改变线上运行路径
```

### 5B.8 remove old snapshot trap

目标：当 ready state path 被启用后，删除或绕开 reader body 内旧 snapshot/loading/restore gate 陷阱。

必须确保：

```text
首帧直接真实 BookListenContent
无 stable snapshot 伪正文
无 “正在恢复小说内容” reader body
无 visible 后 restore scroll/align
```

### 5B.9 first manual smoke

目标：只验证 ready state path 的真实设备体验。

检查项：

```text
第一眼就是正文
正文能立即滑动
点播放不提示恢复中
点播放后正文不刷新、不跳
点播放后进度不跳
播放/自动续读/高亮/暂停继续正常
```

### 5B.10 cleanup old restore gate if safe

目标：确认 ready state path 通过后，再考虑清理旧 restore gate / snapshot handoff 代码。

要求：

```text
只在 manual smoke PASS 后清理
保留必要 fallback
不要影响非 ready state 入口
```

## 6. 验收标准

Phase 5B BookListenScreen integration 只有同时满足以下条件才算通过：

```text
第一眼就是正文
无加载圈 / 无恢复阅读位置
正文立即能滑动
点播放不提示恢复中
点播放后正文不刷新、不跳
点播放后进度不跳
播放、自动续读、高亮、暂停继续正常
```

如果任一项失败，不得继续声明完成，也不得用 snapshot/loading/alpha 等遮挡方案补丁式收口。

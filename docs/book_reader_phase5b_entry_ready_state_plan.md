# Book Reader Phase 5B Entry Ready State Plan

## 1. 当前 clean baseline 数据流

### 从点击小说听书入口到 BookListenScreen 首帧

当前进入小说听书页时，调用方把 `bookFile` 和 `initialParagraphIndex` 传给 `BookListenScreen`。`BookListenScreen` 在 composition 开始时同步读取 `BookReadStateCache`，并用其中的 `lastParagraphIndex` / `lastSentenceIndexInParagraph` 初始化当前阅读位置。

但是正文内容本身不是首帧前准备好的。`paragraphs` 在首帧时是空列表，`isLoading` 初始为 `bookFile != null`。因此首帧不会直接拥有真实的 `paragraphs`、`chapterSentences`、真实 reader content 和真实可交互状态。

### paragraphs 何时加载

`paragraphs` 在 `LaunchedEffect(bookFile?.uri)` 中异步加载：

```text
TxtBookReader.readParagraphs(context.applicationContext, bookFile.uri)
```

加载完成后才执行：

```text
paragraphs = loaded
isLoading = false
```

这意味着 `BookListenScreen` 的首帧和真实正文 ready 之间存在时间差。当前 Phase 5A 的 flicker / loading / snapshot 问题都发生在这个时间差里。

### saved restore position 何时读取

`saved restore position` 通过 `loadBookReadStateCache(...)` 在 composition 初始阶段同步读取，并生成：

```text
initialReadStateCache
restorePositionKey
hasSavedRestorePosition
cachedReadState
hasInitialReaderSnapshot
```

它包含 paragraph/sentence/progress/cache window，但不等于完整真实正文数据。

### canonical target 何时计算

当前 baseline 没有一个独立的 entry-ready canonical target。实际存在多套目标：

```text
1. initialReadStateCache.lastParagraphIndex / lastSentenceIndexInParagraph
2. cachedReadState.cachedVisibleText 对应的 snapshot visible window
3. real content 分支中由 paragraphs/chapterSentences 计算出的 currentChapterSentenceIndex
4. 点击播放时 buildFormalBookReaderPlaybackTarget(...) 使用的 currentParagraphIndex/currentSentenceIndexInParagraph
```

真正用于播放的 target 只有在点击播放时才由 `buildFormalBookReaderPlaybackTarget(...)` 生成。

### LazyListState 何时创建

`LazyListState` 在 `BookListenContent` 内创建：

```text
rememberLazyListState(initialFirstVisibleItemIndex, initialFirstVisibleItemScrollOffset)
```

`BookListenContent` 可能被 snapshot 分支创建，也可能被 real content 分支创建。snapshot 与 real content 不是同一个真实 reader 数据源。

real content 分支的初始 index 当前来自：

```text
snapshotMappedRealIndex
或 cachedReadState.viewportFirstVisibleItemIndex
或 defaultInitialSentenceListIndex = currentChapterSentenceIndex - 1
```

这使得 LazyList 初始位置可能与播放 target 不完全一致。

### 播放 target 何时生成

播放 target 在 `speakCurrentSentence()` 中生成：

```text
buildFormalBookReaderPlaybackTarget(
  paragraphIndex = currentParagraphIndex,
  sentenceIndex = currentSentenceIndexInParagraph,
  fallbackSentenceText = currentSpeakText()
)
```

因此播放 target 依赖当前 runtime state，而不是 snapshot window。若首帧显示的是 stale cached viewport，点击播放时就会发生正文/进度向真实 target 对齐的跳变。

## 2. Phase 5B 正确目标

进入 `BookListenScreen` 第一帧时，必须已经有一个完整、可交互、统一坐标系的 ready state：

```text
paragraphs
chapterSentences
canonical paragraphIndex
canonical sentenceIndex
canonical chapterSentenceIndex
canonical progress/time
LazyList initial index/offset
playback target
```

`BookListenScreen` 只消费 ready state，不再在首帧后自己恢复正文位置。

正确状态应满足：

```text
first frame text target == LazyList initial target == progress/time target == playback target == highlight target
```

首帧不能依赖：

```text
cached snapshot window
loading overlay
visible 后 scrollToItem 修正
播放时再修正 current sentence
```

## 3. 最小可实施方案

### 新增边界

新增 `BookReaderEntryState` / `BookReaderEntryLoader` 或等价结构。

建议职责：

```text
BookReaderEntryState
- bookFile
- paragraphs
- chapters
- chapterTitle
- chapterSentences
- canonicalParagraphIndex
- canonicalSentenceIndex
- canonicalChapterSentenceIndex
- canonicalProgressValue
- canonicalProgressMaxValue
- listenedTimeLabel
- remainingTimeLabel
- lazyListInitialIndex
- lazyListInitialOffset
- playbackTargetSeed

BookReaderEntryLoader
- 读取 BookReadStateCache / recent record
- 读取 paragraphs
- detect chapters
- 计算 canonical target
- 计算 chapterSentences
- 计算 LazyList initial index/offset
- 计算 progress/time
- 返回 ready state
```

### 在进入听书页前准备数据

Phase 5B 不应让 `BookListenScreen` 自己在首帧之后异步恢复正文位置。调用方或路由层需要先准备 ready state，再进入真实 reader。

推荐流程：

```text
点击听书入口
-> 创建/请求 BookReaderEntryLoader
-> 读取 paragraphs + cache/recent
-> 计算 canonical target
-> 生成 BookReaderEntryState.Ready
-> 进入 BookListenScreen(entryState)
-> 首帧直接渲染真实 BookListenContent
```

如果准备期间需要 UI 反馈，应发生在进入 reader 之前的外层页面，而不是在 reader body 内显示 spinner / 恢复阅读位置。

### BookListenScreen 消费 ready state

`BookListenScreen` 接收 ready state 后：

```text
paragraphs = entryState.paragraphs
chapterSentences = entryState.chapterSentences
currentParagraphIndex = entryState.canonicalParagraphIndex
currentSentenceIndexInParagraph = entryState.canonicalSentenceIndex
currentChapterSentenceIndex = entryState.canonicalChapterSentenceIndex
LazyList initial index = entryState.lazyListInitialIndex
LazyList initial offset = entryState.lazyListInitialOffset
progress/time = entryState.canonical progress/time
playback target = entryState canonical target
```

播放、进度、高亮、LazyList 初始位置全部来自同一个 canonical target。

## 4. 风险边界

Phase 5B 只处理 reader entry ready state，不碰播放主链路和 TTS 引擎。

明确不改：

```text
FastSpeech2
MB-MelGAN
mapper
AudioTrack
auto advance
pause/resume
音乐模块
视频模块
模型文件
```

也不再使用：

```text
stable snapshot 长时间占位
loading / 恢复阅读位置遮挡
整屏 alpha
双层 reader swap
visible 后 scroll/align
播放时再修正位置
```

## 5. 分阶段实施计划

### Phase 5B.1 docs-only

- [x] 记录当前 clean baseline 的真实数据流。
- [x] 明确 Phase 5B 的正确架构方向。
- [x] 明确禁止继续使用 snapshot/loading/gate 遮挡方案。

### Phase 5B.2 type/readiness boundary

- [ ] 新增 entry ready state 类型。
- [ ] 定义 canonical target 字段。
- [ ] 增加纯单元测试，验证 entry state 能表达 paragraph/sentence/chapterSentence/progress/LazyList/playback target。

### Phase 5B.3 entry loader

- [ ] 新增 loader，负责读取 paragraphs 和 saved position。
- [ ] loader 内计算 chapters/chapterSentences/canonical target。
- [ ] 增加单元测试：有 saved position 时，loader 输出的 lazyListInitialIndex 和 playback target 指向同一句。
- [ ] 增加 stale cached viewport 测试：cached window 不得覆盖 canonical target。

### Phase 5B.4 BookListenScreen consume ready state

- [ ] 让 `BookListenScreen` 消费 ready state。
- [ ] 首帧直接创建真实 `BookListenContent`。
- [ ] `LazyListState` 初始 index/offset 来自 ready state。
- [ ] 播放按钮 target 来自同一个 ready state/current state。

### Phase 5B.5 remove old snapshot trap

- [ ] 移除 reader body 内的长时间 stable snapshot 兜底。
- [ ] 移除 reader body 内“恢复阅读位置”遮挡路径。
- [ ] 禁止 real content visible 后再做 restore scroll/align。
- [ ] 保留必要日志，用于确认 first frame / play target / progress target 一致。

### Phase 5B.6 manual smoke

- [ ] 安装 Formal App。
- [ ] 首次进入同一本小说听书页。
- [ ] 录屏确认第一眼是真实正文。
- [ ] 点播放确认正文不刷新、不跳，进度不突变。
- [ ] 验证播放、自动续读、高亮、暂停/继续不回归。

## 6. 验收标准

Phase 5B 必须同时满足：

```text
进入听书页第一眼就是真实正文
无 loading / 无恢复阅读位置
正文立即可滑动
点播放不提示恢复中
点播放后正文不刷新、不跳
点播放后进度不突然跳
播放、自动续读、高亮、暂停/继续不回归
```

如果不能同时满足，不应报告 Phase 5B 通过。

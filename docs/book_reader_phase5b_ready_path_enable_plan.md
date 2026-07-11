# Book Reader Phase 5B.15 Ready Path Enable Plan

## 0. Hard Rule

目标不是“看起来不闪”。

目标是：进入小说听书页的第一帧，就必须是真实、可交互、位置正确的正文。

绝对禁止再用这些方式解决：

```text
不允许用 snapshot 假正文覆盖
不允许用 loading / “恢复阅读位置”遮挡
不允许用 alpha / overlay / placeholder 遮住真实跳动
不允许先显示一个位置，再 scroll/align 跳到另一个位置
不允许双层 reader swap
不允许进入页面后再修正位置
不允许点击播放时才校正 target / progress
不允许显示正文但不能滑动
不允许显示正文但点击播放提示“正在恢复小说内容”
```

正确方向只有一个：

```text
BookReaderEntryReadyRoute 先准备 readyState
readyState 完整后才调用真正的 BookListenScreen ready consume path
BookListenScreen 首帧直接渲染真实 BookListenContent
```

如果 ready state 没准备好，不要进入真实 `BookListenScreen`。可以在 route / 上一级入口等待 ready，但一旦进入听书页，就必须已经 ready。

## 1. Current State

目前已具备的基础：

```text
BookReaderEntryReadyState
BookReaderEntryReadyStateFactory
BookReaderEntryReadyStateLoader
BookReaderEntryReadyStateWiringAdapter
BookReaderRestoreCacheSource
BookReaderEntryReadyRoute skeleton
MainActivity route shadow wiring disabled-by-default
BookListenScreen read-only shadow logging
```

当前还没有做的事：

```text
BookListenScreen 还没有真正接收 readyState
MainActivity 还没有在进入 reader 前等待 readyState
旧 BookListenScreen 仍然自己加载 paragraphs / restore cache
旧 snapshot/loading/restore gate 仍然存在
```

Phase 5B.15 只制定接入计划，不实现 Kotlin。

## 2. Ready Path Goal

进入 `BookListenScreen` 第一帧时，必须已经具备：

```text
paragraphs 已加载
chapterSentences 已生成
canonical paragraphIndex 已确定
canonical sentenceIndex 已确定
canonical chapterSentenceIndex 已确定
canonical progress/time 已确定
LazyList initial index/offset 已确定
playback target 已确定
```

并且这些目标必须同源：

```text
first visible reader text target
== LazyList initial target
== progress/time target
== playback target
== highlight initial target
```

进入页面后必须立即满足：

```text
正文能上下滑动
点击播放能立刻播放
不提示“正在恢复小说内容”
点播放后正文不刷新、不跳
点播放后进度不突然跳
高亮、自动续读、暂停/继续正常
```

## 3. Target Architecture

### Route-level ready flow

目标流程：

```text
用户点击小说
-> MainActivity 保存 selectedMediaFile / selectedBookUri
-> 导航到 BookListen route
-> BookReaderEntryReadyRoute 创建 input
-> route-level loader 读取 paragraphs + restore/cache
-> BookReaderEntryReadyStateLoader 生成 readyState
-> readyState 成功后才渲染 BookListenScreen ready path
-> BookListenScreen 首帧直接渲染真实 BookListenContent
```

### BookListenScreen ready consume path

`BookListenScreen` 需要新增明确的 ready consume path。建议形态：

```kotlin
@Composable
fun BookListenScreen(
    bookFile: LocalMediaFile?,
    initialParagraphIndex: Int,
    entryReadyState: BookReaderEntryReadyState? = null,
    onProgressChanged: (String, Int, Int) -> Unit,
    onBeforeSpeak: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
)
```

约束：

```text
entryReadyState == null:
- 保持旧路径，行为不变。

entryReadyState != null:
- 不走旧 isLoading 首帧。
- 不走 stable snapshot 伪正文。
- 不走 reader body “恢复阅读位置”提示。
- 不在 visible 后 restore scroll/align。
- 首帧 state 全部来自 entryReadyState。
```

### Route waits before entering true reader

如果 `BookReaderEntryReadyState` 尚未准备好：

```text
不要调用 BookListenScreen ready path
不要显示 reader body 内 loading
不要显示 reader body 内 snapshot
```

可接受的等待位置：

```text
BookListen route 外层
上一级 route-level boundary
非 reader body 的轻量等待状态
```

但验收时，一旦用户看到听书页真实 reader，必须已经 ready。

## 4. Minimal Future Files to Change

### `app/src/main/java/com/shenghui/localvibe/MainActivity.kt`

职责：

```text
在 BookListen route 内持有 route-level ready state
调用 route-level loader
ready 后把 entryReadyState 传给 BookListenScreen
旧 flag 默认保护
```

禁止：

```text
不要在 MainActivity 复制 factory 逻辑
不要塞复杂 UI
不要影响音乐/视频模块
```

### `app/src/main/java/com/shenghui/localvibe/feature/book/BookReaderEntryReadyRoute.kt`

职责：

```text
扩展 route state:
- Idle / Preparing / Ready / Failed

提供 route-level decision:
- Ready 才允许进入 BookListenScreen ready path
- Preparing/Failed 不产生 fake reader content
```

### `app/src/main/java/com/shenghui/localvibe/feature/book/BookListenScreen.kt`

职责：

```text
新增 entryReadyState consume path
ready path 初始 paragraphs / chapterSentences / current target / progress / playback seed 全部来自 readyState
保持旧路径作为 fallback
```

禁止：

```text
不要继续修 snapshot/loading/gate
不要用 alpha/overlay/crossfade
不要 visible 后 scroll/align
不要播放时才修正 target
```

### Tests

建议新增或扩展：

```text
app/src/test/java/com/shenghui/localvibe/feature/book/BookReaderEntryReadyRouteTest.kt
app/src/test/java/com/shenghui/localvibe/feature/book/BookListenScreenEntryReadyStateContractTest.kt
app/src/test/java/com/shenghui/localvibe/feature/book/playback/BookReaderEntryReadyStateLoaderTest.kt
```

## 5. Phased Implementation Plan

### Phase 5B.16: Ready route state contract

目标：先扩展 route state，表达“未 ready 不进入 reader”。

测试应验证：

```text
Preparing 不产生 ready content
Failed 不产生 ready content
Ready 才携带 BookReaderEntryReadyState
Ready slot 不暴露 stable snapshot
```

运行：

```powershell
.\gradlew.bat :app:testDebugUnitTest --no-daemon --max-workers=1 --console=plain -Dorg.gradle.jvmargs="-Xmx6144m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8" -Dkotlin.daemon.jvm.options="-Xmx6144m"
```

### Phase 5B.17: BookListenScreen ready-state contract tests

目标：先用纯 contract/helper 测试定义 ready path 的初始化约束，不直接重构 UI。

测试应验证：

```text
readyState.paragraphs -> initial paragraphs
readyState.canonicalTarget -> current paragraph/sentence/chapterSentence
readyState.progressSnapshot -> initial progress/time
readyState.lazyListInitialIndex/Offset -> initial LazyList target
readyState.playbackSeed -> initial playback target
```

必须验证：

```text
firstFrame target == play target
progress target == play target
LazyList target == play target
```

### Phase 5B.18: Add BookListenScreen ready parameter without enabling route

目标：给 `BookListenScreen` 增加 nullable `entryReadyState` 参数，但 MainActivity 暂不传。

约束：

```text
默认 null，旧行为必须完全不变
不删除旧 snapshot/loading/restore gate
不改播放逻辑
```

验证：

```text
testDebugUnitTest PASS
assembleDebug PASS
git diff --check PASS
```

### Phase 5B.19: Implement ready consume initialization

目标：当 `entryReadyState != null` 时，BookListenScreen 初始状态全部从 readyState 派生。

实现要点：

```text
paragraphs 初始值 = entryReadyState.paragraphs
currentParagraphIndex 初始值 = entryReadyState.canonicalTarget.paragraphIndex
currentSentenceIndexInParagraph 初始值 = entryReadyState.canonicalTarget.sentenceIndex
currentChapterSentenceIndex 初始值 = entryReadyState.canonicalTarget.chapterSentenceIndex
progress/time 初始值 = entryReadyState.progressSnapshot
LazyList initial index/offset = entryReadyState.lazyListInitialIndex/lazyListInitialOffset
playback initial seed = entryReadyState.playbackSeed
isLoading 初始值 = false
hasSettledInitialReaderPosition 初始值 = true
hasCompletedSnapshotToRealHandoff 初始值 = true
```

禁止：

```text
ready path 不允许显示 stable_ui_snapshot
ready path 不允许显示 “正在恢复小说内容”
ready path 不允许 visible 后 restore scroll/align
ready path 不允许播放时校正 current target
```

### Phase 5B.20: Route-level ready loading behind debug-only flag

目标：MainActivity route 在 flag 打开时先准备 readyState，再把它传入 BookListenScreen。

建议 flag：

```kotlin
private const val ENABLE_BOOK_READER_ENTRY_READY_ROUTE = false
```

初始必须 false。

当 flag=true：

```text
BookListen route 创建 BookReaderEntryReadyRouteInput
route-level loader 读取 paragraphs/cache
ready 后调用 BookListenScreen(entryReadyState = readyState)
ready 前不调用真实 reader ready path
```

当 flag=false：

```text
继续旧 BookListenScreen(bookFile, initialParagraphIndex, ...)
```

### Phase 5B.21: Remove ready-path snapshot trap only after tests pass

目标：只在 ready path 内绕开旧 snapshot/loading/gate，不影响旧 fallback path。

必须保留：

```text
旧 path 可继续作为 fallback
ready path 单独验证
```

不要一次性删除所有旧逻辑。

### Phase 5B.22: Manual smoke

只在自动验证通过后安装 Formal App。

手测清单：

```text
进入听书页第一眼就是正确真实正文
无 loading / 无恢复阅读位置
正文立即能滑动
点播放不提示恢复中
点播放后正文不刷新、不跳
点播放后进度不突然跳
播放较快出声
声音清楚
自动续读 3-5 句正常
紫色高亮跟随
暂停正常
继续正常
无语音包误跳
无黑屏 / 闪退 / 卡死
```

日志必须确认：

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

## 6. Validation Commands

每个 Kotlin 阶段必须跑：

```powershell
git status --short

python -c "from pathlib import Path; files=[Path(r'app/src/main/java/com/shenghui/localvibe/MainActivity.kt'), Path(r'app/src/main/java/com/shenghui/localvibe/feature/book/BookListenScreen.kt')]; [p.read_bytes().decode('utf-8') for p in files if p.exists()]; print('utf-8 decode: PASS')"

.\gradlew.bat :app:testDebugUnitTest --no-daemon --max-workers=1 --console=plain -Dorg.gradle.jvmargs="-Xmx6144m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8" -Dkotlin.daemon.jvm.options="-Xmx6144m"

.\gradlew.bat :app:assembleDebug --no-daemon --max-workers=1 --console=plain -Dorg.gradle.jvmargs="-Xmx6144m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8" -Dkotlin.daemon.jvm.options="-Xmx6144m"

git diff --check
```

真机阶段必须使用指定设备：

```powershell
E:\Android\platform-tools\adb.exe -s GEY5UCAMQ8A6YPB6 ...
```

除非用户明确批准，不启动 ADB、不安装 APK。

## 7. Stop Conditions

出现以下任一情况，必须停止，不要继续补丁式修 UI：

```text
readyState 没准备好就进入 BookListenScreen ready path
BookListenScreen ready path 首帧 paragraphs 为空
ready path 仍显示 stable snapshot
ready path 仍显示 loading / 恢复阅读位置
ready path visible 后仍 scroll/align
点击播放后 target/progress 改变
显示正文但不能滑动
点击播放提示“正在恢复小说内容”
```

这些不是视觉小 bug，而是 ready path 架构失败。

## 8. Success Criteria

Phase 5B ready path 只有在以下全部成立时才算通过：

```text
进入听书页第一眼就是真实正文
正文位置正确
正文立即可滑动
点击播放能立刻播放
不提示“正在恢复小说内容”
点播放后正文不刷新、不跳
点播放后进度不突然跳
播放、自动续读、高亮、暂停/继续不回归
无 TFLite buffer error
无 Formal crash
```

不能用“看起来不闪”替代这些条件。

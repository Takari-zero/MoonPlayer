# LocalVibe / Moon播放器 模块技术栈

## 视频模块技术栈

- Kotlin
- Jetpack Compose
- Media3 ExoPlayer
- MediaStore
- DataStore
- ActivityResult 文件 / 权限选择

视频模块以本地视频扫描、文件夹管理和横屏播放为核心。播放器页面可以创建面向当前视频的 ExoPlayer 实例，但不得把视频播放迁移到音乐后台 Service。

## 音乐模块技术栈

- Kotlin
- Jetpack Compose
- Media3 ExoPlayer
- Media3 MediaSessionService
- MediaStore
- Notification / MediaSession
- DataStore

音乐模块以后台播放为核心。`MusicPlaybackService` 持有全局 ExoPlayer 和 MediaSession，负责播放队列、通知栏、锁屏控制和播放模式。`AudioPlayerScreen` 只连接和控制 Service，不应创建独立播放器。

## 小说 / TTS 模块技术栈

- Kotlin
- Jetpack Compose
- Storage Access Framework
- Android TextToSpeech
- DataStore
- 自定义 TXT reader

小说模块只展示用户主动导入的 TXT。第一阶段按段落切分并支持听书进度保存；章节识别、后台朗读和更复杂的书源能力后续单独评估。

## 设置 / 数据管理技术栈

- Jetpack Compose
- DataStore
- 系统权限 Intent
- 本地状态清理

设置页只管理本地数据和本地权限提示，不扩展账号、云备份、会员或广告配置。

## 扫描 / 文件访问策略

- 媒体自动发现优先使用 MediaStore。
- 用户主动选择文件或目录时使用 Storage Access Framework。
- 不申请 `MANAGE_EXTERNAL_STORAGE`。
- 删除真实文件必须依赖系统允许的 URI 删除或系统删除授权流程。
- 删除失败时必须保留原始数据，不得误删 UI 状态。
- 隐藏 / 移除列表项应写入本地隐藏记录，不等同于删除真实文件。

## 状态持久化策略

- 使用 DataStore Preferences。
- 先保持轻量，不引入 Room。
- 视频保存播放进度、最近播放、隐藏视频 URI、隐藏视频文件夹 ID。
- 音乐保存隐藏音频 URI、最近音频 URI、必要的播放模式状态；音乐不保存播放进度。
- 小说保存导入记录和听书进度。
- 清理数据必须按模块明确，不做跨模块顺手清理。

## 开发辅助工具

### Codex

用于代码阅读、任务拆分、实现、验证和文档维护。每轮任务必须明确模块、范围和禁止项。

### CodeGraph

用于本地代码索引、影响面分析、函数调用关系检查。修改共享文件前优先用 CodeGraph 确认调用方和影响范围。

### GitHub

用于仓库、提交、PR、issue 管理。不要在未确认修改范围和验证结果前提交或 push。

### Superpowers

用于项目规划、技术栈梳理、任务拆分、调试流程和开发纪律。使用前要说明用途。

### Product Design

用于 UI 方案、页面效果图和交互风格探索。设计产物不能未经确认直接混入业务代码。

### Figma

用于设计稿整理和后续 UI 交付。当前 Android 本地播放器开发不依赖 Figma，但需要设计沉淀时可使用。

### Linear

用于任务排期、Bug 列表和迭代管理。个人开发阶段可选用。

### Vercel

主要用于 Web 部署。当前 Android 项目暂少用。

### Atlassian Rovo

用于团队文档和知识库。个人项目暂少用。

### Gradle

用于 Android 编译验证。业务代码开发轮次必须运行：

```powershell
.\gradlew.bat :app:assembleDebug
```

docs-only 轮次不需要编译，但必须确认只改文档。

### 真机安装验证

涉及业务代码、权限、播放、扫描、删除、TTS、通知栏或锁屏控制的轮次，编译成功后应尽量进行真机验证。无法真机验证时必须明确说明。

# LocalVibe 自动任务队列

## 当前模块：视频模块

### TODO-001 自动化 workflow 验证

状态：doing

目标：
验证本地 workflow 脚本是否能正常：

* 检查 git clean
* 读取 state.json
* 生成 current-task.md
* 执行 Android 编译 / 安装 / 启动检查

允许修改：

* .workflow/**
* AGENTS.md

禁止修改：

* app/**
* gradle/**
* build.gradle
* settings.gradle
* AndroidManifest.xml

验收：

* git-guard.ps1 可运行
* android-check.ps1 可运行
* run-next.ps1 可生成 .workflow/current-task.md
* 不自动提交
* 不自动 push

是否需要真机确认：否

---

### TODO-002 均衡器文档同步

状态：todo

目标：
同步记录系统均衡器面板已完成，包括：

* Equalizer
* BassBoost
* Virtualizer
* 半透明面板
* 横向细滑杆
* 4dp 轨道
* 16dp 圆形 thumb
* 一页优先布局
* ADB 62001 测试方式

允许修改：

* docs/DEVELOPMENT_PITFALLS.md
* docs/PROJECT_ROADMAP.md
* docs/MODULE_TECH_STACK.md

禁止修改：

* app/**
* gradle/**
* AndroidManifest.xml

是否需要真机确认：否

---

### TODO-003 新弹窗 UI 设计提醒

状态：todo

目标：
后续开发新的弹窗 / 面板 / 复杂 UI 前，不直接进入代码实现。
必须先提醒用户：
“先用 Stitch / Product Design 生成 UI 效果图，确认后再让 Codex 实现。”

允许修改：

* .workflow/queue.md
* AGENTS.md

禁止修改：

* app/**

是否需要真机确认：否

---

### TODO-004 画面调节真实第一版

状态：todo

目标：
做播放器画面调节真实第一版前，必须先走 UI 设计流程。
先提醒用户使用 Stitch / Product Design 设计弹窗或面板。
确认效果图后再写 Codex 实现任务。

初步功能方向：

* 亮度
* 对比度
* 饱和度
* 色温
* 恢复默认

要求：
先调研低风险实现方式，不改系统亮度，不假成功。

是否需要真机确认：是

---

### TODO-005 FFmpeg 音频扩展样本验证

状态：blocked

阻塞原因：
缺少 AC3 / DTS / EAC3 / TrueHD 样本。
GPL-3.0 风险未确认。

实验分支：
experiment/ffmpeg-audio-decoder

本地实验 commit：
d1b2022

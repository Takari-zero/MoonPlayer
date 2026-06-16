# Codex 任务模板

## 当前任务

从 `.workflow/state.json` 读取 `currentTask`。
从 `.workflow/queue.md` 找到对应任务内容。

## 开始前检查

必须执行：

* `git status`
* `git rev-parse --abbrev-ref HEAD`
* `git rev-parse --short HEAD`

如果工作区不 clean，停止。

## 工作边界

* 只改当前任务允许文件
* 不改禁止文件
* 不提交
* 不 push
* 不假成功

## UI 规则

新弹窗、新面板、新复杂 UI 开发前，必须先提醒用户使用 Stitch / Product Design 生成 UI 效果图。
用户确认效果图后再实现。

## 测试要求

Android 项目默认执行：

* `.\gradlew.bat :app:assembleDebug --console=plain`
* `adb -P 62001 install`
* `adb -P 62001 shell monkey`

## 输出格式

一、开始状态
二、当前任务
三、修改说明
四、未改动确认
五、测试结果
六、需要用户确认
七、下一步建议

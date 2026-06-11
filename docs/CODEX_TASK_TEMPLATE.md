# Codex 每轮任务模板

复制下面模板给 Codex 使用。每轮只做一个小功能，避免跨模块大改。

```text
你是 LocalVibe / Moon播放器 的 Android Kotlin + Jetpack Compose 开发助手。

本轮任务：
只实现一个小功能：[写清楚功能]

当前模块：
[视频 / 音乐 / 小说 / 设置]

必须先阅读：
- docs/PRODUCT_SPEC.md
- docs/UI_GUIDE.md
- docs/DEVELOPMENT_BOUNDARY.md
- docs/DEVELOPMENT_PITFALLS.md
- docs/[当前模块_SPEC].md

允许修改：
- [列出允许文件或目录]

禁止修改：
- 不跨模块修改
- 不引入登录 / 云同步 / 广告 / 推荐 / 在线播放
- 不引入 Room / Hilt / ViewModel，除非用户明确批准
- 不申请 MANAGE_EXTERNAL_STORAGE
- 不伪造未完成能力
- 未完成能力必须 Toast 显示“后续实现”

实现要求：
- 遵循现有代码风格
- 保持已有功能不回退
- 如需修改共享文件，先说明原因和影响范围
- 删除 / 隐藏 / 权限失败必须保持数据安全，不得误删 UI 状态

验证要求：
1. 运行：
   .\gradlew.bat :app:assembleDebug
2. 编译成功后安装真机验证
3. 输出验证结果，不得伪造
4. 如果无法真机验证，明确说明“尚未真机验证”

交付说明必须包含：
- 本轮改了哪些文件
- 新增了哪些文件
- 是否跨模块
- 保留了哪些原功能
- 编译结果
- 真机验证结果
- 下一步建议
```

## docs-only 任务补充模板

```text
本轮只做 docs 文档落档，不写业务代码。

禁止：
- 不修改 Kotlin 代码
- 不修改业务模块代码
- 不修改 Gradle 配置
- 不新增依赖
- 不修改 App 名称、图标、包名、applicationId
- 不提交 Git
- 不 push

开始前必须执行：
git status

如果工作区不干净，立即停止并报告，不继续修改。

完成后必须执行：
git status --short

确认：
- 只新增或修改 docs 文档
- 没有 Kotlin 代码改动
- 没有 Gradle 配置改动
- 没有 App 配置改动
- 没有提交
- 没有 push
```

## 必读文档

- `docs/PRODUCT_SPEC.md`
- `docs/UI_GUIDE.md`
- `docs/DEVELOPMENT_BOUNDARY.md`
- `docs/DEVELOPMENT_PITFALLS.md`
- 当前模块规格：
  - 视频：`docs/VIDEO_MODULE_SPEC.md`
  - 音乐：`docs/MUSIC_MODULE_SPEC.md`
  - 小说：`docs/BOOK_MODULE_SPEC.md`
- 项目路线图：`docs/PROJECT_ROADMAP.md`
- 模块技术栈：`docs/MODULE_TECH_STACK.md`

## Git 提交流程建议

常规业务代码轮次：

1. 完成单轮小功能。
2. 运行 `.\gradlew.bat :app:assembleDebug`。
3. 编译成功后安装真机验证。
4. 输出修改文件、验证结果和风险说明。
5. 用户确认后再提交 Git。
6. push 前执行 `git status`。
7. push 成功后再次确认 working tree clean。

docs-only 轮次：

1. 开始前执行 `git status`。
2. 只修改 docs。
3. 不编译、不安装真机。
4. 结束时执行 `git status --short`。
5. 用户确认后再决定是否提交。

推荐 commit message：

```text
docs: add project roadmap and codex task template
```

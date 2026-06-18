# Moon播放器开发避坑记录

这份文档不是产品说明书，也不是 UI 指南。它用于记录 Moon播放器 开发过程中已经踩过的坑、容易误判的问题、测试注意事项和 Git 提交流程风险。

后续每次开发前，尤其是修改视频播放页、视频主页、视频文件夹详情页、DataStore、删除 / 隐藏逻辑或提交 Git 前，都应该先阅读本文件。

如果本文件与 `PRODUCT_SPEC.md`、`UI_GUIDE.md`、`DEVELOPMENT_BOUNDARY.md` 或模块专项文档冲突，以产品锚点文档和模块专项文档为准。

## 一、总原则

### 每轮只改一个模块

- 问题：一次同时改视频、音乐、小说和设置，容易产生联动 bug。
- 风险：一个小 UI 调整可能影响共享状态、导航或持久化逻辑，排查成本会迅速变高。
- 避免方式：每轮明确范围，只处理一个模块；共享文件只有在当前模块确实需要时才修改，并说明影响范围。

### 不要跨模块顺手优化

- 问题：视频模块收口时顺手改音乐或小说 UI。
- 风险：用户测试焦点被打散，已稳定功能可能回退。
- 避免方式：视频轮只改视频，音乐轮只改音乐，小说轮只改小说。除非用户明确要求，否则不跨模块。

### 不要擅自提交或 push

- 问题：还没经过用户验收就提交 Git。
- 风险：把未验证改动、截图、临时文件或其它残留一起提交。
- 避免方式：用户明确要求提交前，不执行 commit 或 push。每次提交前先检查工作区。

### 不要默认使用 `git add .`

- 问题：工作区经常存在多轮残留改动。
- 风险：会把无关代码、截图、缓存、测试产物一起提交。
- 避免方式：只按本轮范围精确暂存文件。只有确认工作区完全干净且只有本轮改动时，才考虑批量暂存。

### 提交前必须检查工作区

- 问题：忽略未跟踪文件或其它模块改动。
- 风险：提交内容和用户要求不一致。
- 避免方式：提交前必须执行 `git status`，并确认 `.kotlin/`、`test-results/`、截图、临时文件不会进入提交。

### 未实现功能必须明确反馈

- 问题：按钮点击无反应，或者把占位功能做成假成功。
- 风险：用户以为功能已完成，后续测试会误判。
- 避免方式：未实现功能必须 Toast：“后续实现”。不能无反应，也不能假装执行成功。

### 修改共享文件必须说明影响范围

- 问题：`MainActivity.kt`、`AppStateStore.kt`、`FolderScreen.kt` 是共享入口或共享页面。
- 风险：视频需求可能影响音乐、小说、设置或持久化恢复。
- 避免方式：修改共享文件时说明可能影响哪些模块，并在测试报告中单独列出风险。

## 二、视频播放页踩坑

### 画面比例文案乱码

- 问题：画面比例弹窗、选项或 Toast 出现乱码、英文和中文混用。
- 风险：用户无法确认当前画面比例模式。
- 避免方式：所有画面比例相关文案统一为：`适应`、`填充`、`缩放`、`拉伸`。修复文案时不要改播放逻辑。

### 控制层自动隐藏误判

- 问题：自动化测试时以为按钮打不开，实际是顶部栏 / 底部栏自动隐藏了。
- 风险：误判功能失败，甚至错误修改播放器逻辑。
- 避免方式：测试播放器按钮前，先点击视频画面唤出控制层，并立刻点击目标按钮。控制层自动隐藏是正常播放器行为，不要随便注释掉。

### 临时调整自动隐藏要恢复

- 问题：为了截图或自动化测试，临时延长控制层显示时间。
- 风险：测试结束后忘记恢复，导致播放器体验变差。
- 避免方式：只有用户明确同意时才临时调整；测试完成后必须恢复原自动隐藏逻辑。

### 画面比例按钮交互变更

- 问题：旧逻辑是打开弹窗，新逻辑是底部按钮点击后循环切换。
- 风险：后续开发误把旧弹窗交互恢复。
- 避免方式：当前策略是按 `适应 -> 填充 -> 缩放 -> 拉伸 -> 适应` 循环切换，并在视频中央短暂显示当前模式文字。不要恢复旧弹窗，除非用户明确要求。

### 返回按钮卡顿

- 问题：点击左上角返回按钮有明显延迟。
- 风险：用户会感觉播放器卡死或返回无效。
- 避免方式：返回点击必须优先触发页面返回，不要等待播放器释放、DataStore 写入或动画结束。播放进度保存和播放器释放应避免同步阻塞 UI，同时不能破坏进度恢复。

### 倍速显示长小数

- 问题：倍速显示为 `1.5363582x`、`1.500000x` 等长小数。
- 风险：界面显得粗糙，也影响用户理解当前速度。
- 避免方式：倍速最多显示 2 位小数，并去掉无意义的 0。示例：`0.25x`、`0.5x`、`1x`、`1.05x`、`1.25x`、`1.5x`、`2x`、`3x`、`5x`。

### 倍速面板不要重写播放器核心

- 问题：为了改 UI，重写 ExoPlayer 初始化或播放速度核心逻辑。
- 风险：引入播放中断、返回卡顿或状态不同步。
- 避免方式：如果实现底部倍速面板，只改 UI 和触发方式，保留原有 ExoPlayer 倍速设置逻辑。

## 三、视频主页 / 文件夹列表踩坑

### 自动扫描文件夹移除后又回来

- 问题：自动扫描文件夹从列表移除后，重启 App 又被扫描出来。
- 风险：用户认为移除功能无效。
- 避免方式：自动扫描文件夹从列表移除后，必须写入 `hidden_video_folder_ids_json` 或对应隐藏记录。启动和重新扫描时必须过滤隐藏记录。

### 手动添加文件夹移除要同步持久化

- 问题：手动添加的视频文件夹只从内存移除。
- 风险：重启后文件夹从 DataStore 恢复回来。
- 避免方式：手动添加文件夹从列表移除后，必须从手动文件夹持久化记录中移除。

### 从列表移除不等于删除文件

- 问题：用户选择“从列表移除”时误删真实视频文件。
- 风险：造成不可逆数据损失。
- 避免方式：从列表移除只隐藏 App 内记录，不删除真实文件。永久删除必须单独确认。

### 不要申请全盘管理权限

- 问题：为了删除或扫描方便，尝试申请 `MANAGE_EXTERNAL_STORAGE`。
- 风险：违反产品边界和 Android 权限策略。
- 避免方式：继续使用 MediaStore、SAF、ContentResolver 能力；删除失败时明确提示权限问题。

### 多选测试不要误删用户文件

- 问题：自动化测试直接执行永久删除。
- 风险：删除用户真实视频。
- 避免方式：自动化测试不要擅自执行会改变真实文件的操作。除非用户明确允许，否则只验证入口和 UI，不执行永久删除。

### 三点面板不要塞假功能

- 问题：功能面板出现无效按钮或点击无反馈。
- 风险：用户无法判断功能是否完成。
- 避免方式：三点面板中的功能必须有行为；暂未实现必须 Toast：“后续实现”。

### 搜索关闭优先级

- 问题：搜索浮层打开后，返回键直接退出页面。
- 风险：用户预期是先关闭搜索。
- 避免方式：搜索浮层打开时，返回键优先关闭搜索；点击外部也应关闭搜索。

## 四、视频文件夹详情页踩坑

### `FolderScreen.kt` 是共享页面

- 问题：虽然当前改的是视频二级页，但 `FolderScreen.kt` 也可能承载音乐或小说文件夹详情。
- 风险：视频 UI 改动影响音乐 / 小说入口。
- 避免方式：修改 `FolderScreen.kt` 后，至少确认音乐 Tab、小说 Tab 能打开；如果涉及共享布局，必须说明影响范围。

### 排序不能只验证菜单打开

- 问题：排序菜单能打开，但点击排序项后崩溃或没有更新。
- 风险：用户以为排序可用，实际场景失败。
- 避免方式：视频二级页排序至少验证名称、时长、大小、进度点击不崩溃。准确性若自动化难确认，需要列出手动测试步骤。

### 删除和移除要分清

- 问题：单项更多菜单里的“从列表移除”和“永久删除文件”语义混乱。
- 风险：误删真实文件或 UI 状态错误。
- 避免方式：从列表移除不删除真实文件；永久删除必须二次确认。Android 权限失败时不能假装删除成功。

## 五、小说模块踩坑

### 小说来源必须明确

- 问题：打开小说页时显示很多用户没添加过的 TXT。
- 风险：侵犯用户预期，也让书架变得不可控。
- 避免方式：小说只展示用户明确导入的单本 TXT 或 TXT 文件夹扫描结果。不要自动扫描父文件夹，不要自动导入同目录其它 TXT。

### 小说导入记录必须持久化

- 问题：多选 TXT 导入后，杀掉 App 重开消失。
- 风险：书架不可信。
- 避免方式：导入单本 TXT 后写入 `book_files_json` 或对应持久化记录，并保存 SAF 持久读取权限。恢复时不要过度依赖权限列表，能读取则恢复，读取失败再跳过。

### 删除书籍后不能重启回来

- 问题：删除或移除书籍只改内存列表。
- 风险：重启后书籍重新出现。
- 避免方式：单本 TXT 必须同步删除持久化记录；文件夹扫描来源需要记录当前移除状态或明确说明重新扫描可能恢复。

### TTS 不可用不能崩溃

- 问题：系统缺少中文 TTS 语音包时播放失败。
- 风险：用户无法理解失败原因。
- 避免方式：TTS 不可用时引导用户安装语音数据或打开系统语音设置，不要崩溃，不要假装播放成功。

### 未实现小说能力不能伪装完成

- 问题：小说后台朗读、通知栏、AI 语音尚未实现却显示为可用。
- 风险：用户误判功能状态。
- 避免方式：未实现能力必须 Toast：“后续实现”或明确说明不可用。

## 六、音乐模块踩坑

### 音乐不记录播放位置

- 问题：把视频进度记忆逻辑套到音乐上。
- 风险：音乐每次点击歌曲不能从头播放，不符合当前产品策略。
- 避免方式：当前音乐策略是不记录上次播放位置。不要恢复音乐进度记忆，除非用户重新确认。

### 音乐删除失败是常见权限限制

- 问题：Android 不允许直接删除 MediaStore 音频文件。
- 风险：如果只 Toast 删除失败，用户下次仍看到歌曲。
- 避免方式：永久删除失败后，可以降级为“从列表移除”，写入 `hidden_audio_uris_json`，并明确提示用户系统不允许直接删除。

### 隐藏音乐必须过滤所有来源

- 问题：只从 `audioFiles` 移除，没有过滤自动扫描或手动文件夹扫描结果。
- 风险：重启或重新扫描后歌曲回来。
- 避免方式：自动扫描、手动扫描、最终展示列表都要过滤 `hidden_audio_uris_json`，并按 normalized uri 去重。

### 后台播放 Service 改动要谨慎

- 问题：随手修改 `MusicPlaybackService`。
- 风险：通知栏、锁屏控制、队列和迷你播放器状态不同步。
- 避免方式：只有音乐后台播放轮次才改 Service；视频或小说轮次不得顺手修改。

### 迷你播放器范围

- 问题：迷你播放器出现在视频页、小说页或我的页。
- 风险：模块边界混乱。
- 避免方式：迷你播放器只在音乐主页显示。

## 七、DataStore / 持久化踩坑

### 新增 key 必须说明用途

- 问题：DataStore key 越加越多，没有说明用途和影响范围。
- 风险：后续恢复、清除和迁移难以维护。
- 避免方式：新增 key 时说明用于哪个模块、保存什么、何时读取、何时清除。

### `AppStateStore.kt` 是共享持久化入口

- 问题：只考虑当前模块，忽略其它模块也在使用 DataStore。
- 风险：清除播放进度、隐藏记录、导入记录互相覆盖。
- 避免方式：修改 `AppStateStore.kt` 必须谨慎，避免共用错误 key。删除、隐藏、导入、播放进度都要重启验证。

### UI 点击中不要同步阻塞写入

- 问题：返回按钮或 UI 点击时同步写 DataStore。
- 风险：页面卡顿，尤其是视频播放页返回。
- 避免方式：DataStore 写入使用协程异步处理，不阻塞主线程。

### 设置页能力要对应持久化状态

- 问题：写入隐藏记录后，没有恢复或清除入口。
- 风险：用户误移除后无法找回。
- 避免方式：隐藏音乐需要“恢复隐藏音乐”；小说导入记录需要清除入口；视频隐藏记录后续也应有清除或恢复策略。

## 八、ADB / 真机测试踩坑

### 编译命令

必须运行：

```powershell
.\gradlew.bat :app:assembleDebug --console=plain
```

### 安装命令

```powershell
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

### 启动命令

```powershell
adb shell monkey -p com.shenghui.localvibe 1
```

### logcat 必查关键字

- `FATAL EXCEPTION`
- `AndroidRuntime`
- `ANR`
- `NullPointerException`
- `IllegalStateException`
- `SecurityException`
- `ExoPlaybackException`

### 小米手机安装限制

- 问题：出现 `INSTALL_FAILED_USER_RESTRICTED`。
- 风险：误以为 APK 或 Gradle 有问题。
- 避免方式：手机端开启 USB 调试、USB 安装、USB 调试（安全设置），并在手机弹窗允许安装。

### 自动化坐标测试不能过度相信

- 问题：坐标点击不稳定，误判功能不可用。
- 风险：错误修改代码。
- 避免方式：坐标测试失败时先截图确认 UI 状态；如果无法验证手感、排序准确性、删除持久化，应列出最少手动步骤。

### 截图默认不提交

- 问题：`test-results/` 截图进入 Git。
- 风险：提交内容污染。
- 避免方式：截图只作为测试产物，默认不提交。

## 九、Git 提交踩坑

### 每次提交前先看状态

- 问题：未确认工作区就提交。
- 风险：误提交无关文件。
- 避免方式：提交前先 `git status`，再按文件精确暂存。

### 多个残留文件要分批提交

- 问题：多个模块残留改动混在一起。
- 风险：难以回滚和审查。
- 避免方式：文档先单独提交；视频主页隐藏 / 多选相关文件一起提交；`FolderScreen.kt` 单独提交，因为它是共享页面。

### 视频主页隐藏 / 多选相关文件应一起提交

通常包含：

- `MainActivity.kt`
- `AppStateStore.kt`
- `VideoLibraryScreen.kt`

原因：主页 UI、隐藏记录和状态接入是同一条链路，拆开提交容易导致编译或功能不完整。

### `FolderScreen.kt` 单独提交

- 问题：`FolderScreen.kt` 是共享页面。
- 风险：视频二级页改动可能影响音乐 / 小说文件夹详情。
- 避免方式：单独提交，单独验证，提交说明中明确影响范围。

### push 失败不要重复 commit

- 问题：`Could not resolve host: github.com` 后重复提交。
- 风险：产生多余 commit。
- 避免方式：这通常是网络 / DNS 问题。本地已提交时，等网络恢复后重新 push 即可。

### ahead 状态含义

- 问题：看到 `ahead of origin/main by 1 commit` 后误以为没提交。
- 风险：重复提交。
- 避免方式：这表示本地已有 commit，只差 push。

### 提交前排除临时文件

不要提交：

- `test-results/`
- `.kotlin/`
- 临时截图
- 无关代码
- 本轮范围外文件

## 十、文档维护规则

### 避坑文档要持续补充

- 问题：踩坑只留在聊天记录里。
- 风险：后续开发重复犯错。
- 避免方式：新踩坑后沉淀进本文件。

### 本文档不写新功能规划

- 问题：把避坑记录写成需求池。
- 风险：和产品说明、UI 指南、模块 spec 混淆。
- 避免方式：这里只写开发经验、错误复盘和注意事项，不写新功能规划。

### 开发前阅读顺序

建议顺序：

1. `PRODUCT_SPEC.md`
2. `UI_GUIDE.md`
3. `DEVELOPMENT_BOUNDARY.md`
4. 当前模块专项文档
5. `DEVELOPMENT_PITFALLS.md`

### 不夸大已完成功能

- 问题：把未实现功能写成已完成。
- 风险：用户验收和后续开发判断错误。
- 避免方式：文档中只描述当前策略、已知风险和避免方式；未实现能力必须明确为后续实现或占位。

## 视频播放页近期完成能力与禁止回退边界

### 字幕样式

- 已完成：顶部“字幕”入口直接进入“字幕样式”面板；字幕选择/清除真实可用。
- 默认字幕背景为关闭；字幕大小、基础位置、颜色选择会真实应用到字幕。
- 边界：字幕时间/同步仍属于后续能力，不要把提前/延后做成假成功；如果不能真实影响外挂字幕时间轴，只能明确提示“后续实现”。

### AB 循环

- 已完成：AB 循环不再使用右侧大面板，已改为底部半透明悬浮条。
- 已支持设置 A/B 点、确认循环、重置；播放到 B 点回到 A 点；切换视频清空 AB。
- 交互边界：点击或拖动进度条不关闭 AB；点击视频空白区域可以关闭 AB；不要让控制层 auto-hide 或父层点击误关 AB。
- 禁止回退：不要把 AB 改回右侧面板。

### 睡眠定时

- 已完成：睡眠定时为居中深色半透明弹窗，默认 30 分钟。
- 快捷按钮为 `30min / 45min / 60min / 1h30min / 2h`。
- 大号小时/分钟可直接上下拖动；拖动只是选择待确认时间，点击“确认”后才开始倒计时。
- 取消、X、遮罩只关闭弹窗，不取消运行中的定时；重置会取消定时并回到 `0:30`，不暂停播放。
- 文案为“播完当前视频后停止”；倒计时结束后只暂停播放，不退出 App、不锁屏、不切视频。
- 禁止回退：不要改回右侧面板；不要恢复横向 Slider；不要恢复下方滚轮区域；不要使用系统 TimePicker。

### 播放页工具栏与解码信息

- 已完成：播放页工具栏图标已去重，信息、播放列表、睡眠、控制、解码、高级等入口不再大量重复使用齿轮图标。
- 图标边界：后续新增播放器工具时必须优先使用语义明确的图标，不要滥用齿轮导致用户无法区分入口。
- 已完成：`解码` 入口改为真实的“解码与格式”信息面板，不再是纯“后续”占位。
- 面板显示播放内核、文件扩展名、视频/音频轨道 MIME、codec、分辨率、设备支持状态；数据不可用时显示“未知”，不能伪造成已支持。
- 手动文件夹扫描已补充常见视频扩展名识别：`mp4 / mkv / webm / avi / mov / m4v / 3gp / 3gpp / ts / m2ts / mts / flv / wmv / asf`。
- 解码边界：扩展名识别只是“识别并尝试播放”，实际能否播放取决于当前设备系统解码器和 Media3 支持状态；不要写成“已支持所有格式”或“万能解码”。
- 播放失败提示已优化为提示当前设备或系统解码器可能不支持该视频编码；不要改成“设置成功”“已支持”等假成功文案。
- 后置高风险：真正万能解码、FFmpeg / mpv / native decoder、硬解/软解切换、复杂画面调节、手势系统、字幕真实时间轴偏移。
- 禁止回退：不要把解码信息面板改回纯“后续”；不要未经确认新增解码库、依赖或权限。

### 视频均衡器

- 已完成：播放页“视频均衡器”入口为真实系统音效面板，不再是“后续”占位。
- 音效链路：基于 Android `Equalizer`、`BassBoost`、`Virtualizer`，绑定当前播放器 `audioSessionId`；`audioSessionId` 变化时 release/recreate，播放页销毁时 release。
- UI：右侧深色半透明无边框面板；预设、5 个频段、低音增强、环境声、完成、恢复默认设置采用紧凑一页优先布局。
- 滑杆：顶部 5 个频段使用横向细滑杆；轨道 4dp、圆形 thumb 16dp；低音增强 / 环境声也使用同一细滑杆风格。
- 行为：频段拖动实时 `setBandLevel`；预设真实写入频段曲线；BassBoost / Virtualizer 真实 `setStrength`；“完成”只关闭面板；“恢复默认设置”真实恢复频段、低音增强、环境声。
- 不支持状态：设备不支持 Equalizer / BassBoost / Virtualizer 时显示“不支持”或说明，不假成功。
- 禁止回退：不要恢复竖向滑杆、`rotationZ`、粗 Material Slider、假滑杆或只改 UI 不改声音；不要新增音效依赖 / 权限；不要写 DataStore 持久化，除非单独任务明确允许。

### 测试结论

- `:app:assembleDebug` 已通过。
- 真机验证无明显问题。
- 完成对应功能提交后，工作区应保持 clean。

## Video Home Field / Placeholder Closure Notes

### Do not restore misleading "more actions" placeholders
- Video library home and video folder detail pages should not expose normal button-style "more actions" placeholders.
- Unfinished actions must be visibly weakened as future work, or hidden if they add no current value.
- Do not show fake success copy such as "enabled", "done", or "setting applied" for unfinished actions.

### Completed video home fields must stay real
- Date display is a real field toggle. List and grid cards can show `最近：yyyy-MM-dd`.
- Date display uses the latest modified time among videos in the folder. Date sorting and date display should keep the same meaning.
- Thumbnail duration is a real advanced toggle. It uses MediaStore `DURATION`; when duration is unavailable, do not show a duration badge and do not fake `00:00`.
- Extension / format display is a real field toggle. It uses existing `LocalMediaFile.extension`; examples: `格式：mp4`, `格式：mp4 / mkv`, `格式：mp4 / mkv +2`.

### Avoid heavyweight metadata work for home fields
- Do not use batch `MediaMetadataRetriever` or content parsing for video home fields.
- Do not add new scanning passes just to show date, duration, or extension fields.
- Do not show fake fallback values such as `1970-01-01` or `00:00` when source data is missing.

### Keep home field changes scoped
- Do not modify `VideoPlayerScreen.kt` for video library field display.
- Do not touch music or novel modules for video home field work.
- Do not restore "more actions" placeholders while editing field settings.

### Deferred field / advanced items
- Path display remains deferred: paths can be long, private, and unstable across manual/automatic scan sources.
- Playback progress display remains deferred: folder-level progress has unclear meaning.
- Folder total duration remains deferred: aggregating all videos has performance and semantic risk.
- Resolution / frame rate remains deferred: reliable values may require extra metadata scanning.
- Showing hidden files/folders remains deferred: it affects hidden/recover semantics.
- `.nomedia` recognition remains deferred: it affects scanning strategy and system media library behavior.

## Video Folder Detail Closure Notes

### Completed detail fields and grid behavior
- Date display is complete for video folder detail rows and grid items. It uses `LocalMediaFile.modifiedAt`, formats as `yyyy-MM-dd`, and must omit empty/invalid dates instead of showing `1970-01-01`.
- Detail grid UI is aligned with the video home grid: 3-column lightweight grid, transparent ordinary state, rounded thumbnails, one-line ellipsized titles, compact metadata, and thumbnail duration badges.
- Detail format display is complete. It uses existing `LocalMediaFile.extension`, shows the lowercased extension in the list/grid metadata row, and omits empty extensions instead of showing fake values such as `未知格式`.

### Do not regress detail grid layout
- Do not change the video folder detail grid back to a 2-column large-card layout.
- Do not restore heavy blue-black full-card backgrounds for ordinary grid items.
- Do not let grid titles become multi-line and stretch item height.
- Do not show meaningless progress such as `上次 0:00`; only show real previous playback progress.
- Do not show fake fallback metadata such as `1970-01-01`, `00:00`, or `未知格式`.

### Keep detail field work scoped
- Do not modify scanners, `VideoPlayerScreen.kt`, `VideoLibraryScreen.kt`, music, or novel modules just to show detail-page fields.
- Do not use batch `MediaMetadataRetriever`, file-content parsing, or extra scan passes for detail date/format display.
- Do not change delete authorization flow or scanning strategy while polishing detail metadata UI.

### Deferred detail items
- Path display remains deferred.
- Dedicated missing-file / invalid-file states remain deferred.
- Folder total duration remains deferred.
- Resolution / frame rate remains deferred.
- Delete authorization flow redesign remains deferred.
- Scan strategy changes remain deferred.
## Video Picture Adjustment Pitfalls

- The video player `画面调节` panel is a real first-version feature and is no longer a placeholder.
- Completed controls: brightness, contrast, saturation, color temperature, effect toggle, presets (`默认 / 明亮 / 影院 / 护眼 / 鲜艳`), and Done.
- The bottom duplicate `恢复默认设置` entry has been removed from picture adjustment. Keep the `默认` preset as the reset path for neutral picture parameters.
- Do not remove the internal reset/default logic because the `默认` preset still depends on it.
- Do not remove the equalizer panel's separate `恢复默认设置`; that belongs to audio effects.
- Android 12+ applies picture adjustment with `RenderEffect` and `ColorMatrixColorFilter` on the player `texture_view` surface.
- Effects apply only to the current playback view and never modify the original video file.
- Android 12 and below must show unsupported state, disable picture controls, and avoid fake success.
- Brightness, contrast, saturation, and color temperature must stay independent unless a preset is tapped.
- The effect toggle must bypass effects without clearing user parameters.
- Video player `ON_STOP` must pause playback so returning to the desktop does not leave video audio playing.
- Do not add sharpening, dark enhancement, advanced filters, or native decode changes as fake or partial features.

## Video Playback Regression Review Notes

- TODO-006 video playback regression review is complete after equalizer and picture adjustment changes.
- Fixed double-tap seek regression: left-side double tap must really seek backward, right-side double tap must really seek forward, and the hint must not appear without an actual seek.
- Fixed sleep timer end-of-current-video mode: `播完当前视频后停止` must stay independent from timed countdowns, must not fall back to 30 minutes, and should pause at current video end without auto-playing the next item.
- Timed sleep options such as 30 minutes remain valid countdown modes and must not be removed while fixing end-of-current-video mode.
- Fixed full-screen navigation gesture conflict: drags that start in the bottom system gesture safe area must not trigger player brightness, volume, horizontal seek, or other full-screen gestures.
- Middle-screen gestures remain valid: left-side brightness, right-side volume, horizontal seek, and double-tap seek should continue to work outside the bottom system gesture area.
- Current accepted background behavior: pressing Home pauses video playback, and returning to the app does not auto-resume; the user manually resumes playback.
- Still deferred: subtitle time sync, two-finger zoom and more complex gestures, FFmpeg/mpv/native decoder work, advanced filters, sharpening, and dark enhancement. Do not document these as completed.

## External SRT Subtitle Sync Notes

- External SRT subtitle time sync first version is complete for manually selected `.srt` subtitles.
- Supported scope: whole-subtitle offset from `-5s` to `+5s`, quick buttons (`-1s`, `-0.5s`, reset, `+0.5s`, `+1s`), thin slider control, and reset to `0s`.
- Implementation boundary: read the selected external SRT, parse cue timestamps, apply one global offset, clamp times below `0` to `0`, write an adjusted temporary SRT under app cache, and reload it through `MediaItem.SubtitleConfiguration` while preserving playback position and playback state as much as possible.
- Do not regress this feature back to a "future" placeholder, and do not implement a UI-only number that does not change the rendered subtitle timing.
- Unsupported cases remain explicit: embedded subtitles, ASS/SSA advanced subtitle sync, and universal subtitle sync are not completed.
- Do not claim embedded subtitles or ASS/SSA full sync are supported unless their rendering path is truly implemented and verified.
- Sync failures should show a clear error and suggest reloading the subtitle file; never fake success.
- Local validation files such as `test_subtitle_sync.srt` may be used during manual testing, but test SRT files must not be committed.

## Unavailable Video File State Notes

- Video unavailable-file state first version is complete.
- Current coverage: video folder detail list, video folder detail grid, click interception for unavailable videos, and existing remove-from-list flow.
- Detection is intentionally lightweight: check whether the video URI is still readable with `contentResolver.openFileDescriptor(uri, "r")`.
- Do not add permissions, do not run full-disk validation, and do not change the scanner architecture for this state.
- UI behavior: unavailable video rows/cards are dimmed, the thumbnail area shows `文件已失效`, and the metadata line shows `文件已失效`.
- Interaction behavior: tapping an unavailable video must not enter the player; show `文件已失效，可从列表移除`.
- Unavailable videos may reuse remove-from-list. Do not show `永久删除文件` for an unavailable item, and do not report a fake permanent delete success for a file that no longer exists.
- Regression boundaries: do not let unavailable videos behave like normal playable videos, do not add heavy checks that make list scrolling or folder entry slow, and do not change music or novel modules.
- Deferred enhancements: batch unavailable-file cleanup, a unified unavailable-file management view, and automatic cleanup after rescan.

## Video Hidden Records, Delete, And Thumbnail Cache Notes

- Hidden record management is complete in the video home `More` panel under the action entry `Hidden records`.
- Hidden records expand inside the existing `More` panel and must not navigate to a new page.
- The manager uses real persisted records only: hidden folders, hidden videos, and unavailable-file records. Do not show mock or hard-coded sample records.
- Category filters must stay accurate: `All`, `Hidden folders`, `Hidden videos`, and `Unavailable files` can show counts, but each non-all filter must show only its own record type.
- Hidden folders and hidden videos can be restored or cleared from the app record. Unavailable files can only be cleared from app records; do not show restore or permanent-delete actions for unavailable files.
- Multi-select hide/delete is complete on both video home and video folder detail pages. The selection bar contains selected count, cancel, select all, hide, and delete.
- Hide means app-level hiding only and must never delete real files.
- Delete means real local video deletion and must always go through the existing confirmation and MediaStore/permission delete flow.
- If delete permission is denied or deletion fails, do not show success.
- Video home delete only deletes app-recognized video files in the selected folders; it must not delete non-video files.
- Video folder detail delete only deletes selected video files.
- Floating play buttons should stay hidden during selection mode on both home and folder detail pages.
- Video thumbnail disk cache is complete under app cache `video_thumbnails`.
- Thumbnail cache keys must remain bound to video identity with `uri + modifiedAt + size`; do not fall back to file name, title, or folder name as the cache key.
- Folder thumbnails must be derived from a real accessible representative video in the folder. If that representative disappears, switch to the next accessible video. If no accessible videos remain, show the default placeholder and do not display a stale thumbnail.
- Folder detail thumbnails are one-video-one-thumbnail and must not reuse another video's cached image.
- Deleting a real video must clear only that video's thumbnail cache. Hiding a video must not delete its thumbnail cache.
- Invalid thumbnail fallback is complete: black frames, white/overexposed blank frames, and low-information solid frames are rejected.
- Thumbnail generation should try multiple frame times and should not cache invalid frames as normal thumbnails.
- Old invalid cached thumbnails should be deleted and regenerated when encountered; if all candidates fail, show a placeholder instead of a fake success thumbnail.
- Background thumbnail prewarming is complete. It starts after app startup/restored scans, video scan completion, rescan, and manual video folder add.
- Prewarming must only process already scanned videos, skip valid cached thumbnails, skip unreadable/unavailable videos, run serially in the background, limit each run to about 100 videos, and pause briefly between items.
- Thumbnail cache size limiting is complete for app cache `video_thumbnails`.
- The cache limit is `300MB`; when exceeded, cleanup trims the directory to about `260MB`.
- Cleanup must only scan and delete files inside `video_thumbnails`; never clear the whole app cache and never delete real video files.
- Cache cleanup deletes by `lastModified` from oldest to newest. Successful cache reads should update `lastModified` so recently used thumbnails are kept longer.
- Thumbnail delete failures during cleanup must not crash the app.
- Background prewarming still goes through `VideoThumbnailStore`; generated thumbnails must pass black/white/low-information frame validation and then obey the same cache-size limit.
- Do not introduce full-disk thumbnail scans, startup-wide parallel thumbnail generation, new permissions, or new dependencies for prewarming.
- Regression boundaries: do not restore `remove` as the main destructive/non-destructive wording, do not make hide delete files, do not fake delete success, do not let unavailable files show restore, do not let unrelated videos share thumbnail cache, do not cache black/white frames, do not let thumbnail cache grow without limit, do not clear real videos or the whole app cache, and do not start unlimited thumbnail work on app startup.
- Deferred enhancements: show thumbnail cache usage in settings, add a manual clear-thumbnail-cache action, and adjust the cache limit based on available device storage.

## Video Player Queue Panel And Gesture Pitfalls

- The player `视频列表 / 播放队列` panel is a real right-side translucent queue panel, not a placeholder and not a file-management page.
- Keep the panel close to full height. Do not let it fall back to wrap-content height that only shows a few videos.
- Keep the header compact so the list area can show more queue items.
- Queue search must filter the real current queue. Do not show a fake search field.
- Queue filters (`全部 / 未看 / 已看 / 失效`) must be based on real progress/unavailable state.
- The current playing item should be identifiable through dark purple highlight and the `正在播放` label.
- Do not restore the circular play button overlay on the current item's thumbnail; it covers the thumbnail and is redundant.
- Unavailable queue items must not switch playback. They can be removed from the list, but the queue panel must not expose permanent-delete actions.
- Player function panels should stay translucent and border-light/no-obvious-border while keeping text readable.
- Do not restore pure-black solid panel backgrounds or thick outlines while editing player panels.
- Gesture hints must not be decoupled from real actions. A seek/brightness/volume hint is not enough unless the real player/window/audio action is executed.
- Double-tap seek must call the current player's real seek path.
- Horizontal drag seek must apply the final target on drag end.
- Left vertical drag must write activity window brightness; right vertical drag must change media volume through `AudioManager`.
- Player gesture `pointerInput` must stay bound to the current `mediaFile.uri` and `player`; otherwise switching videos from the queue can leave gestures using an old player reference.
- Keep the bottom system gesture safe area: drags starting from the bottom navigation zone must not trigger brightness, volume, or seek.
- When adding full-screen overlays for player panels, ensure they are only present while the panel is open and do not keep intercepting gestures after dismissal.
- Deferred queue ideas such as queue sorting, current-item locator, density tuning, and unavailable-item batch cleanup are not complete and must not be documented as shipped.

## Video Module Closure Checklist And Regression Guardrails

- The video module is now in final closure/polish mode. New video work should be small, scoped, and regression-oriented unless the user explicitly approves a new feature.
- Completed video home behaviors include search, add, list/grid switch, sorting, multi-select, hide, real delete, hidden records, field settings, and thumbnail cache display.
- Completed folder detail behaviors include search, sorting, list/grid, multi-select, hide, real delete, unavailable-file state, and one-video-one-thumbnail binding.
- Completed player behaviors include basic playback, progress, speed, resize mode, screenshot, lock, orientation, queue panel, subtitle style, external SRT sync, AB loop, sleep timer, gestures, equalizer, picture adjustment, and decode/format info.
- Completed thumbnail behaviors include disk cache, per-video key, black/white/low-information frame rejection, background prewarm, delete cleanup, and cache size limiting.
- Completed safety behavior: hide is non-destructive; delete is real local video deletion with confirmation and the existing Android permission flow.

High-risk items still deferred:

- FFmpeg/mpv/native decoder work.
- Hardware/software decoder switching.
- Embedded subtitle offset.
- ASS/SSA advanced subtitle sync.
- Delete authorization flow redesign.
- Hidden-file and `.nomedia` scan policy.
- Batch resolution/frame-rate statistics.
- Complex two-finger gestures.
- Advanced picture filters such as sharpening and dark enhancement.

Manual real-device regression must cover:

- Video home search/add/sort/view switch/multi-select/hidden records/thumbnails.
- Folder detail search/sort/list-grid/multi-select/unavailable state/thumbnails/play entry.
- Player playback, queue panel, subtitle style/sync, gestures, equalizer, picture adjustment, AB, sleep timer, screenshot, lock, and Home pause.
- Hide/delete safety, permission denial behavior, unavailable-file removal, and thumbnail cleanup.

Hard regression bans:

- Do not change hide into real delete.
- Do not show delete success when the system permission flow is canceled or deletion fails.
- Do not restore `移除` as the main action wording where the behavior is now `隐藏`.
- Do not make player queue search/filters fake.
- Do not let gesture hints appear without real seek/brightness/volume actions.
- Do not cache black/white/blank thumbnails as valid cache.
- Do not let one video display another video's thumbnail.
- Do not let unavailable files enter the player.
- Do not add permanent delete to the player queue panel.
- Do not add permissions/dependencies or enter music/novel modules while video regressions remain unresolved.

## Recent Video Completion Regression Notes

Video home `More` panel:

- Do not let expanded `Cache management`, `Fields`, `Advanced`, or `Hidden records` stretch the outer `More` panel. The shell height should stay fixed and the content should scroll internally.
- Keep `Hidden records` at the bottom of the folded section stack.
- Keep the `Advanced` folded row visually aligned with `Cache management`, `Fields`, and `Hidden records`.
- Do not turn cache management into a new page. It belongs inside the video home `More` panel.

Thumbnail cache management:

- Cache size must come from the real `cache/video_thumbnails/` directory, not a design placeholder such as `128 MB`.
- Clearing thumbnail cache must only clear thumbnail cache files. It must not delete local video files, hidden records, playback progress, subtitle data, or the entire app cache.
- Keep the cache-size boundary: about `300MB` maximum and about `260MB` trim target.
- Do not bypass `VideoThumbnailStore`; it owns invalid-frame rejection and cache-size enforcement.

Player function area:

- The default player function grid should stay scoped to speed, portrait/landscape, equalizer, picture adjustment, and expand.
- Expanded controls should contain screenshot, info, queue/list, AB loop, sleep timer, control bar, gestures, decode, advanced, and collapse.
- Audio track should not be duplicated in the function grid; it remains a top-bar entry.
- Do not mistake entry cleanup for feature removal.

Advanced panel:

- Do not show external SRT subtitle time sync as an unfinished generic `future` item.
- External SRT subtitle time sync is complete.
- Embedded subtitle time offset, ASS/SSA advanced subtitle sync, FFmpeg/mpv/native decoder work, decoder enhancement, and complex picture filters remain deferred.
- Do not claim deferred subtitle or decoder work is complete without a real rendering/playback path and real-device verification.

Subtitle style and inline subtitle timing:

- Subtitle time sync now lives inside the subtitle style panel. Do not reintroduce a jump from subtitle style to a separate subtitle-time page.
- Without an external `.srt`, show `需外挂 SRT`, disable plus/minus and drag controls, and do not fake success.
- With an external `.srt`, plus/minus and drag controls must call the existing real offset path.
- Short press on plus/minus should change only `0.1s`; long press is the fast-repeat path.
- Vertical drag on the center value should accumulate distance, preview multiple `0.1s` steps during one drag, and apply the final offset on release.
- Keep the clamp around `-5.0s` to `+5.0s`.
- Do not rewrite SRT parsing for this UI; reuse the existing external SRT offset implementation.

Current video-module boundary:

- Supported subtitle sync remains external `.srt` only.
- Embedded subtitle offset and ASS/SSA advanced sync are not complete.
- Do not add FFmpeg/mpv/native decoder, permissions, dependencies, Gradle changes, Manifest changes, music-module changes, or novel-module changes while documenting this status.

## Player Sleep And Gesture Regression Notes

Sleep pause recovery:

- Do not let sleep-mode pause become a permanent playback block.
- After a timed sleep pause, the bottom play button must clear the consumed sleep-triggered state before resuming playback.
- After `pause after current video`, the player may be in `Player.STATE_ENDED`; a plain `player.play()` can be insufficient.
- In `Player.STATE_ENDED`, manual play should move to the next queue item when one exists, or seek the current video to `0` and play when no next item exists.
- Do not delete sleep functionality to fix this. Sleep remains a one-shot automatic pause that users can configure again after manually resuming.

Top-center gesture safe area:

- Do not let a downward swipe starting from the upper middle of the player trigger brightness or volume adjustment.
- The safe area is about `96dp` from the top and `25%` to `75%` of screen width.
- Decide safety from the gesture start point only; do not dynamically toggle the safe area while dragging.
- A gesture that starts in the safe area must not show brightness/volume hints or change window brightness/system media volume.
- Do not disable the whole top edge. Left brightness and right volume gestures outside the top-center zone must still work.
- Do not regress click, double-tap seek, horizontal seek, control bar, subtitles, sleep, AB loop, or queue interactions while changing gesture safety.

Boundary:

- These fixes must not add permissions, dependencies, Gradle changes, Manifest changes, music changes, novel changes, video home changes, or folder detail changes.

## Video Home And Folder Detail UI Regression Notes

Shared bottom navigation:

- Do not implement a separate "similar" bottom bar for video folder detail. It can drift from video home in height, padding, selected pill size, icon/text position, and safe-area behavior.
- Video home and video folder detail must share `app/src/main/java/com/shenghui/localvibe/core/ui/MoonBottomNavigationBar.kt`.
- In video folder detail, the `Video` tab is highlighted but tapping it does not force navigation back to video home. Use the top-left back button to return to video home.
- `Music`, `Novel`, and `Me` remain real top-level navigation targets and must not become fake buttons.

Search fields:

- Do not shrink video home or video folder detail search fields back to a small fixed height that clips input text, placeholder, or cursor.
- Keep search fields single-line with stable vertical centering and readable text while the IME is visible.

Long folder titles:

- Do not force video home folder titles back to a single line.
- Folder titles may use up to 2 lines and should ellipsize only after the second line.

Layout safety:

- Video folder detail list content must avoid the bottom navigation bar.
- The lower-right play FAB must not overlap the bottom navigation bar.
- Selection mode controls must not collide with bottom navigation.
- This UI work must not modify the video player page, Gradle, Manifest, permissions, dependencies, music internals, or novel internals.

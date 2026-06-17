# LocalVibe / Moon播放器 项目路线图

## 项目定位

LocalVibe / Moon播放器是 Android 本地媒体播放器，面向本机文件管理和播放体验。

核心模块：

- 本地视频
- 本地音乐
- TXT 小说 / 听书
- 我的 / 设置

项目不做在线播放、登录、云同步、广告、推荐和会员体系。所有能力必须以本地文件、本地状态和系统授权为边界。

## 产品模块边界

### 视频模块

做：

- 本地视频扫描
- 文件夹管理
- 视频列表
- 横屏播放
- 播放进度
- 继续播放
- 隐藏 / 移除
- 系统允许范围内删除
- 倍速
- 截图
- 字幕入口
- 工具占位状态

不做：

- 在线视频
- 弹幕
- 云字幕搜索
- 账号同步
- 推荐流
- 视频后台播放

### 音乐模块

做：

- 本地音频扫描
- 曲目列表
- 文件夹列表
- 后台播放
- 通知栏控制
- 锁屏控制
- 播放队列
- 播放模式

不做：

- 在线音乐
- 在线歌词
- 云歌单
- 登录
- 推荐
- 云盘音乐

### TXT 小说 / 听书模块

做：

- 用户主动导入 TXT
- 书架
- 阅读 / 听书页
- Android TextToSpeech
- 听书进度保存

不做：

- 在线书源
- AI 配音
- 云同步
- 全盘 TXT 自动扫描
- 自动导入同目录文件

### 我的 / 设置模块

做：

- 数据管理
- 恢复隐藏内容
- 清理播放进度
- App 信息
- 权限提示

不做：

- 账号中心
- 会员
- 云备份
- 广告配置

## 开发原则

1. 一轮只做一个小功能。
2. 每轮必须可编译。
3. 每轮尽量可真机验证。
4. 不跨模块大改。
5. 不伪造未完成功能。
6. 未完成能力必须明确显示“后续实现”。
7. 不做登录、云同步、广告、推荐、在线播放。
8. 不申请 `MANAGE_EXTERNAL_STORAGE`。
9. 不引入 Room / Hilt / 大型架构迁移，除非用户明确批准。
10. 共享文件修改前必须说明影响范围。
11. 删除 / 隐藏 / 权限失败必须保证数据安全。
12. 不能为了测试自动删除用户真实文件。
13. 每次 push 前必须确认 `git status`。
14. push 成功后必须确认 working tree clean。
15. 使用 Superpowers / Product Design 等插件前，要先说明用途，不要把插件生成物直接混进业务代码提交。

## 未来 4 周开发顺序

### 第 1 周：视频模块收口

- 视频首页三点功能面板收口
- 视频首页搜索 / 排序
- 视频文件夹详情页搜索 / 排序
- 视频多选移除 / 删除一致性验证
- 播放页倍速 / 画面比例 / 手势细节收口
- 播放页 UI 统一收口

### 第 2 周：音乐核心链路

- 曲目列表稳定扫描与去重
- 点击曲目建立播放队列
- Mini Player
- `AudioPlayerScreen` 连接 Service
- 通知栏 / 锁屏控制真机验证

### 第 3 周：小说 / TTS

- TXT 单本导入
- 书架持久化恢复
- 听书页基础播放 / 暂停 / 停止
- 听书进度保存与恢复
- TTS 不可用引导

### 第 4 周：设置与发布准备

- 恢复隐藏视频 / 音乐
- 清除播放进度
- 清除小说导入记录
- 权限拒绝场景梳理
- 真机全链路回归

## 暂时不要做的功能

- 在线视频
- 在线音乐
- 在线小说
- 登录
- 账号
- 会员
- 云同步
- 云备份
- 推荐
- 猜你喜欢
- 排行榜
- 广告
- AI 配音
- 在线歌词
- 在线字幕搜索
- Room
- Hilt
- 大型架构迁移
- `MANAGE_EXTERNAL_STORAGE`
- 跨模块重构
- 全盘 TXT 自动扫描
- 视频后台播放
- 未完成能力的假成功 UI

## 总体开发策略

先把视频模块彻底收口，再做音乐后台播放闭环，再做 TXT/TTS 闭环，最后用设置页把本地数据管理补齐。

每一轮开发都要遵循“小功能、窄范围、可编译、可验证”的节奏。若功能需要跨模块或共享文件修改，必须先说明原因、影响范围和回归验证点。

## 视频播放页近期完成状态

以下能力已完成并已进入视频播放页收口范围：

- 字幕样式：顶部“字幕”直接进入字幕样式；字幕选择/清除真实可用；默认字幕背景关闭；字幕大小、位置、颜色真实生效；字幕时间/同步仍为后续能力，不应伪装为已实现。
- AB 循环：已从右侧面板改为底部半透明悬浮条；设置 A/B、确认循环、重置可用；点/拖进度条不关闭 AB；点空白区域关闭 AB；切换视频清空 AB。
- 睡眠定时：已改为居中深色半透明弹窗；默认 30 分钟；快捷按钮为 `30min / 45min / 60min / 1h30min / 2h`；大号小时/分钟直接上下拖动；点击“确认”后才开始倒计时；取消、X、遮罩只关闭弹窗；重置取消定时并回到 `0:30`；倒计时结束后暂停播放，不退出 App、不锁屏、不切视频。
- 工具栏图标去重：信息、播放列表、睡眠、控制、解码、高级等入口已使用更明确的语义图标，不再大量重复齿轮。
- 解码与格式信息面板：`解码` 入口显示播放内核、文件扩展名、视频/音频轨道 MIME、codec、分辨率、设备支持状态；数据不可用时显示“未知”，不假装已支持。
- 多格式识别边界：手动文件夹扫描已补充 `mp4 / mkv / webm / avi / mov / m4v / 3gp / 3gpp / ts / m2ts / mts / flv / wmv / asf` 识别；这只是识别并尝试播放，实际播放能力仍取决于设备系统解码器和 Media3。
- 播放失败提示：播放失败时提示当前设备或系统解码器可能不支持该视频编码，不假装自动修复。
- 视频均衡器：已完成真实系统均衡器面板；基于 Android Equalizer / BassBoost / Virtualizer；支持预设、5 个频段、低音增强、环境声、恢复默认；面板为深色半透明右侧面板，使用横向 4dp 细轨道和 16dp 圆形 thumb，并采用一页优先紧凑布局。

后续视频播放页工作不得回退以上交互。尤其不要把睡眠定时改回右侧面板，不要恢复横向 Slider 或下方滚轮区域，不要使用系统 TimePicker；不要把 AB 改回右侧面板；不要把未实现的字幕时间同步做成假成功；不要恢复一排重复齿轮图标；不要把解码信息面板改回纯“后续”；不要声称全格式万能播放；不要未经确认新增解码库、依赖或权限；不要把视频均衡器改回后续占位、竖向滑杆、粗滑杆或假滑杆；不要把“完成”按钮改成清除效果。

仍后置的播放页高风险能力：真正万能解码、FFmpeg / mpv / native decoder、硬解/软解切换、复杂画面调节、手势系统、字幕真实时间轴偏移。

测试结论：`assembleDebug` 已通过；真机验证无明显问题；均衡器相关回归使用 ADB 62001 安装 / 启动流程，真实声音效果仍需真机确认；对应提交完成后工作区应保持 clean。

## 视频主页字段 / 占位收口进度

以下能力已完成并进入视频主页收口范围：

- 更多功能占位清理：视频主页和视频文件夹详情页不再保留普通按钮式“更多功能”占位；后续项必须弱化显示，不能假成功。
- 日期显示：字段设置里的“日期显示”是真实开关；列表 / 网格可显示 `最近：yyyy-MM-dd`；日期来源为文件夹内最近视频修改时间；日期排序和日期显示口径应保持一致。
- 缩略图显示时长：高级设置里的“缩略图显示时长”是真实开关；时长来源为 MediaStore `DURATION`；不使用 `MediaMetadataRetriever` 批量扫描；无时长时不显示标签，不显示假 `00:00`。
- 扩展名 / 格式显示：字段设置里的“扩展名显示”是真实开关；使用已有 `LocalMediaFile.extension`；多格式文件夹显示为 `格式：mp4`、`格式：mp4 / mkv`、`格式：mp4 / mkv +2`；不新增扫描，不解析文件内容。

禁止回退边界：

- 不要恢复“更多功能”普通占位入口。
- 不要把已完成的日期、缩略图时长、扩展名字段改回“后续”。
- 不要用重型批量元数据扫描实现主页字段。
- 不要把无数据字段显示成假值，比如 `00:00` / `1970-01-01`。
- 不要为了主页字段改播放页 `VideoPlayerScreen.kt`。
- 不要改音乐 / 小说模块。

仍后置的字段 / 高级项：

- 路径显示：路径可能过长，涉及隐私，且自动/手动扫描 key 不稳定。
- 播放进度：主页是文件夹卡片，进度含义不清。
- 文件夹总时长：需要聚合所有视频，容易有性能和语义问题。
- 分辨率 / 帧率：可能需要额外元数据扫描。
- 显示隐藏文件和文件夹：涉及隐藏 / 恢复语义。
- 识别 `.nomedia`：涉及扫描策略和系统媒体库规则。

## 视频文件夹详情页阶段性收口进度

以下能力已完成并进入视频文件夹详情页收口范围：

- 日期显示：列表 / 网格条目可显示视频修改日期；来源为 `LocalMediaFile.modifiedAt`；格式为 `yyyy-MM-dd`；空日期不显示，不显示假 `1970-01-01`。
- 网格 UI 对齐：二级详情页网格已改为 3 列轻量网格；普通态不再使用厚重蓝黑大卡片背景；标题一行省略；大小和日期合并显示；未播放视频不显示 `上次 0:00`；已播放视频显示 `上次 xx:xx`；保留缩略图右下角时长标签。
- 格式 / 扩展名显示：列表 / 网格元信息行显示格式；来源为已有 `LocalMediaFile.extension`；无扩展名不显示假值；不新增扫描，不使用 `MediaMetadataRetriever`，不解析文件内容。

禁止回退边界：

- 不要把二级详情页网格改回 2 列大卡片。
- 不要恢复厚重整卡背景。
- 不要让标题重新变成多行撑高网格。
- 不要显示 `上次 0:00` 这种无意义进度。
- 不要显示 `1970-01-01` 或 `未知格式` 这类假值。
- 不要为了详情页字段改扫描器、播放页 `VideoPlayerScreen.kt`、视频主页 `VideoLibraryScreen.kt`、音乐 / 小说模块。
- 不要改删除授权链路和扫描策略。

仍后置的详情页功能：

- 路径显示。
- 文件失效状态专项。
- 文件夹总时长。
- 分辨率 / 帧率。
- 删除授权链路大改。
- 扫描策略大改。
## Video Picture Adjustment Completion Notes

- Video picture adjustment is complete as a real player feature and is no longer a placeholder.
- First-version scope: brightness, contrast, saturation, color temperature, effect toggle, presets (`默认 / 明亮 / 影院 / 护眼 / 鲜艳`), and Done button.
- The duplicate bottom `恢复默认设置` entry was removed from the picture adjustment panel.
- The `默认` preset remains and is responsible for restoring neutral picture parameters.
- Android 12+ uses `RenderEffect` plus `ColorMatrixColorFilter`; Android 12 and below show unsupported state and disabled controls.
- The player uses the `texture_view` surface so effects apply only to the current playback display and never modify source video files.
- Confirmed fixes: `ON_STOP` pauses video playback, saturation no longer changes other sliders, and the effect toggle bypasses effects without clearing parameters.
- Deferred high-risk work: sharpening, dark enhancement, advanced filters, and native decode pipeline changes require separate design, research, and verification.

## Video Playback Regression Review Completion

- TODO-006 playback regression review is complete after the equalizer and picture adjustment rounds.
- Confirmed fixes:
  - Double-tap seek now performs real seek: left side rewinds and right side fast-forwards.
  - Sleep timer `播完当前视频后停止` no longer becomes a 30-minute countdown; current video end pauses playback and does not auto-play the next item.
  - Timed sleep countdowns, including 30 minutes, remain available.
  - Bottom full-screen system gesture safe area no longer triggers player brightness, volume, seek, or other full-screen gestures.
  - Middle-screen brightness/volume gestures, horizontal seek, and double-tap seek remain available.
- Accepted playback lifecycle: pressing Home pauses video playback; returning to the app does not auto-resume and the user resumes manually.
- Still deferred and not completed: subtitle time sync, two-finger zoom / complex gestures, FFmpeg/mpv/native decoder, advanced filters, sharpening, and dark enhancement.

## External SRT Subtitle Sync Completion

- External SRT subtitle time sync first version is complete.
- Current real support: manually selected external `.srt` subtitles, whole-subtitle advance/delay offset, `-5s` to `+5s` range, quick buttons, thin slider, and reset to `0s`.
- Implementation: read the external SRT, parse timestamps, apply the global offset, clamp times below `0` to `0`, generate a temporary adjusted SRT file under app cache, reload it with `MediaItem.SubtitleConfiguration`, and preserve playback position/state as much as possible.
- Unsupported boundaries: embedded subtitle offset is not supported, ASS/SSA advanced subtitle sync is not fully supported, and this is not universal subtitle sync for every subtitle format.
- Regression boundaries: do not move this feature back to a future placeholder, do not fake sync by changing only the UI number, and do not claim embedded or ASS/SSA full sync is complete.
- Testing note: local files such as `test_subtitle_sync.srt` may be used for validation, but test SRT files must not be committed.
- Next workflow step: real-device regression review for external SRT subtitle sync.

## Unavailable Video File State Completion

- Video unavailable-file state first version is complete.
- Current coverage: folder detail list, folder detail grid, click interception for unavailable video items, and remove-from-list reuse.
- Detection uses a low-risk URI readability check through `contentResolver.openFileDescriptor(uri, "r")`.
- UI behavior: unavailable items are dimmed, thumbnails show `文件已失效`, and metadata shows `文件已失效`.
- Interaction behavior: tapping an unavailable video does not navigate to the player and shows `文件已失效，可从列表移除`.
- Removal behavior: unavailable videos can be removed from the list; the UI must not expose permanent-delete wording for files that are already inaccessible.
- Boundaries: no new permissions, no full-disk scan, no scanner architecture rewrite, and no music/novel module changes.
- Deferred enhancements: batch cleanup for unavailable files, a central unavailable-file management page, and an automatic cleanup policy after rescan.

## Video Management And Thumbnail Cache Completion

- Hidden record management is complete in the video home `More` panel: `Video home -> More -> Actions -> Hidden records`.
- The hidden record manager expands inside the existing `More` panel and does not open a new page.
- It uses real records for hidden folders, hidden videos, and unavailable files. It does not use fake data.
- Categories are complete with counts: `All`, `Hidden folders`, `Hidden videos`, and `Unavailable files`.
- Hidden folders and hidden videos can be restored or cleared. Unavailable files can only be cleared from records and cannot be restored or permanently deleted from that manager.
- Multi-select hide/delete is complete for video home and video folder detail pages.
- Both selection bars include selected count, cancel, select all, hide, and delete.
- Hide only hides app records and never deletes real files.
- Delete performs real local video deletion through confirmation and the existing MediaStore/permission delete path.
- Home-level delete only deletes app-recognized videos inside selected folders and must not delete non-video files.
- Folder-detail delete only deletes selected videos.
- Floating play buttons are hidden during selection mode on both video home and folder detail pages.
- Video thumbnail disk cache is complete under `cache/video_thumbnails`.
- Thumbnail cache keys use `uri + modifiedAt + size`.
- Folder-detail videos are bound one-to-one with their own thumbnails, avoiding cross-video thumbnail reuse.
- Video home folder thumbnails are bound to a real accessible representative video. If the representative disappears, the folder switches to the next accessible video; if no video remains, stale thumbnails are not shown.
- Real video deletion clears the corresponding thumbnail cache. Hiding does not delete thumbnail cache.
- Invalid thumbnail fallback is complete: black frames, white/overexposed blank frames, and low-information solid-color frames are detected and rejected.
- Thumbnail generation tries multiple frame times and does not cache invalid thumbnails as normal images. If all candidates fail, the UI falls back to a placeholder.
- Background thumbnail prewarming is complete.
- Prewarming triggers after startup/restored scans, scan completion, rescan, and manual video folder add.
- Prewarming only processes scanned videos, skips existing valid cache, skips unavailable videos, runs serially, limits each run to about 100 videos, waits about 90ms between items, and does not block UI.
- Thumbnail cache size limiting is complete for `cache/video_thumbnails`.
- Current limit: maximum `300MB`, then trim to about `260MB`.
- Cleanup scans only the `video_thumbnails` directory, deletes oldest cache files first by `lastModified`, and should not crash the app if an individual cache file cannot be deleted.
- Successful cache reads update `lastModified`, so recently used thumbnails are treated as newer cache entries.
- Background prewarming still writes through `VideoThumbnailStore`, so cache writes continue to run invalid-frame checks and enforce the same size limit.
- Safety boundaries: thumbnail cleanup must not delete real videos, must not clear the whole app cache, must not clear non-thumbnail cache directories, and hiding videos must not delete thumbnail cache.
- No new permissions, dependencies, full-disk scan, or startup-wide parallel thumbnail generation were added for this thumbnail work.

Regression boundaries:

- Do not restore `remove` as the main wording for non-destructive hide behavior.
- Do not make hide delete real files.
- Do not fake delete success when permission is denied or deletion fails.
- Do not show restore for unavailable-file records.
- Do not let different videos share an incorrect thumbnail.
- Do not cache black, white, or low-information frames as valid thumbnails.
- Do not let `cache/video_thumbnails` grow without a size limit.
- Do not clear real video files or the whole app cache while trimming thumbnail cache.
- Do not bypass invalid-frame detection during background prewarming.
- Do not run full-disk thumbnail scans or unbounded parallel prewarming at startup.

Deferred thumbnail cache enhancements:

- Show thumbnail cache usage in settings.
- Provide a manual clear-thumbnail-cache button.
- Dynamically adjust the cache limit based on device storage.

## Player Queue Panel And Gesture Recovery Completion

- The video player `视频列表 / 播放队列` panel refinement is complete.
- Entry: the queue/list button inside the video player page.
- Shape: right-side dark translucent glass panel, close to full-screen height, with compressed header spacing and more visible queue rows.
- Visual behavior: player-side panels now use a more translucent background and no obvious heavy border while keeping rounded corners and readable text.
- Queue data is real playback queue data only. Do not use mock queue entries or hard-coded titles/counts.
- Search inside the current queue is a real filter.
- Filters are real: `全部 / 未看 / 已看 / 失效`.
- The current playing item uses a dark purple highlight and keeps the `正在播放` label.
- The circular play overlay on the current item's thumbnail was removed so the thumbnail is not covered.
- Tapping another playable queue item switches playback.
- Unavailable queue items cannot be played and can only be removed from the list.
- The queue panel must not add permanent-delete or dangerous file-management actions.
- Player gesture recovery is complete for the current player instance.
- Double-tap right performs real fast-forward; double-tap left performs real rewind.
- Horizontal drag performs real seek on release.
- Left vertical drag changes activity window brightness; right vertical drag changes media volume.
- The gesture `pointerInput` is bound to the current `mediaFile.uri` and `player` so switching queue items does not leave gestures holding an old player reference.
- The bottom system gesture safe area remains reserved so bottom-up system navigation does not trigger brightness, volume, or seek.

Regression boundaries:

- Do not let the queue panel become short/wrap-content again.
- Do not restore the circular play overlay on the current queue thumbnail.
- Do not turn queue search or filters into UI-only fake controls.
- Do not make unavailable queue items playable.
- Do not add permanent delete to the player queue panel.
- Do not let gestures show hints without executing the real action.
- Do not leave transparent overlays intercepting player gestures after panels close.
- Do not restore thick borders or pure-black solid backgrounds for player function panels.

Deferred player queue enhancements:

- Queue sorting.
- A small "locate current item" affordance.
- Further queue information-density tuning.
- Batch cleanup for unavailable queue items.

## Video Module Final Closure Baseline

The video module has entered the closure/polish stage. The following capabilities are treated as completed baseline behavior:

- Video home: search, add, list/grid switch, sorting, multi-select, hide, delete, hidden records, field settings, folder thumbnail cache, and thumbnail prewarm.
- Video folder detail: search, sorting, list/grid switch, multi-select, hide, delete, unavailable-file state, and one-video-one-thumbnail cache binding.
- Video player: play/pause, progress, speed, resize mode, screenshot, screen lock, orientation toggle, player queue panel, subtitle style, external SRT subtitle sync, AB loop, sleep timer, gestures, equalizer, picture adjustment, decode/format info, and basic playback diagnostics.
- Thumbnail system: disk cache under app cache, invalid black/white/low-information frame fallback, background prewarm, per-video cache key, delete cleanup, and 300MB/260MB cache trim.
- Delete/hide safety: hide never deletes files; delete is real local video deletion with confirmation and the existing permission chain; permission denial or failure must not be reported as success.

High-risk items remain deferred and must not be treated as completed:

- FFmpeg/mpv/native decoder.
- Hardware/software decoder switching.
- Embedded subtitle time offset.
- Full ASS/SSA advanced subtitle sync.
- Delete authorization flow redesign.
- Showing hidden files and `.nomedia` scan strategy.
- Batch resolution/frame-rate statistics.
- Complex two-finger gestures.
- Advanced picture filters such as sharpening and dark enhancement.

Real-device acceptance checklist before moving out of video closure:

- Video home: search, add, view switch, sorting, field settings, multi-select hide/delete, hidden records, folder thumbnails.
- Folder detail: search, sorting, list/grid, multi-select hide/delete, unavailable files, thumbnails, normal playback entry.
- Player basics: play/pause, progress seek bar, speed, resize mode, screenshot, lock, orientation, Home pause behavior.
- Player panels: queue panel, subtitles, subtitle sync, equalizer, picture adjustment, AB loop, sleep timer, decode/info panels.
- Gestures: double-tap seek, horizontal seek, brightness, volume, bottom system gesture safe area.
- Data safety: hide/delete boundaries, unavailable-file removal, thumbnail cleanup, no fake success after permission denial.

Do not regress:

- Do not turn hide into delete.
- Do not fake delete success.
- Do not restore `移除` as the main wording for non-destructive hide behavior.
- Do not make queue search/filters fake UI.
- Do not let gestures show hints without executing real actions.
- Do not cache black/white invalid thumbnails.
- Do not let thumbnails cross-bind between different videos.
- Do not allow unavailable files to play.
- Do not add dangerous delete actions to the player queue panel.
- Do not add permissions, dependencies, or scanner-wide changes without a separate approved task.
- Do not move into music or novel modules by regressing video-module behavior.

## Video Module Recent Completion Sync

Recent shipped video work includes `9d82c47 feat(video): add cache management and refine controls`, `002d11e fix(video): clarify advanced panel roadmap`, and `9bdbd5d feat(video): improve subtitle sync controls`.

Video home `More` panel completion:

- The video home `More` panel is unified as a dark translucent panel.
- The panel shell has a fixed height: the header remains fixed and the content area scrolls internally.
- Expanded sections such as cache management, fields, advanced, and hidden records must not stretch the outer panel.
- Hidden records are placed at the bottom of the panel.
- The advanced entry height is aligned with other folded entries.

Cache management completion:

- Thumbnail cache management uses the real cache directory: `cache/video_thumbnails/`.
- The UI displays the real current thumbnail cache size.
- Clearing cache only clears thumbnail cache files and never deletes local video files.
- Thumbnail cache size remains bounded: about `300MB` maximum, trimmed to about `260MB`.

Player function entry completion:

- The default player function entries are now scoped to: speed, portrait/landscape, equalizer, picture adjustment, and expand.
- Expanded player functions show screenshot, info, queue/list, AB loop, sleep timer, control bar, gestures, decode, advanced, and collapse.
- Audio track remains only in the top bar and is no longer duplicated in the function area.
- Player features were not removed; only the entry hierarchy was cleaned up.

Advanced panel completion and boundary:

- The old misleading "future specialty" panel has been replaced by the `Advanced` panel.
- The stale `subtitle time sync / future` placeholder has been removed or rewritten.
- External SRT subtitle time sync is complete.
- Embedded subtitle time offset remains deferred.
- Full ASS/SSA advanced subtitle sync remains deferred.
- Decoder enhancement, FFmpeg/mpv/native decoder work, and complex picture filters remain deferred high-risk work.

Subtitle style and subtitle time sync completion:

- The subtitle style panel has been tightened while keeping the right-side player panel style.
- Subtitle time sync is embedded inside the subtitle style panel.
- Tapping subtitle time no longer opens a separate subtitle-time page.
- Without an external SRT, the panel shows `需外挂 SRT`; plus/minus and drag adjustment are disabled and must not fake success.
- With an external SRT, plus/minus adjusts subtitle offset. Short press changes exactly `0.1s`; long press continuously adjusts.
- Vertical drag on the central value can continuously cross multiple `0.1s` steps, shows the draft value during drag, and applies the final offset on release.
- The supported offset range remains about `-5.0s` to `+5.0s`.
- The implementation continues to reuse the existing external SRT offset path and must not rewrite a separate SRT parser.

Current video-module boundaries:

- Current subtitle sync support is external `.srt` only.
- Do not document embedded subtitle offset as complete.
- Do not document ASS/SSA advanced subtitle sync as complete.
- Do not add FFmpeg, mpv, native decoder, permissions, dependencies, or cross-module changes as part of these completed items.
- Music and novel modules are not changed by this video-module completion work.

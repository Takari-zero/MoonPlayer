# Moon播放器 视频模块说明

## 1. 模块定位

视频模块是 Moon播放器 的本地视频库和本地视频播放器。

目标：

- 自动发现本地视频。
- 支持用户手动添加视频文件夹。
- 按文件夹管理视频。
- 提供成熟横屏视频播放体验。
- 支持删除、隐藏、播放进度和继续播放。

不做：

- 在线视频
- 弹幕
- 云端字幕搜索
- 账号同步观看记录
- 视频后台播放
- 视频推荐流

## 2. 视频主页结构

视频主页是底部导航“视频”的一级页面。

页面结构：

- 顶部栏
  - 标题：视频
  - 副标题：本地视频
  - 搜索入口
  - 添加文件夹入口
  - 重新扫描入口
  - 更多三点入口
- 内容区
  - 直接展示视频文件夹列表或网格
  - 不显示“视频文件夹”section 标题，除非后续重新确认
  - 不显示“本地 / 添加文件夹 / 刷新”等内容区快捷按钮
- 继续播放 FAB

视频主页不应显示“自动扫描”来源文字。自动扫描是能力，不是用户需要反复看到的状态标签。

## 3. 视频主页列表模式

列表模式用于展示视频文件夹。

每个文件夹项包含：

- 左侧文件夹图标或预览卡片
- 右上角视频数量角标
- 文件夹名
- 视频数量，例如“13 个视频”

规则：

- 列表项高度紧凑。
- 左侧文件夹图标不宜过大。
- 文件夹名和数量层级清楚。
- 不显示“自动扫描”文字。
- 点击文件夹进入视频文件夹详情页。
- 长按文件夹进入多选模式，并默认选中当前文件夹。

## 4. 视频主页网格模式

网格模式用于更紧凑地展示多个视频文件夹。

规则：

- 3 列。
- Item 必须居中。
- 文件夹图标尺寸控制在 86dp - 100dp 宽、64dp - 76dp 高。
- 文件夹名居中，最多 1 到 2 行。
- 视频数量居中。
- 数量角标放在文件夹图标右上角。
- 角标使用蓝色，不使用粉色。
- 网格左右边距统一。
- FAB 不能遮挡最后一行。

## 5. 视频主页三点功能面板

点击视频主页右上角三点，打开功能面板，不直接进入多选模式。

功能面板应包含：

- 搜索
- 添加
- 列表
- 网格
- 名称排序
- 数量排序
- 多选
- 日期排序
- 字段
- 高级

当前明确不要放入：

- 来源
- 字段排序项
- 继续
- 刷新

字段区域默认折叠，点击“字段”后展开。

字段区域可包含：

- 缩略图
- 长度
- 文件扩展名
- 播放时间
- 分辨率
- 帧率
- 路径
- 大小
- 日期

高级区域默认折叠，点击“高级”后展开。

高级区域可包含：

- 在缩略图上显示长度
- 显示隐藏文件和文件夹
- 识别 `.nomedia`

功能面板规则：

- 深色圆角面板。
- 图标 + 文案。
- 点击外部可关闭。
- “取消 / 完成”按钮位于底部。
- 不要出现无效按钮。
- 暂未实现功能必须 Toast 说明“后续实现”。

## 6. 视频主页多选文件夹

进入方式：

- 三点功能面板点击“多选”。
- 长按文件夹，并默认选中当前文件夹。

多选模式：

- 顶部显示“已选择 X 项”。
- 左侧为取消按钮。
- 右侧提供全选、移除、删除等操作。
- 点击文件夹切换选中状态，不进入详情页。

文件夹删除策略：

- 从列表移除：不删除真实文件，写入隐藏记录，自动扫描不再显示。
- 永久删除：高风险操作，必须二次确认；如果系统不允许删除，提示权限问题，并可降级为从列表移除。

## 7. 视频二级页面结构

视频二级页面是视频文件夹详情页。

顶部栏：

- 左侧返回箭头。
- 中间文件夹名。
- 右侧搜索、视图切换、更多。
- 顶部栏和列表内容要有明显层级区分。

内容区：

- 展示当前文件夹内视频。
- 支持搜索当前文件夹。
- 支持列表 / 网格切换。
- 支持排序。
- 支持单项更多菜单。
- 支持多选删除。

## 8. 视频二级页面列表项

每个视频项包含：

- 左侧缩略图。
- 缩略图右下角时长标签。
- 视频标题，最多 2 行。
- 文件大小。
- 上次播放进度。
- 右侧三点菜单。

规则：

- 列表项紧凑但不拥挤。
- 缩略图圆角统一。
- 时长标签为半透明深色背景。
- 最近播放或上次进度使用蓝色弱提示。
- 长按视频进入多选模式，并默认选中当前视频。

## 9. 视频二级页面排序、搜索、多选删除

搜索：

- 搜索当前文件夹内视频标题。
- 搜索入口默认是图标，不常驻大搜索框。

排序：

- 可按名称、日期、大小、时长等字段扩展。
- 未实现的排序项必须 Toast 说明“后续实现”。

多选删除：

- 从列表移除：写入隐藏记录，不删除真实文件。
- 永久删除：必须二次确认。
- 删除成功后更新 UI、最近播放、播放队列和视频进度。
- 删除失败不得从 UI 移除。

## 10. 视频播放页结构

视频播放页默认横屏。

顶部栏只保留：

- 返回
- 标题
- 音轨
- 字幕
- 更多

底部控制栏包含：

- 当前时间
- 进度条
- 总时长
- 上一集
- 播放 / 暂停
- 下一集
- 适应屏幕 / 画面比例入口

左侧快捷工具条跟随顶部 / 底部控制层同步显示和隐藏。

## 11. 视频播放页手势

保留手势：

- 单击显示 / 隐藏控制层。
- 左右滑动快进 / 快退。
- 左右区域上下滑动调节亮度 / 音量。
- 右侧边缘左滑返回或关闭面板。

手势优先级：

1. 如果音轨、字幕、均衡器、倍速等面板打开，右侧边缘左滑优先关闭面板。
2. 如果没有面板打开，右侧边缘左滑返回上一页。
3. 非边缘横向滑动用于快进 / 快退。
4. 垂直滑动用于亮度 / 音量。

## 12. 视频播放页工具面板

快捷工具条建议包含：

- 均衡器
- 倍速
- 截图
- 画面
- 更多

规则：

- “设置”按钮应改为“均衡器”。
- 音轨入口只保留在顶部栏，避免重复。
- 字幕入口只保留在顶部栏，避免重复。
- 适应屏幕放到底部控制栏右侧。
- 工具条跟随控制层显示和隐藏。

## 13. 倍速播放

倍速功能应支持：

- 常用快捷值，例如 0.5x、1.0x、1.25x、1.5x、2.0x。
- 自定义 0 到 5 范围内的任意倍速。
- 使用滑杆或数值步进输入。
- 设置后立即影响当前视频播放速度。

规则：

- 0 表示极限下限，实际播放可按播放器能力限制处理。
- UI 必须明确显示当前倍速。
- 面板打开后不应自动消失，需用户完成、关闭或返回手势关闭。

## 14. 音轨和字幕

音轨入口：

- 放在视频播放页顶部栏右侧。
- 打开右侧半透明面板。
- 面板包含音轨同步说明、当前偏移值、归零、正负步进按钮。
- 本阶段先做 UI 和状态，不做高风险底层真实音频延迟。

字幕入口：

- 放在视频播放页顶部栏右侧。
- 支持外挂字幕入口。
- 支持字幕提前 / 延后或加减速 UI。
- 本阶段可以先做入口和面板，未完成能力必须 Toast 说明。

面板规则：

- 打开后不自动消失。
- 点关闭、设置完成或右侧边缘左滑关闭。
- 不遮挡关键播放控制。

## 15. 视频删除策略

删除分为：

- 从列表移除
- 永久删除

从列表移除：

- 不删除真实文件。
- 写入隐藏记录。
- 自动扫描过滤隐藏记录。
- 重启后不再显示。

永久删除：

- 必须二次确认。
- 尝试删除真实文件。
- 删除成功后更新 UI、队列、进度、最近播放和 DataStore。
- 删除失败时提示权限问题。

## 16. 视频进度策略

视频必须记录播放进度。

规则：

- key 优先使用视频 uri。
- 退出播放页时保存进度。
- 如果接近结尾，例如剩余小于等于 5 秒，可以重置为 0。
- 再次播放同一视频时从上次位置继续。
- 文件夹详情页显示“上次播放到 xx:xx”。

## 17. Recent Video Module Completion Status

Video home `More` panel:

- The video home `More` panel uses a dark translucent style with fixed shell height, fixed header, and internally scrollable content.
- Expanded entries such as cache management, fields, advanced, and hidden records must not stretch the outer panel.
- Hidden records are placed at the bottom.
- The advanced entry height matches the other folded entries.

Cache management:

- The video thumbnail cache directory is `cache/video_thumbnails/`.
- The cache management entry shows real cache usage, not mock data.
- Clearing cache only clears thumbnail cache files and does not delete local videos.
- Thumbnail cache remains bounded at about `300MB` and trims to about `260MB`.

Player function area:

- Default player function entries are speed, portrait/landscape, equalizer, picture adjustment, and expand.
- Expanded entries are screenshot, info, queue/list, AB loop, sleep timer, control bar, gestures, decode, advanced, and collapse.
- Audio track selection is kept in the top bar only.
- The feature set is not reduced; the entry hierarchy is only organized.

Advanced panel and subtitle boundary:

- The advanced panel no longer shows the misleading generic `subtitle time sync / future` placeholder.
- External `.srt` subtitle time sync is complete.
- Embedded subtitle time offset remains deferred.
- ASS/SSA advanced subtitle sync remains deferred.
- FFmpeg/mpv/native decoder, decoder enhancement, and complex picture filters remain deferred high-risk items.

Subtitle style and inline subtitle timing:

- Subtitle time sync is embedded inside the subtitle style panel.
- Tapping subtitle time no longer opens an independent subtitle-time page.
- Without external SRT, the panel shows `需外挂 SRT`, disables plus/minus and drag controls, and must not fake success.
- With external SRT, plus/minus adjusts offset; short press changes `0.1s`, long press repeats quickly.
- Vertical drag on the center value can adjust multiple `0.1s` steps, previews the draft value during drag, and applies the final offset on release.
- Offset remains limited to about `-5.0s` through `+5.0s`.
- The implementation reuses the existing external SRT offset path and does not introduce a separate parser.

Boundaries:

- Current subtitle sync support is external `.srt` only.
- Do not mark embedded subtitle offset or ASS/SSA advanced sync as complete.
- Do not add FFmpeg/mpv/native decoder, permissions, dependencies, Gradle changes, Manifest changes, music changes, or novel changes as part of this video status.

## 18. Latest Player Sleep And Gesture Fixes

Sleep pause recovery:

- Sleep-mode automatic pause is a one-shot pause state and must not permanently block manual playback.
- After timed sleep pauses playback, tapping the bottom play button clears the consumed sleep-triggered state and resumes playback instead of immediately pausing again.
- After `pause after current video` triggers, tapping play resumes deterministically:
  - If the queue has a next item, the player moves to the next item and starts playback.
  - If the queue has no next item, the current video seeks to the beginning and starts playback.
- `Player.STATE_ENDED` is handled explicitly because a plain `player.play()` can be ineffective when the player is parked at the end.
- Sleep features remain available after manual playback resumes; users can set a new sleep timer later.

Top-center gesture safe area:

- The player has a top-center gesture safe area to avoid accidental brightness or volume changes when swiping down from the upper middle of the screen.
- Safe area rule: about `96dp` from the top and the horizontal center range from `25%` to `75%` of screen width.
- The safe area is evaluated from the gesture start point. If a vertical gesture starts inside it, that gesture does not trigger brightness or volume adjustment.
- This does not disable the whole top edge: left-side brightness and right-side volume gestures outside the safe area remain available.
- Click, double-tap, control bar, subtitles, sleep, AB loop, and the player queue are not changed by this safe area.

## 19. Video Home And Folder Detail UI Fixes

Video home search:

- The video home search field height and text rendering are optimized.
- Search input, placeholder, and cursor should not be vertically clipped.
- The search field uses a stable height and text style while remaining single-line.

Video home long folder names:

- Video home folder titles may show up to 2 lines.
- If the second line still overflows, it uses ellipsis.
- This is intended to help distinguish folders with similar long names.

Video folder detail search:

- The video folder detail search field uses the same stable height and text rendering direction as the video home search field.
- Short and long query text should not be vertically clipped.
- The field remains single-line and should keep the cursor position readable.

Bottom navigation:

- A shared bottom navigation component exists at `app/src/main/java/com/shenghui/localvibe/core/ui/MoonBottomNavigationBar.kt`.
- The video home page and the video folder detail page both reuse this same component, so the bottom bar visuals must remain identical.
- The video folder detail page shows the bottom bar with the `Video` tab highlighted.
- Folder detail bottom navigation is for top-level module navigation: `Music` opens the music module, `Novel` opens the novel module, and `Me` opens the profile page.
- Current interaction convention: tapping the `Video` tab while already in a video folder detail page does not force navigation back to the video home page. Use the top-left back button to return to the video home page.

Layout boundaries:

- Folder detail list content must avoid the bottom navigation bar.
- The lower-right play FAB must not overlap the bottom navigation bar.
- Selection mode controls must not overlap the bottom navigation bar.
- The video player page was not changed for this UI fix.

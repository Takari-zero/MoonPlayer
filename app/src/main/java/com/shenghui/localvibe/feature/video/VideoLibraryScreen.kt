package com.shenghui.localvibe.feature.video

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.shenghui.localvibe.core.scanner.LocalMediaFile
import com.shenghui.localvibe.feature.video.model.VideoFolderUiModel

private val VideoBackground = Color(0xFF0B0D14)
private val VideoSurface = Color(0xFF101722)
private val VideoSurfaceSoft = Color(0xFF151E2B)
private val VideoPrimary = Color(0xFF4D8DFF)
private val VideoPrimarySoft = Color(0xFF8AB6FF)
private val VideoTextPrimary = Color(0xFFF5F7FA)
private val VideoTextSecondary = Color(0xFFA8B2C2)
private val VideoTextMuted = Color(0xFF6F7A8A)

private enum class VideoLibrarySortMode {
    NAME,
    COUNT
}

@Composable
fun VideoLibraryScreen(
    videoFolders: List<VideoFolderUiModel>,
    permissionDeniedMessage: String?,
    recentVideoFile: LocalMediaFile?,
    onAddFolder: () -> Unit,
    onOpenFolder: (VideoFolderUiModel) -> Unit,
    onContinueVideo: (LocalMediaFile) -> Unit,
    onRemoveFolders: (List<VideoFolderUiModel>) -> Unit,
    onRescanVideo: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var searchKeyword by rememberSaveable { mutableStateOf("") }
    var isGridMode by rememberSaveable { mutableStateOf(false) }
    var showMorePanel by rememberSaveable { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf(VideoLibrarySortMode.NAME) }
    var isMultiSelectMode by rememberSaveable { mutableStateOf(false) }
    var selectedFolderIds by rememberSaveable { mutableStateOf(emptySet<String>()) }
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val shownFolders = remember(videoFolders, searchKeyword, sortMode) {
        val filtered = if (searchKeyword.isBlank()) {
            videoFolders
        } else {
            val keyword = searchKeyword.trim()
            videoFolders.filter { item ->
                item.folder.name.contains(keyword, ignoreCase = true) ||
                    item.videos.any { it.name.contains(keyword, ignoreCase = true) }
            }
        }
        when (sortMode) {
            VideoLibrarySortMode.NAME -> filtered.sortedBy { it.folder.name.lowercase() }
            VideoLibrarySortMode.COUNT -> filtered.sortedByDescending { it.videos.size }
        }
    }

    BackHandler(enabled = isSearching) {
        searchKeyword = ""
        isSearching = false
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(VideoBackground),
        containerColor = VideoBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (recentVideoFile != null) {
                        onContinueVideo(recentVideoFile)
                    } else {
                        Toast.makeText(context, "暂无可继续播放的视频", Toast.LENGTH_SHORT).show()
                    }
                },
                containerColor = VideoPrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "继续播放",
                    modifier = Modifier.size(34.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(VideoBackground)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                VideoHeader(
                    isSearching = isSearching,
                    isGridMode = isGridMode,
                    onToggleSearch = {
                        if (isSearching) searchKeyword = ""
                        isSearching = !isSearching
                    },
                    onAddFolder = onAddFolder,
                    onToggleViewMode = {
                        isGridMode = !isGridMode
                    },
                    onMore = {
                        showMorePanel = true
                    }
                )

                if (isMultiSelectMode) {
                    VideoFolderMultiSelectBar(
                        selectedCount = selectedFolderIds.size,
                        onCancel = {
                            isMultiSelectMode = false
                            selectedFolderIds = emptySet()
                        },
                        onSelectAll = {
                            selectedFolderIds = shownFolders.map { it.folder.id }.toSet()
                        },
                        onRemoveSelected = {
                            val selectedFolders = shownFolders.filter { it.folder.id in selectedFolderIds }
                            if (selectedFolders.isEmpty()) {
                                Toast.makeText(context, "请先选择文件夹", Toast.LENGTH_SHORT).show()
                            } else {
                                onRemoveFolders(selectedFolders)
                                Toast.makeText(context, "已移除 ${selectedFolders.size} 个文件夹", Toast.LENGTH_SHORT).show()
                                selectedFolderIds = emptySet()
                                isMultiSelectMode = false
                            }
                        }
                    )
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 8.dp,
                        bottom = 104.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!permissionDeniedMessage.isNullOrBlank()) {
                        item {
                            PermissionHint(message = permissionDeniedMessage)
                        }
                    }

                    if (shownFolders.isEmpty()) {
                        item {
                            EmptyVideoState(
                                text = if (searchKeyword.isBlank()) {
                                    "暂无视频，下载视频后会自动显示，也可以手动添加文件夹"
                                } else {
                                    "没有找到相关视频"
                                }
                            )
                        }
                    } else if (isGridMode) {
                        item {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                state = gridState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(
                                        ((shownFolders.size + 2) / 3 * 132)
                                            .coerceAtLeast(132)
                                            .dp
                                    ),
                                horizontalArrangement = Arrangement.spacedBy(18.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp),
                                userScrollEnabled = false
                            ) {
                                items(
                                    items = shownFolders,
                                    key = { it.folder.id }
                                ) { item ->
                                    VideoFolderGridItem(
                                        item = item,
                                        isSelectionMode = isMultiSelectMode,
                                        isSelected = item.folder.id in selectedFolderIds,
                                        onToggleSelected = {
                                            selectedFolderIds = selectedFolderIds.toggle(item.folder.id)
                                        },
                                        onClick = {
                                            if (isMultiSelectMode) {
                                                selectedFolderIds = selectedFolderIds.toggle(item.folder.id)
                                            } else {
                                                onOpenFolder(item)
                                            }
                                        },
                                        onLongClick = {
                                            isMultiSelectMode = true
                                            selectedFolderIds = setOf(item.folder.id)
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        shownFolders.forEach { item ->
                            item(key = item.folder.id) {
                                    VideoFolderCard(
                                        item = item,
                                        compact = false,
                                        isSelectionMode = isMultiSelectMode,
                                        isSelected = item.folder.id in selectedFolderIds,
                                        onToggleSelected = {
                                            selectedFolderIds = selectedFolderIds.toggle(item.folder.id)
                                        },
                                        onClick = {
                                            if (isMultiSelectMode) {
                                                selectedFolderIds = selectedFolderIds.toggle(item.folder.id)
                                            } else {
                                                onOpenFolder(item)
                                            }
                                        },
                                        onLongClick = {
                                            isMultiSelectMode = true
                                            selectedFolderIds = setOf(item.folder.id)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                            }
                        }
                    }
                }
            }

            if (isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(8f)
                        .clickable {
                            searchKeyword = ""
                            isSearching = false
                        }
                )
                FloatingSearchBar(
                    value = searchKeyword,
                    onValueChange = { searchKeyword = it },
                    placeholder = "搜索视频或文件夹",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 76.dp)
                        .zIndex(9f)
                )
            }
            if (showMorePanel) {
                VideoLibraryMorePanel(
                    isGridMode = isGridMode,
                    sortMode = sortMode,
                    onDismiss = { showMorePanel = false },
                    onSearch = {
                        showMorePanel = false
                        isSearching = true
                    },
                    onListMode = {
                        isGridMode = false
                        showMorePanel = false
                    },
                    onGridMode = {
                        isGridMode = true
                        showMorePanel = false
                    },
                    onAddFolder = {
                        showMorePanel = false
                        onAddFolder()
                    },
                    onRescan = {
                        showMorePanel = false
                        onRescanVideo()
                        Toast.makeText(context, "已重新扫描视频", Toast.LENGTH_SHORT).show()
                    },
                    onSortByName = {
                        sortMode = VideoLibrarySortMode.NAME
                        showMorePanel = false
                    },
                    onSortByCount = {
                        sortMode = VideoLibrarySortMode.COUNT
                        showMorePanel = false
                    },
                    onMultiDelete = {
                        showMorePanel = false
                        isMultiSelectMode = true
                        selectedFolderIds = emptySet()
                    },
                    onMore = {
                        showMorePanel = false
                        Toast.makeText(context, "更多功能后续实现", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun VideoHeader(
    isSearching: Boolean,
    isGridMode: Boolean,
    onToggleSearch: () -> Unit,
    onAddFolder: () -> Unit,
    onToggleViewMode: () -> Unit,
    onMore: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VideoBackground)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "视频",
                    style = MaterialTheme.typography.headlineMedium,
                    color = VideoTextPrimary
                )
                Text(
                    text = "本地视频",
                    style = MaterialTheme.typography.bodySmall,
                    color = VideoTextMuted
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onToggleSearch) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = if (isSearching) "关闭搜索" else "搜索",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onAddFolder) {
                    Icon(Icons.Filled.CreateNewFolder, contentDescription = "添加文件夹", tint = Color.White)
                }
                IconButton(onClick = onToggleViewMode) {
                    Icon(
                        imageVector = if (isGridMode) Icons.Filled.ViewList else Icons.Filled.GridView,
                        contentDescription = "切换视图",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onMore) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun VideoLibraryMorePanel(
    isGridMode: Boolean,
    sortMode: VideoLibrarySortMode,
    onDismiss: () -> Unit,
    onSearch: () -> Unit,
    onListMode: () -> Unit,
    onGridMode: () -> Unit,
    onAddFolder: () -> Unit,
    onRescan: () -> Unit,
    onSortByName: () -> Unit,
    onSortByCount: () -> Unit,
    onMultiDelete: () -> Unit,
    onMore: () -> Unit
) {
    var fieldsExpanded by rememberSaveable { mutableStateOf(false) }
    var advancedExpanded by rememberSaveable { mutableStateOf(false) }
    var showThumbnail by rememberSaveable { mutableStateOf(true) }
    var showDuration by rememberSaveable { mutableStateOf(true) }
    var showExtension by rememberSaveable { mutableStateOf(false) }
    var showPlayTime by rememberSaveable { mutableStateOf(false) }
    var showResolution by rememberSaveable { mutableStateOf(false) }
    var showFrameRate by rememberSaveable { mutableStateOf(false) }
    var showPath by rememberSaveable { mutableStateOf(false) }
    var showSize by rememberSaveable { mutableStateOf(false) }
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showDurationOnThumbnail by rememberSaveable { mutableStateOf(true) }
    var showHiddenFiles by rememberSaveable { mutableStateOf(false) }
    var recognizeNoMedia by rememberSaveable { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(314.dp)
                .clickable(onClick = {}),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF282828))
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 18.dp, vertical = 18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("视图模式", color = VideoTextPrimary, style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                            MorePanelOption(
                                icon = Icons.Filled.Search,
                                label = "搜索",
                                selected = true,
                                onClick = onSearch
                            )
                            MorePanelOption(
                                icon = Icons.Filled.CreateNewFolder,
                                label = "添加",
                                selected = false,
                                onClick = onAddFolder
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("布局", color = VideoTextPrimary, style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                            MorePanelOption(
                                icon = Icons.Filled.ViewList,
                                label = "列表",
                                selected = !isGridMode,
                                onClick = onListMode
                            )
                            MorePanelOption(
                                icon = Icons.Filled.GridView,
                                label = "网格",
                                selected = isGridMode,
                                onClick = onGridMode
                            )
                        }
                    }
                }
                PanelDivider()
                Text("排序", color = VideoTextPrimary, style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MorePanelOption(Icons.Filled.SortByAlpha, "名称", sortMode == VideoLibrarySortMode.NAME, onSortByName)
                    MorePanelOption(Icons.Filled.Movie, "数量", sortMode == VideoLibrarySortMode.COUNT, onSortByCount)
                    MorePanelOption(Icons.Filled.Schedule, "多选", false, onMultiDelete)
                    MorePanelOption(Icons.Filled.DateRange, "日期", false, onMore)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    MorePanelOption(Icons.Filled.Refresh, "重扫", false, onRescan)
                    MorePanelOption(Icons.Filled.MoreHoriz, "更多", false, onMore)
                }
                PanelDivider()
                VideoPanelExpandableHeader(
                    title = "字段",
                    expanded = fieldsExpanded,
                    onClick = { fieldsExpanded = !fieldsExpanded }
                )
                if (fieldsExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            VideoPanelCheckOption("缩略图", showThumbnail) { showThumbnail = it }
                            VideoPanelCheckOption("长度", showDuration) { showDuration = it }
                            VideoPanelCheckOption("文件扩展名", showExtension) { showExtension = it }
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            VideoPanelCheckOption("播放时间", showPlayTime) { showPlayTime = it }
                            VideoPanelCheckOption("分辨率", showResolution) { showResolution = it }
                            VideoPanelCheckOption("帧率", showFrameRate) { showFrameRate = it }
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            VideoPanelCheckOption("路径", showPath) { showPath = it }
                            VideoPanelCheckOption("大小", showSize) { showSize = it }
                            VideoPanelCheckOption("日期", showDate) { showDate = it }
                        }
                    }
                }
                PanelDivider()
                VideoPanelExpandableHeader(
                    title = "高级",
                    expanded = advancedExpanded,
                    onClick = { advancedExpanded = !advancedExpanded }
                )
                if (advancedExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        VideoPanelSwitchOption(
                            label = "在缩略图上显示长度",
                            checked = showDurationOnThumbnail,
                            onCheckedChange = { showDurationOnThumbnail = it }
                        )
                        VideoPanelSwitchOption(
                            label = "显示隐藏文件和文件夹",
                            checked = showHiddenFiles,
                            onCheckedChange = { showHiddenFiles = it }
                        )
                        VideoPanelSwitchOption(
                            label = "识别 .nomedia",
                            checked = recognizeNoMedia,
                            onCheckedChange = { recognizeNoMedia = it }
                        )
                    }
                }
                PanelDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "取消",
                        color = VideoTextPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                    Text(
                        text = "完成",
                        color = VideoPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoPanelExpandableHeader(
    title: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = VideoTextPrimary, style = MaterialTheme.typography.titleMedium)
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = VideoTextSecondary
        )
    }
}

@Composable
private fun VideoPanelCheckOption(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .width(86.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(
            text = label,
            color = VideoTextSecondary,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VideoPanelSwitchOption(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = VideoTextSecondary, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun MorePanelOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) VideoPrimary else Color.White,
            modifier = Modifier.size(26.dp)
        )
        Text(
            text = label,
            color = if (selected) VideoPrimary else VideoTextSecondary,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PanelDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.12f))
    )
}

@Composable
private fun VideoFolderMultiSelectBar(
    selectedCount: Int,
    onCancel: () -> Unit,
    onSelectAll: () -> Unit,
    onRemoveSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VideoBackground)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "已选择 $selectedCount 项",
            color = VideoTextPrimary,
            style = MaterialTheme.typography.titleMedium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "取消",
                color = VideoTextSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
            Text(
                text = "全选",
                color = VideoPrimarySoft,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onSelectAll)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
            Text(
                text = "移除",
                color = Color(0xFFF97066),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onRemoveSelected)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun PermissionHint(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF241B1F)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFFFB4AB),
            modifier = Modifier.padding(14.dp)
        )
    }
}

@Composable
private fun FloatingSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = {}),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF2111722))
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = VideoPrimarySoft) },
            trailingIcon = {
                if (value.isNotBlank()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "清空搜索", tint = VideoTextSecondary)
                    }
                }
            },
            placeholder = { Text(placeholder, color = VideoTextMuted) }
        )
    }
}

@Composable
private fun EmptyVideoState(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 50.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(VideoSurfaceSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Movie, contentDescription = null, tint = VideoPrimarySoft, modifier = Modifier.size(30.dp))
        }
        Text(text = text, color = VideoTextSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoFolderCard(
    item: VideoFolderUiModel,
    compact: Boolean,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelected: () -> Unit = {},
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        if (compact) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VideoSurface)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FolderPreview(videoCount = item.videos.size, compact = true)
                FolderText(item = item, compact = true)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VideoBackground)
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Checkbox(checked = isSelected, onCheckedChange = { onToggleSelected() })
                }
                FolderPreview(videoCount = item.videos.size, compact = false)
                FolderText(item = item, compact = false, modifier = Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoFolderGridItem(
    item: VideoFolderUiModel,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelected: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box {
            FolderPreview(videoCount = item.videos.size, compact = true)
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelected() },
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
        }
        Text(
            text = item.folder.name,
            style = MaterialTheme.typography.titleSmall,
            color = VideoTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${item.videos.size} 个视频",
            style = MaterialTheme.typography.bodySmall,
            color = VideoTextSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun FolderPreview(
    videoCount: Int,
    compact: Boolean
) {
    Box(
        modifier = Modifier
            .size(width = if (compact) 94.dp else 76.dp, height = if (compact) 62.dp else 52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF263244)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.Folder,
            contentDescription = null,
            tint = Color(0xFF4D5967),
            modifier = Modifier.size(if (compact) 40.dp else 34.dp)
        )
        if (videoCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(VideoPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = videoCount.coerceAtMost(99).toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun FolderText(
    item: VideoFolderUiModel,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = item.folder.name,
            style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
            color = VideoTextPrimary,
            maxLines = if (compact) 2 else 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${item.videos.size} 个视频",
            style = MaterialTheme.typography.bodySmall,
            color = VideoTextSecondary
        )
    }
}

private fun Set<String>.toggle(value: String): Set<String> {
    return if (value in this) this - value else this + value
}

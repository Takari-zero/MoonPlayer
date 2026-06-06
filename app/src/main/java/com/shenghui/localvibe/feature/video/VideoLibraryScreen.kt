package com.shenghui.localvibe.feature.video

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.shenghui.localvibe.core.scanner.LocalMediaFile
import com.shenghui.localvibe.feature.video.model.VideoFolderUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val VideoBackground = Color(0xFF000204)
private val VideoSurface = Color(0xFF101012)
private val VideoSurfaceSoft = Color(0xFF15141A)
private val VideoCard = Color(0xFF08090B)
private val VideoPrimary = Color(0xFF8B5CFF)
private val VideoPrimarySoft = Color(0xFFD5C8FF)
private val VideoAccent = Color(0xFF8D55FF)
private val VideoAccentSoft = Color(0xFFBDA7FF)
private val VideoTextPrimary = Color(0xFFF8F7FB)
private val VideoTextSecondary = Color(0xFFBBB5C8)
private val VideoTextMuted = Color(0xFF7B7785)
private val VideoDivider = Color(0xFF18181D)

private enum class VideoLibrarySortMode {
    NAME,
    COUNT
}

@Composable
fun VideoLibraryScreen(
    videoFolders: List<VideoFolderUiModel>,
    isLoading: Boolean,
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
                modifier = Modifier.size(44.dp),
                containerColor = VideoAccent.copy(alpha = 0.78f),
                contentColor = VideoTextPrimary,
                shape = CircleShape
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "继续播放",
                    modifier = Modifier.size(24.dp)
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
                        top = 2.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    if (!permissionDeniedMessage.isNullOrBlank()) {
                        item {
                            PermissionHint(message = permissionDeniedMessage)
                        }
                    }

                    if (isLoading && searchKeyword.isBlank()) {
                        item {
                            LoadingVideoState()
                        }
                    } else if (shownFolders.isEmpty()) {
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
                                        ((shownFolders.size + 2) / 3 * 142)
                                            .coerceAtLeast(142)
                                            .dp
                                    ),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
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
                        .padding(horizontal = 16.dp, vertical = 58.dp)
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
            .padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = "视频",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 29.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = VideoTextPrimary
                )
                Text(
                    text = "本地视频",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                    color = VideoTextMuted
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                HeaderIconButton(
                    icon = Icons.Filled.Search,
                    contentDescription = if (isSearching) "关闭搜索" else "搜索",
                    selected = isSearching,
                    onClick = onToggleSearch
                )
                HeaderIconButton(
                    icon = Icons.Filled.CreateNewFolder,
                    contentDescription = "添加文件夹",
                    onClick = onAddFolder
                )
                HeaderIconButton(
                    icon = if (isGridMode) Icons.Filled.ViewList else Icons.Filled.GridView,
                    contentDescription = "切换视图",
                    selected = isGridMode,
                    onClick = onToggleViewMode
                )
                HeaderIconButton(
                    icon = Icons.Filled.MoreVert,
                    contentDescription = "更多",
                    onClick = onMore
                )
            }
        }
    }
}

@Composable
private fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.size(if (selected) 36.dp else 32.dp),
        shape = if (selected) CircleShape else RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) VideoPrimary.copy(alpha = 0.34f) else Color.Transparent
        ),
        border = if (selected) {
            BorderStroke(1.dp, VideoAccentSoft.copy(alpha = 0.2f))
        } else {
            null
        }
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (selected) VideoPrimarySoft else VideoTextSecondary.copy(alpha = 0.92f),
                modifier = Modifier.size(if (selected) 18.dp else 17.dp)
            )
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
                .width(326.dp)
                .clickable(onClick = {}),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = VideoSurface),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.065f))
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("视图模式", color = VideoTextPrimary, style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
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
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("布局", color = VideoTextPrimary, style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
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
                Text("排序", color = VideoTextPrimary, style = MaterialTheme.typography.titleSmall)
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
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
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
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = VideoTextPrimary, style = MaterialTheme.typography.titleSmall)
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
            .width(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) VideoPrimary.copy(alpha = 0.11f) else Color.White.copy(alpha = 0.03f))
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) VideoPrimarySoft else VideoTextSecondary.copy(alpha = 0.88f),
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = if (selected) VideoPrimarySoft else VideoTextMuted,
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
            .background(VideoDivider.copy(alpha = 0.9f))
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
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(VideoSurfaceSoft.copy(alpha = 0.86f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
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
        colors = CardDefaults.cardColors(containerColor = VideoSurface.copy(alpha = 0.98f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.075f))
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
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
private fun LoadingVideoState() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, start = 18.dp, end = 18.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = VideoCard.copy(alpha = 0.86f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.065f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = VideoPrimarySoft,
                strokeWidth = 2.dp
            )
            Text(
                text = "正在扫描本地视频…",
                color = VideoTextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun EmptyVideoState(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 38.dp, start = 18.dp, end = 18.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = VideoCard.copy(alpha = 0.84f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.065f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(VideoPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Movie, contentDescription = null, tint = VideoPrimarySoft, modifier = Modifier.size(30.dp))
            }
            Text(
                text = text.replace("，", "，\n"),
                color = VideoTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
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
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) VideoPrimary.copy(alpha = 0.08f) else Color.Transparent)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        if (compact) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) VideoPrimary.copy(alpha = 0.1f) else VideoSurface.copy(alpha = 0.68f))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FolderPreview(
                    folderName = item.folder.name,
                    previewUri = item.videos.firstOrNull()?.uri,
                    videoCount = item.videos.size,
                    compact = true
                )
                FolderText(item = item, compact = true)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Checkbox(checked = isSelected, onCheckedChange = { onToggleSelected() })
                }
                FolderPreview(
                    folderName = item.folder.name,
                    previewUri = item.videos.firstOrNull()?.uri,
                    videoCount = item.videos.size,
                    compact = false
                )
                FolderText(item = item, compact = false, modifier = Modifier.weight(1f))
                Icon(
                    Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = VideoTextMuted.copy(alpha = 0.64f),
                    modifier = Modifier.size(17.dp)
                )
            }
        }
        if (!compact) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(start = if (isSelectionMode) 44.dp else 0.dp)
                    .background(VideoDivider.copy(alpha = 0.42f))
            )
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) VideoPrimary.copy(alpha = 0.1f) else VideoCard.copy(alpha = 0.9f)
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) VideoPrimary.copy(alpha = 0.34f) else Color.White.copy(alpha = 0.04f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box {
                FolderPreview(
                    folderName = item.folder.name,
                    previewUri = item.videos.firstOrNull()?.uri,
                    videoCount = item.videos.size,
                    compact = true
                )
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = folderMetaText(item),
                style = MaterialTheme.typography.bodySmall,
                color = VideoTextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FolderPreview(
    folderName: String,
    previewUri: String?,
    videoCount: Int,
    compact: Boolean
) {
    val coverKind = remember(folderName) { folderCoverKind(folderName) }
    val context = LocalContext.current
    val thumbnail by produceState<Bitmap?>(initialValue = null, previewUri, compact) {
        value = if (compact || previewUri.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                loadFolderPreviewBitmap(context, previewUri)
            }
        }
    }
    Box(
        modifier = Modifier
            .size(width = if (compact) 88.dp else 104.dp, height = if (compact) 56.dp else 58.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(
                Brush.linearGradient(
                    colors = folderCoverGradient(coverKind)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.06f))
            )
        } else {
            FolderPreviewArtwork(coverKind = coverKind, compact = compact)
        }
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = VideoTextPrimary.copy(alpha = if (compact) 0.1f else 0.08f),
            modifier = Modifier.size(if (compact) 20.dp else 18.dp)
        )
        if (compact && videoCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .height(16.dp)
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (videoCount >= 100) "99+" else videoCount.toString(),
                    color = VideoPrimarySoft.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
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
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = item.folder.name,
            style = if (compact) {
                MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            } else {
                MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            },
            color = VideoTextPrimary,
            maxLines = if (compact) 2 else 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = folderMetaText(item),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = VideoTextSecondary
        )
    }
}

@Composable
private fun FolderPreviewArtwork(coverKind: FolderCoverKind, compact: Boolean) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        when (coverKind) {
            FolderCoverKind.Lens -> {
                drawCircle(Color(0xFFE86BCD).copy(alpha = 0.24f), radius = size.minDimension * 0.48f, center = Offset(size.width * 0.33f, size.height * 0.52f))
                drawCircle(Color(0xFF4CD7EA).copy(alpha = 0.18f), radius = size.minDimension * 0.38f, center = Offset(size.width * 0.52f, size.height * 0.48f))
                drawCircle(Color.Black.copy(alpha = 0.6f), radius = size.minDimension * 0.28f, center = Offset(size.width * 0.45f, size.height * 0.5f))
                drawCircle(Color.White.copy(alpha = 0.24f), radius = size.minDimension * 0.22f, center = Offset(size.width * 0.45f, size.height * 0.5f), style = Stroke(width = 1.6f))
                drawCircle(Color.White.copy(alpha = 0.2f), radius = size.minDimension * 0.055f, center = Offset(size.width * 0.39f, size.height * 0.42f))
                drawRect(Color.Black.copy(alpha = 0.22f), topLeft = Offset(size.width * 0.72f, 0f), size = androidx.compose.ui.geometry.Size(size.width * 0.28f, size.height))
            }
            FolderCoverKind.Theater -> {
                drawCircle(Color(0xFFFFC06A).copy(alpha = 0.3f), radius = size.minDimension * 0.38f, center = Offset(size.width * 0.52f, size.height * 0.08f))
                drawRect(Color.Black.copy(alpha = 0.18f), topLeft = Offset(0f, size.height * 0.32f), size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.68f))
                repeat(5) { line ->
                    val y = size.height * (0.42f + line * 0.085f)
                    drawLine(Color(0xFFFF8A45).copy(alpha = 0.16f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                }
                repeat(3) { row ->
                    val y = size.height * (0.54f + row * 0.11f)
                    repeat(5) { col ->
                        val x = size.width * (0.08f + col * 0.19f)
                        drawRoundRect(
                            Color(0xFFD45B37).copy(alpha = 0.28f),
                            topLeft = Offset(x, y),
                            size = androidx.compose.ui.geometry.Size(size.width * 0.14f, size.height * 0.07f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                        )
                    }
                }
            }
            FolderCoverKind.Mountain -> {
                drawCircle(Color(0xFFD7E2FF).copy(alpha = 0.14f), radius = size.minDimension * 0.16f, center = Offset(size.width * 0.8f, size.height * 0.24f))
                drawLine(Color(0xFFE4EAFF).copy(alpha = 0.32f), Offset(size.width * 0.02f, size.height * 0.74f), Offset(size.width * 0.33f, size.height * 0.34f), strokeWidth = 2.6f)
                drawLine(Color(0xFFE4EAFF).copy(alpha = 0.32f), Offset(size.width * 0.33f, size.height * 0.34f), Offset(size.width * 0.58f, size.height * 0.72f), strokeWidth = 2.6f)
                drawLine(Color(0xFFBDA7FF).copy(alpha = 0.26f), Offset(size.width * 0.34f, size.height * 0.74f), Offset(size.width * 0.66f, size.height * 0.32f), strokeWidth = 2.5f)
                drawLine(Color(0xFFBDA7FF).copy(alpha = 0.26f), Offset(size.width * 0.66f, size.height * 0.32f), Offset(size.width * 0.98f, size.height * 0.74f), strokeWidth = 2.5f)
                drawRect(Color.Black.copy(alpha = 0.2f), topLeft = Offset(0f, size.height * 0.72f), size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.28f))
            }
            FolderCoverKind.Night -> {
                drawCircle(Color(0xFFE3DFFF).copy(alpha = 0.28f), radius = size.minDimension * 0.14f, center = Offset(size.width * 0.78f, size.height * 0.24f))
                drawCircle(Color(0xFF172653).copy(alpha = 0.94f), radius = size.minDimension * 0.11f, center = Offset(size.width * 0.73f, size.height * 0.2f))
                repeat(5) { index ->
                    val x = size.width * (0.08f + index * 0.18f)
                    drawRect(
                        Color(0xFF262B42).copy(alpha = 0.82f),
                        topLeft = Offset(x, size.height * (0.5f - index % 2 * 0.08f)),
                        size = androidx.compose.ui.geometry.Size(size.width * 0.1f, size.height * 0.28f)
                    )
                }
                drawLine(Color(0xFFBDA7FF).copy(alpha = 0.22f), Offset(0f, size.height * 0.78f), Offset(size.width, size.height * 0.78f), strokeWidth = 1.4f)
            }
        }
        if (!compact) {
            drawRect(
                Color.Black.copy(alpha = 0.14f),
                topLeft = Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.16f)
            )
            drawRect(
                Color.Black.copy(alpha = 0.18f),
                topLeft = Offset(0f, size.height * 0.84f),
                size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.16f)
            )
        }
    }
}

private enum class FolderCoverKind {
    Lens,
    Theater,
    Mountain,
    Night
}

private fun folderCoverKind(folderName: String): FolderCoverKind {
    val name = folderName.lowercase()
    return when {
        "camera" in name || "相机" in folderName -> FolderCoverKind.Lens
        "download" in name || "movie" in name || "电影" in folderName -> FolderCoverKind.Theater
        "夜" in folderName -> FolderCoverKind.Night
        else -> FolderCoverKind.Mountain
    }
}

private fun folderCoverGradient(kind: FolderCoverKind): List<Color> {
    return when (kind) {
        FolderCoverKind.Lens -> listOf(Color(0xFF4A183D), Color(0xFF0D333A), Color(0xFF050506))
        FolderCoverKind.Theater -> listOf(Color(0xFF402412), Color(0xFF171011), Color(0xFF050506))
        FolderCoverKind.Mountain -> listOf(Color(0xFF293752), Color(0xFF12182A), Color(0xFF050506))
        FolderCoverKind.Night -> listOf(Color(0xFF172653), Color(0xFF12111D), Color(0xFF050506))
    }
}

private fun folderMetaText(item: VideoFolderUiModel): String {
    val totalSize = item.videos.sumOf { it.size.coerceAtLeast(0L) }
    return "${item.videos.size} 个视频 · ${formatByteCount(totalSize)}"
}

private fun formatByteCount(bytes: Long): String {
    if (bytes <= 0L) return "0 KB"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex += 1
    }
    return if (unitIndex == 0) {
        "${bytes} B"
    } else {
        "${"%.1f".format(value)} ${units[unitIndex]}"
    }
}

private fun loadFolderPreviewBitmap(
    context: android.content.Context,
    uri: String
): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, Uri.parse(uri))
        val durationMs = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()
            ?: 0L
        val targetUs = when {
            durationMs >= 8_000L -> 3_000_000L
            durationMs > 0L -> (durationMs / 2) * 1_000L
            else -> 1_000_000L
        }
        retriever.getFrameAtTime(
            targetUs,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
        ) ?: retriever.getFrameAtTime(
            0L,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
        )
    } catch (_: Exception) {
        null
    } finally {
        runCatching { retriever.release() }
    }
}

private fun Set<String>.toggle(value: String): Set<String> {
    return if (value in this) this - value else this + value
}

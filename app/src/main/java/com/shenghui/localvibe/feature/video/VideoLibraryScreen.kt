package com.shenghui.localvibe.feature.video

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.shenghui.localvibe.core.scanner.LocalMediaFile
import com.shenghui.localvibe.feature.video.model.VideoFolderUiModel

private val VideoBackground = Color(0xFF05070D)
private val VideoSurface = Color(0xFF101722)
private val VideoSurfaceSoft = Color(0xFF151E2B)
private val VideoPrimary = Color(0xFF4D8DFF)
private val VideoPrimarySoft = Color(0xFF8AB6FF)
private val VideoTextPrimary = Color(0xFFF5F7FA)
private val VideoTextSecondary = Color(0xFFA8B2C2)
private val VideoTextMuted = Color(0xFF6F7A8A)

@Composable
fun VideoLibraryScreen(
    videoFolders: List<VideoFolderUiModel>,
    permissionDeniedMessage: String?,
    recentVideoFile: LocalMediaFile?,
    onAddFolder: () -> Unit,
    onOpenFolder: (VideoFolderUiModel) -> Unit,
    onContinueVideo: (LocalMediaFile) -> Unit,
    onRescanVideo: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var searchKeyword by rememberSaveable { mutableStateOf("") }
    var isGridMode by rememberSaveable { mutableStateOf(false) }
    var showMorePanel by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val shownFolders = remember(videoFolders, searchKeyword) {
        if (searchKeyword.isBlank()) {
            videoFolders
        } else {
            val keyword = searchKeyword.trim()
            videoFolders.filter { item ->
                item.folder.name.contains(keyword, ignoreCase = true) ||
                    item.videos.any { it.name.contains(keyword, ignoreCase = true) }
            }
        }
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
                    searchKeyword = searchKeyword,
                    onSearchKeywordChange = { searchKeyword = it },
                    onToggleSearch = {
                        if (isSearching) searchKeyword = ""
                        isSearching = !isSearching
                    },
                    onAddFolder = onAddFolder,
                    isGridMode = isGridMode,
                    onToggleViewMode = { isGridMode = !isGridMode },
                    onRescanVideo = onRescanVideo,
                    onMore = { showMorePanel = true }
                )

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

                    item {
                        Text(
                            text = "视频文件夹",
                            style = MaterialTheme.typography.titleMedium,
                            color = VideoTextPrimary
                        )
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
                        shownFolders.chunked(2).forEach { rowItems ->
                            item(key = rowItems.joinToString { it.folder.id }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowItems.forEach { item ->
                                        VideoFolderCard(
                                            item = item,
                                            compact = true,
                                            onClick = { onOpenFolder(item) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (rowItems.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    } else {
                        shownFolders.forEach { item ->
                            item(key = item.folder.id) {
                                VideoFolderCard(
                                    item = item,
                                    compact = false,
                                    onClick = { onOpenFolder(item) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            if (isSearching) {
                FloatingSearchBar(
                    value = searchKeyword,
                    onValueChange = { searchKeyword = it },
                    placeholder = "搜索视频或文件夹",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 76.dp)
                )
            }
            if (showMorePanel) {
                VideoLibraryMorePanel(
                    isGridMode = isGridMode,
                    onDismiss = { showMorePanel = false },
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
    searchKeyword: String,
    onSearchKeywordChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onAddFolder: () -> Unit,
    isGridMode: Boolean,
    onToggleViewMode: () -> Unit,
    onRescanVideo: () -> Unit,
    onMore: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VideoBackground)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        contentDescription = if (isGridMode) "列表视图" else "网格视图",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onRescanVideo) {
                    Icon(Icons.Filled.Refresh, contentDescription = "重新扫描", tint = Color.White)
                }
                IconButton(onClick = onMore) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = Color.White)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            VideoQuickPill(icon = Icons.Filled.Movie, text = "本地")
            VideoQuickPill(icon = Icons.Filled.Add, text = "添加文件夹", onClick = onAddFolder)
            VideoQuickPill(icon = Icons.Filled.Refresh, text = "刷新", onClick = onRescanVideo)
        }
    }
}

@Composable
private fun VideoLibraryMorePanel(
    isGridMode: Boolean,
    onDismiss: () -> Unit,
    onListMode: () -> Unit,
    onGridMode: () -> Unit,
    onAddFolder: () -> Unit,
    onRescan: () -> Unit,
    onMore: () -> Unit
) {
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
            shape = RoundedCornerShape(2.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF282828))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("视图模式", color = VideoTextPrimary, style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                            MorePanelOption(
                                icon = Icons.Filled.Folder,
                                label = "所有文件夹",
                                selected = true,
                                onClick = onMore
                            )
                            MorePanelOption(
                                icon = Icons.Filled.Description,
                                label = "文件",
                                selected = false,
                                onClick = onMore
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
                    MorePanelOption(Icons.Filled.SortByAlpha, "标题", false, onMore)
                    MorePanelOption(Icons.Filled.DateRange, "日期", false, onMore)
                    MorePanelOption(Icons.Filled.Schedule, "播放时间", false, onMore)
                    MorePanelOption(Icons.Filled.PlayArrow, "状态", true, onMore)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MorePanelOption(Icons.Filled.Storage, "大小", false, onMore)
                    MorePanelOption(Icons.Filled.Movie, "类型", false, onMore)
                    MorePanelOption(Icons.Filled.CreateNewFolder, "添加", false, onAddFolder)
                    MorePanelOption(Icons.Filled.Refresh, "刷新", false, onRescan)
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
private fun VideoQuickPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(VideoSurfaceSoft)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = VideoPrimarySoft, modifier = Modifier.size(18.dp))
        Text(text = text, color = VideoTextSecondary, style = MaterialTheme.typography.labelMedium)
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
        modifier = modifier.fillMaxWidth(),
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

@Composable
private fun VideoFolderCard(
    item: VideoFolderUiModel,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
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
                FolderPreview(videoCount = item.videos.size, compact = false)
                FolderText(item = item, compact = false, modifier = Modifier.weight(1f))
            }
        }
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
        Text(
            text = item.source,
            style = MaterialTheme.typography.labelSmall,
            color = VideoTextMuted
        )
    }
}

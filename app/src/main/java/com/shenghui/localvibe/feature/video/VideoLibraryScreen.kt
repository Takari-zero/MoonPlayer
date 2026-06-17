package com.shenghui.localvibe.feature.video

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
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
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.shenghui.localvibe.core.scanner.LocalMediaType
import com.shenghui.localvibe.core.media.VideoThumbnailStore
import com.shenghui.localvibe.feature.video.model.VideoFolderUiModel
import com.shenghui.localvibe.feature.video.model.VideoVisibilityRecordType
import com.shenghui.localvibe.feature.video.model.VideoVisibilityRecordUiModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    COUNT,
    DATE
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
    onDeleteFolderVideos: (List<LocalMediaFile>) -> Unit,
    onRescanVideo: () -> Unit,
    visibilityRecords: List<VideoVisibilityRecordUiModel> = emptyList(),
    onRestoreVisibilityRecords: (List<VideoVisibilityRecordUiModel>) -> Unit = {},
    onClearVisibilityRecords: (List<VideoVisibilityRecordUiModel>) -> Unit = {},
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var searchKeyword by rememberSaveable { mutableStateOf("") }
    var isGridMode by rememberSaveable { mutableStateOf(false) }
    var showMorePanel by rememberSaveable { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf(VideoLibrarySortMode.NAME) }
    var showFolderThumbnail by rememberSaveable { mutableStateOf(true) }
    var showFolderSize by rememberSaveable { mutableStateOf(true) }
    var showFolderDate by rememberSaveable { mutableStateOf(false) }
    var showFolderFormat by rememberSaveable { mutableStateOf(false) }
    var showThumbnailDuration by rememberSaveable { mutableStateOf(false) }
    var isMultiSelectMode by rememberSaveable { mutableStateOf(false) }
    var selectedFolderIds by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var showDeleteSelectedFoldersConfirm by remember { mutableStateOf(false) }
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
            VideoLibrarySortMode.DATE -> filtered.sortedWith(
                compareByDescending<VideoFolderUiModel> {
                    folderLatestVideoModifiedAt(it) ?: Long.MIN_VALUE
                }.thenBy { it.folder.name.lowercase() }
            )
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
            if (!isMultiSelectMode) {
                FloatingActionButton(
                    onClick = {
                        if (recentVideoFile != null) {
                            onContinueVideo(recentVideoFile)
                        } else {
                            Toast.makeText(context, "暂无可继续播放的视频", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(44.dp),
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
                                Toast.makeText(context, "已隐藏 ${selectedFolders.size} 个文件夹", Toast.LENGTH_SHORT).show()
                                selectedFolderIds = emptySet()
                                isMultiSelectMode = false
                            }
                        },
                        onDeleteSelected = {
                            val selectedFolders = shownFolders.filter { it.folder.id in selectedFolderIds }
                            val selectedVideos = selectedFolders.flatMap { it.videos }
                                .filter { it.type == LocalMediaType.VIDEO }
                                .distinctBy { it.uri }
                            if (selectedFolders.isEmpty()) {
                                Toast.makeText(context, "请选择文件夹", Toast.LENGTH_SHORT).show()
                            } else if (selectedVideos.isEmpty()) {
                                Toast.makeText(context, "所选文件夹中没有可删除的视频", Toast.LENGTH_SHORT).show()
                            } else {
                                showDeleteSelectedFoldersConfirm = true
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
                        bottom = 144.dp
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
                            val gridRowCount = ((shownFolders.size + 2) / 3).coerceAtLeast(1)
                            val gridRowHeight = when {
                                showFolderDate && showFolderFormat -> 224
                                showFolderDate || showFolderFormat -> 204
                                else -> 184
                            }
                            val gridRowSpacing = 24
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                state = gridState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(
                                        (gridRowCount * gridRowHeight + (gridRowCount - 1) * gridRowSpacing).dp
                                    ),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp),
                                userScrollEnabled = false
                            ) {
                                items(
                                    items = shownFolders,
                                    key = { it.folder.id }
                                ) { item ->
                                    VideoFolderGridItem(
                                        item = item,
                                        showThumbnail = showFolderThumbnail,
                                        showThumbnailDuration = showThumbnailDuration,
                                        showSize = showFolderSize,
                                        showDate = showFolderDate,
                                        showFormat = showFolderFormat,
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
                                        showThumbnail = showFolderThumbnail,
                                        showThumbnailDuration = showThumbnailDuration,
                                        showSize = showFolderSize,
                                        showDate = showFolderDate,
                                        showFormat = showFolderFormat,
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
                        .padding(top = 78.dp)
                        .zIndex(8f)
                        .clickable {
                            searchKeyword = ""
                            isSearching = false
                        }
                )
            }
            if (showMorePanel) {
                VideoLibraryMorePanelV2(
                    isGridMode = isGridMode,
                    sortMode = sortMode,
                    showThumbnail = showFolderThumbnail,
                    showThumbnailDuration = showThumbnailDuration,
                    showSize = showFolderSize,
                    showDate = showFolderDate,
                    showFormat = showFolderFormat,
                    visibilityRecords = visibilityRecords,
                    onShowThumbnailChange = { showFolderThumbnail = it },
                    onShowThumbnailDurationChange = { showThumbnailDuration = it },
                    onShowSizeChange = { showFolderSize = it },
                    onShowDateChange = { showFolderDate = it },
                    onShowFormatChange = { showFolderFormat = it },
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
                    onSortByDate = {
                        sortMode = VideoLibrarySortMode.DATE
                        showMorePanel = false
                    },
                    onMultiDelete = {
                        showMorePanel = false
                        isMultiSelectMode = true
                        selectedFolderIds = emptySet()
                    },
                    onRestore = onRestoreVisibilityRecords,
                    onClear = onClearVisibilityRecords
                )
            }
        }
    }

    if (showDeleteSelectedFoldersConfirm) {
        val selectedFolders = shownFolders.filter { it.folder.id in selectedFolderIds }
        val selectedVideos = selectedFolders.flatMap { it.videos }
            .filter { it.type == LocalMediaType.VIDEO }
            .distinctBy { it.uri }
        AlertDialog(
            onDismissRequest = { showDeleteSelectedFoldersConfirm = false },
            title = { Text("删除所选文件夹内视频？") },
            text = {
                Text(
                    "将删除所选文件夹中已识别的 ${selectedVideos.size} 个本地视频文件，" +
                        "此操作无法撤销。不会删除文件夹本身或非视频文件。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteSelectedFoldersConfirm = false
                        onDeleteFolderVideos(selectedVideos)
                        selectedFolderIds = emptySet()
                        isMultiSelectMode = false
                    }
                ) {
                    Text("删除", color = Color(0xFFF97066))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedFoldersConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun VideoHeader(
    isSearching: Boolean,
    searchKeyword: String,
    onSearchKeywordChange: (String) -> Unit,
    isGridMode: Boolean,
    onToggleSearch: () -> Unit,
    onAddFolder: () -> Unit,
    onToggleViewMode: () -> Unit,
    onMore: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(2f)
            .background(VideoBackground)
            .padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            VideoHeaderTitleOrSearch(
                isSearching = isSearching,
                searchKeyword = searchKeyword,
                onSearchKeywordChange = onSearchKeywordChange,
                modifier = Modifier.weight(1f)
            )
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
private fun VideoHeaderTitleOrSearch(
    isSearching: Boolean,
    searchKeyword: String,
    onSearchKeywordChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isSearching) {
        androidx.compose.material3.OutlinedTextField(
            value = searchKeyword,
            onValueChange = onSearchKeywordChange,
            modifier = modifier
                .padding(end = 10.dp)
                .height(48.dp),
            singleLine = true,
            placeholder = {
                Text(
                    text = "\u641c\u7d22\u89c6\u9891\u6216\u6587\u4ef6\u5939",
                    color = VideoTextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        )
    } else {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = "\u89c6\u9891",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 29.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = VideoTextPrimary
            )
            Text(
                text = "\u672c\u5730\u89c6\u9891",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                color = VideoTextMuted
            )
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
private fun PanelDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(VideoDivider.copy(alpha = 0.9f))
    )
}

private enum class VideoVisibilityRecordFilter {
    ALL,
    HIDDEN_FOLDER,
    REMOVED_VIDEO,
    UNAVAILABLE_FILE
}

@Composable
private fun VideoVisibilityRecordsExpandable(
    records: List<VideoVisibilityRecordUiModel>,
    expanded: Boolean,
    onExpandedChange: () -> Unit,
    onRestore: (List<VideoVisibilityRecordUiModel>) -> Unit,
    onClear: (List<VideoVisibilityRecordUiModel>) -> Unit
) {
    var selectedFilter by rememberSaveable { mutableStateOf(VideoVisibilityRecordFilter.ALL) }
    var selectedRecordKeys by rememberSaveable { mutableStateOf(emptySet<String>()) }
    val filteredRecords = remember(records, selectedFilter) {
        records.filter { record ->
            when (selectedFilter) {
                VideoVisibilityRecordFilter.ALL -> true
                VideoVisibilityRecordFilter.HIDDEN_FOLDER -> record.type == VideoVisibilityRecordType.HIDDEN_FOLDER
                VideoVisibilityRecordFilter.REMOVED_VIDEO -> record.type == VideoVisibilityRecordType.REMOVED_VIDEO
                VideoVisibilityRecordFilter.UNAVAILABLE_FILE -> record.type == VideoVisibilityRecordType.UNAVAILABLE_FILE
            }
        }
    }
    val selectedRecords = remember(records, selectedRecordKeys) {
        records.filter { it.visibilityRecordKey() in selectedRecordKeys }
    }
    val selectedRestorableRecords = selectedRecords.filter { it.type != VideoVisibilityRecordType.UNAVAILABLE_FILE }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Color.White.copy(alpha = 0.055f))
            .clickable(onClick = onExpandedChange)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Filled.VisibilityOff,
                contentDescription = null,
                tint = VideoTextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "隐藏记录",
                    color = VideoTextPrimary,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1
                )
                Text(
                    text = "管理隐藏文件夹、已隐藏视频和失效文件",
                    color = VideoTextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "收起" else "展开",
                tint = VideoTextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }

        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = {}),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VideoVisibilityFilterChip(
                        label = "全部",
                        count = records.size,
                        selected = selectedFilter == VideoVisibilityRecordFilter.ALL,
                        onClick = {
                            selectedFilter = VideoVisibilityRecordFilter.ALL
                            selectedRecordKeys = emptySet()
                        },
                        modifier = Modifier.weight(0.9f)
                    )
                    VideoVisibilityFilterChip(
                        label = "隐藏文件夹",
                        count = records.count { it.type == VideoVisibilityRecordType.HIDDEN_FOLDER },
                        selected = selectedFilter == VideoVisibilityRecordFilter.HIDDEN_FOLDER,
                        onClick = {
                            selectedFilter = VideoVisibilityRecordFilter.HIDDEN_FOLDER
                            selectedRecordKeys = emptySet()
                        },
                        modifier = Modifier.weight(1.34f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VideoVisibilityFilterChip(
                        label = "已隐藏视频",
                        count = records.count { it.type == VideoVisibilityRecordType.REMOVED_VIDEO },
                        selected = selectedFilter == VideoVisibilityRecordFilter.REMOVED_VIDEO,
                        onClick = {
                            selectedFilter = VideoVisibilityRecordFilter.REMOVED_VIDEO
                            selectedRecordKeys = emptySet()
                        },
                        modifier = Modifier.weight(1.15f)
                    )
                    VideoVisibilityFilterChip(
                        label = "失效文件",
                        count = records.count { it.type == VideoVisibilityRecordType.UNAVAILABLE_FILE },
                        selected = selectedFilter == VideoVisibilityRecordFilter.UNAVAILABLE_FILE,
                        onClick = {
                            selectedFilter = VideoVisibilityRecordFilter.UNAVAILABLE_FILE
                            selectedRecordKeys = emptySet()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = if (filteredRecords.isEmpty()) 78.dp else 0.dp, max = 250.dp),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    if (filteredRecords.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(78.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.045f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无相关记录",
                                    color = VideoTextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else {
                        lazyItems(
                            items = filteredRecords,
                            key = { it.visibilityRecordKey() }
                        ) { record ->
                            val key = record.visibilityRecordKey()
                            VideoVisibilityRecordCard(
                                record = record,
                                selected = key in selectedRecordKeys,
                                onToggleSelected = {
                                    selectedRecordKeys = selectedRecordKeys.toggle(key)
                                },
                                onRestore = {
                                    onRestore(listOf(record))
                                    selectedRecordKeys = selectedRecordKeys - key
                                },
                                onClear = {
                                    onClear(listOf(record))
                                    selectedRecordKeys = selectedRecordKeys - key
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VideoVisibilityActionButton(
                        text = "批量恢复",
                        enabled = selectedRestorableRecords.isNotEmpty(),
                        primary = false,
                        onClick = {
                            onRestore(selectedRestorableRecords)
                            selectedRecordKeys = emptySet()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    VideoVisibilityActionButton(
                        text = "批量清除",
                        enabled = selectedRecords.isNotEmpty(),
                        primary = false,
                        onClick = {
                            onClear(selectedRecords)
                            selectedRecordKeys = emptySet()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoVisibilityFilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(if (selected) VideoPrimary else Color.White.copy(alpha = 0.07f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$label（$count）",
            color = if (selected) Color.White else VideoTextSecondary,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VideoVisibilityRecordCard(
    record: VideoVisibilityRecordUiModel,
    selected: Boolean,
    onToggleSelected: () -> Unit,
    onRestore: () -> Unit,
    onClear: () -> Unit
) {
    val canRestore = record.type != VideoVisibilityRecordType.UNAVAILABLE_FILE
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) VideoPrimary.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.055f))
            .then(
                if (selected) {
                    Modifier.background(VideoPrimary.copy(alpha = 0.04f))
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = { onToggleSelected() },
            modifier = Modifier.size(32.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = VideoPrimary,
                checkmarkColor = Color.White,
                uncheckedColor = VideoTextMuted
            )
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = record.name,
                color = if (record.type == VideoVisibilityRecordType.UNAVAILABLE_FILE) {
                    VideoTextPrimary.copy(alpha = 0.7f)
                } else {
                    VideoTextPrimary
                },
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = record.path.orEmpty().ifBlank { record.id },
                color = VideoTextSecondary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.type.recordLabel(),
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(record.type.recordColor().copy(alpha = 0.18f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    color = record.type.recordColor(),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = formatVisibilityRecordTime(record),
                    color = VideoTextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (canRestore) {
            VideoVisibilitySmallButton("恢复", primary = true, onClick = onRestore)
        }
        VideoVisibilitySmallButton("清除", primary = false, onClick = onClear)
    }
}

@Composable
private fun VideoVisibilitySmallButton(
    text: String,
    primary: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (primary) VideoPrimary.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (primary) VideoPrimarySoft else VideoTextSecondary,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
private fun VideoVisibilityActionButton(
    text: String,
    enabled: Boolean,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                when {
                    !enabled -> Color.White.copy(alpha = 0.05f)
                    primary -> VideoPrimarySoft
                    else -> Color.White.copy(alpha = 0.08f)
                }
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = when {
                !enabled -> VideoTextMuted
                primary -> Color(0xFF2C1668)
                else -> VideoTextSecondary
            },
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
private fun VideoLibraryMorePanelV2(
    isGridMode: Boolean,
    sortMode: VideoLibrarySortMode,
    showThumbnail: Boolean,
    showThumbnailDuration: Boolean,
    showSize: Boolean,
    showDate: Boolean,
    showFormat: Boolean,
    visibilityRecords: List<VideoVisibilityRecordUiModel>,
    onShowThumbnailChange: (Boolean) -> Unit,
    onShowThumbnailDurationChange: (Boolean) -> Unit,
    onShowSizeChange: (Boolean) -> Unit,
    onShowDateChange: (Boolean) -> Unit,
    onShowFormatChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSearch: () -> Unit,
    onListMode: () -> Unit,
    onGridMode: () -> Unit,
    onAddFolder: () -> Unit,
    onRescan: () -> Unit,
    onSortByName: () -> Unit,
    onSortByCount: () -> Unit,
    onSortByDate: () -> Unit,
    onMultiDelete: () -> Unit,
    onRestore: (List<VideoVisibilityRecordUiModel>) -> Unit,
    onClear: (List<VideoVisibilityRecordUiModel>) -> Unit
) {
    val context = LocalContext.current
    var fieldsExpanded by rememberSaveable { mutableStateOf(false) }
    var advancedExpanded by rememberSaveable { mutableStateOf(false) }
    var visibilityRecordsExpanded by rememberSaveable { mutableStateOf(false) }
    fun showFuture(feature: String) {
        Toast.makeText(context, "$feature 后续实现", Toast.LENGTH_SHORT).show()
    }
    fun showBriefFuture(feature: String) {
        val toast = Toast.makeText(context, "$feature 后续实现", Toast.LENGTH_SHORT)
        toast.show()
        Handler(Looper.getMainLooper()).postDelayed({ toast.cancel() }, 1_000L)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(304.dp)
                .heightIn(max = 620.dp)
                .clickable(onClick = {}),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF214141C)),
            border = BorderStroke(1.dp, Color(0xFF5B526F).copy(alpha = 0.72f))
        ) {
            Column {
                Column(
                    modifier = Modifier
                        .padding(start = 16.dp, top = 18.dp, end = 16.dp, bottom = 10.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "\u66f4\u591a",
                            color = VideoTextPrimary,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "\u89c6\u9891\u4e3b\u9875\u8bbe\u7f6e",
                            color = VideoTextSecondary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    MorePanelSectionTitle("\u89c6\u56fe")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MorePanelOptionV2(Icons.Filled.ViewList, "\u5217\u8868", !isGridMode, onListMode, Modifier.weight(1f))
                        MorePanelOptionV2(Icons.Filled.GridView, "\u7f51\u683c", isGridMode, onGridMode, Modifier.weight(1f))
                    }

                    PanelDivider()
                    MorePanelSectionTitle("\u6392\u5e8f")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MorePanelOptionV2(Icons.Filled.SortByAlpha, "\u540d\u79f0", sortMode == VideoLibrarySortMode.NAME, onSortByName, Modifier.weight(1f))
                        MorePanelOptionV2(Icons.Filled.Movie, "\u6570\u91cf", sortMode == VideoLibrarySortMode.COUNT, onSortByCount, Modifier.weight(1f))
                        MorePanelOptionV2(Icons.Filled.DateRange, "\u65e5\u671f", sortMode == VideoLibrarySortMode.DATE, onSortByDate, Modifier.weight(1f))
                    }

                    PanelDivider()
                    MorePanelSectionTitle("\u64cd\u4f5c")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MorePanelOptionV2(Icons.Filled.Search, "\u641c\u7d22", false, onSearch, Modifier.weight(1f))
                        MorePanelOptionV2(Icons.Filled.CreateNewFolder, "\u6dfb\u52a0", false, onAddFolder, Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MorePanelOptionV2(Icons.Filled.Schedule, "\u591a\u9009", false, onMultiDelete, Modifier.weight(1f))
                        MorePanelOptionV2(Icons.Filled.Refresh, "\u91cd\u626b", false, onRescan, Modifier.weight(1f))
                    }
                    VideoVisibilityRecordsExpandable(
                        records = visibilityRecords,
                        expanded = visibilityRecordsExpanded,
                        onExpandedChange = { visibilityRecordsExpanded = !visibilityRecordsExpanded },
                        onRestore = onRestore,
                        onClear = onClear
                    )

                    PanelDivider()
                    VideoPanelExpandableHeaderV2(
                        title = "字段",
                        icon = Icons.Filled.Description,
                        expanded = fieldsExpanded,
                        onClick = { fieldsExpanded = !fieldsExpanded }
                    )
                    if (fieldsExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            VideoPanelToggleOptionV2(
                                label = "缩略图",
                                description = "显示文件夹封面",
                                checked = showThumbnail,
                                onCheckedChange = onShowThumbnailChange
                            )
                            VideoPanelToggleOptionV2(
                                label = "大小",
                                description = "显示文件夹总大小",
                                checked = showSize,
                                onCheckedChange = onShowSizeChange
                            )
                            VideoPanelToggleOptionV2(
                                label = "日期显示",
                                description = "显示最近视频修改日期",
                                checked = showDate,
                                onCheckedChange = onShowDateChange
                            )
                            VideoPanelToggleOptionV2(
                                label = "扩展名",
                                description = "显示文件夹视频格式概览",
                                checked = showFormat,
                                onCheckedChange = onShowFormatChange
                            )
                            VideoPanelFutureOptionV2("长度 / 时长") { showFuture("长度显示") }
                            VideoPanelFutureOptionV2("播放时间 / 进度") { showFuture("播放时间显示") }
                            VideoPanelFutureOptionV2("路径") { showFuture("路径显示") }
                            VideoPanelFutureOptionV2("分辨率 / 帧率") { showFuture("分辨率和帧率显示") }
                        }
                    }

                    PanelDivider()
                    VideoPanelExpandableHeaderV2(
                        title = "高级",
                        icon = Icons.Filled.MoreHoriz,
                        expanded = advancedExpanded,
                        onClick = { advancedExpanded = !advancedExpanded }
                    )
                    if (advancedExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            VideoPanelToggleOptionV2(
                                label = "缩略图显示时长",
                                description = "在封面右下角显示代表视频时长",
                                checked = showThumbnailDuration,
                                onCheckedChange = onShowThumbnailDurationChange
                            )
                            VideoPanelFutureOptionV2(
                                label = "显示隐藏文件和文件夹",
                                reason = "不会影响当前手动隐藏内容"
                            ) { showBriefFuture("显示隐藏文件和文件夹") }
                            VideoPanelFutureOptionV2(
                                label = "识别 .nomedia",
                                reason = "涉及扫描策略，暂不启用"
                            ) { showBriefFuture("识别 .nomedia") }
                        }
                    }
                }

                PanelDivider()
                Row(modifier = Modifier.height(50.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("\u53d6\u6d88", color = VideoTextPrimary, style = MaterialTheme.typography.bodyLarge)
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(50.dp)
                            .background(VideoDivider.copy(alpha = 0.95f))
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "\u5b8c\u6210",
                            color = VideoPrimary,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MorePanelSectionTitle(text: String) {
    Text(
        text = text,
        color = VideoTextSecondary,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(start = 2.dp, top = 1.dp)
    )
}

@Composable
private fun VideoPanelExpandableHeaderV2(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(Color.White.copy(alpha = 0.055f))
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = VideoPrimarySoft, modifier = Modifier.size(17.dp))
            Text(title, color = VideoTextPrimary, style = MaterialTheme.typography.bodyMedium)
        }
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = VideoTextSecondary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun VideoPanelToggleOptionV2(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .clickable { onCheckedChange(!checked) }
            .padding(start = 12.dp, end = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = label,
                color = VideoTextPrimary,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = description,
                color = VideoTextSecondary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.height(32.dp)
        )
    }
}

@Composable
private fun VideoPanelFutureOptionV2(
    label: String,
    reason: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (reason == null) 38.dp else 50.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(Color.White.copy(alpha = 0.025f))
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                color = VideoTextSecondary.copy(alpha = 0.78f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (reason != null) {
                Text(
                    text = reason,
                    color = VideoTextMuted.copy(alpha = 0.86f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(VideoTextMuted.copy(alpha = 0.14f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "后续",
                color = VideoTextMuted,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MorePanelOptionV2(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(66.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) {
                    VideoPrimary.copy(alpha = 0.26f)
                } else {
                    Color.White.copy(alpha = 0.055f)
                }
            )
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) VideoPrimarySoft else VideoTextSecondary.copy(alpha = 0.92f),
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = if (selected) VideoPrimarySoft else VideoTextSecondary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VideoFolderMultiSelectBar(
    selectedCount: Int,
    onCancel: () -> Unit,
    onSelectAll: () -> Unit,
    onRemoveSelected: () -> Unit,
    onDeleteSelected: () -> Unit
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
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                text = "隐藏",
                color = VideoPrimarySoft,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onRemoveSelected)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            )
            Text(
                text = "删除",
                color = Color(0xFFF97066),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onDeleteSelected)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
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
    showThumbnail: Boolean,
    showThumbnailDuration: Boolean,
    showSize: Boolean,
    showDate: Boolean,
    showFormat: Boolean,
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
                    previewVideos = item.videos.takeIf { showThumbnail }.orEmpty(),
                    videoCount = item.videos.size,
                    showThumbnailDuration = showThumbnailDuration,
                    compact = true
                )
                FolderText(
                    item = item,
                    compact = true,
                    showSize = showSize,
                    showDate = showDate,
                    showFormat = showFormat
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FolderPreview(
                    folderName = item.folder.name,
                    previewVideos = item.videos.takeIf { showThumbnail }.orEmpty(),
                    videoCount = item.videos.size,
                    showThumbnailDuration = showThumbnailDuration,
                    compact = false
                )
                FolderText(
                    item = item,
                    compact = false,
                    showSize = showSize,
                    showDate = showDate,
                    showFormat = showFormat,
                    modifier = Modifier.weight(1f)
                )
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelected() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = VideoPrimary,
                            checkmarkColor = VideoTextPrimary,
                            uncheckedColor = VideoTextSecondary
                        )
                    )
                } else {
                    Icon(
                        Icons.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = VideoTextMuted.copy(alpha = 0.64f),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
        if (!compact) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(VideoDivider.copy(alpha = 0.42f))
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoFolderGridItem(
    item: VideoFolderUiModel,
    showThumbnail: Boolean,
    showThumbnailDuration: Boolean,
    showSize: Boolean,
    showDate: Boolean,
    showFormat: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelected: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) VideoPrimary.copy(alpha = 0.08f) else Color.Transparent)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box {
            FolderPreview(
                folderName = item.folder.name,
                previewVideos = item.videos.takeIf { showThumbnail }.orEmpty(),
                videoCount = item.videos.size,
                showThumbnailDuration = showThumbnailDuration,
                compact = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.12f),
                useDefaultSize = false
            )
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelected() },
                    modifier = Modifier.align(Alignment.TopEnd),
                    colors = CheckboxDefaults.colors(
                        checkedColor = VideoPrimary,
                        checkmarkColor = VideoTextPrimary,
                        uncheckedColor = VideoTextSecondary
                    )
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 7.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = item.folder.name,
                modifier = Modifier.height(20.dp),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = VideoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start
            )
            Text(
                text = folderMetaText(item, showSize),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = VideoTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showDate) {
                Text(
                    text = folderDateText(item),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = VideoTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (showFormat) {
                folderFormatText(item)?.let { formatText ->
                    Text(
                        text = formatText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = VideoTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderPreview(
    folderName: String,
    previewVideos: List<LocalMediaFile>,
    videoCount: Int,
    showThumbnailDuration: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
    useDefaultSize: Boolean = true,
    showCountBadge: Boolean = false
) {
    val coverKind = remember(folderName) { folderCoverKind(folderName) }
    val context = LocalContext.current
    val preview by produceState<FolderPreviewBitmap?>(initialValue = null, previewVideos, compact) {
        value = withContext(Dispatchers.IO) {
            previewVideos.firstNotNullOfOrNull { video ->
                VideoThumbnailStore.loadOrCreate(context.applicationContext, video)
                    ?.let { bitmap -> FolderPreviewBitmap(bitmap, video.durationMs) }
            }
        }
    }
    val thumbnail = preview?.bitmap
    val durationLabel = preview
        ?.durationMs
        ?.takeIf { showThumbnailDuration }
        ?.let(::formatThumbnailDuration)
    Box(
        modifier = modifier
            .then(
                if (useDefaultSize) {
                    Modifier.size(
                        width = if (compact) 88.dp else 104.dp,
                        height = if (compact) 56.dp else 58.dp
                    )
                } else {
                    Modifier
                }
            )
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
        if (showCountBadge && videoCount > 0) {
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
        if (!durationLabel.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.58f))
                    .height(if (compact) 17.dp else 18.dp)
                    .padding(horizontal = if (compact) 6.dp else 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = durationLabel,
                    color = Color.White.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = if (compact) 10.sp else 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

private data class FolderPreviewBitmap(
    val bitmap: Bitmap,
    val durationMs: Long?
)

@Composable
private fun FolderText(
    item: VideoFolderUiModel,
    compact: Boolean,
    showSize: Boolean,
    showDate: Boolean,
    showFormat: Boolean,
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
            text = folderMetaText(item, showSize),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = VideoTextSecondary
        )
        if (showDate) {
            Text(
                text = folderDateText(item),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = if (compact) 11.sp else 12.sp),
                color = VideoTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (showFormat) {
            folderFormatText(item)?.let { formatText ->
                Text(
                    text = formatText,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = if (compact) 11.sp else 12.sp),
                    color = VideoTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FolderPreviewArtwork(coverKind: FolderCoverKind, compact: Boolean) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * 0.52f, size.height * 0.48f)
        val moonRadius = size.minDimension * if (compact) 0.34f else 0.32f

        drawRect(
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF050509),
                    Color(0xFF151023),
                    Color(0xFF050509)
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
        )
        drawCircle(
            Brush.radialGradient(
                colors = listOf(
                    Color(0xFFBC74FF).copy(alpha = 0.5f),
                    Color(0xFF7C4BEF).copy(alpha = 0.18f),
                    Color.Transparent
                ),
                center = Offset(center.x + moonRadius * 0.28f, center.y),
                radius = moonRadius * 1.75f
            ),
            radius = moonRadius * 1.75f,
            center = Offset(center.x + moonRadius * 0.28f, center.y)
        )
        repeat(9) { index ->
            val y = size.height * (0.18f + index * 0.07f)
            val alpha = if (index % 2 == 0) 0.12f else 0.07f
            drawLine(
                Color(0xFFB78AFF).copy(alpha = alpha),
                Offset(size.width * 0.12f, y),
                Offset(size.width * 0.9f, y + size.height * 0.02f),
                strokeWidth = 1f
            )
        }
        drawCircle(
            Color(0xFF06050A),
            radius = moonRadius,
            center = center
        )
        drawCircle(
            Color(0xFFE6B8FF).copy(alpha = 0.42f),
            radius = moonRadius,
            center = center,
            style = Stroke(width = size.minDimension * 0.018f)
        )
        drawCircle(
            Color(0xFFD787FF).copy(alpha = 0.28f),
            radius = moonRadius * 1.05f,
            center = center,
            style = Stroke(width = size.minDimension * 0.012f)
        )
        drawCircle(
            Color(0xFF06050A).copy(alpha = 0.88f),
            radius = moonRadius * 0.96f,
            center = Offset(center.x - moonRadius * 0.08f, center.y - moonRadius * 0.02f)
        )
        val frameColor = Color(0xFFC58AFF).copy(alpha = if (compact) 0.26f else 0.22f)
        val insetX = size.width * 0.12f
        val insetY = size.height * 0.14f
        val frameW = size.width * 0.17f
        val frameH = size.height * 0.16f
        val stroke = size.minDimension * 0.012f
        drawLine(frameColor, Offset(insetX, insetY), Offset(insetX + frameW, insetY), strokeWidth = stroke)
        drawLine(frameColor, Offset(insetX, insetY), Offset(insetX, insetY + frameH), strokeWidth = stroke)
        drawLine(frameColor, Offset(size.width - insetX, insetY), Offset(size.width - insetX - frameW, insetY), strokeWidth = stroke)
        drawLine(frameColor, Offset(size.width - insetX, insetY), Offset(size.width - insetX, insetY + frameH), strokeWidth = stroke)
        drawLine(frameColor, Offset(insetX, size.height - insetY), Offset(insetX + frameW, size.height - insetY), strokeWidth = stroke)
        drawLine(frameColor, Offset(insetX, size.height - insetY), Offset(insetX, size.height - insetY - frameH), strokeWidth = stroke)
        drawLine(frameColor, Offset(size.width - insetX, size.height - insetY), Offset(size.width - insetX - frameW, size.height - insetY), strokeWidth = stroke)
        drawLine(frameColor, Offset(size.width - insetX, size.height - insetY), Offset(size.width - insetX, size.height - insetY - frameH), strokeWidth = stroke)
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

private fun folderMetaText(item: VideoFolderUiModel, showSize: Boolean): String {
    if (!showSize) {
        return "${item.videos.size} 个视频"
    }
    val totalSize = item.videos.sumOf { it.size.coerceAtLeast(0L) }
    return "${item.videos.size} 个视频 · ${formatByteCount(totalSize)}"
}

private fun folderDateText(item: VideoFolderUiModel): String {
    val latestModifiedAt = folderLatestVideoModifiedAt(item)
    return "最近：${latestModifiedAt?.let(::formatFolderDate) ?: "未知"}"
}

private fun folderFormatText(item: VideoFolderUiModel): String? {
    val extensions = item.videos
        .map { it.extension.trim().lowercase(Locale.getDefault()) }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()
    if (extensions.isEmpty()) return null

    val visibleFormats = extensions.take(2).joinToString(" / ")
    val extraCount = extensions.size - 2
    return if (extraCount > 0) {
        "格式：$visibleFormats +$extraCount"
    } else {
        "格式：$visibleFormats"
    }
}

private fun folderLatestVideoModifiedAt(item: VideoFolderUiModel): Long? {
    return item.videos
        .mapNotNull { it.modifiedAt?.takeIf { modifiedAt -> modifiedAt > 0L } }
        .maxOrNull()
}

private fun formatFolderDate(timestampMs: Long): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestampMs))
}

private fun formatThumbnailDuration(durationMs: Long): String? {
    val totalSeconds = durationMs / 1_000L
    if (totalSeconds <= 0L) return null

    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

private fun VideoVisibilityRecordUiModel.visibilityRecordKey(): String {
    return "${type.name}:$id"
}

private fun VideoVisibilityRecordType.recordLabel(): String {
    return when (this) {
        VideoVisibilityRecordType.HIDDEN_FOLDER -> "隐藏文件夹"
        VideoVisibilityRecordType.REMOVED_VIDEO -> "已隐藏视频"
        VideoVisibilityRecordType.UNAVAILABLE_FILE -> "文件已失效"
    }
}

private fun VideoVisibilityRecordType.recordColor(): Color {
    return when (this) {
        VideoVisibilityRecordType.HIDDEN_FOLDER -> VideoPrimarySoft
        VideoVisibilityRecordType.REMOVED_VIDEO -> Color(0xFFA8B6FF)
        VideoVisibilityRecordType.UNAVAILABLE_FILE -> Color(0xFFFF8F8F)
    }
}

private fun formatVisibilityRecordTime(record: VideoVisibilityRecordUiModel): String {
    val action = when (record.type) {
        VideoVisibilityRecordType.HIDDEN_FOLDER -> "隐藏"
        VideoVisibilityRecordType.REMOVED_VIDEO -> "隐藏"
        VideoVisibilityRecordType.UNAVAILABLE_FILE -> "标记"
    }
    val timestamp = record.recordedAt
    if (timestamp <= 0L) return "记录时间未知"
    val diffMs = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
    val minutes = diffMs / 60_000L
    val hours = diffMs / 3_600_000L
    val days = diffMs / 86_400_000L
    return when {
        minutes < 1L -> "刚刚$action"
        minutes < 60L -> "${minutes}分钟前$action"
        hours < 24L -> "${hours}小时前$action"
        days <= 3L -> "${days}天前$action"
        else -> SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
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

private fun Set<String>.toggle(value: String): Set<String> {
    return if (value in this) this - value else this + value
}

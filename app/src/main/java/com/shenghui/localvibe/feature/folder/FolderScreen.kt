package com.shenghui.localvibe.feature.folder

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.shenghui.localvibe.core.media.VideoMetadata
import com.shenghui.localvibe.core.media.formatDuration
import com.shenghui.localvibe.core.media.formatFileSize
import com.shenghui.localvibe.core.media.loadVideoMetadata
import com.shenghui.localvibe.core.scanner.LocalMediaFile
import com.shenghui.localvibe.core.scanner.LocalMediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Categories = listOf("全部", "视频", "音乐", "小说")

private enum class VideoFolderSortMode {
    NAME,
    DURATION,
    SIZE,
    PROGRESS
}

@Composable
fun FolderScreen(
    folderName: String,
    files: List<LocalMediaFile>,
    targetType: LocalMediaType? = null,
    videoProgressMap: Map<String, Long>,
    videoMetadataCache: Map<String, VideoMetadata> = emptyMap(),
    onVideoMetadataLoaded: (String, VideoMetadata) -> Unit = { _, _ -> },
    audioProgressMap: Map<String, Long>,
    recentVideoFile: LocalMediaFile? = null,
    onOpenVideo: (LocalMediaFile) -> Unit,
    onContinueVideo: (LocalMediaFile) -> Unit = {},
    onOpenAudio: (LocalMediaFile) -> Unit,
    onRemoveFile: (LocalMediaFile) -> Unit,
    onDeleteFile: (LocalMediaFile) -> Unit,
    onRemoveFiles: (List<LocalMediaFile>) -> Unit,
    onDeleteFiles: (List<LocalMediaFile>) -> Unit,
    onUnavailableVideoDetected: (LocalMediaFile) -> Unit = {},
    deleteSuccessSignal: Long = 0L,
    onRescanFolder: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedCategory by rememberSaveable(targetType) {
        mutableStateOf(if (targetType == LocalMediaType.VIDEO) "视频" else Categories.first())
    }
    var isMultiSelectMode by rememberSaveable { mutableStateOf(false) }
    var selectedUris by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var showBatchRemoveConfirm by remember { mutableStateOf(false) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var isVideoSearching by rememberSaveable { mutableStateOf(false) }
    var videoSearchKeyword by rememberSaveable { mutableStateOf("") }
    var isVideoGridMode by rememberSaveable { mutableStateOf(false) }
    var videoSortMode by rememberSaveable { mutableStateOf(VideoFolderSortMode.NAME) }
    val unavailableVideoUris = remember { mutableStateMapOf<String, Boolean>() }
    val categoryFiles = remember(files, selectedCategory, targetType) {
        if (targetType != null) files.filter { it.type == targetType } else files.filterForCategory(selectedCategory)
    }
    val visibleFiles = remember(categoryFiles, targetType, videoSearchKeyword, videoSortMode, videoMetadataCache, videoProgressMap) {
        val filtered = if (targetType == LocalMediaType.VIDEO && videoSearchKeyword.isNotBlank()) {
            val keyword = videoSearchKeyword.trim()
            categoryFiles.filter { it.name.contains(keyword, ignoreCase = true) }
        } else {
            categoryFiles
        }
        if (targetType == LocalMediaType.VIDEO) {
            when (videoSortMode) {
                VideoFolderSortMode.NAME -> filtered.sortedBy { it.name.lowercase() }
                VideoFolderSortMode.DURATION -> filtered.sortedByDescending { videoMetadataCache[it.id]?.durationMs ?: 0L }
                VideoFolderSortMode.SIZE -> filtered.sortedByDescending { it.size }
                VideoFolderSortMode.PROGRESS -> filtered.sortedByDescending { videoProgressMap[it.uri] ?: 0L }
            }
        } else {
            filtered
        }
    }

    BackHandler(enabled = targetType == LocalMediaType.VIDEO && isVideoSearching) {
        videoSearchKeyword = ""
        isVideoSearching = false
    }

    BackHandler(enabled = targetType == LocalMediaType.VIDEO && isMultiSelectMode && !isVideoSearching) {
        selectedUris = emptySet()
        isMultiSelectMode = false
    }

    LaunchedEffect(deleteSuccessSignal) {
        if (deleteSuccessSignal > 0L) {
            selectedUris = emptySet()
            isMultiSelectMode = false
        }
    }

    LaunchedEffect(categoryFiles) {
        val visibleVideoUris = categoryFiles
            .filter { it.type == LocalMediaType.VIDEO }
            .map { it.uri }
            .toSet()
        unavailableVideoUris.keys
            .filterNot { it in visibleVideoUris }
            .toList()
            .forEach { unavailableVideoUris.remove(it) }
    }

    fun openVideoIfAvailable(file: LocalMediaFile) {
        coroutineScope.launch {
            val isAvailable = withContext(Dispatchers.IO) {
                isMediaFileReadable(context.applicationContext, file)
            }
            unavailableVideoUris[file.uri] = !isAvailable
            if (isAvailable) {
                onOpenVideo(file)
            } else {
                onUnavailableVideoDetected(file)
                Toast.makeText(context, "文件已失效，可从列表移除", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (targetType == LocalMediaType.VIDEO) {
            Color(0xFF05070D)
        } else {
            MaterialTheme.colorScheme.background
        },
        floatingActionButton = {
            if (targetType == LocalMediaType.VIDEO) {
                FloatingActionButton(
                    onClick = {
                        if (recentVideoFile != null) {
                            onContinueVideo(recentVideoFile)
                        } else {
                            Toast.makeText(context, "暂无可继续播放的视频", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .padding(end = 10.dp, bottom = 72.dp)
                        .size(52.dp),
                    containerColor = Color(0xFF6F43F2),
                    contentColor = Color(0xFFF8F5FF),
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "继续播放",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(
                    if (targetType == LocalMediaType.VIDEO) {
                        Color(0xFF05070D)
                    } else {
                        MaterialTheme.colorScheme.background
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(
                        start = if (targetType == LocalMediaType.VIDEO) 14.dp else 20.dp,
                        end = if (targetType == LocalMediaType.VIDEO) 14.dp else 20.dp,
                        top = if (targetType == LocalMediaType.VIDEO) 12.dp else 20.dp,
                        bottom = if (targetType == LocalMediaType.VIDEO) 96.dp else 20.dp
                    )
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(
                    if (targetType == LocalMediaType.VIDEO) 10.dp else 16.dp
                )
            ) {
            if (isMultiSelectMode) {
                MultiSelectHeader(
                    selectedCount = selectedUris.size,
                    isVideoMode = targetType == LocalMediaType.VIDEO,
                    onCancel = {
                        isMultiSelectMode = false
                        selectedUris = emptySet()
                    },
                    onSelectAll = { selectedUris = visibleFiles.map { it.uri }.toSet() },
                    onRemoveSelected = {
                        val selectedFiles = visibleFiles.filter { it.uri in selectedUris }
                        if (selectedFiles.isEmpty()) {
                            Toast.makeText(context, "请先选择项目", Toast.LENGTH_SHORT).show()
                        } else {
                            showBatchRemoveConfirm = true
                        }
                    },
                    onDeleteSelected = {
                        if (selectedUris.isEmpty()) {
                            Toast.makeText(context, "请先选择项目", Toast.LENGTH_SHORT).show()
                        } else {
                            showBatchDeleteConfirm = true
                        }
                    }
                )
            } else {
                if (targetType == LocalMediaType.VIDEO) {
                    VideoFolderTopBarV2(
                        folderName = folderName.ifBlank { "未命名文件夹" },
                        videoCount = visibleFiles.size,
                        totalSize = visibleFiles.sumOf { it.size.coerceAtLeast(0L) },
                        isSearching = isVideoSearching,
                        searchKeyword = videoSearchKeyword,
                        onSearchKeywordChange = { videoSearchKeyword = it },
                        isGridMode = isVideoGridMode,
                        sortMode = videoSortMode,
                        onBack = onBack,
                        onToggleSearch = {
                            if (isVideoSearching) videoSearchKeyword = ""
                            isVideoSearching = !isVideoSearching
                        },
                        onToggleViewMode = { isVideoGridMode = !isVideoGridMode },
                        onSortChange = { videoSortMode = it },
                        onStartMultiSelect = {
                            isMultiSelectMode = true
                            selectedUris = emptySet()
                        },
                        onRescanFolder = onRescanFolder
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                        }
                        Text(
                            text = folderName.ifBlank { "未命名文件夹" },
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth()
                                .padding(horizontal = 56.dp),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("多选删除") },
                                    onClick = {
                                        expanded = false
                                        isMultiSelectMode = true
                                        selectedUris = emptySet()
                                    }
                                )
                            }
                        }
                        }
                    }
                }

            if (targetType == null && !isMultiSelectMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Categories.forEach { category ->
                        AssistChip(
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            enabled = selectedCategory != category
                        )
                    }
                }
            }

            if (visibleFiles.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (targetType == LocalMediaType.VIDEO) {
                            Color(0xFF101722)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        }
                    )
                ) {
                    Text(
                        text = "这个分类下还没有文件。",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (targetType == LocalMediaType.VIDEO) {
                            Color(0xFFA8B2C2)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(
                        if (targetType == LocalMediaType.VIDEO) 8.dp else 12.dp
                    )
                ) {
                    if (targetType == LocalMediaType.VIDEO && isVideoGridMode) {
                        val gridColumnCount = 3
                        visibleFiles.chunked(gridColumnCount).forEach { rowFiles ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowFiles.forEach { file ->
                                    val isFileUnavailable = unavailableVideoUris[file.uri] == true
                                    VideoGridFileCard(
                                        file = file,
                                        progressMs = videoProgressMap[file.uri] ?: 0L,
                                        metadata = videoMetadataCache[file.id],
                                        isFileUnavailable = isFileUnavailable,
                                        isSelectionMode = isMultiSelectMode,
                                        isSelected = file.uri in selectedUris,
                                        onToggleSelected = { selectedUris = selectedUris.toggle(file.uri) },
                                        onMetadataLoaded = onVideoMetadataLoaded,
                                        onAvailabilityChanged = { isUnavailable ->
                                            unavailableVideoUris[file.uri] = isUnavailable
                                            if (isUnavailable) onUnavailableVideoDetected(file)
                                        },
                                        onRemove = { onRemoveFile(file) },
                                        onDelete = { onDeleteFile(file) },
                                        onOpenVideo = {
                                            if (isMultiSelectMode) {
                                                selectedUris = selectedUris.toggle(file.uri)
                                            } else if (isFileUnavailable) {
                                                Toast.makeText(context, "文件已失效，可从列表移除", Toast.LENGTH_SHORT).show()
                                            } else {
                                                openVideoIfAvailable(file)
                                            }
                                        },
                                        onLongPress = {
                                            isMultiSelectMode = true
                                            selectedUris = setOf(file.uri)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(gridColumnCount - rowFiles.size) {
                                    Box(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        visibleFiles.forEach { file ->
                            when (file.type) {
                                LocalMediaType.VIDEO -> {
                                    val isFileUnavailable = unavailableVideoUris[file.uri] == true
                                    VideoFileCard(
                                        file = file,
                                        progressMs = videoProgressMap[file.uri] ?: 0L,
                                        metadata = videoMetadataCache[file.id],
                                        isFileUnavailable = isFileUnavailable,
                                        isSelectionMode = isMultiSelectMode,
                                        isSelected = file.uri in selectedUris,
                                        onToggleSelected = { selectedUris = selectedUris.toggle(file.uri) },
                                        onMetadataLoaded = onVideoMetadataLoaded,
                                        onAvailabilityChanged = { isUnavailable ->
                                            unavailableVideoUris[file.uri] = isUnavailable
                                            if (isUnavailable) onUnavailableVideoDetected(file)
                                        },
                                        onRemove = { onRemoveFile(file) },
                                        onDelete = { onDeleteFile(file) },
                                        onOpenVideo = {
                                            if (isMultiSelectMode) {
                                                selectedUris = selectedUris.toggle(file.uri)
                                            } else if (isFileUnavailable) {
                                                Toast.makeText(context, "文件已失效，可从列表移除", Toast.LENGTH_SHORT).show()
                                            } else {
                                                openVideoIfAvailable(file)
                                            }
                                        },
                                        onLongPress = {
                                            isMultiSelectMode = true
                                            selectedUris = setOf(file.uri)
                                        }
                                    )
                                }

                                LocalMediaType.AUDIO -> TypedFileCard(
                                    file = file,
                                    iconText = "♪",
                                    progressMs = audioProgressMap[file.uri] ?: 0L,
                                    isSelectionMode = isMultiSelectMode,
                                    isSelected = file.uri in selectedUris,
                                    onToggleSelected = { selectedUris = selectedUris.toggle(file.uri) },
                                    onRemove = { onRemoveFile(file) },
                                    onDelete = { onDeleteFile(file) },
                                    onClick = {
                                        if (isMultiSelectMode) {
                                            selectedUris = selectedUris.toggle(file.uri)
                                        } else {
                                            onOpenAudio(file)
                                        }
                                    }
                                )

                                LocalMediaType.BOOK -> TypedFileCard(
                                    file = file,
                                    iconText = "文",
                                    progressMs = 0L,
                                    isSelectionMode = isMultiSelectMode,
                                    isSelected = file.uri in selectedUris,
                                    onToggleSelected = { selectedUris = selectedUris.toggle(file.uri) },
                                    onRemove = { onRemoveFile(file) },
                                    onDelete = { onDeleteFile(file) },
                                    onClick = {
                                        if (isMultiSelectMode) {
                                            selectedUris = selectedUris.toggle(file.uri)
                                        } else {
                                            Toast.makeText(context, "TXT 听书功能下一步实现", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )

                                else -> Unit
                            }
                        }
                    }
                }
            }
            }
            if (targetType == LocalMediaType.VIDEO && isVideoSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 92.dp)
                        .zIndex(8f)
                        .clickable {
                            videoSearchKeyword = ""
                            isVideoSearching = false
                        }
                )
            }
        }
    }

    if (showBatchRemoveConfirm && targetType == LocalMediaType.VIDEO) {
        val selectedCountForDialog = visibleFiles.count { it.uri in selectedUris }
        AlertDialog(
            onDismissRequest = { showBatchRemoveConfirm = false },
            title = { Text("从列表移除？") },
            text = {
                Text(
                    if (selectedCountForDialog > 1) {
                        "将隐藏选中的 $selectedCountForDialog 个视频，不会删除本地文件。可在设置中通过“恢复隐藏视频”重新显示。"
                    } else {
                        "将隐藏选中的视频，不会删除本地文件。可在设置中通过“恢复隐藏视频”重新显示。"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedFiles = visibleFiles.filter { it.uri in selectedUris }
                        showBatchRemoveConfirm = false
                        onRemoveFiles(selectedFiles)
                        selectedUris = emptySet()
                        isMultiSelectMode = false
                    }
                ) {
                    Text("移除列表")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchRemoveConfirm = false }) { Text("取消") }
            }
        )
    }

    if (showBatchRemoveConfirm && targetType != LocalMediaType.VIDEO) {
        AlertDialog(
            onDismissRequest = { showBatchRemoveConfirm = false },
            title = { Text("从列表移除？") },
            text = {
                Text(
                    if (targetType == LocalMediaType.VIDEO) {
                        "不会删除本地文件，只会在 Moon播放器 中隐藏选中的视频。"
                    } else {
                        "不会删除本地文件，只会从当前列表移除选中的项目。"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedFiles = visibleFiles.filter { it.uri in selectedUris }
                        showBatchRemoveConfirm = false
                        onRemoveFiles(selectedFiles)
                        selectedUris = emptySet()
                        isMultiSelectMode = false
                    }
                ) {
                    Text("移除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchRemoveConfirm = false }) { Text("取消") }
            }
        )
    }

    if (showBatchDeleteConfirm && targetType == LocalMediaType.VIDEO) {
        val selectedCountForDialog = visibleFiles.count { it.uri in selectedUris }
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            title = { Text("永久删除视频？") },
            text = {
                Text(
                    if (selectedCountForDialog > 1) {
                        "将从本机删除选中的 $selectedCountForDialog 个视频文件，此操作不可撤销。"
                    } else {
                        "将从本机删除选中的视频文件，此操作不可撤销。"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedFiles = visibleFiles.filter { it.uri in selectedUris }
                        showBatchDeleteConfirm = false
                        onDeleteFiles(selectedFiles)
                    }
                ) {
                    Text("永久删除", color = Color(0xFFF97066))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) { Text("取消") }
            }
        )
    }

    if (showBatchDeleteConfirm && targetType != LocalMediaType.VIDEO) {
        val title = when (targetType) {
            LocalMediaType.VIDEO -> "永久删除视频？"
            LocalMediaType.AUDIO -> "永久删除音乐？"
            LocalMediaType.BOOK -> "永久删除小说？"
            else -> "永久删除文件？"
        }
        val text = when (targetType) {
            LocalMediaType.VIDEO -> "将从本机删除选中的视频文件，此操作不可撤销。"
            LocalMediaType.AUDIO -> "将删除选中的本地音乐文件，此操作无法从 Moon播放器 恢复。"
            LocalMediaType.BOOK -> "将删除选中的 TXT 文件，此操作无法从 Moon播放器 恢复。"
            else -> "将删除选中的本地文件，此操作无法从 Moon播放器 恢复。"
        }
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            title = { Text(title) },
            text = { Text(text) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedFiles = visibleFiles.filter { it.uri in selectedUris }
                        showBatchDeleteConfirm = false
                        onDeleteFiles(selectedFiles)
                    }
                ) {
                    Text(if (targetType == LocalMediaType.VIDEO) "永久删除" else "删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun VideoFolderTopBar(
    folderName: String,
    videoCount: Int,
    totalSize: Long,
    isSearching: Boolean,
    searchKeyword: String,
    onSearchKeywordChange: (String) -> Unit,
    isGridMode: Boolean,
    sortMode: VideoFolderSortMode,
    onBack: () -> Unit,
    onToggleSearch: () -> Unit,
    onToggleViewMode: () -> Unit,
    onSortChange: (VideoFolderSortMode) -> Unit,
    onStartMultiSelect: () -> Unit,
    onRescanFolder: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF080D16))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
            }
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 140.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = folderName,
                    color = Color(0xFFF8F5FF),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "$videoCount 个视频 · ${formatFileSize(totalSize)}",
                    color = Color(0xFFA7A0B8),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleSearch) {
                    Icon(Icons.Filled.Search, contentDescription = "搜索", tint = Color.White)
                }
                IconButton(onClick = onToggleViewMode) {
                    Icon(
                        imageVector = if (isGridMode) Icons.Filled.ViewList else Icons.Filled.GridView,
                        contentDescription = "切换视图",
                        tint = Color.White
                    )
                }
                var expanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        containerColor = Color(0xFF101722),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("搜索", color = Color(0xFFF5F7FA)) },
                            onClick = {
                                expanded = false
                                onToggleSearch()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isGridMode) "列表视图" else "网格视图", color = Color(0xFFF5F7FA)) },
                            onClick = {
                                expanded = false
                                onToggleViewMode()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (sortMode == VideoFolderSortMode.NAME) "按名称 ✓" else "按名称", color = Color(0xFFF5F7FA)) },
                            onClick = {
                                expanded = false
                                onSortChange(VideoFolderSortMode.NAME)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (sortMode == VideoFolderSortMode.DURATION) "按时长 ✓" else "按时长", color = Color(0xFFF5F7FA)) },
                            onClick = {
                                expanded = false
                                onSortChange(VideoFolderSortMode.DURATION)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (sortMode == VideoFolderSortMode.SIZE) "按大小 ✓" else "按大小", color = Color(0xFFF5F7FA)) },
                            onClick = {
                                expanded = false
                                onSortChange(VideoFolderSortMode.SIZE)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (sortMode == VideoFolderSortMode.PROGRESS) "按进度 ✓" else "按进度", color = Color(0xFFF5F7FA)) },
                            onClick = {
                                expanded = false
                                onSortChange(VideoFolderSortMode.PROGRESS)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("多选删除", color = Color(0xFFF5F7FA)) },
                            onClick = {
                                expanded = false
                                onStartMultiSelect()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("重新扫描当前文件夹", color = Color(0xFFF5F7FA)) },
                            onClick = {
                                expanded = false
                                onRescanFolder()
                            }
                        )
                    }
                }
            }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            )
        }
    }
}

@Composable
private fun VideoFolderTopBarV2(
    folderName: String,
    videoCount: Int,
    totalSize: Long,
    isSearching: Boolean,
    searchKeyword: String,
    onSearchKeywordChange: (String) -> Unit,
    isGridMode: Boolean,
    sortMode: VideoFolderSortMode,
    onBack: () -> Unit,
    onToggleSearch: () -> Unit,
    onToggleViewMode: () -> Unit,
    onSortChange: (VideoFolderSortMode) -> Unit,
    onStartMultiSelect: () -> Unit,
    onRescanFolder: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(2f)
            .padding(top = 18.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(42.dp)
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Color(0xFFCCC5D8))
        }

        if (isSearching) {
            androidx.compose.material3.OutlinedTextField(
                value = searchKeyword,
                onValueChange = onSearchKeywordChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp, end = 10.dp)
                    .height(48.dp),
                singleLine = true,
                placeholder = {
                    Text(
                        text = "\u641c\u7d22\u89c6\u9891",
                        color = Color(0xFFA7A0B8),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp, end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = folderName,
                    color = Color(0xFFF8F5FF),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$videoCount \u4e2a\u89c6\u9891 \u00b7 ${formatFileSize(totalSize)}",
                    color = Color(0xFFA7A0B8),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleSearch,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Filled.Search, contentDescription = "搜索", tint = Color(0xFFF8F5FF))
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF15111F))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (isGridMode) onToggleViewMode() },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (!isGridMode) Color(0xFF4A2D83) else Color.Transparent)
                ) {
                    Icon(
                        Icons.Filled.ViewList,
                        contentDescription = "列表视图",
                        tint = if (!isGridMode) Color(0xFFF8F5FF) else Color(0xFFBEB7CD)
                    )
                }
                IconButton(
                    onClick = { if (!isGridMode) onToggleViewMode() },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isGridMode) Color(0xFF4A2D83) else Color.Transparent)
                ) {
                    Icon(
                        Icons.Filled.GridView,
                        contentDescription = "网格视图",
                        tint = if (isGridMode) Color(0xFFF8F5FF) else Color(0xFFBEB7CD)
                    )
                }
            }

            var expanded by remember { mutableStateOf(false) }
            Box {
                IconButton(
                    onClick = { expanded = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = Color(0xFFF8F5FF))
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = Color(0xFF101722),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("搜索", color = Color(0xFFF5F7FA)) },
                        onClick = {
                            expanded = false
                            onToggleSearch()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isGridMode) "列表视图" else "网格视图", color = Color(0xFFF5F7FA)) },
                        onClick = {
                            expanded = false
                            onToggleViewMode()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (sortMode == VideoFolderSortMode.NAME) "按名称 ✓" else "按名称", color = Color(0xFFF5F7FA)) },
                        onClick = {
                            expanded = false
                            onSortChange(VideoFolderSortMode.NAME)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (sortMode == VideoFolderSortMode.DURATION) "按时长 ✓" else "按时长", color = Color(0xFFF5F7FA)) },
                        onClick = {
                            expanded = false
                            onSortChange(VideoFolderSortMode.DURATION)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (sortMode == VideoFolderSortMode.SIZE) "按大小 ✓" else "按大小", color = Color(0xFFF5F7FA)) },
                        onClick = {
                            expanded = false
                            onSortChange(VideoFolderSortMode.SIZE)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (sortMode == VideoFolderSortMode.PROGRESS) "按进度 ✓" else "按进度", color = Color(0xFFF5F7FA)) },
                        onClick = {
                            expanded = false
                            onSortChange(VideoFolderSortMode.PROGRESS)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("多选删除", color = Color(0xFFF5F7FA)) },
                        onClick = {
                            expanded = false
                            onStartMultiSelect()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("重新扫描当前文件夹", color = Color(0xFFF5F7FA)) },
                        onClick = {
                            expanded = false
                            onRescanFolder()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoFolderSearchOverlay(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = {}),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF2111722))
    ) {
        androidx.compose.material3.OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF8AB6FF)) },
            trailingIcon = {
                if (value.isNotBlank()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "清空", tint = Color(0xFFA8B2C2))
                    }
                }
            },
            placeholder = { Text("搜索当前文件夹视频", color = Color(0xFF6F7A8A)) }
        )
    }
}

@Composable
private fun MultiSelectHeader(
    selectedCount: Int,
    isVideoMode: Boolean = false,
    onCancel: () -> Unit,
    onSelectAll: () -> Unit,
    onRemoveSelected: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    if (isVideoMode) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101722))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已选 $selectedCount 项",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFF5F7FA)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = onSelectAll) { Text("全选") }
                        TextButton(onClick = onCancel) { Text("取消") }
                    }
                }
                SelectionActionItem(
                    title = "移除列表",
                    description = "不删除本地文件，仅从列表隐藏",
                    onClick = onRemoveSelected
                )
                SelectionActionItem(
                    title = "永久删除",
                    description = "删除本地文件，此操作不可撤销",
                    danger = true,
                    onClick = onDeleteSelected
                )
            }
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onCancel) { Text("取消") }
        Text(
            text = "已选择 $selectedCount 项",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        Row {
            TextButton(onClick = onSelectAll) { Text("全选") }
            TextButton(onClick = onRemoveSelected) { Text("移除") }
            TextButton(onClick = onDeleteSelected) { Text("删除") }
        }
    }
}

@Composable
private fun SelectionActionItem(
    title: String,
    description: String,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (danger) Color(0xFF2A1418) else Color(0xFF151E2B)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (danger) Color(0xFFF97066) else Color(0xFFF5F7FA)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA8B2C2)
                )
            }
            Text(
                text = if (danger) "删除" else "隐藏",
                style = MaterialTheme.typography.labelLarge,
                color = if (danger) Color(0xFFF97066) else Color(0xFF8AB6FF)
            )
        }
    }
}

@Composable
private fun TypedFileCard(
    file: LocalMediaFile,
    iconText: String,
    progressMs: Long,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelected: () -> Unit,
    onRemove: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                Color(0x33264D8D)
            } else {
                Color.Transparent
            }
        ),
        border = if (isSelected) BorderStroke(1.dp, Color(0xFF4D8DFF)) else null
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = iconText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${file.type.displayName()} · ${formatFileSize(file.size)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (progressMs > 0L) {
                    Text(
                        text = "上次播放到 ${formatDuration(progressMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelected() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF7B55FF),
                        checkmarkColor = Color(0xFFF8F5FF),
                        uncheckedColor = Color(0xFFA8B2C2)
                    )
                )
            } else {
                FileMoreMenu(
                    file = file,
                    onRemove = onRemove,
                    onDelete = onDelete
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoFileCard(
    file: LocalMediaFile,
    progressMs: Long,
    metadata: VideoMetadata?,
    isFileUnavailable: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelected: () -> Unit,
    onMetadataLoaded: (String, VideoMetadata) -> Unit,
    onAvailabilityChanged: (Boolean) -> Unit,
    onRemove: () -> Unit,
    onDelete: () -> Unit,
    onOpenVideo: () -> Unit,
    onLongPress: () -> Unit
) {
    val context = LocalContext.current
    val progressFraction = videoProgressFraction(progressMs, metadata?.durationMs)
    val progressPercent = (progressFraction * 100).toInt().coerceIn(0, 99)
    val progressText = if (progressMs > 0L) {
        if (progressPercent > 0) "\u5df2\u89c2\u770b $progressPercent%" else "\u5df2\u89c2\u770b"
    } else {
        "\u672a\u89c2\u770b"
    }
    val metaText = videoListMetaText(file, progressText)
    LaunchedEffect(file.id, metadata) {
        val isAvailable = withContext(Dispatchers.IO) {
            isMediaFileReadable(context.applicationContext, file)
        }
        onAvailabilityChanged(!isAvailable)
        if (!isAvailable || metadata != null) return@LaunchedEffect
        val loadedMetadata = withContext(Dispatchers.IO) {
            loadVideoMetadata(context.applicationContext, file.uri)
        }
        onMetadataLoaded(file.id, loadedMetadata)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isFileUnavailable) 0.58f else 1f)
            .combinedClickable(
                onClick = onOpenVideo,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                Color(0x44264D8D)
            } else {
                Color.Transparent
            }
        ),
        border = if (isSelected) BorderStroke(1.dp, Color(0xFF4D8DFF)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VideoThumbnail(
                metadata = metadata,
                durationMs = if (isFileUnavailable) null else metadata?.durationMs,
                isUnavailable = isFileUnavailable,
                modifier = Modifier.size(width = 150.dp, height = 84.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFF8F5FF),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isFileUnavailable) "文件已失效" else metaText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isFileUnavailable) Color(0xFFF97066) else Color(0xFFA7A0B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!isFileUnavailable) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(Color(0xFF1B1724))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                                .height(2.dp)
                                .background(Color(0xFF8B52FF))
                        )
                    }
                }
                if (false && progressMs > 0L) {
                    Text(
                        text = "上次播放到 ${formatDuration(progressMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8AB6FF)
                    )
                }
            }
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelected() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF7B55FF),
                        checkmarkColor = Color(0xFFF8F5FF),
                        uncheckedColor = Color(0xFFA8B2C2)
                    )
                )
            } else {
                FileMoreMenu(
                    file = file,
                    onRemove = onRemove,
                    onDelete = onDelete,
                    isFileUnavailable = isFileUnavailable,
                    iconTint = Color(0xFF5E576A)
                )
            }
        }
    }
}

private fun videoProgressFraction(progressMs: Long, durationMs: Long?): Float {
    if (progressMs <= 0L || durationMs == null || durationMs <= 0L) return 0f
    return (progressMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}

private fun fileModifiedDateValue(file: LocalMediaFile): String? {
    val modifiedAt = file.modifiedAt?.takeIf { it > 0L } ?: return null
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(modifiedAt))
}

private fun fileExtensionText(file: LocalMediaFile): String? {
    return file.extension
        .trim()
        .trimStart('.')
        .lowercase(Locale.getDefault())
        .takeIf { it.isNotBlank() }
}

private fun videoListMetaText(file: LocalMediaFile, progressText: String): String {
    val sizeText = file.size
        .takeIf { it > 0L }
        ?.let(::formatFileSize)
    return listOfNotNull(
        sizeText,
        fileModifiedDateValue(file),
        fileExtensionText(file),
        progressText
    ).joinToString(" · ")
}

private fun videoGridMetaText(file: LocalMediaFile): String? {
    val sizeText = file.size
        .takeIf { it > 0L }
        ?.let(::formatFileSize)
    val dateText = fileModifiedDateValue(file)
    val extensionText = fileExtensionText(file)
    return listOfNotNull(sizeText, dateText, extensionText)
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" · ")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoGridFileCard(
    file: LocalMediaFile,
    progressMs: Long,
    metadata: VideoMetadata?,
    isFileUnavailable: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelected: () -> Unit,
    onMetadataLoaded: (String, VideoMetadata) -> Unit,
    onAvailabilityChanged: (Boolean) -> Unit,
    onRemove: () -> Unit,
    onDelete: () -> Unit,
    onOpenVideo: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val metaText = videoGridMetaText(file)
    val shouldShowProgress = progressMs >= 1_000L
    LaunchedEffect(file.id, metadata) {
        val isAvailable = withContext(Dispatchers.IO) {
            isMediaFileReadable(context.applicationContext, file)
        }
        onAvailabilityChanged(!isAvailable)
        if (!isAvailable || metadata != null) return@LaunchedEffect
        val loadedMetadata = withContext(Dispatchers.IO) {
            loadVideoMetadata(context.applicationContext, file.uri)
        }
        onMetadataLoaded(file.id, loadedMetadata)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .alpha(if (isFileUnavailable) 0.58f else 1f)
            .background(if (isSelected) Color(0x44264D8D) else Color.Transparent)
            .combinedClickable(
                onClick = onOpenVideo,
                onLongClick = onLongPress
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box {
                VideoThumbnail(
                    metadata = metadata,
                    durationMs = if (isFileUnavailable) null else metadata?.durationMs,
                    isUnavailable = isFileUnavailable,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.12f)
                )
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelected() },
                        modifier = Modifier.align(Alignment.TopEnd),
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF7B55FF),
                            checkmarkColor = Color(0xFFF8F5FF),
                            uncheckedColor = Color(0xFFA8B2C2)
                        )
                    )
                } else {
                    Box(modifier = Modifier.align(Alignment.TopEnd)) {
                        FileMoreMenu(
                            file = file,
                            onRemove = onRemove,
                            onDelete = onDelete,
                            isFileUnavailable = isFileUnavailable
                        )
                    }
                }
            }
            Text(
                text = file.name,
                modifier = Modifier.height(20.dp),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFFF5F7FA),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isFileUnavailable) {
                Text(
                    text = "文件已失效",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFF97066),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else if (metaText != null) {
                Text(
                    text = metaText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFA8B2C2),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!isFileUnavailable && shouldShowProgress) {
                Text(
                    text = "上次 ${formatDuration(progressMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8AB6FF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun VideoThumbnail(
    metadata: VideoMetadata?,
    durationMs: Long? = null,
    isUnavailable: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bitmap = metadata?.thumbnail
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF151E2B)),
        contentAlignment = Alignment.Center
    ) {
        if (isUnavailable) {
            VideoUnavailableThumbnail()
        } else if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            VideoThumbnailPlaceholder()
        }
        if (!isUnavailable && durationMs != null && durationMs > 0L) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.56f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = formatDuration(durationMs),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun VideoUnavailableThumbnail() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF252733)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "文件已失效",
            color = Color(0xFFB8B3C6),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VideoThumbnailPlaceholder() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * 0.52f, size.height * 0.48f)
        val moonRadius = size.minDimension * 0.33f

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
        repeat(8) { index ->
            val y = size.height * (0.18f + index * 0.075f)
            drawLine(
                Color(0xFFB78AFF).copy(alpha = if (index % 2 == 0) 0.12f else 0.07f),
                Offset(size.width * 0.12f, y),
                Offset(size.width * 0.9f, y + size.height * 0.02f),
                strokeWidth = 1f
            )
        }
        drawCircle(Color(0xFF06050A), radius = moonRadius, center = center)
        drawCircle(
            Color(0xFFE6B8FF).copy(alpha = 0.42f),
            radius = moonRadius,
            center = center,
            style = Stroke(width = size.minDimension * 0.018f)
        )
        drawCircle(
            Color(0xFF06050A).copy(alpha = 0.88f),
            radius = moonRadius * 0.96f,
            center = Offset(center.x - moonRadius * 0.08f, center.y - moonRadius * 0.02f)
        )

        val frameColor = Color(0xFFC58AFF).copy(alpha = 0.24f)
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
    }
}

@Composable
private fun FileMoreMenu(
    file: LocalMediaFile,
    onRemove: () -> Unit,
    onDelete: () -> Unit,
    isFileUnavailable: Boolean = false,
    iconTint: Color? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var showRemoveConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = "更多",
                tint = iconTint ?: MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(14.dp)
        ) {
            DropdownMenuItem(
                text = { Text("从列表移除") },
                onClick = {
                    expanded = false
                    showRemoveConfirm = true
                }
            )
            if (!isFileUnavailable) {
            DropdownMenuItem(
                text = { Text("永久删除文件") },
                onClick = {
                    expanded = false
                    showDeleteConfirm = true
                }
            )
            }
        }
    }

    if (showRemoveConfirm && file.type == LocalMediaType.VIDEO) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            title = { Text("从列表移除？") },
            text = {
                Text("将隐藏选中的视频，不会删除本地文件。可在设置中通过“恢复隐藏视频”重新显示。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveConfirm = false
                        onRemove()
                    }
                ) {
                    Text("移除列表")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) { Text("取消") }
            }
        )
    }

    if (showRemoveConfirm && file.type != LocalMediaType.VIDEO) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            title = { Text("从列表移除？") },
            text = {
                Text(
                    if (file.type == LocalMediaType.VIDEO) {
                        "不会删除本地文件，只会在 Moon播放器 中隐藏这个视频。"
                    } else {
                        "不会删除本地文件，只会从当前列表移除这个项目。"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveConfirm = false
                        onRemove()
                    }
                ) {
                    Text("移除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) { Text("取消") }
            }
        )
    }

    if (showDeleteConfirm && file.type == LocalMediaType.VIDEO) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("永久删除视频？") },
            text = { Text("将从本机删除选中的视频文件，此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("永久删除", color = Color(0xFFF97066))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }

    if (showDeleteConfirm && file.type != LocalMediaType.VIDEO) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    when (file.type) {
                        LocalMediaType.VIDEO -> "永久删除视频？"
                        LocalMediaType.BOOK -> "永久删除小说？"
                        else -> "永久删除？"
                    }
                )
            },
            text = {
                Text(
                    when (file.type) {
                        LocalMediaType.VIDEO -> "将从本机删除选中的视频文件，此操作不可撤销。"
                        LocalMediaType.BOOK -> "此操作会删除本地 TXT 文件，无法从 Moon播放器 恢复。"
                        else -> "此操作会删除本地文件，无法从 Moon播放器 恢复。"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text(if (file.type == LocalMediaType.VIDEO) "永久删除" else "删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }
}

private fun List<LocalMediaFile>.filterForCategory(category: String): List<LocalMediaFile> {
    return when (category) {
        "视频" -> filter { it.type == LocalMediaType.VIDEO }
        "音乐" -> filter { it.type == LocalMediaType.AUDIO }
        "小说" -> filter { it.type == LocalMediaType.BOOK }
        else -> filter {
            it.type == LocalMediaType.VIDEO ||
                it.type == LocalMediaType.AUDIO ||
                it.type == LocalMediaType.BOOK
        }
    }
}

private fun LocalMediaType.displayName(): String {
    return when (this) {
        LocalMediaType.VIDEO -> "视频"
        LocalMediaType.AUDIO -> "音乐"
        LocalMediaType.BOOK -> "小说"
        LocalMediaType.SUBTITLE -> "字幕"
        LocalMediaType.UNKNOWN -> "未知"
    }
}

private fun Set<String>.toggle(value: String): Set<String> {
    return if (value in this) this - value else this + value
}

private fun isMediaFileReadable(context: android.content.Context, file: LocalMediaFile): Boolean {
    return try {
        context.contentResolver.openFileDescriptor(Uri.parse(file.uri), "r")?.use { true } == true
    } catch (_: Exception) {
        false
    }
}

package com.shenghui.localvibe.feature.folder

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.shenghui.localvibe.core.media.VideoMetadata
import com.shenghui.localvibe.core.media.formatDuration
import com.shenghui.localvibe.core.media.formatFileSize
import com.shenghui.localvibe.core.media.loadVideoMetadata
import com.shenghui.localvibe.core.scanner.LocalMediaFile
import com.shenghui.localvibe.core.scanner.LocalMediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    onOpenVideo: (LocalMediaFile) -> Unit,
    onOpenAudio: (LocalMediaFile) -> Unit,
    onRemoveFile: (LocalMediaFile) -> Unit,
    onDeleteFile: (LocalMediaFile) -> Unit,
    onRemoveFiles: (List<LocalMediaFile>) -> Unit,
    onDeleteFiles: (List<LocalMediaFile>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategory by rememberSaveable(targetType) {
        mutableStateOf(if (targetType == LocalMediaType.VIDEO) "视频" else Categories.first())
    }
    var isMultiSelectMode by rememberSaveable { mutableStateOf(false) }
    var selectedUris by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var isVideoSearching by rememberSaveable { mutableStateOf(false) }
    var videoSearchKeyword by rememberSaveable { mutableStateOf("") }
    var isVideoGridMode by rememberSaveable { mutableStateOf(false) }
    var videoSortMode by rememberSaveable { mutableStateOf(VideoFolderSortMode.NAME) }
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (targetType == LocalMediaType.VIDEO) {
            Color(0xFF05070D)
        } else {
            MaterialTheme.colorScheme.background
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
                    horizontal = if (targetType == LocalMediaType.VIDEO) 14.dp else 20.dp,
                    vertical = if (targetType == LocalMediaType.VIDEO) 12.dp else 20.dp
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
                            onRemoveFiles(selectedFiles)
                            selectedUris = emptySet()
                            isMultiSelectMode = false
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
                    VideoFolderTopBar(
                        folderName = folderName.ifBlank { "未命名文件夹" },
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
                        onRescanFolder = {
                            Toast.makeText(context, "已重新扫描当前文件夹", Toast.LENGTH_SHORT).show()
                        }
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
                        visibleFiles.chunked(2).forEach { rowFiles ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowFiles.forEach { file ->
                                    VideoGridFileCard(
                                        file = file,
                                        progressMs = videoProgressMap[file.uri] ?: 0L,
                                        metadata = videoMetadataCache[file.id],
                                        isSelectionMode = isMultiSelectMode,
                                        isSelected = file.uri in selectedUris,
                                        onToggleSelected = { selectedUris = selectedUris.toggle(file.uri) },
                                        onMetadataLoaded = onVideoMetadataLoaded,
                                        onRemove = { onRemoveFile(file) },
                                        onDelete = { onDeleteFile(file) },
                                        onOpenVideo = {
                                            if (isMultiSelectMode) {
                                                selectedUris = selectedUris.toggle(file.uri)
                                            } else {
                                                onOpenVideo(file)
                                            }
                                        },
                                        onLongPress = {
                                            isMultiSelectMode = true
                                            selectedUris = setOf(file.uri)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowFiles.size == 1) {
                                    Box(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        visibleFiles.forEach { file ->
                            when (file.type) {
                                LocalMediaType.VIDEO -> VideoFileCard(
                                    file = file,
                                    progressMs = videoProgressMap[file.uri] ?: 0L,
                                    metadata = videoMetadataCache[file.id],
                                    isSelectionMode = isMultiSelectMode,
                                    isSelected = file.uri in selectedUris,
                                    onToggleSelected = { selectedUris = selectedUris.toggle(file.uri) },
                                    onMetadataLoaded = onVideoMetadataLoaded,
                                    onRemove = { onRemoveFile(file) },
                                    onDelete = { onDeleteFile(file) },
                                    onOpenVideo = {
                                        if (isMultiSelectMode) {
                                            selectedUris = selectedUris.toggle(file.uri)
                                        } else {
                                            onOpenVideo(file)
                                        }
                                    },
                                    onLongPress = {
                                        isMultiSelectMode = true
                                        selectedUris = setOf(file.uri)
                                    }
                                )

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
                        .zIndex(8f)
                        .clickable {
                            videoSearchKeyword = ""
                            isVideoSearching = false
                        }
                )
                VideoFolderSearchOverlay(
                    value = videoSearchKeyword,
                    onValueChange = { videoSearchKeyword = it },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 18.dp, vertical = 76.dp)
                        .zIndex(9f)
                )
            }
        }
    }

    if (showBatchDeleteConfirm) {
        val title = when (targetType) {
            LocalMediaType.VIDEO -> "永久删除视频？"
            LocalMediaType.AUDIO -> "永久删除音乐？"
            LocalMediaType.BOOK -> "永久删除小说？"
            else -> "永久删除文件？"
        }
        val text = when (targetType) {
            LocalMediaType.VIDEO -> "将删除选中的本地视频文件，此操作无法从 Moon播放器 恢复。"
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
                        selectedUris = emptySet()
                        isMultiSelectMode = false
                    }
                ) {
                    Text("删除")
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
            Text(
                text = folderName,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 140.dp),
                color = Color(0xFFF5F7FA),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
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
                        DropdownMenuItem(
                            text = { Text("更多功能", color = Color(0xFFF5F7FA)) },
                            onClick = {
                                expanded = false
                                Toast.makeText(context, "更多功能后续实现", Toast.LENGTH_SHORT).show()
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
    onCancel: () -> Unit,
    onSelectAll: () -> Unit,
    onRemoveSelected: () -> Unit,
    onDeleteSelected: () -> Unit
) {
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
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onToggleSelected() })
            }
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
            if (!isSelectionMode) {
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
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelected: () -> Unit,
    onMetadataLoaded: (String, VideoMetadata) -> Unit,
    onRemove: () -> Unit,
    onDelete: () -> Unit,
    onOpenVideo: () -> Unit,
    onLongPress: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(file.id, metadata) {
        if (metadata != null) return@LaunchedEffect
        val loadedMetadata = withContext(Dispatchers.IO) {
            loadVideoMetadata(context.applicationContext, file.uri)
        }
        onMetadataLoaded(file.id, loadedMetadata)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onOpenVideo,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                Color(0x33264D8D)
            } else {
                Color.Transparent
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 0.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onToggleSelected() })
            }
            VideoThumbnail(
                metadata = metadata,
                durationMs = metadata?.durationMs,
                modifier = Modifier.size(width = 104.dp, height = 60.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFF5F7FA),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatFileSize(file.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA8B2C2)
                )
                if (progressMs > 0L) {
                    Text(
                        text = "上次播放到 ${formatDuration(progressMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8AB6FF)
                    )
                }
            }
            if (!isSelectionMode) {
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
private fun VideoGridFileCard(
    file: LocalMediaFile,
    progressMs: Long,
    metadata: VideoMetadata?,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelected: () -> Unit,
    onMetadataLoaded: (String, VideoMetadata) -> Unit,
    onRemove: () -> Unit,
    onDelete: () -> Unit,
    onOpenVideo: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(file.id, metadata) {
        if (metadata != null) return@LaunchedEffect
        val loadedMetadata = withContext(Dispatchers.IO) {
            loadVideoMetadata(context.applicationContext, file.uri)
        }
        onMetadataLoaded(file.id, loadedMetadata)
    }

    Card(
        modifier = modifier.combinedClickable(
            onClick = onOpenVideo,
            onLongClick = onLongPress
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                Color(0x33264D8D)
            } else {
                Color(0xFF101722)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box {
                VideoThumbnail(
                    metadata = metadata,
                    durationMs = metadata?.durationMs,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp)
                )
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelected() },
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                } else {
                    Box(modifier = Modifier.align(Alignment.TopEnd)) {
                        FileMoreMenu(
                            file = file,
                            onRemove = onRemove,
                            onDelete = onDelete
                        )
                    }
                }
            }
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFF5F7FA),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatFileSize(file.size),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFA8B2C2)
            )
            if (progressMs > 0L) {
                Text(
                    text = "上次播放到 ${formatDuration(progressMs)}",
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
    modifier: Modifier = Modifier
) {
    val bitmap = metadata?.thumbnail
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF151E2B)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = "视频",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFA8B2C2)
            )
        }
        if (durationMs != null && durationMs > 0L) {
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
private fun FileMoreMenu(
    file: LocalMediaFile,
    onRemove: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "更多")
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
                    onRemove()
                }
            )
            DropdownMenuItem(
                text = { Text("永久删除文件") },
                onClick = {
                    expanded = false
                    showDeleteConfirm = true
                }
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(if (file.type == LocalMediaType.BOOK) "永久删除小说？" else "永久删除？") },
            text = {
                Text(
                    if (file.type == LocalMediaType.BOOK) {
                        "此操作会删除本地 TXT 文件，无法从 Moon播放器 恢复。"
                    } else {
                        "此操作会删除本地文件，无法从 Moon播放器 恢复。"
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
                    Text("删除")
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

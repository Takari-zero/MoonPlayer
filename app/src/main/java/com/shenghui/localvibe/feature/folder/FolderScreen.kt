package com.shenghui.localvibe.feature.folder

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shenghui.localvibe.core.media.VideoMetadata
import com.shenghui.localvibe.core.media.formatFileSize
import com.shenghui.localvibe.core.media.formatDuration
import com.shenghui.localvibe.core.media.loadVideoMetadata
import com.shenghui.localvibe.core.scanner.LocalMediaFile
import com.shenghui.localvibe.core.scanner.LocalMediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

private val Categories = listOf("全部", "视频", "音乐", "小说")

@OptIn(ExperimentalMaterial3Api::class)
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
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategory by rememberSaveable(targetType) {
        mutableStateOf(if (targetType == LocalMediaType.VIDEO) "视频" else Categories.first())
    }
    val visibleFiles = remember(files, selectedCategory, targetType) {
        if (targetType != null) {
            files.filter { it.type == targetType }
        } else {
            files.filterForCategory(selectedCategory)
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextButton(onClick = onBack) {
                Text("返回")
            }

            Text(
                text = folderName.ifBlank { "未命名文件夹" },
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (targetType == null) {
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
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Text(
                        text = "这个分类下还没有文件。",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (targetType != null) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    visibleFiles.forEach { file ->
                        when (targetType) {
                            LocalMediaType.VIDEO -> VideoFileCard(
                                file = file,
                                progressMs = videoProgressMap[file.uri] ?: 0L,
                                metadata = videoMetadataCache[file.id],
                                onMetadataLoaded = onVideoMetadataLoaded,
                                onRemove = { onRemoveFile(file) },
                                onDelete = { onDeleteFile(file) },
                                onOpenVideo = { onOpenVideo(file) }
                            )

                            LocalMediaType.AUDIO -> TypedFileCard(
                                file = file,
                                iconText = "♪",
                                progressMs = audioProgressMap[file.uri] ?: 0L,
                                onRemove = { onRemoveFile(file) },
                                onDelete = { onDeleteFile(file) },
                                onClick = { onOpenAudio(file) }
                            )

                            LocalMediaType.BOOK -> TypedFileCard(
                                file = file,
                                iconText = "文",
                                progressMs = 0L,
                                onRemove = { onRemoveFile(file) },
                                onDelete = { onDeleteFile(file) },
                                onClick = {
                                    Toast.makeText(context, "TXT 听书功能下一步实现", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            )

                            else -> Unit
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    visibleFiles.forEach { file ->
                        FileCard(
                            file = file,
                            progressMs = when (file.type) {
                                LocalMediaType.VIDEO -> videoProgressMap[file.uri] ?: 0L
                                LocalMediaType.AUDIO -> audioProgressMap[file.uri] ?: 0L
                                else -> 0L
                            },
                            onRemove = { onRemoveFile(file) },
                            onDelete = { onDeleteFile(file) },
                            onClick = {
                                when (file.type) {
                                    LocalMediaType.VIDEO -> onOpenVideo(file)
                                    LocalMediaType.AUDIO -> onOpenAudio(file)
                                    LocalMediaType.BOOK -> Toast
                                        .makeText(context, "TXT 听书功能下一步实现", Toast.LENGTH_SHORT)
                                        .show()
                                    else -> Unit
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TypedFileCard(
    file: LocalMediaFile,
    iconText: String,
    progressMs: Long,
    onRemove: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
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
            FileMoreMenu(
                file = file,
                onRemove = onRemove,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun VideoFileCard(
    file: LocalMediaFile,
    progressMs: Long,
    metadata: VideoMetadata?,
    onMetadataLoaded: (String, VideoMetadata) -> Unit,
    onRemove: () -> Unit,
    onDelete: () -> Unit,
    onOpenVideo: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(file.id, metadata) {
        if (metadata != null) {
            return@LaunchedEffect
        }
        val loadedMetadata = withContext(Dispatchers.IO) {
            loadVideoMetadata(context.applicationContext, file.uri)
        }
        onMetadataLoaded(file.id, loadedMetadata)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenVideo),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VideoThumbnail(metadata = metadata)
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
                    text = "${formatFileSize(file.size)} · ${metadata?.durationMs?.let { formatDuration(it) } ?: "--:--"}",
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
            FileMoreMenu(
                file = file,
                onRemove = onRemove,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun VideoThumbnail(metadata: VideoMetadata?) {
    val bitmap = metadata?.thumbnail
    Box(
        modifier = Modifier
            .size(width = 120.dp, height = 72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FileCard(
    file: LocalMediaFile,
    progressMs: Long,
    onRemove: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${file.type.displayName()} · ${file.size.formatFileSize()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if ((file.type == LocalMediaType.VIDEO || file.type == LocalMediaType.AUDIO) && progressMs > 0L) {
                    Text(
                        text = "上次播放到 ${formatDuration(progressMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            FileMoreMenu(
                file = file,
                onRemove = onRemove,
                onDelete = onDelete
            )
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
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
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

private fun Long.formatFileSize(): String {
    if (this <= 0L) {
        return "0 B"
    }

    val kb = this / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
        else -> "$this B"
    }
}

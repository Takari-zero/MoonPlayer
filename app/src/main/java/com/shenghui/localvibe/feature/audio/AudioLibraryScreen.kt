package com.shenghui.localvibe.feature.audio

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shenghui.localvibe.core.media.formatDuration
import com.shenghui.localvibe.core.media.formatFileSize
import com.shenghui.localvibe.core.scanner.LocalMediaFile
import com.shenghui.localvibe.feature.library.MediaFolderGroupUiModel

@Composable
fun AudioLibraryScreen(
    audioFiles: List<LocalMediaFile>,
    audioFolders: List<MediaFolderGroupUiModel>,
    audioProgressMap: Map<String, Long>,
    permissionDeniedMessage: String?,
    currentAudioUri: String?,
    onAddFolder: () -> Unit,
    onOpenFolder: (MediaFolderGroupUiModel) -> Unit,
    onOpenAudio: (LocalMediaFile, List<LocalMediaFile>) -> Unit,
    onShuffleAll: (List<LocalMediaFile>) -> Unit,
    onRemoveAudio: (LocalMediaFile) -> Unit,
    onDeleteAudio: (LocalMediaFile) -> Unit,
    onRemoveAudios: (List<LocalMediaFile>) -> Unit,
    onDeleteAudios: (List<LocalMediaFile>) -> Unit,
    onRescanAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var searchKeyword by rememberSaveable { mutableStateOf("") }
    var isMultiSelectMode by rememberSaveable { mutableStateOf(false) }
    var selectedSongUris by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var showFolders by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val sortedSongs = remember(audioFiles) {
        audioFiles.distinctBy { it.uri }.sortedBy { it.displayTitle().lowercase() }
    }
    val shownSongs = remember(sortedSongs, searchKeyword) {
        val keyword = searchKeyword.trim()
        if (keyword.isBlank()) {
            sortedSongs
        } else {
            sortedSongs.filter {
                it.name.contains(keyword, ignoreCase = true) ||
                    it.displayTitle().contains(keyword, ignoreCase = true) ||
                    it.parentFolderName.orEmpty().contains(keyword, ignoreCase = true)
            }
        }
    }
    val recentlyPlayedCount = remember(audioProgressMap) {
        audioProgressMap.count { it.value > 0L }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MusicBackground)
    ) {
        if (isMultiSelectMode) {
            AudioMultiSelectBar(
                selectedCount = selectedSongUris.size,
                onCancel = {
                    isMultiSelectMode = false
                    selectedSongUris = emptySet()
                },
                onSelectAll = {
                    selectedSongUris = shownSongs.map { it.uri }.toSet()
                },
                onRemoveSelected = {
                    val selectedSongs = shownSongs.filter { it.uri in selectedSongUris }
                    if (selectedSongs.isEmpty()) {
                        Toast.makeText(context, "请先选择歌曲", Toast.LENGTH_SHORT).show()
                    } else {
                        onRemoveAudios(selectedSongs)
                        isMultiSelectMode = false
                        selectedSongUris = emptySet()
                    }
                },
                onDeleteSelected = {
                    if (selectedSongUris.isEmpty()) {
                        Toast.makeText(context, "请先选择歌曲", Toast.LENGTH_SHORT).show()
                    } else {
                        showBatchDeleteConfirm = true
                    }
                }
            )
        } else {
            MusicHomeHeader(
                isSearching = isSearching,
                searchKeyword = searchKeyword,
                onSearchKeywordChange = { searchKeyword = it },
                onToggleSearch = {
                    if (isSearching) searchKeyword = ""
                    isSearching = !isSearching
                },
                onAddFolder = onAddFolder,
                onShuffleAll = {
                    if (sortedSongs.isEmpty()) {
                        Toast.makeText(context, "暂无音乐", Toast.LENGTH_SHORT).show()
                    } else {
                        onShuffleAll(sortedSongs)
                    }
                },
                onRescanAudio = onRescanAudio,
                onStartMultiSelect = {
                    showFolders = false
                    isMultiSelectMode = true
                    selectedSongUris = emptySet()
                }
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 6.dp,
                bottom = 18.dp
            ),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            item {
                MusicEntryCards(
                    likedCount = 0,
                    recentCount = recentlyPlayedCount,
                    playlistCount = audioFolders.size,
                    onLikedClick = {
                        Toast.makeText(context, "喜欢歌曲后续支持", Toast.LENGTH_SHORT).show()
                    },
                    onRecentClick = {
                        Toast.makeText(
                            context,
                            if (recentlyPlayedCount > 0) "最近播放会在后续整理为独立列表" else "暂无最近播放",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onPlaylistClick = {
                        if (audioFolders.isEmpty()) {
                            Toast.makeText(context, "暂无音乐文件夹", Toast.LENGTH_SHORT).show()
                        } else {
                            showFolders = !showFolders
                        }
                    }
                )
            }

            item {
                SectionHeader(
                    title = if (showFolders) "音乐文件夹" else "全部歌曲",
                    count = if (showFolders) "${audioFolders.size} 个" else "${shownSongs.size} 首"
                )
            }

            if (!permissionDeniedMessage.isNullOrBlank()) {
                item {
                    Text(
                        text = permissionDeniedMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (showFolders) {
                if (audioFolders.isEmpty()) {
                    item { MusicEmptyState("暂无音乐文件夹，下载音乐后会自动显示，也可以手动添加文件夹。") }
                } else {
                    items(audioFolders, key = { "${it.folder.id}:${it.source}" }) { item ->
                        MusicFolderRow(item = item, onClick = { onOpenFolder(item) })
                    }
                }
            } else {
                if (shownSongs.isEmpty()) {
                    item {
                        MusicEmptyState(
                            if (sortedSongs.isEmpty()) {
                                "暂无音乐，下载音乐后会自动显示，也可以手动添加文件夹。"
                            } else {
                                "没有找到相关歌曲"
                            }
                        )
                    }
                } else {
                    items(shownSongs, key = { it.uri }) { song ->
                        MusicSongRow(
                            file = song,
                            progressMs = audioProgressMap[song.uri] ?: 0L,
                            isCurrent = song.uri == currentAudioUri,
                            isSelectionMode = isMultiSelectMode,
                            isSelected = song.uri in selectedSongUris,
                            onToggleSelected = {
                                selectedSongUris = selectedSongUris.toggle(song.uri)
                            },
                            onRemove = { onRemoveAudio(song) },
                            onDelete = { onDeleteAudio(song) },
                            onClick = {
                                if (isMultiSelectMode) {
                                    selectedSongUris = selectedSongUris.toggle(song.uri)
                                } else {
                                    onOpenAudio(song, shownSongs)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showBatchDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            title = { Text("永久删除音乐？") },
            text = { Text("将删除选中的本地音乐文件，此操作无法从 Moon播放器 恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedSongs = shownSongs.filter { it.uri in selectedSongUris }
                        showBatchDeleteConfirm = false
                        onDeleteAudios(selectedSongs)
                        isMultiSelectMode = false
                        selectedSongUris = emptySet()
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun MusicHomeHeader(
    isSearching: Boolean,
    searchKeyword: String,
    onSearchKeywordChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onAddFolder: () -> Unit,
    onShuffleAll: () -> Unit,
    onRescanAudio: () -> Unit,
    onStartMultiSelect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MusicBackground)
            .padding(horizontal = 20.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = "音乐",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "本地音乐",
                    style = MaterialTheme.typography.labelSmall,
                    color = MusicMuted.copy(alpha = 0.82f)
                )
            }
            var expanded by remember { mutableStateOf(false) }
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                IconButton(onClick = onToggleSearch) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = if (isSearching) "关闭搜索" else "搜索",
                        tint = Color.White.copy(alpha = 0.82f)
                    )
                }
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "更多",
                            tint = Color.White.copy(alpha = 0.82f)
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("添加文件夹") },
                            onClick = {
                                expanded = false
                                onAddFolder()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("随机播放全部") },
                            onClick = {
                                expanded = false
                                onShuffleAll()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("多选") },
                            onClick = {
                                expanded = false
                                onStartMultiSelect()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("重扫") },
                            onClick = {
                                expanded = false
                                onRescanAudio()
                            }
                        )
                    }
                }
            }
        }

        if (isSearching) {
            OutlinedTextField(
                value = searchKeyword,
                onValueChange = onSearchKeywordChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = MusicMuted)
                },
                trailingIcon = {
                    if (searchKeyword.isNotBlank()) {
                        IconButton(onClick = { onSearchKeywordChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "清空搜索")
                        }
                    }
                },
                placeholder = { Text("搜索歌曲") }
            )
        }
    }
}

@Composable
private fun AudioMultiSelectBar(
    selectedCount: Int,
    onCancel: () -> Unit,
    onSelectAll: () -> Unit,
    onRemoveSelected: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MusicBackground)
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MusicSurface),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "已选择 $selectedCount 项",
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        Row {
            TextButton(onClick = onCancel) { Text("取消") }
            TextButton(onClick = onSelectAll) { Text("全选") }
            TextButton(onClick = onRemoveSelected) { Text("隐藏") }
            TextButton(onClick = onDeleteSelected) { Text("删除", color = Color(0xFFFF8B8B)) }
        }
    }
}

@Composable
private fun MusicEntryCards(
    likedCount: Int,
    recentCount: Int,
    playlistCount: Int,
    onLikedClick: () -> Unit,
    onRecentClick: () -> Unit,
    onPlaylistClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MusicEntryCard(
            modifier = Modifier.weight(1f),
            icon = "♥",
            title = "我喜欢",
            subtitle = "$likedCount 首歌曲",
            accent = Color(0xFFFF9AE6),
            onClick = onLikedClick
        )
        MusicEntryCard(
            modifier = Modifier.weight(1f),
            icon = "↻",
            title = "最近播放",
            subtitle = if (recentCount > 0) "$recentCount 首" else "暂无记录",
            accent = Color(0xFF22D3EE),
            onClick = onRecentClick
        )
        MusicEntryCard(
            modifier = Modifier.weight(1f),
            icon = "≡",
            title = "播放列表",
            subtitle = "$playlistCount 个列表",
            accent = Color(0xFFB28CFF),
            onClick = onPlaylistClick
        )
    }
}

@Composable
private fun MusicEntryCard(
    icon: String,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(82.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MusicSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = icon,
                color = accent,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MusicMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = count,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.82f)
        )
    }
}

@Composable
private fun MusicSongRow(
    file: LocalMediaFile,
    progressMs: Long,
    isCurrent: Boolean,
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
            .drawWithContent {
                drawContent()
                if (isCurrent) {
                    val strokeWidth = 1.05.dp.toPx()
                    val corner = 10.dp.toPx()
                    val edgeX = strokeWidth / 2f
                    val segmentHeight = size.height * 0.34f
                    val segmentTop = (size.height - segmentHeight) / 2f
                    drawRoundRect(
                        color = Color(0xFFB28CFF).copy(alpha = 0.24f),
                        cornerRadius = CornerRadius(corner, corner),
                        style = Stroke(width = strokeWidth)
                    )
                    drawLine(
                        color = Color(0xFFFFA7FF).copy(alpha = 0.58f),
                        start = Offset(edgeX, segmentTop),
                        end = Offset(edgeX, segmentTop + segmentHeight),
                        strokeWidth = 1.7.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> Color(0xFF21183A)
                isCurrent -> Color(0xFF121023)
                else -> Color.Transparent
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .background(
                    if (isCurrent) {
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF8B5CFF).copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    } else {
                        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                    }
                )
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isSelectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onToggleSelected() })
            }
            DefaultMusicArtwork(isCurrent = isCurrent)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = file.displayTitle(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrent) Color(0xFFFFA7FF) else Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = file.musicSubtitle(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MusicMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (progressMs > 0L) {
                    Text(
                        text = "上次 ${formatDuration(progressMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB28CFF)
                    )
                }
            }
            Text(
                text = file.durationText(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.66f),
                maxLines = 1
            )
            if (!isSelectionMode) {
                SongMoreMenu(onRemove = onRemove, onDelete = onDelete)
            }
        }
    }
}

@Composable
private fun DefaultMusicArtwork(isCurrent: Boolean) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    colors = if (isCurrent) {
                        listOf(Color(0xFF4A2C73), Color(0xFF1A1830))
                    } else {
                        listOf(Color(0xFF33284F), Color(0xFF20212F))
                    }
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF101019)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "◉",
                color = if (isCurrent) Color(0xFFFFA7FF) else Color(0xFFC8B7FF),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun SongMoreMenu(
    onRemove: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(34.dp)) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = "更多",
                tint = Color.White.copy(alpha = 0.82f)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(14.dp)
        ) {
            DropdownMenuItem(
                text = { Text("从列表隐藏") },
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
            title = { Text("永久删除？") },
            text = { Text("此操作会删除本地文件，无法从 Moon播放器 恢复。") },
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

@Composable
private fun MusicFolderRow(
    item: MediaFolderGroupUiModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MusicSurface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DefaultMusicArtwork(isCurrent = false)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.folder.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.files.size} 首音乐 · ${item.source}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MusicMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MusicEmptyState(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MusicSurface)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MusicMuted
        )
    }
}

private fun LocalMediaFile.displayTitle(): String {
    return name.substringBeforeLast('.', name)
}

private fun LocalMediaFile.musicSubtitle(): String {
    return listOfNotNull(
        parentFolderName?.takeIf { it.isNotBlank() },
        formatFileSize(size).takeIf { it.isNotBlank() }
    ).joinToString(" · ").ifBlank { "本地音乐" }
}

private fun LocalMediaFile.durationText(): String {
    return durationMs?.takeIf { it > 0L }?.let(::formatDuration) ?: "--:--"
}

private fun Set<String>.toggle(value: String): Set<String> {
    return if (value in this) this - value else this + value
}

private val MusicBackground = Color(0xFF090A16)
private val MusicSurface = Color(0xFF15162A)
private val MusicMuted = Color(0xFFAAA3BF)

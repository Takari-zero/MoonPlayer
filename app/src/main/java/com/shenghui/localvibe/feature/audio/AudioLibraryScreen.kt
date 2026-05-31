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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shenghui.localvibe.core.media.formatDuration
import com.shenghui.localvibe.core.media.formatFileSize
import com.shenghui.localvibe.core.scanner.LocalMediaFile
import com.shenghui.localvibe.feature.library.MediaFolderGroupUiModel

private val AudioTabs = listOf("曲目", "列表", "专辑", "艺人", "文件夹")

@Composable
fun AudioLibraryScreen(
    audioFiles: List<LocalMediaFile>,
    audioFolders: List<MediaFolderGroupUiModel>,
    audioProgressMap: Map<String, Long>,
    permissionDeniedMessage: String?,
    onAddFolder: () -> Unit,
    onOpenFolder: (MediaFolderGroupUiModel) -> Unit,
    onOpenAudio: (LocalMediaFile, List<LocalMediaFile>) -> Unit,
    onShuffleAll: (List<LocalMediaFile>) -> Unit,
    onRescanAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableStateOf("曲目") }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var searchKeyword by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val sortedSongs = remember(audioFiles) {
        audioFiles.distinctBy { it.uri }.sortedBy { it.displayTitle().lowercase() }
    }
    val shownSongs = remember(sortedSongs, searchKeyword) {
        val keyword = searchKeyword.trim()
        if (keyword.isBlank()) sortedSongs else sortedSongs.filter {
            it.name.contains(keyword, ignoreCase = true) ||
                it.displayTitle().contains(keyword, ignoreCase = true)
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            AudioHeader(
                isSearching = isSearching,
                selectedTab = selectedTab,
                onSelectedTabChange = { selectedTab = it },
                searchKeyword = searchKeyword,
                onSearchKeywordChange = { searchKeyword = it },
                onToggleSearch = {
                    if (isSearching) {
                        searchKeyword = ""
                    }
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
                onMore = {
                    Toast.makeText(context, "更多功能后续实现", Toast.LENGTH_SHORT).show()
                }
            )

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 8.dp,
                        bottom = 112.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (selectedTab) {
                        "曲目" -> {
                            if (!permissionDeniedMessage.isNullOrBlank()) {
                                item {
                                    Text(
                                        text = permissionDeniedMessage,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            if (shownSongs.isEmpty()) {
                                item {
                                    Text(
                                        text = if (sortedSongs.isEmpty()) {
                                            "暂无音乐，下载音乐后会自动显示，也可以手动添加文件夹。"
                                        } else {
                                            "没有找到相关歌曲"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                items(shownSongs, key = { it.uri }) { song ->
                                    SongCard(
                                        file = song,
                                        progressMs = audioProgressMap[song.uri] ?: 0L,
                                        onClick = { onOpenAudio(song, shownSongs) }
                                    )
                                }
                            }
                        }

                        "文件夹" -> {
                            if (audioFolders.isEmpty()) {
                                item {
                                    Text(
                                        text = "暂无音乐文件夹，下载音乐后会自动显示，也可以手动添加文件夹。",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                items(audioFolders, key = { "${it.folder.id}:${it.source}" }) { item ->
                                    FolderCard(
                                        item = item,
                                        onClick = { onOpenFolder(item) }
                                    )
                                }
                            }
                        }

                        else -> item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                )
                            ) {
                                Text(
                                    text = "${selectedTab}功能后续实现",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                if (isSearching) {
                    FloatingSearchBar(
                        value = searchKeyword,
                        onValueChange = { searchKeyword = it },
                        placeholder = "搜索歌曲",
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioHeader(
    isSearching: Boolean,
    selectedTab: String,
    onSelectedTabChange: (String) -> Unit,
    searchKeyword: String,
    onSearchKeywordChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onAddFolder: () -> Unit,
    onShuffleAll: () -> Unit,
    onRescanAudio: () -> Unit,
    onMore: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("音乐", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "本地音乐",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                IconButton(onClick = onToggleSearch) {
                    Icon(Icons.Filled.Search, contentDescription = if (isSearching) "关闭搜索" else "搜索")
                }
                IconButton(onClick = onAddFolder) {
                    Icon(Icons.Filled.CreateNewFolder, contentDescription = "添加音乐文件夹")
                }
                IconButton(onClick = onShuffleAll) {
                    Icon(Icons.Filled.Shuffle, contentDescription = "随机播放全部")
                }
                IconButton(onClick = onRescanAudio) {
                    Icon(Icons.Filled.Refresh, contentDescription = "重新扫描")
                }
                IconButton(onClick = onMore) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AudioTabs.forEach { tab ->
                AudioTabChip(
                    modifier = Modifier.weight(1f),
                    selected = selectedTab == tab,
                    onClick = { onSelectedTabChange(tab) },
                    text = tab
                )
            }
        }
    }
}

@Composable
private fun AudioTabChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        )
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp, vertical = 6.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            fontSize = 12.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        )
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (value.isNotBlank()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "清空搜索")
                    }
                }
            },
            placeholder = { Text(placeholder) }
        )
    }
}

@Composable
private fun SongCard(
    file: LocalMediaFile,
    progressMs: Long,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text("♪", style = MaterialTheme.typography.titleLarge)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = file.displayTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Unknown · ${formatFileSize(file.size)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (progressMs > 0L) {
                    Text(
                        text = "上次播放到 ${formatDuration(progressMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            TextButton(
                onClick = {
                    Toast.makeText(context, "更多操作后续实现", Toast.LENGTH_SHORT).show()
                }
            ) {
                Text("更多")
            }
        }
    }
}

@Composable
private fun FolderCard(
    item: MediaFolderGroupUiModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = item.folder.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${item.files.size} 首音乐",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "来源：${item.source}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun LocalMediaFile.displayTitle(): String {
    return name.substringBeforeLast('.', name)
}

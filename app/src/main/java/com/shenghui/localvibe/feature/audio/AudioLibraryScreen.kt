package com.shenghui.localvibe.feature.audio

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shenghui.localvibe.core.media.formatDuration
import com.shenghui.localvibe.core.media.formatFileSize
import com.shenghui.localvibe.core.scanner.LocalMediaFile
import com.shenghui.localvibe.core.scanner.LocalMediaType
import com.shenghui.localvibe.feature.library.MediaFolderGroupUiModel
import kotlinx.coroutines.launch

enum class AudioSortMode(val label: String) {
    Default("默认排序"),
    Name("按名称排序"),
    Size("按大小排序"),
    Duration("按时长排序"),
    ModifiedTime("按修改时间排序")
}

@Composable
fun AudioLibraryScreen(
    audioFiles: List<LocalMediaFile>,
    audioFolders: List<MediaFolderGroupUiModel>,
    audioProgressMap: Map<String, Long>,
    favoriteAudioUris: Set<String>,
    recentAudioUris: List<String>,
    hasHiddenAudioSongs: Boolean,
    hasHiddenAudioFolders: Boolean,
    audioSortMode: AudioSortMode,
    audioSortAscending: Boolean,
    permissionDeniedMessage: String?,
    currentAudioUri: String?,
    librarySection: AudioLibrarySection,
    selectedAudioFolderKey: String?,
    onLibrarySectionChange: (AudioLibrarySection) -> Unit,
    onSelectedAudioFolderKeyChange: (String?) -> Unit,
    onAddFolder: () -> Unit,
    onOpenAudio: (LocalMediaFile, List<LocalMediaFile>) -> Unit,
    onToggleCurrentAudioPlayback: () -> Unit,
    onToggleFavoriteAudio: (LocalMediaFile) -> Unit,
    onRemoveFavoriteAudio: (LocalMediaFile) -> Unit,
    onQueueScopeChanged: (List<LocalMediaFile>) -> Unit,
    onPlayAll: (List<LocalMediaFile>) -> Unit,
    onShuffleAll: (List<LocalMediaFile>) -> Unit,
    onRemoveAudio: (LocalMediaFile) -> Unit,
    onDeleteAudio: (LocalMediaFile) -> Unit,
    onRemoveAudios: (List<LocalMediaFile>) -> Unit,
    onDeleteAudios: (List<LocalMediaFile>) -> Unit,
    onRemoveAudioFolder: (MediaFolderGroupUiModel) -> Unit,
    onDeleteAudioFolder: (MediaFolderGroupUiModel) -> Unit,
    onRestoreHiddenAudioSongs: () -> Unit,
    onRestoreHiddenAudioFolders: () -> Unit,
    onAudioSortModeChange: (AudioSortMode) -> Unit,
    onToggleAudioSortDirection: () -> Unit,
    onRescanAudio: () -> Unit,
    onBackToDesktop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var searchKeyword by rememberSaveable { mutableStateOf("") }
    var isMultiSelectMode by rememberSaveable { mutableStateOf(false) }
    var selectedSongUris by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var isFolderMultiSelectMode by rememberSaveable { mutableStateOf(false) }
    var selectedFolderKeys by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var showFolderBatchDeleteConfirm by remember { mutableStateOf(false) }
    val selectedAudioFolder = remember(audioFolders, selectedAudioFolderKey) {
        selectedAudioFolderKey?.let { key -> audioFolders.firstOrNull { it.selectionKey() == key } }
    }
    val showFolders = librarySection == AudioLibrarySection.Folders && selectedAudioFolder == null
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val sortedSongs = remember(audioFiles, audioSortMode, audioSortAscending) {
        audioFiles
            .distinctBy { it.uri }
            .sortedForAudioLibrary(audioSortMode, audioSortAscending)
    }
    val normalizedFavoriteUris = remember(favoriteAudioUris) {
        favoriteAudioUris.map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }
    val favoriteSongs = remember(sortedSongs, normalizedFavoriteUris) {
        sortedSongs.filter { it.uri.trim() in normalizedFavoriteUris }
    }
    val recentSongs = remember(sortedSongs, recentAudioUris) {
        val byUri = sortedSongs.associateBy { it.uri.trim() }
        recentAudioUris.mapNotNull { byUri[it.trim()] }.distinctBy { it.uri.trim() }
    }
    val sectionSongs = remember(
        sortedSongs,
        favoriteSongs,
        recentSongs,
        librarySection,
        selectedAudioFolder,
        audioSortMode,
        audioSortAscending
    ) {
        when {
            selectedAudioFolder != null -> selectedAudioFolder.files
                .filter { it.type == LocalMediaType.AUDIO }
                .distinctBy { it.uri }
                .sortedForAudioLibrary(audioSortMode, audioSortAscending)
            librarySection == AudioLibrarySection.Favorites -> favoriteSongs
            librarySection == AudioLibrarySection.Recent -> recentSongs
            else -> sortedSongs
        }
    }
    val shownSongs = remember(sectionSongs, searchKeyword) {
        val keyword = searchKeyword.trim()
        if (keyword.isBlank()) {
            sectionSongs
        } else {
            sectionSongs.filter {
                it.name.contains(keyword, ignoreCase = true) ||
                    it.displayTitle().contains(keyword, ignoreCase = true) ||
                    it.parentFolderName.orEmpty().contains(keyword, ignoreCase = true)
            }
        }
    }
    val recentlyPlayedCount = recentSongs.size
    val listBottomPadding = if (currentAudioUri.isNullOrBlank()) 32.dp else 128.dp
    val closeSearch = {
        searchKeyword = ""
        isSearching = false
        focusManager.clearFocus()
    }

    LaunchedEffect(librarySection, selectedAudioFolderKey, sectionSongs) {
        val shouldScopeQueue = selectedAudioFolder != null ||
            librarySection == AudioLibrarySection.Favorites
        if (shouldScopeQueue && sectionSongs.isNotEmpty()) {
            onQueueScopeChanged(sectionSongs)
        }
    }

    BackHandler {
        when {
            isSearching -> closeSearch()
            isMultiSelectMode -> {
                isMultiSelectMode = false
                selectedSongUris = emptySet()
            }
            isFolderMultiSelectMode -> {
                isFolderMultiSelectMode = false
                selectedFolderKeys = emptySet()
            }
            selectedAudioFolder != null -> {
                onSelectedAudioFolderKeyChange(null)
                selectedSongUris = emptySet()
            }
            librarySection != AudioLibrarySection.AllSongs -> {
                onLibrarySectionChange(AudioLibrarySection.AllSongs)
                selectedSongUris = emptySet()
                selectedFolderKeys = emptySet()
                onSelectedAudioFolderKeyChange(null)
            }
            else -> onBackToDesktop()
        }
    }
    val dismissSearchModifier = if (isSearching) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            closeSearch()
        }
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MusicBackground)
    ) {
        MusicHomeHeader(
            isSearching = isSearching,
            searchKeyword = searchKeyword,
            showFolders = showFolders,
            hasHiddenAudioSongs = hasHiddenAudioSongs,
            hasHiddenAudioFolders = hasHiddenAudioFolders,
            audioSortMode = audioSortMode,
            audioSortAscending = audioSortAscending,
            onSearchKeywordChange = { searchKeyword = it },
            onToggleSearch = {
                if (isSearching) {
                    closeSearch()
                } else {
                    isSearching = true
                }
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
            onRestoreHiddenAudioSongs = onRestoreHiddenAudioSongs,
            onRestoreHiddenAudioFolders = onRestoreHiddenAudioFolders,
            onAudioSortModeChange = onAudioSortModeChange,
            onToggleAudioSortDirection = onToggleAudioSortDirection,
            onStartMultiSelect = {
                onLibrarySectionChange(AudioLibrarySection.AllSongs)
                isFolderMultiSelectMode = false
                selectedFolderKeys = emptySet()
                isMultiSelectMode = true
                selectedSongUris = emptySet()
            },
            onStartFolderMultiSelect = {
                onLibrarySectionChange(AudioLibrarySection.Folders)
                isMultiSelectMode = false
                selectedSongUris = emptySet()
                isFolderMultiSelectMode = true
                selectedFolderKeys = emptySet()
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(dismissSearchModifier)
                .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            MusicEntryCards(
                likedCount = favoriteSongs.size,
                recentCount = recentlyPlayedCount,
                playlistCount = audioFolders.size,
                onLikedClick = {
                    onLibrarySectionChange(AudioLibrarySection.Favorites)
                    isMultiSelectMode = false
                    selectedSongUris = emptySet()
                    isFolderMultiSelectMode = false
                    selectedFolderKeys = emptySet()
                    onSelectedAudioFolderKeyChange(null)
                },
                onRecentClick = {
                    onLibrarySectionChange(AudioLibrarySection.Recent)
                    isMultiSelectMode = false
                    selectedSongUris = emptySet()
                    isFolderMultiSelectMode = false
                    selectedFolderKeys = emptySet()
                    onSelectedAudioFolderKeyChange(null)
                },
                onPlaylistClick = {
                    if (audioFolders.isEmpty()) {
                        Toast.makeText(context, "暂无音乐文件夹", Toast.LENGTH_SHORT).show()
                    } else {
                        onLibrarySectionChange(
                            if (showFolders) AudioLibrarySection.AllSongs else AudioLibrarySection.Folders
                        )
                        isMultiSelectMode = false
                        selectedSongUris = emptySet()
                        isFolderMultiSelectMode = false
                        selectedFolderKeys = emptySet()
                        onSelectedAudioFolderKeyChange(null)
                    }
                }
            )
            SectionHeader(
                title = when {
                    selectedAudioFolder != null -> "音乐文件夹-${selectedAudioFolder.folder.name.ifBlank { selectedAudioFolder.folder.id }}"
                    librarySection == AudioLibrarySection.Folders -> "音乐文件夹"
                    librarySection == AudioLibrarySection.Favorites -> "我喜欢"
                    librarySection == AudioLibrarySection.Recent -> "最近播放"
                    else -> "主页"
                },
                count = if (showFolders) "${audioFolders.size} 个" else "共 ${shownSongs.size} 首歌曲",
                actionText = when {
                    selectedAudioFolder != null -> "返回音乐文件夹"
                    librarySection == AudioLibrarySection.Folders -> null
                    librarySection == AudioLibrarySection.AllSongs -> "定位到当前播放位置"
                    else -> "返回主页"
                },
                actionEnabled = when {
                    selectedAudioFolder != null -> true
                    librarySection == AudioLibrarySection.AllSongs -> !currentAudioUri.isNullOrBlank()
                    librarySection == AudioLibrarySection.Folders -> false
                    else -> true
                },
                onActionClick = {
                    when {
                        selectedAudioFolder != null -> {
                            onSelectedAudioFolderKeyChange(null)
                            isMultiSelectMode = false
                            selectedSongUris = emptySet()
                        }

                        librarySection != AudioLibrarySection.AllSongs -> {
                            onLibrarySectionChange(AudioLibrarySection.AllSongs)
                            isMultiSelectMode = false
                            selectedSongUris = emptySet()
                        }

                        else -> {
                        val currentIndex = if (showFolders) {
                            -1
                        } else {
                            shownSongs.indexOfFirst { it.uri == currentAudioUri }
                        }
                        if (currentIndex >= 0) {
                            coroutineScope.launch {
                                listState.animateScrollToItem(currentIndex)
                            }
                        } else {
                            Toast.makeText(context, "当前播放歌曲不在当前列表中", Toast.LENGTH_SHORT).show()
                        }
                        }
                    }
                }
            )
            if (
                !isMultiSelectMode &&
                (selectedAudioFolder != null ||
                    librarySection == AudioLibrarySection.Favorites)
            ) {
                AudioPlayAllActions(
                    onPlayAll = {
                        if (shownSongs.isEmpty()) {
                            Toast.makeText(context, "暂无可播放歌曲", Toast.LENGTH_SHORT).show()
                        } else {
                            onPlayAll(shownSongs)
                        }
                    },
                    onShuffleAll = {
                        if (shownSongs.isEmpty()) {
                            Toast.makeText(context, "暂无可播放歌曲", Toast.LENGTH_SHORT).show()
                        } else {
                            onShuffleAll(shownSongs)
                        }
                    }
                )
            }
            if (isMultiSelectMode && !showFolders) {
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
            }
            if (isFolderMultiSelectMode && showFolders) {
                AudioMultiSelectBar(
                    selectedCount = selectedFolderKeys.size,
                    removeText = "隐藏",
                    onCancel = {
                        isFolderMultiSelectMode = false
                        selectedFolderKeys = emptySet()
                    },
                    onSelectAll = {
                        selectedFolderKeys = audioFolders.map { it.selectionKey() }.toSet()
                    },
                    onRemoveSelected = {
                        val selectedFolders = audioFolders.filter { it.selectionKey() in selectedFolderKeys }
                        if (selectedFolders.isEmpty()) {
                            Toast.makeText(context, "请先选择文件夹", Toast.LENGTH_SHORT).show()
                        } else {
                            selectedFolders.forEach(onRemoveAudioFolder)
                            isFolderMultiSelectMode = false
                            selectedFolderKeys = emptySet()
                        }
                    },
                    onDeleteSelected = {
                        if (selectedFolderKeys.isEmpty()) {
                            Toast.makeText(context, "请先选择文件夹", Toast.LENGTH_SHORT).show()
                        } else {
                            showFolderBatchDeleteConfirm = true
                        }
                    }
                )
            }
        }


        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .then(dismissSearchModifier)
                .weight(1f),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 0.dp,
                bottom = listBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {

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
                        MusicFolderRow(
                            item = item,
                            isSelectionMode = isFolderMultiSelectMode,
                            isSelected = item.selectionKey() in selectedFolderKeys,
                            onClick = {
                                if (isFolderMultiSelectMode) {
                                    selectedFolderKeys = selectedFolderKeys.toggle(item.selectionKey())
                                } else {
                                    if (isSearching) closeSearch()
                                    onSelectedAudioFolderKeyChange(item.selectionKey())
                                    isMultiSelectMode = false
                                    selectedSongUris = emptySet()
                                }
                            },
                            onToggleSelected = {
                                selectedFolderKeys = selectedFolderKeys.toggle(item.selectionKey())
                            },
                            onRemove = { onRemoveAudioFolder(item) },
                            onDelete = { onDeleteAudioFolder(item) }
                        )
                    }
                }
            } else {
                if (shownSongs.isEmpty()) {
                    item {
                        MusicEmptyState(
                            when {
                                searchKeyword.isNotBlank() -> "没有找到相关歌曲"
                                librarySection == AudioLibrarySection.Favorites -> "还没有喜欢的歌曲\n在播放页点击收藏后会出现在这里"
                                librarySection == AudioLibrarySection.Recent -> "暂无最近播放"
                                sortedSongs.isEmpty() -> "暂无音乐，下载音乐后会自动显示，也可以手动添加文件夹。"
                                else -> "没有找到相关歌曲"
                            }
                        )
                    }
                } else {
                    items(shownSongs, key = { it.uri }) { song ->
                        MusicSongRow(
                            file = song,
                            progressMs = audioProgressMap[song.uri] ?: 0L,
                            isCurrent = song.uri == currentAudioUri,
                            isFavorite = song.uri.trim() in normalizedFavoriteUris,
                            isSelectionMode = isMultiSelectMode,
                            isSelected = song.uri in selectedSongUris,
                            onToggleSelected = {
                                selectedSongUris = selectedSongUris.toggle(song.uri)
                            },
                            onRemove = { onRemoveAudio(song) },
                            onDelete = { onDeleteAudio(song) },
                            onToggleFavorite = {
                                onToggleFavoriteAudio(song)
                                Toast.makeText(
                                    context,
                                    if (song.uri.trim() in normalizedFavoriteUris) {
                                        "已取消喜欢"
                                    } else {
                                        "已加入我喜欢"
                                    },
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onRemoveFavorite = if (librarySection == AudioLibrarySection.Favorites) {
                                { onRemoveFavoriteAudio(song) }
                            } else {
                                null
                            },
                            onClick = {
                                if (isMultiSelectMode) {
                                    selectedSongUris = selectedSongUris.toggle(song.uri)
                                } else if (song.uri == currentAudioUri) {
                                    onToggleCurrentAudioPlayback()
                                    if (isSearching) closeSearch()
                                } else {
                                    if (librarySection == AudioLibrarySection.Recent && selectedAudioFolder == null) {
                                        val songUri = song.uri.trim()
                                        val sourceSong = sortedSongs.firstOrNull {
                                            it.uri.trim() == songUri
                                        }
                                        if (sourceSong == null) {
                                            Toast.makeText(context, "歌曲不存在或已被移除", Toast.LENGTH_SHORT).show()
                                        } else {
                                            onOpenAudio(sourceSong, sortedSongs)
                                        }
                                    } else {
                                        onOpenAudio(song, shownSongs)
                                    }
                                    if (isSearching) closeSearch()
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

    if (showFolderBatchDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showFolderBatchDeleteConfirm = false },
            title = { Text("删除文件夹内音乐？") },
            text = { Text("将删除选中文件夹下已扫描到的音频文件，不会删除文件夹本身或非音频文件。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedFolders = audioFolders.filter { it.selectionKey() in selectedFolderKeys }
                        showFolderBatchDeleteConfirm = false
                        selectedFolders.forEach(onDeleteAudioFolder)
                        isFolderMultiSelectMode = false
                        selectedFolderKeys = emptySet()
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFolderBatchDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun AudioSortMenuItems(
    audioSortMode: AudioSortMode,
    audioSortAscending: Boolean,
    onAudioSortModeChange: (AudioSortMode) -> Unit,
    onToggleAudioSortDirection: () -> Unit,
    onDismissMenu: () -> Unit
) {
    AudioSortMode.values().forEach { mode ->
        if (mode == AudioSortMode.ModifiedTime) return@forEach
        DropdownMenuItem(
            text = { Text(mode.label + if (mode == audioSortMode) " ✓" else "") },
            onClick = {
                onDismissMenu()
                onAudioSortModeChange(mode)
            }
        )
    }
    DropdownMenuItem(
        text = { Text(if (audioSortAscending) "切换降序" else "切换升序") },
        onClick = {
            onDismissMenu()
            onToggleAudioSortDirection()
        }
    )
}

@Composable
private fun MusicHomeHeader(
    isSearching: Boolean,
    searchKeyword: String,
    showFolders: Boolean,
    hasHiddenAudioSongs: Boolean,
    hasHiddenAudioFolders: Boolean,
    audioSortMode: AudioSortMode,
    audioSortAscending: Boolean,
    onSearchKeywordChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onAddFolder: () -> Unit,
    onShuffleAll: () -> Unit,
    onRescanAudio: () -> Unit,
    onRestoreHiddenAudioSongs: () -> Unit,
    onRestoreHiddenAudioFolders: () -> Unit,
    onAudioSortModeChange: (AudioSortMode) -> Unit,
    onToggleAudioSortDirection: () -> Unit,
    onStartMultiSelect: () -> Unit,
    onStartFolderMultiSelect: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MusicBackground)
            .padding(horizontal = 20.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(if (isSearching) 0.42f else 1f)
                    .padding(end = if (isSearching) 2.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = "音乐",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "本地音乐",
                    style = MaterialTheme.typography.labelSmall,
                    color = MusicMuted.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isSearching) {
                BasicTextField(
                    value = searchKeyword,
                    onValueChange = { onSearchKeywordChange(it) },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(
                            width = 1.dp,
                            color = Color(0xFF8B5CFF).copy(alpha = 0.38f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 12.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White,
                        fontSize = 13.sp
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchKeyword.isBlank()) {
                                Text(
                                    text = "搜索歌曲",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MusicMuted.copy(alpha = 0.72f),
                                    maxLines = 1
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                IconButton(onClick = onToggleSearch) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "搜索",
                        tint = Color.White.copy(alpha = 0.82f)
                    )
                }
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "更多",
                            tint = Color.White.copy(alpha = 0.56f)
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
                        AudioSortMenuItems(
                            audioSortMode = audioSortMode,
                            audioSortAscending = audioSortAscending,
                            onAudioSortModeChange = onAudioSortModeChange,
                            onToggleAudioSortDirection = onToggleAudioSortDirection,
                            onDismissMenu = { expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text(if (showFolders) "多选管理" else "多选") },
                            onClick = {
                                expanded = false
                                if (showFolders) {
                                    onStartFolderMultiSelect()
                                } else {
                                    onStartMultiSelect()
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("重扫") },
                            onClick = {
                                expanded = false
                                onRescanAudio()
                            }
                        )
                        if (hasHiddenAudioSongs) {
                            DropdownMenuItem(
                                text = { Text("恢复隐藏歌曲") },
                                onClick = {
                                    expanded = false
                                    onRestoreHiddenAudioSongs()
                                }
                            )
                        }
                        if (hasHiddenAudioFolders) {
                            DropdownMenuItem(
                                text = { Text("恢复隐藏音乐文件夹") },
                                onClick = {
                                    expanded = false
                                    onRestoreHiddenAudioFolders()
                                }
                            )
                        }
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    IconButton(onClick = onToggleSearch) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "搜索",
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
                            AudioSortMenuItems(
                                audioSortMode = audioSortMode,
                                audioSortAscending = audioSortAscending,
                                onAudioSortModeChange = onAudioSortModeChange,
                                onToggleAudioSortDirection = onToggleAudioSortDirection,
                                onDismissMenu = { expanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text(if (showFolders) "多选管理" else "多选") },
                                onClick = {
                                    expanded = false
                                    if (showFolders) {
                                        onStartFolderMultiSelect()
                                    } else {
                                        onStartMultiSelect()
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("重扫") },
                                onClick = {
                                    expanded = false
                                    onRescanAudio()
                                }
                            )
                            if (hasHiddenAudioSongs) {
                                DropdownMenuItem(
                                    text = { Text("恢复隐藏歌曲") },
                                    onClick = {
                                        expanded = false
                                        onRestoreHiddenAudioSongs()
                                    }
                                )
                            }
                            if (hasHiddenAudioFolders) {
                                DropdownMenuItem(
                                    text = { Text("恢复隐藏音乐文件夹") },
                                    onClick = {
                                        expanded = false
                                        onRestoreHiddenAudioFolders()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioPlayAllActions(
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AudioActionChip(
            text = "播放全部",
            onClick = onPlayAll
        )
        AudioActionChip(
            text = "随机播放",
            onClick = onShuffleAll
        )
    }
}

@Composable
private fun AudioActionChip(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF15162A).copy(alpha = 0.92f))
            .border(
                width = 1.dp,
                color = Color(0xFF8B5CFF).copy(alpha = 0.26f),
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFE7DCFF),
            maxLines = 1
        )
    }
}

@Composable
private fun AudioMultiSelectBar(
    selectedCount: Int,
    removeText: String = "隐藏",
    onCancel: () -> Unit,
    onSelectAll: () -> Unit,
    onRemoveSelected: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF15162A).copy(alpha = 0.92f))
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "已选择 $selectedCount 项",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        CompactActionText(text = "取消", onClick = onCancel)
        CompactActionText(text = "全选", onClick = onSelectAll)
        CompactActionText(text = removeText, onClick = onRemoveSelected)
        CompactActionText(text = "删除", color = Color(0xFFFF8B8B), onClick = onDeleteSelected)
    }
}

@Composable
private fun CompactActionText(
    text: String,
    color: Color = Color(0xFFB28CFF),
    onClick: () -> Unit
) {
    Text(
        text = text,
        modifier = Modifier.clickable(onClick = onClick),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip
    )
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
            accent = FavoritePink,
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
            title = "音乐文件夹",
            subtitle = "$playlistCount 个文件夹",
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
private fun SectionHeader(
    title: String,
    count: String,
    actionText: String? = null,
    actionEnabled: Boolean = false,
    onActionClick: (() -> Unit)? = null
) {
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (actionText != null && onActionClick != null) {
                TextButton(
                    onClick = onActionClick,
                    enabled = actionEnabled,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        text = actionText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (actionEnabled) {
                            Color(0xFFB28CFF)
                        } else {
                            Color.White.copy(alpha = 0.28f)
                        }
                    )
                }
            }
            Text(
                text = count,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.82f)
            )
        }
    }
}

@Composable
private fun MusicSongRow(
    file: LocalMediaFile,
    progressMs: Long,
    isCurrent: Boolean,
    isFavorite: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelected: () -> Unit,
    onRemove: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemoveFavorite: (() -> Unit)?,
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
            if (isSelectionMode) {
                Text(
                    modifier = Modifier.width(88.dp),
                    text = file.durationText(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.66f),
                    maxLines = 1,
                    textAlign = TextAlign.End
                )
            } else {
                Row(
                    modifier = Modifier.width(88.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFavorite) "取消喜欢" else "加入我喜欢",
                            tint = if (isFavorite) FavoritePink else Color.White.copy(alpha = 0.58f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        modifier = Modifier.width(38.dp),
                        text = file.durationText(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.66f),
                        maxLines = 1,
                        textAlign = TextAlign.End
                    )
                    SongMoreMenu(
                        onRemoveFavorite = onRemoveFavorite,
                        onRemove = onRemove,
                        onDelete = onDelete
                    )
                }
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
    onRemoveFavorite: (() -> Unit)?,
    onRemove: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(24.dp)) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = "更多",
                tint = Color.White.copy(alpha = 0.82f),
                modifier = Modifier.size(20.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(14.dp)
        ) {
            if (onRemoveFavorite != null) {
                DropdownMenuItem(
                    text = { Text("移除喜欢") },
                    onClick = {
                        expanded = false
                        onRemoveFavorite()
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("隐藏") },
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
@OptIn(ExperimentalFoundationApi::class)
private fun MusicFolderRow(
    item: MediaFolderGroupUiModel,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onToggleSelected: () -> Unit,
    onRemove: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val folderSize = remember(item.files) { item.files.sumOf { it.size.coerceAtLeast(0L) } }

    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        if (isSelectionMode) {
                            onToggleSelected()
                        } else {
                            expanded = true
                        }
                    }
                ),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) Color(0xFF21183A) else MusicSurface
            )
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isSelectionMode) {
                    Checkbox(checked = isSelected, onCheckedChange = { onToggleSelected() })
                }
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
                        text = "${item.files.size} 首音乐 · ${formatFileSize(folderSize)} · ${item.source}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MusicMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!isSelectionMode) {
                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "文件夹操作",
                                tint = MusicMuted
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("隐藏") },
                                onClick = {
                                    expanded = false
                                    onRemove()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("删除音频文件") },
                                onClick = {
                                    expanded = false
                                    showDeleteConfirm = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除文件夹内音乐？") },
            text = { Text("只会删除该文件夹中已扫描到的 ${item.files.size} 个音频文件，不会删除文件夹本身或其它非音频文件。") },
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

private fun List<LocalMediaFile>.sortedForAudioLibrary(
    mode: AudioSortMode,
    ascending: Boolean
): List<LocalMediaFile> {
    if (mode == AudioSortMode.Default || mode == AudioSortMode.ModifiedTime) {
        val withModifiedTime = filter { (it.modifiedAt ?: 0L) > 0L }
        val withoutModifiedTime = filterNot { (it.modifiedAt ?: 0L) > 0L }
        val sortedWithModifiedTime = if (ascending) {
            withModifiedTime.sortedBy { it.modifiedAt ?: Long.MAX_VALUE }
        } else {
            withModifiedTime.sortedByDescending { it.modifiedAt ?: Long.MIN_VALUE }
        }
        return sortedWithModifiedTime + withoutModifiedTime.sortedBy { it.displayTitle().lowercase() }
    }

    val comparator = when (mode) {
        AudioSortMode.Name -> compareBy<LocalMediaFile> { it.displayTitle().lowercase() }
            .thenBy { it.name.lowercase() }

        AudioSortMode.Size -> compareBy<LocalMediaFile> { it.size }
            .thenBy { it.displayTitle().lowercase() }

        AudioSortMode.Duration -> compareBy<LocalMediaFile> { it.durationMs ?: Long.MAX_VALUE }
            .thenBy { it.displayTitle().lowercase() }

        AudioSortMode.Default,
        AudioSortMode.ModifiedTime -> compareBy { it.displayTitle().lowercase() }
    }

    return if (ascending) {
        sortedWith(comparator)
    } else {
        sortedWith(comparator.reversed())
    }
}

private fun Set<String>.toggle(value: String): Set<String> {
    return if (value in this) this - value else this + value
}

private fun MediaFolderGroupUiModel.selectionKey(): String {
    return "${folder.id}:${source}"
}

enum class AudioLibrarySection {
    AllSongs,
    Favorites,
    Recent,
    Folders
}

private val MusicBackground = Color(0xFF090A16)
private val MusicSurface = Color(0xFF15162A)
private val MusicMuted = Color(0xFFAAA3BF)
private val FavoritePink = Color(0xFFFF9AE6)

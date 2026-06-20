package com.shenghui.localvibe.feature.book

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shenghui.localvibe.core.ui.MoonInlineSearchField
import com.shenghui.localvibe.core.scanner.LocalMediaFile

@Composable
fun BookLibraryScreen(
    bookFiles: List<LocalMediaFile>,
    bookProgressPercentMap: Map<String, Int>,
    onImportBookFile: () -> Unit,
    onRescanBooks: () -> Unit,
    onRemoveBook: (LocalMediaFile) -> Unit,
    onDeleteBook: (LocalMediaFile) -> Unit,
    onRemoveBooks: (List<LocalMediaFile>) -> Unit,
    onDeleteBooks: (List<LocalMediaFile>) -> Unit,
    onOpenBook: (LocalMediaFile) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val searchFocusRequester = remember { FocusRequester() }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var searchKeyword by rememberSaveable { mutableStateOf("") }
    var isMultiSelectMode by rememberSaveable { mutableStateOf(false) }
    var selectedBookUris by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    val shownBooks = remember(bookFiles, searchKeyword) {
        val sortedBooks = bookFiles.distinctBy { it.uri }.sortedBy { it.displayTitle().lowercase() }
        val keyword = searchKeyword.trim()
        if (keyword.isBlank()) {
            sortedBooks
        } else {
            sortedBooks.filter { it.displayTitle().contains(keyword, ignoreCase = true) }
        }
    }

    fun closeSearch() {
        searchKeyword = ""
        isSearching = false
        focusManager.clearFocus()
    }

    BackHandler(enabled = isSearching) {
        closeSearch()
    }

    androidx.compose.runtime.LaunchedEffect(isSearching) {
        if (isSearching) {
            searchFocusRequester.requestFocus()
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BookHeader(
                isSearching = isSearching,
                searchKeyword = searchKeyword,
                onSearchKeywordChange = { searchKeyword = it },
                searchModifier = Modifier.focusRequester(searchFocusRequester),
                isMultiSelectMode = isMultiSelectMode,
                selectedCount = selectedBookUris.size,
                onToggleSearch = {
                    if (isSearching) {
                        closeSearch()
                    } else {
                        isSearching = true
                    }
                },
                onImportBookFile = onImportBookFile,
                onRescanBooks = onRescanBooks,
                onManage = {
                    Toast.makeText(context, "管理功能后续实现", Toast.LENGTH_SHORT).show()
                },
                onMore = {
                    Toast.makeText(context, "更多功能后续实现", Toast.LENGTH_SHORT).show()
                },
                onStartMultiSelect = {
                    isMultiSelectMode = true
                    selectedBookUris = emptySet()
                },
                onCancelMultiSelect = {
                    isMultiSelectMode = false
                    selectedBookUris = emptySet()
                },
                onSelectAll = {
                    selectedBookUris = shownBooks.map { it.uri }.toSet()
                },
                onRemoveSelected = {
                    val selectedBooks = shownBooks.filter { it.uri in selectedBookUris }
                    if (selectedBooks.isEmpty()) {
                        Toast.makeText(context, "请先选择项目", Toast.LENGTH_SHORT).show()
                    } else {
                        onRemoveBooks(selectedBooks)
                        selectedBookUris = emptySet()
                        isMultiSelectMode = false
                    }
                },
                onDeleteSelected = {
                    if (selectedBookUris.isEmpty()) {
                        Toast.makeText(context, "请先选择项目", Toast.LENGTH_SHORT).show()
                    } else {
                        showBatchDeleteConfirm = true
                    }
                }
            )

            Box(modifier = Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isSearching && !isMultiSelectMode) {
                                Modifier.clickable { closeSearch() }
                            } else {
                                Modifier
                            }
                        ),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    item(key = "add-book") {
                        AddBookCard(onClick = onImportBookFile)
                    }

                    items(items = shownBooks, key = { it.uri }) { book ->
                        BookCard(
                            file = book,
                            progressPercent = bookProgressPercentMap[book.uri] ?: 0,
                            isSelectionMode = isMultiSelectMode,
                            isSelected = book.uri in selectedBookUris,
                            onToggleSelected = {
                                selectedBookUris = selectedBookUris.toggle(book.uri)
                            },
                            onRemove = { onRemoveBook(book) },
                            onDelete = { onDeleteBook(book) },
                            onClick = {
                                if (isMultiSelectMode) {
                                    selectedBookUris = selectedBookUris.toggle(book.uri)
                                } else {
                                    if (isSearching) closeSearch()
                                    onOpenBook(book)
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
            title = { Text("永久删除小说？") },
            text = { Text("将删除选中的 TXT 文件，此操作无法从 Moon播放器 恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedBooks = shownBooks.filter { it.uri in selectedBookUris }
                        showBatchDeleteConfirm = false
                        onDeleteBooks(selectedBooks)
                        selectedBookUris = emptySet()
                        isMultiSelectMode = false
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
private fun BookHeader(
    isSearching: Boolean,
    searchKeyword: String,
    onSearchKeywordChange: (String) -> Unit,
    searchModifier: Modifier = Modifier,
    isMultiSelectMode: Boolean,
    selectedCount: Int,
    onToggleSearch: () -> Unit,
    onImportBookFile: () -> Unit,
    onRescanBooks: () -> Unit,
    onManage: () -> Unit,
    onMore: () -> Unit,
    onStartMultiSelect: () -> Unit,
    onCancelMultiSelect: () -> Unit,
    onSelectAll: () -> Unit,
    onRemoveSelected: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    if (isMultiSelectMode) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancelMultiSelect) { Text("取消") }
            Text(
                text = "已选择 $selectedCount 项",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Row {
                TextButton(onClick = onSelectAll) { Text("全选") }
                TextButton(onClick = onRemoveSelected) { Text("移除") }
                TextButton(onClick = onDeleteSelected) { Text("删除") }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(if (isSearching) 0.42f else 1f)
                    .padding(end = if (isSearching) 2.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    "小说",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "TXT 小说",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isSearching) {
                MoonInlineSearchField(
                    value = searchKeyword,
                    onValueChange = onSearchKeywordChange,
                    placeholder = "搜索小说",
                    textColor = MaterialTheme.colorScheme.onSurface,
                    placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    modifier = searchModifier
                        .weight(1f)
                        .padding(end = 4.dp)
                )
            }
            var expanded by remember { mutableStateOf(false) }
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                BookHeaderIconButton(
                    icon = Icons.Filled.Search,
                    contentDescription = if (isSearching) "关闭搜索" else "搜索",
                    onClick = onToggleSearch
                )
                BookHeaderIconButton(
                    icon = Icons.Filled.Add,
                    contentDescription = "导入文档",
                    onClick = onImportBookFile
                )
                BookHeaderIconButton(
                    icon = Icons.Filled.Refresh,
                    contentDescription = "重新扫描",
                    onClick = onRescanBooks
                )
                Box {
                    BookHeaderIconButton(
                        icon = Icons.Filled.MoreVert,
                        contentDescription = "更多",
                        onClick = { expanded = true }
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("管理") },
                            onClick = {
                                expanded = false
                                onManage()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("多选删除") },
                            onClick = {
                                expanded = false
                                onStartMultiSelect()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("更多功能后续实现") },
                            onClick = {
                                expanded = false
                                onMore()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookHeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun AddBookCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.62f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = "导入本地文档",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookCard(
    file: LocalMediaFile,
    progressPercent: Int,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelected: () -> Unit,
    onRemove: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = {
                if (isSelectionMode) onToggleSelected() else expanded = true
            }
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.62f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "txt",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Text("✓", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = file.displayTitle(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = progressPercent.toBookProgressText(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!isSelectionMode) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("从书架移除") },
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
        }
        Text(
            text = file.displayTitle(),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("永久删除小说？") },
            text = { Text("此操作会删除本地 TXT 文件，无法从 Moon播放器 恢复。") },
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

private fun LocalMediaFile.displayTitle(): String {
    return name.substringBeforeLast('.', name)
}

private fun Int.toBookProgressText(): String {
    return when {
        this <= 0 -> "未开始"
        this >= 100 -> "已完成"
        else -> "已听 $this%"
    }
}

private fun Set<String>.toggle(value: String): Set<String> {
    return if (value in this) this - value else this + value
}

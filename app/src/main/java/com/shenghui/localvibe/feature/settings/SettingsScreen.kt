package com.shenghui.localvibe.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val SettingsItems = listOf(
    "播放设置",
    "扫描设置",
    "主题设置",
    "关于 Moon播放器"
)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onClearProgress: () -> Unit,
    onClearFolders: () -> Unit,
    onClearBooks: () -> Unit,
    onRescanMedia: () -> Unit,
    onRestoreHiddenAudio: () -> Unit,
    onRestoreHiddenVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRestoreHiddenVideoDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(48.dp)
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                }
                Text(
                    text = "设置",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 56.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(48.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsItems.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Text(
                            text = item,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                SettingsActionItem(
                    title = "清除播放进度",
                    onClick = onClearProgress
                )
                SettingsActionItem(
                    title = "清除已添加文件夹记录",
                    onClick = onClearFolders
                )
                SettingsActionItem(
                    title = "清除小说导入记录",
                    onClick = onClearBooks
                )
                SettingsActionItem(
                    title = "重新扫描媒体",
                    onClick = onRescanMedia
                )
                SettingsActionItem(
                    title = "恢复隐藏音乐",
                    onClick = onRestoreHiddenAudio
                )
                SettingsActionItem(
                    title = "恢复隐藏视频",
                    subtitle = "显示之前从视频列表移除的视频和隐藏的视频文件夹",
                    onClick = { showRestoreHiddenVideoDialog = true }
                )
            }
        }
    }

    if (showRestoreHiddenVideoDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreHiddenVideoDialog = false },
            title = { Text("恢复隐藏视频？") },
            text = {
                Text(
                    "将清除隐藏视频记录，之前从列表移除的视频和隐藏的视频文件夹会重新显示。本地文件不会被修改。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreHiddenVideoDialog = false
                        onRestoreHiddenVideo()
                    }
                ) {
                    Text("恢复")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreHiddenVideoDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SettingsActionItem(
    title: String,
    subtitle: String? = null,
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
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

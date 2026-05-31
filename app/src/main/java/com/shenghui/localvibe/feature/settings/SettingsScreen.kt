package com.shenghui.localvibe.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val SettingsItems = listOf(
    "播放设置",
    "扫描设置",
    "主题设置",
    "关于Moon播放器"
)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onClearProgress: () -> Unit,
    onClearFolders: () -> Unit,
    onClearBooks: () -> Unit,
    onRescanMedia: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextButton(onClick = onBack) {
                Text("返回")
            }

            Text(
                text = "设置",
                style = MaterialTheme.typography.headlineMedium
            )

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
            }
        }
    }
}

@Composable
private fun SettingsActionItem(
    title: String,
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
        Text(
            text = title,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

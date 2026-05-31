package com.shenghui.localvibe.feature.profile

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "我的",
            style = MaterialTheme.typography.headlineMedium
        )

        ProfileItem(
            title = "播放历史",
            subtitle = "查看最近播放内容",
            onClick = {
                Toast.makeText(context, "播放历史功能后续实现", Toast.LENGTH_SHORT).show()
            }
        )
        ProfileItem(
            title = "收藏",
            subtitle = "管理收藏内容",
            onClick = {
                Toast.makeText(context, "收藏功能后续实现", Toast.LENGTH_SHORT).show()
            }
        )
        ProfileItem(
            title = "扫描管理",
            subtitle = "管理本地文件夹扫描",
            onClick = {
                Toast.makeText(context, "扫描管理功能后续实现", Toast.LENGTH_SHORT).show()
            }
        )
        ProfileItem(
            title = "设置",
            subtitle = "播放、扫描和主题设置",
            onClick = onOpenSettings
        )
        ProfileItem(
            title = "关于Moon播放器",
            subtitle = "Moon播放器 · v0.1.0",
            onClick = {}
        )
    }
}

@Composable
private fun ProfileItem(
    title: String,
    subtitle: String,
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
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

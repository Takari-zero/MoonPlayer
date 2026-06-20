package com.shenghui.localvibe.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.shenghui.localvibe.LocalVibeRoute

@Composable
fun MoonBottomNavigationBar(
    selectedRoute: String?,
    onRouteSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF08080C),
        tonalElevation = 0.dp
    ) {
        MoonBottomTabs.forEach { tab ->
            NavigationBarItem(
                selected = selectedRoute == tab.route,
                onClick = { onRouteSelected(tab.route) },
                icon = {
                    if (tab.icon != null) {
                        Icon(tab.icon, contentDescription = tab.label)
                    } else {
                        Text(tab.iconText)
                    }
                },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFFF6F3FF),
                    selectedTextColor = Color(0xFFF6F3FF),
                    indicatorColor = Color(0xFF6F4BEF).copy(alpha = 0.84f),
                    unselectedIconColor = Color(0xFFA8A1B8),
                    unselectedTextColor = Color(0xFFA8A1B8)
                )
            )
        }
    }
}

private data class MoonBottomTab(
    val route: String,
    val label: String,
    val iconText: String,
    val icon: ImageVector? = null
)

private val MoonBottomTabs = listOf(
    MoonBottomTab(LocalVibeRoute.VideoLibrary, "视频", "▶"),
    MoonBottomTab(LocalVibeRoute.AudioLibrary, "音乐", "♪"),
    MoonBottomTab(LocalVibeRoute.BookLibrary, "小说", "", Icons.AutoMirrored.Filled.MenuBook),
    MoonBottomTab(LocalVibeRoute.Profile, "我的", "", Icons.Filled.Person)
)

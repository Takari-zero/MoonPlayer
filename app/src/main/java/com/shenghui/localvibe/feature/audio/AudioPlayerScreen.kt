package com.shenghui.localvibe.feature.audio

import android.media.audiofx.Equalizer
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import com.shenghui.localvibe.core.media.formatDuration
import com.shenghui.localvibe.core.player.AudioPlayMode
import com.shenghui.localvibe.core.player.EqualizerPreset
import com.shenghui.localvibe.core.player.applyPreset
import com.shenghui.localvibe.core.scanner.LocalMediaFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    mediaFile: LocalMediaFile?,
    player: Player?,
    currentPositionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    audioSessionId: Int,
    queue: List<LocalMediaFile>,
    currentIndex: Int,
    playMode: AudioPlayMode,
    onPlayModeChanged: (AudioPlayMode) -> Unit,
    onSelectAudio: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.Black)
                .padding(20.dp)
        ) {
            if (mediaFile == null || player == null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
                }
                Text(
                    text = "未选择音乐文件。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                ServiceAudioPlayer(
                    mediaFile = mediaFile,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    isPlaying = isPlaying,
                    audioSessionId = audioSessionId,
                    queue = queue,
                    currentIndex = currentIndex,
                    playMode = playMode,
                    onPlayModeChanged = onPlayModeChanged,
                    onSelectAudio = onSelectAudio,
                    onPlayPause = onPlayPause,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onSeekTo = onSeekTo,
                    onBack = onBack
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceAudioPlayer(
    mediaFile: LocalMediaFile,
    currentPositionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    audioSessionId: Int,
    queue: List<LocalMediaFile>,
    currentIndex: Int,
    playMode: AudioPlayMode,
    onPlayModeChanged: (AudioPlayMode) -> Unit,
    onSelectAudio: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var displayPositionMs by remember(mediaFile.uri) { mutableLongStateOf(currentPositionMs) }
    var isDraggingProgress by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var showPlayModeMenu by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf(EqualizerPreset.DEFAULT) }
    var equalizerEnabled by remember { mutableStateOf(true) }
    var bandLevels by remember { mutableStateOf(listOf(0f, 0f, 0f, 0f, 0f)) }
    var equalizerSupported by remember { mutableStateOf(true) }

    LaunchedEffect(currentPositionMs, isDraggingProgress) {
        if (!isDraggingProgress) {
            displayPositionMs = currentPositionMs
        }
    }

    DisposableEffect(audioSessionId, selectedPreset, equalizerEnabled, bandLevels) {
        val equalizer = runCatching {
            if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) null else Equalizer(0, audioSessionId)
        }.getOrNull()
        if (equalizer == null) {
            equalizerSupported = false
            onDispose { }
        } else {
            equalizerSupported = true
            runCatching {
                equalizer.enabled = equalizerEnabled
                if (selectedPreset == EqualizerPreset.CUSTOM) {
                    equalizer.applyBandLevels(bandLevels)
                } else {
                    equalizer.applyPreset(selectedPreset)
                }
            }.onFailure {
                equalizerSupported = false
            }
            onDispose { equalizer.release() }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        PlayerTopBar(
            title = mediaFile.displayTitle(),
            onBack = onBack,
            onMore = {
                Toast.makeText(context, "更多功能后续实现", Toast.LENGTH_SHORT).show()
            }
        )

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF202020)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3A3A3A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("♪", style = MaterialTheme.typography.displayMedium, color = Color.White)
                }
            }
            Text(
                text = mediaFile.displayTitle(),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp)
            )
            Text(
                text = "Unknown",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SmallIconAction(
                    label = "均衡器",
                    icon = { Icon(Icons.Filled.Equalizer, contentDescription = null) },
                    onClick = { showEqualizer = true }
                )
                SmallIconAction(
                    label = "A-B",
                    icon = { Text("A-B", color = Color.White.copy(alpha = 0.86f), fontSize = 12.sp) },
                    onClick = { Toast.makeText(context, "A-B 循环功能后续实现", Toast.LENGTH_SHORT).show() }
                )
                SmallIconAction(
                    label = "定时",
                    icon = { Icon(Icons.Filled.Timer, contentDescription = null) },
                    onClick = { Toast.makeText(context, "睡眠定时功能后续实现", Toast.LENGTH_SHORT).show() }
                )
                SmallIconAction(
                    label = "收藏",
                    icon = { Icon(Icons.Filled.FavoriteBorder, contentDescription = null) },
                    onClick = { Toast.makeText(context, "收藏功能后续实现", Toast.LENGTH_SHORT).show() }
                )
                SmallIconAction(
                    label = "更多",
                    icon = { Icon(Icons.Filled.MoreVert, contentDescription = null) },
                    onClick = { Toast.makeText(context, "更多功能后续实现", Toast.LENGTH_SHORT).show() }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AudioProgressSlider(
                    positionMs = displayPositionMs.coerceAtMost(durationMs.coerceAtLeast(0L)),
                    durationMs = durationMs,
                    onValueChange = {
                        isDraggingProgress = true
                        displayPositionMs = it
                    },
                    onValueChangeFinished = { targetPositionMs ->
                        displayPositionMs = targetPositionMs
                        onSeekTo(targetPositionMs)
                        isDraggingProgress = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatDuration(displayPositionMs), color = Color.White.copy(alpha = 0.7f))
                    Text(formatDuration(durationMs), color = Color.White.copy(alpha = 0.7f))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.TopCenter) {
                    SmallIconAction(
                        label = playMode.label,
                        icon = { Icon(playMode.icon(), contentDescription = null) },
                        onClick = { onPlayModeChanged(playMode.next()) },
                        onLongClick = { showPlayModeMenu = true },
                        active = playMode != AudioPlayMode.NORMAL,
                        iconSize = 32.dp
                    )
                    DropdownMenu(
                        expanded = showPlayModeMenu,
                        onDismissRequest = { showPlayModeMenu = false },
                        modifier = Modifier.width(168.dp),
                        containerColor = Color(0xFF141821),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AudioPlayMode.entries.forEach { mode ->
                                val selected = mode == playMode
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (selected) Color(0xFF203B68) else Color.Transparent)
                                        .clickable {
                                            onPlayModeChanged(mode)
                                            showPlayModeMenu = false
                                        }
                                        .padding(horizontal = 10.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = mode.icon(),
                                        contentDescription = null,
                                        tint = if (selected) Color(0xFF8AB6FF) else Color.White.copy(alpha = 0.82f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = mode.fullLabel(),
                                        color = if (selected) Color(0xFF8AB6FF) else Color.White,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
                SmallIconAction(
                    label = "上一曲",
                    icon = { Icon(Icons.Filled.SkipPrevious, contentDescription = null) },
                    onClick = onPrevious,
                    iconSize = 38.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = onPlayPause,
                        modifier = Modifier.size(88.dp),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                    Text(
                        text = if (isPlaying) "暂停" else "播放",
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 12.sp
                    )
                }
                SmallIconAction(
                    label = "下一曲",
                    icon = { Icon(Icons.Filled.SkipNext, contentDescription = null) },
                    onClick = onNext,
                    iconSize = 38.dp
                )
                SmallIconAction(
                    label = "列表",
                    icon = { Icon(Icons.Filled.QueueMusic, contentDescription = null) },
                    onClick = { showQueue = true },
                    iconSize = 32.dp
                )
            }

            TextButton(
                onClick = { Toast.makeText(context, "歌词功能后续实现", Toast.LENGTH_SHORT).show() }
            ) {
                Text("歌词", color = Color.White)
            }
        }
    }

    if (showQueue) {
        QueueDialog(
            queue = queue,
            currentIndex = currentIndex,
            onDismiss = { showQueue = false },
            onSelect = {
                showQueue = false
                onSelectAudio(it)
            }
        )
    }

    if (showEqualizer) {
        EqualizerDialog(
            selectedPreset = selectedPreset,
            enabled = equalizerEnabled,
            bandLevels = bandLevels,
            equalizerSupported = equalizerSupported,
            onSelectPreset = {
                selectedPreset = it
                bandLevels = it.defaultBandLevels()
                if (!equalizerSupported) {
                    Toast.makeText(context, "当前设备不支持均衡器", Toast.LENGTH_SHORT).show()
                }
            },
            onEnabledChange = { equalizerEnabled = it },
            onBandChange = { index, level ->
                selectedPreset = EqualizerPreset.CUSTOM
                bandLevels = bandLevels.toMutableList().also { levels -> levels[index] = level }
            },
            onReset = {
                selectedPreset = EqualizerPreset.DEFAULT
                bandLevels = EqualizerPreset.DEFAULT.defaultBandLevels()
            },
            onDismiss = { showEqualizer = false }
        )
    }
}

@Composable
private fun AudioProgressSlider(
    positionMs: Long,
    durationMs: Long,
    onValueChange: (Long) -> Unit,
    onValueChangeFinished: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val thumbRadiusPx = with(density) { 6.5.dp.toPx() }
    val trackStrokePx = with(density) { 4.5.dp.toPx() }
    val safeDurationMs = durationMs.coerceAtLeast(0L)
    val rangeDurationMs = safeDurationMs.coerceAtLeast(1L)
    var pendingSeekMs by remember { mutableLongStateOf(positionMs.coerceIn(0L, safeDurationMs)) }

    LaunchedEffect(positionMs, safeDurationMs) {
        pendingSeekMs = positionMs.coerceIn(0L, safeDurationMs)
    }

    fun positionFromX(x: Float, width: Float): Long {
        if (safeDurationMs <= 0L) return 0L
        val startX = thumbRadiusPx
        val endX = (width - thumbRadiusPx).coerceAtLeast(startX)
        val usableWidth = (endX - startX).coerceAtLeast(1f)
        val fraction = ((x.coerceIn(startX, endX) - startX) / usableWidth).coerceIn(0f, 1f)
        return (fraction * safeDurationMs).toLong().coerceIn(0L, safeDurationMs)
    }

    Canvas(
        modifier = modifier
            .height(32.dp)
            .pointerInput(safeDurationMs, thumbRadiusPx) {
                detectTapGestures { offset ->
                    val target = positionFromX(offset.x, size.width.toFloat())
                    pendingSeekMs = target
                    onValueChange(target)
                    onValueChangeFinished(target)
                }
            }
            .pointerInput(safeDurationMs, thumbRadiusPx) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val target = positionFromX(offset.x, size.width.toFloat())
                        pendingSeekMs = target
                        onValueChange(target)
                    },
                    onDragEnd = {
                        onValueChangeFinished(pendingSeekMs)
                    },
                    onDragCancel = {
                        onValueChangeFinished(pendingSeekMs)
                    }
                ) { change, _ ->
                    val target = positionFromX(change.position.x, size.width.toFloat())
                    pendingSeekMs = target
                    onValueChange(target)
                    change.consume()
                }
            }
    ) {
        val centerY = size.height / 2f
        val startX = thumbRadiusPx
        val endX = (size.width - thumbRadiusPx).coerceAtLeast(startX)
        val fraction = (positionMs.toFloat() / rangeDurationMs.toFloat()).coerceIn(0f, 1f)
        val thumbCenterX = startX + (endX - startX) * fraction

        drawLine(
            color = Color(0xFF2E3445),
            start = Offset(startX, centerY),
            end = Offset(endX, centerY),
            strokeWidth = trackStrokePx,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF4D8DFF),
            start = Offset(startX, centerY),
            end = Offset(thumbCenterX, centerY),
            strokeWidth = trackStrokePx,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = Color(0xFF8AB6FF),
            radius = thumbRadiusPx,
            center = Offset(thumbCenterX, centerY)
        )
    }
}

@Composable
private fun PlayerTopBar(
    title: String,
    onBack: () -> Unit,
    onMore: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Unknown",
                color = Color.White.copy(alpha = 0.62f),
                style = MaterialTheme.typography.bodySmall
            )
        }
        IconButton(
            onClick = onMore,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = Color.White)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SmallIconAction(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    active: Boolean = false,
    iconSize: androidx.compose.ui.unit.Dp = 28.dp
) {
    val tint = if (active) Color(0xFF8AB6FF) else Color.White.copy(alpha = 0.86f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(iconSize), contentAlignment = Alignment.Center) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides tint
                ) {
                    icon()
                }
            }
        }
        Text(text = label, color = tint, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun QueueDialog(
    queue: List<LocalMediaFile>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("播放列表") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                queue.take(30).forEachIndexed { index, file ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(index) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (index == currentIndex) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            }
                        )
                    ) {
                        Text(
                            text = file.displayTitle(),
                            modifier = Modifier.padding(12.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EqualizerDialog(
    selectedPreset: EqualizerPreset,
    enabled: Boolean,
    bandLevels: List<Float>,
    equalizerSupported: Boolean,
    onSelectPreset: (EqualizerPreset) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onBandChange: (Int, Float) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF17141C)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.42f)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("均衡器", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (enabled) "开" else "关", color = Color.White.copy(alpha = 0.76f))
                    Switch(
                        checked = enabled,
                        onCheckedChange = onEnabledChange,
                        modifier = Modifier.scale(0.62f),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF8AB6FF),
                            checkedTrackColor = Color(0xFF254A7A),
                            uncheckedThumbColor = Color(0xFF6C7485),
                            uncheckedTrackColor = Color(0xFF2E3445),
                            uncheckedBorderColor = Color(0xFF2E3445)
                        )
                    )
                }
            }
            if (!equalizerSupported) {
                Text(
                    text = "当前设备暂不支持均衡器，仍可保留预设选择。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EqualizerPreset.entries
                    .filterNot { it == EqualizerPreset.CUSTOM }
                    .forEach { preset ->
                        AssistChip(
                            onClick = { onSelectPreset(preset) },
                            label = { Text(preset.label) },
                            enabled = preset != selectedPreset
                        )
                    }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EqualizerBands.forEachIndexed { index, label ->
                    VerticalBandControl(
                        label = label,
                        level = bandLevels.getOrElse(index) { 0f },
                        enabled = enabled,
                        onLevelChange = { onBandChange(index, it) }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onReset) {
                    Text("重置", color = Color(0xFF8AB6FF))
                }
                TextButton(onClick = onDismiss) {
                    Text("完成", color = Color(0xFF8AB6FF))
                }
            }
        }
    }
}

private fun LocalMediaFile.displayTitle(): String {
    return name.substringBeforeLast('.', name)
}

private fun AudioPlayMode.icon(): androidx.compose.ui.graphics.vector.ImageVector {
    return when (this) {
        AudioPlayMode.NORMAL -> Icons.Filled.PlayArrow
        AudioPlayMode.SHUFFLE -> Icons.Filled.Shuffle
        AudioPlayMode.REPEAT_ONE -> Icons.Filled.RepeatOne
        AudioPlayMode.REPEAT_ALL -> Icons.Filled.Repeat
    }
}

private fun AudioPlayMode.fullLabel(): String {
    return when (this) {
        AudioPlayMode.NORMAL -> "顺序播放"
        AudioPlayMode.SHUFFLE -> "随机播放"
        AudioPlayMode.REPEAT_ONE -> "单曲循环"
        AudioPlayMode.REPEAT_ALL -> "列表循环"
    }
}

private val EqualizerBands = listOf("60Hz", "230Hz", "910Hz", "3600Hz", "14000Hz")

private fun EqualizerPreset.defaultBandLevels(): List<Float> {
    return when (this) {
        EqualizerPreset.DEFAULT -> listOf(0f, 0f, 0f, 0f, 0f)
        EqualizerPreset.CLASSICAL -> listOf(-2f, -1f, 0f, 2f, 3f)
        EqualizerPreset.DANCE -> listOf(5f, 3f, 0f, 2f, 4f)
        EqualizerPreset.VOCAL -> listOf(-1f, 1f, 5f, 3f, 0f)
        EqualizerPreset.ROCK -> listOf(4f, 2f, 0f, 3f, 5f)
        EqualizerPreset.METAL -> listOf(5f, 2f, -2f, 4f, 6f)
        EqualizerPreset.CUSTOM -> listOf(0f, 0f, 0f, 0f, 0f)
    }
}

private fun Equalizer.applyBandLevels(levels: List<Float>) {
    val range = bandLevelRange
    val minLevel = range[0].toInt()
    val maxLevel = range[1].toInt()
    val bandCount = numberOfBands.toInt().coerceAtLeast(1)
    repeat(bandCount) { band ->
        val sourceIndex = ((band / bandCount.toFloat()) * levels.size).toInt().coerceIn(0, levels.lastIndex)
        val millibels = (levels[sourceIndex] * 100).toInt().coerceIn(minLevel, maxLevel)
        setBandLevel(band.toShort(), millibels.toShort())
    }
}

@Composable
private fun VerticalBandControl(
    label: String,
    level: Float,
    enabled: Boolean,
    onLevelChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.width(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val fraction = ((level + 12f) / 24f).coerceIn(0f, 1f)
        val controlAlpha = if (enabled) 1f else 0.38f
        fun androidx.compose.ui.geometry.Offset.toBandLevel(height: Int): Float {
            val nextFraction = (1f - y / height.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
            return (-12f + nextFraction * 24f).coerceIn(-12f, 12f)
        }

        Text(
            text = "${if (level > 0f) "+" else ""}${level.toInt()} dB",
            color = Color.White.copy(alpha = 0.76f),
            fontSize = 11.sp,
            maxLines = 1
        )
        BoxWithConstraints(
            modifier = Modifier
                .height(170.dp)
                .width(38.dp)
                .pointerInput(enabled) {
                    detectTapGestures { offset ->
                        if (enabled) onLevelChange(offset.toBandLevel(size.height))
                    }
                }
                .pointerInput(enabled) {
                    detectDragGestures { change, _ ->
                        if (enabled) {
                            onLevelChange(change.position.toBandLevel(size.height))
                            change.consume()
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .height(maxHeight)
                    .width(2.dp)
                    .background(Color(0xFF2E3445).copy(alpha = controlAlpha), RoundedCornerShape(99.dp))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .height(maxHeight * fraction)
                    .width(2.dp)
                    .background(Color(0xFF4D8DFF).copy(alpha = controlAlpha), RoundedCornerShape(99.dp))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (maxHeight - 8.dp) * (1f - fraction))
                    .size(8.dp)
                    .background(Color(0xFF8AB6FF).copy(alpha = controlAlpha), CircleShape)
            )
        }
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

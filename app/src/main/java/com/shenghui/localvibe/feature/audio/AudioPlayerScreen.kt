package com.shenghui.localvibe.feature.audio

import android.media.audiofx.Equalizer
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.delay
import kotlin.math.abs

private val AudioPlayerBackground = Color(0xFF090910)
private val AudioPanelColor = Color(0xFF161421)
private val AudioPurple = Color(0xFF8B5CFF)
private val AudioPurpleLight = Color(0xFFD6C0FF)
private val AudioTextPrimary = Color(0xFFF7F0FF)
private val AudioTextSecondary = Color(0xFF9F95B4)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    mediaFile: LocalMediaFile?,
    player: Player?,
    currentPositionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    isFavorite: Boolean,
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
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    onRemoveCurrent: (LocalMediaFile) -> Unit = {},
    onDeleteCurrent: (LocalMediaFile) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isClosing by remember { mutableStateOf(false) }
    var hasDispatchedBack by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent),
        containerColor = Color.Transparent
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            val closeOffsetY by animateDpAsState(
                targetValue = if (isClosing) maxHeight + 80.dp else 0.dp,
                animationSpec = tween(durationMillis = 260),
                label = "audioPlayerCloseOffset"
            )
            val requestClose = {
                if (!isClosing) {
                    isClosing = true
                }
            }
            val edgeGestureWidthPx = with(LocalDensity.current) { 32.dp.toPx() }
            val edgeDismissThresholdPx = with(LocalDensity.current) { 64.dp.toPx() }

            LaunchedEffect(isClosing) {
                if (isClosing && !hasDispatchedBack) {
                    delay(260)
                    hasDispatchedBack = true
                    onBack()
                }
            }

            BackHandler(enabled = !isClosing) {
                requestClose()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = closeOffsetY)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF221C2D),
                                AudioPlayerBackground,
                                Color(0xFF08080F)
                            )
                        )
                    )
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                if (mediaFile == null || player == null) {
                    IconButton(
                        onClick = requestClose,
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "返回", tint = Color.White)
                    }
                    Text(
                        text = "未选择音乐文件。",
                        style = MaterialTheme.typography.bodyLarge,
                        color = AudioTextSecondary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    ServiceAudioPlayer(
                        mediaFile = mediaFile,
                        player = player,
                        currentPositionMs = currentPositionMs,
                        durationMs = durationMs,
                        isPlaying = isPlaying,
                        isFavorite = isFavorite,
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
                        onToggleFavorite = onToggleFavorite,
                        onBack = requestClose,
                        onRemoveCurrent = onRemoveCurrent,
                        onDeleteCurrent = onDeleteCurrent
                    )
                }
                EdgeDismissArea(
                    direction = 1,
                    widthPx = edgeGestureWidthPx,
                    thresholdPx = edgeDismissThresholdPx,
                    enabled = !isClosing,
                    onDismiss = requestClose,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                EdgeDismissArea(
                    direction = -1,
                    widthPx = edgeGestureWidthPx,
                    thresholdPx = edgeDismissThresholdPx,
                    enabled = !isClosing,
                    onDismiss = requestClose,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }
}

@Composable
private fun EdgeDismissArea(
    direction: Int,
    widthPx: Float,
    thresholdPx: Float,
    enabled: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val widthDp = with(LocalDensity.current) { widthPx.toDp() }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(widthDp)
            .pointerInput(direction, thresholdPx, enabled) {
                var dragX = 0f
                var dragY = 0f
                var triggered = false
                detectDragGestures(
                    onDragStart = {
                        dragX = 0f
                        dragY = 0f
                        triggered = false
                    },
                    onDragEnd = {
                        dragX = 0f
                        dragY = 0f
                        triggered = false
                    },
                    onDragCancel = {
                        dragX = 0f
                        dragY = 0f
                        triggered = false
                    }
                ) { change, dragAmount ->
                    if (!enabled || triggered) return@detectDragGestures
                    dragX += dragAmount.x * direction
                    dragY += dragAmount.y
                    if (dragX > 0f) {
                        change.consume()
                    }
                    if (dragX >= thresholdPx && dragX > abs(dragY) * 1.15f) {
                        triggered = true
                        change.consume()
                        onDismiss()
                    }
                }
            }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceAudioPlayer(
    mediaFile: LocalMediaFile,
    player: Player,
    currentPositionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    isFavorite: Boolean,
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
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    onRemoveCurrent: (LocalMediaFile) -> Unit,
    onDeleteCurrent: (LocalMediaFile) -> Unit
) {
    val context = LocalContext.current
    var displayPositionMs by remember(mediaFile.uri) { mutableLongStateOf(currentPositionMs) }
    var isDraggingProgress by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showPlayModeMenu by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var sleepTimerEndAtMs by rememberSaveable { mutableLongStateOf(0L) }
    var sleepTimerMinutes by rememberSaveable { mutableStateOf<Int?>(null) }
    var sleepTimerMode by rememberSaveable { mutableStateOf(AudioSleepTimerMode.Off) }
    var sleepTimerNowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var abPointAms by rememberSaveable { mutableLongStateOf(-1L) }
    var abPointBms by rememberSaveable { mutableLongStateOf(-1L) }
    var lastAbSeekAtMs by remember { mutableLongStateOf(0L) }
    var draftSleepHours by rememberSaveable { mutableStateOf(0) }
    var draftSleepMinutes by rememberSaveable { mutableStateOf(30) }
    var draftStopAfterCurrent by rememberSaveable { mutableStateOf(false) }
    var draftFinishTrackAfterTimer by rememberSaveable { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf(EqualizerPreset.DEFAULT) }
    var equalizerEnabled by remember { mutableStateOf(true) }
    var bandLevels by remember { mutableStateOf(listOf(0f, 0f, 0f, 0f, 0f)) }
    var equalizerSupported by remember { mutableStateOf(true) }
    val sleepRemainingMs = (sleepTimerEndAtMs - sleepTimerNowMs).coerceAtLeast(0L)
    val sleepTimerActive = sleepTimerMode != AudioSleepTimerMode.Off
    val abHasA = abPointAms >= 0L
    val abLoopActive = abPointAms >= 0L && abPointBms > abPointAms
    val clearAbRepeat = {
        abPointAms = -1L
        abPointBms = -1L
        lastAbSeekAtMs = 0L
    }
    val isTrackEndSleepMode = { mode: AudioSleepTimerMode ->
        mode == AudioSleepTimerMode.StopAfterCurrentTrack ||
            mode == AudioSleepTimerMode.CountdownThenFinishTrack ||
            mode == AudioSleepTimerMode.PendingPauseAfterCurrentTrack
    }
    val clearTrackEndSleepState = {
        if (isTrackEndSleepMode(sleepTimerMode)) {
            sleepTimerEndAtMs = 0L
            sleepTimerMinutes = null
            sleepTimerMode = AudioSleepTimerMode.Off
            sleepTimerNowMs = System.currentTimeMillis()
            draftStopAfterCurrent = false
            draftFinishTrackAfterTimer = false
        }
    }
    val clearManualTrackScopedState = {
        clearAbRepeat()
        clearTrackEndSleepState()
    }
    val handleAbRepeatClick = {
        val currentMs = player.currentPosition.coerceAtLeast(0L)
        when {
            !abHasA -> {
                clearTrackEndSleepState()
                abPointAms = currentMs
                abPointBms = -1L
                Toast.makeText(context, "已设置 A 点", Toast.LENGTH_SHORT).show()
            }
            !abLoopActive -> {
                when {
                    currentMs <= abPointAms -> {
                        abPointBms = -1L
                        Toast.makeText(context, "B 点必须晚于 A 点", Toast.LENGTH_SHORT).show()
                    }
                    currentMs - abPointAms < MinAbRepeatDurationMs -> {
                        abPointBms = -1L
                        Toast.makeText(context, "A-B 间隔太短", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        clearTrackEndSleepState()
                        abPointBms = currentMs
                        Toast.makeText(context, "已开启 A-B 循环", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            else -> {
                clearAbRepeat()
                Toast.makeText(context, "已关闭 A-B 循环", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val openSleepTimerSheet = {
        val minutes = when {
            sleepTimerEndAtMs > System.currentTimeMillis() -> {
                ((sleepTimerEndAtMs - System.currentTimeMillis() + 59_999L) / 60_000L)
                    .toInt()
                    .coerceIn(1, MaxSleepTimerHours * 60 + 59)
            }
            sleepTimerMinutes != null -> sleepTimerMinutes!!.coerceIn(1, MaxSleepTimerHours * 60 + 59)
            else -> DefaultSleepTimerMinutes
        }
        draftSleepHours = (minutes / 60).coerceIn(0, MaxSleepTimerHours)
        draftSleepMinutes = (minutes % 60).coerceIn(0, 59)
        draftStopAfterCurrent = sleepTimerMode == AudioSleepTimerMode.StopAfterCurrentTrack
        draftFinishTrackAfterTimer =
            sleepTimerMode == AudioSleepTimerMode.CountdownThenFinishTrack ||
                sleepTimerMode == AudioSleepTimerMode.PendingPauseAfterCurrentTrack
        showSleepTimer = true
    }
    val clearSleepTimer = {
        sleepTimerEndAtMs = 0L
        sleepTimerMinutes = null
        sleepTimerMode = AudioSleepTimerMode.Off
        sleepTimerNowMs = System.currentTimeMillis()
        draftStopAfterCurrent = false
        draftFinishTrackAfterTimer = false
    }

    LaunchedEffect(currentPositionMs, isDraggingProgress) {
        if (!isDraggingProgress) {
            displayPositionMs = currentPositionMs
        }
    }

    LaunchedEffect(mediaFile.uri) {
        clearManualTrackScopedState()
    }

    LaunchedEffect(abPointAms, abPointBms, isPlaying, mediaFile.uri, player) {
        if (!abLoopActive || !isPlaying) return@LaunchedEffect
        while (true) {
            val now = System.currentTimeMillis()
            val positionMs = player.currentPosition.coerceAtLeast(0L)
            if (positionMs >= abPointBms - AbRepeatSeekThresholdMs &&
                now - lastAbSeekAtMs >= AbRepeatSeekDebounceMs
            ) {
                lastAbSeekAtMs = now
                player.seekTo(abPointAms)
                if (isPlaying) {
                    player.play()
                }
            }
            delay(250L)
        }
    }

    LaunchedEffect(sleepTimerEndAtMs, sleepTimerMode, player) {
        if (
            sleepTimerEndAtMs <= 0L ||
            sleepTimerMode == AudioSleepTimerMode.Off ||
            sleepTimerMode == AudioSleepTimerMode.StopAfterCurrentTrack ||
            sleepTimerMode == AudioSleepTimerMode.PendingPauseAfterCurrentTrack
        ) {
            return@LaunchedEffect
        }
        while (true) {
            val now = System.currentTimeMillis()
            sleepTimerNowMs = now
            val remainingMs = sleepTimerEndAtMs - now
            if (remainingMs <= 0L) {
                if (sleepTimerMode == AudioSleepTimerMode.CountdownThenFinishTrack) {
                    sleepTimerMode = AudioSleepTimerMode.PendingPauseAfterCurrentTrack
                    sleepTimerEndAtMs = 0L
                    Toast.makeText(context, "定时结束，将播完当前歌曲后暂停", Toast.LENGTH_SHORT).show()
                } else {
                    player.pause()
                    sleepTimerEndAtMs = 0L
                    sleepTimerMinutes = null
                    sleepTimerMode = AudioSleepTimerMode.Off
                    Toast.makeText(context, "定时结束，已暂停播放", Toast.LENGTH_SHORT).show()
                }
                break
            }
            delay(remainingMs.coerceAtMost(1000L))
        }
    }

    LaunchedEffect(currentPositionMs, durationMs, sleepTimerMode, player) {
        if (
            sleepTimerMode == AudioSleepTimerMode.StopAfterCurrentTrack ||
            sleepTimerMode == AudioSleepTimerMode.PendingPauseAfterCurrentTrack
        ) {
            if (durationMs <= 0L) return@LaunchedEffect
            val remainingToTrackEndMs = durationMs - currentPositionMs
            if (remainingToTrackEndMs in 0L..1200L) {
                player.pause()
                sleepTimerEndAtMs = 0L
                sleepTimerMinutes = null
                sleepTimerMode = AudioSleepTimerMode.Off
                Toast.makeText(context, "已播完当前歌曲，暂停播放", Toast.LENGTH_SHORT).show()
            }
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
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        PlayerTopBar(
            title = mediaFile.displayTitle(),
            onBack = onBack,
            onSongInfo = { Toast.makeText(context, "歌曲信息后续实现", Toast.LENGTH_SHORT).show() },
            onShowQueue = { showQueue = true },
            onShowEqualizer = { showEqualizer = true },
            onShare = { Toast.makeText(context, "分享功能后续实现", Toast.LENGTH_SHORT).show() },
            onRemove = { onRemoveCurrent(mediaFile) },
            onDelete = { showDeleteConfirm = true },
            onMore = { Toast.makeText(context, "更多功能后续实现", Toast.LENGTH_SHORT).show() }
        )

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AudioMoonDisc(
                isPlaying = isPlaying,
                modifier = Modifier
                    .size(292.dp)
                    .sizeIn(maxWidth = 318.dp, maxHeight = 318.dp)
            )
            Text(
                text = mediaFile.displayTitle(),
                style = MaterialTheme.typography.headlineMedium,
                color = AudioTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 28.dp)
                    .fillMaxWidth()
            )
            Text(
                text = mediaFile.audioSubtitle(),
                style = MaterialTheme.typography.bodyMedium,
                color = AudioTextSecondary,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
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
                    Text(formatDuration(displayPositionMs), color = AudioTextSecondary, fontSize = 12.sp)
                    Text(formatDuration(durationMs), color = AudioTextSecondary, fontSize = 12.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.TopCenter) {
                    SmallIconAction(
                        label = playMode.shortLabel(),
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
                        containerColor = AudioPanelColor,
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
                                        .background(if (selected) AudioPurple.copy(alpha = 0.18f) else Color.Transparent)
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
                                        tint = if (selected) AudioPurpleLight else AudioTextPrimary.copy(alpha = 0.82f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = mode.fullLabel(),
                                        color = if (selected) AudioPurpleLight else AudioTextPrimary,
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
                    onClick = {
                        clearManualTrackScopedState()
                        onPrevious()
                    },
                    iconSize = 38.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(AudioPurple)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                    Text(
                        text = if (isPlaying) "暂停" else "播放",
                        color = AudioTextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 7.dp)
                    )
                }
                SmallIconAction(
                    label = "下一曲",
                    icon = { Icon(Icons.Filled.SkipNext, contentDescription = null) },
                    onClick = {
                        clearManualTrackScopedState()
                        onNext()
                    },
                    iconSize = 38.dp
                )
                SmallIconAction(
                    label = "列表",
                    icon = { Icon(Icons.Filled.QueueMusic, contentDescription = null) },
                    onClick = { showQueue = true },
                    iconSize = 32.dp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SmallIconAction(
                    label = "均衡器",
                    icon = { Icon(Icons.Filled.Equalizer, contentDescription = null) },
                    onClick = { showEqualizer = true }
                )
                SmallIconAction(
                    label = abRepeatLabel(abPointAms, abPointBms),
                    icon = {
                        Text(
                            text = if (abHasA && !abLoopActive) "A-" else "A-B",
                            color = if (abHasA) AudioPurpleLight else AudioTextPrimary.copy(alpha = 0.86f),
                            fontSize = 12.sp
                        )
                    },
                    onClick = handleAbRepeatClick,
                    active = abHasA
                )
                SmallIconAction(
                    label = sleepTimerButtonLabel(sleepTimerMode, sleepRemainingMs),
                    icon = { Icon(Icons.Filled.Timer, contentDescription = null) },
                    onClick = openSleepTimerSheet,
                    active = sleepTimerActive
                )
                SmallIconAction(
                    label = "收藏",
                    icon = {
                        Icon(
                            if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = null
                        )
                    },
                    onClick = onToggleFavorite,
                    active = isFavorite,
                    activeTint = Color(0xFFFF9AE6)
                )
                SmallIconAction(
                    label = "歌词",
                    icon = { Icon(Icons.Filled.MoreVert, contentDescription = null) },
                    onClick = { Toast.makeText(context, "歌词后续实现", Toast.LENGTH_SHORT).show() }
                )
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
                clearManualTrackScopedState()
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

    if (showSleepTimer) {
        SleepTimerSheet(
            hours = draftSleepHours,
            minutes = draftSleepMinutes,
            stopAfterCurrent = draftStopAfterCurrent,
            finishTrackAfterTimer = draftFinishTrackAfterTimer,
            mode = sleepTimerMode,
            remainingMs = sleepRemainingMs,
            onHoursChange = { draftSleepHours = it.coerceIn(0, MaxSleepTimerHours) },
            onMinutesChange = { draftSleepMinutes = it.coerceIn(0, 59) },
            onStopAfterCurrentChange = { enabled ->
                draftStopAfterCurrent = enabled
                if (enabled) draftFinishTrackAfterTimer = false
            },
            onFinishTrackAfterTimerChange = { enabled ->
                draftFinishTrackAfterTimer = enabled
                if (enabled) draftStopAfterCurrent = false
            },
            onConfirm = {
                if (draftStopAfterCurrent) {
                    if (abHasA) {
                        clearAbRepeat()
                        Toast.makeText(context, "已关闭 A-B，避免影响定时暂停", Toast.LENGTH_SHORT).show()
                    }
                    sleepTimerEndAtMs = 0L
                    sleepTimerMinutes = null
                    sleepTimerMode = AudioSleepTimerMode.StopAfterCurrentTrack
                    sleepTimerNowMs = System.currentTimeMillis()
                    showSleepTimer = false
                    Toast.makeText(context, "已开启：播完当前歌曲后暂停", Toast.LENGTH_SHORT).show()
                    return@SleepTimerSheet
                }
                val totalMinutes = draftSleepHours * 60 + draftSleepMinutes
                if (totalMinutes <= 0) {
                    Toast.makeText(context, "请选择有效时间", Toast.LENGTH_SHORT).show()
                    return@SleepTimerSheet
                }
                val now = System.currentTimeMillis()
                sleepTimerMinutes = totalMinutes
                sleepTimerEndAtMs = now + totalMinutes * 60_000L
                sleepTimerNowMs = now
                if (draftFinishTrackAfterTimer && abHasA) {
                    clearAbRepeat()
                    Toast.makeText(context, "已关闭 A-B，避免影响定时暂停", Toast.LENGTH_SHORT).show()
                }
                sleepTimerMode = if (draftFinishTrackAfterTimer) {
                    AudioSleepTimerMode.CountdownThenFinishTrack
                } else {
                    AudioSleepTimerMode.Countdown
                }
                showSleepTimer = false
                Toast.makeText(context, "已开启睡眠定时", Toast.LENGTH_SHORT).show()
            },
            onCancelTimer = {
                clearSleepTimer()
                showSleepTimer = false
                Toast.makeText(context, "已关闭睡眠定时", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showSleepTimer = false }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("永久删除音乐？") },
            text = { Text("此操作会尝试删除本地音乐文件，无法从 Moon播放器 恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteCurrent(mediaFile)
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
private fun AudioMoonDisc(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "audioMoonDisc")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "audioMoonDiscRotation"
    )
    val appliedRotation = if (isPlaying) rotation else 0f

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.46f
        val labelRadius = radius * 0.25f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF302640).copy(alpha = 0.52f),
                    Color(0xFF14121B).copy(alpha = 0.94f),
                    Color(0xFF07080C)
                ),
                center = center,
                radius = radius * 1.15f
            ),
            radius = radius * 1.06f,
            center = center
        )

        rotate(appliedRotation, center) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF24202C),
                        Color(0xFF101114),
                        Color(0xFF050607)
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            repeat(34) { index ->
                val ringRadius = radius * (0.18f + index * 0.022f)
                drawCircle(
                    color = Color.White.copy(alpha = if (index % 3 == 0) 0.035f else 0.018f),
                    radius = ringRadius.coerceAtMost(radius * 0.94f),
                    center = center,
                    style = Stroke(width = 0.8f)
                )
            }
            repeat(10) { index ->
                rotate(index * 36f, center) {
                    drawArc(
                        color = AudioPurpleLight.copy(alpha = 0.08f),
                        startAngle = 210f,
                        sweepAngle = 34f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius * 0.86f, center.y - radius * 0.86f),
                        size = androidx.compose.ui.geometry.Size(radius * 1.72f, radius * 1.72f),
                        style = Stroke(width = 1.2f, cap = StrokeCap.Round)
                    )
                }
            }
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF2A2334), Color(0xFF08080E)),
                center = center,
                radius = labelRadius * 1.45f
            ),
            radius = labelRadius,
            center = center
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.42f),
            radius = labelRadius * 0.44f,
            center = Offset(center.x + labelRadius * 0.45f, center.y + labelRadius * 0.16f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.92f), AudioPurpleLight.copy(alpha = 0.76f)),
                center = Offset(center.x - labelRadius * 0.12f, center.y - labelRadius * 0.12f),
                radius = labelRadius * 0.72f
            ),
            radius = labelRadius * 0.48f,
            center = Offset(center.x - labelRadius * 0.08f, center.y - labelRadius * 0.08f)
        )
        drawCircle(
            color = Color(0xFF08080E),
            radius = labelRadius * 0.46f,
            center = Offset(center.x + labelRadius * 0.24f, center.y - labelRadius * 0.05f)
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
            color = Color(0xFF3F374D).copy(alpha = 0.72f),
            start = Offset(startX, centerY),
            end = Offset(endX, centerY),
            strokeWidth = trackStrokePx,
            cap = StrokeCap.Round
        )
        drawLine(
            color = AudioPurple,
            start = Offset(startX, centerY),
            end = Offset(thumbCenterX, centerY),
            strokeWidth = trackStrokePx,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = AudioPurpleLight,
            radius = thumbRadiusPx,
            center = Offset(thumbCenterX, centerY)
        )
    }
}

@Composable
private fun PlayerTopBar(
    title: String,
    onBack: () -> Unit,
    onSongInfo: () -> Unit,
    onShowQueue: () -> Unit,
    onShowEqualizer: () -> Unit,
    onShare: () -> Unit,
    onRemove: () -> Unit,
    onDelete: () -> Unit,
    onMore: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        @Suppress("UNUSED_VARIABLE")
        val ignoredTitle = title
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "返回", tint = AudioTextPrimary)
        }
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = AudioTextPrimary)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = AudioPanelColor,
                shape = RoundedCornerShape(18.dp)
            ) {
                AudioTopMenuItem("歌曲信息") {
                    expanded = false
                    onSongInfo()
                }
                AudioTopMenuItem("播放列表") {
                    expanded = false
                    onShowQueue()
                }
                AudioTopMenuItem("均衡器") {
                    expanded = false
                    onShowEqualizer()
                }
                AudioTopMenuItem("分享") {
                    expanded = false
                    onShare()
                }
                AudioTopMenuItem("隐藏") {
                    expanded = false
                    onRemove()
                }
                AudioTopMenuItem("永久删除文件", danger = true) {
                    expanded = false
                    onDelete()
                }
                AudioTopMenuItem("更多功能") {
                    expanded = false
                    onMore()
                }
            }
        }
    }
}
@Composable
private fun AudioTopMenuItem(
    text: String,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                color = if (danger) Color(0xFFF97066) else AudioTextPrimary.copy(alpha = 0.92f)
            )
        },
        onClick = onClick
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SmallIconAction(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    active: Boolean = false,
    activeTint: Color = AudioPurpleLight,
    iconSize: androidx.compose.ui.unit.Dp = 28.dp
) {
    val tint = if (active) activeTint else AudioTextPrimary.copy(alpha = 0.86f)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueDialog(
    queue: List<LocalMediaFile>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val targetIndex = currentIndex.takeIf { it in queue.indices }
    LaunchedEffect(queue.size, targetIndex) {
        if (targetIndex != null) {
            listState.scrollToItem((targetIndex - 3).coerceAtLeast(0))
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF161321),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(99.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.76f)
                .navigationBarsPadding()
                .padding(start = 22.dp, end = 22.dp, bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.align(Alignment.CenterStart)) {
                    Text(
                        text = "播放队列",
                        color = AudioTextPrimary,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "当前队列 · ${queue.size} 首",
                        color = AudioTextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "关闭",
                        tint = AudioTextPrimary.copy(alpha = 0.82f)
                    )
                }
            }
            Text(
                text = "队列",
                color = AudioTextSecondary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无播放队列",
                        color = AudioTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(
                        items = queue,
                        key = { _, file -> file.normalizedQueueKey() }
                    ) { index, file ->
                        QueueSheetItem(
                            file = file,
                            selected = index == currentIndex,
                            onClick = { onSelect(index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueSheetItem(
    file: LocalMediaFile,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) AudioPurple.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(42.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(if (selected) AudioPurpleLight else Color.Transparent)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 12.dp)
        ) {
            Text(
                text = file.displayTitle(),
                color = if (selected) AudioPurpleLight else AudioTextPrimary.copy(alpha = 0.88f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = file.queueSubtitle(),
                color = AudioTextSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Text(
            text = file.queueDurationText(),
            color = AudioTextSecondary,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerSheet(
    hours: Int,
    minutes: Int,
    stopAfterCurrent: Boolean,
    finishTrackAfterTimer: Boolean,
    mode: AudioSleepTimerMode,
    remainingMs: Long,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    onStopAfterCurrentChange: (Boolean) -> Unit,
    onFinishTrackAfterTimerChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF181421),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(99.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.62f)
                .navigationBarsPadding()
                .padding(start = 22.dp, end = 22.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "设定睡眠定时",
                    color = AudioTextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "×",
                    color = AudioTextPrimary.copy(alpha = 0.82f),
                    fontSize = 28.sp,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (mode != AudioSleepTimerMode.Off) {
                    Text(
                        text = sleepTimerStatusText(mode, remainingMs),
                        color = AudioPurpleLight.copy(alpha = 0.86f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 20.dp),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(118.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.025f)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimeWheelColumn(
                            value = hours,
                            range = 0..MaxSleepTimerHours,
                            enabled = !stopAfterCurrent,
                            loop = true,
                            onValueChange = onHoursChange
                        )
                        Text(
                            text = ":",
                            color = AudioPurpleLight.copy(alpha = if (stopAfterCurrent) 0.36f else 0.82f),
                            fontSize = 26.sp,
                            modifier = Modifier.padding(horizontal = 18.dp)
                        )
                        TimeWheelColumn(
                            value = minutes,
                            range = 0..59,
                            enabled = !stopAfterCurrent,
                            loop = true,
                            onValueChange = onMinutesChange
                        )
                    }
                }

                if (stopAfterCurrent) {
                    Text(
                        text = "将忽略倒计时，在当前歌曲结束后暂停。",
                        color = AudioTextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                SleepTimerSwitchRow(
                    text = "播完当前歌曲后暂停",
                    checked = stopAfterCurrent,
                    onCheckedChange = onStopAfterCurrentChange
                )
                SleepTimerSwitchRow(
                    text = "定时结束后播放完整歌曲再暂停",
                    checked = finishTrackAfterTimer,
                    onCheckedChange = onFinishTrackAfterTimerChange
                )

                if (mode != AudioSleepTimerMode.Off) {
                    TextButton(
                        onClick = onCancelTimer,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("关闭定时", color = Color(0xFFFFB4C1))
                    }
                }
            }

            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AudioPurple.copy(alpha = 0.34f),
                    contentColor = AudioTextPrimary
                )
            ) {
                Text("确认定时", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun TimeWheelColumn(
    value: Int,
    range: IntRange,
    enabled: Boolean,
    loop: Boolean,
    onValueChange: (Int) -> Unit
) {
    val stepPx = with(LocalDensity.current) { 18.dp.toPx() }
    val latestValue by rememberUpdatedState(value)
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val alpha = if (enabled) 1f else 0.38f
    val previous = value.stepIn(range, -1, loop)
    val next = value.stepIn(range, 1, loop)
    Column(
        modifier = Modifier
            .width(86.dp)
            .graphicsLayer { this.alpha = alpha }
            .pointerInput(enabled, stepPx, loop, range.first, range.last) {
                var accumulatedDragPx = 0f
                var gestureValue = latestValue
                detectVerticalDragGestures(
                    onDragStart = {
                        accumulatedDragPx = 0f
                        gestureValue = latestValue
                    },
                    onDragEnd = { accumulatedDragPx = 0f },
                    onDragCancel = { accumulatedDragPx = 0f }
                ) { change, dragAmount ->
                    if (enabled) {
                        change.consume()
                        accumulatedDragPx += dragAmount
                        val stepCount = (accumulatedDragPx / stepPx).toInt()
                        if (stepCount != 0) {
                            gestureValue = gestureValue.stepIn(range, -stepCount, loop)
                            latestOnValueChange(gestureValue)
                            accumulatedDragPx -= stepCount * stepPx
                        }
                    } else {
                        change.consume()
                    }
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = previous.toString().padStart(2, '0'),
            color = AudioTextSecondary.copy(alpha = 0.24f),
            fontSize = 13.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(enabled = enabled) { onValueChange(previous) }
                .padding(horizontal = 18.dp, vertical = 2.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.035f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value.toString().padStart(2, '0'),
                color = AudioPurpleLight,
                fontSize = 27.sp
            )
        }
        Text(
            text = next.toString().padStart(2, '0'),
            color = AudioTextSecondary.copy(alpha = 0.24f),
            fontSize = 13.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(enabled = enabled) { onValueChange(next) }
                .padding(horizontal = 18.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun SleepTimerSwitchRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.025f))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            color = AudioTextPrimary.copy(alpha = 0.88f),
            style = MaterialTheme.typography.bodySmall
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.72f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AudioPurple.copy(alpha = 0.74f),
                uncheckedThumbColor = Color.White.copy(alpha = 0.82f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.16f),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
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

private fun LocalMediaFile.queueSubtitle(): String {
    return listOfNotNull(
        extension.ifBlank { "Mp3" }.uppercase().takeIf { it.isNotBlank() },
        parentFolderName?.takeIf { it.isNotBlank() }
    ).joinToString(" · ").ifBlank { "本地音乐" }
}

private fun LocalMediaFile.queueDurationText(): String {
    return durationMs?.takeIf { it > 0L }?.let(::formatDuration) ?: "--:--"
}

private fun LocalMediaFile.normalizedQueueKey(): String {
    return uri.trim().ifBlank { id.ifBlank { name } }
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

private fun AudioPlayMode.shortLabel(): String {
    return when (this) {
        AudioPlayMode.NORMAL -> "顺序"
        AudioPlayMode.SHUFFLE -> "随机"
        AudioPlayMode.REPEAT_ONE -> "单曲"
        AudioPlayMode.REPEAT_ALL -> "循环"
    }
}

private fun LocalMediaFile.audioSubtitle(): String {
    val folder = parentFolderName?.takeIf { it.isNotBlank() }
    val extension = name.substringAfterLast('.', missingDelimiterValue = "")
        .takeIf { it.isNotBlank() }
        ?.uppercase()
    return when {
        folder != null && extension != null -> "$folder · $extension"
        folder != null -> folder
        extension != null -> extension
        else -> "本地音乐"
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

private enum class AudioSleepTimerMode {
    Off,
    Countdown,
    CountdownThenFinishTrack,
    StopAfterCurrentTrack,
    PendingPauseAfterCurrentTrack
}

private const val MaxSleepTimerHours = 24
private const val DefaultSleepTimerMinutes = 30
private const val MinAbRepeatDurationMs = 1_000L
private const val AbRepeatSeekThresholdMs = 100L
private const val AbRepeatSeekDebounceMs = 500L

private fun Int.stepIn(range: IntRange, delta: Int, loop: Boolean): Int {
    if (!loop) {
        return (this + delta).coerceIn(range.first, range.last)
    }
    val size = range.last - range.first + 1
    val normalized = (this - range.first + delta).floorMod(size)
    return range.first + normalized
}

private fun Int.floorMod(divisor: Int): Int {
    return ((this % divisor) + divisor) % divisor
}

private fun abRepeatLabel(aPointMs: Long, bPointMs: Long): String {
    return when {
        aPointMs < 0L -> "A-B"
        bPointMs > aPointMs -> "循环中"
        else -> "已设 A"
    }
}

private fun sleepTimerButtonLabel(mode: AudioSleepTimerMode, remainingMs: Long): String {
    return when (mode) {
        AudioSleepTimerMode.Off -> "定时"
        AudioSleepTimerMode.StopAfterCurrentTrack -> "播完暂停"
        AudioSleepTimerMode.PendingPauseAfterCurrentTrack -> "待播完"
        AudioSleepTimerMode.Countdown,
        AudioSleepTimerMode.CountdownThenFinishTrack -> {
            if (remainingMs > 0L) formatSleepTimerRemaining(remainingMs) else "定时中"
        }
    }
}

private fun sleepTimerStatusText(mode: AudioSleepTimerMode, remainingMs: Long): String {
    return when (mode) {
        AudioSleepTimerMode.Off -> ""
        AudioSleepTimerMode.Countdown -> "已开启 · 剩余 ${formatSleepTimerRemaining(remainingMs)}"
        AudioSleepTimerMode.CountdownThenFinishTrack -> {
            "已开启 · 剩余 ${formatSleepTimerRemaining(remainingMs)} · 到时播完本曲"
        }
        AudioSleepTimerMode.StopAfterCurrentTrack -> "已开启 · 播完当前歌曲后暂停"
        AudioSleepTimerMode.PendingPauseAfterCurrentTrack -> "定时已到 · 将播完当前歌曲后暂停"
    }
}

private fun formatSleepTimerRemaining(remainingMs: Long): String {
    val totalSeconds = (remainingMs / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else if (minutes > 0L) {
        "${minutes}:${seconds.toString().padStart(2, '0')}"
    } else {
        "${seconds}秒"
    }
}

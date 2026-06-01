package com.shenghui.localvibe.feature.video

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.shenghui.localvibe.core.media.formatDuration
import com.shenghui.localvibe.core.scanner.LocalMediaFile
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    mediaFile: LocalMediaFile?,
    initialPositionMs: Long,
    queue: List<LocalMediaFile>,
    currentIndex: Int,
    onSelectVideo: (Int) -> Unit,
    onProgressChanged: (String, Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    DisposableEffect(activity) {
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        activity?.hideSystemBars()

        onDispose {
            activity?.showSystemBars()
            activity?.requestedOrientation =
                previousOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (mediaFile == null) {
            Text(
                text = "未选择视频文件。",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.Center)
            )
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
            }
            return@Box
        }

        LocalVideoPlayer(
            mediaFile = mediaFile,
            initialPositionMs = initialPositionMs,
            queue = queue,
            currentIndex = currentIndex,
            onSelectVideo = onSelectVideo,
            onProgressChanged = onProgressChanged,
            onBack = onBack,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun LocalVideoPlayer(
    mediaFile: LocalMediaFile,
    initialPositionMs: Long,
    queue: List<LocalMediaFile>,
    currentIndex: Int,
    onSelectVideo: (Int) -> Unit,
    onProgressChanged: (String, Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val player = remember(mediaFile.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(mediaFile.uri)))
            if (initialPositionMs > 0L) {
                seekTo(initialPositionMs)
            }
            prepare()
            playWhenReady = true
        }
    }
    val latestQueue by rememberUpdatedState(queue)
    val latestIndex by rememberUpdatedState(currentIndex)
    var gestureOverlay by remember { mutableStateOf<String?>(null) }
    var seekPreviewOverlay by remember { mutableStateOf<VideoSeekPreview?>(null) }
    var showControls by remember { mutableStateOf(true) }
    var currentPositionMs by remember(mediaFile.uri) { mutableLongStateOf(initialPositionMs.coerceAtLeast(0L)) }
    var draggingPositionMs by remember(mediaFile.uri) { mutableLongStateOf(initialPositionMs.coerceAtLeast(0L)) }
    var isSeekingByUser by remember { mutableStateOf(false) }
    var durationMs by remember(mediaFile.uri) { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(true) }
    var gestureStart by remember { mutableStateOf(Offset.Zero) }
    var dragMode by remember { mutableStateOf(VideoDragMode.UNKNOWN) }
    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }
    var gestureSeekStartMs by remember { mutableLongStateOf(0L) }
    var gestureSeekPreviewMs by remember { mutableLongStateOf(0L) }
    var startBrightness by remember { mutableFloatStateOf(activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.5f) }
    var startVolume by remember { mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) }

    fun saveCurrentProgress() {
        onProgressChanged(mediaFile.uri, player.savedPosition())
    }

    fun selectVideo(index: Int) {
        saveCurrentProgress()
        onSelectVideo(index)
    }

    fun previous() {
        if (latestIndex > 0) {
            selectVideo(latestIndex - 1)
        } else {
            Toast.makeText(context, "已经是第一个", Toast.LENGTH_SHORT).show()
        }
    }

    fun next(auto: Boolean = false) {
        if (latestIndex < latestQueue.lastIndex) {
            selectVideo(latestIndex + 1)
        } else if (!auto) {
            Toast.makeText(context, "已经是最后一个", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(gestureOverlay) {
        if (gestureOverlay != null) {
            delay(1000)
            gestureOverlay = null
        }
    }
    LaunchedEffect(seekPreviewOverlay, dragMode) {
        if (seekPreviewOverlay != null && dragMode != VideoDragMode.SEEK) {
            delay(900)
            seekPreviewOverlay = null
        }
    }
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(2_000)
            showControls = false
        }
    }
    LaunchedEffect(player) {
        while (true) {
            if (!isSeekingByUser) {
                currentPositionMs = player.currentPosition.coerceAtLeast(0L)
            }
            durationMs = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L
            isPlaying = player.isPlaying
            delay(500)
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    next(auto = true)
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            saveCurrentProgress()
            player.release()
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showControls = !showControls }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        gestureStart = offset
                        dragMode = VideoDragMode.UNKNOWN
                        totalDragX = 0f
                        totalDragY = 0f
                        gestureSeekStartMs = player.currentPosition.coerceAtLeast(0L)
                        gestureSeekPreviewMs = gestureSeekStartMs
                        startBrightness = activity?.window?.attributes?.screenBrightness
                            ?.takeIf { it >= 0f } ?: 0.5f
                        startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    },
                    onDragEnd = {
                        if (dragMode == VideoDragMode.SEEK) {
                            val target = gestureSeekPreviewMs.coerceIn(0L, durationMs.coerceAtLeast(1L))
                            currentPositionMs = target
                            player.seekTo(target)
                            seekPreviewOverlay = VideoSeekPreview(target, target - gestureSeekStartMs)
                            showControls = true
                        }
                        dragMode = VideoDragMode.UNKNOWN
                    },
                    onDragCancel = {
                        dragMode = VideoDragMode.UNKNOWN
                    },
                    onDrag = { change, dragAmount ->
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y
                        if (dragMode == VideoDragMode.UNKNOWN) {
                            val absX = totalDragX.absoluteValue
                            val absY = totalDragY.absoluteValue
                            if (absX > 18f || absY > 18f) {
                                dragMode = if (absX > absY * 1.25f) {
                                    VideoDragMode.SEEK
                                } else {
                                    VideoDragMode.VERTICAL
                                }
                            }
                        }
                        when (dragMode) {
                            VideoDragMode.SEEK -> {
                                val width = size.width.toFloat().coerceAtLeast(1f)
                                val deltaMs = ((totalDragX / (width * 0.5f)) * 60_000L)
                                    .toLong()
                                    .coerceIn(-300_000L, 300_000L)
                                val target = (gestureSeekStartMs + deltaMs)
                                    .coerceIn(0L, durationMs.coerceAtLeast(1L))
                                gestureSeekPreviewMs = target
                                seekPreviewOverlay = VideoSeekPreview(target, target - gestureSeekStartMs)
                                showControls = true
                            }
                            VideoDragMode.VERTICAL -> {
                                val height = size.height.toFloat().coerceAtLeast(1f)
                                val percentDelta = (-totalDragY / height) * 1.8f
                                if (gestureStart.x < size.width / 2f) {
                                    val brightness = (startBrightness + percentDelta).coerceIn(0.05f, 1f)
                                    activity?.setScreenBrightness(brightness)
                                    gestureOverlay = "亮度 ${(brightness * 100).toInt()}%"
                                    showControls = true
                                } else {
                                    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    val volumeDelta = (percentDelta * maxVolume).toInt()
                                    val nextVolume = (startVolume + volumeDelta).coerceIn(0, maxVolume)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, nextVolume, 0)
                                    val percent = if (maxVolume == 0) 0 else nextVolume * 100 / maxVolume
                                    gestureOverlay = "音量 $percent%"
                                    showControls = true
                                }
                            }
                            VideoDragMode.UNKNOWN -> Unit
                        }
                        change.consume()
                    }
                )
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    useController = true
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            update = { playerView ->
                playerView.player = player
            }
        )

        if (showControls) {
            VideoControlOverlay(
                title = mediaFile.name,
                currentPositionMs = if (isSeekingByUser) draggingPositionMs else currentPositionMs,
                durationMs = durationMs,
                isPlaying = isPlaying,
                onBack = onBack,
                onSeekStart = {
                    isSeekingByUser = true
                    draggingPositionMs = currentPositionMs
                    showControls = true
                },
                onSeekPreview = {
                    draggingPositionMs = it
                    showControls = true
                },
                onSeekFinished = {
                    val target = draggingPositionMs.coerceIn(0L, durationMs.coerceAtLeast(1L))
                    currentPositionMs = target
                    player.seekTo(target)
                    isSeekingByUser = false
                    showControls = true
                },
                onPrevious = { previous() },
                onPlayPause = {
                    if (player.isPlaying) player.pause() else player.play()
                    isPlaying = player.isPlaying
                    showControls = true
                },
                onNext = { next() }
            )
        }

        gestureOverlay?.let { text ->
            val isBrightness = text.startsWith("亮度")
            GestureValueOverlay(
                text = text,
                percent = text.substringAfter(" ").substringBefore("%").toIntOrNull() ?: 0,
                isBrightness = isBrightness,
                modifier = Modifier
                    .align(if (isBrightness) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 28.dp)
            )
        }

        seekPreviewOverlay?.let { preview ->
            SeekPreviewOverlay(
                preview = preview,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun GestureValueOverlay(
    text: String,
    percent: Int,
    isBrightness: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(54.dp)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(
            imageVector = if (isBrightness) Icons.Filled.Brightness6 else Icons.Filled.VolumeUp,
            contentDescription = null,
            tint = Color.White
        )
        Box(
            modifier = Modifier
                .height(88.dp)
                .width(3.dp)
                .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((88 * percent.coerceIn(0, 100) / 100f).dp)
                    .background(Color(0xFF8AB6FF), RoundedCornerShape(6.dp))
            )
        }
        Text(
            text = text.substringAfter(" "),
            color = Color.White,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun SeekPreviewOverlay(
    preview: VideoSeekPreview,
    modifier: Modifier = Modifier
) {
    val isForward = preview.deltaMs >= 0L
    Column(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isForward) Icons.Filled.FastForward else Icons.Filled.FastRewind,
            contentDescription = null,
            tint = Color(0xFF8AB6FF),
            modifier = Modifier.size(34.dp)
        )
        Text(
            text = formatDuration(preview.targetMs),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = formatSeekDelta(preview.deltaMs),
            color = Color.White.copy(alpha = 0.72f),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun VideoControlOverlay(
    title: String,
    currentPositionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onSeekStart: () -> Unit,
    onSeekPreview: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    var moreMenuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.34f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
            }
            Text(
                text = title,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 64.dp),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                IconButton(onClick = { moreMenuExpanded = !moreMenuExpanded }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = Color.White)
                }
                if (moreMenuExpanded) {
                    VideoToolPanel(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 50.dp, end = 4.dp),
                        onToolClick = { label ->
                            moreMenuExpanded = false
                            Toast.makeText(context, "$label 功能后续实现", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.24f))
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(formatDuration(currentPositionMs), color = Color.White)
                ThinVideoProgressBar(
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    onSeekStart = onSeekStart,
                    onSeekPreview = onSeekPreview,
                    onSeekFinished = onSeekFinished,
                    modifier = Modifier.weight(1f)
                )
                Text(formatDuration(durationMs), color = Color.White)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(30.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "上一个",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = onPlayPause, modifier = Modifier.size(58.dp)) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "下一个",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoToolPanel(
    onToolClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tools = listOf(
        "音轨" to Icons.Filled.Audiotrack,
        "字幕" to Icons.Filled.Subtitles,
        "解码" to Icons.Filled.Settings,
        "倍速" to Icons.Filled.Speed,
        "画面" to Icons.Filled.AspectRatio,
        "循环" to Icons.Filled.Loop,
        "截图" to Icons.Filled.PhotoCamera,
        "更多" to Icons.Filled.MoreHoriz
    )
    Column(
        modifier = modifier
            .width(292.dp)
            .background(Color(0xE6101722), RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        tools.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { (label, icon) ->
                    VideoToolItem(
                        label = label,
                        icon = icon,
                        onClick = { onToolClick(label) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoToolItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFF151E2B), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF8AB6FF),
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.86f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

@Composable
private fun ThinVideoProgressBar(
    currentPositionMs: Long,
    durationMs: Long,
    onSeekStart: () -> Unit,
    onSeekPreview: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    val progressFraction = (currentPositionMs.coerceIn(0L, safeDuration) / safeDuration.toFloat())
        .coerceIn(0f, 1f)

    fun Offset.toSeekPosition(width: Int): Long {
        val fraction = (x / width.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
        return (safeDuration * fraction).toLong()
    }

    BoxWithConstraints(
        modifier = modifier
            .height(22.dp)
            .pointerInput(safeDuration) {
                detectTapGestures { offset ->
                    onSeekStart()
                    onSeekPreview(offset.toSeekPosition(size.width))
                    onSeekFinished()
                }
            }
            .pointerInput(safeDuration) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onSeekStart()
                        onSeekPreview(offset.toSeekPosition(size.width))
                    },
                    onDragEnd = { onSeekFinished() },
                    onDragCancel = { onSeekFinished() },
                    onDrag = { change, _ ->
                        onSeekPreview(change.position.toSeekPosition(size.width))
                        change.consume()
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color(0xFF2E3445), RoundedCornerShape(99.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(progressFraction)
                .height(2.dp)
                .background(Color(0xFF4D8DFF), RoundedCornerShape(99.dp))
        )
        Box(
            modifier = Modifier
                .offset(x = (maxWidth - 10.dp) * progressFraction)
                .size(10.dp)
                .background(Color(0xFF8AB6FF), CircleShape)
        )
    }
}

private fun ExoPlayer.savedPosition(): Long {
    val currentPosition = currentPosition.coerceAtLeast(0L)
    val duration = duration
    return if (
        duration != C.TIME_UNSET &&
        duration > 0L &&
        duration - currentPosition <= FINISHED_THRESHOLD_MS
    ) {
        0L
    } else {
        currentPosition
    }
}

private enum class VideoDragMode {
    UNKNOWN,
    VERTICAL,
    SEEK
}

private data class VideoSeekPreview(
    val targetMs: Long,
    val deltaMs: Long
)

private fun formatSeekDelta(deltaMs: Long): String {
    val sign = if (deltaMs >= 0L) "+" else "-"
    return "$sign${formatDuration(deltaMs.absoluteValue)}"
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private fun Activity.hideSystemBars() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    WindowInsetsControllerCompat(window, window.decorView).apply {
        hide(WindowInsetsCompat.Type.systemBars())
        systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

private fun Activity.showSystemBars() {
    WindowInsetsControllerCompat(window, window.decorView)
        .show(WindowInsetsCompat.Type.systemBars())
    WindowCompat.setDecorFitsSystemWindows(window, true)
}

private fun Activity.setScreenBrightness(value: Float) {
    val attrs = window.attributes
    attrs.screenBrightness = value
    window.attributes = attrs
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
}

private const val FINISHED_THRESHOLD_MS = 5_000L

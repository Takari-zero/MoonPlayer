package com.shenghui.localvibe.feature.video

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.util.Locale
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import kotlin.math.round

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
                Icon(Icons.Filled.ArrowBack, contentDescription = "杩斿洖", tint = Color.White)
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
            setMediaItem(buildVideoMediaItem(mediaFile.uri, null))
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
    var resizeModeOverlay by remember { mutableStateOf<String?>(null) }
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
    var playbackSpeed by remember(mediaFile.uri) { mutableFloatStateOf(1f) }
    var videoResizeMode by remember(mediaFile.uri) { mutableStateOf(VideoResizeMode.FIT) }
    var isRepeatOne by remember(mediaFile.uri) { mutableStateOf(false) }
    var externalSubtitleUri by remember(mediaFile.uri) { mutableStateOf<Uri?>(null) }
    var audioDelayMs by remember(mediaFile.uri) { mutableLongStateOf(0L) }
    var isBackRequested by remember(mediaFile.uri) { mutableStateOf(false) }
    val subtitleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        externalSubtitleUri = uri
        Toast.makeText(context, "已加载外挂字幕", Toast.LENGTH_SHORT).show()
    }

    fun saveCurrentProgress() {
        onProgressChanged(mediaFile.uri, player.savedPosition())
    }

    fun requestBack() {
        if (isBackRequested) return
        isBackRequested = true
        onBack()
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
    LaunchedEffect(resizeModeOverlay) {
        if (resizeModeOverlay != null) {
            delay(1_000)
            resizeModeOverlay = null
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
            Handler(Looper.getMainLooper()).post {
                player.release()
            }
        }
    }

    LaunchedEffect(externalSubtitleUri) {
        val subtitleUri = externalSubtitleUri ?: return@LaunchedEffect
        val current = player.currentPosition.coerceAtLeast(0L)
        val shouldPlay = player.playWhenReady
        player.setMediaItem(buildVideoMediaItem(mediaFile.uri, subtitleUri), current)
        player.prepare()
        player.playWhenReady = shouldPlay
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
                        if (dragMode == VideoDragMode.BACK) {
                            requestBack()
                        } else if (dragMode == VideoDragMode.SEEK) {
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
                                dragMode = if (
                                    gestureStart.x > size.width - 56.dp.toPx() &&
                                    totalDragX < -40f &&
                                    absX > absY * 1.25f
                                ) {
                                    VideoDragMode.BACK
                                } else if (absX > absY * 1.25f) {
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
                                    gestureOverlay = "浜害 ${(brightness * 100).toInt()}%"
                                    showControls = true
                                } else {
                                    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    val volumeDelta = (percentDelta * maxVolume).toInt()
                                    val nextVolume = (startVolume + volumeDelta).coerceIn(0, maxVolume)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, nextVolume, 0)
                                    val percent = if (maxVolume == 0) 0 else nextVolume * 100 / maxVolume
                                    gestureOverlay = "闊抽噺 $percent%"
                                    showControls = true
                                }
                            }
                            VideoDragMode.UNKNOWN -> Unit
                            VideoDragMode.BACK -> {
                                showControls = true
                            }
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
                    resizeMode = videoResizeMode.playerResizeMode
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            update = { playerView ->
                playerView.player = player
                playerView.resizeMode = videoResizeMode.playerResizeMode
            }
        )

        if (showControls) {
            VideoControlOverlay(
                title = mediaFile.name,
                currentPositionMs = if (isSeekingByUser) draggingPositionMs else currentPositionMs,
                durationMs = durationMs,
                isPlaying = isPlaying,
                onBack = ::requestBack,
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
                onNext = { next() },
                playbackSpeed = playbackSpeed,
                resizeMode = videoResizeMode,
                isRepeatOne = isRepeatOne,
                audioDelayMs = audioDelayMs,
                onSpeedSelected = { speed ->
                    val safeSpeed = speed.coerceIn(0.1f, 5f)
                    playbackSpeed = safeSpeed
                    player.setPlaybackSpeed(safeSpeed)
                    Toast.makeText(context, "已切换到 ${safeSpeed.formatSpeed()}x", Toast.LENGTH_SHORT).show()
                },
                onResizeModeSelected = { mode ->
                    videoResizeMode = mode
                    resizeModeOverlay = mode.label
                },
                onToggleRepeat = {
                    val nextRepeat = !isRepeatOne
                    isRepeatOne = nextRepeat
                    player.repeatMode = if (nextRepeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                    Toast.makeText(
                        context,
                        if (nextRepeat) "循环播放已开启" else "循环播放已关闭",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onSubtitleSelect = {
                    subtitleLauncher.launch(
                        arrayOf(
                            "application/x-subrip",
                            "text/vtt",
                            "text/x-ssa",
                            "text/plain",
                            "text/*",
                            "application/octet-stream"
                        )
                    )
                },
                onAudioDelayChange = { nextDelayMs ->
                    audioDelayMs = nextDelayMs.coerceIn(-5_000L, 5_000L)
                }
            )
        }

        gestureOverlay?.let { text ->
            val isBrightness = text.startsWith("浜害")
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

        resizeModeOverlay?.let { label ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.48f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 30.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
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
    onNext: () -> Unit,
    playbackSpeed: Float,
    resizeMode: VideoResizeMode,
    isRepeatOne: Boolean,
    audioDelayMs: Long,
    onSpeedSelected: (Float) -> Unit,
    onResizeModeSelected: (VideoResizeMode) -> Unit,
    onToggleRepeat: () -> Unit,
    onSubtitleSelect: () -> Unit,
    onAudioDelayChange: (Long) -> Unit
) {
    val context = LocalContext.current
    var moreMenuExpanded by remember { mutableStateOf(false) }
    var showSpeedPanel by remember { mutableStateOf(false) }
    var showResizePanel by remember { mutableStateOf(false) }
    var showAudioDelayPanel by remember { mutableStateOf(false) }
    var showSubtitlePanel by remember { mutableStateOf(false) }
    var showEqualizerPanel by remember { mutableStateOf(false) }
    var eqBass by remember { mutableFloatStateOf(0f) }
    var eqMid by remember { mutableFloatStateOf(0f) }
    var eqTreble by remember { mutableFloatStateOf(0f) }

    fun closePanelOrBack() {
        when {
            showAudioDelayPanel -> showAudioDelayPanel = false
            showSubtitlePanel -> showSubtitlePanel = false
            showEqualizerPanel -> showEqualizerPanel = false
            showSpeedPanel -> showSpeedPanel = false
            showResizePanel -> showResizePanel = false
            moreMenuExpanded -> moreMenuExpanded = false
            else -> onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(showAudioDelayPanel, showSubtitlePanel, showEqualizerPanel, showSpeedPanel, showResizePanel, moreMenuExpanded) {
                detectDragGestures { change, dragAmount ->
                    if (change.position.x > size.width - 72.dp.toPx() && dragAmount.x < -26f) {
                        closePanelOrBack()
                        change.consume()
                    }
                }
            }
    ) {
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
                    .padding(horizontal = 176.dp),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(onClick = { showAudioDelayPanel = true }) {
                    Icon(Icons.Filled.Audiotrack, contentDescription = "音轨", tint = Color.White)
                }
                IconButton(onClick = { showSubtitlePanel = true }) {
                    Icon(Icons.Filled.Subtitles, contentDescription = "字幕", tint = Color.White)
                }
                Box {
                    IconButton(onClick = { moreMenuExpanded = !moreMenuExpanded }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = Color.White)
                    }
                }
            }
        }

        if (moreMenuExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { moreMenuExpanded = false }
            )
            VideoToolPanel(
                playbackSpeed = playbackSpeed,
                isRepeatOne = isRepeatOne,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 60.dp, end = 18.dp),
                onToolClick = { label ->
                    moreMenuExpanded = false
                    when (label) {
                        "倍速" -> showSpeedPanel = true
                        "循环" -> onToggleRepeat()
                        "解码" -> Toast.makeText(context, "解码方式后续实现", Toast.LENGTH_SHORT).show()
                        "截图" -> Toast.makeText(context, "截图功能后续实现", Toast.LENGTH_SHORT).show()
                        else -> Toast.makeText(context, "更多功能后续实现", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        if (showSpeedPanel) {
            VideoSpeedPanel(
                speed = playbackSpeed,
                onDismiss = { showSpeedPanel = false },
                onSpeedChange = onSpeedSelected,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 60.dp, end = 18.dp)
            )
        }

        if (showResizePanel) {
            VideoChoicePanel(
                title = "画面比例",
                options = VideoResizeMode.entries.map { it.label },
                selected = resizeMode.label,
                onDismiss = { showResizePanel = false },
                onSelect = { option ->
                    VideoResizeMode.entries.firstOrNull { it.label == option }?.let(onResizeModeSelected)
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 60.dp, end = 18.dp)
            )
        }

        if (showAudioDelayPanel) {
            AudioDelayPanel(
                delayMs = audioDelayMs,
                onDismiss = { showAudioDelayPanel = false },
                onChange = { nextDelayMs ->
                    onAudioDelayChange(nextDelayMs)
                    Toast.makeText(context, "音轨偏移 ${formatSignedDelay(nextDelayMs)}", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 60.dp, end = 18.dp)
            )
        }

        if (showSubtitlePanel) {
            SubtitlePanel(
                onDismiss = { showSubtitlePanel = false },
                onPickSubtitle = onSubtitleSelect,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 60.dp, end = 18.dp)
            )
        }

        if (showEqualizerPanel) {
            VideoEqualizerPanel(
                bass = eqBass,
                mid = eqMid,
                treble = eqTreble,
                onBassChange = { eqBass = it },
                onMidChange = { eqMid = it },
                onTrebleChange = { eqTreble = it },
                onDismiss = { showEqualizerPanel = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 60.dp, end = 18.dp)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 42.dp, top = 72.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VideoQuickToolButton(
                icon = Icons.Filled.Tune,
                label = "均衡器",
                onClick = { showEqualizerPanel = true }
            )
            VideoQuickToolButton(
                icon = Icons.Filled.Speed,
                label = "${playbackSpeed.formatSpeed()}X",
                onClick = { showSpeedPanel = true }
            )
            VideoQuickToolButton(
                icon = Icons.Filled.PhotoCamera,
                label = "截图",
                onClick = { Toast.makeText(context, "截图功能后续实现", Toast.LENGTH_SHORT).show() }
            )
            VideoQuickToolButton(
                icon = Icons.Filled.MoreHoriz,
                label = "更多",
                onClick = { moreMenuExpanded = true }
            )
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
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "上一集", tint = Color.White, modifier = Modifier.size(32.dp))
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
                    Icon(Icons.Filled.SkipNext, contentDescription = "下一集", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = { onResizeModeSelected(resizeMode.next()) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.AspectRatio, contentDescription = "适应屏幕", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}
@Composable
private fun VideoToolPanel(
    playbackSpeed: Float,
    isRepeatOne: Boolean,
    onToolClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tools = listOf(
        VideoToolAction("解码", "自动", Icons.Filled.Settings),
        VideoToolAction("倍速", "${playbackSpeed.formatSpeed()}x", Icons.Filled.Speed),
        VideoToolAction("循环", if (isRepeatOne) "开启" else "关闭", Icons.Filled.Loop),
        VideoToolAction("截图", null, Icons.Filled.PhotoCamera),
        VideoToolAction("更多", null, Icons.Filled.MoreHoriz)
    )
    Column(
        modifier = modifier
            .width(244.dp)
            .background(Color(0xE6101722), RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        tools.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { tool ->
                    VideoToolItem(
                        label = tool.label,
                        value = tool.value,
                        icon = tool.icon,
                        onClick = { onToolClick(tool.label) }
                    )
                }
            }
        }
    }
}
@Composable
private fun VideoToolItem(
    label: String,
    value: String? = null,
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
            text = value?.let { "$label\n$it" } ?: label,
            color = Color.White.copy(alpha = 0.86f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun VideoQuickToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (label.endsWith("X")) {
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(25.dp)
            )
        }
    }
}

@Composable
private fun AudioDelayPanel(
    delayMs: Long,
    onDismiss: () -> Unit,
    onChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    SidePanelShell(title = "音轨同步", onDismiss = onDismiss, modifier = modifier.width(260.dp)) {
        Text(
            text = "当前偏移 ${formatSignedDelay(delayMs)}",
            color = Color.White.copy(alpha = 0.78f),
            style = MaterialTheme.typography.bodySmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(-500L, -100L, 0L, 100L, 500L).forEach { delta ->
                Text(
                    text = if (delta == 0L) "归零" else formatSignedDelay(delta),
                    color = if (delta == 0L) Color(0xFF8AB6FF) else Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { if (delta == 0L) onChange(0L) else onChange(delayMs + delta) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        Text(
            text = "负数表示声音提前，正数表示声音延后。本版先保存偏移设置，底层真实延迟后续专项接入。",
            color = Color.White.copy(alpha = 0.58f),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun SubtitlePanel(
    onDismiss: () -> Unit,
    onPickSubtitle: () -> Unit,
    modifier: Modifier = Modifier
) {
    SidePanelShell(title = "字幕", onDismiss = onDismiss, modifier = modifier.width(240.dp)) {
        Text(
            text = "外挂字幕",
            color = Color.White.copy(alpha = 0.78f),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "选择 SRT / ASS / SSA / VTT 字幕文件",
            color = Color.White.copy(alpha = 0.58f),
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = "选择字幕",
            color = Color(0xFF8AB6FF),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .clickable(onClick = onPickSubtitle)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = "字幕加减速与同步微调后续接入。",
            color = Color.White.copy(alpha = 0.5f),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun VideoEqualizerPanel(
    bass: Float,
    mid: Float,
    treble: Float,
    onBassChange: (Float) -> Unit,
    onMidChange: (Float) -> Unit,
    onTrebleChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    SidePanelShell(title = "均衡器", onDismiss = onDismiss, modifier = modifier.width(250.dp)) {
        EqualizerSlider("低频", bass, onBassChange)
        EqualizerSlider("中频", mid, onMidChange)
        EqualizerSlider("高频", treble, onTrebleChange)
        Text(
            text = "当前为播放器内调节面板，真实音效处理后续专项接入。",
            color = Color.White.copy(alpha = 0.5f),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun VideoSpeedPanel(
    speed: Float,
    onDismiss: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var draftSpeed by remember(speed) { mutableFloatStateOf(speed.coerceIn(0.1f, 5f)) }
    val speedOptions = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 3f, 5f)
    SidePanelShell(title = "播放速度", onDismiss = onDismiss, modifier = modifier.width(280.dp)) {
        Text(
            text = "${draftSpeed.formatSpeed()}x",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Slider(
            value = draftSpeed,
            onValueChange = {
                draftSpeed = it.coerceIn(0.1f, 5f)
            },
            onValueChangeFinished = {
                onSpeedChange(draftSpeed)
            },
            valueRange = 0.1f..5f,
            colors = videoSliderColors()
        )
        Text(
            text = "常用速度",
            color = Color.White.copy(alpha = 0.62f),
            style = MaterialTheme.typography.labelSmall
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            speedOptions.chunked(4).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { option ->
                        val selected = (draftSpeed - option).absoluteValue < 0.01f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) Color(0xFF4D8DFF) else Color.White.copy(alpha = 0.08f))
                                .clickable {
                                    draftSpeed = option
                                    onSpeedChange(option)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${option.formatSpeed()}x",
                                color = if (selected) Color.White else Color.White.copy(alpha = 0.84f),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                    repeat(4 - row.size) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SidePanelShell(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .background(Color(0xE6101722), RoundedCornerShape(18.dp))
            .clickable(onClick = {})
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Color.White, style = MaterialTheme.typography.titleSmall)
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "关闭", tint = Color.White.copy(alpha = 0.78f))
            }
        }
        content()
    }
}

@Composable
private fun EqualizerSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.labelMedium)
            Text("${value.toInt()} dB", color = Color(0xFF8AB6FF), style = MaterialTheme.typography.labelSmall)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -10f..10f,
            colors = videoSliderColors()
        )
    }
}

@Composable
private fun videoSliderColors() = SliderDefaults.colors(
    thumbColor = Color(0xFF8AB6FF),
    activeTrackColor = Color(0xFF4D8DFF),
    inactiveTrackColor = Color(0xFF2E3445)
)
@Composable
private fun VideoChoicePanel(
    title: String,
    options: List<String>,
    selected: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss)
    )
    Column(
        modifier = modifier
            .width(190.dp)
            .background(Color(0xE6101722), RoundedCornerShape(18.dp))
            .clickable(onClick = {})
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, color = Color.White, style = MaterialTheme.typography.titleSmall)
        options.forEach { option ->
            Text(
                text = if (option == selected) "$option  ✓" else option,
                color = if (option == selected) Color(0xFF8AB6FF) else Color.White.copy(alpha = 0.86f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelect(option) }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
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

private fun buildVideoMediaItem(videoUri: String, subtitleUri: Uri?): MediaItem {
    val builder = MediaItem.Builder().setUri(Uri.parse(videoUri))
    if (subtitleUri != null) {
        builder.setSubtitleConfigurations(
            listOf(
                MediaItem.SubtitleConfiguration.Builder(subtitleUri)
                    .setMimeType(subtitleMimeType(subtitleUri))
                    .setLanguage("und")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
            )
        )
    }
    return builder.build()
}

private fun subtitleMimeType(uri: Uri): String {
    return when (uri.toString().substringAfterLast('.', "").lowercase()) {
        "srt" -> "application/x-subrip"
        "vtt" -> "text/vtt"
        "ass", "ssa" -> "text/x-ssa"
        else -> "text/vtt"
    }
}

private enum class VideoDragMode {
    UNKNOWN,
    BACK,
    VERTICAL,
    SEEK
}

private enum class VideoResizeMode(
    val label: String,
    val playerResizeMode: Int
) {
    FIT("适应", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("填充", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    ZOOM("缩放", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    STRETCH("拉伸", AspectRatioFrameLayout.RESIZE_MODE_FILL);

    fun next(): VideoResizeMode {
        val nextIndex = (entries.indexOf(this) + 1) % entries.size
        return entries[nextIndex]
    }
}

private data class VideoToolAction(
    val label: String,
    val value: String?,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private data class VideoSeekPreview(
    val targetMs: Long,
    val deltaMs: Long
)

private fun Float.formatSpeed(): String {
    val rounded = round(this * 100f) / 100f
    return String.format(Locale.US, "%.2f", rounded)
        .trimEnd('0')
        .trimEnd('.')
}

private fun formatSeekDelta(deltaMs: Long): String {
    val sign = if (deltaMs >= 0L) "+" else "-"
    return "$sign${formatDuration(deltaMs.absoluteValue)}"
}

private fun formatSignedDelay(delayMs: Long): String {
    return when {
        delayMs > 0L -> "+${delayMs}ms"
        delayMs < 0L -> "${delayMs}ms"
        else -> "0ms"
    }
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




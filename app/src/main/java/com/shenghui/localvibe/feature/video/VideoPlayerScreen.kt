package com.shenghui.localvibe.feature.video

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.shenghui.localvibe.core.media.formatFileSize
import com.shenghui.localvibe.core.media.formatDuration
import com.shenghui.localvibe.core.scanner.LocalMediaFile
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
import kotlin.math.round

private val PlayerMoonPurple = Color(0xFF7B55FF)
private val PlayerMoonPurpleSoft = Color(0xFFB7A7FF)
private val PlayerPanelDark = Color(0xE60B0A12)
private val PlayerPanelStroke = Color(0x30B7A7FF)
private val PlayerTrackInactive = Color(0xFF3A3449)
private val VideoFolderPlaybackSpeeds = mutableMapOf<String, Float>()
private const val VIDEO_SEEK_END_GUARD_MS = 500L
private const val PLAYER_SIDE_PANEL_HEIGHT_FRACTION = 0.96f

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    mediaFile: LocalMediaFile?,
    initialPositionMs: Long,
    queue: List<LocalMediaFile>,
    currentIndex: Int,
    onSelectVideo: (Int) -> Unit,
    onProgressChanged: (String, Long) -> Unit,
    folderPlaybackSpeeds: Map<String, Float>,
    onFolderPlaybackSpeedChanged: (String, Float) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current
    var isPlayerForeground by remember { mutableStateOf(true) }

    DisposableEffect(activity, lifecycleOwner) {
        val previousOrientation =
            activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        fun restoreActivityChrome() {
            activity?.showSystemBars()
            activity?.requestedOrientation = previousOrientation
        }

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity?.hideSystemBars()

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    isPlayerForeground = true
                    activity?.hideSystemBars()
                }

                Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                    isPlayerForeground = false
                    restoreActivityChrome()
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            restoreActivityChrome()
        }
    }

    LaunchedEffect(activity, isPlayerForeground) {
        if (isPlayerForeground) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
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
            folderPlaybackSpeeds = folderPlaybackSpeeds,
            onFolderPlaybackSpeedChanged = onFolderPlaybackSpeedChanged,
            isPlayerForeground = isPlayerForeground,
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
    folderPlaybackSpeeds: Map<String, Float>,
    onFolderPlaybackSpeedChanged: (String, Float) -> Unit,
    isPlayerForeground: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val coroutineScope = rememberCoroutineScope()
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
    val latestOnFolderPlaybackSpeedChanged by rememberUpdatedState(onFolderPlaybackSpeedChanged)
    val navigationQueue = remember(queue) { queue.videoNavigationQueue() }
    val folderSpeedKey = remember(mediaFile.uri, queue) { mediaFile.videoFolderPlaybackSpeedKey(queue) }
    val savedFolderPlaybackSpeed = folderPlaybackSpeeds[folderSpeedKey]?.normalizeVideoSpeed()
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
    var playbackSpeed by remember(folderSpeedKey, savedFolderPlaybackSpeed) {
        mutableFloatStateOf(
            VideoFolderPlaybackSpeeds[folderSpeedKey]?.normalizeVideoSpeed()
                ?: savedFolderPlaybackSpeed
                ?: 1f
        )
    }
    var pendingFolderSpeedSave by remember { mutableStateOf<Pair<String, Float>?>(null) }
    val latestPendingFolderSpeedSave by rememberUpdatedState(pendingFolderSpeedSave)
    var videoResizeMode by remember(mediaFile.uri) { mutableStateOf(VideoResizeMode.FIT) }
    var isRepeatOne by remember(mediaFile.uri) { mutableStateOf(false) }
    var externalSubtitleUri by remember(mediaFile.uri) { mutableStateOf<Uri?>(null) }
    var externalSubtitleName by remember(mediaFile.uri) { mutableStateOf<String?>(null) }
    var subtitleOffsetMs by remember(mediaFile.uri) { mutableLongStateOf(0L) }
    var audioDelayMs by remember(mediaFile.uri) { mutableLongStateOf(0L) }
    var isBackRequested by remember(mediaFile.uri) { mutableStateOf(false) }
    var isSpeedPanelVisible by remember { mutableStateOf(false) }
    var isScreenLocked by remember(mediaFile.uri) { mutableStateOf(false) }
    var isPortraitPlayback by remember(mediaFile.uri) { mutableStateOf(false) }
    var isSavingScreenshot by remember { mutableStateOf(false) }
    var isQuickToolsExpanded by remember { mutableStateOf(false) }
    var showLockedUnlockButton by remember(mediaFile.uri) { mutableStateOf(false) }
    var sleepTimerMode by remember { mutableStateOf(VideoSleepTimerMode.OFF) }
    var sleepTimerSelectedOption by remember { mutableStateOf(VideoSleepTimerOption.Off) }
    var sleepTimerEndAtElapsedMs by remember { mutableLongStateOf(0L) }
    var sleepTimerRemainingMs by remember { mutableLongStateOf(0L) }
    val latestSleepTimerMode by rememberUpdatedState(sleepTimerMode)
    var abLoopStartMs by remember(mediaFile.uri) { mutableStateOf<Long?>(null) }
    var abLoopEndMs by remember(mediaFile.uri) { mutableStateOf<Long?>(null) }
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
        externalSubtitleName = context.subtitleDisplayName(uri)
        showVideoPlayerToast(context, "已加载外挂字幕")
    }

    LaunchedEffect(player, playbackSpeed) {
        player.setPlaybackSpeed(playbackSpeed.normalizeVideoSpeed())
    }

    LaunchedEffect(pendingFolderSpeedSave) {
        val pending = pendingFolderSpeedSave ?: return@LaunchedEffect
        delay(VIDEO_SPEED_SAVE_DEBOUNCE_MS)
        latestOnFolderPlaybackSpeedChanged(pending.first, pending.second)
        if (pendingFolderSpeedSave == pending) {
            pendingFolderSpeedSave = null
        }
    }

    DisposableEffect(folderSpeedKey) {
        onDispose {
            latestPendingFolderSpeedSave?.let { pending ->
                latestOnFolderPlaybackSpeedChanged(pending.first, pending.second)
            }
        }
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

    fun currentNavigationIndex(): Int {
        return navigationQueue.indexOfFirst { it.uri == mediaFile.uri }
            .takeIf { it >= 0 }
            ?: latestIndex.coerceIn(0, navigationQueue.lastIndex.coerceAtLeast(0))
    }

    fun selectNavigationVideo(file: LocalMediaFile) {
        val queueIndex = latestQueue.indexOfFirst { it.uri == file.uri }
        if (queueIndex >= 0) {
            selectVideo(queueIndex)
        }
    }

    fun previous() {
        val index = currentNavigationIndex()
        if (index > 0) {
            selectNavigationVideo(navigationQueue[index - 1])
        } else {
            showVideoPlayerToast(context, "已经是第一个")
        }
    }

    fun next(auto: Boolean = false) {
        val index = currentNavigationIndex()
        if (index < navigationQueue.lastIndex) {
            selectNavigationVideo(navigationQueue[index + 1])
        } else if (!auto) {
            showVideoPlayerToast(context, "已经是最后一个")
        }
    }

    fun captureScreenshot() {
        if (isSavingScreenshot) return
        val positionMs = player.currentPosition.coerceAtLeast(0L)
        isSavingScreenshot = true
        coroutineScope.launch {
            val result = withContext(Dispatchers.IO) {
                saveVideoFrameScreenshot(
                    context = context.applicationContext,
                    videoUri = Uri.parse(mediaFile.uri),
                    positionMs = positionMs
                )
            }
            isSavingScreenshot = false
            showVideoPlayerToast(
                context = context,
                message = result.message,
                durationMs = result.toastDurationMs
            )
        }
    }

    fun clearSleepTimer(showHint: Boolean = true) {
        sleepTimerMode = VideoSleepTimerMode.OFF
        sleepTimerSelectedOption = VideoSleepTimerOption.Off
        sleepTimerEndAtElapsedMs = 0L
        sleepTimerRemainingMs = 0L
        if (showHint) {
            showVideoPlayerToast(context, "已关闭睡眠定时")
        }
    }

    fun pauseForSleepTimer() {
        sleepTimerMode = VideoSleepTimerMode.OFF
        sleepTimerSelectedOption = VideoSleepTimerOption.Off
        sleepTimerEndAtElapsedMs = 0L
        sleepTimerRemainingMs = 0L
        player.pause()
        isPlaying = false
        showVideoPlayerToast(context, "睡眠定时已暂停播放")
    }

    fun applySleepTimerOption(option: VideoSleepTimerOption) {
        when (option.mode) {
            VideoSleepTimerMode.TIMED -> {
                val durationMs = option.durationMs ?: return
                sleepTimerMode = VideoSleepTimerMode.TIMED
                sleepTimerSelectedOption = option
                sleepTimerEndAtElapsedMs = SystemClock.elapsedRealtime() + durationMs
                sleepTimerRemainingMs = durationMs
                showVideoPlayerToast(context, "已设置睡眠定时 ${option.label}")
            }

            VideoSleepTimerMode.END_OF_VIDEO -> {
                sleepTimerMode = VideoSleepTimerMode.END_OF_VIDEO
                sleepTimerSelectedOption = option
                sleepTimerEndAtElapsedMs = 0L
                sleepTimerRemainingMs = 0L
                showVideoPlayerToast(context, "已设置当前视频结束后暂停")
            }

            VideoSleepTimerMode.OFF -> {
                clearSleepTimer(showHint = true)
            }
        }
    }

    fun seekToUserPosition(positionMs: Long): Long {
        val target = safeVideoSeekPosition(positionMs, durationMs)
        isSeekingByUser = false
        draggingPositionMs = target
        currentPositionMs = target
        player.seekTo(target)
        coroutineScope.launch {
            delay(120L)
            if (!isSeekingByUser) {
                currentPositionMs = player.currentPosition.coerceAtLeast(0L)
            }
        }
        return target
    }

    fun currentAbMarkerPosition(): Long {
        return safeVideoMarkerPosition(player.currentPosition.coerceAtLeast(0L), durationMs)
    }

    fun setAbLoopStart() {
        val target = currentAbMarkerPosition()
        abLoopStartMs = target
        if (abLoopEndMs != null && abLoopEndMs!! <= target) {
            abLoopEndMs = null
        }
        showVideoPlayerToast(context, "已设置 A 点")
    }

    fun setAbLoopEnd() {
        val target = currentAbMarkerPosition()
        val start = abLoopStartMs
        if (start != null && target <= start) {
            showVideoPlayerToast(context, "B 点必须晚于 A 点")
            return
        }
        abLoopEndMs = target
        showVideoPlayerToast(context, "已设置 B 点")
    }

    fun clearAbLoop() {
        abLoopStartMs = null
        abLoopEndMs = null
        showVideoPlayerToast(context, "已关闭 AB 循环")
    }

    LaunchedEffect(gestureOverlay) {
        if (gestureOverlay != null) {
            delay(GESTURE_HINT_MS)
            gestureOverlay = null
        }
    }
    LaunchedEffect(resizeModeOverlay) {
        if (resizeModeOverlay != null) {
            delay(SHORT_HINT_MS)
            resizeModeOverlay = null
        }
    }
    LaunchedEffect(seekPreviewOverlay, dragMode) {
        if (seekPreviewOverlay != null && dragMode != VideoDragMode.SEEK) {
            delay(GESTURE_HINT_MS)
            seekPreviewOverlay = null
        }
    }
    LaunchedEffect(showControls, isSpeedPanelVisible, isScreenLocked, isQuickToolsExpanded) {
        if (showControls && !isSpeedPanelVisible && !isScreenLocked && !isQuickToolsExpanded) {
            delay(2_000)
            if (!isSpeedPanelVisible && !isScreenLocked && !isQuickToolsExpanded) {
                showControls = false
            }
        }
    }
    LaunchedEffect(isScreenLocked) {
        if (isScreenLocked) {
            showControls = false
            isSpeedPanelVisible = false
            isSeekingByUser = false
            dragMode = VideoDragMode.UNKNOWN
            gestureOverlay = null
            seekPreviewOverlay = null
            resizeModeOverlay = null
            isQuickToolsExpanded = false
            showLockedUnlockButton = true
        } else {
            showLockedUnlockButton = false
        }
    }
    LaunchedEffect(isScreenLocked, showLockedUnlockButton) {
        if (isScreenLocked && showLockedUnlockButton) {
            delay(LOCKED_UNLOCK_HINT_MS)
            showLockedUnlockButton = false
        }
    }
    BackHandler(enabled = isScreenLocked) {
        isScreenLocked = false
        showLockedUnlockButton = false
        showControls = true
        showVideoPlayerToast(context, "已解锁")
    }

    LaunchedEffect(activity, isPortraitPlayback, isPlayerForeground) {
        if (isPlayerForeground) {
            activity?.requestedOrientation =
                if (isPortraitPlayback) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
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

    LaunchedEffect(currentPositionMs, abLoopStartMs, abLoopEndMs, isSeekingByUser, player) {
        val start = abLoopStartMs ?: return@LaunchedEffect
        val end = abLoopEndMs ?: return@LaunchedEffect
        if (end <= start || isSeekingByUser || !player.isPlaying) return@LaunchedEffect
        if (currentPositionMs >= end) {
            val target = safeVideoSeekPosition(start, durationMs)
            currentPositionMs = target
            draggingPositionMs = target
            player.seekTo(target)
        }
    }

    LaunchedEffect(sleepTimerMode, sleepTimerEndAtElapsedMs, player) {
        if (sleepTimerMode != VideoSleepTimerMode.TIMED || sleepTimerEndAtElapsedMs <= 0L) {
            if (sleepTimerMode == VideoSleepTimerMode.OFF) {
                sleepTimerRemainingMs = 0L
            }
            return@LaunchedEffect
        }

        while (isActive && sleepTimerMode == VideoSleepTimerMode.TIMED) {
            val remainingMs = (sleepTimerEndAtElapsedMs - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            sleepTimerRemainingMs = remainingMs
            if (remainingMs <= 0L) {
                pauseForSleepTimer()
                break
            }
            delay(1_000L)
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    if (latestSleepTimerMode == VideoSleepTimerMode.END_OF_VIDEO) {
                        pauseForSleepTimer()
                    } else {
                        next(auto = true)
                    }
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
        val current = player.currentPosition.coerceAtLeast(0L)
        val shouldPlay = player.playWhenReady
        player.setMediaItem(
            buildVideoMediaItem(
                videoUri = mediaFile.uri,
                subtitleUri = externalSubtitleUri
            ),
            current
        )
        player.prepare()
        player.playWhenReady = shouldPlay
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(isScreenLocked, isSpeedPanelVisible, showControls, isQuickToolsExpanded) {
                detectTapGestures(
                    onTap = {
                        if (isScreenLocked) {
                            showLockedUnlockButton = true
                            return@detectTapGestures
                        }
                        if (isSpeedPanelVisible) {
                            isSpeedPanelVisible = false
                            showControls = true
                        } else if (showControls) {
                            showControls = false
                            isQuickToolsExpanded = false
                        } else {
                            showControls = true
                        }
                    }
                )
            }
            .pointerInput(isScreenLocked, isSpeedPanelVisible) {
                if (!isScreenLocked && !isSpeedPanelVisible) {
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
                                val target = seekToUserPosition(gestureSeekPreviewMs)
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

        if (showControls && !isScreenLocked) {
            VideoControlOverlay(
                title = mediaFile.name,
                currentPositionMs = if (isSeekingByUser) draggingPositionMs else currentPositionMs,
                durationMs = durationMs,
                mediaFile = mediaFile,
                player = player,
                navigationQueue = navigationQueue,
                currentQueueUri = mediaFile.uri,
                isPlaying = isPlaying,
                onBack = ::requestBack,
                onQueueItemSelected = { file ->
                    selectNavigationVideo(file)
                },
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
                    seekToUserPosition(draggingPositionMs)
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
                subtitleName = externalSubtitleName,
                subtitleOffsetMs = subtitleOffsetMs,
                sleepTimerMode = sleepTimerMode,
                sleepTimerSelectedOption = sleepTimerSelectedOption,
                sleepTimerRemainingMs = sleepTimerRemainingMs,
                abLoopStartMs = abLoopStartMs,
                abLoopEndMs = abLoopEndMs,
                onSpeedSelected = { speed ->
                    val safeSpeed = speed.normalizeVideoSpeed()
                    playbackSpeed = safeSpeed
                    if (folderSpeedKey.isNotBlank()) {
                        VideoFolderPlaybackSpeeds[folderSpeedKey] = safeSpeed
                        pendingFolderSpeedSave = folderSpeedKey to safeSpeed
                    }
                    player.setPlaybackSpeed(safeSpeed)
                },
                onResizeModeSelected = { mode ->
                    videoResizeMode = mode
                    resizeModeOverlay = mode.label
                },
                onToggleRepeat = {
                    val nextRepeat = !isRepeatOne
                    isRepeatOne = nextRepeat
                    player.repeatMode = if (nextRepeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                    showVideoPlayerToast(
                        context,
                        if (nextRepeat) "循环播放已开启" else "循环播放已关闭"
                    )
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
                onSubtitleClear = {
                    externalSubtitleUri = null
                    externalSubtitleName = null
                    subtitleOffsetMs = 0L
                    showVideoPlayerToast(context, "已清除外挂字幕")
                },
                onAudioDelayChange = { nextDelayMs ->
                    audioDelayMs = nextDelayMs.coerceIn(-5_000L, 5_000L)
                },
                onSleepTimerSelected = ::applySleepTimerOption,
                onSleepTimerPanelClosed = {
                    if (!isScreenLocked) {
                        showControls = true
                    }
                },
                onSetAbLoopStart = ::setAbLoopStart,
                onSetAbLoopEnd = ::setAbLoopEnd,
                onClearAbLoop = ::clearAbLoop,
                onScreenshot = ::captureScreenshot,
                isPortraitPlayback = isPortraitPlayback,
                onToggleOrientation = {
                    val nextPortrait = !isPortraitPlayback
                    isPortraitPlayback = nextPortrait
                    showVideoPlayerToast(
                        context,
                        if (nextPortrait) "已切换竖屏" else "已切换横屏"
                    )
                },
                onLockScreen = {
                    isScreenLocked = true
                    isQuickToolsExpanded = false
                    showLockedUnlockButton = true
                    showVideoPlayerToast(context, "已锁定")
                },
                onSpeedPanelVisibilityChange = { isSpeedPanelVisible = it },
                isQuickToolsExpanded = isQuickToolsExpanded,
                onQuickToolsExpandedChange = { isQuickToolsExpanded = it }
            )
        }

        if (isScreenLocked && showLockedUnlockButton) {
            VideoScreenLockButton(
                locked = true,
                onClick = {
                    isScreenLocked = false
                    showLockedUnlockButton = false
                    showControls = true
                    showVideoPlayerToast(context, "已解锁")
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 56.dp, bottom = 20.dp)
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
                    .background(PlayerMoonPurpleSoft, RoundedCornerShape(6.dp))
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
            tint = PlayerMoonPurpleSoft,
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
private fun VideoScreenLockButton(
    locked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = if (locked) 0.62f else 0.42f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (locked) Icons.Filled.LockOpen else Icons.Filled.Lock,
            contentDescription = if (locked) "解锁屏幕" else "锁定屏幕",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun VideoControlOverlay(
    title: String,
    currentPositionMs: Long,
    durationMs: Long,
    mediaFile: LocalMediaFile,
    player: ExoPlayer,
    navigationQueue: List<LocalMediaFile>,
    currentQueueUri: String,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onQueueItemSelected: (LocalMediaFile) -> Unit,
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
    subtitleName: String?,
    subtitleOffsetMs: Long,
    sleepTimerMode: VideoSleepTimerMode,
    sleepTimerSelectedOption: VideoSleepTimerOption,
    sleepTimerRemainingMs: Long,
    abLoopStartMs: Long?,
    abLoopEndMs: Long?,
    onSpeedSelected: (Float) -> Unit,
    onResizeModeSelected: (VideoResizeMode) -> Unit,
    onToggleRepeat: () -> Unit,
    onSubtitleSelect: () -> Unit,
    onSubtitleClear: () -> Unit,
    onAudioDelayChange: (Long) -> Unit,
    onSleepTimerSelected: (VideoSleepTimerOption) -> Unit,
    onSleepTimerPanelClosed: () -> Unit,
    onSetAbLoopStart: () -> Unit,
    onSetAbLoopEnd: () -> Unit,
    onClearAbLoop: () -> Unit,
    onScreenshot: () -> Unit,
    isPortraitPlayback: Boolean,
    onToggleOrientation: () -> Unit,
    onLockScreen: () -> Unit,
    onSpeedPanelVisibilityChange: (Boolean) -> Unit,
    isQuickToolsExpanded: Boolean,
    onQuickToolsExpandedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val quickToolsScrollState = rememberScrollState()
    var showSpeedPanel by remember { mutableStateOf(false) }
    var showResizePanel by remember { mutableStateOf(false) }
    var showSyncPanel by remember { mutableStateOf(false) }
    var showEqualizerPanel by remember { mutableStateOf(false) }
    var showInfoPanel by remember { mutableStateOf(false) }
    var showQueuePanel by remember { mutableStateOf(false) }
    var showAudioTrackPanel by remember { mutableStateOf(false) }
    var showSleepTimerPanel by remember { mutableStateOf(false) }
    var showAbLoopPanel by remember { mutableStateOf(false) }
    var audioTrackRevision by remember(player) { mutableIntStateOf(0) }
    var eqBass by remember { mutableFloatStateOf(0f) }
    var eqMid by remember { mutableFloatStateOf(0f) }
    var eqTreble by remember { mutableFloatStateOf(0f) }
    val audioTracks = remember(player, audioTrackRevision) { player.availableAudioTracks() }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                audioTrackRevision += 1
            }
        }
        player.addListener(listener)
        audioTrackRevision += 1
        onDispose { player.removeListener(listener) }
    }

    fun showFutureTool(feature: String) {
        showFutureToolToast(context, feature)
    }

    fun openSyncPanel() {
        showSpeedPanel = false
        showResizePanel = false
        showEqualizerPanel = false
        showInfoPanel = false
        showQueuePanel = false
        showAudioTrackPanel = false
        showSleepTimerPanel = false
        showAbLoopPanel = false
        onQuickToolsExpandedChange(false)
        showSyncPanel = true
    }

    fun openAudioTrackPanel() {
        showSpeedPanel = false
        showResizePanel = false
        showSyncPanel = false
        showEqualizerPanel = false
        showInfoPanel = false
        showQueuePanel = false
        showSleepTimerPanel = false
        showAbLoopPanel = false
        onQuickToolsExpandedChange(false)
        audioTrackRevision += 1
        showAudioTrackPanel = true
    }

    fun openSleepTimerPanel() {
        showSpeedPanel = false
        showResizePanel = false
        showSyncPanel = false
        showEqualizerPanel = false
        showInfoPanel = false
        showQueuePanel = false
        showAudioTrackPanel = false
        showAbLoopPanel = false
        onQuickToolsExpandedChange(false)
        showSleepTimerPanel = true
    }

    fun openAbLoopPanel() {
        showSpeedPanel = false
        showResizePanel = false
        showSyncPanel = false
        showEqualizerPanel = false
        showInfoPanel = false
        showQueuePanel = false
        showAudioTrackPanel = false
        showSleepTimerPanel = false
        onQuickToolsExpandedChange(false)
        showAbLoopPanel = true
    }

    fun openInfoPanel() {
        showSpeedPanel = false
        showResizePanel = false
        showSyncPanel = false
        showEqualizerPanel = false
        showQueuePanel = false
        showAudioTrackPanel = false
        showSleepTimerPanel = false
        showAbLoopPanel = false
        onQuickToolsExpandedChange(false)
        showInfoPanel = true
    }

    fun openQueuePanel() {
        showSpeedPanel = false
        showResizePanel = false
        showSyncPanel = false
        showEqualizerPanel = false
        showInfoPanel = false
        showAudioTrackPanel = false
        showSleepTimerPanel = false
        showAbLoopPanel = false
        onQuickToolsExpandedChange(false)
        showQueuePanel = true
    }

    fun closeSyncPanel() {
        showSyncPanel = false
    }

    fun closeSleepTimerPanel() {
        showSleepTimerPanel = false
        onSleepTimerPanelClosed()
    }

    BackHandler(enabled = showSpeedPanel || showSyncPanel || showInfoPanel || showQueuePanel || showAudioTrackPanel || showSleepTimerPanel || showAbLoopPanel) {
        if (showSyncPanel) {
            closeSyncPanel()
        } else if (showAudioTrackPanel) {
            showAudioTrackPanel = false
        } else if (showSleepTimerPanel) {
            closeSleepTimerPanel()
        } else if (showAbLoopPanel) {
            showAbLoopPanel = false
        } else if (showInfoPanel) {
            showInfoPanel = false
        } else if (showQueuePanel) {
            showQueuePanel = false
        } else {
            showSpeedPanel = false
        }
    }

    LaunchedEffect(showSpeedPanel, showSyncPanel, showInfoPanel, showQueuePanel, showAudioTrackPanel, showSleepTimerPanel, showAbLoopPanel) {
        onSpeedPanelVisibilityChange(showSpeedPanel || showSyncPanel || showInfoPanel || showQueuePanel || showAudioTrackPanel || showSleepTimerPanel || showAbLoopPanel)
    }

    DisposableEffect(Unit) {
        onDispose {
            onSpeedPanelVisibilityChange(false)
        }
    }

    fun closePanelOrBack() {
        when {
            showSyncPanel -> closeSyncPanel()
            showEqualizerPanel -> showEqualizerPanel = false
            showAudioTrackPanel -> showAudioTrackPanel = false
            showSleepTimerPanel -> closeSleepTimerPanel()
            showAbLoopPanel -> showAbLoopPanel = false
            showInfoPanel -> showInfoPanel = false
            showQueuePanel -> showQueuePanel = false
            showSpeedPanel -> showSpeedPanel = false
            showResizePanel -> showResizePanel = false
            isQuickToolsExpanded -> onQuickToolsExpandedChange(false)
            else -> onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(showSyncPanel, showEqualizerPanel, showAudioTrackPanel, showSleepTimerPanel, showAbLoopPanel, showInfoPanel, showQueuePanel, showSpeedPanel, showResizePanel, isQuickToolsExpanded) {
                detectDragGestures { change, dragAmount ->
                    if (change.position.x > size.width - 72.dp.toPx() && dragAmount.x < -26f) {
                        closePanelOrBack()
                        change.consume()
                    }
                }
            }
    ) {
        val ordinaryControlsVisible = !showSpeedPanel && !showSyncPanel && !showAudioTrackPanel && !showSleepTimerPanel && !showAbLoopPanel && !showInfoPanel && !showQueuePanel

        if (ordinaryControlsVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.50f),
                                Color.Black.copy(alpha = 0.18f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(start = 18.dp, end = 20.dp, top = 6.dp, bottom = 16.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(42.dp)
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White,
                        modifier = Modifier.size(29.dp)
                    )
                }
                Text(
                    text = title,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 252.dp, vertical = 8.dp),
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Row(
                    modifier = Modifier.align(Alignment.TopEnd),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { openAudioTrackPanel() }
                            .padding(horizontal = 5.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Filled.Audiotrack,
                            contentDescription = "音轨",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Text("音轨", color = Color.White.copy(alpha = 0.88f), fontSize = 13.sp)
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { openSyncPanel() }
                            .padding(horizontal = 5.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Filled.Subtitles,
                            contentDescription = "字幕同步",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Text("字幕", color = Color.White.copy(alpha = 0.88f), fontSize = 13.sp)
                    }
                    Box {
                        IconButton(
                            onClick = { onQuickToolsExpandedChange(!isQuickToolsExpanded) },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "更多",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showSpeedPanel) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showSpeedPanel = false }
            )
            VideoSpeedPanel(
                speed = playbackSpeed,
                onDismiss = { showSpeedPanel = false },
                onSpeedChange = onSpeedSelected,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 82.dp, end = 82.dp, bottom = 22.dp)
            )
        }

        if (showResizePanel) {
            VideoChoicePanel(
                title = "画面比例",
                options = VideoResizeModes.map { it.label },
                selected = resizeMode.label,
                onDismiss = { showResizePanel = false },
                onSelect = { option ->
                    VideoResizeModes.firstOrNull { it.label == option }?.let(onResizeModeSelected)
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 56.dp, end = 20.dp)
            )
        }

        if (showSyncPanel) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { closeSyncPanel() }
            )
            SyncAdjustmentPanel(
                subtitleName = subtitleName,
                subtitleOffsetMs = subtitleOffsetMs,
                audioDelayMs = audioDelayMs,
                onDismiss = { closeSyncPanel() },
                onPickSubtitle = onSubtitleSelect,
                onClearSubtitle = onSubtitleClear,
                onAudioTrackOpen = {
                    closeSyncPanel()
                    openAudioTrackPanel()
                },
                onAudioDelayRequest = {
                    onAudioDelayChange(audioDelayMs)
                    showFutureTool("音频同步")
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 56.dp, end = 20.dp)
            )
        }

        if (showAudioTrackPanel) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showAudioTrackPanel = false }
            )
            AudioTrackPanel(
                tracks = audioTracks,
                onSelect = { track ->
                    val switched = player.selectAudioTrack(track)
                    if (switched) {
                        audioTrackRevision += 1
                    } else {
                        showVideoPlayerToast(context, "音轨切换失败", ERROR_HINT_MS)
                    }
                },
                onDismiss = { showAudioTrackPanel = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 56.dp, end = 20.dp)
            )
        }

        if (showSleepTimerPanel) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { closeSleepTimerPanel() }
            )
            SleepTimerPanel(
                mode = sleepTimerMode,
                selectedOption = sleepTimerSelectedOption,
                remainingMs = sleepTimerRemainingMs,
                onSelect = onSleepTimerSelected,
                onDismiss = { closeSleepTimerPanel() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 14.dp, bottom = 12.dp)
            )
        }

        if (showAbLoopPanel) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showAbLoopPanel = false }
            )
            AbLoopPanel(
                startMs = abLoopStartMs,
                endMs = abLoopEndMs,
                isActive = abLoopStartMs != null && abLoopEndMs != null && abLoopEndMs > abLoopStartMs,
                onSetStart = onSetAbLoopStart,
                onSetEnd = onSetAbLoopEnd,
                onClear = onClearAbLoop,
                onDismiss = { showAbLoopPanel = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 14.dp, bottom = 12.dp)
            )
        }

        if (showInfoPanel) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showInfoPanel = false }
            )
            VideoInfoPanel(
                mediaFile = mediaFile,
                durationMs = durationMs,
                currentPositionMs = currentPositionMs,
                playbackSpeed = playbackSpeed,
                resizeMode = resizeMode,
                onDismiss = { showInfoPanel = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 56.dp, end = 20.dp)
            )
        }

        if (showQueuePanel) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showQueuePanel = false }
            )
            VideoQueuePanel(
                queue = navigationQueue,
                currentUri = currentQueueUri,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                onSelect = { file ->
                    showQueuePanel = false
                    if (file.uri != currentQueueUri) {
                        onQueueItemSelected(file)
                    }
                },
                onDismiss = { showQueuePanel = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 56.dp, end = 20.dp)
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
                    .padding(top = 56.dp, end = 20.dp)
            )
        }

        if (ordinaryControlsVisible) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 38.dp, end = 16.dp, top = 70.dp)
                    .horizontalScroll(quickToolsScrollState),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VideoQuickToolButton(
                    icon = Icons.Filled.Tune,
                    label = "均衡器",
                    isFuture = true,
                    onClick = { showFutureTool("均衡器") }
                )
                VideoQuickToolButton(
                    icon = Icons.Filled.Speed,
                    label = "${playbackSpeed.formatSpeed()}x",
                    onClick = {
                        showSpeedPanel = true
                    }
                )
                VideoQuickToolButton(
                    icon = Icons.Filled.ScreenRotation,
                    label = if (isPortraitPlayback) "横屏" else "竖屏",
                    onClick = {
                        onToggleOrientation()
                    }
                )
                VideoQuickToolButton(
                    icon = Icons.Filled.PhotoCamera,
                    label = "截图",
                    onClick = {
                        onScreenshot()
                    }
                )
                if (isQuickToolsExpanded) {
                    VideoQuickToolButton(
                        icon = Icons.Filled.Settings,
                        label = "解码",
                        isFuture = true,
                        onClick = {
                            showFutureTool("解码方式")
                        }
                    )
                    VideoQuickToolButton(
                        icon = Icons.Filled.Audiotrack,
                        label = "音轨",
                        onClick = { openAudioTrackPanel() }
                    )
                    VideoQuickToolButton(
                        icon = Icons.Filled.Tune,
                        label = "画面调节",
                        isFuture = true,
                        onClick = { showFutureTool("画面调节") }
                    )
                    VideoQuickToolButton(
                        icon = Icons.Filled.Settings,
                        label = "信息",
                        onClick = { openInfoPanel() }
                    )
                    VideoQuickToolButton(
                        icon = Icons.Filled.Settings,
                        label = "播放列表",
                        onClick = { openQueuePanel() }
                    )
                    VideoQuickToolButton(
                        icon = Icons.Filled.Loop,
                        label = "AB",
                        onClick = { openAbLoopPanel() }
                    )
                    VideoQuickToolButton(
                        icon = Icons.Filled.Settings,
                        label = "睡眠",
                        onClick = { openSleepTimerPanel() }
                    )
                    VideoQuickToolButton(
                        icon = Icons.Filled.Tune,
                        label = "手势",
                        isFuture = true,
                        onClick = { showFutureTool("手势设置") }
                    )
                    VideoQuickToolButton(
                        icon = Icons.Filled.Settings,
                        label = "控制栏",
                        isFuture = true,
                        onClick = { showFutureTool("控制栏设置") }
                    )
                    VideoQuickToolButton(
                        icon = Icons.Filled.KeyboardArrowLeft,
                        label = "收起",
                        onClick = { onQuickToolsExpandedChange(false) }
                    )
                } else {
                    VideoQuickToolButton(
                        icon = Icons.Filled.KeyboardArrowRight,
                        label = "展开更多工具",
                        onClick = { onQuickToolsExpandedChange(true) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.20f),
                                Color.Black.copy(alpha = 0.58f)
                            )
                        )
                    )
                    .padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        formatDuration(currentPositionMs),
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    ThinVideoProgressBar(
                        currentPositionMs = currentPositionMs,
                        durationMs = durationMs,
                        onSeekStart = onSeekStart,
                        onSeekPreview = onSeekPreview,
                        onSeekFinished = onSeekFinished,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        formatDuration(durationMs),
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 34.dp, end = 30.dp)
                ) {
                    IconButton(
                        onClick = onLockScreen,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(42.dp)
                    ) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = "锁定屏幕",
                            tint = Color.White.copy(alpha = 0.88f),
                            modifier = Modifier.size(25.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(236.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onPrevious, modifier = Modifier.size(42.dp)) {
                            Icon(
                                Icons.Filled.SkipPrevious,
                                contentDescription = "上一集",
                                tint = Color.White.copy(alpha = 0.72f),
                                modifier = Modifier.size(29.dp)
                            )
                        }
                        IconButton(
                            onClick = onPlayPause,
                            modifier = Modifier
                                .size(56.dp)
                                .border(2.dp, PlayerMoonPurple, CircleShape)
                                .background(Color.Black.copy(alpha = 0.18f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "暂停" else "播放",
                                tint = Color.White,
                                modifier = Modifier.size(33.dp)
                            )
                        }
                        IconButton(onClick = onNext, modifier = Modifier.size(42.dp)) {
                            Icon(
                                Icons.Filled.SkipNext,
                                contentDescription = "下一集",
                                tint = Color.White.copy(alpha = 0.72f),
                                modifier = Modifier.size(29.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = { onResizeModeSelected(resizeMode.next()) },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(42.dp)
                    ) {
                        Icon(
                            Icons.Filled.AspectRatio,
                            contentDescription = "适应屏幕",
                            tint = Color.White.copy(alpha = 0.88f),
                            modifier = Modifier.size(25.dp)
                        )
                    }
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
        VideoToolAction("解码", "后续", Icons.Filled.Settings, enabled = false),
        VideoToolAction("倍速", "${playbackSpeed.formatSpeed()}x", Icons.Filled.Speed),
        VideoToolAction("循环", if (isRepeatOne) "开启" else "关闭", Icons.Filled.Loop),
        VideoToolAction("截图", "可用", Icons.Filled.PhotoCamera)
    )
    Column(
        modifier = modifier
            .width(244.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(PlayerPanelDark)
            .border(1.dp, PlayerPanelStroke, RoundedCornerShape(22.dp))
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
                        enabled = tool.enabled,
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
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val itemAlpha = if (enabled) 1f else 0.48f
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
                .background(
                    if (enabled) Color(0xFF151E2B) else Color(0xFF11151D),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) PlayerMoonPurpleSoft else Color.White.copy(alpha = 0.42f),
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = value?.let { "$label\n$it" } ?: label,
            color = Color.White.copy(alpha = 0.86f * itemAlpha),
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
    isFuture: Boolean = false,
    onClick: () -> Unit
) {
    val contentAlpha = if (isFuture) 0.58f else 1f
    val displayLabel = when {
        label == "展开更多工具" -> "展开"
        label == "画面调节" -> "画调"
        label == "睡眠定时" -> "睡眠"
        label == "控制栏设置" -> "控制"
        label == "控制栏" -> "控制"
        else -> label
    }
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = if (isFuture) "$label 后续实现" else label,
                tint = Color.White.copy(alpha = contentAlpha),
                modifier = Modifier.size(if (isFuture) 18.dp else 19.dp)
            )
            Text(
                text = displayLabel,
                color = Color.White.copy(alpha = if (isFuture) 0.52f else 0.78f),
                fontSize = 9.sp,
                lineHeight = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            if (isFuture) {
                Text(
                    text = "后续",
                    color = Color.White.copy(alpha = 0.38f),
                    fontSize = 7.sp,
                    lineHeight = 7.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SyncAdjustmentPanel(
    subtitleName: String?,
    subtitleOffsetMs: Long,
    audioDelayMs: Long,
    onDismiss: () -> Unit,
    onPickSubtitle: () -> Unit,
    onClearSubtitle: () -> Unit,
    onAudioTrackOpen: () -> Unit,
    onAudioDelayRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasSubtitle = subtitleName != null
    val subtitleOffsetAvailable = false
    SidePanelShell(
        title = "同步调节",
        onDismiss = onDismiss,
        modifier = modifier
            .width(360.dp)
            .fillMaxHeight(PLAYER_SIDE_PANEL_HEIGHT_FRACTION)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SyncSectionTitle("字幕同步")
            Text(
                text = "当前字幕：${subtitleName ?: "未加载"}",
                color = Color.White.copy(alpha = 0.66f),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitleSyncStatus(subtitleOffsetMs),
                color = Color.White.copy(alpha = 0.78f),
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                SyncStepButton("提前0.5s", hasSubtitle && subtitleOffsetAvailable) {}
                SyncStepButton("提前0.1s", hasSubtitle && subtitleOffsetAvailable) {}
                SyncStepButton("重置", hasSubtitle && subtitleOffsetAvailable) {}
                SyncStepButton("延后0.1s", hasSubtitle && subtitleOffsetAvailable) {}
                SyncStepButton("延后0.5s", hasSubtitle && subtitleOffsetAvailable) {}
            }
            Text(
                text = if (hasSubtitle) {
                    "当前版本暂不支持真实调整字幕显示时间，字幕可正常加载显示。"
                } else {
                    "请先加载外挂字幕"
                },
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelSmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SyncActionButton("选择外挂字幕", enabled = true, onClick = onPickSubtitle)
                SyncActionButton("清除字幕", enabled = hasSubtitle, onClick = onClearSubtitle)
            }
            SyncSectionTitle("字幕后续")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SyncActionButton("字幕样式", enabled = true, isFuture = true) {
                        showFutureToolToast(context, "字幕样式")
                    }
                    SyncActionButton("字幕大小", enabled = true, isFuture = true) {
                        showFutureToolToast(context, "字幕大小")
                    }
                    SyncActionButton("字幕位置", enabled = true, isFuture = true) {
                        showFutureToolToast(context, "字幕位置")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SyncActionButton("字幕速度", enabled = true, isFuture = true) {
                        showFutureToolToast(context, "字幕速度")
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            )

            SyncSectionTitle("音频同步")
            Text(
                text = "当前延迟 ${formatSignedDelay(audioDelayMs)}",
                color = Color.White.copy(alpha = 0.56f),
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SyncStepButton("提前0.1s", enabled = false, onClick = onAudioDelayRequest)
                SyncStepButton("重置", enabled = false, onClick = onAudioDelayRequest)
                SyncStepButton("延后0.1s", enabled = false, onClick = onAudioDelayRequest)
            }
            Text(
                text = "当前版本暂不支持调整音频与画面延迟，音频同步调节后续实现。",
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelSmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SyncActionButton("音轨切换", enabled = true, onClick = onAudioTrackOpen)
            }
        }
    }
}

@Composable
private fun AudioTrackPanel(
    tracks: List<VideoAudioTrackOption>,
    onSelect: (VideoAudioTrackOption) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    SidePanelShell(
        title = "音轨",
        onDismiss = onDismiss,
        modifier = modifier
            .width(340.dp)
            .fillMaxHeight(PLAYER_SIDE_PANEL_HEIGHT_FRACTION)
    ) {
        when {
            tracks.isEmpty() -> {
                Text(
                    text = "未检测到可用音轨",
                    color = Color.White.copy(alpha = 0.68f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.045f))
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                )
            }

            tracks.size == 1 -> {
                Text(
                    text = "当前视频只有一个音轨",
                    color = Color.White.copy(alpha = 0.62f),
                    style = MaterialTheme.typography.labelMedium
                )
                AudioTrackRow(
                    track = tracks.first(),
                    onClick = {},
                    enabled = false
                )
            }

            else -> {
                Text(
                    text = "共 ${tracks.size} 条音轨，点击可切换",
                    color = Color.White.copy(alpha = 0.62f),
                    style = MaterialTheme.typography.labelMedium
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    itemsIndexed(tracks, key = { _, track -> track.id }) { _, track ->
                        AudioTrackRow(
                            track = track,
                            enabled = track.isSupported,
                            onClick = { onSelect(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioTrackRow(
    track: VideoAudioTrackOption,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val isCurrent = track.isSelected
    val backgroundColor = when {
        isCurrent -> PlayerMoonPurple.copy(alpha = 0.10f)
        enabled -> Color.White.copy(alpha = 0.035f)
        else -> Color.White.copy(alpha = 0.020f)
    }
    val titleColor = when {
        isCurrent -> Color.White.copy(alpha = 0.94f)
        enabled -> Color.White.copy(alpha = 0.82f)
        else -> Color.White.copy(alpha = 0.44f)
    }
    val detailColor = when {
        isCurrent -> PlayerMoonPurpleSoft
        enabled -> Color.White.copy(alpha = 0.54f)
        else -> Color.White.copy(alpha = 0.34f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = if (isCurrent) PlayerMoonPurpleSoft.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.035f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(enabled = enabled && !isCurrent, onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(999.dp))
                .background(if (isCurrent) PlayerMoonPurpleSoft else Color.Transparent)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "音轨 ${track.displayIndex}",
                color = titleColor,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.detailText,
                color = detailColor,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isCurrent) {
            Text(
                text = "当前",
                color = PlayerMoonPurpleSoft,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(PlayerMoonPurple.copy(alpha = 0.12f))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                maxLines = 1
            )
        } else if (!enabled) {
            Text(
                text = "不可用",
                color = Color.White.copy(alpha = 0.36f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AbLoopPanel(
    startMs: Long?,
    endMs: Long?,
    isActive: Boolean,
    onSetStart: () -> Unit,
    onSetEnd: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    SidePanelShell(
        title = "AB 循环",
        onDismiss = onDismiss,
        modifier = modifier
            .width(390.dp)
            .fillMaxHeight(PLAYER_SIDE_PANEL_HEIGHT_FRACTION),
        contentPadding = 14.dp,
        contentSpacing = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isActive) PlayerMoonPurple.copy(alpha = 0.10f)
                        else Color.White.copy(alpha = 0.040f)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(if (isActive) PlayerMoonPurpleSoft else Color.White.copy(alpha = 0.20f))
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "当前状态",
                        color = Color.White.copy(alpha = 0.56f),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                    Text(
                        text = if (isActive) "循环中" else "未开启",
                        color = if (isActive) PlayerMoonPurpleSoft else Color.White.copy(alpha = 0.86f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.035f)),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                AbLoopPointRow(label = "A", value = startMs?.let(::formatDuration) ?: "未设置")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.050f))
                )
                AbLoopPointRow(label = "B", value = endMs?.let(::formatDuration) ?: "未设置")
            }

            AbLoopButton(
                text = "设置 A 点",
                primary = true,
                onClick = onSetStart
            )
            AbLoopButton(
                text = "设置 B 点",
                primary = true,
                onClick = onSetEnd
            )
            AbLoopButton(
                text = "清除 AB 循环",
                primary = false,
                onClick = onClear
            )
            Text(
                text = "A/B 点仅对当前视频生效，播放到 B 点后会回到 A 点继续播放。",
                color = Color.White.copy(alpha = 0.48f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun AbLoopPointRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            color = PlayerMoonPurpleSoft,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(20.dp),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            text = value,
            color = Color.White.copy(alpha = 0.88f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AbLoopButton(
    text: String,
    primary: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(
                if (primary) PlayerMoonPurple.copy(alpha = 0.15f)
                else Color.White.copy(alpha = 0.040f)
            )
            .border(
                width = 1.dp,
                color = if (primary) PlayerMoonPurpleSoft.copy(alpha = 0.48f) else Color.White.copy(alpha = 0.070f),
                shape = RoundedCornerShape(11.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (primary) PlayerMoonPurpleSoft else Color.White.copy(alpha = 0.82f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun SleepTimerPanel(
    mode: VideoSleepTimerMode,
    selectedOption: VideoSleepTimerOption,
    remainingMs: Long,
    onSelect: (VideoSleepTimerOption) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var customMinutes by remember { mutableIntStateOf(45) }
    val customOption = remember(customMinutes) {
        VideoSleepTimerOption(
            label = "${customMinutes} 分钟",
            displayLabel = "${customMinutes} 分钟",
            mode = VideoSleepTimerMode.TIMED,
            durationMs = customMinutes * 60 * 1000L,
            isCustom = true
        )
    }
    val isTimerActive = mode != VideoSleepTimerMode.OFF
    val customRowLabel = when (mode) {
        VideoSleepTimerMode.OFF -> "自定义时长"
        VideoSleepTimerMode.TIMED -> "剩余时间"
        VideoSleepTimerMode.END_OF_VIDEO -> "剩余时间"
    }
    val customRowValue = when (mode) {
        VideoSleepTimerMode.OFF -> "${customMinutes} 分钟"
        VideoSleepTimerMode.TIMED -> formatSleepTimerCountdown(remainingMs)
        VideoSleepTimerMode.END_OF_VIDEO -> "本集结束后"
    }

    SidePanelShell(
        title = "睡眠定时",
        onDismiss = onDismiss,
        modifier = modifier
            .width(430.dp)
            .fillMaxHeight(PLAYER_SIDE_PANEL_HEIGHT_FRACTION),
        contentPadding = 12.dp,
        contentSpacing = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SleepTimerSectionTitle("快捷时长")
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VideoSleepTimerFixedOptions.chunked(3).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowOptions.forEach { option ->
                            SleepTimerChip(
                                option = option,
                                selected = option == selectedOption,
                                onClick = { onSelect(option) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            SleepTimerSectionTitle("自定义时长")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.040f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = customRowLabel,
                    color = Color.White.copy(alpha = 0.62f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                SleepTimerStepButton(
                    label = "−",
                    enabled = !isTimerActive && customMinutes > VIDEO_SLEEP_TIMER_MIN_MINUTES,
                    onClick = {
                        customMinutes = (customMinutes - VIDEO_SLEEP_TIMER_STEP_MINUTES)
                            .coerceAtLeast(VIDEO_SLEEP_TIMER_MIN_MINUTES)
                    }
                )
                Text(
                    text = customRowValue,
                    color = if (isTimerActive) PlayerMoonPurpleSoft else Color.White.copy(alpha = 0.92f),
                    fontSize = if (mode == VideoSleepTimerMode.TIMED) 20.sp else 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                SleepTimerStepButton(
                    label = "+",
                    enabled = !isTimerActive && customMinutes < VIDEO_SLEEP_TIMER_MAX_MINUTES,
                    onClick = {
                        customMinutes = (customMinutes + VIDEO_SLEEP_TIMER_STEP_MINUTES)
                            .coerceAtMost(VIDEO_SLEEP_TIMER_MAX_MINUTES)
                    }
                )
            }
            Text(
                text = "每次 5 分钟 · 5-180 分钟",
                color = Color.White.copy(alpha = 0.48f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            SleepTimerPrimaryButton(
                text = "开始定时",
                onClick = { onSelect(customOption) }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.030f)),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                SleepTimerActionRow(
                    option = VideoSleepTimerOption.EndOfVideo,
                    selected = selectedOption == VideoSleepTimerOption.EndOfVideo,
                    onClick = { onSelect(VideoSleepTimerOption.EndOfVideo) }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.050f))
                )
                SleepTimerActionRow(
                    option = VideoSleepTimerOption.Off,
                    selected = selectedOption == VideoSleepTimerOption.Off,
                    onClick = { onSelect(VideoSleepTimerOption.Off) }
                )
            }
        }
    }
}

@Composable
private fun SleepTimerSectionTitle(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.60f),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 2.dp)
    )
}

@Composable
private fun SleepTimerChip(
    option: VideoSleepTimerOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(
                if (selected) PlayerMoonPurple.copy(alpha = 0.15f)
                else Color.White.copy(alpha = 0.045f)
            )
            .border(
                width = 1.dp,
                color = if (selected) PlayerMoonPurpleSoft.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.070f),
                shape = RoundedCornerShape(11.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = option.displayLabel,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.80f),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SleepTimerStepButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 0.090f else 0.035f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = if (enabled) 0.86f else 0.32f),
            fontSize = 24.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SleepTimerPrimaryButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(PlayerMoonPurple.copy(alpha = 0.18f))
            .border(
                width = 1.dp,
                color = PlayerMoonPurpleSoft.copy(alpha = 0.56f),
                shape = RoundedCornerShape(11.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = PlayerMoonPurpleSoft,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SleepTimerActionRow(
    option: VideoSleepTimerOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    if (selected) PlayerMoonPurple.copy(alpha = 0.18f)
                    else Color.White.copy(alpha = 0.055f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (selected) PlayerMoonPurpleSoft else Color.White.copy(alpha = 0.30f))
            )
        }
        Text(
            text = option.label,
            color = Color.White.copy(alpha = if (selected) 0.92f else 0.78f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "›",
            color = Color.White.copy(alpha = 0.42f),
            fontSize = 22.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SyncSectionTitle(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.9f),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun SyncStepButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = text,
        color = if (enabled) PlayerMoonPurpleSoft else Color.White.copy(alpha = 0.38f),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = if (enabled) 0.08f else 0.04f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1
    )
}

@Composable
private fun SyncActionButton(
    text: String,
    enabled: Boolean,
    isFuture: Boolean = false,
    onClick: () -> Unit
) {
    val textColor = when {
        !enabled -> Color.White.copy(alpha = 0.34f)
        isFuture -> Color.White.copy(alpha = 0.58f)
        else -> PlayerMoonPurpleSoft
    }
    val backgroundAlpha = when {
        !enabled -> 0.04f
        isFuture -> 0.055f
        else -> 0.09f
    }
    Text(
        text = text,
        color = textColor,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = backgroundAlpha))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 9.dp),
        style = MaterialTheme.typography.labelMedium
    )
}

@Composable
private fun VideoInfoPanel(
    mediaFile: LocalMediaFile,
    durationMs: Long,
    currentPositionMs: Long,
    playbackSpeed: Float,
    resizeMode: VideoResizeMode,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var infoMetadata by remember(mediaFile.uri) { mutableStateOf<VideoInfoMetadata?>(null) }

    LaunchedEffect(mediaFile.uri) {
        infoMetadata = withContext(Dispatchers.IO) {
            loadVideoInfoMetadata(context.applicationContext, mediaFile)
        }
    }

    val effectiveDurationMs = durationMs.takeIf { it > 0L }
        ?: infoMetadata?.durationMs?.takeIf { it > 0L }
    val formatText = infoMetadata?.mimeType
        ?: mediaFile.extension.trim().takeIf { it.isNotBlank() }?.uppercase(Locale.US)
        ?: "未知"

    SidePanelShell(
        title = "视频信息",
        onDismiss = onDismiss,
        modifier = modifier
            .width(360.dp)
            .fillMaxHeight(PLAYER_SIDE_PANEL_HEIGHT_FRACTION)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            VideoInfoRow("文件名", mediaFile.name.ifBlank { "未知" })
            VideoInfoRow("文件大小", formatFileSize(mediaFile.size.coerceAtLeast(0L)))
            VideoInfoRow("视频时长", effectiveDurationMs?.let { formatDuration(it) } ?: "未知")
            VideoInfoRow(
                "当前进度",
                if (effectiveDurationMs != null) {
                    "${formatDuration(currentPositionMs)} / ${formatDuration(effectiveDurationMs)}"
                } else {
                    formatDuration(currentPositionMs)
                }
            )
            VideoInfoRow("文件夹", mediaFile.parentFolderName?.takeIf { it.isNotBlank() } ?: "未知")
            VideoInfoRow("URI / 路径", mediaFile.uri.ifBlank { "未知" }, maxValueLines = 2)
            VideoInfoRow("倍速", "${playbackSpeed.normalizeVideoSpeed().formatSpeed()}x")
            VideoInfoRow("画面比例", resizeMode.label)
            VideoInfoRow("分辨率", infoMetadata?.resolution ?: "未知")
            VideoInfoRow("格式", formatText)
            VideoInfoRow(
                "最后修改",
                infoMetadata?.lastModifiedMs
                    ?.takeIf { it > 0L }
                    ?.let { formatVideoInfoDate(it) }
                    ?: "未知"
            )
        }
    }
}

@Composable
private fun VideoInfoRow(
    label: String,
    value: String,
    maxValueLines: Int = 1
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.045f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.58f),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(76.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = Color.White.copy(alpha = 0.88f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            maxLines = maxValueLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VideoQueuePanel(
    queue: List<LocalMediaFile>,
    currentUri: String,
    currentPositionMs: Long,
    durationMs: Long,
    onSelect: (LocalMediaFile) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentFile = queue.firstOrNull { it.uri == currentUri }
    val folderName = currentFile?.parentFolderName?.takeIf { it.isNotBlank() }
        ?: queue.firstOrNull()?.parentFolderName?.takeIf { it.isNotBlank() }
        ?: "未知文件夹"
    val currentIndex = queue.indexOfFirst { it.uri == currentUri }

    Column(
        modifier = modifier
            .fillMaxWidth(0.36f)
            .widthIn(min = 292.dp, max = 400.dp)
            .fillMaxHeight(PLAYER_SIDE_PANEL_HEIGHT_FRACTION)
            .clip(RoundedCornerShape(13.dp))
            .background(PlayerPanelDark.copy(alpha = 0.86f))
            .border(1.dp, Color.White.copy(alpha = 0.055f), RoundedCornerShape(13.dp))
            .clickable(onClick = {})
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "播放列表",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "关闭播放列表",
                    tint = Color.White.copy(alpha = 0.64f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.020f))
                .padding(horizontal = 9.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = folderName,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "共 ${queue.size} 个视频" + if (currentIndex >= 0) " · 当前第 ${currentIndex + 1} 个" else "",
                color = Color.White.copy(alpha = 0.58f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (queue.isEmpty()) {
                item {
                    Text(
                        text = "当前没有可展示的播放列表",
                        color = Color.White.copy(alpha = 0.62f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.028f))
                            .padding(horizontal = 9.dp, vertical = 9.dp)
                    )
                }
            } else {
                itemsIndexed(queue, key = { _, file -> file.uri }) { index, file ->
                    VideoQueueItem(
                        index = index,
                        file = file,
                        isCurrent = file.uri == currentUri,
                        currentPositionMs = currentPositionMs,
                        durationMs = durationMs,
                        onClick = { onSelect(file) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoQueueItem(
    index: Int,
    file: LocalMediaFile,
    isCurrent: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    onClick: () -> Unit
) {
    val progressText = if (isCurrent) {
        val current = formatDuration(currentPositionMs)
        val total = durationMs.takeIf { it > 0L }?.let { formatDuration(it) }
        total?.let { "$current / $it" } ?: current
    } else {
        "时长未知"
    }
    val detailText = listOfNotNull(
        formatFileSize(file.size.coerceAtLeast(0L)).takeIf { it.isNotBlank() },
        progressText
    ).joinToString(" · ")
    val backgroundColor = if (isCurrent) {
        PlayerMoonPurple.copy(alpha = 0.085f)
    } else {
        Color.White.copy(alpha = 0.024f)
    }
    val borderColor = if (isCurrent) {
        PlayerMoonPurpleSoft.copy(alpha = 0.16f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(999.dp))
                .background(if (isCurrent) PlayerMoonPurpleSoft else Color.Transparent)
        )
        Text(
            text = (index + 1).toString().padStart(2, '0'),
            color = if (isCurrent) PlayerMoonPurpleSoft else Color.White.copy(alpha = 0.42f),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(22.dp),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = file.name.ifBlank { "未知视频" },
                color = Color.White.copy(alpha = if (isCurrent) 0.94f else 0.82f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = detailText,
                color = if (isCurrent) PlayerMoonPurpleSoft else Color.White.copy(alpha = 0.52f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isCurrent) {
            Text(
                text = "正在播放",
                color = PlayerMoonPurpleSoft,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(PlayerMoonPurple.copy(alpha = 0.10f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                maxLines = 1
            )
        }
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
    val context = LocalContext.current
    var draftSpeed by remember(speed) { mutableFloatStateOf(speed.normalizeVideoSpeed()) }
    var showInputDialog by remember { mutableStateOf(false) }
    var inputText by remember(draftSpeed) { mutableStateOf(draftSpeed.formatSpeed()) }
    val speedInputPattern = remember { Regex("""^\d{0,2}(\.\d{0,2})?$""") }
    val keySpeeds = listOf(1f, 2f, 3f, 4f, 5f)

    fun applySpeed(nextSpeed: Float) {
        val normalized = nextSpeed.normalizeVideoSpeed()
        draftSpeed = normalized
        onSpeedChange(normalized)
    }

    Column(
        modifier = modifier
            .fillMaxWidth(0.82f)
            .clip(RoundedCornerShape(18.dp))
            .background(PlayerPanelDark)
            .border(1.dp, PlayerPanelStroke, RoundedCornerShape(18.dp))
            .clickable(onClick = {})
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "播放速度",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "关闭",
                    tint = Color.White.copy(alpha = 0.82f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            VideoSpeedStepButton(text = "-", onStep = { applySpeed(draftSpeed - VIDEO_SPEED_STEP) })

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable {
                        inputText = draftSpeed.formatSpeed()
                        showInputDialog = true
                    }
                    .padding(horizontal = 18.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${draftSpeed.formatSpeed()}x",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "编辑",
                    color = PlayerMoonPurpleSoft,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            VideoSpeedStepButton(text = "+", onStep = { applySpeed(draftSpeed + VIDEO_SPEED_STEP) })
        }

        VideoSpeedSlider(
            speed = draftSpeed,
            keySpeeds = keySpeeds,
            onSpeedPreview = { previewSpeed ->
                draftSpeed = previewSpeed
                onSpeedChange(previewSpeed)
            },
            onSpeedCommit = ::applySpeed,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
        )
    }

    if (showInputDialog) {
        AlertDialog(
            onDismissRequest = { showInputDialog = false },
            title = { Text("输入播放速度") },
            text = {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { next ->
                        val cleaned = next.trim().removeSuffix("x").removeSuffix("X")
                        if (cleaned.isEmpty() || speedInputPattern.matches(cleaned)) {
                            inputText = cleaned
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = { Text("0.25 - 5") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsed = inputText.trim().removeSuffix("x").removeSuffix("X").toFloatOrNull()
                        if (parsed == null) {
                            showVideoPlayerToast(context, "请输入有效倍速", ERROR_HINT_MS)
                        } else {
                            applySpeed(parsed)
                            showInputDialog = false
                        }
                    }
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInputDialog = false }) {
                    Text("取消")
                }
            },
            containerColor = Color(0xFF101722),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}

@Composable
private fun VideoSpeedSlider(
    speed: Float,
    keySpeeds: List<Float>,
    onSpeedPreview: (Float) -> Unit,
    onSpeedCommit: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    fun speedToFraction(value: Float): Float {
        return ((value.normalizeVideoSpeed() - MIN_VIDEO_SPEED) / (MAX_VIDEO_SPEED - MIN_VIDEO_SPEED))
            .coerceIn(0f, 1f)
    }

    fun xToSpeed(x: Float, width: Int): Float {
        val fraction = (x / width.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
        return (MIN_VIDEO_SPEED + fraction * (MAX_VIDEO_SPEED - MIN_VIDEO_SPEED)).normalizeVideoSpeed()
    }

    BoxWithConstraints(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val nextSpeed = xToSpeed(offset.x, size.width)
                    onSpeedPreview(nextSpeed)
                    onSpeedCommit(nextSpeed)
                }
            }
            .pointerInput(Unit) {
                var pendingSpeed = speed.normalizeVideoSpeed()
                detectDragGestures(
                    onDragStart = { offset ->
                        pendingSpeed = xToSpeed(offset.x, size.width)
                        onSpeedPreview(pendingSpeed)
                    },
                    onDrag = { change, _ ->
                        pendingSpeed = xToSpeed(change.position.x, size.width)
                        onSpeedPreview(pendingSpeed)
                        change.consume()
                    },
                    onDragEnd = {
                        onSpeedCommit(pendingSpeed)
                    },
                    onDragCancel = {
                        onSpeedPreview(speed.normalizeVideoSpeed())
                    }
                )
            }
    ) {
        val thumbSize = 12.dp
        val trackHeight = 5.dp
        val labelWidth = 48.dp
        val sliderWidth = maxWidth
        val fraction = speedToFraction(speed)
        val thumbOffset = ((sliderWidth * fraction) - (thumbSize / 2))
            .coerceIn(0.dp, sliderWidth - thumbSize)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .align(Alignment.TopCenter),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .clip(RoundedCornerShape(999.dp))
                    .background(PlayerTrackInactive)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(trackHeight)
                    .clip(RoundedCornerShape(999.dp))
                    .background(PlayerMoonPurple)
            )
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(PlayerMoonPurpleSoft)
            )
        }

        keySpeeds.forEach { option ->
            val optionFraction = speedToFraction(option)
            val optionOffset = ((sliderWidth * optionFraction) - (labelWidth / 2))
                .coerceIn(0.dp, sliderWidth - labelWidth)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .width(labelWidth)
                    .offset(x = optionOffset)
                    .align(Alignment.BottomStart)
                    .clickable {
                        val normalized = option.normalizeVideoSpeed()
                        onSpeedPreview(normalized)
                        onSpeedCommit(normalized)
                    }
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.82f))
                )
                Text(
                    text = "${option.formatSpeed()}x",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun VideoSpeedStepButton(
    text: String,
    onStep: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val currentOnStep by rememberUpdatedState(onStep)

    LaunchedEffect(isPressed) {
        if (!isPressed) return@LaunchedEffect

        currentOnStep()
        delay(VIDEO_SPEED_REPEAT_INITIAL_DELAY_MS)
        while (isActive) {
            currentOnStep()
            delay(VIDEO_SPEED_REPEAT_INTERVAL_MS)
        }
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.09f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SidePanelShell(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: Dp = 14.dp,
    contentSpacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(PlayerPanelDark)
            .border(1.dp, PlayerPanelStroke, RoundedCornerShape(18.dp))
            .clickable(onClick = {})
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(contentSpacing)
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
            Text("${value.toInt()} dB", color = PlayerMoonPurpleSoft, style = MaterialTheme.typography.labelSmall)
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
    thumbColor = PlayerMoonPurpleSoft,
    activeTrackColor = PlayerMoonPurple,
    inactiveTrackColor = PlayerTrackInactive
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
            .clip(RoundedCornerShape(18.dp))
            .background(PlayerPanelDark)
            .border(1.dp, PlayerPanelStroke, RoundedCornerShape(18.dp))
            .clickable(onClick = {})
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, color = Color.White, style = MaterialTheme.typography.titleSmall)
        options.forEach { option ->
            Text(
                text = if (option == selected) "$option  ✓" else option,
                color = if (option == selected) PlayerMoonPurpleSoft else Color.White.copy(alpha = 0.86f),
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
                .background(PlayerTrackInactive, RoundedCornerShape(99.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(progressFraction)
                .height(2.dp)
                .background(PlayerMoonPurple, RoundedCornerShape(99.dp))
        )
        Box(
            modifier = Modifier
                .offset(x = (maxWidth - 10.dp) * progressFraction)
                .size(10.dp)
                .background(PlayerMoonPurpleSoft, CircleShape)
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

private fun buildVideoMediaItem(
    videoUri: String,
    subtitleUri: Uri?
): MediaItem {
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

private fun Context.subtitleDisplayName(uri: Uri): String {
    return runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else {
                    null
                }
            }
    }.getOrNull()
        ?: uri.lastPathSegment?.substringAfterLast('/')
        ?: "外挂字幕"
}

private fun subtitleSyncStatus(offsetMs: Long): String {
    return when {
        offsetMs > 0L -> "字幕延后 ${formatDelayValue(offsetMs)}"
        offsetMs < 0L -> "字幕提前 ${formatDelayValue(-offsetMs)}"
        else -> "字幕同步 0.0s"
    }
}

private fun formatDelayValue(delayMs: Long): String {
    return String.format(Locale.US, "%.1fs", delayMs / 1000f)
}

private fun formatSleepTimerCountdown(remainingMs: Long): String {
    val totalSeconds = (remainingMs.coerceAtLeast(0L) + 999L) / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

private fun safeVideoSeekPosition(positionMs: Long, durationMs: Long): Long {
    val safeDuration = durationMs.takeIf { it > 0L } ?: return positionMs.coerceAtLeast(0L)
    val maxSeekPosition = (safeDuration - VIDEO_SEEK_END_GUARD_MS).coerceAtLeast(0L)
    return positionMs.coerceIn(0L, maxSeekPosition)
}

private fun safeVideoMarkerPosition(positionMs: Long, durationMs: Long): Long {
    val safeDuration = durationMs.takeIf { it > 0L } ?: return positionMs.coerceAtLeast(0L)
    return positionMs.coerceIn(0L, safeDuration)
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
    STRETCH("拉伸", AspectRatioFrameLayout.RESIZE_MODE_FILL)
}

private fun VideoResizeMode.next(): VideoResizeMode {
    val nextIndex = (VideoResizeModes.indexOf(this) + 1) % VideoResizeModes.size
    return VideoResizeModes[nextIndex]
}

private val VideoResizeModes = listOf(
    VideoResizeMode.FIT,
    VideoResizeMode.FILL,
    VideoResizeMode.ZOOM,
    VideoResizeMode.STRETCH
)

private enum class VideoSleepTimerMode {
    OFF,
    TIMED,
    END_OF_VIDEO
}

private data class VideoSleepTimerOption(
    val label: String,
    val displayLabel: String = label,
    val mode: VideoSleepTimerMode,
    val durationMs: Long? = null,
    val isCustom: Boolean = false
) {
    companion object {
        val FifteenMinutes = VideoSleepTimerOption(
            label = "15 分钟",
            mode = VideoSleepTimerMode.TIMED,
            durationMs = 15 * 60 * 1000L
        )
        val ThirtyMinutes = VideoSleepTimerOption(
            label = "30 分钟",
            mode = VideoSleepTimerMode.TIMED,
            durationMs = 30 * 60 * 1000L
        )
        val FortyFiveMinutes = VideoSleepTimerOption(
            label = "45 分钟",
            mode = VideoSleepTimerMode.TIMED,
            durationMs = 45 * 60 * 1000L
        )
        val SixtyMinutes = VideoSleepTimerOption(
            label = "60 分钟",
            mode = VideoSleepTimerMode.TIMED,
            durationMs = 60 * 60 * 1000L
        )
        val SeventyFiveMinutes = VideoSleepTimerOption(
            label = "1小时15分钟",
            displayLabel = "1:15",
            mode = VideoSleepTimerMode.TIMED,
            durationMs = 75 * 60 * 1000L
        )
        val NinetyMinutes = VideoSleepTimerOption(
            label = "1小时30分钟",
            displayLabel = "1:30",
            mode = VideoSleepTimerMode.TIMED,
            durationMs = 90 * 60 * 1000L
        )
        val EndOfVideo = VideoSleepTimerOption(
            label = "当前视频结束后暂停",
            mode = VideoSleepTimerMode.END_OF_VIDEO
        )
        val Off = VideoSleepTimerOption(
            label = "关闭定时",
            mode = VideoSleepTimerMode.OFF
        )
    }
}

private const val VIDEO_SLEEP_TIMER_MIN_MINUTES = 5
private const val VIDEO_SLEEP_TIMER_MAX_MINUTES = 180
private const val VIDEO_SLEEP_TIMER_STEP_MINUTES = 5

private val VideoSleepTimerFixedOptions = listOf(
    VideoSleepTimerOption.FifteenMinutes,
    VideoSleepTimerOption.ThirtyMinutes,
    VideoSleepTimerOption.FortyFiveMinutes,
    VideoSleepTimerOption.SixtyMinutes,
    VideoSleepTimerOption.SeventyFiveMinutes,
    VideoSleepTimerOption.NinetyMinutes
)

private data class VideoToolAction(
    val label: String,
    val value: String?,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val enabled: Boolean = true
)

private data class VideoSeekPreview(
    val targetMs: Long,
    val deltaMs: Long
)

private data class VideoInfoMetadata(
    val durationMs: Long?,
    val resolution: String?,
    val mimeType: String?,
    val lastModifiedMs: Long?
)

private data class VideoAudioTrackOption(
    val id: String,
    val displayIndex: Int,
    val trackGroup: TrackGroup,
    val trackIndex: Int,
    val isSelected: Boolean,
    val isSupported: Boolean,
    val label: String?,
    val language: String,
    val channelText: String,
    val sampleRateText: String,
    val formatText: String
) {
    val detailText: String
        get() = listOfNotNull(
            label?.takeIf { it.isNotBlank() },
            "语言：$language",
            "声道：$channelText",
            "采样率：$sampleRateText",
            "格式：$formatText"
        ).joinToString(" · ")
}

private fun Float.formatSpeed(): String {
    val rounded = round(this * 100f) / 100f
    return String.format(Locale.US, "%.2f", rounded)
        .trimEnd('0')
        .trimEnd('.')
}

private fun ExoPlayer.availableAudioTracks(): List<VideoAudioTrackOption> {
    var displayIndex = 1
    return currentTracks.groups.flatMapIndexed { groupIndex, group ->
        if (group.type != C.TRACK_TYPE_AUDIO) {
            emptyList()
        } else {
            (0 until group.length).map { trackIndex ->
                val format = group.getTrackFormat(trackIndex)
                VideoAudioTrackOption(
                    id = "audio-$groupIndex-$trackIndex-${format.id.orEmpty()}",
                    displayIndex = displayIndex++,
                    trackGroup = group.mediaTrackGroup,
                    trackIndex = trackIndex,
                    isSelected = group.isTrackSelected(trackIndex),
                    isSupported = group.isTrackSupported(trackIndex),
                    label = format.label?.takeIf { it.isNotBlank() },
                    language = formatAudioLanguage(format.language),
                    channelText = formatAudioChannels(format.channelCount),
                    sampleRateText = formatAudioSampleRate(format.sampleRate),
                    formatText = formatAudioMimeType(format.sampleMimeType)
                )
            }
        }
    }
}

private fun ExoPlayer.selectAudioTrack(track: VideoAudioTrackOption): Boolean {
    if (!track.isSupported) return false
    return runCatching {
        trackSelectionParameters = trackSelectionParameters
            .buildUpon()
            .setOverrideForType(TrackSelectionOverride(track.trackGroup, listOf(track.trackIndex)))
            .build()
    }.isSuccess
}

private fun formatAudioLanguage(language: String?): String {
    val normalized = language?.trim()?.lowercase(Locale.US).orEmpty()
    return when (normalized) {
        "", "und", "unknown" -> "未知"
        "zh", "zho", "chi", "cmn", "zh-cn", "zh-hans" -> "中文"
        "zh-tw", "zh-hant", "yue" -> "中文"
        "en", "eng" -> "英文"
        "ja", "jpn" -> "日文"
        "ko", "kor" -> "韩文"
        "fr", "fra", "fre" -> "法文"
        "de", "deu", "ger" -> "德文"
        "es", "spa" -> "西班牙文"
        else -> language?.uppercase(Locale.US) ?: "未知"
    }
}

private fun formatAudioChannels(channelCount: Int): String {
    return if (channelCount > 0) "${channelCount}ch" else "未知"
}

private fun formatAudioSampleRate(sampleRate: Int): String {
    return if (sampleRate > 0) "${sampleRate}Hz" else "未知"
}

private fun formatAudioMimeType(mimeType: String?): String {
    val normalized = mimeType?.trim()?.lowercase(Locale.US).orEmpty()
    return when {
        normalized.isBlank() -> "未知"
        normalized.contains("mp4a") || normalized.contains("aac") -> "AAC"
        normalized.contains("eac3") -> "EAC3"
        normalized.contains("ac3") -> "AC3"
        normalized.contains("opus") -> "OPUS"
        normalized.contains("vorbis") -> "VORBIS"
        normalized.contains("flac") -> "FLAC"
        normalized.contains("dts") -> "DTS"
        normalized.contains("mpeg") || normalized.contains("mp3") -> "MP3"
        else -> normalized.substringAfter('/').uppercase(Locale.US).ifBlank { "未知" }
    }
}

private fun Float.normalizeVideoSpeed(): Float {
    val coerced = coerceIn(MIN_VIDEO_SPEED, MAX_VIDEO_SPEED)
    return (round(coerced / VIDEO_SPEED_STEP) * VIDEO_SPEED_STEP)
        .coerceIn(MIN_VIDEO_SPEED, MAX_VIDEO_SPEED)
}

private fun LocalMediaFile.videoFolderPlaybackSpeedKey(queue: List<LocalMediaFile>): String {
    val folderNames = queue
        .mapNotNull { it.parentFolderName?.trim()?.takeIf(String::isNotBlank) }
        .distinct()
    if (folderNames.size == 1) {
        return "folder:${folderNames.first().lowercase(Locale.ROOT)}"
    }

    parentFolderName?.trim()
        ?.takeIf(String::isNotBlank)
        ?.let { return "folder:${it.lowercase(Locale.ROOT)}" }

    if (queue.isNotEmpty()) {
        return "queue:${queue.map { it.uri.trim() }.sorted().joinToString("|").hashCode()}"
    }

    val normalizedUri = uri.trim()
    val parentUri = Uri.decode(normalizedUri)
        .trimEnd('/')
        .substringBeforeLast('/', missingDelimiterValue = "")
        .takeIf { it.isNotBlank() }
    return "uri:${parentUri ?: normalizedUri}"
}

private fun List<LocalMediaFile>.videoNavigationQueue(): List<LocalMediaFile> {
    return sortedWith { first, second ->
        val firstEpisode = first.name.extractEpisodeNumber()
        val secondEpisode = second.name.extractEpisodeNumber()
        when {
            firstEpisode != null && secondEpisode != null && firstEpisode != secondEpisode ->
                firstEpisode.compareTo(secondEpisode)
            firstEpisode != null && secondEpisode == null -> -1
            firstEpisode == null && secondEpisode != null -> 1
            else -> naturalCompareVideoNames(first.name, second.name)
        }.takeIf { it != 0 } ?: first.uri.compareTo(second.uri)
    }
}

private fun String.extractEpisodeNumber(): Int? {
    val source = substringBeforeLast('.', missingDelimiterValue = this)
    val patterns = listOf(
        Regex("""第\s*0*(\d{1,4})\s*[集话話]"""),
        Regex("""(?i)\bS\d{1,3}\s*E0*(\d{1,4})\b"""),
        Regex("""(?i)\bEP\s*0*(\d{1,4})\b"""),
        Regex("""(?i)\bE0*(\d{1,4})\b""")
    )
    patterns.forEach { pattern ->
        pattern.find(source)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { return it }
    }
    return Regex("""(?<!\d)0*(\d{1,4})(?!\d)""")
        .findAll(source)
        .mapNotNull { match -> match.groupValues.getOrNull(1)?.toIntOrNull() }
        .filterNot { number -> number in setOf(480, 720, 1080, 2160, 4320) }
        .filterNot { number -> number in 1900..2099 }
        .lastOrNull()
}

private fun naturalCompareVideoNames(first: String, second: String): Int {
    val firstParts = first.videoNameParts()
    val secondParts = second.videoNameParts()
    val maxSize = minOf(firstParts.size, secondParts.size)
    for (index in 0 until maxSize) {
        val left = firstParts[index]
        val right = secondParts[index]
        val compare = when {
            left.number != null && right.number != null -> left.number.compareTo(right.number)
            left.number != null -> -1
            right.number != null -> 1
            else -> left.text.compareTo(right.text, ignoreCase = true)
        }
        if (compare != 0) return compare
    }
    return firstParts.size.compareTo(secondParts.size)
}

private fun String.videoNameParts(): List<VideoNamePart> {
    return Regex("""\d+|\D+""")
        .findAll(substringBeforeLast('.', missingDelimiterValue = this))
        .map { match ->
            val value = match.value
            VideoNamePart(
                text = value,
                number = value.toIntOrNull()
            )
        }
        .toList()
}

private data class VideoNamePart(
    val text: String,
    val number: Int?
)

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

private fun loadVideoInfoMetadata(
    context: Context,
    mediaFile: LocalMediaFile
): VideoInfoMetadata {
    val uri = Uri.parse(mediaFile.uri)
    var durationMs: Long? = null
    var resolution: String? = null
    var mimeType: String? = null

    runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.takeIf { it > 0L }
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull()
                ?.takeIf { it > 0 }
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull()
                ?.takeIf { it > 0 }
            resolution = if (width != null && height != null) {
                "${width} × ${height}"
            } else {
                null
            }
            mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                ?.takeIf { it.isNotBlank() }
        } finally {
            runCatching { retriever.release() }
        }
    }

    val resolverMimeType = runCatching {
        context.contentResolver.getType(uri)
    }.getOrNull()?.takeIf { it.isNotBlank() }

    return VideoInfoMetadata(
        durationMs = durationMs,
        resolution = resolution,
        mimeType = mimeType ?: resolverMimeType,
        lastModifiedMs = queryVideoLastModifiedMs(context, uri)
    )
}

private fun queryVideoLastModifiedMs(context: Context, uri: Uri): Long? {
    return runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(MediaStore.MediaColumns.DATE_MODIFIED),
            null,
            null,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
            if (index < 0) return@use null
            cursor.getLong(index)
                .takeIf { it > 0L }
                ?.let { it * 1000L }
        }
    }.getOrNull()
}

private fun formatVideoInfoDate(timestampMs: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(timestampMs))
}

private fun saveVideoFrameScreenshot(
    context: Context,
    videoUri: Uri,
    positionMs: Long
): ScreenshotSaveResult {
    val bitmap = extractVideoFrame(context, videoUri, positionMs)
        ?: return ScreenshotSaveResult.Unavailable
    if (bitmap.isProbablyBlankFrame()) {
        return ScreenshotSaveResult.Unavailable
    }
    val fileName = "MoonPlayer_${
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    }.png"
    return if (saveBitmapToGallery(context, bitmap, fileName)) {
        ScreenshotSaveResult.Saved
    } else {
        ScreenshotSaveResult.Failed
    }
}

private fun extractVideoFrame(
    context: Context,
    videoUri: Uri,
    positionMs: Long
): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, videoUri)
        retriever.getFrameAtTime(
            positionMs.coerceAtLeast(0L) * 1_000L,
            MediaMetadataRetriever.OPTION_CLOSEST
        )
    } catch (_: Exception) {
        null
    } finally {
        runCatching { retriever.release() }
    }
}

private fun saveBitmapToGallery(
    context: Context,
    bitmap: Bitmap,
    fileName: String
): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        saveBitmapToGalleryQAndAbove(context, bitmap, fileName)
    } else {
        saveBitmapToGalleryLegacy(context, bitmap, fileName)
    }
}

private fun saveBitmapToGalleryQAndAbove(
    context: Context,
    bitmap: Bitmap,
    fileName: String
): Boolean {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/MoonPlayer")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: return false
    return try {
        resolver.openOutputStream(imageUri)?.use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                return@use false
            }
            true
        } == true
    } catch (_: Exception) {
        false
    }.also { saved ->
        if (saved) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(imageUri, values, null, null)
        } else {
            resolver.delete(imageUri, null, null)
        }
    }
}

private fun saveBitmapToGalleryLegacy(
    context: Context,
    bitmap: Bitmap,
    fileName: String
): Boolean {
    return try {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "MoonPlayer"
        )
        if (!directory.exists() && !directory.mkdirs()) {
            return false
        }
        val imageFile = File(directory, fileName)
        FileOutputStream(imageFile).use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                return false
            }
        }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
        }
        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) != null
    } catch (_: Exception) {
        false
    }
}

private fun Bitmap.isProbablyBlankFrame(): Boolean {
    if (width <= 0 || height <= 0) return true
    val columns = 8
    val rows = 8
    var brightest = 0
    for (xIndex in 0 until columns) {
        val x = ((xIndex + 0.5f) * width / columns).toInt().coerceIn(0, width - 1)
        for (yIndex in 0 until rows) {
            val y = ((yIndex + 0.5f) * height / rows).toInt().coerceIn(0, height - 1)
            val pixel = getPixel(x, y)
            brightest = maxOf(
                brightest,
                android.graphics.Color.red(pixel),
                android.graphics.Color.green(pixel),
                android.graphics.Color.blue(pixel)
            )
            if (brightest > 12) return false
        }
    }
    return true
}

private enum class ScreenshotSaveResult(val message: String, val toastDurationMs: Long) {
    Saved("截图已保存", SHORT_HINT_MS),
    Failed("截图失败", ERROR_HINT_MS),
    Unavailable("当前画面暂不可截图", ERROR_HINT_MS)
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

private fun showVideoPlayerToast(
    context: Context,
    message: String,
    durationMs: Long = SHORT_HINT_MS
) {
    val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
    toast.show()
    Handler(Looper.getMainLooper()).postDelayed({ toast.cancel() }, durationMs)
}

private fun showFutureToolToast(context: Context, feature: String) {
    showVideoPlayerToast(context, "${feature}后续实现")
}

private const val FINISHED_THRESHOLD_MS = 5_000L
private const val MIN_VIDEO_SPEED = 0.25f
private const val MAX_VIDEO_SPEED = 5f
private const val VIDEO_SPEED_STEP = 0.01f
private const val VIDEO_SPEED_REPEAT_INITIAL_DELAY_MS = 80L
private const val VIDEO_SPEED_REPEAT_INTERVAL_MS = 50L
private const val VIDEO_SPEED_SAVE_DEBOUNCE_MS = 400L
private const val LOCKED_UNLOCK_HINT_MS = 1_500L
private const val SHORT_HINT_MS = 900L
private const val GESTURE_HINT_MS = 650L
private const val ERROR_HINT_MS = 1_400L

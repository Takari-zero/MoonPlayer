package com.shenghui.localvibe.feature.video

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
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
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.shenghui.localvibe.core.media.formatFileSize
import com.shenghui.localvibe.core.media.formatDuration
import com.shenghui.localvibe.core.media.VideoThumbnailStore
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
import kotlin.math.roundToInt

private val PlayerMoonPurple = Color(0xFF7B55FF)
private val PlayerMoonPurpleSoft = Color(0xFFB7A7FF)
private val VideoLibrarySelectionPurple = Color(0xFF8B5CFF)
private const val VideoLibrarySelectionContainerAlpha = 0.34f
private val PlayerPanelDark = Color(0xB30B0A12)
private val PlayerTrackInactive = Color(0xFF3A3449)
private val VideoFolderPlaybackSpeeds = mutableMapOf<String, Float>()
private const val VIDEO_SEEK_END_GUARD_MS = 500L
private const val PLAYER_SIDE_PANEL_HEIGHT_FRACTION = 0.96f
private const val VIDEO_EQUALIZER_PRIORITY = 0
private const val VIDEO_AUDIO_EFFECT_MAX_STRENGTH = 1000
private const val VIDEO_SUBTITLE_SYNC_OFFSET_LIMIT_MS = 5_000L
private val SRT_TIMESTAMP_REGEX = Regex("""(\d{1,2}):(\d{2}):(\d{2}),(\d{3})""")
private val SRT_TIME_RANGE_REGEX = Regex("""(\d{1,2}:\d{2}:\d{2},\d{3})\s*-->\s*(\d{1,2}:\d{2}:\d{2},\d{3})""")
private val VIDEO_EQUALIZER_TARGET_FREQ_HZ = listOf(60f, 230f, 910f, 4_000f, 14_000f)

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    mediaFile: LocalMediaFile?,
    initialPositionMs: Long,
    queue: List<LocalMediaFile>,
    currentIndex: Int,
    videoProgressByUri: Map<String, Long> = emptyMap(),
    onRemoveUnavailableVideo: (LocalMediaFile) -> Unit = {},
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
            videoProgressByUri = videoProgressByUri,
            onRemoveUnavailableVideo = onRemoveUnavailableVideo,
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
    videoProgressByUri: Map<String, Long>,
    onRemoveUnavailableVideo: (LocalMediaFile) -> Unit,
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
    val lifecycleOwner = LocalLifecycleOwner.current
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
    var lastPlaybackError by remember(mediaFile.uri) { mutableStateOf<VideoPlaybackErrorInfo?>(null) }
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
    var subtitleSyncError by remember(mediaFile.uri) { mutableStateOf<String?>(null) }
    var subtitleStyleSettings by remember(mediaFile.uri) { mutableStateOf(VideoSubtitleStyleSettings()) }
    val hasSyncableExternalSrtSubtitle = isSyncableExternalSrtSubtitle(externalSubtitleUri, externalSubtitleName)
    var audioDelayMs by remember(mediaFile.uri) { mutableLongStateOf(0L) }
    var isBackRequested by remember(mediaFile.uri) { mutableStateOf(false) }
    var isSpeedPanelVisible by remember { mutableStateOf(false) }
    var isAbLoopBarVisible by remember { mutableStateOf(false) }
    var gestureSettings by remember { mutableStateOf(VideoGestureSettings()) }
    var pictureAdjustmentSettings by remember { mutableStateOf(VideoPictureAdjustmentSettings()) }
    var audioSessionId by remember(mediaFile.uri) { mutableIntStateOf(C.AUDIO_SESSION_ID_UNSET) }
    var systemEqualizer by remember(mediaFile.uri) { mutableStateOf<Equalizer?>(null) }
    var systemBassBoost by remember(mediaFile.uri) { mutableStateOf<BassBoost?>(null) }
    var systemVirtualizer by remember(mediaFile.uri) { mutableStateOf<Virtualizer?>(null) }
    var equalizerState by remember(mediaFile.uri) { mutableStateOf(VideoEqualizerUiState.waiting()) }
    var equalizerWantedEnabled by remember(mediaFile.uri) { mutableStateOf(false) }
    var equalizerSelectedPreset by remember(mediaFile.uri) { mutableStateOf<VideoEqualizerPreset?>(VideoEqualizerPreset.Normal) }
    var bassBoostState by remember(mediaFile.uri) { mutableStateOf(VideoAudioEffectStrengthUiState.waiting()) }
    var bassBoostPercent by remember(mediaFile.uri) { mutableIntStateOf(0) }
    var virtualizerState by remember(mediaFile.uri) { mutableStateOf(VideoAudioEffectStrengthUiState.waiting()) }
    var virtualizerPercent by remember(mediaFile.uri) { mutableIntStateOf(0) }
    var closeAbLoopBarRequest by remember { mutableIntStateOf(0) }
    var keepControlsVisible by remember { mutableStateOf(false) }
    var isScreenLocked by remember(mediaFile.uri) { mutableStateOf(false) }
    var isPortraitPlayback by remember(mediaFile.uri) { mutableStateOf(false) }
    var isSavingScreenshot by remember { mutableStateOf(false) }
    var isQuickToolsExpanded by remember { mutableStateOf(false) }
    var showLockedUnlockButton by remember(mediaFile.uri) { mutableStateOf(false) }
    var sleepTimerMode by remember { mutableStateOf(VideoSleepTimerMode.OFF) }
    var sleepTimerSelectedOption by remember { mutableStateOf(VideoSleepTimerOption.Off) }
    var sleepTimerEndAtElapsedMs by remember { mutableLongStateOf(0L) }
    var sleepTimerRemainingMs by remember { mutableLongStateOf(0L) }
    var sleepTimerEndOfVideoEnabled by remember { mutableStateOf(false) }
    val latestSleepTimerMode by rememberUpdatedState(sleepTimerMode)
    val latestSleepTimerEndOfVideoEnabled by rememberUpdatedState(sleepTimerEndOfVideoEnabled)
    val latestGestureSettings by rememberUpdatedState(gestureSettings)
    val pictureRenderEffect = remember(pictureAdjustmentSettings) {
        pictureAdjustmentSettings.toAndroidRenderEffectOrNull()
    }
    val latestShowControls by rememberUpdatedState(showControls)
    val latestIsScreenLocked by rememberUpdatedState(isScreenLocked)
    val latestIsSpeedPanelVisible by rememberUpdatedState(isSpeedPanelVisible)
    val latestIsAbLoopBarVisible by rememberUpdatedState(isAbLoopBarVisible)
    val latestDurationMs by rememberUpdatedState(durationMs)
    val latestCurrentPositionMs by rememberUpdatedState(currentPositionMs)
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
        subtitleOffsetMs = 0L
        subtitleSyncError = null
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

    DisposableEffect(player, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                player.pause()
                isPlaying = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
        sleepTimerEndOfVideoEnabled = false
        if (showHint) {
            showVideoPlayerToast(context, "已关闭睡眠定时")
        }
    }

    fun pauseForSleepTimer() {
        sleepTimerMode = VideoSleepTimerMode.OFF
        sleepTimerSelectedOption = VideoSleepTimerOption.Off
        sleepTimerEndAtElapsedMs = 0L
        sleepTimerRemainingMs = 0L
        sleepTimerEndOfVideoEnabled = false
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
                sleepTimerEndOfVideoEnabled = false
                showVideoPlayerToast(context, "已设置睡眠定时 ${option.label}")
            }

            VideoSleepTimerMode.END_OF_VIDEO -> {
                sleepTimerMode = VideoSleepTimerMode.END_OF_VIDEO
                sleepTimerSelectedOption = option
                sleepTimerEndAtElapsedMs = 0L
                sleepTimerRemainingMs = 0L
                sleepTimerEndOfVideoEnabled = true
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
                val refreshedPosition = player.currentPosition.coerceAtLeast(0L)
                currentPositionMs = if ((refreshedPosition - target).absoluteValue <= 1_500L) {
                    refreshedPosition
                } else {
                    target
                }
            }
        }
        return target
    }

    fun performQuickSeek(deltaMs: Long, durationForClampMs: Long, basePositionMs: Long, showHint: Boolean) {
        val playerPosition = player.currentPosition.coerceAtLeast(0L)
        val start = if ((playerPosition - basePositionMs).absoluteValue <= 1_500L) {
            playerPosition
        } else {
            basePositionMs.coerceAtLeast(0L)
        }
        val target = safeVideoSeekPosition(start + deltaMs, durationForClampMs)
        isSeekingByUser = false
        draggingPositionMs = target
        currentPositionMs = target
        player.seekTo(target)
        coroutineScope.launch {
            delay(120L)
            if (!isSeekingByUser) {
                val refreshedPosition = player.currentPosition.coerceAtLeast(0L)
                currentPositionMs = if ((refreshedPosition - target).absoluteValue <= 1_500L) {
                    refreshedPosition
                } else {
                    target
                }
            }
        }
        if (showHint) {
            seekPreviewOverlay = VideoSeekPreview(target, target - start)
        }
        showControls = true
    }

    fun seekByGesture(deltaMs: Long, durationForClampMs: Long, showHint: Boolean) {
        val start = player.currentPosition.coerceAtLeast(0L)
        val target = safeVideoSeekPosition(start + deltaMs, durationForClampMs)
        isSeekingByUser = false
        draggingPositionMs = target
        currentPositionMs = target
        player.seekTo(target)
        coroutineScope.launch {
            delay(120L)
            if (!isSeekingByUser) {
                val refreshedPosition = player.currentPosition.coerceAtLeast(0L)
                currentPositionMs = if ((refreshedPosition - target).absoluteValue <= 1_500L) {
                    refreshedPosition
                } else {
                    target
                }
            }
        }
        if (showHint) {
            seekPreviewOverlay = VideoSeekPreview(target, target - start)
        }
        showControls = true
    }

    fun currentAbMarkerPosition(): Long {
        return safeVideoMarkerPosition(player.currentPosition.coerceAtLeast(0L), durationMs)
    }

    fun setAbLoopStartAt(positionMs: Long) {
        val target = safeVideoMarkerPosition(positionMs, durationMs)
        abLoopStartMs = target
        if (abLoopEndMs != null && abLoopEndMs!! <= target) {
            abLoopEndMs = null
        }
        showVideoPlayerToast(context, "已设置 A 点")
    }

    fun setAbLoopStart() {
        setAbLoopStartAt(currentAbMarkerPosition())
    }

    fun setAbLoopEndAt(positionMs: Long) {
        val target = safeVideoMarkerPosition(positionMs, durationMs)
        val start = abLoopStartMs
        if (start != null && target <= start) {
            showVideoPlayerToast(context, "B 点必须晚于 A 点")
            return
        }
        abLoopEndMs = target
        showVideoPlayerToast(context, "已设置 B 点")
    }

    fun setAbLoopEnd() {
        setAbLoopEndAt(currentAbMarkerPosition())
    }

    fun enableAbLoop() {
        val start = abLoopStartMs
        val end = abLoopEndMs
        when {
            start == null || end == null -> showVideoPlayerToast(context, "请先设置 A/B 点")
            end <= start -> showVideoPlayerToast(context, "B 点必须晚于 A 点")
            else -> showVideoPlayerToast(context, "AB 循环已开启")
        }
    }

    fun clearAbLoop() {
        abLoopStartMs = null
        abLoopEndMs = null
        showVideoPlayerToast(context, "已关闭 AB 循环")
    }

    fun refreshEqualizerState() {
        val equalizer = systemEqualizer
        equalizerState = if (equalizer == null) {
            if (audioSessionId.isValidVideoAudioSessionId()) {
                equalizerState
            } else {
                VideoEqualizerUiState.waiting()
            }
        } else {
            equalizer.toVideoEqualizerUiState(
                wantedEnabled = equalizerWantedEnabled,
                selectedPreset = equalizerSelectedPreset
            )
        }
        bassBoostState = systemBassBoost?.toVideoAudioEffectStrengthUiState(
            percent = bassBoostPercent,
            unsupportedMessage = "当前设备不支持低音增强"
        ) ?: if (audioSessionId.isValidVideoAudioSessionId()) {
            bassBoostState
        } else {
            VideoAudioEffectStrengthUiState.waiting()
        }
        virtualizerState = systemVirtualizer?.toVideoAudioEffectStrengthUiState(
            percent = virtualizerPercent,
            unsupportedMessage = "当前设备不支持环境声"
        ) ?: if (audioSessionId.isValidVideoAudioSessionId()) {
            virtualizerState
        } else {
            VideoAudioEffectStrengthUiState.waiting()
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        val equalizer = systemEqualizer
        if (equalizer == null) {
            equalizerWantedEnabled = false
            showVideoPlayerToast(context, equalizerState.message)
            return
        }
        runCatching {
            equalizer.enabled = enabled
            equalizerWantedEnabled = enabled
            refreshEqualizerState()
        }.onFailure {
            equalizerWantedEnabled = false
            equalizerState = VideoEqualizerUiState.unsupported("均衡器开关失败")
            showVideoPlayerToast(context, "当前设备不支持系统均衡器")
        }
    }

    fun setEqualizerBandLevel(bandIndex: Short, level: Short) {
        val equalizer = systemEqualizer ?: return
        runCatching {
            equalizer.enabled = true
            equalizerWantedEnabled = true
            equalizerSelectedPreset = null
            equalizer.setBandLevel(bandIndex, level)
            refreshEqualizerState()
        }.onFailure {
            equalizerState = VideoEqualizerUiState.unsupported("频段调节失败")
            showVideoPlayerToast(context, "均衡器频段调节失败")
        }
    }

    fun applyEqualizerPreset(preset: VideoEqualizerPreset) {
        val equalizer = systemEqualizer ?: return
        runCatching {
            equalizer.applyVideoEqualizerPreset(preset)
            equalizerSelectedPreset = preset
            equalizerWantedEnabled = true
            equalizer.enabled = true
            refreshEqualizerState()
        }.onFailure {
            equalizerState = VideoEqualizerUiState.unsupported("预设应用失败")
            showVideoPlayerToast(context, "均衡器预设应用失败")
        }
    }

    fun setBassBoostStrength(percent: Int) {
        val bassBoost = systemBassBoost
        if (bassBoost == null || !bassBoostState.supported) {
            showVideoPlayerToast(context, bassBoostState.message)
            return
        }
        runCatching {
            val safePercent = percent.coerceIn(0, 100)
            bassBoostPercent = safePercent
            bassBoost.setStrength(safePercent.toAudioEffectStrength())
            bassBoost.enabled = safePercent > 0
            refreshEqualizerState()
        }.onFailure {
            bassBoostState = VideoAudioEffectStrengthUiState.unsupported("低音增强不可用")
            showVideoPlayerToast(context, "低音增强不可用")
        }
    }

    fun setVirtualizerStrength(percent: Int) {
        val virtualizer = systemVirtualizer
        if (virtualizer == null || !virtualizerState.supported) {
            showVideoPlayerToast(context, virtualizerState.message)
            return
        }
        runCatching {
            val safePercent = percent.coerceIn(0, 100)
            virtualizerPercent = safePercent
            virtualizer.setStrength(safePercent.toAudioEffectStrength())
            virtualizer.enabled = safePercent > 0
            refreshEqualizerState()
        }.onFailure {
            virtualizerState = VideoAudioEffectStrengthUiState.unsupported("环境声不可用")
            showVideoPlayerToast(context, "环境声不可用")
        }
    }

    fun resetEqualizer() {
        runCatching {
            systemEqualizer?.let { equalizer ->
                repeat(equalizer.numberOfBands.toInt()) { index ->
                    equalizer.setBandLevel(index.toShort(), 0)
                }
                equalizerWantedEnabled = true
                equalizer.enabled = true
                equalizerSelectedPreset = VideoEqualizerPreset.Normal
            }
            systemBassBoost?.let { bassBoost ->
                bassBoost.setStrength(0)
                bassBoost.enabled = false
                bassBoostPercent = 0
            }
            systemVirtualizer?.let { virtualizer ->
                virtualizer.setStrength(0)
                virtualizer.enabled = false
                virtualizerPercent = 0
            }
            refreshEqualizerState()
        }.onFailure {
            showVideoPlayerToast(context, "均衡器恢复默认失败")
        }
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
    LaunchedEffect(showControls, isSpeedPanelVisible, isAbLoopBarVisible, isScreenLocked, isQuickToolsExpanded, keepControlsVisible) {
        if (showControls && !keepControlsVisible && !isSpeedPanelVisible && !isAbLoopBarVisible && !isScreenLocked && !isQuickToolsExpanded) {
            delay(2_000)
            if (!keepControlsVisible && !isSpeedPanelVisible && !isAbLoopBarVisible && !isScreenLocked && !isQuickToolsExpanded) {
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
                    if (latestSleepTimerEndOfVideoEnabled || latestSleepTimerMode == VideoSleepTimerMode.END_OF_VIDEO) {
                        pauseForSleepTimer()
                    } else {
                        next(auto = true)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val errorInfo = error.toVideoPlaybackErrorInfo()
                lastPlaybackError = errorInfo
                showVideoPlayerToast(
                    context,
                    "播放失败：${errorInfo.detail}",
                    ERROR_HINT_MS
                )
            }

            override fun onAudioSessionIdChanged(newAudioSessionId: Int) {
                audioSessionId = newAudioSessionId
            }
        }
        player.addListener(listener)
        audioSessionId = player.audioSessionId
        onDispose {
            player.removeListener(listener)
            saveCurrentProgress()
            Handler(Looper.getMainLooper()).post {
                player.release()
            }
        }
    }

    DisposableEffect(audioSessionId) {
        val sessionId = audioSessionId
        if (!sessionId.isValidVideoAudioSessionId()) {
            systemEqualizer = null
            systemBassBoost = null
            systemVirtualizer = null
            equalizerState = VideoEqualizerUiState.waiting()
            bassBoostState = VideoAudioEffectStrengthUiState.waiting()
            virtualizerState = VideoAudioEffectStrengthUiState.waiting()
            onDispose { }
        } else {
            val createdEqualizer = runCatching {
                Equalizer(VIDEO_EQUALIZER_PRIORITY, sessionId).apply {
                    enabled = equalizerWantedEnabled
                }
            }.getOrElse { error ->
                systemEqualizer = null
                equalizerWantedEnabled = false
                equalizerState = VideoEqualizerUiState.unsupported(
                    error.message ?: "当前设备不支持系统均衡器"
                )
                null
            }
            val createdBassBoost = runCatching {
                BassBoost(VIDEO_EQUALIZER_PRIORITY, sessionId).apply {
                    enabled = bassBoostPercent > 0
                    setStrength(bassBoostPercent.toAudioEffectStrength())
                }
            }.getOrElse { error ->
                systemBassBoost = null
                bassBoostPercent = 0
                bassBoostState = VideoAudioEffectStrengthUiState.unsupported(
                    error.message ?: "当前设备不支持低音增强"
                )
                null
            }
            val createdVirtualizer = runCatching {
                Virtualizer(VIDEO_EQUALIZER_PRIORITY, sessionId).apply {
                    enabled = virtualizerPercent > 0
                    setStrength(virtualizerPercent.toAudioEffectStrength())
                }
            }.getOrElse { error ->
                systemVirtualizer = null
                virtualizerPercent = 0
                virtualizerState = VideoAudioEffectStrengthUiState.unsupported(
                    error.message ?: "当前设备不支持环境声"
                )
                null
            }

            if (createdEqualizer != null) {
                systemEqualizer = createdEqualizer
                equalizerState = createdEqualizer.toVideoEqualizerUiState(
                    wantedEnabled = equalizerWantedEnabled,
                    selectedPreset = equalizerSelectedPreset
                )
            }
            if (createdBassBoost != null) {
                systemBassBoost = createdBassBoost
                bassBoostState = createdBassBoost.toVideoAudioEffectStrengthUiState(
                    percent = bassBoostPercent,
                    unsupportedMessage = "当前设备不支持低音增强"
                )
            }
            if (createdVirtualizer != null) {
                systemVirtualizer = createdVirtualizer
                virtualizerState = createdVirtualizer.toVideoAudioEffectStrengthUiState(
                    percent = virtualizerPercent,
                    unsupportedMessage = "当前设备不支持环境声"
                )
            }

            onDispose {
                runCatching {
                    createdEqualizer?.enabled = false
                    createdEqualizer?.release()
                }
                runCatching {
                    createdBassBoost?.enabled = false
                    createdBassBoost?.release()
                }
                runCatching {
                    createdVirtualizer?.enabled = false
                    createdVirtualizer?.release()
                }
                if (systemEqualizer === createdEqualizer) {
                    systemEqualizer = null
                }
                if (systemBassBoost === createdBassBoost) {
                    systemBassBoost = null
                }
                if (systemVirtualizer === createdVirtualizer) {
                    systemVirtualizer = null
                }
            }
        }
    }

    LaunchedEffect(externalSubtitleUri, externalSubtitleName, subtitleOffsetMs) {
        val current = player.currentPosition.coerceAtLeast(0L)
        val shouldPlay = player.playWhenReady
        val subtitleUri = withContext(Dispatchers.IO) {
            buildSyncedSrtSubtitleUriOrNull(
                context = context,
                sourceUri = externalSubtitleUri,
                subtitleName = externalSubtitleName,
                offsetMs = subtitleOffsetMs
            )
        }
        subtitleSyncError = if (externalSubtitleUri != null && subtitleOffsetMs != 0L && subtitleUri == null) {
            "字幕同步异常，请尝试重新加载字幕文件"
        } else {
            null
        }
        player.setMediaItem(
            buildVideoMediaItem(
                videoUri = mediaFile.uri,
                subtitleUri = subtitleUri
            ),
            current
        )
        player.prepare()
        player.playWhenReady = shouldPlay
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(mediaFile.uri, player) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        val settings = latestGestureSettings
                        if (
                            latestIsScreenLocked ||
                            latestIsSpeedPanelVisible ||
                            latestIsAbLoopBarVisible ||
                            !settings.doubleTapSeekEnabled
                        ) {
                            return@detectTapGestures
                        }
                        if (latestDurationMs <= 0L) {
                            if (settings.showHints) {
                                showVideoPlayerToast(context, "视频时长未知", ERROR_HINT_MS)
                            }
                            return@detectTapGestures
                        }
                        val deltaMs = if (offset.x < size.width / 2f) {
                            -VIDEO_DOUBLE_TAP_SEEK_MS
                        } else {
                            VIDEO_DOUBLE_TAP_SEEK_MS
                        }
                        performQuickSeek(
                            deltaMs = deltaMs,
                            durationForClampMs = latestDurationMs,
                            basePositionMs = latestCurrentPositionMs,
                            showHint = settings.showHints
                        )
                    },
                    onTap = {
                        if (latestIsScreenLocked) {
                            showLockedUnlockButton = true
                            return@detectTapGestures
                        }
                        if (latestIsSpeedPanelVisible) {
                            isSpeedPanelVisible = false
                            showControls = true
                        } else if (latestIsAbLoopBarVisible && latestShowControls) {
                            closeAbLoopBarRequest += 1
                            showControls = true
                        } else if (latestShowControls) {
                            showControls = false
                            isQuickToolsExpanded = false
                        } else {
                            showControls = true
                        }
                    }
                )
            }
            .pointerInput(mediaFile.uri, player) {
                var isDragGestureBlocked = false
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragGestureBlocked = latestIsScreenLocked ||
                                latestIsSpeedPanelVisible ||
                                latestIsAbLoopBarVisible ||
                                offset.y >= size.height - VIDEO_SYSTEM_GESTURE_BOTTOM_SAFE_DP.dp.toPx()
                            if (isDragGestureBlocked) {
                                dragMode = VideoDragMode.UNKNOWN
                                seekPreviewOverlay = null
                                return@detectDragGestures
                            }
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
                            if (isDragGestureBlocked) {
                                isDragGestureBlocked = false
                                dragMode = VideoDragMode.UNKNOWN
                                seekPreviewOverlay = null
                                return@detectDragGestures
                            }
                            if (dragMode == VideoDragMode.BACK) {
                                requestBack()
                            } else if (dragMode == VideoDragMode.SEEK) {
                                val target = seekToUserPosition(gestureSeekPreviewMs)
                                if (latestGestureSettings.showHints) {
                                    seekPreviewOverlay = VideoSeekPreview(target, target - gestureSeekStartMs)
                                }
                                showControls = true
                            } else if (dragMode == VideoDragMode.SEEK_UNAVAILABLE) {
                                if (latestGestureSettings.showHints) {
                                    showVideoPlayerToast(context, "视频时长未知", ERROR_HINT_MS)
                                }
                            }
                            dragMode = VideoDragMode.UNKNOWN
                        },
                        onDragCancel = {
                            isDragGestureBlocked = false
                            seekPreviewOverlay = null
                            dragMode = VideoDragMode.UNKNOWN
                        },
                        onDrag = { change, dragAmount ->
                            if (isDragGestureBlocked) {
                                return@detectDragGestures
                            }
                            totalDragX += dragAmount.x
                            totalDragY += dragAmount.y
                            if (dragMode == VideoDragMode.UNKNOWN) {
                                val absX = totalDragX.absoluteValue
                                val absY = totalDragY.absoluteValue
                                val threshold = VIDEO_GESTURE_DRAG_THRESHOLD_DP.dp.toPx()
                                if (absX > threshold || absY > threshold) {
                                    dragMode = if (
                                        gestureStart.x > size.width - 56.dp.toPx() &&
                                        totalDragX < -40f &&
                                        absX > absY * VIDEO_GESTURE_HORIZONTAL_RATIO
                                    ) {
                                        VideoDragMode.BACK
                                    } else if (latestGestureSettings.horizontalSeekEnabled && absX > absY * VIDEO_GESTURE_HORIZONTAL_RATIO) {
                                        if (latestDurationMs > 0L) {
                                            VideoDragMode.SEEK
                                        } else {
                                            VideoDragMode.SEEK_UNAVAILABLE
                                        }
                                    } else if (
                                        absY > absX * VIDEO_GESTURE_HORIZONTAL_RATIO &&
                                        gestureStart.x < size.width / 2f &&
                                        latestGestureSettings.brightnessGestureEnabled
                                    ) {
                                        VideoDragMode.BRIGHTNESS
                                    } else if (
                                        absY > absX * VIDEO_GESTURE_HORIZONTAL_RATIO &&
                                        gestureStart.x >= size.width / 2f &&
                                        latestGestureSettings.volumeGestureEnabled
                                    ) {
                                        VideoDragMode.VOLUME
                                    } else {
                                        VideoDragMode.UNKNOWN
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
                                        .coerceIn(0L, latestDurationMs.coerceAtLeast(1L))
                                    gestureSeekPreviewMs = target
                                    if (latestGestureSettings.showHints) {
                                        seekPreviewOverlay = VideoSeekPreview(target, target - gestureSeekStartMs)
                                    }
                                    showControls = true
                                }
                                VideoDragMode.BRIGHTNESS -> {
                                    val height = size.height.toFloat().coerceAtLeast(1f)
                                    val percentDelta = (-totalDragY / height) * 1.8f
                                    val brightness = (startBrightness + percentDelta).coerceIn(0.05f, 1f)
                                    activity?.setScreenBrightness(brightness)
                                    if (latestGestureSettings.showHints) {
                                        gestureOverlay = "亮度 ${(brightness * 100).toInt()}%"
                                    }
                                    showControls = true
                                }
                                VideoDragMode.VOLUME -> {
                                    val height = size.height.toFloat().coerceAtLeast(1f)
                                    val percentDelta = (-totalDragY / height) * 1.8f
                                    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    val volumeDelta = round(percentDelta * maxVolume).toInt()
                                    val nextVolume = (startVolume + volumeDelta).coerceIn(0, maxVolume)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, nextVolume, 0)
                                    if (latestGestureSettings.showHints) {
                                        val percent = if (maxVolume == 0) 0 else nextVolume * 100 / maxVolume
                                        gestureOverlay = "音量 $percent%"
                                    }
                                    showControls = true
                                }
                                VideoDragMode.SEEK_UNAVAILABLE -> Unit
                                VideoDragMode.UNKNOWN -> Unit
                                VideoDragMode.BACK -> {
                                    showControls = true
                                }
                            }
                            if (
                                dragMode == VideoDragMode.SEEK ||
                                dragMode == VideoDragMode.SEEK_UNAVAILABLE ||
                                dragMode == VideoDragMode.BACK ||
                                dragMode == VideoDragMode.BRIGHTNESS ||
                                dragMode == VideoDragMode.VOLUME
                            ) {
                                change.consume()
                            }
                        }
                    )
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                (LayoutInflater.from(viewContext).inflate(
                    com.shenghui.localvibe.R.layout.video_player_texture_view,
                    null,
                    false
                ) as PlayerView).apply {
                    this.player = player
                    useController = false
                    resizeMode = videoResizeMode.playerResizeMode
                    setBackgroundColor(android.graphics.Color.BLACK)
                    applyVideoSubtitleStyle(subtitleStyleSettings)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        setRenderEffect(pictureRenderEffect)
                    }
                }
            },
            update = { playerView ->
                playerView.player = player
                playerView.resizeMode = videoResizeMode.playerResizeMode
                playerView.applyVideoSubtitleStyle(subtitleStyleSettings)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    playerView.setRenderEffect(pictureRenderEffect)
                }
            }
        )

        if (showControls && !isScreenLocked) {
            VideoControlOverlay(
                title = mediaFile.name,
                currentPositionMs = if (isSeekingByUser) draggingPositionMs else currentPositionMs,
                durationMs = durationMs,
                mediaFile = mediaFile,
                player = player,
                lastPlaybackError = lastPlaybackError,
                navigationQueue = navigationQueue,
                currentQueueUri = mediaFile.uri,
                videoProgressByUri = videoProgressByUri,
                isPlaying = isPlaying,
                onBack = ::requestBack,
                onQueueItemSelected = { file ->
                    selectNavigationVideo(file)
                },
                onRemoveUnavailableVideo = onRemoveUnavailableVideo,
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
                subtitleSyncAvailable = hasSyncableExternalSrtSubtitle,
                subtitleSyncError = subtitleSyncError,
                subtitleStyleSettings = subtitleStyleSettings,
                sleepTimerMode = sleepTimerMode,
                sleepTimerSelectedOption = sleepTimerSelectedOption,
                sleepTimerRemainingMs = sleepTimerRemainingMs,
                sleepTimerEndOfVideoEnabled = sleepTimerEndOfVideoEnabled,
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
                    subtitleSyncError = null
                    showVideoPlayerToast(context, "已清除外挂字幕")
                },
                onSubtitleStyleChanged = { settings ->
                    subtitleStyleSettings = settings
                    showVideoPlayerToast(context, "已应用字幕样式")
                },
                onSubtitleOffsetChange = { offsetMs ->
                    if (hasSyncableExternalSrtSubtitle) {
                        subtitleOffsetMs = offsetMs.coerceIn(
                            -VIDEO_SUBTITLE_SYNC_OFFSET_LIMIT_MS,
                            VIDEO_SUBTITLE_SYNC_OFFSET_LIMIT_MS
                        )
                    } else {
                        showVideoPlayerToast(context, "仅支持外挂 SRT 字幕同步")
                    }
                },
                onAudioDelayChange = { nextDelayMs ->
                    audioDelayMs = nextDelayMs.coerceIn(-5_000L, 5_000L)
                },
                onSleepTimerSelected = ::applySleepTimerOption,
                onSleepTimerEndOfVideoChanged = { enabled ->
                    sleepTimerEndOfVideoEnabled = enabled
                    showVideoPlayerToast(
                        context,
                        if (enabled) "已设置当前视频结束后暂停" else "已关闭播完当前视频后停止"
                    )
                },
                onSleepTimerPanelClosed = {
                    if (!isScreenLocked) {
                        showControls = true
                    }
                },
                onSetAbLoopStart = ::setAbLoopStart,
                onSetAbLoopEnd = ::setAbLoopEnd,
                onSetAbLoopStartAt = ::setAbLoopStartAt,
                onSetAbLoopEndAt = ::setAbLoopEndAt,
                onEnableAbLoop = ::enableAbLoop,
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
                abLoopCloseRequest = closeAbLoopBarRequest,
                onAbLoopBarVisibilityChange = { isAbLoopBarVisible = it },
                isQuickToolsExpanded = isQuickToolsExpanded,
                onQuickToolsExpandedChange = { isQuickToolsExpanded = it },
                gestureSettings = gestureSettings,
                onGestureSettingsChange = { gestureSettings = it },
                pictureAdjustmentSettings = pictureAdjustmentSettings,
                onPictureAdjustmentChange = { pictureAdjustmentSettings = it },
                equalizerState = equalizerState,
                onEqualizerBandLevelChange = ::setEqualizerBandLevel,
                onEqualizerPresetSelected = ::applyEqualizerPreset,
                bassBoostState = bassBoostState,
                onBassBoostChange = ::setBassBoostStrength,
                virtualizerState = virtualizerState,
                onVirtualizerChange = ::setVirtualizerStrength,
                onEqualizerReset = ::resetEqualizer,
                keepControlsVisible = keepControlsVisible,
                onKeepControlsVisibleChange = { keepControlsVisible = it }
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
    lastPlaybackError: VideoPlaybackErrorInfo?,
    navigationQueue: List<LocalMediaFile>,
    currentQueueUri: String,
    videoProgressByUri: Map<String, Long>,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onQueueItemSelected: (LocalMediaFile) -> Unit,
    onRemoveUnavailableVideo: (LocalMediaFile) -> Unit,
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
    subtitleSyncAvailable: Boolean,
    subtitleSyncError: String?,
    subtitleStyleSettings: VideoSubtitleStyleSettings,
    sleepTimerMode: VideoSleepTimerMode,
    sleepTimerSelectedOption: VideoSleepTimerOption,
    sleepTimerRemainingMs: Long,
    sleepTimerEndOfVideoEnabled: Boolean,
    abLoopStartMs: Long?,
    abLoopEndMs: Long?,
    onSpeedSelected: (Float) -> Unit,
    onResizeModeSelected: (VideoResizeMode) -> Unit,
    onToggleRepeat: () -> Unit,
    onSubtitleSelect: () -> Unit,
    onSubtitleClear: () -> Unit,
    onSubtitleStyleChanged: (VideoSubtitleStyleSettings) -> Unit,
    onSubtitleOffsetChange: (Long) -> Unit,
    onAudioDelayChange: (Long) -> Unit,
    onSleepTimerSelected: (VideoSleepTimerOption) -> Unit,
    onSleepTimerEndOfVideoChanged: (Boolean) -> Unit,
    onSleepTimerPanelClosed: () -> Unit,
    onSetAbLoopStart: () -> Unit,
    onSetAbLoopEnd: () -> Unit,
    onSetAbLoopStartAt: (Long) -> Unit,
    onSetAbLoopEndAt: (Long) -> Unit,
    onEnableAbLoop: () -> Unit,
    onClearAbLoop: () -> Unit,
    onScreenshot: () -> Unit,
    isPortraitPlayback: Boolean,
    onToggleOrientation: () -> Unit,
    onLockScreen: () -> Unit,
    onSpeedPanelVisibilityChange: (Boolean) -> Unit,
    abLoopCloseRequest: Int,
    onAbLoopBarVisibilityChange: (Boolean) -> Unit,
    isQuickToolsExpanded: Boolean,
    onQuickToolsExpandedChange: (Boolean) -> Unit,
    gestureSettings: VideoGestureSettings,
    onGestureSettingsChange: (VideoGestureSettings) -> Unit,
    pictureAdjustmentSettings: VideoPictureAdjustmentSettings,
    onPictureAdjustmentChange: (VideoPictureAdjustmentSettings) -> Unit,
    equalizerState: VideoEqualizerUiState,
    onEqualizerBandLevelChange: (Short, Short) -> Unit,
    onEqualizerPresetSelected: (VideoEqualizerPreset) -> Unit,
    bassBoostState: VideoAudioEffectStrengthUiState,
    onBassBoostChange: (Int) -> Unit,
    virtualizerState: VideoAudioEffectStrengthUiState,
    onVirtualizerChange: (Int) -> Unit,
    onEqualizerReset: () -> Unit,
    keepControlsVisible: Boolean,
    onKeepControlsVisibleChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val quickToolsScrollState = rememberScrollState()
    var showSpeedPanel by remember { mutableStateOf(false) }
    var showResizePanel by remember { mutableStateOf(false) }
    var showInfoPanel by remember { mutableStateOf(false) }
    var showQueuePanel by remember { mutableStateOf(false) }
    var showAudioTrackPanel by remember { mutableStateOf(false) }
    var showSleepTimerPanel by remember { mutableStateOf(false) }
    var showAbLoopPanel by remember { mutableStateOf(false) }
    var showSubtitleStylePanel by remember { mutableStateOf(false) }
    var showSubtitleSyncPanel by remember { mutableStateOf(false) }
    var showDecodeFormatPanel by remember { mutableStateOf(false) }
    var showControlSettingsPanel by remember { mutableStateOf(false) }
    var showGestureSettingsPanel by remember { mutableStateOf(false) }
    var showEqualizerPanel by remember { mutableStateOf(false) }
    var showPictureAdjustmentPanel by remember { mutableStateOf(false) }
    var showAdvancedFuturePanel by remember { mutableStateOf(false) }
    var showTopToolbarSetting by remember { mutableStateOf(true) }
    var showBottomControlsSetting by remember { mutableStateOf(true) }
    var showProgressTimeSetting by remember { mutableStateOf(true) }
    var abLoopEditTarget by remember { mutableStateOf<VideoAbLoopPoint?>(null) }
    var abLoopEditText by remember { mutableStateOf("") }
    var audioTrackRevision by remember(player) { mutableIntStateOf(0) }
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

    LaunchedEffect(currentQueueUri) {
        showAbLoopPanel = false
        abLoopEditTarget = null
    }

    LaunchedEffect(showAbLoopPanel) {
        onAbLoopBarVisibilityChange(showAbLoopPanel)
    }

    LaunchedEffect(abLoopCloseRequest) {
        if (abLoopCloseRequest > 0) {
            showAbLoopPanel = false
            abLoopEditTarget = null
        }
    }

    fun openSubtitleStylePanel() {
        showSpeedPanel = false
        showResizePanel = false
        showInfoPanel = false
        showQueuePanel = false
        showAudioTrackPanel = false
        showSleepTimerPanel = false
        showAbLoopPanel = false
        showControlSettingsPanel = false
        showGestureSettingsPanel = false
        showSubtitleSyncPanel = false
        showAdvancedFuturePanel = false
        showDecodeFormatPanel = false
        onQuickToolsExpandedChange(false)
        showSubtitleStylePanel = true
    }

    fun openSubtitleSyncPanel() {
        showSpeedPanel = false
        showResizePanel = false
        showInfoPanel = false
        showQueuePanel = false
        showAudioTrackPanel = false
        showSleepTimerPanel = false
        showAbLoopPanel = false
        showSubtitleStylePanel = false
        showControlSettingsPanel = false
        showGestureSettingsPanel = false
        showEqualizerPanel = false
        showPictureAdjustmentPanel = false
        showAdvancedFuturePanel = false
        showDecodeFormatPanel = false
        onQuickToolsExpandedChange(false)
        showSubtitleSyncPanel = true
    }

    fun openAudioTrackPanel() {
        showSpeedPanel = false
        showResizePanel = false
        showInfoPanel = false
        showQueuePanel = false
        showSleepTimerPanel = false
        showAbLoopPanel = false
        showSubtitleStylePanel = false
        showControlSettingsPanel = false
        showGestureSettingsPanel = false
        showAdvancedFuturePanel = false
        showDecodeFormatPanel = false
        onQuickToolsExpandedChange(false)
        audioTrackRevision += 1
        showAudioTrackPanel = true
    }

    fun openSleepTimerPanel() {
        showSpeedPanel = false
        showResizePanel = false
        showInfoPanel = false
        showQueuePanel = false
        showAudioTrackPanel = false
        showAbLoopPanel = false
        showSubtitleStylePanel = false
        showControlSettingsPanel = false
        showGestureSettingsPanel = false
        showAdvancedFuturePanel = false
        showDecodeFormatPanel = false
        onQuickToolsExpandedChange(false)
        showSleepTimerPanel = true
    }

    fun openAbLoopPanel() {
        showSpeedPanel = false
        showResizePanel = false
        showInfoPanel = false
        showQueuePanel = false
        showAudioTrackPanel = false
        showSleepTimerPanel = false
        showSubtitleStylePanel = false
        showControlSettingsPanel = false
        showGestureSettingsPanel = false
        showAdvancedFuturePanel = false
        showDecodeFormatPanel = false
        onQuickToolsExpandedChange(false)
        showAbLoopPanel = true
    }

    fun openInfoPanel() {
        showSpeedPanel = false
        showResizePanel = false
        showQueuePanel = false
        showAudioTrackPanel = false
        showSleepTimerPanel = false
        showAbLoopPanel = false
        showSubtitleStylePanel = false
        showControlSettingsPanel = false
        showGestureSettingsPanel = false
        showAdvancedFuturePanel = false
        showDecodeFormatPanel = false
        onQuickToolsExpandedChange(false)
        showInfoPanel = true
    }

    fun openQueuePanel() {
        showSpeedPanel = false
        showResizePanel = false
        showInfoPanel = false
        showAudioTrackPanel = false
        showSleepTimerPanel = false
        showAbLoopPanel = false
        showSubtitleStylePanel = false
        showControlSettingsPanel = false
        showGestureSettingsPanel = false
        showAdvancedFuturePanel = false
        showDecodeFormatPanel = false
        onQuickToolsExpandedChange(false)
        showQueuePanel = true
    }

    fun openControlSettingsPanel() {
        showSpeedPanel = false
        showResizePanel = false
        showInfoPanel = false
        showQueuePanel = false
        showAudioTrackPanel = false
        showSleepTimerPanel = false
        showAbLoopPanel = false
        showSubtitleStylePanel = false
        showGestureSettingsPanel = false
        showAdvancedFuturePanel = false
        showDecodeFormatPanel = false
        onQuickToolsExpandedChange(false)
        showControlSettingsPanel = true
    }

    fun openGestureSettingsPanel() {
        showSpeedPanel = false
        showResizePanel = false
        showInfoPanel = false
        showQueuePanel = false
        showAudioTrackPanel = false
        showSleepTimerPanel = false
        showAbLoopPanel = false
        showSubtitleStylePanel = false
        showControlSettingsPanel = false
        showAdvancedFuturePanel = false
        showDecodeFormatPanel = false
        onQuickToolsExpandedChange(false)
        showGestureSettingsPanel = true
    }

    fun openEqualizerPanel() {
        showSpeedPanel = false
        showResizePanel = false
        showInfoPanel = false
        showQueuePanel = false
        showAudioTrackPanel = false
        showSleepTimerPanel = false
        showAbLoopPanel = false
        showSubtitleStylePanel = false
        showControlSettingsPanel = false
        showGestureSettingsPanel = false
        showAdvancedFuturePanel = false
        showDecodeFormatPanel = false
        onQuickToolsExpandedChange(false)
        showEqualizerPanel = true
    }

    fun openPictureAdjustmentPanel() {
        showSpeedPanel = false
        showResizePanel = false
        showInfoPanel = false
        showQueuePanel = false
        showAudioTrackPanel = false
        showSleepTimerPanel = false
        showAbLoopPanel = false
        showSubtitleStylePanel = false
        showControlSettingsPanel = false
        showGestureSettingsPanel = false
        showEqualizerPanel = false
        showAdvancedFuturePanel = false
        showDecodeFormatPanel = false
        onQuickToolsExpandedChange(false)
        showPictureAdjustmentPanel = true
    }

    fun openAdvancedFuturePanel() {
        showSpeedPanel = false
        showResizePanel = false
        showInfoPanel = false
        showQueuePanel = false
        showAudioTrackPanel = false
        showSleepTimerPanel = false
        showAbLoopPanel = false
        showSubtitleStylePanel = false
        showControlSettingsPanel = false
        showGestureSettingsPanel = false
        showDecodeFormatPanel = false
        onQuickToolsExpandedChange(false)
        showAdvancedFuturePanel = true
    }

    fun openDecodeFormatPanel() {
        showSpeedPanel = false
        showResizePanel = false
        showInfoPanel = false
        showQueuePanel = false
        showAudioTrackPanel = false
        showSleepTimerPanel = false
        showAbLoopPanel = false
        showSubtitleStylePanel = false
        showControlSettingsPanel = false
        showGestureSettingsPanel = false
        showAdvancedFuturePanel = false
        onQuickToolsExpandedChange(false)
        showDecodeFormatPanel = true
    }

    fun closeSleepTimerPanel() {
        showSleepTimerPanel = false
        onSleepTimerPanelClosed()
    }

    fun openAbLoopTimeEditor(target: VideoAbLoopPoint, currentValueMs: Long?) {
        abLoopEditTarget = target
        abLoopEditText = formatDuration(currentValueMs ?: currentPositionMs)
    }

    BackHandler(enabled = showSpeedPanel || showInfoPanel || showQueuePanel || showAudioTrackPanel || showSleepTimerPanel || showAbLoopPanel || showSubtitleStylePanel || showSubtitleSyncPanel || showDecodeFormatPanel || showControlSettingsPanel || showGestureSettingsPanel || showEqualizerPanel || showPictureAdjustmentPanel || showAdvancedFuturePanel) {
        if (showSubtitleStylePanel) {
            showSubtitleStylePanel = false
        } else if (showSubtitleSyncPanel) {
            showSubtitleSyncPanel = false
        } else if (showDecodeFormatPanel) {
            showDecodeFormatPanel = false
        } else if (showControlSettingsPanel) {
            showControlSettingsPanel = false
        } else if (showGestureSettingsPanel) {
            showGestureSettingsPanel = false
        } else if (showEqualizerPanel) {
            showEqualizerPanel = false
        } else if (showPictureAdjustmentPanel) {
            showPictureAdjustmentPanel = false
        } else if (showAdvancedFuturePanel) {
            showAdvancedFuturePanel = false
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

    LaunchedEffect(showSpeedPanel, showInfoPanel, showQueuePanel, showAudioTrackPanel, showSleepTimerPanel, showSubtitleStylePanel, showSubtitleSyncPanel, showDecodeFormatPanel, showControlSettingsPanel, showGestureSettingsPanel, showEqualizerPanel, showPictureAdjustmentPanel, showAdvancedFuturePanel) {
        onSpeedPanelVisibilityChange(showSpeedPanel || showInfoPanel || showQueuePanel || showAudioTrackPanel || showSleepTimerPanel || showSubtitleStylePanel || showSubtitleSyncPanel || showDecodeFormatPanel || showControlSettingsPanel || showGestureSettingsPanel || showEqualizerPanel || showPictureAdjustmentPanel || showAdvancedFuturePanel)
    }

    DisposableEffect(Unit) {
        onDispose {
            onSpeedPanelVisibilityChange(false)
            onAbLoopBarVisibilityChange(false)
        }
    }

    fun closePanelOrBack() {
        when {
            showSubtitleStylePanel -> showSubtitleStylePanel = false
            showSubtitleSyncPanel -> showSubtitleSyncPanel = false
            showDecodeFormatPanel -> showDecodeFormatPanel = false
            showControlSettingsPanel -> showControlSettingsPanel = false
            showGestureSettingsPanel -> showGestureSettingsPanel = false
            showEqualizerPanel -> showEqualizerPanel = false
            showPictureAdjustmentPanel -> showPictureAdjustmentPanel = false
            showAdvancedFuturePanel -> showAdvancedFuturePanel = false
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

    val panelGestureEnabled = showSubtitleStylePanel ||
        showSubtitleSyncPanel ||
        showDecodeFormatPanel ||
        showAudioTrackPanel ||
        showSleepTimerPanel ||
        showAbLoopPanel ||
        showInfoPanel ||
        showQueuePanel ||
        showSpeedPanel ||
        showResizePanel ||
        showControlSettingsPanel ||
        showGestureSettingsPanel ||
        showEqualizerPanel ||
        showPictureAdjustmentPanel ||
        showAdvancedFuturePanel ||
        isQuickToolsExpanded

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (panelGestureEnabled) {
                    Modifier.pointerInput(panelGestureEnabled) {
                        detectDragGestures { change, dragAmount ->
                            if (change.position.x > size.width - 72.dp.toPx() && dragAmount.x < -26f) {
                                closePanelOrBack()
                                change.consume()
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        val ordinaryControlsVisible = !showSpeedPanel && !showSubtitleStylePanel && !showSubtitleSyncPanel && !showDecodeFormatPanel && !showAudioTrackPanel && !showSleepTimerPanel && !showInfoPanel && !showQueuePanel && !showControlSettingsPanel && !showGestureSettingsPanel && !showEqualizerPanel && !showPictureAdjustmentPanel && !showAdvancedFuturePanel

        if (ordinaryControlsVisible && showTopToolbarSetting) {
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
                            .clickable { openSubtitleStylePanel() }
                            .padding(horizontal = 5.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Filled.Subtitles,
                            contentDescription = "字幕样式",
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
                    .padding(top = 12.dp, end = 16.dp, bottom = 12.dp)
            )
        }

        if (showSubtitleStylePanel) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showSubtitleStylePanel = false }
            )
            SubtitleStylePanel(
                subtitleName = subtitleName,
                subtitleOffsetMs = subtitleOffsetMs,
                subtitleSyncAvailable = subtitleSyncAvailable,
                subtitleSyncError = subtitleSyncError,
                settings = subtitleStyleSettings,
                onSubtitleSelect = onSubtitleSelect,
                onSubtitleClear = onSubtitleClear,
                onOpenSubtitleSync = { openSubtitleSyncPanel() },
                onSettingsChange = onSubtitleStyleChanged,
                onDismiss = { showSubtitleStylePanel = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 14.dp, bottom = 12.dp)
            )
        }

        if (showSubtitleSyncPanel) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showSubtitleSyncPanel = false }
            )
            SubtitleSyncPanel(
                subtitleName = subtitleName,
                subtitleOffsetMs = subtitleOffsetMs,
                subtitleSyncAvailable = subtitleSyncAvailable,
                subtitleSyncError = subtitleSyncError,
                onOffsetChange = onSubtitleOffsetChange,
                onDismiss = { showSubtitleSyncPanel = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 14.dp, bottom = 12.dp)
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
                    .background(Color.Black.copy(alpha = 0.46f))
                    .clickable { closeSleepTimerPanel() }
            )
            SleepTimerPanel(
                mode = sleepTimerMode,
                selectedOption = sleepTimerSelectedOption,
                remainingMs = sleepTimerRemainingMs,
                endOfVideoEnabled = sleepTimerEndOfVideoEnabled,
                onSelect = onSleepTimerSelected,
                onEndOfVideoChange = onSleepTimerEndOfVideoChanged,
                onDismiss = { closeSleepTimerPanel() },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 22.dp, vertical = 18.dp)
            )
        }

        if (showAbLoopPanel) {
            AbLoopFloatingBar(
                startMs = abLoopStartMs,
                endMs = abLoopEndMs,
                isActive = abLoopStartMs != null && abLoopEndMs != null && abLoopEndMs > abLoopStartMs,
                onSetStart = onSetAbLoopStart,
                onSetEnd = onSetAbLoopEnd,
                onEditStart = { openAbLoopTimeEditor(VideoAbLoopPoint.A, abLoopStartMs) },
                onEditEnd = { openAbLoopTimeEditor(VideoAbLoopPoint.B, abLoopEndMs) },
                onEnable = onEnableAbLoop,
                onClear = onClearAbLoop,
                onDismiss = { showAbLoopPanel = false },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 112.dp)
            )
        }

        abLoopEditTarget?.let { target ->
            AbLoopTimeInputDialog(
                target = target,
                value = abLoopEditText,
                durationMs = durationMs,
                onValueChange = { abLoopEditText = it },
                onConfirm = {
                    val parsedMs = parseAbLoopTimeInput(abLoopEditText, durationMs)
                    if (parsedMs == null) {
                        showVideoPlayerToast(context, "请输入有效时间")
                    } else {
                        if (target == VideoAbLoopPoint.A) {
                            onSetAbLoopStartAt(parsedMs)
                        } else {
                            onSetAbLoopEndAt(parsedMs)
                        }
                        abLoopEditTarget = null
                    }
                },
                onDismiss = { abLoopEditTarget = null }
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

        if (showDecodeFormatPanel) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showDecodeFormatPanel = false }
            )
            VideoDecodeFormatPanel(
                mediaFile = mediaFile,
                player = player,
                lastPlaybackError = lastPlaybackError,
                onDismiss = { showDecodeFormatPanel = false },
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
            BoxWithConstraints(
                modifier = Modifier.matchParentSize()
            ) {
                val panelHeight = if (maxHeight > 24.dp) maxHeight - 24.dp else maxHeight
                VideoQueuePanel(
                    queue = navigationQueue,
                    currentUri = currentQueueUri,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    progressByUri = videoProgressByUri,
                    onSelect = { file ->
                        showQueuePanel = false
                        if (file.uri != currentQueueUri) {
                            onQueueItemSelected(file)
                        }
                    },
                    onRemoveUnavailable = onRemoveUnavailableVideo,
                    onDismiss = { showQueuePanel = false },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                        .fillMaxWidth(0.42f)
                        .widthIn(min = 360.dp, max = 520.dp)
                        .requiredHeight(panelHeight)
                )
            }
        }

        if (showControlSettingsPanel) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showControlSettingsPanel = false }
            )
            VideoControlSettingsPanel(
                showTopToolbar = showTopToolbarSetting,
                onShowTopToolbarChange = { showTopToolbarSetting = it },
                showBottomControls = showBottomControlsSetting,
                onShowBottomControlsChange = { showBottomControlsSetting = it },
                showProgressTime = showProgressTimeSetting,
                onShowProgressTimeChange = { showProgressTimeSetting = it },
                keepControlsVisible = keepControlsVisible,
                onKeepControlsVisibleChange = onKeepControlsVisibleChange,
                onReset = {
                    showTopToolbarSetting = true
                    showBottomControlsSetting = true
                    showProgressTimeSetting = true
                    onKeepControlsVisibleChange(false)
                },
                onDismiss = { showControlSettingsPanel = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 56.dp, end = 20.dp)
            )
        }

        if (showGestureSettingsPanel) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showGestureSettingsPanel = false }
            )
            VideoGestureSettingsPanel(
                settings = gestureSettings,
                onSettingsChange = onGestureSettingsChange,
                onDismiss = { showGestureSettingsPanel = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 56.dp, end = 20.dp)
            )
        }

        if (showEqualizerPanel) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showEqualizerPanel = false }
            )
            VideoEqualizerPanel(
                state = equalizerState,
                onBandLevelChange = onEqualizerBandLevelChange,
                onPresetSelected = onEqualizerPresetSelected,
                bassBoostState = bassBoostState,
                onBassBoostChange = onBassBoostChange,
                virtualizerState = virtualizerState,
                onVirtualizerChange = onVirtualizerChange,
                onReset = onEqualizerReset,
                onDismiss = { showEqualizerPanel = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 12.dp, bottom = 8.dp)
            )
        }

        if (showPictureAdjustmentPanel) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showPictureAdjustmentPanel = false }
            )
            VideoPictureAdjustmentPanel(
                settings = pictureAdjustmentSettings,
                onSettingsChange = onPictureAdjustmentChange,
                onDismiss = { showPictureAdjustmentPanel = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 12.dp, bottom = 8.dp)
            )
        }

        if (showAdvancedFuturePanel) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showAdvancedFuturePanel = false }
            )
            VideoAdvancedFuturePanel(
                onDismiss = { showAdvancedFuturePanel = false },
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
                    icon = Icons.Filled.Tune,
                    label = "均衡器",
                    onClick = {
                        openEqualizerPanel()
                    }
                )
                VideoQuickToolButton(
                    icon = Icons.Filled.Brightness6,
                    label = "画调",
                    onClick = {
                        openPictureAdjustmentPanel()
                    }
                )
                if (isQuickToolsExpanded) {
                    VideoQuickToolButton(
                        icon = Icons.Filled.PhotoCamera,
                        label = "截图",
                        onClick = {
                            onScreenshot()
                        }
                    )
                    VideoQuickToolButton(
                        icon = Icons.Filled.Info,
                        label = "信息",
                        onClick = { openInfoPanel() }
                    )
                    VideoQuickToolButton(
                        icon = Icons.Filled.QueueMusic,
                        label = "播放列表",
                        onClick = { openQueuePanel() }
                    )
                    VideoQuickToolButton(
                        icon = Icons.Filled.Loop,
                        label = "AB",
                        onClick = { openAbLoopPanel() }
                    )
                    VideoQuickToolButton(
                        icon = Icons.Filled.Timer,
                        label = "睡眠",
                        onClick = { openSleepTimerPanel() }
                    )
                    VideoQuickToolButton(
                        icon = Icons.Filled.Settings,
                        label = "控制栏",
                        onClick = { openControlSettingsPanel() }
                    )
                    VideoQuickToolButton(
                        icon = Icons.Filled.TouchApp,
                        label = "手势",
                        onClick = { openGestureSettingsPanel() }
                    )
                    VideoQuickToolButton(
                        icon = Icons.Filled.Memory,
                        label = "解码",
                        onClick = { openDecodeFormatPanel() }
                    )
                    VideoQuickToolButton(
                        icon = Icons.Filled.MoreHoriz,
                        label = "高级",
                        onClick = { openAdvancedFuturePanel() }
                    )
                    VideoQuickToolButton(
                        icon = Icons.Filled.KeyboardArrowLeft,
                        label = "收起",
                        onClick = { onQuickToolsExpandedChange(false) }
                    )
                } else {
                    VideoQuickToolButton(
                        icon = Icons.Filled.KeyboardArrowRight,
                        label = "展开",
                        onClick = { onQuickToolsExpandedChange(true) }
                    )
                }
            }

            if (showBottomControlsSetting) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {})
                    }
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
                    if (showProgressTimeSetting) {
                        Text(
                            formatDuration(currentPositionMs),
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    ThinVideoProgressBar(
                        currentPositionMs = currentPositionMs,
                        durationMs = durationMs,
                        onSeekStart = onSeekStart,
                        onSeekPreview = onSeekPreview,
                        onSeekFinished = onSeekFinished,
                        modifier = Modifier.weight(1f)
                    )
                    if (showProgressTimeSetting) {
                        Text(
                            formatDuration(durationMs),
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
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
private fun SubtitleStylePanel(
    subtitleName: String?,
    subtitleOffsetMs: Long,
    subtitleSyncAvailable: Boolean,
    subtitleSyncError: String?,
    settings: VideoSubtitleStyleSettings,
    onSubtitleSelect: () -> Unit,
    onSubtitleClear: () -> Unit,
    onOpenSubtitleSync: () -> Unit,
    onSettingsChange: (VideoSubtitleStyleSettings) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentHsv = remember(settings.color.styleColor) { subtitleColorHsv(settings.color.styleColor) }
    var customHue by remember(settings.color.styleColor) { mutableFloatStateOf(currentHsv[0]) }
    var customSaturation by remember(settings.color.styleColor) { mutableFloatStateOf(currentHsv[1] * 100f) }
    var showCustomColorControls by remember { mutableStateOf(false) }
    val customColor = remember(customHue, customSaturation) {
        subtitleColorFromHueSaturation(customHue, customSaturation)
    }

    SidePanelShell(
        title = "字幕样式",
        onDismiss = onDismiss,
        modifier = modifier
            .width(300.dp)
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
            SubtitleFilePickerSection(
                subtitleName = subtitleName,
                onPickSubtitle = onSubtitleSelect,
                onClearSubtitle = onSubtitleClear
            )
            SubtitleStyleChoiceGroup(
                title = "字号大小",
                options = VideoSubtitleSize.values().toList(),
                selected = settings.size,
                label = { it.label },
                onSelect = { onSettingsChange(settings.copy(size = it)) }
            )
            SubtitleStyleChoiceGroup(
                title = "字幕位置",
                options = listOf(
                    VideoSubtitlePosition.TOP,
                    VideoSubtitlePosition.MIDDLE,
                    VideoSubtitlePosition.BOTTOM
                ),
                selected = settings.position,
                label = { it.label },
                onSelect = { onSettingsChange(settings.copy(position = it)) }
            )
            SubtitleTimingSection(
                subtitleName = subtitleName,
                subtitleOffsetMs = subtitleOffsetMs,
                subtitleSyncAvailable = subtitleSyncAvailable,
                subtitleSyncError = subtitleSyncError,
                onOpenSubtitleSync = onOpenSubtitleSync
            )
            SubtitleStyleChoiceGroup(
                title = "字幕背景",
                options = VideoSubtitleBackground.values().toList(),
                selected = settings.background,
                label = { it.label },
                onSelect = { onSettingsChange(settings.copy(background = it)) },
                columns = 2
            )
            SubtitlePresetColorGrid(
                selectedColor = settings.color,
                onSelect = { onSettingsChange(settings.copy(color = it)) }
            )
            SubtitleCustomColorSection(
                currentColor = settings.color,
                draftColor = customColor,
                expanded = showCustomColorControls,
                onToggleExpanded = { showCustomColorControls = !showCustomColorControls },
                hue = customHue,
                saturation = customSaturation,
                onHueChange = { customHue = it },
                onSaturationChange = { customSaturation = it },
                onApply = { onSettingsChange(settings.copy(color = customColor)) }
            )
        }
    }
}

@Composable
private fun SubtitleFilePickerSection(
    subtitleName: String?,
    onPickSubtitle: () -> Unit,
    onClearSubtitle: () -> Unit
) {
    val hasSubtitle = subtitleName != null
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        SubtitlePanelSectionLabel("字幕选择")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1B263A).copy(alpha = 0.78f))
                .border(1.dp, Color.White.copy(alpha = 0.055f), RoundedCornerShape(10.dp))
                .clickable(onClick = onPickSubtitle)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PlayerMoonPurple.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Subtitles,
                    contentDescription = null,
                    tint = PlayerMoonPurpleSoft,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = subtitleName ?: "选择文件...",
                    color = Color.White.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (hasSubtitle) "已加载，点击可重新选择" else "支持 .srt, .ass, .vtt",
                    color = Color.White.copy(alpha = 0.48f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (hasSubtitle) {
                Text(
                    text = "清除",
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(PlayerMoonPurple.copy(alpha = 0.88f))
                        .border(1.dp, PlayerMoonPurpleSoft.copy(alpha = 0.78f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onClearSubtitle)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
            Text(
                text = "›",
                color = Color.White.copy(alpha = 0.58f),
                fontSize = 24.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SubtitleTimingSection(
    subtitleName: String?,
    subtitleOffsetMs: Long,
    subtitleSyncAvailable: Boolean,
    subtitleSyncError: String?,
    onOpenSubtitleSync: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SubtitlePanelSectionLabel("字幕时间")
            Text(
                text = if (subtitleSyncAvailable) "可设置" else "需外挂 SRT",
                color = if (subtitleSyncAvailable) PlayerMoonPurpleSoft else Color.White.copy(alpha = 0.46f),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (subtitleSyncAvailable) {
                            PlayerMoonPurple.copy(alpha = 0.22f)
                        } else {
                            Color.White.copy(alpha = 0.045f)
                        }
                    )
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1B263A).copy(alpha = 0.42f))
                .border(1.dp, Color.White.copy(alpha = 0.045f), RoundedCornerShape(10.dp))
                .clickable(onClick = onOpenSubtitleSync),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SubtitleTimingCell(
                text = "提前 (-)",
                enabled = subtitleSyncAvailable,
                onClick = onOpenSubtitleSync,
                modifier = Modifier.weight(1f)
            )
            SubtitleTimingCell(
                text = formatSubtitleOffsetSeconds(subtitleOffsetMs),
                enabled = subtitleSyncAvailable,
                onClick = onOpenSubtitleSync,
                modifier = Modifier.weight(0.82f)
            )
            SubtitleTimingCell(
                text = "延后 (+)",
                enabled = subtitleSyncAvailable,
                onClick = onOpenSubtitleSync,
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = subtitleSyncError ?: if (!subtitleSyncAvailable) {
                "请先选择外挂 .srt 字幕，再进行时间同步。"
            } else {
                "第一版仅支持外挂 SRT 字幕时间同步。"
            },
            color = if (subtitleSyncError == null) Color.White.copy(alpha = 0.42f) else Color(0xFFFFC1C1),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SubtitleSyncPanel(
    subtitleName: String?,
    subtitleOffsetMs: Long,
    subtitleSyncAvailable: Boolean,
    subtitleSyncError: String?,
    onOffsetChange: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val offsetSeconds = subtitleOffsetMs / 1000f
    val canSync = subtitleSyncAvailable
    val quickOptions = listOf(
        "-1s" to -1000L,
        "-0.5s" to -500L,
        "重置" to 0L,
        "+0.5s" to 500L,
        "+1s" to 1000L
    )

    SidePanelShell(
        title = "字幕时间同步",
        onDismiss = onDismiss,
        modifier = modifier
            .width(338.dp)
            .fillMaxHeight(PLAYER_SIDE_PANEL_HEIGHT_FRACTION),
        contentPadding = 18.dp,
        contentSpacing = 14.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.035f))
                    .border(1.dp, Color.White.copy(alpha = 0.055f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = if (canSync) "当前字幕：外挂 SRT" else "未加载可同步字幕",
                    color = Color.White.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "仅支持外挂 SRT 字幕同步",
                    color = Color.White.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.labelSmall
                )
                if (!subtitleName.isNullOrBlank()) {
                    Text(
                        text = subtitleName,
                        color = Color.White.copy(alpha = 0.42f),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.025f))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "同步偏移",
                    color = Color.White.copy(alpha = 0.42f),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = formatSubtitleOffsetSecondsForPanel(subtitleOffsetMs),
                    color = if (canSync) Color.White else Color.White.copy(alpha = 0.42f),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Light,
                    maxLines = 1
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                quickOptions.forEach { (label, deltaMs) ->
                    val isReset = label == "重置"
                    val isSelected = isReset && subtitleOffsetMs == 0L
                    SubtitleSyncQuickButton(
                        label = label,
                        enabled = canSync,
                        selected = isSelected,
                        onClick = {
                            val nextOffset = if (isReset) {
                                0L
                            } else {
                                (subtitleOffsetMs + deltaMs).coerceIn(
                                    -VIDEO_SUBTITLE_SYNC_OFFSET_LIMIT_MS,
                                    VIDEO_SUBTITLE_SYNC_OFFSET_LIMIT_MS
                                )
                            }
                            onOffsetChange(nextOffset)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("-5s", color = Color.White.copy(alpha = 0.58f), style = MaterialTheme.typography.labelSmall)
                    Text("+5s", color = Color.White.copy(alpha = 0.58f), style = MaterialTheme.typography.labelSmall)
                }
                ThinEqualizerSlider(
                    value = offsetSeconds,
                    onValueChange = { seconds ->
                        val stepped = (seconds * 10f).roundToInt() * 100L
                        onOffsetChange(stepped)
                    },
                    valueRange = -5f..5f,
                    enabled = canSync,
                    sliderKey = "subtitle-sync-$canSync"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "← 负数：字幕提前",
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "正数：字幕延后 →",
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Text(
                text = subtitleSyncError ?: "注：内嵌字幕及 ASS/SSA 特效字幕暂不支持同步。如遇同步异常，请尝试重新加载字幕文件。",
                color = if (subtitleSyncError == null) {
                    Color.White.copy(alpha = 0.68f)
                } else {
                    Color(0xFFFFC1C1)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelSmall,
                lineHeight = 17.sp
            )
        }

        Text(
            text = "完成",
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(VideoLibrarySelectionPurple)
                .clickable(onClick = onDismiss)
                .padding(vertical = 10.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun SubtitleSyncQuickButton(
    label: String,
    enabled: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = when {
        selected && enabled -> VideoLibrarySelectionPurple
        enabled -> Color.White.copy(alpha = 0.055f)
        else -> Color.White.copy(alpha = 0.035f)
    }
    val textColor = when {
        selected && enabled -> Color.White
        enabled -> Color.White.copy(alpha = 0.86f)
        else -> Color.White.copy(alpha = 0.32f)
    }
    Text(
        text = label,
        color = textColor,
        textAlign = TextAlign.Center,
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .border(1.dp, Color.White.copy(alpha = if (enabled) 0.08f else 0.035f), RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1
    )
}

@Composable
private fun SubtitleTimingCell(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) Color.White.copy(alpha = 0.74f) else Color.White.copy(alpha = 0.42f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SubtitlePanelSectionLabel(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.74f),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun <T> SubtitleStyleChoiceGroup(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    columns: Int = 3
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        SubtitlePanelSectionLabel(title)
        options.chunked(columns).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowOptions.forEach { option ->
                    SubtitleStyleChoiceChip(
                        text = label(option),
                        selected = option == selected,
                        onClick = { onSelect(option) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(columns - rowOptions.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SubtitleStyleChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(
                if (selected) PlayerMoonPurple.copy(alpha = 0.95f)
                else Color(0xFF1D2233).copy(alpha = 0.72f)
            )
            .border(
                width = 1.dp,
                color = if (selected) PlayerMoonPurpleSoft.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.055f),
                shape = RoundedCornerShape(11.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.76f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SubtitlePresetColorGrid(
    selectedColor: VideoSubtitleColor,
    onSelect: (VideoSubtitleColor) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SubtitlePanelSectionLabel("字幕颜色")
        VideoSubtitlePresetColors.chunked(5).forEach { rowColors ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowColors.forEach { color ->
                    SubtitlePresetColorDot(
                        color = color,
                        selected = color.styleColor == selectedColor.styleColor && !selectedColor.isCustom,
                        onClick = { onSelect(color) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(5 - rowColors.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SubtitlePresetColorDot(
    color: VideoSubtitleColor,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(color.previewColor)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) PlayerMoonPurpleSoft else Color.White.copy(alpha = 0.20f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Text(
                    text = "✓",
                    color = if (color.label == "白色" || color.label == "黄色") {
                        Color.Black.copy(alpha = 0.78f)
                    } else {
                        Color.White
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SubtitleCustomColorSection(
    currentColor: VideoSubtitleColor,
    draftColor: VideoSubtitleColor,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    hue: Float,
    saturation: Float,
    onHueChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onApply: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.035f)),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(currentColor.previewColor)
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(5.dp))
            )
            Text(
                text = "自定义颜色",
                color = Color.White.copy(alpha = 0.78f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Text(
                text = subtitleColorHex(currentColor.styleColor),
                color = Color.White.copy(alpha = 0.48f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
            Text(
                text = if (expanded) "⌃" else "⌄",
                color = Color.White.copy(alpha = 0.58f),
                fontSize = 18.sp,
                maxLines = 1
            )
        }
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(draftColor.previewColor)
                            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(7.dp))
                    )
                    Text(
                        text = subtitleColorHex(draftColor.styleColor),
                        color = Color.White.copy(alpha = 0.58f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "应用颜色",
                        color = PlayerMoonPurpleSoft,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(PlayerMoonPurple.copy(alpha = 0.12f))
                            .border(1.dp, PlayerMoonPurpleSoft.copy(alpha = 0.40f), RoundedCornerShape(10.dp))
                            .clickable(onClick = onApply)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
                SubtitleColorSlider("色相", "${round(hue).toInt().coerceIn(0, 360)}°", hue, 0f..360f, onHueChange)
                SubtitleColorSlider(
                    "饱和度",
                    "${round(saturation).toInt().coerceIn(0, 100)}%",
                    saturation,
                    0f..100f,
                    onSaturationChange
                )
            }
        }
    }
}

@Composable
private fun SubtitleColorSlider(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.58f),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(42.dp),
            maxLines = 1
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = videoSliderColors(),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = valueText,
            color = Color.White.copy(alpha = 0.70f),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.End,
            modifier = Modifier.width(38.dp),
            maxLines = 1
        )
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
private fun AbLoopFloatingBar(
    startMs: Long?,
    endMs: Long?,
    isActive: Boolean,
    onSetStart: () -> Unit,
    onSetEnd: () -> Unit,
    onEditStart: () -> Unit,
    onEditEnd: () -> Unit,
    onEnable: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .widthIn(max = 620.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(PlayerPanelDark)
            .clickable(onClick = {})
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "A-B 循环",
                color = if (isActive) PlayerMoonPurpleSoft else Color.White.copy(alpha = 0.90f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "关闭 AB 悬浮条",
                    tint = Color.White.copy(alpha = 0.66f),
                    modifier = Modifier.size(17.dp)
                )
            }
        }
        AbLoopDivider()
        AbLoopPointChip(
            label = "设置点 A",
            timeText = startMs?.let(::formatDuration) ?: "--",
            selected = startMs != null,
            onSet = onSetStart,
            onEdit = onEditStart
        )
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.30f), RoundedCornerShape(99.dp))
        )
        AbLoopPointChip(
            label = "设置点 B",
            timeText = endMs?.let(::formatDuration) ?: "--",
            selected = endMs != null,
            onSet = onSetEnd,
            onEdit = onEditEnd
        )
        AbLoopDivider()
        AbLoopActionChip(
            text = "开启循环",
            highlighted = isActive,
            onClick = onEnable
        )
        AbLoopActionChip(
            text = "重置",
            highlighted = false,
            onClick = onClear
        )
    }
}

@Composable
private fun AbLoopPointChip(
    label: String,
    timeText: String,
    selected: Boolean,
    onSet: () -> Unit,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) PlayerMoonPurple.copy(alpha = 0.20f)
                else Color.White.copy(alpha = 0.045f)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            color = if (selected) PlayerMoonPurpleSoft else Color.White.copy(alpha = 0.68f),
            modifier = Modifier.clickable(onClick = onSet),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            text = timeText,
            color = if (selected) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.44f),
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onEdit)
                .padding(horizontal = 4.dp, vertical = 1.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun AbLoopActionChip(
    text: String,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = text,
        color = if (highlighted) PlayerMoonPurpleSoft else Color.White.copy(alpha = 0.72f),
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (highlighted) PlayerMoonPurple.copy(alpha = 0.16f)
                else Color.White.copy(alpha = 0.045f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1
    )
}

@Composable
private fun AbLoopDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(26.dp)
            .background(Color.White.copy(alpha = 0.08f))
    )
}

@Composable
private fun AbLoopTimeInputDialog(
    target: VideoAbLoopPoint,
    value: String,
    durationMs: Long,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PlayerPanelDark,
        title = {
            Text(
                text = "手动设置 ${target.label} 点",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    label = { Text("时间，例如 00:45 或 1:02:30") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                )
                Text(
                    text = "视频时长 ${durationMs.takeIf { it > 0L }?.let(::formatDuration) ?: "未知"}",
                    color = Color.White.copy(alpha = 0.54f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确定", color = PlayerMoonPurpleSoft)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color.White.copy(alpha = 0.72f))
            }
        }
    )
}

@Composable
private fun SleepTimerPanel(
    mode: VideoSleepTimerMode,
    selectedOption: VideoSleepTimerOption,
    remainingMs: Long,
    endOfVideoEnabled: Boolean,
    onSelect: (VideoSleepTimerOption) -> Unit,
    onEndOfVideoChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTimedActive = mode == VideoSleepTimerMode.TIMED && remainingMs > 0L
    val isAnyTimerActive = isTimedActive || endOfVideoEnabled || mode == VideoSleepTimerMode.END_OF_VIDEO
    val activeRemainingMinutes = remember(remainingMs) { remainingMs.toDisplayMinutes() }
    var draftMinutes by remember(mode, selectedOption) {
        mutableIntStateOf(
            when {
                isTimedActive -> activeRemainingMinutes
                selectedOption.mode == VideoSleepTimerMode.TIMED -> {
                    ((selectedOption.durationMs ?: VIDEO_SLEEP_TIMER_DEFAULT_MINUTES * 60_000L) / 60_000L)
                        .toInt()
                        .coerceIn(VIDEO_SLEEP_TIMER_MIN_MINUTES, VIDEO_SLEEP_TIMER_MAX_MINUTES)
                }
                else -> VIDEO_SLEEP_TIMER_DEFAULT_MINUTES
            }
        )
    }
    var draftEndOfVideoEnabled by remember(mode, selectedOption, endOfVideoEnabled) {
        mutableStateOf(
            mode == VideoSleepTimerMode.END_OF_VIDEO ||
                selectedOption.mode == VideoSleepTimerMode.END_OF_VIDEO ||
                endOfVideoEnabled
        )
    }
    var userAdjustedTime by remember(mode, selectedOption) { mutableStateOf(false) }
    val displayedMinutes = if (isTimedActive && !userAdjustedTime) activeRemainingMinutes else draftMinutes
    val normalizedDisplayMinutes = displayedMinutes.coerceIn(0, VIDEO_SLEEP_TIMER_MAX_MINUTES)
    val displayHours = normalizedDisplayMinutes / 60
    val displayMinutes = normalizedDisplayMinutes % 60
    val selectedQuickMinutes = if (draftEndOfVideoEnabled) {
        null
    } else if (isTimedActive && !userAdjustedTime) {
        selectedOption.durationMs?.let { (it / 60_000L).toInt() }
    } else {
        draftMinutes
    }
    val canStartTimer = draftEndOfVideoEnabled || draftMinutes >= VIDEO_SLEEP_TIMER_MIN_MINUTES
    val primaryText = "确认"

    fun updateDraft(minutes: Int) {
        draftMinutes = minutes.coerceIn(0, VIDEO_SLEEP_TIMER_MAX_MINUTES)
        draftEndOfVideoEnabled = false
        userAdjustedTime = true
    }

    val draftOption = remember(draftMinutes) {
        VideoSleepTimerOption(
            label = formatSleepTimerOptionLabel(draftMinutes),
            displayLabel = formatSleepTimerOptionLabel(draftMinutes),
            mode = VideoSleepTimerMode.TIMED,
            durationMs = draftMinutes.coerceAtLeast(0) * 60_000L,
            isCustom = true
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth(0.58f)
            .widthIn(min = 420.dp, max = 560.dp)
            .heightIn(max = 360.dp)
            .clip(RoundedCornerShape(34.dp))
            .background(PlayerPanelDark)
            .clickable(onClick = {})
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "睡眠定时",
                color = Color.White.copy(alpha = 0.86f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center),
                maxLines = 1
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(28.dp)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "关闭睡眠定时",
                    tint = Color.White.copy(alpha = 0.62f),
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SleepTimerTimeNumber(
                    value = displayHours.toString(),
                    label = "小时",
                    active = normalizedDisplayMinutes > 0,
                    width = 118.dp,
                    dragStepPx = SLEEP_TIMER_HOUR_DRAG_STEP_PX,
                    onStep = { deltaHours ->
                        updateDraft(normalizedDisplayMinutes + deltaHours * 60)
                    }
                )
                Text(
                    text = ":",
                    color = Color.White.copy(alpha = 0.34f),
                    fontSize = 48.sp,
                    lineHeight = 46.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(24.dp),
                    maxLines = 1
                )
                SleepTimerTimeNumber(
                    value = displayMinutes.toString().padStart(2, '0'),
                    label = "分钟",
                    active = normalizedDisplayMinutes > 0,
                    width = 118.dp,
                    dragStepPx = SLEEP_TIMER_MINUTE_DRAG_STEP_PX,
                    onStep = { deltaMinutes ->
                        updateDraft(normalizedDisplayMinutes + deltaMinutes)
                    }
                )
            }
            Text(
                text = if (draftEndOfVideoEnabled) {
                    "点击确认后将在当前视频结束时暂停"
                } else if (isTimedActive && !userAdjustedTime) {
                    "剩余 ${formatSleepTimerCountdown(remainingMs)} · 拖动后点击确认更新"
                } else {
                    "上下拖动修改时间，点击确认后开始倒计时"
                },
                color = if (draftEndOfVideoEnabled || (isTimedActive && !userAdjustedTime)) {
                    PlayerMoonPurpleSoft.copy(alpha = 0.78f)
                } else {
                    Color.White.copy(alpha = 0.42f)
                },
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VideoSleepTimerFixedOptions.forEach { option ->
                val optionMinutes = ((option.durationMs ?: 0L) / 60_000L).toInt()
                SleepTimerQuickChip(
                    label = option.displayLabel,
                    selected = optionMinutes == selectedQuickMinutes,
                    onClick = {
                        updateDraft(optionMinutes)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.055f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Filled.Loop,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.62f),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "播完当前视频后停止",
                color = Color.White.copy(alpha = 0.74f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            SleepTimerToggle(
                checked = draftEndOfVideoEnabled,
                onClick = {
                    draftEndOfVideoEnabled = !draftEndOfVideoEnabled
                    userAdjustedTime = false
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SleepTimerModalButton(
                text = "取消",
                primary = false,
                enabled = true,
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            SleepTimerModalButton(
                text = "重置",
                primary = false,
                enabled = true,
                onClick = {
                    if (isAnyTimerActive) {
                        onSelect(VideoSleepTimerOption.Off)
                    }
                    draftEndOfVideoEnabled = false
                    updateDraft(VIDEO_SLEEP_TIMER_DEFAULT_MINUTES)
                },
                modifier = Modifier.weight(1f)
            )
            SleepTimerModalButton(
                text = primaryText,
                primary = true,
                enabled = canStartTimer,
                onClick = {
                    if (canStartTimer) {
                        onSelect(
                            if (draftEndOfVideoEnabled) {
                                VideoSleepTimerOption.EndOfVideo
                            } else {
                                draftOption
                            }
                        )
                        userAdjustedTime = false
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SleepTimerTimeNumber(
    value: String,
    label: String,
    active: Boolean,
    width: Dp,
    dragStepPx: Float,
    onStep: (Int) -> Unit
) {
    var dragRemainder by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val latestOnStep by rememberUpdatedState(onStep)
    Column(
        modifier = Modifier
            .width(width)
            .height(82.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isDragging) PlayerMoonPurple.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        dragRemainder = 0f
                    },
                    onDragEnd = {
                        isDragging = false
                        dragRemainder = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        dragRemainder = 0f
                    },
                    onDrag = { change, dragAmount ->
                        dragRemainder += dragAmount.y
                        val steps = (-dragRemainder / dragStepPx).toInt()
                        if (steps != 0) {
                            latestOnStep(steps)
                            dragRemainder += steps * dragStepPx
                        }
                        change.consume()
                    }
                )
            }
            .padding(horizontal = 10.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = value,
            color = if (active) PlayerMoonPurpleSoft else Color.White.copy(alpha = 0.32f),
            fontSize = 48.sp,
            lineHeight = 50.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.58f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun SleepTimerQuickChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(31.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (selected) PlayerMoonPurple.copy(alpha = 0.36f)
                else Color.White.copy(alpha = 0.045f)
            )
            .border(
                width = 1.dp,
                color = if (selected) PlayerMoonPurpleSoft.copy(alpha = 0.52f) else Color.White.copy(alpha = 0.075f),
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.78f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SleepTimerToggle(
    checked: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(42.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (checked) PlayerMoonPurple.copy(alpha = 0.72f)
                else Color.White.copy(alpha = 0.16f)
            )
            .clickable(onClick = onClick)
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.94f))
        )
    }
}

@Composable
private fun SleepTimerModalButton(
    text: String,
    primary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    primary && enabled -> PlayerMoonPurpleSoft.copy(alpha = 0.92f)
                    primary -> Color.White.copy(alpha = 0.10f)
                    else -> Color.White.copy(alpha = 0.075f)
                }
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = when {
                primary && enabled -> PlayerPanelDark
                primary -> Color.White.copy(alpha = 0.36f)
                else -> Color.White.copy(alpha = 0.76f)
            },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

private fun Long.toDisplayMinutes(): Int {
    if (this <= 0L) return 0
    return ((this + 59_999L) / 60_000L)
        .toInt()
        .coerceIn(0, VIDEO_SLEEP_TIMER_MAX_MINUTES)
}

private fun formatSleepTimerOptionLabel(minutes: Int): String {
    val safeMinutes = minutes.coerceIn(0, VIDEO_SLEEP_TIMER_MAX_MINUTES)
    val hours = safeMinutes / 60
    val restMinutes = safeMinutes % 60
    return when {
        safeMinutes <= 0 -> "0 分钟"
        hours <= 0 -> "$restMinutes 分钟"
        restMinutes == 0 -> "$hours 小时"
        else -> "$hours 小时 $restMinutes 分钟"
    }
}

@Composable
private fun VideoDecodeFormatPanel(
    mediaFile: LocalMediaFile,
    player: ExoPlayer,
    lastPlaybackError: VideoPlaybackErrorInfo?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val videoInfo = remember(player.currentTracks) { player.currentDecodeTrackInfo(C.TRACK_TYPE_VIDEO) }
    val audioInfo = remember(player.currentTracks) { player.currentDecodeTrackInfo(C.TRACK_TYPE_AUDIO) }
    val subtitleInfos = remember(player.currentTracks) { player.currentSubtitleTrackInfos() }
    val extension = mediaFile.extension.trim().lowercase(Locale.US).ifBlank { "未知" }

    SidePanelShell(
        title = "解码与格式",
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
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            VideoDecodeSectionHeader("基本信息")
            VideoInfoRow("播放内核", "Media3 / 系统解码")
            VideoInfoRow("文件格式", extension)
            Text(
                text = "只读诊断：本页只展示当前文件和轨道信息，不提供硬解/软解切换，也不代表万能解码。",
                color = Color.White.copy(alpha = 0.52f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.032f))
                    .padding(horizontal = 11.dp, vertical = 8.dp)
            )

            VideoDecodeSectionHeader("视频轨道")
            VideoInfoRow("视频编码类型", videoInfo?.mimeType ?: "未知")
            VideoInfoRow("编码标识", videoInfo?.codec ?: "未知", maxValueLines = 2)
            VideoInfoRow("分辨率", videoInfo?.resolution ?: "未知")
            VideoInfoRow("帧率", videoInfo?.frameRate ?: "未知")
            VideoInfoRow("系统报告", videoInfo?.supportText ?: "未知")

            VideoDecodeSectionHeader("音频轨道")
            VideoInfoRow("音频编码类型", audioInfo?.mimeType ?: "未知")
            VideoInfoRow("编码标识", audioInfo?.codec ?: "未知", maxValueLines = 2)
            VideoInfoRow("声道", audioInfo?.channelCount ?: "未知")
            VideoInfoRow("采样率", audioInfo?.sampleRate ?: "未知")
            VideoInfoRow("系统报告", audioInfo?.supportText ?: "未知")

            if (subtitleInfos.isNotEmpty()) {
                VideoDecodeSectionHeader("字幕轨道")
                subtitleInfos.take(3).forEachIndexed { index, subtitleInfo ->
                    VideoInfoRow(
                        label = "字幕 ${index + 1}",
                        value = subtitleInfo.displayText,
                        maxValueLines = 2
                    )
                }
            } else {
                VideoDecodeSectionHeader("字幕轨道")
                VideoInfoRow("字幕轨道", "暂无字幕轨道")
            }

            if (lastPlaybackError != null) {
                VideoDecodeSectionHeader("最近播放错误")
                VideoInfoRow("类型", lastPlaybackError.title)
                VideoInfoRow("说明", lastPlaybackError.detail, maxValueLines = 2)
                VideoInfoRow("错误码", lastPlaybackError.code, maxValueLines = 2)
                VideoInfoRow("时间", lastPlaybackError.timeText)
                VideoInfoRow("原始摘要", lastPlaybackError.rawSummary, maxValueLines = 3)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.045f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "兼容性说明",
                    color = Color.White.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "本页只展示当前文件和轨道的可读信息，不提供硬解/软解切换，也不声明万能解码。未知表示当前播放器未拿到可靠数据。",
                    color = Color.White.copy(alpha = 0.52f),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "手动文件夹识别：mp4 / mkv / webm / avi / mov / m4v / 3gp / 3gpp / ts / m2ts / mts / flv / wmv / asf；系统媒体库扫描仍由 Android MediaStore 决定。",
                    color = Color.White.copy(alpha = 0.44f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun VideoDecodeSectionHeader(text: String) {
    Text(
        text = text,
        color = PlayerMoonPurpleSoft.copy(alpha = 0.94f),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp)
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
    progressByUri: Map<String, Long>,
    onSelect: (LocalMediaFile) -> Unit,
    onRemoveUnavailable: (LocalMediaFile) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val currentFile = queue.firstOrNull { it.uri == currentUri }
    val folderName = currentFile?.parentFolderName?.takeIf { it.isNotBlank() }
        ?: queue.firstOrNull()?.parentFolderName?.takeIf { it.isNotBlank() }
        ?: "未知文件夹"
    val currentIndex = queue.indexOfFirst { it.uri == currentUri }
    var searchQuery by remember(queue) { mutableStateOf("") }
    var selectedFilter by remember(queue) { mutableStateOf(VideoQueueFilter.All) }
    var unavailableByUri by remember(queue) { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    LaunchedEffect(queue) {
        unavailableByUri = withContext(Dispatchers.IO) {
            queue.associate { file ->
                file.uri to !VideoThumbnailStore.isSourceReadable(context, file)
            }
        }
    }

    val entries = remember(queue, currentUri, currentPositionMs, durationMs, progressByUri, unavailableByUri) {
        queue.mapIndexed { index, file ->
            val isCurrent = file.uri == currentUri
            val progressMs = if (isCurrent) {
                currentPositionMs.coerceAtLeast(0L)
            } else {
                progressByUri[file.uri]?.coerceAtLeast(0L) ?: 0L
            }
            val itemDurationMs = if (isCurrent && durationMs > 0L) {
                durationMs
            } else {
                file.durationMs?.takeIf { it > 0L } ?: 0L
            }
            VideoQueueEntry(
                index = index,
                file = file,
                isCurrent = isCurrent,
                isUnavailable = unavailableByUri[file.uri] == true,
                progressMs = progressMs,
                durationMs = itemDurationMs
            )
        }
    }

    val filteredEntries = remember(entries, selectedFilter, searchQuery) {
        val query = searchQuery.trim()
        entries.filter { entry ->
            entry.matchesFilter(selectedFilter) && entry.matchesSearch(query)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(PlayerPanelDark)
            .clickable(onClick = {})
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = null,
                        tint = VideoLibrarySelectionPurple,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = "视频列表",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "关闭播放列表",
                        tint = Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            VideoQueueSearchBar(
                folderName = folderName,
                queueSize = queue.size,
                currentIndex = currentIndex,
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VideoQueueFilter.values().forEach { filter ->
                    VideoQueueFilterChip(
                        label = filter.label,
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter }
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (filteredEntries.isEmpty()) {
                    item {
                        Text(
                            text = if (queue.isEmpty()) "当前没有可展示的视频" else "暂无匹配视频",
                            color = Color.White.copy(alpha = 0.66f),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.035f))
                                .padding(horizontal = 12.dp, vertical = 16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    itemsIndexed(filteredEntries, key = { _, entry -> entry.file.uri }) { _, entry ->
                        VideoQueueItem(
                            entry = entry,
                            onClick = {
                                if (entry.isUnavailable) {
                                    Toast.makeText(context, "文件已失效，无法播放", Toast.LENGTH_SHORT).show()
                                } else {
                                    onSelect(entry.file)
                                }
                            },
                            onRemoveUnavailable = {
                                onRemoveUnavailable(entry.file)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoQueueSearchBar(
    folderName: String,
    queueSize: Int,
    currentIndex: Int,
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val indexText = if (currentIndex >= 0 && queueSize > 0) {
        " · ${currentIndex + 1}/$queueSize"
    } else {
        ""
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.055f))
            .border(1.dp, Color.White.copy(alpha = 0.055f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$folderName · $queueSize 个视频",
            color = Color.White.copy(alpha = 0.84f),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        Text(
            text = indexText.trim(),
            color = VideoLibrarySelectionPurple,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .widthIn(min = 116.dp, max = 160.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.labelMedium.copy(
                    color = Color.White.copy(alpha = 0.90f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 28.dp)
            )
            if (query.isBlank()) {
                Text(
                    text = "搜索当前列表",
                    color = Color.White.copy(alpha = 0.38f),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.68f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(20.dp)
            )
        }
    }
}

@Composable
private fun VideoQueueFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = label,
        color = if (selected) Color.White else Color.White.copy(alpha = 0.74f),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) VideoLibrarySelectionPurple else Color.White.copy(alpha = 0.075f))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 7.dp),
        maxLines = 1
    )
}

@Composable
private fun VideoQueueItem(
    entry: VideoQueueEntry,
    onClick: () -> Unit,
    onRemoveUnavailable: () -> Unit
) {
    val backgroundColor = when {
        entry.isCurrent -> VideoLibrarySelectionPurple.copy(alpha = 0.20f)
        entry.isUnavailable -> Color.White.copy(alpha = 0.030f)
        else -> Color.Transparent
    }
    val titleColor = when {
        entry.isUnavailable -> Color.White.copy(alpha = 0.62f)
        else -> Color.White.copy(alpha = 0.92f)
    }
    val metaText = entry.metaText()
    val progressFraction = entry.progressFraction()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 78.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        VideoQueueThumbnail(
            file = entry.file,
            durationMs = entry.durationMs,
            isCurrent = entry.isCurrent,
            isUnavailable = entry.isUnavailable,
            modifier = Modifier
                .width(104.dp)
                .height(62.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.file.name.ifBlank { "未知视频" },
                    color = titleColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (entry.isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (entry.isCurrent) {
                    Text(
                        text = "正在播放",
                        color = VideoLibrarySelectionPurple,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(VideoLibrarySelectionPurple.copy(alpha = 0.16f))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        maxLines = 1
                    )
                }
            }

            if (metaText.isNotBlank()) {
                Text(
                    text = metaText,
                    color = Color.White.copy(alpha = if (entry.isUnavailable) 0.48f else 0.64f),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!entry.isUnavailable && progressFraction > 0f) {
                VideoQueueProgressBar(
                    fraction = progressFraction,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (entry.isUnavailable) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "无法播放",
                        color = Color.White.copy(alpha = 0.54f),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                    Text(
                        text = "从列表移除",
                        color = Color.White.copy(alpha = 0.88f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.070f))
                            .clickable(onClick = onRemoveUnavailable)
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        maxLines = 1
                    )
                }
            } else if (entry.progressMs > 0L) {
                Text(
                    text = "上次 ${formatDuration(entry.progressMs)}",
                    color = if (entry.isCurrent) VideoLibrarySelectionPurple else Color.White.copy(alpha = 0.58f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.58f),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun VideoQueueThumbnail(
    file: LocalMediaFile,
    durationMs: Long,
    isCurrent: Boolean,
    isUnavailable: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var thumbnail by remember(file.uri, file.modifiedAt, file.size, isUnavailable) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(file.uri, file.modifiedAt, file.size, isUnavailable) {
        thumbnail = null
        if (!isUnavailable) {
            thumbnail = withContext(Dispatchers.IO) {
                VideoThumbnailStore.loadOrCreate(context, file, durationMs.takeIf { it > 0L })
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(Color.White.copy(alpha = 0.070f)),
        contentAlignment = Alignment.Center
    ) {
        if (isUnavailable) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.46f),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "文件已失效",
                    color = Color.White.copy(alpha = 0.58f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        } else if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.40f),
                modifier = Modifier.size(26.dp)
            )
        }

        if (!isUnavailable && durationMs > 0L) {
            Text(
                text = formatDuration(durationMs),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.58f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun VideoQueueProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(4.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.12f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(VideoLibrarySelectionPurple)
        )
    }
}

private enum class VideoQueueFilter(val label: String) {
    All("全部"),
    Unwatched("未看"),
    Watched("已看"),
    Unavailable("失效")
}

private data class VideoQueueEntry(
    val index: Int,
    val file: LocalMediaFile,
    val isCurrent: Boolean,
    val isUnavailable: Boolean,
    val progressMs: Long,
    val durationMs: Long
) {
    fun progressFraction(): Float {
        val duration = durationMs.takeIf { it > 0L } ?: return 0f
        return (progressMs.coerceIn(0L, duration) / duration.toFloat()).coerceIn(0f, 1f)
    }

    fun matchesFilter(filter: VideoQueueFilter): Boolean = when (filter) {
        VideoQueueFilter.All -> true
        VideoQueueFilter.Unwatched -> !isUnavailable && progressMs <= 0L
        VideoQueueFilter.Watched -> !isUnavailable && progressMs > 0L
        VideoQueueFilter.Unavailable -> isUnavailable
    }

    fun matchesSearch(query: String): Boolean {
        if (query.isBlank()) return true
        return file.name.contains(query, ignoreCase = true) ||
            file.parentFolderName.orEmpty().contains(query, ignoreCase = true) ||
            file.extension.contains(query, ignoreCase = true)
    }

    fun metaText(): String {
        if (isUnavailable) {
            return listOfNotNull(
                formatFileSize(file.size.coerceAtLeast(0L)).takeIf { it.isNotBlank() },
                formatVideoQueueDate(file.modifiedAt)
            ).joinToString(" · ")
        }
        return listOfNotNull(
            durationMs.takeIf { it > 0L }?.let(::formatDuration),
            formatFileSize(file.size.coerceAtLeast(0L)).takeIf { it.isNotBlank() },
            formatVideoQueueDate(file.modifiedAt)
        ).joinToString(" · ")
    }
}

private fun formatVideoQueueDate(timestampMs: Long?): String? {
    val value = timestampMs?.takeIf { it > 0L } ?: return null
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(value))
}

private data class VideoGestureSettings(
    val doubleTapSeekEnabled: Boolean = true,
    val horizontalSeekEnabled: Boolean = true,
    val brightnessGestureEnabled: Boolean = true,
    val volumeGestureEnabled: Boolean = true,
    val showHints: Boolean = true
)

private enum class VideoPicturePreset(
    val label: String,
    val brightnessPercent: Int,
    val contrastPercent: Int,
    val saturationPercent: Int,
    val temperatureStep: Int
) {
    Default("默认", 100, 100, 100, 0),
    Bright("明亮", 116, 106, 106, -1),
    Cinema("影院", 94, 116, 92, 1),
    EyeCare("护眼", 98, 94, 88, 2),
    Vivid("鲜艳", 106, 112, 132, -1)
}

private data class VideoPictureAdjustmentSettings(
    val enabled: Boolean = true,
    val selectedPreset: VideoPicturePreset? = VideoPicturePreset.Default,
    val brightnessPercent: Int = 100,
    val contrastPercent: Int = 100,
    val saturationPercent: Int = 100,
    val temperatureStep: Int = 0
) {
    fun withPreset(preset: VideoPicturePreset): VideoPictureAdjustmentSettings = copy(
        enabled = true,
        selectedPreset = preset,
        brightnessPercent = preset.brightnessPercent,
        contrastPercent = preset.contrastPercent,
        saturationPercent = preset.saturationPercent,
        temperatureStep = preset.temperatureStep
    )

    fun withoutPreset(): VideoPictureAdjustmentSettings = copy(selectedPreset = null)

    fun reset(): VideoPictureAdjustmentSettings = VideoPictureAdjustmentSettings().withPreset(VideoPicturePreset.Default)

    val isNeutral: Boolean
        get() = brightnessPercent == 100 &&
            contrastPercent == 100 &&
            saturationPercent == 100 &&
            temperatureStep == 0
}

private enum class VideoEqualizerStatus {
    WAITING_SESSION,
    AVAILABLE,
    UNSUPPORTED
}

private enum class VideoEqualizerPreset(val label: String) {
    Normal("默认"),
    Bass("重低音"),
    Classical("古典"),
    Pop("流行"),
    Rock("摇滚")
}

private data class VideoEqualizerBandUi(
    val index: Short,
    val centerFrequencyHz: Float,
    val frequencyText: String,
    val level: Short
)

private data class VideoEqualizerUiState(
    val status: VideoEqualizerStatus,
    val message: String,
    val enabled: Boolean = false,
    val minBandLevel: Short = 0,
    val maxBandLevel: Short = 0,
    val bands: List<VideoEqualizerBandUi> = emptyList(),
    val selectedPreset: VideoEqualizerPreset? = VideoEqualizerPreset.Normal
) {
    val isAvailable: Boolean
        get() = status == VideoEqualizerStatus.AVAILABLE

    companion object {
        fun waiting(): VideoEqualizerUiState = VideoEqualizerUiState(
            status = VideoEqualizerStatus.WAITING_SESSION,
            message = "等待播放器音频会话"
        )

        fun unsupported(reason: String = "当前设备不支持系统均衡器"): VideoEqualizerUiState =
            VideoEqualizerUiState(
                status = VideoEqualizerStatus.UNSUPPORTED,
                message = reason.ifBlank { "当前设备不支持系统均衡器" }
            )
    }
}

private data class VideoAudioEffectStrengthUiState(
    val supported: Boolean,
    val message: String,
    val percent: Int = 0
) {
    companion object {
        fun waiting(): VideoAudioEffectStrengthUiState = VideoAudioEffectStrengthUiState(
            supported = false,
            message = "等待音频会话"
        )

        fun unsupported(reason: String): VideoAudioEffectStrengthUiState = VideoAudioEffectStrengthUiState(
            supported = false,
            message = reason.ifBlank { "当前设备不支持" }
        )

        fun supported(percent: Int): VideoAudioEffectStrengthUiState = VideoAudioEffectStrengthUiState(
            supported = true,
            message = "可用",
            percent = percent.coerceIn(0, 100)
        )
    }
}

@Composable
private fun VideoPictureAdjustmentPanel(
    settings: VideoPictureAdjustmentSettings,
    onSettingsChange: (VideoPictureAdjustmentSettings) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val renderSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val controlsEnabled = renderSupported && settings.enabled
    Column(
        modifier = modifier
            .width(352.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(18.dp))
            .background(PlayerPanelDark)
            .clickable(onClick = {})
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = null,
                    tint = VideoLibrarySelectionPurple,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "画面调节",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("效果", color = Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.labelSmall)
                VideoPictureEffectToggle(
                    checked = settings.enabled,
                    enabled = renderSupported,
                    onCheckedChange = { onSettingsChange(settings.copy(enabled = it)) }
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "关闭",
                        tint = Color.White.copy(alpha = 0.76f)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 2.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            if (!renderSupported) {
                VideoPictureUnsupportedCard()
            }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "画面预设",
                    color = Color.White.copy(alpha = 0.48f),
                    style = MaterialTheme.typography.labelSmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    VideoPicturePreset.values().forEach { preset ->
                        VideoPicturePresetChip(
                            preset = preset,
                            selected = settings.selectedPreset == preset,
                            enabled = renderSupported,
                            onClick = { onSettingsChange(settings.withPreset(preset)) }
                        )
                    }
                }
            }

            Text(
                text = "细致微调",
                color = Color.White.copy(alpha = 0.48f),
                style = MaterialTheme.typography.labelSmall
            )
            VideoPictureAdjustmentSliderRow(
                sliderKey = "picture-brightness",
                title = "亮度",
                valueText = "${settings.brightnessPercent}%",
                value = settings.brightnessPercent.toFloat(),
                valueRange = 70f..130f,
                enabled = controlsEnabled,
                onValueChange = { next ->
                    onSettingsChange(
                        settings.copy(brightnessPercent = round(next).toInt().coerceIn(70, 130)).withoutPreset()
                    )
                }
            )
            VideoPictureAdjustmentSliderRow(
                sliderKey = "picture-contrast",
                title = "对比度",
                valueText = "${settings.contrastPercent}%",
                value = settings.contrastPercent.toFloat(),
                valueRange = 70f..130f,
                enabled = controlsEnabled,
                onValueChange = { next ->
                    onSettingsChange(
                        settings.copy(contrastPercent = round(next).toInt().coerceIn(70, 130)).withoutPreset()
                    )
                }
            )
            VideoPictureAdjustmentSliderRow(
                sliderKey = "picture-saturation",
                title = "饱和度",
                valueText = "${settings.saturationPercent}%",
                value = settings.saturationPercent.toFloat(),
                valueRange = 50f..150f,
                enabled = controlsEnabled,
                onValueChange = { next ->
                    onSettingsChange(
                        settings.copy(saturationPercent = round(next).toInt().coerceIn(50, 150)).withoutPreset()
                    )
                }
            )
            VideoPictureAdjustmentSliderRow(
                sliderKey = "picture-temperature",
                title = "色温",
                valueText = settings.temperatureStep.temperatureLabel(),
                value = settings.temperatureStep.toFloat(),
                valueRange = -2f..2f,
                enabled = controlsEnabled,
                onValueChange = { next ->
                    onSettingsChange(
                        settings.copy(temperatureStep = round(next).toInt().coerceIn(-2, 2)).withoutPreset()
                    )
                }
            )
            Text(
                text = "仅调整当前播放页画面显示，不修改原视频文件。",
                color = Color.White.copy(alpha = 0.42f),
                style = MaterialTheme.typography.labelSmall,
                lineHeight = 15.sp
            )
        }

        TextButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.textButtonColors(
                containerColor = VideoLibrarySelectionPurple.copy(alpha = VideoLibrarySelectionContainerAlpha),
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Text("完成", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun VideoPicturePresetChip(
    preset: VideoPicturePreset,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(26.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(
                when {
                    !enabled -> Color.White.copy(alpha = 0.035f)
                    selected -> VideoLibrarySelectionPurple.copy(alpha = VideoLibrarySelectionContainerAlpha)
                    else -> Color.White.copy(alpha = 0.055f)
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = preset.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = when {
                !enabled -> Color.White.copy(alpha = 0.32f)
                selected -> Color.White
                else -> Color.White.copy(alpha = 0.76f)
            },
            maxLines = 1
        )
    }
}

@Composable
private fun VideoPictureAdjustmentSliderRow(
    sliderKey: Any,
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            color = Color.White.copy(alpha = if (enabled) 0.76f else 0.42f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.width(48.dp)
        )
        ThinEqualizerSlider(
            sliderKey = sliderKey,
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = valueText,
            color = Color.White.copy(alpha = if (enabled) 0.72f else 0.38f),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(48.dp)
        )
    }
}

@Composable
private fun VideoPictureEffectToggle(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .width(38.dp)
            .height(20.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(
                when {
                    !enabled -> Color.White.copy(alpha = 0.10f)
                    checked -> VideoLibrarySelectionPurple.copy(alpha = 0.72f)
                    else -> Color.White.copy(alpha = 0.14f)
                }
            )
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = if (enabled) 0.94f else 0.42f))
        )
    }
}

@Composable
private fun VideoPictureUnsupportedCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.045f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "当前系统版本暂不支持画面滤镜",
            color = Color.White.copy(alpha = 0.78f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "画面调节依赖 Android 12 及以上系统渲染层能力；本页不会显示假成功。",
            color = Color.White.copy(alpha = 0.48f),
            style = MaterialTheme.typography.labelSmall,
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun VideoEqualizerPanel(
    state: VideoEqualizerUiState,
    onBandLevelChange: (Short, Short) -> Unit,
    onPresetSelected: (VideoEqualizerPreset) -> Unit,
    bassBoostState: VideoAudioEffectStrengthUiState,
    onBassBoostChange: (Int) -> Unit,
    virtualizerState: VideoAudioEffectStrengthUiState,
    onVirtualizerChange: (Int) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(352.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(18.dp))
            .background(PlayerPanelDark)
            .clickable(onClick = {})
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = null,
                    tint = VideoLibrarySelectionPurple,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "视频均衡器",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "关闭",
                    tint = Color.White.copy(alpha = 0.76f)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 2.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "预设模式",
                    color = Color.White.copy(alpha = 0.48f),
                    style = MaterialTheme.typography.labelSmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    VideoEqualizerPreset.values().forEach { preset ->
                        VideoEqualizerPresetChip(
                            preset = preset,
                            selected = state.selectedPreset == preset,
                            enabled = state.isAvailable,
                            onClick = { onPresetSelected(preset) }
                        )
                    }
                }
            }

            if (state.isAvailable) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    state.bands.forEach { band ->
                        VideoEqualizerBandHorizontalSlider(
                            band = band,
                            minLevel = state.minBandLevel,
                            maxLevel = state.maxBandLevel,
                            onLevelChange = { nextLevel ->
                                onBandLevelChange(band.index, nextLevel)
                            }
                        )
                    }
                }
            } else {
                VideoEqualizerUnavailableCard(state.message)
            }

            VideoAudioEffectStrengthControl(
                title = "低音增强",
                state = bassBoostState,
                onPercentChange = onBassBoostChange
            )
            VideoAudioEffectStrengthControl(
                title = "环境声",
                state = virtualizerState,
                onPercentChange = onVirtualizerChange
            )
        }

        TextButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.textButtonColors(
                containerColor = VideoLibrarySelectionPurple.copy(alpha = VideoLibrarySelectionContainerAlpha),
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Text("完成", fontWeight = FontWeight.SemiBold)
        }
        TextButton(
            onClick = onReset,
            enabled = state.isAvailable || bassBoostState.supported || virtualizerState.supported,
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
        ) {
            Text("恢复默认设置", color = Color.White.copy(alpha = 0.58f))
        }
    }
}

@Composable
private fun VideoEqualizerPresetChip(
    preset: VideoEqualizerPreset,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(26.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(
                when {
                    !enabled -> Color.White.copy(alpha = 0.035f)
                    selected -> VideoLibrarySelectionPurple.copy(alpha = VideoLibrarySelectionContainerAlpha)
                    else -> Color.White.copy(alpha = 0.055f)
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = preset.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = when {
                !enabled -> Color.White.copy(alpha = 0.32f)
                selected -> Color.White
                else -> Color.White.copy(alpha = 0.76f)
            },
            maxLines = 1
        )
    }
}

@Composable
private fun VideoEqualizerBandHorizontalSlider(
    band: VideoEqualizerBandUi,
    minLevel: Short,
    maxLevel: Short,
    onLevelChange: (Short) -> Unit
) {
    val safeMin = minLevel.toFloat()
    val safeMax = if (maxLevel > minLevel) maxLevel.toFloat() else (minLevel + 1).toFloat()
    var sliderValue by remember(band.index) {
        mutableFloatStateOf(band.level.toFloat().coerceIn(safeMin, safeMax))
    }

    LaunchedEffect(band.level, minLevel, maxLevel) {
        sliderValue = band.level.toFloat().coerceIn(safeMin, safeMax)
    }

    fun updateBandLevel(nextValue: Float) {
        val clampedValue = nextValue.coerceIn(safeMin, safeMax)
        sliderValue = clampedValue
        onLevelChange(clampedValue.toEqualizerBandLevel(minLevel, maxLevel))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = band.frequencyText,
            color = Color.White.copy(alpha = 0.74f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.width(50.dp)
        )
        ThinEqualizerSlider(
            value = sliderValue,
            onValueChange = { nextValue ->
                updateBandLevel(nextValue)
            },
            valueRange = safeMin..safeMax,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatEqualizerLevel(sliderValue.toEqualizerBandLevel(minLevel, maxLevel)),
            color = Color.White.copy(alpha = 0.62f),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(54.dp)
        )
    }
}

@Composable
private fun VideoAudioEffectStrengthControl(
    title: String,
    state: VideoAudioEffectStrengthUiState,
    onPercentChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (title == "低音增强") Icons.Filled.VolumeUp else Icons.Filled.Tune,
                    contentDescription = null,
                    tint = if (state.supported) VideoLibrarySelectionPurple else Color.White.copy(alpha = 0.42f),
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = title,
                    color = Color.White.copy(alpha = if (state.supported) 0.76f else 0.42f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = if (state.supported) "${state.percent}%" else "不支持",
                color = if (state.supported) Color.White.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.36f),
                style = MaterialTheme.typography.labelSmall
            )
        }
        if (state.supported) {
            ThinEqualizerSlider(
                value = state.percent.toFloat(),
                onValueChange = { onPercentChange(round(it).toInt()) },
                valueRange = 0f..100f
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(999.dp))
            )
        }
    }
}

@Composable
private fun ThinEqualizerSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    sliderKey: Any = Unit
) {
    val safeStart = valueRange.start
    val safeEnd = if (valueRange.endInclusive > valueRange.start) {
        valueRange.endInclusive
    } else {
        valueRange.start + 1f
    }
    val clampedValue = value.coerceIn(safeStart, safeEnd)
    val fraction = ((clampedValue - safeStart) / (safeEnd - safeStart)).coerceIn(0f, 1f)
    val trackHeight = 4.dp
    val thumbSize = 16.dp
    val touchHeight = 32.dp
    val inactiveColor = Color.White.copy(alpha = 0.16f)
    val activeColor = if (enabled) VideoLibrarySelectionPurple else Color.White.copy(alpha = 0.28f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(touchHeight)
            .pointerInput(sliderKey, enabled, safeStart, safeEnd) {
                if (!enabled) return@pointerInput

                fun updateFromX(x: Float) {
                    val widthPx = size.width.toFloat().coerceAtLeast(1f)
                    val nextFraction = (x / widthPx).coerceIn(0f, 1f)
                    onValueChange(safeStart + (safeEnd - safeStart) * nextFraction)
                }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    updateFromX(down.position.x)
                    down.consume()
                    drag(down.id) { change ->
                        updateFromX(change.position.x)
                        change.consume()
                    }
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(RoundedCornerShape(999.dp))
                .background(inactiveColor)
        )
        Box(
            modifier = Modifier
                .width(maxWidth * fraction)
                .height(trackHeight)
                .clip(RoundedCornerShape(999.dp))
                .background(activeColor)
        )
        Box(
            modifier = Modifier
                .offset(x = (maxWidth - thumbSize) * fraction)
                .size(thumbSize)
                .clip(CircleShape)
                .background(activeColor)
        )
    }
}

@Composable
private fun VideoEqualizerUnavailableCard(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.045f))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = message,
            color = Color.White.copy(alpha = 0.78f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "开始播放后如果设备提供音频会话，会重新尝试绑定系统均衡器。",
            color = Color.White.copy(alpha = 0.48f),
            style = MaterialTheme.typography.labelSmall,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun VideoGestureSettingsPanel(
    settings: VideoGestureSettings,
    onSettingsChange: (VideoGestureSettings) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    SidePanelShell(title = "手势设置", onDismiss = onDismiss, modifier = modifier.width(320.dp)) {
        Text(
            text = "仅影响当前播放页，不写入持久设置。",
            color = Color.White.copy(alpha = 0.48f),
            style = MaterialTheme.typography.labelSmall
        )
        VideoControlSettingRow(
            title = "双击快退/快进",
            description = "左侧双击快退 10 秒，右侧双击快进 10 秒",
            checked = settings.doubleTapSeekEnabled,
            onCheckedChange = { onSettingsChange(settings.copy(doubleTapSeekEnabled = it)) }
        )
        VideoControlSettingRow(
            title = "左右滑动快退/快进",
            description = "在画面空白处左右滑动预览位置，松手后跳转",
            checked = settings.horizontalSeekEnabled,
            onCheckedChange = { onSettingsChange(settings.copy(horizontalSeekEnabled = it)) }
        )
        VideoControlSettingRow(
            title = "左侧上下滑动调亮度",
            description = "在画面左半屏上下滑动，只调整当前播放页窗口亮度",
            checked = settings.brightnessGestureEnabled,
            onCheckedChange = { onSettingsChange(settings.copy(brightnessGestureEnabled = it)) }
        )
        VideoControlSettingRow(
            title = "右侧上下滑动调音量",
            description = "在画面右半屏上下滑动，调整媒体音量",
            checked = settings.volumeGestureEnabled,
            onCheckedChange = { onSettingsChange(settings.copy(volumeGestureEnabled = it)) }
        )
        VideoControlSettingRow(
            title = "显示手势提示",
            description = "触发快退、快进、亮度或音量手势时显示中心提示",
            checked = settings.showHints,
            onCheckedChange = { onSettingsChange(settings.copy(showHints = it)) }
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.045f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "后续专项",
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "双指缩放和更复杂手势后续专项处理，不做假开关。",
                color = Color.White.copy(alpha = 0.48f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun VideoControlSettingsPanel(
    showTopToolbar: Boolean,
    onShowTopToolbarChange: (Boolean) -> Unit,
    showBottomControls: Boolean,
    onShowBottomControlsChange: (Boolean) -> Unit,
    showProgressTime: Boolean,
    onShowProgressTimeChange: (Boolean) -> Unit,
    keepControlsVisible: Boolean,
    onKeepControlsVisibleChange: (Boolean) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    SidePanelShell(title = "控制栏设置", onDismiss = onDismiss, modifier = modifier.width(300.dp)) {
        Text(
            text = "仅影响当前播放页显示，不写入持久设置。",
            color = Color.White.copy(alpha = 0.48f),
            style = MaterialTheme.typography.labelSmall
        )
        VideoControlSettingRow(
            title = "显示顶部工具栏",
            description = "返回、标题、字幕和音轨入口",
            checked = showTopToolbar,
            onCheckedChange = onShowTopToolbarChange
        )
        VideoControlSettingRow(
            title = "显示底部控制栏",
            description = "播放、上一集、下一集、锁屏和画面比例",
            checked = showBottomControls,
            onCheckedChange = onShowBottomControlsChange
        )
        VideoControlSettingRow(
            title = "显示进度时间",
            description = "进度条两侧当前时间和总时长",
            checked = showProgressTime,
            onCheckedChange = onShowProgressTimeChange
        )
        VideoControlSettingRow(
            title = "控制层常亮",
            description = "打开后不再 2 秒自动隐藏，点击空白仍可收起",
            checked = keepControlsVisible,
            onCheckedChange = onKeepControlsVisibleChange
        )
        TextButton(
            onClick = onReset,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("恢复默认", color = PlayerMoonPurpleSoft)
        }
    }
}

@Composable
private fun VideoControlSettingRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = if (checked) 0.09f else 0.045f))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.48f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .padding(start = 12.dp)
                .width(46.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (checked) PlayerMoonPurple.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.12f)),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (checked) 0.96f else 0.72f))
            )
        }
    }
}

@Composable
private fun VideoAdvancedFuturePanel(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    SidePanelShell(title = "高级", onDismiss = onDismiss, modifier = modifier.width(320.dp)) {
        Text(
            text = "这里显示仍在后续规划中的高级能力。已完成的功能会出现在对应入口中。",
            color = Color.White.copy(alpha = 0.62f),
            style = MaterialTheme.typography.labelSmall,
            lineHeight = 16.sp
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.045f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "已完成",
                color = PlayerMoonPurpleSoft,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "外挂 SRT 字幕时间同步、字幕样式、均衡器、画面调节、解码与格式信息已在对应入口提供。",
                color = Color.White.copy(alpha = 0.62f),
                style = MaterialTheme.typography.labelSmall,
                lineHeight = 16.sp
            )
        }
        VideoFutureItem("内嵌字幕时间偏移", "当前仅支持外挂 SRT 字幕时间同步；内嵌字幕时间轴偏移仍需后续专项。")
        VideoFutureItem("ASS / SSA 高级字幕同步", "复杂样式和特效字幕同步暂不做完整支持。")
        VideoFutureItem("解码增强", "FFmpeg / mpv / native decoder 属于高风险专项，需单独评估。")
        VideoFutureItem("高级画面滤镜", "当前已支持基础画面调节；更复杂滤镜后续再做。")
    }
}

@Composable
private fun VideoFutureItem(
    title: String,
    reason: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.045f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.86f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "后续",
                color = Color.White.copy(alpha = 0.46f),
                style = MaterialTheme.typography.labelSmall
            )
        }
        Text(
            text = reason,
            color = Color.White.copy(alpha = 0.48f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
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

private fun buildSyncedSrtSubtitleUriOrNull(
    context: Context,
    sourceUri: Uri?,
    subtitleName: String?,
    offsetMs: Long
): Uri? {
    if (sourceUri == null) return null
    if (offsetMs == 0L) return sourceUri
    if (!isSyncableExternalSrtSubtitle(sourceUri, subtitleName)) return null

    return runCatching {
        val sourceText = context.contentResolver.openInputStream(sourceUri)?.use { input ->
            input.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } ?: return null
        val shiftedText = shiftSrtSubtitleTimeline(sourceText, offsetMs) ?: return null
        val outputDir = File(context.cacheDir, "subtitle_sync").apply { mkdirs() }
        val sourceHash = sourceUri.toString().hashCode().absoluteValue
        val outputFile = File(outputDir, "subtitle_${sourceHash}_${offsetMs}.srt")
        outputFile.writeText(shiftedText, Charsets.UTF_8)
        Uri.fromFile(outputFile)
    }.getOrNull()
}

private fun isSyncableExternalSrtSubtitle(uri: Uri?, subtitleName: String?): Boolean {
    if (uri == null) return false
    val candidates = listOfNotNull(
        subtitleName,
        uri.lastPathSegment,
        uri.toString()
    )
    return candidates.any { candidate ->
        candidate.substringBefore('?').substringBefore('#').lowercase(Locale.US).endsWith(".srt")
    }
}

private fun shiftSrtSubtitleTimeline(sourceText: String, offsetMs: Long): String? {
    var changed = false
    val shifted = SRT_TIME_RANGE_REGEX.replace(sourceText) { match ->
        val startMs = parseSrtTimestampMs(match.groupValues[1]) ?: return@replace match.value
        val endMs = parseSrtTimestampMs(match.groupValues[2]) ?: return@replace match.value
        changed = true
        val shiftedStart = (startMs + offsetMs).coerceAtLeast(0L)
        val shiftedEnd = (endMs + offsetMs).coerceAtLeast(shiftedStart + 1L)
        "${formatSrtTimestamp(shiftedStart)} --> ${formatSrtTimestamp(shiftedEnd)}"
    }
    return shifted.takeIf { changed }
}

private fun parseSrtTimestampMs(value: String): Long? {
    val match = SRT_TIMESTAMP_REGEX.matchEntire(value.trim()) ?: return null
    val hours = match.groupValues[1].toLongOrNull() ?: return null
    val minutes = match.groupValues[2].toLongOrNull() ?: return null
    val seconds = match.groupValues[3].toLongOrNull() ?: return null
    val millis = match.groupValues[4].toLongOrNull() ?: return null
    return (((hours * 60L) + minutes) * 60L + seconds) * 1000L + millis
}

private fun formatSrtTimestamp(valueMs: Long): String {
    val millis = valueMs % 1000L
    val totalSeconds = valueMs / 1000L
    val seconds = totalSeconds % 60L
    val totalMinutes = totalSeconds / 60L
    val minutes = totalMinutes % 60L
    val hours = totalMinutes / 60L
    return String.format(Locale.US, "%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
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

private fun parseAbLoopTimeInput(input: String, durationMs: Long): Long? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return null
    val parts = trimmed.split(":").map { it.trim() }
    if (parts.any { it.isEmpty() || it.any { char -> !char.isDigit() } }) return null
    val totalSeconds = when (parts.size) {
        1 -> parts[0].toLongOrNull()
        2 -> {
            val minutes = parts[0].toLongOrNull() ?: return null
            val seconds = parts[1].toLongOrNull() ?: return null
            if (seconds > 59L) return null
            minutes * 60L + seconds
        }
        3 -> {
            val hours = parts[0].toLongOrNull() ?: return null
            val minutes = parts[1].toLongOrNull() ?: return null
            val seconds = parts[2].toLongOrNull() ?: return null
            if (minutes > 59L || seconds > 59L) return null
            hours * 3600L + minutes * 60L + seconds
        }
        else -> return null
    } ?: return null
    return safeVideoMarkerPosition(totalSeconds * 1000L, durationMs)
}

private enum class VideoDragMode {
    UNKNOWN,
    BACK,
    BRIGHTNESS,
    VOLUME,
    SEEK,
    SEEK_UNAVAILABLE
}

private enum class VideoAbLoopPoint(val label: String) {
    A("A"),
    B("B")
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

private enum class VideoSubtitleSize(val label: String, val textSizeSp: Float) {
    SMALL("小", 18f),
    MEDIUM("中", 22f),
    LARGE("大", 26f)
}

private enum class VideoSubtitlePosition(val label: String, val bottomPaddingFraction: Float) {
    BOTTOM("底部", 0.14f),
    MIDDLE("中部", 0.46f),
    TOP("顶部", 0.74f)
}

private enum class VideoSubtitleBackground(
    val label: String,
    val styleColor: Int,
    val previewColor: Color
) {
    OFF("关闭", android.graphics.Color.TRANSPARENT, Color.Transparent),
    BLACK("半透明黑底", android.graphics.Color.argb(176, 0, 0, 0), Color.Black.copy(alpha = 0.62f))
}

private data class VideoSubtitleColor(
    val label: String,
    val styleColor: Int,
    val previewColor: Color,
    val isCustom: Boolean = false
)

private val VideoSubtitlePresetColors = listOf(
    VideoSubtitleColor("白色", android.graphics.Color.WHITE, Color.White),
    VideoSubtitleColor("黄色", android.graphics.Color.rgb(255, 224, 107), Color(0xFFFFE06B)),
    VideoSubtitleColor("绿色", android.graphics.Color.rgb(112, 224, 128), Color(0xFF70E080)),
    VideoSubtitleColor("青色", android.graphics.Color.rgb(92, 224, 214), Color(0xFF5CE0D6)),
    VideoSubtitleColor("蓝色", android.graphics.Color.rgb(101, 183, 255), Color(0xFF65B7FF)),
    VideoSubtitleColor("红色", android.graphics.Color.rgb(255, 112, 106), Color(0xFFFF706A)),
    VideoSubtitleColor("粉色", android.graphics.Color.rgb(255, 142, 203), Color(0xFFFF8ECB)),
    VideoSubtitleColor("紫色", android.graphics.Color.rgb(183, 167, 255), PlayerMoonPurpleSoft),
    VideoSubtitleColor("橙色", android.graphics.Color.rgb(255, 172, 92), Color(0xFFFFAC5C)),
    VideoSubtitleColor("蓝绿", android.graphics.Color.rgb(24, 190, 204), Color(0xFF18BECC))
)

private data class VideoSubtitleStyleSettings(
    val size: VideoSubtitleSize = VideoSubtitleSize.MEDIUM,
    val position: VideoSubtitlePosition = VideoSubtitlePosition.BOTTOM,
    val background: VideoSubtitleBackground = VideoSubtitleBackground.OFF,
    val color: VideoSubtitleColor = VideoSubtitlePresetColors.first()
)

private fun subtitleColorFromHueSaturation(hue: Float, saturation: Float): VideoSubtitleColor {
    val color = android.graphics.Color.HSVToColor(
        floatArrayOf(
            hue.coerceIn(0f, 360f),
            (saturation / 100f).coerceIn(0f, 1f),
            1f
        )
    )
    return VideoSubtitleColor(
        label = "自定义",
        styleColor = color,
        previewColor = Color(color),
        isCustom = true
    )
}

private fun subtitleColorHsv(color: Int): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color, hsv)
    return hsv
}

private fun subtitleColorHex(color: Int): String {
    return String.format(Locale.US, "#%06X", 0xFFFFFF and color)
}

private fun PlayerView.applyVideoSubtitleStyle(settings: VideoSubtitleStyleSettings) {
    val subtitleView = findViewById<androidx.media3.ui.SubtitleView>(
        androidx.media3.ui.R.id.exo_subtitles
    ) ?: return
    subtitleView.setApplyEmbeddedStyles(false)
    subtitleView.setApplyEmbeddedFontSizes(false)
    subtitleView.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, settings.size.textSizeSp)
    subtitleView.setBottomPaddingFraction(settings.position.bottomPaddingFraction)
    subtitleView.setStyle(
        CaptionStyleCompat(
            settings.color.styleColor,
            settings.background.styleColor,
            android.graphics.Color.TRANSPARENT,
            CaptionStyleCompat.EDGE_TYPE_OUTLINE,
            android.graphics.Color.BLACK,
            null
        )
    )
}

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
            displayLabel = "30min",
            mode = VideoSleepTimerMode.TIMED,
            durationMs = 30 * 60 * 1000L
        )
        val FortyFiveMinutes = VideoSleepTimerOption(
            label = "45 分钟",
            displayLabel = "45min",
            mode = VideoSleepTimerMode.TIMED,
            durationMs = 45 * 60 * 1000L
        )
        val SixtyMinutes = VideoSleepTimerOption(
            label = "60 分钟",
            displayLabel = "60min",
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
            displayLabel = "1h30min",
            mode = VideoSleepTimerMode.TIMED,
            durationMs = 90 * 60 * 1000L
        )
        val OneHundredTwentyMinutes = VideoSleepTimerOption(
            label = "120 分钟",
            displayLabel = "2h",
            mode = VideoSleepTimerMode.TIMED,
            durationMs = 120 * 60 * 1000L
        )
        val OneHundredEightyMinutes = VideoSleepTimerOption(
            label = "180 分钟",
            displayLabel = "180 min",
            mode = VideoSleepTimerMode.TIMED,
            durationMs = 180 * 60 * 1000L
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

private const val VIDEO_SLEEP_TIMER_MIN_MINUTES = 1
private const val VIDEO_SLEEP_TIMER_MAX_MINUTES = 24 * 60
private const val VIDEO_SLEEP_TIMER_DEFAULT_MINUTES = 30
private const val SLEEP_TIMER_HOUR_DRAG_STEP_PX = 88f
private const val SLEEP_TIMER_MINUTE_DRAG_STEP_PX = 24f

private val VideoSleepTimerFixedOptions = listOf(
    VideoSleepTimerOption.ThirtyMinutes,
    VideoSleepTimerOption.FortyFiveMinutes,
    VideoSleepTimerOption.SixtyMinutes,
    VideoSleepTimerOption.NinetyMinutes,
    VideoSleepTimerOption.OneHundredTwentyMinutes
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

private data class VideoDecodeTrackInfo(
    val mimeType: String,
    val codec: String,
    val resolution: String?,
    val frameRate: String?,
    val channelCount: String?,
    val sampleRate: String?,
    val supportText: String
)

private data class VideoSubtitleTrackInfo(
    val mimeType: String,
    val language: String,
    val label: String
) {
    val displayText: String
        get() = listOf(
            mimeType.takeIf { it.isNotBlank() },
            language.takeIf { it.isNotBlank() },
            label.takeIf { it.isNotBlank() }
        ).filterNotNull().joinToString(" · ").ifBlank { "未知" }
}

private data class VideoPlaybackErrorInfo(
    val title: String,
    val detail: String,
    val code: String,
    val rawSummary: String,
    val happenedAtMs: Long
) {
    val timeText: String
        get() = formatVideoInfoDate(happenedAtMs)
}

private fun ExoPlayer.currentDecodeTrackInfo(trackType: Int): VideoDecodeTrackInfo? {
    val selected = currentTracks.groups
        .filter { it.type == trackType }
        .firstNotNullOfOrNull { group ->
            (0 until group.length)
                .firstOrNull { group.isTrackSelected(it) }
                ?.let { trackIndex ->
                    group.getTrackFormat(trackIndex) to group.isTrackSupported(trackIndex)
                }
        }
    val fallback = selected ?: currentTracks.groups
        .filter { it.type == trackType }
        .firstNotNullOfOrNull { group ->
            (0 until group.length)
                .firstOrNull { group.isTrackSupported(it) }
                ?.let { trackIndex ->
                    group.getTrackFormat(trackIndex) to group.isTrackSupported(trackIndex)
                }
        }
    val (format, isSupported) = fallback ?: return null
    return format.toDecodeTrackInfo(isSupported, trackType)
}

private fun ExoPlayer.currentSubtitleTrackInfos(): List<VideoSubtitleTrackInfo> {
    return currentTracks.groups
        .filter { it.type == C.TRACK_TYPE_TEXT }
        .flatMap { group ->
            (0 until group.length).map { trackIndex ->
                val format = group.getTrackFormat(trackIndex)
                VideoSubtitleTrackInfo(
                    mimeType = format.sampleMimeType?.trim()?.takeIf { it.isNotBlank() } ?: "未知",
                    language = format.language?.trim()?.takeIf { it.isNotBlank() } ?: "",
                    label = format.label?.trim()?.takeIf { it.isNotBlank() } ?: ""
                )
            }
        }
}

private fun Format.toDecodeTrackInfo(isSupported: Boolean, trackType: Int): VideoDecodeTrackInfo {
    val mimeText = sampleMimeType?.trim()?.takeIf { it.isNotBlank() } ?: "未知"
    val codecText = codecs?.trim()?.takeIf { it.isNotBlank() } ?: "未知"
    val resolutionText = if (width > 0 && height > 0) {
        "$width × $height"
    } else {
        null
    }
    val frameRateText = frameRate.takeIf { it > 0f }?.let {
        String.format(Locale.US, "%.2f", it).trimEnd('0').trimEnd('.') + " fps"
    }
    val channelCountText = channelCount.takeIf { it > 0 }?.let { "$it 声道" }
    val sampleRateText = sampleRate.takeIf { it > 0 }?.let { "${it / 1000.0}".trimEnd('0').trimEnd('.') + " kHz" }
    val trackLabel = when (trackType) {
        C.TRACK_TYPE_VIDEO -> "视频"
        C.TRACK_TYPE_AUDIO -> "音频"
        else -> ""
    }
    return VideoDecodeTrackInfo(
        mimeType = mimeText,
        codec = codecText,
        resolution = resolutionText,
        frameRate = frameRateText,
        channelCount = channelCountText,
        sampleRate = sampleRateText,
        supportText = if (isSupported) {
            "支持当前${trackLabel}轨道"
        } else {
            "可能不支持当前${trackLabel}轨道"
        }
    )
}

private fun PlaybackException.toVideoPlaybackErrorInfo(): VideoPlaybackErrorInfo {
    val codeText = errorCodeName.takeIf { it.isNotBlank() } ?: "ERROR_CODE_$errorCode"
    val causeText = cause.chainSummary()
    val joinedText = listOfNotNull(codeText, message, causeText)
        .joinToString(" ")
        .lowercase(Locale.US)
    val category = when {
        joinedText.containsAny(
            "source",
            "io",
            "file",
            "permission",
            "securityexception",
            "filenotfoundexception",
            "cleartext",
            "network"
        ) -> VideoPlaybackErrorCategory(
            title = "文件读取失败",
            detail = "视频文件无法读取，可能已移动、损坏或无访问权限"
        )

        joinedText.containsAny("parser", "parsing", "extractor", "container", "malformed", "unrecognized") -> {
            VideoPlaybackErrorCategory(
                title = "容器解析失败",
                detail = "当前视频容器或封装格式可能不受系统播放器支持"
            )
        }

        joinedText.contains("audio") && joinedText.containsAny("decoder", "codec", "format", "mime", "unsupported", "init") -> {
            VideoPlaybackErrorCategory(
                title = "音频解码可能不支持",
                detail = "当前设备可能不支持此视频的音频编码，画面或声音可能无法播放"
            )
        }

        joinedText.containsAny("decoder", "codec", "mediacodec", "decode", "decoding", "init failed", "unsupported") -> {
            VideoPlaybackErrorCategory(
                title = "视频解码可能不支持",
                detail = "当前设备或系统解码器可能不支持此视频编码"
            )
        }

        else -> VideoPlaybackErrorCategory(
            title = "播放失败",
            detail = "播放失败，当前设备或系统解码器可能不支持此文件"
        )
    }
    return VideoPlaybackErrorInfo(
        title = category.title,
        detail = category.detail,
        code = codeText,
        rawSummary = listOfNotNull(message, causeText)
            .joinToString(" · ")
            .ifBlank { "无原始错误摘要" }
            .compactPanelText(),
        happenedAtMs = System.currentTimeMillis()
    )
}

private data class VideoPlaybackErrorCategory(
    val title: String,
    val detail: String
)

private fun Throwable?.chainSummary(maxDepth: Int = 4): String {
    var current = this
    val parts = mutableListOf<String>()
    var depth = 0
    while (current != null && depth < maxDepth) {
        val name = current::class.java.simpleName
        val msg = current.message?.takeIf { it.isNotBlank() }
        parts += listOfNotNull(name, msg).joinToString(": ")
        current = current.cause
        depth += 1
    }
    return parts.joinToString(" <- ")
}

private fun String.containsAny(vararg needles: String): Boolean {
    return needles.any { contains(it) }
}

private fun String.compactPanelText(maxLength: Int = 180): String {
    val compact = replace(Regex("\\s+"), " ").trim()
    return if (compact.length <= maxLength) compact else compact.take(maxLength - 1) + "…"
}

private fun Int.isValidVideoAudioSessionId(): Boolean {
    return this != C.AUDIO_SESSION_ID_UNSET && this > 0
}

private fun Equalizer.toVideoEqualizerUiState(
    wantedEnabled: Boolean,
    selectedPreset: VideoEqualizerPreset?
): VideoEqualizerUiState {
    val levelRange = bandLevelRange
    val minLevel = levelRange.getOrNull(0) ?: 0
    val maxLevel = levelRange.getOrNull(1) ?: 0
    val allBands = (0 until numberOfBands.toInt()).map { index ->
        val band = index.toShort()
        val centerFrequencyHz = getCenterFreq(band) / 1000f
        VideoEqualizerBandUi(
            index = band,
            centerFrequencyHz = centerFrequencyHz,
            frequencyText = formatEqualizerFrequencyHz(centerFrequencyHz),
            level = getBandLevel(band)
        )
    }
    return VideoEqualizerUiState(
        status = VideoEqualizerStatus.AVAILABLE,
        message = "系统均衡器可用",
        enabled = enabled && wantedEnabled,
        minBandLevel = minLevel,
        maxBandLevel = maxLevel,
        bands = allBands.pickDisplayEqualizerBands(),
        selectedPreset = selectedPreset
    )
}

private fun Equalizer.applyVideoEqualizerPreset(preset: VideoEqualizerPreset) {
    val levelRange = bandLevelRange
    val minLevel = levelRange.getOrNull(0) ?: 0
    val maxLevel = levelRange.getOrNull(1) ?: 0
    repeat(numberOfBands.toInt()) { index ->
        val band = index.toShort()
        val frequencyHz = getCenterFreq(band) / 1000f
        val ratio = preset.equalizerGainRatio(frequencyHz)
        setBandLevel(band, ratio.toBandLevel(minLevel, maxLevel))
    }
}

private fun VideoEqualizerPreset.equalizerGainRatio(frequencyHz: Float): Float {
    return when (this) {
        VideoEqualizerPreset.Normal -> 0f
        VideoEqualizerPreset.Bass -> when {
            frequencyHz < 180f -> 0.72f
            frequencyHz < 500f -> 0.42f
            frequencyHz < 2_000f -> -0.10f
            else -> -0.22f
        }
        VideoEqualizerPreset.Classical -> when {
            frequencyHz < 180f -> 0.28f
            frequencyHz < 1_500f -> 0.04f
            frequencyHz < 6_000f -> 0.18f
            else -> 0.36f
        }
        VideoEqualizerPreset.Pop -> when {
            frequencyHz < 180f -> 0.18f
            frequencyHz < 700f -> -0.05f
            frequencyHz < 4_500f -> 0.36f
            else -> 0.24f
        }
        VideoEqualizerPreset.Rock -> when {
            frequencyHz < 200f -> 0.55f
            frequencyHz < 700f -> 0.18f
            frequencyHz < 3_000f -> -0.10f
            else -> 0.52f
        }
    }
}

private fun Float.toBandLevel(minLevel: Short, maxLevel: Short): Short {
    val clampedRatio = coerceIn(-1f, 1f)
    val target = if (clampedRatio >= 0f) {
        clampedRatio * maxLevel
    } else {
        -clampedRatio.absoluteValue * minLevel.toInt().absoluteValue
    }
    return round(target)
        .toInt()
        .coerceIn(minLevel.toInt(), maxLevel.toInt())
        .toShort()
}

private fun List<VideoEqualizerBandUi>.pickDisplayEqualizerBands(): List<VideoEqualizerBandUi> {
    if (size <= VIDEO_EQUALIZER_TARGET_FREQ_HZ.size) return sortedBy { it.centerFrequencyHz }
    val picked = mutableListOf<VideoEqualizerBandUi>()
    VIDEO_EQUALIZER_TARGET_FREQ_HZ.forEach { target ->
        val nearest = filterNot { candidate -> picked.any { it.index == candidate.index } }
            .minByOrNull { candidate -> (candidate.centerFrequencyHz - target).absoluteValue }
        if (nearest != null) picked += nearest
    }
    return picked.sortedBy { it.centerFrequencyHz }
}

private fun BassBoost.toVideoAudioEffectStrengthUiState(
    percent: Int,
    unsupportedMessage: String
): VideoAudioEffectStrengthUiState {
    return if (strengthSupported) {
        VideoAudioEffectStrengthUiState.supported(percent)
    } else {
        VideoAudioEffectStrengthUiState.unsupported(unsupportedMessage)
    }
}

private fun Virtualizer.toVideoAudioEffectStrengthUiState(
    percent: Int,
    unsupportedMessage: String
): VideoAudioEffectStrengthUiState {
    return if (strengthSupported) {
        VideoAudioEffectStrengthUiState.supported(percent)
    } else {
        VideoAudioEffectStrengthUiState.unsupported(unsupportedMessage)
    }
}

private fun Int.toAudioEffectStrength(): Short {
    return (coerceIn(0, 100) * 10)
        .coerceIn(0, VIDEO_AUDIO_EFFECT_MAX_STRENGTH)
        .toShort()
}

private fun Float.toEqualizerBandLevel(minLevel: Short, maxLevel: Short): Short {
    return round(this)
        .toInt()
        .coerceIn(minLevel.toInt(), maxLevel.toInt())
        .toShort()
}

private fun VideoPictureAdjustmentSettings.toAndroidRenderEffectOrNull(): RenderEffect? {
    if (!enabled || isNeutral || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val saturation = saturationPercent / 100f
    val contrast = contrastPercent / 100f
    val brightnessOffset = (brightnessPercent - 100) * 1.6f
    val contrastOffset = (1f - contrast) * 127.5f + brightnessOffset
    val temperature = temperatureStep.coerceIn(-2, 2) / 2f

    val matrix = ColorMatrix().apply {
        setSaturation(saturation)
    }
    val contrastMatrix = ColorMatrix(
        floatArrayOf(
            contrast, 0f, 0f, 0f, contrastOffset,
            0f, contrast, 0f, 0f, contrastOffset,
            0f, 0f, contrast, 0f, contrastOffset,
            0f, 0f, 0f, 1f, 0f
        )
    )
    matrix.postConcat(contrastMatrix)

    if (temperature != 0f) {
        val redScale = (1f + temperature * 0.10f).coerceIn(0.82f, 1.18f)
        val greenScale = (1f + kotlin.math.abs(temperature) * 0.02f).coerceIn(0.90f, 1.08f)
        val blueScale = (1f - temperature * 0.12f).coerceIn(0.78f, 1.22f)
        val temperatureMatrix = ColorMatrix().apply {
            setScale(redScale, greenScale, blueScale, 1f)
        }
        matrix.postConcat(temperatureMatrix)
    }

    return RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(matrix))
}

private fun Int.temperatureLabel(): String = when (coerceIn(-2, 2)) {
    -2 -> "偏冷"
    -1 -> "微冷"
    0 -> "标准"
    1 -> "微暖"
    else -> "偏暖"
}

private fun formatEqualizerFrequencyHz(hz: Float): String {
    return if (hz >= 1000f) {
        val khz = hz / 1000f
        val text = if (khz >= 10f) {
            round(khz).toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", khz).trimEnd('0').trimEnd('.')
        }
        "$text kHz"
    } else {
        "${round(hz).toInt()} Hz"
    }
}

private fun formatEqualizerLevel(level: Short): String {
    return String.format(Locale.US, "%+.1f dB", level / 100f)
        .replace(".0 dB", " dB")
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
    val direction = if (deltaMs >= 0L) "快进" else "快退"
    val totalSeconds = (deltaMs.absoluteValue / 1000L).coerceAtLeast(1L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    val durationText = when {
        minutes > 0L && seconds > 0L -> "${minutes}分${seconds}秒"
        minutes > 0L -> "${minutes}分钟"
        else -> "${seconds}秒"
    }
    return "$direction $durationText"
}

private fun formatSignedDelay(delayMs: Long): String {
    return when {
        delayMs > 0L -> "+${delayMs}ms"
        delayMs < 0L -> "${delayMs}ms"
        else -> "0ms"
    }
}

private fun formatSubtitleOffsetSeconds(offsetMs: Long): String {
    val seconds = offsetMs / 1000f
    return String.format(Locale.US, "%.1fs", seconds)
}

private fun formatSubtitleOffsetSecondsForPanel(offsetMs: Long): String {
    val seconds = offsetMs / 1000f
    val sign = if (seconds >= 0f) "+" else ""
    return String.format(Locale.US, "%s%.1f 秒", sign, seconds)
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
private const val VIDEO_DOUBLE_TAP_SEEK_MS = 10_000L
private const val VIDEO_GESTURE_DRAG_THRESHOLD_DP = 22f
private const val VIDEO_GESTURE_HORIZONTAL_RATIO = 1.2f
private const val VIDEO_SYSTEM_GESTURE_BOTTOM_SAFE_DP = 96

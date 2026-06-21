package com.shenghui.localvibe

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.documentfile.provider.DocumentFile
import com.shenghui.localvibe.core.datastore.AppStateStore
import com.shenghui.localvibe.core.datastore.PersistedAudioRecentRecord
import com.shenghui.localvibe.core.datastore.PersistedBookFile
import com.shenghui.localvibe.core.datastore.PersistedBookProgress
import com.shenghui.localvibe.core.datastore.PersistedFolder
import com.shenghui.localvibe.core.datastore.PersistedPlaybackProgress
import com.shenghui.localvibe.core.datastore.PersistedVideoVisibilityRecord
import com.shenghui.localvibe.core.media.deleteUri
import com.shenghui.localvibe.core.media.resolveDocumentTreeName
import com.shenghui.localvibe.core.media.VideoMetadata
import com.shenghui.localvibe.core.media.VideoThumbnailPrewarmer
import com.shenghui.localvibe.core.media.VideoThumbnailStore
import com.shenghui.localvibe.core.media.videoMetadataCacheKey
import com.shenghui.localvibe.core.player.AudioPlayMode
import com.shenghui.localvibe.core.player.MusicPlaybackService
import com.shenghui.localvibe.core.scanner.FolderScanner
import com.shenghui.localvibe.core.scanner.LocalMediaFile
import com.shenghui.localvibe.core.scanner.LocalMediaType
import com.shenghui.localvibe.core.scanner.MediaStoreScanner
import com.shenghui.localvibe.core.ui.MoonBottomNavigationBar
import com.shenghui.localvibe.core.ui.RotatingMusicThumb
import com.shenghui.localvibe.core.ui.theme.LocalVibeTheme
import com.shenghui.localvibe.feature.audio.AudioPlayerScreen
import com.shenghui.localvibe.feature.audio.AudioLibraryScreen
import com.shenghui.localvibe.feature.audio.AudioLibrarySection
import com.shenghui.localvibe.feature.audio.AudioSortMode
import com.shenghui.localvibe.feature.book.BookListenScreen
import com.shenghui.localvibe.feature.book.BookLibraryScreen
import com.shenghui.localvibe.feature.folder.FolderScreen
import com.shenghui.localvibe.feature.home.model.MediaFolderUiModel
import com.shenghui.localvibe.feature.profile.ProfileScreen
import com.shenghui.localvibe.feature.settings.SettingsScreen
import com.shenghui.localvibe.feature.video.VideoLibraryScreen
import com.shenghui.localvibe.feature.video.VideoPlayerScreen
import com.shenghui.localvibe.feature.video.model.VideoFolderUiModel
import com.shenghui.localvibe.feature.video.model.VideoVisibilityRecordType
import com.shenghui.localvibe.feature.video.model.VideoVisibilityRecordUiModel
import com.shenghui.localvibe.feature.library.MediaFolderGroupUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocalVibeTheme {
                LocalVibeApp()
            }
        }
    }
}

@Composable
private fun LocalVibeApp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val appStateStore = remember { AppStateStore(context.applicationContext) }
    val videoFolders = remember { mutableStateListOf<MediaFolderUiModel>() }
    val audioFolders = remember { mutableStateListOf<MediaFolderUiModel>() }
    val bookFolders = remember { mutableStateListOf<MediaFolderUiModel>() }
    val importedBookFiles = remember { mutableStateListOf<LocalMediaFile>() }
    val folderBookFiles = remember { mutableStateListOf<LocalMediaFile>() }
    val folderBookFilesByFolder = remember { mutableStateMapOf<String, List<LocalMediaFile>>() }
    val scannedFilesByFolder = remember { mutableStateMapOf<String, List<LocalMediaFile>>() }
    val videoProgressMap = remember { mutableStateMapOf<String, Long>() }
    val videoMetadataCache = remember { mutableStateMapOf<String, VideoMetadata>() }
    val videoThumbnailPrewarmer = remember(context, coroutineScope) {
        VideoThumbnailPrewarmer(context.applicationContext, coroutineScope)
    }
    val audioProgressMap = remember { mutableStateMapOf<String, Long>() }
    val bookProgressMap = remember { mutableStateMapOf<String, PersistedBookProgress>() }
    var hiddenAudioUris by remember { mutableStateOf(emptySet<String>()) }
    var hiddenAudioFolderKeys by remember { mutableStateOf(emptySet<String>()) }
    var favoriteAudioUris by remember { mutableStateOf(emptySet<String>()) }
    var audioRecentRecords by remember { mutableStateOf<List<PersistedAudioRecentRecord>>(emptyList()) }
    var hiddenVideoUris by remember { mutableStateOf(emptySet<String>()) }
    var hiddenVideoFolderIds by remember { mutableStateOf(emptySet<String>()) }
    val hiddenVideoRecords = remember { mutableStateListOf<PersistedVideoVisibilityRecord>() }
    val hiddenVideoFolderRecords = remember { mutableStateListOf<PersistedVideoVisibilityRecord>() }
    val unavailableVideoRecords = remember { mutableStateListOf<PersistedVideoVisibilityRecord>() }
    val videoFolderPlaybackSpeeds = remember { mutableStateMapOf<String, Float>() }
    var isVideoVisibilityReady by remember { mutableStateOf(false) }
    var isVideoInitialScanComplete by remember { mutableStateOf(false) }
    var isVideoAutoScanning by remember { mutableStateOf(false) }
    var currentFolder by remember { mutableStateOf<MediaFolderUiModel?>(null) }
    var currentFolderTargetType by remember { mutableStateOf<LocalMediaType?>(null) }
    var currentAddTargetType by remember { mutableStateOf<LocalMediaType?>(null) }
    var lastAutoScanAt by remember { mutableStateOf(0L) }
    var videoPermissionDenied by remember { mutableStateOf(false) }
    var audioPermissionDenied by remember { mutableStateOf(false) }
    var selectedMediaFile by remember { mutableStateOf<LocalMediaFile?>(null) }
    var audioQueue by remember { mutableStateOf<List<LocalMediaFile>>(emptyList()) }
    var currentAudioIndex by remember { mutableStateOf(0) }
    var audioPlayMode by remember { mutableStateOf(AudioPlayMode.NORMAL) }
    var audioSortMode by remember { mutableStateOf(AudioSortMode.Default) }
    var audioSortAscending by remember { mutableStateOf(false) }
    var audioShuffleHistory by remember { mutableStateOf<List<String>>(emptyList()) }
    var audioLibrarySection by rememberSaveable { mutableStateOf(AudioLibrarySection.AllSongs) }
    var selectedAudioFolderKey by rememberSaveable { mutableStateOf<String?>(null) }
    var videoQueue by remember { mutableStateOf<List<LocalMediaFile>>(emptyList()) }
    var currentVideoIndex by remember { mutableStateOf(0) }
    var selectedVideoUri by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAudioUri by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedBookUri by rememberSaveable { mutableStateOf<String?>(null) }
    var recentVideoFile by remember { mutableStateOf<LocalMediaFile?>(null) }
    var recentVideoUri by remember { mutableStateOf<String?>(null) }
    var recentAudioUri by remember { mutableStateOf<String?>(null) }
    var lastAudioUri by remember { mutableStateOf<String?>(null) }
    var musicController by remember { mutableStateOf<MediaController?>(null) }
    var musicCurrentUri by remember { mutableStateOf<String?>(null) }
    var musicCurrentPositionMs by remember { mutableStateOf(0L) }
    var musicDurationMs by remember { mutableStateOf(0L) }
    var musicIsPlaying by remember { mutableStateOf(false) }
    var initialMainRoute by remember { mutableStateOf<String?>(null) }
    var lastRootBackToastAt by remember { mutableLongStateOf(0L) }
    var rootBackToast by remember { mutableStateOf<Toast?>(null) }
    var pendingFolderVideoDeleteFiles by remember { mutableStateOf<List<LocalMediaFile>>(emptyList()) }
    var pendingFolderVideoDeleteMode by remember { mutableStateOf<FolderVideoDeleteMode?>(null) }
    var folderVideoDeleteSuccessSignal by remember { mutableStateOf(0L) }
    lateinit var folderVideoDeleteLauncher:
        ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>
    DisposableEffect(videoThumbnailPrewarmer) {
        onDispose {
            videoThumbnailPrewarmer.cancel()
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "未开启通知权限，后台播放通知可能不会显示", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun returnToDesktopFromRoot() {
        val now = System.currentTimeMillis()
        if (now - lastRootBackToastAt <= 1600L) {
            rootBackToast?.cancel()
            rootBackToast = null
            (context as? Activity)?.moveTaskToBack(true)
            return
        }
        rootBackToast?.cancel()
        val toast = Toast.makeText(context, "再次滑动返回桌面", Toast.LENGTH_SHORT)
        rootBackToast = toast
        toast.show()
        coroutineScope.launch {
            delay(1000L)
            if (rootBackToast === toast) {
                toast.cancel()
                rootBackToast = null
            }
        }
        lastRootBackToastAt = now
    }

    fun applyAudioPlayMode(controller: Player?, mode: AudioPlayMode) {
        controller ?: return
        controller.shuffleModeEnabled = false
        controller.repeatMode = when (mode) {
            AudioPlayMode.REPEAT_ONE -> Player.REPEAT_MODE_ONE
            AudioPlayMode.REPEAT_ALL -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun List<LocalMediaFile>.filterVisibleAudios(): List<LocalMediaFile> {
        return filterNot {
            it.type == LocalMediaType.AUDIO && it.normalizedUri() in hiddenAudioUris
        }.distinctBy { it.normalizedUri() }
    }

    fun List<LocalMediaFile>.filterVisibleVideos(): List<LocalMediaFile> {
        return filterNot {
            it.type == LocalMediaType.VIDEO && it.normalizedUri() in hiddenVideoUris
        }.distinctBy { it.normalizedUri() }
    }

    fun List<com.shenghui.localvibe.core.scanner.ScannedMediaFolder>.filterHiddenAudioFolders(): List<com.shenghui.localvibe.core.scanner.ScannedMediaFolder> {
        return filterNot { it.folder.id.trim() in hiddenAudioFolderKeys }
            .mapNotNull { result ->
                val visibleFiles = result.files.filterVisibleAudios()
                if (visibleFiles.isEmpty()) {
                    null
                } else {
                    result.copy(
                        folder = result.folder.copy(
                            audioCount = visibleFiles.count { it.type == LocalMediaType.AUDIO }
                        ),
                        files = visibleFiles
                    )
                }
            }
    }

    suspend fun hideAudioFiles(files: List<LocalMediaFile>) {
        val uris = files
            .filter { it.type == LocalMediaType.AUDIO }
            .map { it.normalizedUri() }
            .filter { it.isNotBlank() }
            .toSet()
        if (uris.isEmpty()) return
        hiddenAudioUris = hiddenAudioUris + uris
        appStateStore.hideAudioUris(uris)
    }

    suspend fun hideVideoFiles(files: List<LocalMediaFile>) {
        val uris = files
            .filter { it.type == LocalMediaType.VIDEO }
            .map { it.normalizedUri() }
            .filter { it.isNotBlank() }
            .toSet()
        if (uris.isEmpty()) return
        hiddenVideoUris = hiddenVideoUris + uris
        appStateStore.hideVideoFiles(files)
        hiddenVideoRecords.clear()
        hiddenVideoRecords.addAll(appStateStore.loadHiddenVideoRecords())
    }

    fun allVisibleAudioFiles(): List<LocalMediaFile> {
        return scannedFilesByFolder.values
            .flatten()
            .filter { it.type == LocalMediaType.AUDIO }
            .filterVisibleAudios()
            .sortedBy { it.name.lowercase() }
    }

    fun audioPlaybackQueueFor(file: LocalMediaFile?): List<LocalMediaFile> {
        val visibleCurrentQueue = audioQueue.filterVisibleAudios()
        if (visibleCurrentQueue.size > 1) return visibleCurrentQueue
        val scannedQueue = allVisibleAudioFiles()
        if (scannedQueue.isNotEmpty()) return scannedQueue
        return file?.let { listOf(it) }.orEmpty()
    }

    fun restoreAudioQueueFromScannedFilesIfNeeded() {
        val scannedQueue = allVisibleAudioFiles()
        if (scannedQueue.isEmpty()) return
        val currentQueueUris = audioQueue
            .map { it.normalizedUri() }
            .filter { it.isNotBlank() }
            .toSet()
        val scannedQueueUris = scannedQueue
            .map { it.normalizedUri() }
            .filter { it.isNotBlank() }
            .toSet()
        val shouldRestoreQueue = audioQueue.isEmpty() || currentQueueUris.none { it in scannedQueueUris }
        if (!shouldRestoreQueue) return
        val currentUri = selectedAudioUri ?: lastAudioUri ?: recentAudioUri ?: musicCurrentUri
        val nextIndex = scannedQueue.indexOfFirst {
            it.uri == currentUri || it.normalizedUri() == currentUri
        }.takeIf { it >= 0 } ?: 0
        audioQueue = scannedQueue
        currentAudioIndex = nextIndex
        if (selectedAudioUri.isNullOrBlank() && currentUri != null) {
            selectedAudioUri = scannedQueue.getOrNull(nextIndex)?.uri
        }
    }

    fun reshuffleAudioQueueKeepingCurrent(currentFile: LocalMediaFile?): List<LocalMediaFile> {
        val queue = audioPlaybackQueueFor(currentFile)
        val current = currentFile ?: return queue.shuffled(Random.Default)
        val currentUri = current.normalizedUri()
        return listOf(current) + queue
            .filterNot { it.normalizedUri() == currentUri }
            .shuffled(Random.Default)
    }

    fun findAudioFileByUri(uri: String?): LocalMediaFile? {
        if (uri.isNullOrBlank()) return null
        return audioQueue.firstOrNull { it.uri == uri || it.normalizedUri() == uri }
            ?: scannedFilesByFolder.values
                .flatten()
                .firstOrNull { it.uri == uri || it.normalizedUri() == uri }
    }

    fun fallbackCurrentAudioFile(preferred: LocalMediaFile? = null): LocalMediaFile? {
        return preferred
            ?: findAudioFileByUri(musicController?.currentMediaItem?.mediaId)
            ?: findAudioFileByUri(musicCurrentUri)
            ?: selectedMediaFile?.takeIf { it.type == LocalMediaType.AUDIO }
            ?: findAudioFileByUri(selectedAudioUri)
            ?: findAudioFileByUri(lastAudioUri)
            ?: findAudioFileByUri(recentAudioUri)
    }

    fun recordRecentAudio(file: LocalMediaFile?) {
        val uri = file?.normalizedUri().orEmpty()
        if (uri.isBlank()) return
        val next = listOf(PersistedAudioRecentRecord(uri, System.currentTimeMillis()))
            .plus(audioRecentRecords.filterNot { it.uri.trim() == uri })
            .take(100)
        audioRecentRecords = next
        lastAudioUri = uri
        recentAudioUri = uri
        coroutineScope.launch {
            appStateStore.saveLastAudioUri(uri)
            appStateStore.saveRecentAudioUri(uri)
            appStateStore.saveAudioRecentRecords(next)
        }
    }

    fun toggleFavoriteAudio(file: LocalMediaFile?) {
        val uri = file?.normalizedUri().orEmpty()
        if (uri.isBlank()) return
        val next = if (uri in favoriteAudioUris) {
            favoriteAudioUris - uri
        } else {
            favoriteAudioUris + uri
        }
        favoriteAudioUris = next
        coroutineScope.launch {
            appStateStore.saveFavoriteAudioUris(next)
        }
    }

    fun removeFavoriteAudio(file: LocalMediaFile?) {
        val uri = file?.normalizedUri().orEmpty()
        if (uri.isBlank() || uri !in favoriteAudioUris) return
        val next = favoriteAudioUris - uri
        favoriteAudioUris = next
        coroutineScope.launch {
            appStateStore.saveFavoriteAudioUris(next)
        }
    }

    fun removeAudioMemoryRecords(uris: Set<String>) {
        if (uris.isEmpty()) return
        val normalizedUris = uris.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        if (normalizedUris.isEmpty()) return
        val nextFavorites = favoriteAudioUris - normalizedUris
        val nextRecent = audioRecentRecords.filterNot { it.uri.trim() in normalizedUris }
        favoriteAudioUris = nextFavorites
        audioRecentRecords = nextRecent
        coroutineScope.launch {
            appStateStore.saveFavoriteAudioUris(nextFavorites)
            appStateStore.saveAudioRecentRecords(nextRecent)
        }
    }

    fun playAudioQueue(queue: List<LocalMediaFile>, file: LocalMediaFile, shuffle: Boolean = false) {
        val controller = musicController ?: return
        requestNotificationPermissionIfNeeded()
        val nextQueue = queue.ifEmpty { listOf(file) }
            .filterVisibleAudios()
            .ifEmpty { listOf(file) }
        val startIndex = nextQueue.indexOfFirst { it.normalizedUri() == file.normalizedUri() }
            .takeIf { it >= 0 } ?: 0
        audioQueue = nextQueue
        currentAudioIndex = startIndex
        selectedMediaFile = nextQueue[startIndex]
        selectedAudioUri = nextQueue[startIndex].uri
        recentAudioUri = selectedAudioUri
        recordRecentAudio(nextQueue[startIndex])
        if (shuffle) {
            audioPlayMode = AudioPlayMode.SHUFFLE
            audioShuffleHistory = emptyList()
            coroutineScope.launch {
                appStateStore.saveAudioPlayModeName(AudioPlayMode.SHUFFLE.name)
            }
        }
        applyAudioPlayMode(controller, audioPlayMode)
        controller.setMediaItems(
            nextQueue.map { it.toAudioMediaItem() },
            startIndex,
            0L
        )
        controller.prepare()
        controller.play()
    }

    fun applyAudioQueueScope(queue: List<LocalMediaFile>): Boolean {
        val nextQueue = queue.filterVisibleAudios().distinctBy { it.normalizedUri() }
        if (nextQueue.isEmpty()) return false
        val currentUri = musicController?.currentMediaItem?.mediaId
            ?: musicCurrentUri
            ?: selectedAudioUri
        if (currentUri.isNullOrBlank()) {
            audioQueue = nextQueue
            currentAudioIndex = 0
            return true
        }
        val currentIndex = nextQueue.indexOfFirst {
            it.uri == currentUri || it.normalizedUri() == currentUri
        }
        if (currentIndex < 0) return false
        audioQueue = nextQueue
        currentAudioIndex = currentIndex
        selectedAudioUri = nextQueue[currentIndex].uri
        selectedMediaFile = nextQueue[currentIndex]
        return true
    }

    fun playAllAudioWithoutInterrupt(queue: List<LocalMediaFile>) {
        if (applyAudioQueueScope(queue)) {
            Toast.makeText(context, "已切换到当前列表队列", Toast.LENGTH_SHORT).show()
            return
        }
        val firstFile = queue.filterVisibleAudios().firstOrNull()
        if (firstFile != null) {
            playAudioQueue(queue, firstFile)
        }
    }

    fun playOrResumeAudio(preferred: LocalMediaFile? = null): Boolean {
        val controller = musicController ?: return false
        if (controller.isPlaying) {
            controller.pause()
            return true
        }
        val currentFile = fallbackCurrentAudioFile(preferred)
        if (controller.mediaItemCount == 0) {
            val file = currentFile ?: allVisibleAudioFiles().firstOrNull() ?: return false
            playAudioQueue(audioPlaybackQueueFor(file), file)
            return true
        }
        when (controller.playbackState) {
            Player.STATE_IDLE -> controller.prepare()
            Player.STATE_ENDED -> controller.seekTo(
                controller.currentMediaItemIndex.coerceAtLeast(0),
                0L
            )
        }
        controller.play()
        return true
    }

    fun playAdjacentAudioFromRealQueue(
        preferred: LocalMediaFile?,
        direction: Int
    ): Boolean {
        val controller = musicController ?: return false
        val currentFile = fallbackCurrentAudioFile(preferred)
        val queue = audioPlaybackQueueFor(currentFile)
        if (queue.isEmpty()) return false
        val currentIndex = currentFile
            ?.let { file ->
                queue.indexOfFirst {
                    it.uri == file.uri || it.normalizedUri() == file.normalizedUri()
                }
            }
            ?.takeIf { it >= 0 }
            ?: controller.currentMediaItemIndex.takeIf { it in queue.indices }
            ?: 0
        val targetIndex = currentIndex + direction
        if (targetIndex !in queue.indices) return false
        playAudioQueue(queue, queue[targetIndex])
        return true
    }

    fun playWrappedAudioFromRealQueue(
        preferred: LocalMediaFile?,
        direction: Int
    ): Boolean {
        val controller = musicController ?: return false
        val currentFile = fallbackCurrentAudioFile(preferred)
        val queue = audioPlaybackQueueFor(currentFile)
        if (queue.size <= 1) return false
        val currentIndex = currentFile
            ?.let { file ->
                queue.indexOfFirst {
                    it.uri == file.uri || it.normalizedUri() == file.normalizedUri()
                }
            }
            ?.takeIf { it >= 0 }
            ?: controller.currentMediaItemIndex.takeIf { it in queue.indices }
            ?: 0
        val targetIndex = (currentIndex + direction + queue.size) % queue.size
        playAudioQueue(queue, queue[targetIndex])
        return true
    }

    fun playRandomAudioFromRealQueue(
        preferred: LocalMediaFile?,
        direction: Int
    ): Boolean {
        val currentFile = fallbackCurrentAudioFile(preferred)
        val queue = audioPlaybackQueueFor(currentFile)
        if (queue.size <= 1) return false
        val currentUri = currentFile?.normalizedUri()
        if (direction < 0) {
            val previousUri = audioShuffleHistory.lastOrNull()
            if (previousUri != null) {
                audioShuffleHistory = audioShuffleHistory.dropLast(1)
                val target = queue.firstOrNull { it.normalizedUri() == previousUri }
                if (target != null) {
                    playAudioQueue(queue, target)
                    return true
                }
            }
        }
        val candidates = queue.filterNot { it.normalizedUri() == currentUri }
        val target = candidates.randomOrNull(Random.Default) ?: queue.random(Random.Default)
        if (direction > 0 && !currentUri.isNullOrBlank()) {
            audioShuffleHistory = (audioShuffleHistory + currentUri).takeLast(80)
        }
        playAudioQueue(queue, target)
        return true
    }

    fun playAudioByMode(
        preferred: LocalMediaFile?,
        direction: Int
    ): Boolean {
        val controller = musicController ?: return false
        return when (audioPlayMode) {
            AudioPlayMode.SHUFFLE -> playRandomAudioFromRealQueue(preferred, direction)
            AudioPlayMode.REPEAT_ONE -> {
                controller.seekTo(0L)
                controller.play()
                true
            }
            AudioPlayMode.NORMAL,
            AudioPlayMode.REPEAT_ALL -> playWrappedAudioFromRealQueue(preferred, direction)
        }
    }

    fun refreshMusicControllerAfterAudioRemoval(removedUris: Set<String>) {
        if (removedUris.isEmpty()) return
        removeAudioMemoryRecords(removedUris)
        val controller = musicController ?: return
        val wasPlaying = controller.isPlaying
        val currentUri = controller.currentMediaItem?.mediaId
        val currentPosition = controller.currentPosition.coerceAtLeast(0L)
        if (audioQueue.isEmpty()) {
            controller.stop()
            controller.clearMediaItems()
            selectedMediaFile = selectedMediaFile?.takeIf { it.type != LocalMediaType.AUDIO }
            selectedAudioUri = null
            recentAudioUri = null
            lastAudioUri = null
            coroutineScope.launch {
                appStateStore.saveLastAudioUri(null)
                appStateStore.saveRecentAudioUri(null)
            }
            return
        }
        val currentWasRemoved = currentUri in removedUris
        val nextIndex = if (!currentWasRemoved && currentUri != null) {
            audioQueue.indexOfFirst { it.uri == currentUri }.takeIf { it >= 0 } ?: 0
        } else {
            currentAudioIndex.coerceIn(0, audioQueue.lastIndex)
        }
        val nextFile = audioQueue[nextIndex]
        currentAudioIndex = nextIndex
        selectedMediaFile = nextFile
        selectedAudioUri = nextFile.uri
        recentAudioUri = nextFile.uri
        lastAudioUri = nextFile.uri
        controller.setMediaItems(
            audioQueue.map { it.toAudioMediaItem() },
            nextIndex,
            if (currentWasRemoved) 0L else currentPosition
        )
        controller.prepare()
        if (wasPlaying && !currentWasRemoved) {
            controller.play()
        } else if (wasPlaying && currentWasRemoved) {
            controller.play()
        }
        coroutineScope.launch {
            appStateStore.saveLastAudioUri(lastAudioUri)
            appStateStore.saveRecentAudioUri(recentAudioUri)
        }
    }
    fun rebuildFolderBookFiles() {
        folderBookFiles.clear()
        folderBookFiles.addAll(
            folderBookFilesByFolder.values
                .flatten()
                .distinctBy { it.normalizedBookKey() }
                .sortedBy { it.name.lowercase() }
        )
    }

    fun replaceFolderBookFiles(folderId: String, files: List<LocalMediaFile>) {
        if (files.isEmpty()) {
            folderBookFilesByFolder.remove(folderId)
        } else {
            folderBookFilesByFolder[folderId] = files
                .filter { it.type == LocalMediaType.BOOK }
                .distinctBy { it.normalizedBookKey() }
        }
        rebuildFolderBookFiles()
    }

    fun clearFolderBookFiles() {
        folderBookFilesByFolder.clear()
        folderBookFiles.clear()
    }

    fun refreshFolderCounts(type: LocalMediaType, folderId: String) {
        val files = scannedFilesByFolder[typedFolderKey(type, folderId)].orEmpty()
        val targetFolders = when (type) {
            LocalMediaType.VIDEO -> videoFolders
            LocalMediaType.AUDIO -> audioFolders
            LocalMediaType.BOOK -> bookFolders
            else -> return
        }
        targetFolders.updateFolder(folderId) {
            copy(
                videoCount = files.count { it.type == LocalMediaType.VIDEO },
                audioCount = files.count { it.type == LocalMediaType.AUDIO },
                bookCount = files.count { it.type == LocalMediaType.BOOK },
                isScanning = false
            )
        }
    }

    fun removeFileFromFolderState(type: LocalMediaType, file: LocalMediaFile) {
        val prefix = "${type.name}:"
        val removedUri = file.normalizedUri()
        scannedFilesByFolder.keys
            .filter { it.startsWith(prefix) }
            .toList()
            .forEach { key ->
                val nextFiles = scannedFilesByFolder[key]
                    .orEmpty()
                    .filterNot { it.normalizedUri() == removedUri }
                if (nextFiles.isEmpty()) {
                    scannedFilesByFolder.remove(key)
                } else {
                    scannedFilesByFolder[key] = nextFiles
                }
                val folderId = key.removePrefix(prefix)
                if (type == LocalMediaType.BOOK) {
                    replaceFolderBookFiles(folderId, nextFiles)
                }
                refreshFolderCounts(type, folderId)
            }
    }

    fun removeMediaFromMemory(file: LocalMediaFile) {
        when (file.type) {
            LocalMediaType.VIDEO -> {
                removeFileFromFolderState(LocalMediaType.VIDEO, file)
                videoProgressMap.remove(file.uri)
                videoMetadataCache.remove(file.id)
                videoMetadataCache.remove(videoMetadataCacheKey(file))
                videoQueue = videoQueue.filterNot { it.uri == file.uri }
                currentVideoIndex = currentVideoIndex.coerceAtMost(videoQueue.lastIndex.coerceAtLeast(0))
                if (selectedVideoUri == file.uri) {
                    selectedVideoUri = null
                    selectedMediaFile = selectedMediaFile?.takeIf { it.uri != file.uri }
                }
                if (recentVideoUri == file.uri) {
                    recentVideoUri = null
                    recentVideoFile = null
                }
            }

            LocalMediaType.AUDIO -> {
                removeFileFromFolderState(LocalMediaType.AUDIO, file)
                audioProgressMap.remove(file.uri)
                audioQueue = audioQueue.filterNot { it.normalizedUri() == file.normalizedUri() }
                currentAudioIndex = currentAudioIndex.coerceAtMost(audioQueue.lastIndex.coerceAtLeast(0))
                if (selectedAudioUri?.trim() == file.normalizedUri()) {
                    selectedAudioUri = null
                    selectedMediaFile = selectedMediaFile?.takeIf { it.uri != file.uri }
                }
                if (recentAudioUri?.trim() == file.normalizedUri()) {
                    recentAudioUri = null
                }
                if (lastAudioUri?.trim() == file.normalizedUri()) {
                    lastAudioUri = null
                }
            }

            LocalMediaType.BOOK -> {
                val bookKey = file.normalizedBookKey()
                importedBookFiles.removeAll { it.normalizedBookKey() == bookKey }
                removeFileFromFolderState(LocalMediaType.BOOK, file)
                bookProgressMap.remove(file.uri)
                if (selectedBookUri == file.uri) {
                    selectedBookUri = null
                    selectedMediaFile = selectedMediaFile?.takeIf { it.uri != file.uri }
                }
            }

            else -> Unit
        }
    }

    fun removeMediaFromList(file: LocalMediaFile) {
        coroutineScope.launch {
            val bookKey = file.normalizedBookKey()
            val wasImportedBook = file.type == LocalMediaType.BOOK &&
                importedBookFiles.any { it.normalizedBookKey() == bookKey }
            removeMediaFromMemory(file)
            if (file.type == LocalMediaType.VIDEO) {
                hideVideoFiles(listOf(file))
                appStateStore.saveProgress(
                    PersistedPlaybackProgress(
                        mediaUri = file.uri,
                        mediaType = LocalMediaType.VIDEO.name,
                        positionMs = 0L,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                appStateStore.saveRecentVideoUri(recentVideoUri)
            }
            if (file.type == LocalMediaType.AUDIO) {
                hideAudioFiles(listOf(file))
                appStateStore.saveRecentAudioUri(recentAudioUri)
                refreshMusicControllerAfterAudioRemoval(setOf(file.normalizedUri()))
            }
            if (wasImportedBook) {
                appStateStore.removePersistedBookFile(file.uri)
                appStateStore.removeBookProgress(file.uri)
            }
            Toast.makeText(
                context,
                if (file.type == LocalMediaType.BOOK) {
                    "已从书架移除"
                } else if (file.type == LocalMediaType.VIDEO) {
                    "已从列表移除"
                } else if (file.type == LocalMediaType.AUDIO) {
                    "已隐藏歌曲"
                } else {
                    "已移除"
                },
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun removeMediaFromList(files: List<LocalMediaFile>) {
        val uniqueFiles = files.distinctBy { it.uri }
        if (uniqueFiles.isEmpty()) {
            Toast.makeText(context, "请先选择项目", Toast.LENGTH_SHORT).show()
            return
        }
        coroutineScope.launch {
            val removedAudioUris = uniqueFiles
                .filter { it.type == LocalMediaType.AUDIO }
                .map { it.normalizedUri() }
                .toSet()
            hideVideoFiles(uniqueFiles)
            hideAudioFiles(uniqueFiles)
            uniqueFiles.forEach { file ->
                val wasImportedBook = file.type == LocalMediaType.BOOK &&
                    importedBookFiles.any { it.normalizedBookKey() == file.normalizedBookKey() }
                removeMediaFromMemory(file)
                when (file.type) {
                    LocalMediaType.VIDEO -> {
                        appStateStore.saveProgress(
                            PersistedPlaybackProgress(
                                mediaUri = file.uri,
                                mediaType = LocalMediaType.VIDEO.name,
                                positionMs = 0L,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }

                    LocalMediaType.BOOK -> if (wasImportedBook) {
                        appStateStore.removePersistedBookFile(file.uri)
                        appStateStore.removeBookProgress(file.uri)
                    }

                    else -> Unit
                }
            }
            appStateStore.saveRecentVideoUri(recentVideoUri)
            appStateStore.saveRecentAudioUri(recentAudioUri)
            refreshMusicControllerAfterAudioRemoval(removedAudioUris)
            val message = if (uniqueFiles.all { it.type == LocalMediaType.AUDIO }) {
                "已隐藏歌曲"
            } else {
                "已移除 ${uniqueFiles.size} 项"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun isLocalMediaFileReadable(file: LocalMediaFile): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openFileDescriptor(Uri.parse(file.uri), "r")?.use { true } == true
            } catch (_: Exception) {
                false
            }
        }
    }

    suspend fun hideAudioFolder(folder: MediaFolderUiModel) {
        val folderKey = folder.id.trim()
        if (folderKey.isBlank()) return
        hiddenAudioFolderKeys = hiddenAudioFolderKeys + folderKey
        appStateStore.hideAudioFolderKeys(listOf(folderKey))
        audioFolders.removeAll { it.id.trim() == folderKey }
        scannedFilesByFolder.remove(typedFolderKey(LocalMediaType.AUDIO, folderKey))
        selectedAudioFolderKey = null
        Toast.makeText(context, "已隐藏音乐文件夹", Toast.LENGTH_SHORT).show()
    }

    fun showUnavailableVideoHint() {
        Toast.makeText(context, "文件已失效，可从列表移除", Toast.LENGTH_SHORT).show()
    }

    fun openVideoIfReadable(file: LocalMediaFile, queue: List<LocalMediaFile>) {
        coroutineScope.launch {
            if (!isLocalMediaFileReadable(file)) {
                withContext(Dispatchers.IO) {
                    VideoThumbnailStore.delete(context.applicationContext, file)
                }
                videoMetadataCache.remove(file.id)
                videoMetadataCache.remove(videoMetadataCacheKey(file))
                appStateStore.upsertUnavailableVideoRecord(file)
                unavailableVideoRecords.clear()
                unavailableVideoRecords.addAll(appStateStore.loadUnavailableVideoRecords())
                showUnavailableVideoHint()
                return@launch
            }
            videoQueue = queue.ifEmpty { listOf(file) }
            currentVideoIndex = videoQueue.indexOfFirst { it.uri == file.uri }
                .takeIf { it >= 0 } ?: 0
            selectedMediaFile = file
            selectedVideoUri = file.uri
            recentVideoFile = file
            recentVideoUri = file.uri
            appStateStore.saveRecentVideoUri(file.uri)
            navController.navigate(LocalVibeRoute.VideoPlayer)
        }
    }

    fun completeFolderVideoDelete(deletedFiles: List<LocalMediaFile>, requestedCount: Int) {
        coroutineScope.launch {
            val uniqueDeletedFiles = deletedFiles
                .filter { it.type == LocalMediaType.VIDEO }
                .distinctBy { it.normalizedUri() }
            withContext(Dispatchers.IO) {
                VideoThumbnailStore.delete(context.applicationContext, uniqueDeletedFiles)
            }
            uniqueDeletedFiles.forEach { file ->
                removeMediaFromMemory(file)
                appStateStore.saveProgress(
                    PersistedPlaybackProgress(
                        mediaUri = file.uri,
                        mediaType = LocalMediaType.VIDEO.name,
                        positionMs = 0L,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            if (uniqueDeletedFiles.isNotEmpty()) {
                appStateStore.saveRecentVideoUri(recentVideoUri)
            }
            if (uniqueDeletedFiles.size == requestedCount && requestedCount > 0) {
                folderVideoDeleteSuccessSignal = System.currentTimeMillis()
            } else {
                Toast.makeText(context, "删除失败，请检查系统权限", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun launchFolderVideoDeleteRequest(
        files: List<LocalMediaFile>,
        mode: FolderVideoDeleteMode,
        intentSender: android.content.IntentSender
    ) {
        val uniqueFiles = files
            .filter { it.type == LocalMediaType.VIDEO }
            .distinctBy { it.normalizedUri() }
        if (uniqueFiles.isEmpty()) {
            Toast.makeText(context, "请先选择项目", Toast.LENGTH_SHORT).show()
            return
        }
        pendingFolderVideoDeleteFiles = uniqueFiles
        pendingFolderVideoDeleteMode = mode
        runCatching {
            folderVideoDeleteLauncher.launch(
                IntentSenderRequest.Builder(intentSender).build()
            )
        }.onFailure {
            pendingFolderVideoDeleteFiles = emptyList()
            pendingFolderVideoDeleteMode = null
            Toast.makeText(context, "删除失败，请检查系统权限", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestFolderVideoPermanentDelete(files: List<LocalMediaFile>) {
        val uniqueFiles = files
            .filter { it.type == LocalMediaType.VIDEO }
            .distinctBy { it.normalizedUri() }
        if (uniqueFiles.isEmpty()) {
            Toast.makeText(context, "请先选择项目", Toast.LENGTH_SHORT).show()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uris = uniqueFiles.map { Uri.parse(it.uri) }
            runCatching {
                MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
            }.onSuccess { intentSender ->
                launchFolderVideoDeleteRequest(
                    files = uniqueFiles,
                    mode = FolderVideoDeleteMode.SystemDelete,
                    intentSender = intentSender
                )
            }.onFailure {
                Toast.makeText(context, "删除失败，请检查系统权限", Toast.LENGTH_SHORT).show()
            }
            return
        }
        coroutineScope.launch {
            val deletedFiles = mutableListOf<LocalMediaFile>()
            var recoverableIntentSender: android.content.IntentSender? = null
            var recoverableFiles: List<LocalMediaFile> = emptyList()
            var failedCount = 0
            withContext(Dispatchers.IO) {
                for ((index, file) in uniqueFiles.withIndex()) {
                    try {
                        if (deleteContentUriDirect(context.applicationContext, file.uri)) {
                            deletedFiles += file
                        } else {
                            failedCount += 1
                        }
                    } catch (exception: RecoverableSecurityException) {
                        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                            recoverableIntentSender =
                                exception.userAction.actionIntent.intentSender
                            recoverableFiles = uniqueFiles.drop(index)
                            break
                        } else {
                            failedCount += 1
                        }
                    } catch (exception: SecurityException) {
                        failedCount += 1
                    }
                }
            }
            if (deletedFiles.isNotEmpty()) {
                completeFolderVideoDelete(deletedFiles, deletedFiles.size)
            }
            val intentSender = recoverableIntentSender
            if (intentSender != null) {
                launchFolderVideoDeleteRequest(
                    files = recoverableFiles,
                    mode = FolderVideoDeleteMode.RetryAfterGrant,
                    intentSender = intentSender
                )
            } else if (failedCount > 0) {
                Toast.makeText(context, "删除失败，请检查系统权限", Toast.LENGTH_SHORT).show()
            }
        }
    }

    folderVideoDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val pendingFiles = pendingFolderVideoDeleteFiles
        val pendingMode = pendingFolderVideoDeleteMode
        pendingFolderVideoDeleteFiles = emptyList()
        pendingFolderVideoDeleteMode = null
        if (pendingFiles.isEmpty() || pendingMode == null) {
            return@rememberLauncherForActivityResult
        }
        if (result.resultCode != Activity.RESULT_OK) {
            Toast.makeText(context, "已取消删除", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        coroutineScope.launch {
            val deletedFiles = if (pendingMode == FolderVideoDeleteMode.RetryAfterGrant) {
                withContext(Dispatchers.IO) {
                    pendingFiles.filter { file ->
                        deleteContentUriDirect(context.applicationContext, file.uri)
                    }
                }
            } else {
                pendingFiles
            }
            completeFolderVideoDelete(deletedFiles, pendingFiles.size)
        }
    }

    fun permanentlyDeleteMedia(file: LocalMediaFile) {
        coroutineScope.launch {
            val deleted = withContext(Dispatchers.IO) {
                deleteUri(context.applicationContext, file.uri)
            }
            if (!deleted && file.type == LocalMediaType.AUDIO) {
                hideAudioFiles(listOf(file))
                removeMediaFromMemory(file)
                appStateStore.saveRecentAudioUri(recentAudioUri)
                refreshMusicControllerAfterAudioRemoval(setOf(file.normalizedUri()))
                Toast.makeText(context, "系统不允许直接删除该文件，已为你隐藏。", Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (deleted) {
                if (file.type == LocalMediaType.VIDEO) {
                    withContext(Dispatchers.IO) {
                        VideoThumbnailStore.delete(context.applicationContext, file)
                    }
                }
                removeMediaFromMemory(file)
                when (file.type) {
                    LocalMediaType.VIDEO -> {
                        appStateStore.saveProgress(
                            PersistedPlaybackProgress(
                                mediaUri = file.uri,
                                mediaType = LocalMediaType.VIDEO.name,
                                positionMs = 0L,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        appStateStore.saveRecentVideoUri(recentVideoUri)
                    }

                    LocalMediaType.AUDIO -> {
                        appStateStore.saveRecentAudioUri(recentAudioUri)
                        refreshMusicControllerAfterAudioRemoval(setOf(file.normalizedUri()))
                    }
                    LocalMediaType.BOOK -> {
                        appStateStore.removePersistedBookFile(file.uri)
                        appStateStore.removeBookProgress(file.uri)
                    }
                    else -> Unit
                }
                Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "删除失败，请检查文件权限", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun permanentlyDeleteMedia(files: List<LocalMediaFile>) {
        val uniqueFiles = files.distinctBy { it.uri }
        if (uniqueFiles.isEmpty()) {
            Toast.makeText(context, "请先选择项目", Toast.LENGTH_SHORT).show()
            return
        }
        coroutineScope.launch {
            val results = withContext(Dispatchers.IO) {
                uniqueFiles.associateWith { file ->
                    deleteUri(context.applicationContext, file.uri)
                }
            }
            val deletedFiles = results.filterValues { it }.keys.toList()
            withContext(Dispatchers.IO) {
                VideoThumbnailStore.delete(
                    context.applicationContext,
                    deletedFiles.filter { it.type == LocalMediaType.VIDEO }
                )
            }
            val failedAudioFiles = results
                .filter { !it.value && it.key.type == LocalMediaType.AUDIO }
                .keys
                .toList()
            val failedCount = results.count { !it.value && it.key.type != LocalMediaType.AUDIO }
            val deletedAudioUris = deletedFiles
                .filter { it.type == LocalMediaType.AUDIO }
                .map { it.normalizedUri() }
                .toSet()
            if (failedAudioFiles.isNotEmpty()) {
                hideAudioFiles(failedAudioFiles)
            }
            deletedFiles.forEach { file ->
                removeMediaFromMemory(file)
                when (file.type) {
                    LocalMediaType.VIDEO -> {
                        appStateStore.saveProgress(
                            PersistedPlaybackProgress(
                                mediaUri = file.uri,
                                mediaType = LocalMediaType.VIDEO.name,
                                positionMs = 0L,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }

                    LocalMediaType.BOOK -> {
                        appStateStore.removePersistedBookFile(file.uri)
                        appStateStore.removeBookProgress(file.uri)
                    }
                    else -> Unit
                }
            }
            failedAudioFiles.forEach { file ->
                removeMediaFromMemory(file)
            }
            appStateStore.saveRecentVideoUri(recentVideoUri)
            appStateStore.saveRecentAudioUri(recentAudioUri)
            refreshMusicControllerAfterAudioRemoval(
                deletedAudioUris + failedAudioFiles.map { it.normalizedUri() }
            )
            if (deletedFiles.isNotEmpty()) {
                Toast.makeText(context, "已删除 ${deletedFiles.size} 项", Toast.LENGTH_SHORT).show()
            }
            if (failedCount > 0) {
                Toast.makeText(context, "$failedCount 项删除失败，请检查文件权限", Toast.LENGTH_SHORT).show()
            }
            if (failedAudioFiles.isNotEmpty()) {
                Toast.makeText(context, "系统不允许直接删除 ${failedAudioFiles.size} 首音乐，已为你隐藏。", Toast.LENGTH_SHORT).show()
            }
        }
    }


    fun updateCategoryFolder(
        targetType: LocalMediaType,
        folder: MediaFolderUiModel,
        files: List<LocalMediaFile>
    ) {
        val targetFolders = when (targetType) {
            LocalMediaType.VIDEO -> videoFolders
            LocalMediaType.AUDIO -> audioFolders
            LocalMediaType.BOOK -> bookFolders
            else -> return
        }
        targetFolders.upsertFolder(folder.id) { folder }
        val visibleFiles = when (targetType) {
            LocalMediaType.VIDEO -> files.filterVisibleVideos()
            LocalMediaType.AUDIO -> files.filterVisibleAudios()
            else -> files
        }
        scannedFilesByFolder[typedFolderKey(targetType, folder.id)] = visibleFiles
        if (targetType == LocalMediaType.BOOK) {
            replaceFolderBookFiles(folder.id, visibleFiles)
        }
    }

    fun List<com.shenghui.localvibe.core.scanner.ScannedMediaFolder>.filterHiddenVideoFolders(): List<com.shenghui.localvibe.core.scanner.ScannedMediaFolder> {
        return filterNot { it.folder.id.trim() in hiddenVideoFolderIds }
            .mapNotNull { result ->
                val visibleFiles = result.files.filterVisibleVideos()
                if (visibleFiles.isEmpty()) {
                    null
                } else {
                    result.copy(
                        folder = result.folder.copy(
                            videoCount = visibleFiles.count { it.type == LocalMediaType.VIDEO }
                        ),
                        files = visibleFiles
                    )
                }
            }
    }

    suspend fun hideVideoFolders(folders: List<MediaFolderUiModel>) {
        val folderIds = folders.map { it.id.trim() }.filter { it.isNotBlank() }.toSet()
        if (folderIds.isEmpty()) return
        val autoFolderIds = folderIds.filter { it.startsWith("auto:") }.toSet()
        val manualFolderIds = folderIds - autoFolderIds
        if (autoFolderIds.isNotEmpty()) {
            hiddenVideoFolderIds = hiddenVideoFolderIds + autoFolderIds
            appStateStore.hideVideoFolderRecords(
                folders
                    .filter { it.id.trim() in autoFolderIds }
                    .map { folder ->
                        PersistedVideoVisibilityRecord(
                            id = folder.id.trim(),
                            name = folder.name.ifBlank { folder.id.trim() },
                            uri = folder.uri,
                            path = folder.uri,
                            recordedAt = System.currentTimeMillis()
                        )
                    }
            )
            hiddenVideoFolderRecords.clear()
            hiddenVideoFolderRecords.addAll(appStateStore.loadHiddenVideoFolderRecords())
        }
        if (manualFolderIds.isNotEmpty()) {
            appStateStore.removeFolders(LocalMediaType.VIDEO.name, manualFolderIds)
        }
        videoFolders.removeAll { it.id in folderIds }
        folderIds.forEach { folderId ->
            scannedFilesByFolder.remove(typedFolderKey(LocalMediaType.VIDEO, folderId))
        }
    }

    suspend fun cleanupVideoThumbnailsForMissingFiles(
        previousFiles: Collection<LocalMediaFile>,
        nextFiles: Collection<LocalMediaFile>
    ) {
        val nextUris = nextFiles
            .filter { it.type == LocalMediaType.VIDEO }
            .map { it.normalizedUri() }
            .toSet()
        val missingFiles = previousFiles
            .filter { it.type == LocalMediaType.VIDEO && it.normalizedUri() !in nextUris }
            .distinctBy { it.normalizedUri() }
        if (missingFiles.isEmpty()) return
        missingFiles.forEach { file ->
            videoMetadataCache.remove(file.id)
            videoMetadataCache.remove(videoMetadataCacheKey(file))
        }
        withContext(Dispatchers.IO) {
            VideoThumbnailStore.delete(context.applicationContext, missingFiles)
        }
    }

    fun currentAutoVideoFiles(): List<LocalMediaFile> {
        val prefix = "${LocalMediaType.VIDEO.name}:auto:"
        return scannedFilesByFolder
            .filterKeys { it.startsWith(prefix) }
            .values
            .flatten()
            .filter { it.type == LocalMediaType.VIDEO }
    }

    fun currentScannedVideoFiles(): List<LocalMediaFile> {
        return scannedFilesByFolder
            .filterKeys { it.startsWith("${LocalMediaType.VIDEO.name}:") }
            .values
            .flatten()
            .filter { it.type == LocalMediaType.VIDEO }
            .distinctBy { it.normalizedUri() }
    }

    fun prewarmVideoThumbnails(files: Collection<LocalMediaFile>) {
        videoThumbnailPrewarmer.start(files.filter { it.type == LocalMediaType.VIDEO })
    }

    fun runAutoScan(targetType: LocalMediaType, showCompletionToast: Boolean = false) {
        if (targetType == LocalMediaType.VIDEO && !isVideoVisibilityReady) {
            return
        }
        if (targetType == LocalMediaType.VIDEO) {
            isVideoAutoScanning = true
        }
        coroutineScope.launch {
            when (targetType) {
                LocalMediaType.VIDEO -> {
                    try {
                        val previousAutoVideoFiles = currentAutoVideoFiles()
                        val folders = withContext(Dispatchers.IO) {
                            MediaStoreScanner.scanVideos(context.applicationContext)
                        }.filterHiddenVideoFolders()
                        cleanupVideoThumbnailsForMissingFiles(
                            previousFiles = previousAutoVideoFiles,
                            nextFiles = folders.flatMap { it.files }
                        )
                        videoFolders.removeAutoFoldersAndScans(LocalMediaType.VIDEO, scannedFilesByFolder)
                        folders.forEach { result ->
                            videoFolders.add(result.folder)
                            scannedFilesByFolder[typedFolderKey(LocalMediaType.VIDEO, result.folder.id)] =
                                result.files
                        }
                        prewarmVideoThumbnails(currentScannedVideoFiles())
                        if (showCompletionToast && folders.isEmpty()) {
                            Toast.makeText(context, "没有扫描到视频文件", Toast.LENGTH_SHORT).show()
                        }
                        if (showCompletionToast) {
                            Toast.makeText(context, "媒体扫描已完成", Toast.LENGTH_SHORT).show()
                        }
                    } finally {
                        isVideoInitialScanComplete = true
                        isVideoAutoScanning = false
                    }
                }

                LocalMediaType.AUDIO -> {
                    val folders = withContext(Dispatchers.IO) {
                        MediaStoreScanner.scanAudios(context.applicationContext)
                    }.filterHiddenAudioFolders()
                    audioFolders.removeAutoFoldersAndScans(LocalMediaType.AUDIO, scannedFilesByFolder)
                    folders.forEach { result ->
                        audioFolders.add(result.folder)
                        scannedFilesByFolder[typedFolderKey(LocalMediaType.AUDIO, result.folder.id)] =
                            result.files
                    }
                    restoreAudioQueueFromScannedFilesIfNeeded()
                    if (showCompletionToast && folders.isEmpty()) {
                        Toast.makeText(context, "没有扫描到音乐文件", Toast.LENGTH_SHORT).show()
                    }
                    if (showCompletionToast) {
                        Toast.makeText(context, "媒体扫描已完成", Toast.LENGTH_SHORT).show()
                    }
                }

                LocalMediaType.BOOK -> {
                    Log.d(
                        BOOK_LOG_TAG,
                        "book folder scan skipped, imported=${importedBookFiles.size}, folder=${folderBookFiles.size}"
                    )
                    if (showCompletionToast) {
                        Toast.makeText(context, "小说请通过导入本地文档添加", Toast.LENGTH_SHORT).show()
                    }
                }

                else -> Unit
            }
        }
    }

    fun rescanCurrentVideoFolder() {
        val folder = currentFolder
        val folderId = folder?.id?.trim().orEmpty()
        if (currentFolderTargetType != LocalMediaType.VIDEO || folder == null || folderId.isBlank()) {
            Toast.makeText(context, "重新扫描失败", Toast.LENGTH_SHORT).show()
            return
        }
        if (!isVideoVisibilityReady) {
            Toast.makeText(context, "重新扫描失败", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(context, "正在重新扫描当前文件夹…", Toast.LENGTH_SHORT).show()
        if (folderId.startsWith("auto:")) {
            isVideoAutoScanning = true
            coroutineScope.launch {
                try {
                    val previousAutoVideoFiles = currentAutoVideoFiles()
                    val folders = withContext(Dispatchers.IO) {
                        MediaStoreScanner.scanVideos(context.applicationContext)
                    }.filterHiddenVideoFolders()
                    cleanupVideoThumbnailsForMissingFiles(
                        previousFiles = previousAutoVideoFiles,
                        nextFiles = folders.flatMap { it.files }
                    )
                    videoFolders.removeAutoFoldersAndScans(LocalMediaType.VIDEO, scannedFilesByFolder)
                    folders.forEach { result ->
                        videoFolders.add(result.folder)
                        scannedFilesByFolder[typedFolderKey(LocalMediaType.VIDEO, result.folder.id)] =
                            result.files
                    }
                    prewarmVideoThumbnails(currentScannedVideoFiles())
                    currentFolder = folders.firstOrNull { it.folder.id == folderId }?.folder
                        ?: folder.copy(videoCount = 0, isScanning = false)
                    Toast.makeText(
                        context,
                        "已重新扫描媒体库并刷新当前文件夹",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (throwable: Throwable) {
                    Log.w(VIDEO_LOG_TAG, "rescan current video folder failed id=$folderId", throwable)
                    Toast.makeText(context, "重新扫描失败", Toast.LENGTH_SHORT).show()
                } finally {
                    isVideoInitialScanComplete = true
                    isVideoAutoScanning = false
                }
            }
            return
        }

        coroutineScope.launch {
            try {
                val folderUri = Uri.parse(folder.uri.ifBlank { folderId })
                val previousFiles = scannedFilesByFolder[
                    typedFolderKey(LocalMediaType.VIDEO, folderId)
                ].orEmpty()
                val typedFiles = withContext(Dispatchers.IO) {
                    FolderScanner.scanFolder(context.applicationContext, folderUri)
                }
                    .filter { it.type == LocalMediaType.VIDEO }
                    .filterVisibleVideos()
                cleanupVideoThumbnailsForMissingFiles(previousFiles, typedFiles)
                val refreshedFolder = folder.copy(
                    videoCount = typedFiles.count { it.type == LocalMediaType.VIDEO },
                    isScanning = false
                )
                updateCategoryFolder(
                    targetType = LocalMediaType.VIDEO,
                    folder = refreshedFolder,
                    files = typedFiles
                )
                prewarmVideoThumbnails(typedFiles)
                currentFolder = refreshedFolder
                appStateStore.upsertFolder(
                    PersistedFolder(
                        folderId = folderId,
                        name = refreshedFolder.name,
                        uri = refreshedFolder.uri,
                        targetType = LocalMediaType.VIDEO.name,
                        source = "MANUAL",
                        addedAt = System.currentTimeMillis(),
                        lastScannedAt = System.currentTimeMillis()
                    )
                )
                Toast.makeText(context, "已刷新当前文件夹", Toast.LENGTH_SHORT).show()
            } catch (throwable: Throwable) {
                Log.w(VIDEO_LOG_TAG, "rescan manual video folder failed id=$folderId", throwable)
                Toast.makeText(context, "重新扫描失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val canScanVideo = LocalMediaType.VIDEO.mediaReadPermission()
            ?.let { grants[it] == true || hasPermission(context, it) } ?: true
        val canScanAudio = LocalMediaType.AUDIO.mediaReadPermission()
            ?.let { grants[it] == true || hasPermission(context, it) } ?: true
        videoPermissionDenied = !canScanVideo
        audioPermissionDenied = !canScanAudio
        if (canScanVideo && isVideoVisibilityReady) runAutoScan(LocalMediaType.VIDEO)
        if (!canScanVideo) {
            isVideoInitialScanComplete = true
            isVideoAutoScanning = false
        }
        if (canScanAudio) runAutoScan(LocalMediaType.AUDIO)
        if (!canScanVideo || !canScanAudio) {
            Toast.makeText(context, "没有媒体权限时可以使用手动添加文件夹。", Toast.LENGTH_SHORT).show()
        }
    }
    val openDocumentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }
        val targetType = currentAddTargetType ?: LocalMediaType.VIDEO

        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        val uriText = uri.toString()
        val targetFolders = when (targetType) {
            LocalMediaType.VIDEO -> videoFolders
            LocalMediaType.AUDIO -> audioFolders
            LocalMediaType.BOOK -> bookFolders
            else -> videoFolders
        }
        targetFolders.upsertFolder(uriText) {
            MediaFolderUiModel(
                id = uriText,
                name = resolveDocumentTreeName(context, uri),
                uri = uriText,
                videoCount = 0,
                audioCount = 0,
                bookCount = 0,
                isScanning = true
            )
        }

        coroutineScope.launch {
            val previousFiles = scannedFilesByFolder[typedFolderKey(targetType, uriText)].orEmpty()
            val scannedFiles = withContext(Dispatchers.IO) {
                runCatching {
                    FolderScanner.scanFolder(context.applicationContext, uri)
                }.getOrDefault(emptyList())
            }
            val typedFiles = scannedFiles.filter { it.type == targetType }
                .let { files ->
                    when (targetType) {
                        LocalMediaType.VIDEO -> files.filterVisibleVideos()
                        LocalMediaType.AUDIO -> files.filterVisibleAudios()
                        else -> files
                    }
                }
            if (targetType == LocalMediaType.VIDEO) {
                cleanupVideoThumbnailsForMissingFiles(previousFiles, typedFiles)
            }

            if (typedFiles.isEmpty()) {
                targetFolders.removeAll { it.id == uriText }
                scannedFilesByFolder.remove(typedFolderKey(targetType, uriText))
                if (targetType == LocalMediaType.BOOK) {
                    replaceFolderBookFiles(uriText, emptyList())
                }
                Toast.makeText(context, targetType.emptyFolderMessage(), Toast.LENGTH_SHORT).show()
            } else {
                updateCategoryFolder(
                    targetType = targetType,
                    folder = MediaFolderUiModel(
                        id = uriText,
                        name = resolveDocumentTreeName(context, uri),
                        uri = uriText,
                        videoCount = typedFiles.count { it.type == LocalMediaType.VIDEO },
                        audioCount = typedFiles.count { it.type == LocalMediaType.AUDIO },
                        bookCount = typedFiles.count { it.type == LocalMediaType.BOOK },
                        isScanning = false
                    ),
                    files = typedFiles
                )
                if (targetType == LocalMediaType.VIDEO) {
                    prewarmVideoThumbnails(typedFiles)
                }
                appStateStore.upsertFolder(
                    PersistedFolder(
                        folderId = uriText,
                        name = resolveDocumentTreeName(context, uri),
                        uri = uriText,
                        targetType = targetType.name,
                        source = "MANUAL",
                        addedAt = System.currentTimeMillis(),
                        lastScannedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }
    val openTextDocumentsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) {
            return@rememberLauncherForActivityResult
        }
        var skippedCount = 0
        val selectedBooks = uris.mapNotNull { uri ->
            val bookFile = uri.toLocalBookFile(context.applicationContext)
            if (bookFile == null || bookFile.extension.lowercase() != "txt") {
                skippedCount += 1
                null
            } else {
                uri to bookFile
            }
        }
        Log.d(
            BOOK_LOG_TAG,
            "selected txt count=${selectedBooks.size}, uris=${selectedBooks.joinToString { it.second.uri }}"
        )
        val persistedBooks = mutableListOf<PersistedBookFile>()
        selectedBooks.forEach { (uri, bookFile) ->
            val uriText = uri.toString()
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }.onFailure {
                Log.w(BOOK_LOG_TAG, "persist permission failed uri=$uriText", it)
            }
            persistedBooks.add(
                PersistedBookFile(
                    id = uriText,
                    name = bookFile.name,
                    uri = uriText,
                    addedAt = System.currentTimeMillis()
                )
            )
        }
        if (persistedBooks.isNotEmpty()) {
            coroutineScope.launch {
                val importedBefore = importedBookFiles.size
                val persistedBefore = appStateStore.getPersistedBookFiles().size
                appStateStore.upsertPersistedBookFiles(persistedBooks)
                val savedBooks = appStateStore.getPersistedBookFiles()
                val selectedBookKeys = selectedBooks
                    .map { it.second.normalizedBookKey() }
                    .toSet()
                val nextImportedBooks = importedBookFiles
                    .filterNot { it.normalizedBookKey() in selectedBookKeys }
                    .plus(selectedBooks.map { it.second })
                    .distinctBy { it.normalizedBookKey() }
                    .sortedBy { it.name.lowercase() }
                importedBookFiles.clear()
                importedBookFiles.addAll(nextImportedBooks)
                Log.d(
                    BOOK_LOG_TAG,
                    "persisted before=$persistedBefore, saved book files count=${savedBooks.size}, distinct before=${persistedBooks.size}, distinct after=${savedBooks.distinctBy { it.uri.trim() }.size}, uris=${savedBooks.joinToString { it.uri }}"
                )
                Log.d(
                    BOOK_LOG_TAG,
                    "imported before=$importedBefore, importedBookFiles count=${importedBookFiles.size}, uris=${importedBookFiles.joinToString { it.uri }}"
                )
                Toast.makeText(context, "已导入 ${selectedBooks.size} 本小说", Toast.LENGTH_SHORT).show()
            }
        }
        if (skippedCount > 0) {
            Toast.makeText(context, "已跳过非 TXT 文件", Toast.LENGTH_SHORT).show()
        }
    }
    fun autoScanVideoAndAudio(force: Boolean = false, showCompletionToast: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastAutoScanAt < AUTO_SCAN_THROTTLE_MS) {
            return
        }
        lastAutoScanAt = now

        val missingPermissions = mediaScanPermissions()
            .filter { !hasPermission(context, it) }
        if (missingPermissions.isNotEmpty()) {
            mediaPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            videoPermissionDenied = false
            audioPermissionDenied = false
            runAutoScan(LocalMediaType.VIDEO, showCompletionToast = false)
            runAutoScan(LocalMediaType.AUDIO, showCompletionToast = showCompletionToast)
        }
    }

    suspend fun restoreHiddenAudioFolders() {
        appStateStore.clearHiddenAudioFolderKeys()
        hiddenAudioFolderKeys = emptySet()
        autoScanVideoAndAudio(force = true, showCompletionToast = false)
        Toast.makeText(context, "已恢复隐藏音乐文件夹", Toast.LENGTH_SHORT).show()
    }

    suspend fun restoreHiddenAudioSongs() {
        appStateStore.clearHiddenAudioUris()
        hiddenAudioUris = emptySet()
        autoScanVideoAndAudio(force = true, showCompletionToast = false)
        Toast.makeText(context, "已恢复隐藏歌曲", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(Unit) {
        initialMainRoute = appStateStore.loadLastMainTabRoute().toRestorableMainRoute()
    }

    LaunchedEffect(Unit) {
        appStateStore.loadProgress().forEach { progress ->
            when (progress.mediaType) {
                LocalMediaType.VIDEO.name -> {
                    if (progress.positionMs > 0L) {
                        videoProgressMap[progress.mediaUri] = progress.positionMs
                    }
                }

                LocalMediaType.AUDIO.name -> Unit
            }
        }
        recentVideoUri = appStateStore.loadRecentVideoUri()
        val persistedRecentAudioUri = appStateStore.loadRecentAudioUri()
        recentAudioUri = persistedRecentAudioUri
        lastAudioUri = appStateStore.loadLastAudioUri() ?: persistedRecentAudioUri
        audioPlayMode = appStateStore.loadAudioPlayModeName().toAudioPlayMode()
        audioSortMode = appStateStore.loadAudioSortModeName().toAudioSortMode()
        audioSortAscending = appStateStore.loadAudioSortAscending()
        favoriteAudioUris = appStateStore.loadFavoriteAudioUris()
        audioRecentRecords = appStateStore.loadAudioRecentRecords()
            .ifEmpty {
                recentAudioUri
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { listOf(PersistedAudioRecentRecord(it, System.currentTimeMillis())) }
                    .orEmpty()
            }
        hiddenAudioUris = appStateStore.loadHiddenAudioUris()
        hiddenAudioFolderKeys = appStateStore.loadHiddenAudioFolderKeys()
        hiddenVideoUris = appStateStore.loadHiddenVideoUris()
        hiddenVideoFolderIds = appStateStore.loadHiddenVideoFolderIds()
        hiddenVideoRecords.clear()
        hiddenVideoRecords.addAll(appStateStore.loadHiddenVideoRecords())
        hiddenVideoFolderRecords.clear()
        hiddenVideoFolderRecords.addAll(appStateStore.loadHiddenVideoFolderRecords())
        unavailableVideoRecords.clear()
        unavailableVideoRecords.addAll(appStateStore.loadUnavailableVideoRecords())
        videoFolderPlaybackSpeeds.clear()
        videoFolderPlaybackSpeeds.putAll(appStateStore.loadVideoFolderPlaybackSpeeds())
        isVideoVisibilityReady = true
        bookProgressMap.clear()
        appStateStore.loadBookProgress().forEach { progress ->
            if (progress.totalParagraphs > 0) {
                bookProgressMap[progress.uri] = progress
            }
        }

        val grantedUris = context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission }
            .map { it.uri.toString() }
            .toSet()
        var skippedFolderCount = 0
        var restoredFolderBookCount = 0

        val persistedBookFiles = appStateStore.getPersistedBookFiles()
        Log.d(BOOK_LOG_TAG, "persistedBookFiles count=${persistedBookFiles.size}")
        val restoredImportedBooks = mutableListOf<LocalMediaFile>()
        persistedBookFiles.forEach { persistedBook ->
            val uri = Uri.parse(persistedBook.uri)
            val canOpen = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { true } ?: false
                }.onFailure {
                    Log.w(BOOK_LOG_TAG, "restore imported book failed uri=${persistedBook.uri}", it)
                }.getOrDefault(false)
            }
            if (!canOpen) {
                return@forEach
            }
            val file = uri.toLocalBookFile(context.applicationContext)
                ?: LocalMediaFile(
                    id = persistedBook.uri,
                    name = persistedBook.name,
                    uri = persistedBook.uri,
                    type = LocalMediaType.BOOK,
                    extension = "txt",
                    size = 0L,
                    parentFolderName = null
                )
            restoredImportedBooks.add(file)
            Log.d(
                BOOK_LOG_TAG,
                "restored imported book uri=${persistedBook.uri}, hasPermission=${persistedBook.uri in grantedUris}"
            )
        }
        importedBookFiles.clear()
        importedBookFiles.addAll(
            restoredImportedBooks
                .distinctBy { it.normalizedBookKey() }
                .sortedBy { it.name.lowercase() }
        )
        Log.d(BOOK_LOG_TAG, "restored imported book files count=${importedBookFiles.size}")

        appStateStore.loadFolders()
            .filter { it.source == "MANUAL" }
            .forEach { persistedFolder ->
                val targetType = persistedFolder.targetType.toLocalMediaTypeOrNull()
                    ?: return@forEach
                if (targetType == LocalMediaType.BOOK) {
                    Log.d(BOOK_LOG_TAG, "skip restored book folder uri=${persistedFolder.uri}")
                    return@forEach
                }
                if (persistedFolder.uri !in grantedUris) {
                    skippedFolderCount += 1
                    return@forEach
                }

                val scannedFiles = withContext(Dispatchers.IO) {
                    runCatching {
                        FolderScanner.scanFolder(
                            context.applicationContext,
                            Uri.parse(persistedFolder.uri)
                        )
                    }.getOrDefault(emptyList())
                }
                val typedFiles = scannedFiles.filter { it.type == targetType }
                    .let { files ->
                        when (targetType) {
                            LocalMediaType.VIDEO -> files.filterVisibleVideos()
                            LocalMediaType.AUDIO -> files.filterVisibleAudios()
                            else -> files
                        }
                    }
                if (typedFiles.isNotEmpty()) {
                    if (targetType == LocalMediaType.BOOK) {
                        restoredFolderBookCount += typedFiles.size
                    }
                    updateCategoryFolder(
                        targetType = targetType,
                        folder = MediaFolderUiModel(
                            id = persistedFolder.folderId,
                            name = persistedFolder.name,
                            uri = persistedFolder.uri,
                            videoCount = typedFiles.count { it.type == LocalMediaType.VIDEO },
                            audioCount = typedFiles.count { it.type == LocalMediaType.AUDIO },
                            bookCount = typedFiles.count { it.type == LocalMediaType.BOOK },
                            isScanning = false
                        ),
                        files = typedFiles
                    )
                } else if (targetType == LocalMediaType.BOOK) {
                    replaceFolderBookFiles(persistedFolder.folderId, emptyList())
                }
            }
        prewarmVideoThumbnails(currentScannedVideoFiles())
        Log.d(BOOK_LOG_TAG, "restored folder book count=$restoredFolderBookCount")
        Log.d(
            BOOK_LOG_TAG,
            "final bookFiles count=${importedBookFiles.distinctBy { it.normalizedBookKey() }.size}"
        )

        if (skippedFolderCount > 0) {
            Toast.makeText(context, "部分文件夹需要重新授权", Toast.LENGTH_SHORT).show()
        }
        autoScanVideoAndAudio(force = true)
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                autoScanVideoAndAudio()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    DisposableEffect(Unit) {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicPlaybackService::class.java)
        )
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                musicController = controllerFuture.get()
                applyAudioPlayMode(musicController, audioPlayMode)
            },
            ContextCompat.getMainExecutor(context)
        )
        onDispose {
            MediaController.releaseFuture(controllerFuture)
            musicController = null
        }
    }
    DisposableEffect(musicController) {
        val controller = musicController ?: return@DisposableEffect onDispose { }
        fun syncMusicState() {
            musicCurrentUri = controller.currentMediaItem?.mediaId
            musicIsPlaying = controller.isPlaying
            musicCurrentPositionMs = controller.currentPosition.coerceAtLeast(0L)
            musicDurationMs = controller.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L
            currentAudioIndex = controller.currentMediaItemIndex.coerceAtLeast(0)
            selectedAudioUri = musicCurrentUri ?: selectedAudioUri
        }
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                syncMusicState()
                mediaItem?.mediaId?.let { uri ->
                    val file = findAudioFileByUri(uri)
                    selectedMediaFile = file ?: selectedMediaFile
                    recordRecentAudio(file)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                syncMusicState()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                syncMusicState()
            }
        }
        controller.addListener(listener)
        syncMusicState()
        onDispose {
            controller.removeListener(listener)
        }
    }
    LaunchedEffect(musicController) {
        while (musicController != null) {
            val controller = musicController
            if (controller != null) {
                musicCurrentUri = controller.currentMediaItem?.mediaId
                musicIsPlaying = controller.isPlaying
                musicCurrentPositionMs = controller.currentPosition.coerceAtLeast(0L)
                musicDurationMs = controller.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L
            }
            kotlinx.coroutines.delay(500)
        }
    }
    LaunchedEffect(musicController, audioPlayMode) {
        applyAudioPlayMode(musicController, audioPlayMode)
    }

    fun refreshVideoVisibilityRecordState() {
        coroutineScope.launch {
            hiddenVideoRecords.clear()
            hiddenVideoRecords.addAll(appStateStore.loadHiddenVideoRecords())
            hiddenVideoFolderRecords.clear()
            hiddenVideoFolderRecords.addAll(appStateStore.loadHiddenVideoFolderRecords())
            unavailableVideoRecords.clear()
            unavailableVideoRecords.addAll(appStateStore.loadUnavailableVideoRecords())
        }
    }

    fun restoreVideoVisibilityRecords(records: List<VideoVisibilityRecordUiModel>) {
        val restorableRecords = records.filter { it.type != VideoVisibilityRecordType.UNAVAILABLE_FILE }
        if (restorableRecords.isEmpty()) {
            Toast.makeText(context, "失效文件无法恢复，只能清除记录", Toast.LENGTH_SHORT).show()
            return
        }
        coroutineScope.launch {
            val folderIds = restorableRecords
                .filter { it.type == VideoVisibilityRecordType.HIDDEN_FOLDER }
                .map { it.id.trim() }
                .filter { it.isNotBlank() }
                .toSet()
            val removedVideoRecords = restorableRecords
                .filter { it.type == VideoVisibilityRecordType.REMOVED_VIDEO }
            if (folderIds.isNotEmpty()) {
                hiddenVideoFolderIds = hiddenVideoFolderIds - folderIds
                appStateStore.removeHiddenVideoFolderIds(folderIds)
            }

            val removedUris = removedVideoRecords.map { it.id.trim() }.filter { it.isNotBlank() }.toSet()
            if (removedUris.isNotEmpty()) {
                hiddenVideoUris = hiddenVideoUris - removedUris
                appStateStore.removeHiddenVideoUris(removedUris)
            }

            var unavailableCount = 0
            removedVideoRecords.forEach { record ->
                val file = record.toLocalVideoFile()
                if (!isLocalMediaFileReadable(file)) {
                    unavailableCount += 1
                    withContext(Dispatchers.IO) {
                        VideoThumbnailStore.delete(context.applicationContext, file)
                    }
                    videoMetadataCache.remove(file.id)
                    videoMetadataCache.remove(videoMetadataCacheKey(file))
                    appStateStore.upsertUnavailableVideoRecord(file)
                }
            }

            hiddenVideoRecords.clear()
            hiddenVideoRecords.addAll(appStateStore.loadHiddenVideoRecords())
            hiddenVideoFolderRecords.clear()
            hiddenVideoFolderRecords.addAll(appStateStore.loadHiddenVideoFolderRecords())
            unavailableVideoRecords.clear()
            unavailableVideoRecords.addAll(appStateStore.loadUnavailableVideoRecords())

            Toast.makeText(
                context,
                if (unavailableCount > 0) {
                    "部分视频已失效，已转入失效文件记录"
                } else {
                    "已恢复显示"
                },
                Toast.LENGTH_SHORT
            ).show()
            autoScanVideoAndAudio(force = true, showCompletionToast = false)
        }
    }

    fun clearVideoVisibilityRecords(records: List<VideoVisibilityRecordUiModel>) {
        if (records.isEmpty()) {
            Toast.makeText(context, "请先选择记录", Toast.LENGTH_SHORT).show()
            return
        }
        coroutineScope.launch {
            val folderIds = records
                .filter { it.type == VideoVisibilityRecordType.HIDDEN_FOLDER }
                .map { it.id.trim() }
                .filter { it.isNotBlank() }
                .toSet()
            val removedUris = records
                .filter { it.type == VideoVisibilityRecordType.REMOVED_VIDEO }
                .map { it.id.trim() }
                .filter { it.isNotBlank() }
                .toSet()
            val unavailableUris = records
                .filter { it.type == VideoVisibilityRecordType.UNAVAILABLE_FILE }
                .map { it.id.trim() }
                .filter { it.isNotBlank() }
                .toSet()

            if (folderIds.isNotEmpty()) {
                hiddenVideoFolderIds = hiddenVideoFolderIds - folderIds
                appStateStore.removeHiddenVideoFolderIds(folderIds)
            }
            if (removedUris.isNotEmpty()) {
                hiddenVideoUris = hiddenVideoUris - removedUris
                appStateStore.removeHiddenVideoUris(removedUris)
            }
            if (unavailableUris.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    VideoThumbnailStore.deleteForUris(context.applicationContext, unavailableUris)
                }
                appStateStore.removeUnavailableVideoUris(unavailableUris)
            }

            hiddenVideoRecords.clear()
            hiddenVideoRecords.addAll(appStateStore.loadHiddenVideoRecords())
            hiddenVideoFolderRecords.clear()
            hiddenVideoFolderRecords.addAll(appStateStore.loadHiddenVideoFolderRecords())
            unavailableVideoRecords.clear()
            unavailableVideoRecords.addAll(appStateStore.loadUnavailableVideoRecords())

            Toast.makeText(context, "已清除记录，不会删除真实文件", Toast.LENGTH_SHORT).show()
            if (folderIds.isNotEmpty() || removedUris.isNotEmpty()) {
                autoScanVideoAndAudio(force = true, showCompletionToast = false)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val miniAudioFile = musicCurrentUri?.let { uri ->
                audioQueue.firstOrNull { it.uri == uri }
                    ?: scannedFilesByFolder.values.flatten().firstOrNull { it.uri == uri }
            }
            ?: selectedMediaFile?.takeIf { it.type == LocalMediaType.AUDIO }
            ?: selectedAudioUri?.let { uri ->
                scannedFilesByFolder.values.flatten().firstOrNull { it.uri == uri }
            }
            ?: recentAudioUri?.let { uri ->
                scannedFilesByFolder.values.flatten().firstOrNull { it.uri == uri }
            }
            ?.takeIf { it.normalizedUri() !in hiddenAudioUris }
        fun openMiniAudio() {
            val file = miniAudioFile ?: return
            if (audioQueue.size <= 1 || audioQueue.none { it.uri == file.uri }) {
                val nextQueue = audioPlaybackQueueFor(file)
                audioQueue = nextQueue
                currentAudioIndex = nextQueue.indexOfFirst { it.uri == file.uri }
                    .takeIf { it >= 0 } ?: 0
            }
            selectedMediaFile = file
            selectedAudioUri = file.uri
            navController.navigate(LocalVibeRoute.AudioPlayer)
        }
        fun navigateToMainTab(route: String) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        LaunchedEffect(currentRoute, currentFolderTargetType) {
            currentRoute.toMainTabRoute(currentFolderTargetType)?.let { route ->
                appStateStore.saveLastMainTabRoute(route)
            }
        }
        val startRoute = initialMainRoute
        if (startRoute != null) NavHost(
            navController = navController,
            startDestination = startRoute
        ) {
            composable(LocalVibeRoute.VideoLibrary) {
                MainTabScaffold(navController = navController) { contentModifier ->
                    val videoFolderGroups = videoFolders.map { folder ->
                        VideoFolderUiModel(
                            folder = folder,
                            videos = scannedFilesByFolder[
                                typedFolderKey(LocalMediaType.VIDEO, folder.id)
                            ].orEmpty(),
                            source = folder.sourceLabel()
                        )
                    }
                    val videoVisibilityRecords = buildVideoVisibilityRecords(
                        hiddenFolderRecords = hiddenVideoFolderRecords,
                        removedVideoRecords = hiddenVideoRecords,
                        unavailableRecords = unavailableVideoRecords
                    )
                    VideoLibraryScreen(
                        videoFolders = videoFolderGroups,
                        isLoading = !isVideoVisibilityReady ||
                            !isVideoInitialScanComplete ||
                            isVideoAutoScanning,
                        permissionDeniedMessage = if (videoPermissionDenied) {
                            "没有媒体权限，无法自动扫描视频。你仍然可以手动添加文件夹。"
                        } else {
                            null
                        },
                        recentVideoFile = recentVideoFile ?: recentVideoUri?.let { uri ->
                            scannedFilesByFolder.values.flatten().firstOrNull { it.uri == uri }
                        },
                        onAddFolder = {
                            currentAddTargetType = LocalMediaType.VIDEO
                            openDocumentTreeLauncher.launch(null)
                        },
                        onOpenFolder = { item ->
                            currentFolder = item.folder
                            currentFolderTargetType = LocalMediaType.VIDEO
                            navController.navigate(LocalVibeRoute.Folder)
                        },
                        onContinueVideo = { file ->
                            openVideoIfReadable(file, listOf(file))
                        },
                        onRemoveFolders = { folders ->
                            coroutineScope.launch {
                                hideVideoFolders(folders.map { it.folder })
                            }
                        },
                        onDeleteFolderVideos = { files ->
                            requestFolderVideoPermanentDelete(files)
                        },
                        onRescanVideo = {
                            autoScanVideoAndAudio(force = true, showCompletionToast = true)
                        },
                        visibilityRecords = videoVisibilityRecords,
                        onRestoreVisibilityRecords = { records ->
                            restoreVideoVisibilityRecords(records)
                        },
                        onClearVisibilityRecords = { records ->
                            clearVideoVisibilityRecords(records)
                        },
                        onMore = {
                            Toast.makeText(context, "请进入文件夹后多选删除", Toast.LENGTH_SHORT).show()
                        },
                        onBackToDesktop = { returnToDesktopFromRoot() },
                        modifier = contentModifier
                    )
                }
            }
            composable(LocalVibeRoute.AudioLibrary) {
                MainTabScaffold(
                    navController = navController,
                    miniAudioFile = miniAudioFile,
                    miniIsPlaying = musicIsPlaying,
                    miniProgressMs = musicCurrentPositionMs,
                    miniDurationMs = musicDurationMs,
                    onMiniPrevious = {
                        if (!playAudioByMode(miniAudioFile, direction = -1)) {
                            Toast.makeText(context, "当前列表只有一首歌", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onMiniPlayPause = {
                        if (!playOrResumeAudio(miniAudioFile)) {
                            Toast.makeText(context, "暂无可播放音乐", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onMiniNext = {
                        val controller = musicController ?: return@MainTabScaffold
                        if (controller.currentMediaItem == null) {
                            val file = miniAudioFile ?: allVisibleAudioFiles().firstOrNull()
                            if (file == null) {
                                Toast.makeText(context, "暂无可播放音乐", Toast.LENGTH_SHORT).show()
                            } else {
                                playAudioQueue(audioPlaybackQueueFor(file), file)
                            }
                            return@MainTabScaffold
                        }
                        if (!playAudioByMode(miniAudioFile, direction = 1)) {
                            Toast.makeText(context, "当前列表只有一首歌", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onMiniSeekTo = { positionMs ->
                        musicController?.seekTo(positionMs)
                    },
                    onOpenMiniPlayer = { openMiniAudio() }
                ) { contentModifier ->
                    val audioFolderGroups = audioFolders.map { folder ->
                        val visibleFiles = scannedFilesByFolder[
                            typedFolderKey(LocalMediaType.AUDIO, folder.id)
                        ].orEmpty().filterVisibleAudios()
                        MediaFolderGroupUiModel(
                            folder = folder,
                            files = visibleFiles,
                            source = folder.sourceLabel()
                        )
                    }.filter { it.files.isNotEmpty() }
                    val audioFiles = audioFolderGroups
                        .flatMap { it.files }
                        .filterVisibleAudios()
                        .sortedBy { it.name.lowercase() }
                    val visibleFavoriteAudioUris = favoriteAudioUris.filter { uri ->
                        audioFiles.any { it.normalizedUri() == uri.trim() }
                    }.toSet()
                    val visibleRecentAudioUris = audioRecentRecords
                        .map { it.uri.trim() }
                        .filter { uri -> audioFiles.any { it.normalizedUri() == uri } }
                    AudioLibraryScreen(
                        audioFiles = audioFiles,
                        audioFolders = audioFolderGroups,
                        audioProgressMap = emptyMap(),
                        favoriteAudioUris = visibleFavoriteAudioUris,
                        recentAudioUris = visibleRecentAudioUris,
                        hasHiddenAudioSongs = hiddenAudioUris.isNotEmpty(),
                        hasHiddenAudioFolders = hiddenAudioFolderKeys.isNotEmpty(),
                        audioSortMode = audioSortMode,
                        audioSortAscending = audioSortAscending,
                        permissionDeniedMessage = if (audioPermissionDenied) {
                            "没有媒体权限，无法自动扫描音乐。你仍然可以手动添加文件夹。"
                        } else {
                            null
                        },
                        currentAudioUri = musicCurrentUri,
                        librarySection = audioLibrarySection,
                        selectedAudioFolderKey = selectedAudioFolderKey,
                        onLibrarySectionChange = { audioLibrarySection = it },
                        onSelectedAudioFolderKeyChange = { selectedAudioFolderKey = it },
                        onAddFolder = {
                            currentAddTargetType = LocalMediaType.AUDIO
                            openDocumentTreeLauncher.launch(null)
                        },
                        onOpenAudio = { file, queue ->
                            playAudioQueue(queue, file)
                        },
                        onToggleCurrentAudioPlayback = {
                            val controller = musicController
                            if (controller != null) {
                                if (controller.isPlaying) controller.pause() else controller.play()
                            }
                        },
                        onToggleFavoriteAudio = { file ->
                            toggleFavoriteAudio(file)
                        },
                        onRemoveFavoriteAudio = { file ->
                            removeFavoriteAudio(file)
                        },
                        onQueueScopeChanged = { queue ->
                            applyAudioQueueScope(queue)
                        },
                        onPlayAll = { queue ->
                            playAllAudioWithoutInterrupt(queue)
                        },
                        onShuffleAll = { queue ->
                            val shuffledQueue = queue.shuffled()
                            val firstFile = shuffledQueue.firstOrNull()
                            if (firstFile != null) {
                                playAudioQueue(shuffledQueue, firstFile, shuffle = true)
                            }
                        },
                        onRemoveAudio = { file ->
                            removeMediaFromList(file)
                        },
                        onDeleteAudio = { file ->
                            permanentlyDeleteMedia(file)
                        },
                        onRemoveAudios = { files ->
                            removeMediaFromList(files)
                        },
                        onDeleteAudios = { files ->
                            permanentlyDeleteMedia(files)
                        },
                        onRemoveAudioFolder = { item ->
                            coroutineScope.launch {
                                hideAudioFolder(item.folder)
                            }
                        },
                        onDeleteAudioFolder = { item ->
                            permanentlyDeleteMedia(item.files)
                        },
                        onRestoreHiddenAudioSongs = {
                            coroutineScope.launch {
                                restoreHiddenAudioSongs()
                            }
                        },
                        onRestoreHiddenAudioFolders = {
                            coroutineScope.launch {
                                restoreHiddenAudioFolders()
                            }
                        },
                        onAudioSortModeChange = { mode ->
                            audioSortMode = mode
                            val nextAscending = if (mode == AudioSortMode.Default) false else audioSortAscending
                            audioSortAscending = nextAscending
                            coroutineScope.launch {
                                appStateStore.saveAudioSortModeName(mode.name)
                                appStateStore.saveAudioSortAscending(nextAscending)
                            }
                        },
                        onToggleAudioSortDirection = {
                            val nextAscending = !audioSortAscending
                            audioSortAscending = nextAscending
                            coroutineScope.launch {
                                appStateStore.saveAudioSortAscending(nextAscending)
                            }
                        },
                        onRescanAudio = {
                            autoScanVideoAndAudio(force = true, showCompletionToast = true)
                        },
                        onBackToDesktop = { returnToDesktopFromRoot() },
                        modifier = contentModifier
                    )
                }
            }
            composable(LocalVibeRoute.BookLibrary) {
                MainTabScaffold(navController = navController) { contentModifier ->
                    val bookFiles = importedBookFiles
                        .distinctBy { it.normalizedBookKey() }
                        .sortedBy { it.name.lowercase() }
                    LaunchedEffect(bookFiles.size) {
                        Log.d(
                            BOOK_LOG_TAG,
                            "final bookFiles count=${bookFiles.size}, imported=${importedBookFiles.size}, folderIgnored=${folderBookFiles.size}, uris=${bookFiles.joinToString { it.uri }}"
                        )
                    }
                    BookLibraryScreen(
                        bookFiles = bookFiles,
                        bookProgressPercentMap = bookFiles.associate { file ->
                            file.uri to (bookProgressMap[file.uri]?.progressPercent() ?: 0)
                        },
                        onImportBookFile = {
                            openTextDocumentsLauncher.launch(
                                arrayOf("text/plain", "application/octet-stream")
                            )
                        },
                        onRescanBooks = {
                            Toast.makeText(context, "小说请通过导入本地文档添加", Toast.LENGTH_SHORT).show()
                        },
                        onRemoveBook = { file ->
                            removeMediaFromList(file)
                        },
                        onDeleteBook = { file ->
                            permanentlyDeleteMedia(file)
                        },
                        onRemoveBooks = { files ->
                            removeMediaFromList(files)
                        },
                        onDeleteBooks = { files ->
                            permanentlyDeleteMedia(files)
                        },
                        onOpenBook = { file ->
                            selectedMediaFile = file
                            selectedBookUri = file.uri
                            navController.navigate(LocalVibeRoute.BookListen)
                        },
                        modifier = contentModifier
                    )
                }
            }
            composable(LocalVibeRoute.BookListen) {
                val allBookFiles = importedBookFiles
                    .plus(folderBookFiles)
                    .distinctBy { it.normalizedBookKey() }
                val resolvedBookFile = selectedMediaFile
                    ?.takeIf { it.type == LocalMediaType.BOOK }
                    ?: selectedBookUri?.let { uri ->
                        allBookFiles.firstOrNull { it.uri == uri }
                    }
                BookListenScreen(
                    bookFile = resolvedBookFile,
                    initialParagraphIndex = resolvedBookFile?.let { file ->
                        bookProgressMap[file.uri]?.paragraphIndex ?: 0
                    } ?: 0,
                    onProgressChanged = { uri, paragraphIndex, totalParagraphs ->
                        val progress = PersistedBookProgress(
                            uri = uri,
                            paragraphIndex = paragraphIndex,
                            totalParagraphs = totalParagraphs,
                            updatedAt = System.currentTimeMillis()
                        )
                        bookProgressMap[uri] = progress
                        coroutineScope.launch {
                            appStateStore.saveBookProgress(progress)
                        }
                    },
                    onBeforeSpeak = {
                        val controller = musicController
                        if (controller?.isPlaying == true) {
                            controller.pause()
                            Toast.makeText(context, "已暂停音乐播放", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(LocalVibeRoute.Profile) {
                MainTabScaffold(navController = navController) { contentModifier ->
                    ProfileScreen(
                        onOpenSettings = { navController.navigate(LocalVibeRoute.Settings) },
                        modifier = contentModifier
                    )
                }
            }
            composable(LocalVibeRoute.Folder) {
                FolderScreen(
                    folderName = currentFolder?.name ?: "未命名文件夹",
                    files = currentFolder?.let { folder ->
                        scannedFilesByFolder[
                            typedFolderKey(currentFolderTargetType ?: LocalMediaType.VIDEO, folder.id)
                        ]
                    }.orEmpty(),
                    targetType = currentFolderTargetType,
                    videoProgressMap = videoProgressMap,
                    videoMetadataCache = videoMetadataCache,
                    onVideoMetadataLoaded = { mediaId, metadata ->
                        videoMetadataCache[mediaId] = metadata
                    },
                    audioProgressMap = emptyMap(),
                    recentVideoFile = recentVideoFile ?: recentVideoUri?.let { uri ->
                        scannedFilesByFolder.values.flatten().firstOrNull { it.uri == uri }
                    },
                    onOpenVideo = { file ->
                        val files = currentFolder?.let { folder ->
                            scannedFilesByFolder[
                                typedFolderKey(currentFolderTargetType ?: LocalMediaType.VIDEO, folder.id)
                            ]
                        }.orEmpty().filter { it.type == LocalMediaType.VIDEO }
                        openVideoIfReadable(file, files)
                    },
                    onContinueVideo = { file ->
                        openVideoIfReadable(file, listOf(file))
                    },
                    onOpenAudio = { file ->
                        val files = currentFolder?.let { folder ->
                            scannedFilesByFolder[
                                typedFolderKey(currentFolderTargetType ?: LocalMediaType.AUDIO, folder.id)
                            ]
                        }.orEmpty().filter { it.type == LocalMediaType.AUDIO }
                        playAudioQueue(files, file)
                        navController.navigate(LocalVibeRoute.AudioPlayer)
                    },
                    onRemoveFile = { file ->
                        removeMediaFromList(file)
                    },
                    onDeleteFile = { file ->
                        if (file.type == LocalMediaType.VIDEO) {
                            requestFolderVideoPermanentDelete(listOf(file))
                        } else {
                            permanentlyDeleteMedia(file)
                        }
                    },
                    onRemoveFiles = { files ->
                        removeMediaFromList(files)
                    },
                    onDeleteFiles = { files ->
                        if (currentFolderTargetType == LocalMediaType.VIDEO) {
                            requestFolderVideoPermanentDelete(files)
                        } else {
                            permanentlyDeleteMedia(files)
                        }
                    },
                    onUnavailableVideoDetected = { file ->
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                VideoThumbnailStore.delete(context.applicationContext, file)
                            }
                            videoMetadataCache.remove(file.id)
                            videoMetadataCache.remove(videoMetadataCacheKey(file))
                            appStateStore.upsertUnavailableVideoRecord(file)
                            unavailableVideoRecords.clear()
                            unavailableVideoRecords.addAll(appStateStore.loadUnavailableVideoRecords())
                        }
                    },
                    deleteSuccessSignal = folderVideoDeleteSuccessSignal,
                    onRescanFolder = { rescanCurrentVideoFolder() },
                    showBottomNavigation = currentFolderTargetType == LocalMediaType.VIDEO,
                    onNavigateToVideoHome = { navigateToMainTab(LocalVibeRoute.VideoLibrary) },
                    onNavigateToAudio = { navigateToMainTab(LocalVibeRoute.AudioLibrary) },
                    onNavigateToBooks = { navigateToMainTab(LocalVibeRoute.BookLibrary) },
                    onNavigateToProfile = { navigateToMainTab(LocalVibeRoute.Profile) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(LocalVibeRoute.VideoPlayer) {
                val allVideoFiles = scannedFilesByFolder.values
                    .flatten()
                    .filter { it.type == LocalMediaType.VIDEO }
                    .distinctBy { it.uri }
                val resolvedVideoFile = selectedMediaFile
                    ?.takeIf { it.type == LocalMediaType.VIDEO }
                    ?: selectedVideoUri?.let { uri ->
                        allVideoFiles.firstOrNull { it.uri == uri }
                    }
                    ?: recentVideoUri?.let { uri ->
                        allVideoFiles.firstOrNull { it.uri == uri }
                    }
                val resolvedVideoQueue = videoQueue
                    .takeIf { it.isNotEmpty() }
                    ?: resolvedVideoFile?.let { listOf(it) }
                    ?: emptyList()
                val resolvedVideoIndex = resolvedVideoQueue.indexOfFirst {
                    it.uri == resolvedVideoFile?.uri
                }.takeIf { it >= 0 } ?: currentVideoIndex
                VideoPlayerScreen(
                    mediaFile = resolvedVideoFile,
                    initialPositionMs = resolvedVideoFile?.let { videoProgressMap[it.uri] } ?: 0L,
                    queue = resolvedVideoQueue,
                    currentIndex = resolvedVideoIndex,
                    videoProgressByUri = videoProgressMap,
                    onRemoveUnavailableVideo = ::removeMediaFromList,
                    onSelectVideo = { index ->
                        val nextFile = resolvedVideoQueue.getOrNull(index) ?: return@VideoPlayerScreen
                        currentVideoIndex = index
                        selectedMediaFile = nextFile
                        selectedVideoUri = nextFile.uri
                        recentVideoFile = nextFile
                        recentVideoUri = nextFile.uri
                        coroutineScope.launch {
                            appStateStore.saveRecentVideoUri(nextFile.uri)
                        }
                    },
                    onProgressChanged = { mediaId, positionMs ->
                        if (positionMs > 0L) {
                            videoProgressMap[mediaId] = positionMs
                        } else {
                            videoProgressMap.remove(mediaId)
                        }
                        coroutineScope.launch {
                            appStateStore.saveProgress(
                                PersistedPlaybackProgress(
                                    mediaUri = mediaId,
                                    mediaType = LocalMediaType.VIDEO.name,
                                    positionMs = positionMs,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    },
                    folderPlaybackSpeeds = videoFolderPlaybackSpeeds,
                    onFolderPlaybackSpeedChanged = { folderKey, speed ->
                        val normalizedKey = folderKey.trim()
                        if (
                            normalizedKey.isNotBlank() &&
                            !speed.isNaN() &&
                            speed >= 0.25f &&
                            speed <= 5f
                        ) {
                            videoFolderPlaybackSpeeds[normalizedKey] = speed
                            coroutineScope.launch {
                                appStateStore.saveVideoFolderPlaybackSpeed(normalizedKey, speed)
                            }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(LocalVibeRoute.AudioPlayer) {
                val resolvedAudioFile = musicCurrentUri?.let { uri ->
                    audioQueue.firstOrNull { it.uri == uri }
                        ?: scannedFilesByFolder.values.flatten().firstOrNull { it.uri == uri }
                } ?: selectedMediaFile
                    ?.takeIf { it.type == LocalMediaType.AUDIO }
                    ?: selectedAudioUri?.let { uri -> audioQueue.firstOrNull { it.uri == uri } }
                Box(modifier = Modifier.fillMaxSize()) {
                    MainTabScaffold(
                        navController = navController,
                        miniAudioFile = miniAudioFile,
                        miniIsPlaying = musicIsPlaying,
                        miniProgressMs = musicCurrentPositionMs,
                        miniDurationMs = musicDurationMs,
                        onMiniPrevious = {
                            if (!playAudioByMode(miniAudioFile, direction = -1)) {
                                Toast.makeText(context, "当前列表只有一首歌", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onMiniPlayPause = {
                            if (!playOrResumeAudio(miniAudioFile)) {
                                Toast.makeText(context, "暂无可播放音乐", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onMiniNext = {
                            if (!playAudioByMode(miniAudioFile, direction = 1)) {
                                Toast.makeText(context, "当前列表只有一首歌", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onMiniSeekTo = { positionMs ->
                            musicController?.seekTo(positionMs)
                        },
                        onOpenMiniPlayer = { openMiniAudio() }
                    ) { contentModifier ->
                        val audioFolderGroups = audioFolders.map { folder ->
                            val visibleFiles = scannedFilesByFolder[
                                typedFolderKey(LocalMediaType.AUDIO, folder.id)
                            ].orEmpty().filterVisibleAudios()
                            MediaFolderGroupUiModel(
                                folder = folder,
                                files = visibleFiles,
                                source = folder.sourceLabel()
                            )
                        }.filter { it.files.isNotEmpty() }
                        val audioFiles = audioFolderGroups
                            .flatMap { it.files }
                            .filterVisibleAudios()
                            .sortedBy { it.name.lowercase() }
                        val visibleFavoriteAudioUris = favoriteAudioUris.filter { uri ->
                            audioFiles.any { it.normalizedUri() == uri.trim() }
                        }.toSet()
                        val visibleRecentAudioUris = audioRecentRecords
                            .map { it.uri.trim() }
                            .filter { uri -> audioFiles.any { it.normalizedUri() == uri } }
                        AudioLibraryScreen(
                            audioFiles = audioFiles,
                            audioFolders = audioFolderGroups,
                            audioProgressMap = emptyMap(),
                            favoriteAudioUris = visibleFavoriteAudioUris,
                            recentAudioUris = visibleRecentAudioUris,
                            hasHiddenAudioSongs = hiddenAudioUris.isNotEmpty(),
                            hasHiddenAudioFolders = hiddenAudioFolderKeys.isNotEmpty(),
                            audioSortMode = audioSortMode,
                            audioSortAscending = audioSortAscending,
                            permissionDeniedMessage = if (audioPermissionDenied) {
                                "没有媒体权限，无法自动扫描音乐。你仍然可以手动添加文件夹。"
                            } else {
                                null
                            },
                            currentAudioUri = musicCurrentUri,
                            librarySection = audioLibrarySection,
                            selectedAudioFolderKey = selectedAudioFolderKey,
                            onLibrarySectionChange = { audioLibrarySection = it },
                            onSelectedAudioFolderKeyChange = { selectedAudioFolderKey = it },
                            onAddFolder = {
                                currentAddTargetType = LocalMediaType.AUDIO
                                openDocumentTreeLauncher.launch(null)
                            },
                            onOpenAudio = { file, queue ->
                                playAudioQueue(queue, file)
                            },
                            onToggleCurrentAudioPlayback = {
                                val currentFile = fallbackCurrentAudioFile()
                                if (!playOrResumeAudio(currentFile)) {
                                    Toast.makeText(context, "暂无可播放音乐", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onToggleFavoriteAudio = { file ->
                                toggleFavoriteAudio(file)
                            },
                            onRemoveFavoriteAudio = { file ->
                                removeFavoriteAudio(file)
                            },
                            onQueueScopeChanged = { queue ->
                                applyAudioQueueScope(queue)
                            },
                            onPlayAll = { queue ->
                                playAllAudioWithoutInterrupt(queue)
                            },
                            onShuffleAll = { queue ->
                                val shuffledQueue = queue.shuffled()
                                val firstFile = shuffledQueue.firstOrNull()
                                if (firstFile != null) {
                                    playAudioQueue(shuffledQueue, firstFile, shuffle = true)
                                }
                            },
                            onRemoveAudio = { file ->
                                removeMediaFromList(file)
                            },
                            onDeleteAudio = { file ->
                                permanentlyDeleteMedia(file)
                            },
                            onRemoveAudios = { files ->
                                removeMediaFromList(files)
                            },
                            onDeleteAudios = { files ->
                                permanentlyDeleteMedia(files)
                            },
                            onRemoveAudioFolder = { item ->
                                coroutineScope.launch {
                                    hideAudioFolder(item.folder)
                                }
                            },
                            onDeleteAudioFolder = { item ->
                                permanentlyDeleteMedia(item.files)
                            },
                            onRestoreHiddenAudioSongs = {
                                coroutineScope.launch {
                                    restoreHiddenAudioSongs()
                                }
                            },
                            onRestoreHiddenAudioFolders = {
                                coroutineScope.launch {
                                    restoreHiddenAudioFolders()
                                }
                            },
                            onAudioSortModeChange = { mode ->
                                audioSortMode = mode
                                val nextAscending = if (mode == AudioSortMode.Default) false else audioSortAscending
                                audioSortAscending = nextAscending
                                coroutineScope.launch {
                                    appStateStore.saveAudioSortModeName(mode.name)
                                    appStateStore.saveAudioSortAscending(nextAscending)
                                }
                            },
                            onToggleAudioSortDirection = {
                                val nextAscending = !audioSortAscending
                                audioSortAscending = nextAscending
                                coroutineScope.launch {
                                    appStateStore.saveAudioSortAscending(nextAscending)
                                }
                            },
                            onRescanAudio = {
                                autoScanVideoAndAudio(force = true, showCompletionToast = true)
                            },
                            onBackToDesktop = { returnToDesktopFromRoot() },
                            modifier = contentModifier
                        )
                    }
                    val resolvedAudioQueue = audioPlaybackQueueFor(resolvedAudioFile)
                    val resolvedAudioIndex = resolvedAudioFile?.let { file ->
                        resolvedAudioQueue.indexOfFirst {
                            it.uri == file.uri || it.normalizedUri() == file.normalizedUri()
                        }.takeIf { it >= 0 }
                    } ?: currentAudioIndex.coerceIn(
                        0,
                        resolvedAudioQueue.lastIndex.coerceAtLeast(0)
                    )
                    AudioPlayerScreen(
                        mediaFile = resolvedAudioFile,
                        player = musicController,
                        currentPositionMs = musicCurrentPositionMs,
                        durationMs = musicDurationMs,
                        isPlaying = musicIsPlaying,
                        isFavorite = resolvedAudioFile?.normalizedUri() in favoriteAudioUris,
                        audioSessionId = MusicPlaybackService.currentAudioSessionId,
                        queue = resolvedAudioQueue,
                        currentIndex = resolvedAudioIndex,
                        playMode = audioPlayMode,
                        onPlayModeChanged = { mode ->
                            audioPlayMode = mode
                            coroutineScope.launch {
                                appStateStore.saveAudioPlayModeName(mode.name)
                            }
                            audioShuffleHistory = emptyList()
                            applyAudioPlayMode(musicController, mode)
                        },
                        onSelectAudio = { index ->
                            val nextFile = resolvedAudioQueue.getOrNull(index)
                                ?: return@AudioPlayerScreen
                            playAudioQueue(resolvedAudioQueue, nextFile)
                        },
                        onPlayPause = {
                            if (!playOrResumeAudio(resolvedAudioFile)) {
                                Toast.makeText(context, "暂无可播放音乐", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onPrevious = {
                            if (!playAudioByMode(resolvedAudioFile, direction = -1)) {
                                Toast.makeText(context, "当前列表只有一首歌", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onNext = {
                            if (!playAudioByMode(resolvedAudioFile, direction = 1)) {
                                Toast.makeText(context, "当前列表只有一首歌", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onSeekTo = { positionMs ->
                            musicController?.seekTo(positionMs)
                        },
                        onToggleFavorite = {
                            val uri = resolvedAudioFile?.normalizedUri().orEmpty()
                            val willFavorite = uri.isNotBlank() && uri !in favoriteAudioUris
                            toggleFavoriteAudio(resolvedAudioFile)
                            if (uri.isNotBlank()) {
                                Toast.makeText(
                                    context,
                                    if (willFavorite) "已加入我喜欢" else "已取消喜欢",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onBack = { navController.popBackStack() },
                        onRemoveCurrent = { file -> removeMediaFromList(file) },
                        onDeleteCurrent = { file -> permanentlyDeleteMedia(file) }
                    )
                }
            }
            composable(LocalVibeRoute.Settings) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onClearProgress = {
                        coroutineScope.launch {
                            appStateStore.clearProgress()
                            videoProgressMap.clear()
                            audioProgressMap.clear()
                            recentVideoFile = null
                            recentVideoUri = null
                            recentAudioUri = null
                            Toast.makeText(context, "播放进度已清除", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onClearFolders = {
                        coroutineScope.launch {
                            appStateStore.clearFolders()
                            appStateStore.clearPersistedBookFiles()
                            appStateStore.clearBookProgress()
                            videoFolders.removeManualFoldersAndScans(LocalMediaType.VIDEO, scannedFilesByFolder)
                            audioFolders.removeManualFoldersAndScans(LocalMediaType.AUDIO, scannedFilesByFolder)
                            bookFolders.removeManualFoldersAndScans(LocalMediaType.BOOK, scannedFilesByFolder)
                            importedBookFiles.clear()
                            bookProgressMap.clear()
                            clearFolderBookFiles()
                            Toast.makeText(context, "已添加文件夹记录已清除", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onClearBooks = {
                        coroutineScope.launch {
                            val remainingFolders = appStateStore.loadFolders()
                                .filterNot { it.targetType == LocalMediaType.BOOK.name }
                            appStateStore.clearFolders()
                            remainingFolders.forEach { appStateStore.upsertFolder(it) }
                            appStateStore.clearPersistedBookFiles()
                            appStateStore.clearBookProgress()
                            bookFolders.removeManualFoldersAndScans(LocalMediaType.BOOK, scannedFilesByFolder)
                            importedBookFiles.clear()
                            bookProgressMap.clear()
                            clearFolderBookFiles()
                            Toast.makeText(context, "小说导入记录已清除", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRescanMedia = {
                        Toast.makeText(context, "正在重新扫描媒体", Toast.LENGTH_SHORT).show()
                        autoScanVideoAndAudio(force = true, showCompletionToast = true)
                    },
                    onRestoreHiddenAudio = {
                        coroutineScope.launch {
                            appStateStore.clearHiddenAudioUris()
                            hiddenAudioUris = emptySet()
                            Toast.makeText(context, "已恢复隐藏音乐，请稍后查看", Toast.LENGTH_SHORT).show()
                            autoScanVideoAndAudio(force = true, showCompletionToast = true)
                        }
                    },
                    onRestoreHiddenVideo = {
                        coroutineScope.launch {
                            if (hiddenVideoUris.isEmpty() && hiddenVideoFolderIds.isEmpty()) {
                                Toast.makeText(context, "暂无隐藏视频", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            appStateStore.clearHiddenVideoUris()
                            appStateStore.clearHiddenVideoFolderIds()
                            hiddenVideoUris = emptySet()
                            hiddenVideoFolderIds = emptySet()
                            hiddenVideoRecords.clear()
                            hiddenVideoFolderRecords.clear()
                            Toast.makeText(context, "已恢复隐藏视频", Toast.LENGTH_SHORT).show()
                            autoScanVideoAndAudio(force = true)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MainTabScaffold(
    navController: NavController,
    miniAudioFile: LocalMediaFile? = null,
    miniIsPlaying: Boolean = false,
    miniProgressMs: Long = 0L,
    miniDurationMs: Long = 0L,
    onMiniPrevious: () -> Unit = {},
    onMiniPlayPause: () -> Unit = {},
    onMiniNext: () -> Unit = {},
    onMiniSeekTo: (Long) -> Unit = {},
    onOpenMiniPlayer: () -> Unit = {},
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        bottomBar = {
            MainBottomBar(navController = navController)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            content(Modifier)
            if (miniAudioFile != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Transparent)
                ) {
                    MiniAudioPlayerBar(
                        file = miniAudioFile,
                        isPlaying = miniIsPlaying,
                        progressMs = miniProgressMs,
                        durationMs = miniDurationMs,
                        onPrevious = onMiniPrevious,
                        onPlayPause = onMiniPlayPause,
                        onNext = onMiniNext,
                        onSeekTo = onMiniSeekTo,
                        onOpen = onOpenMiniPlayer
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniAudioPlayerBar(
    file: LocalMediaFile,
    isPlaying: Boolean,
    progressMs: Long,
    durationMs: Long,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onOpen: () -> Unit
) {
    val safeDurationMs = durationMs.coerceAtLeast(0L)
    var pendingSeekMs by remember { mutableLongStateOf(progressMs.coerceIn(0L, safeDurationMs)) }
    LaunchedEffect(progressMs, safeDurationMs) {
        pendingSeekMs = progressMs.coerceIn(0L, safeDurationMs)
    }

    fun seekPositionFromX(x: Float, width: Float): Long {
        if (safeDurationMs <= 0L) return 0L
        val fraction = (x / width.coerceAtLeast(1f)).coerceIn(0f, 1f)
        return (safeDurationMs * fraction).toLong().coerceIn(0L, safeDurationMs)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .background(Color.Transparent)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = Color(0xFF8B5CFF).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xEA141528)
            )
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 5.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpen),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        RotatingMusicThumb(
                            artworkUri = null,
                            isRotating = true,
                            modifier = Modifier.size(48.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.name.substringBeforeLast('.', file.name),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.92f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = file.parentFolderName ?: "本地音乐",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFAAA3BF),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = onPrevious, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Filled.SkipPrevious,
                            contentDescription = "上一首",
                            tint = Color.White.copy(alpha = 0.82f)
                        )
                    }
                    IconButton(onClick = onPlayPause, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            tint = Color.White.copy(alpha = 0.92f)
                        )
                    }
                    IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Filled.SkipNext,
                            contentDescription = "下一首",
                            tint = Color.White.copy(alpha = 0.86f)
                        )
                    }
                }
                if (durationMs > 0L) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .height(18.dp)
                            .pointerInput(safeDurationMs) {
                                detectTapGestures { offset ->
                                    val target = seekPositionFromX(offset.x, size.width.toFloat())
                                    pendingSeekMs = target
                                    onSeekTo(target)
                                }
                            }
                            .pointerInput(safeDurationMs) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        pendingSeekMs = seekPositionFromX(offset.x, size.width.toFloat())
                                    },
                                    onDragEnd = {
                                        onSeekTo(pendingSeekMs)
                                    },
                                    onDragCancel = {
                                        pendingSeekMs = progressMs.coerceIn(0L, safeDurationMs)
                                    }
                                ) { change, _ ->
                                    pendingSeekMs = seekPositionFromX(
                                        change.position.x,
                                        size.width.toFloat()
                                    )
                                    change.consume()
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(2.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFFB7A8D8).copy(alpha = 0.14f))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth(
                                    (pendingSeekMs.toFloat() / safeDurationMs.toFloat())
                                        .coerceIn(0f, 1f)
                                )
                                .height(2.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFF8B5CFF))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MainBottomBar(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val selectedRoute = if (currentRoute == LocalVibeRoute.AudioPlayer) {
        LocalVibeRoute.AudioLibrary
    } else {
        currentRoute
    }

    MoonBottomNavigationBar(
        selectedRoute = selectedRoute,
        onRouteSelected = { route ->
            if (currentRoute != route) {
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    )
}

private enum class FolderVideoDeleteMode {
    SystemDelete,
    RetryAfterGrant
}

private const val AUTO_SCAN_THROTTLE_MS = 10_000L
private const val BOOK_LOG_TAG = "LocalVibeBooks"
private const val VIDEO_LOG_TAG = "LocalVibeVideo"

private fun mediaScanPermissions(): List<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

private fun hasPermission(
    context: android.content.Context,
    permission: String
): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        permission
    ) == PackageManager.PERMISSION_GRANTED
}

private fun deleteContentUriDirect(
    context: android.content.Context,
    uriString: String
): Boolean {
    return context.contentResolver.delete(Uri.parse(uriString), null, null) > 0
}

private fun typedFolderKey(
    type: LocalMediaType,
    folderId: String
): String = "${type.name}:$folderId"

private fun LocalMediaFile.normalizedBookKey(): String {
    val normalizedUri = uri.trim()
    return normalizedUri.ifBlank { "${name.trim().lowercase()}:$size" }
}

private fun buildVideoVisibilityRecords(
    hiddenFolderRecords: List<PersistedVideoVisibilityRecord>,
    removedVideoRecords: List<PersistedVideoVisibilityRecord>,
    unavailableRecords: List<PersistedVideoVisibilityRecord>
): List<VideoVisibilityRecordUiModel> {
    return buildList {
        hiddenFolderRecords.forEach { record ->
            add(record.toVideoVisibilityRecordUiModel(VideoVisibilityRecordType.HIDDEN_FOLDER))
        }
        removedVideoRecords.forEach { record ->
            add(record.toVideoVisibilityRecordUiModel(VideoVisibilityRecordType.REMOVED_VIDEO))
        }
        unavailableRecords.forEach { record ->
            add(record.toVideoVisibilityRecordUiModel(VideoVisibilityRecordType.UNAVAILABLE_FILE))
        }
    }.sortedByDescending { it.recordedAt }
}

private fun PersistedVideoVisibilityRecord.toVideoVisibilityRecordUiModel(
    type: VideoVisibilityRecordType
): VideoVisibilityRecordUiModel {
    return VideoVisibilityRecordUiModel(
        id = id.ifBlank { uri },
        type = type,
        name = name.ifBlank { id.ifBlank { uri } },
        path = path?.takeIf { it.isNotBlank() } ?: uri.takeIf { it.isNotBlank() },
        recordedAt = recordedAt
    )
}

private fun VideoVisibilityRecordUiModel.toLocalVideoFile(): LocalMediaFile {
    val uriValue = id.ifBlank { path.orEmpty() }
    val safeName = name.ifBlank { uriValue }
    return LocalMediaFile(
        id = uriValue,
        name = safeName,
        uri = uriValue,
        type = LocalMediaType.VIDEO,
        extension = safeName.substringAfterLast('.', "").lowercase(),
        size = 0L,
        parentFolderName = path
    )
}

private fun LocalMediaFile.normalizedUri(): String = uri.trim()

private fun String?.toAudioPlayMode(): AudioPlayMode {
    val value = this?.trim().orEmpty()
    return AudioPlayMode.values().firstOrNull { it.name == value } ?: AudioPlayMode.NORMAL
}

private fun String?.toAudioSortMode(): AudioSortMode {
    val value = this?.trim().orEmpty()
    return AudioSortMode.values().firstOrNull { it.name == value } ?: AudioSortMode.Default
}

private fun PersistedBookProgress.progressPercent(): Int {
    if (totalParagraphs <= 0) return 0
    return (((paragraphIndex.coerceAtLeast(0) + 1) * 100f) / totalParagraphs)
        .toInt()
        .coerceIn(0, 100)
}

private fun LocalMediaFile.toAudioMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setUri(uri)
        .setMediaId(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(name.substringBeforeLast('.', name))
                .setArtist("Unknown")
                .build()
        )
        .build()
}

private fun formatMiniProgress(positionMs: Long, durationMs: Long): String {
    if (durationMs <= 0L) return "--:--"
    val percent = (positionMs.coerceAtLeast(0L) * 100 / durationMs).coerceIn(0L, 100L)
    return "$percent%"
}

private fun LocalMediaType.mediaReadPermission(): String? {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && this == LocalMediaType.VIDEO ->
            Manifest.permission.READ_MEDIA_VIDEO

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && this == LocalMediaType.AUDIO ->
            Manifest.permission.READ_MEDIA_AUDIO

        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
            (this == LocalMediaType.VIDEO || this == LocalMediaType.AUDIO) ->
            Manifest.permission.READ_EXTERNAL_STORAGE

        else -> null
    }
}

private fun LocalMediaType.emptyFolderMessage(): String {
    return when (this) {
        LocalMediaType.VIDEO -> "该文件夹中没有视频文件"
        LocalMediaType.AUDIO -> "该文件夹中没有音乐文件"
        LocalMediaType.BOOK -> "该文件夹中没有 TXT 小说文件"
        else -> "该文件夹中没有可用文件"
    }
}

private fun String.toLocalMediaTypeOrNull(): LocalMediaType? {
    return runCatching { LocalMediaType.valueOf(this) }.getOrNull()
}

private fun String?.toRestorableMainRoute(): String {
    return when (this) {
        LocalVibeRoute.AudioLibrary -> LocalVibeRoute.AudioLibrary
        LocalVibeRoute.BookLibrary -> LocalVibeRoute.BookLibrary
        LocalVibeRoute.VideoLibrary -> LocalVibeRoute.VideoLibrary
        else -> LocalVibeRoute.VideoLibrary
    }
}

private fun String?.toMainTabRoute(folderTargetType: LocalMediaType?): String? {
    return when (this) {
        LocalVibeRoute.AudioLibrary,
        LocalVibeRoute.AudioPlayer -> LocalVibeRoute.AudioLibrary
        LocalVibeRoute.VideoLibrary,
        LocalVibeRoute.VideoPlayer -> LocalVibeRoute.VideoLibrary
        LocalVibeRoute.BookLibrary,
        LocalVibeRoute.BookListen -> LocalVibeRoute.BookLibrary
        LocalVibeRoute.Folder -> when (folderTargetType) {
            LocalMediaType.AUDIO -> LocalVibeRoute.AudioLibrary
            LocalMediaType.BOOK -> LocalVibeRoute.BookLibrary
            LocalMediaType.VIDEO -> LocalVibeRoute.VideoLibrary
            else -> null
        }
        else -> null
    }
}

private fun Uri.toLocalBookFile(context: android.content.Context): LocalMediaFile? {
    val documentFile = DocumentFile.fromSingleUri(context, this) ?: return null
    val name = documentFile.name ?: "未命名小说.txt"
    val extension = name.substringAfterLast('.', "")
    return LocalMediaFile(
        id = toString(),
        name = name,
        uri = toString(),
        type = LocalMediaType.BOOK,
        extension = extension,
        size = documentFile.length(),
        parentFolderName = null
    )
}

private fun MediaFolderUiModel.sourceLabel(): String {
    return if (id.startsWith("auto:")) "自动扫描" else "手动添加"
}

private fun MutableList<MediaFolderUiModel>.removeManualFoldersAndScans(
    type: LocalMediaType,
    scannedFilesByFolder: MutableMap<String, List<LocalMediaFile>>
) {
    val manualFolderIds = filterNot { it.id.startsWith("auto:") }
        .map { it.id }
    removeAll { !it.id.startsWith("auto:") }
    manualFolderIds.forEach { folderId ->
        scannedFilesByFolder.remove(typedFolderKey(type, folderId))
    }
}

private fun MutableList<MediaFolderUiModel>.removeAutoFoldersAndScans(
    type: LocalMediaType,
    scannedFilesByFolder: MutableMap<String, List<LocalMediaFile>>
) {
    val autoFolderIds = filter { it.id.startsWith("auto:") }
        .map { it.id }
    removeAll { it.id.startsWith("auto:") }
    autoFolderIds.forEach { folderId ->
        scannedFilesByFolder.remove(typedFolderKey(type, folderId))
    }
}

private fun MutableList<MediaFolderUiModel>.upsertFolder(
    folderId: String,
    createOrUpdate: () -> MediaFolderUiModel
) {
    val index = indexOfFirst { it.id == folderId }
    val folder = createOrUpdate()
    if (index >= 0) {
        this[index] = folder
    } else {
        add(folder)
    }
}

private fun MutableList<MediaFolderUiModel>.updateFolder(
    folderId: String,
    update: MediaFolderUiModel.() -> MediaFolderUiModel
) {
    val index = indexOfFirst { it.id == folderId }
    if (index >= 0) {
        this[index] = this[index].update()
    }
}

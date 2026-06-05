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
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
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
import com.shenghui.localvibe.core.datastore.PersistedBookFile
import com.shenghui.localvibe.core.datastore.PersistedBookProgress
import com.shenghui.localvibe.core.datastore.PersistedFolder
import com.shenghui.localvibe.core.datastore.PersistedPlaybackProgress
import com.shenghui.localvibe.core.media.deleteUri
import com.shenghui.localvibe.core.media.resolveDocumentTreeName
import com.shenghui.localvibe.core.media.VideoMetadata
import com.shenghui.localvibe.core.player.AudioPlayMode
import com.shenghui.localvibe.core.player.MusicPlaybackService
import com.shenghui.localvibe.core.scanner.FolderScanner
import com.shenghui.localvibe.core.scanner.LocalMediaFile
import com.shenghui.localvibe.core.scanner.LocalMediaType
import com.shenghui.localvibe.core.scanner.MediaStoreScanner
import com.shenghui.localvibe.core.ui.theme.LocalVibeTheme
import com.shenghui.localvibe.feature.audio.AudioPlayerScreen
import com.shenghui.localvibe.feature.audio.AudioLibraryScreen
import com.shenghui.localvibe.feature.book.BookListenScreen
import com.shenghui.localvibe.feature.book.BookLibraryScreen
import com.shenghui.localvibe.feature.folder.FolderScreen
import com.shenghui.localvibe.feature.home.model.MediaFolderUiModel
import com.shenghui.localvibe.feature.profile.ProfileScreen
import com.shenghui.localvibe.feature.settings.SettingsScreen
import com.shenghui.localvibe.feature.video.VideoLibraryScreen
import com.shenghui.localvibe.feature.video.VideoPlayerScreen
import com.shenghui.localvibe.feature.video.model.VideoFolderUiModel
import com.shenghui.localvibe.feature.library.MediaFolderGroupUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val audioProgressMap = remember { mutableStateMapOf<String, Long>() }
    val bookProgressMap = remember { mutableStateMapOf<String, PersistedBookProgress>() }
    var hiddenAudioUris by remember { mutableStateOf(emptySet<String>()) }
    var hiddenVideoUris by remember { mutableStateOf(emptySet<String>()) }
    var hiddenVideoFolderIds by remember { mutableStateOf(emptySet<String>()) }
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
    var videoQueue by remember { mutableStateOf<List<LocalMediaFile>>(emptyList()) }
    var currentVideoIndex by remember { mutableStateOf(0) }
    var selectedVideoUri by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAudioUri by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedBookUri by rememberSaveable { mutableStateOf<String?>(null) }
    var recentVideoFile by remember { mutableStateOf<LocalMediaFile?>(null) }
    var recentVideoUri by remember { mutableStateOf<String?>(null) }
    var recentAudioUri by remember { mutableStateOf<String?>(null) }
    var musicController by remember { mutableStateOf<MediaController?>(null) }
    var musicCurrentUri by remember { mutableStateOf<String?>(null) }
    var musicCurrentPositionMs by remember { mutableStateOf(0L) }
    var musicDurationMs by remember { mutableStateOf(0L) }
    var musicIsPlaying by remember { mutableStateOf(false) }
    var pendingFolderVideoDeleteFiles by remember { mutableStateOf<List<LocalMediaFile>>(emptyList()) }
    var pendingFolderVideoDeleteMode by remember { mutableStateOf<FolderVideoDeleteMode?>(null) }
    var folderVideoDeleteSuccessSignal by remember { mutableStateOf(0L) }
    lateinit var folderVideoDeleteLauncher:
        ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>
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

    fun applyAudioPlayMode(controller: Player?, mode: AudioPlayMode) {
        controller ?: return
        controller.shuffleModeEnabled = mode == AudioPlayMode.SHUFFLE
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
        return mapNotNull { result ->
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
        appStateStore.hideVideoUris(uris)
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
        if (shuffle) {
            audioPlayMode = AudioPlayMode.SHUFFLE
        }
        applyAudioPlayMode(controller, audioPlayMode)
        controller.setMediaItems(
            nextQueue.map { it.toAudioMediaItem() },
            startIndex,
            0L
        )
        controller.prepare()
        controller.play()
        coroutineScope.launch {
            appStateStore.saveRecentAudioUri(recentAudioUri)
        }
    }

    fun refreshMusicControllerAfterAudioRemoval(removedUris: Set<String>) {
        if (removedUris.isEmpty()) return
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
            coroutineScope.launch {
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
                } else {
                    "已从当前列表移除，重新扫描后可能恢复"
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
            Toast.makeText(context, "已移除 ${uniqueFiles.size} 项", Toast.LENGTH_SHORT).show()
        }
    }

    fun completeFolderVideoDelete(deletedFiles: List<LocalMediaFile>, requestedCount: Int) {
        coroutineScope.launch {
            val uniqueDeletedFiles = deletedFiles
                .filter { it.type == LocalMediaType.VIDEO }
                .distinctBy { it.normalizedUri() }
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
                Toast.makeText(context, "系统不允许直接删除该文件，已为你从列表移除。", Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (deleted) {
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
                Toast.makeText(context, "系统不允许直接删除 ${failedAudioFiles.size} 首音乐，已为你从列表移除。", Toast.LENGTH_SHORT).show()
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
            appStateStore.hideVideoFolderIds(autoFolderIds)
        }
        if (manualFolderIds.isNotEmpty()) {
            appStateStore.removeFolders(LocalMediaType.VIDEO.name, manualFolderIds)
        }
        videoFolders.removeAll { it.id in folderIds }
        folderIds.forEach { folderId ->
            scannedFilesByFolder.remove(typedFolderKey(LocalMediaType.VIDEO, folderId))
        }
    }

    fun runAutoScan(targetType: LocalMediaType, showCompletionToast: Boolean = false) {
        coroutineScope.launch {
            when (targetType) {
                LocalMediaType.VIDEO -> {
                    val folders = withContext(Dispatchers.IO) {
                        MediaStoreScanner.scanVideos(context.applicationContext)
                    }.filterHiddenVideoFolders()
                    videoFolders.removeAutoFoldersAndScans(LocalMediaType.VIDEO, scannedFilesByFolder)
                    folders.forEach { result ->
                        videoFolders.add(result.folder)
                        scannedFilesByFolder[typedFolderKey(LocalMediaType.VIDEO, result.folder.id)] =
                            result.files
                    }
                    if (showCompletionToast && folders.isEmpty()) {
                        Toast.makeText(context, "没有扫描到视频文件", Toast.LENGTH_SHORT).show()
                    }
                    if (showCompletionToast) {
                        Toast.makeText(context, "媒体扫描已完成", Toast.LENGTH_SHORT).show()
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

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val canScanVideo = LocalMediaType.VIDEO.mediaReadPermission()
            ?.let { grants[it] == true || hasPermission(context, it) } ?: true
        val canScanAudio = LocalMediaType.AUDIO.mediaReadPermission()
            ?.let { grants[it] == true || hasPermission(context, it) } ?: true
        videoPermissionDenied = !canScanVideo
        audioPermissionDenied = !canScanAudio
        if (canScanVideo) runAutoScan(LocalMediaType.VIDEO)
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
        recentAudioUri = appStateStore.loadRecentAudioUri()
        hiddenAudioUris = appStateStore.loadHiddenAudioUris()
        hiddenVideoUris = appStateStore.loadHiddenVideoUris()
        hiddenVideoFolderIds = appStateStore.loadHiddenVideoFolderIds()
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
                    recentAudioUri = uri
                    selectedMediaFile = audioQueue.firstOrNull { it.uri == uri } ?: selectedMediaFile
                    coroutineScope.launch {
                        appStateStore.saveRecentAudioUri(uri)
                    }
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
            if (audioQueue.none { it.uri == file.uri }) {
                audioQueue = listOf(file)
                currentAudioIndex = 0
            }
            selectedMediaFile = file
            selectedAudioUri = file.uri
            navController.navigate(LocalVibeRoute.AudioPlayer)
        }
        NavHost(
            navController = navController,
            startDestination = LocalVibeRoute.VideoLibrary
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
                    VideoLibraryScreen(
                        videoFolders = videoFolderGroups,
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
                            selectedMediaFile = file
                            selectedVideoUri = file.uri
                            videoQueue = listOf(file)
                            currentVideoIndex = 0
                            recentVideoFile = file
                            recentVideoUri = file.uri
                            coroutineScope.launch {
                                appStateStore.saveRecentVideoUri(file.uri)
                            }
                            navController.navigate(LocalVibeRoute.VideoPlayer)
                        },
                        onRemoveFolders = { folders ->
                            coroutineScope.launch {
                                hideVideoFolders(folders.map { it.folder })
                            }
                        },
                        onRescanVideo = {
                            autoScanVideoAndAudio(force = true, showCompletionToast = true)
                        },
                        onMore = {
                            Toast.makeText(context, "请进入文件夹后多选删除", Toast.LENGTH_SHORT).show()
                        },
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
                    onMiniPlayPause = {
                        val controller = musicController ?: return@MainTabScaffold
                        if (controller.isPlaying) controller.pause() else controller.play()
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
                    AudioLibraryScreen(
                        audioFiles = audioFiles,
                        audioFolders = audioFolderGroups,
                        audioProgressMap = emptyMap(),
                        permissionDeniedMessage = if (audioPermissionDenied) {
                            "没有媒体权限，无法自动扫描音乐。你仍然可以手动添加文件夹。"
                        } else {
                            null
                        },
                        onAddFolder = {
                            currentAddTargetType = LocalMediaType.AUDIO
                            openDocumentTreeLauncher.launch(null)
                        },
                        onOpenFolder = { item ->
                            currentFolder = item.folder
                            currentFolderTargetType = LocalMediaType.AUDIO
                            navController.navigate(LocalVibeRoute.Folder)
                        },
                        onOpenAudio = { file, queue ->
                            playAudioQueue(queue, file)
                            navController.navigate(LocalVibeRoute.AudioPlayer)
                        },
                        onShuffleAll = { queue ->
                            val shuffledQueue = queue.shuffled()
                            val firstFile = shuffledQueue.firstOrNull()
                            if (firstFile != null) {
                                playAudioQueue(shuffledQueue, firstFile, shuffle = true)
                                navController.navigate(LocalVibeRoute.AudioPlayer)
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
                        onRescanAudio = {
                            autoScanVideoAndAudio(force = true, showCompletionToast = true)
                        },
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
                    onOpenVideo = { file ->
                        val files = currentFolder?.let { folder ->
                            scannedFilesByFolder[
                                typedFolderKey(currentFolderTargetType ?: LocalMediaType.VIDEO, folder.id)
                            ]
                        }.orEmpty().filter { it.type == LocalMediaType.VIDEO }
                        videoQueue = files.ifEmpty { listOf(file) }
                        currentVideoIndex = videoQueue.indexOfFirst { it.uri == file.uri }
                            .takeIf { it >= 0 } ?: 0
                        selectedMediaFile = file
                        selectedVideoUri = file.uri
                        recentVideoFile = file
                        recentVideoUri = file.uri
                        coroutineScope.launch {
                            appStateStore.saveRecentVideoUri(file.uri)
                        }
                        navController.navigate(LocalVibeRoute.VideoPlayer)
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
                    deleteSuccessSignal = folderVideoDeleteSuccessSignal,
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
                AudioPlayerScreen(
                    mediaFile = resolvedAudioFile,
                    player = musicController,
                    currentPositionMs = musicCurrentPositionMs,
                    durationMs = musicDurationMs,
                    isPlaying = musicIsPlaying,
                    audioSessionId = MusicPlaybackService.currentAudioSessionId,
                    queue = audioQueue,
                    currentIndex = currentAudioIndex,
                    playMode = audioPlayMode,
                    onPlayModeChanged = { mode ->
                        audioPlayMode = mode
                        applyAudioPlayMode(musicController, mode)
                    },
                    onSelectAudio = { index ->
                        val nextFile = audioQueue.getOrNull(index) ?: return@AudioPlayerScreen
                        playAudioQueue(audioQueue, nextFile)
                    },
                    onPlayPause = {
                        val controller = musicController ?: return@AudioPlayerScreen
                        if (controller.isPlaying) controller.pause() else controller.play()
                    },
                    onPrevious = {
                        val controller = musicController ?: return@AudioPlayerScreen
                        if (controller.currentPosition > 3_000L) {
                            controller.seekTo(0L)
                        } else if (controller.hasPreviousMediaItem()) {
                            controller.seekToPreviousMediaItem()
                        } else {
                            Toast.makeText(context, "已经是第一首", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onNext = {
                        val controller = musicController ?: return@AudioPlayerScreen
                        if (controller.hasNextMediaItem() || audioPlayMode == AudioPlayMode.REPEAT_ALL) {
                            controller.seekToNextMediaItem()
                        } else {
                            Toast.makeText(context, "已经是最后一首", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onSeekTo = { positionMs ->
                        musicController?.seekTo(positionMs)
                    },
                    onBack = { navController.popBackStack() },
                    onRemoveCurrent = { file -> removeMediaFromList(file) },
                    onDeleteCurrent = { file -> permanentlyDeleteMedia(file) }
                )
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
    onMiniPlayPause: () -> Unit = {},
    onOpenMiniPlayer: () -> Unit = {},
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        bottomBar = {
            Column {
                if (miniAudioFile != null) {
                    MiniAudioPlayerBar(
                        file = miniAudioFile,
                        isPlaying = miniIsPlaying,
                        progressMs = miniProgressMs,
                        durationMs = miniDurationMs,
                        onPlayPause = onMiniPlayPause,
                        onOpen = onOpenMiniPlayer
                    )
                }
                MainBottomBar(navController = navController)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            content(Modifier)
        }
    }
}

@Composable
private fun MiniAudioPlayerBar(
    file: LocalMediaFile,
    isPlaying: Boolean,
    progressMs: Long,
    durationMs: Long,
    onPlayPause: () -> Unit,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("♪", color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name.substringBeforeLast('.', file.name),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                Text(
                    text = "Unknown",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            if (durationMs > 0L) {
                Text(
                    text = formatMiniProgress(progressMs, durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onPlayPause) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放"
                )
            }
            IconButton(onClick = onOpen) {
                Icon(Icons.Filled.QueueMusic, contentDescription = "播放队列")
            }
        }
    }
}

@Composable
private fun MainBottomBar(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
        MainTabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = {
                    if (currentRoute != tab.route) {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Text(tab.iconText) },
                label = { Text(tab.label) }
            )
        }
    }
}

private data class MainTab(
    val route: String,
    val label: String,
    val iconText: String
)

private val MainTabs = listOf(
    MainTab(LocalVibeRoute.VideoLibrary, "视频", "▶"),
    MainTab(LocalVibeRoute.AudioLibrary, "音乐", "♪"),
    MainTab(LocalVibeRoute.BookLibrary, "小说", "文"),
    MainTab(LocalVibeRoute.Profile, "我的", "我")
)

private enum class FolderVideoDeleteMode {
    SystemDelete,
    RetryAfterGrant
}

private const val AUTO_SCAN_THROTTLE_MS = 10_000L
private const val BOOK_LOG_TAG = "LocalVibeBooks"

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

private fun LocalMediaFile.normalizedUri(): String = uri.trim()

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

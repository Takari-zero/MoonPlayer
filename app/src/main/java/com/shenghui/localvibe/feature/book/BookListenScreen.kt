package com.shenghui.localvibe.feature.book

import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shenghui.localvibe.BuildConfig
import com.shenghui.localvibe.core.book.BookChapter
import com.shenghui.localvibe.core.book.BookChapterDetector
import com.shenghui.localvibe.core.book.TxtBookReader
import com.shenghui.localvibe.core.scanner.LocalMediaFile
import com.shenghui.localvibe.core.tts.Aishell3SegmentedStreamingTtsEngine
import com.shenghui.localvibe.core.tts.BuiltInOfflineTtsEngine
import com.shenghui.localvibe.core.tts.BuiltInOfflineTtsResult
import com.shenghui.localvibe.core.tts.BookTtsController
import com.shenghui.localvibe.core.tts.BookTtsVoice
import com.shenghui.localvibe.core.tts.StreamingPcmAudioPlayer
import com.shenghui.localvibe.core.tts.StreamingTtsParams
import com.shenghui.localvibe.core.tts.StreamingTtsResult
import com.shenghui.localvibe.core.tts.ToneStreamingTtsEngine
import com.shenghui.localvibe.core.tts.fastspeech2.FastSpeech2BookTtsEngineAdapter
import com.shenghui.localvibe.feature.book.playback.BookReaderAutoAdvanceCoordinator
import com.shenghui.localvibe.feature.book.playback.BookReaderAutoAdvanceDecision
import com.shenghui.localvibe.feature.book.playback.BookReaderAudioTrackSink
import com.shenghui.localvibe.feature.book.playback.BookReaderPlaybackController
import com.shenghui.localvibe.feature.book.playback.BookReaderPlaybackEvent
import com.shenghui.localvibe.feature.book.playback.BookReaderPlaybackTarget
import com.shenghui.localvibe.feature.book.playback.BookReaderEntryReadyState
import com.shenghui.localvibe.feature.book.playback.BookReaderRestoreCache
import com.shenghui.localvibe.feature.book.playback.BookReaderRestoreCacheSource
import com.shenghui.localvibe.feature.book.playback.BookReaderRestorePositionGate
import com.shenghui.localvibe.feature.book.playback.BookTtsEngine
import com.shenghui.localvibe.feature.book.playback.BookTtsPcmResult
import com.shenghui.localvibe.feature.book.playback.NoopBookReaderAudioSink
import com.shenghui.localvibe.feature.book.playback.firstVisibleChapterSentenceIndex
import com.shenghui.localvibe.feature.book.playback.firstVisibleParagraphIndex
import com.shenghui.localvibe.feature.book.playback.firstVisibleSentenceIndex
import com.shenghui.localvibe.feature.book.playback.stableUiSnapshotSource
import com.shenghui.localvibe.feature.book.playback.toEntryRestoreSnapshotInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val USE_FORMAL_BOOK_PLAYBACK_CONTROLLER = BuildConfig.ENABLE_FORMAL_BOOK_PLAYBACK_CONTROLLER
private const val FORMAL_BOOK_PLAYBACK_TAG = "LV_BOOK_FORMAL"
private const val LOG_FORMAL_BOOK_PLAYBACK_DRY_RUN = true
private const val FORMAL_BOOK_PLAYBACK_DRY_RUN_TAG = "LV_BOOK_FORMAL_DRYRUN"

@Composable
fun BookListenScreen(
    bookFile: LocalMediaFile?,
    initialParagraphIndex: Int,
    entryReadyState: BookReaderEntryReadyState? = null,
    onProgressChanged: (String, Int, Int) -> Unit,
    onBeforeSpeak: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val screenEnterAtMillis = remember(bookFile?.uri) { System.currentTimeMillis() }
    val entryReadyConsumeBoundary = remember(entryReadyState) {
        BookReaderEntryReadyConsumeBoundary.from(entryReadyState)
    }
    val entryReadyFirstFrame = entryReadyConsumeBoundary.firstFrameOrNull()
    val usesEntryReadyState = entryReadyConsumeBoundary.usesReadyState
    val entryReadyScreenPlan = entryReadyConsumeBoundary.screenPlan()
    val entryReadyInteractionState = remember(entryReadyState) {
        BookReaderEntryReadyInteractionState()
    }
    val initialReadStateCache = remember(bookFile?.uri, usesEntryReadyState) {
        if (usesEntryReadyState) return@remember null
        loadBookReadStateCache(context.applicationContext, bookFile?.uri)
            ?.let { state ->
                backfillStableUiSnapshot(state, speechRate = 1.0f)?.also { backfilled ->
                    if (backfilled != state) {
                        saveBookReadStateCache(context.applicationContext, backfilled)
                    }
                } ?: state
            }
    }
    val restorePositionKey = remember(
        bookFile?.uri,
        initialReadStateCache?.lastParagraphIndex,
        initialReadStateCache?.lastSentenceIndexInParagraph,
    ) {
        BookReaderRestorePositionGate.restoreKey(
            bookId = bookFile?.uri,
            paragraphIndex = initialReadStateCache?.lastParagraphIndex,
            sentenceIndex = initialReadStateCache?.lastSentenceIndexInParagraph,
        )
    }
    val hasSavedRestorePosition = !usesEntryReadyState && initialReadStateCache != null
    var cachedReadState by remember(bookFile?.uri, usesEntryReadyState) { mutableStateOf(initialReadStateCache) }
    val hasInitialReaderSnapshot = cachedReadState?.let { state ->
        !usesEntryReadyState &&
            state.bookUri == bookFile?.uri &&
            state.hasViewportSnapshot() &&
            state.updatedAt > 0L
    } == true
    var paragraphs by remember(bookFile?.uri, usesEntryReadyState) {
        mutableStateOf(entryReadyFirstFrame?.paragraphs ?: emptyList())
    }
    var currentParagraphIndex by remember(bookFile?.uri, usesEntryReadyState) {
        mutableIntStateOf(
            (
                entryReadyFirstFrame?.firstFrameTarget?.paragraphIndex
                    ?: initialReadStateCache?.lastParagraphIndex
                    ?: initialParagraphIndex
                ).coerceAtLeast(0)
        )
    }
    var currentSentenceIndexInParagraph by remember(bookFile?.uri, usesEntryReadyState) {
        mutableIntStateOf(
            (
                entryReadyFirstFrame?.firstFrameTarget?.sentenceIndex
                    ?: initialReadStateCache?.lastSentenceIndexInParagraph
                    ?: 0
                ).coerceAtLeast(0)
        )
    }
    var currentReadingTargetName by rememberSaveable(bookFile?.uri, usesEntryReadyState) {
        mutableStateOf(
            if (usesEntryReadyState) {
                BookReadingTarget.SENTENCE.name
            } else {
                initialReadStateCache?.lastReadingTargetName ?: BookReadingTarget.SENTENCE.name
            }
        )
    }
    var isLoading by remember(bookFile?.uri, usesEntryReadyState) {
        mutableStateOf(bookFile != null && !usesEntryReadyState)
    }
    var loadError by remember(bookFile?.uri) { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackSessionId by remember { mutableLongStateOf(0L) }
    var activePlaybackEngineName by remember { mutableStateOf(BookPlaybackEngine.NONE.name) }
    var formalAutoAdvanceEnabled by remember { mutableStateOf(false) }
    var hasSettledInitialReaderPosition by remember(restorePositionKey, usesEntryReadyState) {
        mutableStateOf(usesEntryReadyState || initialReadStateCache == null)
    }
    var hasCompletedSnapshotToRealHandoff by remember(restorePositionKey, usesEntryReadyState) {
        mutableStateOf(usesEntryReadyState || !hasInitialReaderSnapshot)
    }
    var restorePositionStartedAtMillis by remember(bookFile?.uri) { mutableLongStateOf(0L) }
    var isTtsReady by remember { mutableStateOf(false) }
    var isTtsChecking by remember { mutableStateOf(true) }
    var speechRate by remember { mutableFloatStateOf(1.0f) }
    var pitch by remember { mutableFloatStateOf(1.0f) }
    var ttsError by remember { mutableStateOf<String?>(null) }
    var ttsRetryKey by remember { mutableIntStateOf(0) }
    var chapterRefreshKey by remember { mutableIntStateOf(0) }
    var showChapterSheet by remember { mutableStateOf(false) }
    var showVoicePackageSheet by remember { mutableStateOf(false) }
    var ttsVoices by remember { mutableStateOf(emptyList<BookTtsVoice>()) }
    var selectedVoiceName by rememberSaveable { mutableStateOf<String?>(null) }
    var voiceDataInstallUnavailable by remember { mutableStateOf(false) }
    var readerFontSizeSp by rememberSaveable(bookFile?.uri) { mutableStateOf(18f) }
    var playbackModeName by rememberSaveable(bookFile?.uri) {
        mutableStateOf(BookPlaybackMode.SEQUENTIAL.name)
    }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var sleepTimerEnabled by rememberSaveable(bookFile?.uri) { mutableStateOf(false) }
    var sleepTimerEndAtMillis by rememberSaveable(bookFile?.uri) { mutableStateOf<Long?>(null) }
    var sleepTimerHours by rememberSaveable(bookFile?.uri) { mutableIntStateOf(0) }
    var sleepTimerMinutes by rememberSaveable(bookFile?.uri) { mutableIntStateOf(30) }
    var sleepTimerStopModeName by rememberSaveable(bookFile?.uri) {
        mutableStateOf(SleepTimerStopMode.IMMEDIATE.name)
    }
    var pendingStopAfterChapter by rememberSaveable(bookFile?.uri) { mutableStateOf(false) }
    var pendingStopChapterIndex by rememberSaveable(bookFile?.uri) { mutableIntStateOf(-1) }
    var sleepTimerRemainingMillis by rememberSaveable(bookFile?.uri) { mutableStateOf(0L) }
    val sleepTimerStopMode = remember(sleepTimerStopModeName) {
        runCatching { SleepTimerStopMode.valueOf(sleepTimerStopModeName) }
            .getOrDefault(SleepTimerStopMode.IMMEDIATE)
    }
    val playbackMode = remember(playbackModeName) {
        runCatching { BookPlaybackMode.valueOf(playbackModeName) }
            .getOrDefault(BookPlaybackMode.SEQUENTIAL)
    }
    val instantEntryRenderMode = BookReaderRestorePositionGate.initialRenderMode(
        hasSavedPosition = hasSavedRestorePosition,
        hasSnapshotContent = hasInitialReaderSnapshot,
        isLoading = isLoading,
        hasLoadError = loadError != null,
    )
    val shouldHideReaderContentForRestore = BookReaderRestorePositionGate.shouldHideContent(
        hasRestorePosition = hasSavedRestorePosition,
        hasSettledPosition = hasSettledInitialReaderPosition,
        isLoading = isLoading,
        hasLoadError = loadError != null,
        hasSnapshotContent = hasInitialReaderSnapshot,
    )

    LaunchedEffect(bookFile?.uri, hasSavedRestorePosition, hasInitialReaderSnapshot, instantEntryRenderMode, restorePositionKey) {
        Log.d(FORMAL_BOOK_PLAYBACK_TAG, "instant entry savedPosition exists=$hasSavedRestorePosition")
        Log.d(FORMAL_BOOK_PLAYBACK_TAG, "instant entry snapshot available=$hasInitialReaderSnapshot restoreKey=$restorePositionKey")
        when (instantEntryRenderMode) {
            BookReaderRestorePositionGate.INITIAL_RENDER_STABLE_UI_SNAPSHOT -> {
                val state = cachedReadState
                Log.d(
                    FORMAL_BOOK_PLAYBACK_TAG,
                    "instant entry render mode=stable_ui_snapshot firstVisible=${state?.viewportFirstVisibleItemIndex ?: -1} " +
                        "offset=${state?.viewportFirstVisibleItemScrollOffset ?: 0} count=${state?.cachedVisibleText?.size ?: 0} " +
                        "source=${state?.stableUiSnapshotSource()} " +
                        "progress=${state?.cachedProgressValue ?: -1f} progressMax=${state?.cachedProgressMaxValue ?: -1f} " +
                        "listened=${state?.cachedListenedTimeLabel.orEmpty()} remaining=${state?.cachedRemainingTimeLabel.orEmpty()}"
                )
            }
            BookReaderRestorePositionGate.INITIAL_RENDER_PLACEHOLDER -> {
                Log.d(FORMAL_BOOK_PLAYBACK_TAG, "instant entry render mode=placeholder reason=no_snapshot_pending_restore")
                Log.d(FORMAL_BOOK_PLAYBACK_TAG, "instant entry blocked default content reason=pending_restore")
            }
        }
    }

    LaunchedEffect(bookFile?.uri, shouldHideReaderContentForRestore) {
        if (shouldHideReaderContentForRestore) {
            Log.d(
                FORMAL_BOOK_PLAYBACK_TAG,
                "restore position gate hidden reason=pending_restore key=$restorePositionKey"
            )
        } else if (!hasSavedRestorePosition) {
            Log.d(FORMAL_BOOK_PLAYBACK_TAG, "restore position skipped reason=no_saved_position")
            Log.d(FORMAL_BOOK_PLAYBACK_TAG, "restore position visible content enabled reason=no_saved_position")
        }
    }

    LaunchedEffect(entryReadyState) {
        val firstFrame = entryReadyFirstFrame ?: return@LaunchedEffect
        val firstFrameEqualsPlayTarget =
            firstFrame.firstFrameTarget.paragraphIndex == firstFrame.playbackSeed.paragraphIndex &&
                firstFrame.firstFrameTarget.sentenceIndex == firstFrame.playbackSeed.sentenceIndex &&
                firstFrame.firstFrameTarget.chapterSentenceIndex == firstFrame.progressSnapshot.chapterSentenceIndex
        Log.d(
            FORMAL_BOOK_PLAYBACK_TAG,
            "phase5b formal ready path BookListenScreen entryReadyState non-null " +
                "firstFrame target=${firstFrame.firstFrameTarget.paragraphIndex}/" +
                "${firstFrame.firstFrameTarget.sentenceIndex}/" +
                "${firstFrame.firstFrameTarget.chapterSentenceIndex} " +
                "playback seed target=${firstFrame.playbackSeed.paragraphIndex}/" +
                "${firstFrame.playbackSeed.sentenceIndex}"
        )
        Log.d(
            FORMAL_BOOK_PLAYBACK_TAG,
            "phase5c ready screen path selected entryReadyState non-null=true " +
                "legacyRestoreLoadSkipped=${entryReadyScreenPlan.skipLegacyRestoreLoad} " +
                "stableSnapshotSkipped=${entryReadyScreenPlan.skipStableSnapshot} " +
                "restorePromptReachable=${!entryReadyScreenPlan.skipRestorePrompt} " +
                "firstFrameEqualsPlayTarget=$firstFrameEqualsPlayTarget"
        )
    }

    LaunchedEffect(bookFile?.uri, entryReadyState) {
        val firstFrame = entryReadyFirstFrame
        if (firstFrame != null) {
            paragraphs = firstFrame.paragraphs
            currentParagraphIndex = firstFrame.firstFrameTarget.paragraphIndex
            currentSentenceIndexInParagraph = firstFrame.firstFrameTarget.sentenceIndex
            currentReadingTargetName = BookReadingTarget.SENTENCE.name
            cachedReadState = null
            isLoading = false
            loadError = null
            hasSettledInitialReaderPosition = true
            hasCompletedSnapshotToRealHandoff = true
            Log.d(
                FORMAL_BOOK_PLAYBACK_TAG,
                "phase5b formal ready path firstFrame target=" +
                    "${firstFrame.firstFrameTarget.paragraphIndex}/" +
                    "${firstFrame.firstFrameTarget.sentenceIndex}/" +
                    "${firstFrame.firstFrameTarget.chapterSentenceIndex}"
            )
            Log.d(
                FORMAL_BOOK_PLAYBACK_TAG,
                "phase5b formal ready path playback seed target=" +
                    "${firstFrame.playbackSeed.paragraphIndex}/" +
                    "${firstFrame.playbackSeed.sentenceIndex}"
            )
            return@LaunchedEffect
        }
        Log.d(FORMAL_BOOK_PLAYBACK_TAG, "perf screen_enter timestamp=$screenEnterAtMillis")
        val snapshotElapsedMs = (System.currentTimeMillis() - screenEnterAtMillis).coerceAtLeast(0L)
        Log.d(
            FORMAL_BOOK_PLAYBACK_TAG,
            "perf recent_snapshot_load elapsedMs=$snapshotElapsedMs available=$hasInitialReaderSnapshot"
        )
        if (hasInitialReaderSnapshot) {
            Log.d(
                FORMAL_BOOK_PLAYBACK_TAG,
                "perf first_visible_snapshot elapsedMs=$snapshotElapsedMs"
            )
        }
    }
    val currentReadingTarget = remember(currentReadingTargetName) {
        runCatching { BookReadingTarget.valueOf(currentReadingTargetName) }
            .getOrDefault(BookReadingTarget.SENTENCE)
    }
    val chapters = remember(paragraphs, chapterRefreshKey) {
        BookChapterDetector.detect(paragraphs)
    }
    val currentChapter = remember(chapters, currentParagraphIndex) {
        chapters.lastOrNull { it.paragraphIndex <= currentParagraphIndex }
    }
    val latestIsPlaying by rememberUpdatedState(isPlaying)
    val latestParagraphs by rememberUpdatedState(paragraphs)
    val latestBookFile by rememberUpdatedState(bookFile)
    val latestSpeechRate by rememberUpdatedState(speechRate)
    val latestPitch by rememberUpdatedState(pitch)
    val latestPlaybackMode by rememberUpdatedState(playbackMode)
    val latestPendingStopAfterChapter by rememberUpdatedState(pendingStopAfterChapter)
    val latestPendingStopChapterIndex by rememberUpdatedState(pendingStopChapterIndex)
    val latestChapters by rememberUpdatedState(chapters)
    val latestCurrentParagraphIndex by rememberUpdatedState(currentParagraphIndex)
    val latestCurrentSentenceIndexInParagraph by rememberUpdatedState(currentSentenceIndexInParagraph)
    val latestCurrentReadingTarget by rememberUpdatedState(currentReadingTarget)
    val latestActivePlaybackEngineName by rememberUpdatedState(activePlaybackEngineName)
    val latestPlaybackSessionId by rememberUpdatedState(playbackSessionId)
    val latestFormalAutoAdvanceEnabled by rememberUpdatedState(formalAutoAdvanceEnabled)

    var ttsController by remember { mutableStateOf<BookTtsController?>(null) }
    val builtInOfflineTtsEngine = remember { BuiltInOfflineTtsEngine() }
    val aishell3TtsEngine = remember { Aishell3SegmentedStreamingTtsEngine(context.applicationContext) }
    var aishell3Player by remember { mutableStateOf<StreamingPcmAudioPlayer?>(null) }
    var isAishell3Paused by remember { mutableStateOf(false) }
    val formalPlaybackEventChannel = remember { Channel<BookReaderPlaybackEvent>(Channel.UNLIMITED) }
    val formalAutoAdvanceCoordinator = remember { BookReaderAutoAdvanceCoordinator() }
    val formalBookTtsEngine = remember(USE_FORMAL_BOOK_PLAYBACK_CONTROLLER) {
        if (USE_FORMAL_BOOK_PLAYBACK_CONTROLLER) {
            FastSpeech2BookTtsEngineAdapter(context.applicationContext)
        } else {
            DisabledBookReaderTtsEngine
        }
    }
    val formalPlaybackController = remember(USE_FORMAL_BOOK_PLAYBACK_CONTROLLER) {
        val formalAudioSink = if (USE_FORMAL_BOOK_PLAYBACK_CONTROLLER) {
            BookReaderAudioTrackSink()
        } else {
            NoopBookReaderAudioSink()
        }
        Log.d(FORMAL_BOOK_PLAYBACK_TAG, "reader formal enabled=$USE_FORMAL_BOOK_PLAYBACK_CONTROLLER")
        BookReaderPlaybackController(
            engine = formalBookTtsEngine,
            audioSink = formalAudioSink,
            logger = { message -> Log.d(FORMAL_BOOK_PLAYBACK_TAG, message) },
            eventSink = { event ->
                val sent = formalPlaybackEventChannel.trySend(event).isSuccess
                Log.d(FORMAL_BOOK_PLAYBACK_TAG, "auto advance event queued type=${event::class.simpleName} sent=$sent")
            }
        )
    }
    var isBuiltInOfflineTtsInitializing by remember { mutableStateOf(false) }
    var builtInOfflineTtsError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            builtInOfflineTtsEngine.release()
            aishell3TtsEngine.release()
            aishell3Player?.release()
            formalPlaybackController.release()
        }
    }

    LaunchedEffect(bookFile?.uri, USE_FORMAL_BOOK_PLAYBACK_CONTROLLER) {
        if (!USE_FORMAL_BOOK_PLAYBACK_CONTROLLER || bookFile == null) return@LaunchedEffect
        launch(Dispatchers.Default) {
            val startMs = System.currentTimeMillis()
            Log.d(FORMAL_BOOK_PLAYBACK_TAG, "tts prewarm start")
            val warmTarget = BookReaderPlaybackTarget(
                bookId = bookFile.uri,
                bookTitle = bookFile.displayTitle(),
                chapterIndex = 0,
                chapterTitle = initialReadStateCache?.lastChapterTitle.orEmpty().ifBlank { "Prewarm" },
                paragraphIndex = currentParagraphIndex,
                sentenceIndex = currentSentenceIndexInParagraph,
                clauseIndex = 0,
                sentenceText = "这是一段离线语音预热。",
                sentencePreview = "这是一段离线语音预热。",
            )
            val result = formalBookTtsEngine.synthesizeToPcm(
                target = warmTarget,
                text = warmTarget.sentenceText,
                sessionId = -1L,
            )
            val elapsedMs = (System.currentTimeMillis() - startMs).coerceAtLeast(0L)
            Log.d(
                FORMAL_BOOK_PLAYBACK_TAG,
                "tts prewarm done elapsedMs=$elapsedMs success=${result.isSuccess}"
            )
            Log.d(FORMAL_BOOK_PLAYBACK_TAG, "perf tts_prewarm done elapsedMs=$elapsedMs")
        }
    }

    fun saveProgress(index: Int, total: Int = paragraphs.size) {
        val file = bookFile ?: return
        if (total <= 0) return
        onProgressChanged(file.uri, index.coerceIn(0, total - 1), total)
    }

    fun stopAishell3Playback() {
        isAishell3Paused = false
        aishell3TtsEngine.stop()
        aishell3Player?.stop()
        aishell3Player?.release()
        aishell3Player = null
    }

    fun stopCurrentPlayback(reason: String, invalidateSession: Boolean = true) {
        if (!USE_FORMAL_BOOK_PLAYBACK_CONTROLLER && LOG_FORMAL_BOOK_PLAYBACK_DRY_RUN) {
            Log.d(FORMAL_BOOK_PLAYBACK_DRY_RUN_TAG, "stop reason=$reason")
        }
        if (USE_FORMAL_BOOK_PLAYBACK_CONTROLLER) {
            formalAutoAdvanceEnabled = false
            formalAutoAdvanceCoordinator.onStopRequested()
            formalPlaybackController.stop(reason)
            Log.d(FORMAL_BOOK_PLAYBACK_TAG, "reader stop formal reason=$reason")
            activePlaybackEngineName = BookPlaybackEngine.NONE.name
            return
        }
        if (invalidateSession) {
            playbackSessionId += 1
        }
        Log.d(
            "BookReaderPlayback",
            "stop current session reason=$reason sessionId=$playbackSessionId engine=$activePlaybackEngineName"
        )
        stopAishell3Playback()
        ttsController?.stop()
        activePlaybackEngineName = BookPlaybackEngine.NONE.name
    }

    fun clearSleepTimer() {
        sleepTimerEnabled = false
        sleepTimerEndAtMillis = null
        sleepTimerRemainingMillis = 0L
        pendingStopAfterChapter = false
        pendingStopChapterIndex = -1
    }

    fun stopForSleepTimer(showToast: Boolean = true) {
        stopCurrentPlayback(reason = "sleep_timer", invalidateSession = true)
        isPlaying = false
        if (paragraphs.isNotEmpty()) {
            saveProgress(currentParagraphIndex)
        }
        clearSleepTimer()
        if (showToast) {
            Toast.makeText(context, "定时结束，已停止听书", Toast.LENGTH_SHORT).show()
        }
    }

    fun selectedSleepTimerDurationMillis(): Long {
        return ((sleepTimerHours * 60L) + sleepTimerMinutes) * 60_000L
    }

    fun startOrUpdateSleepTimer(isUpdate: Boolean) {
        val durationMillis = selectedSleepTimerDurationMillis()
        if (durationMillis <= 0L) {
            Toast.makeText(context, "请选择定时时间", Toast.LENGTH_SHORT).show()
            return
        }
        val now = System.currentTimeMillis()
        sleepTimerEndAtMillis = now + durationMillis
        sleepTimerRemainingMillis = durationMillis
        sleepTimerEnabled = true
        pendingStopAfterChapter = false
        pendingStopChapterIndex = -1
        showSleepTimerSheet = false
        Toast.makeText(
            context,
            if (isUpdate) "已更新定时关闭" else "已开启定时关闭",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun chapterStartFor(index: Int, chapterList: List<BookChapter>): Int {
        return chapterList
            .lastOrNull { it.paragraphIndex <= index }
            ?.paragraphIndex
            ?: 0
    }

    fun chapterEndExclusiveFor(index: Int, total: Int, chapterList: List<BookChapter>): Int {
        if (total <= 0) return 0
        val chapterIndex = chapterList.indexOfLast { it.paragraphIndex <= index }
        return chapterList
            .getOrNull(chapterIndex + 1)
            ?.paragraphIndex
            ?.coerceIn(0, total)
            ?: total
    }

    fun paragraphSentences(list: List<String>, index: Int): List<String> {
        if (index !in list.indices) return emptyList()
        return splitParagraphIntoSentences(list[index]).ifEmpty { listOf(list[index]) }
    }

    fun currentChapterIndexFor(paragraphIndex: Int, chapterList: List<BookChapter> = chapters): Int {
        return chapterList.indexOfLast { it.paragraphIndex <= paragraphIndex }
    }

    fun isChapterStartParagraph(paragraphIndex: Int): Boolean {
        return chapters.any { it.paragraphIndex == paragraphIndex }
    }

    fun moveToSentenceTarget(paragraphIndex: Int, sentenceIndex: Int): Boolean {
        if (paragraphs.isEmpty()) return false
        val safeParagraphIndex = paragraphIndex.coerceIn(0, paragraphs.lastIndex)
        val sentences = paragraphSentences(paragraphs, safeParagraphIndex)
        if (sentences.isEmpty()) return false
        currentParagraphIndex = safeParagraphIndex
        currentSentenceIndexInParagraph = sentenceIndex.coerceIn(0, sentences.lastIndex)
        currentReadingTargetName = BookReadingTarget.SENTENCE.name
        saveProgress(safeParagraphIndex)
        return true
    }

    fun moveToChapterTitleTarget(chapterIndex: Int): Boolean {
        val chapter = chapters.getOrNull(chapterIndex) ?: return false
        if (paragraphs.isEmpty()) return false
        currentParagraphIndex = chapter.paragraphIndex.coerceIn(0, paragraphs.lastIndex)
        currentSentenceIndexInParagraph = 0
        currentReadingTargetName = BookReadingTarget.CHAPTER_TITLE.name
        saveProgress(currentParagraphIndex)
        return true
    }

    fun firstSentenceInChapter(chapterIndex: Int): Pair<Int, Int>? {
        val chapter = chapters.getOrNull(chapterIndex) ?: return null
        val chapterStart = chapter.paragraphIndex.coerceIn(0, paragraphs.size)
        val chapterEndExclusive = chapters
            .getOrNull(chapterIndex + 1)
            ?.paragraphIndex
            ?.coerceIn(0, paragraphs.size)
            ?: paragraphs.size
        val chapterTitle = chapter.title.trim()
        for (paragraphIndex in chapterStart until chapterEndExclusive) {
            val paragraph = paragraphs[paragraphIndex].trim()
            if (paragraph.isBlank()) continue
            if (paragraphIndex == chapterStart && chapterTitle.isNotBlank() && paragraph == chapterTitle) {
                continue
            }
            val sentences = paragraphSentences(paragraphs, paragraphIndex)
            val sentenceIndex = sentences.indexOfFirst { isReadableBookTtsText(it) }
            if (sentenceIndex >= 0) return paragraphIndex to sentenceIndex
        }
        return null
    }

    fun moveToNextSequentialTarget(paragraphIndex: Int, sentenceIndex: Int): Boolean {
        if (paragraphs.isEmpty()) return false
        val currentSentences = paragraphSentences(paragraphs, paragraphIndex)
        for (nextSentenceIndex in (sentenceIndex + 1) until currentSentences.size) {
            if (isReadableBookTtsText(currentSentences[nextSentenceIndex])) {
                return moveToSentenceTarget(paragraphIndex, nextSentenceIndex)
            }
        }
        for (nextParagraphIndex in (paragraphIndex + 1)..paragraphs.lastIndex) {
            val nextChapterIndex = chapters.indexOfFirst { it.paragraphIndex == nextParagraphIndex }
            if (nextChapterIndex >= 0) {
                return moveToChapterTitleTarget(nextChapterIndex)
            }
            val sentences = paragraphSentences(paragraphs, nextParagraphIndex)
            val nextSentenceIndex = sentences.indexOfFirst { isReadableBookTtsText(it) }
            if (nextSentenceIndex >= 0) {
                return moveToSentenceTarget(nextParagraphIndex, nextSentenceIndex)
            }
        }
        return false
    }

    fun shouldStopAfterCurrentChapter(): Boolean {
        if (currentReadingTargetName == BookReadingTarget.CHAPTER_TITLE.name) return false
        if (!pendingStopAfterChapter) return false
        val chapterIndex = currentChapterIndexFor(currentParagraphIndex)
        if (chapterIndex != pendingStopChapterIndex) return false
        val chapterEndExclusive = chapters
            .getOrNull(chapterIndex + 1)
            ?.paragraphIndex
            ?.coerceIn(0, paragraphs.size)
            ?: paragraphs.size
        val sentences = paragraphSentences(paragraphs, currentParagraphIndex)
        return currentParagraphIndex >= chapterEndExclusive - 1 &&
            currentSentenceIndexInParagraph >= sentences.lastIndex
    }

    fun moveAfterSpokenTarget(): Boolean {
        if (paragraphs.isEmpty()) return false
        if (shouldStopAfterCurrentChapter()) {
            stopCurrentPlayback(reason = "sleep_timer_after_chapter_done", invalidateSession = true)
            isPlaying = false
            clearSleepTimer()
            Toast.makeText(context, "本章已播完，已停止听书", Toast.LENGTH_SHORT).show()
            return false
        }

        val chapterIndex = currentChapterIndexFor(currentParagraphIndex)
        if (currentReadingTargetName == BookReadingTarget.CHAPTER_TITLE.name) {
            val firstSentence = firstSentenceInChapter(chapterIndex)
            return firstSentence?.let { moveToSentenceTarget(it.first, it.second) } == true
        }

        return when (playbackMode) {
            BookPlaybackMode.SEQUENTIAL -> moveToNextSequentialTarget(
                currentParagraphIndex,
                currentSentenceIndexInParagraph
            )
            BookPlaybackMode.SINGLE_PARAGRAPH -> {
                val sentences = paragraphSentences(paragraphs, currentParagraphIndex)
                if (sentences.isEmpty()) {
                    false
                } else {
                    var nextSentenceIndex = currentSentenceIndexInParagraph
                    repeat(sentences.size) {
                        nextSentenceIndex = (nextSentenceIndex + 1) % sentences.size
                        if (isReadableBookTtsText(sentences[nextSentenceIndex])) {
                            return moveToSentenceTarget(currentParagraphIndex, nextSentenceIndex)
                        }
                    }
                    false
                }
            }
            BookPlaybackMode.CHAPTER_LOOP -> {
                val currentChapterIndex = currentChapterIndexFor(currentParagraphIndex)
                val chapterEndExclusive = chapters
                    .getOrNull(currentChapterIndex + 1)
                    ?.paragraphIndex
                    ?.coerceIn(0, paragraphs.size)
                    ?: paragraphs.size
                val moved = moveToNextSequentialTarget(
                    currentParagraphIndex,
                    currentSentenceIndexInParagraph
                )
                val stayedInChapter = moved && currentChapterIndexFor(currentParagraphIndex) == currentChapterIndex &&
                    currentParagraphIndex < chapterEndExclusive
                if (stayedInChapter) {
                    true
                } else {
                    moveToChapterTitleTarget(currentChapterIndex)
                }
            }
        }
    }

    fun currentSpeakText(): String? {
        if (paragraphs.isEmpty()) return null
        val index = currentParagraphIndex.coerceIn(0, paragraphs.lastIndex)
        val isChapterTitleTarget = currentReadingTargetName == BookReadingTarget.CHAPTER_TITLE.name
        val sentences = paragraphSentences(paragraphs, index)
        val sentenceIndex = currentSentenceIndexInParagraph.coerceIn(0, (sentences.size - 1).coerceAtLeast(0))
        currentParagraphIndex = index
        if (!isChapterTitleTarget) {
            currentSentenceIndexInParagraph = sentenceIndex
        }
        return if (isChapterTitleTarget) {
            chapters.lastOrNull { it.paragraphIndex <= index }
                ?.title
                ?.takeIf { it.isNotBlank() }
                ?: paragraphs[index]
        } else {
            sentences.getOrNull(sentenceIndex)
        }?.trim()
    }

    var requestSpeakCurrent: () -> Unit = {}

    fun continuePlaybackAfterCurrentTarget(sessionId: Long) {
        if (sessionId != playbackSessionId) {
            Log.d("BookReaderPlayback", "discard stale session sessionId=$sessionId current=$playbackSessionId")
            return
        }
        val moved = moveAfterSpokenTarget()
        Log.d(
            "BookReaderPlayback",
            "advance next target sessionId=$sessionId moved=$moved targetType=$currentReadingTargetName " +
                "paragraphIndex=$currentParagraphIndex sentenceIndexInParagraph=$currentSentenceIndexInParagraph"
        )
        if (moved && isPlaying) {
            requestSpeakCurrent()
        } else {
            isPlaying = false
            activePlaybackEngineName = BookPlaybackEngine.NONE.name
            bookFile?.let { file ->
                if (paragraphs.isNotEmpty()) {
                    onProgressChanged(file.uri, currentParagraphIndex.coerceIn(0, paragraphs.lastIndex), paragraphs.size)
                }
            }
        }
    }

    LaunchedEffect(formalPlaybackController) {
        Log.d(
            FORMAL_BOOK_PLAYBACK_TAG,
            "auto advance collector started controller=${System.identityHashCode(formalPlaybackController)}"
        )
        if (!USE_FORMAL_BOOK_PLAYBACK_CONTROLLER) return@LaunchedEffect
        try {
            for (event in formalPlaybackEventChannel) {
                if (event !is BookReaderPlaybackEvent.PlaybackCompleted) continue
                Log.d(
                    FORMAL_BOOK_PLAYBACK_TAG,
                    "auto advance event received sessionId=${event.sessionId} target=${event.target.paragraphIndex}:${event.target.sentenceIndex}"
                )

                val controllerState = formalPlaybackController.state
                Log.d(
                    FORMAL_BOOK_PLAYBACK_TAG,
                    "auto advance state userPaused=${!latestFormalAutoAdvanceEnabled || controllerState.isPaused} " +
                        "userStopped=${!latestIsPlaying} targetMatches=${controllerState.target == event.target} " +
                        "engine=${latestActivePlaybackEngineName}"
                )
                val decision = formalAutoAdvanceCoordinator.decide(
                    completedTarget = event.target,
                    currentTarget = controllerState.target,
                    isPlaybackPaused = controllerState.isPaused,
                    isReaderPlaying = latestIsPlaying,
                    activePlaybackEngineName = latestActivePlaybackEngineName,
                    expectedPlaybackEngineName = BookPlaybackEngine.FASTSPEECH2.name,
                    nextTargetProvider = {
                        findNextFormalAutoAdvanceTarget(
                            bookFile = latestBookFile,
                            chapters = latestChapters,
                            paragraphs = latestParagraphs,
                            completedTarget = event.target
                        )
                    }
                )

                when (decision) {
                    is BookReaderAutoAdvanceDecision.Advance -> {
                        val nextTarget = decision.target
                        currentParagraphIndex = nextTarget.paragraphIndex
                        currentSentenceIndexInParagraph = nextTarget.sentenceIndex
                        currentReadingTargetName = BookReadingTarget.SENTENCE.name
                        latestBookFile?.let { file ->
                            if (latestParagraphs.isNotEmpty()) {
                                onProgressChanged(
                                    file.uri,
                                    nextTarget.paragraphIndex.coerceIn(0, latestParagraphs.lastIndex),
                                    latestParagraphs.size
                                )
                            }
                        }
                        Log.d(
                            FORMAL_BOOK_PLAYBACK_TAG,
                            "reader highlight from auto advance paragraphIndex=${nextTarget.paragraphIndex} sentenceIndex=${nextTarget.sentenceIndex}"
                        )
                        Log.d(
                            FORMAL_BOOK_PLAYBACK_TAG,
                            "auto advance next target chapterIndex=${nextTarget.chapterIndex} paragraphIndex=${nextTarget.paragraphIndex} " +
                                "sentenceIndex=${nextTarget.sentenceIndex}"
                        )
                        activePlaybackEngineName = BookPlaybackEngine.FASTSPEECH2.name
                        isPlaying = true
                        formalAutoAdvanceEnabled = true
                        formalAutoAdvanceCoordinator.onPlayRequested()
                        Log.d(
                            FORMAL_BOOK_PLAYBACK_TAG,
                            "auto advance play next session request paragraphIndex=${nextTarget.paragraphIndex} sentenceIndex=${nextTarget.sentenceIndex}"
                        )
                        formalPlaybackController.play(nextTarget)
                    }

                    BookReaderAutoAdvanceDecision.StopAtEnd -> {
                        Log.d(FORMAL_BOOK_PLAYBACK_TAG, "auto advance stop reason=end_of_chapter")
                        formalAutoAdvanceEnabled = false
                        formalAutoAdvanceCoordinator.onStopRequested()
                        isPlaying = false
                        activePlaybackEngineName = BookPlaybackEngine.NONE.name
                        latestBookFile?.let { file ->
                            if (latestParagraphs.isNotEmpty()) {
                                onProgressChanged(
                                    file.uri,
                                    event.target.paragraphIndex.coerceIn(0, latestParagraphs.lastIndex),
                                    latestParagraphs.size
                                )
                            }
                        }
                    }

                    is BookReaderAutoAdvanceDecision.Skip -> {
                        Log.d(FORMAL_BOOK_PLAYBACK_TAG, "auto advance skipped reason=${decision.reason}")
                    }
                }
            }
        } finally {
            Log.d(
                FORMAL_BOOK_PLAYBACK_TAG,
                "auto advance collector disposed/cancelled controller=${System.identityHashCode(formalPlaybackController)}"
            )
        }
    }
    DisposableEffect(ttsRetryKey) {
        val controller = BookTtsController(
            context = context,
            onReady = {
                isTtsReady = true
                isTtsChecking = false
                ttsError = null
                selectedVoiceName?.let { ttsController?.selectVoice(it) }
                ttsVoices = ttsController?.getAvailableVoices().orEmpty()
            },
            onError = { message ->
                ttsError = message
                isTtsReady = false
                isTtsChecking = false
                ttsVoices = emptyList()
                if (latestActivePlaybackEngineName == BookPlaybackEngine.SYSTEM_TTS.name && latestIsPlaying) {
                    isPlaying = false
                    showVoicePackageSheet = true
                }
                Log.d("BookReaderPlayback", "system tts unavailable message=$message")
            },
            onWarning = { message ->
                ttsError = message
            },
            onDone = {
                if (latestActivePlaybackEngineName != BookPlaybackEngine.SYSTEM_TTS.name) {
                    Log.d(
                        "BookReaderPlayback",
                        "ignore system onDone activeEngine=$latestActivePlaybackEngineName"
                    )
                    return@BookTtsController
                }
                if (!latestIsPlaying || latestParagraphs.isEmpty()) return@BookTtsController
                continuePlaybackAfterCurrentTarget(latestPlaybackSessionId)
            }
        )
        ttsController = controller
        onDispose {
            latestBookFile?.let { file ->
                if (latestParagraphs.isNotEmpty()) {
                    onProgressChanged(
                        file.uri,
                        currentParagraphIndex.coerceIn(0, latestParagraphs.lastIndex),
                        latestParagraphs.size
                    )
                }
            }
            controller.shutdown()
            ttsController = null
        }
    }

    fun restartTtsCheck() {
        isTtsReady = false
        isTtsChecking = true
        ttsError = null
        ttsController?.shutdown()
        ttsController = null
        ttsRetryKey += 1
        Toast.makeText(context, "正在重新检测系统语音", Toast.LENGTH_SHORT).show()
    }

    fun openIntentSafely(intent: Intent, errorMessage: String) {
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    fun canHandleIntent(intent: Intent): Boolean {
        return intent.resolveActivity(context.packageManager) != null
    }

    fun copySearchKeyword(keyword: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("TTS engine", keyword))
    }

    fun openVoiceDataInstaller() {
        val intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
        if (!canHandleIntent(intent)) {
            voiceDataInstallUnavailable = true
            showVoicePackageSheet = true
            Toast.makeText(
                context,
                "当前系统不支持直接安装语音数据，请安装可用的 TTS 引擎",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        voiceDataInstallUnavailable = false
        openIntentSafely(intent, "无法打开语音数据安装页面")
    }

    fun openSystemVoiceSettings() {
        openIntentSafely(
            Intent("com.android.settings.TTS_SETTINGS")
                .also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
            "无法打开语音设置"
        )
    }

    fun searchRhVoiceEngine() {
        val keyword = "RHVoice"
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$keyword&c=apps"))
            .also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        runCatching {
            context.startActivity(marketIntent)
        }.onFailure {
            copySearchKeyword(keyword)
            Toast.makeText(
                context,
                "已复制 RHVoice，请到应用商店搜索安装",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun selectTtsVoice(voiceName: String?) {
        if (!isTtsReady) {
            Toast.makeText(context, "系统语音不可用", Toast.LENGTH_SHORT).show()
            return
        }
        val success = ttsController?.selectVoice(voiceName) == true
        if (success) {
            selectedVoiceName = voiceName
            Toast.makeText(
                context,
                if (voiceName == null) "已使用默认声线" else "已选择系统声线",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(context, "该声线不可用", Toast.LENGTH_SHORT).show()
        }
    }

    fun previewSystemVoice() {
        if (isPlaying) {
            Toast.makeText(context, "请先暂停听书再试听", Toast.LENGTH_SHORT).show()
            return
        }
        if (!isTtsReady) {
            showVoicePackageSheet = true
            Toast.makeText(context, "请先安装或启用系统语音", Toast.LENGTH_SHORT).show()
            return
        }
        val result = ttsController?.speakSentence(
            text = "这是一段系统语音试听。",
            speechRate = speechRate,
            pitch = pitch,
            utteranceId = "book-tts-preview"
        )
        if (result?.success == true) {
            Toast.makeText(context, "正在试听系统语音", Toast.LENGTH_SHORT).show()
        } else {
            ttsError = result?.message ?: "试听失败"
            showVoicePackageSheet = true
            Toast.makeText(context, ttsError, Toast.LENGTH_SHORT).show()
        }
    }

    fun speakCurrentSentence() {
        if (activePlaybackEngineName == BookPlaybackEngine.AISHELL3.name && isAishell3Paused) {
            if (LOG_FORMAL_BOOK_PLAYBACK_DRY_RUN) {
                Log.d(FORMAL_BOOK_PLAYBACK_DRY_RUN_TAG, "resume")
            }
            Log.d("BookReaderPlayback", "resume paused aishell3 sessionId=$playbackSessionId")
            aishell3Player?.resume()
            isAishell3Paused = false
            isPlaying = true
            return
        }

        val file = bookFile
        if (file == null) {
            Toast.makeText(context, "未选择小说文件", Toast.LENGTH_SHORT).show()
            return
        }
        if (paragraphs.isEmpty()) {
            Toast.makeText(context, "小说内容为空", Toast.LENGTH_SHORT).show()
            return
        }

        val textToSpeak = currentSpeakText()
        if (!isReadableBookTtsText(textToSpeak.orEmpty())) {
            Log.d(
                "BookReaderPlayback",
                "skip unreadable target targetType=$currentReadingTargetName " +
                    "paragraphIndex=$currentParagraphIndex sentenceIndexInParagraph=$currentSentenceIndexInParagraph " +
                    "textPreview=${textToSpeak.orEmpty().take(24)}"
            )
            if (moveAfterSpokenTarget()) {
                speakCurrentSentence()
            } else {
                isPlaying = false
                activePlaybackEngineName = BookPlaybackEngine.NONE.name
            }
            return
        }

        val formalTargetForPlay = if (USE_FORMAL_BOOK_PLAYBACK_CONTROLLER || LOG_FORMAL_BOOK_PLAYBACK_DRY_RUN) {
            buildFormalBookReaderPlaybackTarget(
                bookFile = file,
                chapters = chapters,
                paragraphs = paragraphs,
                paragraphIndex = currentParagraphIndex,
                sentenceIndex = currentSentenceIndexInParagraph,
                fallbackSentenceText = textToSpeak.orEmpty(),
                dryRunSource = "play"
            )
        } else {
            null
        }
        if (!USE_FORMAL_BOOK_PLAYBACK_CONTROLLER && LOG_FORMAL_BOOK_PLAYBACK_DRY_RUN) {
            logFormalDryRunTarget("play", formalTargetForPlay, resumeIfPlaying = true)
        }
        if (USE_FORMAL_BOOK_PLAYBACK_CONTROLLER) {
            val target = formalTargetForPlay
            if (target == null) {
                Log.d(FORMAL_BOOK_PLAYBACK_TAG, "fallback to legacy reason=missing_target")
            } else {
                val firstPlayElapsedMs = (System.currentTimeMillis() - screenEnterAtMillis).coerceAtLeast(0L)
                Log.d(
                    FORMAL_BOOK_PLAYBACK_TAG,
                    "first play request elapsedSinceScreenEnterMs=$firstPlayElapsedMs"
                )
                Log.d(
                    FORMAL_BOOK_PLAYBACK_TAG,
                    "reader play formal target chapterIndex=${target.chapterIndex} " +
                        "paragraphIndex=${target.paragraphIndex} sentenceIndex=${target.sentenceIndex}"
                )
                coroutineScope.launch {
                    val playbackState = formalPlaybackController.state
                    val targetMatches = playbackState.target == target
                    val canResumeCurrent = playbackState.isPaused && playbackState.canResume && targetMatches
                    val resumeFallbackReason = when {
                        playbackState.isPaused && !playbackState.canResume -> "no_resumable_audio"
                        playbackState.isPaused && !targetMatches -> "target_changed"
                        else -> null
                    }
                    Log.d(
                        FORMAL_BOOK_PLAYBACK_TAG,
                        "reader play decision action=${if (canResumeCurrent) "resume" else "play"} " +
                            "canResume=${playbackState.canResume} targetMatches=$targetMatches"
                    )
                    activePlaybackEngineName = BookPlaybackEngine.FASTSPEECH2.name
                    isPlaying = true
                    formalAutoAdvanceEnabled = true
                    formalAutoAdvanceCoordinator.onPlayRequested()
                    if (canResumeCurrent) {
                        Log.d(
                            FORMAL_BOOK_PLAYBACK_TAG,
                            "resume requested canResume=${playbackState.canResume} target=${target.paragraphIndex}:${target.sentenceIndex}"
                        )
                        Log.d(FORMAL_BOOK_PLAYBACK_TAG, "reader resume formal")
                        formalPlaybackController.resume()
                    } else {
                        if (resumeFallbackReason != null) {
                            Log.d(
                                FORMAL_BOOK_PLAYBACK_TAG,
                                "resume fallback action=play_current_or_next reason=$resumeFallbackReason"
                            )
                        }
                        Log.d(FORMAL_BOOK_PLAYBACK_TAG, "resume autoAdvanceEnabled=true")
                        formalPlaybackController.play(target)
                    }
                }
                return
            }
        } else {
            Log.d(FORMAL_BOOK_PLAYBACK_TAG, "fallback to legacy reason=feature_flag_disabled")
        }

        playbackSessionId += 1
        val sessionId = playbackSessionId
        val targetType = currentReadingTargetName
        val targetParagraphIndex = currentParagraphIndex
        val targetSentenceIndex = currentSentenceIndexInParagraph
        val targetChapterIndex = currentChapterIndexFor(targetParagraphIndex)
        val textPreview = textToSpeak.orEmpty().replace('\n', ' ').take(40)
        Log.d(
            "BookReaderPlayback",
            "playbackRequested sessionId=$sessionId targetType=$targetType " +
                "paragraphIndex=$targetParagraphIndex sentenceIndexInParagraph=$targetSentenceIndex " +
                "chapterIndex=$targetChapterIndex textPreview=$textPreview"
        )
        stopAishell3Playback()
        ttsController?.stop()
        activePlaybackEngineName = BookPlaybackEngine.NONE.name
        onBeforeSpeak()

        fun fallbackToSystemTts(reason: String): Boolean {
            Log.d("BookReaderPlayback", "fallback reason=$reason sessionId=$sessionId")
            if (sessionId != playbackSessionId) {
                Log.d("BookReaderPlayback", "discard stale fallback sessionId=$sessionId current=$playbackSessionId")
                return false
            }
            if (!isTtsReady) {
                isPlaying = false
                activePlaybackEngineName = BookPlaybackEngine.NONE.name
                showVoicePackageSheet = true
                Toast.makeText(context, "请先安装或启用系统语音", Toast.LENGTH_SHORT).show()
                return false
            }
            val result = ttsController?.speakSentence(
                text = textToSpeak.orEmpty(),
                speechRate = speechRate,
                pitch = pitch
            )
            return if (result?.success == true) {
                activePlaybackEngineName = BookPlaybackEngine.SYSTEM_TTS.name
                isPlaying = true
                saveProgress(targetParagraphIndex)
                true
            } else {
                isPlaying = false
                activePlaybackEngineName = BookPlaybackEngine.NONE.name
                ttsError = result?.message ?: "系统语音不可用，请安装或启用系统语音引擎后重试"
                showVoicePackageSheet = true
                Toast.makeText(context, "请先安装或启用系统语音", Toast.LENGTH_SHORT).show()
                false
            }
        }

        coroutineScope.launch {
            val player = StreamingPcmAudioPlayer()
            aishell3Player = player
            var playbackDurationMs = 0L
            var playbackStartedAtMs = 0L
            var playbackStarted = false
            var chunkIndex = 0

            val result = aishell3TtsEngine.speak(
                text = textToSpeak.orEmpty(),
                params = StreamingTtsParams(
                    voiceId = "aishell3-speaker-10",
                    speed = speechRate.coerceIn(0.5f, 2.0f),
                    pitch = pitch,
                    volume = 1f
                ),
                onStart = {
                    Log.d("BookReaderPlayback", "aishell3 onStart sessionId=$sessionId")
                },
                onChunk = onChunk@ { chunk ->
                    val isCurrent = withContext(Dispatchers.Main) {
                        sessionId == playbackSessionId
                    }
                    if (!isCurrent) {
                        Log.d("BookReaderPlayback", "discard stale session sessionId=$sessionId current=$playbackSessionId")
                        return@onChunk
                    }

                    Log.d(
                        "BookReaderPlayback",
                        "sessionId=$sessionId segmentIndex=$chunkIndex chunkBytes=${chunk.data.size} " +
                            "sampleRate=${chunk.format.sampleRate}"
                    )

                    if (!playbackStarted) {
                        val startResult = player.start(chunk.format)
                        if (startResult.isFailure) {
                            val message = startResult.exceptionOrNull()?.message ?: "AudioTrack 启动失败"
                            throw IllegalStateException(message)
                        }
                        playbackStarted = true
                        playbackStartedAtMs = System.currentTimeMillis()
                        withContext(Dispatchers.Main) {
                            if (sessionId == playbackSessionId) {
                                activePlaybackEngineName = BookPlaybackEngine.AISHELL3.name
                                isAishell3Paused = false
                                isPlaying = true
                                saveProgress(targetParagraphIndex)
                            }
                        }
                    }

                    val writeResult = player.write(chunk)
                    if (writeResult.isFailure) {
                        val message = writeResult.exceptionOrNull()?.message ?: "AudioTrack 写入失败"
                        throw IllegalStateException(message)
                    }
                    playbackDurationMs += chunk.data.size * 1000L / (chunk.format.sampleRate * 2L)
                    chunkIndex += 1
                },
                onDone = {
                    Log.d(
                        "BookReaderPlayback",
                        "aishell3 onDone sessionId=$sessionId playbackDurationMs=$playbackDurationMs"
                    )
                },
                onError = { error ->
                    Log.e("BookReaderPlayback", "aishell3 error sessionId=$sessionId error=$error")
                }
            )

            if (playbackStarted) {
                val elapsedSinceStart = (System.currentTimeMillis() - playbackStartedAtMs).coerceAtLeast(0L)
                var remainingWaitMs = (playbackDurationMs - elapsedSinceStart + 160L).coerceIn(0L, 2_000L)
                Log.d(
                    "BookReaderPlayback",
                    "sessionId=$sessionId remainingPlaybackWaitMs=$remainingWaitMs " +
                        "playbackDurationMs=$playbackDurationMs elapsedSinceStart=$elapsedSinceStart"
                )
                while (remainingWaitMs > 0L && sessionId == playbackSessionId) {
                    if (isAishell3Paused) {
                        delay(80L)
                    } else {
                        val step = remainingWaitMs.coerceAtMost(80L)
                        delay(step)
                        remainingWaitMs -= step
                    }
                }
            }
            player.release()
            if (aishell3Player === player) {
                aishell3Player = null
            }

            withContext(Dispatchers.Main) {
                if (sessionId != playbackSessionId) {
                    Log.d("BookReaderPlayback", "discard stale session sessionId=$sessionId current=$playbackSessionId")
                    return@withContext
                }
                when (result) {
                    StreamingTtsResult.Success -> {
                        continuePlaybackAfterCurrentTarget(sessionId)
                    }
                    StreamingTtsResult.Stopped -> {
                        isPlaying = false
                        activePlaybackEngineName = BookPlaybackEngine.NONE.name
                    }
                    is StreamingTtsResult.Error -> {
                        val didFallback = fallbackToSystemTts(result.message)
                        if (!didFallback) {
                            Log.d("BookReaderPlayback", "all engines unavailable sessionId=$sessionId")
                        }
                    }
                }
            }
        }
    }

    fun pauseReading() {
        if (!USE_FORMAL_BOOK_PLAYBACK_CONTROLLER && LOG_FORMAL_BOOK_PLAYBACK_DRY_RUN) {
            Log.d(FORMAL_BOOK_PLAYBACK_DRY_RUN_TAG, "pause")
        }
        if (USE_FORMAL_BOOK_PLAYBACK_CONTROLLER) {
            formalAutoAdvanceEnabled = false
            formalAutoAdvanceCoordinator.onPauseRequested()
            formalPlaybackController.pause()
            Log.d(FORMAL_BOOK_PLAYBACK_TAG, "reader pause formal")
            isPlaying = false
            saveProgress(currentParagraphIndex)
            return
        }
        if (activePlaybackEngineName == BookPlaybackEngine.AISHELL3.name && aishell3Player != null) {
            Log.d("BookReaderPlayback", "pause aishell3 sessionId=$playbackSessionId")
            aishell3Player?.pause()
            isAishell3Paused = true
            isPlaying = false
            saveProgress(currentParagraphIndex)
            return
        }
        playbackSessionId += 1
        stopAishell3Playback()
        ttsController?.pause()
        ttsController?.stop()
        activePlaybackEngineName = BookPlaybackEngine.NONE.name
        isPlaying = false
        saveProgress(currentParagraphIndex)
    }

    fun stopReading() {
        stopCurrentPlayback(reason = "stop_button", invalidateSession = true)
        currentParagraphIndex = 0
        currentSentenceIndexInParagraph = 0
        currentReadingTargetName = BookReadingTarget.SENTENCE.name
        isPlaying = false
        pendingStopAfterChapter = false
        pendingStopChapterIndex = -1
        saveProgress(0)
    }

    fun jumpToParagraph(index: Int, autoPlay: Boolean = isPlaying) {
        if (paragraphs.isEmpty()) return
        val nextIndex = index.coerceIn(0, paragraphs.lastIndex)
        stopCurrentPlayback(reason = "jump_to_paragraph", invalidateSession = true)
        currentParagraphIndex = nextIndex
        currentSentenceIndexInParagraph = 0
        currentReadingTargetName = BookReadingTarget.SENTENCE.name
        saveProgress(nextIndex)
        if (autoPlay) {
            isPlaying = false
            speakCurrentSentence()
        }
    }

    fun jumpToChapter(offset: Int) {
        if (paragraphs.isEmpty() || chapters.isEmpty()) {
            Toast.makeText(context, "未识别到章节", Toast.LENGTH_SHORT).show()
            return
        }
        val currentIndex = chapters.indexOfLast { it.paragraphIndex <= currentParagraphIndex }
            .coerceAtLeast(0)
        val targetIndex = currentIndex + offset
        val source = if (offset > 0) "reader_next_chapter" else "reader_previous_chapter"
        if (!USE_FORMAL_BOOK_PLAYBACK_CONTROLLER && LOG_FORMAL_BOOK_PLAYBACK_DRY_RUN && targetIndex in chapters.indices) {
            val dryRunTarget = buildFormalBookReaderPlaybackTargetForChapter(
                bookFile = bookFile,
                chapters = chapters,
                paragraphs = paragraphs,
                chapterIndex = targetIndex,
                dryRunSource = source
            )
            logFormalDryRunTarget(source, dryRunTarget, resumeIfPlaying = isPlaying)
        } else if (!USE_FORMAL_BOOK_PLAYBACK_CONTROLLER && LOG_FORMAL_BOOK_PLAYBACK_DRY_RUN) {
            val reason = if (targetIndex < 0) "no_previous_chapter" else "no_next_chapter"
            Log.d(FORMAL_BOOK_PLAYBACK_DRY_RUN_TAG, "target skipped reason=$reason source=$source")
        }
        if (USE_FORMAL_BOOK_PLAYBACK_CONTROLLER) {
            if (targetIndex < 0) {
                Log.d(FORMAL_BOOK_PLAYBACK_TAG, "reader chapter jump no-op reason=no_previous_chapter")
                return
            }
            if (targetIndex > chapters.lastIndex) {
                Log.d(FORMAL_BOOK_PLAYBACK_TAG, "reader chapter jump no-op reason=no_next_chapter")
                return
            }
            val target = buildFormalBookReaderPlaybackTargetForChapter(
                bookFile = bookFile,
                chapters = chapters,
                paragraphs = paragraphs,
                chapterIndex = targetIndex
            )
            if (target == null) {
                Log.d(FORMAL_BOOK_PLAYBACK_TAG, "reader chapter jump fallback to legacy reason=missing_target source=$source")
                return
            }
            val resumeIfPlaying = isPlaying
            currentParagraphIndex = target.paragraphIndex
            currentSentenceIndexInParagraph = target.sentenceIndex
            currentReadingTargetName = BookReadingTarget.SENTENCE.name
            saveProgress(currentParagraphIndex)
            formalAutoAdvanceEnabled = resumeIfPlaying
            if (resumeIfPlaying) {
                formalAutoAdvanceCoordinator.onPlayRequested()
                isPlaying = true
                activePlaybackEngineName = BookPlaybackEngine.FASTSPEECH2.name
            } else {
                formalAutoAdvanceCoordinator.onPauseRequested()
            }
            val direction = if (offset > 0) "next" else "previous"
            Log.d(
                FORMAL_BOOK_PLAYBACK_TAG,
                "reader $direction chapter via controller chapterIndex=${target.chapterIndex} " +
                    "paragraphIndex=${target.paragraphIndex} sentenceIndex=${target.sentenceIndex} " +
                    "resumeIfPlaying=$resumeIfPlaying"
            )
            coroutineScope.launch {
                formalPlaybackController.jumpToChapter(target, resumeIfPlaying = resumeIfPlaying)
            }
            return
        }
        if (targetIndex < 0) {
            Toast.makeText(context, "已经是第一章", Toast.LENGTH_SHORT).show()
            return
        }
        if (targetIndex > chapters.lastIndex) {
            Toast.makeText(context, "已经是最后一章", Toast.LENGTH_SHORT).show()
            return
        }
        val wasPlaying = isPlaying
        pendingStopAfterChapter = false
        pendingStopChapterIndex = -1
        stopCurrentPlayback(reason = "jump_to_chapter", invalidateSession = true)
        currentParagraphIndex = chapters[targetIndex].paragraphIndex.coerceIn(0, paragraphs.lastIndex)
        currentSentenceIndexInParagraph = 0
        currentReadingTargetName = BookReadingTarget.CHAPTER_TITLE.name
        saveProgress(currentParagraphIndex)
        if (wasPlaying) {
            isPlaying = false
            speakCurrentSentence()
        }
    }

    fun jumpToSentence(
        sentence: ReaderSentence,
        autoPlay: Boolean = isPlaying,
        source: String = "sentence_tap",
    ) {
        if (paragraphs.isEmpty()) return
        val nextIndex = sentence.paragraphIndex.coerceIn(0, paragraphs.lastIndex)
        val formalJumpTarget = if (USE_FORMAL_BOOK_PLAYBACK_CONTROLLER || LOG_FORMAL_BOOK_PLAYBACK_DRY_RUN) {
            buildFormalBookReaderPlaybackTarget(
                bookFile = bookFile,
                chapters = chapters,
                paragraphs = paragraphs,
                paragraphIndex = nextIndex,
                sentenceIndex = sentence.sentenceIndexInParagraph,
                fallbackSentenceText = sentence.text,
                dryRunSource = source
            )
        } else {
            null
        }
        if (!USE_FORMAL_BOOK_PLAYBACK_CONTROLLER && LOG_FORMAL_BOOK_PLAYBACK_DRY_RUN) {
            logFormalDryRunTarget(source, formalJumpTarget, resumeIfPlaying = autoPlay)
        }
        if (USE_FORMAL_BOOK_PLAYBACK_CONTROLLER) {
            val target = formalJumpTarget
            if (target == null) {
                Log.d(FORMAL_BOOK_PLAYBACK_TAG, "reader jump fallback to legacy reason=missing_target source=$source")
            } else {
                currentParagraphIndex = nextIndex
                currentSentenceIndexInParagraph = sentence.sentenceIndexInParagraph.coerceAtLeast(0)
                currentReadingTargetName = BookReadingTarget.SENTENCE.name
                saveProgress(nextIndex)
                val action = if (source == "seek") "seek" else "sentence tap"
                Log.d(
                    FORMAL_BOOK_PLAYBACK_TAG,
                    "reader $action via controller paragraphIndex=${target.paragraphIndex} " +
                        "sentenceIndex=${target.sentenceIndex} resumeIfPlaying=$autoPlay"
                )
                formalAutoAdvanceEnabled = autoPlay
                if (autoPlay) {
                    formalAutoAdvanceCoordinator.onPlayRequested()
                    isPlaying = true
                    activePlaybackEngineName = BookPlaybackEngine.FASTSPEECH2.name
                } else {
                    formalAutoAdvanceCoordinator.onPauseRequested()
                }
                coroutineScope.launch {
                    if (source == "seek") {
                        formalPlaybackController.seekTo(target, resumeIfPlaying = autoPlay)
                    } else {
                        formalPlaybackController.jumpToSentence(target, resumeIfPlaying = autoPlay)
                    }
                }
                return
            }
        }
        stopCurrentPlayback(reason = "jump_to_sentence", invalidateSession = true)
        currentParagraphIndex = nextIndex
        currentSentenceIndexInParagraph = sentence.sentenceIndexInParagraph.coerceAtLeast(0)
        currentReadingTargetName = BookReadingTarget.SENTENCE.name
        saveProgress(nextIndex)
        if (autoPlay) {
            isPlaying = false
            speakCurrentSentence()
        }
    }

    requestSpeakCurrent = { speakCurrentSentence() }

    LaunchedEffect(bookFile?.uri, usesEntryReadyState) {
        if (usesEntryReadyState) {
            val firstFrame = entryReadyFirstFrame
            if (firstFrame != null) {
                paragraphs = firstFrame.paragraphs
                currentParagraphIndex = firstFrame.firstFrameTarget.paragraphIndex
                currentSentenceIndexInParagraph = firstFrame.firstFrameTarget.sentenceIndex
                currentReadingTargetName = BookReadingTarget.SENTENCE.name
            }
            isLoading = false
            loadError = null
            hasSettledInitialReaderPosition = true
            hasCompletedSnapshotToRealHandoff = true
            Log.d(
                FORMAL_BOOK_PLAYBACK_TAG,
                "phase5c ready screen path legacyRestoreLoadSkipped=true restorePromptReachable=false"
            )
            return@LaunchedEffect
        }
        if (bookFile == null) {
            isLoading = false
            loadError = "未选择小说文件"
            paragraphs = emptyList()
            return@LaunchedEffect
        }
        isLoading = true
        loadError = null
        hasSettledInitialReaderPosition = initialReadStateCache == null
        restorePositionStartedAtMillis = System.currentTimeMillis()
        Log.d(
            FORMAL_BOOK_PLAYBACK_TAG,
            "restore position start bookId=${bookFile.uri} chapterIndex=${initialReadStateCache?.lastChapterTitle.orEmpty().take(24)} " +
                "paragraphIndex=${initialReadStateCache?.lastParagraphIndex ?: initialParagraphIndex} " +
                "sentenceIndex=${initialReadStateCache?.lastSentenceIndexInParagraph ?: 0}"
        )
        hasCompletedSnapshotToRealHandoff = !hasInitialReaderSnapshot
        isPlaying = false
        stopCurrentPlayback(reason = "book_changed", invalidateSession = true)
        val result = withContext(Dispatchers.IO) {
            TxtBookReader.readParagraphs(context.applicationContext, bookFile.uri)
        }
        result
            .onSuccess { loaded ->
                paragraphs = loaded
                Log.d(FORMAL_BOOK_PLAYBACK_TAG, "restore position content ready paragraphs=${loaded.size}")
                Log.d(
                    FORMAL_BOOK_PLAYBACK_TAG,
                    "perf full_chapter_load done elapsedMs=${(System.currentTimeMillis() - restorePositionStartedAtMillis).coerceAtLeast(0L)}"
                )
                Log.d(
                    FORMAL_BOOK_PLAYBACK_TAG,
                    "perf paragraph_parse done elapsedMs=${(System.currentTimeMillis() - restorePositionStartedAtMillis).coerceAtLeast(0L)}"
                )
                currentParagraphIndex = currentParagraphIndex.coerceIn(
                    0,
                    (loaded.size - 1).coerceAtLeast(0)
                )
                currentSentenceIndexInParagraph = currentSentenceIndexInParagraph.coerceAtLeast(0)
                if (loaded.isEmpty()) {
                    loadError = "小说内容为空"
                }
            }
            .onFailure {
                paragraphs = emptyList()
                loadError = "小说文件无法读取，请重新导入"
                Toast.makeText(context, "小说读取失败", Toast.LENGTH_SHORT).show()
            }
        isLoading = false
    }

    LaunchedEffect(speechRate, pitch) {
        if (isPlaying && paragraphs.isNotEmpty()) {
            speakCurrentSentence()
        }
    }

    LaunchedEffect(isTtsReady, selectedVoiceName, ttsRetryKey) {
        if (isTtsReady) {
            selectedVoiceName?.let { ttsController?.selectVoice(it) }
            ttsVoices = ttsController?.getAvailableVoices().orEmpty()
        }
    }

    LaunchedEffect(
        bookFile?.uri,
        paragraphs.size,
        currentParagraphIndex,
        currentSentenceIndexInParagraph,
        cachedReadState,
        speechRate,
        chapters,
    ) {
        if (bookFile != null && paragraphs.isNotEmpty()) {
            if (!shouldRunBookReaderEntryReadyShadow(usesEntryReadyState = usesEntryReadyState)) {
                Log.d(
                    FORMAL_BOOK_PLAYBACK_TAG,
                    "phase5c ready shadow diagnostics skipped usesEntryReadyState=true"
                )
                return@LaunchedEffect
            }
            val shadowChapterIndex = currentChapterIndexFor(currentParagraphIndex)
            val shadowChapter = chapters.getOrNull(shadowChapterIndex)
            val shadowChapterStart = shadowChapter?.paragraphIndex ?: 0
            val shadowChapterEndExclusive = chapters
                .getOrNull(shadowChapterIndex + 1)
                ?.paragraphIndex
                ?.coerceIn(shadowChapterStart, paragraphs.size)
                ?: paragraphs.size
            Log.d(
                FORMAL_BOOK_PLAYBACK_TAG,
                "real reader screen entered bookId=${bookFile.uri} chapterIndex=$shadowChapterIndex"
            )
            Log.d(
                FORMAL_BOOK_PLAYBACK_TAG,
                "real reader controller identity=${System.identityHashCode(formalPlaybackController)}"
            )
            logBookReaderEntryReadyShadow(
                bookId = bookFile.uri,
                bookTitle = bookFile.displayTitle(),
                paragraphs = paragraphs,
                restoreSnapshot = cachedReadState?.toEntryRestoreSnapshotInput(),
                chapterIndex = shadowChapterIndex,
                chapterTitle = shadowChapter?.title ?: "姝ｆ枃",
                chapterStartIndex = shadowChapterStart,
                chapterEndExclusive = shadowChapterEndExclusive,
                speechRate = speechRate,
                currentParagraphIndex = currentParagraphIndex,
                currentSentenceIndex = currentSentenceIndexInParagraph,
                currentChapterIndex = shadowChapterIndex,
                currentListenedTimeLabel = cachedReadState?.cachedListenedTimeLabel.orEmpty(),
                currentRemainingTimeLabel = cachedReadState?.cachedRemainingTimeLabel.orEmpty(),
            )
        }
    }

    LaunchedEffect(sleepTimerEnabled, sleepTimerEndAtMillis, sleepTimerStopMode) {
        while (sleepTimerEnabled) {
            val endAt = sleepTimerEndAtMillis
            if (endAt == null) {
                clearSleepTimer()
                break
            }
            val remaining = (endAt - System.currentTimeMillis()).coerceAtLeast(0L)
            sleepTimerRemainingMillis = remaining
            if (remaining <= 0L) {
                if (sleepTimerStopMode == SleepTimerStopMode.AFTER_CURRENT_CHAPTER && isPlaying) {
                    val currentChapterIndex = chapters.indexOfLast {
                        it.paragraphIndex <= currentParagraphIndex
                    }
                    if (currentChapterIndex >= 0) {
                        pendingStopAfterChapter = true
                        pendingStopChapterIndex = currentChapterIndex
                        sleepTimerRemainingMillis = 0L
                    } else {
                        stopForSleepTimer(showToast = true)
                    }
                } else {
                    stopForSleepTimer(showToast = isPlaying)
                }
                break
            }
            delay(1_000L)
        }
    }

    fun shouldShowCachedReaderWhileLoading(): Boolean {
        val state = cachedReadState ?: return false
        return state.bookUri == bookFile?.uri &&
            state.hasViewportSnapshot() &&
            state.updatedAt > 0L
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF080B12)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFF080B12))
                .padding(vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            BookListenTopBar(
                title = bookFile?.displayTitle().orEmpty().ifBlank { "未选择小说文件" },
                onBack = {
                    if (paragraphs.isNotEmpty()) saveProgress(currentParagraphIndex)
                    stopCurrentPlayback(reason = "back", invalidateSession = true)
                    isPlaying = false
                    onBack()
                },
                onMenuAction = { action ->
                    when (action) {
                        "语音设置" -> openIntentSafely(
                            Intent("com.android.settings.TTS_SETTINGS"),
                            "无法打开语音设置"
                        )
                        "重新检测 TTS" -> restartTtsCheck()
                    }
                }
            )

            when {
                !entryReadyScreenPlan.skipStableSnapshot &&
                    instantEntryRenderMode == BookReaderRestorePositionGate.INITIAL_RENDER_STABLE_UI_SNAPSHOT &&
                    shouldShowCachedReaderWhileLoading() &&
                    cachedReadState?.cachedVisibleText.orEmpty().isNotEmpty() -> {
                    val rawCachedState = cachedReadState
                    val cachedState = remember(rawCachedState, speechRate) {
                        rawCachedState?.let { state -> backfillStableUiSnapshot(state, speechRate) ?: state }
                    }
                    LaunchedEffect(cachedState, rawCachedState) {
                        if (cachedState != null && cachedState != rawCachedState) {
                            cachedReadState = cachedState
                            saveBookReadStateCache(context.applicationContext, cachedState)
                            Log.d(
                                FORMAL_BOOK_PLAYBACK_TAG,
                                "stable ui snapshot backfilled source=${cachedState.stableUiSnapshotSource()} " +
                                    "progress=${cachedState.cachedProgressValue} progressMax=${cachedState.cachedProgressMaxValue} " +
                                    "listened=${cachedState.cachedListenedTimeLabel} remaining=${cachedState.cachedRemainingTimeLabel}"
                            )
                        } else if (cachedState != null) {
                            Log.d(
                                FORMAL_BOOK_PLAYBACK_TAG,
                                "stable ui snapshot source=${cachedState.stableUiSnapshotSource()} " +
                                    "progress=${cachedState.cachedProgressValue} progressMax=${cachedState.cachedProgressMaxValue} " +
                                    "listened=${cachedState.cachedListenedTimeLabel} remaining=${cachedState.cachedRemainingTimeLabel}"
                            )
                        }
                    }
                    val cachedTarget = remember(cachedState?.lastReadingTargetName) {
                        runCatching {
                            BookReadingTarget.valueOf(
                                cachedState?.lastReadingTargetName ?: BookReadingTarget.SENTENCE.name
                            )
                        }.getOrDefault(BookReadingTarget.SENTENCE)
                    }
                    val cachedSentences = remember(cachedState) {
                        val state = cachedState
                        state?.cachedVisibleText
                            .orEmpty()
                            .mapIndexedNotNull { index, text ->
                                val trimmed = text.trim()
                                if (trimmed.isBlank()) {
                                    null
                                } else {
                                    ReaderSentence(
                                        text = trimmed,
                                        paragraphIndex = state?.cachedVisibleParagraphIndexes?.getOrNull(index)
                                            ?: (state?.lastParagraphIndex ?: 0),
                                        sentenceIndexInParagraph = state?.cachedVisibleSentenceIndexes?.getOrNull(index)
                                            ?: (state?.lastSentenceIndexInParagraph ?: 0),
                                        chapterSentenceIndex = state?.cachedVisibleChapterSentenceIndexes?.getOrNull(index)
                                            ?: index
                                    )
                                }
                            }
                    }
                    val cachedIndex = cachedState?.let { state ->
                        cachedSentences
                            .indexOfFirst { sentence ->
                                sentence.paragraphIndex == state.lastParagraphIndex &&
                                    sentence.sentenceIndexInParagraph == state.lastSentenceIndexInParagraph
                            }
                            .let { index ->
                                if (index >= 0) index else cachedSentences.indexOfFirst { it.paragraphIndex == state.lastParagraphIndex }
                            }
                            .coerceAtLeast(0)
                    } ?: 0
                    val cachedAbsoluteTimeLabels = estimateAbsoluteSnapshotTimeLabels(
                        visibleText = cachedState?.cachedVisibleText.orEmpty(),
                        chapterSentenceIndex = cachedSentences.getOrNull(cachedIndex)?.chapterSentenceIndex
                            ?: cachedState?.lastChapterSentenceIndex,
                        totalChapterSentenceCount = cachedState?.cachedChapterSentenceCount,
                        speechRate = speechRate,
                    )
                    val cachedListenedTimeLabel = cachedState?.cachedListenedTimeLabel
                        ?.takeIf { it.isNotBlank() }
                        ?: cachedAbsoluteTimeLabels?.first
                        ?: "00:00"
                    val cachedRemainingTimeLabel = cachedState?.cachedRemainingTimeLabel
                        ?.takeIf { it.isNotBlank() }
                        ?: cachedAbsoluteTimeLabels?.second
                        ?: "00:00"
                    val cachedProgressTimeState = BookReaderRestorePositionGate.resolveProgressTimeForSnapshotOrPendingHandoff(
                        savedProgressValue = cachedState?.cachedProgressValue?.takeIf { it >= 0f },
                        savedProgressMaxValue = cachedState?.cachedProgressMaxValue?.takeIf { it > 0f },
                        savedListenedTimeLabel = cachedState?.cachedListenedTimeLabel?.ifBlank { cachedListenedTimeLabel },
                        savedRemainingTimeLabel = cachedState?.cachedRemainingTimeLabel?.ifBlank { cachedRemainingTimeLabel },
                        chapterSentenceIndex = cachedSentences.getOrNull(cachedIndex)?.chapterSentenceIndex
                            ?: cachedState?.lastChapterSentenceIndex,
                        totalChapterSentenceCount = cachedState?.cachedChapterSentenceCount,
                    )
                    val cachedProgressState = cachedProgressTimeState.progress
                    LaunchedEffect(cachedProgressTimeState) {
                        val modeName = cachedProgressTimeState.mode.name.lowercase()
                        Log.d(
                            FORMAL_BOOK_PLAYBACK_TAG,
                            "progress resolver source=canonical_restore mode=$modeName snapshotSource=${cachedState?.stableUiSnapshotSource()} " +
                                "chapterSentenceIndex=${cachedProgressState?.chapterSentenceIndex} total=${cachedProgressState?.totalChapterSentenceCount} " +
                                "progress=${cachedProgressState?.value} max=${cachedProgressState?.maxValue} " +
                                "listened=${cachedProgressTimeState.listenedTimeLabel ?: cachedListenedTimeLabel} " +
                                "remaining=${cachedProgressTimeState.remainingTimeLabel ?: cachedRemainingTimeLabel} " +
                                "savedProgress=${cachedState?.cachedProgressValue} savedListened=${cachedState?.cachedListenedTimeLabel.orEmpty()} " +
                                "delta=${cachedProgressTimeState.normalizedDelta}"
                        )
                        if ((cachedProgressTimeState.normalizedDelta ?: 0f) > 0.02f) {
                            Log.d(
                                FORMAL_BOOK_PLAYBACK_TAG,
                                "progress resolver conflict saved=${cachedState?.cachedListenedTimeLabel.orEmpty()} " +
                                    "real=${cachedProgressTimeState.listenedTimeLabel ?: cachedListenedTimeLabel} action=use_canonical"
                            )
                        }
                    }
                    BookListenContent(
                        chapterTitle = cachedState?.lastChapterTitle.orEmpty().ifBlank { "正在恢复上次阅读" },
                        chapterSentences = cachedSentences,
                        currentChapterSentenceIndex = cachedIndex,
                        isChapterTitleCurrent = cachedTarget == BookReadingTarget.CHAPTER_TITLE,
                        currentParagraphIndex = cachedSentences
                            .getOrNull(cachedIndex)
                            ?.paragraphIndex
                            ?: (cachedState?.lastParagraphIndex ?: currentParagraphIndex),
                        currentSentenceIndexInParagraph = cachedSentences
                            .getOrNull(cachedIndex)
                            ?.sentenceIndexInParagraph
                            ?: currentSentenceIndexInParagraph,
                        listenedTimeLabel = cachedListenedTimeLabel,
                        remainingTimeLabel = cachedRemainingTimeLabel,
                        listenedTimeLabelOverride = cachedProgressTimeState.listenedTimeLabel,
                        remainingTimeLabelOverride = cachedProgressTimeState.remainingTimeLabel,
                        progressValueOverride = cachedProgressState?.value,
                        progressMaxValueOverride = cachedProgressState?.maxValue,
                        showReaderProgress = cachedProgressTimeState.shouldShowProgress,
                        progressUserSeekEnabled = false,
                        isPlaying = false,
                        isTtsReady = isTtsReady,
                        isTtsChecking = isTtsChecking,
                        ttsError = ttsError,
                        currentVoiceName = selectedVoiceName,
                        speechRate = speechRate,
                        pitch = pitch,
                        readerFontSizeSp = readerFontSizeSp,
                        onPlayPause = { Toast.makeText(context, "正在恢复小说内容", Toast.LENGTH_SHORT).show() },
                        onStop = { },
                        onPrevious = {
                            Toast.makeText(context, "正在恢复小说内容", Toast.LENGTH_SHORT).show()
                        },
                        onNext = {
                            Toast.makeText(context, "正在恢复小说内容", Toast.LENGTH_SHORT).show()
                        },
                        onSeekSentence = { sentenceIndex ->
                            cachedSentences.getOrNull(sentenceIndex)?.let { sentence ->
                                currentParagraphIndex = sentence.paragraphIndex
                                currentSentenceIndexInParagraph = sentence.sentenceIndexInParagraph
                                currentReadingTargetName = BookReadingTarget.SENTENCE.name
                            }
                        },
                        onSentenceClick = { sentence ->
                            currentParagraphIndex = sentence.paragraphIndex
                            currentSentenceIndexInParagraph = sentence.sentenceIndexInParagraph
                            currentReadingTargetName = BookReadingTarget.SENTENCE.name
                        },
                        onSpeechRateChange = { speechRate = it },
                        onPitchChange = { pitch = it },
                        onReaderFontSizeChange = { readerFontSizeSp = it },
                        onOpenCatalog = { Toast.makeText(context, "正在恢复目录", Toast.LENGTH_SHORT).show() },
                        sleepTimerEnabled = sleepTimerEnabled,
                        onOpenSleepTimer = { showSleepTimerSheet = true },
                        playbackModeLabel = playbackMode.label,
                        onTogglePlaybackMode = {
                            val nextMode = playbackMode.next()
                            playbackModeName = nextMode.name
                            Toast.makeText(context, "已切换为${nextMode.label}", Toast.LENGTH_SHORT).show()
                        },
                        onInstallVoiceData = ::openVoiceDataInstaller,
                        onOpenVoicePackageSettings = { showVoicePackageSheet = true },
                        onOpenVoiceSettings = {
                            openIntentSafely(
                                Intent("com.android.settings.TTS_SETTINGS")
                                    .also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
                                "无法打开语音设置"
                            )
                        },
                        onRetryTts = { restartTtsCheck() },
                        onPositionSettled = {
                            Log.d(
                                FORMAL_BOOK_PLAYBACK_TAG,
                                "restore position snapshot visible waiting_for_full_content"
                            )
                        },
                        showPositioningOverlay = BookReaderRestorePositionGate.shouldHideContent(
                            hasRestorePosition = hasSavedRestorePosition,
                            hasSettledPosition = hasSettledInitialReaderPosition,
                            isLoading = isLoading,
                            hasLoadError = loadError != null,
                            hasSnapshotContent = hasInitialReaderSnapshot,
                        ),
                        instantEntryRenderMode = BookReaderRestorePositionGate.INITIAL_RENDER_STABLE_UI_SNAPSHOT,
                        restoreKey = "snapshot#$restorePositionKey",
                        modifier = Modifier.weight(1f)
                    )
                }

                isLoading && !entryReadyScreenPlan.skipRestorePrompt -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF4D8DFF))
                            Text(
                                text = "正在恢复上次阅读...",
                                color = Color.White.copy(alpha = 0.72f),
                                modifier = Modifier.padding(top = 14.dp)
                            )
                        }
                    }
                }

                loadError != null -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = loadError.orEmpty(),
                            color = Color.White.copy(alpha = 0.76f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    val chapterStartIndex = chapterStartFor(currentParagraphIndex, chapters)
                    val chapterEndExclusive = chapterEndExclusiveFor(
                        currentParagraphIndex,
                        paragraphs.size,
                        chapters
                    ).coerceAtLeast(chapterStartIndex + 1)
                    val chapterTitle = when {
                        chapters.isEmpty() -> "正文"
                        currentChapter == null -> "开头"
                        else -> currentChapter.title
                    }
                    val legacyChapterSentences = remember(
                        paragraphs,
                        chapterStartIndex,
                        chapterEndExclusive,
                        currentChapter?.title
                    ) {
                        buildReaderSentences(
                            paragraphs = paragraphs,
                            startIndex = chapterStartIndex,
                            endExclusive = chapterEndExclusive.coerceAtMost(paragraphs.size),
                            chapterTitle = currentChapter?.title
                        )
                    }
                    val chapterSentences = legacyChapterSentences
                    val currentChapterSentenceIndex = chapterSentences
                        .indexOfFirst {
                            it.paragraphIndex == currentParagraphIndex &&
                                it.sentenceIndexInParagraph == currentSentenceIndexInParagraph
                        }
                        .let { exactIndex ->
                            if (exactIndex >= 0) {
                                exactIndex
                            } else {
                                chapterSentences.indexOfFirst { it.paragraphIndex == currentParagraphIndex }
                            }
                        }
                        .let { fallbackIndex -> fallbackIndex.coerceAtLeast(0) }
                        .coerceAtMost((chapterSentences.size - 1).coerceAtLeast(0))
                    val readyInitialLocalTargetIndex = remember(entryReadyState, currentChapter?.title, chapterSentences.size) {
                        currentChapterSentenceIndex
                    }
                    val readyUiListPlan = entryReadyFirstFrame?.uiListPlan(
                        readerListSize = chapterSentences.size,
                        targetLocalIndex = readyInitialLocalTargetIndex,
                        interactionState = entryReadyInteractionState,
                    )
                    LaunchedEffect(readyUiListPlan, currentChapter?.title) {
                        val plan = readyUiListPlan ?: return@LaunchedEffect
                        Log.d(
                            FORMAL_BOOK_PLAYBACK_TAG,
                            "phase5c ready screen path reader list plan " +
                                "readyListSize=${plan.readerListSize} " +
                                "totalReadySentenceCount=${plan.totalReadySentenceCount} " +
                                "targetChapterIndex=${currentChapterIndexFor(currentParagraphIndex)} " +
                                "targetSentenceGlobalIndex=${plan.targetGlobalIndex} " +
                                "targetSentenceLocalIndex=${plan.targetLocalIndex} " +
                                "firstFrameEqualsPlayTarget=${plan.firstFrameEqualsPlayTarget} " +
                                "readyWindowBuildSkippedFullList=${!plan.exposesFullReadyList}"
                        )
                    }
                    LaunchedEffect(readyUiListPlan?.shouldApplyInitialSeed) {
                        if (readyUiListPlan?.shouldApplyInitialSeed == true) {
                            entryReadyInteractionState.markInitialSeedConsumed()
                            Log.d(
                                FORMAL_BOOK_PLAYBACK_TAG,
                                "phase5c ready initial target consumed " +
                                    "targetSentenceLocalIndex=${readyUiListPlan.targetLocalIndex}"
                            )
                        }
                    }
                    val listenedTimeLabel = estimateSentenceTimeLabel(
                        sentences = chapterSentences,
                        fromIndex = 0,
                        toIndex = currentChapterSentenceIndex,
                        speechRate = speechRate
                    )
                    val remainingTimeLabel = estimateSentenceTimeLabel(
                        sentences = chapterSentences,
                        fromIndex = currentChapterSentenceIndex,
                        toIndex = chapterSentences.size,
                        speechRate = speechRate
                    )
                    val snapshotFirstVisibleParagraphIndex = cachedReadState?.firstVisibleParagraphIndex()
                    val snapshotFirstVisibleSentenceIndex = cachedReadState?.firstVisibleSentenceIndex()
                    val snapshotFirstVisibleChapterSentenceIndex = cachedReadState?.firstVisibleChapterSentenceIndex()
                    val snapshotMappedRealIndex = findSnapshotRealContentInitialIndex(
                        cachedReadState = cachedReadState,
                        chapterSentences = chapterSentences,
                        hasSavedRestorePosition = hasSavedRestorePosition,
                        hasInitialReaderSnapshot = hasInitialReaderSnapshot,
                        hasSettledInitialReaderPosition = hasSettledInitialReaderPosition
                    )
                    val pendingSnapshotHandoff = BookReaderRestorePositionGate.shouldBlockRealContentForSnapshotHandoff(
                        hasSnapshotContent = hasInitialReaderSnapshot,
                        hasCompletedSnapshotHandoff = hasCompletedSnapshotToRealHandoff,
                        hasLoadError = loadError != null
                    )
                    LaunchedEffect(
                        restorePositionKey,
                        pendingSnapshotHandoff,
                        snapshotMappedRealIndex,
                        snapshotFirstVisibleParagraphIndex,
                        snapshotFirstVisibleSentenceIndex,
                        snapshotFirstVisibleChapterSentenceIndex
                    ) {
                        if (pendingSnapshotHandoff) {
                            Log.d(
                                FORMAL_BOOK_PLAYBACK_TAG,
                                "instant entry blocked real_content reason=pending_snapshot_handoff " +
                                    "snapshotFirstVisibleParagraphIndex=$snapshotFirstVisibleParagraphIndex " +
                                    "snapshotFirstVisibleSentenceIndex=$snapshotFirstVisibleSentenceIndex " +
                                    "snapshotFirstVisibleChapterSentenceIndex=$snapshotFirstVisibleChapterSentenceIndex " +
                                    "realIndex=${snapshotMappedRealIndex ?: -1}"
                            )
                        }
                    }
                    fun saveViewportReadState(
                        firstVisibleItemIndex: Int,
                        firstVisibleItemScrollOffset: Int,
                        visibleSentences: List<ReaderSentence>,
                        reason: String
                    ) {
                        if (!entryReadyScreenPlan.allowLegacyViewportCacheWrite) {
                            Log.d(
                                FORMAL_BOOK_PLAYBACK_TAG,
                                "phase5c ready screen path legacy viewport cache skipped reason=$reason " +
                                    "firstVisible=$firstVisibleItemIndex offset=$firstVisibleItemScrollOffset"
                            )
                            return
                        }
                        val snapshotSentences = visibleSentences
                            .filter { it.text.isNotBlank() }
                            .take(32)
                        if (snapshotSentences.isEmpty()) return
                        val state = BookReadStateCache(
                            bookUri = bookFile?.uri.orEmpty(),
                            bookTitle = bookFile?.displayTitle().orEmpty(),
                            lastParagraphIndex = currentParagraphIndex,
                            lastSentenceIndexInParagraph = currentSentenceIndexInParagraph,
                            lastChapterSentenceIndex = currentChapterSentenceIndex,
                            lastReadingTargetName = currentReadingTarget.name,
                            lastChapterTitle = chapterTitle,
                            cachedVisibleText = snapshotSentences.map { it.text },
                            cachedVisibleParagraphIndexes = snapshotSentences.map { it.paragraphIndex },
                            cachedVisibleSentenceIndexes = snapshotSentences.map { it.sentenceIndexInParagraph },
                            cachedVisibleChapterSentenceIndexes = snapshotSentences.map { it.chapterSentenceIndex },
                            cachedChapterSentenceCount = chapterSentences.size,
                            cachedProgressValue = currentChapterSentenceIndex.coerceIn(0, (chapterSentences.size - 1).coerceAtLeast(0)).toFloat(),
                            cachedProgressMaxValue = (chapterSentences.size - 1).coerceAtLeast(1).toFloat(),
                            cachedListenedTimeLabel = listenedTimeLabel,
                            cachedRemainingTimeLabel = remainingTimeLabel,
                            viewportFirstVisibleItemIndex = firstVisibleItemIndex.coerceAtLeast(0),
                            viewportFirstVisibleItemScrollOffset = firstVisibleItemScrollOffset.coerceAtLeast(0),
                            updatedAt = System.currentTimeMillis()
                        )
                        cachedReadState = state
                        saveBookReadStateCache(context.applicationContext, state)
                        Log.d(
                            FORMAL_BOOK_PLAYBACK_TAG,
                            "stable ui snapshot saved reason=$reason firstVisible=${state.viewportFirstVisibleItemIndex} " +
                                "offset=${state.viewportFirstVisibleItemScrollOffset} count=${state.cachedVisibleText.size} " +
                                "targetParagraph=$currentParagraphIndex targetSentence=$currentSentenceIndexInParagraph " +
                                "progress=${state.cachedProgressValue} progressMax=${state.cachedProgressMaxValue} " +
                                "listened=${state.cachedListenedTimeLabel} remaining=${state.cachedRemainingTimeLabel}"
                        )
                    }
                    if (!usesEntryReadyState) {
                        LaunchedEffect(
                            bookFile?.uri,
                            chapterTitle,
                            currentParagraphIndex,
                            currentSentenceIndexInParagraph,
                            currentChapterSentenceIndex,
                            chapterSentences
                        ) {
                            val firstSentenceIndex = (currentChapterSentenceIndex - 3).coerceAtLeast(0)
                            saveViewportReadState(
                                firstVisibleItemIndex = if (firstSentenceIndex == 0) 0 else firstSentenceIndex + 1,
                                firstVisibleItemScrollOffset = 0,
                                visibleSentences = chapterSentences.drop(firstSentenceIndex).take(24),
                                reason = "target_neighborhood"
                            )
                        }
                    }
                    val savedProgressTimeState = BookReaderRestorePositionGate.resolveProgressTimeForSnapshotOrPendingHandoff(
                        savedProgressValue = cachedReadState?.cachedProgressValue?.takeIf { it >= 0f },
                        savedProgressMaxValue = cachedReadState?.cachedProgressMaxValue?.takeIf { it > 0f },
                        savedListenedTimeLabel = cachedReadState?.cachedListenedTimeLabel,
                        savedRemainingTimeLabel = cachedReadState?.cachedRemainingTimeLabel,
                        chapterSentenceIndex = cachedReadState?.lastChapterSentenceIndex,
                        totalChapterSentenceCount = cachedReadState?.cachedChapterSentenceCount,
                    )
                    val activeSavedProgressTimeState = if (!usesEntryReadyState && (pendingSnapshotHandoff || !hasSettledInitialReaderPosition)) {
                        savedProgressTimeState
                    } else {
                        null
                    }
                    val realProgressTimeState = BookReaderRestorePositionGate.resolveProgressTimeForRealContent(
                        savedState = activeSavedProgressTimeState,
                        handoffDone = !pendingSnapshotHandoff,
                        chapterSentenceIndex = currentChapterSentenceIndex,
                        totalChapterSentenceCount = chapterSentences.size,
                        realListenedTimeLabel = listenedTimeLabel,
                        realRemainingTimeLabel = remainingTimeLabel,
                    )
                    val realProgressState = realProgressTimeState.progress
                    LaunchedEffect(realProgressTimeState, savedProgressTimeState, pendingSnapshotHandoff) {
                        val modeName = realProgressTimeState.mode.name.lowercase()
                        Log.d(
                            FORMAL_BOOK_PLAYBACK_TAG,
                            "progress resolver source=real_content mode=$modeName progress=${realProgressState?.value} " +
                                "max=${realProgressState?.maxValue} listened=${realProgressTimeState.listenedTimeLabel ?: listenedTimeLabel} " +
                                "remaining=${realProgressTimeState.remainingTimeLabel ?: remainingTimeLabel} " +
                                "reason=${if (pendingSnapshotHandoff) "pending_snapshot_handoff" else "handoff_done"} " +
                                "chapterSentenceIndex=${realProgressState?.chapterSentenceIndex} total=${realProgressState?.totalChapterSentenceCount} " +
                                "delta=${realProgressTimeState.normalizedDelta}"
                        )
                        if (realProgressTimeState.mode == BookReaderRestorePositionGate.ProgressTimeHandoffMode.KEEP_SAVED) {
                            Log.d(
                                FORMAL_BOOK_PLAYBACK_TAG,
                                "progress time handoff keep_saved reason=large_delta delta=${realProgressTimeState.normalizedDelta} " +
                                    "saved=${activeSavedProgressTimeState?.progress?.value} real=${BookReaderRestorePositionGate.resolveRealProgress(currentChapterSentenceIndex, chapterSentences.size).value}"
                            )
                        } else if (!pendingSnapshotHandoff) {
                            Log.d(
                                FORMAL_BOOK_PLAYBACK_TAG,
                                "progress time handoff stable before=${savedProgressTimeState.progress?.value} " +
                                    "after=${realProgressState?.value} delta=${realProgressTimeState.normalizedDelta ?: 0f}"
                            )
                        }
                    }
                    BookListenContent(
                        chapterTitle = chapterTitle,
                        chapterSentences = chapterSentences,
                        currentChapterSentenceIndex = currentChapterSentenceIndex,
                        isChapterTitleCurrent = currentReadingTarget == BookReadingTarget.CHAPTER_TITLE,
                        currentParagraphIndex = currentParagraphIndex,
                        currentSentenceIndexInParagraph = currentSentenceIndexInParagraph,
                        listenedTimeLabel = listenedTimeLabel,
                        remainingTimeLabel = remainingTimeLabel,
                        listenedTimeLabelOverride = realProgressTimeState.listenedTimeLabel,
                        remainingTimeLabelOverride = realProgressTimeState.remainingTimeLabel,
                        progressValueOverride = realProgressState?.value,
                        progressMaxValueOverride = realProgressState?.maxValue,
                        showReaderProgress = realProgressTimeState.shouldShowProgress,
                        progressUserSeekEnabled = realProgressTimeState.mode == BookReaderRestorePositionGate.ProgressTimeHandoffMode.REAL_ABSOLUTE,
                        isPlaying = isPlaying,
                        isTtsReady = isTtsReady,
                        isTtsChecking = isTtsChecking,
                        ttsError = ttsError,
                        currentVoiceName = selectedVoiceName,
                        speechRate = speechRate,
                        pitch = pitch,
                        readerFontSizeSp = readerFontSizeSp,
                        onPlayPause = {
                            if (isPlaying) pauseReading() else speakCurrentSentence()
                        },
                        onStop = { stopReading() },
                        onPrevious = {
                            jumpToChapter(-1)
                        },
                        onNext = {
                            jumpToChapter(1)
                        },
                        onSeekSentence = { sentenceIndex ->
                            chapterSentences.getOrNull(sentenceIndex)?.let { sentence ->
                                jumpToSentence(sentence, source = "seek")
                            }
                        },
                        onSentenceClick = { sentence ->
                            jumpToSentence(sentence, source = "sentence_tap")
                        },
                        onSpeechRateChange = { speechRate = it },
                        onPitchChange = { pitch = it },
                        onReaderFontSizeChange = { readerFontSizeSp = it },
                        onOpenCatalog = { showChapterSheet = true },
                        sleepTimerEnabled = sleepTimerEnabled,
                        onOpenSleepTimer = { showSleepTimerSheet = true },
                        playbackModeLabel = playbackMode.label,
                        onTogglePlaybackMode = {
                            val nextMode = playbackMode.next()
                            playbackModeName = nextMode.name
                            Toast.makeText(context, "已切换为${nextMode.label}", Toast.LENGTH_SHORT).show()
                        },
                        onInstallVoiceData = ::openVoiceDataInstaller,
                        onOpenVoicePackageSettings = { showVoicePackageSheet = true },
                        onOpenVoiceSettings = {
                            openIntentSafely(
                                Intent("com.android.settings.TTS_SETTINGS")
                                    .also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
                                "无法打开语音设置"
                            )
                        },
                        onRetryTts = { restartTtsCheck() },
                        initialSentenceListIndexOverride = if (readyUiListPlan?.shouldApplyInitialSeed == true) {
                            readyUiListPlan.targetLocalIndex.plus(1)
                        } else {
                            snapshotMappedRealIndex
                                ?: cachedReadState
                                    ?.viewportFirstVisibleItemIndex
                                    ?.takeIf { hasSavedRestorePosition && !hasSettledInitialReaderPosition && it >= 0 }
                        },
                        initialSentenceListScrollOffsetOverride = if (readyUiListPlan?.shouldApplyInitialSeed == true) {
                            entryReadyFirstFrame?.lazyListInitialOffset ?: 0
                        } else {
                            cachedReadState
                                ?.viewportFirstVisibleItemScrollOffset
                                ?.takeIf { hasSavedRestorePosition && !hasSettledInitialReaderPosition }
                                ?: 0
                        },
                        onViewportSnapshotChanged = { firstVisibleItemIndex, firstVisibleItemScrollOffset, visibleSentences ->
                            if (usesEntryReadyState) {
                                visibleSentences.firstOrNull()?.let { firstVisible ->
                                    if (!entryReadyInteractionState.manualViewportTakeover) {
                                        entryReadyInteractionState.markManualViewportTakeover()
                                        Log.d(
                                            FORMAL_BOOK_PLAYBACK_TAG,
                                            "phase5c ready manual viewport takeover"
                                        )
                                        Log.d(
                                            FORMAL_BOOK_PLAYBACK_TAG,
                                            "phase5c ready skip seed reapply after manual"
                                        )
                                    }
                                    currentParagraphIndex = firstVisible.paragraphIndex.coerceIn(
                                        0,
                                        (paragraphs.size - 1).coerceAtLeast(0)
                                    )
                                    currentSentenceIndexInParagraph = firstVisible.sentenceIndexInParagraph.coerceAtLeast(0)
                                    currentReadingTargetName = BookReadingTarget.SENTENCE.name
                                    Log.d(
                                        FORMAL_BOOK_PLAYBACK_TAG,
                                        "phase5c ready screen path manual viewport target updated " +
                                            "paragraphIndex=$currentParagraphIndex " +
                                            "sentenceIndex=$currentSentenceIndexInParagraph " +
                                            "chapterSentenceIndex=${firstVisible.chapterSentenceIndex}"
                                    )
                                }
                            } else {
                                saveViewportReadState(
                                    firstVisibleItemIndex = firstVisibleItemIndex,
                                    firstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
                                    visibleSentences = visibleSentences,
                                    reason = "visible_window"
                                )
                            }
                        },
                        logRestoreScrollSkipped = readyUiListPlan?.shouldLogRestoreScrollSkipped ?: true,
                        onRestoreScrollSkippedLogged = {
                            entryReadyInteractionState.markRestoreScrollSkipLogged()
                        },
                        onPositionSettled = {
                            if (!hasSettledInitialReaderPosition) {
                                hasSettledInitialReaderPosition = true
                                val elapsedMs = (System.currentTimeMillis() - restorePositionStartedAtMillis)
                                    .coerceAtLeast(0L)
                                Log.d(FORMAL_BOOK_PLAYBACK_TAG, "restore position scroll done elapsedMs=$elapsedMs")
                                Log.d(FORMAL_BOOK_PLAYBACK_TAG, "restore position visible content enabled")
                                Log.d(FORMAL_BOOK_PLAYBACK_TAG, "perf restore_visible_content elapsedMs=$elapsedMs")
                            }
                            if (pendingSnapshotHandoff && !hasCompletedSnapshotToRealHandoff) {
                                hasCompletedSnapshotToRealHandoff = true
                                Log.d(
                                    FORMAL_BOOK_PLAYBACK_TAG,
                                    "instant entry handoff snapshot_to_real done stable=${snapshotMappedRealIndex != null} " +
                                        "realIndex=${snapshotMappedRealIndex ?: -1}"
                                )
                            }
                        },
                        showPositioningOverlay = if (usesEntryReadyState) {
                            false
                        } else {
                            BookReaderRestorePositionGate.shouldHideContent(
                                hasRestorePosition = hasSavedRestorePosition,
                                hasSettledPosition = hasSettledInitialReaderPosition,
                                isLoading = isLoading,
                                hasLoadError = loadError != null,
                                hasSnapshotContent = hasInitialReaderSnapshot,
                            ) || pendingSnapshotHandoff
                        },
                        instantEntryRenderMode = "real_content",
                        restoreKey = "real#$restorePositionKey#$currentChapterSentenceIndex",
                        allowInitialRestoreScroll = entryReadyScreenPlan.allowInitialRestoreScroll,
                        snapshotHandoffPending = pendingSnapshotHandoff,
                        snapshotFirstVisibleParagraphIndex = snapshotFirstVisibleParagraphIndex,
                        snapshotFirstVisibleSentenceIndex = snapshotFirstVisibleSentenceIndex,
                        snapshotFirstVisibleChapterSentenceIndex = snapshotFirstVisibleChapterSentenceIndex,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showChapterSheet) {
        ChapterCatalogBottomSheet(
            chapters = chapters,
            currentChapter = currentChapter,
            currentParagraphIndex = currentParagraphIndex,
            onDismiss = { showChapterSheet = false },
            onRefresh = { chapterRefreshKey += 1 },
            onChapterClick = { chapter ->
                showChapterSheet = false
                val targetIndex = chapters.indexOfFirst { it.paragraphIndex == chapter.paragraphIndex }
                if (targetIndex >= 0) {
                    val wasPlaying = isPlaying
                    pendingStopAfterChapter = false
                    pendingStopChapterIndex = -1
                    stopCurrentPlayback(reason = "catalog_chapter", invalidateSession = true)
                    currentParagraphIndex = chapters[targetIndex].paragraphIndex.coerceIn(0, paragraphs.lastIndex)
                    currentSentenceIndexInParagraph = 0
                    currentReadingTargetName = BookReadingTarget.CHAPTER_TITLE.name
                    saveProgress(currentParagraphIndex)
                    if (wasPlaying) {
                        isPlaying = false
                        speakCurrentSentence()
                    }
                }
            }
        )
    }

    if (showSleepTimerSheet) {
        SleepTimerBottomSheet(
            timerEnabled = sleepTimerEnabled,
            remainingMillis = sleepTimerRemainingMillis,
            selectedHours = sleepTimerHours,
            selectedMinutes = sleepTimerMinutes,
            stopMode = sleepTimerStopMode,
            onHoursChange = { sleepTimerHours = it.coerceIn(0, 23) },
            onMinutesChange = { sleepTimerMinutes = it.coerceIn(0, 59) },
            onQuickMinutesSelected = { minutes ->
                sleepTimerHours = (minutes / 60).coerceIn(0, 23)
                sleepTimerMinutes = (minutes % 60).coerceIn(0, 59)
            },
            onStopModeChange = { sleepTimerStopModeName = it.name },
            onStartOrUpdate = { startOrUpdateSleepTimer(isUpdate = sleepTimerEnabled) },
            onDisableTimer = {
                clearSleepTimer()
                showSleepTimerSheet = false
                Toast.makeText(context, "已关闭定时", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showSleepTimerSheet = false }
        )
    }

    if (showVoicePackageSheet) {
        VoicePackageSettingsBottomSheet(
            isTtsReady = isTtsReady,
            isTtsChecking = isTtsChecking,
            message = ttsError,
            speechRate = speechRate,
            pitch = pitch,
            voices = ttsVoices,
            selectedVoiceName = selectedVoiceName,
            voiceDataInstallUnavailable = voiceDataInstallUnavailable,
            isBuiltInOfflineTtsInitializing = isBuiltInOfflineTtsInitializing,
            builtInOfflineTtsError = builtInOfflineTtsError,
            onDismiss = { showVoicePackageSheet = false },
            onInstallVoiceData = ::openVoiceDataInstaller,
            onOpenSystemSettings = ::openSystemVoiceSettings,
            onRetry = {
                restartTtsCheck()
            },
            onPreview = ::previewSystemVoice,
            onAudioChannelTest = {
                coroutineScope.launch {
                    Toast.makeText(context, "正在播放测试音...", Toast.LENGTH_SHORT).show()
                    val result = builtInOfflineTtsEngine.playAudioChannelTest {
                        Toast.makeText(context, "测试音开始播放", Toast.LENGTH_SHORT).show()
                    }
                        when (result) {
                            is BuiltInOfflineTtsResult.Success -> {
                                Toast.makeText(context, "测试音播放完成", Toast.LENGTH_SHORT).show()
                            }
                        is BuiltInOfflineTtsResult.Failure -> {
                            builtInOfflineTtsError = result.message
                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            onAishell3OfflinePreview = {
                coroutineScope.launch {
                    Log.d("Aishell3StreamingTts", "start aishell3 preview")
                    Toast.makeText(context, "正在生成自研离线语音...", Toast.LENGTH_SHORT).show()

                    val engine = Aishell3SegmentedStreamingTtsEngine(context.applicationContext)
                    val streamingPlayer = StreamingPcmAudioPlayer()
                    var chunkIndex = 0
                    var playbackDurationMs = 0L
                    var playbackStarted = false

                    val result = runCatching {
                        engine.speak(
                            text = Aishell3SegmentedStreamingTtsEngine.PREVIEW_TEXT,
                            params = StreamingTtsParams(
                                voiceId = "aishell3-speaker-10",
                                speed = 1f,
                                pitch = 1f,
                                volume = 1f
                            ),
                            onStart = {
                                Log.d("Aishell3StreamingTts", "preview onStart")
                            },
                            onChunk = { chunk ->
                                Log.d(
                                    "Aishell3StreamingTts",
                                    "ui chunk index=$chunkIndex bytes=${chunk.data.size} sampleRate=${chunk.format.sampleRate}"
                                )

                                if (chunkIndex == 0) {
                                    val startResult = streamingPlayer.start(chunk.format)
                                    if (startResult.isFailure) {
                                        val message = startResult.exceptionOrNull()?.message ?: "AudioTrack 启动失败"
                                        throw IllegalStateException(message)
                                    }
                                    playbackStarted = true
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "自研离线语音开始播放", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                val writeResult = streamingPlayer.write(chunk)
                                if (writeResult.isFailure) {
                                    val message = writeResult.exceptionOrNull()?.message ?: "AudioTrack 写入失败"
                                    throw IllegalStateException(message)
                                }

                                playbackDurationMs += chunk.data.size * 1000L /
                                    (chunk.format.sampleRate * 2L)
                                chunkIndex += 1
                            },
                            onDone = {
                                Log.d("Aishell3StreamingTts", "preview onDone playbackDurationMs=$playbackDurationMs")
                            },
                            onError = { error ->
                                Log.e("Aishell3StreamingTts", "preview error: $error")
                            }
                        )
                    }.getOrElse { error ->
                        Log.e("Aishell3StreamingTts", "preview exception", error)
                        StreamingTtsResult.Error(error.message ?: "未知错误")
                    }

                    if (playbackStarted) {
                        delay((playbackDurationMs + 220L).coerceAtMost(5000L))
                    }
                    streamingPlayer.release()
                    engine.release()

                    when (result) {
                        StreamingTtsResult.Success -> {
                            Toast.makeText(context, "自研离线语音播放完成", Toast.LENGTH_SHORT).show()
                        }
                        StreamingTtsResult.Stopped -> {
                            Toast.makeText(context, "自研离线语音已停止", Toast.LENGTH_SHORT).show()
                        }
                        is StreamingTtsResult.Error -> {
                            Toast.makeText(
                                context,
                                "自研离线语音失败：${result.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            },
            onStreamingAudioTest = {
                coroutineScope.launch {
                    Log.d("ToneStreamingTtsEngine", "start streaming tone test")
                    Toast.makeText(context, "正在播放流式测试音...", Toast.LENGTH_SHORT).show()

                    val toneEngine = ToneStreamingTtsEngine()
                    val streamingPlayer = StreamingPcmAudioPlayer()
                    var chunkIndex = 0

                    val initializeResult = toneEngine.initialize()
                    if (initializeResult.isFailure) {
                        val message = initializeResult.exceptionOrNull()?.message ?: "初始化失败"
                        Log.e("ToneStreamingTtsEngine", "initialize failed: $message")
                        Toast.makeText(context, "流式音频测试失败：$message", Toast.LENGTH_LONG).show()
                        streamingPlayer.release()
                        toneEngine.release()
                        return@launch
                    }

                    val result = runCatching {
                        toneEngine.speak(
                            text = "tone streaming test",
                            params = StreamingTtsParams(
                                voiceId = "tone",
                                speed = 1f,
                                pitch = 1f,
                                volume = 1f
                            ),
                            onStart = {
                                Log.d("ToneStreamingTtsEngine", "streaming tone onStart")
                            },
                            onChunk = { chunk ->
                                Log.d(
                                    "ToneStreamingTtsEngine",
                                    "chunk index=$chunkIndex chunk bytes=${chunk.data.size} sampleRate=${chunk.format.sampleRate}"
                                )

                                if (chunkIndex == 0) {
                                    val startResult = streamingPlayer.start(chunk.format)
                                    if (startResult.isFailure) {
                                        val message = startResult.exceptionOrNull()?.message ?: "AudioTrack 启动失败"
                                        throw IllegalStateException(message)
                                    }
                                }

                                val writeResult = streamingPlayer.write(chunk)
                                if (writeResult.isFailure) {
                                    val message = writeResult.exceptionOrNull()?.message ?: "AudioTrack 写入失败"
                                    throw IllegalStateException(message)
                                }

                                chunkIndex += 1
                            },
                            onDone = {
                                Log.d("ToneStreamingTtsEngine", "playback completed")
                            },
                            onError = { error ->
                                Log.e("ToneStreamingTtsEngine", "streaming tone failed: $error")
                            }
                        )
                    }.getOrElse { error ->
                        Log.e("ToneStreamingTtsEngine", "streaming tone exception", error)
                        StreamingTtsResult.Error(error.message ?: "未知错误")
                    }

                    delay(180)
                    streamingPlayer.release()
                    toneEngine.release()

                    when (result) {
                        StreamingTtsResult.Success -> {
                            Toast.makeText(context, "流式音频测试完成", Toast.LENGTH_SHORT).show()
                        }
                        StreamingTtsResult.Stopped -> {
                            Toast.makeText(context, "流式音频测试已停止", Toast.LENGTH_SHORT).show()
                        }
                        is StreamingTtsResult.Error -> {
                            Toast.makeText(
                                context,
                                "流式音频测试失败：${result.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            },
            onBuiltInPreview = {
                if (isBuiltInOfflineTtsInitializing) {
                    Toast.makeText(context, "内置语音正在初始化", Toast.LENGTH_SHORT).show()
                } else {
                    coroutineScope.launch {
                        builtInOfflineTtsError = null
                        isBuiltInOfflineTtsInitializing = true
                        Toast.makeText(context, "正在生成内置语音...", Toast.LENGTH_SHORT).show()
                        val initialized = builtInOfflineTtsEngine.isReady ||
                            builtInOfflineTtsEngine.initialize(context.applicationContext)
                        isBuiltInOfflineTtsInitializing = false

                        if (!initialized) {
                            val error = builtInOfflineTtsEngine.lastError ?: "内置语音初始化失败"
                            builtInOfflineTtsError = error
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            return@launch
                        }

                        Toast.makeText(context, "正在播放内置语音...", Toast.LENGTH_SHORT).show()
                        val result = builtInOfflineTtsEngine.speak("这是一段内置离线语音试听。") {
                            Toast.makeText(context, "内置语音开始播放", Toast.LENGTH_SHORT).show()
                        }
                        when (result) {
                            is BuiltInOfflineTtsResult.Success -> {
                                Toast.makeText(
                                    context,
                                    "内置语音试听完成：已播放${result.sourceLabel}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            is BuiltInOfflineTtsResult.Failure -> {
                                builtInOfflineTtsError = result.message
                                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            },
            onRefreshVoices = {
                ttsVoices = ttsController?.getAvailableVoices().orEmpty()
                if (ttsVoices.isEmpty()) {
                    Toast.makeText(context, "未读取到系统声线", Toast.LENGTH_SHORT).show()
                }
            },
            onSelectVoice = ::selectTtsVoice,
            onSearchRhVoice = ::searchRhVoiceEngine
        )
    }
}

@Composable
private fun BookListenTopBar(
    title: String,
    onBack: () -> Unit,
    onMenuAction: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "杩斿洖", tint = Color.White)
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
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                IconButton(onClick = { expanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = Color.White)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = Color(0xFF141821),
                shape = RoundedCornerShape(18.dp)
            ) {
                listOf(
                    "语音设置",
                    "重新检测 TTS"
                ).forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item, color = Color.White.copy(alpha = 0.92f)) },
                        onClick = {
                            expanded = false
                            onMenuAction(item)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookListenContent(
    chapterTitle: String,
    chapterSentences: List<ReaderSentence>,
    currentChapterSentenceIndex: Int,
    isChapterTitleCurrent: Boolean,
    currentParagraphIndex: Int,
    currentSentenceIndexInParagraph: Int,
    listenedTimeLabel: String,
    remainingTimeLabel: String,
    isPlaying: Boolean,
    isTtsReady: Boolean,
    isTtsChecking: Boolean,
    ttsError: String?,
    currentVoiceName: String?,
    speechRate: Float,
    pitch: Float,
    readerFontSizeSp: Float,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekSentence: (Int) -> Unit,
    onSentenceClick: (ReaderSentence) -> Unit,
    onSpeechRateChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onReaderFontSizeChange: (Float) -> Unit,
    onOpenCatalog: () -> Unit,
    sleepTimerEnabled: Boolean,
    onOpenSleepTimer: () -> Unit,
    playbackModeLabel: String,
    onTogglePlaybackMode: () -> Unit,
    onInstallVoiceData: () -> Unit,
    onOpenVoicePackageSettings: () -> Unit,
    onOpenVoiceSettings: () -> Unit,
    onRetryTts: () -> Unit,
    initialSentenceListIndexOverride: Int? = null,
    initialSentenceListScrollOffsetOverride: Int = 0,
    onViewportSnapshotChanged: (Int, Int, List<ReaderSentence>) -> Unit = { _, _, _ -> },
    onPositionSettled: () -> Unit = {},
    logRestoreScrollSkipped: Boolean = true,
    onRestoreScrollSkippedLogged: () -> Unit = {},
    showPositioningOverlay: Boolean = false,
    instantEntryRenderMode: String = "real_content",
    restoreKey: String = "",
    allowInitialRestoreScroll: Boolean = true,
    snapshotHandoffPending: Boolean = false,
    snapshotFirstVisibleParagraphIndex: Int? = null,
    snapshotFirstVisibleSentenceIndex: Int? = null,
    snapshotFirstVisibleChapterSentenceIndex: Int? = null,
    progressValueOverride: Float? = null,
    progressMaxValueOverride: Float? = null,
    listenedTimeLabelOverride: String? = null,
    remainingTimeLabelOverride: String? = null,
    showReaderProgress: Boolean = true,
    progressUserSeekEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showVoiceControls by remember { mutableStateOf(false) }
    var showFontSizeControls by remember { mutableStateOf(false) }
    val context = LocalContext.current
    fun showDevelopingToast() {
        Toast.makeText(context, "功能开发中", Toast.LENGTH_SHORT).show()
    }
    val defaultInitialSentenceListIndex = if (isChapterTitleCurrent) {
        0
    } else {
        (currentChapterSentenceIndex - 1).coerceAtLeast(0)
    }
    val initialSentenceListIndex = initialSentenceListIndexOverride
        ?.coerceIn(0, chapterSentences.size)
        ?: defaultInitialSentenceListIndex
    val initialSentenceListScrollOffset = if (initialSentenceListIndexOverride != null) {
        initialSentenceListScrollOffsetOverride.coerceAtLeast(0)
    } else {
        0
    }
    val sentenceListState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialSentenceListIndex,
        initialFirstVisibleItemScrollOffset = initialSentenceListScrollOffset
    )
    LaunchedEffect(instantEntryRenderMode, restoreKey, initialSentenceListIndex) {
        Log.d(
            FORMAL_BOOK_PLAYBACK_TAG,
            "instant entry render mode=$instantEntryRenderMode initialIndex=$initialSentenceListIndex offset=$initialSentenceListScrollOffset"
        )
        Log.d(
            FORMAL_BOOK_PLAYBACK_TAG,
            "instant entry LazyListState created initialIndex=$initialSentenceListIndex offset=$initialSentenceListScrollOffset restoreKey=$restoreKey"
        )
    }
    val seekEndIndex = (chapterSentences.size - 1).coerceAtLeast(0)
    val sliderEndIndex = seekEndIndex.coerceAtLeast(1)
    LaunchedEffect(sentenceListState, chapterSentences) {
        snapshotFlow {
            BookReaderVisibleViewport(
                firstVisibleItemIndex = sentenceListState.firstVisibleItemIndex,
                firstVisibleItemScrollOffset = sentenceListState.firstVisibleItemScrollOffset,
                visibleLazyItemIndexes = sentenceListState.layoutInfo.visibleItemsInfo.map { it.index }
            )
        }
            .distinctUntilChanged()
            .collect { viewport ->
                val visibleSentences = viewport.visibleLazyItemIndexes
                    .map { lazyItemIndex -> lazyItemIndex - 1 }
                    .filter { sentenceIndex -> sentenceIndex in chapterSentences.indices }
                    .map { sentenceIndex -> chapterSentences[sentenceIndex] }
                if (visibleSentences.isNotEmpty()) {
                    onViewportSnapshotChanged(
                        viewport.firstVisibleItemIndex,
                        viewport.firstVisibleItemScrollOffset,
                        visibleSentences
                    )
                }
            }
    }
    val seekValue = if (isChapterTitleCurrent) {
        0f
    } else {
        currentChapterSentenceIndex.coerceIn(0, seekEndIndex).toFloat()
    }
    val progressSeekInteraction = remember(chapterTitle, chapterSentences.size) {
        BookReaderProgressSeekInteraction()
    }
    var progressSeekPreviewIndex by remember(chapterTitle, chapterSentences.size) {
        mutableStateOf<Int?>(null)
    }
    var progressSeekRepeatedSkipLoggedIndex by remember(chapterTitle, chapterSentences.size) {
        mutableStateOf<Int?>(null)
    }
    val previewSeekIndex = progressSeekPreviewIndex?.coerceIn(0, seekEndIndex)
    val displayedSeekValue = previewSeekIndex?.toFloat() ?: progressValueOverride ?: seekValue
    val displayedSliderEndIndex = progressMaxValueOverride ?: sliderEndIndex.toFloat()
    val previewListenedTimeLabel = previewSeekIndex?.let { index ->
        estimateSentenceTimeLabel(chapterSentences, 0, index, speechRate)
    }
    val previewRemainingTimeLabel = previewSeekIndex?.let { index ->
        estimateSentenceTimeLabel(chapterSentences, index, chapterSentences.size, speechRate)
    }
    val displayedListenedTimeLabel = previewListenedTimeLabel ?: listenedTimeLabelOverride ?: listenedTimeLabel
    val displayedRemainingTimeLabel = previewRemainingTimeLabel ?: remainingTimeLabelOverride ?: remainingTimeLabel

    LaunchedEffect(chapterTitle, currentChapterSentenceIndex, chapterSentences.size, isChapterTitleCurrent) {
        Log.d(
            FORMAL_BOOK_PLAYBACK_TAG,
            "perf restore_initial_state index=$initialSentenceListIndex offset=$initialSentenceListScrollOffset renderMode=$instantEntryRenderMode restoreKey=$restoreKey"
        )
        if (!allowInitialRestoreScroll) {
            if (logRestoreScrollSkipped) {
                Log.d(
                    FORMAL_BOOK_PLAYBACK_TAG,
                    "phase5c ready restore scroll skipped once allowInitialRestoreScroll=false " +
                        "index=$initialSentenceListIndex offset=$initialSentenceListScrollOffset"
                )
                onRestoreScrollSkippedLogged()
            }
            return@LaunchedEffect
        }
        Log.d(FORMAL_BOOK_PLAYBACK_TAG, "restore position scroll start index=$initialSentenceListIndex offset=$initialSentenceListScrollOffset")
        val alignStartedAtMillis = System.currentTimeMillis()
        if (snapshotHandoffPending) {
            Log.d(
                FORMAL_BOOK_PLAYBACK_TAG,
                "instant entry real content align start " +
                    "snapshotFirstVisibleParagraphIndex=$snapshotFirstVisibleParagraphIndex " +
                    "snapshotFirstVisibleSentenceIndex=$snapshotFirstVisibleSentenceIndex " +
                    "snapshotFirstVisibleChapterSentenceIndex=$snapshotFirstVisibleChapterSentenceIndex " +
                    "realIndex=$initialSentenceListIndex offset=$initialSentenceListScrollOffset"
            )
        }
        if (isChapterTitleCurrent) {
            sentenceListState.scrollToItem(0)
        } else if (chapterSentences.isNotEmpty()) {
            sentenceListState.scrollToItem(initialSentenceListIndex, initialSentenceListScrollOffset)
        }
        val alignElapsedMs = (System.currentTimeMillis() - alignStartedAtMillis).coerceAtLeast(0L)
        Log.d(
            FORMAL_BOOK_PLAYBACK_TAG,
            "instant entry real content align done elapsedMs=$alignElapsedMs realIndex=$initialSentenceListIndex " +
                "offset=$initialSentenceListScrollOffset renderMode=$instantEntryRenderMode"
        )
        onPositionSettled()
    }

    Column(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TtsStatusStrip(
            isTtsReady = isTtsReady,
            isTtsChecking = isTtsChecking,
            message = ttsError,
            currentVoiceName = currentVoiceName,
            speechRate = speechRate,
            pitch = pitch,
            onInstallVoiceData = onInstallVoiceData,
            onOpenVoiceSettings = onOpenVoicePackageSettings,
            onRetry = onRetryTts,
            modifier = Modifier.padding(horizontal = 18.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 30.dp, vertical = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (showPositioningOverlay) 0f else 1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LazyColumn(
                    state = sentenceListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    item {
                        Text(
                            text = chapterTitle,
                            color = if (isChapterTitleCurrent) Color(0xFFB388FF) else Color.White.copy(alpha = 0.98f),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = (readerFontSizeSp + 2f).sp,
                                lineHeight = (readerFontSizeSp * 1.55f + 2f).sp,
                                fontWeight = if (isChapterTitleCurrent) FontWeight.Bold else FontWeight.SemiBold
                            )
                        )
                    }
                    if (chapterSentences.isEmpty()) {
                        item {
                            Text(
                                text = "暂无正文",
                                color = Color.White.copy(alpha = 0.62f),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = readerFontSizeSp.sp,
                                    lineHeight = (readerFontSizeSp * 1.6f).sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    } else {
                        itemsIndexed(chapterSentences) { index, sentence ->
                            val isCurrentSentence =
                                !isChapterTitleCurrent &&
                                    ((sentence.paragraphIndex == currentParagraphIndex &&
                                        sentence.sentenceIndexInParagraph == currentSentenceIndexInParagraph) ||
                                        index == currentChapterSentenceIndex)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSentenceClick(sentence) }
                                    .padding(horizontal = 4.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = sentence.text,
                                    color = if (isCurrentSentence) Color(0xFFB388FF) else Color.White.copy(alpha = 0.72f),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = readerFontSizeSp.sp,
                                        lineHeight = (readerFontSizeSp * 1.6f).sp,
                                        fontWeight = if (isCurrentSentence) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }
            }
            if (showPositioningOverlay) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF4D8DFF))
                        Text(
                            text = "姝ｅ湪鎭㈠涓婃闃呰浣嶇疆...",
                            color = Color.White.copy(alpha = 0.72f),
                            modifier = Modifier.padding(top = 14.dp)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .clip(RoundedCornerShape(0.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF21182F), Color(0xFF11101B))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "已听 $displayedListenedTimeLabel",
                        color = Color.White.copy(alpha = 0.52f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        "剩余 $displayedRemainingTimeLabel",
                        color = Color.White.copy(alpha = 0.52f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (showReaderProgress) {
                    ReaderChapterProgressBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        value = displayedSeekValue,
                        maxValue = displayedSliderEndIndex,
                        onValueChange = {
                            if (progressUserSeekEnabled) {
                                val previewIndex = it.roundToInt().coerceIn(0, seekEndIndex)
                                val previousPreviewIndex = progressSeekPreviewIndex
                                progressSeekInteraction.preview(previewIndex)
                                if (previousPreviewIndex == null) {
                                    Log.d(
                                        FORMAL_BOOK_PLAYBACK_TAG,
                                        "phase5c progress seek preview start index=$previewIndex"
                                    )
                                } else if (previousPreviewIndex == previewIndex) {
                                    if (progressSeekRepeatedSkipLoggedIndex != previewIndex) {
                                        Log.d(
                                            FORMAL_BOOK_PLAYBACK_TAG,
                                            "phase5c progress seek skipped repeated onValueChange index=$previewIndex"
                                        )
                                        progressSeekRepeatedSkipLoggedIndex = previewIndex
                                    }
                                } else {
                                    Log.d(
                                        FORMAL_BOOK_PLAYBACK_TAG,
                                        "phase5c progress seek preview update index=$previewIndex"
                                    )
                                    progressSeekRepeatedSkipLoggedIndex = null
                                }
                                progressSeekPreviewIndex = previewIndex
                            }
                        },
                        onValueChangeFinished = {
                            val commitIndex = progressSeekInteraction.finish()
                            progressSeekPreviewIndex = null
                            progressSeekRepeatedSkipLoggedIndex = null
                            if (commitIndex != null && progressUserSeekEnabled) {
                                val safeCommitIndex = commitIndex.coerceIn(0, seekEndIndex)
                                Log.d(
                                    FORMAL_BOOK_PLAYBACK_TAG,
                                    "phase5c progress seek commit"
                                )
                                Log.d(
                                    FORMAL_BOOK_PLAYBACK_TAG,
                                    "phase5c progress seek commit target index=$safeCommitIndex"
                                )
                                onSeekSentence(safeCommitIndex)
                            }
                        },
                        onValueChangeCanceled = {
                            progressSeekInteraction.cancel()
                            progressSeekPreviewIndex = null
                            progressSeekRepeatedSkipLoggedIndex = null
                        }
                    )
                } else {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ReaderControlButton(playbackModeLabel, onClick = onTogglePlaybackMode) {
                    Text("⇄", color = Color(0xFFC3B0FF), fontSize = 22.sp, fontWeight = FontWeight.Medium)
                }
                ReaderControlButton("上一章", onPrevious) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = null, modifier = Modifier.size(24.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF724CFF), Color(0xFFA66EFF))
                                )
                            )
                            .clickable(onClick = onPlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = if (isPlaying) "暂停" else "播放",
                        color = Color.White.copy(alpha = 0.76f),
                        fontSize = 11.sp
                    )
                }
                ReaderControlButton("下一章", onNext) {
                    Icon(Icons.Filled.SkipNext, contentDescription = null, modifier = Modifier.size(24.dp))
                }
                ReaderControlButton("播放列表", onClick = onOpenCatalog, weak = true) {
                    Icon(Icons.Filled.List, contentDescription = null, modifier = Modifier.size(22.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ReaderActionButton(
                    label = "背景音乐",
                    icon = { Text("♪", fontSize = 20.sp, color = Color.White.copy(alpha = 0.52f)) },
                    onClick = ::showDevelopingToast,
                    modifier = Modifier.weight(1f)
                )
                ReaderActionButton(
                    label = "定时关闭",
                    icon = {
                        Text(
                            "◷",
                            fontSize = 20.sp,
                            color = if (sleepTimerEnabled) Color(0xFFC3B0FF) else Color.White.copy(alpha = 0.52f)
                        )
                    },
                    onClick = onOpenSleepTimer,
                    modifier = Modifier.weight(1f)
                )
                ReaderActionButton(
                    label = "夜间模式",
                    icon = { Text("☾", fontSize = 20.sp, color = Color.White.copy(alpha = 0.52f)) },
                    onClick = ::showDevelopingToast,
                    modifier = Modifier.weight(1f)
                )
                ReaderActionButton(
                    label = "字号",
                    icon = { Text("A", fontSize = 20.sp, color = Color.White.copy(alpha = 0.52f), fontWeight = FontWeight.SemiBold) },
                    onClick = { showFontSizeControls = true },
                    modifier = Modifier.weight(1f)
                )
            }

        }
    }

    if (showVoiceControls) {
        VoiceSettingsBottomSheet(
            speechRate = speechRate,
            pitch = pitch,
            onSpeechRateChange = onSpeechRateChange,
            onPitchChange = onPitchChange,
            onDismiss = { showVoiceControls = false }
        )
    }

    if (showFontSizeControls) {
        FontSizeBottomSheetV3(
            fontSizeSp = readerFontSizeSp,
            onFontSizeChange = onReaderFontSizeChange,
            onDismiss = { showFontSizeControls = false }
        )
    }
}

@Composable
private fun ReaderChapterProgressBar(
    value: Float,
    maxValue: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    onValueChangeCanceled: () -> Unit,
    modifier: Modifier = Modifier
) {
    ThinMoonSlider(
        value = value,
        valueRange = 0f..maxValue.coerceAtLeast(1f),
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        onValueChangeCanceled = onValueChangeCanceled,
        modifier = modifier,
        trackHeight = 3.dp,
        thumbSize = 11.dp
    )
}

@Composable
private fun ThinMoonSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit = {},
    onValueChangeCanceled: () -> Unit = {},
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color = Color(0xFF8B5CFF),
    inactiveColor: Color = Color(0xFF332A45),
    thumbColor: Color = Color(0xFFE7DCFF),
    trackHeight: androidx.compose.ui.unit.Dp = 3.dp,
    thumbSize: androidx.compose.ui.unit.Dp = 11.dp,
    touchHeight: androidx.compose.ui.unit.Dp = 32.dp
) {
    val density = LocalDensity.current
    var trackWidthPx by remember { mutableIntStateOf(1) }
    val rangeSize = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val progress = ((value - valueRange.start) / rangeSize).coerceIn(0f, 1f)
    val thumbSizePx = with(density) { thumbSize.roundToPx() }

    fun updateFromX(x: Float) {
        val usableWidth = (trackWidthPx - thumbSizePx).coerceAtLeast(1)
        val nextProgress = ((x - thumbSizePx / 2f) / usableWidth.toFloat()).coerceIn(0f, 1f)
        onValueChange(valueRange.start + nextProgress * rangeSize)
    }

    Box(
        modifier = modifier
            .height(touchHeight)
            .onSizeChanged { size -> trackWidthPx = size.width.coerceAtLeast(1) }
            .then(
                if (enabled) {
                    Modifier
                        .pointerInput(valueRange.start, valueRange.endInclusive) {
                            detectTapGestures { offset ->
                                updateFromX(offset.x)
                                onValueChangeFinished()
                            }
                        }
                        .pointerInput(valueRange.start, valueRange.endInclusive) {
                            detectDragGestures(
                                onDragStart = { offset -> updateFromX(offset.x) },
                                onDrag = { change, _ ->
                                    updateFromX(change.position.x)
                                    change.consume()
                                },
                                onDragEnd = { onValueChangeFinished() },
                                onDragCancel = { onValueChangeCanceled() }
                            )
                        }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(RoundedCornerShape(99.dp))
                .background(inactiveColor)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(trackHeight)
                .clip(RoundedCornerShape(99.dp))
                .background(activeColor)
        )
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = ((trackWidthPx - thumbSizePx).coerceAtLeast(0) * progress).roundToInt(),
                        y = 0
                    )
                }
                .size(thumbSize)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

@Composable
private fun TtsStatusStrip(
    isTtsReady: Boolean,
    isTtsChecking: Boolean,
    message: String?,
    currentVoiceName: String?,
    speechRate: Float,
    pitch: Float,
    onInstallVoiceData: () -> Unit,
    onOpenVoiceSettings: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val voiceTitle = when {
        isTtsChecking -> "系统语音检测中"
        isTtsReady -> "系统语音"
        else -> "系统语音不可用"
    }
    val voiceLabel = currentVoiceName?.takeIf { it.isNotBlank() } ?: "默认声线"
    val safeSpeechRate = if (speechRate > 0f) speechRate else 1f
    val speechRateLabel = "语速 ${"%.1f".format(safeSpeechRate)}x"
    val chipEnabled = isTtsReady && !isTtsChecking

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF8B5CFF), Color(0xFF151021), Color(0xFF05060A))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.GraphicEq,
                contentDescription = null,
                tint = Color(0xFFC3B0FF),
                modifier = Modifier.size(22.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = voiceTitle,
                color = if (isTtsReady) Color(0xFFCDBEFF) else Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ReaderVoiceChip(voiceLabel, enabled = chipEnabled)
                ReaderVoiceChip(speechRateLabel, enabled = chipEnabled)
            }
        }
        Text(
            text = "语音设置 ›",
            color = Color(0xFFCDBEFF),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable(onClick = onOpenVoiceSettings)
        )
    }
}

@Composable
private fun ReaderVoiceChip(
    text: String,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = if (enabled) 0.12f else 0.07f))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = if (enabled) 0.9f else 0.58f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun VoiceSettingsBottomSheet(
    speechRate: Float,
    pitch: Float,
    onSpeechRateChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF171222),
        scrimColor = Color.Black.copy(alpha = 0.34f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color.White.copy(alpha = 0.22f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "语音设置",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onDismiss) {
                    Text("关闭", color = Color(0xFFC3B0FF))
                }
            }
            SettingSlider(
                title = "语速",
                valueText = "${"%.1f".format(speechRate)}x",
                value = speechRate,
                valueRange = 0.6f..1.8f,
                onValueChange = onSpeechRateChange
            )
            SettingSlider(
                title = "闊宠皟",
                valueText = "${"%.1f".format(pitch)}x",
                value = pitch,
                valueRange = 0.7f..1.5f,
                onValueChange = onPitchChange
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun VoicePackageSettingsBottomSheet(
    isTtsReady: Boolean,
    isTtsChecking: Boolean,
    message: String?,
    speechRate: Float,
    pitch: Float,
    voices: List<BookTtsVoice>,
    selectedVoiceName: String?,
    voiceDataInstallUnavailable: Boolean,
    isBuiltInOfflineTtsInitializing: Boolean,
    builtInOfflineTtsError: String?,
    onDismiss: () -> Unit,
    onInstallVoiceData: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onRetry: () -> Unit,
    onPreview: () -> Unit,
    onAudioChannelTest: () -> Unit,
    onAishell3OfflinePreview: () -> Unit,
    onStreamingAudioTest: () -> Unit,
    onBuiltInPreview: () -> Unit,
    onRefreshVoices: () -> Unit,
    onSelectVoice: (String?) -> Unit,
    onSearchRhVoice: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF15111F),
        scrimColor = Color.Black.copy(alpha = 0.48f),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 2.dp)
                    .width(46.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color.White.copy(alpha = 0.25f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "语音包与语音设置",
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "选择系统语音或安装离线语音引擎",
                        color = Color.White.copy(alpha = 0.54f),
                        fontSize = 13.sp
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "关闭", tint = Color.White.copy(alpha = 0.84f))
                }
            }

            VoiceStatusCard(
                isTtsReady = isTtsReady,
                isTtsChecking = isTtsChecking,
                message = message,
                currentVoiceName = selectedVoiceName,
                speechRate = speechRate,
                pitch = pitch
            )

            if (!isTtsReady) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF8258FF).copy(alpha = 0.08f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "系统语音不可用",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "当前设备未检测到可用中文声线，请安装或启用支持系统 TTS 的语音引擎后重试。",
                        color = Color.White.copy(alpha = 0.66f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    if (!message.isNullOrBlank()) {
                        Text(
                            text = "失败原因：$message",
                            color = Color.White.copy(alpha = 0.46f),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                    if (voiceDataInstallUnavailable) {
                        Text(
                            text = "当前系统不支持直接打开语音数据安装页面，可先打开系统语音设置，或到应用商店搜索 RHVoice 等系统 TTS 引擎。",
                            color = Color(0xFFC3B0FF).copy(alpha = 0.86f),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VoiceSheetActionButton("系统语音设置", onOpenSystemSettings, Modifier.weight(1f))
                VoiceSheetActionButton("重新检测", onRetry, Modifier.weight(1f), emphasized = isTtsReady.not())
                VoiceSheetActionButton(
                    text = if (voiceDataInstallUnavailable) "安装数据不可用" else "安装语音数据",
                    onClick = onInstallVoiceData,
                    modifier = Modifier.weight(1f),
                    muted = voiceDataInstallUnavailable
                )
            }

            VoiceSheetActionButton(
                text = "试听",
                onClick = onPreview,
                modifier = Modifier.fillMaxWidth(),
                emphasized = isTtsReady
            )

            VoiceSheetActionButton(
                text = if (isBuiltInOfflineTtsInitializing) "内置语音初始化中" else "内置语音试听",
                onClick = onBuiltInPreview,
                modifier = Modifier.fillMaxWidth(),
                emphasized = true
            )
            VoiceSheetActionButton(
                text = "音频通道测试",
                onClick = onAudioChannelTest,
                modifier = Modifier.fillMaxWidth()
            )
            VoiceSheetActionButton(
                text = "自研离线试听",
                onClick = onAishell3OfflinePreview,
                modifier = Modifier.fillMaxWidth(),
                emphasized = true
            )
            VoiceSheetActionButton(
                text = "流式音频测试",
                onClick = onStreamingAudioTest,
                modifier = Modifier.fillMaxWidth()
            )
            if (!builtInOfflineTtsError.isNullOrBlank()) {
                Text(
                    text = "内置语音：$builtInOfflineTtsError",
                    color = Color(0xFFFFD5D5).copy(alpha = 0.88f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }

            if (!isTtsReady) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF7B55FF).copy(alpha = 0.08f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "修复建议",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "1. 先点击“安装语音数据”\n2. 如果无效，点击“系统语音设置”选择可用语音引擎\n3. 回到 App 后点击“重新检测”",
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "系统声线列表",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "刷新",
                        color = Color(0xFFC3B0FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(onClick = onRefreshVoices)
                    )
                }
                if (voices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .padding(horizontal = 14.dp, vertical = 14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("未找到可用中文声线", color = Color.White.copy(alpha = 0.86f), fontSize = 14.sp)
                            Text(
                                "请安装系统语音数据，或在系统语音设置中启用外部离线语音引擎。",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                } else {
                    VoiceRow(
                        title = "默认声线",
                        subtitle = "使用系统当前默认 TextToSpeech 声线",
                        selected = selectedVoiceName == null,
                        onClick = { onSelectVoice(null) }
                    )
                    voices.take(8).forEach { voice ->
                        VoiceRow(
                            title = voice.name,
                            subtitle = buildString {
                                append(voice.localeTag)
                                append(" 路 ")
                                append(if (voice.isNetworkConnectionRequired) "可能需要网络" else "系统声线")
                            },
                            selected = selectedVoiceName == voice.name,
                            onClick = { onSelectVoice(voice.name) }
                        )
                    }
                    if (voices.size > 8) {
                        Text(
                            text = "还有 ${voices.size - 8} 个系统声线，可在系统语音设置中查看。",
                            color = Color.White.copy(alpha = 0.42f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "推荐离线语音引擎",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                RecommendedVoiceEngineCard(
                    title = "系统自带离线语音数据",
                    description = if (voiceDataInstallUnavailable) {
                        "当前系统不支持直接安装语音数据，请从系统语音设置或应用商店安装可用引擎。"
                    } else {
                        "优先推荐，兼容性最好，可作为系统 TTS 直接使用。"
                    },
                    action = if (voiceDataInstallUnavailable) "系统语音设置" else "安装 / 设置",
                    onClick = if (voiceDataInstallUnavailable) onOpenSystemSettings else onInstallVoiceData
                )
                RecommendedVoiceEngineCard(
                    title = "RHVoice",
                    description = "开源离线 TTS 引擎，可安装后作为系统语音引擎使用。",
                    action = "搜索安装",
                    onClick = onSearchRhVoice
                )
                RecommendedVoiceEngineCard(
                    title = "Sherpa / Piper 离线语音",
                    description = "后续可作为内置离线语音方案研究，目前暂不内置。",
                    action = "后续支持",
                    onClick = { }
                )
            }
        }
    }
}

@Composable
private fun VoiceStatusCard(
    isTtsReady: Boolean,
    isTtsChecking: Boolean,
    message: String?,
    currentVoiceName: String?,
    speechRate: Float,
    pitch: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.045f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF7B55FF).copy(alpha = if (isTtsReady) 0.26f else 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.GraphicEq,
                contentDescription = null,
                tint = if (isTtsReady || isTtsChecking) Color(0xFFC3B0FF) else Color.White.copy(alpha = 0.46f),
                modifier = Modifier.size(22.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Text(
                text = when {
                    isTtsChecking -> "正在检测系统语音"
                    isTtsReady -> "系统语音可用"
                    else -> "系统语音不可用"
                },
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = when {
                    isTtsChecking -> "请稍候，正在重新初始化系统 TextToSpeech"
                    isTtsReady -> {
                        val voiceText = "当前声线：${currentVoiceName ?: "默认声线"}"
                        message?.takeIf { it.isNotBlank() }?.let { "$voiceText 路 $it" } ?: voiceText
                    }
                    else -> "请安装或启用系统语音引擎后重试"
                },
                color = Color.White.copy(alpha = 0.58f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (!isTtsReady && !isTtsChecking && !message.isNullOrBlank()) {
                    "失败原因：$message"
                } else {
                    "语速：${"%.1f".format(if (speechRate > 0f) speechRate else 1f)}x  路  音调：${"%.1f".format(if (pitch > 0f) pitch else 1f)}x"
                },
                color = Color.White.copy(alpha = 0.46f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun VoiceSheetActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    muted: Boolean = false
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    muted -> Color.White.copy(alpha = 0.025f)
                    emphasized -> Color(0xFF8258FF).copy(alpha = 0.82f)
                    else -> Color.White.copy(alpha = 0.055f)
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = when {
                muted -> Color.White.copy(alpha = 0.42f)
                emphasized -> Color.White
                else -> Color(0xFFD8D0FF)
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VoiceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color(0xFF8258FF).copy(alpha = 0.16f) else Color.White.copy(alpha = 0.035f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (selected) Color(0xFFC3B0FF) else Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF15111F)))
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.46f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecommendedVoiceEngineCard(
    title: String,
    description: String,
    action: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.035f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = Color.White.copy(alpha = 0.48f), fontSize = 11.sp, lineHeight = 16.sp)
        }
        Text(
            text = action,
            color = Color(0xFFC3B0FF),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onClick)
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FontSizeBottomSheet(
    fontSizeSp: Float,
    onFontSizeChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF171222),
        scrimColor = Color.Black.copy(alpha = 0.34f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color.White.copy(alpha = 0.22f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "字号",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${fontSizeSp.roundToInt()}sp",
                        color = Color(0xFFC3B0FF),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text("关闭", color = Color(0xFFC3B0FF))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = { onFontSizeChange((fontSizeSp - 1f).coerceIn(16f, 30f)) }
                ) {
                    Text("鍑忓皬", color = Color.White.copy(alpha = 0.78f))
                }
                Slider(
                    modifier = Modifier.weight(1f),
                    value = fontSizeSp,
                    onValueChange = { onFontSizeChange(it.coerceIn(16f, 30f)) },
                    valueRange = 16f..30f,
                    steps = 13,
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD2C0FF))
                        )
                    },
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color(0xFF8B5CFF),
                        inactiveTrackColor = Color(0xFF2A2638),
                        thumbColor = Color(0xFFD2C0FF)
                    )
                )
                TextButton(
                    onClick = { onFontSizeChange((fontSizeSp + 1f).coerceIn(16f, 30f)) }
                ) {
                    Text("澧炲ぇ", color = Color.White.copy(alpha = 0.78f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FontSizeBottomSheetV2(
    fontSizeSp: Float,
    onFontSizeChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val roundedFontSize = fontSizeSp.roundToInt()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF15101F),
        scrimColor = Color.Black.copy(alpha = 0.34f),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color.White.copy(alpha = 0.18f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "字号",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "关闭",
                        tint = Color.White.copy(alpha = 0.78f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = "${roundedFontSize}sp",
                color = Color(0xFFF2E8FF),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FontSizeAdjustButton(
                    label = "A-",
                    onClick = { onFontSizeChange((fontSizeSp - 1f).coerceIn(16f, 30f)) }
                )
                Slider(
                    modifier = Modifier.weight(1f),
                    value = roundedFontSize.toFloat(),
                    onValueChange = {
                        onFontSizeChange(it.roundToInt().toFloat().coerceIn(16f, 30f))
                    },
                    valueRange = 16f..30f,
                    steps = 13,
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD2C0FF))
                        )
                    },
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color(0xFF8B5CFF),
                        inactiveTrackColor = Color(0xFF2A2638),
                        thumbColor = Color(0xFFD2C0FF)
                    )
                )
                FontSizeAdjustButton(
                    label = "A+",
                    onClick = { onFontSizeChange((fontSizeSp + 1f).coerceIn(16f, 30f)) }
                )
            }

        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FontSizeBottomSheetV3(
    fontSizeSp: Float,
    onFontSizeChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val roundedFontSize = fontSizeSp.roundToInt()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF15101F),
        scrimColor = Color.Black.copy(alpha = 0.34f),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color.White.copy(alpha = 0.18f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "字号",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "关闭",
                        tint = Color.White.copy(alpha = 0.78f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = "${roundedFontSize}sp",
                color = Color(0xFFF2E8FF),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FontSizeAdjustButton(
                    label = "A-",
                    onClick = { onFontSizeChange((fontSizeSp - 1f).coerceIn(16f, 30f)) }
                )
                ThinMoonSlider(
                    modifier = Modifier.weight(1f),
                    value = roundedFontSize.toFloat(),
                    valueRange = 16f..30f,
                    onValueChange = {
                        onFontSizeChange(it.roundToInt().toFloat().coerceIn(16f, 30f))
                    },
                    inactiveColor = Color(0xFF2A2638),
                    thumbColor = Color(0xFFE7DCFF),
                    trackHeight = 3.dp,
                    thumbSize = 11.dp
                )
                FontSizeAdjustButton(
                    label = "A+",
                    onClick = { onFontSizeChange((fontSizeSp + 1f).coerceIn(16f, 30f)) }
                )
            }
        }
    }
}

@Composable
private fun FontSizeAdjustButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.075f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color(0xFFCDBEFF),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ReaderControlButton(
    label: String,
    onClick: () -> Unit,
    weak: Boolean = false,
    enabled: Boolean = true,
    icon: @Composable () -> Unit
) {
    val itemAlpha = if (enabled) 1f else 0.62f
    val contentColor = if (weak) Color.White.copy(alpha = 0.56f * itemAlpha) else Color(0xFFC3B0FF).copy(alpha = itemAlpha)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (weak) Color.White.copy(alpha = 0.04f) else Color(0xFF171326))
                .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides contentColor
            ) {
                icon()
            }
        }
        Text(label, color = Color.White.copy(alpha = 0.58f * itemAlpha), fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun ReaderActionButton(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val itemAlpha = if (enabled) 1f else 0.62f
    Column(
        modifier = modifier
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides Color(0xFFC3B0FF).copy(alpha = itemAlpha)
        ) {
            icon()
        }
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.62f * itemAlpha),
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingSlider(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            title,
            color = Color.White.copy(alpha = 0.66f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(34.dp)
        )
        Text(
            valueText,
            color = Color(0xFFC3B0FF),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(42.dp)
        )
        Slider(
            modifier = Modifier.weight(1f),
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            thumb = {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD2C0FF))
                )
            },
            colors = SliderDefaults.colors(
                activeTrackColor = Color(0xFF8B5CFF),
                inactiveTrackColor = Color(0xFF2A2638),
                thumbColor = Color(0xFFD2C0FF)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerBottomSheet(
    timerEnabled: Boolean,
    remainingMillis: Long,
    selectedHours: Int,
    selectedMinutes: Int,
    stopMode: SleepTimerStopMode,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    onQuickMinutesSelected: (Int) -> Unit,
    onStopModeChange: (SleepTimerStopMode) -> Unit,
    onStartOrUpdate: () -> Unit,
    onDisableTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    val selectedTotalMinutes = selectedHours * 60 + selectedMinutes
    val statusTitle = when {
        timerEnabled -> "剩余 ${formatSleepTimerDuration(remainingMillis)}"
        selectedTotalMinutes > 0 -> "已选择 ${formatSleepTimerChoice(selectedHours, selectedMinutes)}"
        else -> "未开启定时"
    }
    val statusSubtitle = if (timerEnabled || selectedTotalMinutes > 0) stopMode.label else null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF15111F),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 3.dp)
                    .size(width = 48.dp, height = 5.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color.White.copy(alpha = 0.36f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("定时关闭", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text("到时间后自动停止听书", color = Color.White.copy(alpha = 0.56f), fontSize = 13.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "关闭", tint = Color.White.copy(alpha = 0.86f))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (timerEnabled) 62.dp else 54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.045f))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF7B55FF).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("◷", color = Color(0xFFC3B0FF), fontSize = 25.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        statusTitle,
                        color = Color.White,
                        fontSize = if (timerEnabled) 23.sp else 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (statusSubtitle != null) {
                        Text(statusSubtitle, color = Color.White.copy(alpha = 0.48f), fontSize = 12.sp)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("快捷时间", color = Color.White.copy(alpha = 0.52f), fontSize = 13.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(0, 15, 30, 45, 60).forEach { minutes ->
                        SleepTimerPill(
                            text = if (minutes == 0) "不开启" else "${minutes} 分钟",
                            selected = selectedTotalMinutes == minutes,
                            onClick = { onQuickMinutesSelected(minutes) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("自定义时间", color = Color.White.copy(alpha = 0.52f), fontSize = 13.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.035f))
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SleepTimerRoundButton("−") {
                        if (selectedMinutes > 0) {
                            onMinutesChange(selectedMinutes - 1)
                        } else if (selectedHours > 0) {
                            onHoursChange(selectedHours - 1)
                            onMinutesChange(59)
                        }
                    }
                    Text(selectedHours.toString().padStart(2, '0'), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text("小时", color = Color.White.copy(alpha = 0.52f), fontSize = 13.sp)
                    Box(Modifier.height(28.dp).width(1.dp).background(Color.White.copy(alpha = 0.16f)))
                    Text(selectedMinutes.toString().padStart(2, '0'), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text("分钟", color = Color.White.copy(alpha = 0.52f), fontSize = 13.sp)
                    SleepTimerRoundButton("+") {
                        if (selectedMinutes < 59) {
                            onMinutesChange(selectedMinutes + 1)
                        } else if (selectedHours < 23) {
                            onHoursChange(selectedHours + 1)
                            onMinutesChange(0)
                        }
                    }
                }
                Text("最大 23 小时 59 分钟", color = Color.White.copy(alpha = 0.36f), fontSize = 11.sp)
            }

            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("停止方式", color = Color.White.copy(alpha = 0.52f), fontSize = 13.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SleepTimerModeOption(
                        text = SleepTimerStopMode.IMMEDIATE.label,
                        selected = stopMode == SleepTimerStopMode.IMMEDIATE,
                        onClick = { onStopModeChange(SleepTimerStopMode.IMMEDIATE) },
                        modifier = Modifier.weight(1f)
                    )
                    SleepTimerModeOption(
                        text = SleepTimerStopMode.AFTER_CURRENT_CHAPTER.label,
                        selected = stopMode == SleepTimerStopMode.AFTER_CURRENT_CHAPTER,
                        onClick = { onStopModeChange(SleepTimerStopMode.AFTER_CURRENT_CHAPTER) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SleepTimerActionButton(
                    text = if (timerEnabled) "关闭定时" else "取消",
                    primary = false,
                    onClick = { if (timerEnabled) onDisableTimer() else onDismiss() },
                    modifier = Modifier.weight(1f)
                )
                SleepTimerActionButton(
                    text = if (timerEnabled) "更新定时" else "开始定时",
                    primary = true,
                    onClick = onStartOrUpdate,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SleepTimerPill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(if (selected) Color(0xFF8258FF) else Color.White.copy(alpha = 0.035f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.76f),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SleepTimerRoundButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.White.copy(alpha = 0.88f), fontSize = 22.sp)
    }
}

@Composable
private fun SleepTimerModeOption(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFF8258FF).copy(alpha = 0.1f) else Color.White.copy(alpha = 0.028f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (selected) Color(0xFF9B74FF) else Color.White.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(Color.White))
            }
        }
        Text(
            text = text,
            color = if (selected) Color(0xFFD9CEFF) else Color.White.copy(alpha = 0.72f),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SleepTimerActionButton(text: String, primary: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (primary) Color(0xFF8258FF) else Color.White.copy(alpha = 0.035f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = if (primary) Color.White else Color.White.copy(alpha = 0.82f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatSleepTimerDuration(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

private fun formatSleepTimerChoice(hours: Int, minutes: Int): String {
    return when {
        hours > 0 && minutes > 0 -> "${hours} 小时 ${minutes} 分钟"
        hours > 0 -> "${hours} 小时"
        minutes > 0 -> "${minutes} 分钟"
        else -> "0 分钟"
    }
}

private enum class SleepTimerStopMode(val label: String) {
    IMMEDIATE("到时间立即停止"),
    AFTER_CURRENT_CHAPTER("播完本章再停止")
}

private enum class BookReadingTarget {
    CHAPTER_TITLE,
    SENTENCE
}

private enum class BookPlaybackEngine {
    NONE,
    AISHELL3,
    SYSTEM_TTS,
    FASTSPEECH2
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterCatalogBottomSheet(
    chapters: List<BookChapter>,
    currentChapter: BookChapter?,
    currentParagraphIndex: Int,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onChapterClick: (BookChapter) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val scrollScope = rememberCoroutineScope()
    val initialChapterIndex = remember(chapters, currentChapter) {
        currentChapter?.let { chapter ->
            chapters.indexOfFirst { it.paragraphIndex == chapter.paragraphIndex }.takeIf { it >= 0 }
        }
    }

    LaunchedEffect(Unit) {
        val targetIndex = initialChapterIndex ?: return@LaunchedEffect
        listState.scrollToItem((targetIndex - 3).coerceAtLeast(0))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0B1014),
        contentColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.38f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .background(Color(0xFF0B1014))
                .padding(horizontal = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 14.dp)
                    .width(44.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color.White.copy(alpha = 0.22f))
                    .align(Alignment.CenterHorizontally)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "目录",
                        color = Color.White.copy(alpha = 0.92f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = currentChapter?.let { "当前阅读：${it.title}" } ?: "当前阅读：开头",
                        color = Color(0xFFB9B1D9).copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "共 ${chapters.size} 章",
                        color = Color.White.copy(alpha = 0.74f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "关闭目录",
                            tint = Color.White.copy(alpha = 0.84f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            if (chapters.isEmpty()) {
                ChapterEmptyState(onRefresh = onRefresh)
            } else {
                val showFastScroller = chapters.size >= 20
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = if (showFastScroller) 24.dp else 0.dp),
                        state = listState,
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(
                            items = chapters,
                            key = { _, chapter -> chapter.paragraphIndex }
                        ) { _, chapter ->
                            ChapterRow(
                                chapter = chapter,
                                isCurrent = chapter == currentChapter,
                                onClick = { onChapterClick(chapter) }
                            )
                        }
                    }
                    if (showFastScroller) {
                        ChapterFastScroller(
                            itemCount = chapters.size,
                            currentIndex = listState.firstVisibleItemIndex,
                            onScrollToIndex = { targetIndex ->
                                scrollScope.launch {
                                    listState.scrollToItem(targetIndex)
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterEmptyState(
    onRefresh: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "未识别到章节",
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "可以继续按段落收听",
                color = Color.White.copy(alpha = 0.58f),
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = onRefresh) {
                Text("重新识别", color = Color(0xFFC9B8FF))
            }
        }
    }
}

@Composable
private fun ChapterRow(
    chapter: BookChapter,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val background = if (isCurrent) Color(0xFF24263A) else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(36.dp)
                .background(if (isCurrent) Color(0xFFB58DFF) else Color.Transparent)
        )
        Text(
            text = chapter.title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 12.dp),
            color = Color.White.copy(alpha = if (isCurrent) 0.94f else 0.68f),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


@Composable
private fun ChapterFastScroller(
    itemCount: Int,
    currentIndex: Int,
    onScrollToIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var trackHeightPx by remember { mutableIntStateOf(1) }
    val scrollProgress = if (itemCount <= 1) {
        0f
    } else {
        (currentIndex.toFloat() / (itemCount - 1).toFloat()).coerceIn(0f, 1f)
    }

    fun scrollToOffset(y: Float) {
        val progress = (y / trackHeightPx.toFloat()).coerceIn(0f, 1f)
        val targetIndex = (progress * (itemCount - 1)).roundToInt().coerceIn(0, itemCount - 1)
        onScrollToIndex(targetIndex)
    }

    Column(
        modifier = modifier
            .width(26.dp)
            .padding(vertical = 10.dp)
            .onSizeChanged { size -> trackHeightPx = size.height.coerceAtLeast(1) }
            .pointerInput(itemCount) {
                detectVerticalDragGestures(
                    onDragStart = { offset -> scrollToOffset(offset.y) },
                    onVerticalDrag = { change, _ -> scrollToOffset(change.position.y) }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(scrollProgress.coerceAtLeast(0.001f)))
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Color(0xFF8B5CFF).copy(alpha = 0.72f))
        )
        Spacer(modifier = Modifier.weight((1f - scrollProgress).coerceAtLeast(0.001f)))
    }
}

private fun LocalMediaFile.displayTitle(): String {
    return name.substringBeforeLast('.', name)
}

private fun buildFormalBookReaderPlaybackTarget(
    bookFile: LocalMediaFile?,
    chapters: List<BookChapter>,
    paragraphs: List<String>,
    paragraphIndex: Int,
    sentenceIndex: Int,
    fallbackSentenceText: String? = null,
    dryRunSource: String? = null,
): BookReaderPlaybackTarget? {
    val file = bookFile ?: run {
        Log.d(FORMAL_BOOK_PLAYBACK_TAG, "build target skipped reason=missing_book")
        logFormalDryRunSkipped(dryRunSource, "missing_book")
        return null
    }
    val safeParagraphIndex = paragraphIndex.coerceAtLeast(0)
    val sentenceText = resolveFormalReaderSentenceText(
        paragraphs = paragraphs,
        paragraphIndex = safeParagraphIndex,
        sentenceIndex = sentenceIndex,
        fallbackSentenceText = fallbackSentenceText,
    ) ?: run {
        Log.d(
            FORMAL_BOOK_PLAYBACK_TAG,
            "build target skipped reason=missing_sentence_text paragraphIndex=$paragraphIndex sentenceIndex=$sentenceIndex"
        )
        logFormalDryRunSkipped(dryRunSource, "missing_sentence_text")
        return null
    }
    val chapterIndex = chapters.indexOfLast { it.paragraphIndex <= safeParagraphIndex }
    if (chapterIndex < 0) {
        Log.d(FORMAL_BOOK_PLAYBACK_TAG, "build target skipped reason=missing_chapter_index")
        logFormalDryRunSkipped(dryRunSource, "missing_chapter_index")
        return null
    }
    val chapter = chapters.getOrNull(chapterIndex) ?: run {
        Log.d(FORMAL_BOOK_PLAYBACK_TAG, "build target skipped reason=missing_chapter")
        logFormalDryRunSkipped(dryRunSource, "missing_chapter")
        return null
    }
    val safePreview = sentenceText.replace('\n', ' ').trim().take(40)
    Log.d(
        FORMAL_BOOK_PLAYBACK_TAG,
        "build target text source=reader_sentence paragraphIndex=$safeParagraphIndex " +
            "sentenceIndex=$sentenceIndex textLength=${sentenceText.length} preview=$safePreview"
    )
    Log.d(
        FORMAL_BOOK_PLAYBACK_TAG,
        "build target chapterIndex=$chapterIndex chapterTitle=${chapter.title.take(24)} " +
            "paragraphIndex=$safeParagraphIndex sentenceIndex=$sentenceIndex"
    )
    return BookReaderPlaybackTarget(
        bookId = file.uri,
        bookTitle = file.displayTitle(),
        chapterIndex = chapterIndex,
        chapterTitle = chapter.title.ifBlank { "正文" },
        paragraphIndex = safeParagraphIndex,
        sentenceIndex = sentenceIndex.coerceAtLeast(0),
        clauseIndex = 0,
        sentenceText = sentenceText,
        sentencePreview = safePreview
    )
}

private fun findNextFormalAutoAdvanceTarget(
    bookFile: LocalMediaFile?,
    chapters: List<BookChapter>,
    paragraphs: List<String>,
    completedTarget: BookReaderPlaybackTarget,
): BookReaderPlaybackTarget? {
    if (paragraphs.isEmpty()) return null
    val chapterIndex = completedTarget.chapterIndex
    val chapter = chapters.getOrNull(chapterIndex) ?: return null
    val chapterStart = chapter.paragraphIndex.coerceIn(0, paragraphs.size)
    val chapterEndExclusive = chapters
        .getOrNull(chapterIndex + 1)
        ?.paragraphIndex
        ?.coerceIn(0, paragraphs.size)
        ?: paragraphs.size
    val startParagraphIndex = completedTarget.paragraphIndex
        .coerceIn(chapterStart, (chapterEndExclusive - 1).coerceAtLeast(chapterStart))
    val chapterTitle = chapter.title.trim()

    fun targetFor(paragraphIndex: Int, sentenceIndex: Int, sentence: String): BookReaderPlaybackTarget? {
        return buildFormalBookReaderPlaybackTarget(
            bookFile = bookFile,
            chapters = chapters,
            paragraphs = paragraphs,
            paragraphIndex = paragraphIndex,
            sentenceIndex = sentenceIndex,
            fallbackSentenceText = sentence
        )
    }

    val currentSentences = splitParagraphIntoSentences(paragraphs[startParagraphIndex]).ifEmpty {
        listOf(paragraphs[startParagraphIndex])
    }
    for (nextSentenceIndex in (completedTarget.sentenceIndex + 1) until currentSentences.size) {
        val sentence = currentSentences[nextSentenceIndex]
        if (isReadableBookTtsText(sentence)) {
            return targetFor(startParagraphIndex, nextSentenceIndex, sentence)
        }
    }

    for (nextParagraphIndex in (startParagraphIndex + 1) until chapterEndExclusive) {
        val paragraph = paragraphs.getOrNull(nextParagraphIndex)?.trim().orEmpty()
        if (paragraph.isBlank()) continue
        if (nextParagraphIndex == chapterStart && chapterTitle.isNotBlank() && paragraph == chapterTitle) continue
        val sentences = splitParagraphIntoSentences(paragraph).ifEmpty { listOf(paragraph) }
        val nextSentenceIndex = sentences.indexOfFirst { isReadableBookTtsText(it) }
        if (nextSentenceIndex >= 0) {
            return targetFor(nextParagraphIndex, nextSentenceIndex, sentences[nextSentenceIndex])
        }
    }
    return null
}

private fun resolveFormalReaderSentenceText(
    paragraphs: List<String>,
    paragraphIndex: Int,
    sentenceIndex: Int,
    fallbackSentenceText: String?,
): String? {
    val paragraph = paragraphs.getOrNull(paragraphIndex)?.trim().orEmpty()
    if (paragraph.isBlank()) return fallbackSentenceText?.trim()?.takeIf { it.isNotBlank() }
    val sentences = splitParagraphIntoSentences(paragraph).ifEmpty { listOf(paragraph) }
    return sentences
        .getOrNull(sentenceIndex.coerceAtLeast(0))
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: fallbackSentenceText?.trim()?.takeIf { it.isNotBlank() }
}
private fun logFormalDryRunSkipped(source: String?, reason: String) {
    if (source == null || USE_FORMAL_BOOK_PLAYBACK_CONTROLLER || !LOG_FORMAL_BOOK_PLAYBACK_DRY_RUN) return
    Log.d(FORMAL_BOOK_PLAYBACK_DRY_RUN_TAG, "target skipped reason=$reason source=$source")
}

private fun logFormalDryRunTarget(
    source: String,
    target: BookReaderPlaybackTarget?,
    resumeIfPlaying: Boolean? = null,
) {
    if (target == null) {
        Log.d(FORMAL_BOOK_PLAYBACK_DRY_RUN_TAG, "target skipped reason=missing_target source=$source")
        return
    }
    val resumeSuffix = resumeIfPlaying?.let { " resumeIfPlaying=$it" }.orEmpty()
    Log.d(
        FORMAL_BOOK_PLAYBACK_DRY_RUN_TAG,
        "$source target chapterIndex=${target.chapterIndex} chapterTitle=${target.chapterTitle.take(24)} " +
            "paragraphIndex=${target.paragraphIndex} sentenceIndex=${target.sentenceIndex}$resumeSuffix"
    )
}

private fun buildFormalBookReaderPlaybackTargetForChapter(
    bookFile: LocalMediaFile?,
    chapters: List<BookChapter>,
    paragraphs: List<String>,
    chapterIndex: Int,
    dryRunSource: String? = null,
): BookReaderPlaybackTarget? {
    val chapter = chapters.getOrNull(chapterIndex) ?: run {
        Log.d(FORMAL_BOOK_PLAYBACK_TAG, "build chapter target skipped reason=missing_chapter_index")
        logFormalDryRunSkipped(dryRunSource, "missing_chapter_index")
        return null
    }
    if (paragraphs.isEmpty()) {
        Log.d(FORMAL_BOOK_PLAYBACK_TAG, "build chapter target skipped reason=empty_book")
        logFormalDryRunSkipped(dryRunSource, "empty_book")
        return null
    }
    val chapterStart = chapter.paragraphIndex.coerceIn(0, paragraphs.size)
    val chapterEndExclusive = chapters
        .getOrNull(chapterIndex + 1)
        ?.paragraphIndex
        ?.coerceIn(0, paragraphs.size)
        ?: paragraphs.size
    val chapterTitle = chapter.title.trim()

    for (paragraphIndex in chapterStart until chapterEndExclusive) {
        val paragraph = paragraphs[paragraphIndex].trim()
        if (paragraph.isBlank()) continue
        if (paragraphIndex == chapterStart && chapterTitle.isNotBlank() && paragraph == chapterTitle) {
            continue
        }
        val sentences = splitParagraphIntoSentences(paragraph).ifEmpty { listOf(paragraph) }
        val sentenceIndex = sentences.indexOfFirst { isReadableBookTtsText(it) }
        if (sentenceIndex >= 0) {
            return buildFormalBookReaderPlaybackTarget(
                bookFile = bookFile,
                chapters = chapters,
                paragraphs = paragraphs,
                paragraphIndex = paragraphIndex,
                sentenceIndex = sentenceIndex,
                fallbackSentenceText = sentences[sentenceIndex],
                dryRunSource = dryRunSource
            )
        }
    }

    Log.d(
        FORMAL_BOOK_PLAYBACK_TAG,
        "build chapter target skipped reason=empty_chapter chapterIndex=$chapterIndex"
    )
    logFormalDryRunSkipped(dryRunSource, "empty_chapter")
    return null
}

private object DisabledBookReaderTtsEngine : BookTtsEngine {
    override suspend fun synthesizeToPcm(
        target: BookReaderPlaybackTarget,
        text: String,
        sessionId: Long,
    ): Result<BookTtsPcmResult> {
        Log.d(
            FORMAL_BOOK_PLAYBACK_TAG,
            "formal controller engine disabled sessionId=$sessionId chapterIndex=${target.chapterIndex}"
        )
        return Result.failure(IllegalStateException("FastSpeech2 formal engine is not wired in Phase 3B"))
    }

    override fun release() = Unit
}

private data class ReaderSentence(
    val text: String,
    val paragraphIndex: Int,
    val sentenceIndexInParagraph: Int,
    val chapterSentenceIndex: Int
)

private typealias BookReadStateCache = BookReaderRestoreCache
private typealias BookReaderStableUiSnapshot = BookReaderRestoreCache

private data class BookReaderVisibleViewport(
    val firstVisibleItemIndex: Int,
    val firstVisibleItemScrollOffset: Int,
    val visibleLazyItemIndexes: List<Int>
)

private fun backfillStableUiSnapshot(
    state: BookReadStateCache,
    speechRate: Float,
): BookReadStateCache? {
    if (state.bookUri.isBlank()) return null
    val total = state.cachedChapterSentenceCount.takeIf { it > 0 }
    val index = state.lastChapterSentenceIndex.takeIf { it >= 0 }
    val canonicalState = BookReaderRestorePositionGate.resolveProgressTimeForSnapshotOrPendingHandoff(
        savedProgressValue = state.cachedProgressValue.takeIf { it >= 0f },
        savedProgressMaxValue = state.cachedProgressMaxValue.takeIf { it > 0f },
        savedListenedTimeLabel = state.cachedListenedTimeLabel,
        savedRemainingTimeLabel = state.cachedRemainingTimeLabel,
        chapterSentenceIndex = index,
        totalChapterSentenceCount = total,
    )
    val fallbackTimeLabels = estimateAbsoluteSnapshotTimeLabels(
        visibleText = state.cachedVisibleText,
        chapterSentenceIndex = index,
        totalChapterSentenceCount = total,
        speechRate = speechRate,
    )
    val progressValue = canonicalState.progress?.value ?: state.cachedProgressValue
    val progressMaxValue = canonicalState.progress?.maxValue ?: state.cachedProgressMaxValue
    val listened = canonicalState.listenedTimeLabel
        ?: fallbackTimeLabels?.first
        ?: state.cachedListenedTimeLabel
    val remaining = canonicalState.remainingTimeLabel
        ?: fallbackTimeLabels?.second
        ?: state.cachedRemainingTimeLabel

    if (progressValue < 0f && progressMaxValue <= 0f && listened.isBlank() && remaining.isBlank()) {
        return state
    }

    val canonicalized = state.copy(
        cachedProgressValue = progressValue,
        cachedProgressMaxValue = progressMaxValue,
        cachedListenedTimeLabel = listened,
        cachedRemainingTimeLabel = remaining,
    )
    return canonicalized
}

private fun estimateAbsoluteSnapshotTimeLabels(
    visibleText: List<String>,
    chapterSentenceIndex: Int?,
    totalChapterSentenceCount: Int?,
    speechRate: Float,
): Pair<String, String>? {
    val total = totalChapterSentenceCount?.takeIf { it > 0 } ?: return null
    val index = chapterSentenceIndex?.coerceIn(0, total - 1) ?: return null
    val visibleSentences = visibleText.filter { it.isNotBlank() }
    if (visibleSentences.isEmpty()) return null
    val averageChars = visibleSentences
        .map { text -> text.count { !it.isWhitespace() } }
        .average()
        .takeIf { it.isFinite() && it > 0.0 }
        ?: return null
    val safeSpeechRate = if (speechRate > 0f) speechRate else 1f
    val effectiveRate = (300f * safeSpeechRate).coerceAtLeast(60f)
    fun estimateSeconds(sentenceCount: Int): Int {
        return ((averageChars * sentenceCount * 60.0) / effectiveRate).roundToInt().coerceAtLeast(0)
    }
    val listenedSeconds = estimateSeconds(index)
    val remainingSeconds = estimateSeconds((total - index).coerceAtLeast(0))
    return formatDuration(listenedSeconds) to formatDuration(remainingSeconds)
}

private fun findSnapshotRealContentInitialIndex(
    cachedReadState: BookReadStateCache?,
    chapterSentences: List<ReaderSentence>,
    hasSavedRestorePosition: Boolean,
    hasInitialReaderSnapshot: Boolean,
    hasSettledInitialReaderPosition: Boolean,
): Int? {
    if (!hasSavedRestorePosition || !hasInitialReaderSnapshot || hasSettledInitialReaderPosition) {
        return null
    }
    val state = cachedReadState ?: return null
    return BookReaderRestorePositionGate.findRealContentIndexForSnapshotFirstVisible(
        realSentences = chapterSentences.map { sentence ->
            BookReaderRestorePositionGate.SentenceIdentity(
                paragraphIndex = sentence.paragraphIndex,
                sentenceIndex = sentence.sentenceIndexInParagraph,
                chapterSentenceIndex = sentence.chapterSentenceIndex
            )
        },
        snapshotFirstVisibleParagraphIndex = state.firstVisibleParagraphIndex(),
        snapshotFirstVisibleSentenceIndex = state.firstVisibleSentenceIndex(),
        snapshotFirstVisibleChapterSentenceIndex = state.firstVisibleChapterSentenceIndex(),
        hasChapterTitle = true
    )
}

private fun loadBookReadStateCache(context: Context, bookUri: String?): BookReadStateCache? {
    return BookReaderRestoreCacheSource.load(context, bookUri)
}

private fun saveBookReadStateCache(context: Context, state: BookReadStateCache) {
    BookReaderRestoreCacheSource.save(context, state)
}

private fun buildReaderSentences(
    paragraphs: List<String>,
    startIndex: Int,
    endExclusive: Int,
    chapterTitle: String?
): List<ReaderSentence> {
    if (paragraphs.isEmpty()) return emptyList()
    val safeStart = startIndex.coerceIn(0, paragraphs.size)
    val safeEnd = endExclusive.coerceIn(safeStart, paragraphs.size)
    val title = chapterTitle?.trim().orEmpty()
    val result = mutableListOf<ReaderSentence>()
    for (paragraphIndex in safeStart until safeEnd) {
        val paragraph = paragraphs[paragraphIndex].trim()
        if (paragraph.isBlank()) continue
        if (paragraphIndex == safeStart && title.isNotBlank() && paragraph == title) continue
        splitParagraphIntoSentences(paragraph).forEachIndexed { sentenceIndex, sentence ->
            if (sentence.isNotBlank()) {
                result += ReaderSentence(
                    text = sentence,
                    paragraphIndex = paragraphIndex,
                    sentenceIndexInParagraph = sentenceIndex,
                    chapterSentenceIndex = result.size
                )
            }
        }
    }
    return result
}

private fun splitParagraphIntoSentences(paragraph: String): List<String> {
    val trimmed = paragraph.trim()
    if (trimmed.isBlank()) return emptyList()
    val result = mutableListOf<String>()
    val builder = StringBuilder()
    trimmed.forEach { char ->
        builder.append(char)
        if (char in sentenceBreakChars) {
            val sentence = builder.toString().trim()
            if (sentence.isNotBlank()) result += sentence
            builder.clear()
        }
    }
    val tail = builder.toString().trim()
    if (tail.isNotBlank()) result += tail
    return result
}

private fun isReadableBookTtsText(text: String): Boolean {
    val trimmed = text.trim()
    if (trimmed.isBlank()) return false
    return trimmed.any { it.isLetterOrDigit() }
}

private val sentenceBreakChars = setOf(
    '。', '！', '？', '；', '!', '?', ';', '…'
)

private fun estimateSentenceTimeLabel(
    sentences: List<ReaderSentence>,
    fromIndex: Int,
    toIndex: Int,
    speechRate: Float
): String {
    if (sentences.isEmpty()) return "00:00"
    val start = fromIndex.coerceIn(0, sentences.size)
    val end = toIndex.coerceIn(start, sentences.size)
    val chars = sentences
        .subList(start, end)
        .sumOf { sentence -> sentence.text.count { !it.isWhitespace() } }
    val safeSpeechRate = if (speechRate > 0f) speechRate else 1f
    val effectiveRate = (300f * safeSpeechRate).coerceAtLeast(60f)
    val seconds = ((chars * 60f) / effectiveRate).roundToInt().coerceAtLeast(0)
    return formatDuration(seconds)
}

private fun estimateChapterTimeLabel(
    paragraphs: List<String>,
    fromIndex: Int,
    toIndex: Int,
    speechRate: Float
): String {
    if (paragraphs.isEmpty()) return "00:00"
    val start = fromIndex.coerceIn(0, paragraphs.size)
    val end = toIndex.coerceIn(start, paragraphs.size)
    val chars = paragraphs
        .subList(start, end)
        .sumOf { paragraph -> paragraph.count { !it.isWhitespace() } }
    val safeSpeechRate = if (speechRate > 0f) speechRate else 1f
    val effectiveRate = (300f * safeSpeechRate).coerceAtLeast(60f)
    val seconds = ((chars * 60f) / effectiveRate).roundToInt().coerceAtLeast(0)
    return formatDuration(seconds)
}

internal class BookReaderProgressSeekInteraction {
    private var previewIndex: Int? = null

    fun preview(index: Int): Int? {
        previewIndex = index
        return null
    }

    fun finish(): Int? {
        val commitIndex = previewIndex
        previewIndex = null
        return commitIndex
    }

    fun cancel() {
        previewIndex = null
    }

    fun tap(index: Int): Int = index
}

private fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

private enum class BookPlaybackMode(val label: String) {
    SEQUENTIAL("顺序播放"),
    SINGLE_PARAGRAPH("单段循环"),
    CHAPTER_LOOP("本章循环");

    fun next(): BookPlaybackMode {
        return when (this) {
            SEQUENTIAL -> SINGLE_PARAGRAPH
            SINGLE_PARAGRAPH -> CHAPTER_LOOP
            CHAPTER_LOOP -> SEQUENTIAL
        }
    }
}

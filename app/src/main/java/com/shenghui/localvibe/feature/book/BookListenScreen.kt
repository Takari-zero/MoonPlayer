package com.shenghui.localvibe.feature.book

import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
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
import androidx.compose.runtime.key
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.shenghui.localvibe.core.book.BookChapter
import com.shenghui.localvibe.core.book.BookChapterDetector
import com.shenghui.localvibe.core.book.TxtBookReader
import com.shenghui.localvibe.core.datastore.AppStateStore
import com.shenghui.localvibe.core.scanner.LocalMediaFile
import com.shenghui.localvibe.core.tts.Aishell3SegmentedStreamingTtsEngine
import com.shenghui.localvibe.core.tts.Aishell3VoiceRegistry
import com.shenghui.localvibe.core.tts.BookTtsCacheKey
import com.shenghui.localvibe.core.tts.BuiltInOfflineTtsEngine
import com.shenghui.localvibe.core.tts.BuiltInOfflineTtsResult
import com.shenghui.localvibe.core.tts.BookTtsController
import com.shenghui.localvibe.core.tts.BookTtsVoice
import com.shenghui.localvibe.core.tts.BookSpeechRate
import com.shenghui.localvibe.core.tts.BookTtsPlaybackRateResolver
import com.shenghui.localvibe.core.tts.OfflineTtsAvailability
import com.shenghui.localvibe.core.tts.PcmAudioChunk
import com.shenghui.localvibe.core.tts.StreamingPcmAudioPlayer
import com.shenghui.localvibe.core.tts.StreamingTtsParams
import com.shenghui.localvibe.core.tts.StreamingTtsResult
import com.shenghui.localvibe.core.tts.ToneStreamingTtsEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun BookListenScreen(
    bookFile: LocalMediaFile?,
    initialParagraphIndex: Int,
    onProgressChanged: (String, Int, Int) -> Unit,
    onBeforeSpeak: () -> Unit,
    onBack: () -> Unit,
    matchaRuntimeOwner: BookMatchaRuntimeOwner,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val readerContentCycleCoordinator = remember(coroutineScope) {
        ReaderContentCycleCoordinator(coroutineScope, context.applicationContext.contentResolver)
    }
    val initialReadStateCache = remember(bookFile?.uri) {
        loadBookReadStateCache(context.applicationContext, bookFile?.uri)
    }
    LaunchedEffect(bookFile?.uri, initialReadStateCache) {
        val state = initialReadStateCache
        if (state != null) {
            Log.i(
                BOOK_HOT_TTS_TAG,
                "snapshot hit currentChapterSentenceIndex=${state.lastChapterSentenceIndex} " +
                    "cachedStart=${state.cachedStartChapterSentenceIndex} cachedSize=${state.cachedSentences.size} " +
                    "firstVisible=${state.lastVisibleFirstItemIndex} offset=${state.lastVisibleFirstItemScrollOffset} " +
                    "elapsed=${state.cachedElapsedSeconds} remaining=${state.cachedRemainingSeconds} " +
                    "progress=${state.cachedProgressFraction}"
            )
            Log.d(
                BOOK_RESTORE_TAG,
                "restore saved position bookUri=${bookFile?.uri} " +
                    "paragraph=${state.lastParagraphIndex} " +
                    "sentence=${state.lastSentenceIndexInParagraph} " +
                    "chapterSentence=${state.lastChapterSentenceIndex} " +
                    "target=${state.lastReadingTargetName} " +
                    "chapterTitle=${state.lastChapterTitle}"
            )
        } else {
            Log.d(
                BOOK_RESTORE_TAG,
                "restore saved position missing bookUri=${bookFile?.uri} fallbackParagraph=$initialParagraphIndex"
            )
        }
    }
    var cachedReadState by remember(bookFile?.uri) { mutableStateOf(initialReadStateCache) }
    var paragraphs by remember(bookFile?.uri) { mutableStateOf(emptyList<String>()) }
    var currentParagraphIndex by remember(bookFile?.uri) {
        mutableIntStateOf((initialReadStateCache?.lastParagraphIndex ?: initialParagraphIndex).coerceAtLeast(0))
    }
    var currentSentenceIndexInParagraph by remember(bookFile?.uri) {
        mutableIntStateOf(initialReadStateCache?.lastSentenceIndexInParagraph ?: 0)
    }
    var currentReadingTargetName by rememberSaveable(bookFile?.uri) {
        mutableStateOf(initialReadStateCache?.lastReadingTargetName ?: BookReadingTarget.SENTENCE.name)
    }
    var isLoading by remember(bookFile?.uri) { mutableStateOf(bookFile != null) }
    var loadError by remember(bookFile?.uri) { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackIntentPlaying by remember(bookFile?.uri) { mutableStateOf(false) }
    var wasPlayingBeforeSliderSeek by remember { mutableStateOf(false) }
    var playbackSessionId by remember { mutableLongStateOf(0L) }
    var pendingPlaySessionId by remember { mutableLongStateOf(-1L) }
    var pendingPlayTargetChapterSentenceIndex by remember { mutableIntStateOf(-1) }
    var lastPlaybackRequestTarget by remember { mutableIntStateOf(-1) }
    var lastPlaybackRequestTrigger by remember { mutableStateOf("") }
    var lastPlaybackRequestAtMs by remember { mutableLongStateOf(0L) }
    var activePlaybackEngineName by remember { mutableStateOf(BookPlaybackEngine.NONE.name) }
    var preferredPlaybackEngineName by remember {
        mutableStateOf(BookPlaybackEngine.AISHELL3.name)
    }
    var providerRestoreComplete by remember(bookFile?.uri) { mutableStateOf(false) }
    var preferredPlaybackEngineUserOverride by remember { mutableStateOf(false) }
    var selectedAishell3VoiceId by remember {
        mutableStateOf(Aishell3VoiceRegistry.DEFAULT_AISHELL3_VOICE_ID)
    }
    var isTtsReady by remember { mutableStateOf(false) }
    var isTtsChecking by remember { mutableStateOf(true) }
    var speechRate by remember { mutableFloatStateOf(1.0f) }
    var pitch by remember { mutableFloatStateOf(1.0f) }
    var ttsError by remember { mutableStateOf<String?>(null) }
    var ttsRetryKey by remember { mutableIntStateOf(0) }
    var chapterRefreshKey by remember { mutableIntStateOf(0) }
    var preparedReaderContent by remember(bookFile?.uri) {
        mutableStateOf(PreparedReaderContent())
    }
    var contentReadiness by remember(bookFile?.uri) {
        mutableStateOf(BookContentReadiness.PREVIEW)
    }
    var pendingPlaybackTarget by remember(bookFile?.uri) {
        mutableStateOf<PendingBookPlaybackTarget?>(null)
    }
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
    val currentReadingTarget = remember(currentReadingTargetName) {
        runCatching { BookReadingTarget.valueOf(currentReadingTargetName) }
            .getOrDefault(BookReadingTarget.SENTENCE)
    }
    val chapters = preparedReaderContent.chapters
    val activeChapterSentences = preparedReaderContent.activeSentences
    val sequentialTargetSnapshot = preparedReaderContent.sequentialTargetSnapshot
    val currentChapter = remember(chapters, currentParagraphIndex) {
        chapters.lastOrNull { it.paragraphIndex <= currentParagraphIndex }
    }
    val latestIsPlaying by rememberUpdatedState(isPlaying)
    val latestParagraphs by rememberUpdatedState(paragraphs)
    val latestBookFile by rememberUpdatedState(bookFile)
    val latestSpeechRate by rememberUpdatedState(speechRate)
    val latestPitch by rememberUpdatedState(pitch)
    val latestAishell3VoiceId by rememberUpdatedState(selectedAishell3VoiceId)
    val latestPlaybackMode by rememberUpdatedState(playbackMode)
    val latestPendingStopAfterChapter by rememberUpdatedState(pendingStopAfterChapter)
    val latestPendingStopChapterIndex by rememberUpdatedState(pendingStopChapterIndex)
    val latestChapters by rememberUpdatedState(chapters)
    val latestCurrentParagraphIndex by rememberUpdatedState(currentParagraphIndex)
    val latestCurrentSentenceIndexInParagraph by rememberUpdatedState(currentSentenceIndexInParagraph)
    val latestCurrentReadingTarget by rememberUpdatedState(currentReadingTarget)
    val latestActivePlaybackEngineName by rememberUpdatedState(activePlaybackEngineName)
    val latestPlaybackSessionId by rememberUpdatedState(playbackSessionId)

    fun logBookUiEvent(
        source: String,
        target: Int = -1,
        speechRateValue: Float = speechRate,
        providerValue: String = activePlaybackEngineName
    ) {
        Log.i(
            BOOK_HOT_TTS_TAG,
            "BOOK_UI_EVENT source=$source target=$target session=$playbackSessionId " +
                "intentPlaying=$playbackIntentPlaying isPlaying=$isPlaying " +
                "provider=$providerValue speechRate=$speechRateValue"
        )
    }

    var ttsController by remember { mutableStateOf<BookTtsController?>(null) }
    val sentencePlaybackDispatcher = remember { BookSentencePlaybackDispatcher() }
    val previewLocalPlaybackAdapter = remember {
        PreviewLocalPlaybackProductionAdapter()
    }
    val previewLocalPlaybackHost = remember { PreviewLocalPlaybackHost() }
    val previewLocalPlaybackBridge = remember(previewLocalPlaybackAdapter) {
        PreviewLocalPlaybackProductionBridge(previewLocalPlaybackAdapter, previewLocalPlaybackHost)
    }
    val builtInOfflineTtsEngine = remember { BuiltInOfflineTtsEngine() }
    val aishell3TtsEngine = remember { Aishell3SegmentedStreamingTtsEngine(context.applicationContext) }
    val matchaPlaybackCoordinator = remember(matchaRuntimeOwner) {
        BookMatchaPlaybackCoordinator(matchaRuntimeOwner)
    }
    val matchaBookAvailable = remember(matchaPlaybackCoordinator) { matchaPlaybackCoordinator.isAvailable }
    val matchaAuditionController = remember(context) { BookMatchaAuditionController(context.applicationContext) }
    val matchaAuditionAvailable = remember(matchaAuditionController) { matchaAuditionController.isAvailable }
    val offlineTtsAvailability = remember(context) {
        OfflineTtsAvailability.check(context.applicationContext)
    }
    val appStateStore = remember(context) { AppStateStore(context.applicationContext) }
    val aishell3VoiceRegistry = remember(context) {
        runCatching { Aishell3VoiceRegistry.fromAsset(context.assets) }.getOrNull()
    }
    val preferredPlaybackEngine = remember(preferredPlaybackEngineName) {
        BookPlaybackEngineSelection.parse(
            preferredPlaybackEngineName,
            BookPlaybackEngine.SYSTEM_TTS
        )
    }
    val effectivePlaybackEngine = remember(
        preferredPlaybackEngine,
        offlineTtsAvailability.aishell3Available,
        matchaBookAvailable
    ) {
        BookPlaybackEngineSelection.effective(
            preferredPlaybackEngine,
            offlineTtsAvailability.aishell3Available,
            matchaBookAvailable
        )
    }
    val latestBookPlaybackEngineSnapshot by rememberUpdatedState(
        BookPlaybackEngineSnapshot(preferredPlaybackEngine, effectivePlaybackEngine, matchaBookAvailable)
    )
    var aishell3Player by remember { mutableStateOf<StreamingPcmAudioPlayer?>(null) }
    val aishell3StopJobRef = remember { java.util.concurrent.atomic.AtomicReference<kotlinx.coroutines.Job?>(null) }
    var isAishell3Paused by remember { mutableStateOf(false) }
    var isBuiltInOfflineTtsInitializing by remember { mutableStateOf(false) }
    var builtInOfflineTtsError by remember { mutableStateOf<String?>(null) }
    val bookSpeechRate = remember(speechRate) {
        BookTtsPlaybackRateResolver.resolveLatest(speechRate)
    }
    val latestBookSpeechRate by rememberUpdatedState(bookSpeechRate)
    val latestPreferredPlaybackEngineUserOverride by rememberUpdatedState(preferredPlaybackEngineUserOverride)
    val screenDisposed = remember(bookFile?.uri) { java.util.concurrent.atomic.AtomicBoolean(false) }
    val disposeCleanupStarted = remember(screenDisposed) {
        java.util.concurrent.atomic.AtomicBoolean(false)
    }
    val readerExitCoordinator = remember(screenDisposed) {
        BookReaderExitCoordinator()
    }
    LaunchedEffect(offlineTtsAvailability.aishell3Available) {
        providerRestoreComplete = false
        Log.i(
            "BOOK_PROVIDER_STATE",
            "event=RESTORE_START matchaAvailable=$matchaBookAvailable " +
                "aishell3Available=${offlineTtsAvailability.aishell3Available}"
        )
        Log.i(
            "BOOK_PROVIDER_STATE",
            "event=AVAILABILITY matchaAvailable=$matchaBookAvailable"
        )
        val defaultEngine = if (offlineTtsAvailability.aishell3Available) {
            BookPlaybackEngine.AISHELL3
        } else {
            BookPlaybackEngine.SYSTEM_TTS
        }
        val persistedEngine = appStateStore.loadBookPlaybackEngineName()
        val restoredPreferredEngine = BookPlaybackEngineSelection.parse(persistedEngine, defaultEngine)
        Log.i(
            "BOOK_PROVIDER_STATE",
            "event=RESTORE_VALUE rawValue=${persistedEngine ?: "<null>"} " +
                "mappedProvider=${restoredPreferredEngine.name}"
        )
        val persistedVoiceId = appStateStore.loadBookTtsVoiceId()
        if (!latestPreferredPlaybackEngineUserOverride) {
            preferredPlaybackEngineName = restoredPreferredEngine.name
        }
        selectedAishell3VoiceId = aishell3VoiceRegistry
            ?.resolveOrDefault(persistedVoiceId)
            ?.voiceId
            ?: Aishell3VoiceRegistry.DEFAULT_AISHELL3_VOICE_ID
        val restoredEffectiveEngine = BookPlaybackEngineSelection.effective(
            restoredPreferredEngine,
            offlineTtsAvailability.aishell3Available,
            matchaBookAvailable
        )
        providerRestoreComplete = true
        if (!latestPreferredPlaybackEngineUserOverride && restoredPreferredEngine == BookPlaybackEngine.MATCHA_EXPERIMENTAL && matchaBookAvailable) {
            matchaPlaybackCoordinator.prepareEngine()
        }
        Log.i(
            "BOOK_PROVIDER_STATE",
            "event=RESTORE_COMPLETE preferred=${restoredPreferredEngine.name} " +
                "matchaAvailable=$matchaBookAvailable effective=${restoredEffectiveEngine.name}"
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            if (!disposeCleanupStarted.compareAndSet(false, true)) return@onDispose
            screenDisposed.set(true)
            playbackIntentPlaying = false
            if (!readerExitCoordinator.hasExited) {
                playbackSessionId += 1
            }
            pendingPlaybackTarget = null
            pendingPlaySessionId = -1L
            pendingPlayTargetChapterSentenceIndex = -1
            isAishell3Paused = false
            aishell3TtsEngine.stop()
            val player = aishell3Player
            aishell3Player = null
            if (player != null) {
                val stopJob = CoroutineScope(Dispatchers.IO).launch {
                    player.stop()
                    player.release()
                }
                aishell3StopJobRef.set(stopJob)
            }
            ttsController?.stop()
            builtInOfflineTtsEngine.release()
            matchaPlaybackCoordinator.releasePlaybackState()
            matchaAuditionController.release()
            previewLocalPlaybackAdapter.reset()
            readerContentCycleCoordinator.release()
        }
    }

    fun saveProgress(index: Int, total: Int = paragraphs.size) {
        val file = bookFile ?: return
        if (total <= 0) return
        onProgressChanged(file.uri, index.coerceIn(0, total - 1), total)
    }

    fun stopAishell3Playback(trigger: String = "unknown"): kotlinx.coroutines.Job? {
        isAishell3Paused = false
        aishell3TtsEngine.stop()
        val player = aishell3Player
        aishell3Player = null
        return if (player != null) {
            val stopJob = CoroutineScope(Dispatchers.IO).launch {
                Log.i(
                    BOOK_HOT_TTS_TAG,
                    "audio output stop old requested trigger=$trigger session=$playbackSessionId main=${Looper.myLooper() == Looper.getMainLooper()}"
                )
                player.stop()
                player.release()
                Log.i(BOOK_HOT_TTS_TAG, "audio output release done session=$playbackSessionId trigger=$trigger")
            }
            aishell3StopJobRef.set(stopJob)
            stopJob
        } else {
            null
        }
    }

    suspend fun awaitAishell3OutputSwitch(trigger: String, sessionId: Long) {
        val stopJob = aishell3StopJobRef.get()
        Log.i(
            BOOK_HOT_TTS_TAG,
            "audio output switch begin trigger=$trigger newSession=$sessionId " +
                "hasStopJob=${stopJob != null} stopActive=${stopJob?.isActive == true}"
        )
        if (stopJob != null && stopJob.isActive) {
            withContext(Dispatchers.IO) {
                stopJob.join()
            }
        }
        Log.i(BOOK_HOT_TTS_TAG, "audio output new session start allowed trigger=$trigger session=$sessionId")
    }

    fun aishell3PreparedKey(chapterSentenceIndex: Int, text: String): String {
        val voice = aishell3VoiceRegistry
            ?.resolveOrDefault(latestAishell3VoiceId)
            ?: Aishell3VoiceRegistry.defaultDescriptor()
        return BookTtsCacheKey.aishell3(
            bookUri = bookFile?.uri.orEmpty(),
            chapterSentenceIndex = chapterSentenceIndex,
            textIdentity = text.hashCode(),
            speed = latestBookSpeechRate.sherpaGenerateSpeed,
            pitch = pitch,
            voice = voice
        )
    }

    fun aishell3SegmentKey(chapterSentenceIndex: Int, segmentIndex: Int, segmentText: String): String {
        val voice = aishell3VoiceRegistry
            ?.resolveOrDefault(latestAishell3VoiceId)
            ?: Aishell3VoiceRegistry.defaultDescriptor()
        return BookTtsCacheKey.aishell3Segment(
            bookUri = bookFile?.uri.orEmpty(),
            chapterSentenceIndex = chapterSentenceIndex,
            segmentIndex = segmentIndex,
            textIdentity = segmentText.hashCode(),
            speed = latestBookSpeechRate.sherpaGenerateSpeed,
            pitch = pitch,
            voice = voice
        )
    }

    fun isPlausibleChapterSentenceIndex(index: Int): Boolean {
        return index in 0..50_000
    }

    fun shouldIgnoreDuplicatePlaybackRequest(target: Int, trigger: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        val duplicate = target == lastPlaybackRequestTarget &&
            trigger == lastPlaybackRequestTrigger &&
            now - lastPlaybackRequestAtMs < 500L
        if (duplicate) {
            Log.i(
                BOOK_HOT_TTS_TAG,
                "duplicate playback request ignored trigger=$trigger target=$target"
            )
            return true
        }
        lastPlaybackRequestTarget = target
        lastPlaybackRequestTrigger = trigger
        lastPlaybackRequestAtMs = now
        return false
    }

    fun trimPreparedAishell3Cache(keepKeys: Set<String>) {
        BookAishell3PreparedAudioCache.trim(keepKeys)
    }

    suspend fun waitForPreparedAishell3Audio(key: String, timeoutMs: Long = 2_000L): List<PcmAudioChunk>? {
        val startedAt = SystemClock.elapsedRealtime()
        while (SystemClock.elapsedRealtime() - startedAt < timeoutMs) {
            BookAishell3PreparedAudioCache.get(key)?.let { return it }
            if (!BookAishell3PreparedAudioCache.isPrewarming(key)) return null
            delay(40L)
        }
        return BookAishell3PreparedAudioCache.get(key)
    }

    suspend fun waitForAishell3Segment(key: String, timeoutMs: Long = 2_000L): PcmAudioChunk? {
        val startedAt = SystemClock.elapsedRealtime()
        while (SystemClock.elapsedRealtime() - startedAt < timeoutMs) {
            BookAishell3SegmentAudioCache.get(key)?.let { return it }
            if (!BookAishell3SegmentAudioCache.isPrewarming(key)) return null
            delay(40L)
        }
        return BookAishell3SegmentAudioCache.get(key)
    }

    suspend fun synthesizeAishell3SegmentForCache(
        key: String,
        segmentText: String,
        segmentIndex: Int,
        isFinal: Boolean,
        source: String,
        waitTimeoutMs: Long = 6_000L
    ): PcmAudioChunk? {
        if (BookAishell3SegmentAudioCache.get(key) != null) {
            Log.i(BOOK_HOT_TTS_TAG, "segment cache hit key=$key source=$source")
            return BookAishell3SegmentAudioCache.get(key)
        }
        if (!BookAishell3SegmentAudioCache.markPrewarming(key)) {
            if (segmentIndex == 0) {
                Log.i(BOOK_HOT_TTS_TAG, "segment0 in-flight wait session=$source key=$key")
            }
            Log.i(BOOK_HOT_TTS_TAG, "play wait in-flight segment$segmentIndex key=$key")
            val waitStartedAt = SystemClock.elapsedRealtime()
            val waited = waitForAishell3Segment(key, timeoutMs = waitTimeoutMs)
            if (waited != null) {
                if (segmentIndex == 0) {
                    Log.i(
                        BOOK_HOT_TTS_TAG,
                        "play auto start after segment0 ready session=$source waitMs=${SystemClock.elapsedRealtime() - waitStartedAt}"
                    )
                }
                Log.i(BOOK_HOT_TTS_TAG, "play hit after wait segment$segmentIndex key=$key")
            }
            return waited
        }
        val startedAt = System.currentTimeMillis()
        if (segmentIndex == 0) {
            Log.i(BOOK_HOT_TTS_TAG, "segment0 urgent request session=$source key=$key")
            Log.i(BOOK_HOT_TTS_TAG, "segment0 cache miss session=$source key=$key")
        }
        Log.i(BOOK_HOT_TTS_TAG, "first segment synth start key=$key index=$segmentIndex source=$source")
        Log.i(
            BOOK_HOT_TTS_TAG,
            "SYNTH_REQUEST purpose=${if (source.startsWith("play")) "playback" else "prewarm"} " +
                "session=$source sentenceIndex=$segmentIndex rate=${latestBookSpeechRate.multiplier} " +
                "mappedSpeed=${latestBookSpeechRate.sherpaGenerateSpeed}"
        )
        val result = aishell3TtsEngine.synthesizeSegmentToChunk(
            segmentText = segmentText,
            params = latestBookSpeechRate.asStreamingParams(
                voiceId = latestAishell3VoiceId,
                pitch = pitch,
                volume = 1f
            ),
            sessionLabel = source,
            segmentIndex = segmentIndex,
            isFinal = isFinal
        )
        BookAishell3SegmentAudioCache.unmarkPrewarming(key)
        return result
                .onSuccess { chunk ->
                    BookAishell3SegmentAudioCache.put(key, chunk, source = source)
                    Log.i(
                        BOOK_HOT_TTS_TAG,
                        "SYNTH_FIRST_PCM purpose=${if (source.startsWith("play")) "playback" else "prewarm"} " +
                            "session=$source rate=${latestBookSpeechRate.multiplier} pcmSamples=${chunk.data.size / 2}"
                    )
                    if (segmentIndex == 0) {
                    Log.i(
                        BOOK_HOT_TTS_TAG,
                        "segment0 ready session=$source costMs=${System.currentTimeMillis() - startedAt} key=$key"
                    )
                }
                Log.i(
                    BOOK_HOT_TTS_TAG,
                    "first segment ready costMs=${System.currentTimeMillis() - startedAt} key=$key index=$segmentIndex"
                )
            }
            .onFailure { error ->
                Log.i(BOOK_HOT_TTS_TAG, "segment synth failed key=$key index=$segmentIndex error=${error.message}")
            }
            .getOrNull()
    }

    fun aishell3PrewarmRequest() = BookAishell3PrewarmGuard.begin(
        latestProvider = { latestBookPlaybackEngineSnapshot },
        providerSelectionRestored = { providerRestoreComplete },
        latestSessionId = { latestPlaybackSessionId },
        screenDisposed = screenDisposed::get
    )

    fun prewarmAishell3Audio(
        key: String,
        text: String,
        label: String,
        keepKeys: Set<String> = setOf(key),
        onReady: (() -> Unit)? = null
    ) {
        val prewarm = aishell3PrewarmRequest() ?: return
        if (!isReadableBookTtsText(text)) return
        if (!BookAishell3PreparedAudioCache.markPrewarming(key)) {
            val alreadyPrepared = BookAishell3PreparedAudioCache.contains(key)
            Log.i(
                BOOK_HOT_TTS_TAG,
                "prewarm $label skip key=$key prepared=$alreadyPrepared " +
                    "running=${BookAishell3PreparedAudioCache.isPrewarming(key)} " +
                    "prepared cache size=${BookAishell3PreparedAudioCache.size()}"
            )
            if (alreadyPrepared) {
                Log.i(BOOK_HOT_TTS_TAG, "prepared cache restore hit key=$key label=$label")
                prewarm.runIfActive { onReady?.invoke() }
            }
            return
        }
        val startedAt = System.currentTimeMillis()
        Log.i(
            BOOK_HOT_TTS_TAG,
            "prewarm start key=$key label=$label textPreview=${text.take(32)}"
        )
        coroutineScope.launch {
            if (!prewarm.isActive()) {
                BookAishell3PreparedAudioCache.unmarkPrewarming(key)
                return@launch
            }
            val result = aishell3TtsEngine.synthesizeToChunks(
                text = text,
                params = latestBookSpeechRate.asStreamingParams(
                    voiceId = latestAishell3VoiceId,
                    pitch = pitch,
                    volume = 1f
                ),
                isRequestValid = prewarm::isActive
            )
            BookAishell3PreparedAudioCache.unmarkPrewarming(key)
            if (!prewarm.isActive()) return@launch
            result
                .onSuccess { chunks ->
                    if (chunks.isNotEmpty()) {
                        BookAishell3PreparedAudioCache.put(key, chunks, source = "prewarm")
                        trimPreparedAishell3Cache(keepKeys + key)
                        Log.d(
                            "BookReaderPlayback",
                            "tts prewarm $label done costMs=${System.currentTimeMillis() - startedAt} " +
                                "chunks=${chunks.size} bytes=${chunks.sumOf { it.data.size }} " +
                                "prepared cache size=${BookAishell3PreparedAudioCache.size()} " +
                                "prepared cache survives screen dispose=true"
                        )
                        Log.i(
                            BOOK_HOT_TTS_TAG,
                            "prewarm done key=$key label=$label costMs=${System.currentTimeMillis() - startedAt} " +
                                "chunks=${chunks.size} bytes=${chunks.sumOf { it.data.size }} " +
                                "prepared cache size=${BookAishell3PreparedAudioCache.size()} " +
                                "prepared cache survives screen dispose=true"
                        )
                        prewarm.runIfActive { onReady?.invoke() }
                    }
                }
                .onFailure { error ->
                    Log.w("BookReaderPlayback", "tts prewarm $label failed costMs=${System.currentTimeMillis() - startedAt}", error)
                    Log.i(
                        BOOK_HOT_TTS_TAG,
                        "prewarm failed key=$key label=$label error=${error.message}"
                    )
                }
        }
    }

    fun prewarmAishell3Segments(
        chapterSentenceIndex: Int,
        text: String,
        label: String,
        onCurrentFirstReady: (() -> Unit)? = null
    ) {
        val prewarm = aishell3PrewarmRequest() ?: return
        if (!isReadableBookTtsText(text)) return
        val segments = aishell3TtsEngine.splitTextSegments(text)
        if (segments.isEmpty()) return
        Log.i(
            BOOK_HOT_TTS_TAG,
            "segment split sentence=$chapterSentenceIndex segmentCount=${segments.size} firstLen=${segments.first().length}"
        )
        Log.i(BOOK_HOT_TTS_TAG, "segment[0] preview=${segments.first().take(24)}")
        coroutineScope.launch {
            val priorityReadyIndex = if (segments.size > 1) 1 else 0
            segments.forEachIndexed { index, segmentText ->
                if (!prewarm.isActive()) {
                    return@launch
                }
                val key = aishell3SegmentKey(chapterSentenceIndex, index, segmentText)
                val cacheLabel = if (index == 0) "$label segment0" else "$label segment$index"
                if (BookAishell3SegmentAudioCache.contains(key)) {
                    Log.i(BOOK_HOT_TTS_TAG, "segment cache hit key=$key label=$cacheLabel")
                    if (index == priorityReadyIndex) prewarm.runIfActive { onCurrentFirstReady?.invoke() }
                    return@forEachIndexed
                }
                if (!BookAishell3SegmentAudioCache.markPrewarming(key)) {
                    Log.i(
                        BOOK_HOT_TTS_TAG,
                        "prewarm $cacheLabel skip key=$key running=${BookAishell3SegmentAudioCache.isPrewarming(key)}"
                    )
                    if (index == priorityReadyIndex) {
                        waitForAishell3Segment(key, timeoutMs = 6_000L)?.let {
                            prewarm.runIfActive { onCurrentFirstReady?.invoke() }
                        }
                    }
                    return@forEachIndexed
                }
                val startedAt = System.currentTimeMillis()
                if (index == 0) {
                    Log.i(BOOK_HOT_TTS_TAG, "prewarm current segment0 priority key=$key")
                } else {
                    Log.i(BOOK_HOT_TTS_TAG, "prewarm $cacheLabel key=$key")
                }
                val result = aishell3TtsEngine.synthesizeSegmentToChunk(
                    segmentText = segmentText,
                    params = latestBookSpeechRate.asStreamingParams(
                        voiceId = latestAishell3VoiceId,
                        pitch = pitch,
                        volume = 1f
                    ),
                    sessionLabel = "prewarm",
                    segmentIndex = index,
                    isFinal = index == segments.lastIndex,
                    isRequestValid = prewarm::isActive
                )
                BookAishell3SegmentAudioCache.unmarkPrewarming(key)
                if (!prewarm.isActive()) return@launch
                result
                    .onSuccess { chunk ->
                        if (chunk == null) return@launch
                        BookAishell3SegmentAudioCache.put(key, chunk, source = "prewarm")
                        if (index == 0) {
                            Log.i(
                                BOOK_HOT_TTS_TAG,
                                "prewarm current segment0 done costMs=${System.currentTimeMillis() - startedAt} key=$key"
                            )
                        } else {
                            Log.i(
                                BOOK_HOT_TTS_TAG,
                                "prewarm $cacheLabel done costMs=${System.currentTimeMillis() - startedAt} key=$key"
                            )
                        }
                        if (index == priorityReadyIndex) {
                            prewarm.runIfActive { onCurrentFirstReady?.invoke() }
                        }
                    }
                    .onFailure { error ->
                        Log.i(
                            BOOK_HOT_TTS_TAG,
                            "prewarm failed key=$key label=$cacheLabel error=${error.message}"
                        )
                    }
            }
        }
    }

    fun playPreparedAishell3Audio(
        sessionId: Long,
        key: String,
        chunks: List<PcmAudioChunk>,
        paragraphIndex: Int,
        onSuccess: () -> Unit = {},
        onStarted: (Long) -> Unit = {},
        onDrained: (Long) -> Unit = {},
        onFailed: (Long, String) -> Unit = { _, _ -> }
    ) {
        coroutineScope.launch {
            awaitAishell3OutputSwitch(trigger = "prepared playback", sessionId = sessionId)
            if (sessionId != playbackSessionId || screenDisposed.get()) {
                Log.i(
                    BOOK_HOT_TTS_TAG,
                    "audio output write ignored old session=$sessionId trigger=prepared playback"
                )
                return@launch
            }
            val player = StreamingPcmAudioPlayer()
            aishell3Player = player
            val clickStartMs = System.currentTimeMillis()

            suspend fun isPreparedSessionActive(): Boolean = withContext(Dispatchers.Main.immediate) {
                sessionId == playbackSessionId && !screenDisposed.get()
            }

            suspend fun releasePreparedPlayer() {
                withContext(Dispatchers.IO) {
                    player.release()
                }
                withContext(Dispatchers.Main.immediate) {
                    if (aishell3Player === player) {
                        aishell3Player = null
                    }
                }
            }

            try {
                val completed = withContext(Dispatchers.IO) {
                    var playbackDurationMs = 0L
                    var playbackStartedAtMs = 0L
                    var playbackStarted = false
                    Log.i(
                        BOOK_HOT_TTS_TAG,
                        "prepared audio write start session=$sessionId main=${Looper.myLooper() == Looper.getMainLooper()} chunks=${chunks.size}"
                    )
                    for ((index, chunk) in chunks.withIndex()) {
                        if (!isPreparedSessionActive()) {
                            Log.i(
                                BOOK_HOT_TTS_TAG,
                                "prepared audio write ignored old session=$sessionId key=$key index=$index"
                            )
                            player.release()
                            return@withContext false
                        }
                        if (!playbackStarted) {
                            val startResult = player.startSession(sessionId, chunk.format)
                            if (startResult.isFailure) {
                                throw IllegalStateException(startResult.exceptionOrNull()?.message ?: "AudioTrack 启动失败")
                            }
                            playbackStarted = true
                            playbackStartedAtMs = System.currentTimeMillis()
                            withContext(Dispatchers.Main.immediate) {
                                if (sessionId == playbackSessionId && !screenDisposed.get()) {
                                    activePlaybackEngineName = BookPlaybackEngine.AISHELL3.name
                                    isAishell3Paused = false
                                    isPlaying = true
                                    saveProgress(paragraphIndex)
                                    onStarted(sessionId)
                                }
                            }
                            Log.d(
                                "BookReaderPlayback",
                                "audio track play start prepared key=$key costMs=${System.currentTimeMillis() - clickStartMs}"
                            )
                        }
                        Log.i(
                            BOOK_HOT_TTS_TAG,
                            "prepared audio write chunk session=$sessionId main=${Looper.myLooper() == Looper.getMainLooper()} size=${chunk.data.size}"
                        )
                        val writeResult = player.write(sessionId, chunk.copy(isFinal = index == chunks.lastIndex))
                        if (writeResult.isFailure) {
                            throw IllegalStateException(writeResult.exceptionOrNull()?.message ?: "AudioTrack 写入失败")
                        }
                        playbackDurationMs += chunk.data.size * 1000L / (chunk.format.sampleRate * 2L)
                    }
                    val drained = player.awaitSessionPlaybackComplete(sessionId)
                    Log.i(
                        BOOK_HOT_TTS_TAG,
                        "prepared audio playback drained session=$sessionId drained=$drained " +
                            "estimatedDurationMs=$playbackDurationMs elapsedMs=${System.currentTimeMillis() - playbackStartedAtMs}"
                    )
                    if (!drained) {
                        player.release()
                        return@withContext false
                    }
                    player.release()
                    Log.i(BOOK_HOT_TTS_TAG, "prepared audio write end session=$sessionId")
                    true
                }
                withContext(Dispatchers.Main.immediate) {
                    if (aishell3Player === player) {
                        aishell3Player = null
                    }
                }
                if (completed && isPreparedSessionActive()) {
                    onDrained(sessionId)
                    onSuccess()
                }
            } catch (error: Throwable) {
                releasePreparedPlayer()
                withContext(Dispatchers.Main.immediate) {
                    if (BookPlaybackSessionGuard.isActive(sessionId, playbackSessionId, screenDisposed.get())) {
                        isPlaying = false
                        activePlaybackEngineName = BookPlaybackEngine.NONE.name
                    } else {
                        Log.i(BOOK_HOT_TTS_TAG, "prepared playback error ignored old session=$sessionId")
                    }
                }
                Log.e("BookReaderPlayback", "prepared playback failed key=$key", error)
                onFailed(sessionId, error.message ?: error::class.java.simpleName)
            }
        }
    }

    fun playSegmentedAishell3Audio(
        sessionId: Long,
        sentenceKey: String,
        chapterSentenceIndex: Int,
        text: String,
        paragraphIndex: Int,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit,
        onStarted: (Long) -> Unit = {},
        onDrained: (Long) -> Unit = {},
        onFailed: (Long, String) -> Unit = { _, _ -> }
    ) {
        coroutineScope.launch {
            val segments = aishell3TtsEngine.splitTextSegments(text)
            if (segments.isEmpty()) {
                onFailed(sessionId, "text is blank")
                onError("text is blank")
                return@launch
            }
            Log.i(
                BOOK_HOT_TTS_TAG,
                "sentence segmented playback start current=$chapterSentenceIndex segments=${segments.size}"
            )
            Log.i(
                BOOK_HOT_TTS_TAG,
                "segment split sentence=$chapterSentenceIndex segmentCount=${segments.size} firstLen=${segments.first().length}"
            )
            Log.i(BOOK_HOT_TTS_TAG, "segment[0] preview=${segments.first().take(24)}")
            awaitAishell3OutputSwitch(trigger = "segmented playback", sessionId = sessionId)
            if (sessionId != playbackSessionId || screenDisposed.get()) {
                Log.i(BOOK_HOT_TTS_TAG, "segmented playback ignored old session=$sessionId")
                return@launch
            }

            val player = StreamingPcmAudioPlayer()
            aishell3Player = player
            val clickStartMs = System.currentTimeMillis()
            var playbackDurationMs = 0L
            var playbackStartedAtMs = 0L
            var playbackStarted = false
            var firstWriteLogged = false
            val synthesizedChunks = mutableListOf<PcmAudioChunk>()

            suspend fun isSegmentSessionActive(): Boolean = withContext(Dispatchers.Main.immediate) {
                sessionId == playbackSessionId && !screenDisposed.get()
            }

            suspend fun hasPendingSegmentPlayIntent(): Boolean = withContext(Dispatchers.Main.immediate) {
                pendingPlaySessionId == sessionId &&
                    pendingPlayTargetChapterSentenceIndex == chapterSentenceIndex &&
                    !screenDisposed.get()
            }

            try {
                for ((index, segmentText) in segments.withIndex()) {
                    if (!isSegmentSessionActive()) {
                        Log.i(BOOK_HOT_TTS_TAG, "segmented playback ignored old session=$sessionId index=$index")
                        break
                    }
                    val segmentKey = aishell3SegmentKey(chapterSentenceIndex, index, segmentText)
                    val cached = BookAishell3SegmentAudioCache.get(segmentKey)
                    val chunk = if (cached != null) {
                        if (index == 0) {
                            Log.i(BOOK_HOT_TTS_TAG, "segment0 cache hit session=$sessionId key=$segmentKey")
                        }
                        Log.i(BOOK_HOT_TTS_TAG, "segment cache hit key=$segmentKey")
                        cached.copy(isFinal = index == segments.lastIndex)
                    } else {
                        if (index == 0) {
                            Log.i(BOOK_HOT_TTS_TAG, "segment0 cache miss session=$sessionId key=$segmentKey")
                        }
                        Log.i(BOOK_HOT_TTS_TAG, "segment cache miss key=$segmentKey")
                        synthesizeAishell3SegmentForCache(
                            key = segmentKey,
                            segmentText = segmentText,
                            segmentIndex = index,
                            isFinal = index == segments.lastIndex,
                            source = "play session=$sessionId",
                            waitTimeoutMs = if (index == 0) 15_000L else 6_000L
                        ) ?: throw IllegalStateException("segment synth failed index=$index")
                    }

                    if (!isSegmentSessionActive()) {
                        Log.i(BOOK_HOT_TTS_TAG, "segment write ignored old session=$sessionId index=$index")
                        break
                    }
                    if (index == 0 && !hasPendingSegmentPlayIntent()) {
                        Log.i(
                            BOOK_HOT_TTS_TAG,
                            "play intent ignored old session=$sessionId target=$chapterSentenceIndex"
                        )
                        break
                    }

                    synthesizedChunks += chunk
                    if (!playbackStarted) {
                        val startResult = withContext(Dispatchers.IO) {
                            player.startSession(sessionId, chunk.format)
                        }
                        if (startResult.isFailure) {
                            throw IllegalStateException(startResult.exceptionOrNull()?.message ?: "AudioTrack 启动失败")
                        }
                        playbackStarted = true
                        playbackStartedAtMs = System.currentTimeMillis()
                        withContext(Dispatchers.Main.immediate) {
                            if (sessionId == playbackSessionId && !screenDisposed.get()) {
                                activePlaybackEngineName = BookPlaybackEngine.AISHELL3.name
                                isAishell3Paused = false
                                isPlaying = true
                                saveProgress(paragraphIndex)
                                onStarted(sessionId)
                            }
                        }
                        Log.i(
                            BOOK_HOT_TTS_TAG,
                            "first audio start costMs=${System.currentTimeMillis() - clickStartMs} current=$chapterSentenceIndex"
                        )
                    }
                    if (index == 0) {
                        Log.i(BOOK_HOT_TTS_TAG, "segment0 enqueue audio session=$sessionId key=$segmentKey")
                    }

                    Log.i(
                        BOOK_HOT_TTS_TAG,
                        "next segment ready index=$index costMs=${System.currentTimeMillis() - clickStartMs}"
                    )
                    val writeResult = withContext(Dispatchers.IO) {
                        Log.i(
                            BOOK_HOT_TTS_TAG,
                            "prepared audio write start main=${Looper.myLooper() == Looper.getMainLooper()} session=$sessionId segment=$index"
                        )
                        player.write(sessionId, chunk.copy(isFinal = index == segments.lastIndex))
                    }
                    if (writeResult.isFailure) {
                        throw IllegalStateException(writeResult.exceptionOrNull()?.message ?: "AudioTrack 写入失败")
                    }
                    if (!firstWriteLogged) {
                        firstWriteLogged = true
                        Log.i(
                            BOOK_HOT_TTS_TAG,
                            "PCM_FIRST_WRITE purpose=playback session=$sessionId " +
                                "playerId=${System.identityHashCode(player)} rate=${latestBookSpeechRate.multiplier} " +
                                "bytes=${chunk.data.size}"
                        )
                    }
                    playbackDurationMs += chunk.data.size * 1000L / (chunk.format.sampleRate * 2L)
                    if (index + 1 < segments.size) {
                        Log.i(BOOK_HOT_TTS_TAG, "next segment synth queued index=${index + 1}")
                    }
                }

                if (playbackStarted) {
                    val drained = player.awaitSessionPlaybackComplete(sessionId)
                    Log.i(
                        BOOK_HOT_TTS_TAG,
                        "segmented playback drained session=$sessionId drained=$drained " +
                            "estimatedDurationMs=$playbackDurationMs elapsedMs=${System.currentTimeMillis() - playbackStartedAtMs}"
                    )
                    if (!drained) {
                        withContext(Dispatchers.IO) {
                            player.release()
                        }
                        return@launch
                    }
                }

                withContext(Dispatchers.IO) {
                    player.release()
                }
                withContext(Dispatchers.Main.immediate) {
                    if (aishell3Player === player) {
                        aishell3Player = null
                    }
                }
                if (synthesizedChunks.isNotEmpty()) {
                    BookAishell3PreparedAudioCache.put(sentenceKey, synthesizedChunks.toList(), source = "auto-play")
                }
                if (isSegmentSessionActive()) {
                    Log.i(BOOK_HOT_TTS_TAG, "sentence segmented playback end current=$chapterSentenceIndex")
                    onDrained(sessionId)
                    onSuccess()
                }
            } catch (error: Throwable) {
                withContext(Dispatchers.IO) {
                    player.release()
                }
                withContext(Dispatchers.Main.immediate) {
                    if (aishell3Player === player) {
                        aishell3Player = null
                    }
                    if (BookPlaybackSessionGuard.isActive(sessionId, playbackSessionId, screenDisposed.get())) {
                        isPlaying = false
                        activePlaybackEngineName = BookPlaybackEngine.NONE.name
                    } else {
                        Log.i(BOOK_HOT_TTS_TAG, "segmented playback error ignored old session=$sessionId")
                    }
                }
                Log.e("BookReaderPlayback", "segmented playback failed key=$sentenceKey", error)
                onFailed(sessionId, error.message ?: error::class.java.simpleName)
                onError(error.message ?: error::class.java.simpleName)
            }
        }
    }

    val providerRouter = BookSentencePlaybackProviderRouter(
        context = context,
        coroutineScope = coroutineScope,
        dispatcher = sentencePlaybackDispatcher,
        matchaPlaybackCoordinator = matchaPlaybackCoordinator,
        ttsController = { ttsController },
        pitch = { pitch },
        isTtsReady = { isTtsReady },
        currentPlaybackSessionId = { playbackSessionId },
        preparedKey = ::aishell3PreparedKey,
        isPrewarming = BookAishell3PreparedAudioCache::isPrewarming,
        getPrepared = BookAishell3PreparedAudioCache::get,
        waitPrepared = ::waitForPreparedAishell3Audio,
        playPrepared = { sessionId, key, chunks, paragraphIndex, onStarted, onDrained, onFailed ->
            playPreparedAishell3Audio(
                sessionId = sessionId,
                key = key,
                chunks = chunks,
                paragraphIndex = paragraphIndex,
                onStarted = onStarted,
                onDrained = onDrained,
                onFailed = onFailed
            )
        },
        playSegmented = { sessionId, key, chapterSentenceIndex, text, paragraphIndex, onError, onStarted, onDrained, onFailed ->
            playSegmentedAishell3Audio(
                sessionId = sessionId,
                sentenceKey = key,
                chapterSentenceIndex = chapterSentenceIndex,
                text = text,
                paragraphIndex = paragraphIndex,
                onError = onError,
                onStarted = onStarted,
                onDrained = onDrained,
                onFailed = onFailed
            )
        }
    )

    fun stopCurrentPlayback(reason: String, invalidateSession: Boolean = true) {
        if (invalidateSession) {
            playbackSessionId += 1
        }
        previewLocalPlaybackAdapter.reset()
        Log.d(
            "BookReaderPlayback",
            "stop current session reason=$reason sessionId=$playbackSessionId engine=$activePlaybackEngineName"
        )
        stopAishell3Playback(trigger = reason)
        matchaPlaybackCoordinator.stop()
        ttsController?.stop()
        activePlaybackEngineName = BookPlaybackEngine.NONE.name
    }

    fun exitReader(reason: String) {
        if (!readerExitCoordinator.tryStartExit()) return
        if (paragraphs.isNotEmpty()) saveProgress(currentParagraphIndex)
        stopCurrentPlayback(reason = reason, invalidateSession = true)
        playbackIntentPlaying = false
        pendingPlaybackTarget = null
        pendingPlaySessionId = -1L
        pendingPlayTargetChapterSentenceIndex = -1
        isAishell3Paused = false
        isPlaying = false
        onBack()
    }

    BackHandler {
        exitReader("back")
    }

    fun selectPreferredPlaybackEngine(engine: BookPlaybackEngine) {
        logBookUiEvent("PROVIDER_CHANGE", providerValue = engine.name)
        if (engine == BookPlaybackEngine.AISHELL3 && !offlineTtsAvailability.aishell3Available) {
            Toast.makeText(context, "自研离线不可用：${offlineTtsAvailability.aishell3UnavailableReason ?: "资源未安装"}", Toast.LENGTH_LONG).show()
            return
        }
        if (preferredPlaybackEngine == engine) {
            preferredPlaybackEngineUserOverride = true
            if (engine == BookPlaybackEngine.MATCHA_EXPERIMENTAL) matchaPlaybackCoordinator.prepareEngine()
            Log.i(
                "BOOK_PROVIDER_STATE",
                "event=SELECT selected=${engine.name}"
            )
            return
        }
        stopCurrentPlayback(reason = "provider_change", invalidateSession = true)
        val clearedPendingPlaybackIntent = PendingBookPlaybackTargetResolver.clearPendingPlaybackIntent()
        pendingPlaybackTarget = clearedPendingPlaybackIntent.target
        pendingPlaySessionId = clearedPendingPlaybackIntent.sessionId
        pendingPlayTargetChapterSentenceIndex = clearedPendingPlaybackIntent.targetChapterSentenceIndex
        playbackIntentPlaying = false
        isPlaying = false
        preferredPlaybackEngineUserOverride = true
        preferredPlaybackEngineName = engine.name
        if (engine == BookPlaybackEngine.MATCHA_EXPERIMENTAL) matchaPlaybackCoordinator.prepareEngine()
        Log.i(
            "BOOK_PROVIDER_STATE",
            "event=SELECT selected=${engine.name}"
        )
        coroutineScope.launch {
            Log.i(
                "BOOK_PROVIDER_STATE",
                "event=PERSIST_START provider=${engine.name}"
            )
            try {
                appStateStore.saveBookPlaybackEngineName(engine.name)
                Log.i(
                    "BOOK_PROVIDER_STATE",
                    "event=PERSIST_COMPLETE provider=${engine.name}"
                )
            } catch (error: Throwable) {
                Log.i(
                    "BOOK_PROVIDER_STATE",
                    "event=PERSIST_FAILED provider=${engine.name} exception=${error::class.simpleName}"
                )
                Log.e(
                    "BOOK_PROVIDER_STATE",
                    "persist provider failed provider=${engine.name}",
                    error
                )
                throw error
            }
        }
        Toast.makeText(context, "正文播放引擎：${engine.displayName()}", Toast.LENGTH_SHORT).show()
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
        val sessionValid = sessionId == playbackSessionId && !screenDisposed.get()
        if (!sessionValid) {
            Log.d("BookReaderPlayback", "discard stale session sessionId=$sessionId current=$playbackSessionId")
            return
        }
        val fromParagraph = currentParagraphIndex
        val fromSentence = currentSentenceIndexInParagraph
        val moved = moveAfterSpokenTarget()
        Log.d(
            "BookReaderPlayback",
            "playback advance begin sessionId=$sessionId moved=$moved " +
                "fromParagraph=$fromParagraph fromSentence=$fromSentence " +
                "paragraphIndex=$currentParagraphIndex sentenceIndexInParagraph=$currentSentenceIndexInParagraph"
        )
        val shouldContinue = shouldContinuePlaybackAfterTarget(
            moved = moved,
            playbackIntentPlaying = playbackIntentPlaying,
            sessionValid = sessionValid
        )
        Log.d(
            "BookReaderPlayback",
            "playback advance decision moved=$moved shouldContinue=$shouldContinue " +
                "intentPlaying=$playbackIntentPlaying isPlaying=$isPlaying sessionValid=$sessionValid"
        )
        if (shouldContinue) {
            Log.d(
                "BookReaderPlayback",
                "move current sentence reason=playback advance " +
                    "paragraphIndex=$currentParagraphIndex sentenceIndexInParagraph=$currentSentenceIndexInParagraph " +
                    "targetType=$currentReadingTargetName"
            )
            coroutineScope.launch {
                yield()
                withFrameNanos { }
                Log.d(
                    "BookReaderPlayback",
                    "highlight updated paragraphIndex=$currentParagraphIndex " +
                        "sentenceIndexInParagraph=$currentSentenceIndexInParagraph"
                )
                requestSpeakCurrent()
            }
        } else {
            isPlaying = false
            playbackIntentPlaying = BookPlaybackIntent.afterPlaybackAdvance(
                moved = moved,
                playbackIntentPlaying = playbackIntentPlaying
            )
            activePlaybackEngineName = BookPlaybackEngine.NONE.name
            bookFile?.let { file ->
                if (paragraphs.isNotEmpty()) {
                    onProgressChanged(file.uri, currentParagraphIndex.coerceIn(0, paragraphs.lastIndex), paragraphs.size)
                }
            }
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
            onError = { message, callbackSessionId ->
                callbackSessionId?.let { sessionId ->
                    sentencePlaybackDispatcher.notifySystemTtsFailed(sessionId, message)
                }
                val accepted = callbackSessionId == null ||
                    (callbackSessionId == latestPlaybackSessionId && !screenDisposed.get())
                if (!accepted) return@BookTtsController
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
            onStop = { callbackSessionId ->
                callbackSessionId?.let(sentencePlaybackDispatcher::clearSystemTts)
            },
            onDone = { completedSessionId ->
                completedSessionId?.let(sentencePlaybackDispatcher::notifySystemTtsDrained)
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

    var preparedWaitTimeout: (() -> Unit)? = null
    var requestSpeakCurrentWithOptions: (Boolean, PreparedReaderContent?, Boolean, Long?) -> Unit = { _, _, _, _ -> }

    fun dispatchSentencePlayback(
        request: BookSentencePlaybackRequest,
        callbacks: BookSentencePlaybackCallbacks,
        formalCallbacksFor: ((BookSentencePlaybackRequest) -> BookSentencePlaybackCallbacks)? = null,
        skipPreparedWait: Boolean = false,
        onPreparedWaitTimeout: (() -> Unit)? = null
    ) {
        providerRouter.dispatch(
            request = request,
            callbacks = callbacks,
            formalCallbacksFor = formalCallbacksFor,
            skipPreparedWait = skipPreparedWait,
            onPreparedWaitTimeout = onPreparedWaitTimeout ?: {
                preparedWaitTimeout?.invoke()
                Unit
            }
        )
    }

    fun speakCurrentSentence(
        skipPreparedWait: Boolean = false,
        preparedContentOverride: PreparedReaderContent? = null,
        allowFormalContinuation: Boolean = true,
        sessionIdOverride: Long? = null
    ) {
        val playbackEngineSnapshot = latestBookPlaybackEngineSnapshot
        val dispatchEngine = playbackEngineSnapshot.dispatchEngine()
        val localSnapshot = previewLocalPlaybackAdapter.snapshot()
        if (localSnapshot.ownership == PreviewLocalPlaybackOwnership.LOCAL &&
            localSnapshot.playbackSessionId == playbackSessionId &&
            localSnapshot.localPlaybackPaused && playbackIntentPlaying && !isPlaying
        ) {
            val generation = localSnapshot.readerGeneration
            if (activePlaybackEngineName == BookPlaybackEngine.AISHELL3.name && isAishell3Paused) {
                Log.d("BookReaderPlayback", "resume paused local aishell3 sessionId=$playbackSessionId")
                aishell3Player?.resume()
                generation?.let { previewLocalPlaybackAdapter.onLocalResumed(it, playbackSessionId) }
                isAishell3Paused = false
                isPlaying = true
                return
            }
            if (activePlaybackEngineName == BookPlaybackEngine.MATCHA_EXPERIMENTAL.name &&
                matchaPlaybackCoordinator.resume()
            ) {
                generation?.let { previewLocalPlaybackAdapter.onLocalResumed(it, playbackSessionId) }
                Log.i(
                    "PREVIEW_LOCAL_PLAYBACK",
                    "RESUME_ACCEPTED sessionBefore=$playbackSessionId sessionAfter=$playbackSessionId " +
                        "ownership=${localSnapshot.ownership} provider=MATCHA_EXPERIMENTAL"
                )
                isPlaying = true
                return
            }
        }
        if (contentReadiness != BookContentReadiness.PLAYBACK_READY) {
            return
        }
        if (activePlaybackEngineName == BookPlaybackEngine.AISHELL3.name && isAishell3Paused) {
            Log.d("BookReaderPlayback", "resume paused aishell3 sessionId=$playbackSessionId")
            aishell3Player?.resume()
            isAishell3Paused = false
            isPlaying = true
            return
        }
        if (activePlaybackEngineName == BookPlaybackEngine.MATCHA_EXPERIMENTAL.name &&
            !isPlaying && playbackIntentPlaying && matchaPlaybackCoordinator.resume()
        ) {
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

        val targetType = currentReadingTargetName
        val targetParagraphIndex = currentParagraphIndex
        val targetSentenceIndex = currentSentenceIndexInParagraph
        val targetChapterIndex = currentChapterIndexFor(targetParagraphIndex)
        val playbackPreparedContent = preparedContentOverride ?: preparedReaderContent
        val targetChapterSentences = playbackPreparedContent.activeSentences
        val playbackTargetSnapshot = playbackPreparedContent.sequentialTargetSnapshot
        val rawDisplayIndex = if (targetType == BookReadingTarget.CHAPTER_TITLE.name) {
            0
        } else {
            targetChapterSentences.indexOfFirst {
                it.paragraphIndex == targetParagraphIndex &&
                    it.sentenceIndexInParagraph == targetSentenceIndex
            }.coerceAtLeast(0)
        }
        val cachedTargets = cachedReadState?.cachedSentences.orEmpty().map {
            BookPlaybackSentenceTarget(
                paragraphIndex = it.paragraphIndex,
                sentenceIndexInParagraph = it.sentenceIndexInParagraph,
                chapterSentenceIndex = it.chapterSentenceIndex
            )
        }
        val chapterTargets = targetChapterSentences.map {
            BookPlaybackSentenceTarget(
                paragraphIndex = it.paragraphIndex,
                sentenceIndexInParagraph = it.sentenceIndexInParagraph,
                chapterSentenceIndex = it.chapterSentenceIndex
            )
        }
        val targetChapterSentenceIndex = if (targetType == BookReadingTarget.CHAPTER_TITLE.name) {
            0
        } else {
            BookPlaybackTargetResolver.resolveChapterSentenceIndex(
                paragraphIndex = targetParagraphIndex,
                sentenceIndexInParagraph = targetSentenceIndex,
                cachedTargets = cachedTargets,
                chapterTargets = chapterTargets
            ) ?: -1
        }
        val validInFull = targetType == BookReadingTarget.CHAPTER_TITLE.name ||
            targetChapterSentences.any {
                it.paragraphIndex == targetParagraphIndex &&
                    it.sentenceIndexInParagraph == targetSentenceIndex &&
                    it.chapterSentenceIndex == targetChapterSentenceIndex
            } || cachedTargets.any {
                it.paragraphIndex == targetParagraphIndex &&
                    it.sentenceIndexInParagraph == targetSentenceIndex &&
                    it.chapterSentenceIndex == targetChapterSentenceIndex
            }
        if (!isPlausibleChapterSentenceIndex(targetChapterSentenceIndex) || !validInFull) {
            Log.w(
                BOOK_HOT_TTS_TAG,
                "PLAYBACK_TARGET_REJECTED reason=not_chapter_sentence_index " +
                    "rawDisplayIndex=$rawDisplayIndex resolvedChapterSentenceIndex=$targetChapterSentenceIndex " +
                    "fullSize=${targetChapterSentences.size} " +
                    "paragraph=$targetParagraphIndex sentence=$targetSentenceIndex"
            )
            isPlaying = false
            activePlaybackEngineName = BookPlaybackEngine.NONE.name
            return
        }
        if (sessionIdOverride == null) {
            playbackSessionId += 1
        }
        val sessionId = sessionIdOverride ?: playbackSessionId
        if (!skipPreparedWait && shouldIgnoreDuplicatePlaybackRequest(targetChapterSentenceIndex, "play button")) {
            return
        }
        val preparedKey = aishell3PreparedKey(targetChapterSentenceIndex, textToSpeak.orEmpty())
        pendingPlaySessionId = sessionId
        pendingPlayTargetChapterSentenceIndex = targetChapterSentenceIndex
        Log.i(
            BOOK_HOT_TTS_TAG,
            "play intent latched session=$sessionId target=$targetChapterSentenceIndex"
        )
        val textPreview = textToSpeak.orEmpty().replace('\n', ' ').take(40)
        val containsPlayKey = BookAishell3PreparedAudioCache.contains(preparedKey)
        Log.i(
            BOOK_HOT_TTS_TAG,
            "play click currentIndex=$targetChapterSentenceIndex targetType=$targetType " +
                "textPreview=$textPreview"
        )
        Log.i(
            BOOK_HOT_TTS_TAG,
            "play click key=$preparedKey cache contains play key=$containsPlayKey " +
                BookAishell3PreparedAudioCache.dumpSummary()
        )
        Log.d(
            "BookReaderPlayback",
            "playbackRequested sessionId=$sessionId targetType=$targetType " +
                "paragraphIndex=$targetParagraphIndex sentenceIndexInParagraph=$targetSentenceIndex " +
                "chapterIndex=$targetChapterIndex currentChapterSentenceIndex=$targetChapterSentenceIndex " +
                "textPreview=$textPreview"
        )
        stopAishell3Playback(trigger = "play button")
        ttsController?.stop()
        activePlaybackEngineName = BookPlaybackEngine.NONE.name
        onBeforeSpeak()

        val playbackRequest = BookSentencePlaybackRequest(
            target = BookPlaybackTargetId(
                chapterSentenceIndex = targetChapterSentenceIndex,
                paragraphIndex = targetParagraphIndex,
                sentenceIndexInParagraph = targetSentenceIndex,
                isChapterTitle = targetType == BookReadingTarget.CHAPTER_TITLE.name
            ),
            text = textToSpeak.orEmpty(),
            provider = dispatchEngine,
            speechRate = latestBookSpeechRate.multiplier,
            playbackSessionId = sessionId,
            preparedSnapshot = playbackTargetSnapshot,
            allowNextPrewarm = true,
            playbackIntentPlaying = playbackIntentPlaying
        )
        fun callbacksFor(request: BookSentencePlaybackRequest) = BookSentencePlaybackCallbacks(
            onStarted = { callbackSessionId ->
                if (callbackSessionId == playbackSessionId && !screenDisposed.get()) {
                    activePlaybackEngineName = request.provider.name
                    isPlaying = true
                    saveProgress(request.target.paragraphIndex)
                }
            },
            onDrained = { callbackSessionId ->
                if (callbackSessionId != playbackSessionId || screenDisposed.get()) return@BookSentencePlaybackCallbacks
                if (request.provider == BookPlaybackEngine.SYSTEM_TTS &&
                    latestActivePlaybackEngineName != BookPlaybackEngine.SYSTEM_TTS.name
                ) {
                    Log.d(
                        "BookReaderPlayback",
                        "ignore system onDone activeEngine=$latestActivePlaybackEngineName"
                    )
                    return@BookSentencePlaybackCallbacks
                }
                if (!latestIsPlaying || latestParagraphs.isEmpty()) return@BookSentencePlaybackCallbacks
                isPlaying = false
                activePlaybackEngineName = BookPlaybackEngine.NONE.name
                if (!allowFormalContinuation) {
                    playbackIntentPlaying = false
                    Log.i(
                        "PREVIEW_LOCAL_PLAYBACK",
                        "NEXT_SUPPRESSED_PHASE session=$callbackSessionId reason=prepared_fallback"
                    )
                    return@BookSentencePlaybackCallbacks
                }
                continuePlaybackAfterCurrentTarget(callbackSessionId)
            },
            onFailed = { callbackSessionId, reason ->
                if (!BookPlaybackSessionGuard.isActive(callbackSessionId, playbackSessionId, screenDisposed.get())) return@BookSentencePlaybackCallbacks
                isPlaying = false
                activePlaybackEngineName = BookPlaybackEngine.NONE.name
                if (request.provider == BookPlaybackEngine.SYSTEM_TTS) {
                    ttsError = reason
                    showVoicePackageSheet = true
                }
            }
        )
        dispatchSentencePlayback(
            request = playbackRequest,
            callbacks = callbacksFor(playbackRequest),
            formalCallbacksFor = ::callbacksFor,
            skipPreparedWait = skipPreparedWait
        )
    }

    requestSpeakCurrentWithOptions = { skipPreparedWait, preparedContentOverride, allowFormalContinuation, sessionIdOverride ->
        speakCurrentSentence(
            skipPreparedWait = skipPreparedWait,
            preparedContentOverride = preparedContentOverride,
            allowFormalContinuation = allowFormalContinuation,
            sessionIdOverride = sessionIdOverride
        )
    }
    previewLocalPlaybackHost.screenDisposed = { screenDisposed.get() }
    previewLocalPlaybackHost.fullReaderReady = {
        contentReadiness == BookContentReadiness.PLAYBACK_READY && paragraphs.isNotEmpty()
    }
    previewLocalPlaybackHost.preparedContent = { preparedReaderContent }
    previewLocalPlaybackHost.currentReaderGeneration = {
        readerContentCycleCoordinator.currentCycleSnapshot()?.generation
    }
    previewLocalPlaybackHost.preparedBookUri = { bookFile?.uri }
    previewLocalPlaybackHost.currentPlaybackSessionId = { playbackSessionId }
    previewLocalPlaybackHost.playbackIntentPlaying = { playbackIntentPlaying }
    previewLocalPlaybackHost.dispatchPlayback = { request, callbacks ->
        dispatchSentencePlayback(request, callbacks, skipPreparedWait = true)
    }
    previewLocalPlaybackHost.onStarted = { provider, paragraphIndex ->
        activePlaybackEngineName = provider.name
        isPlaying = true
        saveProgress(paragraphIndex)
    }
    previewLocalPlaybackHost.onStopped = {
        isPlaying = false
        activePlaybackEngineName = BookPlaybackEngine.NONE.name
    }
    previewLocalPlaybackHost.adoptPreparedCurrent = { plan ->
        currentParagraphIndex = plan.paragraphIndex
        currentSentenceIndexInParagraph = plan.sentenceIndexInParagraph
        currentReadingTargetName = BookReadingTarget.SENTENCE.name
        Log.i(
            "PREVIEW_LOCAL_PLAYBACK",
            "ADOPTION_SUCCESS paragraph=${plan.paragraphIndex} " +
                "sentence=${plan.sentenceIndexInParagraph} " +
                "chapterSentence=${plan.chapterSentenceIndex}"
        )
        true
    }
    previewLocalPlaybackHost.onContinueFormalNext = { session ->
        Log.i(
            "PREVIEW_LOCAL_PLAYBACK",
            "FORMAL_CONTINUATION session=$session source=FORMAL_PREPARED"
        )
        continuePlaybackAfterCurrentTarget(session)
    }
    previewLocalPlaybackHost.onRestorePreparedFallback = { sentence, prepared ->
        stopCurrentPlayback(reason = "preview_local_fallback", invalidateSession = true)
        currentParagraphIndex = sentence.paragraphIndex
        currentSentenceIndexInParagraph = sentence.sentenceIndexInParagraph
        currentReadingTargetName = BookReadingTarget.SENTENCE.name
        playbackIntentPlaying = true
        requestSpeakCurrentWithOptions(false, prepared, false, playbackSessionId)
    }
    previewLocalPlaybackHost.onFallbackUnavailable = {
        playbackIntentPlaying = false
        isPlaying = false
        previewLocalPlaybackAdapter.reset()
    }
    previewLocalPlaybackHost.onStopAutoContinue = { reason ->
        playbackIntentPlaying = false
        Log.i(
            "PREVIEW_LOCAL_PLAYBACK",
            "STOP_AUTO_CONTINUE reason=$reason"
        )
        stopCurrentPlayback(reason = "preview_local_runtime_failed", invalidateSession = true)
    }

    preparedWaitTimeout = { speakCurrentSentence(skipPreparedWait = true) }

    fun consumePendingPlaybackTarget(preparedContentOverride: PreparedReaderContent? = null) {
        val pending = pendingPlaybackTarget ?: return
        val playbackEngineSnapshot = latestBookPlaybackEngineSnapshot
        val preparedContent = preparedContentOverride ?: preparedReaderContent
        val preparedTargets = preparedContent.activeSentences.map {
            BookPlaybackSentenceTarget(
                paragraphIndex = it.paragraphIndex,
                sentenceIndexInParagraph = it.sentenceIndexInParagraph,
                chapterSentenceIndex = it.chapterSentenceIndex
            )
        }
        val preparedAvailable = contentReadiness == BookContentReadiness.PLAYBACK_READY &&
            (preparedContentOverride != null || paragraphs.isNotEmpty())
        Log.i(
            BOOK_HOT_TTS_TAG,
            "PENDING_TARGET_CONSUME paragraph=${pending.paragraphIndex} " +
                "sentence=${pending.sentenceIndexInParagraph} readiness=$contentReadiness " +
                "preparedAvailable=$preparedAvailable preparedTargetCount=${preparedTargets.size} " +
                "activeTargetCount=${activeChapterSentences.size}"
        )
        Log.i(
            "BOOK_PROVIDER_STATE",
            "event=PENDING_CONSUME preferred=${playbackEngineSnapshot.preferred.name} effective=${playbackEngineSnapshot.effective.name} " +
                "matchaAvailable=${playbackEngineSnapshot.matchaAvailable} restoreComplete=$providerRestoreComplete"
        )
        val result = PendingBookPlaybackTargetResolver.consume(
            pending = pending,
            readiness = contentReadiness,
            preparedAvailable = preparedAvailable,
            preparedTargets = preparedTargets,
            currentPlaybackIntentPlaying = playbackIntentPlaying
        )
        if (result.preparedNotReady) {
            return
        }
        if (result.terminalRejected) {
            pendingPlaybackTarget = null
            pendingPlaySessionId = -1L
            pendingPlayTargetChapterSentenceIndex = -1
            playbackIntentPlaying = false
            Log.w(
                BOOK_HOT_TTS_TAG,
                "PENDING_TARGET_REJECTED paragraph=${pending.paragraphIndex} " +
                    "sentence=${pending.sentenceIndexInParagraph} reason=prepared_target_missing"
            )
            return
        }
        val resolved = result.resolvedTarget ?: return
        val resolvedSentence = preparedContent.activeSentences.firstOrNull {
            it.paragraphIndex == resolved.paragraphIndex &&
                it.sentenceIndexInParagraph == resolved.sentenceIndexInParagraph
        }
        if (resolvedSentence == null) {
            pendingPlaybackTarget = null
            pendingPlaySessionId = -1L
            pendingPlayTargetChapterSentenceIndex = -1
            playbackIntentPlaying = false
            Log.w(BOOK_HOT_TTS_TAG, "PENDING_TARGET_REJECTED reason=sentence_missing")
            return
        }
        currentParagraphIndex = resolvedSentence.paragraphIndex
        currentSentenceIndexInParagraph = resolvedSentence.sentenceIndexInParagraph
        currentReadingTargetName = BookReadingTarget.SENTENCE.name
        pendingPlaybackTarget = result.pendingTarget
        Log.i(
            BOOK_HOT_TTS_TAG,
            "PENDING_TARGET_RESOLVED paragraph=${resolved.paragraphIndex} " +
                "sentence=${resolved.sentenceIndexInParagraph} " +
                "chapterSentenceIndex=${resolved.chapterSentenceIndex} source=PREPARED_CONTENT"
        )
        playbackIntentPlaying = result.playbackIntentPlaying
        if (result.playbackIntentPlaying) {
            Log.i(
                "BOOK_PROVIDER_STATE",
                "event=PENDING_DISPATCH preferred=${latestBookPlaybackEngineSnapshot.preferred.name} effective=${latestBookPlaybackEngineSnapshot.effective.name} " +
                    "matchaAvailable=${latestBookPlaybackEngineSnapshot.matchaAvailable} source=LATEST_STATE"
            )
            speakCurrentSentence(preparedContentOverride = preparedContent)
        } else {
            playbackIntentPlaying = result.playbackIntentPlaying
        }
    }

    fun pauseReading() {
        pendingPlaybackTarget = pendingPlaybackTarget?.let {
            PendingBookPlaybackTargetResolver.setPlayRequested(it, false)
        }
        pendingPlaySessionId = -1L
        pendingPlayTargetChapterSentenceIndex = -1
        Log.i(BOOK_HOT_TTS_TAG, "play intent cancelled session=$playbackSessionId reason=pause")
        val localSnapshot = previewLocalPlaybackAdapter.snapshot()
        if (localSnapshot.ownership == PreviewLocalPlaybackOwnership.LOCAL &&
            localSnapshot.playbackSessionId == playbackSessionId
        ) {
            if (activePlaybackEngineName == BookPlaybackEngine.MATCHA_EXPERIMENTAL.name &&
                matchaPlaybackCoordinator.pause()
            ) {
                localSnapshot.readerGeneration?.let { generation ->
                    previewLocalPlaybackAdapter.onLocalPaused(generation, playbackSessionId)
                }
                playbackIntentPlaying = false
                Log.i(
                    "PREVIEW_LOCAL_PLAYBACK",
                    "PAUSE_ACCEPTED sessionBefore=$playbackSessionId sessionAfter=$playbackSessionId " +
                        "ownership=${localSnapshot.ownership} provider=MATCHA_EXPERIMENTAL"
                )
                isPlaying = false
                saveProgress(currentParagraphIndex)
                return
            }
            if (activePlaybackEngineName == BookPlaybackEngine.AISHELL3.name && aishell3Player != null) {
                Log.d("BookReaderPlayback", "pause local aishell3 sessionId=$playbackSessionId")
                aishell3Player?.pause()
                localSnapshot.readerGeneration?.let { generation ->
                    previewLocalPlaybackAdapter.onLocalPaused(generation, playbackSessionId)
                }
                playbackIntentPlaying = false
                isAishell3Paused = true
                isPlaying = false
                saveProgress(currentParagraphIndex)
                return
            }
            localSnapshot.readerGeneration?.let { generation ->
                previewLocalPlaybackAdapter.onPlaybackIntentChanged(generation, playbackSessionId, false)
            }
            playbackIntentPlaying = false
            ttsController?.pause()
            ttsController?.stop()
            activePlaybackEngineName = BookPlaybackEngine.NONE.name
            isPlaying = false
            saveProgress(currentParagraphIndex)
            return
        }
        if (activePlaybackEngineName == BookPlaybackEngine.MATCHA_EXPERIMENTAL.name && matchaPlaybackCoordinator.pause()) {
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
        stopAishell3Playback(trigger = "pause")
        ttsController?.pause()
        ttsController?.stop()
        activePlaybackEngineName = BookPlaybackEngine.NONE.name
        isPlaying = false
        saveProgress(currentParagraphIndex)
    }

    fun currentLocalPlaybackSnapshot(): PreviewLocalSessionSnapshot? {
        val snapshot = previewLocalPlaybackAdapter.snapshot()
        val generation = readerContentCycleCoordinator.currentCycleSnapshot()?.generation
        return snapshot.takeIf {
            previewLocalPlaybackAdapter.hasCurrentLocalOwnership(generation, playbackSessionId)
        }
    }

    fun playbackToggleFacts(
        hasPending: Boolean,
        playbackPreparing: Boolean
    ): BookPlaybackToggleFacts {
        val localSnapshot = currentLocalPlaybackSnapshot()
        return BookPlaybackToggleFacts(
            hasCurrentLocalOwnership = localSnapshot != null,
            localPlaybackPaused = localSnapshot?.localPlaybackPaused == true,
            formalIsPlaying = isPlaying,
            playbackIntentPlaying = playbackIntentPlaying,
            contentReadiness = contentReadiness,
            hasPendingPlaybackTarget = hasPending,
            playbackPreparing = playbackPreparing
        )
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

    fun jumpToSentence(sentence: ReaderSentence, autoPlay: Boolean) {
        if (paragraphs.isEmpty()) return
        val validChapterSentences = activeChapterSentences
        val rawDisplayIndex = sentence.chapterSentenceIndex
        val resolvedChapterSentenceIndex = BookPlaybackTargetResolver.resolveChapterSentenceIndex(
            paragraphIndex = sentence.paragraphIndex,
            sentenceIndexInParagraph = sentence.sentenceIndexInParagraph,
            cachedTargets = cachedReadState?.cachedSentences.orEmpty().map {
                BookPlaybackSentenceTarget(
                    paragraphIndex = it.paragraphIndex,
                    sentenceIndexInParagraph = it.sentenceIndexInParagraph,
                    chapterSentenceIndex = it.chapterSentenceIndex
                )
            },
            chapterTargets = validChapterSentences.map {
                BookPlaybackSentenceTarget(
                    paragraphIndex = it.paragraphIndex,
                    sentenceIndexInParagraph = it.sentenceIndexInParagraph,
                    chapterSentenceIndex = it.chapterSentenceIndex
                )
            }
        )
        val validTarget = resolvedChapterSentenceIndex != null &&
            isPlausibleChapterSentenceIndex(resolvedChapterSentenceIndex)
        if (!validTarget) {
            Log.w(
                BOOK_HOT_TTS_TAG,
                "PLAYBACK_TARGET_REJECTED reason=unresolved_display_target " +
                    "rawDisplayIndex=$rawDisplayIndex " +
                    "resolvedChapterSentenceIndex=$resolvedChapterSentenceIndex " +
                    "fullSize=${validChapterSentences.size} cachedSize=${cachedReadState?.cachedSentences?.size ?: 0}"
            )
            return
        }
        val nextIndex = sentence.paragraphIndex.coerceIn(0, paragraphs.lastIndex)
        stopCurrentPlayback(reason = "jump_to_sentence", invalidateSession = true)
        currentParagraphIndex = nextIndex
        currentSentenceIndexInParagraph = sentence.sentenceIndexInParagraph.coerceAtLeast(0)
        currentReadingTargetName = BookReadingTarget.SENTENCE.name
        saveProgress(nextIndex)
        if (autoPlay) {
            playbackIntentPlaying = true
            isPlaying = false
            speakCurrentSentence()
        }
    }

    requestSpeakCurrent = { speakCurrentSentence() }

    LaunchedEffect(bookFile?.uri) {
        if (bookFile == null) {
            isLoading = false
            loadError = "未选择小说文件"
            paragraphs = emptyList()
            preparedReaderContent = PreparedReaderContent()
            return@LaunchedEffect
        }
        contentReadiness = BookContentReadiness.PREVIEW
        pendingPlaybackTarget = null
        isLoading = true
        loadError = null
        isPlaying = false
        stopCurrentPlayback(reason = "book_changed", invalidateSession = true)
        val readerCycle = readerContentCycleCoordinator.beginContentCycle(
            bookUri = bookFile.uri,
            expectedSize = bookFile.size,
            modifiedAt = bookFile.modifiedAt,
            cachedVersion = initialReadStateCache?.contentVersion ?: PreviewContentVersion.Unavailable
        )
        readerContentCycleCoordinator.startCurrentVersionShadow(readerCycle)
        val result = TxtBookReader.readBookWithFingerprint(context.applicationContext, bookFile.uri)
        if (result.isSuccess) {
            val loadedBook = result.getOrThrow()
            val loaded = loadedBook.paragraphs
            val restoredParagraphIndex = currentParagraphIndex.coerceIn(0, (loaded.size - 1).coerceAtLeast(0))
            val restoredSentenceIndex = currentSentenceIndexInParagraph.coerceAtLeast(0)
            val prepared = prepareBookContent(
                paragraphs = loaded,
                restoredParagraphIndex = restoredParagraphIndex,
                restoredSentenceIndex = restoredSentenceIndex,
                authoritativeContentVersion = loadedBook.fingerprint.toPreviewContentVersion()
            ).let { content -> content.copy(
                cacheProvenance = BookPreviewCacheVersionPolicy.prepare(readerCycle,
                    content.authoritativeContentVersion, bookFile.size, bookFile.modifiedAt)
            ) }
            Log.i("FULL_CONTENT_VERSION_SHADOW", "event=READY generation=${readerCycle.generation} versionKind=FINGERPRINT size=${loadedBook.fingerprint.size} source=SHARED_RAW_READ")
            preparedReaderContent = prepared
            paragraphs = loaded
            currentParagraphIndex = restoredParagraphIndex
            currentSentenceIndexInParagraph = restoredSentenceIndex
            contentReadiness = BookContentReadiness.PLAYBACK_READY
            if (!previewLocalPlaybackBridge.handleFullReaderReady(prepared)) {
                consumePendingPlaybackTarget(preparedContentOverride = prepared)
            }
            if (loaded.isEmpty()) {
                loadError = "小说内容为空"
            }
        } else {
            paragraphs = emptyList()
            loadError = "小说文件无法读取，请重新导入"
            Toast.makeText(context, "小说读取失败", Toast.LENGTH_SHORT).show()
        }
        isLoading = false
    }

    LaunchedEffect(paragraphs, chapterRefreshKey, currentParagraphIndex) {
        if (paragraphs.isEmpty()) {
            preparedReaderContent = PreparedReaderContent()
            return@LaunchedEffect
        }
        val activeChapterContainsCurrentParagraph = activeChapterSentences.firstOrNull()?.let { first ->
            val last = activeChapterSentences.lastOrNull() ?: first
            currentParagraphIndex in first.paragraphIndex..last.paragraphIndex
        } == true
        if (chapterRefreshKey == 0 && chapters.isNotEmpty() && activeChapterContainsCurrentParagraph) {
            return@LaunchedEffect
        }
        val sourceContent = preparedReaderContent
        val existingSnapshot = sourceContent.sequentialTargetSnapshot
        val result = withContext(Dispatchers.Default) {
            val chaptersForBuild = if (chapterRefreshKey > 0 || chapters.isEmpty()) {
                BookChapterDetector.detect(paragraphs)
            } else {
                chapters
            }
            val chapterStart = chapterStartFor(currentParagraphIndex, chaptersForBuild)
            val chapterEnd = chapterEndExclusiveFor(
                currentParagraphIndex,
                paragraphs.size,
                chaptersForBuild
            ).coerceAtLeast(chapterStart + 1)
            val sentences = buildReaderSentences(
                paragraphs = paragraphs,
                startIndex = chapterStart,
                endExclusive = chapterEnd.coerceAtMost(paragraphs.size),
                chapterTitle = chaptersForBuild.lastOrNull {
                    it.paragraphIndex <= currentParagraphIndex
                }?.title
            )
            val snapshot = if (chapterRefreshKey > 0 || chapters.isEmpty()) {
                BookSequentialPlaybackBridge.build(
                    paragraphs = paragraphs,
                    chapters = chaptersForBuild,
                    splitParagraph = ::splitParagraphIntoSentences
                ).snapshot
            } else {
                existingSnapshot
            }
            PreparedReaderContent(
                chapters = chaptersForBuild,
                activeSentences = sentences,
                sequentialTargetSnapshot = snapshot,
                authoritativeContentVersion = sourceContent.authoritativeContentVersion,
                cacheProvenance = sourceContent.cacheProvenance
            )
        }
        preparedReaderContent = result
    }

    LaunchedEffect(isTtsReady, selectedVoiceName, ttsRetryKey) {
        if (isTtsReady) {
            selectedVoiceName?.let { ttsController?.selectVoice(it) }
            ttsVoices = ttsController?.getAvailableVoices().orEmpty()
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
            (state.cachedSentences.isNotEmpty() || state.cachedVisibleText.isNotEmpty()) &&
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
                onBack = { exitReader("back") },
                onMenuAction = { action ->
                    when (action) {
                        "语音设置" -> showVoicePackageSheet = true
                        "重新检测 TTS" -> restartTtsCheck()
                    }
                }
            )

            when {
                isLoading &&
                    shouldShowCachedReaderWhileLoading() -> {
                    val cachedState = cachedReadState
                    val cachedTarget = remember(cachedState?.lastReadingTargetName) {
                        runCatching {
                            BookReadingTarget.valueOf(
                                cachedState?.lastReadingTargetName ?: BookReadingTarget.SENTENCE.name
                            )
                        }.getOrDefault(BookReadingTarget.SENTENCE)
                    }
                    val cachedSentences = remember(cachedState) {
                        val state = cachedState
                        val cachedStartChapterSentenceIndex = state?.cachedStartChapterSentenceIndex ?: 0
                        val sentenceWindow = state?.cachedSentences.orEmpty()
                        if (sentenceWindow.isNotEmpty()) {
                            sentenceWindow.map { sentence ->
                                ReaderSentence(
                                    text = sentence.text,
                                    paragraphIndex = sentence.paragraphIndex,
                                    sentenceIndexInParagraph = sentence.sentenceIndexInParagraph,
                                    chapterSentenceIndex = sentence.chapterSentenceIndex
                                )
                            }
                        } else {
                            state?.cachedVisibleText
                                .orEmpty()
                                .filter { it.isNotBlank() }
                                .mapIndexed { index, text ->
                                    ReaderSentence(
                                        text = text,
                                        paragraphIndex = state?.lastParagraphIndex ?: 0,
                                        sentenceIndexInParagraph = (state?.lastSentenceIndexInParagraph ?: 0) + index,
                                        chapterSentenceIndex = cachedStartChapterSentenceIndex + index
                                    )
                                }
                        }
                    }
                    val cachedStartChapterSentenceIndex = cachedState?.cachedStartChapterSentenceIndex ?: 0
                    val cachedIndex = if (cachedSentences.isEmpty()) {
                        0
                    } else {
                        ((cachedState?.lastChapterSentenceIndex ?: cachedStartChapterSentenceIndex) -
                            cachedStartChapterSentenceIndex)
                            .coerceIn(0, cachedSentences.lastIndex)
                    }
                    val cachedInitialFirstVisibleItemIndex = cachedState
                        ?.lastVisibleFirstChapterSentenceIndex
                        ?.takeIf { it >= 0 }
                        ?.let { anchor ->
                            (anchor - cachedStartChapterSentenceIndex + 1)
                                .coerceIn(0, cachedSentences.size)
                        }
                        ?: cachedState
                            ?.lastVisibleFirstItemIndex
                            ?.takeIf { it >= 0 }
                            ?.let { savedFirstVisible ->
                                if (savedFirstVisible <= 0) {
                                    0
                                } else {
                                    (savedFirstVisible - cachedStartChapterSentenceIndex)
                                        .coerceIn(0, cachedSentences.size)
                                }
                            }
                    val cachedInitialFirstVisibleOffset = cachedState
                        ?.lastVisibleFirstItemScrollOffset
                        ?: 0
                    Log.d(
                        BOOK_RESTORE_TAG,
                        "Book restore window snapshot hit firstVisible=$cachedInitialFirstVisibleItemIndex " +
                            "offset=$cachedInitialFirstVisibleOffset size=${cachedSentences.size}"
                    )
                    Log.d(
                        BOOK_RESTORE_TAG,
                        "Book restore window targetChapterSentence=${cachedState?.lastChapterSentenceIndex ?: cachedIndex}"
                    )
                    val cachedElapsedSeconds = cachedState?.cachedElapsedSeconds ?: -1
                    val cachedRemainingSeconds = cachedState?.cachedRemainingSeconds ?: -1
                    val cachedProgressFraction = cachedState?.cachedProgressFraction ?: -1f
                    val cachedBottomSnapshotHit = cachedElapsedSeconds >= 0 &&
                        cachedRemainingSeconds >= 0 &&
                        cachedProgressFraction in 0f..1f
                    if (cachedBottomSnapshotHit) {
                        Log.d(
                            BOOK_RESTORE_TAG,
                            "bottom snapshot hit elapsed=$cachedElapsedSeconds remaining=$cachedRemainingSeconds " +
                                "progress=$cachedProgressFraction"
                        )
                    } else {
                        Log.d(BOOK_RESTORE_TAG, "bottom snapshot miss")
                    }
                    val cachedListenedTimeLabel = if (cachedBottomSnapshotHit) {
                        formatDuration(cachedElapsedSeconds)
                    } else {
                        estimateSentenceTimeLabel(cachedSentences, 0, cachedIndex, speechRate)
                    }
                    val cachedRemainingTimeLabel = if (cachedBottomSnapshotHit) {
                        formatDuration(cachedRemainingSeconds)
                    } else {
                        estimateSentenceTimeLabel(
                            cachedSentences,
                            cachedIndex,
                            cachedSentences.size,
                            speechRate
                        )
                    }
                    Log.d(
                        BOOK_RESTORE_TAG,
                        "hot resume snapshot hit currentChapterSentenceIndex=${cachedState?.lastChapterSentenceIndex ?: cachedIndex} " +
                            "cachedStart=$cachedStartChapterSentenceIndex cachedSize=${cachedSentences.size} " +
                            "firstVisible=$cachedInitialFirstVisibleItemIndex " +
                            "firstVisibleChapterSentenceIndex=${cachedState?.lastVisibleFirstChapterSentenceIndex} " +
                            "offset=$cachedInitialFirstVisibleOffset " +
                            "elapsed=$cachedElapsedSeconds remaining=$cachedRemainingSeconds progress=$cachedProgressFraction " +
                            "fullContentReady=${paragraphs.isNotEmpty() && !isLoading} isPlaying=$isPlaying"
                    )

                    fun saveCachedPreviewSnapshot(reason: String, targetWindowIndex: Int, target: BookReadingTarget) {
                        val file = bookFile ?: return
                        val state = cachedState ?: return
                        val safeIndex = targetWindowIndex.coerceIn(0, cachedSentences.lastIndex)
                        val sentence = cachedSentences.getOrNull(safeIndex) ?: return
                        val baseElapsed = if (state.cachedElapsedSeconds >= 0) {
                            val elapsedInsideWindow = estimateSentenceDurationSeconds(
                                sentences = cachedSentences,
                                fromIndex = 0,
                                toIndex = cachedIndex.coerceIn(0, cachedSentences.size),
                                speechRate = speechRate
                            )
                            (state.cachedElapsedSeconds - elapsedInsideWindow).coerceAtLeast(0)
                        } else {
                            0
                        }
                        val elapsedSeconds = baseElapsed + estimateSentenceDurationSeconds(
                            sentences = cachedSentences,
                            fromIndex = 0,
                            toIndex = safeIndex,
                            speechRate = speechRate
                        )
                        val fallbackRemaining = estimateSentenceDurationSeconds(
                            sentences = cachedSentences,
                            fromIndex = safeIndex,
                            toIndex = cachedSentences.size,
                            speechRate = speechRate
                        )
                        val chapterDuration = state.cachedChapterEstimatedDurationSeconds
                            .takeIf { it > 0 }
                            ?: (elapsedSeconds + fallbackRemaining)
                        val remainingSeconds = (chapterDuration - elapsedSeconds).coerceAtLeast(fallbackRemaining)
                        val progressFraction = if (chapterDuration > 0) {
                            elapsedSeconds.toFloat() / chapterDuration.toFloat()
                        } else {
                            0f
                        }.coerceIn(0f, 1f)
                        val visibleFirstIndex = if (target == BookReadingTarget.CHAPTER_TITLE) {
                            0
                        } else {
                            (cachedStartChapterSentenceIndex + safeIndex - 1).coerceAtLeast(0)
                        }
                        val visibleFirstChapterSentenceIndex = if (target == BookReadingTarget.CHAPTER_TITLE) {
                            sentence.chapterSentenceIndex
                        } else {
                            (cachedStartChapterSentenceIndex + safeIndex - 1).coerceAtLeast(0)
                        }
                        val updatedSentences = cachedSentences.mapIndexed { index, item ->
                            BookCachedSentence(
                                text = item.text,
                                paragraphIndex = item.paragraphIndex,
                                sentenceIndexInParagraph = item.sentenceIndexInParagraph,
                                chapterSentenceIndex = item.chapterSentenceIndex,
                                isCurrent = index == safeIndex
                            )
                        }
                        val updatedState = BookReadStateCache(
                            bookUri = file.uri,
                            contentVersion = BookPreviewCacheVersionPolicy.preserveCached(state.contentVersion),
                            bookTitle = file.displayTitle(),
                            lastParagraphIndex = sentence.paragraphIndex,
                            lastSentenceIndexInParagraph = sentence.sentenceIndexInParagraph,
                            lastChapterSentenceIndex = sentence.chapterSentenceIndex,
                            lastReadingTargetName = target.name,
                            lastChapterTitle = state.lastChapterTitle,
                            lastVisibleFirstItemIndex = visibleFirstIndex,
                            lastVisibleFirstChapterSentenceIndex = visibleFirstChapterSentenceIndex,
                            lastVisibleFirstItemScrollOffset = 0,
                            cachedStartChapterSentenceIndex = cachedStartChapterSentenceIndex,
                            cachedElapsedSeconds = elapsedSeconds,
                            cachedRemainingSeconds = remainingSeconds,
                            cachedProgressFraction = progressFraction,
                            cachedChapterEstimatedDurationSeconds = chapterDuration,
                            cachedSentences = updatedSentences,
                            cachedVisibleText = updatedSentences.map { it.text },
                            updatedAt = System.currentTimeMillis()
                        )
                        cachedReadState = updatedState
                        currentParagraphIndex = sentence.paragraphIndex
                        currentSentenceIndexInParagraph = sentence.sentenceIndexInParagraph
                        currentReadingTargetName = target.name
                        saveBookReadStateCache(context.applicationContext, updatedState)
                        readerContentCycleCoordinator.recordCacheWrite(updatedState.contentVersion)
                        Log.d(
                            BOOK_RESTORE_TAG,
                            "save snapshot reason=$reason currentChapterSentenceIndex=${sentence.chapterSentenceIndex} " +
                                "cachedStart=$cachedStartChapterSentenceIndex cachedSize=${cachedSentences.size} " +
                                "firstVisible=$visibleFirstIndex firstVisibleChapterSentenceIndex=$visibleFirstChapterSentenceIndex " +
                                "offset=0 elapsed=$elapsedSeconds " +
                                "remaining=$remainingSeconds progress=$progressFraction " +
                                "fullContentReady=${paragraphs.isNotEmpty() && !isLoading} isPlaying=$isPlaying"
                        )
                    }

                    fun nextReadableCachedIndex(afterIndex: Int): Int {
                        for (index in (afterIndex + 1)..cachedSentences.lastIndex) {
                            if (isReadableBookTtsText(cachedSentences[index].text)) return index
                        }
                        return -1
                    }

                    fun playCachedPreviewAt(
                        targetWindowIndex: Int,
                        target: BookReadingTarget,
                        skipPreparedWait: Boolean = false
                    ) {
                        if (activePlaybackEngineName == BookPlaybackEngine.AISHELL3.name && isAishell3Paused) {
                            Log.d("BookReaderPlayback", "resume paused aishell3 sessionId=$playbackSessionId")
                            aishell3Player?.resume()
                            isAishell3Paused = false
                            isPlaying = true
                            return
                        }
                        val safeIndex = targetWindowIndex.coerceIn(0, cachedSentences.lastIndex)
                        val sentence = cachedSentences.getOrNull(safeIndex) ?: return
                        val speakText = if (target == BookReadingTarget.CHAPTER_TITLE) {
                            cachedState?.lastChapterTitle.orEmpty().ifBlank { sentence.text }
                        } else {
                            sentence.text
                        }.trim()
                        if (!isReadableBookTtsText(speakText)) {
                            val nextIndex = nextReadableCachedIndex(safeIndex)
                            if (nextIndex >= 0) {
                                saveCachedPreviewSnapshot("playback sentence change", nextIndex, BookReadingTarget.SENTENCE)
                                playCachedPreviewAt(nextIndex, BookReadingTarget.SENTENCE)
                            }
                            return
                        }
                        val validInCached = cachedSentences.any {
                            it.chapterSentenceIndex == sentence.chapterSentenceIndex
                        }
                        if (!isPlausibleChapterSentenceIndex(sentence.chapterSentenceIndex) || !validInCached) {
                            Log.w(
                                BOOK_HOT_TTS_TAG,
                                "reject invalid playback index currentIndex=${sentence.chapterSentenceIndex} " +
                                    "reason=not_chapter_sentence_index cachedStart=$cachedStartChapterSentenceIndex " +
                                    "cachedSize=${cachedSentences.size}"
                            )
                            isPlaying = false
                            activePlaybackEngineName = BookPlaybackEngine.NONE.name
                            return
                        }
                        if (!skipPreparedWait &&
                            shouldIgnoreDuplicatePlaybackRequest(sentence.chapterSentenceIndex, "cached preview")
                        ) {
                            return
                        }
                        saveCachedPreviewSnapshot("playback sentence change", safeIndex, target)
                        playbackSessionId += 1
                        val sessionId = playbackSessionId
                        pendingPlaySessionId = sessionId
                        pendingPlayTargetChapterSentenceIndex = sentence.chapterSentenceIndex
                        Log.i(
                            BOOK_HOT_TTS_TAG,
                            "play intent latched session=$sessionId target=${sentence.chapterSentenceIndex}"
                        )
                        Log.d(
                            "BookReaderPlayback",
                            "hot resume play from cached sentence sessionId=$sessionId " +
                                "targetType=$target currentChapterSentenceIndex=${sentence.chapterSentenceIndex} " +
                                "cachedStart=$cachedStartChapterSentenceIndex cachedSize=${cachedSentences.size} " +
                                "textPreview=${speakText.take(40)}"
                        )
                        stopAishell3Playback(trigger = "cached preview play")
                        ttsController?.stop()
                        activePlaybackEngineName = BookPlaybackEngine.NONE.name
                        onBeforeSpeak()
                        val preparedKey = aishell3PreparedKey(sentence.chapterSentenceIndex, speakText)
                        fun continueCachedPreviewAfterSuccess() {
                            if (sessionId != playbackSessionId) {
                                Log.d("BookReaderPlayback", "discard stale cached session sessionId=$sessionId current=$playbackSessionId")
                                return
                            }
                            if (paragraphs.isNotEmpty() && !isLoading) {
                                Log.d(BOOK_RESTORE_TAG, "full content attached keep playback state currentChapterSentenceIndex=${sentence.chapterSentenceIndex}")
                                continuePlaybackAfterCurrentTarget(sessionId)
                                return
                            }
                            val nextIndex = if (target == BookReadingTarget.CHAPTER_TITLE) {
                                safeIndex
                            } else {
                                nextReadableCachedIndex(safeIndex)
                            }
                            if (nextIndex >= 0) {
                                Log.d(
                                    BOOK_RESTORE_TAG,
                                    "cached playback advance currentChapterSentenceIndex=${cachedSentences[nextIndex].chapterSentenceIndex} " +
                                        "cachedStart=$cachedStartChapterSentenceIndex cachedSize=${cachedSentences.size}"
                                )
                                saveCachedPreviewSnapshot("playback sentence change", nextIndex, BookReadingTarget.SENTENCE)
                                coroutineScope.launch {
                                    yield()
                                    withFrameNanos { }
                                    Log.d(
                                        BOOK_RESTORE_TAG,
                                        "highlight updated index=${cachedSentences[nextIndex].chapterSentenceIndex}"
                                    )
                                    playCachedPreviewAt(nextIndex, BookReadingTarget.SENTENCE)
                                }
                            } else {
                                Log.d(
                                    BOOK_RESTORE_TAG,
                                    "cached playback window exhausted waiting full content currentChapterSentenceIndex=${sentence.chapterSentenceIndex} " +
                                        "cachedStart=$cachedStartChapterSentenceIndex cachedSize=${cachedSentences.size}"
                                )
                                isPlaying = false
                                activePlaybackEngineName = BookPlaybackEngine.NONE.name
                            }
                        }
                        Log.i(
                            BOOK_HOT_TTS_TAG,
                            "play click currentIndex=${sentence.chapterSentenceIndex} targetType=$target " +
                                "cachedStart=$cachedStartChapterSentenceIndex cachedSize=${cachedSentences.size}"
                        )
                        Log.i(BOOK_HOT_TTS_TAG, "play prepared path enter key=$preparedKey")
                        BookAishell3PreparedAudioCache.get(preparedKey)?.let { chunks ->
                            Log.i(
                                BOOK_HOT_TTS_TAG,
                                "prepared hit key=$preparedKey currentIndex=${sentence.chapterSentenceIndex} " +
                                    "chunks=${chunks.size} prepared cache size=${BookAishell3PreparedAudioCache.size()}"
                            )
                            playPreparedAishell3Audio(
                                sessionId = sessionId,
                                key = preparedKey,
                                chunks = chunks,
                                paragraphIndex = sentence.paragraphIndex,
                                onSuccess = { continueCachedPreviewAfterSuccess() }
                            )
                            return
                        }
                        val missReason = if (BookAishell3PreparedAudioCache.isPrewarming(preparedKey)) {
                            "prewarm_in_progress"
                        } else {
                            "cache_empty_or_key_not_ready"
                        }
                        if (!skipPreparedWait && missReason == "prewarm_in_progress") {
                            Log.i(BOOK_HOT_TTS_TAG, "prepared wait prewarm key=$preparedKey")
                            coroutineScope.launch {
                                val waitedChunks = waitForPreparedAishell3Audio(preparedKey)
                                if (sessionId != playbackSessionId) return@launch
                                if (waitedChunks != null) {
                                    Log.i(
                                        BOOK_HOT_TTS_TAG,
                                        "prepared hit after wait key=$preparedKey currentIndex=${sentence.chapterSentenceIndex} " +
                                            "chunks=${waitedChunks.size}"
                                    )
                                    playPreparedAishell3Audio(
                                        sessionId = sessionId,
                                        key = preparedKey,
                                        chunks = waitedChunks,
                                        paragraphIndex = sentence.paragraphIndex,
                                        onSuccess = { continueCachedPreviewAfterSuccess() }
                                    )
                                    return@launch
                                }
                                Log.i(
                                    BOOK_HOT_TTS_TAG,
                                    "prepared miss key=$preparedKey reason=prewarm_timeout currentIndex=${sentence.chapterSentenceIndex}"
                                )
                                Log.i(BOOK_HOT_TTS_TAG, "prepared wait timeout retry without wait key=$preparedKey")
                                playCachedPreviewAt(safeIndex, target, skipPreparedWait = true)
                            }
                            return
                        }
                        Log.i(
                            BOOK_HOT_TTS_TAG,
                            "prepared miss key=$preparedKey reason=$missReason currentIndex=${sentence.chapterSentenceIndex} " +
                                "prepared cache size=${BookAishell3PreparedAudioCache.size()}"
                        )
                        Log.i(BOOK_HOT_TTS_TAG, "fallback synth start key=$preparedKey")
                        coroutineScope.launch {
                            val player = StreamingPcmAudioPlayer()
                            aishell3Player = player
                            var playbackDurationMs = 0L
                            var playbackStartedAtMs = 0L
                            var playbackStarted = false
                            val synthesizedChunks = mutableListOf<PcmAudioChunk>()
                            val result = aishell3TtsEngine.speak(
                                text = speakText,
                                params = bookSpeechRate.asStreamingParams(
                                    voiceId = latestAishell3VoiceId,
                                    pitch = pitch,
                                    volume = 1f
                                ),
                                purpose = "playback",
                                onStart = {
                                    Log.d("BookReaderPlayback", "cached aishell3 onStart sessionId=$sessionId")
                                },
                                onChunk = onChunk@ { chunk ->
                                    val isCurrent = withContext(Dispatchers.Main) {
                                        sessionId == playbackSessionId && !screenDisposed.get()
                                    }
                                    if (!isCurrent) {
                                        Log.d("BookReaderPlayback", "discard stale cached session sessionId=$sessionId current=$playbackSessionId")
                                        return@onChunk
                                    }
                                    synthesizedChunks += chunk
                                    if (!playbackStarted) {
                                        val startResult = player.startSession(sessionId, chunk.format)
                                        if (startResult.isFailure) {
                                            throw IllegalStateException(startResult.exceptionOrNull()?.message ?: "AudioTrack 启动失败")
                                        }
                                        playbackStarted = true
                                        playbackStartedAtMs = System.currentTimeMillis()
                                        withContext(Dispatchers.Main) {
                                            if (sessionId == playbackSessionId && !screenDisposed.get()) {
                                                activePlaybackEngineName = BookPlaybackEngine.AISHELL3.name
                                                isAishell3Paused = false
                                                isPlaying = true
                                                saveProgress(sentence.paragraphIndex)
                                            }
                                        }
                                    }
                                    val writeResult = player.write(sessionId, chunk)
                                    if (writeResult.isFailure) {
                                        throw IllegalStateException(writeResult.exceptionOrNull()?.message ?: "AudioTrack 写入失败")
                                    }
                                    playbackDurationMs += chunk.data.size * 1000L / (chunk.format.sampleRate * 2L)
                                },
                                onDone = {
                                    Log.d("BookReaderPlayback", "cached aishell3 onDone sessionId=$sessionId playbackDurationMs=$playbackDurationMs")
                                },
                                onError = { error ->
                                    Log.e("BookReaderPlayback", "cached aishell3 error sessionId=$sessionId error=$error")
                                }
                            )
                            if (playbackStarted) {
                                val drained = player.awaitSessionPlaybackComplete(sessionId)
                                Log.i(
                                    BOOK_HOT_TTS_TAG,
                                    "cached fallback playback drained session=$sessionId drained=$drained " +
                                        "estimatedDurationMs=$playbackDurationMs elapsedMs=${System.currentTimeMillis() - playbackStartedAtMs}"
                                )
                                if (!drained) {
                                    player.release()
                                    return@launch
                                }
                            }
                            player.release()
                            if (aishell3Player === player) {
                                aishell3Player = null
                            }
                            withContext(Dispatchers.Main) {
                                if (sessionId != playbackSessionId || screenDisposed.get()) return@withContext
                                when (result) {
                                    StreamingTtsResult.Success -> {
                                        if (synthesizedChunks.isNotEmpty()) {
                                            BookAishell3PreparedAudioCache.put(
                                                preparedKey,
                                                synthesizedChunks.toList(),
                                                source = "fallback"
                                            )
                                        }
                                        continueCachedPreviewAfterSuccess()
                                    }
                                    StreamingTtsResult.Stopped -> {
                                        isPlaying = false
                                        activePlaybackEngineName = BookPlaybackEngine.NONE.name
                                    }
                                    is StreamingTtsResult.Error -> {
                                        isPlaying = false
                                        activePlaybackEngineName = BookPlaybackEngine.NONE.name
                                        Log.e("BookReaderPlayback", "cached playback error sessionId=$sessionId error=${result.message}")
                                    }
                                }
                            }
                        }
                    }

                    LaunchedEffect(
                        cachedState?.updatedAt,
                        cachedIndex,
                        cachedSentences.size,
                        speechRate,
                        pitch
                    ) {
                        val prewarm = aishell3PrewarmRequest() ?: return@LaunchedEffect
                        val currentSentence = cachedSentences.getOrNull(cachedIndex)
                        val nextSentence = cachedSentences
                            .drop(cachedIndex + 1)
                            .firstOrNull { isReadableBookTtsText(it.text) }
                        val currentText = if (cachedTarget == BookReadingTarget.CHAPTER_TITLE) {
                            cachedState?.lastChapterTitle.orEmpty().ifBlank { currentSentence?.text.orEmpty() }
                        } else {
                            currentSentence?.text.orEmpty()
                        }
                        val currentKey = currentSentence?.let {
                            aishell3PreparedKey(it.chapterSentenceIndex, currentText)
                        }
                        val nextKey = nextSentence?.let {
                            aishell3PreparedKey(it.chapterSentenceIndex, it.text)
                        }
                        val keepKeys = listOfNotNull(currentKey, nextKey).toSet()
                        Log.i(
                            BOOK_HOT_TTS_TAG,
                            "current cached sentence resolved index=${currentSentence?.chapterSentenceIndex} " +
                                "cachedStart=$cachedStartChapterSentenceIndex cachedSize=${cachedSentences.size} " +
                                "textEmpty=${currentText.isBlank()}"
                        )
                        currentKey?.let { key ->
                            Log.i(
                                BOOK_HOT_TTS_TAG,
                                "prepared cache restore ${if (BookAishell3PreparedAudioCache.contains(key)) "hit" else "miss"} " +
                                    "key=$key ${BookAishell3PreparedAudioCache.dumpSummary()}"
                            )
                            Log.i(BOOK_HOT_TTS_TAG, "prewarm current priority key=$key")
                            fun queueNextAfterCurrent() {
                                if (!prewarm.isActive()) return
                                nextSentence?.let { sentence ->
                                    nextKey?.let { next ->
                                        Log.i(BOOK_HOT_TTS_TAG, "prewarm next segment0 queued key=$next")
                                        prewarmAishell3Segments(
                                            chapterSentenceIndex = sentence.chapterSentenceIndex,
                                            text = sentence.text,
                                            label = "next"
                                        )
                                    }
                                }
                            }
                            prewarmAishell3Segments(
                                chapterSentenceIndex = currentSentence?.chapterSentenceIndex ?: cachedIndex,
                                text = currentText,
                                label = "current",
                                onCurrentFirstReady = ::queueNextAfterCurrent
                            )
                        }
                    }

                    key("cached-${bookFile?.uri}-${cachedState?.updatedAt}") {
                        val cachedLocalSnapshot = currentLocalPlaybackSnapshot()
                        val hasCachedLocalOwnership = cachedLocalSnapshot != null
                        val cachedPlaybackPreparing = contentReadiness == BookContentReadiness.PREVIEW &&
                            playbackIntentPlaying && pendingPlaybackTarget != null
                        val cachedToggleDecision = decideBookPlaybackToggle(
                            playbackToggleFacts(
                                hasPending = pendingPlaybackTarget != null,
                                playbackPreparing = cachedPlaybackPreparing
                            )
                        )
                        BookListenContent(
                            chapterTitle = cachedState?.lastChapterTitle.orEmpty().ifBlank {
                                bookFile?.displayTitle().orEmpty()
                            },
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
                            isPlaying = hasCachedLocalOwnership && cachedLocalSnapshot?.localPlaybackPaused == false,
                            isPlaybackPreparing = !hasCachedLocalOwnership && cachedPlaybackPreparing,
                            isTtsReady = isTtsReady,
                            isTtsChecking = isTtsChecking,
                            ttsError = ttsError,
                            currentVoiceName = selectedVoiceName,
                            speechRate = speechRate,
                            pitch = pitch,
                            readerFontSizeSp = readerFontSizeSp,
                            onPlayPause = {
                                logBookUiEvent("PLAY_PAUSE_BUTTON")
                                val playableIndex = cachedSentences
                                    .indexOfFirst {
                                        it.chapterSentenceIndex == (cachedState?.lastChapterSentenceIndex ?: -1)
                                    }
                                    .takeIf { it >= 0 }
                                    ?: cachedState?.cachedSentences.orEmpty().indexOfFirst { it.isCurrent }.takeIf { it >= 0 }
                                    ?: cachedIndex.takeIf { it in cachedSentences.indices }
                                    ?: 0
                                Log.d(
                                    BOOK_RESTORE_TAG,
                                    "hot resume playable cached sentence found index=$playableIndex " +
                                        "currentChapterSentenceIndex=${cachedSentences.getOrNull(playableIndex)?.chapterSentenceIndex} " +
                                        "fullContentReady=${paragraphs.isNotEmpty() && !isLoading}"
                                )
                                cachedSentences.getOrNull(playableIndex)?.let { sentence ->
                                    currentParagraphIndex = sentence.paragraphIndex
                                    currentSentenceIndexInParagraph = sentence.sentenceIndexInParagraph
                                    currentReadingTargetName = cachedTarget.name
                                }
                                applyBookPlaybackToggleDecision(
                                    decision = cachedToggleDecision,
                                    onPause = {
                                        playbackIntentPlaying = false
                                        pauseReading()
                                    },
                                    onResume = {
                                        playbackIntentPlaying = true
                                        speakCurrentSentence()
                                    },
                                    onCancelPreviewPending = {
                                        playbackIntentPlaying = false
                                        pendingPlaybackTarget = pendingPlaybackTarget?.let {
                                            PendingBookPlaybackTargetResolver.setPlayRequested(it, false)
                                        }
                                    },
                                    onStartPreview = {
                                        cachedSentences.getOrNull(playableIndex)?.let { sentence ->
                                            playbackIntentPlaying = true
                                            pendingPlaybackTarget = PendingBookPlaybackTarget(
                                                paragraphIndex = sentence.paragraphIndex,
                                                sentenceIndexInParagraph = sentence.sentenceIndexInParagraph,
                                                stableTextHash = sentence.text.hashCode(),
                                                source = PendingBookPlaybackSource.PLAY_BUTTON,
                                                playRequested = true
                                            )
                                        }
                                    }
                                )
                            },
                            onStop = { if (paragraphs.isNotEmpty()) stopReading() },
                            onPrevious = {
                                if (paragraphs.isNotEmpty()) {
                                    jumpToChapter(-1)
                                } else {
                                    Log.d(BOOK_RESTORE_TAG, "cached preview previous ignored until parser attaches silently")
                                }
                            },
                            onNext = {
                                if (paragraphs.isNotEmpty()) {
                                    jumpToChapter(1)
                                } else {
                                    Log.d(BOOK_RESTORE_TAG, "cached preview next ignored until parser attaches silently")
                                }
                            },
                            onSeekSentence = { sentenceIndex ->
                                cachedSentences.getOrNull(sentenceIndex)?.let { sentence ->
                                    logBookUiEvent(
                                        "PROGRESS_SEEK_FINISH",
                                        target = sentence.chapterSentenceIndex
                                    )
                                    currentParagraphIndex = sentence.paragraphIndex
                                    currentSentenceIndexInParagraph = sentence.sentenceIndexInParagraph
                                    currentReadingTargetName = BookReadingTarget.SENTENCE.name
                                    saveCachedPreviewSnapshot("slider seek", sentenceIndex, BookReadingTarget.SENTENCE)
                                    if (wasPlayingBeforeSliderSeek && contentReadiness == BookContentReadiness.PLAYBACK_READY) {
                                        wasPlayingBeforeSliderSeek = false
                                        speakCurrentSentence()
                                    } else if (wasPlayingBeforeSliderSeek) {
                                        wasPlayingBeforeSliderSeek = false
                                        playbackIntentPlaying = true
                                        pendingPlaybackTarget = PendingBookPlaybackTarget(
                                            paragraphIndex = sentence.paragraphIndex,
                                            sentenceIndexInParagraph = sentence.sentenceIndexInParagraph,
                                            stableTextHash = sentence.text.hashCode(),
                                            source = PendingBookPlaybackSource.PLAY_BUTTON,
                                            playRequested = true
                                        )
                                    }
                                }
                            },
                            onSeekStart = {
                                logBookUiEvent("PROGRESS_SEEK_START")
                                Log.i(BOOK_HOT_TTS_TAG, "slider seek start cachedPreview=true isPlaying=$isPlaying")
                                wasPlayingBeforeSliderSeek = isPlaying
                                if (isPlaying) {
                                    pauseReading()
                                } else {
                                    stopCurrentPlayback(reason = "slider_seek_start", invalidateSession = true)
                                }
                            },
                            onSentenceClick = { sentence ->
                                logBookUiEvent("SENTENCE_CLICK", target = sentence.chapterSentenceIndex)
                                val validTarget = isPlausibleChapterSentenceIndex(sentence.chapterSentenceIndex) &&
                                    cachedSentences.any {
                                        it.chapterSentenceIndex == sentence.chapterSentenceIndex &&
                                            it.paragraphIndex == sentence.paragraphIndex &&
                                            it.sentenceIndexInParagraph == sentence.sentenceIndexInParagraph
                                    }
                                if (!validTarget) {
                                    Log.w(
                                        BOOK_HOT_TTS_TAG,
                                        "sentence tap invalid target=${sentence.chapterSentenceIndex} " +
                                            "fullSize=${paragraphs.size} cachedSize=${cachedSentences.size}"
                                    )
                                } else {
                                    val shadowResult = PreviewTargetShadow.observeClick(
                                        readerContentCycleCoordinator,
                                        initialReadStateCache,
                                        sentence,
                                        contentReadiness,
                                        playRequested = true
                                    )
                                    val localTarget = (shadowResult as? PreviewTargetShadowResult.Resolved)
                                        ?.resolution
                                        ?.let { it as? PreviewPlaybackResolution.Allowed }
                                        ?.target
                                    currentParagraphIndex = sentence.paragraphIndex
                                    currentSentenceIndexInParagraph = sentence.sentenceIndexInParagraph
                                    currentReadingTargetName = BookReadingTarget.SENTENCE.name
                                    val sentenceIndex = cachedSentences.indexOfFirst {
                                        it.chapterSentenceIndex == sentence.chapterSentenceIndex
                                    }.coerceAtLeast(0)
                                    saveCachedPreviewSnapshot("sentence tap", sentenceIndex, BookReadingTarget.SENTENCE)
                                    if (localTarget != null) {
                                        stopCurrentPlayback(reason = "preview_local_target_replace", invalidateSession = true)
                                        playbackIntentPlaying = true
                                        pendingPlaybackTarget = null
                                        pendingPlaySessionId = -1L
                                        pendingPlayTargetChapterSentenceIndex = -1
                                        val localDispatch = previewLocalPlaybackAdapter.begin(
                                            target = localTarget,
                                            provider = latestBookPlaybackEngineSnapshot.dispatchEngine(),
                                            speechRate = latestBookSpeechRate.multiplier,
                                            playbackSessionId = playbackSessionId
                                        )
                                        if (localDispatch != null) {
                                            previewLocalPlaybackBridge.dispatch(localDispatch)
                                        }
                                    } else {
                                        playbackIntentPlaying = true
                                        pendingPlaybackTarget = PendingBookPlaybackTarget(
                                            paragraphIndex = sentence.paragraphIndex,
                                            sentenceIndexInParagraph = sentence.sentenceIndexInParagraph,
                                            stableTextHash = sentence.text.hashCode(),
                                            source = PendingBookPlaybackSource.USER_SENTENCE_TAP,
                                            playRequested = true
                                        )
                                    }
                                }
                            },
                            onSpeechRateChange = { newRate ->
                                logBookUiEvent("SPEECH_RATE_CHANGE", speechRateValue = newRate)
                                matchaPlaybackCoordinator.invalidatePrewarm("speech_rate_change")
                                speechRate = newRate
                            },
                            onSpeechRateChangeFinished = { newRate ->
                                logBookUiEvent("SPEECH_RATE_CHANGE_FINISHED", speechRateValue = newRate)
                            },
                            onPitchChange = { pitch = it },
                            onReaderFontSizeChange = { readerFontSizeSp = it },
                            onOpenCatalog = { if (chapters.isNotEmpty()) showChapterSheet = true },
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
                            onOpenVoiceSettings = { showVoicePackageSheet = true },
                            onRetryTts = { restartTtsCheck() },
                            restoreGateLabel = "cached preview",
                            initialFirstVisibleItemIndex = cachedInitialFirstVisibleItemIndex,
                            initialFirstVisibleItemScrollOffset = cachedInitialFirstVisibleOffset,
                            progressFractionOverride = cachedProgressFraction.takeIf { cachedBottomSnapshotHit },
                            onVisibleWindowSnapshot = { },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                isLoading -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Spacer(modifier = Modifier.height(1.dp))
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
                    val chapterSentences = activeChapterSentences
                    val cacheProvenance = preparedReaderContent.cacheProvenance
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
                    val listenedTimeLabel = estimateSentenceTimeLabel(
                        sentences = chapterSentences,
                        fromIndex = 0,
                        toIndex = currentChapterSentenceIndex,
                        speechRate = speechRate
                    )
                    val listenedSeconds = estimateSentenceDurationSeconds(
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
                    val remainingSeconds = estimateSentenceDurationSeconds(
                        sentences = chapterSentences,
                        fromIndex = currentChapterSentenceIndex,
                        toIndex = chapterSentences.size,
                        speechRate = speechRate
                    )
                    val chapterEstimatedDurationSeconds = listenedSeconds + remainingSeconds
                    val progressFraction = if (chapterSentences.size <= 1) {
                        0f
                    } else {
                        currentChapterSentenceIndex.toFloat() / (chapterSentences.size - 1).coerceAtLeast(1)
                    }.coerceIn(0f, 1f)
                    val fullAnchorChapterSentenceIndex = cachedReadState
                        ?.lastVisibleFirstChapterSentenceIndex
                        ?.takeIf { it >= 0 }
                    if (fullAnchorChapterSentenceIndex != null &&
                        chapterSentences.none { it.chapterSentenceIndex == fullAnchorChapterSentenceIndex }
                    ) {
                        Log.i(
                            BOOK_HOT_TTS_TAG,
                            "restore snapshot invalid current=${cachedReadState?.lastChapterSentenceIndex} " +
                                "anchor=$fullAnchorChapterSentenceIndex fallback=${currentChapterSentenceIndex.coerceIn(0, chapterSentences.lastIndex)}"
                        )
                    }
                    val fullInitialFirstVisibleItemIndex = fullAnchorChapterSentenceIndex
                        ?.let { anchor ->
                            chapterSentences.indexOfFirst { it.chapterSentenceIndex == anchor }
                                .takeIf { it >= 0 }
                                ?.let { sentenceIndex -> sentenceIndex + 1 }
                        }
                        ?: cachedReadState
                            ?.lastVisibleFirstItemIndex
                            ?.takeIf { it >= 0 }
                    val fullInitialFirstVisibleOffset = cachedReadState
                        ?.lastVisibleFirstItemScrollOffset
                        ?: 0
                    Log.d(
                        BOOK_RESTORE_TAG,
                        "bottom full content swap keep elapsed=$listenedSeconds remaining=$remainingSeconds " +
                            "progress=$progressFraction"
                    )
                    Log.d(
                        BOOK_RESTORE_TAG,
                        "full content attach anchor fullIndex=$fullInitialFirstVisibleItemIndex " +
                            "firstVisibleChapterSentenceIndex=$fullAnchorChapterSentenceIndex " +
                            "offset=$fullInitialFirstVisibleOffset"
                    )
                    LaunchedEffect(
                        bookFile?.uri,
                        chapterTitle,
                        currentChapterSentenceIndex,
                        chapterSentences.size,
                        speechRate,
                        pitch
                    ) {
                        val prewarm = aishell3PrewarmRequest() ?: return@LaunchedEffect
                        val currentSentence = chapterSentences.getOrNull(currentChapterSentenceIndex)
                        val nextSentence = chapterSentences
                            .drop(currentChapterSentenceIndex + 1)
                            .firstOrNull { isReadableBookTtsText(it.text) }
                        val currentText = if (currentReadingTarget == BookReadingTarget.CHAPTER_TITLE) {
                            chapterTitle.ifBlank { currentSentence?.text.orEmpty() }
                        } else {
                            currentSentence?.text.orEmpty()
                        }
                        val currentKey = currentSentence?.let {
                            aishell3PreparedKey(it.chapterSentenceIndex, currentText)
                        }
                        val nextKey = nextSentence?.let {
                            aishell3PreparedKey(it.chapterSentenceIndex, it.text)
                        }
                        val keepKeys = listOfNotNull(currentKey, nextKey).toSet()
                        Log.i(
                            BOOK_HOT_TTS_TAG,
                            "current cached sentence resolved index=${currentSentence?.chapterSentenceIndex} " +
                                "fullContentReady=true textEmpty=${currentText.isBlank()}"
                        )
                        currentKey?.let { key ->
                            Log.i(
                                BOOK_HOT_TTS_TAG,
                                "prepared cache restore ${if (BookAishell3PreparedAudioCache.contains(key)) "hit" else "miss"} " +
                                    "key=$key ${BookAishell3PreparedAudioCache.dumpSummary()}"
                            )
                            Log.i(BOOK_HOT_TTS_TAG, "prewarm current priority key=$key")
                            fun queueNextAfterCurrent() {
                                if (!prewarm.isActive()) return
                                nextSentence?.let { sentence ->
                                    nextKey?.let { next ->
                                        Log.i(BOOK_HOT_TTS_TAG, "prewarm next segment0 queued key=$next")
                                        prewarmAishell3Segments(
                                            chapterSentenceIndex = sentence.chapterSentenceIndex,
                                            text = sentence.text,
                                            label = "next"
                                        )
                                    }
                                }
                            }
                            prewarmAishell3Segments(
                                chapterSentenceIndex = currentSentence?.chapterSentenceIndex ?: currentChapterSentenceIndex,
                                text = currentText,
                                label = "current",
                                onCurrentFirstReady = ::queueNextAfterCurrent
                            )
                        }
                    }
                    key("full-${bookFile?.uri}-$chapterStartIndex") {
                        val formalToggleDecision = decideBookPlaybackToggle(
                            playbackToggleFacts(
                                hasPending = pendingPlaybackTarget != null,
                                playbackPreparing = false
                            )
                        )
                        BookListenContent(
                            chapterTitle = chapterTitle,
                            chapterSentences = chapterSentences,
                            currentChapterSentenceIndex = currentChapterSentenceIndex,
                            isChapterTitleCurrent = currentReadingTarget == BookReadingTarget.CHAPTER_TITLE,
                            currentParagraphIndex = currentParagraphIndex,
                            currentSentenceIndexInParagraph = currentSentenceIndexInParagraph,
                            listenedTimeLabel = listenedTimeLabel,
                            remainingTimeLabel = remainingTimeLabel,
                            isPlaying = isPlaying,
                            isPlaybackPreparing = false,
                            isTtsReady = isTtsReady,
                            isTtsChecking = isTtsChecking,
                            ttsError = ttsError,
                            currentVoiceName = selectedVoiceName,
                            speechRate = speechRate,
                            pitch = pitch,
                            readerFontSizeSp = readerFontSizeSp,
                            onPlayPause = {
                                logBookUiEvent("PLAY_PAUSE_BUTTON")
                                applyBookPlaybackToggleDecision(
                                    decision = formalToggleDecision,
                                    onPause = {
                                        playbackIntentPlaying = false
                                        pauseReading()
                                    },
                                    onResume = {
                                        playbackIntentPlaying = true
                                        speakCurrentSentence()
                                    },
                                    onCancelPreviewPending = {},
                                    onStartPreview = {}
                                )
                            },
                            onStop = {
                                playbackIntentPlaying = false
                                stopReading()
                            },
                            onPrevious = {
                                jumpToChapter(-1)
                            },
                            onNext = {
                                jumpToChapter(1)
                            },
                        onSeekSentence = { sentenceIndex ->
                            chapterSentences.getOrNull(sentenceIndex)?.let { sentence ->
                                logBookUiEvent(
                                    "PROGRESS_SEEK_FINISH",
                                    target = sentence.chapterSentenceIndex
                                )
                                Log.i(
                                    BOOK_HOT_TTS_TAG,
                                    "snapshot save reason=slider seek current=${sentence.chapterSentenceIndex}"
                                )
                                jumpToSentence(sentence, autoPlay = wasPlayingBeforeSliderSeek)
                                wasPlayingBeforeSliderSeek = false
                            }
                        },
                        onSeekStart = {
                            logBookUiEvent("PROGRESS_SEEK_START")
                            Log.i(BOOK_HOT_TTS_TAG, "slider seek start isPlaying=$isPlaying")
                            wasPlayingBeforeSliderSeek = isPlaying
                            if (isPlaying) {
                                stopCurrentPlayback(reason = "slider_seek_start", invalidateSession = true)
                                isPlaying = false
                            }
                        },
                        onSentenceClick = { sentence ->
                            logBookUiEvent("SENTENCE_CLICK", target = sentence.chapterSentenceIndex)
                            PreviewTargetShadow.observeClick(readerContentCycleCoordinator,
                                initialReadStateCache, sentence, contentReadiness, playRequested = true)
                            jumpToSentence(
                                sentence,
                                autoPlay = true
                            )
                        },
                        onSpeechRateChange = { newRate ->
                            logBookUiEvent("SPEECH_RATE_CHANGE", speechRateValue = newRate)
                            matchaPlaybackCoordinator.invalidatePrewarm("speech_rate_change")
                            speechRate = newRate
                        },
                        onSpeechRateChangeFinished = { newRate ->
                            logBookUiEvent("SPEECH_RATE_CHANGE_FINISHED", speechRateValue = newRate)
                        },
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
                            onOpenVoiceSettings = { showVoicePackageSheet = true },
                            onRetryTts = { restartTtsCheck() },
                            restoreGateLabel = "full content",
                            initialFirstVisibleItemIndex = fullInitialFirstVisibleItemIndex,
                            initialFirstVisibleItemScrollOffset = fullInitialFirstVisibleOffset,
                            progressFractionOverride = null,
                            onVisibleWindowSnapshot = snapshot@{ snapshot ->
                                val cacheVersion = BookPreviewCacheVersionPolicy.forFullSave(
                                    cacheProvenance, readerContentCycleCoordinator.currentCycleSnapshot()
                                ) ?: return@snapshot
                                val currentCachedSentence = snapshot.sentences
                                    .firstOrNull { it.isCurrent }
                                    ?: snapshot.sentences.firstOrNull {
                                        it.chapterSentenceIndex == snapshot.currentChapterSentenceIndex
                                    }
                                val state = BookReadStateCache(
                                    bookUri = bookFile?.uri.orEmpty(),
                                    contentVersion = cacheVersion,
                                    bookTitle = bookFile?.displayTitle().orEmpty(),
                                    lastParagraphIndex = currentCachedSentence?.paragraphIndex ?: currentParagraphIndex,
                                    lastSentenceIndexInParagraph = currentCachedSentence?.sentenceIndexInParagraph
                                        ?: currentSentenceIndexInParagraph,
                                    lastChapterSentenceIndex = snapshot.currentChapterSentenceIndex,
                                    lastReadingTargetName = currentReadingTarget.name,
                                    lastChapterTitle = chapterTitle,
                                    lastVisibleFirstItemIndex = snapshot.firstVisibleItemIndex,
                                    lastVisibleFirstChapterSentenceIndex = snapshot.firstVisibleChapterSentenceIndex,
                                    lastVisibleFirstItemScrollOffset = snapshot.firstVisibleItemScrollOffset,
                                    cachedStartChapterSentenceIndex = snapshot.cachedStartChapterSentenceIndex,
                                    cachedElapsedSeconds = snapshot.cachedElapsedSeconds,
                                    cachedRemainingSeconds = snapshot.cachedRemainingSeconds,
                                    cachedProgressFraction = snapshot.cachedProgressFraction,
                                    cachedChapterEstimatedDurationSeconds = snapshot.cachedChapterEstimatedDurationSeconds,
                                    cachedSentences = snapshot.sentences,
                                    cachedVisibleText = snapshot.sentences.map { it.text },
                                    updatedAt = System.currentTimeMillis()
                                )
                                cachedReadState = state
                                saveBookReadStateCache(context.applicationContext, state)
                                readerContentCycleCoordinator.recordCacheWrite(cacheVersion, cacheProvenance)
                                Log.d(
                                    BOOK_RESTORE_TAG,
                                    "Book restore snapshot saved start=${snapshot.cachedStartChapterSentenceIndex} " +
                                        "current=${snapshot.currentChapterSentenceIndex} " +
                                        "firstVisibleChapterSentenceIndex=${snapshot.firstVisibleChapterSentenceIndex} " +
                                        "size=${snapshot.sentences.size}"
                                )
                                Log.d(
                                    BOOK_RESTORE_TAG,
                                    "bottom snapshot saved elapsed=${snapshot.cachedElapsedSeconds} " +
                                        "remaining=${snapshot.cachedRemainingSeconds} progress=${snapshot.cachedProgressFraction}"
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
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
            isBuiltInOfflineAvailable = offlineTtsAvailability.builtInOfflineAvailable,
            builtInOfflineUnavailableReason = offlineTtsAvailability.builtInOfflineUnavailableReason,
            isAishell3OfflineAvailable = offlineTtsAvailability.aishell3Available,
            aishell3OfflineUnavailableReason = offlineTtsAvailability.aishell3UnavailableReason,
            preferredPlaybackEngine = preferredPlaybackEngine,
            effectivePlaybackEngine = effectivePlaybackEngine,
            isMatchaPlaybackAvailable = matchaBookAvailable,
            isMatchaAuditionAvailable = matchaAuditionAvailable,
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
                if (!offlineTtsAvailability.aishell3Available) {
                    Toast.makeText(
                        context,
                        offlineTtsAvailability.aishell3UnavailableReason ?: "Aishell3 离线语音不可用",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
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
                                params = bookSpeechRate.asStreamingParams(
                                    voiceId = latestAishell3VoiceId,
                                    pitch = 1f,
                                    volume = 1f
                                ),
                                purpose = "preview",
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
                }
            },
            onMatchaPreview = {
                coroutineScope.launch {
                    if (!matchaAuditionAvailable) {
                        Toast.makeText(context, "Matcha benchmark 模型未安装", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    Toast.makeText(context, "正在试听 Matcha...", Toast.LENGTH_SHORT).show()
                    val result = runCatching {
                        matchaAuditionController.play(1f) {
                            Toast.makeText(context, "Matcha 开始播放", Toast.LENGTH_SHORT).show()
                        }
                    }.getOrElse { error ->
                        StreamingTtsResult.Error(error.message ?: "Matcha 试听失败")
                    }
                    when (result) {
                        StreamingTtsResult.Success -> Toast.makeText(context, "Matcha 播放完成", Toast.LENGTH_SHORT).show()
                        StreamingTtsResult.Stopped -> Unit
                        is StreamingTtsResult.Error -> Toast.makeText(context, "Matcha 试听失败：${result.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onPreferredPlaybackEngineChange = ::selectPreferredPlaybackEngine,
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
                } else if (!offlineTtsAvailability.builtInOfflineAvailable) {
                    val reason = offlineTtsAvailability.builtInOfflineUnavailableReason ?: "内置离线语音不可用"
                    builtInOfflineTtsError = reason
                    Toast.makeText(context, reason, Toast.LENGTH_LONG).show()
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
                        val result = builtInOfflineTtsEngine.speak(
                            text = "这是一段内置离线语音试听。",
                            speechRate = bookSpeechRate
                        ) {
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
    isPlaybackPreparing: Boolean,
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
    onSeekStart: () -> Unit,
    onSentenceClick: (ReaderSentence) -> Unit,
    onSpeechRateChange: (Float) -> Unit,
    onSpeechRateChangeFinished: (Float) -> Unit,
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
    restoreGateLabel: String,
    initialFirstVisibleItemIndex: Int?,
    initialFirstVisibleItemScrollOffset: Int,
    progressFractionOverride: Float?,
    onVisibleWindowSnapshot: (BookVisibleWindowSnapshot) -> Unit,
    modifier: Modifier = Modifier
) {
    var showVoiceControls by remember { mutableStateOf(false) }
    var showFontSizeControls by remember { mutableStateOf(false) }
    val context = LocalContext.current
    fun showDevelopingToast() {
        Toast.makeText(context, "功能开发中", Toast.LENGTH_SHORT).show()
    }
    val initialSentenceListIndex = if (isChapterTitleCurrent) {
        0
    } else {
        (currentChapterSentenceIndex - 1).coerceAtLeast(0)
    }
    val safeInitialFirstVisibleItemIndex = (initialFirstVisibleItemIndex ?: initialSentenceListIndex)
        .coerceIn(0, chapterSentences.size)
    val safeInitialFirstVisibleItemScrollOffset = initialFirstVisibleItemScrollOffset.coerceAtLeast(0)
    val sentenceListState = rememberLazyListState(
        initialFirstVisibleItemIndex = safeInitialFirstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = safeInitialFirstVisibleItemScrollOffset
    )
    val latestCurrentChapterSentenceIndex by rememberUpdatedState(currentChapterSentenceIndex)
    val latestCurrentParagraphIndex by rememberUpdatedState(currentParagraphIndex)
    val latestCurrentSentenceIndexInParagraph by rememberUpdatedState(currentSentenceIndexInParagraph)
    val latestIsPlaying by rememberUpdatedState(isPlaying)
    var suppressScrollIdleSaveUntilMs by remember { mutableLongStateOf(0L) }
    val seekEndIndex = (chapterSentences.size - 1).coerceAtLeast(0)
    val sliderEndIndex = seekEndIndex.coerceAtLeast(1)
    var isSliderSeeking by remember { mutableStateOf(false) }
    var sliderTempValue by remember { mutableFloatStateOf(0f) }
    val seekValue = if (isChapterTitleCurrent) {
        0f
    } else if (progressFractionOverride != null) {
        (progressFractionOverride.coerceIn(0f, 1f) * sliderEndIndex).coerceIn(0f, sliderEndIndex.toFloat())
    } else {
        currentChapterSentenceIndex.coerceIn(0, seekEndIndex).toFloat()
    }
    val displayedSeekValue = if (isSliderSeeking) {
        sliderTempValue.coerceIn(0f, sliderEndIndex.toFloat())
    } else {
        seekValue
    }

    LaunchedEffect(chapterTitle, chapterSentences.size, safeInitialFirstVisibleItemIndex, safeInitialFirstVisibleItemScrollOffset) {
        Log.d(
            BOOK_RESTORE_TAG,
            "Book restore window firstVisible=$safeInitialFirstVisibleItemIndex " +
                "offset=$safeInitialFirstVisibleItemScrollOffset label=$restoreGateLabel"
        )
        Log.d(
            BOOK_RESTORE_TAG,
            "Book restore window rendered immediately label=$restoreGateLabel"
        )
        Log.d(
            BOOK_RESTORE_TAG,
            "bottom initial render elapsed=$listenedTimeLabel remaining=$remainingTimeLabel " +
                "progress=${progressFractionOverride ?: (seekValue / sliderEndIndex.toFloat().coerceAtLeast(1f))} " +
                "label=$restoreGateLabel"
        )
        if (restoreGateLabel == "full content") {
            Log.d(BOOK_RESTORE_TAG, "Book restore full content swapped keep position")
        }
        Log.d(
            BOOK_RESTORE_TAG,
            "first visible sentence after restore firstVisible=${sentenceListState.firstVisibleItemIndex} " +
                "initialFirstVisible=$initialSentenceListIndex targetIndex=$currentChapterSentenceIndex " +
                "isChapterTitleCurrent=$isChapterTitleCurrent chapterTitle=$chapterTitle"
        )
    }

    var lastAutoScrollTarget by remember(chapterTitle, chapterSentences.size) {
        mutableStateOf(currentChapterSentenceIndex to isChapterTitleCurrent)
    }
    LaunchedEffect(chapterTitle, currentChapterSentenceIndex, chapterSentences.size, isChapterTitleCurrent) {
        val currentTarget = currentChapterSentenceIndex to isChapterTitleCurrent
        if (currentTarget != lastAutoScrollTarget) {
            if (isChapterTitleCurrent) {
                suppressScrollIdleSaveUntilMs = SystemClock.elapsedRealtime() + 1_000L
                sentenceListState.scrollToItem(0)
            } else if (chapterSentences.isNotEmpty()) {
                suppressScrollIdleSaveUntilMs = SystemClock.elapsedRealtime() + 1_000L
                sentenceListState.scrollToItem((currentChapterSentenceIndex - 1).coerceAtLeast(0))
            }
            lastAutoScrollTarget = currentTarget
        }
    }

    fun saveVisibleWindowSnapshot(reason: String) {
        if (restoreGateLabel != "full content" || chapterSentences.isEmpty()) return
        if (reason == "scroll idle") {
            val now = SystemClock.elapsedRealtime()
            if (now < suppressScrollIdleSaveUntilMs) {
                Log.i(
                    BOOK_HOT_TTS_TAG,
                    "snapshot skip scroll idle suppressed current=$latestCurrentChapterSentenceIndex"
                )
                return
            }
            if (latestIsPlaying) {
                Log.i(
                    BOOK_HOT_TTS_TAG,
                    "snapshot skip scroll idle playing current=$latestCurrentChapterSentenceIndex"
                )
                return
            }
        }
        val firstVisibleItemIndex = sentenceListState.firstVisibleItemIndex
        val firstVisibleSentenceIndex = (firstVisibleItemIndex - 1).coerceAtLeast(0)
        val currentIndex = latestCurrentChapterSentenceIndex.coerceIn(0, chapterSentences.lastIndex)
        val windowStart = (minOf(firstVisibleSentenceIndex, currentIndex) - 8).coerceAtLeast(0)
        val windowEnd = maxOf(firstVisibleSentenceIndex + 31, currentIndex + 31)
            .coerceAtMost(chapterSentences.size)
        val elapsedSeconds = estimateSentenceDurationSeconds(chapterSentences, 0, currentIndex, speechRate)
        val remainingSeconds = estimateSentenceDurationSeconds(
            chapterSentences,
            currentIndex,
            chapterSentences.size,
            speechRate
        )
        val chapterEstimatedDurationSeconds = elapsedSeconds + remainingSeconds
        val progressFraction = if (chapterSentences.size <= 1) {
            0f
        } else {
            currentIndex.toFloat() / (chapterSentences.size - 1).coerceAtLeast(1)
        }.coerceIn(0f, 1f)
        val sentences = buildBookCachedSentences(
            sentences = chapterSentences,
            startIndex = windowStart,
            endIndex = windowEnd,
            currentChapterSentenceIndex = currentIndex
        )
        if (sentences.isEmpty()) return
        val firstVisibleChapterSentenceIndex = if (firstVisibleItemIndex <= 0) {
            currentIndex
        } else {
            chapterSentences
                .getOrNull(firstVisibleSentenceIndex.coerceIn(0, chapterSentences.lastIndex))
                ?.chapterSentenceIndex
                ?: currentIndex
        }
        Log.d(
            BOOK_RESTORE_TAG,
            "save snapshot reason=$reason currentChapterSentenceIndex=$currentIndex " +
                "firstVisible=$firstVisibleItemIndex firstVisibleChapterSentenceIndex=$firstVisibleChapterSentenceIndex " +
                "offset=${sentenceListState.firstVisibleItemScrollOffset} " +
                "elapsed=$elapsedSeconds remaining=$remainingSeconds progress=$progressFraction"
        )
        Log.i(
            BOOK_HOT_TTS_TAG,
            "snapshot save reason=$reason current=$currentIndex firstVisible=$firstVisibleItemIndex " +
                "firstVisibleChapterSentenceIndex=$firstVisibleChapterSentenceIndex"
        )
        onVisibleWindowSnapshot(
            BookVisibleWindowSnapshot(
                firstVisibleItemIndex = firstVisibleItemIndex,
                firstVisibleChapterSentenceIndex = firstVisibleChapterSentenceIndex,
                firstVisibleItemScrollOffset = sentenceListState.firstVisibleItemScrollOffset,
                currentChapterSentenceIndex = currentIndex,
                cachedStartChapterSentenceIndex = windowStart,
                cachedElapsedSeconds = elapsedSeconds,
                cachedRemainingSeconds = remainingSeconds,
                cachedProgressFraction = progressFraction,
                cachedChapterEstimatedDurationSeconds = chapterEstimatedDurationSeconds,
                sentences = sentences
            )
        )
    }

    LaunchedEffect(chapterTitle, chapterSentences.size, currentChapterSentenceIndex, currentParagraphIndex, currentSentenceIndexInParagraph) {
        yield()
        withFrameNanos { }
        val expectedFirstVisible = if (isChapterTitleCurrent) {
            0
        } else {
            (currentChapterSentenceIndex - 1).coerceAtLeast(0)
        }
        val currentFirstVisible = sentenceListState.firstVisibleItemIndex
        if (currentFirstVisible !in (expectedFirstVisible - 2)..(expectedFirstVisible + 2)) {
            if (isChapterTitleCurrent) {
                sentenceListState.scrollToItem(0)
            } else {
                sentenceListState.scrollToItem(expectedFirstVisible)
            }
            withFrameNanos { }
        }
        saveVisibleWindowSnapshot("playback sentence change")
    }

    val latestSaveVisibleWindowSnapshot by rememberUpdatedState<(String) -> Unit>({ reason ->
        saveVisibleWindowSnapshot(reason)
    })
    DisposableEffect(chapterTitle, chapterSentences.size) {
        onDispose {
            latestSaveVisibleWindowSnapshot("dispose")
        }
    }

    LaunchedEffect(chapterTitle, chapterSentences.size, sentenceListState) {
        snapshotFlow {
            Triple(
                sentenceListState.firstVisibleItemIndex,
                sentenceListState.firstVisibleItemScrollOffset,
                sentenceListState.isScrollInProgress
            )
        }
            .distinctUntilChanged()
            .collect { (_, _, isScrollInProgress) ->
                if (!isScrollInProgress) {
                    delay(250L)
                    if (!sentenceListState.isScrollInProgress) {
                        saveVisibleWindowSnapshot("scroll idle")
                    }
                }
            }
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
            onOpenVoiceSettings = { showVoiceControls = true },
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
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isPlaybackPreparing) {
                    Text(
                        text = "正在准备播放…",
                        color = Color(0xFFB388FF),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
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
                        "已听 $listenedTimeLabel",
                        color = Color.White.copy(alpha = 0.52f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        "剩余 $remainingTimeLabel",
                        color = Color.White.copy(alpha = 0.52f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                ReaderChapterProgressBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp),
                    value = displayedSeekValue,
                    maxValue = sliderEndIndex.toFloat(),
                    onValueChangeStarted = {
                        isSliderSeeking = true
                        sliderTempValue = seekValue
                        suppressScrollIdleSaveUntilMs = SystemClock.elapsedRealtime() + 1_000L
                        Log.i(BOOK_HOT_TTS_TAG, "slider seek start current=$latestCurrentChapterSentenceIndex")
                        onSeekStart()
                    },
                    onValueChange = {
                        sliderTempValue = it.coerceIn(0f, sliderEndIndex.toFloat())
                        Log.i(
                            BOOK_HOT_TTS_TAG,
                            "slider seek progress temp=${sliderTempValue.roundToInt().coerceIn(0, seekEndIndex)}"
                        )
                    },
                    onValueChangeFinished = {
                        val targetIndex = sliderTempValue.roundToInt().coerceIn(0, seekEndIndex)
                        suppressScrollIdleSaveUntilMs = SystemClock.elapsedRealtime() + 1_000L
                        Log.i(BOOK_HOT_TTS_TAG, "slider seek finish target=$targetIndex")
                        isSliderSeeking = false
                        onSeekSentence(targetIndex)
                    }
                )
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
                            if (isPlaying || isPlaybackPreparing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = if (isPlaying || isPlaybackPreparing) "暂停" else "播放",
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
            onSpeechRateChangeFinished = onSpeechRateChangeFinished,
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
    onValueChangeStarted: () -> Unit,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    ThinMoonSlider(
        value = value,
        valueRange = 0f..maxValue.coerceAtLeast(1f),
        onValueChangeStarted = onValueChangeStarted,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        modifier = modifier,
        trackHeight = 3.dp,
        thumbSize = 11.dp
    )
}

@Composable
private fun ThinMoonSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChangeStarted: () -> Unit = {},
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit = {},
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
                                onValueChangeStarted()
                                updateFromX(offset.x)
                                onValueChangeFinished()
                            }
                        }
                        .pointerInput(valueRange.start, valueRange.endInclusive) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    onValueChangeStarted()
                                    updateFromX(offset.x)
                                },
                                onDrag = { change, _ ->
                                    updateFromX(change.position.x)
                                    change.consume()
                                },
                                onDragEnd = { onValueChangeFinished() },
                                onDragCancel = { onValueChangeFinished() }
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
    onSpeechRateChangeFinished: (Float) -> Unit,
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
                onValueChange = onSpeechRateChange,
                onValueChangeFinished = { onSpeechRateChangeFinished(speechRate) }
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
    isBuiltInOfflineAvailable: Boolean,
    builtInOfflineUnavailableReason: String?,
    isAishell3OfflineAvailable: Boolean,
    aishell3OfflineUnavailableReason: String?,
    preferredPlaybackEngine: BookPlaybackEngine,
    effectivePlaybackEngine: BookPlaybackEngine,
    isMatchaPlaybackAvailable: Boolean,
    isMatchaAuditionAvailable: Boolean,
    onDismiss: () -> Unit,
    onInstallVoiceData: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onRetry: () -> Unit,
    onPreview: () -> Unit,
    onAudioChannelTest: () -> Unit,
    onAishell3OfflinePreview: () -> Unit,
    onMatchaPreview: () -> Unit,
    onPreferredPlaybackEngineChange: (BookPlaybackEngine) -> Unit,
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

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "正文播放引擎",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "选择听书正文使用的语音引擎",
                    color = Color.White.copy(alpha = 0.54f),
                    fontSize = 12.sp
                )
                BookPlaybackEngineSelection.options(
                    preferred = preferredPlaybackEngine,
                    aishell3Available = isAishell3OfflineAvailable,
                    aishell3UnavailableReason = aishell3OfflineUnavailableReason,
                    matchaAvailable = isMatchaPlaybackAvailable,
                    includeExperimentalMatcha = true
                ).forEach { option ->
                    PlaybackEngineOption(
                        title = option.title,
                        subtitle = option.unavailableReason?.let { "未安装：$it" },
                        selected = preferredPlaybackEngine == option.engine,
                        enabled = option.enabled,
                        effective = effectivePlaybackEngine == option.engine,
                        onClick = { onPreferredPlaybackEngineChange(option.engine) }
                    )
                }
            }

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
                text = when {
                    !isBuiltInOfflineAvailable -> "内置语音未安装"
                    isBuiltInOfflineTtsInitializing -> "内置语音初始化中"
                    else -> "内置语音试听"
                },
                onClick = onBuiltInPreview,
                modifier = Modifier.fillMaxWidth(),
                emphasized = isBuiltInOfflineAvailable,
                muted = !isBuiltInOfflineAvailable
            )
            VoiceSheetActionButton(
                text = "音频通道测试",
                onClick = onAudioChannelTest,
                modifier = Modifier.fillMaxWidth()
            )
            VoiceSheetActionButton(
                text = if (isAishell3OfflineAvailable) "自研离线试听" else "自研离线未安装",
                onClick = onAishell3OfflinePreview,
                modifier = Modifier.fillMaxWidth(),
                emphasized = isAishell3OfflineAvailable,
                muted = !isAishell3OfflineAvailable
            )
            VoiceSheetActionButton(
                text = if (isMatchaAuditionAvailable) "Matcha 性能试听（实验）" else "Matcha 性能试听未安装",
                onClick = onMatchaPreview,
                modifier = Modifier.fillMaxWidth(),
                emphasized = isMatchaAuditionAvailable,
                muted = !isMatchaAuditionAvailable
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
            if (!isBuiltInOfflineAvailable && !builtInOfflineUnavailableReason.isNullOrBlank()) {
                Text(
                    text = "内置语音：$builtInOfflineUnavailableReason",
                    color = Color.White.copy(alpha = 0.52f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
            if (!isAishell3OfflineAvailable && !aishell3OfflineUnavailableReason.isNullOrBlank()) {
                Text(
                    text = "自研离线：$aishell3OfflineUnavailableReason",
                    color = Color.White.copy(alpha = 0.52f),
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
private fun PlaybackEngineOption(
    title: String,
    subtitle: String? = null,
    selected: Boolean,
    enabled: Boolean,
    effective: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = if (selected) 0.08f else 0.04f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.42f),
                fontSize = 14.sp
            )
            subtitle?.let {
                Text(
                    text = it,
                    color = Color.White.copy(alpha = 0.42f),
                    fontSize = 11.sp
                )
            }
        }
        if (effective) {
            Text(
                text = "当前",
                color = Color(0xFFC3B0FF),
                fontSize = 11.sp
            )
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
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null
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
            onValueChangeFinished = onValueChangeFinished,
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

internal data class ReaderSentence(
    val text: String,
    val paragraphIndex: Int,
    val sentenceIndexInParagraph: Int,
    val chapterSentenceIndex: Int
)

internal data class PreparedReaderContent(
    val chapters: List<BookChapter> = emptyList(),
    val activeSentences: List<ReaderSentence> = emptyList(),
    val restoredSentence: ReaderSentence? = null,
    val sequentialTargetSnapshot: BookSequentialTargetSnapshot =
        BookSequentialTargetSnapshot(emptyList()),
    val authoritativeContentVersion: PreviewContentVersion = PreviewContentVersion.Unavailable,
    val cacheProvenance: PreparedContentProvenance? = null
)

private suspend fun prepareBookContent(
    paragraphs: List<String>,
    restoredParagraphIndex: Int,
    restoredSentenceIndex: Int,
    authoritativeContentVersion: PreviewContentVersion
): PreparedReaderContent = withContext(Dispatchers.Default) {
    val chapters = BookChapterDetector.detect(paragraphs)
    val chapter = chapters.lastOrNull { it.paragraphIndex <= restoredParagraphIndex }
    val chapterStart = preparedChapterStartFor(restoredParagraphIndex, chapters)
    val chapterEnd = preparedChapterEndExclusiveFor(
        restoredParagraphIndex,
        paragraphs.size,
        chapters
    ).coerceAtLeast(chapterStart + 1)
    val activeSentences = buildReaderSentences(
        paragraphs = paragraphs,
        startIndex = chapterStart,
        endExclusive = chapterEnd.coerceAtMost(paragraphs.size),
        chapterTitle = chapter?.title
    )
    PreparedReaderContent(
        chapters = chapters,
        activeSentences = activeSentences,
        restoredSentence = activeSentences.firstOrNull {
            it.paragraphIndex == restoredParagraphIndex &&
                it.sentenceIndexInParagraph == restoredSentenceIndex
        },
        sequentialTargetSnapshot = BookSequentialPlaybackBridge.build(
            paragraphs = paragraphs,
            chapters = chapters,
            splitParagraph = ::splitParagraphIntoSentences
        ).snapshot,
        authoritativeContentVersion = authoritativeContentVersion
    )
}

private fun preparedChapterStartFor(
    index: Int,
    chapters: List<BookChapter>
): Int = chapters.lastOrNull { it.paragraphIndex <= index }?.paragraphIndex ?: 0

private fun preparedChapterEndExclusiveFor(
    index: Int,
    total: Int,
    chapters: List<BookChapter>
): Int {
    if (total <= 0) return 0
    val chapterIndex = chapters.indexOfLast { it.paragraphIndex <= index }
    return chapters.getOrNull(chapterIndex + 1)?.paragraphIndex?.coerceIn(0, total) ?: total
}

private data class BookVisibleWindowSnapshot(
    val firstVisibleItemIndex: Int,
    val firstVisibleChapterSentenceIndex: Int,
    val firstVisibleItemScrollOffset: Int,
    val currentChapterSentenceIndex: Int,
    val cachedStartChapterSentenceIndex: Int,
    val cachedElapsedSeconds: Int,
    val cachedRemainingSeconds: Int,
    val cachedProgressFraction: Float,
    val cachedChapterEstimatedDurationSeconds: Int,
    val sentences: List<BookCachedSentence>
)

private const val BOOK_READ_CACHE_PREFS = "book_read_state_cache"
private const val BOOK_READ_CACHE_SEPARATOR = "\u001E"
private const val BOOK_READ_CACHE_ENTRY_SEPARATOR = "\u001D"
private const val BOOK_READ_CACHE_FIELD_SEPARATOR = "\u001F"
private const val BOOK_RESTORE_TAG = "BookReaderRestore"
private const val BOOK_HOT_TTS_TAG = "BookListenHot"

private object BookAishell3PreparedAudioCache {
    private const val MAX_ITEMS = 24
    private val lock = Any()
    private val chunksByKey = linkedMapOf<String, List<PcmAudioChunk>>()
    private val prewarmingKeys = mutableSetOf<String>()

    fun get(key: String): List<PcmAudioChunk>? = synchronized(lock) {
        chunksByKey[key]
    }

    fun put(key: String, chunks: List<PcmAudioChunk>, source: String) = synchronized(lock) {
        if (chunksByKey.containsKey(key)) {
            chunksByKey.remove(key)
        }
        chunksByKey[key] = chunks
        Log.i(
            BOOK_HOT_TTS_TAG,
            "prepared cache put source=$source key=$key chunks=${chunks.size} " +
                "bytes=${chunks.sumOf { it.data.size }} prepared cache size=${chunksByKey.size}"
        )
        while (chunksByKey.size > MAX_ITEMS) {
            val firstKey = chunksByKey.keys.firstOrNull() ?: break
            if (firstKey == key) break
            Log.i(
                BOOK_HOT_TTS_TAG,
                "prepared cache evict key=$firstKey reason=max_items max=$MAX_ITEMS"
            )
            chunksByKey.remove(firstKey)
        }
    }

    fun contains(key: String): Boolean = synchronized(lock) {
        chunksByKey.containsKey(key)
    }

    fun size(): Int = synchronized(lock) {
        chunksByKey.size
    }

    fun dumpSummary(): String = synchronized(lock) {
        val keys = chunksByKey.keys
            .toList()
            .takeLast(8)
            .joinToString(separator = ",") { it.takeLast(28) }
        "prepared cache size=${chunksByKey.size} prewarming=${prewarmingKeys.size} recentKeys=[$keys]"
    }

    fun markPrewarming(key: String): Boolean = synchronized(lock) {
        if (chunksByKey.containsKey(key) || prewarmingKeys.contains(key)) {
            false
        } else {
            prewarmingKeys.add(key)
            true
        }
    }

    fun unmarkPrewarming(key: String) = synchronized(lock) {
        prewarmingKeys.remove(key)
        Unit
    }

    fun isPrewarming(key: String): Boolean = synchronized(lock) {
        prewarmingKeys.contains(key)
    }

    fun trim(keepKeys: Set<String>) = synchronized(lock) {
        while (chunksByKey.size > MAX_ITEMS) {
            val evictKey = chunksByKey.keys.firstOrNull { it !in keepKeys }
                ?: chunksByKey.keys.firstOrNull()
                ?: break
            if (evictKey in keepKeys && chunksByKey.size <= keepKeys.size) {
                Log.i(BOOK_HOT_TTS_TAG, "prepared cache keep pinned current key=$evictKey")
                break
            }
            Log.i(
                BOOK_HOT_TTS_TAG,
                "prepared cache evict key=$evictKey reason=trim max=$MAX_ITEMS pinned=${evictKey in keepKeys}"
            )
            chunksByKey.remove(evictKey)
        }
    }
}

private object BookAishell3SegmentAudioCache {
    private const val MAX_ITEMS = 48
    private val lock = Any()
    private val chunksByKey = linkedMapOf<String, PcmAudioChunk>()
    private val prewarmingKeys = mutableSetOf<String>()

    fun get(key: String): PcmAudioChunk? = synchronized(lock) {
        chunksByKey[key]
    }

    fun put(key: String, chunk: PcmAudioChunk, source: String) = synchronized(lock) {
        if (chunksByKey.containsKey(key)) {
            chunksByKey.remove(key)
        }
        chunksByKey[key] = chunk
        Log.i(
            BOOK_HOT_TTS_TAG,
            "segment cache put source=$source key=$key bytes=${chunk.data.size} segment cache size=${chunksByKey.size}"
        )
        while (chunksByKey.size > MAX_ITEMS) {
            val evictKey = chunksByKey.keys.firstOrNull() ?: break
            Log.i(BOOK_HOT_TTS_TAG, "segment cache evict key=$evictKey reason=max_items max=$MAX_ITEMS")
            chunksByKey.remove(evictKey)
        }
    }

    fun contains(key: String): Boolean = synchronized(lock) {
        chunksByKey.containsKey(key)
    }

    fun markPrewarming(key: String): Boolean = synchronized(lock) {
        if (chunksByKey.containsKey(key) || prewarmingKeys.contains(key)) {
            false
        } else {
            prewarmingKeys.add(key)
            true
        }
    }

    fun unmarkPrewarming(key: String) = synchronized(lock) {
        prewarmingKeys.remove(key)
        Unit
    }

    fun isPrewarming(key: String): Boolean = synchronized(lock) {
        prewarmingKeys.contains(key)
    }
}

private fun bookReadCachePrefix(bookUri: String): String {
    return "book_${bookUri.hashCode()}_"
}

private fun loadBookReadStateCache(context: Context, bookUri: String?): BookReadStateCache? {
    if (bookUri.isNullOrBlank()) return null
    val prefs = context.getSharedPreferences(BOOK_READ_CACHE_PREFS, Context.MODE_PRIVATE)
    val prefix = bookReadCachePrefix(bookUri)
    val metadata = BookPreviewCacheMetadataCodec.decode(prefix, bookUri, prefs.all) ?: return null
    val cachedText = prefs
        .getString(prefix + "text", null)
        ?.split(BOOK_READ_CACHE_SEPARATOR)
        ?.filter { it.isNotBlank() }
        .orEmpty()
    val cachedSentences = decodeBookCachedSentences(
        metadata.encodedSentenceWindow
    )
    val lastVisibleFirstItemIndex = prefs.getInt(prefix + "visible_first_index", -1)
    val cachedStartChapterSentenceIndex = prefs.getInt(prefix + "cached_start_chapter_sentence", 0)
    val savedFirstVisibleChapterSentenceIndex = prefs.getInt(prefix + "visible_first_chapter_sentence", -1)
    val firstVisibleChapterSentenceIndex = if (savedFirstVisibleChapterSentenceIndex >= 0) {
        savedFirstVisibleChapterSentenceIndex
    } else if (lastVisibleFirstItemIndex > 0) {
        (cachedStartChapterSentenceIndex + lastVisibleFirstItemIndex - 1).coerceAtLeast(0)
    } else {
        cachedStartChapterSentenceIndex.coerceAtLeast(0)
    }
    return BookReadStateCache(
        bookUri = metadata.bookUri,
        contentVersion = metadata.contentVersion,
        bookTitle = prefs.getString(prefix + "title", null).orEmpty(),
        lastParagraphIndex = prefs.getInt(prefix + "paragraph", 0),
        lastSentenceIndexInParagraph = prefs.getInt(prefix + "sentence", 0),
        lastChapterSentenceIndex = prefs.getInt(prefix + "chapter_sentence", 0),
        lastReadingTargetName = prefs.getString(prefix + "reading_target", null)
            ?: "SENTENCE",
        lastChapterTitle = prefs.getString(prefix + "chapter_title", null).orEmpty(),
        lastVisibleFirstItemIndex = lastVisibleFirstItemIndex,
        lastVisibleFirstChapterSentenceIndex = firstVisibleChapterSentenceIndex,
        lastVisibleFirstItemScrollOffset = prefs.getInt(prefix + "visible_first_offset", 0),
        cachedStartChapterSentenceIndex = cachedStartChapterSentenceIndex,
        cachedElapsedSeconds = prefs.getInt(prefix + "cached_elapsed_seconds", -1),
        cachedRemainingSeconds = prefs.getInt(prefix + "cached_remaining_seconds", -1),
        cachedProgressFraction = prefs.getFloat(prefix + "cached_progress_fraction", -1f),
        cachedChapterEstimatedDurationSeconds = prefs.getInt(prefix + "cached_chapter_duration_seconds", -1),
        cachedSentences = cachedSentences,
        cachedVisibleText = cachedText,
        updatedAt = prefs.getLong(prefix + "updated_at", 0L)
    )
}

private fun saveBookReadStateCache(context: Context, state: BookReadStateCache) {
    if (state.bookUri.isBlank()) return
    val prefix = bookReadCachePrefix(state.bookUri)
    val editor = context.getSharedPreferences(BOOK_READ_CACHE_PREFS, Context.MODE_PRIVATE).edit()
    BookPreviewCacheMetadataCodec.encode(
        prefix = prefix,
        metadata = BookPreviewCacheMetadata(
            bookUri = state.bookUri,
            contentVersion = state.contentVersion,
            encodedSentenceWindow = encodeBookCachedSentences(state.cachedSentences)
        )
    ).forEach { (key, value) ->
        when (value) {
            null -> editor.remove(key)
            is String -> editor.putString(key, value)
            is Long -> editor.putLong(key, value)
        }
    }
    editor
        .putString(prefix + "title", state.bookTitle)
        .putInt(prefix + "paragraph", state.lastParagraphIndex.coerceAtLeast(0))
        .putInt(prefix + "sentence", state.lastSentenceIndexInParagraph.coerceAtLeast(0))
        .putInt(prefix + "chapter_sentence", state.lastChapterSentenceIndex.coerceAtLeast(0))
        .putString(prefix + "reading_target", state.lastReadingTargetName)
        .putString(prefix + "chapter_title", state.lastChapterTitle)
        .putInt(prefix + "visible_first_index", state.lastVisibleFirstItemIndex)
        .putInt(prefix + "visible_first_chapter_sentence", state.lastVisibleFirstChapterSentenceIndex)
        .putInt(prefix + "visible_first_offset", state.lastVisibleFirstItemScrollOffset.coerceAtLeast(0))
        .putInt(prefix + "cached_start_chapter_sentence", state.cachedStartChapterSentenceIndex.coerceAtLeast(0))
        .putInt(prefix + "cached_elapsed_seconds", state.cachedElapsedSeconds)
        .putInt(prefix + "cached_remaining_seconds", state.cachedRemainingSeconds)
        .putFloat(prefix + "cached_progress_fraction", state.cachedProgressFraction)
        .putInt(prefix + "cached_chapter_duration_seconds", state.cachedChapterEstimatedDurationSeconds)
        .putString(prefix + "text", state.cachedVisibleText.joinToString(BOOK_READ_CACHE_SEPARATOR))
        .putLong(prefix + "updated_at", state.updatedAt)
        .apply()
}

private fun encodeBookCachedSentences(sentences: List<BookCachedSentence>): String {
    return sentences.joinToString(BOOK_READ_CACHE_ENTRY_SEPARATOR) { sentence ->
        listOf(
            sentence.chapterSentenceIndex.toString(),
            sentence.paragraphIndex.toString(),
            sentence.sentenceIndexInParagraph.toString(),
            if (sentence.isCurrent) "1" else "0",
            Uri.encode(sentence.text)
        ).joinToString(BOOK_READ_CACHE_FIELD_SEPARATOR)
    }
}

private fun decodeBookCachedSentences(raw: String): List<BookCachedSentence> {
    if (raw.isBlank()) return emptyList()
    return raw
        .split(BOOK_READ_CACHE_ENTRY_SEPARATOR)
        .mapNotNull { encoded ->
            val parts = encoded.split(BOOK_READ_CACHE_FIELD_SEPARATOR, limit = 5)
            if (parts.size < 5) return@mapNotNull null
            BookCachedSentence(
                chapterSentenceIndex = parts[0].toIntOrNull() ?: return@mapNotNull null,
                paragraphIndex = parts[1].toIntOrNull() ?: return@mapNotNull null,
                sentenceIndexInParagraph = parts[2].toIntOrNull() ?: return@mapNotNull null,
                isCurrent = parts[3] == "1",
                text = Uri.decode(parts[4]).orEmpty()
            )
        }
        .filter { it.text.isNotBlank() }
}

private fun buildBookCachedSentences(
    sentences: List<ReaderSentence>,
    startIndex: Int,
    endIndex: Int,
    currentChapterSentenceIndex: Int
): List<BookCachedSentence> {
    if (sentences.isEmpty()) return emptyList()
    val safeStart = startIndex.coerceIn(0, sentences.size)
    val safeEnd = endIndex.coerceIn(safeStart, sentences.size)
    return sentences
        .subList(safeStart, safeEnd)
        .map { sentence ->
            BookCachedSentence(
                text = sentence.text,
                paragraphIndex = sentence.paragraphIndex,
                sentenceIndexInParagraph = sentence.sentenceIndexInParagraph,
                chapterSentenceIndex = sentence.chapterSentenceIndex,
                isCurrent = sentence.chapterSentenceIndex == currentChapterSentenceIndex
            )
        }
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


private val sentenceBreakChars = setOf(
    '。', '！', '？', '；', '!', '?', ';', '…'
)

private fun estimateSentenceTimeLabel(
    sentences: List<ReaderSentence>,
    fromIndex: Int,
    toIndex: Int,
    speechRate: Float
): String {
    return formatDuration(estimateSentenceDurationSeconds(sentences, fromIndex, toIndex, speechRate))
}

private fun estimateSentenceDurationSeconds(
    sentences: List<ReaderSentence>,
    fromIndex: Int,
    toIndex: Int,
    speechRate: Float
): Int {
    if (sentences.isEmpty()) return 0
    val start = fromIndex.coerceIn(0, sentences.size)
    val end = toIndex.coerceIn(start, sentences.size)
    val chars = sentences
        .subList(start, end)
        .sumOf { sentence -> sentence.text.count { !it.isWhitespace() } }
    val safeSpeechRate = if (speechRate > 0f) speechRate else 1f
    val effectiveRate = (300f * safeSpeechRate).coerceAtLeast(60f)
    return ((chars * 60f) / effectiveRate).roundToInt().coerceAtLeast(0)
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

internal fun shouldContinuePlaybackAfterTarget(
    moved: Boolean,
    playbackIntentPlaying: Boolean,
    sessionValid: Boolean
): Boolean = moved && playbackIntentPlaying && sessionValid

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

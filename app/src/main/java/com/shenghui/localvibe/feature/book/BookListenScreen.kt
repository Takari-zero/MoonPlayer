package com.shenghui.localvibe.feature.book

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.shenghui.localvibe.core.scanner.LocalMediaFile
import com.shenghui.localvibe.core.tts.BookTtsController
import kotlinx.coroutines.Dispatchers
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val initialReadStateCache = remember(bookFile?.uri) {
        loadBookReadStateCache(context.applicationContext, bookFile?.uri)
    }
    var cachedReadState by remember(bookFile?.uri) { mutableStateOf(initialReadStateCache) }
    var paragraphs by remember(bookFile?.uri) { mutableStateOf(emptyList<String>()) }
    var currentParagraphIndex by remember(bookFile?.uri) {
        mutableIntStateOf((initialReadStateCache?.lastParagraphIndex ?: initialParagraphIndex).coerceAtLeast(0))
    }
    var currentSentenceIndexInParagraph by remember(bookFile?.uri) {
        mutableIntStateOf(initialReadStateCache?.lastSentenceIndexInParagraph ?: 0)
    }
    var isLoading by remember(bookFile?.uri) { mutableStateOf(bookFile != null) }
    var loadError by remember(bookFile?.uri) { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isTtsReady by remember { mutableStateOf(false) }
    var speechRate by remember { mutableFloatStateOf(1.0f) }
    var pitch by remember { mutableFloatStateOf(1.0f) }
    var ttsError by remember { mutableStateOf<String?>(null) }
    var ttsRetryKey by remember { mutableIntStateOf(0) }
    var chapterRefreshKey by remember { mutableIntStateOf(0) }
    var showChapterSheet by remember { mutableStateOf(false) }
    var readerFontSizeSp by rememberSaveable(bookFile?.uri) { mutableStateOf(22f) }
    var playbackModeName by rememberSaveable(bookFile?.uri) {
        mutableStateOf(BookPlaybackMode.SEQUENTIAL.name)
    }
    val playbackMode = remember(playbackModeName) {
        runCatching { BookPlaybackMode.valueOf(playbackModeName) }
            .getOrDefault(BookPlaybackMode.SEQUENTIAL)
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
    val latestChapters by rememberUpdatedState(chapters)
    val latestCurrentParagraphIndex by rememberUpdatedState(currentParagraphIndex)
    val latestCurrentSentenceIndexInParagraph by rememberUpdatedState(currentSentenceIndexInParagraph)

    fun saveProgress(index: Int, total: Int = paragraphs.size) {
        val file = bookFile ?: return
        if (total <= 0) return
        onProgressChanged(file.uri, index.coerceIn(0, total - 1), total)
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

    var ttsController by remember { mutableStateOf<BookTtsController?>(null) }
    DisposableEffect(ttsRetryKey) {
        val controller = BookTtsController(
            context = context,
            onReady = { isTtsReady = true },
            onError = { message ->
                ttsError = message
                isPlaying = false
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            },
            onDone = {
                val file = latestBookFile ?: return@BookTtsController
                val list = latestParagraphs
                if (!latestIsPlaying || list.isEmpty()) return@BookTtsController

                fun paragraphSentences(index: Int): List<String> {
                    return splitParagraphIntoSentences(list[index]).ifEmpty { listOf(list[index]) }
                }

                fun playPosition(paragraphIndex: Int, sentenceIndex: Int) {
                    val safeParagraphIndex = paragraphIndex.coerceIn(0, list.lastIndex)
                    val sentences = paragraphSentences(safeParagraphIndex)
                    val safeSentenceIndex = sentenceIndex.coerceIn(0, sentences.lastIndex)
                    currentParagraphIndex = safeParagraphIndex
                    currentSentenceIndexInParagraph = safeSentenceIndex
                    onProgressChanged(file.uri, safeParagraphIndex, list.size)
                    ttsController?.speak(sentences[safeSentenceIndex], latestSpeechRate, latestPitch)
                }

                fun playNextSequentialFrom(paragraphIndex: Int, sentenceIndex: Int) {
                    val sentences = paragraphSentences(paragraphIndex)
                    if (sentenceIndex < sentences.lastIndex) {
                        playPosition(paragraphIndex, sentenceIndex + 1)
                    } else if (paragraphIndex < list.lastIndex) {
                        playPosition(paragraphIndex + 1, 0)
                    } else {
                        isPlaying = false
                        onProgressChanged(file.uri, list.lastIndex, list.size)
                    }
                }

                when (latestPlaybackMode) {
                    BookPlaybackMode.SEQUENTIAL -> playNextSequentialFrom(
                        latestCurrentParagraphIndex,
                        latestCurrentSentenceIndexInParagraph
                    )
                    BookPlaybackMode.SINGLE_PARAGRAPH -> {
                        val sentences = paragraphSentences(latestCurrentParagraphIndex)
                        val nextSentenceIndex =
                            if (latestCurrentSentenceIndexInParagraph < sentences.lastIndex) {
                                latestCurrentSentenceIndexInParagraph + 1
                            } else {
                                0
                            }
                        playPosition(latestCurrentParagraphIndex, nextSentenceIndex)
                    }
                    BookPlaybackMode.CHAPTER_LOOP -> {
                        val chapterList = latestChapters
                        val chapterIndex = chapterList.indexOfLast { it.paragraphIndex <= latestCurrentParagraphIndex }
                        val chapterStart = chapterList.getOrNull(chapterIndex)?.paragraphIndex
                        if (chapterStart == null) {
                            playNextSequentialFrom(
                                latestCurrentParagraphIndex,
                                latestCurrentSentenceIndexInParagraph
                            )
                        } else {
                            val chapterEndExclusive = chapterList
                                .getOrNull(chapterIndex + 1)
                                ?.paragraphIndex
                                ?.coerceIn(0, list.size)
                                ?: list.size
                            val sentences = paragraphSentences(latestCurrentParagraphIndex)
                            if (latestCurrentSentenceIndexInParagraph < sentences.lastIndex) {
                                playPosition(
                                    latestCurrentParagraphIndex,
                                    latestCurrentSentenceIndexInParagraph + 1
                                )
                            } else {
                                val nextIndex = latestCurrentParagraphIndex + 1
                                if (nextIndex < chapterEndExclusive) {
                                    playPosition(nextIndex, 0)
                                } else {
                                    playPosition(chapterStart, 0)
                                }
                            }
                        }
                    }
                }
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

    fun speakCurrentSentence() {
        val file = bookFile
        if (file == null) {
            Toast.makeText(context, "未选择小说文件", Toast.LENGTH_SHORT).show()
            return
        }
        if (!isTtsReady) {
            Toast.makeText(context, ttsError ?: "系统 TTS 正在初始化", Toast.LENGTH_SHORT).show()
            return
        }
        if (paragraphs.isEmpty()) {
            Toast.makeText(context, "小说内容为空", Toast.LENGTH_SHORT).show()
            return
        }
        val index = currentParagraphIndex.coerceIn(0, paragraphs.lastIndex)
        val sentences = splitParagraphIntoSentences(paragraphs[index]).ifEmpty { listOf(paragraphs[index]) }
        val sentenceIndex = currentSentenceIndexInParagraph.coerceIn(0, sentences.lastIndex)
        currentParagraphIndex = index
        currentSentenceIndexInParagraph = sentenceIndex
        onBeforeSpeak()
        val started = ttsController?.speak(sentences[sentenceIndex], speechRate, pitch) == true
        if (started) {
            isPlaying = true
            saveProgress(index)
        } else {
            isPlaying = false
            Toast.makeText(context, "朗读失败", Toast.LENGTH_SHORT).show()
        }
    }

    fun pauseReading() {
        ttsController?.pause()
        isPlaying = false
        saveProgress(currentParagraphIndex)
    }

    fun stopReading() {
        ttsController?.stop()
        currentParagraphIndex = 0
        currentSentenceIndexInParagraph = 0
        isPlaying = false
        saveProgress(0)
    }

    fun jumpToParagraph(index: Int, autoPlay: Boolean = isPlaying) {
        if (paragraphs.isEmpty()) return
        val nextIndex = index.coerceIn(0, paragraphs.lastIndex)
        ttsController?.stop()
        currentParagraphIndex = nextIndex
        currentSentenceIndexInParagraph = 0
        saveProgress(nextIndex)
        if (autoPlay) {
            isPlaying = false
            speakCurrentSentence()
        }
    }

    fun jumpToSentence(sentence: ReaderSentence, autoPlay: Boolean = isPlaying) {
        if (paragraphs.isEmpty()) return
        val nextIndex = sentence.paragraphIndex.coerceIn(0, paragraphs.lastIndex)
        ttsController?.stop()
        currentParagraphIndex = nextIndex
        currentSentenceIndexInParagraph = sentence.sentenceIndexInParagraph.coerceAtLeast(0)
        saveProgress(nextIndex)
        if (autoPlay) {
            isPlaying = false
            speakCurrentSentence()
        }
    }

    LaunchedEffect(bookFile?.uri) {
        if (bookFile == null) {
            isLoading = false
            loadError = "未选择小说文件"
            paragraphs = emptyList()
            return@LaunchedEffect
        }
        isLoading = true
        loadError = null
        isPlaying = false
        ttsController?.stop()
        val result = withContext(Dispatchers.IO) {
            TxtBookReader.readParagraphs(context.applicationContext, bookFile.uri)
        }
        result
            .onSuccess { loaded ->
                paragraphs = loaded
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
                    ttsController?.stop()
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
                isLoading && cachedReadState?.cachedVisibleText.orEmpty().isNotEmpty() -> {
                    val cachedState = cachedReadState
                    val cachedSentences = remember(cachedState) {
                        cachedState?.cachedVisibleText
                            .orEmpty()
                            .filter { it.isNotBlank() }
                            .mapIndexed { index, text ->
                                ReaderSentence(
                                    text = text,
                                    paragraphIndex = cachedState?.lastParagraphIndex ?: 0,
                                    sentenceIndexInParagraph = index,
                                    chapterSentenceIndex = index
                                )
                            }
                    }
                    val cachedIndex = cachedState
                        ?.lastChapterSentenceIndex
                        ?.coerceIn(0, (cachedSentences.size - 1).coerceAtLeast(0))
                        ?: 0
                    BookListenContent(
                        chapterTitle = cachedState?.lastChapterTitle.orEmpty().ifBlank { "正在恢复上次阅读" },
                        chapterSentences = cachedSentences,
                        currentChapterSentenceIndex = cachedIndex,
                        listenedTimeLabel = estimateSentenceTimeLabel(cachedSentences, 0, cachedIndex, speechRate),
                        remainingTimeLabel = estimateSentenceTimeLabel(
                            cachedSentences,
                            cachedIndex,
                            cachedSentences.size,
                            speechRate
                        ),
                        isPlaying = false,
                        isTtsReady = isTtsReady,
                        ttsError = ttsError,
                        speechRate = speechRate,
                        pitch = pitch,
                        readerFontSizeSp = readerFontSizeSp,
                        onPlayPause = { Toast.makeText(context, "正在恢复小说内容", Toast.LENGTH_SHORT).show() },
                        onStop = { },
                        onPrevious = { },
                        onNext = { },
                        onSeekSentence = { sentenceIndex ->
                            cachedSentences.getOrNull(sentenceIndex)?.let { sentence ->
                                currentParagraphIndex = sentence.paragraphIndex
                                currentSentenceIndexInParagraph = sentence.sentenceIndexInParagraph
                            }
                        },
                        onSentenceClick = { sentence ->
                            currentParagraphIndex = sentence.paragraphIndex
                            currentSentenceIndexInParagraph = sentence.sentenceIndexInParagraph
                        },
                        onSpeechRateChange = { speechRate = it },
                        onPitchChange = { pitch = it },
                        onReaderFontSizeChange = { readerFontSizeSp = it },
                        onOpenCatalog = { Toast.makeText(context, "正在恢复目录", Toast.LENGTH_SHORT).show() },
                        playbackModeLabel = playbackMode.label,
                        onTogglePlaybackMode = {
                            val nextMode = playbackMode.next()
                            playbackModeName = nextMode.name
                            Toast.makeText(context, "已切换为${nextMode.label}", Toast.LENGTH_SHORT).show()
                        },
                        onInstallVoiceData = {
                            openIntentSafely(
                                Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
                                "无法打开语音数据安装页面"
                            )
                        },
                        onOpenVoiceSettings = {
                            openIntentSafely(
                                Intent("com.android.settings.TTS_SETTINGS")
                                    .also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
                                "无法打开语音设置"
                            )
                        },
                        onRetryTts = { restartTtsCheck() },
                        modifier = Modifier.weight(1f)
                    )
                }

                isLoading -> {
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
                    val chapterSentences = remember(
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
                    val remainingTimeLabel = estimateSentenceTimeLabel(
                        sentences = chapterSentences,
                        fromIndex = currentChapterSentenceIndex,
                        toIndex = chapterSentences.size,
                        speechRate = speechRate
                    )
                    LaunchedEffect(
                        bookFile?.uri,
                        chapterTitle,
                        currentParagraphIndex,
                        currentSentenceIndexInParagraph,
                        currentChapterSentenceIndex,
                        chapterSentences
                    ) {
                        val state = BookReadStateCache(
                            bookUri = bookFile?.uri.orEmpty(),
                            bookTitle = bookFile?.displayTitle().orEmpty(),
                            lastParagraphIndex = currentParagraphIndex,
                            lastSentenceIndexInParagraph = currentSentenceIndexInParagraph,
                            lastChapterSentenceIndex = currentChapterSentenceIndex,
                            lastChapterTitle = chapterTitle,
                            cachedVisibleText = chapterSentences
                                .drop(currentChapterSentenceIndex)
                                .take(24)
                                .map { it.text },
                            updatedAt = System.currentTimeMillis()
                        )
                        cachedReadState = state
                        saveBookReadStateCache(context.applicationContext, state)
                    }
                    BookListenContent(
                        chapterTitle = chapterTitle,
                        chapterSentences = chapterSentences,
                        currentChapterSentenceIndex = currentChapterSentenceIndex,
                        listenedTimeLabel = listenedTimeLabel,
                        remainingTimeLabel = remainingTimeLabel,
                        isPlaying = isPlaying,
                        isTtsReady = isTtsReady,
                        ttsError = ttsError,
                        speechRate = speechRate,
                        pitch = pitch,
                        readerFontSizeSp = readerFontSizeSp,
                        onPlayPause = {
                            if (isPlaying) pauseReading() else speakCurrentSentence()
                        },
                        onStop = { stopReading() },
                        onPrevious = {
                            jumpToParagraph(currentParagraphIndex - 1)
                        },
                        onNext = {
                            jumpToParagraph(currentParagraphIndex + 1)
                        },
                        onSeekSentence = { sentenceIndex ->
                            chapterSentences.getOrNull(sentenceIndex)?.let { sentence ->
                                jumpToSentence(sentence)
                            }
                        },
                        onSentenceClick = { sentence ->
                            jumpToSentence(sentence)
                        },
                        onSpeechRateChange = { speechRate = it },
                        onPitchChange = { pitch = it },
                        onReaderFontSizeChange = { readerFontSizeSp = it },
                        onOpenCatalog = { showChapterSheet = true },
                        playbackModeLabel = playbackMode.label,
                        onTogglePlaybackMode = {
                            val nextMode = playbackMode.next()
                            playbackModeName = nextMode.name
                            Toast.makeText(context, "已切换为${nextMode.label}", Toast.LENGTH_SHORT).show()
                        },
                        onInstallVoiceData = {
                            openIntentSafely(
                                Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
                                "无法打开语音数据安装页面"
                            )
                        },
                        onOpenVoiceSettings = {
                            openIntentSafely(
                                Intent("com.android.settings.TTS_SETTINGS")
                                    .also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
                                "无法打开语音设置"
                            )
                        },
                        onRetryTts = { restartTtsCheck() },
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
                jumpToParagraph(chapter.paragraphIndex, autoPlay = isPlaying)
            }
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
    listenedTimeLabel: String,
    remainingTimeLabel: String,
    isPlaying: Boolean,
    isTtsReady: Boolean,
    ttsError: String?,
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
    playbackModeLabel: String,
    onTogglePlaybackMode: () -> Unit,
    onInstallVoiceData: () -> Unit,
    onOpenVoiceSettings: () -> Unit,
    onRetryTts: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showVoiceControls by remember { mutableStateOf(false) }
    var showFontSizeControls by remember { mutableStateOf(false) }
    val context = LocalContext.current
    fun showDevelopingToast() {
        Toast.makeText(context, "功能开发中", Toast.LENGTH_SHORT).show()
    }
    val sentenceListState = rememberLazyListState()
    val seekEndIndex = (chapterSentences.size - 1).coerceAtLeast(0)
    val sliderEndIndex = seekEndIndex.coerceAtLeast(1)
    val seekValue = currentChapterSentenceIndex.coerceIn(0, seekEndIndex).toFloat()

    LaunchedEffect(currentChapterSentenceIndex, chapterSentences.size) {
        if (chapterSentences.isNotEmpty()) {
            sentenceListState.animateScrollToItem((currentChapterSentenceIndex - 1).coerceAtLeast(0))
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TtsStatusStrip(
            isTtsReady = isTtsReady,
            message = ttsError,
            speechRate = speechRate,
            pitch = pitch,
            onInstallVoiceData = onInstallVoiceData,
            onOpenVoiceSettings = onOpenVoiceSettings,
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
                            color = Color.White.copy(alpha = 0.98f),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = (readerFontSizeSp + 2f).sp,
                                lineHeight = (readerFontSizeSp * 1.55f + 2f).sp,
                                fontWeight = FontWeight.SemiBold
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
                            val isCurrentSentence = index == currentChapterSentenceIndex
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSentenceClick(sentence) }
                                    .padding(horizontal = 4.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = sentence.text,
                                    color = if (isCurrentSentence) Color(0xFFF2E8FF) else Color.White.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = readerFontSizeSp.sp,
                                        lineHeight = (readerFontSizeSp * 1.6f).sp,
                                        fontWeight = if (isCurrentSentence) FontWeight.SemiBold else FontWeight.Medium
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
                    value = seekValue,
                    maxValue = sliderEndIndex.toFloat(),
                    onValueChange = { onSeekSentence(it.roundToInt().coerceIn(0, seekEndIndex)) }
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
                ReaderControlButton("上一段", onPrevious) {
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
                ReaderControlButton("下一段", onNext) {
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
                    icon = { Text("◷", fontSize = 20.sp, color = Color.White.copy(alpha = 0.52f)) },
                    onClick = ::showDevelopingToast,
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
    modifier: Modifier = Modifier
) {
    ThinMoonSlider(
        value = value,
        valueRange = 0f..maxValue.coerceAtLeast(1f),
        onValueChange = onValueChange,
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
                            detectTapGestures { offset -> updateFromX(offset.x) }
                        }
                        .pointerInput(valueRange.start, valueRange.endInclusive) {
                            detectDragGestures(
                                onDragStart = { offset -> updateFromX(offset.x) },
                                onDrag = { change, _ ->
                                    updateFromX(change.position.x)
                                    change.consume()
                                }
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
    message: String?,
    speechRate: Float,
    pitch: Float,
    onInstallVoiceData: () -> Unit,
    onOpenVoiceSettings: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                text = "小佩正在读",
                color = Color(0xFFCDBEFF),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ReaderVoiceChip("离线 ▼")
                ReaderVoiceChip("单人 ▼")
                ReaderVoiceChip("语速x2.0 ▼")
            }
        }
        Text(
            text = "换主播 ›",
            color = Color(0xFFCDBEFF),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable(onClick = onOpenVoiceSettings)
        )
    }
}

@Composable
private fun ReaderVoiceChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.9f),
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
                title = "音调",
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
                    Text("减小", color = Color.White.copy(alpha = 0.78f))
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
                    Text("增大", color = Color.White.copy(alpha = 0.78f))
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

private data class ReaderSentence(
    val text: String,
    val paragraphIndex: Int,
    val sentenceIndexInParagraph: Int,
    val chapterSentenceIndex: Int
)

private data class BookReadStateCache(
    val bookUri: String,
    val bookTitle: String,
    val lastParagraphIndex: Int,
    val lastSentenceIndexInParagraph: Int,
    val lastChapterSentenceIndex: Int,
    val lastChapterTitle: String,
    val cachedVisibleText: List<String>,
    val updatedAt: Long
)

private const val BOOK_READ_CACHE_PREFS = "book_read_state_cache"
private const val BOOK_READ_CACHE_SEPARATOR = "\u001E"

private fun bookReadCachePrefix(bookUri: String): String {
    return "book_${bookUri.hashCode()}_"
}

private fun loadBookReadStateCache(context: Context, bookUri: String?): BookReadStateCache? {
    if (bookUri.isNullOrBlank()) return null
    val prefs = context.getSharedPreferences(BOOK_READ_CACHE_PREFS, Context.MODE_PRIVATE)
    val prefix = bookReadCachePrefix(bookUri)
    val storedUri = prefs.getString(prefix + "uri", null) ?: return null
    if (storedUri != bookUri) return null
    val cachedText = prefs
        .getString(prefix + "text", null)
        ?.split(BOOK_READ_CACHE_SEPARATOR)
        ?.filter { it.isNotBlank() }
        .orEmpty()
    return BookReadStateCache(
        bookUri = storedUri,
        bookTitle = prefs.getString(prefix + "title", null).orEmpty(),
        lastParagraphIndex = prefs.getInt(prefix + "paragraph", 0),
        lastSentenceIndexInParagraph = prefs.getInt(prefix + "sentence", 0),
        lastChapterSentenceIndex = prefs.getInt(prefix + "chapter_sentence", 0),
        lastChapterTitle = prefs.getString(prefix + "chapter_title", null).orEmpty(),
        cachedVisibleText = cachedText,
        updatedAt = prefs.getLong(prefix + "updated_at", 0L)
    )
}

private fun saveBookReadStateCache(context: Context, state: BookReadStateCache) {
    if (state.bookUri.isBlank()) return
    val prefix = bookReadCachePrefix(state.bookUri)
    context.getSharedPreferences(BOOK_READ_CACHE_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(prefix + "uri", state.bookUri)
        .putString(prefix + "title", state.bookTitle)
        .putInt(prefix + "paragraph", state.lastParagraphIndex.coerceAtLeast(0))
        .putInt(prefix + "sentence", state.lastSentenceIndexInParagraph.coerceAtLeast(0))
        .putInt(prefix + "chapter_sentence", state.lastChapterSentenceIndex.coerceAtLeast(0))
        .putString(prefix + "chapter_title", state.lastChapterTitle)
        .putString(prefix + "text", state.cachedVisibleText.joinToString(BOOK_READ_CACHE_SEPARATOR))
        .putLong(prefix + "updated_at", state.updatedAt)
        .apply()
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

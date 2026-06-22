package com.shenghui.localvibe.feature.book

import android.content.Intent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shenghui.localvibe.core.book.BookChapter
import com.shenghui.localvibe.core.book.BookChapterDetector
import com.shenghui.localvibe.core.book.TxtBookReader
import com.shenghui.localvibe.core.scanner.LocalMediaFile
import com.shenghui.localvibe.core.tts.BookTtsController
import kotlinx.coroutines.launch

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
    var paragraphs by remember(bookFile?.uri) { mutableStateOf(emptyList<String>()) }
    var currentParagraphIndex by remember(bookFile?.uri) { mutableIntStateOf(initialParagraphIndex.coerceAtLeast(0)) }
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
                if (currentParagraphIndex < list.lastIndex) {
                    val nextIndex = currentParagraphIndex + 1
                    currentParagraphIndex = nextIndex
                    onProgressChanged(file.uri, nextIndex, list.size)
                    ttsController?.speak(list[nextIndex], latestSpeechRate, latestPitch)
                } else {
                    isPlaying = false
                    onProgressChanged(file.uri, list.lastIndex, list.size)
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

    fun speakCurrentParagraph() {
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
        currentParagraphIndex = index
        onBeforeSpeak()
        val started = ttsController?.speak(paragraphs[index], speechRate, pitch) == true
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
        isPlaying = false
        saveProgress(0)
    }

    fun jumpToParagraph(index: Int, autoPlay: Boolean = isPlaying) {
        if (paragraphs.isEmpty()) return
        val nextIndex = index.coerceIn(0, paragraphs.lastIndex)
        ttsController?.stop()
        currentParagraphIndex = nextIndex
        saveProgress(nextIndex)
        if (autoPlay) {
            isPlaying = false
            speakCurrentParagraph()
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
        val result = TxtBookReader.readParagraphs(context.applicationContext, bookFile.uri)
        result
            .onSuccess { loaded ->
                paragraphs = loaded
                currentParagraphIndex = initialParagraphIndex.coerceIn(
                    0,
                    (loaded.size - 1).coerceAtLeast(0)
                )
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
            ttsController?.speak(
                paragraphs[currentParagraphIndex.coerceIn(0, paragraphs.lastIndex)],
                speechRate,
                pitch
            )
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
                                text = "正在读取小说...",
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
                    val visibleChapterParagraphs = paragraphs
                        .asSequence()
                        .drop(currentParagraphIndex.coerceIn(0, paragraphs.lastIndex))
                        .filter { it.isNotBlank() }
                        .take(12)
                        .toList()
                    BookListenContent(
                        chapterTitle = currentChapter?.title ?: "正文",
                        visibleParagraphs = visibleChapterParagraphs,
                        currentParagraphIndex = currentParagraphIndex,
                        totalParagraphs = paragraphs.size,
                        chapterStartIndex = chapterStartIndex,
                        chapterEndExclusive = chapterEndExclusive,
                        isPlaying = isPlaying,
                        isTtsReady = isTtsReady,
                        ttsError = ttsError,
                        speechRate = speechRate,
                        pitch = pitch,
                        onPlayPause = {
                            if (isPlaying) pauseReading() else speakCurrentParagraph()
                        },
                        onStop = { stopReading() },
                        onPrevious = {
                            jumpToParagraph(currentParagraphIndex - 1)
                        },
                        onNext = {
                            jumpToParagraph(currentParagraphIndex + 1)
                        },
                        onSeekParagraph = { index ->
                            jumpToParagraph(index)
                        },
                        onSpeechRateChange = { speechRate = it },
                        onPitchChange = { pitch = it },
                        onOpenCatalog = { showChapterSheet = true },
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
    visibleParagraphs: List<String>,
    currentParagraphIndex: Int,
    totalParagraphs: Int,
    chapterStartIndex: Int,
    chapterEndExclusive: Int,
    isPlaying: Boolean,
    isTtsReady: Boolean,
    ttsError: String?,
    speechRate: Float,
    pitch: Float,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekParagraph: (Int) -> Unit,
    onSpeechRateChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onOpenCatalog: () -> Unit,
    onInstallVoiceData: () -> Unit,
    onOpenVoiceSettings: () -> Unit,
    onRetryTts: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showVoiceControls by remember { mutableStateOf(false) }
    val context = LocalContext.current
    fun showDevelopingToast() {
        Toast.makeText(context, "功能开发中", Toast.LENGTH_SHORT).show()
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (visibleParagraphs.isEmpty()) {
                            Text(
                                text = "暂无正文",
                                color = Color.White.copy(alpha = 0.62f),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 22.sp,
                                    lineHeight = 36.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        } else {
                            visibleParagraphs.forEachIndexed { index, item ->
                                Text(
                                    text = item,
                                    color = Color.White.copy(alpha = if (index == 0) 0.98f else 0.9f),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = if (index == 0) 23.sp else 22.sp,
                                        lineHeight = if (index == 0) 37.sp else 36.sp,
                                        fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
                Text(
                    modifier = Modifier.height(0.dp),
                    text = "$chapterTitle · 本章 ${currentParagraphIndex - chapterStartIndex + 1} / ${(chapterEndExclusive - chapterStartIndex).coerceAtLeast(1)} 段",
                    color = Color.White.copy(alpha = 0.44f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
                        "第 ${currentParagraphIndex + 1} 段",
                        color = Color.White.copy(alpha = 0.52f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        "共 ${totalParagraphs.coerceAtLeast(1)} 段",
                        color = Color.White.copy(alpha = 0.52f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Slider(
                    modifier = Modifier.height(16.dp),
                    value = currentParagraphIndex.toFloat(),
                    onValueChange = { onSeekParagraph(it.toInt()) },
                    valueRange = 0f..(totalParagraphs - 1)
                        .coerceAtLeast(0)
                        .toFloat(),
                    steps = 0,
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(15.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD2C0FF))
                        )
                    },
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color(0xFF8B5CFF),
                        inactiveTrackColor = Color(0xFF332A45),
                        thumbColor = Color(0xFFD2C0FF)
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ReaderControlButton("模式切换", onClick = ::showDevelopingToast) {
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
                    label = "更多",
                    icon = { Text("…", fontSize = 22.sp, color = Color.White.copy(alpha = 0.52f)) },
                    onClick = ::showDevelopingToast,
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
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
                            currentParagraphIndex = currentParagraphIndex,
                            onClick = { onChapterClick(chapter) }
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
    currentParagraphIndex: Int,
    onClick: () -> Unit
) {
    val background = if (isCurrent) Color(0xFF24263A) else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(38.dp)
                .background(if (isCurrent) Color(0xFFB58DFF) else Color.Transparent)
        )
        Text(
            text = chapter.title,
            modifier = Modifier
                .weight(1f)
                .padding(start = 20.dp, end = 12.dp),
            color = Color.White.copy(alpha = if (isCurrent) 0.94f else 0.68f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = if (isCurrent) {
                "当前 · 第 ${currentParagraphIndex + 1} 段"
            } else {
                "第 ${chapter.paragraphIndex + 1} 段"
            },
            modifier = Modifier.padding(end = 16.dp),
            color = if (isCurrent) Color(0xFFD7C4FF) else Color.White.copy(alpha = 0.46f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

private fun LocalMediaFile.displayTitle(): String {
    return name.substringBeforeLast('.', name)
}

package com.shenghui.localvibe.feature.book

import android.content.Intent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                .padding(horizontal = 18.dp, vertical = 10.dp),
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
                        "目录" -> Toast.makeText(context, "章节目录后续实现", Toast.LENGTH_SHORT).show()
                        "跳转段落" -> Toast.makeText(context, "跳转段落功能后续实现", Toast.LENGTH_SHORT).show()
                        "字号设置" -> Toast.makeText(context, "字号设置功能后续实现", Toast.LENGTH_SHORT).show()
                        "夜间模式" -> Toast.makeText(context, "夜间模式后续实现", Toast.LENGTH_SHORT).show()
                        else -> Toast.makeText(context, "更多功能后续实现", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            if (ttsError != null) {
                TtsUnavailableCard(
                    message = ttsError.orEmpty(),
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
                    onRetry = { restartTtsCheck() }
                )
            }

            when {
                isLoading -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
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
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = loadError.orEmpty(),
                            color = Color.White.copy(alpha = 0.76f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    BookListenContent(
                        bookName = bookFile?.displayTitle().orEmpty(),
                        paragraph = paragraphs.getOrNull(currentParagraphIndex).orEmpty(),
                        currentParagraphIndex = currentParagraphIndex,
                        totalParagraphs = paragraphs.size,
                        isPlaying = isPlaying,
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
                        onTimer = {
                            Toast.makeText(context, "定时功能后续实现", Toast.LENGTH_SHORT).show()
                        },
                        onCatalog = {
                            Toast.makeText(context, "章节目录后续实现", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BookListenTopBar(
    title: String,
    onBack: () -> Unit,
    onMenuAction: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
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
                    "重新检测 TTS",
                    "目录",
                    "跳转段落",
                    "字号设置",
                    "夜间模式",
                    "更多功能"
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

@Composable
private fun TtsUnavailableCard(
    message: String,
    onInstallVoiceData: () -> Unit,
    onOpenVoiceSettings: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "系统语音不可用",
                color = Color(0xFF8AB6FF),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = message.ifBlank { "请安装或启用系统中文 TTS 语音包。" },
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onInstallVoiceData) {
                    Text("安装语音数据", color = Color(0xFF8AB6FF))
                }
                TextButton(onClick = onOpenVoiceSettings) {
                    Text("语音设置", color = Color(0xFF8AB6FF))
                }
                TextButton(onClick = onRetry) {
                    Text("重试", color = Color(0xFF8AB6FF))
                }
            }
        }
    }
}

@Composable
private fun BookListenContent(
    bookName: String,
    paragraph: String,
    currentParagraphIndex: Int,
    totalParagraphs: Int,
    isPlaying: Boolean,
    speechRate: Float,
    pitch: Float,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekParagraph: (Int) -> Unit,
    onSpeechRateChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onTimer: () -> Unit,
    onCatalog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        BookCover(title = bookName)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "当前段落",
                    color = Color(0xFF8AB6FF),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = paragraph,
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 7,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(152.dp)
                        .verticalScroll(rememberScrollState())
                )
                Text(
                    text = "${currentParagraphIndex + 1} / $totalParagraphs 段",
                    color = Color.White.copy(alpha = 0.62f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = currentParagraphIndex.toFloat(),
                onValueChange = { onSeekParagraph(it.toInt()) },
                valueRange = 0f..(totalParagraphs - 1).coerceAtLeast(0).toFloat(),
                steps = 0,
                colors = SliderDefaults.colors(
                    activeTrackColor = Color(0xFF4D8DFF),
                    inactiveTrackColor = Color(0xFF2E3445),
                    thumbColor = Color(0xFF8AB6FF)
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("第 ${currentParagraphIndex + 1} 段", color = Color.White.copy(alpha = 0.68f))
                Text("共 $totalParagraphs 段", color = Color.White.copy(alpha = 0.68f))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ListenIconButton("上一段", { Icon(Icons.Filled.SkipPrevious, null) }, onPrevious)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = onPlayPause,
                    modifier = Modifier.size(82.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Text(
                    text = if (isPlaying) "暂停" else "播放",
                    color = Color.White.copy(alpha = 0.76f),
                    fontSize = 12.sp
                )
            }
            ListenIconButton("下一段", { Icon(Icons.Filled.SkipNext, null) }, onNext)
            ListenIconButton("停止", { Icon(Icons.Filled.Stop, null) }, onStop)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ListenIconButton("语速", { Icon(Icons.Filled.Speed, null) }, {})
            ListenIconButton("音调", { Icon(Icons.Filled.GraphicEq, null) }, {})
            ListenIconButton("定时", { Icon(Icons.Filled.Timer, null) }, onTimer)
            ListenIconButton("目录", { Icon(Icons.Filled.List, null) }, onCatalog)
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SettingSlider(
                title = "语速 ${"%.1f".format(speechRate)}x",
                value = speechRate,
                valueRange = 0.6f..1.8f,
                onValueChange = onSpeechRateChange
            )
            SettingSlider(
                title = "音调 ${"%.1f".format(pitch)}",
                value = pitch,
                valueRange = 0.7f..1.5f,
                onValueChange = onPitchChange
            )
        }
    }
}

@Composable
private fun BookCover(title: String) {
    Box(
        modifier = Modifier
            .size(width = 142.dp, height = 202.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF18233A)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(10.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF223459))
        )
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("TXT", color = Color(0xFF8AB6FF), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(14.dp))
            Text(
                text = title.ifBlank { "本地小说" },
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun ListenIconButton(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp)
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides Color.White.copy(alpha = 0.88f)
            ) {
                icon()
            }
        }
        Text(label, color = Color.White.copy(alpha = 0.68f), fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun SettingSlider(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Text(title, color = Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.bodySmall)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                activeTrackColor = Color(0xFF4D8DFF),
                inactiveTrackColor = Color(0xFF2E3445),
                thumbColor = Color(0xFF8AB6FF)
            )
        )
    }
}

private fun LocalMediaFile.displayTitle(): String {
    return name.substringBeforeLast('.', name)
}

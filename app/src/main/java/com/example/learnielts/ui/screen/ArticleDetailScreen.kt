// learnielts/ui/screen/ArticleDetailScreen.kt
package com.example.learnielts.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.learnielts.viewmodel.ArticleViewModel
import com.example.learnielts.viewmodel.DictionaryViewModel
import com.example.learnielts.data.model.Sentence
import com.example.learnielts.utils.AudioPlayer // 用于播放句子音频
import com.example.learnielts.utils.VoiceCacheManager // 用于下载句子音频
import com.example.learnielts.ui.showDefinitionPopup // 用于显示单词释义弹窗
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Translate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.LocalContext
import android.app.AlertDialog
import android.util.Log
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import android.widget.Toast
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.ExperimentalTextApi // ✅ 新增导入

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
@Composable
fun ArticleDetailScreen(
    articleId: Int,
    articleViewModel: ArticleViewModel = viewModel(),
    dictionaryViewModel: DictionaryViewModel = viewModel(),
    onBack: () -> Unit
) {
    val currentArticle by articleViewModel.currentArticle.collectAsState()
    val isLoading by articleViewModel.isLoading.collectAsState()
    val errorMessage by articleViewModel.errorMessage.collectAsState()
    val isFavorite by articleViewModel.isFavorite.collectAsState()
    val userNote by articleViewModel.userNote.collectAsState()

    val context = LocalContext.current
    var showNoteDialog by remember { mutableStateOf(false) }
    var noteInput by remember { mutableStateOf("") }
    val showTranslationForSentence = remember { mutableStateMapOf<Int, Boolean>() }

    LaunchedEffect(articleId) {
        articleViewModel.fetchArticleDetail(articleId)
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            articleViewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(userNote) {
        userNote?.let {
            noteInput = it
        } ?: run {
            noteInput = ""
        }
    }

    // ✅ 核心修改 1: 将 State<T?> 类型的变量转为局部的、可空的 T? 类型变量
    val article = currentArticle

    Scaffold(
        topBar = {
            // ✅ 核心修改 2: 在 UI 的各个部分统一使用这个安全的局部变量
            TopAppBar(
                title = { Text(article?.title ?: "文章详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        articleViewModel.toggleFavorite(articleId)
                    }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "取消收藏" else "收藏",
                            tint = if (isFavorite) Color.Red else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = {
                        showNoteDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "笔记"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (article == null) { // ✅ 使用局部变量进行判断
                Text(
                    text = errorMessage ?: "文章加载失败。",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else { // ✅ 进入此分支时，Kotlin 编译器知道 'article' 是非空的
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column {
                            Text(
                                text = "来源: ${article.source ?: "未知"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "难度: ${article.difficultyLevel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            article.publishDate?.let {
                                Text(
                                    text = "发布日期: ${it.format(DateTimeFormatter.ISO_LOCAL_DATE)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    items(article.content.size) { index ->
                        val sentence = article.content[index]
                        SentenceBlock(
                            sentence = sentence,
                            index = index,
                            dictionaryViewModel = dictionaryViewModel,
                            onToggleTranslation = {
                                showTranslationForSentence[index] = !(showTranslationForSentence[index] ?: false)
                            },
                            showTranslation = showTranslationForSentence[index] ?: false
                        )
                    }
                }
            }
        }
    }

    // 笔记输入对话框
    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("我的笔记") },
            text = {
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("输入你的笔记") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    // ✅ 这里使用安全的 'article' 变量
                    article?.id?.let {
                        articleViewModel.saveUserNote(it, noteInput)
                    }
                    showNoteDialog = false
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalTextApi::class) // ✅ 添加 ExperimentalTextApi
@Composable
fun SentenceBlock(
    sentence: Sentence,
    index: Int,
    dictionaryViewModel: DictionaryViewModel,
    onToggleTranslation: () -> Unit,
    showTranslation: Boolean
) {
    val context = LocalContext.current
    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) // 点击高亮颜色
    val wordRegex = remember { "[a-zA-Z']+".toRegex() } // 匹配英文单词，包括撇号

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color.White, shape = RoundedCornerShape(8.dp)) // 背景色和圆角
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 英文句子 (可点击单词)
            val annotatedString = buildAnnotatedString {
                var lastIndex = 0
                wordRegex.findAll(sentence.sentence).forEach { matchResult ->
                    // 添加非单词部分
                    append(sentence.sentence.substring(lastIndex, matchResult.range.first))

                    // 添加可点击的单词部分
                    val word = matchResult.value
                    withAnnotation(
                        tag = "word_tag",
                        annotation = word
                    ) {
                        withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline, color = highlightColor)) {
                            append(word)
                        }
                    }
                    lastIndex = matchResult.range.last + 1
                }
                // 添加句子剩余部分
                append(sentence.sentence.substring(lastIndex))
            }

            ClickableText(
                text = annotatedString,
                style = MaterialTheme.typography.bodyLarge,
                onClick = { offset ->
                    annotatedString.getStringAnnotations(tag = "word_tag", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            val clickedWord = annotation.item
                            Log.d("ArticleDetailScreen", "Clicked word: $clickedWord")
                            showDefinitionPopup(context, clickedWord, dictionaryViewModel.getDefinition(clickedWord) ?: "（未找到释义）", dictionaryViewModel)
                        }
                },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 播放语音按钮
            sentence.audioUrl?.let { url ->
                // TODO: 可以在这里处理句子语音播放，目前 VoiceCacheManager 只支持通过单词获取文件名
                // 对于句子语音，需要考虑直接通过 URL 下载并播放，或者与后端协商好文件名规则
                // 暂时先使用 AudioPlayer 播放本地文件，假设 url 是可直接播放的本地路径或可下载的文件名
                IconButton(onClick = {
                    // 假设 audioUrl 是服务器上可直接访问的音频文件名，需要下载到本地再播放
                    // 或者你的 VoiceCacheManager 有直接下载 URL 的能力
                    val filenameFromUrl = url.substringAfterLast('/') // 提取文件名
                    val localFile = VoiceCacheManager.getVoiceCacheDir(context).resolve(filenameFromUrl)

                    if (localFile.exists()) {
                        AudioPlayer.play(context, localFile)
                    } else {
                        // 这是一个简化的处理，实际需要从 URL 下载文件并缓存
                        // 由于 VoiceCacheManager 的 getOrDownloadVoiceFile 是针对 word 的，
                        // 这里可能需要扩展 VoiceCacheManager，或者直接使用 OkHttpClient 下载
                        Toast.makeText(context, "正在下载音频...", Toast.LENGTH_SHORT).show()
                        // 简单示例：直接尝试播放 URL （通常不支持）或提醒用户
                        // 实际开发中，需要更复杂的下载逻辑
                        Log.w("ArticleDetailScreen", "Sentence audio not cached: $url. Implement download logic.")
                        // Fallback: 尝试用 TTS 播放整个句子，但这可能不是预期的原始发音
                        // 这一步取决于你的后端如何提供句子音频。如果后端直接提供可播放URL，就直接用MediaPlayer播放URL
                        // 如果后端也需要前端缓存，那就需要下载逻辑
                        // 这里暂时不播放，只做日志提醒
                        Toast.makeText(context, "句子语音暂不可用，请联系管理员上传", Toast.LENGTH_LONG).show()
                    }
                }) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "播放句子语音")
                }
            }

            // 翻译切换按钮
            IconButton(onClick = onToggleTranslation) {
                Icon(Icons.Default.Translate, contentDescription = "切换翻译")
            }
        }

        // 中文翻译
        if (showTranslation) {
            Text(
                text = sentence.translation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
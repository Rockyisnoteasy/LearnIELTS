// 此代码是文章详情页
// learnielts/ui/screen/ArticleDetailScreen.kt
package com.example.learnielts.ui.screen

import androidx.activity.compose.BackHandler // ✅ 新增导入
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
import androidx.compose.ui.text.ExperimentalTextApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp


@OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
@Composable
fun ArticleDetailScreen(
    articleId: Int,
    articleViewModel: ArticleViewModel = viewModel(),
    dictionaryViewModel: DictionaryViewModel = viewModel(),
    onBack: () -> Unit
) {
    // ✅ 新增：处理系统返回手势
    BackHandler {
        onBack()
    }

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

    // 当文章数据成功加载后，触发音频预缓存
    LaunchedEffect(currentArticle) {
        currentArticle?.let { article ->
            articleViewModel.precacheArticleAudio(article)
        }
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

    // 使用 DisposableEffect 来管理资源的生命周期
    // 当这个 Composable 离开屏幕时，onDispose 块中的代码会被执行
    DisposableEffect(Unit) {
        onDispose {
            Log.d("ArticleDetailScreen", "页面离开，停止所有音频播放。")
            AudioPlayer.stop()
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

@OptIn(ExperimentalTextApi::class)
@Composable
fun SentenceBlock(
    sentence: Sentence,
    index: Int,
    dictionaryViewModel: DictionaryViewModel,
    onToggleTranslation: () -> Unit,
    showTranslation: Boolean
) {
    val context = LocalContext.current
    val highlightColor = MaterialTheme.colorScheme.primary
    val wordRegex = remember { "[a-zA-Z']+".toRegex() }
    val scope = rememberCoroutineScope()

    val WORD_TAG = "word_tag"
    val VOLUME_TAG = "volume_tag"
    val TRANSLATE_TAG = "translate_tag"

    // ✅ 在这里分别定义小标题和正文的样式
    val sentenceStyle = if (sentence.isHeading) {
        // --- 小标题样式 ---
        MaterialTheme.typography.headlineSmall.copy(
            fontSize = 24.sp,      // 小标题字号  小标题行高/字号比例保持在1.45
            lineHeight = 35.sp,     // 小标题行高
            fontWeight = FontWeight.Bold
        )
    } else {
        // --- 正文样式 ---
        MaterialTheme.typography.bodyLarge.copy(
            fontSize = 20.sp,       // 正文字号   正文行高/字号比例保持在1.56
            lineHeight = 30.sp      // 正文行高
        )
    }

    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        wordRegex.findAll(sentence.sentence).forEach { matchResult ->
            append(sentence.sentence.substring(lastIndex, matchResult.range.first))
            val word = matchResult.value
            withAnnotation(tag = WORD_TAG, annotation = word) {
                withStyle(style = SpanStyle(color = highlightColor)) {
                    append(word)
                }
            }
            lastIndex = matchResult.range.last + 1
        }
        append(sentence.sentence.substring(lastIndex))

        append("   ")

        sentence.audioUrl?.let {
            withAnnotation(tag = VOLUME_TAG, annotation = it) {
                append("🔊")
            }
        }

        append("  ")

        withAnnotation(tag = TRANSLATE_TAG, annotation = "toggle") {
            append("文A")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        ClickableText(
            text = annotatedString,
            style = sentenceStyle.copy(color = MaterialTheme.colorScheme.onBackground),
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = WORD_TAG, start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        val clickedWord = annotation.item
                        showDefinitionPopup(context, clickedWord, dictionaryViewModel.getDefinition(clickedWord) ?: "（未找到释义）", dictionaryViewModel)
                    }

                annotatedString.getStringAnnotations(tag = VOLUME_TAG, start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        val url = annotation.item
                        val uniqueFilename = md5(url) + ".mp3"
                        val localFile = VoiceCacheManager.getVoiceCacheDir(context).resolve(uniqueFilename)

                        if (localFile.exists()) {
                            AudioPlayer.play(context, localFile)
                        } else {
                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        Toast.makeText(context, "首次播放，正在下载音频...", Toast.LENGTH_SHORT).show()
                                    }
                                    val client = okhttp3.OkHttpClient()
                                    val request = okhttp3.Request.Builder().url(url).build()
                                    val response = client.newCall(request).execute()
                                    if (response.isSuccessful) {
                                        response.body?.byteStream()?.use { input ->
                                            java.io.FileOutputStream(localFile).use { output ->
                                                input.copyTo(output)
                                            }
                                        }
                                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            AudioPlayer.play(context, localFile)
                                        }
                                    } else {
                                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            Toast.makeText(context, "音频下载失败", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        Toast.makeText(context, "音频下载异常", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    }

                annotatedString.getStringAnnotations(tag = TRANSLATE_TAG, start = offset, end = offset)
                    .firstOrNull()?.let {
                        onToggleTranslation()
                    }
            }
        )

        if (showTranslation) {
            Text(
                text = sentence.translation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )
        }
    }
}

// 在App端修改缓存逻辑，确保它为每一个独一无二的音频URL，生成一个同样独一无二的本地缓存文件名。最标准、最稳妥的做法是使用URL的MD5哈希值作为文件名。
private fun md5(input: String): String {
    val md = java.security.MessageDigest.getInstance("MD5")
    val digest = md.digest(input.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}
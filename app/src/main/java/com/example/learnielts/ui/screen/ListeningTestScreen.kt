// 听力测试主页面，播放发音并接收输入答案,MainActivity调用

package com.example.learnielts.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.learnielts.viewmodel.DictionaryViewModel
import androidx.activity.compose.BackHandler

@Composable
fun ListeningTestScreen(
    context: Context,
    words: List<String>,
    viewModel: DictionaryViewModel,
    onFinish: (List<Triple<String, String, Boolean>>) -> Unit,
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }
    var index by remember { mutableStateOf(0) }
    var input by remember { mutableStateOf("") }
    val results = remember { mutableStateListOf<Triple<String, String, Boolean>>() }
    val currentWord = words.getOrNull(index)
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(20.dp)) {

        // ← 返回按钮
        TextButton(onClick = { onBack() }) {
            Text("← 返回")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Listening Test", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(24.dp))

        // 播放按钮
        Button(onClick = {
            currentWord?.let {
                scope.launch {
                    viewModel.playWord(it, context)
                }
            }
        }) {
            Text("🔊 播放语音")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("请输入听到的单词") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (currentWord != null) {
                    val isCorrect = input.trim().equals(currentWord.trim(), ignoreCase = true)
                    results.add(Triple(currentWord, input.trim(), isCorrect))
                }
                input = ""
                if (index + 1 < words.size) {
                    index++
                } else {
                    onFinish(results.toList())
                }
            },
            enabled = input.isNotBlank()
        ) {
            Text("下一题")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("进度：${index + 1} / ${words.size}", style = MaterialTheme.typography.bodyMedium)
    }
}

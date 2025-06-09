// 提供中文释义，用户从中部的4个字母中选择正确字母填写完整单词

package com.example.learnielts.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun ChineseToEnglishSelect(
    questions: List<Pair<String, String>>, // Pair<word, chinese meaning>
    onFinish: (results: List<Quad>) -> Unit,
    onBack: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf("") }
    var isAnswering by remember { mutableStateOf(true) }
    val results = remember { mutableStateListOf<Quad>() }
    val scope = rememberCoroutineScope()

    if (currentIndex >= questions.size) {
        onFinish(results)
        return
    }

    val (word, chinese) = questions[currentIndex]
    val lowerWord = word.lowercase()
    val currentChar = lowerWord.getOrNull(selected.length)

    val candidates = remember(selected, currentIndex) {
        val others = ('a'..'z').filter { it != currentChar }.shuffled().take(3)
        (others + listOfNotNull(currentChar)).shuffled()
    }

    // ✅ 使用全屏Box实现各区域绝对定位
    Box(modifier = Modifier.fillMaxSize()) {

        // 顶部：返回 + 中文释义
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            TextButton(onClick = { onBack() }) {
                Text("← 返回")
            }
            Spacer(Modifier.height(8.dp))
            Text("中文释义：", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(text = chinese)
        }

        // 中部：拼写区固定居中
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.Center
        ) {
            lowerWord.forEachIndexed { i, _ ->
                val ch = selected.getOrNull(i)?.toString() ?: "_"
                Text(
                    text = ch,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        // 底部：候选字母与回退按钮，上移 100.dp
        if (isAnswering) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        if (selected.isNotEmpty()) {
                            selected = selected.dropLast(1)
                        }
                    },
                    modifier = Modifier.width(100.dp)
                ) {
                    Text("← 回退")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    candidates.forEach { option ->
                        Button(
                            onClick = {
                                val newInput = selected + option
                                if (newInput.length == lowerWord.length) {
                                    selected = newInput
                                    isAnswering = false

                                    scope.launch {
                                        delay(1000)
                                        results.add(
                                            Quad(
                                                word = word,
                                                chinese = chinese,
                                                userAnswer = newInput,
                                                correct = newInput.equals(word, ignoreCase = true)
                                            )
                                        )
                                        selected = ""
                                        currentIndex++
                                        isAnswering = true
                                    }
                                } else {
                                    selected = newInput
                                }
                            },
                            modifier = Modifier.size(64.dp)
                        ) {
                            Text(option.toString(), style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }
        }
    }
}




// 提供中文释义，用户从中部的4个字母中选择正确字母填写完整单词

package com.example.learnielts.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun ChineseToEnglishSelect(
    questions: List<Pair<String, String>>, // Pair<word, chinese meaning>
    onFinish: (results: List<Quad>) -> Unit,
    onBack: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf("") }
    val results = remember { mutableStateListOf<Quad>() }

    if (currentIndex >= questions.size) {
        onFinish(results)
        return
    }

    val (word, chinese) = questions[currentIndex]
    val lowerWord = word.lowercase()

    // 当前应选的正确字母
    val currentChar = lowerWord.getOrNull(selected.length)

    // 候选字母（当前正确字母 + 3 个干扰字母）
    val candidates = remember(selected, currentIndex) {
        val others = ('a'..'z').filter { it != currentChar }.shuffled().take(3)
        (others + listOfNotNull(currentChar)).shuffled()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextButton(onClick = { onBack() }) {
            Text("← 返回")
        }
        Spacer(Modifier.height(8.dp))

        Text("中文释义：", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(text = chinese)

        Spacer(Modifier.height(70.dp))

        // 显示已选字母
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
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

        Spacer(modifier = Modifier.height(180.dp))  // 候选字母按钮高度

        // 候选字母按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            candidates.forEach { option ->
                Button(
                    onClick = {
                        val newInput = selected + option
                        if (newInput.length == lowerWord.length) {
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


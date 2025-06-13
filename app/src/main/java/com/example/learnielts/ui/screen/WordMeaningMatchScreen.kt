// learnielts/ui/screen/WordMeaningMatchScreen.kt
package com.example.learnielts.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun WordMeaningMatchScreen(
    initialSessionPairs: List<Pair<String, String>>,
    onFinish: (results: List<Quad>) -> Unit, // 复用 Quad 结构来记录结果
    onBack: () -> Unit
) {
    // --- 状态管理 ---
    var remainingPairs by remember { mutableStateOf(initialSessionPairs) }
    var currentRoundData by remember { mutableStateOf<Pair<List<String>, List<String>>?>(null) }
    var matchedWordsInRound by remember { mutableStateOf(setOf<String>()) }
    var selectedWord by remember { mutableStateOf<String?>(null) }
    var incorrectSelection by remember { mutableStateOf<Pair<String?, String?>>(null to null) }

    // --- 核心逻辑 ---
    fun startNewRound() {
        if (remainingPairs.isEmpty()) {
            onFinish(emptyList()) // 暂时不记录详细对错，直接结束
            return
        }
        val roundPairs = remainingPairs.take(4)
        val words = roundPairs.map { it.first }
        val meanings = roundPairs.map { it.second }.shuffled()
        currentRoundData = words to meanings
        matchedWordsInRound = emptySet()
        selectedWord = null
    }

    LaunchedEffect(remainingPairs) {
        startNewRound()
    }

    // --- UI 渲染 ---
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("← 返回") }
        Spacer(Modifier.height(16.dp))

        if (currentRoundData == null) {
            // 加载中...
        } else {
            val (words, meanings) = currentRoundData!!
            val wordToMeaningMap = remainingPairs.take(4).toMap()

            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 左侧单词列
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    words.forEach { word ->
                        val isMatched = word in matchedWordsInRound
                        val isSelected = word == selectedWord
                        val isIncorrect = incorrectSelection.first == word
                        MatchItem(
                            text = word,
                            isSelected = isSelected,
                            isMatched = isMatched,
                            isIncorrect = isIncorrect,
                            onClick = { if (!isMatched) selectedWord = word }
                        )
                    }
                }

                // 右侧释义列
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    meanings.forEach { meaning ->
                        val correspondingWord = wordToMeaningMap.entries.find { it.value == meaning }?.key
                        val isMatched = correspondingWord in matchedWordsInRound
                        val isIncorrect = incorrectSelection.second == meaning
                        MatchItem(
                            text = meaning,
                            isSelected = false,
                            isMatched = isMatched,
                            isIncorrect = isIncorrect,
                            onClick = {
                                if (!isMatched && selectedWord != null) {
                                    if (wordToMeaningMap[selectedWord] == meaning) {
                                        // 匹配正确
                                        matchedWordsInRound = matchedWordsInRound + selectedWord!!
                                        if (matchedWordsInRound.size == words.size) {
                                            // 本轮结束
                                            remainingPairs = remainingPairs.drop(4)
                                        }
                                        selectedWord = null
                                    } else {
                                        // 匹配错误
                                        incorrectSelection = selectedWord to meaning
                                        selectedWord = null
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // 错误提示效果
    LaunchedEffect(incorrectSelection) {
        if (incorrectSelection.first != null) {
            delay(500)
            incorrectSelection = null to null
        }
    }
}

@Composable
private fun MatchItem(
    text: String,
    isSelected: Boolean,
    isMatched: Boolean,
    isIncorrect: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isMatched -> Color.LightGray.copy(alpha = 0.3f)
            isSelected -> Color(0xFFC8E6C9) // 选中时的淡绿色
            isIncorrect -> Color(0xFFFFCDD2) // 错误时的淡红色
            else -> Color.White
        },
        animationSpec = tween(300)
    )

    val textColor = if (isMatched) Color.Gray else Color.Black

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (!isMatched) {
            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
package com.example.learnielts.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.learnielts.viewmodel.DictionaryViewModel
import kotlinx.coroutines.launch
import com.example.learnielts.utils.ChineseDefinitionExtractor


@Composable
fun WordToMeaningSelect(
    questions: List<Pair<String, String>>,
    viewModel: DictionaryViewModel,
    onFinish: (results: List<Quad>) -> Unit,
    onBack: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(0) }
    val results = remember { mutableStateListOf<Quad>() }
    var candidates by remember(currentIndex) { mutableStateOf<List<String>>(emptyList()) }

    if (currentIndex >= questions.size) {
        onFinish(results)
        return
    }

    val (word, fullDef) = questions[currentIndex]
    val chinese = ChineseDefinitionExtractor.simplify(fullDef) ?: "（无释义）"

    LaunchedEffect(currentIndex) {
        // ✅ --- 修正后的逻辑 ---
        val simplifiedCorrect = ChineseDefinitionExtractor.simplify(fullDef) ?: "（无释义）"
        val distractorsRaw = viewModel.getRandomDistractorDefinitions(word)

        // 1. 简化所有干扰项
        // 2. 过滤掉与正确答案（简化后）相同的项
        // 3. 去重
        // 4. 最后取3个
        val distractors = distractorsRaw
            .mapNotNull { ChineseDefinitionExtractor.simplify(it) }
            .filter { it != simplifiedCorrect }
            .distinct()
            .take(3)

        // 无论干扰项有多少个，都将正确答案添加进去，然后洗牌
        candidates = (distractors + simplifiedCorrect).shuffled()
        // ✅ --- 修正结束 ---
    }


    Column(modifier = Modifier.padding(16.dp)) {
        TextButton(onClick = onBack) { Text("← 返回") }
        Spacer(Modifier.height(8.dp))
        Text("英文单词：", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(word)

        Spacer(Modifier.height(32.dp))

        if (candidates.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            candidates.forEach { option ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        results.add(
                            Quad(
                                word = word,
                                chinese = chinese,
                                userAnswer = option,
                                correct = option == chinese
                            )
                        )
                        currentIndex++
                    }
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text(option.take(30), style = MaterialTheme.typography.titleMedium)
                    }
                }

            }
        }
    }
}

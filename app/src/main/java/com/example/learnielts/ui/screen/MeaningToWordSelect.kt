package com.example.learnielts.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.random.Random
import com.example.learnielts.viewmodel.DictionaryViewModel
import androidx.activity.compose.BackHandler

@Composable
fun MeaningToWordSelect(
    questions: List<Pair<String, String>>,
    viewModel: DictionaryViewModel,
    onFinish: (results: List<Quad>) -> Unit,
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }
    var currentIndex by remember { mutableStateOf(0) }
    val results = remember { mutableStateListOf<Quad>() }

    val coroutineScope = rememberCoroutineScope()

    var candidates by remember(currentIndex) { mutableStateOf<List<String>>(emptyList()) }

    if (currentIndex >= questions.size) {
        onFinish(results)
        return
    }

    val (correctWord, chinese) = questions[currentIndex]

    // 异步加载干扰词
    LaunchedEffect(currentIndex) {
        val distractors = viewModel.getRandomDistractorWords(correctWord)
        candidates = (distractors + correctWord).shuffled()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← 返回")
        }

        Spacer(Modifier.height(8.dp))

        Text("中文释义：", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(chinese)

        Spacer(Modifier.height(32.dp))

        if (candidates.isEmpty()) {
            // 加载中
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            candidates.forEach { option ->
                Button(
                    onClick = {
                        results.add(
                            Quad(
                                word = correctWord,
                                chinese = chinese,
                                userAnswer = option,
                                correct = option.equals(correctWord, ignoreCase = true)
                            )
                        )
                        currentIndex++
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(option, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

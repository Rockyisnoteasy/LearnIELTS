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
        val simplifiedCorrect = ChineseDefinitionExtractor.simplify(fullDef) ?: "（无释义）"
        val distractorsRaw = viewModel.getRandomDistractorDefinitions(word)

        val distractors = distractorsRaw.mapNotNull {
            ChineseDefinitionExtractor.simplify(it)
        }.distinct().filter { it != simplifiedCorrect }.take(3)

        candidates = (distractors + simplifiedCorrect).shuffled()
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

// 中译英测试页面，输入英文回答中文释义，统计成绩,MainActivity调用

package com.example.learnielts.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun ChineseToEnglishTest(
    questions: List<Pair<String, String>>, // Pair<word, chinese meaning>
    onFinish: (results: List<Quad>) -> Unit,
    onBack: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(0) }
    var answer by remember { mutableStateOf(TextFieldValue("")) }
    val results = remember { mutableStateListOf<Quad>() }

    if (currentIndex >= questions.size) {
        onFinish(results)
        return
    }

    val (word, chinese) = questions[currentIndex]

    Column(modifier = Modifier.padding(16.dp)) {

        TextButton(onClick = { onBack() }) {
            Text("← 返回")
        }
        Spacer(Modifier.height(8.dp))

        Text("中文释义：", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(text = chinese)

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it },
            label = { Text("请输入英文") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            val userInput = answer.text.trim()
            val correct = userInput.equals(word, ignoreCase = true)
            results.add(Quad(word, chinese, userInput, correct))
            answer = TextFieldValue("")
            currentIndex++
        }) {
            Text("提交")
        }
    }
}

data class Quad(
    val word: String,
    val chinese: String,
    val userAnswer: String,
    val correct: Boolean
)

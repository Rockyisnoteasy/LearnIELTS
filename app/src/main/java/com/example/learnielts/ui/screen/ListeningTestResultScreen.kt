// 听力测试结果展示页，统计正确率与错词,MainActivity调用

package com.example.learnielts.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ListeningTestResultScreen(
    results: List<Triple<String, String, Boolean>>,
    onRetry: () -> Unit,
    onExit: () -> Unit,
    retryButtonText: String = "重做"
) {
    val correctCount = results.count { it.third }
    val total = results.size
    val percent = if (total > 0) correctCount * 100 / total else 0

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(20.dp)) {

        Text(
            text = "成绩单",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("正确：$correctCount / $total   （$percent%）", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(12.dp))

        Text("错误答案：", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            results.filter { !it.third }.forEach { (correct, userInput, _) ->
                item {
                    SelectionContainer {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text("你的答案：${if (userInput.isBlank()) "<空>" else userInput}")
                            Text("正确答案：$correct")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = onRetry) {
                Text(retryButtonText) // 使用新参数
            }
            Button(onClick = onExit) {
                Text("退出")
            }
        }
    }
}

//所有测试模块通用的测试结果页面（如中译英）,MainActivity调用

package com.example.learnielts.ui.screen
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TestResultScreen(
    results: List<Quad>,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    val correctCount = results.count { it.correct }
    val total = results.size
    val percent = if (total > 0) correctCount * 100 / total else 0

    Column(modifier = Modifier.padding(16.dp)) {
        Text("成绩单", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("正确：$correctCount / $total   正确率：$percent%")

        Spacer(Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(results.filter { !it.correct }) { item ->
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("释义：${item.chinese}")
                    Text("你的答案：${item.userAnswer.ifBlank { "<空>" }}")
                    Text("正确答案：${item.word}", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary))
                }
                Divider()
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onRetry) {
                Text("重试")
            }
            Button(onClick = onBack) {
                Text("返回")
            }
        }
    }
}

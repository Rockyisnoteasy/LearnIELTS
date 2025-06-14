// DailyWordSummaryCard.kt
// 学习计划卡片组件，用于在首页显示进度、新词数等,HomeScreen调用
package com.example.learnielts.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color


@Composable
fun DailyWordSummaryCard(
    planName: String,
    newCount: Int,
    reviewCount: Int,
    totalProgress: Pair<Int, Int>,
    remainingDays: Int,
    onStartClicked: () -> Unit,
    onEditClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp), // 添加间距以便阴影可见
        colors = CardDefaults.cardColors(containerColor = Color.White), // 背景色与整体一致
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp), // 添加阴影
        shape = MaterialTheme.shapes.medium // 可选：圆角优化
    )
 {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(planName, style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onEditClicked) {
                    Text("修改")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = totalProgress.first.toFloat() / totalProgress.second.coerceAtLeast(1),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text("${totalProgress.first} / ${totalProgress.second}    剩余 $remainingDays 天")

            Spacer(modifier = Modifier.height(12.dp))

            Text("今日计划：", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("需新学：$newCount 词   需复习：$reviewCount 词")

            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = onStartClicked, modifier = Modifier.fillMaxWidth()) {
                Text("开始背单词吧！")
            }
        }
    }
}

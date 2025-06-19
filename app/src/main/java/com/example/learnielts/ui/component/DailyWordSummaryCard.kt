// DailyWordSummaryCard.kt
// 学习计划卡片组件，用于在首页显示进度、新词数等,HomeScreen调用
// 文件路径: learnielts/ui/component/DailyWordSummaryCard.kt
package com.example.learnielts.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DailyWordSummaryCard(
    planName: String,
    newCount: Int,
    reviewCount: Int,
    totalProgress: Pair<Int, Int>,
    remainingDays: Int,
    // ✅ 变化点1: 将单个 onStartClicked 替换为两个更具体的 lambda
    onReviewClicked: () -> Unit,
    onLearnNewClicked: () -> Unit,
    onEditClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = MaterialTheme.shapes.medium
    ) {
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

            Text("今日任务：", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("需新学：$newCount 词   需复习：$reviewCount 词")

            Spacer(modifier = Modifier.height(12.dp))

            // ✅ 变化点2: 使用 Row 布局，并根据 newCount 和 reviewCount 的值条件性地显示按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 只有在有单词要复习时，才显示“复习”按钮
                if (reviewCount > 0) {
                    Button(
                        onClick = onReviewClicked,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("复习 $reviewCount 词")
                    }
                }
                // 只有在有单词要新学时，才显示“新学”按钮
                if (newCount > 0) {
                    Button(
                        onClick = onLearnNewClicked,
                        modifier = Modifier.weight(1f),
                        // 如果有复习任务，新学按钮可以设为次要样式
                        colors = if (reviewCount > 0) ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ) else ButtonDefaults.buttonColors()
                    ) {
                        Text("学习 $newCount 词")
                    }
                }
            }
        }
    }
}

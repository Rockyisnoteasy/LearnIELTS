// HomeScreen.kt
// 首页模块，显示搜索栏和今日学习计划卡片，点击按钮进入翻牌页或学习计划页，MainActivity调用
package com.example.learnielts.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.example.learnielts.utils.FileHelper
import com.example.learnielts.ui.component.DailyWordSummaryCard
import com.example.learnielts.ui.component.DictionarySearchBar
import com.example.learnielts.viewmodel.DictionaryViewModel

@Composable
fun HomeScreen(
    context: Context,
    viewModel: DictionaryViewModel,
    onStartClicked: (List<String>) -> Unit,
    onEnterLearningPlan: () -> Unit
) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val yesterday = remember {
        Calendar.getInstance().apply { add(Calendar.DATE, -1) }.let {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.time)
        }
    }

    val allPlans = remember { FileHelper.loadAllPlans(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

    DictionarySearchBar(viewModel)

        Spacer(modifier = Modifier.height(16.dp))

        if (allPlans.isEmpty()) {
            Text("尚未创建任何学习计划", style = MaterialTheme.typography.bodyLarge)
        } else {
            allPlans.forEach { plan ->
                val todayWords = FileHelper.getWordsForDate(
                    context, today, plan.planName
                )
                val reviewWords = FileHelper.getWordsForDate(
                    context, yesterday, plan.planName
                )
                val progress = FileHelper.calculateProgress(context, plan.planName)
                val remainingDays = 30 // 或根据计划动态计算

                Spacer(modifier = Modifier.height(16.dp))
                DailyWordSummaryCard(
                    planName = plan.planName,
                    newCount = todayWords.size,
                    reviewCount = reviewWords.size,
                    totalProgress = progress,
                    remainingDays = remainingDays,
                    onStartClicked = {
                        // 每次点击都生成今天的新词表（防重复）
                        FileHelper.generateTodayWordListFromPlan(
                            context,
                            plan.category,
                            plan.selectedPlan,
                            plan.planName,
                            plan.dailyCount
                        )
                        val updatedTodayWords = FileHelper.getWordsForDate(
                            context, today, plan.planName
                        )
                        onStartClicked(updatedTodayWords)
                    },
                    onEditClicked = onEnterLearningPlan
                )
            }
        }
    }
}


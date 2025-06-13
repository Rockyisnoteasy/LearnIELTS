// HomeScreen.kt
// 首页模块，显示搜索栏和今日学习计划卡片，点击按钮进入翻牌页或学习计划页，MainActivity调用
package com.example.learnielts.ui.screen

import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.example.learnielts.utils.FileHelper
import com.example.learnielts.ui.component.DailyWordSummaryCard
import com.example.learnielts.ui.component.DictionarySearchBar
import com.example.learnielts.viewmodel.DictionaryViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.learnielts.viewmodel.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


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

    val scope = rememberCoroutineScope()
    val authViewModel: AuthViewModel = viewModel()

    // 从 ViewModel 的 StateFlow 中收集计划列表，使其可被观察
    val allPlans by authViewModel.plans.collectAsState()

    // ✅ 新增：用于控制计划列表展开/折叠的状态
    var isExpanded by remember { mutableStateOf(false) }

    // 确保在 HomeScreen 首次加载时，能从文件初始化一次状态
    LaunchedEffect(Unit) {
        authViewModel.loadPlans()
    }


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
            // ✅ 改进的逻辑：总是显示第一个计划
            val firstPlan = allPlans.first()
            DisplayPlanCard(context, firstPlan, viewModel, onStartClicked, onEnterLearningPlan)

            // ✅ 新增：如果计划多于一个，用 AnimatedVisibility 实现带动画的展开和折叠
            if (allPlans.size > 1) {
                AnimatedVisibility(visible = isExpanded) {
                    Column {
                        // 从第二个计划开始显示
                        allPlans.drop(1).forEach { plan ->
                            Spacer(modifier = Modifier.height(16.dp))
                            DisplayPlanCard(context, plan, viewModel, onStartClicked, onEnterLearningPlan)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ✅ 新增：展开/折叠按钮
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isExpanded) "收起" else "展开查看其余 ${allPlans.size - 1} 个计划")
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "收起" else "展开"
                    )
                }
            }
        }
    }
}

/**
 * ✅ 新增：将计划卡片的显示逻辑提取到一个独立的 Composable 函数中，方便复用
 */
@Composable
private fun DisplayPlanCard(
    context: Context,
    plan: com.example.learnielts.utils.PlanInfo,
    viewModel: DictionaryViewModel,
    onStartClicked: (List<String>) -> Unit,
    onEnterLearningPlan: () -> Unit
) {
    val authViewModel: AuthViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val progress = FileHelper.calculateProgress(
        context = context,
        planName = plan.planName,
        category = plan.category,
        selectedPlan = plan.selectedPlan
    )
    Log.d("调试", "📈 显示计划：${plan.planName}，进度 = ${progress.first} / ${progress.second}")

    val remainingDays = if (progress.second > 0 && plan.dailyCount > 0) {
        (progress.second - progress.first + plan.dailyCount - 1).coerceAtLeast(0) / plan.dailyCount
    } else {
        0
    }

    DailyWordSummaryCard(
        planName = plan.planName,
        newCount = FileHelper.getWordsForDate(context, today, plan.planName).size,
        reviewCount = FileHelper.getWordsForDate(
            context,
            remember {
                Calendar.getInstance().apply { add(Calendar.DATE, -1) }.let {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.time)
                }
            },
            plan.planName
        ).size,
        totalProgress = progress,
        remainingDays = remainingDays,
        onStartClicked = {
            scope.launch {
                Log.d("调试", "🚀 点击开始背单词：${plan.planName}")

                val newWords = withContext(Dispatchers.IO) {
                    FileHelper.generateTodayWordListFromPlan(
                        context,
                        plan.category,
                        plan.selectedPlan,
                        plan.planName,
                        plan.dailyCount
                    )
                }
                Log.d("调试", "📦 单词生成完毕，共 ${newWords.size} 个新词")

                plan.serverId?.let { serverId ->
                    authViewModel.uploadDailyWords(serverId, newWords)
                }

                val updatedTodayWords = FileHelper.getWordsForDate(context, today, plan.planName)
                onStartClicked(updatedTodayWords)
            }
        },
        onEditClicked = onEnterLearningPlan
    )
}
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
import androidx.compose.runtime.remember
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.util.UUID


@Composable
fun HomeScreen(
    context: Context,
    viewModel: DictionaryViewModel,
    onStartClicked: (List<String>) -> Unit,
    onEnterLearningPlan: () -> Unit,
    // ✅ 新增：用于进入阅读界面的回调
    onEnterReadingScreen: () -> Unit
) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val yesterday = remember {
        Calendar.getInstance().apply { add(Calendar.DATE, -1) }.let {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.time)
        }
    }

    val scope = rememberCoroutineScope()
    val authViewModel: AuthViewModel = viewModel()
    val allPlans by authViewModel.plans.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }

    // ✅ 修正：使用一个稳定的状态来触发UI刷新
    var refreshKey by remember { mutableStateOf(UUID.randomUUID()) }

    // 确保在 HomeScreen 首次加载时，能从文件初始化一次状态
    LaunchedEffect(Unit) {
        authViewModel.loadPlans()
    }

    // 在计划列表加载后，主动检查并生成当天的单词列表
    LaunchedEffect(allPlans) {
        if (allPlans.isNotEmpty()) {
            var needsRefresh = false
            coroutineScope {
                allPlans.forEach { plan ->
                    val todayFile = File(context.getExternalFilesDir("word_schedule/${plan.planName}"), "$today.json")
                    if (!todayFile.exists()) {
                        needsRefresh = true
                        launch(Dispatchers.IO) {
                            Log.d("调试", "【主动检查】为 ${plan.planName} 生成今日词表...")
                            val newWords = FileHelper.generateTodayWordListFromPlan(
                                context, plan.category, plan.selectedPlan, plan.planName, plan.dailyCount
                            )
                            plan.serverId?.let { serverId ->
                                if (newWords.isNotEmpty()) {
                                    authViewModel.uploadDailyWords(serverId, newWords)
                                }
                            }
                        }
                    }
                }
            }

            if (needsRefresh) {
                delay(1500) // 等待文件IO
                refreshKey = UUID.randomUUID() // 更新Key以触发UI刷新
                Log.d("调试", "【主动检查】文件生成完毕，刷新UI")
            }
        }
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
            // 决定要显示的计划列表（折叠或展开）
            val plansToShow = if (isExpanded || allPlans.size <= 1) allPlans else listOf(allPlans.first())

            plansToShow.forEach { plan ->
                // ✅ 核心修正：在这里计算所有需要显示的值，并用 remember 包裹
                // 当 refreshKey 变化时，这些值会全部重新计算
                val newCount = remember(plan, refreshKey) {
                    FileHelper.getWordsForDate(context, today, plan.planName).size
                }
                val reviewCount = remember(plan, refreshKey) {
                    FileHelper.getWordsForDate(context, yesterday, plan.planName).size
                }
                val progress = remember(plan, refreshKey) {
                    FileHelper.calculateProgress(
                        context, plan.planName, plan.category, plan.selectedPlan
                    )
                }
                val remainingDays = if (progress.second > 0 && plan.dailyCount > 0) {
                    (progress.second - progress.first + plan.dailyCount - 1).coerceAtLeast(0) / plan.dailyCount
                } else {
                    0
                }

                // 将计算好的值传递给卡片组件
                DisplayPlanCard(
                    context = context,
                    plan = plan,
                    newCount = newCount,
                    reviewCount = reviewCount,
                    progress = progress,
                    remainingDays = remainingDays,
                    onStartClicked = onStartClicked,
                    onEditClicked = onEnterLearningPlan // 确保这里是 onEditClicked
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 折叠/展开按钮
            if (allPlans.size > 1) {
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

        // ✅ 新增：精选阅读入口
        Button(
            onClick = onEnterReadingScreen, // 使用传入的回调函数
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp) // 在这里添加一些顶部内边距
        ) {
            Text("📚 精选阅读")
        }
    }
}

/**
 * ✅ 修正：将卡片显示逻辑提取，并只接收计算好的数据，不再自己计算
 */
@Composable
private fun DisplayPlanCard(
    context: Context,
    plan: com.example.learnielts.utils.PlanInfo,
    newCount: Int,
    reviewCount: Int,
    progress: Pair<Int, Int>,
    remainingDays: Int,
    onStartClicked: (List<String>) -> Unit,
    // ✅ 修正：将 onEnterLearningPlan 改名为 onEditClicked 以匹配 DailyWordSummaryCard 的参数名
    onEditClicked: () -> Unit
) {
    val authViewModel: AuthViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    DailyWordSummaryCard(
        planName = plan.planName,
        newCount = newCount,
        reviewCount = reviewCount,
        totalProgress = progress,
        remainingDays = remainingDays,
        onStartClicked = {
            scope.launch {
                // 点击“开始背单词”的逻辑保持不变，它是一个用户主动行为
                val wordsToLearn = withContext(Dispatchers.IO) {
                    FileHelper.getWordsForDate(context, today, plan.planName).ifEmpty {
                        FileHelper.generateTodayWordListFromPlan(
                            context, plan.category, plan.selectedPlan, plan.planName, plan.dailyCount
                        ).also { newWords ->
                            if (newWords.isNotEmpty()) {
                                plan.serverId?.let { serverId ->
                                    authViewModel.uploadDailyWords(serverId, newWords)
                                }
                            }
                        }
                    }
                }
                onStartClicked(wordsToLearn)
            }
        },
        onEditClicked = onEditClicked // 将传入的 onEditClicked 传递给 DailyWordSummaryCard
    )
}
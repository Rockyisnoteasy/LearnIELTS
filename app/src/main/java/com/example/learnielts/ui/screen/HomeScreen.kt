// 文件路径: learnielts/ui/screen/HomeScreen.kt
package com.example.learnielts.ui.screen

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.learnielts.ui.component.DailyWordSummaryCard
import com.example.learnielts.ui.component.DictionarySearchBar
import com.example.learnielts.utils.FileHelper
import com.example.learnielts.utils.PlanInfo
import com.example.learnielts.viewmodel.ArticleViewModel
import com.example.learnielts.viewmodel.AuthViewModel
import com.example.learnielts.viewmodel.DictionaryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    context: Context,
    viewModel: DictionaryViewModel,
    articleViewModel: ArticleViewModel,
    authViewModel: AuthViewModel,
    // ✅ V2.1: 修改回调，让它们能传递被点击的 plan 对象
    onStartLearnNew: (plan: PlanInfo) -> Unit,
    onStartReview: (plan: PlanInfo) -> Unit,
    onEnterLearningPlan: () -> Unit,
    onEnterReadingScreen: () -> Unit,
    onArticleClick: (Int) -> Unit,
    onEnterNotification: () -> Unit
) {
    val allPlans by authViewModel.plans.collectAsState()
    val dailySessions by authViewModel.dailySessions.collectAsState()
    val token by authViewModel.token.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }

    // 主数据加载触发器
    LaunchedEffect(token) {
        if (token != null) {
            authViewModel.loadPlans()
            articleViewModel.fetchLatestArticle()
        }
    }

    // ✅ V2.1: 当计划列表加载后，为每个计划获取其独立的session
    LaunchedEffect(allPlans) {
        if (allPlans.isNotEmpty()) {
            allPlans.forEach { plan ->
                plan.serverId?.let { authViewModel.fetchDailySessionForPlan(it) }
            }
        }
    }

    // “每日新词主动检查”逻辑
    LaunchedEffect(allPlans) {
        if (allPlans.isNotEmpty()) {
            var needsRefresh = false
            // ✅ V2.1: 先获取全局已掌握单词
            val masteredWords = authViewModel.fetchMasteredWords()
            coroutineScope {
                allPlans.forEach { plan ->
                    val todayFile = File(context.getExternalFilesDir("word_schedule/${plan.planName}"), SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) + ".json")
                    if (!todayFile.exists()) {
                        needsRefresh = true
                        launch(Dispatchers.IO) {
                            // ✅ V2.1: 调用新版函数，传入 plan 对象和 masteredWords
                            val newWords = FileHelper.generateTodayWordListFromPlan(context, plan, masteredWords)
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
                delay(1500)
                // 为所有计划重新获取一次session，以刷新数字
                allPlans.forEach { plan ->
                    plan.serverId?.let { authViewModel.fetchDailySessionForPlan(it) }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                DictionarySearchBar(viewModel)
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onEnterNotification) {
                Icon(Icons.Default.Notifications, contentDescription = "通知")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // 我们只在至少有一个计划处于“暂停”状态时显示总提示
        if (dailySessions.values.any { it.isNewWordPaused }) {
            Text(
                text = "💡 部分计划复习任务繁重，已暂停学习新词，请先巩固旧词。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        if (allPlans.isEmpty()) {
            Text("尚未创建任何学习计划", style = MaterialTheme.typography.bodyLarge)
        } else {
            val plansToShow = if (isExpanded || allPlans.size <= 1) allPlans else listOf(allPlans.first())

            plansToShow.forEach { plan ->
                // ✅ V2.1: 从 Map 中精确获取本计划对应的 session
                val session = plan.serverId?.let { dailySessions[it] }
                val reviewCount = session?.reviewWords?.size ?: 0
                // ✅ V2.1: 新学数量也由本计划的session决定
                val newCount = if (session?.isNewWordPaused == false) plan.dailyCount else 0

                DisplayPlanCard(
                    context = context,
                    plan = plan,
                    reviewCount = reviewCount,
                    newCount = newCount,
                    onReviewClicked = { onStartReview(plan) }, // 传递 plan
                    onLearnNewClicked = { onStartLearnNew(plan) }, // 传递 plan
                    onEditClicked = onEnterLearningPlan
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (allPlans.size > 1) {
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isExpanded) "收起" else "展开查看其余 ${allPlans.size - 1} 个计划")
                    Icon(imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
                }
            }
        }

        Button(
            onClick = onEnterReadingScreen,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text("📚 精选阅读")
        }
    }
}

@Composable
private fun DisplayPlanCard(
    context: Context,
    plan: PlanInfo,
    reviewCount: Int,
    newCount: Int,
    onReviewClicked: () -> Unit,
    onLearnNewClicked: () -> Unit,
    onEditClicked: () -> Unit
) {
    val progress = remember(plan, reviewCount) {
        FileHelper.calculateProgress(context, plan.planName, plan.category, plan.selectedPlan)
    }
    val remainingDays = if (progress.second > 0 && plan.dailyCount > 0) {
        (progress.second - progress.first + plan.dailyCount - 1).coerceAtLeast(0) / plan.dailyCount
    } else {
        0
    }

    DailyWordSummaryCard(
        planName = plan.planName,
        newCount = newCount,
        reviewCount = reviewCount,
        totalProgress = progress,
        remainingDays = remainingDays,
        onReviewClicked = onReviewClicked,
        onLearnNewClicked = onLearnNewClicked,
        onEditClicked = onEditClicked
    )
}
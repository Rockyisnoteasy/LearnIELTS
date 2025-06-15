// 设置学习计划（如每日学习词数），保存到本地 word_schedule 目录，MainActivity、FileHelper调用
// 这个页面应当是用户看到的第一个页面，因为用户要先选择学习计划，才能继续下面的学习。

package com.example.learnielts.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import android.util.Log
import androidx.compose.ui.platform.LocalContext

import com.example.learnielts.utils.FileHelper
import com.example.learnielts.utils.PlanInfo
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.learnielts.viewmodel.AuthViewModel
import androidx.activity.compose.BackHandler


@Composable
fun LearningPlanScreen(
    onBack: () -> Unit,
    onPlanSelected: (String) -> Unit
) {
    // 一级分类数据
    val categories = listOf("考研", "四六级", "雅思", "高考", "GRE", "托福", "英专") // ✅ 确保这里包含所有你的分类
    val context = LocalContext.current
    var planNameInput by remember { mutableStateOf("") }

    // 二级单词列表数据动态读取
    val planMap = remember {
        categories.associateWith { category ->
            FileHelper.getAvailableDbPlans(context, category)
        }
    }

    val scope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    var showDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var selectedPlanName by remember { mutableStateOf<String?>(null) } // 这里的 selectedPlanName 是不带 .db 后缀的原始文件名
    var showNameConflictDialog by remember { mutableStateOf(false) }
    val authViewModel: AuthViewModel = viewModel()

    // 处理返回手势
    BackHandler {
        if (selectedCategory != null) {
            // 如果在第二级（已选择分类），则返回第一级
            selectedCategory = null
        } else {
            // 如果在第一级（未选择分类），则关闭此页面返回首页
            onBack()
        }
    }


    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "返回",
                modifier = Modifier
                    .size(36.dp)
                    .clickable {
                        if (selectedCategory != null) {
                            selectedCategory = null
                        } else {
                            onBack()
                        }
                    }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = selectedCategory ?: "选择学习计划分类",
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedCategory == null) {
            // 显示一级分类列表
            LazyColumn {
                items(categories) { category ->
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCategory = category }
                            .padding(vertical = 12.dp)
                    )
                    Divider()
                }
            }
        } else {
            // 显示该分类下的单词列表
            val plans = planMap[selectedCategory] ?: emptyList()
            LazyColumn {
                items(plans) { plan ->
                    Text(
                        text = plan, // plan 现在是不带 .db 后缀的原始文件名
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedPlanName = plan
                                inputText = ""
                                showDialog = true
                                Log.d("调试", "用户选择了词表：$selectedCategory → $plan")
                            }


                            .padding(vertical = 12.dp)
                    )
                    Divider()
                }
            }
        }

        if (showDialog && selectedPlanName != null) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("制定学习计划") },
                text = {
                    Column {
                        Text("请输入该学习计划的名称：")
                        OutlinedTextField(
                            value = planNameInput,
                            onValueChange = { planNameInput = it },
                            label = { Text("如：冲刺四级七天计划") }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("请输入每天学习的单词数：")
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = {
                                // 限制只能输入正整数
                                if (it.all { ch -> ch.isDigit() }) inputText = it
                            },
                            label = { Text("只能输入正整数") }
                        )
                    }
                },
                // 在 AlertDialog 的 confirmButton -> TextButton 的 onClick lambda 中
                confirmButton = {
                    TextButton(onClick = {
                        val count = inputText.toIntOrNull()
                        val planName = planNameInput.trim()

                        if (planName.isNotBlank() && count != null && count > 0) {
                            val existingPlans = FileHelper.loadAllPlans(context)
                            if (existingPlans.any { it.planName.equals(planName, ignoreCase = true) }) {
                                showNameConflictDialog = true
                            } else {
                                // ✅ 调用 ViewModel 的新函数来统一处理创建和上传
                                authViewModel.createNewPlan(
                                    planName = planName,
                                    category = selectedCategory!!,
                                    selectedPlan = selectedPlanName!!,
                                    dailyCount = count
                                )
                                showDialog = false // 关闭对话框
                                onBack() // ✅ 新增：成功创建计划后调用 onBack() 返回首页
                            }
                        }
                    }) {
                        Text("确定")
                    }
                },
// ...
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }


    }

    if (showNameConflictDialog) {
        AlertDialog(
            onDismissRequest = { showNameConflictDialog = false },
            title = { Text("命名冲突") },
            text = { Text("该计划已存在，请重新输入") },
            confirmButton = {
                TextButton(onClick = { showNameConflictDialog = false }) {
                    Text("确定")
                }
            }
        )
    }

}


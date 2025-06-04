// 日期选择器组件，供多个模块选择学习词表来源日期,FlipCardMenuScreen、ChineseToEnglishSetup、ListeningTestSetupScreen等调用
package com.example.learnielts.ui.screen.common

import android.content.Context
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class WordPlanSource {
    LEARNED_WORDS,
    SCHEDULED_PLAN
}

@Composable
fun DateWordPickerScreen(
    context: Context,
    title: String = "选择日期",
    allowMultiSelect: Boolean = false,
    source: WordPlanSource = WordPlanSource.LEARNED_WORDS,
    selectedPlanName: String? = null,
    onConfirm: (List<String>) -> Unit,
    onBack: () -> Unit
) {
    val folder: File = when (source) {
        WordPlanSource.LEARNED_WORDS -> {
            File(context.getExternalFilesDir("learned_words") ?: File(""), "")
        }

        WordPlanSource.SCHEDULED_PLAN -> {
            try {
                val planName = selectedPlanName ?: run {
                    val planFile = File(context.getExternalFilesDir(null), "current_plan.json")
                    if (!planFile.exists()) {
                        Log.e("调试", "❌ current_plan.json 不存在，且未提供 planName")
                        return@run null
                    }
                    val json = JSONObject(planFile.readText())
                    json.getString("planName")
                }

                if (planName == null) {
                    File("") // 返回空路径
                } else {
                    Log.d("调试", "✅ 使用计划名称：$planName")
                    File(context.getExternalFilesDir("word_schedule"), planName)
                }
            } catch (e: Exception) {
                Log.e("调试", "❌ 获取计划目录失败：${e.message}")
                File("")
            }
        }

    }


    val datePattern = Regex("""\d{4}-\d{2}-\d{2}""")
    val files = folder.listFiles { file ->
        file.extension == "json" && datePattern.matches(file.nameWithoutExtension)
    }?.sortedByDescending { it.name } ?: emptyList()

    val dateWords = remember {
        files.associate { file ->
            val words = try {
                val content = file.readText()
                val array = JSONArray(content)
                List(array.length()) { array.getString(it) }
            } catch (e: Exception) {
                emptyList()
            }
            file.nameWithoutExtension to words
        }
    }

    val selectedDates = remember { mutableStateListOf<String>() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            dateWords.forEach { (date, words) ->
                item {
                    val isSelected = selectedDates.contains(date)

                    val rowModifier = if (allowMultiSelect) {
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .toggleable(
                                value = isSelected,
                                onValueChange = {
                                    if (it) selectedDates.add(date) else selectedDates.remove(date)
                                }
                            )
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                selectedDates.clear()
                                selectedDates.add(date)
                            }
                    }

                    Row(modifier = rowModifier) {
                        if (allowMultiSelect) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("$date（${words.size}词）")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = onBack) {
                Text("← 返回")
            }
            Button(
                onClick = {
                    val selectedWords = selectedDates.flatMap { dateWords[it] ?: emptyList() }.distinct()
                    onConfirm(selectedWords)
                },
                enabled = selectedDates.isNotEmpty()
            ) {
                Text("开始")
            }
        }
    }
}


// WordListScreen.kt
// 查看或手动添加某日期的学习单词，支持释义弹窗，MainActivity调用

package com.example.learnielts.ui.screen

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.learnielts.ui.showDefinitionPopup
import java.util.*
import com.example.learnielts.utils.FileHelper
import com.example.learnielts.viewmodel.DictionaryViewModel
import com.example.learnielts.util.RelatedWordsManager

@Composable
fun WordListScreen(
    context: Context,
    getDefinition: (String) -> String?,
    viewModel: DictionaryViewModel,
    onBackToMenu: () -> Unit
) {
    var showAddPage by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf("") }

    if (!showAddPage) {
        Column(modifier = Modifier.padding(16.dp)) {
            Button(onClick = onBackToMenu) {
                Text("← 返回主菜单")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val calendar = Calendar.getInstance()
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            selectedDate = "%04d-%02d-%02d".format(year, month + 1, day)
                            showAddPage = true
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
            ) {
                Text("选择日期")
            }
        }
    } else {
        var wordInput by remember { mutableStateOf("") }

        // ✅ 使用不可变 List<String>
        var wordList by remember {
            mutableStateOf(
                com.example.learnielts.utils.FileHelper
                    .getWordsForDate(context, selectedDate)
            )
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Button(onClick = { showAddPage = false }) {
                Text("← 返回日期选择")
            }

            Spacer(Modifier.height(12.dp))
            Text("日期：$selectedDate", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = wordInput,
                    onValueChange = { wordInput = it },
                    label = { Text("输入单词") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    if (wordInput.isNotBlank()) {
                        val newWord = wordInput.trim()
                        if (!wordList.contains(newWord)) {
                            wordList = wordList + newWord  // ✅ 触发 recomposition
                            com.example.learnielts.utils.FileHelper
                                .saveWordsForDate(context, selectedDate, wordList)
                        }
                        wordInput = ""
                    }
                }) {
                    Text("添加")
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("已添加单词：", style = MaterialTheme.typography.bodyLarge)

            LazyColumn {
                items(wordList) { word ->
                    Text(
                        text = word,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val realWord = RelatedWordsManager.getRealSourceWord(word, context)
                                val def = viewModel.getDefinition(realWord)
                                if (def != null) {
                                    showDefinitionPopup(context, realWord, def ?: "（未找到释义）", viewModel)
                                }
                            }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}


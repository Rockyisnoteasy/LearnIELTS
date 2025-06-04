// DictionarySearchBar.kt
// 统一封装搜索栏组件，供首页和词典查询共用,HomeScreen、DictionaryScreen调用
package com.example.learnielts.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.learnielts.viewmodel.DictionaryViewModel
import com.example.learnielts.viewmodel.TTSProvider

@Composable
fun DictionarySearchBar(viewModel: DictionaryViewModel) {
    var input by remember { mutableStateOf("") }
    var definition by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Column {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("输入中英文单词；右滑打开功能菜单") },
            trailingIcon = {
                IconButton(onClick = {
                    viewModel.playWord(input, context)
                }) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "播放发音")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = {
                val isChinese = input.any { it.toInt() > 127 }
                if (isChinese) {
                    val results = viewModel.queryByChineseKeyword(input)
                    definition = if (results.isNotEmpty()) {
                        "匹配到以下英文单词：\n" + results.joinToString("\n")
                    } else {
                        "❌ 未找到匹配的英文单词"
                    }
                } else {
                    definition = viewModel.getDefinition(input)
                }
            }) {
                Text("查询")
            }

            var showMenu by remember { mutableStateOf(false) }

            Box {
                Button(onClick = { showMenu = true }) {
                    Text("语音来源")
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Google") },
                        onClick = {
                            viewModel.setTtsProvider(TTSProvider.Google)
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Tencent") },
                        onClick = {
                            viewModel.setTtsProvider(TTSProvider.Tencent)
                            showMenu = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        definition?.let {
            Text("释义：\n$it", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

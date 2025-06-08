// DictionarySearchBar.kt
// 统一封装搜索栏组件，供首页和词典查询共用,HomeScreen、DictionaryScreen调用
package com.example.learnielts.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.learnielts.viewmodel.DictionaryViewModel
import com.example.learnielts.viewmodel.TTSProvider
import com.example.learnielts.utils.ChineseDefinitionExtractor
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionarySearchBar(viewModel: DictionaryViewModel) {
    var input by remember { mutableStateOf("") }
    var definition by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val suggestions = remember { mutableStateListOf<Pair<String, String>>() }
    val showDropdown = remember { mutableStateOf(true) }
    val focusManager = LocalFocusManager.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDefinitionSheet by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val screenHeightDp: Dp = configuration.screenHeightDp.dp
    val highlightColor = Color.Blue
    val baseColor = Color.Black

    LaunchedEffect(input) {
        suggestions.clear()
        if (input.isNotBlank() && input.all { it.isLetter() } && showDropdown.value) {
            suggestions += viewModel.entries.value
                .filter { it.word.startsWith(input, ignoreCase = true) }
                .mapNotNull { entry ->
                    val chinese = ChineseDefinitionExtractor.simplify(entry.definition)
                    if (chinese != null) entry.word to chinese else null
                }
                .take(8)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(1f)
    ) {
        Column {
            OutlinedTextField(
                value = input,
                onValueChange = {
                    input = it
                    showDropdown.value = true
                },
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
                    definition = if (isChinese) {
                        val results = viewModel.queryByChineseKeyword(input)
                        if (results.isNotEmpty()) {
                            "匹配到以下英文单词：\n" + results.joinToString("\n")
                        } else {
                            "❌ 未找到匹配的英文单词"
                        }
                    } else {
                        viewModel.getDefinition(input)
                    }
                    showDefinitionSheet = true
                    showDropdown.value = false
                    suggestions.clear()
                    focusManager.clearFocus()
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
        }

        if (suggestions.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 56.dp)
                    .heightIn(max = 300.dp),
                shadowElevation = 8.dp,
                color = Color.White,
                tonalElevation = 4.dp,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    suggestions.forEach { (word, chinese) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    input = word
                                    definition = viewModel.getDefinition(word)
                                    showDefinitionSheet = true
                                    suggestions.clear()
                                    showDropdown.value = false
                                    focusManager.clearFocus()
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(word, style = MaterialTheme.typography.titleMedium)
                            Text(
                                chinese,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        if (definition != null && showDefinitionSheet) {
            ModalBottomSheet(
                onDismissRequest = { showDefinitionSheet = false },
                sheetState = sheetState,
                containerColor = Color.White,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 40.dp, height = 4.dp)
                                .background(Color.Gray, shape = RoundedCornerShape(2.dp))
                        )
                    }
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = screenHeightDp * 0.8f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text("释义", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))

                        // ✅ ⬇️ 替换这里
                        val highlightColor = Color.Blue
                        val baseColor = Color.Black
                        val annotatedText = buildAnnotatedString {
                            val keyword = input.trim()
                            val def = definition ?: ""

                            var start = 0
                            while (start < def.length) {
                                val index = def.indexOf(keyword, start, ignoreCase = true)
                                if (index == -1) {
                                    append(def.substring(start))
                                    break
                                } else {
                                    append(def.substring(start, index))
                                    withStyle(style = SpanStyle(color = highlightColor)) {
                                        append(def.substring(index, index + keyword.length))
                                    }
                                    start = index + keyword.length
                                }
                            }
                        }

                        Text(
                            text = annotatedText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = baseColor
                        )
                    }
                }

            }

        }
    }
}






